# 19 — Testcontainers

## Objetivo deste capítulo

Este capítulo aprofunda a infraestrutura que fornece PostgreSQL real aos testes
de integração.

> **Pergunta central:** por que o Escala 24 usa PostgreSQL temporário e como ele
> é conectado ao Spring Boot?

## Configuração real

`PostgreSqlTestContainerConfiguration` é uma `@TestConfiguration` importada
por `@IntegrationTest`. Ela declara um `@Bean` `PostgreSQLContainer` com a
imagem `postgres:17-alpine` e `@ServiceConnection`.

`IntegrationTest.java` combina essa importação com `@SpringBootTest`.
`@ServiceConnection` permite que o Spring Boot obtenha os dados de conexão do
container e configure o `DataSource` utilizado no contexto de teste. O Flyway
utiliza essa conexão configurada para aplicar as migrations, sem que os testes
precisem definir manualmente URL, usuário ou porta.

```mermaid
flowchart LR
    IT[@IntegrationTest] --> C[PostgreSQLContainer postgres:17-alpine]
    C --> SC[@ServiceConnection]
    SC --> D[DataSource e Flyway]
    D --> T[Teste integrado]
```

## Por que um PostgreSQL real

O banco temporário não é mock nem banco em memória. Ele permite exercitar SQL,
tipos, relacionamentos, constraints, migrations e repositories compatíveis com
PostgreSQL, revelando problemas que uma resposta simulada pode ocultar. O
trade-off é exigir Docker, imagem disponível e inicialização de infraestrutura.

O código não permite afirmar um container novo por método ou um único container
para toda a suíte: o bean depende do ciclo de vida do contexto Spring e do
Testcontainers. O container também não representa rede, backup ou capacidade
de produção.

## Flyway, JPA e falhas de infraestrutura

Com Flyway habilitado e `ddl-auto=validate`, o fluxo esperado do contexto é:

```text
PostgreSQL temporário → DataSource → Flyway → migrations V1–V7
→ Hibernate validate → repositories/services → teste
```

Docker indisponível interrompe a criação do container e pode impedir o contexto
antes do cenário funcional. Isso é falha de infraestrutura, não evidência de
falha na regra de negócio.

## Onde estudar no código

- [`IntegrationTest.java`](../../src/test/java/br/com/escala24/IntegrationTest.java)
- [`PostgreSqlTestContainerConfiguration.java`](../../src/test/java/br/com/escala24/config/PostgreSqlTestContainerConfiguration.java)
- [`pom.xml`](../../pom.xml)
- [`MonthlySchedulePersistenceIntegrationTest.java`](../../src/test/java/br/com/escala24/entity/MonthlySchedulePersistenceIntegrationTest.java)
- [`18 — Testes`](./18-testes.md)

## Perguntas de revisão

1. Por que Testcontainers não é um mock?
2. Qual imagem o projeto usa?
3. Qual o papel de `@ServiceConnection`?
4. Por que Docker é necessário?
5. O que o PostgreSQL real ajuda a verificar?
6. Por que não afirmar o número de containers sem evidência?

## Resumo

Testcontainers fornece PostgreSQL real baseado em `postgres:17-alpine`; a
conexão é integrada ao Spring Boot por `@ServiceConnection`. O ganho é
fidelidade ao banco, e o custo é a dependência de Docker.

> **Frase de fixação:** o container monta um banco real temporário, não uma
> réplica completa de produção.
