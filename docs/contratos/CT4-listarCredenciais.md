# CT4 - listarCredenciais

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT4 - `listarCredenciais` |
| **Fronteira** | HTTP/REST |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E5 |
| **Como chamar** | `GET /credentials` com cabeçalho `Authorization: Bearer <tokenSessao>` |

## Entrada

Sem corpo. O usuário é identificado pelo token (CT3).

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |

## Saída (`200 OK`)

Corpo JSON: lista de objetos Credencial (lista vazia se o usuário não tiver credenciais).

**Objeto Credencial (cada item da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Erros

Erros do Servidor voltam no corpo JSON da resposta como `{"erro": "<código>"}`.

| Quando | Status HTTP | O que volta |
|---|---|---|
| Token ausente, inválido ou expirado | 401 | `SESSAO_INVALIDA` |
| Falha de conexão HTTP (sem resposta) | 503 (gerado pelo Cliente) | `ERRO_CONEXAO` (mensagem do Cliente) |
