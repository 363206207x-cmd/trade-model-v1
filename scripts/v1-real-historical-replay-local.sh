#!/usr/bin/env bash
set -euo pipefail

echo "V1_REAL_HISTORICAL_REPLAY_MODE: LOCAL_ONLY"
echo "LIVE_PROVIDER_CALLS: NO"
echo "PRODUCTION_ACCESS: NO"
echo "REAL_TRADING: NO"

if [[ "${V1_REAL_HISTORICAL_REPLAY_CONFIRM:-NO}" != "YES" ]]; then
  echo "REAL_HISTORICAL_REPLAY_RESULT: DRY_RUN"
  echo "NEXT: supply an allowed local fixture directory and set V1_REAL_HISTORICAL_REPLAY_CONFIRM=YES"
  exit 0
fi

fixture_dir="${V1_REAL_HISTORICAL_FIXTURE_DIR:-}"
if [[ -z "${fixture_dir}" ]]; then
  echo "REAL_HISTORICAL_FIXTURE_STATUS: MISSING"
  echo "REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE"
  exit 0
fi

if [[ "${fixture_dir}" == *"://"* ]]; then
  echo "REAL_HISTORICAL_REPLAY_RESULT: REFUSED_NON_LOCAL_PATH"
  exit 2
fi

normalized="$(printf '%s' "${fixture_dir}" | tr '[:upper:]' '[:lower:]')"
if [[ "${normalized}" =~ (^|/)(prod|production|live|primary|main)(/|$) ]]; then
  echo "REAL_HISTORICAL_REPLAY_RESULT: REFUSED_PRODUCTION_PATH"
  exit 2
fi

if [[ ! -d "${fixture_dir}" ]]; then
  echo "REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE"
  exit 2
fi

csv_count="$(find "${fixture_dir}" -maxdepth 2 -type f -name '*.csv' | wc -l | tr -d ' ')"
if [[ "${csv_count}" == "0" ]]; then
  echo "REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE"
  exit 2
fi

echo "REAL_HISTORICAL_FIXTURE_STATUS: PRESENT_REQUIRES_PROVENANCE_REVIEW"
echo "REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MANIFEST_UPDATE_REQUIRED"
exit 2
