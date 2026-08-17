# Visão geral do Escala 24

## Propósito deste capítulo

Este capítulo apresenta o problema que o Escala 24 procura resolver, seus usuários, suas principais responsabilidades e os limites definidos para a versão 1.0.0.

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

## Limites da versão 1.0.0

A versão 1.0.0 foi desenvolvida para uso piloto por uma única corporação ou equipe de bombeiros.

Ela oferece autenticação, diferentes permissões para administradores e bombeiros, cadastro da equipe, gerenciamento de feriados e indisponibilidades, além da geração, consulta, publicação e remanejamento de escalas.

Ainda não fazem parte desta versão funcionalidades como recuperação de senha por e-mail, múltiplas organizações, pagamentos, planos comerciais e infraestrutura gerenciada em produção.

A versão também recebeu atenção especial em segurança, incluindo proteção CSRF, renovação do identificador da sessão, armazenamento de senhas com BCrypt e controle de acesso baseado no perfil do usuário.

## Visão resumida do funcionamento

A aplicação é formada por frontend, backend e banco de dados.

O usuário acessa a interface pelo navegador. O frontend envia as requisições para a API por meio do Nginx. O backend Spring Boot recebe essas requisições, aplica as regras de negócio e utiliza o PostgreSQL para armazenar e consultar os dados.

Os três serviços são executados com Docker Compose, facilitando a instalação e a execução do projeto em máquinas que tenham Docker disponível.

Os testes são executados pelo Maven e utilizam um PostgreSQL temporário criado pelo Testcontainers. No GitHub, o GitHub Actions executa automaticamente esses testes e verifica a qualidade do projeto.