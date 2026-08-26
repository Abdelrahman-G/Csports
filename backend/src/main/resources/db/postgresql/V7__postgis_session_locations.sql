CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE training_session
    ADD COLUMN location GEOGRAPHY(POINT, 4326);

UPDATE training_session
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography;

ALTER TABLE training_session
    ALTER COLUMN location SET NOT NULL;

CREATE FUNCTION sync_training_session_location()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    
    NEW.location := ST_SetSRID(
        ST_MakePoint(NEW.longitude, NEW.latitude),
        4326
    )::geography;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_training_session_location
BEFORE INSERT OR UPDATE OF latitude, longitude
ON training_session
FOR EACH ROW
EXECUTE FUNCTION sync_training_session_location();

CREATE INDEX idx_training_session_location_gist
    ON training_session USING GIST (location);
