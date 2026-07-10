#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/build/v1-historical-replay"
TEST_NAME="V1HistoricalReplayValidationTest"

echo "V1_HISTORICAL_REPLAY_MODE: LOCAL_ONLY"
echo "REPLAY_DATA_SOURCE: LOCAL_REPLAY_FIXTURE_NOT_PROVIDER"
echo "LIVE_PROVIDER_CALLS: NO"
echo "PRODUCTION_SERVER_ACCESS: NO"
echo "PRODUCTION_DB_ACCESS: NO"
echo "REAL_TRADING: NO"
echo "EXTERNAL_PUSH_OR_TELEGRAM: NO"

if [[ "${V1_HISTORICAL_REPLAY_CONFIRM:-NO}" != "YES" ]]; then
  echo "V1_HISTORICAL_REPLAY_RESULT: DRY_RUN"
  echo "NEXT: set V1_HISTORICAL_REPLAY_CONFIRM=YES to run the local JUnit replay harness"
  exit 0
fi

mkdir -p "${OUTPUT_DIR}"
cd "${ROOT_DIR}"
./mvnw -q -Dtest="${TEST_NAME}" test 2>&1 | tee "${OUTPUT_DIR}/test.log"
echo "V1_HISTORICAL_REPLAY_RESULT: TEST_COMPLETED"
