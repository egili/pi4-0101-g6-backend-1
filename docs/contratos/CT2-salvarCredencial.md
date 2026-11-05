# CT2 - salvarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT2 - `salvarCredencial` |
| **Fronteira** | HTTP/REST |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E4 |
| **Como chamar** | `POST /credentials` com corpo JSON e cabeçalho `Authorization: Bearer <tokenSessao>` |

## Entrada

Corpo JSON da requisição HTTP. O dono da credencial é o usuário identificado pelo token (CT3); o corpo não traz `usuarioId`.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Saída (`201 Created`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `mensagem` | texto | Sim | `Credencial salva com sucesso` |

## Erros

Erros do Servidor voltam no corpo JSON da resposta como `{"erro": "<código>"}`.

| Quando | Status HTTP | O que volta |
|---|---|---|
| Token ausente, inválido ou expirado | 401 | `SESSAO_INVALIDA` |
| `dadosCifrados` vazio | 400 | `DADOS_INVALIDOS` |
| `iv` vazio | 400 | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | 500 | `ERRO_BANCO` |
| Falha de conexão HTTP (sem resposta) | 503 (gerado pelo Cliente) | `ERRO_CONEXAO` (mensagem do Cliente) |
