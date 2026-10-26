# feedback-ingestion

Função de entrada da plataforma: recebe a avaliação do estudante, valida, classifica a urgência pela nota, grava e publica as avaliações críticas para o `critical-notifier`.

| | |
|---|---|
| Gatilho | HTTP `POST /avaliacao` (Quarkus REST) |
| Porta local | `8081` |
| Imagem | `ghcr.io/john-duque/12adjt-fase-4/feedback-ingestion` |
| Na AWS | Lambda exposta por Function URL (milestone 8) |

## Contrato

### Requisição

```json
{"descricao": "Aula sem áudio", "nota": 2}
```

| Campo | Tipo | Regra |
|---|---|---|
| `descricao` | texto | obrigatória, não pode ser em branco, até 1000 caracteres |
| `nota` | inteiro | obrigatória, de 0 a 10 |

### Resposta `201 Created`

```json
{"id": "04efaa14-658b-4c6f-b550-f4bd5bd788a7", "urgencia": "HIGH", "dataEnvio": "2026-10-26T18:26:09.210797842Z"}
```

A urgência vem da nota:

| Nota | Urgência | Crítica? |
|---|---|---|
| 0 a 3 | `HIGH` | sim: publica o evento para o `critical-notifier` |
| 4 a 6 | `MEDIUM` | não |
| 7 a 10 | `LOW` | não |

### Erros `400 Bad Request`

| Caso | Mensagem |
|---|---|
| Sem corpo | `O corpo da requisição é obrigatório` |
| Sem `descricao` ou em branco | `A descrição é obrigatória` |
| `descricao` com mais de 1000 caracteres | `A descrição deve ter no máximo 1000 caracteres` |
| Sem `nota` | `A nota é obrigatória` |
| `nota` menor que 0 | `A nota deve ser no mínimo 0` |
| `nota` maior que 10 | `A nota deve ser no máximo 10` |

Erros de validação vêm no formato do Quarkus (`{"title":"Constraint Violation","status":400,"violations":[{"field":"...","message":"..."}]}`). Regras de negócio violadas no domain viram `{"mensagem": "..."}`.

## Como rodar

```bash
make dev-ingestion                                                        # modo dev, com hot reload
docker compose up --build -d feedback-ingestion                           # só esta função, pelo compose
docker run -p 8081:8081 ghcr.io/john-duque/12adjt-fase-4/feedback-ingestion:develop   # imagem publicada
```

## Como testar

Cada comando em uma linha só:

```bash
curl -i -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' -d '{"descricao":"Aula muito boa","nota":9}'
curl -i -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' -d '{"descricao":"Aula sem áudio","nota":2}'
curl -i -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' -d '{"descricao":"Teste","nota":11}'
curl localhost:8081/q/health
```

Esperado: `201` com `"urgencia":"LOW"`, `201` com `"urgencia":"HIGH"`, `400` com `A nota deve ser no máximo 10` e `{"status":"UP",...}`. A avaliação crítica gera no log:

```
Feedback crítico publicado: id=... urgencia=HIGH
```

Testes automatizados: `mvn -pl functions/feedback-ingestion -am verify`.

## Camadas

| Camada | Classes |
|---|---|
| domain (`shared/feedback-domain`) | `Feedback`, `Urgency`, `CriticalFeedbackEvent`, `InvalidFeedbackException` |
| application | `ReceiveFeedbackUseCase`, `ReceiveFeedbackInput`, gateways `FeedbackGateway` e `CriticalFeedbackEventGateway` |
| infra | `FeedbackController` e DTOs (`FeedbackRequest`, `FeedbackResponse`), `GlobalExceptionHandler`, `UseCaseConfig` |

Gateways provisórios até a integração com a AWS: `InMemoryFeedbackGateway` (grava em memória) e `LoggingCriticalFeedbackEventGateway` (só loga o evento). As implementações reais (DynamoDB e SQS) os substituem sem mudar domain nem application.

## Configuração

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` | `8081` |
