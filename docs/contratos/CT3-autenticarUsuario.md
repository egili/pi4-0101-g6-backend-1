# CT3 - autenticarUsuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT3 - `autenticarUsuario` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E3 |
| **Como chamar** | Comando `AUTH` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `mensagem` | texto | Sim | `Login realizado com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `username` não existe | `USUARIO_NAO_ENCONTRADO` |
| `senhaMestraHash` não bate | `SENHA_INCORRETA` |
| Socket caiu | `ERRO_CONEXAO` |