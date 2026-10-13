# Sistema de Cofre de Senhas Criptografadas - Servidor do cofre digital

Sistema cliente-servidor desenvolvido em Java para o gerenciamento, armazenamento seguro e cifragem de credenciais pessoais, garantindo que o servidor central não tenha acesso aos dados em texto claro.

Servidor principal do sistema. Recebe as requisições do Cliente CLI via Socket, gerencia autenticação, faz o CRUD de credenciais e conversa com o banco de dados. Armazena apenas dados já cifrados e nunca acessa a senha mestra nem o conteúdo em texto claro.

## Documentação

- **Visão geral, módulos e convenção de nomes:** [docs/fluxos/visao-geral.md](https://github.com/egili/pi4-0101-g6-frontend/blob/main/docs/fluxos/visao-geral.md)
- **Fluxos por entregável (E1 a E9):** [docs/fluxos/](https://github.com/egili/pi4-0101-g6-frontend/tree/main/docs/fluxos)
- **Contratos deste módulo:** [docs/contratos/](docs/contratos/)

## Contratos definidos aqui

| ID | Nome | Atende |
|---|---|---|
| CT1 | `cadastrarUsuario` | E2 |
| CT2 | `salvarCredencial` | E4 |
| CT3 | `autenticarUsuario` | E3 |
| CT4 | `listarCredenciais` | E5 |
| CT5 | `editarCredencial` | E6 |
| CT6 | `excluirCredencial` | E6 |
| CT7 | `exportarBackup` | E8 |
| CT8 | `importarBackup` | E9 |
| CT9 | Tabela `usuario` | E2, E3 |
| CT10 | Tabela `credencial` | E4, E5, E6, E8, E9 |

## Stack

- Java (JDK 17+)
- Comunicação com o cliente via Socket TCP
- Banco de dados postgres sql (via JDBC)
- Docker + Docker Compose para empacotar o servidor e o banco


## Integrantes do Grupo
* Eliseu Pereira Gili — [@eliseugili](https://github.com/egili)
* Guilherme Heron da Silva — [@GuilhermeHeron](https://github.com/GuilhermeHeron)
* Leonardo Maciel Ferreira de Moraes — [@leomacifs](https://github.com/leomacifs)
* Pietra Façanha Bortolato — [@piborto](https://github.com/piborto)
