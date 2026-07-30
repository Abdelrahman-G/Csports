# Authorization matrix

Csports uses two security layers:

1. `SecurityConfig` decides whether a route is public or requires authentication.
2. `@PreAuthorize` on controller methods enforces the required role.

Ownership is a third business-level check. For example, having the `TRAINER`
role is not enough to update any session; the authenticated trainer must also
be the trainer who created that session.

| Capability | Public | USER | TRAINER | ADMIN | Ownership rule |
|---|---:|---:|---:|---:|---|
| Register and log in | Yes | Yes | Yes | Yes | None |
| Refresh an access token | Yes | Yes | Yes | Yes | Valid refresh token |
| Log out / view or update current account | No | Yes | Yes | Yes | Current account |
| List sports and regions | Yes | Yes | Yes | Yes | None |
| View a public trainer profile | Yes | Yes | Yes | Yes | None |
| View/update own trainer profile | No | No | Yes | No | Current trainer profile |
| Add a sport | No | No | No | Yes | None |
| List and view training sessions | Yes | Yes | Yes | Yes | None |
| Create a training session | No | No | Yes | No | Current trainer profile |
| Update/cancel/restore a training session | No | No | Yes | No | Session creator only |
| View session participants | No | No | Yes | No | Session creator only |
| Book/cancel a booking | No | Yes | No | No | Current user only |
| View own bookings | No | Yes | No | No | Current user only |
| View/mark notifications | No | Yes | Yes | Yes | Recipient only |
| Development Redis endpoints | No | No | No | No | Development profile only; authenticated |

`ADMIN` is intentionally not treated as a trainer. Administrative session
moderation should be added later as an explicit use case with its own endpoint
and audit trail, instead of silently letting an administrator impersonate a
trainer.

## Expected failures

- No token or an invalid/expired/revoked token returns `401`.
- A valid token with the wrong role returns `403`.
- A trainer trying to manage another trainer's session returns `403`.
- A user requesting another user's notification receives `404`, so the API
  does not reveal whether that notification exists.
