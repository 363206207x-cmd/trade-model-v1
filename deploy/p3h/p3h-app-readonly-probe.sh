#!/bin/sh
set -eu

case "${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION:-}" in
  V1) secret_file=/run/secrets/app_database_password_v1 ;;
  V2) secret_file=/run/secrets/app_database_password_v2 ;;
  *) echo "P3H_APP_ROLE_PROBE: BLOCKED_ACTIVE_SECRET_VERSION" >&2; exit 2 ;;
esac
if [ ! -f "${secret_file}" ] || [ -L "${secret_file}" ] || [ ! -s "${secret_file}" ]; then
  echo "P3H_APP_ROLE_PROBE: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
write_probe_log="$(mktemp)"
set_role_probe_log="$(mktemp)"
trap 'rm -f "${pgpass_file}" "${write_probe_log}" "${set_role_probe_log}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}" "${write_probe_log}" "${set_role_probe_log}"
printf 'postgres:5432:*:p3h_app_readonly:%s\n' "$(tr -d '\r\n' <"${secret_file}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

read_only_value="$(psql --host=postgres --username=p3h_app_readonly \
  --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
  --command="SELECT current_setting('default_transaction_read_only')")"
if [ "${read_only_value}" != "on" ]; then
  echo "P3H_APP_ROLE_PROBE: BLOCKED_NOT_READ_ONLY" >&2
  exit 2
fi

if psql --host=postgres --username=p3h_app_readonly \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc \
    --command="SET default_transaction_read_only=off; UPDATE flyway_schema_history SET description=description WHERE false" \
    >/dev/null 2>"${write_probe_log}"; then
  echo "P3H_APP_ROLE_PROBE: BLOCKED_WRITE_ALLOWED" >&2
  exit 2
fi

if psql --host=postgres --username=p3h_app_readonly \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc \
    --command="SET ROLE p3h_migration_owner" \
    >/dev/null 2>"${set_role_probe_log}"; then
  echo "P3H_APP_ROLE_PROBE: BLOCKED_SET_ROLE_ALLOWED" >&2
  exit 2
fi

echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
echo "READ_ONLY_WRITE_PROBE: DENIED"
echo "SET_ROLE_TO_MIGRATION_OWNER: DENIED"
echo "P3H_APP_ROLE_PROBE: PASS"
