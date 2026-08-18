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

## Fluxo Git

As funcionalidades partem de `dev`, usam `feat/<linear-id>-<nome-curto>` e
retornam por Pull Request. A promoção para produção ocorre de `dev` para
`main`, que permanece protegida.
