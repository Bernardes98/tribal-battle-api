# Tribal Battle API

Backend em Spring Boot 4.1 / Java 17, com Gradle, PostgreSQL em Docker e Liquibase.

## Stack

- Java 17
- Spring Boot 4.1.0
- Gradle
- Spring MVC
- Spring Data JPA
- PostgreSQL 17
- Liquibase
- Lombok

## 1. Gerar o Gradle Wrapper

O projeto inclui `setup-gradle-wrapper.ps1` para gerar o wrapper usando Gradle 9.1.0.
Essa versão pode iniciar com Java 25 e o projeto continua compilando com toolchain Java 17.

No PowerShell, na raiz do projeto:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\setup-gradle-wrapper.ps1
```

Depois disso use sempre o wrapper:

```powershell
.\gradlew.bat clean build
```

## 2. Subir o PostgreSQL

O banco do Docker usa a porta `5433` no Windows para não conflitar com um PostgreSQL local em `5432`.

```powershell
docker compose up -d
docker compose ps
```

Configuração padrão:

- host: `localhost`
- port: `5433`
- database: `tribal_battle`
- username: `tribal_battle`
- password: `tribal_battle`

## 3. Executar a aplicação

```powershell
.\gradlew.bat bootRun
```

API:

```text
http://localhost:8080
```

## 4. Liquibase

O Hibernate está configurado com:

```text
ddl-auto: validate
```

Quem cria e altera o schema é o Liquibase.

Changelog principal:

```text
src/main/resources/db/changelog/db.changelog-master.yaml
```

Primeira migration:

```text
src/main/resources/db/changelog/changes/001-create-shared-simulation.yaml
```

Na primeira execução serão criadas as tabelas:

- `shared_simulation`
- `databasechangelog`
- `databasechangeloglock`

## 5. Acessar o PostgreSQL do container

```powershell
docker exec -it tribal-battle-postgres psql -U tribal_battle -d tribal_battle
```

No `psql`:

```sql
\dt
SELECT * FROM databasechangelog;
SELECT * FROM shared_simulation;
```

## 6. Variáveis de ambiente opcionais

A aplicação aceita:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PORT`
- `FRONTEND_ORIGIN`

Sem variáveis definidas, usa os valores locais do `compose.yaml`.
