# Contrato de recusa de credenciais

POST /api/v1/auth/login usa HTTP 401 para email inexistente, senha incorreta,
conta desativada ou conta sem senha. Todos retornam ProblemDetail com
code INVALID_CREDENTIALS, título Credenciais inválidas, detalhe Dados de acesso
inválidos e correlationId. Diagnósticos internos não são refletidos no detalhe.

Login válido permanece 200 com token e perfil. Formato inválido dos campos
permanece 422; JSON malformado permanece 400; rate limiting permanece 429.
401 de recurso protegido sem autenticação mantém AUTHENTICATION_REQUIRED.

A correção alinha a API ao feedback de login existente no Front e no Mobile.
Não altera papéis, expiração, CORS, limites, dependências ou gates. Uniformidade
da resposta não representa uma avaliação completa de resistência a ataques
por timing ou uma auditoria de segurança do produto.

Regressões: LoginUseCaseImplTest, AuthControllerTest e
InvitationActivationPostgresIntegrationTest, com PostgreSQL/Flyway, encoder e
cadeia de segurança reais. Executar mvnw clean verify. Prova completa de UI,
SMTP e banco local é registrada separadamente na REN-73.
