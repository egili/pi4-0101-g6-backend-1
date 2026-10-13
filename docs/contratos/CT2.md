# CT2 – salvarCredencial

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT2 – `salvarCredencial` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente CLI |
| **Quem responde** | Servidor |
| **Atende** | E4 |
| **Como chamar** | Comando `SAVE_CRED` no canal Socket |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `mensagem` | texto | Sim | `Credencial salva com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `usuarioId` inválido | `USUARIO_NAO_ENCONTRADO` |
| `dadosCifrados` vazio | `DADOS_INVALIDOS` |
| `iv` vazio | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Socket caiu | `ERRO_CONEXAO` |