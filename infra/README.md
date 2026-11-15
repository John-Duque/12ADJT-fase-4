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
│   ├── schedule/       # agenda do EventBridge Scheduler com papel IAM próprio
│   └── monitoring/     # dashboard do CloudWatch e consultas salvas do Logs Insights
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
| Tópico SNS | `alertas-administradores` | Alarmes do CloudWatch; **sem** criptografia, porque o CloudWatch não consegue publicar com a chave `aws/sns` (a mensagem só traz o nome e o estado do alarme) |
| Lambda | `feedback-ingestion` | Java 21, 512 MB, 15 s, SnapStart; **Function URL** pública (HTTPS, sem autenticação) no alias `live`; papel `feedback-ingestion` |
| Lambda | `critical-notifier` | Java 21, 512 MB, 30 s, SnapStart; acionada pela fila (*event source mapping* com `ReportBatchItemFailures`, lotes de 10, no máximo 2 execuções simultâneas); papel `feedback-critical-notifier` |
| Lambda | `weekly-report` | Java 21, 512 MB, 60 s, SnapStart; acionada pela agenda; papel `feedback-weekly-report` |
| Agenda | `feedback-weekly-report` | EventBridge Scheduler, `cron(0 8 ? * MON *)` em `America/Sao_Paulo`, chama o alias `live` do report com até 2 novas tentativas; papel `feedback-scheduler` |
| Logs | `/aws/lambda/<função>` | Um grupo por função, retenção de 7 dias |
| Dashboard | `feedback-plataforma` | CloudWatch: invocações, erros, *throttles* e duração das 3 funções; fila e DLQ; e-mails do SNS; capacidade do DynamoDB; estado dos alarmes; quadro com os erros recentes dos logs (26 métricas) |
| Consultas salvas | pasta `feedback` | Logs Insights: `rastrear-feedback-por-id`, `erros` e `duracao-por-funcao`, nos 3 grupos de logs |
| Alarmes | 5 alarmes do CloudWatch | Erros em cada função, mensagens na DLQ e *throttles* na ingestion; avisam pelo `alertas-administradores` (detalhes em [Monitoramento](#monitoramento)) |

Detalhes das funções:

- **Pacote**: o `function.zip` de cada função é gerado por `mvn package -Plambda` e enviado direto para a Lambda (até 50 MB, sem bucket S3). Cada mudança no zip publica uma versão nova e move o alias `live`; o SnapStart (sem custo para Java) só vale para versões publicadas, por isso URL, fila e agenda chamam o alias. A versão nova só atende depois que o snapshot do SnapStart fica pronto (alguns minutos): o módulo espera com `aws lambda wait published-version-active` (AWS CLI, já instalado no runner do GitHub e no CloudShell) antes de mover o alias, para o deploy não deixar a função fora do ar.
- **Variáveis de ambiente**: `FEEDBACK_TABLE`, `CRITICAL_QUEUE_URL` (ingestion), `CRITICAL_TOPIC_ARN` (notifier), `FEEDBACK_DAY_INDEX` e `REPORT_TOPIC_ARN` (report), todas vindas dos outros módulos; `JAVA_TOOL_OPTIONS` com o JIT só no nível 1 (inicialização mais rápida).
- **Permissões** (privilégio mínimo, detalhes em [`docs/seguranca.md`](../docs/seguranca.md)): cada função só grava no próprio grupo de logs e só acessa o recurso de que precisa; quem publica nos tópicos usa a chave `aws/sns` só pelo SNS (`kms:ViaService`).
- **Fila**: a política da `critical-feedbacks` recusa qualquer acesso sem HTTPS e libera `SendMessage` só para o papel da ingestion e o consumo só para o papel do notifier; a DLQ também recusa acesso sem HTTPS.
- **Limite de requisições**: a variável `ingestion_reserved_concurrency` reservaria execuções simultâneas para a ingestion, mas fica em `-1` (sem reserva): a conta tem limite de 10 execuções simultâneas e a AWS exige que 10 fiquem sem reserva (`aws lambda get-account-settings`). O limite da conta já serve de teto (o excesso recebe `429`); detalhes em [`docs/seguranca.md`](../docs/seguranca.md).
- **Custo**: tudo no sempre gratuito (1 milhão de invocações e 400 mil GB-segundo por mês na Lambda). O *event source mapping* faz *long polling* contínuo na fila: algo como 650 mil requisições por mês, dentro do 1 milhão grátis do SQS.

Depois do primeiro `apply`, a URL da ingestão aparece no resumo da execução (`POST <url>avaliacao`) e na saída `ingestion_url`.

Os e-mails dos administradores vêm do secret `ADMIN_EMAILS`, nunca do código. O formato é um objeto com um apelido por administrador: `{"john":"admin@exemplo.com"}`. Cada e-mail é assinado nos três tópicos, e o administrador precisa clicar em **Confirm subscription** no e-mail que a AWS envia depois do `apply`. A variável é sensível: o plan mostra só os apelidos. Sem o secret, os tópicos são criados sem assinaturas.

Teste de envio depois da confirmação (CloudShell):

```bash
aws sns publish --topic-arn "$(aws sns list-topics --query "Topics[?ends_with(TopicArn,':feedbacks-criticos')].TopicArn" --output text)" --subject "Teste" --message "Mensagem de teste do feedbacks-criticos"
```

## Monitoramento

- **Dashboard**: CloudWatch → **Painéis** → `feedback-plataforma` (saída `dashboard_url`). Os gráficos usam períodos de 5 minutos; a DLQ aparece como número, porque qualquer valor acima de 0 é um evento com falha.
- **Rastrear um feedback**: CloudWatch → **Logs Insights** → **Consultas salvas** → `feedback/rastrear-feedback-por-id`, troque `COLE-O-ID-DO-FEEDBACK-AQUI` pelo `id` devolvido no `POST /avaliacao` e execute. Aparecem as linhas das funções por onde ele passou:

  | Função | Log |
  |---|---|
  | `feedback-ingestion` | `Feedback gravado: id=... urgencia=...` e, se crítico, `Feedback crítico publicado na fila: id=...` |
  | `critical-notifier` | `Feedback crítico notificado: id=...` ou `Falha ao notificar feedback crítico: id=...` |

- **Erros** (`feedback/erros`) e **duração por função** (`feedback/duracao-por-funcao`, das linhas `REPORT` da Lambda) completam as consultas. Todas mostram só o nome da função, sem o ID da conta.
- **Alarmes**: avaliados a cada 5 minutos; mandam e-mail pelo tópico `alertas-administradores` quando entram em alarme e quando voltam ao normal.

  | Alarme | Dispara quando | O que significa |
  |---|---|---|
  | `feedback-ingestion-erros`, `critical-notifier-erros`, `weekly-report-erros` | `Errors` > 0 em 5 min | Exceção não tratada ou timeout na função; ver `feedback/erros` |
  | `critical-feedbacks-dlq-mensagens` | Mensagens visíveis na DLQ > 0 | Um feedback crítico falhou 3 vezes e o e-mail de urgência não saiu. Esse caso não aparece no alarme de erros do notifier, porque ele devolve só a mensagem com falha e a execução termina sem erro |
  | `feedback-ingestion-throttles` | `Throttles` > 0 em 5 min | A URL recusou requisições (`429`) por falta de execuções simultâneas (limite de 10 da conta) |

  Teste no CloudShell (o alarme volta para `OK` sozinho na próxima avaliação, e os dois e-mails chegam):

  ```bash
  aws cloudwatch set-alarm-state --alarm-name feedback-ingestion-erros --state-value ALARM --state-reason "Teste do alarme"
  ```

  Enquanto houver mensagens na DLQ, o alarme dela continua em `ALARM` (o e-mail sai só na mudança de estado). Depois de analisar, devolva as mensagens à fila (**Iniciar redirecionamento da DLQ**) ou limpe a DLQ (**Limpar**).
- **Custo**: até 3 dashboards com 50 métricas cada e 10 alarmes são sempre gratuitos; consultas salvas não têm custo e o que o Logs Insights lê entra nos 5 GB/mês gratuitos do CloudWatch Logs; o volume de logs das funções é pequeno.

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
