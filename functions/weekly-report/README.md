# weekly-report

Função que consolida a semana: busca as avaliações dos 7 dias anteriores, monta o relatório (quantidade por dia, por urgência e média das notas) e entrega aos administradores.

| | |
|---|---|
| Gatilho | Agendamento semanal (Funqy); localmente, HTTP `POST /generate-weekly-report` |
| Porta local | `8083` |
| Imagem | `ghcr.io/john-duque/12adjt-fase-4/weekly-report` |
| Na AWS | Lambda agendada pelo EventBridge Scheduler (segunda, 8h de São Paulo); lê o DynamoDB e envia o e-mail pelo tópico SNS `relatorio-semanal` (build com o profile `lambda`) |

## Contrato

### Entrada

Nenhuma: o período é calculado a partir da data atual no fuso configurado. São os 7 dias completos anteriores a hoje (ex.: executado em 13/10, cobre de 06/10 a 12/10).

### Saída `200 OK`

```json
{
  "startDate": "2026-10-19",
  "endDate": "2026-10-25",
  "totalFeedbacks": 0,
  "averageRating": 0.0,
  "countByDay": {"2026-10-19": 0, "2026-10-20": 0, "2026-10-21": 0, "2026-10-22": 0, "2026-10-23": 0, "2026-10-24": 0, "2026-10-25": 0},
  "countByUrgency": {"HIGH": 0, "MEDIUM": 0, "LOW": 0},
  "feedbacks": []
}
```

| Campo | Conteúdo |
|---|---|
| `startDate`, `endDate` | Período do relatório |
| `totalFeedbacks` | Total de avaliações no período |
| `averageRating` | Média das notas com 2 casas (0 sem avaliações) |
| `countByDay` | Todos os dias do período, inclusive os sem avaliação (0) |
| `countByUrgency` | Todas as urgências, inclusive as sem avaliação (0) |
| `feedbacks` | Avaliações ordenadas pela data, com `description`, `urgency` e `createdAt` |

O dia de cada avaliação é calculado no fuso configurado: uma avaliação às 02:00 UTC do dia 7 conta no dia 6 em São Paulo.

## Como rodar

```bash
make dev-report                                                           # modo dev, com hot reload
docker compose up --build -d weekly-report                                # só esta função, pelo compose
docker run -p 8083:8083 ghcr.io/john-duque/12adjt-fase-4/weekly-report:develop        # imagem publicada
```

## Como testar

Cada comando em uma linha só:

```bash
curl -i -X POST localhost:8083/generate-weekly-report
curl localhost:8083/q/health
```

Esperado: `200` com o relatório da semana anterior e `{"status":"UP",...}`. No log:

```
Relatório 2026-10-19 a 2026-10-25: total=0 media=0.00 porUrgencia={HIGH=0, MEDIUM=0, LOW=0}
```

Enquanto não houver banco, o relatório sai sempre zerado: o gateway provisório não tem avaliações para ler (as do `feedback-ingestion` ficam na memória do outro container).

Testes automatizados: `mvn -pl functions/weekly-report -am verify`.

## Camadas

| Camada | Classes |
|---|---|
| domain | `WeeklyReport` (neste módulo); `Feedback` e `Urgency` (`shared/feedback-domain`) |
| application | `GenerateWeeklyReportUseCase`, gateways `FeedbackQueryGateway` e `ReportDeliveryGateway` |
| infra | `WeeklyReportJob` (`@Funq("generate-weekly-report")`), `UseCaseConfig`, `AwsClientConfig`, gateways provisórios e reais (abaixo) |

Os gateways dependem do build:

| Build | Leitura | Entrega | Gatilho |
|---|---|---|---|
| Padrão (dev mode, Docker, compose, testes) | `NoOpFeedbackQueryGateway` (lista vazia) | `LoggingReportDeliveryGateway` (só loga) | HTTP, pelo `quarkus-funqy-http` (profile `http`, ativo por padrão) |
| Profile `lambda` (AWS) | `DynamoDbFeedbackQueryGateway` (`Query` no índice `por-dia`) | `SnsReportDeliveryGateway` (`Publish` no tópico) | EventBridge Scheduler, pelo `quarkus-funqy-amazon-lambda` |

A troca é feita no build pela propriedade `feedback.aws.enabled` (`@IfBuildProperty`), que só o profile `lambda` liga. Com `-Plambda`, o Maven desliga o profile `http`: o zip da Lambda não leva servidor HTTP nem health check. Domain e application não mudam.

## Build para a AWS

```bash
mvn -pl functions/weekly-report -am package -Plambda -DskipTests   # gera functions/weekly-report/target/function.zip
```

| | |
|---|---|
| Runtime | `java21` |
| Handler | `io.quarkus.funqy.lambda.FunqyStreamHandler::handleRequest` |
| Gatilho | EventBridge Scheduler, `cron(0 8 ? * MON *)` no fuso `America/Sao_Paulo` |

### Leitura no DynamoDB

O período são os 7 dias completos anteriores à execução, no fuso de São Paulo; com o agendamento de segunda, vai de segunda a domingo da semana anterior (por exemplo, `2026-10-06T03:00Z` até `2026-10-13T03:00Z`, sem incluir o fim). O índice `por-dia` tem o dia **UTC** (`createdDay`) como partição, então a função faz uma `Query` para cada dia UTC que o período toca (8 consultas numa semana) com `createdAt BETWEEN` as bordas do período. O `createdAt` é gravado com tamanho fixo (9 casas nos segundos), por isso a comparação como texto segue a ordem do tempo. As páginas de cada consulta são lidas até o fim.

### E-mail do relatório

```
Assunto: Relatório semanal de avaliações: 06/10/2026 a 12/10/2026

Relatório semanal de avaliações
Período: 06/10/2026 a 12/10/2026 (America/Sao_Paulo)

Total de avaliações: 3
Média das notas: 5,00

Quantidade por urgência:
- HIGH: 1
- MEDIUM: 1
- LOW: 1

Quantidade por dia:
- 06/10/2026: 1
- 07/10/2026: 2
- 08/10/2026: 0
...

Avaliações (data de envio | urgência | descrição):
- 06/10/2026 09:00:00 | HIGH | Aula sem áudio
- 07/10/2026 12:00:00 | MEDIUM | Aula média
- 07/10/2026 15:30:00 | LOW | Aula ótima
```

Uma mensagem do SNS tem no máximo 256 KB: se a lista passar disso (cerca de 230 avaliações com descrição de 1000 caracteres), ela é cortada e o e-mail termina com `... e mais N avaliações`. Total, média e contagens continuam completos. Se o SNS falhar, a execução falha e o agendamento tenta de novo.

## Configuração

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8083` (só no build padrão) |
| `quarkus.funqy.export` | — | `generate-weekly-report` |
| `report.zone-id` | `REPORT_ZONE_ID` | `America/Sao_Paulo` (fecha os dias do relatório e formata as datas do e-mail) |
| `feedback.table` | `FEEDBACK_TABLE` | `feedbacks` (só no profile `lambda`) |
| `feedback.day-index` | `FEEDBACK_DAY_INDEX` | `por-dia` (só no profile `lambda`) |
| `report.topic.arn` | `REPORT_TOPIC_ARN` | sem padrão; obrigatória no profile `lambda` |

Na Lambda, a região e as credenciais vêm do próprio ambiente (`AWS_REGION` e o papel IAM da função). O papel precisa de `dynamodb:Query` no índice `por-dia`, `sns:Publish` no tópico e `kms:GenerateDataKey`/`kms:Decrypt` na chave `aws/sns` (o tópico é criptografado).
