#!/usr/bin/env bash
set -euo pipefail

emit_result() {
  local provider="${1:---}"
  local model="${2:---}"
  local auth="${3:-NOT_CHECKED}"
  local status="${4:-FAIL_UNEXPECTED}"
  local calls="${5:-0}"
  local key_reads="${6:-0}"
  local timeout_limit_ms=15000
  if [[ "${provider}" == "GEMINI" ]]; then
    timeout_limit_ms=30000
  elif [[ "${provider}" == "--" || -z "${provider}" ]]; then
    timeout_limit_ms=0
  fi
  printf '%s\n' \
    "AI_PROVIDER: ${provider}" \
    "AI_MODEL: ${model}" \
    "AI_AUTH_STATUS: ${auth}" \
    "AI_HTTP_STATUS_CLASS: NOT_RUN" \
    "AI_ERROR_CATEGORY: --" \
    "AI_PROVIDER_ERROR_REASON: --" \
    "AI_RESPONSE_PARSE_STATUS: NOT_RUN" \
    "AI_TOKEN_USAGE_PRESENT: NO" \
    "AI_REQUEST_ID_PRESENT: NO" \
    "AI_TIMEOUT_LIMIT_MS: ${timeout_limit_ms}" \
    "AI_LATENCY_MS: 0" \
    "AI_PROVIDER_LIVE_SMOKE: ${status}" \
    "LIVE_PROVIDER_CALLS: ${calls}" \
    "REAL_KEYS_READ: ${key_reads}" \
    "PRODUCTION_READINESS: BLOCKED"
}

emit_diagnostic_result() {
  local mode="${1:---}"
  local category="${2:---}"
  local parse_status="${3:-NOT_RUN}"
  local calls="${4:-0}"
  local status="${5:-FAIL_UNEXPECTED}"
  printf '%s\n' \
    "AI_PROVIDER: GEMINI" \
    "AI_DIAGNOSTIC_MODE: ${mode}" \
    "AI_HTTP_STATUS_CLASS: NOT_RUN" \
    "AI_ERROR_CATEGORY: ${category}" \
    "AI_RESPONSE_PARSE_STATUS: ${parse_status}" \
    "AI_LATENCY_MS: 0" \
    "AI_PROVIDER_LIVE_SMOKE: ${status}" \
    "LIVE_PROVIDER_CALLS: ${calls}" \
    "PRODUCTION_READINESS: BLOCKED"
}

read_call_count_marker() {
  local marker_file="${1:-}"
  local marker_value=""
  if [[ -f "${marker_file}" ]]; then
    marker_value="$(tr -d '[:space:]' < "${marker_file}" 2>/dev/null || true)"
  fi
  case "${marker_value}" in
    0|1) printf '%s\n' "${marker_value}" ;;
    *) printf '%s\n' "UNKNOWN_MAX_1" ;;
  esac
}

replace_live_call_count() {
  local output="${1:-}"
  local calls="${2:-UNKNOWN_MAX_1}"
  printf '%s\n' "${output}" | awk -v calls="${calls}" '
    /^LIVE_PROVIDER_CALLS: / { print "LIVE_PROVIDER_CALLS: " calls; next }
    { print }
  '
}

target="${AI_PROVIDER_SMOKE_TARGET:-}"
diagnostic_enabled="${AI_PROVIDER_SMOKE_DIAGNOSTIC:-false}"
diagnostic_mode="${GEMINI_DIAGNOSTIC_MODE:-}"
if [[ "${diagnostic_enabled}" == "true" ]]; then
  case "${diagnostic_mode}" in
    A|B|C) ;;
    *)
      emit_diagnostic_result "--" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0" "FAIL_INVALID_TARGET"
      exit 2
      ;;
  esac
  if [[ -n "${target}" && "${target}" != "GEMINI" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0" "FAIL_INVALID_TARGET"
    exit 2
  fi
  target="GEMINI"
  export AI_PROVIDER_SMOKE_TARGET=GEMINI
fi
if [[ "${AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS:-false}" != "true" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "--" "NOT_RUN" "0" "SKIPPED_EXTERNAL_CALLS_DISABLED"
  else
    emit_result "${target:---}" "--" "NOT_CHECKED" "SKIPPED_EXTERNAL_CALLS_DISABLED" "0"
  fi
  exit 0
fi

if [[ "${AI_PROVIDER_SMOKE_HARNESS_ENTRY:-}" != "I_CONFIRM_SINGLE_PROVIDER_SMOKE" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "--" "NOT_RUN" "0" "SKIPPED_HARNESS_ENTRY_MISSING"
  else
    emit_result "${target:---}" "--" "NOT_CHECKED" "SKIPPED_HARNESS_ENTRY_MISSING" "0"
  fi
  exit 0
fi

case "${target}" in
  OPENAI)
    model="${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL:-gpt-5.6-luna}"
    provider_enabled="${TRADE_MODEL_AI_OPENAI_ENABLED:-false}"
    ;;
  GEMINI)
    model="${TRADE_MODEL_AI_GEMINI_MODEL:-gemini-2.5-pro}"
    provider_enabled="${TRADE_MODEL_AI_GEMINI_ENABLED:-false}"
    ;;
  XAI)
    model="${TRADE_MODEL_AI_XAI_MODEL:-grok-4.5}"
    provider_enabled="${TRADE_MODEL_AI_XAI_ENABLED:-false}"
    ;;
  "")
    emit_result "--" "--" "NOT_CHECKED" "FAIL_INVALID_TARGET" "0"
    exit 2
    ;;
  *)
    emit_result "${target}" "--" "NOT_CHECKED" "FAIL_INVALID_TARGET" "0"
    exit 2
    ;;
esac

if [[ "${TRADE_MODEL_AI_ENABLED:-false}" != "true" || "${provider_enabled}" != "true" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0" "SKIPPED_PROVIDER_DISABLED"
  else
    emit_result "${target}" "${model}" "PROVIDER_DISABLED" "SKIPPED_PROVIDER_DISABLED" "0"
  fi
  exit 0
fi

case "${target}" in
  OPENAI) key_present="${OPENAI_API_KEY:+yes}" ;;
  GEMINI) key_present="${GEMINI_API_KEY:+yes}" ;;
  XAI) key_present="${XAI_API_KEY:+yes}" ;;
esac
if [[ "${key_present:-}" != "yes" ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "NOT_RUN" "0" "SKIPPED_MISSING_API_KEY"
  else
    emit_result "${target}" "${model}" "MISSING" "SKIPPED_MISSING_API_KEY" "0"
  fi
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
if [[ "${target}" == "GEMINI" ]]; then
  export TRADE_MODEL_AI_MAX_OUTPUT_TOKENS=512
else
  export TRADE_MODEL_AI_MAX_OUTPUT_TOKENS=128
fi
export TRADE_MODEL_AI_REQUEST_TIMEOUT_MS=15000
export TRADE_MODEL_AI_OVERALL_TIMEOUT_MS=15000

output_file="$(mktemp)"
call_count_file="$(mktemp)"
chmod 600 "${call_count_file}"
printf '%s\n' "0" > "${call_count_file}"
export AI_PROVIDER_SMOKE_CALL_COUNT_FILE="${call_count_file}"
trap 'rm -f "${output_file}" "${call_count_file}"' EXIT

./mvnw -q \
  -Dtest=AiProviderControlledSmokeTest#controlledLiveSmokeEntryPoint \
  test >"${output_file}" 2>&1 &
smoke_pid=$!
watchdog_seconds="${AI_PROVIDER_SMOKE_WATCHDOG_SECONDS:-60}"
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

if [[ "${diagnostic_enabled}" == "true" ]]; then
  allowed_output="$(awk '/^(AI_PROVIDER|AI_DIAGNOSTIC_MODE|AI_HTTP_STATUS_CLASS|AI_ERROR_CATEGORY|AI_RESPONSE_PARSE_STATUS|AI_LATENCY_MS|AI_PROVIDER_LIVE_SMOKE|LIVE_PROVIDER_CALLS|PRODUCTION_READINESS|GEMINI_REQUEST_DIAGNOSTIC|MODEL|RESPONSE_MIME_TYPE|RESPONSE_SCHEMA_PRESENT|MAX_OUTPUT_TOKENS|TEMPERATURE|SYSTEM_INSTRUCTION_LENGTH|USER_INPUT_LENGTH|STOP_SEQUENCES_PRESENT|TOOLS_PRESENT|GEMINI_EXTRACTION_DIAGNOSTIC_STATUS|CANDIDATES_PRESENT|CANDIDATE_COUNT|CONTENT_PRESENT|PARTS_PRESENT|TEXT_NODE_PRESENT|TEXT_LENGTH|EMPTY_TEXT|EXTRACTED_JSON_PARSE_STATUS|GEMINI_OUTPUT_CLASS|GEMINI_SCHEMA_DIAGNOSTIC_STATUS|GEMINI_EXPECTED_FIELDS|GEMINI_ACTUAL_FIELDS|GEMINI_MISSING_FIELDS|GEMINI_UNEXPECTED_FIELDS|GEMINI_TYPE_MISMATCH): / { print }' "${output_file}")"
else
  allowed_output="$(awk '/^(AI_PROVIDER|AI_MODEL|AI_AUTH_STATUS|AI_HTTP_STATUS_CLASS|AI_ERROR_CATEGORY|AI_PROVIDER_ERROR_REASON|AI_RESPONSE_PARSE_STATUS|AI_TOKEN_USAGE_PRESENT|AI_REQUEST_ID_PRESENT|AI_TIMEOUT_LIMIT_MS|AI_LATENCY_MS|AI_PROVIDER_LIVE_SMOKE|LIVE_PROVIDER_CALLS|REAL_KEYS_READ|PRODUCTION_READINESS|GEMINI_REQUEST_DIAGNOSTIC|MODEL|RESPONSE_MIME_TYPE|RESPONSE_SCHEMA_PRESENT|MAX_OUTPUT_TOKENS|TEMPERATURE|SYSTEM_INSTRUCTION_LENGTH|USER_INPUT_LENGTH|STOP_SEQUENCES_PRESENT|TOOLS_PRESENT|GEMINI_EXTRACTION_DIAGNOSTIC_STATUS|CANDIDATES_PRESENT|CANDIDATE_COUNT|CONTENT_PRESENT|PARTS_PRESENT|TEXT_NODE_PRESENT|TEXT_LENGTH|EMPTY_TEXT|EXTRACTED_JSON_PARSE_STATUS|GEMINI_OUTPUT_CLASS|GEMINI_SCHEMA_DIAGNOSTIC_STATUS|GEMINI_EXPECTED_FIELDS|GEMINI_ACTUAL_FIELDS|GEMINI_MISSING_FIELDS|GEMINI_UNEXPECTED_FIELDS|GEMINI_TYPE_MISMATCH|GEMINI_SCHEMA_DIAGNOSTIC|EXPECTED_FIELDS|ACTUAL_FIELDS|MISSING_FIELDS|UNEXPECTED_FIELDS|TYPE_MISMATCH_FIELDS): / { print }' "${output_file}")"
fi
known_calls="$(read_call_count_marker "${call_count_file}")"
if [[ "${smoke_exit}" -ne 0 || -z "${allowed_output}"
      || "${allowed_output}" != *"LIVE_PROVIDER_CALLS: "* ]]; then
  if [[ "${diagnostic_enabled}" == "true" ]]; then
    emit_diagnostic_result "${diagnostic_mode}" "UNKNOWN_PROVIDER_ERROR" "FAIL" "${known_calls}" "FAIL_UNEXPECTED"
  else
    emit_result "${target}" "${model}" "KEY_PRESENT_NOT_EXPOSED" "FAIL_UNEXPECTED" "${known_calls}" "1"
  fi
  exit 1
fi

replace_live_call_count "${allowed_output}" "${known_calls}"
