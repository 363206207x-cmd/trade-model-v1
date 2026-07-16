#!/bin/sh
set -eu

expected_confirmation=I_CONFIRM_CONTROLLED_APP_DATABASE_SECRET_ROTATION
if [ "${P3H_SECRET_VERSION_ACTIVATION_CONFIRM:-}" != "${expected_confirmation}" ]; then
  echo "P3H_APP_DATABASE_SECRET_ACTIVATION: BLOCKED_CONFIRMATION" >&2
  exit 2
fi

case "${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION:-}" in
  V1) selected_secret=/run/secrets/app_database_password_v1 ;;
  V2) selected_secret=/run/secrets/app_database_password_v2 ;;
  *) echo "P3H_APP_DATABASE_SECRET_ACTIVATION: BLOCKED_VERSION" >&2; exit 2 ;;
esac
admin_secret=/run/secrets/postgres_admin_password
for secret_file in "${admin_secret}" "${selected_secret}"; do
  if [ ! -f "${secret_file}" ] || [ -L "${secret_file}" ] || [ ! -s "${secret_file}" ]; then
    echo "P3H_APP_DATABASE_SECRET_ACTIVATION: BLOCKED_SECRET" >&2
    exit 2
  fi
done

pgpass_file="$(mktemp)"
active_secret_file=/tmp/p3h_selected_app_database_password
sql_file="$(mktemp)"
trap 'rm -f "${pgpass_file}" "${active_secret_file}" "${sql_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}" "${sql_file}"
cp "${selected_secret}" "${active_secret_file}"
chmod 600 "${active_secret_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

cat >"${sql_file}" <<'SQL'
\set ON_ERROR_STOP on
\set app_password `tr -d '\r\n' </tmp/p3h_selected_app_database_password`
SELECT format('ALTER ROLE p3h_app_readonly PASSWORD %L', :'app_password') \gexec
SQL

psql --host=postgres --username=p3h_bootstrap --dbname=postgres --no-psqlrc \
  --file="${sql_file}" >/dev/null

echo "P3H_APP_DATABASE_SECRET_ACTIVATION: PASS"
echo "P3H_ACTIVE_APP_DATABASE_SECRET_VERSION: ${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION}"
