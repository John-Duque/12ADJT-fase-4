# Infraestrutura (Terraform)

Toda a infraestrutura da AWS do projeto, descrita em Terraform ([ADR 0003](../docs/adr/0003-infraestrutura-como-codigo.md)) e aplicada pelo GitHub Actions na conta simplificada, região `us-east-2` ([ADR 0004](../docs/adr/0004-conta-aws-simplificada.md)).

## Estrutura

```
infra/
├── modules/      # um módulo por componente
│   ├── table/          # tabela do DynamoDB com o índice por-dia
│   ├── queue/          # fila SQS com DLQ e política só HTTPS
│   ├── notification/   # tópico SNS com assinaturas de e-mail
│   ├── function/       # Lambda Java 21 com SnapStart, papel IAM, logs, alias live e Function URL opcional
│   └── schedule/       # agenda do EventBridge Scheduler com papel IAM próprio
└── envs/
    └── dev/      # o único ambiente: chama os módulos
        ├── versions.tf   # versões do Terraform e do provider, state no HCP Terraform
        ├── variables.tf
        ├── main.tf
        └── outputs.tf
```

- **State**: HCP Terraform, organização `john-duque-12adjt`, workspace `12adjt-fase-4-dev` em *execution mode Local* (o HCP só guarda o state e faz o lock; o Terraform roda no GitHub Actions).
- **Provider**: `hashicorp/aws`, com as tags `projeto`, `ambiente` e `gerenciado-por` em todos os recursos.
- **`.terraform.lock.hcl`** é versionado, com hashes para Linux (CI) e macOS.

## Recursos

Todos dentro do sempre gratuito da AWS.

| Recurso | Nome | Configuração |
|---|---|---|
| Tabela DynamoDB | `feedbacks` | Provisionada, 5 RCU / 5 WCU, partição `id`; criptografia padrão (chave da AWS); sem PITR nem backups |
| Índice da tabela | `por-dia` | Partição `createdDay`, ordenação `createdAt`, projeção completa, 5 RCU / 5 WCU (tabela + índice = 10/10 de 25/25 grátis) |
| Fila SQS | `critical-feedbacks` | Standard, SSE-SQS, *long polling* de 20 s, retenção de 4 dias, visibilidade de 180 s (6× o timeout de 30 s do notifier) |
| DLQ | `critical-feedbacks-dlq` | Recebe a mensagem depois de 3 tentativas (`maxReceiveCount`); retenção de 14 dias; só aceita mensagens da fila principal |
| Tópico SNS | `feedbacks-criticos` | E-mail de urgência; criptografado com `alias/aws/sns` |
| Tópico SNS | `relatorio-semanal` | Relatório semanal; criptografado com `alias/aws/sns` |
| Lambda | `feedback-ingestion` | Java 21, 512 MB, 15 s, SnapStart; **Function URL** pública (HTTPS, sem autenticação) no alias `live`; papel `feedback-ingestion` |
| Lambda | `critical-notifier` | Java 21, 512 MB, 30 s, SnapStart; acionada pela fila (*event source mapping* com `ReportBatchItemFailures`, lotes de 10, no máximo 2 execuções simultâneas); papel `feedback-critical-notifier` |
| Lambda | `weekly-report` | Java 21, 512 MB, 60 s, SnapStart; acionada pela agenda; papel `feedback-weekly-report` |
| Agenda | `feedback-weekly-report` | EventBridge Scheduler, `cron(0 8 ? * MON *)` em `America/Sao_Paulo`, chama o alias `live` do report com até 2 novas tentativas; papel `feedback-scheduler` |
| Logs | `/aws/lambda/<função>` | Um grupo por função, retenção de 7 dias |

Detalhes das funções:

- **Pacote**: o `function.zip` de cada função é gerado por `mvn package -Plambda` e enviado direto para a Lambda (até 50 MB, sem bucket S3). Cada mudança no zip publica uma versão nova e move o alias `live`; o SnapStart (sem custo para Java) só vale para versões publicadas, por isso URL, fila e agenda chamam o alias.
- **Variáveis de ambiente**: `FEEDBACK_TABLE`, `CRITICAL_QUEUE_URL` (ingestion), `CRITICAL_TOPIC_ARN` (notifier), `FEEDBACK_DAY_INDEX` e `REPORT_TOPIC_ARN` (report), todas vindas dos outros módulos; `JAVA_TOOL_OPTIONS` com o JIT só no nível 1 (inicialização mais rápida).
- **Permissões** (privilégio mínimo, detalhes em [`docs/seguranca.md`](../docs/seguranca.md)): cada função só grava no próprio grupo de logs e só acessa o recurso de que precisa; quem publica nos tópicos usa a chave `aws/sns` só pelo SNS (`kms:ViaService`).
- **Fila**: a política da `critical-feedbacks` recusa qualquer acesso sem HTTPS e libera `SendMessage` só para o papel da ingestion e o consumo só para o papel do notifier; a DLQ também recusa acesso sem HTTPS.
- **Limite de requisições**: a variável `ingestion_reserved_concurrency` reservaria execuções simultâneas para a ingestion, mas fica em `-1` (sem reserva): a conta tem limite de 10 execuções simultâneas e a AWS exige que 10 fiquem sem reserva (`aws lambda get-account-settings`). O limite da conta já serve de teto (o excesso recebe `429`); detalhes em [`docs/seguranca.md`](../docs/seguranca.md).
- **Custo**: tudo no sempre gratuito (1 milhão de invocações e 400 mil GB-segundo por mês na Lambda). O *event source mapping* faz *long polling* contínuo na fila: algo como 650 mil requisições por mês, dentro do 1 milhão grátis do SQS.

Depois do primeiro `apply`, a URL da ingestão aparece no resumo da execução (`POST <url>avaliacao`) e na saída `ingestion_url`.

Os e-mails dos administradores vêm do secret `ADMIN_EMAILS`, nunca do código. O formato é um objeto com um apelido por administrador: `{"john":"admin@exemplo.com"}`. Cada e-mail é assinado nos dois tópicos, e o administrador precisa clicar em **Confirm subscription** no e-mail que a AWS envia depois do `apply`. A variável é sensível: o plan mostra só os apelidos. Sem o secret, os tópicos são criados sem assinaturas.

Teste de envio depois da confirmação (CloudShell):

```bash
aws sns publish --topic-arn "$(aws sns list-topics --query "Topics[?ends_with(TopicArn,':feedbacks-criticos')].TopicArn" --output text)" --subject "Teste" --message "Mensagem de teste do feedbacks-criticos"
```

## Pipeline

Workflow [`.github/workflows/infra.yml`](../.github/workflows/infra.yml):

| Evento | O que roda | Credenciais |
|---|---|---|
| PR para `develop` ou `main` que altera `infra/**`, `functions/**`, `shared/**` ou o `pom.xml` raiz | `fmt -check`, `mvn package -Plambda`, `init`, `validate` e `plan` | `ci-terraform-plan` → papel `github-terraform-plan` (somente leitura) |
| `push` na `main` com as mesmas mudanças | `mvn package -Plambda`, `apply` (publica as funções) e teste rápido (`GET <url>q/health/ready` da ingestão) | `ci-deploy` → papel `github-deploy`, no Environment `production` |
| Manual (*Run workflow* na `main`) | `apply` ou `destroy` | Igual ao `push` na `main` |

O deploy das funções é o próprio `apply`: o Terraform compara o hash de cada `function.zip` e, se mudou, envia o código, publica a versão e move o alias `live`. Como o zip muda a cada build, todo `apply` publica versões novas das três funções.

O plan completo fica só no log da execução, com o ID da conta mascarado. O resumo da página (público) mostra só a lista de recursos e a contagem (`Plan: X to add...`), sem ARNs.

Secrets usados:

| Secret | Onde | Para quê |
|---|---|---|
| `TF_TOKEN_app_terraform_io` | Repositório | Acesso ao state no HCP Terraform |
| `ADMIN_EMAILS` | Repositório | E-mails dos administradores assinados nos tópicos SNS (`{"apelido":"email"}`) |
| `AWS_PLAN_ACCESS_KEY_ID`, `AWS_PLAN_SECRET_ACCESS_KEY` | Repositório | Chave do `ci-terraform-plan`, que só assume o papel de leitura |
| `AWS_DEPLOY_ACCESS_KEY_ID`, `AWS_DEPLOY_SECRET_ACCESS_KEY` | Environment `production` | Chave do `ci-deploy`, que só assume o papel de deploy |

## Comandos

Normalmente nada roda na máquina local: o `plan` aparece no PR e o `apply` acontece no merge na `main`. Para rodar à mão:

```bash
cd infra/envs/dev
terraform login                  # token do HCP Terraform (uma vez)
terraform init                   # baixa o provider e conecta no state remoto
terraform fmt -recursive ../..   # formata
terraform validate               # confere a sintaxe
terraform plan                   # mostra o que vai mudar
terraform apply                  # cria ou atualiza
terraform destroy                # remove tudo (só depois da nota)
```

`plan`, `apply` e `destroy` precisam de credenciais da AWS. Como ninguém tem chave de acesso pessoal ([`docs/seguranca.md`](../docs/seguranca.md)), o caminho manual é o **CloudShell** (instalar o Terraform lá e rodar os mesmos comandos) ou o *Run workflow* do GitHub Actions.

Para validar só a sintaxe, sem credenciais nem state:

```bash
terraform init -backend=false && terraform validate
```

## Destruir no fim da avaliação

GitHub → **Actions → Infra → Run workflow**, branch `main`, ação `destroy`. Remove todos os recursos criados pelo Terraform; os usuários e papéis do CI (criados à mão, ver `docs/seguranca.md`) são apagados depois, no CloudShell.
