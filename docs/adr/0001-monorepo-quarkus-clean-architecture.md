# 0001 - Monorepo Maven, Quarkus e Clean Architecture

- **Status:** Aceita
- **Data:** 2026-10-15

## Contexto

O Tech Challenge da Fase 4 pede uma plataforma de feedback de aulas **serverless**, rodando em **nuvem**, com no mínimo **2 funções** que respeitem **responsabilidade única** (a separação é avaliada). O fluxo tem três responsabilidades distintas: receber e gravar a avaliação (`POST /avaliacao`), enviar o e-mail de urgência para avaliações críticas e gerar o relatório semanal.

Restrições:

- O provedor de nuvem ainda não foi escolhido (ADR 0002), então o código não pode depender de uma nuvem específica.
- O grupo trabalha com Java.
- As três funções compartilham as mesmas regras de negócio (nota de 0 a 10, classificação de urgência, validação da descrição).
- A qualidade do código, os testes e a documentação também são avaliados.

## Opções consideradas

### Organização do código

1. **Um repositório por função** — prós: isolamento total, deploy e versionamento independentes. Contras: as regras de negócio compartilhadas viram cópia ou biblioteca publicada à parte; três CIs, três projetos no Sonar e três backlogs para um grupo pequeno.
2. **Monorepo Maven multi-módulo** — prós: um build, um CI e um Sonar; o domínio é um módulo compartilhado sem publicação externa; mudanças que atravessam funções entram em um único PR. Contras: o build cresce junto com o projeto e é preciso disciplina para uma função não depender de outra.

### Framework das funções

1. **Spring Boot** — prós: mais conhecido e com mais material. Contras: inicialização mais lenta e maior consumo de memória, o que pesa no *cold start* de funções serverless; suporte às funções de cada nuvem por projetos separados (Spring Cloud Function).
2. **Quarkus** — prós: inicialização rápida e baixo consumo de memória; extensões prontas para AWS Lambda, Azure Functions e Google Cloud Functions; **Funqy** abstrai o gatilho (HTTP, fila, agendamento), então a mesma função roda localmente via HTTP e na nuvem acionada por evento. Contras: comunidade menor e menos familiaridade do grupo.

### Organização interna de cada módulo

1. **Camadas simples (controller → service → repository)** — prós: menos classes, rápido de começar. Contras: a regra de negócio tende a misturar com framework e nuvem; trocar de provedor ou testar sem subir a aplicação fica difícil.
2. **Clean Architecture** — prós: regras isoladas de framework e nuvem; casos de uso testáveis com JUnit puro; a nuvem entra só como implementação de gateways. Contras: mais classes e interfaces, e uma regra de dependência que precisa ser verificada em toda revisão.

## Decisão

- **Monorepo Maven**: POM pai na raiz (BOM do Quarkus, versões de plugins, JaCoCo e Sonar), módulo `shared/feedback-domain` com o domínio compartilhado em Java puro e um módulo por função em `functions/`: `feedback-ingestion`, `critical-notifier` e `weekly-report`.
- **Quarkus**: Quarkus REST na função HTTP (`feedback-ingestion`) e Quarkus Funqy nas funções acionadas por fila (`critical-notifier`) e por agendamento (`weekly-report`).
- **Clean Architecture** com três camadas no pacote `br.com.fiap.feedback`:
  - `domain`: entidades, regras e eventos, sem dependência de framework;
  - `application`: casos de uso e interfaces de gateway, dependendo só do `domain`;
  - `infra`: HTTP, fila, agendamento, banco, e-mail e montagem dos casos de uso com CDI.
- **Gateways provisórios** (em memória ou log) anotados com `@DefaultBean`: quando a implementação da nuvem escolhida for criada, ela substitui a provisória sem mudar mais nada.

## Consequências

- **Um CI e um Sonar para tudo**: um único pipeline compila, testa e mede a cobertura de todos os módulos.
- **Deploy independente por função**: cada módulo gera seu próprio artefato e sua própria imagem, e pode ser publicado sozinho.
- **Trocar de nuvem afeta só a `infra`**: `domain` e `application` não conhecem o provedor, o que permite começar o código antes das ADRs 0002 e 0003.
- **Regras testadas sem subir o Quarkus**: domínio e casos de uso são testados com JUnit puro, com o `Clock` injetado para os testes serem determinísticos.
- **Mais classes e mais revisão**: cada recurso externo exige interface em `application` e implementação em `infra`, e a regra de dependência entre as camadas precisa ser conferida em todo PR.
- **Build acoplado**: uma mudança no `feedback-domain` recompila e retesta as três funções.
