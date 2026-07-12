#!/usr/bin/env bash
set -euo pipefail

emit_not_run() {
  local status="${1:-FAIL_HARNESS}"
  local call_count="${2:-0}"
  local aggregate_count="${call_count}"
  if [[ "${call_count}" == "UNKNOWN_MAX_1" ]]; then
    aggregate_count="UNKNOWN_MAX_3"
  fi
  printf '%s\n' \
    "AI_PARALLEL_LIVE_SMOKE: ${status}" \
    "AI_PARALLEL_SMOKE_STATUS: ${status}" \
    "ORCHESTRATION_MODE: NOT_RUN" \
    "ORCHESTRATION_LATENCY_MS: 0" \
    "GLOBAL_DEADLINE_EXCEEDED: false" \
    "PROVIDER_SUBMITTED_COUNT: 0" \
    "PROVIDER_COMPLETED_COUNT: 0" \
    "PROVIDER_SUCCESS_COUNT: 0" \
    "PROVIDER_TIMEOUT_COUNT: 0" \
    "PROVIDER_FAILED_COUNT: 0" \
    "PARTIAL_FALLBACK_USED: false" \
    "OPENAI_STATUS: NOT_RUN" \
    "OPENAI_HTTP_STATUS_CLASS: NOT_RUN" \
    "OPENAI_PARSE_STATUS: NOT_RUN" \
    "OPENAI_LATENCY_MS: 0" \
    "OPENAI_CALL_COUNT: ${call_count}" \
    "GEMINI_STATUS: NOT_RUN" \
    "GEMINI_HTTP_STATUS_CLASS: NOT_RUN" \
    "GEMINI_PARSE_STATUS: NOT_RUN" \
    "GEMINI_LATENCY_MS: 0" \
    "GEMINI_CALL_COUNT: ${call_count}" \
    "XAI_STATUS: NOT_RUN" \
    "XAI_HTTP_STATUS_CLASS: NOT_RUN" \
    "XAI_PARSE_STATUS: NOT_RUN" \
    "XAI_LATENCY_MS: 0" \
    "XAI_CALL_COUNT: ${call_count}" \
    "FINAL_RESULT_ORDER: NOT_RUN" \
    "LIVE_PROVIDER_CALLS: ${aggregate_count}" \
    "REAL_KEYS_READ: 0" \
    "PRODUCTION_READINESS: BLOCKED"
}

if [[ "${AI_PARALLEL_SMOKE_ENABLE_EXTERNAL_CALLS:-false}" != "true" ]]; then
  emit_not_run "SKIPPED_EXTERNAL_CALLS_DISABLED" "0"
  exit 0
fi

if [[ "${AI_PARALLEL_SMOKE_HARNESS_ENTRY:-}" != "I_CONFIRM_THREE_PROVIDER_PARALLEL_SMOKE" ]]; then
  emit_not_run "SKIPPED_HARNESS_ENTRY_MISSING" "0"
  exit 0
fi

if [[ "${TRADE_MODEL_AI_ENABLED:-false}" != "true"
      || "${TRADE_MODEL_AI_OPENAI_ENABLED:-false}" != "true"
      || "${TRADE_MODEL_AI_GEMINI_ENABLED:-false}" != "true"
      || "${TRADE_MODEL_AI_XAI_ENABLED:-false}" != "true" ]]; then
  emit_not_run "SKIPPED_PROVIDER_DISABLED" "0"
  exit 0
fi

if [[ -z "${OPENAI_API_KEY:-}" || -z "${GEMINI_API_KEY:-}" || -z "${XAI_API_KEY:-}" ]]; then
  emit_not_run "SKIPPED_MISSING_API_KEY" "0"
  exit 0
fi

export TRADE_MODEL_SCHEDULERS_ENABLED=false
export TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED=false
export TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false
export TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED=false
export TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED=false
export TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED=false
export TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED=false
export TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED=false
export SPRING_PROFILES_ACTIVE=default
export SPRING_DATASOURCE_URL='jdbc:h2:mem:ai_parallel_controlled_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1'
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD=
export SPRING_SQL_INIT_MODE=always
export TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_ENABLED=false
export TRADE_MODEL_AI_OPENAI_TIMEOUT_MS=10000
export TRADE_MODEL_AI_GEMINI_TIMEOUT_MS=25000
export TRADE_MODEL_AI_XAI_TIMEOUT_MS=10000
export TRADE_MODEL_AI_OVERALL_TIMEOUT_MS=30000
export TRADE_MODEL_AI_DAILY_BUDGET_USD=1
export TRADE_MODEL_AI_PER_ANALYSIS_BUDGET_USD=1
export TRADE_MODEL_AI_OPENAI_RPM=3
export TRADE_MODEL_AI_GEMINI_RPM=3
export TRADE_MODEL_AI_XAI_RPM=3
export TRADE_MODEL_AI_OPENAI_INPUT_COST_PER_MILLION_USD=0.01
export TRADE_MODEL_AI_OPENAI_OUTPUT_COST_PER_MILLION_USD=0.01
export TRADE_MODEL_AI_GEMINI_INPUT_COST_PER_MILLION_USD=0.01
export TRADE_MODEL_AI_GEMINI_OUTPUT_COST_PER_MILLION_USD=0.01
export TRADE_MODEL_AI_XAI_INPUT_COST_PER_MILLION_USD=0.01
export TRADE_MODEL_AI_XAI_OUTPUT_COST_PER_MILLION_USD=0.01

output_file="$(mktemp)"
call_count_file="$(mktemp)"
chmod 600 "${output_file}" "${call_count_file}"
printf '%s\n' "OPENAI=0" "GEMINI=0" "XAI=0" >"${call_count_file}"
export AI_PARALLEL_SMOKE_CALL_COUNT_FILE="${call_count_file}"
trap 'rm -f "${output_file}" "${call_count_file}"' EXIT

./mvnw -q \
  -Dtest=AiParallelOrchestratorControlledSmokeTest#controlledLiveSmokeEntryPoint \
  test >"${output_file}" 2>&1 &
smoke_pid=$!
watchdog_seconds="${AI_PARALLEL_SMOKE_WATCHDOG_SECONDS:-60}"
if [[ ! "${watchdog_seconds}" =~ ^[1-9][0-9]*$ || "${watchdog_seconds}" -gt 60 ]]; then
  watchdog_seconds=60
fi
(
  sleep "${watchdog_seconds}"
  kill -TERM "${smoke_pid}" 2>/dev/null || true
) &
watchdog_pid=$!

if wait "${smoke_pid}"; then
  smoke_exit=0
else
  smoke_exit=$?
fi
kill "${watchdog_pid}" 2>/dev/null || true
wait "${watchdog_pid}" 2>/dev/null || true

allowed_output="$(awk '/^(AI_PARALLEL_LIVE_SMOKE|AI_PARALLEL_SMOKE_STATUS|ORCHESTRATION_MODE|ORCHESTRATION_LATENCY_MS|GLOBAL_DEADLINE_EXCEEDED|PROVIDER_SUBMITTED_COUNT|PROVIDER_COMPLETED_COUNT|PROVIDER_SUCCESS_COUNT|PROVIDER_TIMEOUT_COUNT|PROVIDER_FAILED_COUNT|PARTIAL_FALLBACK_USED|OPENAI_STATUS|OPENAI_HTTP_STATUS_CLASS|OPENAI_PARSE_STATUS|OPENAI_LATENCY_MS|OPENAI_CALL_COUNT|GEMINI_STATUS|GEMINI_HTTP_STATUS_CLASS|GEMINI_PARSE_STATUS|GEMINI_LATENCY_MS|GEMINI_CALL_COUNT|XAI_STATUS|XAI_HTTP_STATUS_CLASS|XAI_PARSE_STATUS|XAI_LATENCY_MS|XAI_CALL_COUNT|FINAL_RESULT_ORDER|LIVE_PROVIDER_CALLS|REAL_KEYS_READ|PRODUCTION_READINESS): / { print }' "${output_file}")"

if [[ "${smoke_exit}" -ne 0 || -z "${allowed_output}" ]]; then
  emit_not_run "FAIL_HARNESS" "UNKNOWN_MAX_1"
  exit 1
fi

printf '%s\n' "${allowed_output}"
