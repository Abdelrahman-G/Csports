# Account and trainer profiles

## Current account

Every authenticated role can retrieve its private account:

```http
GET /api/v1/users/me
Authorization: Bearer <access-token>
```

The response contains account identity and location information:

```json
{
  "id": 6,
  "name": "Profile User",
  "email": "user@example.com",
  "phoneNumber": "+201000000000",
  "age": 25,
  "role": "USER",
  "photoUrl": null,
  "regionId": 1,
  "regionName": "Nasr City",
  "city": "Cairo",
  "country": "Egypt",
  "latitude": 30.0581,
  "longitude": 31.3302
}
```

Passwords are never included. `GET /api/v1/auth/me` remains as a deprecated
alias so existing clients do not break.

## Partial account update

```http
PATCH /api/v1/users/me
Authorization: Bearer <access-token>
Content-Type: application/json
```

Only supplied fields change:

```json
{
  "name": "Updated Name",
  "regionId": 4,
  "latitude": 30.0285,
  "longitude": 31.4913
}
```

Editable fields are `name`, `email`, `phoneNumber`, `age`, `regionId`,
`latitude`, and `longitude`.

- Role and password cannot be changed through this endpoint.
- Email and phone number must remain unique.
- Coordinates must remain inside the configured Greater Cairo/Giza service
  area.
- User accounts must remain at least 13 years old; trainer accounts must
  remain at least 18.
- An empty update returns `400 BUSINESS_RULE_VIOLATION`.

## Trainer profile

Anyone can retrieve the public portion of a trainer profile:

```http
GET /api/v1/trainers/{trainerId}
```

The public response contains name, optional photo URL, bio, experience, sport,
and named region. It deliberately excludes email, phone number, age, and exact
coordinates.

A trainer retrieves or partially updates their own trainer-specific fields:

```http
GET /api/v1/trainers/me
Authorization: Bearer <trainer-access-token>
```

```http
PATCH /api/v1/trainers/me
Authorization: Bearer <trainer-access-token>
Content-Type: application/json
```

```json
{
  "bio": "Swimming coach focused on intermediate athletes.",
  "experienceYears": 6
}
```

Sport is intentionally not editable yet. Existing training sessions retain
their own sport, so changing a trainer's sport requires a separate domain rule
and migration/API decision rather than an implicit profile update.
