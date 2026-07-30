# Testing strategy

## Fast tests

`./mvnw clean test` runs the normal suite with H2. It verifies application
wiring, exception handling, Redis serialization, and route registration without
requiring Docker. Booking tests also cover cancellation/history/rebooking, lock
contention, and the safe PostgreSQL fallback when Redis is unavailable.

## Infrastructure integration test

`./mvnw clean verify -Pintegration-tests` starts disposable PostgreSQL 17 and
Redis 7.4 containers. It verifies:

- the real PostgreSQL Flyway migration succeeds;
- Hibernate validates the migrated schema;
- Spring connects to Redis and receives `PONG`;
- a typed session-search page survives a real Redis write/read round trip;
- two users racing for one seat are serialized by the Redis lock;
- exactly one last-seat booking and participant increment are committed;
- PostgreSQL's unique index rejects a second confirmed booking even when a
  write bypasses the normal service checks.

The class name ends with `IT`, so Maven Surefire does not run it in the fast
suite. Maven Failsafe runs it only when the integration profile is selected.

Testcontainers uses random host ports and temporary storage. It does not connect
to the normal Docker Compose database or Redis instance.
