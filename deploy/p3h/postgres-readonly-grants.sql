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
    IF successful_migrations <> 9 OR final_version <> '9' THEN
        RAISE EXCEPTION 'P3-H Flyway V9 verification failed';
    END IF;
END
$$;

ALTER ROLE p3h_app_readonly SET default_transaction_read_only = off;
ALTER ROLE p3h_backup_reader SET default_transaction_read_only = on;

DO $$
DECLARE
    unsafe_membership_count integer;
BEGIN
    SELECT count(*) INTO unsafe_membership_count
    FROM pg_auth_members membership
    JOIN pg_roles member_role ON member_role.oid = membership.member
    WHERE member_role.rolname IN ('p3h_app_readonly', 'p3h_backup_reader');
    IF unsafe_membership_count <> 0 THEN
        RAISE EXCEPTION 'P3-H read-only role membership drift detected';
    END IF;
END
$$;

REVOKE ALL ON DATABASE trade_model_v1_p3h_primary FROM p3h_app_readonly;
REVOKE ALL ON DATABASE trade_model_v1_p3h_primary FROM p3h_backup_reader;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_primary TO p3h_app_readonly;
GRANT CONNECT ON DATABASE trade_model_v1_p3h_primary TO p3h_backup_reader;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM p3h_app_readonly;
REVOKE ALL ON SCHEMA public FROM p3h_backup_reader;
GRANT USAGE ON SCHEMA public TO p3h_app_readonly;
GRANT USAGE ON SCHEMA public TO p3h_backup_reader;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM p3h_app_readonly;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM p3h_app_readonly;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM p3h_backup_reader;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM p3h_backup_reader;

DO $$
DECLARE
    column_grant record;
    grantee_sql text;
BEGIN
    FOR column_grant IN
        SELECT DISTINCT table_schema, table_name, column_name, grantee
        FROM information_schema.column_privileges
        WHERE table_schema = 'public'
          AND grantee IN ('p3h_app_readonly', 'p3h_backup_reader', 'PUBLIC')
          AND privilege_type IN ('INSERT', 'UPDATE', 'REFERENCES')
    LOOP
        grantee_sql := CASE
            WHEN column_grant.grantee = 'PUBLIC' THEN 'PUBLIC'
            ELSE quote_ident(column_grant.grantee)
        END;
        EXECUTE format(
            'REVOKE ALL PRIVILEGES (%I) ON TABLE %I.%I FROM %s',
            column_grant.column_name, column_grant.table_schema,
            column_grant.table_name, grantee_sql
        );
    END LOOP;
END
$$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO p3h_app_readonly;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO p3h_app_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO p3h_backup_reader;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO p3h_backup_reader;

-- P3-H remains read-only for business data. These column-level grants are the
-- minimum writes required by PersonalUserBootstrap and successful login audit.
GRANT INSERT (username, password_hash, created_at, last_login_at)
    ON TABLE public.tm_user TO p3h_app_readonly;
GRANT UPDATE (last_login_at)
    ON TABLE public.tm_user TO p3h_app_readonly;
GRANT USAGE ON SEQUENCE public.tm_user_id_seq TO p3h_app_readonly;

ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM p3h_app_readonly;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM p3h_app_readonly;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON TABLES FROM p3h_backup_reader;
ALTER DEFAULT PRIVILEGES FOR ROLE p3h_migration_owner IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM p3h_backup_reader;
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
    auth_column_grant_mismatch_count integer;
BEGIN
    SELECT count(*) INTO unsafe_grant_count
    FROM information_schema.role_table_grants
    WHERE grantee IN ('p3h_app_readonly', 'p3h_backup_reader')
      AND privilege_type <> 'SELECT';
    IF unsafe_grant_count <> 0 THEN
        RAISE EXCEPTION 'P3-H read-only grant verification failed';
    END IF;

    WITH expected(table_schema, table_name, column_name, privilege_type) AS (
        VALUES
            ('public', 'tm_user', 'username', 'INSERT'),
            ('public', 'tm_user', 'password_hash', 'INSERT'),
            ('public', 'tm_user', 'created_at', 'INSERT'),
            ('public', 'tm_user', 'last_login_at', 'INSERT'),
            ('public', 'tm_user', 'last_login_at', 'UPDATE')
    ), actual AS (
        SELECT table_schema, table_name, column_name, privilege_type
        FROM information_schema.column_privileges
        WHERE grantee = 'p3h_app_readonly'
          AND privilege_type IN ('INSERT', 'UPDATE', 'REFERENCES')
    ), differences AS (
        (SELECT * FROM expected EXCEPT SELECT * FROM actual)
        UNION ALL
        (SELECT * FROM actual EXCEPT SELECT * FROM expected)
    )
    SELECT count(*) INTO auth_column_grant_mismatch_count FROM differences;

    IF auth_column_grant_mismatch_count <> 0 THEN
        RAISE EXCEPTION 'P3-H authentication column grant verification failed';
    END IF;

    IF NOT has_sequence_privilege(
            'p3h_app_readonly', 'public.tm_user_id_seq', 'USAGE')
       OR has_sequence_privilege(
            'p3h_app_readonly', 'public.tm_user_id_seq', 'UPDATE') THEN
        RAISE EXCEPTION 'P3-H authentication sequence grant verification failed';
    END IF;
END
$$;
