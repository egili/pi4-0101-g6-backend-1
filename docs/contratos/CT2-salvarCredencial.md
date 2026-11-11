# CT2 - salvarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT2 - `salvarCredencial` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E4 |
| **Como chamar** | `PedidoDeCriacao` (token, servico, usuarioServico, dadosCifrados, iv) |

## Entrada

Conteúdo do pedido enviado pelo socket. O dono da credencial é o usuário identificado pelo token (CT3); o corpo não traz `usuarioId`.

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

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| Token ausente, inválido ou expirado | `SESSAO_INVALIDA` |
| `dadosCifrados` vazio | `DADOS_INVALIDOS` |
| `iv` vazio | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
