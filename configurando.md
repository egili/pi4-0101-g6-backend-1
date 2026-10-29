# 🚀 Guia de Configuração: Testes Automáticos no Commit

Para que seus testes rodem automaticamente a cada `git push` ou `Pull Request`, siga estes passos. Como você já possui o arquivo `ci.yml`, precisamos garantir que o GitHub saiba como e quando executá-lo.

## 1. Validar a Estrutura do Arquivo
O GitHub Actions só reconhece o fluxo de trabalho se o arquivo estiver no caminho exato:
`seu-repositorio/.github/workflows/ci.yml`

**O que verificar no seu `ci.yml`:**
Certifique-se de que ele tenha a seção `on:`, que define os gatilhos. Exemplo:
```yaml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
2. Configurar Secrets (Segredos)
Se a sua classe ou teste depende de banco de dados, senhas ou tokens de API, configure-os no GitHub para não expô-los no código:
1. No GitHub, vá em: Settings $\rightarrow$ Secrets and variables $\rightarrow$ Actions.
2. Clique em New repository secret.
3. Adicione as chaves necessárias (ex: DB_PASSWORD, API_KEY).
4. No ci.yml, utilize-as como: ${{ secrets.DB_PASSWORD }}.
3. Ajustar Permissões do Repositório
Para garantir que o GitHub Actions tenha as permissões necessárias:
1. Vá em Settings $\rightarrow$ Actions $\rightarrow$ General.
2. Em Workflow permissions, selecione: Read and write permissions.
3. Clique em Save.
4. Testar a Execução
Para verificar se tudo está funcionando:
1. Vá na aba Actions no topo do seu repositório no GitHub.
2. Selecione o workflow correspondente ao seu arquivo ci.yml.
3. Se o arquivo possuir workflow_dispatch:, use o botão Run workflow.
4. Caso contrário, faça um commit de teste:
git commit -m "test: trigger ci"
git push origin main
5. Monitoramento e Validação
1. Acompanhe a execução na aba Actions.
2. ❌ Vermelho: Clique no job e analise os logs para corrigir falhas de dependência ou ambiente.
3. ✅ Verde: Configuração concluída. Agora, qualquer alteração em classes disparará os testes automaticamente.
Atenção: Verifique sempre os logs na primeira execução. Se o comando de teste estiver errado no ci.yml, o GitHub pode reportar sucesso mesmo sem ter executado nenhum teste de fato.
