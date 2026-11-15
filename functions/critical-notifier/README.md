# critical-notifier

Função que avisa os administradores sobre avaliações críticas: recebe o evento publicado pelo `feedback-ingestion` e envia o alerta de urgência com descrição, urgência e data de envio.

| | |
|---|---|
| Gatilho | Mensagem na fila de críticos (Funqy); localmente, HTTP `POST /notify-critical-feedback` |
| Porta local | `8082` |
| Imagem | `ghcr.io/john-duque/12adjt-fase-4/critical-notifier` |
| Na AWS | Lambda acionada pela fila SQS `critical-feedbacks`; e-mail pelo tópico SNS `feedbacks-criticos` (build com o profile `lambda`) |

## Contrato

### Entrada: `CriticalFeedbackEvent`

Contrato interno da fila, com os campos em inglês:

```json
{"id":"8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}
```

### Saída `200 OK`

```json
{"feedbackId": "8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11", "notified": true}
```

| Evento | `notified` | O que acontece |
|---|---|---|
| Urgência `HIGH` | `true` | envia o alerta aos administradores |
| Urgência `MEDIUM` ou `LOW` | `false` | ignora e loga um aviso |
| Inválido (descrição em branco, urgência que não bate com a nota) | — | loga `Falha ao notificar feedback crítico: id=...` e responde `500`; na AWS a mensagem volta para a fila e, depois de 3 tentativas, vai para a DLQ |

O alerta tem exatamente os dados exigidos no e-mail de urgência: **descrição, urgência e data de envio**.

## Como rodar

```bash
make dev-notifier                                                         # modo dev, com hot reload
docker compose up --build -d critical-notifier                            # só esta função, pelo compose
docker run -p 8082:8082 ghcr.io/john-duque/12adjt-fase-4/critical-notifier:develop    # imagem publicada
```

## Como testar

Localmente não existe fila: o `curl` simula a mensagem que chegaria do `feedback-ingestion`. Cada comando em uma linha só:

```bash
curl -i -X POST localhost:8082/notify-critical-feedback -H 'Content-Type: application/json' -d '{"id":"8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}'
curl -i -X POST localhost:8082/notify-critical-feedback -H 'Content-Type: application/json' -d '{"id":"3c6e0b8a-9c0b-4b1e-8f7a-2d5e6f7a8b9c","description":"Aula ótima","rating":9,"urgency":"LOW","createdAt":"2026-10-13T12:00:00Z"}'
curl localhost:8082/q/health
```

Esperado: `"notified":true`, `"notified":false` e `{"status":"UP",...}`. No log:

```
[ALERTA] urgencia=HIGH dataEnvio=2026-10-13T12:00:00Z descricao=Aula sem áudio
Feedback crítico notificado: id=8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11
Evento ignorado: feedback 3c6e0b8a-... não é crítico (urgencia=LOW)
```

Testes automatizados: `mvn -pl functions/critical-notifier -am verify`.

## Camadas

| Camada | Classes |
|---|---|
| domain (`shared/feedback-domain`) | `Feedback`, `Urgency`, `CriticalFeedbackEvent` |
| application | `NotifyCriticalFeedbackUseCase`, `CriticalFeedbackAlert`, gateway `AdminNotificationGateway` |
| infra | `CriticalFeedbackConsumer` (`@Funq("notify-critical-feedback")`), `NotificationResult`, `UseCaseConfig`, `AwsClientConfig`, `LoggingAdminNotificationGateway`, `SnsAdminNotificationGateway` |

O gateway depende do build:

| Build | E-mail | Gatilho |
|---|---|---|
| Padrão (dev mode, Docker, compose, testes) | `LoggingAdminNotificationGateway` (só loga) | HTTP, pelo `quarkus-funqy-http` (profile `http`, ativo por padrão) |
| Profile `lambda` (AWS) | `SnsAdminNotificationGateway` (`Publish` no tópico) | Mensagens do SQS, pelo `quarkus-funqy-amazon-lambda` |

A troca é feita no build pela propriedade `feedback.aws.enabled` (`@IfBuildProperty`), que só o profile `lambda` liga. Com `-Plambda`, o Maven desliga o profile `http`: o zip da Lambda não leva servidor HTTP nem health check (cold start menor). Domain e application não mudam.

## Build para a AWS

```bash
mvn -pl functions/critical-notifier -am package -Plambda -DskipTests   # gera functions/critical-notifier/target/function.zip
```

| | |
|---|---|
| Runtime | `java21` |
| Handler | `io.quarkus.funqy.lambda.FunqyStreamHandler::handleRequest` |
| Gatilho | *Event source mapping* da fila `critical-feedbacks` com `ReportBatchItemFailures` |

O Funqy trata o evento do SQS (`quarkus.funqy.amazon-lambda.advanced-event-handling.enabled`, `true` por padrão): o corpo de cada mensagem vira um `CriticalFeedbackEvent` e a função roda uma vez por mensagem. Se uma mensagem falhar (evento inválido ou erro do SNS), só ela volta para a fila (`report-batch-item-failures`, também `true` por padrão); depois de 3 tentativas, vai para a DLQ `critical-feedbacks-dlq`.

E-mail enviado aos administradores (assinantes do tópico):

```
Assunto: [Urgência HIGH] Nova avaliação crítica

Uma avaliação crítica foi recebida na plataforma de feedback.

Descrição: Aula sem áudio
Urgência: HIGH
Data de envio: 13/10/2026 09:00:00 (America/Sao_Paulo)
```

## Na AWS

| Recurso | Configuração |
|---|---|
| Lambda `critical-notifier` | Java 21, 512 MB, timeout de 30 s, SnapStart; versão publicada atrás do alias `live` |
| Gatilho | *Event source mapping* da fila **SQS** `critical-feedbacks` para o alias `live`: lotes de até 10 mensagens, `ReportBatchItemFailures` e no máximo 2 execuções ao mesmo tempo |
| Fila | Visibilidade de 180 s (6× o timeout); depois de 3 tentativas a mensagem vai para a DLQ `critical-feedbacks-dlq` (retenção de 14 dias) |
| E-mail | Tópico **SNS** `feedbacks-criticos`, criptografado com `alias/aws/sns`, com uma assinatura de e-mail por administrador |
| Papel IAM | `feedback-critical-notifier` |
| Logs | `/aws/lambda/critical-notifier`, retenção de 7 dias |
| Alarmes | `critical-notifier-erros` (erro na execução) e `critical-feedbacks-dlq-mensagens` (mensagem na DLQ: um alerta não saiu depois de 3 tentativas) |

Permissões do papel (privilégio mínimo):

| Ação | Recurso |
|---|---|
| `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`, `sqs:ChangeMessageVisibility` | Fila `critical-feedbacks`; a política da própria fila também só libera o consumo para este papel |
| `sns:Publish` | Só o tópico `feedbacks-criticos` |
| `kms:GenerateDataKey*`, `kms:Decrypt` | Só quando a chamada vem do SNS (`kms:ViaService`) |
| `logs:CreateLogStream`, `logs:PutLogEvents` | Só o próprio grupo de logs |

Teste na nuvem: envie uma avaliação com nota de 0 a 3 para o `feedback-ingestion`, ou publique o evento direto na fila (CloudShell, em uma linha só):

```bash
aws sqs send-message --queue-url "$(aws sqs get-queue-url --queue-name critical-feedbacks --query QueueUrl --output text)" --message-body '{"id":"8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}'
```

Esperado: o e-mail `[Urgência HIGH] Nova avaliação crítica` em poucos segundos e `Feedback crítico notificado: id=...` no log. Com a descrição em branco (`"description":"   "`), o log mostra `Falha ao notificar feedback crítico`, a mensagem vai para a DLQ em cerca de 10 minutos e o alarme `critical-feedbacks-dlq-mensagens` manda um e-mail.

Evidências: seções 1 e 5 de [`docs/evidencias/teste-ponta-a-ponta.md`](../../docs/evidencias/teste-ponta-a-ponta.md).

## Configuração

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8082` (só no build padrão) |
| `quarkus.funqy.export` | — | `notify-critical-feedback` |
| `critical.topic.arn` | `CRITICAL_TOPIC_ARN` | sem padrão; obrigatória no profile `lambda` |
| `notification.zone-id` | `NOTIFICATION_ZONE_ID` | `America/Sao_Paulo` (fuso da data no e-mail) |

Na Lambda, a região e as credenciais vêm do próprio ambiente (`AWS_REGION` e o papel IAM da função). O tópico é criptografado com a chave `aws/sns`, então o papel precisa de `sns:Publish` no tópico e de `kms:GenerateDataKey` e `kms:Decrypt` na chave.
