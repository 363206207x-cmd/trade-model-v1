#!/usr/bin/env bash
set -euo pipefail

read_provider_count() {
  local marker_file="${1:-}"
  local provider="${2:-}"
  if [[ ! -r "${marker_file}" ]]; then
    printf '%s\n' "UNKNOWN_MAX_1"
    return
  fi
  awk -F= -v provider="${provider}" '
    $1 == provider { matches += 1; value = $2 }
    END {
      if (matches == 1 && (value == "0" || value == "1")) print value
      else print "UNKNOWN_MAX_1"
    }
  ' "${marker_file}" 2>/dev/null || printf '%s\n' "UNKNOWN_MAX_1"
}

aggregate_call_count() {
  local openai="${1:-UNKNOWN_MAX_1}"
  local gemini="${2:-UNKNOWN_MAX_1}"
  local xai="${3:-UNKNOWN_MAX_1}"
  if [[ ! "${openai}" =~ ^[01]$ || ! "${gemini}" =~ ^[01]$ || ! "${xai}" =~ ^[01]$ ]]; then
    printf '%s\n' "UNKNOWN_MAX_3"
    return
  fi
  printf '%s\n' "$((openai + gemini + xai))"
}

read_stage_marker() {
  local marker_file="${1:-}"
  local stage=""
  if [[ -r "${marker_file}" ]]; then
    stage="$(tr -d '\r\n' <"${marker_file}" 2>/dev/null || true)"
  fi
  case "${stage}" in
    PRECHECK|SPRING_STARTING|SPRING_READY|ORCHESTRATOR_STARTING|PROVIDERS_SUBMITTED|ORCHESTRATOR_COMPLETED|OUTPUT_EMITTED)
      printf '%s\n' "${stage}"
      ;;
    *)
      printf '%s\n' "PRECHECK"
      ;;
  esac
}

watchdog_fired() {
  local marker_file="${1:-}"
  [[ -r "${marker_file}" && "$(tr -d '[:space:]' <"${marker_file}" 2>/dev/null || true)" == "1" ]]
}

classify_harness_failure() {
  local output_file="${1:-}"
  local watchdog_file="${2:-}"
  local process_exit="${3:-1}"
  local output_contract_present="${4:-false}"
  if watchdog_fired "${watchdog_file}"; then
    printf '%s\n' "WATCHDOG_TIMEOUT"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'CONTROLLED_SMOKE_CALL_AUDIT_UNAVAILABLE|CONTROLLED_SMOKE_CALL_LIMIT_EXCEEDED|CONTROLLED_SMOKE_STAGE_AUDIT_UNAVAILABLE|AI_PARALLEL_SMOKE_CALL_COUNT_FILE_REQUIRED' \
      "${output_file}"; then
    printf '%s\n' "CALL_COUNT_AUDIT_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'ProductionProfileSafetyGuard|PRODUCTION_PROFILE_SAFETY|PRODUCTION_SAFETY_GUARD' \
      "${output_file}"; then
    printf '%s\n' "PRODUCTION_SAFETY_GUARD_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'ScriptStatementFailedException|JdbcSQL|FlywayException|BadSqlGrammarException|Failed to initialize database|HikariPool.*Exception' \
      "${output_file}"; then
    printf '%s\n' "DATABASE_INITIALIZATION_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'UnsatisfiedDependencyException|NoSuchBeanDefinitionException|NoUniqueBeanDefinitionException|BeanCreationException|BeanDefinitionStoreException' \
      "${output_file}"; then
    printf '%s\n' "BEAN_CONFIGURATION_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'Failed to load ApplicationContext|ApplicationContext failure threshold|ApplicationContextException' \
      "${output_file}"; then
    printf '%s\n' "SPRING_CONTEXT_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      'AssertionFailedError|java\.lang\.AssertionError|Failures: [1-9]' \
      "${output_file}"; then
    printf '%s\n' "TEST_ASSERTION_FAILURE"
  elif [[ -r "${output_file}" ]] && grep -Eq \
      '\[ERROR\] Failed to execute goal|There are test failures|BUILD FAILURE' \
      "${output_file}"; then
    printf '%s\n' "MAVEN_TEST_FAILURE"
  elif [[ "${process_exit}" == "0" && "${output_contract_present}" != "true" ]]; then
    printf '%s\n' "OUTPUT_CONTRACT_MISSING"
  else
    printf '%s\n' "UNKNOWN_HARNESS_FAILURE"
  fi
}

process_exit_status() {
  local process_exit="${1:-1}"
  local watchdog_file="${2:-}"
  if watchdog_fired "${watchdog_file}"; then
    printf '%s\n' "WATCHDOG"
  elif [[ "${process_exit}" == "0" ]]; then
    printf '%s\n' "SUCCESS"
  else
    printf '%s\n' "FAILURE"
  fi
}

emit_not_run() {
  local status="${1:-FAIL_HARNESS}"
  local openai_count="${2:-0}"
  local gemini_count="${3:-0}"
  local xai_count="${4:-0}"
  local aggregate_count
  aggregate_count="$(aggregate_call_count "${openai_count}" "${gemini_count}" "${xai_count}")"
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
    "OPENAI_CALL_COUNT: ${openai_count}" \
    "GEMINI_STATUS: NOT_RUN" \
    "GEMINI_HTTP_STATUS_CLASS: NOT_RUN" \
    "GEMINI_PARSE_STATUS: NOT_RUN" \
    "GEMINI_LATENCY_MS: 0" \
    "GEMINI_CALL_COUNT: ${gemini_count}" \
    "XAI_STATUS: NOT_RUN" \
    "XAI_HTTP_STATUS_CLASS: NOT_RUN" \
    "XAI_PARSE_STATUS: NOT_RUN" \
    "XAI_LATENCY_MS: 0" \
    "XAI_CALL_COUNT: ${xai_count}" \
    "FINAL_RESULT_ORDER: NOT_RUN" \
    "LIVE_PROVIDER_CALLS: ${aggregate_count}" \
    "REAL_KEYS_READ: 0" \
    "PRODUCTION_READINESS: BLOCKED"
}

emit_failure() {
  local output_file="${1:-}"
  local call_count_file="${2:-}"
  local stage_file="${3:-}"
  local watchdog_file="${4:-}"
  local process_exit="${5:-1}"
  local output_contract_present="${6:-false}"
  local openai_count gemini_count xai_count aggregate_count category process_status stage
  openai_count="$(read_provider_count "${call_count_file}" "OPENAI")"
  gemini_count="$(read_provider_count "${call_count_file}" "GEMINI")"
  xai_count="$(read_provider_count "${call_count_file}" "XAI")"
  aggregate_count="$(aggregate_call_count "${openai_count}" "${gemini_count}" "${xai_count}")"
  category="$(classify_harness_failure "${output_file}" "${watchdog_file}" \
      "${process_exit}" "${output_contract_present}")"
  process_status="$(process_exit_status "${process_exit}" "${watchdog_file}")"
  stage="$(read_stage_marker "${stage_file}")"
  printf '%s\n' \
    "AI_PARALLEL_LIVE_SMOKE: FAIL_HARNESS" \
    "AI_PARALLEL_SMOKE_STATUS: FAIL_HARNESS" \
    "AI_PARALLEL_HARNESS_FAILURE_CATEGORY: ${category}" \
    "AI_PARALLEL_HARNESS_PROCESS_EXIT: ${process_status}" \
    "AI_PARALLEL_HARNESS_STAGE: ${stage}" \
    "ORCHESTRATION_MODE: NOT_RUN" \
    "OPENAI_STATUS: NOT_RUN" \
    "OPENAI_CALL_COUNT: ${openai_count}" \
    "GEMINI_STATUS: NOT_RUN" \
    "GEMINI_CALL_COUNT: ${gemini_count}" \
    "XAI_STATUS: NOT_RUN" \
    "XAI_CALL_COUNT: ${xai_count}" \
    "LIVE_PROVIDER_CALLS: ${aggregate_count}" \
    "REAL_KEYS_READ: 0" \
    "PRODUCTION_READINESS: BLOCKED"
}

main() {
  if [[ "${AI_PARALLEL_SMOKE_ENABLE_EXTERNAL_CALLS:-false}" != "true" ]]; then
    emit_not_run "SKIPPED_EXTERNAL_CALLS_DISABLED" "0" "0" "0"
    return 0
  fi
  if [[ "${AI_PARALLEL_SMOKE_HARNESS_ENTRY:-}" != "I_CONFIRM_THREE_PROVIDER_PARALLEL_SMOKE" ]]; then
    emit_not_run "SKIPPED_HARNESS_ENTRY_MISSING" "0" "0" "0"
    return 0
  fi
  if [[ "${TRADE_MODEL_AI_ENABLED:-false}" != "true"
        || "${TRADE_MODEL_AI_OPENAI_ENABLED:-false}" != "true"
        || "${TRADE_MODEL_AI_GEMINI_ENABLED:-false}" != "true"
        || "${TRADE_MODEL_AI_XAI_ENABLED:-false}" != "true" ]]; then
    emit_not_run "SKIPPED_PROVIDER_DISABLED" "0" "0" "0"
    return 0
  fi
  if [[ -z "${OPENAI_API_KEY:-}" || -z "${GEMINI_API_KEY:-}" || -z "${XAI_API_KEY:-}" ]]; then
    emit_not_run "SKIPPED_MISSING_API_KEY" "0" "0" "0"
    return 0
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

  local output_file call_count_file stage_file watchdog_file
  output_file="$(mktemp)"
  call_count_file="$(mktemp)"
  stage_file="$(mktemp)"
  watchdog_file="$(mktemp)"
  chmod 600 "${output_file}" "${call_count_file}" "${stage_file}" "${watchdog_file}"
  printf '%s\n' "OPENAI=0" "GEMINI=0" "XAI=0" >"${call_count_file}"
  printf '%s\n' "PRECHECK" >"${stage_file}"
  printf '%s\n' "0" >"${watchdog_file}"
  export AI_PARALLEL_SMOKE_CALL_COUNT_FILE="${call_count_file}"
  export AI_PARALLEL_SMOKE_STAGE_FILE="${stage_file}"
  trap 'rm -f "${output_file}" "${call_count_file}" "${stage_file}" "${watchdog_file}"' EXIT

  ./mvnw -q \
    -Dtest=AiParallelOrchestratorControlledSmokeTest#controlledLiveSmokeEntryPoint \
    test >"${output_file}" 2>&1 &
  local smoke_pid=$!
  local watchdog_seconds="${AI_PARALLEL_SMOKE_WATCHDOG_SECONDS:-60}"
  if [[ ! "${watchdog_seconds}" =~ ^[1-9][0-9]*$ || "${watchdog_seconds}" -gt 60 ]]; then
    watchdog_seconds=60
  fi
  (
    sleep "${watchdog_seconds}"
    if kill -0 "${smoke_pid}" 2>/dev/null; then
      printf '%s\n' "1" >"${watchdog_file}"
      kill -TERM "${smoke_pid}" 2>/dev/null || true
    fi
  ) &
  local watchdog_pid=$!

  local smoke_exit
  if wait "${smoke_pid}"; then
    smoke_exit=0
  else
    smoke_exit=$?
  fi
  kill "${watchdog_pid}" 2>/dev/null || true
  wait "${watchdog_pid}" 2>/dev/null || true

  local allowed_output output_contract_present=false
  allowed_output="$(awk '/^(AI_PARALLEL_LIVE_SMOKE|AI_PARALLEL_SMOKE_STATUS|ORCHESTRATION_MODE|ORCHESTRATION_LATENCY_MS|GLOBAL_DEADLINE_EXCEEDED|PROVIDER_SUBMITTED_COUNT|PROVIDER_COMPLETED_COUNT|PROVIDER_SUCCESS_COUNT|PROVIDER_TIMEOUT_COUNT|PROVIDER_FAILED_COUNT|PARTIAL_FALLBACK_USED|OPENAI_STATUS|OPENAI_HTTP_STATUS_CLASS|OPENAI_PARSE_STATUS|OPENAI_LATENCY_MS|OPENAI_CALL_COUNT|GEMINI_STATUS|GEMINI_HTTP_STATUS_CLASS|GEMINI_PARSE_STATUS|GEMINI_LATENCY_MS|GEMINI_CALL_COUNT|XAI_STATUS|XAI_HTTP_STATUS_CLASS|XAI_PARSE_STATUS|XAI_LATENCY_MS|XAI_CALL_COUNT|FINAL_RESULT_ORDER|LIVE_PROVIDER_CALLS|REAL_KEYS_READ|PRODUCTION_READINESS): / { print }' "${output_file}")"
  if [[ "${allowed_output}" == *"AI_PARALLEL_LIVE_SMOKE: "*
        && "${allowed_output}" == *"LIVE_PROVIDER_CALLS: "* ]]; then
    output_contract_present=true
  fi
  if [[ "${smoke_exit}" -ne 0 || "${output_contract_present}" != "true" ]]; then
    emit_failure "${output_file}" "${call_count_file}" "${stage_file}" \
      "${watchdog_file}" "${smoke_exit}" "${output_contract_present}"
    return 1
  fi
  printf '%s\n' "${allowed_output}"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
