#!/usr/bin/env bash

set -euo pipefail

TARGET_BASE_URL="${STRESS_TARGET_BASE_URL:-http://localhost:8081}"
CONCURRENCY_LEVELS="${STRESS_CONCURRENCY_LEVELS:-1,5,10,20}"
REQUESTS_PER_ENDPOINT="${STRESS_REQUESTS_PER_ENDPOINT:-100}"
OUTPUT_DIR="${STRESS_OUTPUT_DIR:-build/stress-dashboard}"
TIMEOUT_SECONDS="${STRESS_TIMEOUT_SECONDS:-10}"
CONFIRMATION="${DASHBOARD_STRESS_CONFIRM:-NO}"
MAX_5XX_COUNT="${STRESS_MAX_5XX_COUNT:-0}"
AUTH_FAILURE_STOP_COUNT="${STRESS_AUTH_FAILURE_STOP_COUNT:-3}"

ENDPOINTS=(
  "/actuator/health"
  "/dashboard"
  "/api/dashboard/home"
)

DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: bash scripts/dashboard-stress-local.sh [--dry-run]

Default behavior is dry-run unless DASHBOARD_STRESS_CONFIRM=YES is set.
Allowed target URLs are local only, for example http://localhost:8081.

Required for actual execution:
  DASHBOARD_STRESS_CONFIRM=YES
  APP_ADMIN_USERNAME=<set>
  APP_ADMIN_PASSWORD=<set>

Optional configuration:
  STRESS_TARGET_BASE_URL=http://localhost:8081
  STRESS_CONCURRENCY_LEVELS="1,5,10,20"
  STRESS_REQUESTS_PER_ENDPOINT=100
  STRESS_OUTPUT_DIR=build/stress-dashboard
  STRESS_TIMEOUT_SECONDS=10
EOF
}

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

trim_trailing_slash() {
  local value="$1"
  while [[ "$value" == */ ]]; do
    value="${value%/}"
  done
  printf '%s' "$value"
}

TARGET_BASE_URL="$(trim_trailing_slash "$TARGET_BASE_URL")"

is_local_target() {
  case "$TARGET_BASE_URL" in
    http://localhost|http://localhost:[0-9]*|http://127.0.0.1|http://127.0.0.1:[0-9]*|http://[::1]|http://[::1]:[0-9]*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

contains_production_marker() {
  printf '%s' "$TARGET_BASE_URL" | awk '{ v=tolower($0); if (v ~ /(prod|production|live|primary)/) exit 0; exit 1 }'
}

print_plan() {
  echo "DASHBOARD_STRESS_PLAN"
  echo "target_url=${TARGET_BASE_URL}"
  echo "endpoints=${ENDPOINTS[*]}"
  echo "concurrency_levels=${CONCURRENCY_LEVELS}"
  echo "requests_per_endpoint=${REQUESTS_PER_ENDPOINT}"
  echo "timeout_seconds=${TIMEOUT_SECONDS}"
  echo "output_dir=${OUTPUT_DIR}"
  echo "confirmation=${CONFIRMATION}"
  if [[ -n "${APP_ADMIN_USERNAME:-}" ]]; then
    echo "auth_username=present"
  else
    echo "auth_username=missing"
  fi
  if [[ -n "${APP_ADMIN_PASSWORD:-}" ]]; then
    echo "auth_password=present_redacted"
  else
    echo "auth_password=missing"
  fi
}

if ! is_local_target; then
  print_plan
  echo "REFUSED: STRESS_TARGET_BASE_URL must be local-only http://localhost:8081, http://127.0.0.1:8081, or equivalent loopback HTTP URL." >&2
  exit 2
fi

if contains_production_marker; then
  print_plan
  echo "REFUSED: target URL contains a production-like marker." >&2
  exit 2
fi

if [[ "$DRY_RUN" == "true" || "$CONFIRMATION" != "YES" ]]; then
  print_plan
  echo "DRY_RUN: no requests sent. Set DASHBOARD_STRESS_CONFIRM=YES to execute in a future approved package."
  exit 0
fi

if [[ -z "${APP_ADMIN_USERNAME:-}" || -z "${APP_ADMIN_PASSWORD:-}" ]]; then
  print_plan
  echo "REFUSED: APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD are required for actual stress execution. Password is never printed." >&2
  exit 2
fi

if [[ "${APP_ADMIN_USERNAME}${APP_ADMIN_PASSWORD}" == *'"'* || "${APP_ADMIN_USERNAME}${APP_ADMIN_PASSWORD}" == *'\\'* ]]; then
  echo "REFUSED: credentials contain characters unsupported by this redacted curl config writer." >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "REFUSED: curl is required." >&2
  exit 2
fi

if ! command -v awk >/dev/null 2>&1 || ! command -v sort >/dev/null 2>&1; then
  echo "REFUSED: awk and sort are required for metrics." >&2
  exit 2
fi

mkdir -p "$OUTPUT_DIR"
SUMMARY_FILE="$OUTPUT_DIR/summary.txt"
CSV_FILE="$OUTPUT_DIR/endpoint-results.csv"
FAILURES_FILE="$OUTPUT_DIR/failures.log"
ENV_FILE="$OUTPUT_DIR/environment.txt"

: > "$FAILURES_FILE"
cat > "$SUMMARY_FILE" <<EOF
Dashboard local stress run
target_url=${TARGET_BASE_URL}
confirmation=${CONFIRMATION}
production_readiness=BLOCKED
write_endpoints=none
provider_calls=none_intended
EOF

cat > "$ENV_FILE" <<EOF
timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
target_url=${TARGET_BASE_URL}
endpoints=${ENDPOINTS[*]}
concurrency_levels=${CONCURRENCY_LEVELS}
requests_per_endpoint=${REQUESTS_PER_ENDPOINT}
timeout_seconds=${TIMEOUT_SECONDS}
auth_username_present=yes
auth_password_present_redacted=yes
EOF

echo "timestamp,endpoint,concurrency,total,success_count,failure_count,status_distribution,avg_latency_seconds,max_latency_seconds,p95_latency_seconds,stop_reason" > "$CSV_FILE"

AUTH_CONFIG="$(mktemp "${TMPDIR:-/tmp}/dashboard-stress-curl.XXXXXX")"
chmod 600 "$AUTH_CONFIG"
cat > "$AUTH_CONFIG" <<EOF
user = "${APP_ADMIN_USERNAME}:${APP_ADMIN_PASSWORD}"
EOF
trap 'rm -f "$AUTH_CONFIG"' EXIT

safe_name() {
  printf '%s' "$1" | sed 's#[^A-Za-z0-9._-]#_#g'
}

request_once() {
  local endpoint="$1"
  local raw_file="$2"
  local body_file
  local url
  local curl_output
  local curl_exit=0
  local status
  local latency
  body_file="$(mktemp "${TMPDIR:-/tmp}/dashboard-stress-body.XXXXXX")"
  url="${TARGET_BASE_URL}${endpoint}"
  curl_output="$(curl --silent --show-error --max-time "$TIMEOUT_SECONDS" --config "$AUTH_CONFIG" --output "$body_file" --write-out '%{http_code},%{time_total}' "$url" 2>>"$FAILURES_FILE")" || curl_exit=$?
  if [[ "$curl_exit" -ne 0 ]]; then
    status="CURL_EXIT_${curl_exit}"
    latency="0"
  else
    status="${curl_output%%,*}"
    latency="${curl_output#*,}"
  fi
  if grep -Eiq 'production[ -]?ready|生产就绪' "$body_file"; then
    status="PROD_READY_CLAIM"
  fi
  rm -f "$body_file"
  printf '%s,%s,%s,%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$endpoint" "$status" "$latency" >> "$raw_file"
}

analyze_raw_file() {
  local raw_file="$1"
  local endpoint="$2"
  local concurrency="$3"
  local p95
  local metrics
  p95="$(awk -F, 'NF >= 4 { print $4 + 0 }' "$raw_file" | sort -n | awk '{ values[NR]=$1 } END { if (NR == 0) { print "0" } else { idx=int(NR*0.95); if (idx < 1) idx=1; print values[idx] } }')"
  metrics="$(awk -F, -v max5xx="$MAX_5XX_COUNT" -v authstop="$AUTH_FAILURE_STOP_COUNT" -v p95="$p95" '
    NF >= 4 {
      total++
      status=$3
      latency=$4 + 0
      dist[status]++
      if (status ~ /^[23][0-9][0-9]$/) success++; else failure++
      if (status ~ /^5[0-9][0-9]$/) fivexx++
      if (status == "401" || status == "403") authfail++
      if (status ~ /^CURL_EXIT_/) curlexit++
      if (status == "PROD_READY_CLAIM") prodclaim++
      sum += latency
      if (latency > max) max=latency
    }
    END {
      if (total == 0) total=0
      avg=(total > 0 ? sum / total : 0)
      stop=""
      if (fivexx > max5xx) stop="5XX_RATE_THRESHOLD"
      else if (authfail >= authstop) stop="AUTH_MISCONFIG"
      else if (curlexit > 0) stop="CONNECTION_OR_TIMEOUT"
      else if (prodclaim > 0) stop="PRODUCTION_READY_CLAIM_DETECTED"
      dist_text=""
      for (s in dist) dist_text=dist_text s "=" dist[s] ";"
      printf "%d,%d,%d,%s,%.6f,%.6f,%.6f,%s", total, success + 0, failure + 0, dist_text, avg, max, p95, stop
    }
  ' "$raw_file")"
  printf '%s,%s,%s,%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$endpoint" "$concurrency" "$metrics" >> "$CSV_FILE"
  if [[ "$metrics" == *",5XX_RATE_THRESHOLD" || "$metrics" == *",AUTH_MISCONFIG" || "$metrics" == *",CONNECTION_OR_TIMEOUT" || "$metrics" == *",PRODUCTION_READY_CLAIM_DETECTED" ]]; then
    return 1
  fi
  return 0
}

run_endpoint_level() {
  local endpoint="$1"
  local concurrency="$2"
  local raw_file
  local token_fifo
  local i
  raw_file="$OUTPUT_DIR/raw-$(safe_name "$endpoint")-c${concurrency}.csv"
  : > "$raw_file"
  token_fifo="$(mktemp -u "${TMPDIR:-/tmp}/dashboard-stress-fifo.XXXXXX")"
  mkfifo "$token_fifo"
  exec 9<>"$token_fifo"
  rm -f "$token_fifo"
  for ((i = 0; i < concurrency; i++)); do
    printf '.' >&9
  done
  for ((i = 1; i <= REQUESTS_PER_ENDPOINT; i++)); do
    read -r -n 1 _ <&9
    {
      request_once "$endpoint" "$raw_file"
      printf '.' >&9
    } &
  done
  wait
  exec 9>&-
  exec 9<&-
  analyze_raw_file "$raw_file" "$endpoint" "$concurrency"
}

print_plan | tee -a "$SUMMARY_FILE"

IFS=',' read -r -a LEVELS <<< "$CONCURRENCY_LEVELS"
for level in "${LEVELS[@]}"; do
  level="${level//[[:space:]]/}"
  if ! [[ "$level" =~ ^[0-9]+$ ]] || [[ "$level" -lt 1 ]]; then
    echo "REFUSED: invalid concurrency level: $level" >&2
    exit 2
  fi
  for endpoint in "${ENDPOINTS[@]}"; do
    echo "RUN endpoint=${endpoint} concurrency=${level}" | tee -a "$SUMMARY_FILE"
    if ! run_endpoint_level "$endpoint" "$level"; then
      echo "STOPPED: stop condition reached for endpoint=${endpoint} concurrency=${level}. See ${CSV_FILE} and ${FAILURES_FILE}." | tee -a "$SUMMARY_FILE" >&2
      exit 3
    fi
  done
done

echo "COMPLETED: dashboard local stress run finished without configured stop condition." | tee -a "$SUMMARY_FILE"
