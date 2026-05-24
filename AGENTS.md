# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project At A Glance
- Stack: Spring Boot 3.2.x, Java 17, Maven, JPA, PostgreSQL (default), H2 (local profile).
- Runtime shape: one backend process serves REST APIs and static frontend files from the repository `web/` directory.
- Main source roots:
  - `backend/src/main/java/com/github/insight`
  - `backend/src/main/resources`
  - `web/`

## First Commands To Try
- Run app with PostgreSQL profile (default):
  - `mvn -f backend/pom.xml spring-boot:run`
- Run app with local H2 profile:
  - `mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"`
- Run tests:
  - `mvn -f backend/pom.xml test`
- Build jar:
  - `mvn -f backend/pom.xml clean package -DskipTests`

## Critical Working-Directory Rule
- Static files are served using `file:web/` (see `WebConfig`).
- Keep execution context aligned so `web/` is resolvable from repo root.
- `spring-boot-maven-plugin` already sets working directory to repo root when running via Maven from `backend/`.
- For jar execution, run from repository root:
  - `java -jar backend/target/github-activity-insight-1.0.0.jar`

## Architecture Map
- Controllers: `backend/src/main/java/com/github/insight/controller`
- Services (analysis, GitHub API, scoring): `backend/src/main/java/com/github/insight/service`
- Persistence:
  - Entities: `backend/src/main/java/com/github/insight/entity`
  - Repositories: `backend/src/main/java/com/github/insight/repository`
- DTOs and models:
  - DTO: `backend/src/main/java/com/github/insight/dto`
  - Domain models: `backend/src/main/java/com/github/insight/model`

## Config And Data Notes
- Default profile (`application.properties`) expects PostgreSQL and uses `ddl-auto=validate`.
- Local profile (`application-local.properties`) uses H2 file DB and `ddl-auto=create`.
- Schema file: `backend/src/main/resources/schema.sql`.
- Optional GitHub token env var (`GITHUB_TOKEN`) improves API rate limits.

## Agent Conventions
- Edit only the files directly required to implement the requested change. Do not rename, reformat, or restructure code in files not listed in your change set, even if style improvements are apparent.
- Prefer preserving package/class naming patterns already used in `backend/src/main/java/com/github/insight`.
- When adding endpoints, keep routing style consistent with existing controllers under `/api/**` and `/auth/**`.
- When adding or modifying JPA entities, always update `backend/src/main/resources/schema.sql` to match. The default profile uses `ddl-auto=validate`; do not use `ddl-auto=update` or `ddl-auto=create` in the default profile.
- When changing persistence logic, run `mvn -f backend/pom.xml test` to verify H2 compatibility, and manually inspect raw SQL or `schema.sql` changes for PostgreSQL-specific syntax that H2 may not support.
- Before finalizing backend changes, run `mvn -f backend/pom.xml test` at minimum. Skip only if Maven is unavailable or required local services are unreachable, and explicitly state the reason in your response.

## Docs To Consult (Link, Do Not Duplicate)
- Run guide: `RUNNING.md`
- Deployment guide: `DEPLOYMENT.md`
- Database setup: `DATABASE.md`
- Project overview: `README.md`
