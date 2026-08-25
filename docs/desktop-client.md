# Cliente desktop do Escala 24

## Visão geral

O cliente desktop do Escala 24 é uma aplicação para Windows construída com Electron.

Ele oferece uma janela nativa para acessar a interface web do sistema, permitindo que o Escala 24 seja aberto pelo menu Iniciar ou por um atalho na área de trabalho.

O Electron funciona como uma camada de apresentação e também coordena a configuração e a inicialização dos serviços locais. Os demais componentes são executados pelo Docker Compose:

- frontend servido pelo Nginx;
- backend desenvolvido com Spring Boot;
- banco de dados PostgreSQL.

## Arquitetura atual

```text
Aplicativo Electron
        |
        | | acessa http://localhost:3000
        v
Frontend Nginx
        |
        | encaminha /api
        v
Backend Spring Boot
        |
        | JDBC
        v
PostgreSQL
```

O Electron não contém o backend nem o banco de dados. Ele funciona como uma janela especializada que apresenta a aplicação web instalada localmente.

Uma analogia simples é pensar no Electron como a recepção de um prédio:

- o Electron é a recepção pela qual o usuário entra;
- o Nginx direciona cada solicitação;
- o backend executa as regras do sistema;
- o PostgreSQL guarda os dados.

## Objetivo do cliente desktop

O cliente desktop foi criado para oferecer uma instalação orientada do Escala 24 em computadores Windows. Ele permite:

- funcionar dentro de uma janela nativa do Windows;
- manter a autenticação e a navegação existentes;
- detectar quando o servidor local está indisponível;
- permitir uma nova tentativa de conexão;
- ser distribuído por meio de um instalador `.exe`;
- utilizar nome e ícone personalizados no Windows;
- configurar o primeiro administrador;
- gerar as configurações privadas da instalação;
- iniciar os serviços por meio do Docker Compose;
- baixar imagens versionadas do GitHub Container Registry;
- preservar os dados entre reinicializações.

O cliente foi validado em uma máquina virtual Windows limpa, sem o código-fonte do projeto, utilizando apenas o instalador publicado, o Docker Desktop e acesso à internet na primeira execução.

## Requisitos

Para desenvolver e gerar o cliente desktop, são necessários:

- Windows;
- Node.js e npm;
- Docker Desktop;
- serviços do Escala 24 configurados pelo Docker Compose.

O Docker Desktop precisa estar instalado e com o mecanismo Docker disponível. A partir disso, o Electron prepara e inicia os contêineres necessários durante a configuração inicial e nas próximas execuções.

## Estrutura do cliente

```text
desktop/
├── assets/
│   ├── icon-source.png
│   └── icon.ico
├── scripts/
│   └── create-icon.cjs
├── main.js
├── offline.html
├── offline.js
├── preload.js
├── setup.html
├── setup.css
├── setup.js
├── deployment/
│   ├── docker-compose.yml
│   └── .env.example
├── package.json
└── package-lock.json
```

Responsabilidade dos principais arquivos:

- `main.js`: inicia o Electron, cria a janela e verifica a disponibilidade do servidor;
- `preload.js`: disponibiliza uma comunicação controlada entre a página local e o processo principal;
- `offline.html`: apresenta uma mensagem amigável quando o servidor não responde;
- `offline.js`: trata o botão de nova tentativa;
- `setup.html`, `setup.css` e `setup.js`: implementam a configuração guiada da primeira execução;
- `deployment/`: contém os arquivos usados para iniciar os serviços instalados;
- `package.json`: define dependências, comandos e configurações do instalador;
- `assets/icon.ico`: fornece o ícone usado pelo aplicativo e pelo instalador;
- `scripts/create-icon.cjs`: gera o ícone do Windows a partir da imagem de origem.

## Execução em desenvolvimento

Primeiro, inicie os serviços na raiz do projeto:

```powershell
docker compose up -d
```

Depois, entre no diretório do cliente e instale as dependências:

```powershell
Set-Location "desktop"
npm.cmd install
```

Inicie o Electron:

```powershell
npm.cmd start
```

Durante o desenvolvimento, o Electron acessa:

```text
http://localhost:3000
```

Se o endereço não estiver disponível, a tela local de indisponibilidade será apresentada.

## Geração do instalador

O projeto utiliza o Electron Builder para transformar o cliente em um instalador do Windows.

Entre no diretório do cliente:

```powershell
Set-Location "desktop"
```

Gere o instalador:

```powershell
npm.cmd run dist
```

O arquivo será criado em:

```text
desktop/out/Escala 24-Setup-<versão>.exe
```

A pasta `out` contém arquivos gerados automaticamente e não deve ser enviada ao Git.

## Instalação no Windows

Execute o arquivo `Escala 24-Setup-<versão>.exe` e siga as etapas apresentadas pelo instalador.

Durante a instalação, é possível:

- instalar o programa para o usuário atual;
- escolher o diretório de instalação;
- criar um atalho na área de trabalho;
- criar uma entrada no menu Iniciar;
- abrir o Escala 24 ao finalizar.

O computador precisa ter o Docker Desktop instalado e com o mecanismo do Docker em execução. O usuário não precisa possuir o código-fonte nem construir manualmente as imagens do backend e do frontend.

O instalador inclui:

- o cliente Electron;
- o arquivo de implantação do Docker Compose;
- o modelo das variáveis de ambiente;
- a interface de configuração do primeiro acesso.

As imagens prontas do backend e do frontend são obtidas do GitHub Container Registry.

## Configuração do primeiro acesso

Quando nenhuma instalação local estiver configurada, o cliente apresenta uma tela para criação do primeiro administrador.

O usuário informa:

- nome do administrador;
- endereço de e-mail;
- senha temporária;
- confirmação da senha temporária.

A senha temporária deve possuir pelo menos 12 caracteres. Após o primeiro login, sua alteração é obrigatória.

Depois da confirmação, o Electron:

1. valida os dados informados;
2. cria uma pasta privada de implantação;
3. copia o arquivo `docker-compose.yml`;
4. gera uma senha aleatória para o PostgreSQL;
5. cria localmente o arquivo `.env`;
6. registra os dados do administrador inicial;
7. inicia PostgreSQL, backend e frontend;
8. aguarda a aplicação ficar disponível;
9. abre a tela de login.

```text
Primeira execução
       |
       v
Configuração existente?
    /          \
  sim          não
   |            |
   |            v
   |     Formulário inicial
   |            |
   |            v
   |     Criação do ambiente
   |            |
   +------------+
          |
          v
   Docker Compose
          |
          v
     Tela de login
```

Uma analogia simples é pensar nessa etapa como a preparação de uma casa nova: o instalador entrega a estrutura, a tela inicial define o primeiro responsável e o Docker organiza os serviços necessários para o sistema funcionar.

As configurações são armazenadas na área privada do aplicativo no Windows e reutilizadas nas próximas execuções.

## Funcionamento da inicialização

Ao abrir o aplicativo, o Electron procura a configuração local já registrada.

Se a configuração existir, ele utiliza o Docker Compose para iniciar ou reutilizar:

- PostgreSQL;
- backend Spring Boot;
- frontend Nginx.

Em seguida, aguarda a aplicação responder em:

```text
http://localhost:3000
```

Quando o servidor fica disponível, a tela de login é carregada. Se houver uma falha, o cliente apresenta uma tela amigável de indisponibilidade, permitindo tentar novamente.

```text
Electron iniciado
       |
       v
Iniciar serviços locais
       |
       v
Servidor responde?
    /       \
  sim       não
   |         |
   v         v
Escala 24   Tela de indisponibilidade
             |
             v
       Tentar novamente
```

Essa abordagem evita mostrar erros técnicos do navegador e orienta o usuário quando o Docker Desktop ou algum serviço ainda não estiver disponível.

## Dados e banco de dados

O Electron não possui um banco de dados próprio.

Os usuários, bombeiros, feriados, indisponibilidades e escalas permanecem armazenados no PostgreSQL executado pelo Docker Compose.

Por isso:

- reinstalar apenas o cliente Electron não apaga os dados;
- interromper os contêineres não apaga os dados;
- executar `docker compose down` preserva o volume do banco;
- executar `docker compose down --volumes` remove o banco local e seus dados.

Em outro computador, uma instalação com um novo volume do PostgreSQL começa com um banco vazio. Nesse caso, será necessário configurar o administrador inicial e cadastrar posteriormente os bombeiros e demais informações.

## Limitações atuais

A distribuição desktop já prepara e inicia o ambiente local, mas ainda possui algumas dependências:

- o Docker Desktop precisa estar instalado e com o mecanismo Docker disponível;
- a primeira execução precisa de internet para baixar as imagens dos serviços;
- o endereço da aplicação permanece fixo em `http://localhost:3000`;
- a porta `3000` precisa estar disponível no computador;
- o instalador é destinado atualmente ao Windows de 64 bits;
- o instalador ainda não possui assinatura digital;
- as atualizações do cliente precisam ser instaladas manualmente;
- backup e restauração ainda não são realizados pela interface.

O instalador inclui o cliente Electron e os arquivos de implantação, enquanto as imagens prontas do frontend e do backend são baixadas do GitHub Container Registry.

Uma analogia é pensar no instalador como uma equipe de montagem: ele leva as instruções, prepara o ambiente e organiza os componentes. O Docker Desktop fornece a estrutura necessária para que esses componentes sejam executados.

## Solução de problemas

### O aplicativo informa que o servidor está indisponível

Confirme se o Docker Desktop está aberto e se o mecanismo Docker terminou de inicializar.

Depois:

1. retorne ao Escala 24;
2. clique em **Iniciar serviços** ou **Tentar novamente**;
3. aguarde o download e a inicialização dos componentes.

Na primeira execução, o processo pode demorar mais porque as imagens do frontend, backend e PostgreSQL precisam ser baixadas.

### A configuração inicial não avança

Verifique:

- se o Docker Desktop está aberto;
- se há conexão com a internet;
- se a porta `3000` está livre;
- se a senha temporária possui pelo menos 12 caracteres;
- se os campos de nome, e-mail e confirmação da senha estão preenchidos corretamente.

O aplicativo preserva a configuração criada. Caso a inicialização falhe temporariamente, uma nova tentativa reutiliza o ambiente existente.

### Verificar os serviços pelo Docker Desktop

Abra a seção **Containers** do Docker Desktop e localize o projeto do Escala 24.

Os três serviços esperados são:

- PostgreSQL;
- backend;
- frontend.

O PostgreSQL e o backend devem alcançar o estado saudável, enquanto o frontend deve permanecer em execução.

### Verificar se a porta está ocupada

No PowerShell, execute:

```powershell
docker ps `
    --filter "publish=3000" `
    --format "table {{.Names}}\t{{.Ports}}"
```

Se outro contêiner estiver utilizando a porta `3000`, ele precisará ser interrompido antes da inicialização do Escala 24.

### Diagnóstico durante o desenvolvimento

Ao trabalhar com o código-fonte, entre na pasta do cliente:

```powershell
Set-Location "desktop"
```

Para iniciar o Electron:

```powershell
npm.cmd start
```

Se o npm informar que não encontrou `package.json`, confirme se o terminal está dentro da pasta `desktop`.

## Segurança

O Electron utiliza uma configuração restritiva:

- integração direta com Node.js desativada na interface;
- isolamento de contexto ativado;
- comunicação limitada por meio do arquivo `preload.js`;
- abertura da aplicação somente pelo endereço local configurado.

O instalador atual não possui certificado de assinatura de código. Por isso, o Windows pode exibir um aviso de segurança ao executá-lo em outro computador.

## Evoluções futuras

As próximas versões poderão incluir:

- inicialização ou orientação automática do Docker Desktop;
- configuração de um endereço de servidor remoto;
- conexão com um servidor central da corporação;
- escolha automática de uma porta disponível;
- exibição detalhada do estado de cada serviço;
- atualização automática do cliente;
- assinatura digital do instalador;
- backup e restauração pela interface;
- recuperação assistida de uma instalação com falha;
- geração automatizada do instalador pelo GitHub Actions;
- testes automatizados específicos para o processo Electron.

A distribuição atual já oferece instalação, configuração do primeiro administrador, preparação do ambiente local e inicialização dos serviços. As próximas etapas serão voltadas à manutenção, recuperação, atualização e implantação em ambientes reais.
