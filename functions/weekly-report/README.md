# weekly-report

Função que consolida a semana: busca as avaliações dos 7 dias anteriores, monta o relatório (quantidade por dia, por urgência e média das notas) e entrega aos administradores.

| | |
|---|---|
| Gatilho | Agendamento semanal (Funqy); localmente, HTTP `POST /generate-weekly-report` |
| Porta local | `8083` |
| Imagem | `ghcr.io/john-duque/12adjt-fase-4/weekly-report` |
| Na AWS | Lambda agendada pelo EventBridge Scheduler (segunda, 8h de São Paulo); e-mail pelo SNS (milestone 8) |

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
| infra | `WeeklyReportJob` (`@Funq("generate-weekly-report")`), `UseCaseConfig` |

Gateways provisórios até a integração com a AWS: `NoOpFeedbackQueryGateway` (devolve lista vazia) e `LoggingReportDeliveryGateway` (só loga o resumo). As implementações reais (DynamoDB e SNS) os substituem sem mudar domain nem application.

## Configuração

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8083` |
| `report.zone-id` | `REPORT_ZONE_ID` | `America/Sao_Paulo` |
