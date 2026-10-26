# critical-notifier

Função que avisa os administradores sobre avaliações críticas: recebe o evento publicado pelo `feedback-ingestion` e envia o alerta de urgência com descrição, urgência e data de envio.

| | |
|---|---|
| Gatilho | Mensagem na fila de críticos (Funqy); localmente, HTTP `POST /notify-critical-feedback` |
| Porta local | `8082` |
| Imagem | `ghcr.io/john-duque/12adjt-fase-4/critical-notifier` |
| Na AWS | Lambda acionada pela fila SQS; e-mail pelo SNS (milestone 8) |

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
| Inválido (descrição em branco, urgência que não bate com a nota) | — | responde `500`; na AWS a mensagem volta para a fila e, depois de 3 tentativas, vai para a DLQ |

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
Evento ignorado: feedback 3c6e0b8a-... não é crítico (urgencia=LOW)
```

Testes automatizados: `mvn -pl functions/critical-notifier -am verify`.

## Camadas

| Camada | Classes |
|---|---|
| domain (`shared/feedback-domain`) | `Feedback`, `Urgency`, `CriticalFeedbackEvent` |
| application | `NotifyCriticalFeedbackUseCase`, `CriticalFeedbackAlert`, gateway `AdminNotificationGateway` |
| infra | `CriticalFeedbackConsumer` (`@Funq("notify-critical-feedback")`), `NotificationResult`, `UseCaseConfig` |

Gateway provisório até a integração com a AWS: `LoggingAdminNotificationGateway` (só loga o alerta). A implementação real (SNS) o substitui sem mudar domain nem application.

## Configuração

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8082` |
