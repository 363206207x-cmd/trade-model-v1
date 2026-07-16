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
public_table_count="$(${psql_base} --command="SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")"
flyway_history="$(${psql_base} --command="SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")"

if [ "${public_table_count}" != "0" ] || [ "${flyway_history}" != "f" ]; then
  echo "P3H_GREENFIELD_PREFLIGHT: BLOCKED_NON_EMPTY_DATABASE" >&2
  exit 2
fi

echo "GREENFIELD_PRE_MIGRATION_SCHEMA: EMPTY"
echo "GREENFIELD_PRE_MIGRATION_BUSINESS_ROWS: 0"
echo "GREENFIELD_PRE_MIGRATION_FLYWAY_HISTORY: ABSENT"
echo "P3H_GREENFIELD_PREFLIGHT: PASS"
