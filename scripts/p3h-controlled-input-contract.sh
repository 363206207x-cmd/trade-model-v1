#!/usr/bin/env bash
set -euo pipefail

p3h_has_control_character() {
  printf '%s' "$1" | LC_ALL=C grep -q '[[:cntrl:]]'
}

p3h_validate_dns_name() {
  local value="$1"
  local label
  local -a labels

  [ -n "${value}" ] && [ "${#value}" -le 253 ] || return 1
  [ "${value}" = "$(printf '%s' "${value}" | LC_ALL=C tr '[:upper:]' '[:lower:]')" ] \
    || return 1
  p3h_has_control_character "${value}" && return 1
  [[ "${value}" != .* && "${value}" != *. ]] || return 1
  [[ "${value}" != *..* ]] || return 1
  [[ "${value}" =~ ^[a-z0-9.-]+$ ]] || return 1

  IFS='.' read -r -a labels <<<"${value}"
  [ "${#labels[@]}" -ge 2 ] || return 1
  for label in "${labels[@]}"; do
    [ -n "${label}" ] && [ "${#label}" -le 63 ] || return 1
    [[ "${label}" =~ ^[a-z0-9]([a-z0-9-]*[a-z0-9])?$ ]] || return 1
  done
}

p3h_validate_staging_hostname() {
  local value="$1"
  local label
  local staging_marker=0
  local -a labels

  p3h_validate_dns_name "${value}" || return 1
  IFS='.' read -r -a labels <<<"${value}"
  [ "${#labels[@]}" -ge 3 ] || return 1
  for label in "${labels[@]}"; do
    case "${label}" in
      stage|staging|preprod|pre-prod|uat|stage-*|staging-*|preprod-*|uat-*|*-stage|*-staging)
        staging_marker=1
        ;;
    esac
  done
  [ "${staging_marker}" -eq 1 ]
}

p3h_validate_ipv4() {
  local value="$1"
  local octet
  local -a octets

  [[ "${value}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || return 1
  IFS='.' read -r -a octets <<<"${value}"
  [ "${#octets[@]}" -eq 4 ] || return 1
  for octet in "${octets[@]}"; do
    [ "${#octet}" -le 3 ] || return 1
    [[ "${octet}" = "0" || "${octet}" != 0* ]] || return 1
    [ "$((10#${octet}))" -le 255 ] || return 1
  done
}

p3h_validate_ssh_host() {
  local value="$1"

  [ -n "${value}" ] || return 1
  p3h_has_control_character "${value}" && return 1
  [[ "${value}" != -* ]] || return 1
  if p3h_validate_ipv4 "${value}"; then
    return 0
  fi
  p3h_validate_dns_name "${value}"
}

p3h_validate_ssh_user() {
  local value="$1"

  [[ "${value}" =~ ^[a-z_][a-z0-9_-]{0,31}$ ]] || return 1
  case "${value}" in
    root|daemon|bin|nobody|postgres) return 1 ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  contract_type="${1:-}"
  contract_value="${2:-}"
  case "${contract_type}" in
    STAGING_HOSTNAME)
      p3h_validate_staging_hostname "${contract_value}" || exit 2
      echo "STAGING_HOSTNAME_CONTRACT: PASS_STRICT_DNS"
      ;;
    SSH_HOST)
      p3h_validate_ssh_host "${contract_value}" || exit 2
      echo "SSH_HOST_CONTRACT: PASS_STRICT"
      ;;
    SSH_USER)
      p3h_validate_ssh_user "${contract_value}" || exit 2
      echo "SSH_USER_CONTRACT: PASS_STRICT"
      ;;
    *) exit 2 ;;
  esac
fi
