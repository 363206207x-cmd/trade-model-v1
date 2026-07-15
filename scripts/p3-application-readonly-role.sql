\set ON_ERROR_STOP on

BEGIN;

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'application_role',
    :'application_password'
)
\gexec

SELECT format(
    'ALTER ROLE %I IN DATABASE %I SET default_transaction_read_only = on',
    :'application_role',
    :'database_name'
)
\gexec

SELECT format('REVOKE CONNECT, TEMPORARY ON DATABASE %I FROM PUBLIC', datname)
FROM pg_database
WHERE datallowconn
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM %I', :'database_name', :'application_role')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database_name', :'application_role')
\gexec

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('REVOKE ALL ON SCHEMA public FROM %I', :'application_role')
\gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'application_role')
\gexec
SELECT format('GRANT SELECT ON ALL TABLES IN SCHEMA public TO %I', :'application_role')
\gexec
SELECT format(
    'GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO %I',
    :'application_role'
)
\gexec

COMMIT;
