# CT8 – importarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT8 – `importarBackup` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente CLI |
| **Quem responde** | Servidor |
| **Atende** | E9 |
| **Como chamar** | Comando `IMPORT_BACKUP` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `credenciais` | lista de objeto (Credencial) | Sim | lista com 3 credenciais |

**Objeto Credencial (dentro da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciaisInseridas` | inteiro | Sim | `3` |
| `mensagem` | texto | Sim | `Backup restaurado com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| Lista de credenciais vazia | `BACKUP_VAZIO` |
| Credencial com campo obrigatório vazio | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Socket caiu | `ERRO_CONEXAO` |