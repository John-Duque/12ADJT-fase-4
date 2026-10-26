# 12ADJT-fase-4 — Plataforma de Feedback Serverless

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml) [![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=John-Duque_12ADJT-fase-4&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=John-Duque_12ADJT-fase-4)

Plataforma em que **estudantes avaliam as aulas** de um curso on-line e **administradores acompanham a satisfação**: avaliações críticas geram um alerta imediato por e-mail e, toda semana, um relatório consolidado chega aos administradores.

Projeto do **Tech Challenge – Fase 4 (POS TECH · Arquitetura e Desenvolvimento Java)**, com foco em Cloud Computing, Serverless e deploy em nuvem.

> **Status:** base configurada (CI, Sonar, convenções e backlog). O código é construído por etapas, uma task por vez — o andamento está no GitHub Project do repositório e na seção [Roadmap](#roadmap).

## Sumário

- [A proposta](#a-proposta)
- [Como a solução funciona](#como-a-solução-funciona)
- [Funções serverless](#funções-serverless)
- [Contratos e regras de negócio](#contratos-e-regras-de-negócio)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Roadmap](#roadmap)
- [Como rodar](#como-rodar)
  - [Como testar localmente](#como-testar-localmente)
- [Qualidade e automação](#qualidade-e-automação)
- [Fluxo de trabalho](#fluxo-de-trabalho)
- [Documentação](#documentação)
- [Grupo](#grupo)

---

## A proposta

### Problema

Para garantir a qualidade de cursos on-line, os estudantes precisam conseguir avaliar as aulas e os administradores precisam acompanhar rapidamente essas avaliações — principalmente as negativas.

### Objetivo

Uma aplicação hospedada em nuvem, com **funções serverless** que automatizam:

1. o **recebimento** das avaliações;
2. o **envio de notificações** quando uma avaliação é crítica;
3. a **geração de relatórios** periódicos.

### Requisitos do Tech Challenge

| Requisito | Como será atendido |
|---|---|
| Implementar serverless (obrigatório) | Três funções em Quarkus, publicadas como funções do provedor de nuvem |
| Rodar em ambiente cloud (obrigatório) | Provedor definido na ADR 0002 |
| No mínimo duas funções, com responsabilidade única | Três funções, cada uma com um único motivo para mudar e um gatilho próprio |
| Segurança dos dados e governança de acesso | Identidade por função com privilégio mínimo, criptografia, segredos em cofre, MFA |
| Componentes de suporte (banco etc.) | Banco gerenciado, fila com DLQ, serviço de e-mail e agendador |
| Deploy automatizado das funções | Pipeline no GitHub Actions a cada merge na `main` |
| Aplicação monitorada | Logs centralizados, métricas e dashboard |
| Notificações automáticas de problemas críticos | E-mail de urgência para avaliações críticas e alarmes de falha |
| Relatório semanal com média das avaliações | Função agendada que consolida a semana |

### Entregáveis

- Repositório público com o código-fonte e a documentação.
- Vídeo de demonstração com a aplicação funcionando, as funções ativas e as configurações da nuvem.

---

## Como a solução funciona

```mermaid
flowchart LR
    estudante([Estudante]) -->|POST /avaliacao| gw[API Gateway]
    gw --> ingestion[[feedback-ingestion]]
    ingestion -->|grava| db[(Banco de feedbacks)]
    ingestion -->|nota de 0 a 3| fila{{Fila de críticos}}
    fila --> notifier[[critical-notifier]]
    notifier -->|e-mail de urgência| admin([Administradores])
    agenda((Agendamento semanal)) --> report[[weekly-report]]
    report -->|lê os últimos 7 dias| db
    report -->|relatório| admin
    ingestion & notifier & report -.-> mon[Logs, métricas e alarmes]
```

1. O estudante envia a avaliação (`POST /avaliacao`) com uma descrição e uma nota de 0 a 10.
2. A função **feedback-ingestion** valida, classifica a urgência pela nota e grava no banco.
3. Se a avaliação for **crítica**, ela publica um evento numa fila.
4. A função **critical-notifier** consome a fila e envia o **e-mail de urgência** aos administradores.
5. Uma vez por semana, a função **weekly-report** lê as avaliações dos últimos 7 dias e envia o **relatório** com contagens e média.

A fila entre o recebimento e a notificação desacopla as duas funções: se o envio de e-mail falhar, o recebimento de avaliações continua funcionando e a mensagem pode ser reprocessada (DLQ).

---

## Funções serverless

| Função | Responsabilidade única | Gatilho | Entrada | Saída |
|---|---|---|---|---|
| [`feedback-ingestion`](functions/feedback-ingestion/README.md) | Receber, validar e gravar a avaliação; publicar as críticas | HTTP `POST /avaliacao` | JSON da avaliação | Registro no banco; evento na fila (se crítica); resposta `201` |
| [`critical-notifier`](functions/critical-notifier/README.md) | Avisar os administradores sobre avaliações críticas | Mensagem na fila de críticos | `CriticalFeedbackEvent` | E-mail de urgência |
| [`weekly-report`](functions/weekly-report/README.md) | Consolidar a semana e enviar o relatório | Agendamento semanal | — | E-mail com o relatório |

Cada função tem **um motivo para mudar**: o contrato da API, o canal de notificação ou as métricas do relatório. É essa separação que o Tech Challenge avalia.

Cada uma tem o próprio README, com contrato, exemplos de requisição, como rodar e como testar: [feedback-ingestion](functions/feedback-ingestion/README.md), [critical-notifier](functions/critical-notifier/README.md) e [weekly-report](functions/weekly-report/README.md).

---

## Contratos e regras de negócio

### `POST /avaliacao`

Requisição:

```json
{
  "descricao": "A aula ficou sem áudio a partir dos 10 minutos",
  "nota": 2
}
```

Respostas:

| Status | Quando | Corpo |
|---|---|---|
| `201 Created` | Avaliação aceita | `{"id": "8f14e45f-…", "urgencia": "HIGH", "dataEnvio": "2026-10-13T12:00:00Z"}` |
| `400 Bad Request` | Descrição vazia ou com mais de 1000 caracteres; nota ausente ou fora de 0–10 | Mensagem de erro em português |

### Classificação de urgência

| Nota | Urgência | Gera alerta? |
|---|---|---|
| 0 a 3 | `HIGH` | Sim |
| 4 a 6 | `MEDIUM` | Não |
| 7 a 10 | `LOW` | Não |

### E-mail de urgência

Contém **descrição**, **urgência** e **data de envio** da avaliação.

### Relatório semanal

- Período: os **7 dias completos** anteriores à execução, no fuso `America/Sao_Paulo`.
- Conteúdo: lista das avaliações (**descrição, urgência, data de envio**), **quantidade de avaliações por dia**, **quantidade por urgência** e **média das notas**.

---

## Tecnologias

| Tecnologia | Uso | Por quê |
|---|---|---|
| **Java 21** | Linguagem | Versão LTS, records e recursos modernos |
| **Quarkus 3.40** | Framework das funções | Inicialização rápida (menor *cold start*) e extensões prontas para AWS Lambda, Azure Functions e Google Cloud Functions |
| **Quarkus REST** | `feedback-ingestion` | Endpoint HTTP com validação (Hibernate Validator) e JSON (Jackson) |
| **Quarkus Funqy** | `critical-notifier` e `weekly-report` | A mesma função roda localmente via HTTP e na nuvem acionada por fila ou agendamento, sem prender o código a um provedor |
| **Maven** (multi-módulo) | Build | Um POM pai com versões centralizadas e um módulo por função |
| **JUnit 5, AssertJ, RestAssured** | Testes | Testes unitários das regras e testes de integração das funções (`@QuarkusTest`) |
| **JaCoCo** | Cobertura | Relatório de cobertura enviado ao Sonar |
| **SonarQube Cloud** | Qualidade | Bugs, vulnerabilidades, duplicação e cobertura em cada PR |
| **GitHub Actions** | CI/CD | Build, testes, análise, imagens Docker e deploy |
| **Docker** | Containers | Imagem multi-stage por função e execução local com Compose |
| **Provedor de nuvem e IaC** | Infraestrutura | A definir nas ADRs 0002 e 0003 |

---

## Arquitetura

### Clean Architecture

Todas as funções seguem a mesma organização em três camadas, no pacote `br.com.fiap.feedback`:

```mermaid
flowchart LR
    infra["<b>infra</b><br/>Quarkus, HTTP, fila,<br/>banco, e-mail, agendamento"] --> application["<b>application</b><br/>casos de uso<br/>+ gateways (interfaces)"]
    application --> domain["<b>domain</b><br/>entidades, regras<br/>e eventos"]
```

| Camada | Contém | Pode depender de |
|---|---|---|
| `domain` | Entidades (`Feedback`, `WeeklyReport`), regras (`Urgency`), eventos (`CriticalFeedbackEvent`) e exceções de negócio | Nada além do Java |
| `application` | Casos de uso (`ReceiveFeedbackUseCase`, `NotifyCriticalFeedbackUseCase`, `GenerateWeeklyReportUseCase`) e as interfaces **gateway** para o mundo externo | Apenas `domain` |
| `infra` | Controllers e DTOs, consumidor da fila, gatilho agendado, implementações dos gateways (banco, fila, e-mail), configuração CDI e tratamento de erros | `application` e `domain` |

Consequências práticas:

- As regras de negócio são testadas com JUnit puro, sem subir o Quarkus.
- Trocar o provedor de nuvem muda só a camada `infra` e as extensões do `pom.xml`.
- Enquanto a nuvem não está pronta, os gateways têm implementações provisórias (memória e log) marcadas com `@DefaultBean`; quando a implementação real é criada, ela as substitui sem alterar casos de uso.

### Decisões de arquitetura

Registradas como ADRs em [`docs/adr`](docs/adr):

| ADR | Decisão |
|---|---|
| 0001 | Monorepo Maven, Quarkus e Clean Architecture |
| 0002 | Provedor de nuvem |
| 0003 | Ferramenta de infraestrutura como código |

---

## Estrutura do repositório

Estrutura planejada. Cada parte é criada pela milestone indicada.

```
12ADJT-fase-4/
├── pom.xml                              # POM pai: Java 21, BOM do Quarkus, JaCoCo, Sonar
├── shared/
│   └── feedback-domain/                 # [M2] camada domain compartilhada (Java puro)
│       └── br/com/fiap/feedback/domain/
│           ├── feedback/                #   Feedback, Urgency, InvalidFeedbackException
│           │   └── event/               #   CriticalFeedbackEvent
│           └── shared/exception/        #   DomainException
├── functions/
│   ├── feedback-ingestion/              # [M3] Quarkus REST — POST /avaliacao
│   │   └── br/com/fiap/feedback/
│   │       ├── application/feedback/    #   ReceiveFeedbackUseCase + gateway/
│   │       └── infra/                   #   web/, persistence/, messaging/, config/, shared/exception/
│   ├── critical-notifier/               # [M4] Quarkus Funqy — e-mail de urgência
│   │   └── br/com/fiap/feedback/
│   │       ├── application/notification/
│   │       └── infra/                   #   messaging/, notification/, config/
│   └── weekly-report/                   # [M5] Quarkus Funqy — relatório semanal
│       └── br/com/fiap/feedback/
│           ├── domain/report/           #   WeeklyReport
│           ├── application/report/
│           └── infra/                   #   scheduling/, persistence/, notification/, config/
├── Dockerfile                           # [M6] multi-stage único para as três funções
├── compose.yaml                         # [M6] sobe as três funções localmente
├── Makefile                             # [M6] atalhos de build, testes e Docker
├── infra/                               # [M7] infraestrutura como código
├── docs/
│   ├── guia-github.md                   # configuração do GitHub, Sonar e Project
│   ├── adr/                             # decisões de arquitetura
│   ├── arquitetura.md                   # [M10]
│   ├── funcoes.md                       # [M10]
│   ├── deploy.md                        # [M10]
│   ├── monitoramento.md                 # [M10]
│   └── seguranca.md                     # [M10]
├── .github/
│   ├── workflows/ci.yml                 # build, testes e Sonar
│   ├── workflows/pr-conventions.yml     # nome da branch e título do PR
│   ├── workflows/docker.yml             # [M6] imagens e publicação no GHCR
│   ├── ISSUE_TEMPLATE/                  # templates de task e bug
│   ├── pull_request_template.md
│   └── dependabot.yml
├── CONTRIBUTING.md
└── README.md
```

Portas locais: `feedback-ingestion` **8081**, `critical-notifier` **8082**, `weekly-report` **8083**.

---

## Roadmap

O trabalho está dividido em 10 milestones e 54 tasks numeradas (T01 a T54) no GitHub Project, visualizado como Board. Cada task diz de quais outras depende, é feita numa branch própria e tem um passo a passo em que cada passo é um commit.

| Milestone | Entrega | Status |
|---|---|---|
| 1. Fundação | Sonar, Project, grupo e ADRs | 🔄 em andamento |
| 2. Domínio | Módulo `feedback-domain`: exceções, `Urgency`, `Feedback`, evento crítico | ✅ concluída |
| 3. Função feedback-ingestion | `POST /avaliacao` com validação e tratamento de erros | ✅ concluída |
| 4. Função critical-notifier | Consumo do evento e e-mail de urgência | ✅ concluída |
| 5. Função weekly-report | Agregações e relatório semanal | ✅ concluída |
| 6. Docker | Dockerfile multi-stage, Compose, imagens no CI e no GHCR, testes locais | ✅ concluída |
| 7. Infraestrutura cloud | Conta, IaC, banco, fila, e-mail e segurança | ⏳ |
| 8. Integração com a nuvem | Extensões do provedor e gateways reais | ⏳ |
| 9. Deploy e observabilidade | Deploy automatizado, monitoramento e alarmes | ⏳ |
| 10. Documentação e entrega | Documentos, vídeo e checklist final | ⏳ |

---

## Como rodar

### Pré-requisitos

- JDK 21
- Maven 3.9+
- Docker (a partir da milestone 6)

### Build e testes

```bash
mvn verify
```

Executa build, testes e cobertura de todos os módulos já criados.

### Funções em modo de desenvolvimento

Disponível conforme cada função é criada (milestones 3 a 5):

```bash
mvn install -DskipTests                                     # instala o feedback-domain no Maven local
cd functions/feedback-ingestion && mvn quarkus:dev          # http://localhost:8081
```

```bash
curl -i -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' -d '{"descricao": "Aula sem áudio", "nota": 2}'
```

### Com Docker

Um único `Dockerfile` multi-stage gera a imagem de cada função (escolhida por `--build-arg MODULE`), e o `compose.yaml` sobe as três juntas:

```bash
docker compose up --build -d     # sobe as três funções (8081, 8082 e 8083)
docker compose ps                # cada serviço deve aparecer como healthy
make smoke                       # chama /avaliacao, /notify-critical-feedback e /generate-weekly-report
docker compose down
```

Para uma função só:

```bash
docker compose up --build -d feedback-ingestion                       # sobe só o serviço escolhido
docker build --target test --build-arg MODULE=feedback-ingestion .    # roda os testes dela dentro do Docker
```

`make` sem argumentos lista os outros atalhos (`verify`, `dev-ingestion`, `docker-test`, `logs`, `clean`…).

### Como testar localmente

**1. Subir as funções**

```bash
make up      # sobe as três funções com o compose (8081, 8082 e 8083)
make ps      # os três serviços devem aparecer como healthy
```

Para uma função só em modo dev: `make dev-ingestion`, `make dev-notifier` ou `make dev-report`.

**2. Testar com `curl`**

Cada comando em **uma linha só**: ao colar um comando quebrado com `\`, um espaço depois da `\` separa o comando em dois no zsh.

```bash
curl -i -X POST localhost:8081/avaliacao -H 'Content-Type: application/json' -d '{"descricao":"Aula sem áudio","nota":2}'
curl -i -X POST localhost:8082/notify-critical-feedback -H 'Content-Type: application/json' -d '{"id":"8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}'
curl -i -X POST localhost:8083/generate-weekly-report
```

| Caso | Esperado |
|---|---|
| `POST /avaliacao` com nota 9 / 5 / 2 | `201` com `urgencia` `LOW` / `MEDIUM` / `HIGH`, `id` e `dataEnvio` |
| Nota 11 ou -1, sem `descricao`, `descricao` em branco, sem `nota`, sem corpo | `400` com a mensagem da validação |
| Evento `HIGH` / `LOW` no notifier | `200` com `notified` `true` / `false` |
| Relatório | `200` com o período, `totalFeedbacks = 0` e os 7 dias em `countByDay` |
| `GET /q/health/ready` nas três portas | `200` com `status = UP` |

O `curl` de cada caso, com a resposta completa, está no README da função: [feedback-ingestion](functions/feedback-ingestion/README.md), [critical-notifier](functions/critical-notifier/README.md) e [weekly-report](functions/weekly-report/README.md).

**3. Testar com Postman**

A coleção [`docs/postman/12adjt-fase-4.postman_collection.json`](docs/postman/12adjt-fase-4.postman_collection.json) tem uma pasta por função com todos os casos da tabela (15 requisições), e cada requisição confere o status e os campos da resposta.

1. No Postman: **Import** → selecione o arquivo da coleção.
2. Abra a coleção → **Run** (*Collection Runner*) → **Run 12ADJT-fase-4**.

As URLs ficam nas variáveis da coleção (`ingestionUrl`, `notifierUrl`, `reportUrl`). Sem o Postman instalado, rode pelo terminal:

```bash
npx newman run docs/postman/12adjt-fase-4.postman_collection.json
```

**4. O que aparece nos logs** (`make logs`)

```
Feedback crítico publicado: id=... urgencia=HIGH
[ALERTA] urgencia=HIGH dataEnvio=2026-10-13T12:00:00Z descricao=Aula sem áudio
Evento ignorado: feedback 3c6e0b8a-... não é crítico (urgencia=LOW)
Relatório 2026-10-19 a 2026-10-25: total=0 media=0.00 porUrgencia={HIGH=0, MEDIUM=0, LOW=0}
```

**5. O que dá e o que ainda não dá para testar localmente**

| Dá para testar | Ainda não dá (chega com as milestones 7 e 8) |
|---|---|
| Contrato e validações do `POST /avaliacao` | Integração entre as funções: cada container tem os próprios dados e a fila ainda é só um log, então o notifier é chamado direto, simulando a mensagem |
| Classificação de urgência pela nota | Relatório com dados: o gateway provisório devolve uma lista vazia |
| Alerta do notifier (no log) | E-mail real aos administradores |
| Período e estrutura do relatório | Publicação no GHCR (só no `push` do GitHub Actions) |
| Health checks, imagens Docker e o smoke test do workflow | |

Comportamento atual: um evento inválido no notifier (descrição em branco ou urgência que não bate com a nota) responde **500**. Na AWS, a mensagem volta para a fila e, depois de 3 tentativas, vai para a DLQ.

**6. Validar o workflow Docker localmente**

```bash
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest    # sintaxe do workflow; sem saída = sem erros
```

O mesmo build e smoke test do job `Imagem <função>`, para as três funções:

```bash
for entry in feedback-ingestion:8081 critical-notifier:8082 weekly-report:8083; do
  module=${entry%%:*}; port=${entry##*:}
  docker build --target runtime --build-arg MODULE=$module --build-arg PORT=$port -t teste/$module . || break
  docker rm -f smoke >/dev/null 2>&1
  docker run -d --name smoke -p "$port:$port" teste/$module >/dev/null
  ok=0
  for attempt in $(seq 1 30); do
    curl -fsS "localhost:$port/q/health/ready" >/dev/null 2>&1 && { echo "✅ $module pronta"; ok=1; break; }
    sleep 2
  done
  [ $ok = 1 ] || { echo "❌ $module não ficou pronta"; docker logs smoke; }
  docker rm -f smoke >/dev/null
done
```

Rode da raiz do repositório (onde está o `Dockerfile`) e com `make down` antes, para as portas estarem livres.

---

## Qualidade e automação

| Automação | Quando roda | O que faz |
|---|---|---|
| **CI** (`ci.yml`) | Push e PR em `main` e `develop` | Build, testes, cobertura JaCoCo e análise no SonarQube Cloud |
| **Convenções do PR** (`pr-conventions.yml`) | Todo PR | Valida o nome da branch e o título em Conventional Commits |
| **Dependabot** | Semanal/mensal | PRs de atualização de dependências Maven e de Actions |
| **Rulesets** | Sempre | `main` e `develop` só recebem código por PR, com 1 aprovação e checks verdes |

O quality gate do Sonar exige no código novo: cobertura mínima de 80%, nenhum bug ou vulnerabilidade novos e duplicação de até 3%.

---

## Fluxo de trabalho

```
feature/* ──squash──▶ develop ──merge commit──▶ main
```

1. No Board do Project, pegar a task de menor número (T01…T51) na coluna **Todo** cujas dependências já estejam em **Done**, e mover para **In Progress**.
2. Criar a branch a partir da `develop`: `<tipo>/<issue>-<descricao>` (ex.: `feature/9-urgency`).
3. Fazer os commits do passo a passo da issue, um de cada vez, em Conventional Commits.
4. Abrir o PR para `develop` com `Closes #<issue>` → checks verdes → 1 aprovação → *Squash and merge*.
5. Ao fim de cada milestone, promover `develop` → `main`.

Detalhes em [CONTRIBUTING.md](CONTRIBUTING.md) e em [docs/guia-github.md](docs/guia-github.md).

---

## Documentação

| Documento | Conteúdo | Status |
|---|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branches, commits, PR e regras de código | ✅ |
| [docs/guia-github.md](docs/guia-github.md) | GitHub, rulesets, Sonar, Project e rotina do grupo | ✅ |
| [docs/adr](docs/adr) | Decisões de arquitetura | 🔄 |
| [docs/postman](docs/postman) | Coleção do Postman com os testes locais das funções | ✅ |
| `docs/arquitetura.md` | Modelo de cloud e componentes | ⏳ |
| `docs/funcoes.md` | Documentação de cada função | ⏳ |
| `docs/deploy.md` | Instruções de deploy | ⏳ |
| `docs/monitoramento.md` | Configuração do monitoramento | ⏳ |
| `docs/seguranca.md` | Segurança e governança de acesso | ⏳ |
| Vídeo de demonstração | Aplicação funcionando na nuvem | ⏳ |

---

## Grupo

| Nome | RM | GitHub |
|---|---|---|
| John Pablo da Silva Gomes | RM372387 | [@John-Duque](https://github.com/John-Duque) |
| Jefferson Ricardo dos Santos | RM371825 | [@jeffesa](https://github.com/jeffesa) |
