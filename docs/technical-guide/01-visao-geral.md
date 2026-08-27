# Visão geral do Escala 24

## Propósito deste capítulo

Este capítulo apresenta o problema que o Escala 24 procura resolver, seus usuários, suas principais responsabilidades e o estado final da versão 1.2.0.

## Problema atendido

A ideia do projeto surgiu a partir de uma necessidade apresentada por um professor. Um amigo dele, que trabalha como bombeiro, enfrentava dificuldades para organizar as escalas da equipe, pois o processo era realizado manualmente por meio de tabelas.

Esse tipo de organização precisa considerar a disponibilidade dos bombeiros, os períodos de descanso obrigatório, os feriados e outras regras operacionais. O Escala 24 procura centralizar essas informações e auxiliar na geração segura das escalas mensais.

## Usuários do sistema

### Administrador

O administrador representa o responsável pela organização da equipe. Ele pode cadastrar e desativar bombeiros, gerenciar feriados, analisar solicitações de indisponibilidade, gerar a escala mensal, realizar remanejamentos e publicar a versão final da escala.

### Bombeiro

O bombeiro pode consultar as escalas publicadas e registrar períodos em que não estará disponível para trabalhar. Essas solicitações são analisadas posteriormente pelo administrador.

## Principais responsabilidades do sistema

O sistema deve organizar as informações necessárias para a criação das escalas e aplicar as regras de negócio definidas para os plantões.

Entre suas responsabilidades estão:

- manter o cadastro dos bombeiros;
- registrar e analisar indisponibilidades;
- considerar feriados na classificação dos dias;
- impedir mais de uma escala para o mesmo mês e ano;
- distribuir plantões entre bombeiros elegíveis;
- respeitar o descanso obrigatório;
- impedir a escalação de bombeiros inativos ou indisponíveis;
- permitir a revisão do rascunho antes da publicação;
- impedir alterações indevidas em escalas publicadas.

## Evolução e limites da versão atual

A versão 1.0.0 foi o primeiro marco do projeto e foi desenvolvida para uso piloto por uma única corporação ou equipe de bombeiros. Ela oferecia autenticação, diferentes permissões para administradores e bombeiros, cadastro da equipe, gerenciamento de feriados e indisponibilidades, além da geração, consulta, publicação e remanejamento de escalas.

O estado final documentado corresponde à versão `1.2.0`. Além das funcionalidades centrais de operação, o projeto possui um cliente desktop para Windows, construído com Electron, distribuído por meio de instalador e preparado para orientar a configuração inicial da instalação. A escala publicada também pode ser exportada em PDF no formato de lista, PDF no formato de calendário e planilha Excel.

Continuam fora do escopo atual do projeto funcionalidades como recuperação de senha por e-mail, múltiplas organizações, pagamentos, planos comerciais e infraestrutura gerenciada em produção.

O sistema também possui proteção CSRF, renovação do identificador da sessão, armazenamento de senhas com BCrypt e controle de acesso baseado no perfil do usuário.

## Componentes centrais

As funcionalidades do sistema continuam organizadas em três componentes centrais:

- **frontend:** interface web servida pelo Nginx;
- **backend:** API Spring Boot que aplica as regras de negócio;
- **PostgreSQL:** banco de dados que armazena as informações persistentes.

O cliente desktop não substitui esses componentes. Ele acrescenta uma forma de
acessar e iniciar a aplicação localmente.

## Formas de execução e distribuição

O usuário acessa a interface pelo navegador. O frontend envia as requisições para a API por meio do Nginx. O backend Spring Boot recebe essas requisições, aplica as regras de negócio e utiliza o PostgreSQL para armazenar e consultar os dados.

Na execução local tradicional, os três serviços são executados com Docker Compose. Na distribuição desktop, o cliente Electron prepara a configuração inicial, inicializa e coordena esses mesmos serviços locais e abre a aplicação em `http://localhost:3000`. O pacote de implantação utiliza imagens versionadas do backend e do frontend, e o instalador Windows reúne os recursos necessários para esse fluxo.

Mais detalhes sobre a instalação e a operação do cliente estão em [`docs/desktop-client.md`](../../docs/desktop-client.md).

Os testes são executados pelo Maven e utilizam um PostgreSQL temporário criado pelo Testcontainers. No GitHub, o GitHub Actions executa automaticamente esses testes e verifica a qualidade do projeto.
