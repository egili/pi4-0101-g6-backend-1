# CT3 - autenticarUsuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT3 - `autenticarUsuario` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E3 |
| **Como chamar** | Fase 1: `PedidoDeSalt`. Fase 2: `PedidoDeLogin` |

> **Nota de Arquitetura Zero-Trust:**  
> Para garantir que a senha mestra e a chave de decifragem do cofre nunca cheguem ao servidor, o processo de autenticação é executado em **duas fases coordenadas**. O Cliente primeiro obtém o `salt` do usuário para realizar a derivação de chaves em seu processo local, gerando um hash de autenticação separado da chave de cifragem antes de submeter as credenciais de acesso.

---

## Fase 1: Obtenção do Salt (`PedidoDeSalt`)

O Cliente chama a rota com o nome de usuário no caminho. O Servidor retorna o `salt` correspondente cadastrado no banco. O `salt` é necessário para que o Cliente consiga executar localmente a derivação criptográfica (via PBKDF2).

### Entrada (Fase 1)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto (no caminho da rota) | Sim | `eliseu.gili` |

### Saída (Fase 1) (`200 OK`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

### Erros (Fase 1)

| Quando | O que volta |
|---|---|
| `username` não existe | `USUARIO_NAO_ENCONTRADO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |

---

## Fase 2: Validação da Autenticação (`PedidoDeLogin`)

Com o `salt` em mãos, o Cliente deriva localmente:
1. A **Chave de Cifragem** (mantida unicamente na memória RAM do cliente para cifrar/decifrar o cofre).
2. O **Hash de Autenticação** (`senhaMestraHash`), derivado unidirecionalmente (ex.: HMAC-SHA256 ou hash da chave derivada).

O Cliente então envia um `PedidoDeLogin` com `username` e `hashLogin`. O campo `hashLogin` é o mesmo valor que os demais contratos chamam de `senhaMestraHash`.

### Entrada (Fase 2)

Conteúdo do pedido enviado pelo socket.

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |
| `hashLogin` | texto | Sim | `a1b2c3d4e5f6...` |

### Saída (Fase 2) (`200 OK`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `token` | texto | Sim | `abc123xyz...` |
| `usuarioId` | inteiro | Sim | `1` |

O `token` é o `tokenSessao` usado no cabeçalho `Authorization: Bearer <tokenSessao>` das demais rotas.

### Erros (Fase 2)

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo.

| Quando | O que volta |
|---|---|
| `username` não existe | `USUARIO_NAO_ENCONTRADO` |
| `hashLogin` não bate | `SENHA_INCORRETA` |
| `hashLogin` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |

### Divergências atuais da implementação

Estes pontos ainda diferem do contrato acima (`AuthController`):

- Usuário inexistente e hash errado recebem o mesmo `401` com `erro: "Usuário ou senha inválidos"`, para não revelar quais usernames existem.
- Um `hashLogin` vazio ou ausente hoje cai no `401`, não no `400`.
- O `200` de `PedidoDeLogin` não traz `mensagem`.
- Usuário inexistente recebe um salt falso e estável (o mesmo para o mesmo nome), então a resposta não revela se a conta existe.
