\set ON_ERROR_STOP on

DO $$
DECLARE
    successful_migrations integer;
    final_version text;
BEGIN
    SELECT count(*), max(version)
    INTO successful_migrations, final_version
    FROM flyway_schema_history
    WHERE success = true;
    IF successful_migrations <> 7 OR final_version <> '7' THEN
        RAISE EXCEPTION 'P3-H Flyway V7 verification failed';
    END IF;
END
$$;

ALTER ROLE p3h_app_readonly SET default_transaction_read_only = on;
ALTER ROLE p3h_backup_reader SET default_transaction_read_only = on;

REVOKE ALL ON DATABASE trade_model_v1_p3h_primary FROM p3h_app_readonly;
REVOKE ALL ON DATABASE trade_model_v1_p3h_primary FROM p3h_backup_reader;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_primary TO p3h_app_readonly;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_primary TO p3h_backup_reader;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM p3h_app_readonly;
REVOKE ALL ON SCHEMA public FROM p3h_backup_reader;
GRANT USAGE ON SCHEMA public TO p3h_app_readonly;
GRANT USAGE ON SCHEMA public TO p3h_backup_reader;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO p3h_app_readonly;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO p3h_app_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO p3h_backup_reader;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO p3h_backup_reader;

ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT SELECT ON TABLES TO p3h_app_readonly;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO p3h_app_readonly;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT SELECT ON TABLES TO p3h_backup_reader;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO p3h_backup_reader;

DO $$
DECLARE
    unsafe_grant_count integer;
BEGIN
    SELECT count(*) INTO unsafe_grant_count
    FROM information_schema.role_table_grants
    WHERE grantee IN ('p3h_app_readonly', 'p3h_backup_reader')
      AND privilege_type IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'TRIGGER', 'REFERENCES');
    IF unsafe_grant_count <> 0 THEN
        RAISE EXCEPTION 'P3-H read-only grant verification failed';
    END IF;
END
$$;
