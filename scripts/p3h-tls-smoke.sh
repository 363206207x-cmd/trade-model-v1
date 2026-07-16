#!/usr/bin/env bash
set -euo pipefail

base_url="${P3H_HTTPS_BASE_URL:-}"
hostname="${P3H_STAGING_HOSTNAME:-}"
tls_mode="${P3H_TLS_MODE:-}"
ca_bundle="${P3H_CA_BUNDLE_FILE:-}"

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

require_legacy_tls_rejected() {
  local result_label="$1"
  local protocol_flag="$2"
  local protocol_status
  if openssl_probe s_client -connect "${hostname}:443" -servername "${hostname}" \
      "${protocol_flag}"; then
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
  echo "${result_label}: PASS"
}

if [ -z "${base_url}" ] || [ -z "${hostname}" ]; then
  echo "P3H_TLS_SMOKE: BLOCKED_MISSING_INPUT"
  exit 2
fi
case "${base_url}" in
  https://*) ;;
  *) echo "P3H_TLS_SMOKE: BLOCKED_NON_HTTPS"; exit 2 ;;
esac
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

openssl_verified_probe -connect "${hostname}:443" -servername "${hostname}" \
  -verify_hostname "${hostname}" \
  || { echo "P3H_TLS_SMOKE: BLOCKED_CERTIFICATE_HOSTNAME"; exit 2; }

openssl_verified_probe -connect "${hostname}:443" -servername "${hostname}" \
  -verify_hostname "${hostname}" -tls1_2 \
  || { echo "TLS_1_2: FAIL"; exit 2; }
echo "TLS_1_2: PASS"

if openssl_verified_probe -connect "${hostname}:443" -servername "${hostname}" \
    -verify_hostname "${hostname}" -tls1_3; then
  echo "TLS_1_3: PASS"
else
  echo "TLS_1_3: ENVIRONMENT_NOT_SUPPORTED"
fi

require_legacy_tls_rejected "TLS_1_0_REJECTED" "-tls1"
require_legacy_tls_rejected "TLS_1_1_REJECTED" "-tls1_1"
echo "P3H_TLS_SMOKE: PASS_VERIFIED"
