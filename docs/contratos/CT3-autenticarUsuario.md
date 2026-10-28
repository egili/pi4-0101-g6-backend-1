# CT3 - autenticarUsuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT3 - `autenticarUsuario` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E3 |
| **Como chamar** | Comandos `GET_SALT` (Fase 1) e `AUTH` (Fase 2) no canal Socket |

> **Nota de Arquitetura Zero-Trust:**  
> Para garantir que a senha mestra e a chave de decifragem do cofre nunca cheguem ao servidor, o processo de autenticação é executado em **duas fases coordenadas**. O Cliente primeiro obtém o `salt` do usuário para realizar a derivação de chaves em seu processo local, gerando um hash de autenticação separado da chave de cifragem antes de submeter as credenciais de acesso.

---

## Fase 1: Obtenção do Salt (`GET_SALT`)

O Cliente envia o nome de usuário para que o Servidor retorne o `salt` correspondente cadastrado no banco. O `salt` é necessário para que o Cliente consiga executar localmente a derivação criptográfica (via PBKDF2).

### Entrada (Fase 1)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |

### Saída (Fase 1)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

### Erros (Fase 1)

| Quando | O que volta |
|---|---|
| `username` não existe | `USUARIO_NAO_ENCONTRADO` |
| `username` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| Socket caiu | `ERRO_CONEXAO` |

---

## Fase 2: Validação da Autenticação (`AUTH`)

Com o `salt` em mãos, o Cliente deriva localmente:
1. A **Chave de Cifragem** (mantida unicamente na memória RAM do cliente para cifrar/decifrar o cofre).
2. O **Hash de Autenticação** (`senhaMestraHash`), derivado unidirecionalmente (ex.: HMAC-SHA256 ou hash da chave derivada).

O Cliente então envia o comando `AUTH` contendo o `username` e o `senhaMestraHash`.

### Entrada (Fase 2)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |

### Saída (Fase 2)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `mensagem` | texto | Sim | `Login realizado com sucesso` |

### Erros (Fase 2)

| Quando | O que volta |
|---|---|
| `username` não existe | `USUARIO_NAO_ENCONTRADO` |
| `senhaMestraHash` não bate | `SENHA_INCORRETA` |
| `senhaMestraHash` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| Socket caiu | `ERRO_CONEXAO` |