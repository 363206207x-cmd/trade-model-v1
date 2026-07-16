#!/bin/sh
set -eu

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_ROLE_BOOTSTRAP: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

for secret_name in flyway_password app_database_password_v1 backup_reader_password recovery_owner_password; do
  secret_path="/run/secrets/${secret_name}"
  if [ ! -f "${secret_path}" ] || [ -L "${secret_path}" ] || [ ! -s "${secret_path}" ]; then
    echo "P3H_ROLE_BOOTSTRAP: BLOCKED_MISSING_SECRET" >&2
    exit 2
  fi
done

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql --host=postgres --username=p3h_bootstrap --dbname=postgres --no-psqlrc \
  --file=/p3h/postgres-role-bootstrap.sql >/dev/null

role_count="$(psql --host=postgres --username=p3h_bootstrap --dbname=postgres \
  --no-psqlrc --tuples-only --no-align --command="
    SELECT count(*) FROM pg_roles
    WHERE rolname IN (
      'p3h_migration_owner', 'p3h_app_readonly',
      'p3h_backup_reader', 'p3h_recovery_owner'
    )
      AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
      AND NOT rolinherit AND NOT rolreplication")"
[ "${role_count}" = "4" ] \
  || { echo "P3H_ROLE_BOOTSTRAP: BLOCKED_ROLE_VERIFICATION" >&2; exit 2; }

database_count="$(psql --host=postgres --username=p3h_bootstrap --dbname=postgres \
  --no-psqlrc --tuples-only --no-align --command="
    SELECT count(*)
    FROM pg_database d
    JOIN pg_roles r ON r.oid = d.datdba
    WHERE (d.datname = 'trade_model_v1_p3h_primary'
           AND r.rolname = 'p3h_migration_owner')
       OR (d.datname = 'trade_model_v1_p3h_recovery'
           AND r.rolname = 'p3h_recovery_owner')")"
[ "${database_count}" = "2" ] \
  || { echo "P3H_ROLE_BOOTSTRAP: BLOCKED_DATABASE_VERIFICATION" >&2; exit 2; }

echo "P3H_ROLE_BOOTSTRAP: PASS"
echo "P3H_ROLE_COUNT: ${role_count}"
echo "P3H_DATABASE_COUNT: ${database_count}"
