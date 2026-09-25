# Eyes Project Back-End

API administrativa do Eyes Project, desenvolvida com Spring Boot, PostgreSQL e
Flyway.

## Toolchain fixado

- Java 21 LTS (Temurin recomendado);
- Maven 3.9.14, fornecido exclusivamente pelo Maven Wrapper;
- PostgreSQL 15 na integração contínua.

O arquivo `.java-version` permite que gerenciadores compatíveis selecionem o
Java 21. Não instale Maven globalmente: use sempre `mvnw` ou `mvnw.cmd`.

## Configuração no Windows

1. Instale um JDK 21 e configure `JAVA_HOME` para ele.
2. Confirme que o terminal encontra o Java correto:

```powershell
java -version
./scripts/check-toolchain.ps1
```

3. Execute os testes:

```powershell
./mvnw.cmd "-Dspring.profiles.active=test" clean verify
```

O script de diagnóstico valida o Java 21, o Maven Wrapper e as versões
declaradas no repositório. A CI executa o mesmo diagnóstico antes do build.

## Variáveis de ambiente

As credenciais de banco, SMTP e JWT devem ser fornecidas por variáveis de
ambiente ou secrets da CI. Nunca versionamos `.env`, tokens ou senhas.

### HTTP, CORS e proteção contra abuso

Erros HTTP seguem o formato `application/problem+json` (RFC 9457) e sempre
incluem `type`, `title`, `status`, `detail`, `instance`, um `code` estável e o
`correlationId` da requisição. Falhas inesperadas retornam `500` sem expor
stack trace ou detalhes internos; o diagnóstico completo permanece somente no
log estruturado do servidor. A propriedade de compatibilidade `message` repete
temporariamente o `detail` para clientes anteriores e não deve ser usada em
novas integrações.

As origens permitidas são definidas explicitamente em
`CORS_ALLOWED_ORIGINS`, separadas por vírgula. Wildcards e valores com caminho,
query string ou fragmento são rejeitados na inicialização. O padrão local é
`http://localhost:4200`, sem credenciais gerenciadas pelo navegador.

Os endpoints públicos sensíveis usam token bucket em memória, isolado por
endereço remoto e por fluxo:

| Fluxo | Capacidade padrão | Janela | Variáveis |
| --- | ---: | ---: | --- |
| Login | 10 | 5 minutos | `LOGIN_RATE_LIMIT_CAPACITY`, `LOGIN_RATE_LIMIT_WINDOW_SECONDS` |
| Solicitação de acesso | 5 | 15 minutos | `ACCESS_REQUEST_RATE_LIMIT_CAPACITY`, `ACCESS_REQUEST_RATE_LIMIT_WINDOW_SECONDS` |
| Recuperação/redefinição | 5 | 15 minutos | `PASSWORD_RECOVERY_RATE_LIMIT_CAPACITY`, `PASSWORD_RECOVERY_RATE_LIMIT_WINDOW_SECONDS` |

Ao exceder o limite, a API responde `429` com `Retry-After`. A estratégia é
local ao processo e adequada ao MVP de instância única; armazenamento
distribuído fica fora deste escopo.

Os logs usam JSON no formato Logstash por padrão e incluem os dados do MDC,
inclusive `correlationId`. Para desenvolvimento local, `LOG_STRUCTURED_FORMAT`
pode selecionar outro formato suportado pelo Spring Boot.

### Testes de integração com PostgreSQL

As suítes `*PostgresIntegrationTest` usam Testcontainers com PostgreSQL 15 e
executam o Flyway antes da validação do Hibernate. Assim, migrations, restrições
e fluxos críticos de segurança são verificados no mesmo banco usado pela
aplicação, sem depender de uma instância configurada manualmente.

Para executá-las localmente, inicie o Docker Desktop e rode:

```powershell
./mvnw.cmd "-Dtest=*PostgresIntegrationTest" test
```

Sem Docker, essas suítes são ignoradas localmente; a CI exige que todas sejam
executadas, com zero cenários ignorados.

### Primeiro administrador

O bootstrap do primeiro administrador é opcional, idempotente e desativado por
padrão. Use-o somente em um ambiente controlado, fornecendo todas as variáveis:

```text
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_NAME=<nome>
BOOTSTRAP_ADMIN_EMAIL=<email>
BOOTSTRAP_ADMIN_PASSWORD=<senha-temporaria-segura>
```

Se já existir um usuário com o e-mail informado, ele será promovido sem trocar
uma senha ativa. Se qualquer administrador já existir, o bootstrap não altera
dados. Após o primeiro provisionamento, desative a flag e remova a senha do
ambiente.

Os usuários criados pelo fluxo normal recebem sempre o papel `STUDENT`.

## Autenticação e autorização

O MVP possui dois papéis: `ADMIN` e `STUDENT`. O login e o endpoint autenticado
`GET /api/v1/auth/me` devolvem o papel necessário à interface, e o JWT também
carrega a claim `role`.

A API não usa a claim do cliente como fonte de autorização: a cada requisição,
o filtro valida o token e carrega o papel atual persistido no banco. Usuários
inativos, removidos ou sem papel válido não são autenticados.

- rotas de autenticação por `POST` permanecem públicas;
- a definição da senha de convite permanece pública;
- `POST /api/v1/access-requests` é público e protegido por limite de requisições;
- as demais rotas de `/api/v1/access-requests/**`, além de `/api/v1/users/**` e
  `/api/v1/audit/**`, exigem `ADMIN`;
- uma requisição anônima recebe `401` e um usuário autenticado sem o papel
  necessário recebe `403`.

### Convite, ativação e recuperação

- `POST /api/v1/users` é um convite administrativo e exige papel `ADMIN`;
- `POST /api/v1/users/setup-password` é público porque o convidado ainda não
  possui credenciais;
- tokens `SETUP` e `RESET` são vinculados ao propósito, possuem expiração e são
  consumidos uma única vez;
- a alteração do usuário e o consumo do token acontecem na mesma transação;
- requisições concorrentes são serializadas por bloqueio de escrita no token;
- token inexistente, expirado, consumido ou de outro propósito retorna uma
  mensagem neutra, sem revelar seu estado;
- o valor do token não deve aparecer em logs, métricas ou mensagens de erro.

### Gestão administrativa de usuários

Todas as rotas abaixo exigem JWT de um usuário `ADMIN`:

| Método | Rota | Finalidade |
| --- | --- | --- |
| `GET` | `/api/v1/users` | Lista usuários com paginação e filtros |
| `GET` | `/api/v1/users/{id}` | Consulta um usuário sem campos sensíveis |
| `PATCH` | `/api/v1/users/{id}/status` | Ativa ou desativa uma conta |
| `POST` | `/api/v1/users/{id}/resend-invitation` | Substitui e reenvia um convite pendente |

A listagem aceita `page`, `size` (máximo de 100), `search`, `role`, `active`,
`sortBy` (`NAME`, `EMAIL` ou `CREATED_AT`) e `direction` (`ASC` ou `DESC`).
O campo `search` procura por nome ou e-mail sem diferenciar maiúsculas e
minúsculas.

Contas são desativadas logicamente, sem exclusão física. O último
administrador ativo nunca pode ser desativado; essa invariável é protegida por
bloqueio pessimista no PostgreSQL, inclusive sob requisições concorrentes. Uma
conta convidada sem senha só pode ser ativada pelo link de convite. O reenvio
invalida qualquer token `SETUP` anterior e gera um novo token com 48 horas de
validade.

As respostas administrativas expõem apenas `id`, nome, e-mail, papel, status,
estado do convite e datas de auditoria. Hashes de senha e tokens nunca fazem
parte dos DTOs da API.

O contrato executável está disponível em `/v3/api-docs` e na interface
`/swagger-ui.html`. Rotas protegidas usam o esquema OpenAPI `bearerAuth`.

### Solicitações de acesso

O cadastro público não cria uma conta diretamente. Ele registra uma solicitação
pendente para análise administrativa:

| Método | Rota | Acesso | Finalidade |
| --- | --- | --- | --- |
| `POST` | `/api/v1/access-requests` | Público | Envia uma solicitação |
| `GET` | `/api/v1/access-requests` | `ADMIN` | Lista e filtra solicitações |
| `POST` | `/api/v1/access-requests/{id}/approve` | `ADMIN` | Aprova e emite o convite |
| `POST` | `/api/v1/access-requests/{id}/reject` | `ADMIN` | Rejeita com justificativa |

Nome e e-mail são normalizados antes da persistência. Submissões repetidas para
o mesmo e-mail enquanto existe uma solicitação pendente são idempotentes e
recebem a mesma resposta neutra `202 Accepted`, sem expor dados pessoais. Um
e-mail que já possui conta recebe conflito genérico e não cria solicitação.

A aprovação é transacional: bloqueia a solicitação, cria o usuário `STUDENT`,
emite o convite de definição de senha e grava a auditoria uma única vez. A
rejeição exige justificativa e também é auditada. Decisões finais não podem ser
invertidas; repetir a mesma decisão é seguro e não produz efeitos duplicados.

O endpoint público aplica um token bucket por endereço IP. Os valores padrão são
cinco tentativas a cada 15 minutos e podem ser ajustados pelas variáveis da
tabela de proteção contra abuso. O cabeçalho `X-Forwarded-For` só
deve ser considerado após configurar um proxy reverso confiável; por padrão a
aplicação usa o endereço remoto observado pelo servidor.

### Auditoria administrativa

Operações administrativas críticas produzem eventos imutáveis de auditoria.
São registrados convite e reenvio de convite, ativação ou desativação de conta
e aprovação ou rejeição de solicitação de acesso. Cada evento informa ação,
resultado (`SUCCESS` ou `FAILURE`), identificador do administrador, alvo,
instante, correlação da requisição e metadados técnicos previamente
sanitizados.

`GET /api/v1/audit` exige papel `ADMIN`, retorna os eventos mais recentes
primeiro e aceita `page`, `size` (máximo de 100), `actorUserId`, `action`,
`result`, `occurredFrom` e `occurredTo`. Não existem rotas para alterar ou
excluir eventos. O cabeçalho opcional `X-Correlation-ID` pode conter até 64
caracteres alfanuméricos, ponto, hífen ou sublinhado; valores ausentes ou
inválidos são substituídos por um UUID gerado pelo servidor e devolvido na
resposta.

Senhas, tokens, cabeçalhos de autorização, imagens e frames são bloqueados dos
metadados. Falhas guardam somente uma categoria neutra, nunca a mensagem
interna da exceção. O identificador do ator é um retrato histórico e permanece
mesmo se a conta for removida. Para o MVP, os registros são preservados por 180
dias; a automação de descarte fica desabilitada até a definição da política de
privacidade e deve ser implementada como processo controlado, nunca como uma
rota pública.

## Fluxo Git

As funcionalidades partem de `dev`, usam `feat/<linear-id>-<nome-curto>` e
retornam por Pull Request. A promoção para produção ocorre de `dev` para
`main`, que permanece protegida.
