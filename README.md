# Csports

Csports is a backend-focused learning project for discovering trainers, creating
training sessions, and booking sports activities. The product is intentionally
built as a modular monolith so that advanced backend techniques can be added
without introducing microservices before they are useful.

The project currently demonstrates:

- Spring Boot 4 and Java 21
- JWT access and refresh token authentication
- PostgreSQL persistence with Flyway migrations
- Redis caching and token blacklisting
- Docker Compose infrastructure
- Versioned REST endpoints with backwards-compatible legacy routes
- Generated OpenAPI/Swagger documentation
- Unit tests and opt-in Testcontainers integration tests

## Repository layout

```text
.
├── backend/          Spring Boot API
├── infrastructure/   PostgreSQL and Redis Docker Compose setup
├── docs/             Architecture, database, API, and testing guides
└── .github/workflows Continuous integration
```

Backend code is organized by business feature under `com.csports`:

```text
auth  booking  location  session  sport  trainer  user
common  infrastructure.redis  security
```

This keeps code that changes together close together. `CsportsApplication` is
in the root `com.csports` package, so Spring automatically scans every feature.

## Prerequisites

- Java 21
- Docker Desktop or another Docker Engine with Compose
- Git

The Maven wrapper is included, so a separate Maven installation is not needed.

## Run locally

1. Start PostgreSQL and Redis:

   ```bash
   cd infrastructure
   docker compose up -d
   docker compose ps
   ```

2. Start the backend:

   macOS/Linux:

   ```bash
   cd ../backend
   ./mvnw spring-boot:run
   ```

   Windows PowerShell:

   ```powershell
   Set-Location ..\backend
   $env:JAVA_HOME = "C:\path\to\jdk-21"
   .\mvnw.cmd spring-boot:run
   ```

3. Open Swagger UI:

   ```text
   http://localhost:8080/swagger-ui.html
   ```

The OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`. Postman
can import that URL, which avoids manually maintaining a second endpoint list.

## Configuration

The default `dev` profile matches the Docker Compose defaults. Every value can
be overridden with an environment variable:

| Variable | Development default |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/csports` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `postgres` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `JWT_SECRET` | development-only key |

The `prod` profile requires database credentials and `JWT_SECRET`; it contains
no secret fallback. See [.env.example](.env.example).

## API versioning

New consumers should use `/api/v1`, for example:

```text
POST /api/v1/auth/login
GET  /api/v1/sports/list
GET  /api/v1/sessions
```

The old unversioned routes remain registered for current Postman collections.
They are compatibility routes and can be removed in a future breaking release.
See [API versioning](docs/api/versioning.md).

## Database migrations

Flyway applies ordered SQL migrations from
`backend/src/main/resources/db/migration`. Hibernate uses `validate`, so it
checks the schema but never silently modifies it.

For an existing local database that predates Flyway, the dev profile performs a
one-time baseline at version 1. New databases execute `V1__initial_schema.sql`.
Production disables automatic baselining. Read the
[Flyway guide](docs/database/flyway.md) before adding a migration.

## Tests

Fast tests:

```bash
cd backend
./mvnw clean test
```

Real PostgreSQL and Redis integration test:

```bash
./mvnw clean verify -Pintegration-tests
```

The integration profile creates disposable Testcontainers resources. It does
not use or modify the normal Compose containers or their PostgreSQL volume.

## Docker scope

The current Compose file runs PostgreSQL and Redis. The Spring Boot process
still runs on the host; there is not yet a backend Docker image or service.
This distinction is intentional and documented in
[the Docker guide](infrastructure/README.md). Containerizing the backend can be
added after the runtime configuration and health endpoints are stable.

## Roadmap

- Complete authentication, sessions, bookings, and authorization behavior
- Add integration coverage for the main user journeys
- Introduce observability and health endpoints
- Add Kafka and Elasticsearch only through features that demonstrate them
- Containerize the backend
- Start the React client after the v1 contracts and error format are stable

The frontend should begin when login/refresh/logout, sports, sessions, and
bookings have stable v1 request/response contracts. That avoids learning React
while repeatedly rewriting its API layer.

## More documentation

- [Architecture overview](docs/architecture/overview.md)
- [Authorization matrix](docs/security/authorization.md)
- [API error contract](docs/api/errors.md)
- [Account and trainer profiles](docs/api/account-profiles.md)
- [Session discovery and filtering](docs/api/session-discovery.md)
- [Training session lifecycle](docs/api/session-lifecycle.md)
- [Postman session lifecycle test](docs/development/postman-session-lifecycle.md)
- [Flyway and database changes](docs/database/flyway.md)
- [API versioning](docs/api/versioning.md)
- [Testing strategy](docs/development/testing.md)
- [Architecture decision: modular monolith](docs/decisions/0001-modular-monolith.md)
