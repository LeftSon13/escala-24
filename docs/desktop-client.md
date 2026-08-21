# Cliente desktop do Escala 24

## Visão geral

O cliente desktop do Escala 24 é uma aplicação para Windows construída com Electron.

Ele oferece uma janela nativa para acessar a interface web do sistema, permitindo que o Escala 24 seja aberto pelo menu Iniciar ou por um atalho na área de trabalho.

Nesta primeira versão, o Electron funciona como uma camada de apresentação. Os demais componentes continuam executados separadamente pelo Docker Compose:

- frontend servido pelo Nginx;
- backend desenvolvido com Spring Boot;
- banco de dados PostgreSQL.

## Arquitetura atual

```text
Aplicativo Electron
        |
        | acessa http://localhost:3000
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

## Objetivo da prova de conceito

Esta prova de conceito foi criada para validar se o Escala 24 poderia:

- funcionar dentro de uma janela nativa do Windows;
- manter a autenticação e a navegação existentes;
- detectar quando o servidor local está indisponível;
- permitir uma nova tentativa de conexão;
- ser distribuído por meio de um instalador `.exe`;
- utilizar nome e ícone personalizados no Windows.

O resultado demonstra que a aplicação web atual pode receber uma experiência desktop sem exigir a reescrita do frontend ou do backend.

## Requisitos

Para desenvolver e gerar o cliente desktop, são necessários:

- Windows;
- Node.js e npm;
- Docker Desktop;
- serviços do Escala 24 configurados pelo Docker Compose.

O Electron não inicia os serviços automaticamente nesta prova de conceito. O Docker Desktop e os contêineres precisam estar em execução antes da abertura completa do sistema.

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
├── package.json
└── package-lock.json
```

Responsabilidade dos principais arquivos:

- `main.js`: inicia o Electron, cria a janela e verifica a disponibilidade do servidor;
- `preload.js`: disponibiliza uma comunicação controlada entre a página local e o processo principal;
- `offline.html`: apresenta uma mensagem amigável quando o servidor não responde;
- `offline.js`: trata o botão de nova tentativa;
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
desktop/out/Escala 24-Setup-1.0.0.exe
```

A pasta `out` contém arquivos gerados automaticamente e não deve ser enviada ao Git.

## Instalação no Windows

Execute o arquivo `Escala 24-Setup-1.0.0.exe` e siga as etapas apresentadas pelo instalador.

Durante a instalação, é possível:

- instalar o programa apenas para o usuário atual;
- escolher o diretório de instalação;
- criar um atalho na área de trabalho;
- criar uma entrada no menu Iniciar;
- abrir o Escala 24 ao finalizar.

O instalador configura somente o cliente Electron. Nesta prova de conceito, Docker Desktop e os serviços do Escala 24 ainda precisam ser preparados separadamente.

## Funcionamento da verificação de disponibilidade

Ao iniciar, o cliente tenta acessar o endereço local da aplicação.

```text
Electron iniciado
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

Se o servidor estiver disponível, a interface de login é carregada. Caso contrário, o Electron abre uma página local com orientações e um botão para realizar outra tentativa.

Essa abordagem evita apresentar ao usuário uma mensagem técnica do navegador quando os serviços ainda não estiverem prontos.

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

Esta versão é uma prova de conceito. Atualmente:

- o Electron depende dos serviços executados pelo Docker Compose;
- o aplicativo não inicia automaticamente os contêineres;
- o endereço da aplicação está configurado como `http://localhost:3000`;
- o instalador ainda não possui assinatura digital;
- atualizações precisam ser instaladas manualmente;
- o instalador distribui somente o cliente Electron, não o sistema completo.

Uma analogia é pensar no instalador como um controle remoto: ele facilita o acesso ao sistema, mas os equipamentos controlados — frontend, backend e banco — precisam estar ligados separadamente.

## Solução de problemas

### O aplicativo informa que o servidor está indisponível

Na raiz do projeto, verifique os serviços:

```powershell
docker compose ps
```

Se necessário, inicie-os:

```powershell
docker compose up -d
```

Depois, clique em **Tentar novamente** no aplicativo.

### Verificar os registros dos serviços

```powershell
docker compose logs --tail 100
```

Para consultar somente o backend:

```powershell
docker compose logs --tail 100 backend
```

### O comando npm não encontra o package.json

Entre primeiro na pasta do cliente:

```powershell
Set-Location "desktop"
```

Depois execute o comando desejado.

## Segurança

O Electron utiliza uma configuração restritiva:

- integração direta com Node.js desativada na interface;
- isolamento de contexto ativado;
- comunicação limitada por meio do arquivo `preload.js`;
- abertura da aplicação somente pelo endereço local configurado.

O instalador atual não possui certificado de assinatura de código. Por isso, o Windows pode exibir um aviso de segurança ao executá-lo em outro computador.

## Evoluções futuras

As próximas versões poderão incluir:

- inicialização automática dos serviços locais;
- tela de configuração do endereço do servidor;
- conexão com um servidor central da corporação;
- atualização automática do cliente;
- assinatura digital do instalador;
- criação de backup e restauração pela interface;
- geração automatizada do instalador pelo GitHub Actions.

A prova de conceito atual valida a experiência desktop. As próximas etapas transformarão essa camada de apresentação em uma distribuição mais autônoma e adequada para usuários finais.
