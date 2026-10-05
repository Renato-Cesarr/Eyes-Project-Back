# Ambiente local PostgreSQL + Mailpit — REN-39

Este ambiente destina-se à demonstração local da API administrativa. Não containeriza frontend/mobile, não substitui a homologação completa web/API/e-mail da REN-73 e não altera sessões remotas/consentimento. Java 21/Maven Wrapper permanecem fixados.

## Pré-requisitos e portas

- Docker com Compose v2+ que suporte `up --wait`, Docker Desktop iniciado no Windows.
- PowerShell 7+ (`pwsh`), Java 21 e Maven Wrapper do repositório. Scripts usam a raiz do checkout independentemente do diretório inicial.
- No Linux, executar `chmod +x ./mvnw` antes do diagnóstico/launcher (o wrapper histórico está versionado sem bit executável).
- Portas disponíveis: PostgreSQL **55432**, SMTP **1025**, Mailpit HTTP **8025**, API **8080**. Todas expostas em **127.0.0.1**. Ajustar DB_PORT/SMTP_PORT/MAILPIT_HTTP_PORT/PORT no .env antes de iniciar se necessário; as quatro devem ser diferentes.
- Projeto Compose exclusivo `eyes-local`; volumes `eyes-local_postgres-data` e `eyes-local_mailpit-data`. Um checkout ativo por projeto; o script rejeita containers pertencentes a outro checkout. Serviços de outros projetos não são parados/resetados.

## Inicialização

Na raiz do backend:

```powershell
./scripts/local-services.ps1 -Action start
```

Esse único comando cria .env a partir do exemplo caso não exista, gera três segredos aleatórios distintos, inicia PostgreSQL/Mailpit e aguarda healthchecks. Verifica também TCP PostgreSQL e HTTP /readyz **pelo host**, antes de considerar pronto. O .env existente é preservado; credenciais permanecem somente nesse arquivo ignorado. O formato é CHAVE=valor sem aspas, sem comandos, com valores simples compatíveis com Compose e properties Spring. O script não avalia seu conteúdo como código e restaura variáveis de processo após uso.

API em terminal separado:

```powershell
./scripts/start-local-api.ps1
```

O launcher repete a guarda de prontidão, valida toolchain e executa Maven Wrapper no profile **local**, importando obrigatoriamente .env e vinculando a API ao loopback. Os argumentos do launcher fixam endereços/portas locais com precedência sobre overrides externos do Spring. Flyway permanece ativo e Hibernate valida as migrations. O profile test/H2 não é usado nesse fluxo. O profile local fixa host de banco/SMTP e database/user eyes_local; .env.example não configura SMTP externo. Não usar profile local/.env de demonstração na produção.

Mailpit: [interface local](http://127.0.0.1:8025). Sem autenticação/TLS no SMTP local; a captura não encaminha mensagens a provedores externos. Ajustar FRONTEND_URL/CORS_ALLOWED_ORIGINS ao endereço do frontend nativo (padrão localhost:4200); essas URLs não iniciam o frontend.

## Primeiro administrador

Em banco local vazio, executar explicitamente:

```powershell
./scripts/start-local-api.ps1 -BootstrapAdmin
```

A flag ativa o bootstrap somente para o processo da API, sem gravar true no .env. Nome/e-mail/senha estão nas chaves BOOTSTRAP_ADMIN_* do .env, e a senha é gerada automaticamente. Consultar localmente o arquivo; não copiar credenciais para logs/evidências. O bootstrap é idempotente, não altera uma senha ativa de administrador existente e não é habilitado nos próximos starts sem a flag. Para encerrá-lo, Ctrl+C no terminal da API. Se outro administrador já existir, seguir o provisionamento existente em vez de presumir que a senha gerada corresponde a ele.

## Prova reproduzível sem mocks

Com a API iniciada e o administrador local provisionado:

```powershell
./scripts/verify-local-environment.ps1
```

O roteiro autentica o administrador, convida uma conta única @eyes.test, busca o convite pela API Mailpit, valida a URL configurada, ativa e autentica STUDENT. Em seguida solicita recuperação, captura o segundo e-mail, redefine a senha e autentica a mesma identidade. Confere as cinco migrations Flyway atuais e salva logs/local-verification.json com versão/dirty, passos e IDs de mensagens, **sem senhas, JWT ou tokens de ativação**. Não limpa o Mailpit nem altera contas reais. A conta de demonstração e as duas mensagens ficam no ambiente local até reset. Se existir bootstrap anterior com senha diferente, atualizar as credenciais exclusivamente locais ou usar banco demonstrativo novo.

O recibo prova backend/PostgreSQL/SMTP; não prova interação visual do painel, TTS/TalkBack, aparelho ou envio remoto de sessões. A REN-73 mantém a demonstração completa na UI e as dependências de escopo REN-70/18/75.

## Estado, parada e reset limitado

```powershell
./scripts/local-services.ps1 -Action status
./scripts/local-services.ps1 -Action stop
```

Stop remove apenas containers/rede eyes-local, **preservando os dois volumes e .env**. Parar também a API com Ctrl+C antes de executar stop/reset. Start reutiliza os dados preservados; trocar DB_PASSWORD no .env não muda a senha de uma base já inicializada.

Reset destrói dados **somente** do banco e Mailpit deste projeto. Exige confirmação explícita pelo nome, verifica labels de propriedade antes da remoção e não usa prune ou down --volumes:

```powershell
./scripts/local-services.ps1 -Action reset -ConfirmReset eyes-local
./scripts/local-services.ps1 -Action start
./scripts/start-local-api.ps1 -BootstrapAdmin
```

Sem confirmação ou com labels incompatíveis, a remoção é rejeitada. .env permanece; Flyway aplica novamente as migrations no banco vazio. Nunca executar reset para apagar dados de outro projeto ou uma base externa. Em checkouts simultâneos, parar o ambiente pelo checkout proprietário antes de iniciar outro.

## Versões e fontes

Imagens oficiais fixadas por digest multi-arquitetura, verificadas em 05/10/2026:

| Serviço | Versão | Digest |
| --- | --- | --- |
| PostgreSQL | 15.19 Alpine, mesma major 15 da CI | f7d23353e1b15400d22ebe31189f4d314b87a4c129cc400c8c2d8d4ca127bf81 |
| Mailpit | v1.31.4 | b68349e3a014b90c5610bfb26b2ae36f3892d7b8cf25ee140c6c71c98d2fcf48 |

[Compose up --wait](https://docs.docker.com/reference/cli/docker/compose/up/), [imagens oficiais Mailpit](https://mailpit.axllent.org/docs/install/docker/), [healthchecks /readyz](https://mailpit.axllent.org/docs/integration/healthcheck/), [release Mailpit v1.31.4](https://github.com/axllent/mailpit/releases/tag/v1.31.4). O healthcheck CLI /mailpit readyz foi conferido na imagem fixada. Rede bridge é necessária para as portas publicadas funcionarem no Docker Desktop com a API nativa; rede internal:true não é usada. As publicações em loopback mantêm o acesso restrito ao host.

Não existe artefato de implantação produtiva neste pacote. O exemplo de ambiente antigo com Mailtrap foi substituído pelo cenário local; produção deve fornecer banco/SMTP/JWT reais por configuração própria e sem profile local.

## Evidência da implementação

[Recibo versionado da validação](local-environment-evidence.json): prova executada no Windows com containers Linux, commit de implementação 91be0ce limpo, 140 testes sem skips e ciclo real de migrations/SMTP. O commit posterior do recibo/guia muda somente documentação; CI Linux verifica Java/PostgreSQL, sem alegar execução dos novos scripts PowerShell no Linux. Execução visual web/mobile permanece REN-73/32.
