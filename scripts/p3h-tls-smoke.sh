#!/usr/bin/env bash
set -euo pipefail

base_url="${P3H_HTTPS_BASE_URL:-}"
hostname="${P3H_STAGING_HOSTNAME:-}"
tls_mode="${P3H_TLS_MODE:-}"
ca_bundle="${P3H_CA_BUNDLE_FILE:-}"
approved_port="${P3H_APPROVED_HTTPS_PORT:-443}"

openssl_probe() {
  openssl "$@" </dev/null >/dev/null 2>&1 &
  local probe_pid=$!
  local elapsed_ticks=0
  while kill -0 "${probe_pid}" >/dev/null 2>&1 && [ "${elapsed_ticks}" -lt 200 ]; do
    sleep 0.1
    elapsed_ticks=$((elapsed_ticks + 1))
  done
  if kill -0 "${probe_pid}" >/dev/null 2>&1; then
    kill "${probe_pid}" >/dev/null 2>&1 || true
    wait "${probe_pid}" >/dev/null 2>&1 || true
    return 124
  fi
  local probe_status
  if wait "${probe_pid}"; then
    probe_status=0
  else
    probe_status=$?
  fi
  return "${probe_status}"
}

openssl_verified_probe() {
  if [ "${tls_mode}" = "INTERNAL_CA" ]; then
    openssl_probe s_client "$@" -CAfile "${ca_bundle}" -verify_return_error
  else
    openssl_probe s_client "$@" -verify_return_error
  fi
}

client_supports_tls13() {
  openssl s_client -help 2>&1 | grep -q -- '-tls1_3'
}

require_legacy_tls_rejected() {
  local result_label="$1"
  local protocol_flag="$2"
  local protocol_status
  if openssl_probe s_client -connect "${hostname}:${https_port}" \
      -servername "${hostname}" "${protocol_flag}"; then
    protocol_status=0
  else
    protocol_status=$?
  fi
  if [ "${protocol_status}" -eq 0 ]; then
    echo "${result_label}: FAIL"
    exit 2
  fi
  if [ "${protocol_status}" -eq 124 ]; then
    echo "${result_label}: BLOCKED_TIMEOUT"
    exit 2
  fi
  if ! openssl_verified_probe -connect "${hostname}:${https_port}" \
      -servername "${hostname}" -verify_hostname "${hostname}" -tls1_2; then
    echo "${result_label}: BLOCKED_CONNECTIVITY_NOT_PROTOCOL"
    exit 2
  fi
  echo "${result_label}: PASS"
}

if [ -z "${base_url}" ] || [ -z "${hostname}" ]; then
  echo "P3H_TLS_SMOKE: BLOCKED_MISSING_INPUT"
  exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "P3H_TLS_SMOKE: BLOCKED_URL_PARSER_MISSING"
  exit 2
fi

if ! https_port="$(python3 - "${base_url}" "${hostname}" "${approved_port}" <<'PY'
from urllib.parse import urlsplit
import sys

base_url, expected_host, approved_port_text = sys.argv[1:]
try:
    approved_port = int(approved_port_text)
    parsed = urlsplit(base_url)
    actual_port = parsed.port or 443
except (ValueError, TypeError):
    raise SystemExit(2)

valid = (
    parsed.scheme == "https"
    and parsed.hostname == expected_host
    and parsed.username is None
    and parsed.password is None
    and parsed.path == ""
    and parsed.query == ""
    and parsed.fragment == ""
    and 1 <= approved_port <= 65535
    and actual_port == approved_port
)
if not valid:
    raise SystemExit(2)
print(actual_port)
PY
)"; then
  echo "P3H_TLS_SMOKE: BLOCKED_TARGET_BINDING"
  exit 2
fi

curl_args=(--silent --show-error --max-time 20)
case "${tls_mode}" in
  PUBLIC_CA) ;;
  INTERNAL_CA)
    if [ ! -f "${ca_bundle}" ] || [ -L "${ca_bundle}" ]; then
      echo "P3H_TLS_SMOKE: BLOCKED_CA_BUNDLE"
      exit 2
    fi
    curl_args+=(--cacert "${ca_bundle}")
    ;;
  *) echo "P3H_TLS_SMOKE: BLOCKED_TLS_MODE"; exit 2 ;;
esac

request_code="$(curl "${curl_args[@]}" \
  --output /dev/null --write-out '%{http_code}' "${base_url}/actuator/health")"
[ "${request_code}" = "200" ] \
  || { echo "P3H_TLS_SMOKE: BLOCKED_HEALTH"; exit 2; }

openssl_verified_probe -connect "${hostname}:${https_port}" -servername "${hostname}" \
  -verify_hostname "${hostname}" \
  || { echo "P3H_TLS_SMOKE: BLOCKED_CERTIFICATE_HOSTNAME"; exit 2; }

openssl_verified_probe -connect "${hostname}:${https_port}" -servername "${hostname}" \
  -verify_hostname "${hostname}" -tls1_2 \
  || { echo "TLS_1_2: FAIL"; exit 2; }
echo "TLS_1_2: PASS"

if client_supports_tls13; then
  if openssl_verified_probe -connect "${hostname}:${https_port}" -servername "${hostname}" \
      -verify_hostname "${hostname}" -tls1_3; then
    echo "TLS_1_3: PASS"
  else
    echo "TLS_1_3: FAIL"
    exit 2
  fi
else
  echo "TLS_1_3: ENVIRONMENT_NOT_SUPPORTED"
fi

require_legacy_tls_rejected "TLS_1_0_REJECTED" "-tls1"
require_legacy_tls_rejected "TLS_1_1_REJECTED" "-tls1_1"
echo "HEALTH_AND_CERTIFICATE_TARGET: MATCH"
echo "P3H_TLS_SMOKE: PASS_VERIFIED"
