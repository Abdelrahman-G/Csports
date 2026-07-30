# Booking lifecycle and concurrency

## Endpoints

All booking endpoints require a valid access token for an account with the
`USER` role.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/bookings/{sessionId}` | Create a confirmed booking |
| `GET` | `/api/v1/bookings/me` | List the authenticated user's bookings |
| `DELETE` | `/api/v1/bookings/{sessionId}` | Cancel the user's confirmed booking |

Creating and cancelling a booking do not need a JSON request body. The session
ID identifies the session and the access token identifies the user.

The list endpoint accepts:

| Parameter | Values | Default |
|---|---|---|
| `view` | `UPCOMING`, `HISTORY`, `ALL` | `UPCOMING` |
| `status` | `CONFIRMED`, `CANCELLED_BY_USER`, `CANCELLED_BY_TRAINER` | any |
| `page` | zero or greater | `0` |
| `size` | 1 through 100 | `10` |

Examples:

```http
GET /api/v1/bookings/me
GET /api/v1/bookings/me?view=HISTORY
GET /api/v1/bookings/me?view=HISTORY&status=CANCELLED_BY_USER
GET /api/v1/bookings/me?view=ALL&page=0&size=20
```

`UPCOMING` contains confirmed bookings for scheduled session series that have
not ended. `HISTORY` contains cancelled bookings and bookings whose session was
cancelled, completed, or ended. `ALL` applies no lifecycle filter.

The old `GET /api/v1/users/sessions` endpoint remains temporarily available for
existing clients. It delegates to the upcoming-bookings query and is deprecated
for new clients.

## Lifecycle rules

| Action | Result |
|---|---|
| Book a scheduled session before its first occurrence | A `CONFIRMED` booking is created and the participant count increases |
| Book the trainer's own session | Rejected |
| Book a cancelled, completed, full, or already-started session | Rejected |
| Book the same session twice | Rejected |
| Cancel an active booking before the session series finishes | Status becomes `CANCELLED_BY_USER`, `cancelledAt` is recorded, and the count decreases |
| Trainer cancels the session | Active bookings become `CANCELLED_BY_TRAINER` with the same cancellation time |
| Trainer restores the session | Old bookings stay historical; users must book again |
| User books again after cancellation and restoration | A new booking row is created; the old row remains as history |

There is no separate `COMPLETED` booking status. A confirmed booking moves into
the history view when its parent session becomes `COMPLETED` or its date range
ends. This keeps booking state (confirmed or cancelled) separate from session
state.

Every response contains both booking data and display-ready session data. The
frontend receives `bookingStatus`, timestamps, session/trainer/sport/region
labels, seat counts, `bookingClosesAt`, `bookingOpen`, and session status. It
does not need extra calls merely to render a booking card.

## Last-seat Redis lock

The lock key is scoped to one session:

```text
csports:v1:lock:booking:session:{sessionId}
```

Requests for different sessions do not block each other. Requests competing
for the same session follow this flow:

```text
HTTP request
  -> SET lock-key random-owner NX PX lease-time
  -> PostgreSQL transaction
       -> load user and session
       -> validate lifecycle, duplicate booking, and capacity
       -> increment currentParticipants
       -> insert booking
       -> flush and commit optimistic-version/constraint checks
  -> compare owner and delete lock with a Lua script
  -> clear cached session-search pages
  -> HTTP response
```

`SET ... NX` creates the key only when it does not already exist. The expiry
prevents a dead lock if the process stops unexpectedly. Each owner uses a
random token. Unlocking uses one atomic Lua operation that deletes the key only
when the token still belongs to that request, so an expired request cannot
delete a newer request's lock.

The PostgreSQL work lives in a separate transactional service. This detail is
important: Spring commits the transaction before control returns to the outer
service and releases the Redis lock. If the lock were released before commit,
the next request could read the old participant count.

Configuration can be overridden without code changes:

| Environment variable | Default | Meaning |
|---|---|---|
| `BOOKING_LOCK_ENABLED` | `true` | Enable Redis coordination |
| `BOOKING_LOCK_WAIT_TIME` | `2s` | How long a request waits to acquire the lock |
| `BOOKING_LOCK_LEASE_TIME` | `10s` | Automatic key expiry |
| `BOOKING_LOCK_RETRY_INTERVAL` | `50ms` | Delay between acquisition attempts |

If the lock stays busy beyond the wait time, the API returns `409 Conflict` and
the client can retry. If Redis itself is unavailable, the operation logs a
warning and continues using PostgreSQL protections instead of making Redis a
single point of failure.

## Why Redis does not replace optimistic locking

Redis locking and JPA `@Version` solve related but different problems:

- Redis serializes cooperating application requests before they modify the
  same session. This avoids unnecessary failed work and usually lets the second
  last-seat request receive a clear "session full" response.
- `@Version` detects a conflicting database update at commit time. It still
  protects the data if Redis is unavailable, a lock expires, or another code
  path forgets to use the lock.
- Database constraints reject impossible data even when an application bug or
  direct SQL write bypasses both mechanisms.

Keeping all three layers is stronger than choosing Redis instead of versioning.
The current single-Redis-instance lock is appropriate for the project's Docker
Compose architecture. If Redis later becomes a clustered production service,
its locking availability model should be reviewed as an infrastructure
decision.

## Database integrity

Flyway migration `V5__booking_lifecycle_integrity.sql` adds:

- `booking.cancelled_at`, so new cancellation history is auditable (older
  cancelled rows created before V5 can legitimately contain `NULL`);
- an active-booking marker and unique index, allowing many historical rows but
  only one confirmed booking per user/session pair;
- a check constraint that keeps the marker synchronized with booking status;
- a booking-history index for user/status/time queries;
- a participant-count constraint requiring
  `0 <= currentParticipants <= maxParticipants` and `maxParticipants > 0`.

The active marker is nullable because SQL unique indexes allow multiple `NULL`
values. A confirmed booking stores `1`; every cancelled historical row stores
`NULL`. Therefore one active row is unique while repeated cancel-and-rebook
history remains valid.

The participant constraint prevents negative counts and over-capacity values.
It cannot prove that the counter equals the number of confirmed rows; the
transactional service maintains that relationship, while the Redis lock and
optimistic version prevent concurrent lost updates.
