# CT7 – exportarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT7 – `exportarBackup` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente CLI |
| **Quem responde** | Servidor |
| **Atende** | E8 |
| **Como chamar** | Comando `EXPORT_BACKUP` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciais` | lista de objeto (Credencial) | Sim | lista com 3 credenciais |
| `usuario` | objeto (Usuario) | Sim | dados do usuário |
| `mensagem` | texto | Sim | `Backup pronto para exportação` |

**Objeto Credencial (dentro da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

**Objeto Usuario:**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

## Erros

| Quando | O que volta |
|---|---|
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| `usuarioId` não existe | `USUARIO_NAO_ENCONTRADO` |
| Nenhuma credencial para exportar | `BACKUP_VAZIO` |
| Socket caiu | `ERRO_CONEXAO` |