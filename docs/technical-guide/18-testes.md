# 18 — Testes

## Objetivo deste capítulo

Este capítulo apresenta uma visão consolidada da estratégia de testes do Escala 24,
mostrando o papel de cada nível e como as diferentes ferramentas se complementam.
Os capítulos 19 e 20 aprofundam, respectivamente, Testcontainers e cobertura com
JaCoCo.

> **Pergunta central:** qual pergunta cada nível de teste responde e como esses níveis se complementam no Escala 24?

## Níveis complementares

```text
unitário → comportamento isolado
web/controller → contrato HTTP
integração → colaboração real e persistência
E2E → jornada completa do usuário
```

A pirâmide é modelo didático; quantidade de classes não demonstra proporção ideal nem qualidade. O projeto possui os três primeiros níveis, mas não uma suíte E2E automatizada dedicada.

| Nível | Tecnologia/exemplo | Pode evidenciar | Não garante |
| --- | --- | --- | --- |
| Unitário | JUnit, AssertJ, Mockito | regra e decisão isolada | banco e HTTP |
| Controller | `@WebMvcTest`, MockMvc | binding, JSON, validação e status | PostgreSQL e frontend |
| Integração | `@SpringBootTest`, Testcontainers | Spring, JPA, migrations e banco | jornada visual completa |
| Segurança integrada | MockMvc + contexto real | autenticação/autorização em cenários | todos os fluxos |
| E2E | não identificado | — | permanece lacuna |

Arrange/Act/Assert ou Given/When/Then ajudam a ler preparação, execução e verificação; não são formato formal garantido para todos os testes.

## Ferramentas e limites

JUnit executa cenários; AssertJ verifica expectativas; Mockito controla dependências isoladas; MockMvc exerce HTTP em memória. Testcontainers fornece PostgreSQL temporário e JaCoCo mede execução. Mock não testa banco, integração não é E2E, teste passando não prova ausência de bugs e cobertura não é qualidade.

## Mapa de referência

| Dúvida | Onde estudar |
| --- | --- |
| Frontend e API | [11 — Frontend](./11-frontend.md) e [12 — Integração com API](./12-integracao-com-api.md) |
| Nginx | [13 — Nginx](./13-nginx.md) |
| Regras de domínio | [14 — Indisponibilidades](./14-indisponibilidades.md), [15 — Geração](./15-geracao-de-escalas.md) e [16 — Publicação](./16-publicacao-e-remanejamento.md) |
| Testcontainers | [19 — Testcontainers](./19-testcontainers.md) |
| Cobertura | [20 — Cobertura com JaCoCo](./20-cobertura-com-jacoco.md) |

## Onde estudar no código

- [`src/test/java`](../../src/test/java/)
- [`pom.xml`](../../pom.xml)
- [Capítulo 19](./19-testcontainers.md)
- [Capítulo 20](./20-cobertura-com-jacoco.md)

## Perguntas de revisão

1. O que diferencia unitário, controller e integração?
2. Por que a pirâmide não mede qualidade?
3. O que MockMvc verifica?
4. Por que um mock não prova PostgreSQL?
5. Existe E2E dedicada no projeto?
6. Onde estudar Testcontainers e JaCoCo?

## Resumo

Os testes do Escala 24 formam níveis complementares: unidade, web e integração. Este capítulo orienta a navegação; Testcontainers e cobertura possuem capítulos próprios, e E2E dedicada não foi identificada.

> **Frase de fixação:** cada teste responde uma pergunta; nenhum responde todas.
