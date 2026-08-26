# 18 — Testes

## Objetivo deste capítulo

Este capítulo consolida a estratégia de testes do Escala 24 e funciona como
mapa para os capítulos especializados.

> **Pergunta central:** como a estratégia completa se organiza e onde estudar
> cada nível em profundidade?

## A pirâmide como modelo

A pirâmide é um modelo didático: unidades isoladas na base, web e integração
acima, E2E no topo. Ela não afirma que a quantidade atual segue proporção ideal
nem mede qualidade.

```text
                 E2E
        controller/web
       integração e persistência
             unitários
```

O projeto possui testes unitários, web/controller e integração; não possui uma
suíte E2E automatizada dedicada.

## Mapa de referência

| Dúvida | Onde estudar |
| --- | --- |
| Estratégia geral? | [Capítulo 12](./12-testes-visao-geral.md) |
| Classe isolada? | [Capítulo 13](./13-testes-unitarios.md) |
| Spring, JPA e PostgreSQL? | [Capítulo 14](./14-testes-de-integracao.md) e [19](./19-testcontainers.md) |
| Contrato HTTP? | [Capítulo 15](./15-testes-de-controller.md) |
| Existe E2E? | [Capítulo 16](./16-testes-end-to-end.md) |
| Cobertura? | [Capítulo 20](./20-cobertura-com-jacoco.md) |

## Níveis e limites

| Nível | Evidência principal | Não garante sozinho |
| --- | --- | --- |
| Unitário | regra ou decisão isolada | banco, HTTP e integração |
| Controller | binding, validação, status e JSON | PostgreSQL e frontend |
| Integração | colaboração, JPA, schema e banco de teste | jornada visual completa |
| E2E | produto pela perspectiva do usuário, quando existente | todos os cenários |

JUnit organiza cenários; AssertJ verifica resultados; Mockito controla
dependências isoladas; MockMvc exercita HTTP em memória; Spring Boot, JPA,
Flyway e Testcontainers suportam integração. Arrange/Act/Assert ou
Given/When/Then são ferramentas mentais, não um formato formal garantido para
todos os métodos.

Mock não testa banco real, integração não é E2E, teste passando não prova
ausência de bugs e cobertura não é qualidade.

## Onde estudar no código

- [`src/test/java`](../../src/test/java/)
- [Capítulo 12](./12-testes-visao-geral.md)
- [Capítulo 13](./13-testes-unitarios.md)
- [Capítulo 14](./14-testes-de-integracao.md)
- [Capítulo 15](./15-testes-de-controller.md)
- [Capítulo 16](./16-testes-end-to-end.md)

## Perguntas de revisão

1. Para que serve a pirâmide como modelo?
2. Qual capítulo explica testes unitários?
3. O que controller testa que unitário não necessariamente testa?
4. Por que integração não equivale a E2E?
5. Onde estudar Testcontainers?
6. Por que quantidade não equivale a qualidade?

## Resumo

Este capítulo é o mapa dos níveis documentados em 12–16. Unitários, web e
integração se complementam; E2E dedicada permanece ausente.

> **Frase de fixação:** uma estratégia de testes é um mapa de perguntas, não
> apenas uma contagem de testes.
