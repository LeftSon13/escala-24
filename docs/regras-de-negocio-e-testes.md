# Regras de negócio e cobertura de testes

Este documento apresenta as regras operacionais centrais do Escala 24 e os
testes automatizados que protegem cada comportamento. Ele complementa o
`README.md`: o README apresenta o produto, enquanto este arquivo registra as
decisões de domínio e sua validação.

## Geração da escala mensal

A escala é criada inicialmente como rascunho e contém um plantão de 24 horas
para cada dia do mês solicitado.

| Regra | Proteção automatizada |
| --- | --- |
| Não gerar duas escalas para o mesmo mês e ano | `shouldRejectDuplicateMonthlySchedule` |
| Exigir bombeiros ativos suficientes para cobrir o mês | `shouldRejectGenerationWithoutActiveFirefighters` e `shouldRejectWhenStaffCannotCoverTheWholeMonth` |
| Criar exatamente um plantão para cada data do mês | `shouldGenerateCompleteBalancedMonthlySchedule` |
| Distribuir o total de plantões com diferença máxima de um | `shouldGenerateCompleteBalancedMonthlySchedule` |
| Compensar, durante o ano, a distribuição de fins de semana e feriados | `shouldCompensateAnnualSpecialDutyHistory` |
| Impedir plantões em dias consecutivos para o mesmo bombeiro | `shouldGenerateCompleteBalancedMonthlySchedule` |
| Considerar também o descanso nas fronteiras entre meses | `shouldRespectRestAcrossMonthBoundaries` |
| Excluir bombeiros inativos da seleção | `shouldGenerateCompleteBalancedMonthlySchedule` |
| Bloquear somente indisponibilidades aprovadas | `shouldGenerateCompleteBalancedMonthlySchedule` e `shouldIgnorePendingAndRejectedUnavailabilities` |
| Tratar sábados, domingos e feriados cadastrados como plantões especiais | `shouldClassifyWeekdaysWeekendsAndHolidays` e `shouldGenerateCompleteBalancedMonthlySchedule` |

### Estados da indisponibilidade durante a geração

- `PENDING`: ainda não foi analisada e não bloqueia a geração.
- `REJECTED`: foi analisada e recusada, portanto não bloqueia a geração.
- `APPROVED`: representa impedimento confirmado e retira o bombeiro da seleção
  nas datas abrangidas.

O teste `shouldIgnorePendingAndRejectedUnavailabilities` usa apenas dois
bombeiros, ambos com solicitações que cobrem o mês inteiro: uma pendente e uma
rejeitada. A escala só pode ser completada se os dois estados forem ignorados.
Esse arranjo torna a regra observável sem repetir o teste de indisponibilidade
aprovada.

## Publicação e remanejamento

| Regra | Proteção automatizada |
| --- | --- |
| Publicar somente uma escala completa | `shouldRejectPublishingIncompleteMonthlySchedule` |
| Impedir nova publicação de uma escala já publicada | `shouldRejectPublishingAlreadyPublishedSchedule` |
| Consultar e publicar uma escala completa | `shouldFindAndPublishCompleteMonthlySchedule` |
| Remanejar somente plantões existentes | `shouldRejectWhenDutyAssignmentDoesNotExist` |
| Impedir alteração de escala publicada | `shouldRejectModificationOfPublishedSchedule` |
| Exigir substituto ativo e disponível | `shouldRejectInactiveReplacement` e `shouldRejectReplacementWithApprovedUnavailability` |
| Preservar descanso obrigatório no remanejamento | `shouldRejectReplacementWithoutMandatoryRest` e `shouldRespectMandatoryRestAcrossMonthBoundary` |
| Preservar data, tipo e período do plantão remanejado | `shouldReassignDutyAndPreserveAssignmentData` |

Quando a aprovação de uma indisponibilidade conflita com um plantão existente,
o sistema exige que esse plantão seja remanejado antes da aprovação. O cenário
é protegido por `shouldRequireReassignmentBeforeApproval`.

## Exportações

Somente escalas publicadas podem ser exportadas. Essa regra fica centralizada
em `MonthlyScheduleExportDataService` e é reutilizada pelos formatos PDF e
Excel.

| Comportamento | Proteção automatizada |
| --- | --- |
| Recusar exportação de rascunho | `shouldRejectDraftSchedule` |
| Gerar PDF de uma escala publicada | `shouldGeneratePdfForPublishedSchedule` |
| Renderizar PDF completo em lista | `shouldRenderCompleteMonthlyScheduleTemplate` |
| Renderizar calendário em uma página paisagem | `shouldRenderCalendarLayoutOnOneLandscapePage` |
| Gerar planilha com datas tipadas, filtro e painel congelado | `shouldGenerateReadableSpreadsheetWithTypedDates` |
| Entregar arquivos pelos endpoints com tipo e nome corretos | testes de exportação de `MonthlyScheduleControllerTest` |

Não é necessário repetir em cada formato o teste de rejeição de rascunho,
porque PDF e Excel dependem do mesmo serviço que aplica essa regra. Um novo
teste só deve ser criado quando proteger um comportamento diferente ou uma
integração que o teste central não alcance.

## Níveis de teste utilizados

- Testes de entidade e DTO validam cálculos simples e contratos de entrada.
- Testes de serviço validam as regras de negócio.
- Testes de integração usam PostgreSQL real via Testcontainers para verificar
  persistência, restrições e transações.
- Testes de controller validam contrato HTTP, serialização e tratamento de
  erros.
- Testes de segurança verificam autenticação e autorização por perfil.
- Testes de renderização abrem os arquivos PDF e Excel gerados para verificar
  conteúdo e estrutura.

## Validação da auditoria

Em 25 de agosto de 2026, após a inclusão do cenário de indisponibilidades
pendente e rejeitada, foi executado:

```powershell
mvn verify
```

Resultado observado:

- 153 testes executados;
- nenhuma falha, erro ou teste ignorado;
- sete migrações Flyway aplicadas em PostgreSQL 17 via Testcontainers;
- verificação de cobertura JaCoCo aprovada;
- build concluído com sucesso.

Testes automatizados protegem regras e contratos, mas não substituem a
validação visual do dashboard, do Electron e dos arquivos exportados antes de
uma demonstração ou release.
