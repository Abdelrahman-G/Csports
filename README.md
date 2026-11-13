# Csports

Csports is a full-stack platform for discovering and managing sports training
courses. Trainers can publish recurring courses, while participants can
search, book, cancel, and receive in-app notifications.

## Architecture

```text
Browser -> Nginx (React SPA + /api reverse proxy) -> Spring Boot
                                                       |-- PostgreSQL/PostGIS
                                                       `-- Redis
```

## Technical highlights

- A modular **Java 21 and Spring Boot** REST API with versioned endpoints,
  request validation, consistent errors, OpenAPI, and role-based access through
  **Spring Security**.
- Short-lived **JWT** access tokens, atomic one-time refresh-token rotation, and
  Redis-backed access-token revocation.
- Filtered course discovery using JPA specifications, database indexes,
  **PostGIS** distance queries, and **Redis-cached** search results.
- Concurrency-safe booking using a per-course **Redis distributed lock**, JPA
  optimistic locking, PostgreSQL constraints, and transactional fallback.
- A **React and TypeScript** single-page application with protected routes,
  shared authentication state, automatic token refresh, and an **Nginx** API
  reverse proxy.
- Versioned **Flyway** migrations and multi-stage Docker builds for the Spring
  and React applications, orchestrated with **Docker Compose** health checks and
  persistent PostgreSQL and Redis volumes.
- **GitHub Actions** CI runs JUnit and Testcontainers integration tests, frontend
  linting and production builds, and Docker image builds.

## Run with Docker

Requirements: Docker Desktop or Docker Engine with Compose.

```bash
cd infrastructure
cp .env.example .env
```

Set `JWT_SECRET` in `.env` to a 64-character hexadecimal value, then run:

```bash
docker compose up -d --build
```

Open:

- Application: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Backend health: http://localhost:8080/actuator/health

Flyway creates the database schema and reference data on the first startup.
PostgreSQL and Redis data are preserved in Docker volumes.

Stop the application without deleting its data:

```bash
docker compose down
```

## Isolated local staging

Requires Docker Compose 2.24.4 or newer. This runs the same application with the
production Spring profile, separate PostgreSQL/Redis volumes, and its own local
image tags. It publishes only the frontend on http://localhost:3100.

From `infrastructure/staging`, copy `.env.example` to `.env`. Set a new random
`POSTGRES_PASSWORD`, `APP_DB_PASSWORD`, `MIGRATION_DB_PASSWORD`, and a separate
random 64-character hexadecimal `JWT_SECRET`.
Never commit `.env` or reuse the development secrets. Then run:

```bash
docker compose --env-file .env -p csports-staging config --quiet
docker compose --env-file .env -p csports-staging up -d --build --wait
```

The fresh database contains migrated reference data; create new test accounts
through the app. Do not expect development accounts or courses to exist here.
An administrator-only `database-setup` service provisions the `csports_app` and
`csports_migrator` database login roles before the backend starts. It preserves
existing application data and transfers application-table ownership to the
migration role, leaving PostGIS objects with the administrator. The normal
backend connection uses `csports_app` and disables startup Flyway execution.
The separate `database-migrate` job uses `csports_migrator`, runs the SQL
migrations and exits. The backend starts only after that job succeeds. It has
no administrator or migration credentials. Default grants cover future
migration-created tables and sequences, while a packaged Flyway callback keeps
migration history inaccessible to the runtime role.

The migration job reuses the backend image, selecting the plain Java
`com.csports.migration.DatabaseMigration` entry point through Spring Boot's
`PropertiesLauncher`. It starts no web server or schedulers and needs only its
database connection. Migration SQL and the deployment callback are packaged in
that same image. Check the job separately with:

```bash
docker compose --env-file .env -p csports-staging logs database-migrate
docker compose --env-file .env -p csports-staging ps -a
```

`Exited (0)` is the expected successful job state. A nonzero exit blocks new
backend startup; it does not automatically reverse migrations or stop an already
running older backend. Keep schema compatibility and recovery in the release
procedure. Rebuild images after source changes before deploying a new candidate.

This is a local rehearsal, not a hardened public deployment. Changing the
application/migration passwords in `.env` and rerunning `up -d --wait` reapplies
setup and recreates affected containers with their new credentials. Changing
`POSTGRES_PASSWORD` does not rotate an existing administrator's password.

Staging has memory ceilings of 1 GiB for the backend, 512 MiB for PostgreSQL,
128 MiB for Redis and 64 MiB for Nginx. The migration and setup jobs have
512 MiB and 128 MiB ceilings respectively. These are practice budgets based on
local observations, not guaranteed production capacity. CPU is not capped here;
select a CPU budget when the deployment host is known.

All services use Docker's rotating `local` log driver with three 10 MiB files
per container, before compression/overhead. This limits diagnostic logs, not
database-volume size. Redis retains AOF persistence and uses a 64 MiB internal
memory limit within its 128 MiB container budget. Its `noeviction` policy keeps
existing locks/revocation entries rather than evicting them under pressure;
memory-allocating writes can fail when the internal limit is reached. Observe
usage/errors and increase the budget if the demo grows. Expiry still applies.

To stop staging and retain its data:

```bash
docker compose --env-file .env -p csports-staging down
```

Keep the project name unchanged so Compose reuses the same staging volumes.
Do not add `--volumes`/`-v`; that would delete staging data. Configuration checks
run without starting containers:

```bash
node --test infrastructure/staging/compose.test.mjs
```

Run that check from the repository root. It uses disposable test credentials,
validates the effective Compose model, and rejects missing required settings.

The real PostgreSQL/Redis deployment check builds the images, starts an isolated
temporary project, verifies the migration failure gate, fresh migrations, runtime
credential isolation, reruns, checksum validation, role permissions, future table
grants and authentication through Nginx, then deletes only its disposable data:

```bash
node --test infrastructure/staging/database.test.mjs
```

## License

MIT
