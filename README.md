# Sistema de Cofre de Senhas Criptografadas - Servidor do cofre digital

Sistema cliente-servidor desenvolvido em Java para o gerenciamento, armazenamento seguro e cifragem de credenciais pessoais, garantindo que o servidor central não tenha acesso aos dados em texto claro.

Servidor principal do sistema. Recebe os pedidos do Cliente (Back-end 2) por socket TCP, com uma thread por conexão. Gerencia autenticação, faz o CRUD de credenciais e conversa com o banco de dados. Armazena apenas dados já cifrados e nunca acessa a senha mestra nem o conteúdo em texto claro.

## Documentação

- **Visão geral, módulos e convenção de nomes:** [docs/fluxos/VisaoGeral.md](https://github.com/egili/pi4-0101-g6-frontend/blob/main/docs/fluxos/VisaoGeral.md)
- **Fluxos por entregável (E1 a E9):** [docs/fluxos/](https://github.com/egili/pi4-0101-g6-frontend/tree/main/docs/fluxos)
- **Contratos deste módulo:** [docs/contratos/](docs/contratos/)
- **Decisões de arquitetura (ADRs):** no repositório [principal](https://github.com/egili/pi4-2026-2-0101-grupo06/tree/main/docs/adr)
- **Explicação dos logs:** [docs/logs.md](https://github.com/egili/pi4-2026-2-0101-grupo06/blob/main/docs/logs.md)

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

## Como funciona

O Servidor abre um socket TCP na porta **8090** (`servidor.porta`, ou a variável `SERVIDOR_PORTA`). Cada Cliente conectado é atendido por uma thread própria. Os pedidos e respostas são objetos Serializable do pacote `cofre.protocolo`, idênticos no Cliente.

Ele guarda apenas dados cifrados, hashes de autenticação e salts. Nunca recebe senha mestra nem texto claro.

### Comandos no terminal

- `desativar`: avisa todos os Clientes conectados e encerra o processo.
- Qualquer outro texto responde "Comando invalido" no log.

Para usar, é preciso um terminal anexado ao contêiner: `docker attach vault-backend-1` (veja o README principal).

## Configuração

| Variável | Padrão | Para que serve |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` | `localhost`, `5432`, -, -, - | Conexão com o PostgreSQL. |
| `JWT_SECRET` | obrigatória | Chave que assina os tokens. Mínimo de 32 caracteres. Sem ela o Servidor não sobe. |
| `SERVIDOR_PORTA` | `8090` | Porta do socket. |
| `SERVIDOR_COMANDOS` | `true` | Se `false`, não lê o terminal (útil em testes). |
| `LOG_NIVEL` | `INFO` | Nível dos logs. |

Os logs ficam em `logs/servidor.log`, com um arquivo por dia e sem limite de tempo. Veja [docs/logs.md](https://github.com/egili/pi4-2026-2-0101-grupo06/blob/main/docs/logs.md).

O banco é criado pelo script `database/schema.sql` quando o volume está vazio. O Servidor usa `ddl-auto=validate`: confere o schema e não o altera.

## Stack

- Java (JDK 17+)
- Java 17 (JDK 17+), Spring Boot 3.2.5, Maven
- Comunicação com o Cliente via socket TCP (objetos Serializable, pacote `cofre.protocolo`)
- PostgreSQL 15 via JDBC
- Docker + Docker Compose para empacotar o servidor e o banco

## Testes

```bash
mvn verify
```

`verify` roda os testes e gera o relatório JaCoCo em `target/site/jacoco/`. Os testes de socket usam porta livre e não precisam de banco externo (usam H2 em modo PostgreSQL).

A análise estática roda no SonarQube Cloud: https://sonarcloud.io/dashboard?id=egili_pi4-0101-g6-backend-1


## Integrantes do Grupo
* Eliseu Pereira Gili — [@eliseugili](https://github.com/egili)
* Guilherme Heron da Silva — [@GuilhermeHeron](https://github.com/GuilhermeHeron)
* Leonardo Maciel Ferreira de Moraes — [@leomacifs](https://github.com/leomacifs)
* Pietra Façanha Bortolato — [@piborto](https://github.com/piborto)
