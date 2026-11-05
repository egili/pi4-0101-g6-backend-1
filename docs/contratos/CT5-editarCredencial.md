# CT5 - editarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT5 - `editarCredencial` |
| **Fronteira** | HTTP/REST |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E6 |
| **Como chamar** | `PUT /credentials/{id}` com corpo JSON e cabeçalho `Authorization: Bearer <tokenSessao>` |

## Entrada

Corpo JSON da requisição HTTP. O `credencialId` vai no caminho da rota. O Servidor só altera credenciais que pertencem ao usuário do token.

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

Erros do Servidor voltam no corpo JSON da resposta como `{"erro": "<código>"}`.

| Quando | Status HTTP | O que volta |
|---|---|---|
| Token ausente, inválido ou expirado | 401 | `SESSAO_INVALIDA` |
| `credencialId` não existe | 404 | `CREDENCIAL_NAO_ENCONTRADA` |
| Credencial pertence a outro usuário | 403 | `ACESSO_NEGADO` |
| Nenhum campo para atualizar | 400 | `NADA_PARA_ATUALIZAR` |
| Falha ao atualizar no banco | 500 | `ERRO_BANCO` |
| Falha de conexão HTTP (sem resposta) | 503 (gerado pelo Cliente) | `ERRO_CONEXAO` (mensagem do Cliente) |
