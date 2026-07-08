#!/usr/bin/env bash
set -euo pipefail

# Controlled PostgreSQL Flyway smoke runner.
# This script never prints DB URL/user/password/host values. It only runs when
# the operator supplies a disposable non-production PostgreSQL env and explicit
# confirmations. It does not call production DB and does not run destructive
# cleanup/restore operations.

required_vars=(
  CONTROLLED_POSTGRESQL_JDBC_URL
  CONTROLLED_POSTGRESQL_USERNAME
  CONTROLLED_POSTGRESQL_PASSWORD
)

missing=0
for name in "${required_vars[@]}"; do
  if [ -z "${!name:-}" ]; then
    echo "${name}: MISSING"
    missing=1
  else
    echo "${name}: PRESENT_REDACTED"
  fi
done

if [ "$missing" -ne 0 ]; then
  echo "CONTROLLED_POSTGRESQL_FLYWAY_RESULT: SKIPPED_MISSING_CONTROLLED_DB"
  echo "CONTROLLED_POSTGRESQL_FLYWAY_ACTION: no database access attempted"
  exit 0
fi

if [ "${CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM:-}" != "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL" ]; then
  echo "CONTROLLED_POSTGRESQL_FLYWAY_RESULT: BLOCKED_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_POSTGRESQL_FLYWAY_ACTION: refusing without explicit non-production confirmation"
  exit 2
fi

if [ "${CONTROLLED_POSTGRESQL_FLYWAY_RUN:-}" != "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB" ]; then
  echo "CONTROLLED_POSTGRESQL_FLYWAY_RESULT: BLOCKED_RUN_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_POSTGRESQL_FLYWAY_ACTION: refusing without explicit Flyway run confirmation"
  exit 2
fi

url_lower=$(printf '%s' "$CONTROLLED_POSTGRESQL_JDBC_URL" | tr '[:upper:]' '[:lower:]')
if printf '%s' "$url_lower" | grep -E '(prod|production|live|primary|main)' >/dev/null 2>&1; then
  echo "CONTROLLED_POSTGRESQL_FLYWAY_RESULT: BLOCKED_PRODUCTION_INDICATOR"
  echo "CONTROLLED_POSTGRESQL_FLYWAY_ACTION: refusing because JDBC URL contains a production-like indicator"
  exit 2
fi

echo "CONTROLLED_POSTGRESQL_FLYWAY_RESULT: STARTING_REDACTED_BOUNDED_SMOKE"
python3 - <<'PY'
import subprocess
import sys
cmd = ['./mvnw', '-q', '-Dtest=ControlledPostgreSqlFlywaySmokeTest', 'test']
try:
    result = subprocess.run(cmd, timeout=300)
except subprocess.TimeoutExpired:
    print('CONTROLLED_POSTGRESQL_FLYWAY_RESULT: SKIPPED_TIMEOUT')
    print('CONTROLLED_POSTGRESQL_FLYWAY_ACTION: bounded smoke exceeded 300 seconds')
    sys.exit(124)
if result.returncode == 0:
    print('CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS')
else:
    print('CONTROLLED_POSTGRESQL_FLYWAY_RESULT: FAIL')
sys.exit(result.returncode)
PY
