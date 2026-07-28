# Local infrastructure

This Compose project runs the dependencies used by the Spring application:

- PostgreSQL/PostGIS 17 on port `5432`
- Redis 7.4 on port `6379`

It does not currently run the Spring backend itself.

## Start

```bash
docker compose up -d
docker compose ps
```

The checked-in defaults match the backend `dev` profile. To customize them:

```bash
cp .env.example .env
docker compose up -d
```

On PowerShell, use `Copy-Item .env.example .env`.

## Stop without deleting data

```bash
docker compose stop
```

or:

```bash
docker compose down
```

`docker compose down` removes the Compose containers and network, but preserves
the named PostgreSQL volume unless `--volumes` is explicitly added.

## Important data warning

Do not run `docker compose down --volumes` unless the local PostgreSQL data is
intentionally disposable. That option deletes the named database volume.

## Diagnostics

```bash
docker compose ps
docker compose logs postgres
docker compose logs redis
docker compose exec postgres pg_isready -U postgres -d csports
docker compose exec redis redis-cli ping
```
