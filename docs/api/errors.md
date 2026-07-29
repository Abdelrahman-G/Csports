# API error contract

All controller, validation, business-rule, and Spring Security failures use the
same JSON shape:

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "error": "Bad Request",
  "message": "The request contains invalid values.",
  "path": "/api/v1/auth/register/user",
  "timestamp": "2026-07-29T14:00:00Z",
  "fieldErrors": {
    "email": "Email must have a valid format",
    "latitude": "Latitude must be within the Cairo and Giza service area"
  }
}
```

`code` is the stable value a future React client should use when it needs
special behavior. `message` is readable text. `fieldErrors` is empty for errors
that do not belong to individual request fields.

| Status | Meaning | Common codes |
|---:|---|---|
| 400 | Invalid JSON, fields, parameters, or a business rule | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `BUSINESS_RULE_VIOLATION` |
| 401 | Authentication is missing or invalid | `AUTHENTICATION_REQUIRED`, `INVALID_CREDENTIALS` |
| 403 | The authenticated account is not allowed | `ACCESS_DENIED` |
| 404 | A resource or endpoint does not exist | `RESOURCE_NOT_FOUND`, `ENDPOINT_NOT_FOUND` |
| 405 | The endpoint does not support the HTTP method | `METHOD_NOT_ALLOWED` |
| 409 | Current data/state conflicts with the request | `RESOURCE_CONFLICT` |
| 415 | Unsupported request content type | `UNSUPPORTED_MEDIA_TYPE` |
| 500 | An unexpected server problem occurred | `INTERNAL_ERROR` |

The current geographic validation box represents the application's supported
Greater Cairo/Giza service area:

- Latitude: `29.75` through `30.35`
- Longitude: `30.75` through `31.75`

The values live in `ServiceArea` so future expansion changes one source of
truth.

