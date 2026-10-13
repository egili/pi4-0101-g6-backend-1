# CT1 – cadastrarUsuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT1 – `cadastrarUsuario` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E2 |
| **Como chamar** | Comando `REGISTER` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `mensagem` | texto | Sim | `Conta criada com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `username` já existe | `USUARIO_JA_EXISTE` |
| `username` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| `senhaMestraHash` vazio | `CAMPO_OBRIGATORIO_AUSENTE` |
| Socket caiu | `ERRO_CONEXAO` |
| Falha ao gravar no banco | `ERRO_BANCO` |
