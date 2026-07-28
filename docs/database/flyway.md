# Flyway guide

Flyway is version control for database structure and reference data. On startup,
Spring Boot asks Flyway to inspect `flyway_schema_history`, find migrations that
have not run, and apply them in version order.

## Current transition

`V1__initial_schema.sql` represents the schema that Hibernate previously
created with `ddl-auto=update`.

- A new empty database executes V1.
- A non-empty local database without Flyway history is baselined at version 1.
- Production has `baseline-on-migrate=false` so a wrong target database fails
  instead of being silently accepted.

Automatic baselining is a temporary development compatibility measure. After
every active local database contains `flyway_schema_history`, set
`FLYWAY_BASELINE_ON_MIGRATE=false`.

## Adding a database change

1. Never edit a migration that has already run.
2. Add the next file, for example:

   ```text
   V2__add_booking_status.sql
   ```

3. Write both the schema change and any safe data backfill it requires.
4. Update the JPA entity to match.
5. Run unit and integration tests.
6. Review the SQL before starting the application against important data.

Flyway validates migration checksums. Editing an applied file causes startup to
fail, which protects environments from having different meanings for “V1”.

## Hibernate's role

`spring.jpa.hibernate.ddl-auto=validate` means Hibernate compares entity
mappings with the existing schema. It never creates or alters production
tables. If a migration and an entity disagree, startup fails immediately with
a useful schema-validation error.
