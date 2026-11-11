# CT8 - importarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT8 - `importarBackup` |
| **Fronteira** | Socket TCP (objetos Serializable) |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E9 |
| **Como chamar** | `PedidoDeImportacao` (token, lista de credenciais já re-cifradas) |

> **Nota de Arquitetura Zero-Trust e Re-cifragem:**  
> A abertura do arquivo `.vault`, a extração do `salt`/`iv` do cabeçalho aberto, a decifração do backup com a senha de origem e a **re-cifragem de cada credencial com a chave mestra ativa do cofre atual (com novos IVs gerados)** acontecem 100% no Cliente (processo local).  
> Os itens recebidos no corpo já chegam ao Servidor cifrados com a chave da sessão ativa do usuário. O Servidor valida o `tokenSessao` e persiste os novos blobs na tabela `credencial` (`CT10`) numa **única transação SQL**: ou todas as credenciais são gravadas, ou nenhuma é. Assim, o Servidor nunca vê os dados em texto claro nem participa do processo de decifragem/cifragem.

## Entrada

Conteúdo do pedido enviado pelo socket: lista (array) de objetos Credencial. O usuário é identificado pelo token (CT3).

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `Authorization` (cabeçalho) | texto | Sim | `Bearer abc123xyz...` |
| `credenciais` (corpo) | lista de objeto (Credencial) | Sim | lista com 3 credenciais |

**Objeto Credencial (cada item da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` (re-cifrado com chave atual) |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` (novo IV gerado para a inserção) |

## Saída (`200 OK`)

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciaisInseridas` | inteiro | Sim | `3` |
| `mensagem` | texto | Sim | `Backup restaurado com sucesso` |

## Erros

Erros do Servidor voltam na `Resposta` com o campo `erro` preenchido com o código abaixo. Em qualquer erro desta rota, nenhuma credencial é gravada.

| Quando | O que volta |
|---|---|
| Token ausente, inválido ou expirado | `SESSAO_INVALIDA` |
| Lista de credenciais vazia | `BACKUP_VAZIO` |
| Credencial com campo obrigatório vazio | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | `ERRO_BANCO` |
| Falha de conexão (sem resposta) | `ERRO_CONEXAO` (mensagem do Cliente) |
