# Como contribuir

## Fluxo de branches

```
feature/* ──squash──▶ develop ──merge commit──▶ main
```

- `main`: versão estável, a que vai para a entrega e para o deploy de produção.
- `develop`: integração do grupo. **Todo PR de tarefa aponta para `develop`.**
- Branches de trabalho saem de `develop` e são apagadas após o merge.

## Nome das branches

`<tipo>/<numero-da-issue>-<descricao-curta>`, em minúsculas e com hífens.

| Tipo | Uso |
|---|---|
| `feature` | nova funcionalidade |
| `fix` / `hotfix` | correção |
| `infra` | nuvem, IaC |
| `ci` | pipelines |
| `docs` | documentação |
| `refactor`, `test`, `chore` | demais casos |

Exemplos: `feature/12-endpoint-avaliacao`, `infra/7-banco-feedbacks`, `docs/23-arquitetura`.

## Commits e título do PR

[Conventional Commits](https://www.conventionalcommits.org/pt-br/): `<tipo>(<escopo opcional>): <descrição>`.

Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `infra`, `revert`.
Escopos sugeridos: `domain`, `ingestion`, `notifier`, `report`, `docker`, `infra`, `ci`.

```
feat(domain): cria enum Urgency
test(domain): cobre classificação de urgência
feat(ingestion): cria endpoint POST /avaliacao
infra: provisiona tabela de feedbacks
```

Dentro da branch de uma task, faça **commits separados** por passo (ex.: um `feat` para a classe e um `test` para os testes). Cada issue sugere a sequência.

Como o merge em `develop` é squash, os commits da branch viram **um commit por task** na `develop`, com o **título do PR** como mensagem — por isso ele é validado no CI. Os commits individuais continuam visíveis na aba *Commits* do PR.

## Código

- Nomes de classes, métodos e variáveis em inglês; mensagens ao usuário, logs e documentação em português.
- Clean Architecture: `domain` não importa nada; `application` importa só `domain`; Quarkus, DTOs e SDKs de nuvem ficam em `infra`.
- Novo acesso a recurso externo = interface em `application/<contexto>/gateway` + implementação em `infra/`.
- Toda mudança de regra vem com teste. O Sonar roda em cada PR.

## Pull Request

1. No Board do Project, pegue a task de menor número na coluna **Todo** cujas dependências (campo **Depende de**) já estejam em **Done**. Atribua a você e mova para **In Progress**.
2. Crie a branch a partir de `develop` atualizada.
3. Siga o **Passo a passo** da issue: cada passo é um commit, com os arquivos e o que criar. Marque o checkbox de cada um e rode `mvn verify` no final.
4. Abra o PR para `develop` com `Closes #<issue>` na descrição e mova o card para **In Review**.
5. CI verde + 1 aprovação de outro integrante → squash merge. Depois, feche a issue: o card vai para **Done** sozinho.
