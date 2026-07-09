#!/usr/bin/env bash

set -euo pipefail

CONFIRMATION="${V1_BUSINESS_STRESS_CONFIRM:-NO}"
OUTPUT_DIR="${V1_BUSINESS_STRESS_OUTPUT_DIR:-build/v1-business-stress}"
TEST_NAME="${V1_BUSINESS_STRESS_TEST:-V1BusinessStressTest}"

usage() {
  cat <<'EOF'
Usage: bash scripts/v1-business-stress-local.sh [--dry-run]

Default behavior is dry-run. To execute the local deterministic JUnit stress test:

  V1_BUSINESS_STRESS_CONFIRM=YES bash scripts/v1-business-stress-local.sh

This harness runs only local test/H2 fixtures. It does not access production,
providers, exchanges, Push/Telegram send paths, or order execution paths.
EOF
}

DRY_RUN=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

print_plan() {
  echo "V1_BUSINESS_STRESS_PLAN"
  echo "test_name=${TEST_NAME}"
  echo "output_dir=${OUTPUT_DIR}"
  echo "confirmation=${CONFIRMATION}"
  echo "data_source=SYNTHETIC_SCENARIO_DATA + local H2/test fixtures"
  echo "production_server_access=NO"
  echo "production_db_access=NO"
  echo "provider_calls=NO"
  echo "real_trading=NO"
  echo "write_endpoints=NO"
}

if [[ "$DRY_RUN" == "true" || "$CONFIRMATION" != "YES" ]]; then
  print_plan
  echo "DRY_RUN: no tests executed and no local DB mutation performed."
  exit 0
fi

mkdir -p "$OUTPUT_DIR"
{
  echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "test_name=${TEST_NAME}"
  echo "data_source=SYNTHETIC_SCENARIO_DATA"
  echo "production_readiness=BLOCKED"
} > "$OUTPUT_DIR/environment.txt"

print_plan | tee "$OUTPUT_DIR/summary.txt"
./mvnw -q -Dtest="${TEST_NAME}" test 2>&1 | tee "$OUTPUT_DIR/test-output.log"
echo "V1_BUSINESS_STRESS_RESULT=PASS" | tee -a "$OUTPUT_DIR/summary.txt"
