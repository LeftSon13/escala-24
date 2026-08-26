# 24 — Git e Pull Requests

## Objetivo deste capítulo

Este capítulo explica como o histórico e a revisão colaborativa organizam as
mudanças do Escala 24.

> **Pergunta central:** como Git e Pull Requests ajudam a organizar, revisar e
> integrar mudanças?

## Conceitos essenciais

Git é um sistema distribuído de controle de versão. O repositório contém
arquivos e histórico; um commit é um snapshot lógico; branch é uma linha de
desenvolvimento; `origin` é a referência remota; `push` envia commits; `fetch`
atualiza referências sem integrar; `pull` busca alterações do repositório remoto e tenta integrá-las à branch
atual, de acordo com a configuração do Git; merge une históricos.

Pull Request (PR) é uma proposta de integração na plataforma GitHub, não um
recurso do Git puro.

## Evidências no projeto

O histórico possui commits `feat:`, `fix:`, `docs:`, `test:`, `chore:` e merges
de Pull Requests. Há branches `main` e `docs/technical-handbook`, além de
referências a branches como `feature/`, `fix/`, `test/`, `ci/` e `release/` em
merges históricos. Isso mostra padrões observados, não uma convenção obrigatória
formal.

Um fluxo compatível com o que os arquivos e histórico mostram é:

```mermaid
flowchart LR
    A[Branch de trabalho] --> B[Commit]
    B --> C[Push para remoto]
    C --> D[Pull Request no GitHub]
    D --> E[Workflows e checks]
    E --> F[Revisão]
    F --> G[Merge em main]
```

O YAML configura workflows em pushes e PRs; revisão e regras de merge dependem
do GitHub. O histórico confirma merges de PR, mas não permite afirmar que todo
trabalho sempre seguiu exatamente a sequência.

## Branches, commits e histórico

Uma branch separada isola mudanças e facilita revisão. Mensagens como
`docs: add unit testing chapter` e `fix: ...` tornam o propósito legível. Isso
é padrão observado, não uma política formal de Conventional Commits.

Histórico compreensível significa conseguir relacionar propósito, commit,
branch e integração; não significa obrigatoriamente eliminar merge commits.
Merge preserva a integração de históricos. Rebase reaplica commits sobre outra base e reescreve seus identificadores. Ele
pode produzir um histórico mais linear, mas exige cuidado quando os commits já
foram compartilhados com outras pessoas. Nenhuma política formal do
projeto impõe merge ou rebase.

Conflitos surgem quando linhas alteram regiões incompatíveis e Git não decide
sozinho. `git status` ajuda a identificar o estado durante a resolução.

## Comandos usados no fluxo

Alguns comandos aparecem com frequência no trabalho do projeto:

```bash
git status
git branch --show-current
git fetch origin
git add <arquivo>
git commit -m "mensagem"
git push origin <branch>
```

`git status` deve ser consultado antes e depois de operações importantes porque
mostra a branch atual, alterações locais, arquivos preparados para commit e
eventuais conflitos.

`git fetch origin` atualiza as referências do repositório remoto sem integrar
automaticamente essas mudanças à branch atual. Depois da revisão e do commit,
`git push` publica os commits locais no remoto.

Ao concluir uma tarefa, um estado como:
```bash
nothing to commit, working tree clean
```
indica que não existem alterações locais pendentes naquele momento. Isso não
significa, por si só, que a branch esteja sincronizada com todas as outras
branches do repositório.

## Estudo de caso e cuidados

No fluxo didático, o desenvolvedor cria branch, commita e faz push; o GitHub
recebe o PR, executa CI, recebe revisão e pode integrar. Commit e push não são a
mesma coisa; fetch e pull também não. Verifique a branch e o status antes de
integrar e não versione `.env` ou secrets, conforme capítulo 23.

## Onde estudar no código

- [`backend-ci.yml`](../../.github/workflows/backend-ci.yml)
- [`CHANGELOG.md`](../../CHANGELOG.md)
- [`README.md`](../../README.md)
- [`22 — Integração contínua`](./22-integracao-continua.md)
- [`23 — Segurança do repositório`](./23-seguranca-do-repositorio.md)

## Perguntas de revisão

1. Qual a diferença entre commit e push?
2. Por que PR pertence ao GitHub, e não ao Git puro?
3. O que fetch faz sem necessariamente fazer pull?
4. Qual vantagem uma branch separada oferece?
5. O que caracteriza um conflito?
6. Por que rebase pode ser delicado em histórico compartilhado?
7. O que é padrão observado e o que seria uma política formal?

## Resumo

Git registra snapshots e linhas de desenvolvimento; o GitHub adiciona PRs,
checks e revisão. O histórico do Escala 24 mostra branches, commits descritivos
e merges, mas não prova uma política obrigatória para todo fluxo.

> **Frase de fixação:** commit registra, branch organiza e Pull Request propõe
> integração.
