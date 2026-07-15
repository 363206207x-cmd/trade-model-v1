\set ON_ERROR_STOP on

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'migration_role', :'migration_password'
)
\gexec

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'recovery_role', :'recovery_password'
)
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'primary_database', :'migration_role')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'recovery_database', :'recovery_role')
\gexec

REVOKE CONNECT, TEMPORARY ON DATABASE postgres FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE template1 FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE template0 FROM PUBLIC;

SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'primary_database')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'recovery_database')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'primary_database', :'migration_role')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'recovery_database', :'recovery_role')
\gexec
