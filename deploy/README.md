# Produção do Our Eyes no Google Cloud

O painel Angular e a API Spring Boot usam dois serviços Cloud Run. A API executa as migrações Flyway no PostgreSQL gerenciado pelo Cloud SQL. A detecção, a voz e a vibração continuam no Android. A conta do aplicativo pode usar o endereço HTTPS da API. O módulo de IA publica o modelo que já é embarcado no aplicativo.

## Preparar o projeto

1. Usar um projeto exclusivo com ID no formato `eyes-prod-...` e rótulo `application=our-eyes`. Conferir as permissões e o faturamento com a conta responsável. Em uma demonstração, configurar alertas de orçamento no projeto e estabelecer a janela de remoção. Alertas de orçamento chegam depois que o uso é registrado e não limitam todos os recursos.
2. Definir uma região comum para Cloud Run, Artifact Registry e Cloud SQL. Para a demonstração, o script usa PostgreSQL 15 zonal, instância compartilhada, disco fixo de 10 GB, proteção contra exclusão e backups sem retenção após a remoção. Antes de uma operação contínua, revisar capacidade, disponibilidade, restauração de backups e SMTP autenticado com o responsável.
3. Executar `Initialize-EyesCloud.ps1` com o CLI autenticado na conta autorizada. O script cria identidades dedicadas, credenciais aleatórias no Secret Manager, o repositório imutável e o banco. A conexão do banco exige Cloud SQL Connector e TLS. Acesso direto pela Internet por lista de IPs fica desabilitado.

## Primeira implantação

Construir as imagens a partir dos commits selecionados e aprovados. A API usa o JAR produzido por `./mvnw clean verify`. O painel usa `docker build --platform linux/amd64 --build-arg API_ORIGIN=https://eyes-api-NUMERO.REGIAO.run.app`. Rotular as imagens com `org.opencontainers.image.revision` e guardar os commits e as referências imutáveis do Artifact Registry após o push.

Executar `Start-EyesCloud.ps1` com as duas imagens no formato `imagem@sha256:digest` e os commits correspondentes. Os endereços Cloud Run usam HTTPS. O acesso administrativo requer autenticação da aplicação. A API só permite a origem configurada do painel no CORS. As credenciais vêm do Secret Manager. O serviço reduz a zero instâncias sem requisições e tem limite de duas instâncias.

Na janela de prova, usar somente dados sintéticos. O parâmetro `-BootstrapSyntheticAdmin` habilita a criação de uma conta exclusiva de QA. Executar `Save-EyesCloudEvidence.ps1` com `-VerifySyntheticAdmin` e repetir `Start-EyesCloud.ps1` sem o parâmetro de bootstrap para retirar a configuração e a senha da revisão ativa. O SMTP real e a coleta opcional de metadados têm configuração e validação próprias.

## Promoção por pull request

Executar `Set-EyesReleaseRules.ps1` para exigir PR e CI em `main` nos dois repositórios. `Connect-EyesGitHub.ps1` configura o ambiente `production` e a federação OIDC com o projeto. Cada repositório usa sua própria identidade de deploy. A federação confere IDs imutáveis dos repositórios, proprietário, branch, ambiente e arquivo do workflow. A identidade de execução da API lê apenas os segredos necessários e se conecta ao PostgreSQL.

1. Integrar a configuração primeiro à `dev` em cada repositório. Abrir o PR de promoção da `dev` para `main`. A CI confere a origem e roda testes, build e Quality Gate.
2. Habilitar a variável de repositório `GCP_DEPLOY_ENABLED=true` somente durante uma janela aprovada, com o ambiente provisionado. Depois do merge, a esteira publica uma imagem por digest, testa uma revisão sem tráfego principal e promove apenas se o smoke HTTPS passar e o commit ainda for a ponta da `main`.
3. Se uma revisão promovida apresentar regressão, voltar o tráfego à revisão anterior em Cloud Run. Uma reversão de aplicativo não desfaz migrações. Planejar migrações compatíveis e validar restauração antes de aplicar alterações destrutivas.

Para instalar uma build Android que acesse esse ambiente, informar a origem HTTPS da API em `--dart-define=API_BASE_URL=...` e identificar o APK. O uso assistivo continua disponível offline. A configuração Android permanente só deve apontar para um ambiente que permanecerá ativo.

## Evidências e desligamento

Guardar `deployment.json`, capturas sem dados pessoais ou credenciais, logs sanitizados, SHA-256 e URLs das execuções. Se a implantação parar no meio, guardar o inventário parcial e usar `-FailedProvisioning`.

Executar `Stop-EyesCloud.ps1` com o ID exato do projeto nos dois parâmetros de confirmação e o diretório local de evidências. Ele desativa a publicação, verifica execuções em andamento e remove apenas os serviços, a instância, o repositório de imagens e os segredos do ambiente dedicado. A remoção do banco apaga os dados sintéticos. O arquivo `teardown.json` registra o inventário posterior, inclusive backups. A opção `-UnlinkBilling` desvincula o projeto da conta de faturamento depois da conferência de recursos e exige a permissão de faturamento apropriada.

Manter o código, os registros locais e as configurações de promoção. Para uma nova demonstração, repetir a preparação e a primeira implantação. Conferir o relatório de faturamento quando o uso da janela aparecer. Valores já consumidos durante a janela continuam associados à conta.

- [Cloud Run e PostgreSQL](https://docs.cloud.google.com/sql/docs/postgres/connect-run)
- [Federação com esteiras de deploy](https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines)
- [Orçamentos e alertas](https://docs.cloud.google.com/billing/docs/how-to/budgets)
- [Backups e retenção do Cloud SQL](https://docs.cloud.google.com/sql/docs/postgres/backup-recovery/backups)
