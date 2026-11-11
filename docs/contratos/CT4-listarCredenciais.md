# CT4 - listarCredenciais

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT4 - `listarCredenciais` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E5 |
| **Como chamar** | `PedidoDeListagem` (token) |

## Entrada

Sem corpo. O usuário é identificado pelo token (CT3).

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |

## Saída (`200 OK`)

Lista de objetos Credencial, enviada na `Resposta` (lista vazia se o usuário não tiver credenciais).

**Objeto Credencial (cada item da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Erros

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| Token ausente, inválido ou expirado | `SESSAO_INVALIDA` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
