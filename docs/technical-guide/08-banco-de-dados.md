# 08 — Banco de dados

## Objetivo deste capítulo

Este capítulo explica como o modelo relacional atual do Escala 24 organiza os
dados no PostgreSQL. O foco está nas tabelas, chaves, restrições e índices que
formam o estado final criado pelas migrations existentes.

> **Pergunta central:** como tabelas, chaves e restrições trabalham juntas para
> manter os dados do Escala 24 relacionados e coerentes?

As entities são usadas aqui para confirmar nomes e relações do modelo, mas o
aprofundamento de JPA, Hibernate e repositories ficará para o capítulo
09 — JPA e repositories. As migrations serão tratadas como evidência da
estrutura final; o funcionamento do Flyway será aprofundado no capítulo 10.

## Banco relacional em linguagem simples

Um banco de dados relacional organiza informações em **tabelas**. Uma tabela é
formada por:

- **colunas**, que descrevem os atributos armazenados;
- **linhas**, que representam registros individuais;
- **chaves**, que identificam registros e conectam tabelas.

No Escala 24, uma linha de `users` representa uma conta do sistema, enquanto
uma linha de `duty_assignments` representa um plantão atribuído. O banco não é
apenas um conjunto de arquivos: ele também aplica regras declaradas para
impedir determinados estados incoerentes.

## O modelo de dados do Escala 24

O estado final reconstruído a partir das migrations V1 a V7 contém as tabelas
abaixo:

| Tabela | Representa | Relacionamentos principais |
| --- | --- | --- |
| `users` | contas que podem acessar o sistema | é referenciada por `firefighters` e por revisões de indisponibilidade |
| `firefighters` | dados específicos de bombeiros | referencia uma conta em `users` |
| `monthly_schedules` | uma escala mensal | é referenciada por `duty_assignments` |
| `duty_assignments` | plantões de uma escala | referencia uma escala e um bombeiro |
| `unavailabilities` | períodos de indisponibilidade | referencia um bombeiro e, quando revisada, um usuário |
| `holidays` | feriados cadastrados | não possui foreign key para as demais tabelas |

### `users` e `firefighters`

A primeira migration criou `employees`. A V2 renomeou essa tabela para
`users`, acrescentou `role` e criou `firefighters`. No estado final, `users`
contém os dados comuns da conta: `id`, `name`, `email`, `password`, `role`,
`active` e `must_change_password`.

`firefighters` contém dados específicos da atividade de bombeiro: `id`,
`user_id`, `registration` e `phone`.

Essa separação é importante porque uma conta do sistema e um bombeiro não são
conceitos equivalentes. O banco permite uma conta `ADMIN` sem uma linha em
`firefighters`, enquanto os dados específicos do bombeiro ficam em sua tabela
própria. A coluna `firefighters.user_id` é `NOT NULL`, `UNIQUE` e foreign key
para `users(id)`. Assim, cada linha de bombeiro precisa apontar para uma conta
existente e não pode reutilizar a mesma conta em outra linha de bombeiro.

O campo `users.role` aceita somente `ADMIN` ou `FIREFIGHTER`, por meio de uma
constraint `CHECK`. `must_change_password` é obrigatório e tem padrão `TRUE`.
Ele representa no banco a necessidade inicial de troca de senha; a forma como
isso altera as autoridades durante o login foi explicada no capítulo 07.

### Escalas mensais e plantões

`monthly_schedules` possui `schedule_year`, `schedule_month`, `status` e
`created_at`. A combinação de ano e mês é `UNIQUE`, e o mês deve estar entre 1
e 12. O status tem padrão `DRAFT` e aceita apenas `DRAFT` ou `PUBLISHED`.

`duty_assignments` liga cada plantão a uma escala por
`monthly_schedule_id` e a um bombeiro por `firefighter_id`. Também armazena
`duty_date` e `day_type`. A combinação de escala e data do plantão é única,
impedindo dois plantões para a mesma data dentro da mesma escala. `day_type`
aceita somente `WEEKDAY` ou `WEEKEND_OR_HOLIDAY`.

O índice explícito `idx_duty_assignments_firefighter_date` usa
`(firefighter_id, duty_date)`. Ele apoia consultas que partem do bombeiro e
consideram a data; não há evidência de um benchmark que permita afirmar um
ganho específico de desempenho.

### Indisponibilidades

`unavailabilities` relaciona um período ao bombeiro em `firefighter_id` e
registra `type`, `start_date`, `end_date`, `reason`, `status` e `requested_at`.
Quando há revisão, `reviewed_by_user_id` aponta para o usuário responsável e
`reviewed_at` registra quando ela ocorreu.

Os tipos permitidos são `VACATION`, `MEDICAL_LEAVE`,
`PERSONAL_COMMITMENT` e `OTHER`. Os estados são `PENDING`, `APPROVED` e
`REJECTED`. A constraint de período exige `end_date >= start_date`.

A constraint `ck_unavailabilities_review` combina o estado com os dados de
revisão:

- `PENDING` exige revisor e data de revisão nulos;
- `APPROVED` ou `REJECTED` exigem revisor e data de revisão preenchidos.

Esse é um exemplo de integridade protegida pelo próprio banco. Mesmo que uma
requisição ou service tente gravar uma combinação incompatível, o PostgreSQL
rejeita o registro conforme a condição declarada.

Há dois índices explícitos: `idx_unavailabilities_firefighter_period`, sobre
`(firefighter_id, start_date, end_date)`, e
`idx_unavailabilities_status_period`, sobre `(status, start_date, end_date)`.
Eles correspondem a buscas por bombeiro e período ou por estado e período.

### Feriados

`holidays` contém `id`, `holiday_date`, `name` e `created_at`. A data é
obrigatória e única. O nome também é obrigatório, e
`CHECK (BTRIM(name) <> '')` impede que ele seja vazio ou formado apenas por
espaços.

Feriados influenciam a classificação de uma data. O
`DayTypeClassifier` consulta `HolidayRepository`; quando uma data útil está
cadastrada como feriado, ela pode ser classificada como
`WEEKEND_OR_HOLIDAY`, e a geração da escala grava esse valor em
`duty_assignments.day_type`.

Não existe foreign key de `holidays` para `duty_assignments`. Portanto, há uma
relação entre conceitos do domínio implementada pela aplicação, mas não uma
relação estrutural direta entre as tabelas. O banco não precisa apontar um
plantão para uma linha de feriado para que o service use a data do feriado na
classificação.

## Chaves primárias e estrangeiras

Uma **chave primária** (PK, *primary key*) identifica uma linha dentro da
tabela. No modelo atual, cada tabela possui uma coluna `id BIGSERIAL PRIMARY
KEY`, como `users.id`, `firefighters.id` e `monthly_schedules.id`.
No PostgreSQL, BIGSERIAL é uma forma de criar um identificador numérico cujo valor é gerado automaticamente a partir de uma sequência.

Uma **chave estrangeira** (FK, *foreign key*) armazena uma referência à chave
primária de outra tabela. As FKs confirmadas nas migrations são:

| Coluna | Referencia |
| --- | --- |
| `firefighters.user_id` | `users.id` |
| `duty_assignments.monthly_schedule_id` | `monthly_schedules.id` |
| `duty_assignments.firefighter_id` | `firefighters.id` |
| `unavailabilities.firefighter_id` | `firefighters.id` |
| `unavailabilities.reviewed_by_user_id` | `users.id` |

Se uma aplicação armazenasse apenas números sem foreign keys, poderia existir
um `firefighter_id` apontando para um bombeiro inexistente. Com a FK, o
PostgreSQL verifica a existência do registro referenciado antes de aceitar a
relação.

As FKs não substituem toda a lógica do sistema. Elas protegem referências
estruturais; não sabem, por exemplo, se um bombeiro está indisponível para uma
data ou se uma escala pode ser publicada naquele momento.

## Como as tabelas se relacionam

```mermaid
erDiagram
    users ||--o| firefighters : "possui dados de bombeiro"
    users ||--o{ unavailabilities : "revisa"
    firefighters ||--o{ duty_assignments : "recebe"
    firefighters ||--o{ unavailabilities : "declara"
    monthly_schedules ||--o{ duty_assignments : "contém"

    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR role
        BOOLEAN must_change_password
    }
    firefighters {
        BIGINT id PK
        BIGINT user_id FK UK
        VARCHAR registration UK
    }
    monthly_schedules {
        BIGINT id PK
        INTEGER schedule_year
        INTEGER schedule_month
        VARCHAR status
    }
    duty_assignments {
        BIGINT id PK
        BIGINT monthly_schedule_id FK
        BIGINT firefighter_id FK
        DATE duty_date
        VARCHAR day_type
    }
    unavailabilities {
        BIGINT id PK
        BIGINT firefighter_id FK
        BIGINT reviewed_by_user_id FK
        VARCHAR status
        DATE start_date
        DATE end_date
    }
    holidays {
        BIGINT id PK
        DATE holiday_date UK
        VARCHAR name
    }
```

`users` e `firefighters` formam uma relação de um para zero ou um do ponto de
vista dos dados: uma conta pode não ser bombeiro, mas `user_id` único impede
que duas linhas de `firefighters` usem a mesma conta. Uma linha de
`unavailabilities` sempre pertence a um bombeiro, enquanto o revisor é
opcional até que o estado deixe de ser `PENDING`.

`monthly_schedules` possui vários `duty_assignments`, e cada plantão pertence a
uma única escala. `holidays` permanece isolada no diagrama porque sua
participação na geração é feita por consulta e regra da aplicação, não por FK.

## Constraints: protegendo a integridade dos dados

Uma **constraint** é uma regra que o banco verifica ao inserir ou alterar um
registro.

| Tipo | Exemplo real | Inconsistência evitada |
| --- | --- | --- |
| `NOT NULL` | `users.email`, `duty_assignments.duty_date` | registro sem um dado obrigatório |
| `UNIQUE` | `users.email`, `(schedule_year, schedule_month)` | duplicidade de conta ou de escala mensal |
| `CHECK` | `schedule_month BETWEEN 1 AND 12` | mês fora do intervalo válido |
| `FOREIGN KEY` | `duty_assignments.firefighter_id` | plantão apontando para bombeiro inexistente |

As constraints também aparecem em combinações. Em `unavailabilities`, a
constraint de revisão não verifica apenas um campo: ela exige coerência entre
`status`, `reviewed_by_user_id` e `reviewed_at`.

### Exemplo real de integridade

Considere estes dois estados:

```text
válido:    status = PENDING,  reviewed_by_user_id = NULL, reviewed_at = NULL
inválido:  status = PENDING,  reviewed_by_user_id = 42,   reviewed_at = agora
```

O segundo estado mistura “ainda pendente” com informações de uma revisão já
realizada. A constraint `ck_unavailabilities_review` rejeita essa combinação.
Ela não decide se a revisão foi justa ou se o usuário tinha permissão; apenas
protege a coerência estrutural entre o estado e seus metadados.

## Índices

Um **índice** é uma estrutura auxiliar que pode ajudar o banco a localizar
linhas por determinadas colunas sem examinar toda a tabela. Ele não é uma
cópia completa da tabela e não melhora automaticamente qualquer operação.

Os índices criados explicitamente pelas migrations são:

| Índice | Colunas | Busca que parece apoiar |
| --- | --- | --- |
| `idx_duty_assignments_firefighter_date` | `firefighter_id, duty_date` | plantões de um bombeiro por data |
| `idx_unavailabilities_firefighter_period` | `firefighter_id, start_date, end_date` | indisponibilidades de um bombeiro em um período |
| `idx_unavailabilities_status_period` | `status, start_date, end_date` | indisponibilidades por estado e período |

As constraints `PRIMARY KEY` e `UNIQUE` também fazem o PostgreSQL manter
estruturas de apoio para garantir unicidade, mas este capítulo destaca os
índices criados explicitamente com `CREATE INDEX`.

Índices ocupam espaço e precisam ser atualizados quando os dados mudam. Por
isso, sua presença representa um apoio a padrões de consulta identificados no
código, não uma garantia universal de desempenho.

## Banco de dados x regras de negócio

O banco é uma camada de proteção, mas não é responsável por compreender todo o
domínio.

Uma constraint como `UNIQUE (schedule_year, schedule_month)` impede que duas
escalas representem o mesmo mês. Já a decisão de não atribuir um bombeiro
indisponível a determinado plantão depende de período, estado da
indisponibilidade, regras de substituição e contexto da operação. Essa análise
é feita pelos services, como a geração e o remanejamento de plantões.

Do mesmo modo, `CHECK (day_type IN (...))` restringe os valores armazenados,
mas não determina sozinho se uma data deve ser classificada como feriado. O
`DayTypeClassifier` consulta os feriados e a aplicação grava o resultado.

Essa separação evita dois erros: esperar que o banco execute regras
contextuais que não foram declaradas nele, ou tratar uma validação de service
como se fosse uma proteção que também existe para qualquer outro cliente do
banco.

## Analogia: o arquivo da corporação

Podemos imaginar cada tabela como um arquivo organizado, cada linha como uma
ficha, a PK como o número único da ficha e a FK como uma referência para outra
ficha. Uma constraint seria uma regra que o arquivista não pode violar.

A analogia termina aí: o PostgreSQL aplica condições formais sobre os dados;
ele não compreende, como uma pessoa, o significado operacional de “bombeiro
indisponível” ou a justiça de uma decisão administrativa.

## Por que o modelo foi feito assim

O código confirma algumas propriedades úteis do modelo atual:

- separar `users` de `firefighters` permite representar contas administrativas
  sem obrigá-las a possuir dados de bombeiro;
- FKs impedem referências para registros inexistentes;
- `UNIQUE` evita duplicidades importantes, como e-mail, matrícula, escala por
  mês e feriado por data;
- `CHECK` mantém enums e intervalos dentro dos valores aceitos;
- índices explícitos apoiam buscas recorrentes por bombeiro, estado e período.

Essas descrições explicam o que a estrutura permite e protege. Não significam
que uma intenção histórica dos autores possa ser deduzida apenas dos nomes das
migrations.

## Erros comuns e cuidados

- Confundir PK, que identifica a própria linha, com FK, que referencia outra
  linha.
- Supor que toda relação entre conceitos do domínio precisa ser uma FK. O
  vínculo entre `holidays` e a classificação de um plantão é feito pela
  aplicação.
- Achar que `UNIQUE` e PK são equivalentes. Uma PK identifica a linha e não
  aceita nulo; uma constraint `UNIQUE` protege a não repetição de outro valor
  ou combinação.
- Acreditar que validar no backend torna constraints desnecessárias. Outros
  caminhos de escrita ainda precisam de integridade no banco.
- Achar que um índice é uma cópia da tabela ou que sempre melhora qualquer
  consulta.
- Confundir uma tabela do PostgreSQL com uma classe Java. A entity ajuda a
  mapear o modelo na aplicação; ela não substitui a leitura das migrations.

## Onde estudar no código

| Assunto | Arquivo |
| --- | --- |
| Estado final do esquema | [`V1__create_employee_table.sql`](../../src/main/resources/db/migration/V1__create_employee_table.sql) a [`V7__create_holidays.sql`](../../src/main/resources/db/migration/V7__create_holidays.sql) |
| Modelo de contas e bombeiros | [`User.java`](../../src/main/java/br/com/escala24/entity/User.java), [`Firefighter.java`](../../src/main/java/br/com/escala24/entity/Firefighter.java) |
| Escalas e plantões | [`MonthlySchedule.java`](../../src/main/java/br/com/escala24/entity/MonthlySchedule.java), [`DutyAssignment.java`](../../src/main/java/br/com/escala24/entity/DutyAssignment.java) |
| Indisponibilidades | [`Unavailability.java`](../../src/main/java/br/com/escala24/entity/Unavailability.java) e `V6__create_unavailabilities.sql` |
| Feriados e classificação | [`Holiday.java`](../../src/main/java/br/com/escala24/entity/Holiday.java), [`DayTypeClassifier.java`](../../src/main/java/br/com/escala24/service/DayTypeClassifier.java) |
| Consultas relacionadas | [`DutyAssignmentRepository.java`](../../src/main/java/br/com/escala24/repository/DutyAssignmentRepository.java), [`UnavailabilityRepository.java`](../../src/main/java/br/com/escala24/repository/UnavailabilityRepository.java), [`HolidayRepository.java`](../../src/main/java/br/com/escala24/repository/HolidayRepository.java) |

## Perguntas de revisão

1. Por que `firefighters.user_id` é uma foreign key e também é `UNIQUE`?
2. O que uma foreign key impede em `duty_assignments`?
3. Por que a combinação de ano e mês de `monthly_schedules` precisa ser única?
4. Como `ck_unavailabilities_review` evita um estado incoerente?
5. Por que `holidays` influencia a geração de escalas sem possuir FK para
   `duty_assignments`?
6. Qual é a diferença entre uma constraint do banco e uma regra avaliada por
   um service?
7. Como os índices de período podem ajudar determinadas consultas e por que não podemos afirmar um ganho específico de desempenho sem medições?
    
   

## Resumo

O modelo relacional do Escala 24 separa contas, bombeiros, escalas, plantões,
indisponibilidades e feriados em tabelas próprias. PKs identificam registros;
FKs preservam referências; `NOT NULL`, `UNIQUE` e `CHECK` impedem estados
incoerentes; e índices explícitos apoiam consultas recorrentes.

O banco protege a integridade estrutural, mas services continuam responsáveis
por regras que dependem de contexto, como classificar datas e evitar atribuições
incompatíveis com indisponibilidades. Nem toda relação do domínio precisa ser
uma FK.

Uma frase útil para lembrar é:

> **A tabela organiza o dado, a constraint protege sua coerência e o service aplica o contexto da regra de negócio.**
