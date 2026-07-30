ALTER TABLE booking
    ADD COLUMN cancelled_at TIMESTAMP(6) WITHOUT TIME ZONE;

-- This nullable marker gives us the semantics of a partial unique index while
-- remaining compatible with both PostgreSQL and the fast H2 migration suite.
-- SQL unique indexes allow multiple NULL values but only one value of 1.
ALTER TABLE booking
    ADD COLUMN active_marker SMALLINT DEFAULT 1;

UPDATE booking
SET active_marker = CASE
    WHEN status = 'CONFIRMED' THEN 1
    ELSE NULL
END;

ALTER TABLE booking
    ADD CONSTRAINT chk_booking_active_marker
    CHECK (
        (status = 'CONFIRMED' AND active_marker = 1)
        OR
        (status <> 'CONFIRMED' AND active_marker IS NULL)
    );

CREATE UNIQUE INDEX uq_booking_confirmed_user_session
    ON booking (user_id, session_id, active_marker);

DROP INDEX idx_booking_user_status;

CREATE INDEX idx_booking_user_status_booked_at
    ON booking (user_id, status, booked_at DESC);

ALTER TABLE training_session
    ADD CONSTRAINT chk_training_session_participant_count
    CHECK (
        current_participants >= 0
        AND max_participants > 0
        AND current_participants <= max_participants
    );
