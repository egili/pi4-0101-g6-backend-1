# CT9 – Tabela usuario

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT9 – Tabela `usuario` |
| **Fronteira** | Banco |
| **Quem chama** | Servidor |
| **Quem responde** | Banco de Dados |
| **Atende** | E2, E3 |
| **Como chamar** | Tabela `usuario` no SQLite |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `id` | inteiro | Sim (auto) | `1` |
| `username` | texto | Sim | `eliseu.gili` |
| `senha_mestra_hash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |
| `criado_em` | data e hora | Sim | `2026-10-13T19:20:00` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `id` | inteiro | Sim | `1` |
| `username` | texto | Sim | `eliseu.gili` |
| `senha_mestra_hash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |
| `criado_em` | data e hora | Sim | `2026-10-13T19:20:00` |

## Erros

| Quando | O que volta |
|---|---|
| `username` já existe | `UNIQUE_VIOLATION` |
| Campo obrigatório vazio | `NOT_NULL_VIOLATION` |
| Erro de escrita no arquivo | `SQLITE_IOERR` |