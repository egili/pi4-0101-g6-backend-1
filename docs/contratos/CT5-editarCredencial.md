# CT5 - editarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT5 - `editarCredencial` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E6 |
| **Como chamar** | `PedidoDeAtualizacao` (token, credencialId, servico, usuarioServico, dadosCifrados, iv) |

## Entrada

Conteúdo do pedido enviado pelo socket. O `credencialId` vai dentro do pedido. O Servidor só altera credenciais que pertencem ao usuário do token.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |
| `credencialId` (no caminho `{id}`) | inteiro | Sim | `10` |
| `servico` | texto | Não | `Gmail` |
| `usuarioServico` | texto | Não | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Não | `U2FsdGVkX1+...` |
| `iv` | texto | Não | `3a4b5c6d7e8f...` |

## Saída (`200 OK`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `mensagem` | texto | Sim | `Credencial atualizada com sucesso` |

## Erros

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| Token ausente, inválido ou expirado | `SESSAO_INVALIDA` |
| `credencialId` não existe | `CREDENCIAL_NAO_ENCONTRADA` |
| Credencial pertence a outro usuário | `ACESSO_NEGADO` |
| Nenhum campo para atualizar | `NADA_PARA_ATUALIZAR` |
| Falha ao atualizar no banco | `ERRO_BANCO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
