# CT4 – listarCredenciais

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT4 – `listarCredenciais` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente CLI |
| **Quem responde** | Servidor |
| **Atende** | E5 |
| **Como chamar** | Comando `LIST_CRED` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciais` | lista de objeto (Credencial) | Sim | lista com 3 credenciais |
| `mensagem` | texto | Sim | `3 credenciais encontradas` |

**Objeto Credencial (dentro da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Erros

| Quando | O que volta |
|---|---|
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| `usuarioId` não existe | `USUARIO_NAO_ENCONTRADO` |
| Socket caiu | `ERRO_CONEXAO` |