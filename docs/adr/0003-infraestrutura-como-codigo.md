# 0003 - Infraestrutura como código: Terraform

- **Status:** Aceita
- **Data:** 2026-11-09

## Contexto

O ADR 0002 escolheu a AWS e definiu os serviços de cada componente (Lambda com Function URL, DynamoDB, SQS, SNS, EventBridge Scheduler, CloudWatch e IAM). O Tech Challenge avalia o **deploy automatizado** e a **governança de acesso**, então a infraestrutura precisa ser descrita em código, revisada em PR e aplicada pelo CI.

Restrições:

- **Custo zero**: o state e os artefatos não podem depender de serviço pago. Bucket S3 fica de fora, porque só é grátis nos 12 primeiros meses.
- **Sem chave de acesso da AWS no repositório**: o CI usa o papel IAM via OIDC (T30).
- O projeto fica no ar até a nota e depois é destruído pela própria IaC.

## Opções consideradas

| Critério | Terraform | AWS SAM | AWS CDK |
|---|---|---|---|
| Linguagem | HCL, declarativa | YAML do CloudFormation | Java ou TypeScript que gera CloudFormation |
| Artefato da Lambda | Envia o `function.zip` direto para a Lambda (até 50 MB) | `sam deploy` exige um bucket S3 para os artefatos | O *bootstrap* cria um bucket S3 e um repositório ECR |
| State | Remoto no HCP Terraform (plano gratuito) | No CloudFormation, sem custo | No CloudFormation, sem custo |
| Plano antes de aplicar | `terraform plan` legível no PR | *Change set* do CloudFormation | `cdk diff` |
| Portabilidade e mercado | Independente de provedor, o mais usado no mercado | Só AWS | Só AWS |

1. **Terraform** — prós: o mais conhecido no mercado e independente de provedor; envia o `function.zip` direto para a Lambda, sem bucket; `plan` mostra no PR exatamente o que vai mudar; `destroy` remove tudo ao fim da avaliação. Contras: o state fica fora da AWS e precisa de backend remoto; licença BSL desde a versão 1.6 (sem impacto para uso acadêmico).
2. **AWS SAM** — prós: nativo da AWS, sintaxe curta para Lambda e eventos; state no CloudFormation. Contras: o deploy depende de bucket S3 (custo depois de 12 meses); só AWS.
3. **AWS CDK** — prós: infraestrutura em Java, a mesma linguagem das funções. Contras: o *bootstrap* cria bucket S3 e repositório ECR; gera CloudFormation, então é mais uma camada para depurar; só AWS.

## Decisão

**Terraform**, com o state remoto no **HCP Terraform** (plano gratuito).

- **State**: um workspace no HCP Terraform (bloco `cloud` no `envs/dev`) guarda o state e faz o *lock*. O workspace usa **execution mode Local**: o HCP só guarda o state, e o `plan`/`apply` roda no GitHub Actions, com as credenciais da AWS obtidas via OIDC. Nenhuma chave da AWS fica no HCP nem no GitHub; o GitHub Actions só guarda o token do HCP Terraform (secret `TF_TOKEN_app_terraform_io`).
- **Artefatos**: o `mvn package -Plambda` gera o `function.zip` de cada função, e o Terraform envia o arquivo direto para a Lambda (`filename` + `source_code_hash`), publicando uma versão e movendo o alias `live` (SnapStart só vale para versões publicadas).
- **Validação e aplicação no GitHub Actions**:

| Evento | O que roda | Credenciais |
|---|---|---|
| PR que altera `infra/**` | `terraform fmt -check`, `validate` e `plan` | Papel IAM só de leitura via OIDC |
| `push` na `main` | `terraform apply` (GitHub Environment `production` com aprovação manual) | Papel IAM de deploy via OIDC, aceito só na `main` |
| Fim da avaliação | `terraform destroy`, manual | Papel IAM de deploy |

- **Estrutura de `infra/`**:

```
infra/
├── modules/
│   ├── function/        # Lambda, papel IAM, log group (7 dias), alias live e SnapStart
│   ├── table/           # DynamoDB provisionado e índice por-dia
│   ├── queue/           # SQS e DLQ
│   ├── notification/    # tópicos SNS e assinaturas de e-mail
│   ├── schedule/        # EventBridge Scheduler
│   └── monitoring/      # dashboard e alarmes do CloudWatch
└── envs/
    └── dev/
        ├── versions.tf               # versões do Terraform e do provider, bloco cloud
        ├── main.tf                   # chama os módulos
        ├── variables.tf
        ├── outputs.tf                # Function URL, nomes da fila e dos tópicos
        └── terraform.tfvars.example  # e-mails dos administradores etc. (o .tfvars real não é versionado)
```

Um ambiente só (`dev`), porque a conta é única e gratuita; a pasta `envs/` permite criar outros sem mudar os módulos.

## Consequências

- **Toda mudança de infraestrutura passa por PR**: o `plan` aparece no PR e é revisado antes do merge; nada é criado pelo console.
- **Sem bucket S3 e sem chave da AWS**: o state fica no HCP Terraform e as credenciais vêm do OIDC.
- **Depende de um serviço externo**: o HCP Terraform precisa de conta e organização próprias, e o plano gratuito tem limite de recursos gerenciados (a infraestrutura do projeto fica bem abaixo dele). Se o plano mudar, o state pode ser migrado para outro backend com `terraform init -migrate-state`.
- **Mais papéis IAM na T30**: além do papel de deploy, um papel só de leitura para o `plan` nos PRs.
- **Ordem do build**: o `function.zip` precisa existir antes do `plan`/`apply`, então o workflow empacota as funções antes de rodar o Terraform.
- **Destruição controlada**: depois da nota, `terraform destroy` remove todos os recursos criados pela IaC.
