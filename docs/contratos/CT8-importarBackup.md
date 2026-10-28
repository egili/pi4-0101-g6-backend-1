# CT8 - importarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT8 - `importarBackup` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E9 |
| **Como chamar** | Comando `IMPORT_BACKUP` no canal Socket |

> **Nota de Arquitetura Zero-Trust e Re-cifragem:**  
> A abertura do arquivo `.vault`, a extração do `salt`/`iv` do cabeçalho aberto, a decifração do backup com a senha de origem e a **re-cifragem de cada credencial com a chave mestra ativa do cofre atual (com novos IVs gerados)** acontecem 100% no Cliente (processo local).  
> Os itens recebidos na lista `credenciais` já chegam ao Servidor cifrados com a chave da sessão ativa do usuário. O Servidor valida o `tokenSessao` e persiste os novos blobs na tabela `credencial` (`CT10`), garantindo que o Servidor nunca veja os dados em texto claro nem participe do processo de decifragem/cifragem.

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |
| `credenciais` | lista de objeto (Credencial) | Sim | lista com 3 credenciais |

**Objeto Credencial (dentro da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` (re-cifrado com chave atual) |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` (novo IV gerado para a inserção) |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciaisInseridas` | inteiro | Sim | `3` |
| `mensagem` | texto | Sim | `Backup restaurado com sucesso` |

## Erros

| Quando | O que volta |
|---|---|
| `tokenSessao` inválido ou expirado | `SESSAO_INVALIDA` |
| `usuarioId` não existe | `USUARIO_NAO_ENCONTRADO` |
| Lista de credenciais vazia | `BACKUP_VAZIO` |
| Credencial com campo obrigatório vazio | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Socket caiu | `ERRO_CONEXAO` |