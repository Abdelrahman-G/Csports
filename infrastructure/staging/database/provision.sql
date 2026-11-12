\set ON_ERROR_STOP on
BEGIN;

DO $$
BEGIN
    IF current_user IN ('csports_app', 'csports_migrator') THEN
        RAISE EXCEPTION 'Provisioning requires the database administrator';
    END IF;
END $$;

SELECT 'CREATE ROLE csports_app LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'csports_app')
\gexec
SELECT 'CREATE ROLE csports_migrator LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'csports_migrator')
\gexec

ALTER ROLE csports_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT;
ALTER ROLE csports_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT;
REVOKE csports_migrator FROM csports_app;
SELECT format('ALTER ROLE csports_app PASSWORD %L', :'app_password')
\gexec
SELECT format('ALTER ROLE csports_migrator PASSWORD %L', :'migration_password')
\gexec

SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', current_database())
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO csports_app, csports_migrator', current_database())
\gexec
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO csports_app, csports_migrator;
GRANT CREATE ON SCHEMA public TO csports_migrator;

-- PostGIS provisioning stays with the administrator, outside ordinary migrations.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Adopt existing application tables without changing PostGIS-owned objects or data.
SELECT format('ALTER TABLE %I.%I OWNER TO csports_migrator', n.nspname, c.relname)
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
  AND c.relowner <> 'csports_migrator'::regrole
  AND NOT EXISTS (
      SELECT FROM pg_depend d
      WHERE d.classid = 'pg_class'::regclass AND d.objid = c.oid AND d.deptype = 'e'
  )
\gexec

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM csports_app;
SELECT format('GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE %I.%I TO csports_app', n.nspname, c.relname)
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
  AND c.relowner = 'csports_migrator'::regrole
  AND c.relname <> 'flyway_schema_history'
\gexec
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO csports_app;

-- Future migration-created tables and generated IDs need the same runtime access.
ALTER DEFAULT PRIVILEGES FOR ROLE csports_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO csports_app;
ALTER DEFAULT PRIVILEGES FOR ROLE csports_migrator IN SCHEMA public
    GRANT USAGE ON SEQUENCES TO csports_app;
COMMIT;
