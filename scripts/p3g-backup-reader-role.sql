\set ON_ERROR_STOP on

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'backup_role', :'backup_password'
)
\gexec

SELECT format(
    'ALTER ROLE %I IN DATABASE %I SET default_transaction_read_only = on',
    :'backup_role', :'database_name'
)
\gexec

SELECT format('REVOKE ALL ON DATABASE %I FROM %I', :'database_name', :'backup_role')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database_name', :'backup_role')
\gexec
SELECT format('REVOKE ALL ON SCHEMA public FROM %I', :'backup_role')
\gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'backup_role')
\gexec
SELECT format('GRANT SELECT ON ALL TABLES IN SCHEMA public TO %I', :'backup_role')
\gexec
SELECT format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', :'backup_role')
\gexec
