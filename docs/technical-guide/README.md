# Guia Técnico do Escala 24

Este guia apresenta a arquitetura, o domínio, os fluxos, as decisões técnicas
e a implementação atual do Escala 24. A documentação foi organizada para
apoiar tanto a manutenção do sistema quanto o entendimento progressivo dos
conceitos técnicos envolvidos no projeto.

## 1. Objetivo

O guia busca:

- documentar o funcionamento real do sistema;
- facilitar manutenção e evolução;
- registrar decisões técnicas relevantes;
- ajudar novos desenvolvedores a compreender o projeto;
- conectar conceitos de Engenharia de Software à implementação real.

## 2. Fonte da verdade

O código da versão atual do repositório é a principal fonte da verdade.

As afirmações da documentação devem ser confrontadas, quando aplicável, com:

- código de produção;
- testes automatizados;
- migrations do Flyway;
- configurações;
- estrutura e restrições do banco de dados;
- comportamento efetivamente implementado.

Funcionalidades planejadas não devem ser documentadas como se já estivessem
implementadas. Quando não houver evidência suficiente para uma afirmação, isso
deve ser indicado explicitamente.

## 3. Princípios da documentação

- Priorizar precisão técnica antes de quantidade de conteúdo.
- Explicar o conceito antes de aprofundar sua implementação.
- Usar o código real do Escala 24 como exemplo.
- Não copiar classes inteiras; utilizar pequenos trechos relevantes.
- Explicar siglas e termos técnicos na primeira ocorrência.
- Utilizar diagramas somente quando contribuírem para a compreensão.
- Diferenciar regra de negócio, validação, restrição do banco, segurança e
  decisão arquitetural.
- Evitar repetição excessiva entre capítulos.
- Não apresentar comportamento do framework como se fosse código implementado
  pelo projeto.
- Não afirmar que um comportamento é comprovado por testes quando não existe
  teste que efetivamente o exercite.

## 4. Estrutura didática dos capítulos

Os capítulos seguem, sempre que fizer sentido, uma progressão semelhante:

1. objetivo;
2. pergunta central;
3. explicação inicial do conceito;
4. implementação real no Escala 24;
5. exemplos do código;
6. fluxos ou diagramas;
7. decisões técnicas;
8. alternativas e consequências;
9. limitações;
10. onde estudar no código;
11. perguntas de revisão;
12. resumo;
13. frase final de fixação.

Essa estrutura é uma orientação, não uma obrigação mecânica. Uma seção pode
ser omitida quando não acrescentar valor ao capítulo.

## 5. Evidências e testes

A documentação deve distinguir claramente:

- comportamento identificado diretamente no código;
- comportamento comprovado por um teste existente;
- comportamento fornecido pela semântica padrão do framework;
- explicação conceitual ou didática;
- inferência técnica.

Nunca escrever “os testes comprovam” uma afirmação que o teste existente não
exercita diretamente. É preferível indicar exatamente qual teste verifica qual
cenário e reconhecer quando uma explicação deriva do código ou do framework,
sem ter sido testada explicitamente.

## 6. Diagramas

Mermaid pode ser utilizado para representar, por exemplo:

- arquitetura;
- fluxo de requisições;
- relacionamentos entre entidades;
- diagramas de sequência;
- estados;
- transações.

Todo diagrama deve ter finalidade didática, possuir uma legenda ou explicação
quando necessário e corresponder à implementação real. Diagramas não devem ser
incluídos apenas por estética.

## 7. Organização e roteiro

Os arquivos atualmente existentes nesta pasta são:

| Estado | Capítulo |
| --- | --- |
| ✅ Concluído | [01 — Visão geral](./01-visao-geral.md) |
| ✅ Concluído | [02 — Arquitetura](./02-arquitetura.md) |
| ✅ Concluído | [03 — Domínio e regras](./03-dominio-e-regras.md) |
| ✅ Concluído | [04 — Backend em camadas](./04-backend-em-camadas.md) |
| ✅ Concluído | [05 — Fluxo completo de uma requisição](./05-fluxo-completo-de-uma-requisicao.md) |
| ✅ Concluído | [06 — Tratamento de erros](./06-tratamento-de-erros.md) |
| ✅ Concluído | 07 — Segurança |
| ✅ Concluído | [08 — Banco de dados](./08-banco-de-dados.md) |
| ✅ Concluído | [09 — JPA e repositories](./09-jpa-e-repositories.md) |
| ✅ Concluído | [10 — Flyway](./10-flyway.md) |
| ✅ Concluído | [11 — Configuração e ambientes](./11-configuracao-e-ambientes.md) |
| ✅ Concluído | [12 — Testes: visão geral](./12-testes-visao-geral.md) |


Os capítulos anteriores encaminham autenticação, autorização, sessão e CSRF
para um capítulo próprio. Assim, o capítulo 07 aprofunda Segurança, e o
capítulo 08 inicia a documentação do modelo de banco de dados.
O capítulo 09 inicia a explicação dos mapeamentos JPA e do acesso por
repositories. O capítulo 10 inicia a explicação da evolução versionada do
schema com Flyway.
O capítulo 11 inicia a explicação das configurações e dos contextos de
execução.

Legenda:

- ✅ Concluído
- 🟡 Em elaboração
- ⬜ Planejado

## 8. Controle de escopo entre capítulos

Quando um assunto possui capítulo próprio, os capítulos anteriores devem
apresentar apenas o contexto necessário para que a progressão faça sentido.

Por exemplo:

- tratamento de erros não deve se transformar em um capítulo completo de
  segurança;
- fluxo de requisição não deve antecipar todo o conteúdo de JPA/Hibernate;
- arquitetura não deve repetir detalhadamente todos os capítulos posteriores.

O objetivo é preservar a progressão pedagógica, reduzir repetição e permitir
que cada capítulo aprofunde um conjunto coerente de responsabilidades.

## 9. Manutenção

Alterações relevantes no projeto podem exigir atualização da documentação.
Verifique o guia quando houver mudanças em:

- arquitetura;
- endpoints;
- regras de negócio;
- entidades e relacionamentos;
- banco de dados ou migrations;
- segurança;
- configuração;
- fluxos principais;
- tecnologias;
- execução ou deploy.

A documentação deve representar a versão atual do projeto, e não uma versão
histórica ou uma funcionalidade planejada.

## 10. Checklist antes de considerar um capítulo concluído

- [ ] Conteúdo confrontado com o código atual.
- [ ] Funcionalidades futuras não foram apresentadas como atuais.
- [ ] Afirmações sobre testes foram verificadas.
- [ ] Links relativos foram conferidos.
- [ ] Blocos Mermaid foram conferidos, quando utilizados.
- [ ] Não há informações sensíveis.
- [ ] Não há repetição excessiva.
- [ ] Ortografia e Markdown foram revisados.
- [ ] `git diff --check` não apresenta problemas.
- [ ] O capítulo foi revisado antes do commit.

## 11. Como navegar pelo guia

Para quem está conhecendo o projeto, recomenda-se a leitura sequencial: visão
geral, arquitetura, domínio, camadas e fluxo completo. Desenvolvedores que já
conhecem o sistema também podem consultar capítulos isoladamente conforme o
assunto necessário.
