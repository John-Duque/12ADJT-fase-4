# 0004 - Conta AWS no cadastro simplificado

- **Status:** Aceita
- **Data:** 2026-11-09

Emenda o [ADR 0002](0002-provedor-de-nuvem.md) (região) e o [ADR 0003](0003-infraestrutura-como-codigo.md) (credenciais do CI).

## Contexto

A conta AWS foi criada na T30 pelo cadastro simplificado da AWS (*Sign up for AWS (new)*), no plano gratuito. Esse cadastro cria um **projeto** dentro de uma organização gerenciada pela própria AWS, e não uma conta clássica:

- O dono entra com um **AWS Builder ID**; não existe usuário raiz.
- A **região é fixa**, definida pelo país do endereço: `us-east-2` (Ohio) para o Brasil. Não há seletor de região.
- As políticas da organização (SCP e RCP) são **gerenciadas pela AWS e não podem ser editadas**.
- Usuários IAM servem só para acesso programático; pessoas entram pela página **Equipe**.
- O faturamento é simplificado: sem cobrança enquanto o plano for gratuito, e sem AWS Budgets.

Ao preparar o deploy do ADR 0003, o teste no CloudShell mostrou que a SCP da organização **bloqueia o OIDC**:

```
AccessDenied ... iam:CreateOpenIDConnectProvider ... with an explicit deny in a service control policy
```

Sem provedor OIDC, o GitHub Actions não consegue assumir um papel IAM sem chave. Os demais serviços do ADR 0002 (Lambda, DynamoDB, SQS, SNS, EventBridge, CloudWatch, CloudTrail, IAM e CloudShell) estão disponíveis.

Restrições:

- **Custo zero** (ADR 0002).
- **Prazo**: entrega em 17/11/2026 e avaliação em 3 a 4 meses (até março de 2027). O plano gratuito desta conta vai até **09/05/2027**.
- O deploy continua automatizado pelo GitHub Actions, com privilégio mínimo e separação entre `plan` (PRs) e `apply` (`main`).

## Opções consideradas

### Conta

1. **Manter a conta simplificada** — prós: já criada e configurada; continua no plano gratuito, que não gera cobrança; o prazo do plano cobre a entrega e a avaliação. Contras: região `us-east-2` no lugar de `us-east-1`; sem OIDC; sem Budgets.
2. **Fazer upgrade para o plano pago e ativar os recursos avançados** — prós: libera OIDC, Budgets e a escolha de região. Contras: **irreversível**; perde a garantia de "sem cobranças" do plano gratuito; exige configurar a organização inteira.
3. **Criar outra conta pelo cadastro clássico** (*Sign up for AWS (advanced)*) — prós: tudo como nos ADRs 0002 e 0003. Contras: novo cadastro e verificação; a conta atual fica parada até ser fechada.

### Credenciais do CI, mantendo a conta

1. **Um usuário IAM com as permissões direto nele** — contras: uma chave vazada já dá acesso a tudo que o deploy faz.
2. **Um usuário IAM que assume os dois papéis** — contras: a chave do `plan`, usada nos PRs, também conseguiria fazer deploy.
3. **Dois usuários IAM, cada um só com `sts:AssumeRole` no seu papel** — prós: mantém a separação que o OIDC daria (PR só lê, `main` faz deploy); a chave sozinha não lê nem cria nada; cada execução usa credenciais temporárias. Contras: existe chave de longa duração, que precisa de rotação.

## Decisão

**Manter a conta simplificada** (opção 1), na região **`us-east-2`**, com **dois usuários técnicos que só assumem papel** (opção 3 das credenciais):

| Usuário IAM (sem console) | Pode assumir | Permissões do papel | Onde fica a chave |
|---|---|---|---|
| `ci-terraform-plan` | `github-terraform-plan` | `ReadOnlyAccess` | Secret do repositório, usado no `plan` dos PRs |
| `ci-deploy` | `github-deploy` | `PowerUserAccess` + política que só gerencia papéis `feedback-*` | Secret do Environment `production`, liberado só para a `main` |

O que muda em relação aos ADRs anteriores:

| Item | Antes | Agora |
|---|---|---|
| Região (ADR 0002) | `us-east-1` | `us-east-2`, fixa no projeto |
| Credenciais do CI (ADR 0003) | OIDC, sem chave | Chave de usuário técnico + `sts:AssumeRole`, credenciais temporárias de 1 h |
| Acesso humano | Usuário raiz + usuário IAM com MFA | AWS Builder ID com MFA; novos integrantes pela página **Equipe** |
| Aviso de gasto | *Zero spend budget* | Plano gratuito, que não gera cobrança; só o dono pode fazer upgrade |

O resto dos ADRs 0002 e 0003 continua valendo: serviços sempre gratuitos, Terraform com state no HCP Terraform, `plan` nos PRs e `apply` na `main`.

## Consequências

- **Existem duas chaves de longa duração.** Mitigação: cada uma só assume um papel; as chaves só são criadas quando o pipeline existir; ficam em secrets do GitHub, nunca no código; rotação a cada 90 dias ou na hora, se houver suspeita; todo `AssumeRole` fica no CloudTrail. Detalhes em [`docs/seguranca.md`](../seguranca.md).
- **O deploy (T42) usa `aws-actions/configure-aws-credentials` com chave e `role-to-assume`**, e não `id-token: write`.
- **Todo papel criado pelo Terraform precisa do prefixo `feedback-`**, porque o papel de deploy só pode criar e passar papéis com esse nome.
- **Região `us-east-2`**: README e documentação passam a citar Ohio. Os eventos do IAM, que é global, aparecem no CloudTrail de `us-east-1`.
- **Sem Budgets**: a garantia de custo zero é o próprio plano gratuito; o painel de faturamento mostra créditos e dias restantes.
- **Prazo**: o plano gratuito vai até 09/05/2027, depois da avaliação. Se a avaliação passar dessa data, fazer upgrade antes (o sempre gratuito continua sem custo).
- **Se o OIDC for indispensável no futuro**, o caminho é o upgrade e a ativação dos recursos avançados, sabendo que são irreversíveis.
