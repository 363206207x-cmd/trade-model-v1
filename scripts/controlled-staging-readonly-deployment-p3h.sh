#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION="I_CONFIRM_AUTHORIZED_NON_PRODUCTION_STAGING_DEPLOYMENT"
EXPECTED_REBOOT_CONFIRMATION="I_CONFIRM_CONTROLLED_STAGING_SERVER_REBOOT"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_REAL="$(realpath "${ROOT_DIR}")"
EVIDENCE_DIR="${ROOT_DIR}/.runtime/p3h-staging-evidence"
CURRENT_STAGE="input-presence"
NETWORK_ACCESS_STARTED=0
TMP_DIR=""

P4_ALLOWED="NO"
PRODUCTION_READINESS="BLOCKED"

required_inputs=(
  P3H_CONFIRM
  P3H_SERVER_ATTESTATION_FILE
  P3H_SECRET_BACKEND_ATTESTATION_FILE
  P3H_SSH_HOST
  P3H_SSH_PORT
  P3H_SSH_USER
  P3H_SSH_IDENTITY_FILE
  P3H_SSH_HOST_KEY_SHA256
  P3H_STAGING_HOSTNAME
  P3H_TLS_MODE
  P3H_SECRET_BACKEND
  P3H_SECRET_MOUNT_DIR
  P3H_RELEASE_OWNER_REFERENCE
  P3H_ROLLBACK_OWNER_REFERENCE
  P3H_INCIDENT_OWNER_REFERENCE
  P3H_REBOOT_CONFIRM
  P3H_KEEP_STAGING_RUNNING
)

server_attestation_keys=(
  ENVIRONMENT_CLASS
  PRODUCTION_TRAFFIC
  PRODUCTION_DATABASE
  PRODUCTION_SECRETS
  AUTHORIZED_FOR_P3H
  DISPOSABLE_OR_REBUILDABLE
  LINUX_SERVER
  EXPECTED_SSH_HOST_KEY_SHA256
  EXPECTED_STAGING_HOSTNAME
  SERVER_OWNER_REFERENCE
  APPROVAL_REFERENCE
)

secret_attestation_keys=(
  SECRET_BACKEND_CLASS
  BACKEND_VERSION
  AUTHORIZED_FOR_P3H
  PLAINTEXT_AT_REST
  SECRETS_VERSIONED_OR_ROTATABLE
  SECRET_MOUNT_IS_RUNTIME_ONLY
  SECRET_OWNER_REFERENCE
  ROTATION_ALLOWED
)

umask 077
mkdir -p "${EVIDENCE_DIR}"

cleanup() {
  if [ -n "${TMP_DIR}" ] && [ -d "${TMP_DIR}" ]; then
    rm -rf "${TMP_DIR}"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

wait_for_bounded_pid() {
  local command_pid="$1"
  local timeout_seconds="$2"
  local elapsed_ticks=0
  local max_ticks=$((timeout_seconds * 10))
  while kill -0 "${command_pid}" >/dev/null 2>&1 \
      && [ "${elapsed_ticks}" -lt "${max_ticks}" ]; do
    sleep 0.1
    elapsed_ticks=$((elapsed_ticks + 1))
  done
  if kill -0 "${command_pid}" >/dev/null 2>&1; then
    kill "${command_pid}" >/dev/null 2>&1 || true
    wait "${command_pid}" >/dev/null 2>&1 || true
    return 124
  fi
  set +e
  wait "${command_pid}"
  local command_status=$?
  set -e
  return "${command_status}"
}

run_bounded() {
  local timeout_seconds="$1"
  shift
  "$@" &
  wait_for_bounded_pid "$!" "${timeout_seconds}"
}

write_summary() {
  local result="$1"
  local server_access="$2"
  local secret_access="$3"
  {
    echo "P3H_RESULT: ${result}"
    echo "FAILED_OR_CURRENT_STAGE: ${CURRENT_STAGE}"
    echo "SERVER_ACCESS: ${server_access}"
    echo "SECRET_ACCESS: ${secret_access}"
    echo "P4_ALLOWED: NO"
    echo "PRODUCTION_READINESS: BLOCKED"
  } >"${EVIDENCE_DIR}/summary.txt"
}

blocked() {
  local result="$1"
  local server_access="${2:-NOT_ATTEMPTED}"
  local secret_access="${3:-NOT_ATTEMPTED}"
  local status="${4:-2}"
  write_summary "${result}" "${server_access}" "${secret_access}"
  cat "${EVIDENCE_DIR}/summary.txt"
  exit "${status}"
}

canonical_secure_file() {
  local path="$1"
  local permission_policy="$2"
  local resolved owner_uid mode permissions
  case "${path}" in
    /*) ;;
    *) return 1 ;;
  esac
  [ -f "${path}" ] && [ ! -L "${path}" ] || return 1
  resolved="$(realpath "${path}" 2>/dev/null)" || return 1
  [ "${resolved}" = "${path}" ] || return 1
  case "${resolved}" in
    "${ROOT_REAL}"|"${ROOT_REAL}"/*) return 1 ;;
  esac
  owner_uid="$(portable_owner_uid "${path}")" || return 1
  if [ "${owner_uid}" != "$(id -u)" ] && [ "${owner_uid}" != "0" ]; then
    return 1
  fi
  mode="$(portable_mode "${path}")" || return 1
  [[ "${mode}" =~ ^[0-7]{3,4}$ ]] || return 1
  permissions=$((8#${mode}))
  case "${permission_policy}" in
    private)
      (( (permissions & 0177) == 0 )) || return 1
      ;;
    nonwritable)
      (( (permissions & 0022) == 0 )) || return 1
      ;;
    identity)
      ;;
    *) return 1 ;;
  esac
}

portable_mode() {
  local path="$1"
  if stat -f '%Lp' "${path}" >/dev/null 2>&1; then
    stat -f '%Lp' "${path}"
  else
    stat -c '%a' "${path}"
  fi
}

portable_owner_uid() {
  local path="$1"
  if stat -f '%u' "${path}" >/dev/null 2>&1; then
    stat -f '%u' "${path}"
  else
    stat -c '%u' "${path}"
  fi
}

strict_non_placeholder_value() {
  local value="$1"
  [ -n "${value}" ] || return 1
  [ "${value}" = "${value#${value%%[![:space:]]*}}" ] || return 1
  [ "${value}" = "${value%${value##*[![:space:]]}}" ] || return 1
  case "${value}" in
    *'<'*|*'>'*) return 1 ;;
  esac
  if printf '%s' "${value}" | LC_ALL=C grep -q '[[:cntrl:]]'; then
    return 1
  fi
  if printf '%s' "${value}" \
      | LC_ALL=C grep -Eiq '(^|[^[:alnum:]])(TBD|UNKNOWN|PLACEHOLDER|FIXTURE)([^[:alnum:]]|$)'; then
    return 1
  fi
}

sha256_text() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 | awk '{print $1}'
  else
    sha256sum | awk '{print $1}'
  fi
}

attestation_value() {
  local file="$1"
  local key="$2"
  local count
  count="$(grep -c "^${key}=" "${file}" || true)"
  [ "${count}" = "1" ] || return 1
  sed -n "s/^${key}=//p" "${file}"
}

attestation_has_only_keys() {
  local file="$1"
  shift
  local allowed="|$(printf '%s|' "$@")"
  local line key
  while IFS= read -r line || [ -n "${line}" ]; do
    [ -n "${line}" ] || return 1
    case "${line}" in
      *=*) key="${line%%=*}" ;;
      *) return 1 ;;
    esac
    case "${allowed}" in
      *"|${key}|"*) ;;
      *) return 1 ;;
    esac
  done <"${file}"
}

attestation_is_strict() {
  local file="$1"
  shift
  local key value
  attestation_has_only_keys "${file}" "$@" || return 1
  for key in "$@"; do
    [ "$(grep -c "^${key}=" "${file}" || true)" = "1" ] || return 1
    value="$(attestation_value "${file}" "${key}")" || return 1
    strict_non_placeholder_value "${value}" || return 1
  done
}

require_attestation_value() {
  local file="$1"
  local key="$2"
  local expected="$3"
  local actual
  actual="$(attestation_value "${file}" "${key}")" || return 1
  [ "${actual}" = "${expected}" ]
}

missing_count=0
for input_name in "${required_inputs[@]}"; do
  if [ -z "${!input_name:-}" ]; then
    missing_count=$((missing_count + 1))
  fi
done

if [ "${P3H_TLS_MODE:-}" = "INTERNAL_CA" ] && [ -z "${P3H_CA_BUNDLE_FILE:-}" ]; then
  missing_count=$((missing_count + 1))
fi

if [ "${missing_count}" -ne 0 ]; then
  echo "P3H_INPUT_STATUS: MISSING_REQUIRED_INPUTS"
  echo "P3H_MISSING_INPUT_COUNT: ${missing_count}"
  blocked "BLOCKED_MISSING_CONTROLLED_STAGING_INPUT" "NOT_ATTEMPTED" "NOT_ATTEMPTED" 0
fi

CURRENT_STAGE="local-contract-validation"
if [ "${P3H_CONFIRM}" != "${EXPECTED_CONFIRMATION}" ]; then
  blocked "BLOCKED_INVALID_CONFIRMATION"
fi
if [ "${P3H_REBOOT_CONFIRM}" != "${EXPECTED_REBOOT_CONFIRMATION}" ]; then
  blocked "BLOCKED_SERVER_REBOOT_EVIDENCE_NOT_RUN"
fi

case "${P3H_TLS_MODE}" in
  PUBLIC_CA|INTERNAL_CA) ;;
  *) blocked "BLOCKED_UNSUPPORTED_TLS_MODE" ;;
esac

case "${P3H_SECRET_BACKEND}" in
  SYSTEMD_CREDENTIALS) ;;
  SOPS_AGE_TMPFS|VAULT_AGENT|CLOUD_SECRET_MANAGER_AGENT)
    blocked "BLOCKED_BACKEND_NOT_IMPLEMENTED"
    ;;
  *) blocked "BLOCKED_UNSUPPORTED_SECRET_BACKEND" ;;
esac

for owner_reference in "${P3H_RELEASE_OWNER_REFERENCE}" \
    "${P3H_ROLLBACK_OWNER_REFERENCE}" "${P3H_INCIDENT_OWNER_REFERENCE}"; do
  if ! strict_non_placeholder_value "${owner_reference}"; then
    blocked "BLOCKED_INVALID_OWNER_REFERENCE"
  fi
done
case "${P3H_KEEP_STAGING_RUNNING}" in
  YES|NO) ;;
  *) blocked "BLOCKED_INVALID_KEEP_RUNNING_POLICY" ;;
esac

case "${P3H_SECRET_MOUNT_DIR}" in
  /run/*) ;;
  *) blocked "BLOCKED_PLAINTEXT_SECRET_DIRECTORY" ;;
esac
case "${P3H_SECRET_MOUNT_DIR}" in
  *'/../'*|*/..|*'/./'*) blocked "BLOCKED_PLAINTEXT_SECRET_DIRECTORY" ;;
esac

staging_hostname_lower="$(printf '%s' "${P3H_STAGING_HOSTNAME}" | tr '[:upper:]' '[:lower:]')"
ssh_host_lower="$(printf '%s' "${P3H_SSH_HOST}" | tr '[:upper:]' '[:lower:]')"
case "${staging_hostname_lower}" in
  *prod*|*production*|*primary-live*) blocked "BLOCKED_PRODUCTION_INDICATOR" ;;
esac
case "${ssh_host_lower}" in
  *prod*|*production*|*primary-live*) blocked "BLOCKED_PRODUCTION_INDICATOR" ;;
esac

if ! [[ "${P3H_SSH_PORT}" =~ ^[0-9]+$ ]] \
    || [ "${P3H_SSH_PORT}" -lt 1 ] || [ "${P3H_SSH_PORT}" -gt 65535 ]; then
  blocked "BLOCKED_INVALID_SSH_PORT"
fi
if [ "${P3H_SSH_USER}" = "root" ]; then
  blocked "BLOCKED_ROOT_DEPLOYMENT_USER"
fi
case "${P3H_SSH_HOST_KEY_SHA256}" in
  SHA256:*) ;;
  *) blocked "BLOCKED_INVALID_SSH_HOST_KEY_PIN" ;;
esac

if ! canonical_secure_file "${P3H_SERVER_ATTESTATION_FILE}" private \
    || ! canonical_secure_file "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" private; then
  blocked "BLOCKED_INVALID_STAGING_ATTESTATION"
fi
if ! canonical_secure_file "${P3H_SSH_IDENTITY_FILE}" identity; then
  blocked "BLOCKED_INVALID_SSH_IDENTITY"
fi

identity_mode="$(portable_mode "${P3H_SSH_IDENTITY_FILE}")"
if ! [[ "${identity_mode}" =~ ^[0-7]{3,4}$ ]]; then
  blocked "BLOCKED_INVALID_SSH_IDENTITY"
fi
identity_permissions=$((8#${identity_mode}))
if (( (identity_permissions & 0177) != 0 )); then
  blocked "BLOCKED_SSH_IDENTITY_PERMISSIONS"
fi

if [ "${P3H_TLS_MODE}" = "INTERNAL_CA" ] \
    && ! canonical_secure_file "${P3H_CA_BUNDLE_FILE}" nonwritable; then
  blocked "BLOCKED_INVALID_CA_BUNDLE"
fi

if ! attestation_is_strict "${P3H_SERVER_ATTESTATION_FILE}" "${server_attestation_keys[@]}" \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" ENVIRONMENT_CLASS CONTROLLED_STAGING \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" PRODUCTION_TRAFFIC NO \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" PRODUCTION_DATABASE NO \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" PRODUCTION_SECRETS NO \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" AUTHORIZED_FOR_P3H YES \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" DISPOSABLE_OR_REBUILDABLE YES \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" LINUX_SERVER YES \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" EXPECTED_SSH_HOST_KEY_SHA256 "${P3H_SSH_HOST_KEY_SHA256}" \
    || ! require_attestation_value "${P3H_SERVER_ATTESTATION_FILE}" EXPECTED_STAGING_HOSTNAME "${P3H_STAGING_HOSTNAME}"; then
  blocked "BLOCKED_INVALID_STAGING_ATTESTATION"
fi

if ! attestation_is_strict "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" "${secret_attestation_keys[@]}" \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" SECRET_BACKEND_CLASS "${P3H_SECRET_BACKEND}" \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" AUTHORIZED_FOR_P3H YES \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" PLAINTEXT_AT_REST NO \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" SECRETS_VERSIONED_OR_ROTATABLE YES \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" SECRET_MOUNT_IS_RUNTIME_ONLY YES \
    || ! require_attestation_value "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" ROTATION_ALLOWED YES; then
  blocked "BLOCKED_INVALID_SECRET_BACKEND_ATTESTATION"
fi

{
  printf 'RELEASE_OWNER_REFERENCE_SHA256: '
  printf '%s' "${P3H_RELEASE_OWNER_REFERENCE}" | sha256_text
  printf 'ROLLBACK_OWNER_REFERENCE_SHA256: '
  printf '%s' "${P3H_ROLLBACK_OWNER_REFERENCE}" | sha256_text
  printf 'INCIDENT_OWNER_REFERENCE_SHA256: '
  printf '%s' "${P3H_INCIDENT_OWNER_REFERENCE}" | sha256_text
} >"${EVIDENCE_DIR}/owner-reference-hashes.txt"

CURRENT_STAGE="ssh-host-key-verification"
for command_name in ssh ssh-keyscan ssh-keygen git sha256sum tar realpath; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    if [ "${command_name}" = "sha256sum" ] && command -v shasum >/dev/null 2>&1; then
      continue
    fi
    blocked "BLOCKED_REQUIRED_LOCAL_TOOL_MISSING"
  fi
done

TMP_DIR="$(mktemp -d)"
CURRENT_STAGE="exact-source-archive"
current_head="$(git -C "${ROOT_DIR}" rev-parse HEAD)"
current_branch="$(git -C "${ROOT_DIR}" branch --show-current)"
if [ "${current_branch}" != "codex/staging-readonly-tls-secrets-p3h" ] \
    || [ -n "$(git -C "${ROOT_DIR}" status --porcelain)" ]; then
  blocked "BLOCKED_SOURCE_WORKTREE"
fi
archive_file="${TMP_DIR}/trade-model-p3h-${current_head}.tar"
archive_context="${TMP_DIR}/source"
mkdir -p "${archive_context}"
if ! run_bounded 60 git -C "${ROOT_DIR}" archive --format=tar \
    --output="${archive_file}" "${current_head}" \
    || ! run_bounded 60 tar -xf "${archive_file}" -C "${archive_context}" \
    || ! run_bounded 30 bash "${ROOT_DIR}/scripts/check-docker-context-safety.sh" \
      "${archive_context}" >/dev/null; then
  blocked "BLOCKED_EXACT_SOURCE_ARCHIVE"
fi
if command -v sha256sum >/dev/null 2>&1; then
  archive_sha256="$(sha256sum "${archive_file}" | awk '{print $1}')"
else
  archive_sha256="$(shasum -a 256 "${archive_file}" | awk '{print $1}')"
fi
{
  echo "APPLICATION_IMAGE_SOURCE_HEAD: ${current_head}"
  echo "SOURCE_ARCHIVE_SHA256: ${archive_sha256}"
  echo "DOCKER_CONTEXT_SAFETY: PASS_EXACT_ARCHIVE_CONTEXT"
  echo "APPLICATION_BUILD_DOCKERFILE: deploy/p3h/Dockerfile.p3h"
} >"${EVIDENCE_DIR}/image-metadata.txt"

CURRENT_STAGE="ssh-host-key-verification"
known_hosts="${TMP_DIR}/known_hosts"
NETWORK_ACCESS_STARTED=1
if ! run_bounded 20 ssh-keyscan -T 10 -p "${P3H_SSH_PORT}" \
    "${P3H_SSH_HOST}" >"${known_hosts}" 2>/dev/null; then
  blocked "BLOCKED_SSH_HOST_KEY_UNAVAILABLE" "ATTEMPTED" "NOT_ATTEMPTED"
fi
actual_host_key="$(ssh-keygen -lf "${known_hosts}" -E sha256 | awk 'NR == 1 {print $2}')"
if [ -z "${actual_host_key}" ] || [ "${actual_host_key}" != "${P3H_SSH_HOST_KEY_SHA256}" ]; then
  blocked "BLOCKED_SSH_HOST_KEY_MISMATCH" "ATTEMPTED" "NOT_ATTEMPTED"
fi

ssh_options=(
  -o BatchMode=yes
  -o StrictHostKeyChecking=yes
  -o "UserKnownHostsFile=${known_hosts}"
  -o IdentitiesOnly=yes
  -o IdentityAgent=none
  -o ForwardAgent=no
  -o ForwardX11=no
  -o ClearAllForwardings=yes
  -o PasswordAuthentication=no
  -o KbdInteractiveAuthentication=no
  -o ConnectTimeout=10
  -i "${P3H_SSH_IDENTITY_FILE}"
  -p "${P3H_SSH_PORT}"
)

CURRENT_STAGE="remote-preflight"
remote_preflight_raw="${TMP_DIR}/remote-preflight.raw"
if ! run_bounded 120 ssh "${ssh_options[@]}" \
    "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
    bash -s -- "${P3H_STAGING_HOSTNAME}" "${P3H_SECRET_BACKEND}" \
    "${P3H_SECRET_MOUNT_DIR}" "${P3H_TLS_MODE}" \
    <"${ROOT_DIR}/scripts/p3h-remote-preflight.sh" \
    >"${remote_preflight_raw}" 2>/dev/null; then
  blocked "BLOCKED_REMOTE_PREFLIGHT" "ATTEMPTED" "NOT_ATTEMPTED"
fi

"${ROOT_DIR}/scripts/p3h-server-evidence-redact.sh" \
  "${remote_preflight_raw}" "${EVIDENCE_DIR}/host-baseline.txt"

# The current package deliberately stops after authenticated, pinned-host
# preflight until the complete server and secret inputs are supplied and the
# generated deployment plan receives an operator review. It must never turn a
# preflight-only run into staging PASS evidence.
CURRENT_STAGE="staging-deployment"
blocked "BLOCKED_STAGING_EXECUTION_NOT_COMPLETED" "ATTEMPTED" "ATTESTED_NOT_READ"
