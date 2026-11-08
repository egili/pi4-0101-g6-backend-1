# CT8 - importarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT8 - `importarBackup` |
| **Fronteira** | HTTP/REST |
| **Quem chama** | Cliente (Back-end 2) |
| **Quem responde** | Servidor (Back-end 1) |
| **Atende** | E9 |
| **Como chamar** | `POST /backup` com corpo JSON e cabeçalho `Authorization: Bearer <tokenSessao>` |

> **Nota de Arquitetura Zero-Trust e Re-cifragem:**  
> A abertura do arquivo `.vault`, a extração do `salt`/`iv` do cabeçalho aberto, a decifração do backup com a senha de origem e a **re-cifragem de cada credencial com a chave mestra ativa do cofre atual (com novos IVs gerados)** acontecem 100% no Cliente (processo local).  
> Os itens recebidos no corpo já chegam ao Servidor cifrados com a chave da sessão ativa do usuário. O Servidor valida o `tokenSessao` e persiste os novos blobs na tabela `credencial` (`CT10`) numa **única transação SQL**: ou todas as credenciais são gravadas, ou nenhuma é. Assim, o Servidor nunca vê os dados em texto claro nem participa do processo de decifragem/cifragem.

## Entrada

Corpo JSON da requisição HTTP: lista (array) de objetos Credencial. O usuário é identificado pelo token (CT3).

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

Erros do Servidor voltam no corpo JSON da resposta como `{"erro": "<código>"}`. Em qualquer erro desta rota, nenhuma credencial é gravada.

| Quando | Status HTTP | O que volta |
|---|---|---|
| Token ausente, inválido ou expirado | 401 | `SESSAO_INVALIDA` |
| Lista de credenciais vazia | 400 | `BACKUP_VAZIO` |
| Credencial com campo obrigatório vazio | 400 | `DADOS_INVALIDOS` |
| Falha ao gravar no banco | 500 | `ERRO_BANCO` |
| Falha de conexão HTTP (sem resposta) | 503 (gerado pelo Cliente) | `ERRO_CONEXAO` (mensagem do Cliente) |
