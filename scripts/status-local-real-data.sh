#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${ROOT_DIR}/.runtime/trade-model-v1-local-real.pid"
DB_FILE="${ROOT_DIR}/data/trade-model-v1-local-real.mv.db"
STATUS_URL="http://127.0.0.1:8081/api/local-real/status"

pid=""
if [[ -f "${PID_FILE}" ]]; then pid="$(tr -dc '0-9' <"${PID_FILE}")"; fi
if [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null; then
  echo "LOCAL_REAL_DATA_STATUS: RUNNING"
  echo "PID: ${pid}"
else
  echo "LOCAL_REAL_DATA_STATUS: STOPPED"
  echo "PID: --"
fi

[[ -f "${DB_FILE}" ]] && echo "DATABASE_FILE: PRESENT" || echo "DATABASE_FILE: ABSENT"
if payload="$(curl -fsS --max-time 3 "${STATUS_URL}" 2>/dev/null)"; then
  printf '%s' "${payload}" | python3 -c '
import json,sys
d=json.load(sys.stdin); m=d.get("marketData",{}); a=d.get("analysis",{}); db=d.get("dashboard",{})
print("HEALTH_STATUS:", d.get("health","UNKNOWN"))
print("READINESS_STATE:", d.get("state","UNKNOWN"))
print("DASHBOARD_HTTP: UP")
print("LATEST_KLINE_TIME:", m.get("latestClosedBarAt") or "--")
print("CLOSED_KLINE_COUNT:", m.get("closedBarCount",0))
print("LATEST_ANALYSIS_TIME:", a.get("latestAnalysisAt") or "--")
print("LATEST_DECISION_TIME:", a.get("latestDecisionAt") or "--")
print("DASHBOARD_READY:", str(bool(db.get("ready"))).upper())
'
else
  echo "HEALTH_STATUS: DOWN"
  echo "DASHBOARD_HTTP: DOWN"
  echo "LATEST_KLINE_TIME: --"
  echo "CLOSED_KLINE_COUNT: --"
  echo "LATEST_ANALYSIS_TIME: --"
  echo "LATEST_DECISION_TIME: --"
fi
