# CT6 - excluirCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT6 - `excluirCredencial` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E6 |
| **Como chamar** | Comando `DELETE_CRED` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `credencialId` | inteiro | Sim | `10` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `mensagem` | texto | Sim | `Credencial excluída com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `credencialId` não existe | `CREDENCIAL_NAO_ENCONTRADA` |
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| Falha ao excluir no banco | `ERRO_BANCO` |
| Socket caiu | `ERRO_CONEXAO` |