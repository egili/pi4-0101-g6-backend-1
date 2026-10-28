# CT7 - exportarBackup

## Cabeçalho

| Campo | Valor |
|---|---|
| **ID e nome** | CT7 - `exportarBackup` |
| **Fronteira** | Socket |
| **Quem chama** | Cliente |
| **Quem responde** | Servidor |
| **Atende** | E8 |
| **Como chamar** | Comando `EXPORT_BACKUP` no canal Socket |

> **Nota de Arquitetura Zero-Trust e Especificação do Arquivo `.vault`:**  
> O comando `EXPORT_BACKUP` no Servidor limita-se a extrair os registros cifrados da tabela `credencial` e devolvê-los ao Cliente via Socket.  
> A criação do arquivo portátil `.vault` ocorre **estritamente no Cliente** (via Módulo de Criptografia local). O arquivo gerado é estruturado com um **cabeçalho aberto (metadados com `salt` e `iv`)** e um **corpo cifrado**:
> 1. **Portabilidade:** O cabeçalho aberto permite que o arquivo seja importado em qualquer terminal ou instalação sem requerer dados prévios do servidor;
> 2. **Segurança Criptográfica:** O `salt` e o `iv` são parâmetros públicos de inicialização que não revelam as credenciais sem o conhecimento da senha mestra secreta. O Servidor nunca recebe nem armazena o arquivo `.vault` ou a chave de backup.

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `tokenSessao` | texto | Sim | `abc123xyz...` |

## Saída

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credenciais` | lista de objeto (Credencial) | Sim | lista com 3 credenciais |
| `usuario` | objeto (Usuario) | Sim | dados do usuário |
| `mensagem` | texto | Sim | `Backup pronto para exportação` |

**Objeto Credencial (dentro da lista):**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `credencialId` | inteiro | Sim | `10` |
| `servico` | texto | Sim | `Gmail` |
| `usuarioServico` | texto | Sim | `eliseu@gmail.com` |
| `dadosCifrados` | texto | Sim | `U2FsdGVkX1+...` |
| `iv` | texto | Sim | `3a4b5c6d7e8f...` |

**Objeto Usuario:**

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| `usuarioId` | inteiro | Sim | `1` |
| `username` | texto | Sim | `eliseu.gili` |
| `senhaMestraHash` | texto | Sim | `a1b2c3d4e5f6...` |
| `salt` | texto | Sim | `x9y8z7w6v5u4...` |

## Erros

| Quando | O que volta |
|---|---|
| `tokenSessao` inválido | `SESSAO_INVALIDA` |
| `usuarioId` não existe | `USUARIO_NAO_ENCONTRADO` |
| Nenhuma credencial para exportar | `BACKUP_VAZIO` |
| Socket caiu | `ERRO_CONEXAO` |