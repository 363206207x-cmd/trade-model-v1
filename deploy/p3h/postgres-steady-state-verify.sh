#!/bin/sh
set -eu

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_STEADY_STATE_VERIFY: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql_primary="psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align"
psql_postgres="psql --host=postgres --username=p3h_bootstrap --dbname=postgres --no-psqlrc --tuples-only --no-align"

flyway_state="$(${psql_primary} --command="
  SELECT count(*) FILTER (WHERE success)::text || '|' ||
         coalesce(max(version) FILTER (WHERE success), '') || '|' ||
         count(*) FILTER (WHERE NOT success)::text || '|' ||
         coalesce(string_agg(version, ',' ORDER BY installed_rank) FILTER (WHERE success), '')
  FROM flyway_schema_history")"
[ "${flyway_state}" = "7|7|0|1,2,3,4,5,6,7" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_FLYWAY_STATE" >&2; exit 2; }

role_count="$(${psql_postgres} --command="
  SELECT count(*) FROM pg_roles
  WHERE rolname IN (
    'p3h_migration_owner', 'p3h_app_readonly',
    'p3h_backup_reader', 'p3h_recovery_owner'
  )
    AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
    AND NOT rolinherit AND NOT rolreplication")"
[ "${role_count}" = "4" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_ROLE_STATE" >&2; exit 2; }

database_count="$(${psql_postgres} --command="
  SELECT count(*)
  FROM pg_database d
  JOIN pg_roles r ON r.oid = d.datdba
  WHERE (d.datname = 'trade_model_v1_p3h_primary' AND r.rolname = 'p3h_migration_owner')
     OR (d.datname = 'trade_model_v1_p3h_recovery' AND r.rolname = 'p3h_recovery_owner')")"
[ "${database_count}" = "2" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_DATABASE_STATE" >&2; exit 2; }

unsafe_grants="$(${psql_primary} --command="
  SELECT count(*)
  FROM information_schema.role_table_grants
  WHERE grantee IN ('p3h_app_readonly', 'p3h_backup_reader')
    AND privilege_type IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'TRIGGER', 'REFERENCES')")"
missing_selects="$(${psql_primary} --command="
  SELECT count(*)
  FROM information_schema.tables
  WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    AND (NOT has_table_privilege('p3h_app_readonly', format('%I.%I', table_schema, table_name), 'SELECT')
      OR NOT has_table_privilege('p3h_backup_reader', format('%I.%I', table_schema, table_name), 'SELECT'))")"
schema_contract="$(${psql_primary} --command="
  SELECT has_schema_privilege('p3h_app_readonly', 'public', 'USAGE')
     AND NOT has_schema_privilege('p3h_app_readonly', 'public', 'CREATE')
     AND has_schema_privilege('p3h_backup_reader', 'public', 'USAGE')
     AND NOT has_schema_privilege('p3h_backup_reader', 'public', 'CREATE')")"
database_contract="$(${psql_primary} --command="
  SELECT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'CONNECT')
     AND NOT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'CREATE')
     AND NOT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'TEMP')")"
default_readonly_grants="$(${psql_primary} --command="
  SELECT count(*)
  FROM pg_default_acl d
  JOIN pg_roles owner_role ON owner_role.oid = d.defaclrole
  JOIN pg_namespace n ON n.oid = d.defaclnamespace
  CROSS JOIN LATERAL aclexplode(d.defaclacl) acl
  JOIN pg_roles grantee_role ON grantee_role.oid = acl.grantee
  WHERE owner_role.rolname = 'p3h_migration_owner'
    AND n.nspname = 'public'
    AND grantee_role.rolname IN ('p3h_app_readonly', 'p3h_backup_reader')
    AND d.defaclobjtype IN ('r', 'S')
    AND acl.privilege_type = 'SELECT'")"

if [ "${unsafe_grants}" != "0" ] || [ "${missing_selects}" != "0" ] \
    || [ "${schema_contract}" != "t" ] || [ "${database_contract}" != "t" ] \
    || [ "${default_readonly_grants}" != "4" ]; then
  echo "P3H_STEADY_STATE_VERIFY: BLOCKED_READONLY_CONTRACT" >&2
  exit 2
fi

echo "P3H_STEADY_STATE_VERIFY: PASS"
echo "P3H_FLYWAY_CURRENT_VERSION: 7"
echo "P3H_FLYWAY_FAILED_MIGRATIONS: 0"
echo "P3H_ROLE_AND_GRANT_CONTRACT: PASS"
