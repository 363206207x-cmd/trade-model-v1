#!/bin/sh
set -eu

secret_file=/run/secrets/postgres_admin_password
if [ ! -f "${secret_file}" ] || [ -L "${secret_file}" ] || [ ! -s "${secret_file}" ]; then
  echo "P3H_GREENFIELD_PREFLIGHT: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${secret_file}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql_base="psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align"
violation_count="$(${psql_base} --command="
  WITH violations AS (
    SELECT 'schema' AS kind, nspname AS name
    FROM pg_namespace
    WHERE nspname <> 'public'
      AND nspname <> 'information_schema'
      AND nspname !~ '^pg_'
    UNION ALL
    SELECT 'extension', extname FROM pg_extension WHERE extname <> 'plpgsql'
    UNION ALL
    SELECT 'relation', n.nspname || '.' || c.relname
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
      AND n.nspname <> 'information_schema'
      AND n.nspname !~ '^pg_'
    UNION ALL
    SELECT 'routine', n.nspname || '.' || p.proname
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname <> 'information_schema'
      AND n.nspname !~ '^pg_'
    UNION ALL
    SELECT 'trigger', n.nspname || '.' || t.tgname
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE NOT t.tgisinternal
      AND n.nspname <> 'information_schema'
      AND n.nspname !~ '^pg_'
    UNION ALL
    SELECT 'foreign_data_wrapper', fdwname FROM pg_foreign_data_wrapper
    UNION ALL
    SELECT 'foreign_server', srvname FROM pg_foreign_server
  )
  SELECT count(*) FROM violations")"
flyway_history="$(${psql_base} --command="SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")"

if [ "${violation_count}" != "0" ] || [ "${flyway_history}" != "f" ]; then
  echo "P3H_GREENFIELD_PREFLIGHT: BLOCKED_NON_EMPTY_DATABASE" >&2
  exit 2
fi

echo "GREENFIELD_PRE_MIGRATION_SCHEMA: EMPTY"
echo "GREENFIELD_PRE_MIGRATION_BUSINESS_ROWS: 0"
echo "GREENFIELD_PRE_MIGRATION_FLYWAY_HISTORY: ABSENT"
echo "GREENFIELD_OBJECT_INVENTORY: PASS_STRICT"
echo "P3H_GREENFIELD_PREFLIGHT: PASS"
