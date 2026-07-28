# Architecture overview

Csports is a modular monolith: one deployable Spring Boot application with
clear internal feature boundaries.

```text
HTTP client
    |
    v
Spring MVC controllers
    |
    v
Feature services -----> Redis (cache and token blacklist)
    |
    v
Spring Data repositories
    |
    v
PostgreSQL <--------- Flyway migrations
```

## Package boundaries

- `auth`: registration, login, refresh tokens, and logout
- `user`: user identity and user-owned queries
- `trainer`: trainer profile behavior
- `sport`: sport catalog
- `session`: training session lifecycle
- `booking`: session booking behavior
- `location`: regions and user locations
- `security`: JWT parsing and Spring Security configuration
- `infrastructure.redis`: Redis-specific configuration and operations
- `common`: shared errors, pagination, web constants, and cross-cutting config

Feature packages may depend on another feature's public service, DTO, or model
when the domain relationship requires it. Infrastructure details stay in an
infrastructure package so Kafka, Elasticsearch, or another adapter can be added
without turning the whole project into layer-based folders again.

## Why not microservices yet?

Microservices add network calls, distributed transactions, deployments,
observability, and failure modes. Those are valuable topics, but splitting
before the business boundaries are stable makes feature development slower.
The modular monolith makes boundaries visible now and leaves future extraction
possible if a real learning goal calls for it.
