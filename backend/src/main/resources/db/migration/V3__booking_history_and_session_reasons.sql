ALTER TABLE booking
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE booking
    ADD CONSTRAINT chk_booking_status
    CHECK (status IN ('CONFIRMED', 'CANCELLED_BY_USER', 'CANCELLED_BY_TRAINER'));

CREATE INDEX idx_booking_user_status
    ON booking (user_id, status);

CREATE INDEX idx_booking_session_status
    ON booking (session_id, status);

ALTER TABLE training_session
    ADD COLUMN last_update_reason VARCHAR(500);

ALTER TABLE training_session
    ADD COLUMN cancellation_reason VARCHAR(500);
