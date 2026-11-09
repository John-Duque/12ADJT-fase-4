# 0002 - Provedor de nuvem: AWS

- **Status:** Aceita
- **Data:** 2026-10-26

## Contexto

O Tech Challenge pede as funções **serverless** rodando em **nuvem**, com componentes de suporte (banco, fila, e-mail, agendamento), **deploy automatizado**, **monitoramento**, **notificações automáticas aos administradores** e **segurança dos dados e governança de acesso**. O ADR 0001 deixou `domain` e `application` sem depender de provedor; falta escolher a nuvem onde os gateways reais e a infraestrutura serão criados.

Restrições:

- **Custo zero**: projeto acadêmico, então só entram serviços **sempre gratuitos**. Ofertas grátis só nos 12 primeiros meses ficam de fora, e os créditos de conta nova são só margem de segurança, sem uso planejado.
- O projeto precisa ficar no ar até a nota, de 3 a 4 meses depois da entrega.
- As funções são Java 21 com Quarkus: REST na `feedback-ingestion` e Funqy na `critical-notifier` (fila) e na `weekly-report` (agendamento).

## Opções consideradas

### Provedor

| Critério | AWS | GCP | Azure | Render |
|---|---|---|---|---|
| Custo (sempre gratuito) | Lambda, DynamoDB provisionado, SQS, SNS, EventBridge Scheduler e CloudWatch dentro do gratuito | Cloud Run functions, Firestore, Pub/Sub e Cloud Scheduler dentro do gratuito | Functions (consumo), Cosmos DB e Application Insights dentro do gratuito; créditos de estudante sem cartão | Plano gratuito só para serviços web que hibernam |
| Funções | Java 21; `quarkus-amazon-lambda-http` e `quarkus-funqy-amazon-lambda` (com SQS e agendamento) | Java 21; extensões Quarkus para HTTP e Funqy com eventos | Java 21; Funqy só com gatilho HTTP | Containers sempre ligados (não é FaaS); o produto serverless (Workflows) não aceita Java |
| Componentes | Todos nativos: entrada HTTP, banco, fila com DLQ, e-mail e agendador | Sem e-mail nativo (serviço externo a mais) | Todos nativos | Sem fila gerenciada, DLQ nem e-mail |
| Observabilidade | CloudWatch: logs, métricas, dashboards e alarmes | Cloud Logging e Cloud Monitoring | Application Insights e Azure Monitor | Logs e métricas básicas do painel |
| Segurança | IAM com papel por função, Parameter Store, OIDC com GitHub Actions | IAM, Secret Manager, Workload Identity Federation com GitHub | Entra ID, Key Vault, OIDC com GitHub Actions | Variáveis de ambiente por serviço |

1. **AWS** — prós: Lambda é FaaS no sentido literal; extensões oficiais do Quarkus para HTTP e para Funqy com SQS; todos os componentes são nativos, inclusive o e-mail (SNS); SnapStart reduz o *cold start* do Java sem custo. Contras: o gratuito de alguns serviços populares (API Gateway, SES) vale só 12 meses; a conta nova no *Free plan* expira em 6 meses.
2. **GCP** — prós: bom em serverless e com gratuito generoso. Contras: sem e-mail nativo, o que exige um serviço externo a mais para o alerta e o relatório.
3. **Azure** — prós: e-mail, timer e Application Insights integrados; créditos de estudante sem cartão. Contras: o pior *cold start* do Java entre os três; Funqy só com HTTP, então a fila e o agendamento exigiriam código específico do Azure.
4. **Render** — descartado: é PaaS de containers sempre ligados, não serverless; sem fila gerenciada, DLQ nem e-mail.

### Entrada HTTP na AWS

1. **API Gateway (HTTP API)** — prós: limite de requisições por rota, autorizadores, chaves de API, domínio próprio e integração com WAF. Contras: o gratuito vale só 12 meses (e não existe nas contas do *Free plan*, que usam créditos); depois é cobrado por milhão de requisições.
2. **Lambda Function URL** — prós: endpoint HTTPS da própria Lambda, sem custo além da invocação; usa o mesmo evento (*payload* 2.0) da HTTP API, então o `quarkus-amazon-lambda-http` atende as duas sem mudar o código. Contras: sem limite por rota nem autorizador; autenticação só `NONE` ou `AWS_IAM`.

## Decisão

**AWS**, na região **`us-east-1`** (todos os serviços disponíveis e referência da maioria da documentação).

A entrada HTTP é a **Lambda Function URL**: o `POST /avaliacao` é público (estudantes enviam sem login), a validação já é feita pela aplicação (Bean Validation e domínio) e o limite de requisições fica com a *reserved concurrency* da função. Se o projeto precisar de autorizador, domínio próprio ou WAF, trocar para API Gateway muda só a infraestrutura.

| Componente | Serviço AWS |
|---|---|
| Entrada HTTP (`POST /avaliacao`) | Lambda Function URL (HTTPS, sem custo) |
| Funções | Lambda (Java 21) com SnapStart: `feedback-ingestion`, `critical-notifier`, `weekly-report` (1 milhão de requisições/mês grátis; SnapStart sem custo para Java) |
| Banco | DynamoDB **provisionado**: 5 RCU / 5 WCU na tabela e no índice (o gratuito cobre 25/25) |
| Fila de críticos e DLQ | SQS + DLQ (redrive policy; 1 milhão de requisições/mês grátis) |
| E-mail | SNS com assinaturas de e-mail (1.000 e-mails/mês grátis) |
| Agendador | EventBridge Scheduler (14 milhões de invocações/mês grátis) |
| Logs, métricas e dashboard | CloudWatch Logs, Metrics e Dashboards (5 GB de logs e 3 dashboards grátis) |
| Alarmes aos administradores | CloudWatch Alarms → SNS (e-mail) (10 alarmes grátis) |
| Configuração e criptografia | Variáveis de ambiente e SSM Parameter Store padrão; chaves gerenciadas pela AWS |
| Acesso e auditoria | IAM (papel por função, privilégio mínimo), Event history do CloudTrail, AWS Budgets (*zero spend budget*) |
| Deploy | GitHub Actions com OIDC (sem chave de acesso no repositório) |

**Serviços evitados por gerar custo:** API Gateway e SES (grátis só nos 12 primeiros meses), DynamoDB *on-demand*, point-in-time recovery e backups, Secrets Manager, chave KMS própria, trail do CloudTrail, VPC/NAT Gateway, *provisioned concurrency* e bucket S3.

## Consequências

- **Todos os componentes na mesma nuvem**: fila, e-mail, agendamento e monitoramento são nativos, sem serviço externo nem segredo de terceiros.
- **Código das funções quase não muda**: as extensões AWS entram num profile Maven (`lambda`); os gateways reais (`DynamoDb*`, `Sqs*`, `Sns*`) substituem os provisórios graças ao `@DefaultBean`.
- **Custo zero com vigilância**: o *zero spend budget* avisa de qualquer cobrança. A conta é criada no *Free plan* só na hora do deploy; se a avaliação passar dos 6 meses, é preciso fazer upgrade antes de expirar (o sempre gratuito continua sem custo).
- **Sem API Gateway**: a proteção do endpoint público depende da validação na aplicação e da *reserved concurrency*; não há autorizador nem limite por cliente.
- **DynamoDB provisionado exige modelagem prévia**: o relatório semanal precisa do índice `por-dia` (`createdDay` + `createdAt`), e a capacidade fixa limita picos de escrita.
- **Próxima decisão**: a ferramenta de IaC (ADR 0003).
