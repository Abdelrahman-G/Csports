-- Reference data required by registration and session creation.
--
-- The WHERE NOT EXISTS checks preserve locally added rows when this migration
-- runs against an existing development database. Matching is case-insensitive
-- so an existing value such as "football" is not duplicated as "Football".

INSERT INTO sport (name)
SELECT 'Swimming'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Swimming')
);

INSERT INTO sport (name)
SELECT 'Football'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Football')
);

INSERT INTO sport (name)
SELECT 'Basketball'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Basketball')
);

INSERT INTO sport (name)
SELECT 'Volleyball'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Volleyball')
);

INSERT INTO sport (name)
SELECT 'Tennis'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Tennis')
);

INSERT INTO sport (name)
SELECT 'Padel'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Padel')
);

INSERT INTO sport (name)
SELECT 'Boxing'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Boxing')
);

INSERT INTO sport (name)
SELECT 'Running'
WHERE NOT EXISTS (
    SELECT 1 FROM sport WHERE LOWER(name) = LOWER('Running')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Cairo', 'Nasr City', 30.0581, 31.3302
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('Nasr City')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Cairo', 'Maadi', 29.9602, 31.2569
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('Maadi')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Cairo', 'Heliopolis', 30.0910, 31.3260
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('Heliopolis')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Cairo', 'New Cairo', 30.0285, 31.4913
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('New Cairo')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Giza', '6th of October', 29.9765, 30.9445
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('6th of October')
);

INSERT INTO region (country, city, name, latitude, longitude)
SELECT 'Egypt', 'Giza', 'Hadayek al Ahram', 29.9681637, 31.0795433
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE LOWER(name) = LOWER('Hadayek al Ahram')
);
