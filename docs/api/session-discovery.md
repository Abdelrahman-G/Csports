# Session discovery

## Endpoint

Session discovery is public:

```http
GET /api/v1/sessions
```

Every filter is optional and filters can be combined:

```http
GET /api/v1/sessions?q=swimming&sportId=2&trainerId=7&regionId=1&fromDate=2026-08-01&toDate=2026-12-31&minPrice=100&maxPrice=800&availableOnly=true&sortBy=startDate&direction=asc&page=0&size=10
```

| Parameter | Meaning | Default |
|---|---|---|
| `q` | Case-insensitive text contained in title, description, or location name | none |
| `sportId` | Exact sport | none |
| `trainerId` | Exact trainer account ID | none |
| `regionId` | Exact session region | none |
| `fromDate` | Session series must end on or after this date | today |
| `toDate` | Session series must start on or before this date | none |
| `minPrice` | Inclusive minimum price | none |
| `maxPrice` | Inclusive maximum price | none |
| `availableOnly` | Exclude sessions with no remaining seats | `false` |
| `sortBy` | `startDate`, `price`, or `createdAt` | `startDate` |
| `direction` | `asc` or `desc` | `asc` |
| `page` | Zero-based page | `0` |
| `size` | Items per page, from 1 to 100 | `10` |

Date filtering uses interval overlap. For example, a series running from
August through December matches an October search even though it did not start
in October.

Only `SCHEDULED` series that have not ended are returned. A full series remains
scheduled but is excluded when `availableOnly=true`.

## Region ownership

A session has its own `regionId`. It does not inherit the trainer's region
because trainers may teach outside their home region.

The client obtains valid IDs and labels from:

```http
GET /api/v1/regions
```

The trainer sends the selected `regionId` while creating or moving a session.
The backend validates the ID and returns both machine-readable and
display-ready values:

```json
{
  "regionId": 1,
  "regionName": "Nasr City",
  "city": "Cairo",
  "country": "Egypt"
}
```

React does not need to replace the region ID itself. It can use the returned
labels directly. It will normally keep the `/regions` response for dropdowns
and use the ID only as the selected value or search parameter.

## Redis flow

1. `SessionSearchRequest` normalizes defaults and validates ranges.
2. The normalized request is converted into a SHA-256 cache key.
3. Spring checks the `session-search` Redis cache.
4. A cache hit returns the typed `PageResponse<TrainingSessionResponse>`
   without executing a session search query.
5. A cache miss executes the JPA specification and stores the mapped page for
   two minutes.

The cache uses an explicit generic serializer so nested content is restored as
`TrainingSessionResponse`, not `LinkedHashMap`.

Creating, updating, cancelling, or restoring a session clears search pages.
Booking or cancelling a booking also clears them because availability and seat
counts changed. Redis failures use the cache error handler and fall back to
PostgreSQL rather than failing the endpoint.

Search cache clearing uses Redis `SCAN` in batches instead of the blocking
`KEYS` command.

## Database indexes

Flyway migration `V4__session_discovery.sql` adds indexes for:

- status with date-interval filtering;
- sport, status, and chronological order;
- region, status, and chronological order;
- trainer session lists;
- status with price ranges.

Keyword search deliberately remains a case-insensitive database scan for now.
Ordinary B-tree indexes do not accelerate a contains search such as
`%swimming%`. Adding a misleading text index would waste write and storage
costs. Elasticsearch or PostgreSQL full-text/trigram search can later replace
that predicate behind the same HTTP contract.
