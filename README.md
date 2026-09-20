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
./mvnw.cmd clean verify -Dspring.profiles.active=test
```

O script de diagnóstico valida o Java 21, o Maven Wrapper e as versões
declaradas no repositório. A CI executa o mesmo diagnóstico antes do build.

## Variáveis de ambiente

As credenciais de banco, SMTP e JWT devem ser fornecidas por variáveis de
ambiente ou secrets da CI. Nunca versionamos `.env`, tokens ou senhas.

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
- `/api/v1/users/**`, `/api/v1/audit/**` e `/api/v1/access-requests/**` exigem
  `ADMIN`;
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

## Fluxo Git

As funcionalidades partem de `dev`, usam `feat/<linear-id>-<nome-curto>` e
retornam por Pull Request. A promoção para produção ocorre de `dev` para
`main`, que permanece protegida.
