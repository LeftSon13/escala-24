# Changelog

Todas as alterações relevantes deste projeto serão documentadas neste arquivo.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o projeto utiliza [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [1.2.0] - 2026-08-25

### Adicionado

- exportação da escala mensal publicada em PDF no formato de lista e de calendário;
- exportação da escala mensal publicada em planilha Excel;
- documentação das regras de negócio da geração de escalas e cobertura dos estados de indisponibilidade.

### Corrigido

- dashboard e tabelas operacionais adaptados para janelas menores sem sobreposição ou perda das ações;
- horário de geração dos PDFs calculado no fuso horário de Brasília;
- ações da tela de escalas exibidas somente quando aplicáveis ao período e ao estado consultado;
- controle visual de notificações removido enquanto não existe um fluxo funcional correspondente.

### Alterado

- versões do backend, cliente desktop, pacote de implantação e workflow de imagens alinhadas em `1.2.0`.

## [1.1.1] - 2026-08-25

### Adicionado

- workflow de integração contínua para validar a sintaxe JavaScript, a construção da imagem do frontend e a configuração do Nginx.

### Corrigido

- data exibida no cabeçalho do dashboard passa a ser calculada dinamicamente conforme a data do computador.
- configuração inicial do cliente desktop passa a utilizar a mesma versão do instalador ao selecionar as imagens do backend e frontend.
- formulários modais passam a aceitar o envio pela tecla `Enter`, mantendo os botões de cancelamento apenas para fechar a janela.
- layout do dashboard deixa de aplicar um recuo duplicado e passa a aproveitar corretamente o espaço disponível em janelas não maximizadas.

### Alterado

- versões do backend, cliente desktop, pacote de implantação e workflow de imagens alinhadas em `1.1.1`;
- documentação do cliente desktop atualizada para refletir o fluxo de instalação e inicialização validado.

## [1.1.0] - 2026-08-24

### Adicionado

- cliente desktop para Windows construído com Electron;
- instalador NSIS com nome e ícone personalizados;
- tela amigável para indisponibilidade dos serviços;
- inicialização dos serviços locais pelo cliente desktop;
- configuração automática do primeiro administrador;
- criação segura e local das variáveis de ambiente;
- pacote de implantação baseado em imagens versionadas;
- publicação das imagens do backend e frontend no GitHub Container Registry;
- geração automatizada do instalador pelo GitHub Actions;
- publicação temporária do instalador como artefato da pipeline;
- documentação de instalação, arquitetura e operação do cliente desktop;
- licença, política de segurança e verificação automatizada de segredos.

### Alterado

- distribuição desktop preparada para utilizar imagens prontas do backend e frontend;
- fluxo de primeira execução simplificado para não exigir edição manual do arquivo `.env`;
- inicialização do aplicativo preparada para reutilizar configurações e dados existentes.

## [1.0.0] - 2026-08-17

### Adicionado

- autenticação baseada em sessão para administradores e bombeiros;
- proteção contra CSRF e fixação de sessão;
- troca obrigatória de senha temporária;
- criação segura do administrador inicial por configuração;
- gerenciamento de bombeiros, feriados e indisponibilidades;
- geração, consulta, publicação e remanejamento de escalas mensais;
- interface web integrada às APIs do backend;
- execução completa com Docker Compose, PostgreSQL, Spring Boot e Nginx;
- migrações de banco de dados com Flyway;
- testes unitários e de integração com PostgreSQL temporário via Testcontainers;
- relatório e limites mínimos de cobertura com JaCoCo;
- integração contínua com GitHub Actions;
- guia de instalação, operação e segurança no README.

[Não lançado]: https://github.com/LeftSon13/escala-24/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/LeftSon13/escala-24/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/LeftSon13/escala-24/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/LeftSon13/escala-24/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/LeftSon13/escala-24/releases/tag/v1.0.0
