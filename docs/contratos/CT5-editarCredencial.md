# CT5 - editarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT5 - `editarCredencial` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E6 |
| **Como chamar** | Comando `EDIT_CRED` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Não | `Gmail` |
| `usuarioServico` | texto | Não | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Não | `U2FsdGVkX1+...` |
| `iv` | texto | Não | `3a4b5c6d7e8f...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `mensagem` | texto | Sim | `Credencial atualizada com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `credencialId` não existe | `CREDENCIAL_NAO_ENCONTRADA` |
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| Nenhum campo para atualizar | `NADA_PARA_ATUALIZAR` |
| Falha ao atualizar no banco | `ERRO_BANCO` |
| Socket caiu | `ERRO_CONEXAO` |