# Segurança e governança de acesso

Como a conta AWS do projeto é protegida: quem acessa, com que permissão, como o CI faz deploy sem permissão direta e como tudo é auditado.

> **Rascunho (T30 e T35).** Esta versão cobre a conta, o acesso, as credenciais do CI, os papéis das funções e a proteção dos dados. A versão final é a da T48.

Decisões relacionadas: [ADR 0002](adr/0002-provedor-de-nuvem.md) (AWS e serviços sempre gratuitos) e [ADR 0004](adr/0004-conta-aws-simplificada.md) (conta simplificada, região `us-east-2` e CI sem OIDC).

## Resumo

| Controle | Como é feito |
|---|---|
| Acesso humano | AWS Builder ID com MFA em dois dispositivos; sem usuário raiz |
| Privilégio mínimo do CI | Dois usuários técnicos, cada um só pode assumir um papel |
| Credenciais temporárias | O CI recebe credenciais de 1 h ao assumir o papel |
| Privilégio mínimo das funções | Um papel IAM por função Lambda, só com o que ela usa |
| Criptografia | Chaves gerenciadas pela AWS em repouso; só HTTPS em trânsito |
| Proteção da API | Function URL só HTTPS, validação no código e *reserved concurrency* como limite |
| Custo | Plano gratuito, sem cobrança; só serviços sempre gratuitos |
| Auditoria | Histórico de eventos do CloudTrail (90 dias, gratuito) |

## 1. Conta e plano

| Item | Valor |
|---|---|
| Cadastro | Simplificado (*Sign up for AWS (new)*), um projeto numa organização gerenciada pela AWS |
| Plano | Gratuito: US$ 100 de créditos, sem cobranças enquanto não houver upgrade |
| Criação | 09/11/2026 |
| Fim do plano gratuito | **09/05/2027** |
| Região | `us-east-2` (Ohio), fixa no projeto |

A entrega é em 17/11/2026 e a avaliação leva de 3 a 4 meses, então o plano gratuito cobre todo o período. Se a avaliação passar de 09/05/2027, é preciso fazer upgrade antes dessa data (os serviços sempre gratuitos continuam sem custo).

Dois botões ficam **proibidos** no painel: **Atualizar** (upgrade para o plano pago) e **Continue com a ativação** (recursos avançados). Os dois são irreversíveis (ver ADR 0004).

![Plano gratuito no console](img/seguranca/01-console-free-plan.png)

![Projeto e região us-east-2](img/seguranca/02-projeto-regiao.png)

## 2. Acesso humano

- **Dono da conta**: AWS Builder ID. Não existe usuário raiz nesse tipo de cadastro. O console entra por um papel gerenciado pela AWS, com sessão temporária.
- **MFA**: dois dispositivos registrados, uma *passkey* (WebAuthn) e um app autenticador (TOTP). Se um se perder, o outro mantém o acesso.
- **E-mail de recuperação**: diferente do e-mail de login.
- **Sem chave de acesso pessoal**: quando é preciso usar a AWS CLI, o acesso é pelo **CloudShell**, que usa a sessão do console.
- **Usuários IAM só para programas**: a própria AWS avisa que, nesse cadastro, usuários IAM são só para acesso programático. Os únicos usuários IAM são os dois do CI (seção 5).

![MFA com dois dispositivos](img/seguranca/13-mfa-dois-dispositivos.png)

### Integrantes

Hoje o projeto tem **um integrante**, que é o dono da conta. Para incluir alguém:

1. **Configurações → Equipe → Convidar novo membro**, com o e-mail da pessoa.
2. A pessoa cria o próprio AWS Builder ID e registra MFA nele.
3. O membro tem acesso de administrador aos recursos do projeto, mas **não** vê nem altera faturamento, não convida nem remove pessoas e não faz upgrade.

Nenhuma credencial é compartilhada: cada pessoa entra com o próprio Builder ID.

## 3. Custo

- O plano gratuito **não gera cobrança**: o painel de faturamento mostra "Sem cobranças enquanto estiver no plano gratuito".
- Só o dono pode fazer upgrade; membros da equipe não podem.
- A arquitetura usa só serviços **sempre gratuitos** (ADR 0002). API Gateway, SES, Secrets Manager, chave KMS própria, trilha do CloudTrail, NAT Gateway e bucket S3 ficam de fora.
- O AWS Budgets não está disponível nesse cadastro; os créditos e os dias restantes aparecem no painel de faturamento.

![Faturamento do plano gratuito](img/seguranca/03-faturamento.png)

## 4. Auditoria

O **histórico de eventos do CloudTrail** guarda 90 dias de eventos de gerenciamento, sem custo e sem criar trilha.

- Eventos regionais (Lambda, DynamoDB, SQS, `AssumeRole`…) aparecem em `us-east-2`.
- Eventos do IAM, que é global, aparecem em `us-east-1` (Norte da Virgínia).

O print mostra a configuração da T30 em ordem: a tentativa de criar o provedor OIDC (negada), a criação dos usuários e papéis, as políticas, e a chave de teste criada e apagada. Até a tentativa com a política malformada ficou registrada.

![Eventos do IAM no CloudTrail](img/seguranca/15-cloudtrail-iam-us-east-1.png)

## 5. Credenciais do CI (GitHub Actions)

### Por que não OIDC

O plano original (ADR 0003) era o GitHub Actions assumir um papel via OIDC, sem nenhuma chave. Nesta conta, a política da organização, gerenciada pela AWS, **bloqueia a criação do provedor OIDC**:

![OIDC bloqueado pela SCP](img/seguranca/07-oidc-bloqueado.png)

### Desenho

Dois usuários técnicos, sem console, cada um só pode assumir **um** papel:

| Usuário IAM | Política do usuário | Papel | Permissões do papel | Chave guardada em |
|---|---|---|---|---|
| `ci-terraform-plan` | `sts:AssumeRole` só em `github-terraform-plan` | `github-terraform-plan` | `ReadOnlyAccess` | Secret do repositório (`plan` nos PRs) |
| `ci-deploy` | `sts:AssumeRole` só em `github-deploy` | `github-deploy` | `PowerUserAccess` + `papeis-feedback` | Secret do Environment `production` (só a `main`) |

Cada papel confia **só no seu usuário** (política de confiança com o ARN do usuário).

```mermaid
flowchart LR
    pr[PR que altera infra/**] -->|chave ci-terraform-plan| plan[[github-terraform-plan<br/>ReadOnlyAccess]]
    main[push na main<br/>Environment production] -->|chave ci-deploy| deploy[[github-deploy<br/>PowerUserAccess + papeis-feedback]]
    plan -->|credenciais de 1 h| tfplan[terraform plan]
    deploy -->|credenciais de 1 h| tfapply[terraform apply]
```

A política `papeis-feedback` deixa o papel de deploy criar, alterar, apagar e passar (`iam:PassRole`) **só papéis com nome `feedback-*`**: os das funções Lambda e o do agendador. Ele não consegue mexer em usuários, em políticas gerenciadas nem nos papéis do CI.

![Papéis do CI](img/seguranca/08-papeis-github.png)

![Usuários técnicos do CI](img/seguranca/16-usuarios-iam.png)

![Política do ci-deploy: só assumir o papel](img/seguranca/17-ci-deploy-politica.png)

### Controles compensatórios

Como existe chave de longa duração, ela é limitada assim:

- **A chave sozinha não faz nada**: a única permissão do usuário é assumir o próprio papel.
- **PR não faz deploy**: a chave usada nos PRs só assume o papel de leitura.
- **Chaves criadas só quando o pipeline existir** (T42), direto nos secrets do GitHub, nunca no código. Hoje os dois usuários estão **sem nenhuma chave**.
- **Rotação**: a cada 90 dias, ou na hora se houver suspeita. Criar a chave nova, atualizar o secret, rodar o workflow e apagar a antiga.
- **Rastreio**: todo `AssumeRole` fica no CloudTrail, com o usuário que assumiu.

![ci-deploy sem chave de acesso](img/seguranca/18-ci-deploy-sem-chave.png)

### Teste

Feito no CloudShell, com uma chave temporária do `ci-terraform-plan` apagada no fim:

| Tentativa | Resultado |
|---|---|
| `ci-terraform-plan` assume `github-terraform-plan` | ✅ `assumed-role/github-terraform-plan/teste` |
| `ci-terraform-plan` assume `github-deploy` | ✅ negado (`AccessDenied`) |

### Políticas

`<ACCOUNT_ID>` é o ID da conta do projeto.

Confiança do `github-deploy` (a do `github-terraform-plan` é igual, com `ci-terraform-plan`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<ACCOUNT_ID>:user/ci-deploy" },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

Política do usuário `ci-deploy` (`assumir-github-deploy`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::<ACCOUNT_ID>:role/github-deploy"
    }
  ]
}
```

Política `papeis-feedback` do papel `github-deploy`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SoPapeisDoProjeto",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:UpdateRole",
        "iam:UpdateAssumeRolePolicy", "iam:TagRole", "iam:UntagRole", "iam:ListRoleTags",
        "iam:PutRolePolicy", "iam:GetRolePolicy", "iam:DeleteRolePolicy", "iam:ListRolePolicies",
        "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:ListAttachedRolePolicies",
        "iam:ListInstanceProfilesForRole", "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::<ACCOUNT_ID>:role/feedback-*"
    }
  ]
}
```

Esses usuários, papéis e políticas foram criados à mão no CloudShell, porque precisam existir antes de o Terraform rodar no CI. Todo o resto da infraestrutura é criado pelo Terraform.

## 6. Papéis das funções Lambda

Cada função tem o próprio papel IAM, criado pelo Terraform (prefixo `feedback-`, o único que o papel de deploy consegue criar). Em vez da política gerenciada `AWSLambdaBasicExecutionRole`, que libera qualquer grupo de logs, cada papel só grava no grupo da própria função (`logs:CreateLogStream` e `logs:PutLogEvents` em `/aws/lambda/<função>`). Além disso, só o mínimo:

| Função (papel) | Pode | Não pode |
|---|---|---|
| `feedback-ingestion` (`feedback-ingestion`) | `dynamodb:PutItem` na tabela `feedbacks`; `sqs:SendMessage` na fila `critical-feedbacks` | Ler o banco, consumir a fila, enviar e-mail |
| `critical-notifier` (`feedback-critical-notifier`) | `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes` e `sqs:ChangeMessageVisibility` na fila; `sns:Publish` no tópico `feedbacks-criticos`; `kms:GenerateDataKey*` e `kms:Decrypt` só pelo SNS | Acessar o banco, publicar na fila, enviar o relatório |
| `weekly-report` (`feedback-weekly-report`) | `dynamodb:Query` só no índice `por-dia`; `sns:Publish` no tópico `relatorio-semanal`; `kms:GenerateDataKey*` e `kms:Decrypt` só pelo SNS | Gravar no banco, ler a tabela fora do índice, acessar a fila |
| Agenda `feedback-weekly-report` (`feedback-scheduler`) | `lambda:InvokeFunction` no alias `live` da `weekly-report` | Qualquer outra coisa |

- A permissão de KMS tem a condição `kms:ViaService = sns.us-east-2.amazonaws.com`: a função só usa a chave `aws/sns` por meio do SNS, ao publicar no tópico criptografado.
- O papel da agenda só pode ser assumido pelo EventBridge Scheduler desta conta (condição `aws:SourceAccount`).
- Os módulos `function` e `schedule` recusam nomes de papel sem o prefixo `feedback-` (`validation` no Terraform).

## 7. Proteção dos dados e da API

| Item | Configuração |
|---|---|
| Criptografia em repouso | Tabela com a chave da AWS (padrão do DynamoDB); filas com SSE-SQS; tópicos SNS com a chave gerenciada `aws/sns`. Nenhuma chave KMS própria (US$ 1/mês cada) |
| Criptografia em trânsito | Function URL só aceita HTTPS. As políticas da fila e da DLQ negam qualquer acesso sem HTTPS (`aws:SecureTransport = false`). O SDK da AWS nas funções usa HTTPS |
| Quem acessa a fila | Além do IAM de cada papel, a política da própria fila só libera `SendMessage` para o papel da ingestion e o consumo para o papel do notifier |
| Segredos | Nenhum na aplicação: as funções usam as credenciais temporárias do papel IAM. A configuração (tabela, URL da fila, ARNs dos tópicos) fica em variáveis de ambiente, preenchidas pelo Terraform. Se surgir um segredo, vai para o SSM Parameter Store padrão (`SecureString`, gratuito), não para o Secrets Manager (cobrado) |
| E-mails dos administradores | Secret `ADMIN_EMAILS` no GitHub, variável sensível no Terraform (o plan mostra só os apelidos) |
| API pública | Function URL sem autenticação (o enunciado pede um `POST /avaliacao` aberto aos estudantes). A validação de entrada fica no código (Bean Validation e regras do domain) |
| Limite de requisições | *Reserved concurrency* na ingestion (variável `ingestion_reserved_concurrency`): limita as execuções simultâneas e, com isso, o custo de um abuso. A AWS exige que sobrem pelo menos 10 execuções sem reserva na conta, então o valor depende do limite da conta (`aws lambda get-account-settings`). No notifier, o *event source mapping* limita a 2 execuções simultâneas |
| Logs | Retenção de 7 dias. Na AWS, os logs registram só id, urgência, datas e totais: a descrição da avaliação (texto do estudante) não vai para os logs, só para o banco e os e-mails |
| Plan público | O resumo da execução do Terraform (público) mostra só os recursos; o plan completo fica no log, com o ID da conta mascarado |

## 8. Próximos passos

- **T41**: teste ponta a ponta na AWS e revisão das políticas com o IAM Access Analyzer (`aws accessanalyzer validate-policy`, gratuito) no CloudShell.
- **T43 e T44**: monitoramento (dashboard) e alarmes para o tópico `alertas-administradores` (sem a chave `aws/sns`, que o CloudWatch não consegue usar).
- **T48**: versão final deste documento.
