ALTER TABLE training_session
    ADD COLUMN region_id BIGINT;

-- Existing sessions predate an explicit session region. Use the closest stored
-- region center once for migration compatibility. New writes always provide a
-- region id explicitly and do not use this approximation.
UPDATE training_session session_to_backfill
SET region_id = (
    SELECT region.id
    FROM region
    ORDER BY
        POWER(region.latitude - session_to_backfill.latitude, 2)
        + POWER(region.longitude - session_to_backfill.longitude, 2)
    FETCH FIRST 1 ROW ONLY
)
WHERE session_to_backfill.region_id IS NULL;

ALTER TABLE training_session
    ALTER COLUMN region_id SET NOT NULL;

ALTER TABLE training_session
    ADD CONSTRAINT fk_training_session_region
    FOREIGN KEY (region_id) REFERENCES region (id);

-- The public search always fixes status and searches a date interval.
CREATE INDEX idx_training_session_status_end_start
    ON training_session (status, end_date, start_date);

-- These composite indexes support the most common filtered lists while keeping
-- the default chronological ordering cheap.
CREATE INDEX idx_training_session_sport_status_start
    ON training_session (sport_id, status, start_date);

CREATE INDEX idx_training_session_region_status_start
    ON training_session (region_id, status, start_date);

CREATE INDEX idx_training_session_trainer_start
    ON training_session (trainer_id, start_date);

CREATE INDEX idx_training_session_status_price
    ON training_session (status, price);
