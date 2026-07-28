# API versioning

The canonical API prefix is `/api/v1`.

Versioning is useful when an incompatible contract change is necessary, such
as renaming a required field, changing response structure, or changing the
meaning of an operation. Existing consumers can remain on v1 while a new
consumer adopts v2.

Versioning is not needed for every additive change. Adding an optional field or
a new endpoint can normally remain within v1.

## Compatibility period

The original routes, such as `/sports/list`, are still registered. New clients
should use `/api/v1/sports/list`. Both currently call the same controller method
and therefore have identical behavior.

Before removing legacy routes:

1. update Postman and the future React client;
2. confirm no known consumer still uses them;
3. announce the breaking change;
4. remove them in one deliberate release.

## Generated documentation

The application generates OpenAPI from live Spring controllers:

- JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`

Postman can import the JSON URL. Re-importing or refreshing the API definition
keeps it aligned with controller and DTO changes without maintaining a separate
handwritten endpoint inventory.
