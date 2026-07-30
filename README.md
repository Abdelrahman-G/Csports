# Csports

Csports is a Spring Boot backend for discovering trainers, publishing recurring
training sessions, and booking sports activities. It is a modular monolith built
as a long-term system-design practice project.

## Main lifecycle

1. Users and trainers register and authenticate with access and rotating refresh
   tokens.
2. Trainers create scheduled sessions for their sport and region.
3. Users search sessions by keyword, sport, trainer, region, date, price, and
   availability.
4. Users book or cancel. A per-session Redis lock, JPA optimistic locking, and
   PostgreSQL constraints protect the final seat.
5. Trainers can move, cancel, or restore eligible sessions. Affected users
   receive in-app notifications.
6. Flyway records every schema and reference-data change, while Redis provides
   caching, token revocation, and booking coordination.

## Technology

- Java 21 and Spring Boot
- Spring Security with JWT
- PostgreSQL and Flyway
- Redis
- Docker Compose
- OpenAPI/Swagger
- Maven, JUnit, Testcontainers, and GitHub Actions

## Run with Docker

Requirements: Docker Desktop or Docker Engine with Compose.

```bash
cd infrastructure
cp .env.example .env
```

Generate a 64-character hexadecimal JWT secret and place it after `JWT_SECRET=`
inside `.env`:

```bash
openssl rand -hex 32
```

PowerShell alternative:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
($bytes | ForEach-Object { $_.ToString("x2") }) -join ""
$rng.Dispose()
```

Then start the complete application:

```bash
docker compose up --build -d
docker compose ps
```

Open:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

The first startup creates a fresh PostgreSQL schema and inserts the supported
sports and Cairo/Giza regions through Flyway.

Follow backend logs:

```bash
docker compose logs -f backend
```

Stop without deleting PostgreSQL or Redis data:

```bash
docker compose down
```

The `.env` file is ignored by Git. The checked-in `.env.example` contains names
and safe local defaults, never a real signing key.

## License

MIT
