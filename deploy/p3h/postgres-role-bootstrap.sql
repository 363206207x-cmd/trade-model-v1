\set ON_ERROR_STOP on

\set migration_password `tr -d '\r\n' </run/secrets/flyway_password`
\set app_password `tr -d '\r\n' </run/secrets/app_database_password_v1`
\set backup_password `tr -d '\r\n' </run/secrets/backup_reader_password`
\set recovery_password `tr -d '\r\n' </run/secrets/recovery_owner_password`

SELECT format(
    'CREATE ROLE p3h_migration_owner LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
    :'migration_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'p3h_migration_owner')
\gexec

SELECT format(
    'CREATE ROLE p3h_app_readonly LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
    :'app_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'p3h_app_readonly')
\gexec

SELECT format(
    'CREATE ROLE p3h_backup_reader LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
    :'backup_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'p3h_backup_reader')
\gexec

SELECT format(
    'CREATE ROLE p3h_recovery_owner LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
    :'recovery_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'p3h_recovery_owner')
\gexec

SELECT format('ALTER ROLE p3h_migration_owner PASSWORD %L', :'migration_password') \gexec
SELECT format('ALTER ROLE p3h_app_readonly PASSWORD %L', :'app_password') \gexec
SELECT format('ALTER ROLE p3h_backup_reader PASSWORD %L', :'backup_password') \gexec
SELECT format('ALTER ROLE p3h_recovery_owner PASSWORD %L', :'recovery_password') \gexec

ALTER ROLE p3h_migration_owner NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
ALTER ROLE p3h_app_readonly NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
ALTER ROLE p3h_backup_reader NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
ALTER ROLE p3h_recovery_owner NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;

ALTER DATABASE trade_model_v1_p3h_primary OWNER TO p3h_migration_owner;

SELECT 'CREATE DATABASE trade_model_v1_p3h_recovery OWNER p3h_recovery_owner'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'trade_model_v1_p3h_recovery')
\gexec

REVOKE CONNECT, TEMPORARY ON DATABASE postgres FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE template1 FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE template0 FROM PUBLIC;
REVOKE ALL ON DATABASE trade_model_v1_p3h_primary FROM PUBLIC;
REVOKE ALL ON DATABASE trade_model_v1_p3h_recovery FROM PUBLIC;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_primary TO p3h_migration_owner;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_recovery TO p3h_recovery_owner;

DO $$
DECLARE
    unsafe_role_count integer;
BEGIN
    SELECT count(*) INTO unsafe_role_count
    FROM pg_roles
    WHERE rolname IN (
        'p3h_migration_owner', 'p3h_app_readonly',
        'p3h_backup_reader', 'p3h_recovery_owner'
    )
      AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolinherit OR rolreplication);
    IF unsafe_role_count <> 0 THEN
        RAISE EXCEPTION 'P3-H role capability verification failed';
    END IF;
END
$$;
