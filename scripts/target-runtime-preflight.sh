#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_log="$(mktemp)"
probe_output="$(mktemp)"
chmod 600 "${build_log}" "${probe_output}"
trap 'rm -f -- "${build_log}" "${probe_output}"' EXIT

if ! (cd "${repo_root}" && ./mvnw -q -DskipTests compile >"${build_log}" 2>&1); then
  printf '%s\n' "PREFLIGHT_BUILD=FAILED"
  exit 1
fi

if ! java -cp "${repo_root}/target/classes" org.example.trademodel.config.TargetRuntimePreflight; then
  exit 1
fi

if [[ "${1:-}" != "--probe" ]]; then
  printf '%s\n' "AI_EXACT_MODEL_PROBE=SKIPPED"
  exit 0
fi

base_url="${TARGET_RUNTIME_PREFLIGHT_BASE_URL:-}"
cookie_file="${TARGET_RUNTIME_PREFLIGHT_COOKIE_FILE:-}"
csrf_header_file="${TARGET_RUNTIME_PREFLIGHT_CSRF_HEADER_FILE:-}"
if [[ -z "${base_url}" || ! -r "${cookie_file}" || ! -r "${csrf_header_file}" ]]; then
  printf '%s\n' "AI_EXACT_MODEL_PROBE=BLOCKED_MISSING_AUTH_CONTEXT"
  exit 1
fi

for provider in OPENAI GEMINI XAI; do
  if curl --fail --silent --show-error \
      --cookie "${cookie_file}" \
      --header "@${csrf_header_file}" \
      --request POST \
      --output "${probe_output}" \
      "${base_url%/}/api/ai/providers/${provider}/reverify" \
      && grep -Eq '"state"[[:space:]]*:[[:space:]]*"AUTHORIZED"' "${probe_output}"; then
    printf '%s\n' "${provider}_EXACT_MODEL_PROBE=AUTHORIZED"
  else
    printf '%s\n' "${provider}_EXACT_MODEL_PROBE=BLOCKED"
    exit 1
  fi
  : > "${probe_output}"
done
printf '%s\n' "AI_EXACT_MODEL_PROBE=PASS"
