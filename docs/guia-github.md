# Guia passo a passo: GitHub do 12ADJT-fase-4

Este guia cobre a base do projeto no GitHub: repositório, proteções, Sonar, GitHub Project, tasks e a rotina do grupo. O código não vem pronto: cada parte é construída por uma issue do Project, em ordem.

Cada etapa está marcada assim:

- **[configurado]** — já vem pronto na criação do repositório. Confira; se precisar refazer, siga os passos.
- **[manual]** — precisa ser feito no navegador (a API do GitHub não permite automatizar).

> Os nomes de menus seguem a interface do GitHub em inglês. Se a sua estiver em outro idioma, procure o item equivalente.

## Sumário

0. [Pré-requisitos](#0-pré-requisitos)
1. [Clonar o repositório](#1-clonar-o-repositório)
2. [Como a base foi criada](#2-como-a-base-foi-criada)
3. [Configurações do repositório](#3-configurações-do-repositório)
4. [Proteção das branches (rulesets)](#4-proteção-das-branches-rulesets)
5. [SonarQube Cloud](#5-sonarqube-cloud)
6. [GitHub Project](#6-github-project)
7. [Labels, milestones e tasks](#7-labels-milestones-e-tasks)
8. [Rotina do grupo](#8-rotina-do-grupo)
9. [Docker e GitHub Container Registry](#9-docker-e-github-container-registry)
10. [Checklist da base](#10-checklist-da-base)
11. [Problemas comuns](#11-problemas-comuns)

---

## 0. Pré-requisitos

**[manual]** Para cada integrante:

1. Conta no GitHub com **autenticação em dois fatores** (*Settings → Password and authentication*).
2. Ferramentas (macOS com Homebrew):

   ```bash
   brew install git gh maven
   brew install --cask temurin@21 docker
   java -version      # 21
   mvn -v             # 3.9+
   ```

3. Git identificado com o mesmo e-mail da conta do GitHub:

   ```bash
   git config --global user.name "Seu Nome"
   git config --global user.email "seu-email@exemplo.com"
   git config --global init.defaultBranch main
   ```

4. GitHub CLI com o escopo de Projects:

   ```bash
   gh auth login              # GitHub.com → HTTPS → Login with a web browser
   gh auth refresh -s project
   gh auth status             # 'project' deve aparecer em Token scopes
   ```

## 1. Clonar o repositório

**[manual]** Para cada integrante, depois de aceitar o convite (seção 3.3):

```bash
git clone https://github.com/<dono>/12ADJT-fase-4.git
cd 12ADJT-fase-4
git switch develop
mvn verify          # deve passar desde o primeiro dia
```

Arquivos pessoais (anotações, configurações de ferramentas) que não devem ir para o repositório vão em `.git/info/exclude`, que vale só na sua máquina. O `.gitignore` é para regras do grupo todo.

## 2. Como a base foi criada

### 2.1 O que vem na base

| Arquivo | Para quê |
|---|---|
| `pom.xml` | POM pai: Java 21, BOM do Quarkus, JaCoCo e Sonar. Sem módulos — cada um entra pela sua task |
| `.github/workflows/ci.yml` | Build, testes e análise do Sonar em push e PR |
| `.github/workflows/pr-conventions.yml` | Valida nome da branch e título do PR |
| `.github/` (templates, Dependabot) | Template de PR, templates de issue e atualizações automáticas |
| `README.md`, `CONTRIBUTING.md` | Apresentação e regras de contribuição |
| `docs/guia-github.md`, `docs/adr/` | Este guia e o modelo de ADR |

### 2.2 O que já vem configurado

| Configuração | Seção |
|---|---|
| Repositório público com `main` e `develop` | 2.3 |
| Opções de merge e exclusão automática de branches | 3.1 |
| Rulesets `proteger-main` e `proteger-develop` | 4 |
| 11 labels e 10 milestones | 7.1 e 7.2 |
| Project com colunas Backlog/Todo/In Progress/In Review/Done | 6.3 |
| Campos Prioridade, Estimativa, Área, Depende de e Data limite | 6.4 |
| View **Kanban** em layout **Board** (ou instrução para trocar o layout em 30 segundos) | 6.5 |
| 51 issues numeradas (T01–T51), com dependências e Status, Prioridade, Estimativa e Área preenchidos | 7.3 |

Ficam para o grupo as tasks T01 (Sonar), T02 (views e automações do Project) e T03 (integrantes).

### 2.3 Criar do zero na mão

1. **github.com/new** → *Repository name* `12ADJT-fase-4` → **Public** → sem README, `.gitignore` ou licença → **Create repository**.
2. No terminal:

   ```bash
   git init -b main
   # arquivos pessoais (anotações, configurações de ferramentas) que não devem ir para o repositório:
   echo "<arquivo-pessoal>" >> .git/info/exclude
   git add -A
   git status                     # confira os arquivos antes do commit
   git commit -m "chore: initial project setup"
   git remote add origin https://github.com/<seu-usuario>/12ADJT-fase-4.git
   git push -u origin main
   git switch -c develop
   git push -u origin develop
   ```

3. Seções 3 a 7 deste guia.

## 3. Configurações do repositório

### 3.1 Merge de Pull Requests **[configurado]**

*Settings → General → Pull Requests*:

| Opção | Valor | Por quê |
|---|---|---|
| Allow merge commits | ✅ — *Default message: Pull request title* | Promoção `develop → main` |
| Allow squash merging | ✅ — *Default message: Pull request title* | `task → develop`: um commit por task |
| Allow rebase merging | ❌ | Uma forma só de histórico por branch |
| Always suggest updating pull request branches | ✅ | Botão *Update branch* quando a `develop` andou |
| Automatically delete head branches | ✅ | Apaga a branch da task após o merge |

### 3.2 Branch padrão **[manual]**

Mantenha a **`main`** como padrão (é o que os avaliadores veem). Por isso, **ao abrir um PR, troque o `base` para `develop`**.

### 3.3 Integrantes **[manual]** — task T03

1. *Settings → Collaborators → Add people* → papel **Write**.
2. Cada um aceita o convite e clona:

   ```bash
   git clone https://github.com/<dono>/12ADJT-fase-4.git
   cd 12ADJT-fase-4 && git switch develop
   ```

### 3.4 Actions **[manual]**

*Settings → Actions → General*: *Allow all actions and reusable workflows*; *Workflow permissions*: **Read repository contents and packages permissions**.

### 3.5 Página inicial **[manual]**

Engrenagem ao lado de **About** → *Topics*: `java`, `quarkus`, `serverless`, `clean-architecture`, `fiap`, `pos-tech`.

## 4. Proteção das branches (rulesets)

**[configurado]** — na mão: *Settings → Rules → Rulesets → New ruleset → New branch ruleset*.

> Os *status checks* só aparecem na busca depois que os workflows rodaram uma vez. Se não aparecerem, digite o nome exato.

### 4.1 `proteger-main`

| Campo | Valor |
|---|---|
| Ruleset Name | `proteger-main` |
| Enforcement status | **Active** |
| Bypass list | vazia |
| Target branches | *Include by pattern* → `main` |
| Restrict deletions | ✅ |
| Require a pull request before merging | ✅ — **1** aprovação, ✅ *Dismiss stale approvals*, ✅ *Require conversation resolution*, *Allowed merge methods:* **Merge** |
| Require status checks to pass | ✅ — `Build, testes e Sonar` |
| Block force pushes | ✅ |

### 4.2 `proteger-develop`

Igual, com *Target* `develop`, *Allowed merge methods* **Squash** e checks `Build, testes e Sonar` + `Nome da branch e título do PR`.

Depois da milestone 6 (Docker), adicione também `Imagem feedback-ingestion`, `Imagem critical-notifier` e `Imagem weekly-report`.

### 4.3 Testar

```bash
git switch main
git commit --allow-empty -m "test: push direto"
git push            # deve ser recusado
git reset --hard origin/main
```

## 5. SonarQube Cloud

**[manual]** — task T01 do Project.

### 5.1 Importar

1. **sonarcloud.io** → entrar com o GitHub → **+ → Analyze new project**.
2. Organização **john-duque**. Se o repositório não aparecer: *GitHub app configuration* → *Repository access* → adicionar `12ADJT-fase-4`.
3. Marcar o repositório → **Set Up** → *New code definition*: **Previous version**.

### 5.2 Desligar a análise automática

*Administration → Analysis Method* → desligar **Automatic Analysis**. Sem isso o CI falha.

### 5.3 Token

Na mesma tela, em **With GitHub Actions**, aparece o `SONAR_TOKEN` (ou *My Account → Security → Generate Tokens*). Copie: ele aparece uma vez só.

### 5.4 Secret no GitHub

*Settings → Secrets and variables → Actions → New repository secret* → `SONAR_TOKEN`. Ou: `gh secret set SONAR_TOKEN -R <dono>/12ADJT-fase-4`.

### 5.5 Project key

No Sonar: *Information → Project Key*. Precisa ser igual a `<sonar.projectKey>` no `pom.xml` (`John-Duque_12ADJT-fase-4`).

### 5.6 Validar

1. *Actions → CI → Run workflow* → `develop`.
2. No log, o passo **Análise SonarQube Cloud** precisa ter executado (sem o aviso de Sonar não configurado).
3. O projeto aparece analisado no Sonar. Enquanto não houver código, ele mostra 0 linhas; a cobertura aparece a partir da milestone 2.
4. *Information → Get project badges* → badge **Quality Gate Status** no README (no PR da task T01).

O quality gate **Sonar way** exige no código novo: cobertura ≥ 80%, nenhum bug ou vulnerabilidade novos e duplicação ≤ 3%.

## 6. GitHub Project

### 6.1 Criação e vínculo **[configurado]**

Na mão: perfil → **Projects → New project** → template **Board** → nome `12ADJT-fase-4`. Depois, no repositório: **Projects → Link a project**.

### 6.2 Acesso **[manual]** — task T03

Project → **⋯ → Settings**: *Short description*, *README* (objetivo e link do repositório), *Visibility* **Public** (opcional), *Manage access* → integrantes com **Write**.

### 6.3 Colunas (campo Status) **[configurado]**

| Ordem | Opção | Quando o card fica aqui |
|---|---|---|
| 1 | **Backlog** | Tasks futuras (milestones 3 em diante começam aqui) |
| 2 | **Todo** | Prontas para pegar (milestones 1 e 2 começam aqui) |
| 3 | **In Progress** | Alguém trabalhando, com responsável |
| 4 | **In Review** | PR aberto aguardando revisão |
| 5 | **Done** | Mergeado na `develop` |

Se as colunas Backlog e In Review não existirem: *Settings → Fields → Status* → adicione **Backlog** (primeira) e **In Review** (antes de Done). Não renomeie `Todo`, `In Progress` e `Done`.

### 6.4 Campos **[configurado]** + Sprint **[manual]**

| Campo | Tipo | Valores | Situação |
|---|---|---|---|
| Prioridade | Single select | Alta, Média, Baixa | configurado |
| Estimativa | Number | pontos 1, 2, 3, 5 | configurado |
| Área | Single select | Domain, Ingestion, Notifier, Report, Infra, CI/CD, Docs | configurado |
| Depende de | Text | Tasks que precisam estar em Done antes (ex.: `T08, T09`) | configurado |
| Data limite | Date | — | configurado |
| Sprint | Iteration | 1 semana, começando na próxima segunda | **manual** (*Settings → Fields → + New field*) |

### 6.5 Views — Board como visão principal

A visão principal do kanban é um **Board**: uma coluna por Status, com os cards arrastáveis.

**Kanban (Board)** — criada na configuração inicial quando a API do GitHub permite. Se o Project abrir em tabela (*View 1*):

1. Na aba **View 1**, clique no ▾ ao lado do nome.
2. Em **Layout**, escolha **Board**.
3. Clique no nome da aba e renomeie para `Kanban`.
4. Clique em **Save**.
5. Arraste a aba **Kanban** para ser a primeira: é a que abre por padrão.

Ajustes do Board (▾ da aba → configurações):

| Ajuste | Valor |
|---|---|
| Column by | Status (Backlog · Todo · In Progress · In Review · Done) |
| Fields no card | Assignees, Labels, Milestone, Prioridade, Estimativa, Depende de |
| Sort | Título (os cards ficam em ordem T01, T02, …) |
| Limite da coluna In Progress | 4 (⋯ da coluna → *Set limit*) |

**Outras views [manual] — task T02** (+ New view → configurar → **Save**):

| View | Layout | Configuração |
|---|---|---|
| **Backlog** | Table | *Filter* `status:Backlog,Todo`; *Group by* Milestone; *Sort* Título |
| **Por pessoa** | Table | *Filter* `-status:Done`; colunas Assignees, Status, Milestone, Prioridade, Estimativa e Sprint |
| **Sprint atual** | Board | *Filter* `sprint:@current`; *Column by* Status |
| **Roadmap** | Roadmap | *Date fields* Sprint (ou Data limite); *Zoom* Month |

Agrupar e ordenar: botão **View** ao lado da barra de filtro → *Group by* / *Sort*. Se a opção não aparecer, em uma Table clique no título da coluna → *Group by values*.

Filtros úteis: `assignee:@me`, `label:dominio`, `milestone:"2. Domínio"`, `area:Domain`, `no:assignee`.

### 6.6 Automações **[manual]** — task T02

Project → **⋯ → Workflows**:

| Workflow | Configuração |
|---|---|
| Item added to project | Status → **Backlog** |
| Item reopened | Status → **Todo** |
| Item closed | Status → **Done** |
| Pull request merged | Status → **Done** |
| Code changes requested | Status → **In Progress** |
| Auto-add to project | repositório `12ADJT-fase-4`, filtro `is:issue,pr is:open` |
| Auto-archive items | `is:issue,pr is:closed updated:<@today-14d` |

Mover para **In Progress** e **In Review** é manual (arrastar o card).

### 6.7 Validar o fluxo **[manual]** — task T02

Use a própria task T02: mover para *In Progress* → criar a branch pela issue → commit do `.gitattributes` descrito nela → PR para `develop` → *In Review* → aprovação → *Squash and merge* → fechar a issue → o card vai para *Done* sozinho (automação *Item closed*).

## 7. Labels, milestones e tasks

### 7.1 Labels **[configurado]**

| Label | Cor | Descrição |
|---|---|---|
| `dominio` | `#006B75` | Camada domain (entidades e regras) |
| `funcao` | `#1D76DB` | Código das funções serverless |
| `docker` | `#0DB7ED` | Imagens, compose e containers |
| `infra` | `#5319E7` | Nuvem e infraestrutura como código |
| `ci-cd` | `#0E8A16` | Pipelines de build, qualidade e deploy |
| `seguranca` | `#B60205` | Segurança dos dados e governança de acesso |
| `observabilidade` | `#FBCA04` | Logs, métricas, alarmes e dashboards |
| `documentacao` | `#0075CA` | Documentação exigida na entrega |
| `decisao` | `#C5DEF5` | Decisão técnica registrada em ADR |
| `entrega` | `#D93F0B` | Artefatos finais do Tech Challenge |
| `organizacao` | `#BFD4F2` | Organização do grupo e do repositório |

### 7.2 Milestones **[configurado]** + prazos **[manual]**

Edite cada milestone (*Issues → Milestones → Edit*) e preencha o **Due date** dentro do prazo do Tech Challenge.

| Milestone | Conteúdo |
|---|---|
| 1. Fundação | Sonar, Project, grupo e ADRs |
| 2. Domínio | Módulo `feedback-domain` |
| 3. Função feedback-ingestion | `POST /avaliacao` |
| 4. Função critical-notifier | E-mail de urgência |
| 5. Função weekly-report | Relatório semanal |
| 6. Docker | Dockerfile multi-stage, compose, imagens no CI |
| 7. Infraestrutura cloud | Conta, IaC, banco, fila, e-mail, segurança |
| 8. Integração com a nuvem | Extensões do provedor e gateways reais |
| 9. Deploy e observabilidade | Deploy automatizado, monitoramento, alarmes |
| 10. Documentação e entrega | Documentos, vídeo, checklist |

### 7.3 Tasks **[configurado]**

As 51 tasks estão no Project como issues **numeradas de T01 a T51**, na ordem de execução. Toda issue segue o mesmo formato:

| Seção | Conteúdo |
|---|---|
| Cabeçalho | Número da task, milestone, área, prioridade e estimativa |
| **Objetivo** | O que a task entrega, em uma frase |
| **Dependências** | Tasks que precisam estar em **Done** antes, com link para cada issue |
| **Passo a passo** | A branch a criar e, para cada commit: arquivo(s), o que criar (campos com tipo, validação e mensagem de erro; métodos com assinatura e regra; testes com cenário e resultado esperado) e um checkbox |
| Último passo | `mvn verify`, push, PR com título e `Closes #` já preenchidos, revisão e merge |
| **Critérios de aceite** | Checklist do que precisa estar verdadeiro no fim |

As dependências aparecem em três lugares: na seção **Dependências** da issue, no campo **Depende de** do Project e, quando a API do GitHub permite, no painel **Relationships → Blocked by** da issue.

Só pegue uma task quando todas as dependências dela estiverem em **Done**. Tasks sem dependência (T01 a T05) podem ser feitas em paralelo.

| Task | Título | Milestone | Depende de | Área | Prioridade | Pontos | Commits | Coluna inicial |
|---|---|---|---|---|---|---|---|---|
| T01 | Configurar o SonarQube Cloud e validar a primeira análise | 1 | — | CI/CD | Alta | 2 | 1 | Todo |
| T02 | Configurar o GitHub Project e validar o fluxo de trabalho | 1 | — | Docs | Alta | 2 | 1 | Todo |
| T03 | Adicionar o grupo ao repositório e ao Project | 1 | — | Docs | Alta | 1 | 1 | Todo |
| T04 | ADR 0001: monorepo Maven, Quarkus e Clean Architecture | 1 | — | Docs | Alta | 1 | 1 | Todo |
| T05 | ADR 0002: escolher o provedor de nuvem | 1 | — | Infra | Alta | 3 | 1 | Todo |
| T06 | ADR 0003: escolher a ferramenta de IaC | 1 | T05 | Infra | Alta | 2 | 1 | Todo |
| T07 | Criar o módulo Maven feedback-domain | 2 | T01, T02 | Domain | Alta | 1 | 1 | Todo |
| T08 | Domain: exceções DomainException e InvalidFeedbackException | 2 | T07 | Domain | Alta | 1 | 3 | Todo |
| T09 | Domain: enum Urgency com classificação por nota | 2 | T08 | Domain | Alta | 2 | 2 | Todo |
| T10 | Domain: entidade Feedback com validações | 2 | T09 | Domain | Alta | 3 | 2 | Todo |
| T11 | Domain: evento CriticalFeedbackEvent | 2 | T10 | Domain | Média | 2 | 2 | Todo |
| T12 | Criar o módulo Quarkus feedback-ingestion | 3 | T07 | Ingestion | Alta | 2 | 2 | Backlog |
| T13 | Application: gateways da ingestão | 3 | T11, T12 | Ingestion | Alta | 1 | 2 | Backlog |
| T14 | Application: ReceiveFeedbackUseCase | 3 | T13 | Ingestion | Alta | 3 | 3 | Backlog |
| T15 | Infra: gateways provisórios e UseCaseConfig da ingestão | 3 | T14 | Ingestion | Alta | 2 | 4 | Backlog |
| T16 | Infra: DTOs e FeedbackController POST /avaliacao | 3 | T15 | Ingestion | Alta | 3 | 4 | Backlog |
| T17 | Infra: GlobalExceptionHandler da ingestão | 3 | T16 | Ingestion | Média | 2 | 3 | Backlog |
| T18 | Criar o módulo Quarkus critical-notifier | 4 | T07 | Notifier | Alta | 2 | 2 | Backlog |
| T19 | Application: NotifyCriticalFeedbackUseCase | 4 | T11, T18 | Notifier | Alta | 3 | 4 | Backlog |
| T20 | Infra: gateway provisório de notificação e UseCaseConfig | 4 | T19 | Notifier | Alta | 1 | 3 | Backlog |
| T21 | Infra: CriticalFeedbackConsumer (Funqy) | 4 | T20 | Notifier | Alta | 2 | 3 | Backlog |
| T22 | Criar o módulo Quarkus weekly-report | 5 | T07 | Report | Alta | 2 | 2 | Backlog |
| T23 | Domain: WeeklyReport com agregações | 5 | T10, T22 | Report | Alta | 3 | 2 | Backlog |
| T24 | Application: GenerateWeeklyReportUseCase | 5 | T23 | Report | Alta | 3 | 4 | Backlog |
| T25 | Infra: gateways provisórios e UseCaseConfig do relatório | 5 | T24 | Report | Alta | 1 | 4 | Backlog |
| T26 | Infra: WeeklyReportJob (Funqy) | 5 | T25 | Report | Alta | 2 | 2 | Backlog |
| T27 | Dockerfile multi-stage para as funções | 6 | T17, T21, T26 | CI/CD | Alta | 3 | 2 | Backlog |
| T28 | compose.yaml e Makefile | 6 | T27 | CI/CD | Média | 2 | 3 | Backlog |
| T29 | Workflow Docker: build, smoke test e publicação no GHCR | 6 | T27 | CI/CD | Média | 3 | 3 | Backlog |
| T30 | Configurar conta e governança de acesso na nuvem | 7 | T05 | Infra | Alta | 3 | 1 | Backlog |
| T31 | Estruturar a IaC base | 7 | T06, T30 | Infra | Alta | 3 | 4 | Backlog |
| T32 | Provisionar o banco de feedbacks | 7 | T31 | Infra | Alta | 2 | 1 | Backlog |
| T33 | Provisionar a fila de feedbacks críticos com DLQ | 7 | T31 | Infra | Alta | 2 | 1 | Backlog |
| T34 | Configurar o serviço de e-mail dos administradores | 7 | T31 | Infra | Alta | 2 | 1 | Backlog |
| T35 | Proteger os dados da aplicação | 7 | T32, T33 | Infra | Média | 2 | 1 | Backlog |
| T36 | feedback-ingestion: extensão Quarkus do provedor | 8 | T05, T17 | Ingestion | Alta | 2 | 1 | Backlog |
| T37 | feedback-ingestion: gateway real do banco | 8 | T32, T36 | Ingestion | Alta | 3 | 3 | Backlog |
| T38 | feedback-ingestion: gateway real da fila | 8 | T33, T36 | Ingestion | Alta | 2 | 3 | Backlog |
| T39 | critical-notifier: gatilho da fila e e-mail real | 8 | T21, T33, T34 | Notifier | Alta | 3 | 3 | Backlog |
| T40 | weekly-report: agendamento e gateways reais | 8 | T26, T32, T34 | Report | Alta | 3 | 4 | Backlog |
| T41 | Teste ponta a ponta na nuvem | 8 | T37, T38, T39, T40 | Ingestion | Alta | 2 | 1 | Backlog |
| T42 | Deploy automatizado das funções | 9 | T41 | CI/CD | Alta | 3 | 1 | Backlog |
| T43 | Monitoramento: logs, métricas e dashboard | 9 | T42 | Infra | Alta | 3 | 1 | Backlog |
| T44 | Alarmes de falha para os administradores | 9 | T43 | Infra | Alta | 2 | 1 | Backlog |
| T45 | Documentar arquitetura e modelo de cloud | 10 | T05, T41 | Docs | Média | 2 | 1 | Backlog |
| T46 | Documentar as funções | 10 | T41 | Docs | Média | 2 | 1 | Backlog |
| T47 | Documentar instruções de deploy | 10 | T42 | Docs | Média | 2 | 1 | Backlog |
| T48 | Documentar monitoramento e segurança | 10 | T35, T44 | Docs | Média | 2 | 2 | Backlog |
| T49 | Revisar o README para a entrega | 10 | T45, T46, T47, T48 | Docs | Alta | 2 | 1 | Backlog |
| T50 | Roteiro e gravação do vídeo de demonstração | 10 | T41, T44 | Docs | Alta | 3 | 1 | Backlog |
| T51 | Checklist final de entrega | 10 | T49, T50 | Docs | Alta | 1 | 1 | Backlog |

## 8. Rotina do grupo

### 8.1 Pegar uma task

1. No Board, olhe a coluna **Todo** e escolha a task de menor número cujas **dependências** (campo *Depende de*) já estejam em **Done**.
2. Atribua a você e arraste para **In Progress**.
3. Abra a issue e siga o **Passo a passo** de cima para baixo, marcando cada checkbox.
4. Quando uma task da coluna **Backlog** tiver todas as dependências em Done, mova-a para **Todo**.

### 8.2 Criar a branch

Pela issue: *Development → Create a branch* → **source `develop`** → nome sugerido na issue. Depois, na sua máquina:

```bash
git switch develop && git pull
git fetch origin
git switch feature/9-urgency
```

Ou crie direto pelo terminal:

```bash
git switch develop && git pull
git switch -c feature/9-urgency
```

### 8.3 Desenvolver com commits separados

Cada passo da issue é um commit. Exemplo da task *T09 - Domain: enum Urgency* (issue #9):

```bash
# 1. a classe
git add shared/feedback-domain/src/main
git commit -m "feat(domain): cria enum Urgency"

# 2. os testes
git add shared/feedback-domain/src/test
git commit -m "test(domain): cobre classificação de urgência"

mvn verify
git push -u origin feature/9-urgency
```

### 8.4 Abrir o PR

**base `develop`**; título em Conventional Commits (ex.: `feat(domain): cria enum Urgency`); `Closes #9` na descrição; um revisor; card em **In Review**.

### 8.5 Checks e revisão

| Check | O que valida |
|---|---|
| Build, testes e Sonar | Compilação, testes, cobertura e quality gate |
| Nome da branch e título do PR | Padrões de nome |
| Imagem `<função>` (após a milestone 6) | Build das imagens e health check |

O revisor aprova (*Review changes → Approve*) ou pede mudanças (o card volta para *In Progress*). Toda conversa precisa estar *Resolved*.

### 8.6 Merge

**Squash and merge**: os commits da branch viram um commit na `develop` com o título do PR, e continuam visíveis na aba *Commits* do PR. A branch remota é apagada.

A branch padrão do repositório é a `main`, então o `Closes #9` de um PR para a `develop` **não** fecha a issue no merge. Depois do merge, feche a issue (botão *Close issue* ou `gh issue close 9`): a automação *Item closed* leva o card para **Done**.

```bash
git switch develop && git pull
git branch -D feature/9-urgency   # -D: com squash, o Git não reconhece a branch como mergeada
```

### 8.7 Branch desatualizada

```bash
git fetch origin
git rebase origin/develop
git push --force-with-lease     # permitido só em branches de task
```

### 8.8 Promover `develop` → `main`

Ao fim de cada milestone: PR **base `main`** ← `develop`, título `chore: release <milestone>`, aprovação → **Create a merge commit**.

## 9. Docker e GitHub Container Registry

Disponível a partir da milestone 6 (tasks do Dockerfile, compose e workflow).

```bash
docker compose up --build -d     # as 3 funções
docker compose ps                # STATUS "healthy"
make smoke                       # chama os endpoints
docker compose down
```

Depois da primeira publicação no GHCR: repositório → **Packages** → cada imagem → *Package settings → Change visibility* → **Public**; em *Manage Actions access*, o repositório com papel **Write**.

## 10. Checklist da base

- [ ] Repositório, rulesets, labels, milestones, Project e issues criados (seção 2.2)
- [ ] Repositório público com `main` e `develop`
- [ ] Rulesets ativos e testados (4.3)
- [ ] **T01**: Sonar importado, análise automática desligada, `SONAR_TOKEN`, análise rodando no CI, badge no README
- [ ] **T02**: Board como primeira aba, views e automações; fluxo validado com o card indo para Done sozinho
- [ ] **T03**: integrantes no repositório e no Project
- [ ] Prazos das milestones preenchidos
- [ ] 51 issues (T01–T51) no Project com Status, Prioridade, Estimativa, Área e Depende de

## 11. Problemas comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `missing required scopes [project]` | `gh` sem escopo de Projects | `gh auth refresh -s project` |
| PR parado em *Waiting for status to be reported* | Nome do check no ruleset diferente do job | Ajuste o nome no ruleset (seção 4) |
| CI falha no Sonar citando *Automatic Analysis* | Análise automática ligada | Seção 5.2 |
| Sonar: *Project not found* | `sonar.projectKey` diferente da key do Sonar | Seção 5.5 |
| `push declined due to repository rule violations` | Push direto em `main`/`develop` | Abra um PR |
| Check de convenções falhou | Branch ou título fora do padrão | Edite o título; para a branch: `git branch -m <novo>`, `git push -u origin <novo>`, `git push origin --delete <antigo>` e novo PR |
| Cards sem Backlog/In Review | API não permitiu ajustar o Status | Seção 6.3 (manual) |
| Arquivo pessoal apareceu no `git status` | Não está no exclude local | `echo "<arquivo>" >> .git/info/exclude` (vale só na sua máquina; o `.gitignore` é para regras do grupo todo) |
| `quarkus:dev` não acha o `feedback-domain` | Domain não instalado no Maven local | `mvn install -DskipTests` |
