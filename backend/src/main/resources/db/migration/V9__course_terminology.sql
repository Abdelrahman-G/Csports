-- Keep earlier migrations immutable so existing databases upgrade in place.
ALTER TABLE training_session RENAME TO training_course;
ALTER TABLE training_session_days RENAME TO training_course_days;
ALTER TABLE training_course_days RENAME COLUMN session_id TO course_id;
ALTER TABLE booking RENAME COLUMN session_id TO course_id;
ALTER TABLE user_notification RENAME COLUMN session_id TO course_id;

ALTER TABLE user_notification DROP CONSTRAINT chk_user_notification_type;
UPDATE user_notification SET type = 'COURSE_UPDATED' WHERE type = 'SESSION_UPDATED';
UPDATE user_notification SET type = 'COURSE_CANCELLED' WHERE type = 'SESSION_CANCELLED';
ALTER TABLE user_notification ADD CONSTRAINT chk_user_notification_type
    CHECK (type IN ('COURSE_UPDATED', 'COURSE_CANCELLED'));

ALTER TABLE training_course RENAME CONSTRAINT fk_training_session_trainer TO fk_training_course_trainer;
ALTER TABLE training_course RENAME CONSTRAINT fk_training_session_sport TO fk_training_course_sport;
ALTER TABLE training_course RENAME CONSTRAINT fk_training_session_region TO fk_training_course_region;
ALTER TABLE training_course RENAME CONSTRAINT chk_training_session_status TO chk_training_course_status;
ALTER TABLE training_course RENAME CONSTRAINT chk_training_session_participant_count TO chk_training_course_participant_count;
ALTER TABLE training_course_days RENAME CONSTRAINT fk_training_session_days_session TO fk_training_course_days_course;
ALTER TABLE booking RENAME CONSTRAINT fk_booking_session TO fk_booking_course;

ALTER INDEX idx_training_session_status_start_date RENAME TO idx_training_course_status_start_date;
ALTER INDEX idx_training_session_status_end_start RENAME TO idx_training_course_status_end_start;
ALTER INDEX idx_training_session_sport_status_start RENAME TO idx_training_course_sport_status_start;
ALTER INDEX idx_training_session_region_status_start RENAME TO idx_training_course_region_status_start;
ALTER INDEX idx_training_session_trainer_start RENAME TO idx_training_course_trainer_start;
ALTER INDEX idx_training_session_status_price RENAME TO idx_training_course_status_price;
ALTER INDEX uq_booking_confirmed_user_session RENAME TO uq_booking_confirmed_user_course;
