# Postman test: session lifecycle

These examples use dates valid on July 29, 2026. If they are in the past when
testing, replace them with future dates and make sure every value in `days`
matches at least one weekday inside the chosen date range.

Create these Postman variables:

```text
baseUrl = http://localhost:8080/api/v1
trainerToken
userToken
sessionId
notificationId
```

For authenticated requests use:

```text
Authorization: Bearer {{trainerToken}}
```

or:

```text
Authorization: Bearer {{userToken}}
```

## 1. Discover registration IDs

```http
GET {{baseUrl}}/sports/list
GET {{baseUrl}}/regions
```

Choose a `sportId` and `regionId`.

## 2. Register and log in a trainer

```http
POST {{baseUrl}}/auth/register/trainer
Content-Type: application/json
```

```json
{
  "name": "Lifecycle Trainer",
  "email": "lifecycle.trainer@example.com",
  "phoneNumber": "+201000000101",
  "password": "Password123!",
  "age": 30,
  "bio": "Lifecycle test trainer",
  "experienceYears": 5,
  "sportId": 1,
  "regionId": 1,
  "latitude": 30.0581,
  "longitude": 31.3302
}
```

Then:

```http
POST {{baseUrl}}/auth/login
```

```json
{
  "identifier": "lifecycle.trainer@example.com",
  "password": "Password123!"
}
```

Save `accessToken` as `trainerToken`.

## 3. Register and log in a user

```http
POST {{baseUrl}}/auth/register/user
```

```json
{
  "name": "Lifecycle User",
  "email": "lifecycle.user@example.com",
  "phoneNumber": "+201000000102",
  "password": "Password123!",
  "age": 25,
  "regionId": 1,
  "latitude": 30.0581,
  "longitude": 31.3302
}
```

Log in using the same login endpoint and save `accessToken` as `userToken`.

## 4. Trainer creates a session

```http
POST {{baseUrl}}/sessions
Authorization: Bearer {{trainerToken}}
Content-Type: application/json
```

```json
{
  "title": "Monday football training",
  "description": "Lifecycle test session",
  "locationName": "Nasr City Club",
  "latitude": 30.0581,
  "longitude": 31.3302,
  "startDate": "2026-08-10",
  "endDate": "2026-08-17",
  "startTime": "18:00:00",
  "durationMinutes": 90,
  "days": ["MONDAY"],
  "maxParticipants": 3,
  "price": 150
}
```

Expected: `201 Created`, `status` is `SCHEDULED`, and price is `150`.
Save the returned `id` as `sessionId`.

## 5. User books the session

```http
POST {{baseUrl}}/bookings/{{sessionId}}
Authorization: Bearer {{userToken}}
```

Expected: `200`. Repeating it should return `409`.

Verify:

```http
GET {{baseUrl}}/bookings/me
Authorization: Bearer {{userToken}}
```

## 6. Trainer changes the location

```http
PATCH {{baseUrl}}/sessions/{{sessionId}}
Authorization: Bearer {{trainerToken}}
Content-Type: application/json
```

```json
{
  "reason": "The original field is undergoing maintenance",
  "locationName": "New Cairo Sports Club",
  "latitude": 30.0285,
  "longitude": 31.4913
}
```

Expected: `200`. Fields not included in the request keep their existing values.

## 7. User verifies the location notification

```http
GET {{baseUrl}}/notifications
Authorization: Bearer {{userToken}}
```

Expected: a `SESSION_UPDATED` notification containing the trainer's reason.
Save its `id` as `notificationId`.

```http
PATCH {{baseUrl}}/notifications/{{notificationId}}/read
Authorization: Bearer {{userToken}}
```

Expected: `"read": true`.

## 8. Verify that price is immutable

Repeat the update request but change:

```json
"price": 200
```

Expected: `409 RESOURCE_CONFLICT` and the stored price remains `150`.

## 9. Verify capacity protection

Repeat the valid update but change:

```json
"maxParticipants": 0
```

Expected: `400 VALIDATION_FAILED`.

After at least two users are actively booked, trying `maxParticipants: 1`
returns `409`, because capacity cannot be lower than the active booking count.

## 10. Trainer cancels the session

```http
PATCH {{baseUrl}}/sessions/{{sessionId}}/cancel
Authorization: Bearer {{trainerToken}}
Content-Type: application/json
```

```json
{
  "reason": "The trainer has a medical emergency"
}
```

Expected: `200`.

Verify:

```http
GET {{baseUrl}}/sessions/{{sessionId}}
GET {{baseUrl}}/bookings/me
Authorization: Bearer {{userToken}}
GET {{baseUrl}}/notifications
Authorization: Bearer {{userToken}}
```

Expected:

- Session status is `CANCELLED`.
- `currentParticipants` is `0`.
- `cancellationReason` contains the submitted reason.
- The session is absent from the user's active bookings.
- The user has a `SESSION_CANCELLED` notification with the reason.

## 11. Trainer restores the session

```http
PATCH {{baseUrl}}/sessions/{{sessionId}}/restore
Authorization: Bearer {{trainerToken}}
```

Expected: `200`.

Verify:

```http
GET {{baseUrl}}/sessions/{{sessionId}}
GET {{baseUrl}}/sessions
```

Expected:

- Status returns to `SCHEDULED`.
- `cancelledAt` and `cancellationReason` are `null`.
- `currentParticipants` remains `0`.
- The session is publicly available again.
- The user's old booking is not restored.

The user can now call the booking endpoint again to create a new active
booking.

## Important negative tests

| Test | Expected result |
|---|---|
| Update when 48 hours or less remain | `409 RESOURCE_CONFLICT` |
| Cancel with bookings on the next occurrence's day | `409 RESOURCE_CONFLICT` |
| Cancel without a reason | `400 VALIDATION_FAILED` |
| Update without a reason | `400 VALIDATION_FAILED` |
| Update/cancel another trainer's session | `403 ACCESS_DENIED` |
| Restore a scheduled session | `409 RESOURCE_CONFLICT` |
| Restore after its first occurrence | `409 RESOURCE_CONFLICT` |
| Book a cancelled/completed/full session | `409 RESOURCE_CONFLICT` |

The “cancel after start date with no bookings” and automatic completion rules
are easiest to verify through automated tests because immediate Postman testing
would otherwise require waiting or changing database dates.
