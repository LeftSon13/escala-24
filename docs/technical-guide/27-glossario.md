# 27 — Glossário

## Objetivo deste capítulo

Este glossário define termos usados no projeto e indica capítulos para estudo
aprofundado.

### API
Interface pela qual programas trocam dados; no Escala 24, é a API HTTP do backend.

### REST
Estilo de organização de APIs sobre recursos e HTTP.

### HTTP
Protocolo usado nas requisições e respostas entre cliente e servidor.

### JSON
Formato textual usado nos corpos de requests e responses.

### fetch
API do navegador usada pelo JavaScript para realizar requisições HTTP. No
Escala 24, `app.js` utiliza `fetch` para se comunicar com os endpoints do
backend. Veja o [Capítulo 12](./12-integracao-com-api.md).

### endpoint
Rota HTTP que oferece uma operação da API.

### status HTTP
Código numérico da resposta HTTP que informa o resultado de uma requisição.
Códigos `2xx` indicam sucesso, enquanto códigos `4xx` representam erros
relacionados à requisição ou às regras aplicadas pelo servidor. No Escala 24,
o frontend interpreta esses resultados para apresentar sucesso ou erro ao
usuário.

### CRUD
Sigla para *Create, Read, Update, Delete*: criar, consultar, atualizar e remover
dados. No Escala 24, várias funcionalidades possuem operações desse tipo, mas
as regras de negócio do sistema não se resumem a CRUD.

### controller
Camada que recebe HTTP e coordena a resposta; veja os testes e o fluxo HTTP nos capítulos [04](./04-backend-em-camadas.md) e [18](./18-testes.md).

### service
Componente que concentra operações e regras da aplicação.

### repository
Componente/interface de acesso a dados, normalmente via JPA.

### entity
Objeto Java mapeado para persistência; não é o mesmo que DTO.

### DTO
Objeto de transferência usado na entrada/saída HTTP, separado da entity.

### DOM
*Document Object Model*. Representação da estrutura da página HTML que pode ser
consultada e modificada pelo JavaScript. No Escala 24, `app.js` manipula o DOM
diretamente para atualizar telas, listas, formulários, dialogs e estados da
interface. Veja o [Capítulo 11](./11-frontend.md).

### DRAFT / PUBLISHED
Estados da escala mensal. `DRAFT` representa uma escala ainda passível de
alterações; `PUBLISHED` representa uma escala publicada, para a qual o sistema
bloqueia remanejamentos. Veja o [Capítulo 16](./16-publicacao-e-remanejamento.md).

### PENDING / APPROVED / REJECTED
Estados de uma solicitação de indisponibilidade. `PENDING` indica que ainda
aguarda revisão, `APPROVED` que foi aprovada e `REJECTED` que foi rejeitada.
Somente indisponibilidades aprovadas bloqueiam a elegibilidade para plantões.
Veja o [Capítulo 14](./14-indisponibilidades.md).

### JPA / Hibernate
JPA define o mapeamento objeto-relacional; Hibernate fornece a implementação usada.

### JDBC
API Java de conexão com bancos relacionais.

### ORM
Mapeamento entre objetos e tabelas relacionais.

### Flyway / migration
Flyway controla alterações versionadas do schema; migration é cada alteração registrada. Veja o [Capítulo 10](./10-flyway.md).

### PostgreSQL
Banco relacional usado pelo sistema e nos testes integrados.

### constraint / foreign key
Restrição de consistência do banco; foreign key relaciona uma coluna a outra tabela.

### transaction
Unidade de operações que deve manter consistência; detalhes nos capítulos 08 e 14.

### Spring Boot
Base que configura e inicializa a aplicação Spring.

### Spring Security
Framework usado para autenticação, autorização, sessão e CSRF.

### sessão / cookie
Sessão mantém estado de autenticação no servidor; cookie transporta identificador/atributos no cliente.

### CSRF
Ataque que tenta induzir requisição autenticada; a aplicação possui proteção correspondente.

### BCrypt
Codificador de senha usado pelo bootstrap e pela autenticação.

### role
Papel de autorização, como `ADMIN` ou `FIREFIGHTER`.

### bootstrap
Procedimento inicial; no projeto, cria o primeiro administrador configurado. Veja o [Capítulo 17](./17-administrador-inicial.md).

### JUnit / AssertJ / Mockito
JUnit executa testes; AssertJ verifica expectativas; Mockito substitui dependências com mocks.

### MockMvc
Ferramenta para exercitar a camada HTTP em memória; veja o [Capítulo 18](./18-testes.md).

### Testcontainers
Biblioteca que inicia dependências em containers; o projeto usa PostgreSQL. Veja o [Capítulo 19](./19-testcontainers.md).

### JaCoCo
Ferramenta que mede execução de linhas e branches e aplica limites no build. Veja o [Capítulo 20](./20-cobertura-com-jacoco.md).

### Docker / imagem / container
Docker executa serviços isolados; imagem é o pacote/modelo; container é sua instância em execução.

### Dockerfile / Docker Compose
Dockerfile descreve a construção de uma imagem; Compose coordena serviços. Veja o [Capítulo 21](./21-docker.md).

### volume

Armazenamento persistente gerenciado pelo Docker e montado em containers. No
Escala 24, o PostgreSQL utiliza volume para preservar dados mesmo quando o
container é recriado.

### health check
Verificação de prontidão/saúde de um serviço, distinta de apenas iniciar o processo.

### Nginx / proxy reverso
Nginx serve o frontend e encaminha `/api/` ao backend; esse encaminhamento é um proxy reverso.

### CI / GitHub Actions
CI é verificação contínua automatizada; GitHub Actions é a plataforma dos workflows do projeto. Veja o [Capítulo 22](./22-integracao-continua.md).

### workflow / job / step / runner
Workflow é a sequência; job é um conjunto de steps; step é uma ação; runner é a máquina que executa o job.

### artefato
Resultado preservado de um job, como relatório JaCoCo ou instalador; não é necessariamente um JAR.

### Git / GitHub
Git versiona localmente; GitHub hospeda repositório e acrescenta PRs, Actions e releases.

### commit / push / branch
Commit registra snapshot; push envia commits; branch é linha de desenvolvimento.

### merge / rebase
Merge une históricos; rebase reaplica commits e reescreve identificadores.

### Pull Request
Proposta de integração e revisão no GitHub.

### tag / release
Tag nomeia um commit; release associa publicação/distribuição a uma versão.

### SemVer
Versionamento `MAJOR.MINOR.PATCH`; veja o [Capítulo 25](./25-versionamento-e-release.md).

### secret / Gitleaks
Secret é dado sensível; Gitleaks procura padrões de credenciais no repositório. Veja o [Capítulo 23](./23-seguranca-do-repositorio.md).

## Termos que costumam ser confundidos

- CRUD × regra de negócio: operações sobre dados × decisões e restrições do domínio;
- sessão × CSRF: identificação da sessão autenticada × proteção contra requisições forjadas;
- DRAFT × PUBLISHED: escala alterável × escala publicada e protegida contra remanejamento;
- entity × DTO: persistência × transporte;
- JPA × Hibernate: especificação × implementação;
- Git × GitHub: ferramenta × plataforma;
- commit × push: registrar × enviar;
- branch × tag: linha móvel × referência nomeada;
- imagem × container: pacote × instância;
- secret × variável de ambiente: dado sensível × mecanismo de configuração;
- mock × Testcontainers: substituto controlado × dependência real em container;
- integração × E2E: colaboração de componentes × jornada completa.

## Perguntas de revisão

1. Qual a diferença entre entity e DTO?
2. JPA e Hibernate são a mesma coisa?
3. Qual a diferença entre Git e GitHub?
4. O que distingue imagem de container?
5. O que diferencia CI de deploy?
6. Por que mock e Testcontainers não são equivalentes?
7. Qual a diferença entre tag e release?

## Resumo

O glossário oferece definições rápidas dos termos de aplicação, persistência,
testes, infraestrutura, Git e segurança usados no guia. Os links apontam para
os capítulos que desenvolvem cada conceito.

> **Frase de fixação:** conhecer o vocabulário torna o código e as decisões
> técnicas mais fáceis de navegar.
