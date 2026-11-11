# CT1 - cadastrarUsuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT1 - `cadastrarUsuario` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E2 |
| **Como chamar** | `PedidoDeCadastro` (username, senhaMestraHash, salt) |

> **Nota de Arquitetura Zero-Trust:**  
> - O `salt` é gerado aleatoriamente pelo Cliente (via Módulo de Criptografia) e enviado ao Servidor para ser persistido junto ao usuário na tabela `usuario`, permitindo que o Cliente o recupere antes de futuros logins (Fase 1 do CT3).  
> - O campo `senhaMestraHash` é um **hash de autenticação** gerado de forma isolada e unidirecional da chave de cifragem simétrica do cofre. A **chave de cifragem** nunca sai do processo do cliente; o servidor conhece apenas o hash de autenticação, o que garante matematicamente que o servidor nunca consiga decifrar credenciais cadastradas.

## Entrada

Conteúdo do pedido enviado pelo socket.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

## Saída (`201 Created`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `mensagem` | texto | Sim | `Conta criada com sucesso` |

## Erros

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| `username` já existe | `USUARIO_JA_EXISTE` |
| `username` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| `senhaMestraHash` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| `salt` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
