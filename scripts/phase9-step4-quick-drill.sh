#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TS="${TS:-$(date +%Y%m%d-%H%M%S)}"
OUTPUT_ROOT="artifacts/phase9-step4-quick-drill"
EVIDENCE_DIR="${OUTPUT_ROOT}/${TS}"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $cmd" >&2
    exit 1
  fi
}

fetch_json() {
  local endpoint="$1"
  local output_file="$2"
  local url="${BASE_URL}${endpoint}"
  local http_code

  http_code="$(curl -sS -o "$output_file" -w "%{http_code}" "$url")"
  if [[ "$http_code" != "200" ]]; then
    echo "ERROR: request failed (${http_code}) for ${url}" >&2
    exit 1
  fi
}

extract_or_na() {
  local expr="$1"
  local file="$2"
  jq -r "$expr // \"N/A\"" "$file" 2>/dev/null || echo "N/A"
}

main() {
  require_cmd "curl"
  require_cmd "jq"

  mkdir -p "$EVIDENCE_DIR"

  RUN_BASELINE_FILE="${EVIDENCE_DIR}/run-baseline.json"
  POSITION_STATUS_FILE="${EVIDENCE_DIR}/position-sync-status.json"
  BASELINE_15M_FILE="${EVIDENCE_DIR}/run-baseline-15m.json"
  SUMMARY_FILE="${EVIDENCE_DIR}/quick-summary.txt"

  echo "[1/3] Fetching baseline (60m)..."
  fetch_json "/api/system/run-baseline?windowMinutes=60" "$RUN_BASELINE_FILE"

  echo "[2/3] Fetching position sync status..."
  fetch_json "/api/system/position-sync-status" "$POSITION_STATUS_FILE"

  echo "[3/3] Fetching baseline (15m)..."
  fetch_json "/api/system/run-baseline?windowMinutes=15" "$BASELINE_15M_FILE"

  scheduler_status="$(extract_or_na '.data.systemHealth.schedulerStatus' "$RUN_BASELINE_FILE")"
  scheduler_detail="$(extract_or_na '.data.systemHealth.schedulerStatusDetail' "$RUN_BASELINE_FILE")"
  availability_status="$(extract_or_na '.data.positionSync.availabilityStatus' "$RUN_BASELINE_FILE")"
  freshness_status="$(extract_or_na '.data.freshnessStatus' "$POSITION_STATUS_FILE")"
  last_sync_success="$(extract_or_na '.data.lastSyncSuccess' "$POSITION_STATUS_FILE")"
  recheck_total="$(extract_or_na '.data.recheckSummary.totalCountWindow' "$RUN_BASELINE_FILE")"
  alert_summary="$(extract_or_na '.data.alertSummary' "$BASELINE_15M_FILE")"
  data_quality_summary="$(extract_or_na '.data.dataQualitySummary' "$BASELINE_15M_FILE")"

  {
    echo "Phase 9 Step 4 Quick Drill Summary"
    echo "timestamp: ${TS}"
    echo "base_url: ${BASE_URL}"
    echo "evidence_dir: ${EVIDENCE_DIR}"
    echo
    echo "[Scenario 1] PositionSync freshness"
    echo "schedulerStatus: ${scheduler_status}"
    echo "schedulerStatusDetail: ${scheduler_detail}"
    echo "availabilityStatus: ${availability_status}"
    echo "freshnessStatus: ${freshness_status}"
    echo "lastSyncSuccess: ${last_sync_success}"
    echo
    echo "[Scenario 2] PushRecheck backlog signal"
    echo "recheckSummary.totalCountWindow: ${recheck_total}"
    echo
    echo "[Scenario 3] Baseline anomaly quick scan (15m)"
    echo "alertSummary: ${alert_summary}"
    echo "dataQualitySummary: ${data_quality_summary}"
    echo
    echo "Next:"
    echo "- Fill at least one item in PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md"
    echo "- Reference evidence path: ${EVIDENCE_DIR}"
  } >"$SUMMARY_FILE"

  echo
  echo "Quick drill finished."
  echo "Evidence directory: ${EVIDENCE_DIR}"
  echo "Files:"
  echo "  - run-baseline.json"
  echo "  - position-sync-status.json"
  echo "  - run-baseline-15m.json"
  echo "  - quick-summary.txt"
  echo
  echo "Next: update PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md with evidence path."
}

main "$@"
