# Csports

Csports is a full-stack platform for discovering and managing sports training
sessions. Trainers can publish recurring sessions, while participants can
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
- Filtered session discovery using JPA specifications, database indexes,
  **PostGIS** distance queries, and **Redis-cached** search results.
- Concurrency-safe booking using a per-session **Redis distributed lock**, JPA
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

## License

MIT
