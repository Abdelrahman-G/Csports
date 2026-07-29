# Training session lifecycle

## Meaning of each state

```text
SCHEDULED ──trainer cancellation──> CANCELLED
    │                                  │
    │                                  └──restore before first occurrence──> SCHEDULED
    │
    └──final occurrence ends──> COMPLETED
```

- `SCHEDULED` means the series is active and has not been cancelled or
  finished. It may be open for booking or full.
- `CANCELLED` means the trainer cancelled the series. It is hidden from public
  discovery and its active bookings are cancelled.
- `COMPLETED` means the final selected training occurrence has ended.

Capacity is not a lifecycle state. When `currentParticipants` equals
`maxParticipants`, the session remains `SCHEDULED`, but new booking attempts
return a conflict.

## Operations

- `POST /api/v1/sessions` creates a scheduled session.
- `PATCH /api/v1/sessions/{sessionId}` updates only the supplied
  trainer-editable fields and requires a reason.
- `PATCH /api/v1/sessions/{sessionId}/cancel` cancels a session and requires a
  reason body.
- `PATCH /api/v1/sessions/{sessionId}/restore` restores an eligible cancelled
  session.
- The legacy `DELETE /api/v1/sessions/{sessionId}` performs safe cancellation
  and also requires a reason body.

Only the trainer who created the session may update, cancel, restore, or view
its participants.

## Update rules

- More than 48 hours must remain before the original first occurrence.
- The request always requires a reason.
- Only fields included in the request are changed; omitted fields retain their
  current values.
- Price is immutable. If `price` is supplied for consistency, it must equal the
  original value.
- Capacity may be reduced down to, but not below, the active booking count.
- Schedule and Greater Cairo/Giza coordinate validation still applies.
- Booked users are notified only when `locationName`, latitude, or longitude
  changes. The notification contains the trainer's reason.
- The last update reason is retained on the session.

## Cancellation rules

- A cancellation reason is always required and retained on the session.
- If active bookings exist, the trainer cannot cancel on the next training
  occurrence's calendar day.
- A scheduled session with no active bookings can be cancelled after its
  start date, as long as it has not already transitioned to `COMPLETED`.
- Every active booking becomes `CANCELLED_BY_TRAINER`.
- `currentParticipants` becomes zero.
- Booked users receive a cancellation notification containing the reason.
- Cancelled sessions disappear from public discovery.

Cancelled bookings remain as history, but they are excluded from the user's
active bookings and the trainer's participant list.

## Restore rules

- Only `CANCELLED` sessions can be restored.
- Its first occurrence must still be in the future and its schedule must remain
  valid.
- Status returns to `SCHEDULED`, so it reappears in public discovery.
- Cancellation time and reason are cleared.
- Previously cancelled bookings are not restored. Users must book again.

## Completion

`TrainingSessionLifecycleJob` checks scheduled sessions every minute by default.
After the final selected occurrence's start time plus duration has passed, the
session becomes `COMPLETED`.

The interval can be changed with:

```properties
csports.session-completion-check-interval-ms=60000
```

## Notifications

Successful location changes and cancellations publish a
`SessionChangedEvent`. A transactional listener writes one in-app notification
per actively booked user.

Users retrieve and read notifications with:

- `GET /api/v1/notifications`
- `PATCH /api/v1/notifications/{notificationId}/read`

The event boundary can later be replaced by a transactional outbox and Kafka
without coupling notification persistence directly to session business logic.
