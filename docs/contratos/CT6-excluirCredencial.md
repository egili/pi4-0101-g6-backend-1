# CT6 - excluirCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT6 - `excluirCredencial` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E6 |
| **Como chamar** | `PedidoDeExclusao` (token, credencialId) |

## Entrada

Sem corpo. O `credencialId` vai dentro do pedido. O Servidor só exclui credenciais que pertencem ao usuário do token.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |
| `credencialId` (no caminho `{id}`) | inteiro | Sim | `10` |

## Saída (`204 No Content`)

Sem corpo. O sucesso é indicado apenas pelo status `204`.

## Erros

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| Token ausente, inválido ou expirado | `SESSAO_INVALIDA` |
| `credencialId` não existe | `CREDENCIAL_NAO_ENCONTRADA` |
| Credencial pertence a outro usuário | `ACESSO_NEGADO` |
| Falha ao excluir no banco | `ERRO_BANCO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
