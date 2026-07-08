#!/usr/bin/env bash
set -euo pipefail

# Controlled PostgreSQL evidence setup helper.
# This script is intentionally no-op by default: it never runs Flyway, never
# connects to a database, and never prints URL/user/password/host values.

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
  echo "CONTROLLED_POSTGRESQL_RESULT: SKIPPED_MISSING_CONTROLLED_DB"
  echo "CONTROLLED_POSTGRESQL_ACTION: no database access attempted"
  exit 0
fi

if [ "${CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM:-}" != "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL" ]; then
  echo "CONTROLLED_POSTGRESQL_RESULT: BLOCKED_CONFIRMATION_REQUIRED"
  echo "CONTROLLED_POSTGRESQL_ACTION: refusing without explicit non-production confirmation"
  exit 2
fi

url_lower=$(printf '%s' "$CONTROLLED_POSTGRESQL_JDBC_URL" | tr '[:upper:]' '[:lower:]')
if printf '%s' "$url_lower" | grep -E '(prod|production|live|primary|main)' >/dev/null 2>&1; then
  echo "CONTROLLED_POSTGRESQL_RESULT: BLOCKED_PRODUCTION_INDICATOR"
  echo "CONTROLLED_POSTGRESQL_ACTION: refusing because JDBC URL contains a production-like indicator"
  exit 2
fi

if [ "${CONTROLLED_POSTGRESQL_EVIDENCE_MODE:-dry-run}" != "dry-run" ]; then
  echo "CONTROLLED_POSTGRESQL_RESULT: BLOCKED_SETUP_SCRIPT_NO_RUN_MODE"
  echo "CONTROLLED_POSTGRESQL_ACTION: this setup helper does not run Flyway migrations"
  exit 2
fi

echo "CONTROLLED_POSTGRESQL_RESULT: READY_FOR_SEPARATE_BOUNDED_FLYWAY_RUNNER"
echo "CONTROLLED_POSTGRESQL_ACTION: dry-run only; no database access attempted"
