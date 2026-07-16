#!/usr/bin/env bash
set -euo pipefail

candidate_file="${1:-}"
approved_fingerprint="${2:-}"
output_file="${3:-}"

if [ -z "${candidate_file}" ] || [ ! -f "${candidate_file}" ] \
    || [ -L "${candidate_file}" ] || [ -z "${output_file}" ]; then
  echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_INPUT"
  exit 2
fi
case "${approved_fingerprint}" in
  SHA256:*) ;;
  *) echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_FINGERPRINT"; exit 2 ;;
esac

output_parent="$(dirname "${output_file}")"
if [ ! -d "${output_parent}" ] || [ -L "${output_parent}" ]; then
  echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_OUTPUT"
  exit 2
fi

temporary_output="$(mktemp "${output_parent}/known-hosts-filtered.XXXXXX")"
line_file="$(mktemp "${output_parent}/known-host-line.XXXXXX")"
trap 'rm -f "${temporary_output}" "${line_file}"' EXIT HUP INT TERM
chmod 600 "${temporary_output}" "${line_file}"

match_count=0
while IFS= read -r candidate_line || [ -n "${candidate_line}" ]; do
  case "${candidate_line}" in
    ""|'#'*) continue ;;
  esac
  printf '%s\n' "${candidate_line}" >"${line_file}"
  fingerprint="$(ssh-keygen -lf "${line_file}" -E sha256 2>/dev/null \
    | awk 'NR == 1 {print $2}')"
  [ -n "${fingerprint}" ] \
    || { echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_INVALID_CANDIDATE"; exit 2; }
  if [ "${fingerprint}" = "${approved_fingerprint}" ]; then
    printf '%s\n' "${candidate_line}" >>"${temporary_output}"
    match_count=$((match_count + 1))
  fi
done <"${candidate_file}"

if [ "${match_count}" -eq 0 ]; then
  echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_ZERO_MATCHES"
  exit 2
fi
if [ "${match_count}" -ne 1 ]; then
  echo "P3H_SSH_KNOWN_HOSTS_FILTER: BLOCKED_MULTIPLE_MATCHES"
  exit 2
fi

mv "${temporary_output}" "${output_file}"
chmod 600 "${output_file}"
echo "SSH_KNOWN_HOSTS_FILTER: PASS_EXACT_PIN"
echo "SSH_APPROVED_HOST_KEY_LINES: 1"
