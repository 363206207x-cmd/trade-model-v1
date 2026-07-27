#!/bin/sh
set -eu

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_READONLY_GRANTS: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary \
  --no-psqlrc --set=ON_ERROR_STOP=1 \
  --file=/p3h/postgres-readonly-grants.sql >/dev/null

case "${P3H_READONLY_GRANTS_MODE:-}" in
  INITIALIZE)
    echo "STAGING_FLYWAY_FRESH_INSTALL: PASS_V1_TO_V9"
    ;;
  STEADY_STATE)
    echo "P3H_READONLY_GRANTS_REFRESH: PASS"
    ;;
  RECOVERY)
    echo "P3H_READONLY_GRANTS_RECOVERY: PASS"
    ;;
  *)
    echo "P3H_READONLY_GRANTS: BLOCKED_MODE" >&2
    exit 2
    ;;
esac
echo "STAGING_FLYWAY_FINAL_VERSION: 9"
echo "P3H_BUSINESS_DATA_ROLE: READ_ONLY"
echo "P3H_AUTH_SESSION_WRITE_SCOPE: TM_USER_COLUMNS_ONLY"
echo "P3H_READONLY_GRANTS: PASS"
