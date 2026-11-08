# CT6 - excluirCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT6 - `excluirCredencial` |
| **Fronteira** | HTTP/REST |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E6 |
| **Como chamar** | `DELETE /credentials/{id}` com cabeçalho `Authorization: Bearer <tokenSessao>` |

## Entrada

Sem corpo. O `credencialId` vai no caminho da rota. O Servidor só exclui credenciais que pertencem ao usuário do token.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |
| `credencialId` (no caminho `{id}`) | inteiro | Sim | `10` |

## Saída (`204 No Content`)

Sem corpo. O sucesso é indicado apenas pelo status `204`.

## Erros

Erros do Servidor voltam no corpo JSON da resposta como `{"erro": "<código>"}`.

| Quando | Status HTTP | O que volta |
|---|---|---|
| Token ausente, inválido ou expirado | 401 | `SESSAO_INVALIDA` |
| `credencialId` não existe | 404 | `CREDENCIAL_NAO_ENCONTRADA` |
| Credencial pertence a outro usuário | 403 | `ACESSO_NEGADO` |
| Falha ao excluir no banco | 500 | `ERRO_BANCO` |
| Falha de conexão HTTP (sem resposta) | 503 (gerado pelo Cliente) | `ERRO_CONEXAO` (mensagem do Cliente) |
