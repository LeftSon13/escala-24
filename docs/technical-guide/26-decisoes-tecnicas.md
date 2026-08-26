# 26 — Decisões técnicas

## Objetivo deste capítulo

Este capítulo organiza decisões arquiteturais como problema, benefício e
trade-off, sem inventar motivações históricas da equipe.

> **Pergunta central:** quais decisões estruturais moldaram o Escala 24 e quais
> custos elas introduzem?

## Mapa das decisões

| Decisão | Problema | Benefício | Trade-off | Onde estudar |
| --- | --- | --- | --- | --- |
| API REST/JSON | frontend precisa conversar com backend | separação e contrato HTTP | rede, segurança e versionamento | `controller/`, cap. 05/15 |
| Sessão em vez de JWT | autenticar usuários web | estado de sessão e integração atual | estado no servidor e gestão de cookie | `SecurityConfig`, cap. 07 |
| PostgreSQL relacional | dados relacionados e consistentes | FKs, constraints e transações | schema e operação do banco | migrations, cap. 08 |
| Flyway | schema precisa evoluir | histórico reproduzível | disciplina com migrations | cap. 10 |
| JPA/Hibernate | ligar objetos ao relacional | mapeamento e repositories | abstração não elimina SQL/transações | entities/repositories, cap. 09 |
| DTOs | separar HTTP de persistência | contrato, validação e exposição controlados | conversões adicionais | `dto/`, cap. 05 |
| Testcontainers | integração precisa de PostgreSQL real | fidelidade ao banco | Docker e infraestrutura | cap. 19 |
| Nginx | servir interface e encaminhar API | uma origem e proxy `/api/` | configuração operacional | `frontend/nginx.conf`, cap. 21 |
| Docker/Compose | execução reproduzível de serviços | empacotamento e coordenação | imagens, volumes e configuração | cap. 21 |
| GitHub Actions | verificar mudanças repetidamente | CI e artefatos automatizados | tempo e dependência de runner | cap. 22 |
| Bootstrap admin | instalação nova não tem ADMIN | criação inicial controlada | secret e desativação operacional | cap. 17 |

“A implementação atual favorece” descreve evidência; não afirma uma justificativa
histórica não registrada. Nenhuma decisão é universalmente melhor.

## Comparações importantes

Sessão mantém estado associado no servidor e normalmente usa um cookie para
identificar a sessão do usuário. JWT costuma transportar informações
de autenticação e autorização dentro de um token autocontido.

O projeto implementa sessão, não JWT. Nenhuma das abordagens é
universalmente melhor; cada uma possui consequências diferentes para estado,
segurança e arquitetura. API REST
separa clientes e backend, mas exige tratar contrato e segurança.

DTO não é entity: o primeiro representa entrada/saída, o segundo modelo
persistido. JPA é a especificação de mapeamento; Hibernate é a implementação
usada. Esses custos e detalhes aparecem nos capítulos dedicados.

## Onde estudar no código

- [`SecurityConfig.java`](../../src/main/java/br/com/escala24/config/SecurityConfig.java)
- [`application.properties`](../../src/main/resources/application.properties)
- [`frontend/nginx.conf`](../../frontend/nginx.conf)
- [`pom.xml`](../../pom.xml)
- [Capítulo 17](./17-administrador-inicial.md), [19](./19-testcontainers.md) e [21](./21-docker.md)

## Perguntas de revisão

1. Por que uma decisão técnica deve ser lida junto ao trade-off?
2. Qual problema a API resolve?
3. Qual a consequência de usar sessão em vez de JWT?
4. Por que Flyway e JPA têm responsabilidades diferentes?
5. O que DTO protege em relação à entity?
6. Qual custo Testcontainers introduz?
7. Por que não afirmar que uma arquitetura é “a melhor” sem evidência?

## Resumo

As decisões do Escala 24 formam compromissos: REST, sessão, PostgreSQL,
Flyway, JPA, DTOs, Testcontainers, Nginx, Docker, CI e bootstrap resolvem
problemas concretos, mas introduzem custos operacionais e conceituais.

> **Frase de fixação:** arquitetura é escolher consequências, não apenas listar
> tecnologias.
