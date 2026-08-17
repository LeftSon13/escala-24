# Changelog

Todas as alterações relevantes deste projeto serão documentadas neste arquivo.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o projeto utiliza [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado

- preparação do repositório para publicação aberta;
- documentação da licença e da política de segurança;
- verificação automatizada de segredos.

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

[Não lançado]: https://github.com/LeftSon13/escala-24/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/LeftSon13/escala-24/releases/tag/v1.0.0
