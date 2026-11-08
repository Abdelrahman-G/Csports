# CSports decisions and deferred features

Updated 8 November 2026. This is a project reference for future discussions, not authorization to implement the deferred items. Features have not been prioritised or estimated.

## Agreed behavior

- Keep the Saturday–Friday personal calendar and Cairo time calculations.
- Keep overnight occurrences visible on both occupied dates. A Friday 23:00–Saturday 00:30 occurrence is one training session displayed in two calendar columns.
- Automatically consider an eligible training occurrence attended once its end time passes. A booking or course cancelled before that occurrence starts excludes it. Cancelling later occurrences does not erase past attendance; an occurrence already started before cancellation remains eligible under the agreed calendar rule.
- Use professional attendance wording. No "presumed attended" text in the interface. Automatic attendance is a business rule, distinct from verified check-in.
- Preserve valuable business records and history. Prefer cancellation/archive/soft deletion over physically removing courses, bookings, attendance, announcements, and future financial records. Expiring cache entries, lock keys, and temporary credentials is a separate concern.
- Explicitly configure the Hikari connection-pool default as READ_COMMITTED in the shared application.properties (added 8 November 2026). PostgreSQL integration coverage checks the Spring-managed connection against a server configured with a different default; run it with the integration-tests Maven profile.

## Deferred features

### Trainee sports analytics

- Total attended training sessions, counts by sport, and total sports hours.
- Useful time ranges and weekly/monthly trends are possible additions, not settled requirements.
- Count an overnight occurrence once, regardless of how many calendar columns display it. Apply the automatic-attendance and cancellation rules consistently.
- Before implementation, settle overlapping-session hours (sum of session durations versus unique elapsed time) and how historical attendance/duration is preserved as other editing features evolve. Automatic totals must not be described as independently measured activity.

### Trainer announcements and notifications

- Add trainer-authored announcements on the course page, delivered to trainees enrolled in that course through the existing in-app notification system.
- Examples: cancellation notices, schedule updates, and general course information.
- Existing automatic cancellation/location-change notifications are not a general announcement feed.
- Recipient eligibility, delivery timing, edit/delete history, and permission rules need discussion. This feature does not implicitly relax the current course-edit deadline.

### Cancellation fees and payments

- Discuss cancellation cutoffs, fees, and refunds as a future task.
- Payment collection/gateway integration is a prerequisite for actually charging trainees; no gateway exists currently.
- Fee amounts, currency, whether fees apply to a whole course or an occurrence, and exceptions are undecided.

### Retention and soft-deletion review

- Audit deletion paths, JPA cascade/orphan-removal mappings, and historical links before claiming a global soft-deletion guarantee.
- Current course deletion delegates to cancellation; booking cancellation changes status and records its time.
- Define archive/soft-delete behavior separately from cancellation. Hiding a record must not destroy the data needed for history, analytics, notifications, or financial records.

### Earlier deferred work

- Open-ended training courses: end date optional; details remain unsettled.
- Booking-time overlap warning with the option to proceed; overlaps remain allowed.
- Trainer usability improvements, including the previously discussed copying/duplication idea; requirements need recovery/discussion before implementation.
- Dedicated mobile experience / possible mobile app discussion.
- Optional attendance confirmation after training; automatic attendance is the currently agreed default.

## Engineering proposals, not implemented or approved changes

- Review calendar query projection and date filtering, then measure query plans before adding indexes. Existing booking indexes already begin with user_id.
- Keep firstOccurrence and lastOccurrence as descriptive entry points. A common private directional helper is an option if it improves readability; no flag-based refactor is agreed.
- Review Redis memory limits and policy with awareness that the instance stores locks/security keys as well as disposable cached results. Current application cache TTLs: session search 5 minutes, sports 24 hours, other default caches 1 hour.
- Discuss capped exponential backoff with jitter for booking-lock acquisition if contention warrants it. Keep the total wait budget bounded; no retry-policy change is agreed or implemented yet.

## Current work reference

- Personal calendar: branch feature/my-schedule, PR https://github.com/Abdelrahman-G/Csports/pull/29 targeting develop.
- Relevant code: SessionSchedule, BookingScheduleService, BookingService, BookingTransactionService, RedisSessionBookingLock, RedisConfig, TrainingSessionLifecycleJob, and frontend/src/pages/MySchedulePage.tsx.
