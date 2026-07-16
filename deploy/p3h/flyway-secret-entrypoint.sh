#!/bin/sh
set -eu

secret_file=/run/secrets/flyway_password
if [ ! -f "$secret_file" ] || [ -L "$secret_file" ] || [ ! -s "$secret_file" ]; then
  echo "P3H_FLYWAY_RESULT: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

FLYWAY_PASSWORD="$(cat "$secret_file")"
export FLYWAY_PASSWORD

operation="${P3H_FLYWAY_OPERATION:-migrate}"
case "${operation}" in
  migrate|validate) ;;
  *)
    echo "P3H_FLYWAY_RESULT: BLOCKED_UNSAFE_OPERATION" >&2
    exit 2
    ;;
esac

exec /flyway/flyway -baselineOnMigrate=false -cleanDisabled=true "${operation}"
