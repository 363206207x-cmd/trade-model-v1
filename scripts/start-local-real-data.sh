#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/.runtime"
PID_FILE="${RUNTIME_DIR}/trade-model-v1-local-real.pid"
LOG_FILE="${RUNTIME_DIR}/trade-model-v1-local-real.log"
STATUS_URL="http://127.0.0.1:8081/api/local-real/status"
DASHBOARD_URL="http://127.0.0.1:8081/"

cd "${ROOT_DIR}"
command -v java >/dev/null 2>&1 || { echo "LOCAL_REAL_DATA_START: FAIL_JAVA_MISSING"; exit 1; }
[[ -x ./mvnw ]] || { echo "LOCAL_REAL_DATA_START: FAIL_MAVEN_WRAPPER_MISSING"; exit 1; }

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "LOCAL_REAL_DATA_START: FAIL_TRACKED_WORKTREE_DIRTY"
  exit 1
fi

mkdir -p "${RUNTIME_DIR}" "${ROOT_DIR}/data"
bash "${ROOT_DIR}/scripts/stop-local-real-data.sh" >/dev/null

if command -v lsof >/dev/null 2>&1 && lsof -nP -iTCP:8081 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "LOCAL_REAL_DATA_START: FAIL_PORT_8081_IN_USE"
  exit 1
fi

unset OPENAI_API_KEY GEMINI_API_KEY XAI_API_KEY COINGLASS_API_KEY NEWS_API_KEY
unset AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS AI_PROVIDER_SMOKE_HARNESS_ENTRY AI_PROVIDER_SMOKE_TARGET
unset AI_PROVIDER_SMOKE_DIAGNOSTIC GEMINI_DIAGNOSTIC_MODE AI_PARALLEL_SMOKE_ENABLE_EXTERNAL_CALLS
unset AI_PARALLEL_SMOKE_HARNESS_ENTRY AI_PROVIDER_SMOKE_CALL_COUNT_FILE

export TRADE_MODEL_AI_ENABLED=false
export TRADE_MODEL_AI_OPENAI_ENABLED=false
export TRADE_MODEL_AI_GEMINI_ENABLED=false
export TRADE_MODEL_AI_XAI_ENABLED=false
export TRADE_MODEL_COINGLASS_ENABLED=false
export TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED=false
export TRADE_MODEL_PROVIDER_CALL_ENABLED=true
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=true
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=true
export TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED=true
export TRADE_MODEL_OHLCV_PROVIDER_PRIMARY=kraken
export TRADE_MODEL_OHLCV_PROVIDER_FALLBACK=binance
export TRADE_MODEL_OHLCV_PROVIDER_FALLBACK_ENABLED=true
export TRADE_MODEL_OHLCV_KRAKEN_ENABLED=true
export TRADE_MODEL_OHLCV_KRAKEN_EXTERNAL_CALLS_ENABLED=true
export TRADE_MODEL_OHLCV_BINANCE_ENABLED=true
export TRADE_MODEL_OHLCV_BINANCE_EXTERNAL_CALLS_ENABLED=true
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=true
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=true
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=true
export TELEGRAM_ENABLED=false
export PUSH_ENABLED=false
export AUTO_TRADING_ENABLED=false

nohup ./mvnw spring-boot:run -q -Dspring-boot.run.profiles=local-real >"${LOG_FILE}" 2>&1 &
APP_PID=$!
printf '%s\n' "${APP_PID}" >"${PID_FILE}"

deadline=$((SECONDS + 180))
last_state="STARTING"
last_bars="0"
last_assets="0"
last_ready_assets="0"
last_degraded_assets="none"
last_primary_provider="KRAKEN"
last_reason="LOCAL_REAL_STARTING"
while (( SECONDS < deadline )); do
  if ! kill -0 "${APP_PID}" 2>/dev/null; then
    echo "LOCAL_REAL_DATA_START: FAIL_PROCESS_EXITED"
    echo "LOG_FILE: .runtime/trade-model-v1-local-real.log"
    exit 1
  fi
  if payload="$(curl -fsS --max-time 3 "${STATUS_URL}" 2>/dev/null)"; then
    read -r last_state last_bars last_assets last_reason last_primary_provider last_ready_assets last_degraded_assets < <(printf '%s' "${payload}" | python3 -c '
import json,sys
d=json.load(sys.stdin)
m=d.get("marketData",{})
degraded=",".join(m.get("degradedAssets",[])) or "none"
print(d.get("state","STARTING"), m.get("closedBarCount",0), d.get("analysis",{}).get("completedAssetCount",0), d.get("failureReasonCode","UNKNOWN"), m.get("provider","KRAKEN"), m.get("readyAssetCount",0), degraded)
')
    if [[ "${last_state}" == "DASHBOARD_READY" ]]; then
      echo "LOCAL_REAL_DATA_START: PASS"
      echo "HEALTH_STATUS: UP"
      echo "DATABASE_MODE: LOCAL_PERSISTENT_H2"
      echo "MARKET_PROVIDER_PRIMARY: ${last_primary_provider}"
      echo "READY_ASSETS: ${last_ready_assets}/6"
      echo "DEGRADED_ASSETS: ${last_degraded_assets}"
      echo "DASHBOARD_READY: TRUE"
      echo "AI_PROVIDERS: DISABLED"
      echo "DASHBOARD_URL: ${DASHBOARD_URL}"
      echo "LOG_FILE: .runtime/trade-model-v1-local-real.log"
      if command -v open >/dev/null 2>&1; then open "${DASHBOARD_URL}" >/dev/null 2>&1 || true; fi
      exit 0
    fi
  fi
  sleep 2
done

echo "LOCAL_REAL_DATA_START: FAIL_DASHBOARD_READY_TIMEOUT"
echo "CURRENT_STAGE: ${last_state}"
echo "CLOSED_BAR_COUNT: ${last_bars}"
echo "COMPLETED_ASSET_COUNT: ${last_assets}"
echo "READY_ASSETS: ${last_ready_assets}/6"
echo "DEGRADED_ASSETS: ${last_degraded_assets}"
echo "FAILURE_REASON_CODE: ${last_reason}"
echo "LOG_FILE: .runtime/trade-model-v1-local-real.log"
exit 1
