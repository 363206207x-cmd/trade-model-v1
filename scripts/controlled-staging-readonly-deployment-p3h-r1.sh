#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VM_NAME=trade-model-p3h-staging-lab
LAB_ROOT="${HOME}/.local/share/trade-model-p3h-lab1"
INPUT_FILE="${LAB_ROOT}/p3h-lab-inputs.sh"
METADATA_FILE="${LAB_ROOT}/lab-metadata"
LAB_MARKER="${LAB_ROOT}/lab-owned-by-p3h-lab1"
EVIDENCE_ROOT="${HOME}/.local/share/trade-model-p3h-lab1-evidence"
EXPECTED_BRANCH=codex/p3h-local-vm-staging-lab1
EXPECTED_CONFIRMATION=I_CONFIRM_AUTHORIZED_NON_PRODUCTION_STAGING_DEPLOYMENT
EXPECTED_REBOOT_CONFIRMATION=I_CONFIRM_CONTROLLED_STAGING_SERVER_REBOOT
CURRENT_STAGE=target-class
TEMP_ROOT=""
RAW_EVIDENCE=""
SOURCE_HEAD=""
REMOTE_PREPARED=0
CLEANUP_COMPLETE=0

blocked() {
  echo "P3H_LAB_FAILED_STAGE: ${CURRENT_STAGE}"
  echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: $1"
  echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
  echo "P3H_RESULT: BLOCKED_LOCAL_VM_EVIDENCE"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 2
}

wait_for_bounded_pid() {
  local command_pid="$1"
  local timeout_seconds="$2"
  local elapsed=0
  while kill -0 "${command_pid}" >/dev/null 2>&1 \
      && [ "${elapsed}" -lt "${timeout_seconds}" ]; do
    sleep 1
    elapsed=$((elapsed + 1))
  done
  if kill -0 "${command_pid}" >/dev/null 2>&1; then
    kill "${command_pid}" >/dev/null 2>&1 || true
    wait "${command_pid}" >/dev/null 2>&1 || true
    return 124
  fi
  wait "${command_pid}"
}

run_bounded() {
  local timeout_seconds="$1"
  shift
  "$@" &
  wait_for_bounded_pid "$!" "${timeout_seconds}"
}

run_bounded_with_stdin() {
  local timeout_seconds="$1"
  local input_file="$2"
  shift 2
  [ -f "${input_file}" ] && [ ! -L "${input_file}" ] || return 2
  "$@" <"${input_file}" &
  wait_for_bounded_pid "$!" "${timeout_seconds}"
}

persist_remote_failure_evidence() {
  local failed_stage="$1"
  local stage_output="$2"
  local reason evidence_dir failure_file failure_sha
  reason="$(awk -F ': ' '
    $1 == "P3H_REMOTE_STAGE" && $2 ~ /^BLOCKED_[A-Z0-9_]+$/ {
      print $2
      exit
    }
  ' "${stage_output}")"
  [ -n "${reason}" ] || reason=BLOCKED_REMOTE_STAGE_NO_SAFE_DIAGNOSTIC
  evidence_dir="${EVIDENCE_ROOT}/${SOURCE_HEAD:0:12}"
  failure_file="${evidence_dir}/p3h-lab1-failure-summary.txt"
  mkdir -p "${evidence_dir}"
  chmod 700 "${EVIDENCE_ROOT}" "${evidence_dir}"
  {
    printf '%s\n' "P3H_LAB_FAILED_STAGE: ${failed_stage}"
    printf '%s\n' "P3H_REMOTE_FAILURE_REASON: ${reason}"
    printf '%s\n' 'REAL_EXTERNAL_STAGING_STATUS: NOT_RUN'
    printf '%s\n' 'P3H_RESULT: BLOCKED_LOCAL_VM_EVIDENCE'
    printf '%s\n' 'P4_ALLOWED: NO'
    printf '%s\n' 'PRODUCTION_READINESS: BLOCKED'
  } >"${failure_file}"
  chmod 600 "${failure_file}"
  failure_sha="$(shasum -a 256 "${failure_file}" | awk '{print $1}')"
  printf '%s\n' "${failure_sha}" >"${evidence_dir}/p3h-lab1-failure-summary.sha256"
  chmod 600 "${evidence_dir}/p3h-lab1-failure-summary.sha256"
  echo "P3H_REMOTE_FAILURE_REASON: ${reason}" >&2
  echo "SANITIZED_FAILURE_EVIDENCE_SHA256: ${failure_sha}" >&2
}

run_remote_stage() {
  local timeout_seconds="$1"
  local action="$2"
  local blocked_status="$3"
  local stage_output="${TEMP_ROOT}/remote-${action}.raw"
  local stage_status
  if run_bounded "${timeout_seconds}" remote "${action}" "${SOURCE_HEAD}" \
      >"${stage_output}" 2>&1; then
    cat "${stage_output}" >>"${RAW_EVIDENCE}"
    return 0
  else
    stage_status=$?
  fi
  if [ "${stage_status}" -eq 124 ]; then
    printf '%s\n' 'P3H_REMOTE_STAGE: BLOCKED_REMOTE_STAGE_TIMEOUT' \
      >>"${stage_output}"
  fi
  persist_remote_failure_evidence "${CURRENT_STAGE}" "${stage_output}"
  blocked "${blocked_status}"
}

cleanup() {
  local exit_status=$?
  set +e
  if [ "${CLEANUP_COMPLETE}" -ne 1 ] && [ "${P3H_TARGET_CLASS:-}" = LOCAL_LIMA_LAB ]; then
    if [ "${REMOTE_PREPARED}" -eq 1 ] && [ -n "${SOURCE_HEAD}" ]; then
      remote CLEANUP "${SOURCE_HEAD}" >/dev/null 2>&1 || true
    fi
    P3H_LAB_DESTROY_CONFIRM=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1 \
      bash "${ROOT_DIR}/scripts/p3h-lab-destroy.sh" >/dev/null 2>&1 || true
  fi
  if [ -n "${TEMP_ROOT}" ] && [ -d "${TEMP_ROOT}" ]; then
    rm -rf "${TEMP_ROOT}"
  fi
  exit "${exit_status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

case "${P3H_TARGET_CLASS:-}" in
  AUTHORIZED_EXTERNAL_STAGING)
    if [ -n "${P3H_SERVER_ATTESTATION_FILE:-}" ] \
        && [ -f "${P3H_SERVER_ATTESTATION_FILE}" ] \
        && grep -q 'P3H-LAB1-' "${P3H_SERVER_ATTESTATION_FILE}"; then
      blocked BLOCKED_LAB_ATTESTATION_FOR_EXTERNAL_TARGET
    fi
    exec bash "${ROOT_DIR}/scripts/controlled-staging-readonly-deployment-p3h.sh"
    ;;
  LOCAL_LIMA_LAB) ;;
  '')
    echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: SKIPPED_TARGET_CLASS_REQUIRED"
    echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
    echo "P4_ALLOWED: NO"
    echo "PRODUCTION_READINESS: BLOCKED"
    exit 0
    ;;
  *) blocked BLOCKED_UNSUPPORTED_TARGET_CLASS ;;
esac

CURRENT_STAGE=local-lab-input
[ -f "${LAB_MARKER}" ] && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}" \
  || blocked BLOCKED_LAB_OWNERSHIP
[ -f "${INPUT_FILE}" ] && [ ! -L "${INPUT_FILE}" ] \
  || blocked BLOCKED_LAB_INPUT_FILE
[ -f "${METADATA_FILE}" ] && [ ! -L "${METADATA_FILE}" ] \
  || blocked BLOCKED_LAB_METADATA
if [ "$(stat -f '%Lp' "${INPUT_FILE}" 2>/dev/null || stat -c '%a' "${INPUT_FILE}")" != 600 ]; then
  blocked BLOCKED_LAB_INPUT_PERMISSIONS
fi

# The generated file contains only non-secret paths, pins, and approval labels.
# Random credential values remain encrypted in the VM credential store.
source "${INPUT_FILE}"

required_inputs=(
  P3H_CONFIRM P3H_SERVER_ATTESTATION_FILE P3H_SECRET_BACKEND_ATTESTATION_FILE
  P3H_SSH_HOST P3H_SSH_PORT P3H_SSH_USER P3H_SSH_IDENTITY_FILE
  P3H_SSH_HOST_KEY_SHA256 P3H_STAGING_HOSTNAME P3H_TLS_MODE
  P3H_SECRET_BACKEND P3H_SECRET_MOUNT_DIR P3H_RELEASE_OWNER_REFERENCE
  P3H_ROLLBACK_OWNER_REFERENCE P3H_INCIDENT_OWNER_REFERENCE
  P3H_REBOOT_CONFIRM P3H_KEEP_STAGING_RUNNING
)
ready_count=0
for input_name in "${required_inputs[@]}"; do
  [ -n "${!input_name:-}" ] || blocked BLOCKED_MISSING_LOCAL_LAB_INPUT
  ready_count=$((ready_count + 1))
done
[ "${ready_count}" -eq 17 ] || blocked BLOCKED_LOCAL_LAB_INPUT_COUNT

[ "${P3H_CONFIRM}" = "${EXPECTED_CONFIRMATION}" ] \
  || blocked BLOCKED_CONFIRMATION
[ "${P3H_REBOOT_CONFIRM}" = "${EXPECTED_REBOOT_CONFIRMATION}" ] \
  || blocked BLOCKED_REBOOT_CONFIRMATION
[ "${P3H_SSH_HOST}" = 127.0.0.1 ] \
  || blocked BLOCKED_LOCAL_LAB_SSH_HOST
[ "${P3H_SSH_USER}" = p3h-deploy ] \
  || blocked BLOCKED_LOCAL_LAB_SSH_USER
[ "${P3H_STAGING_HOSTNAME}" = trade-staging.lab.test ] \
  || blocked BLOCKED_LOCAL_LAB_HOSTNAME
[ "${P3H_TLS_MODE}" = INTERNAL_CA ] \
  || blocked BLOCKED_LOCAL_LAB_TLS_MODE
[ "${P3H_SECRET_BACKEND}" = SYSTEMD_CREDENTIALS ] \
  || blocked BLOCKED_LOCAL_LAB_SECRET_BACKEND
[ "${P3H_SECRET_MOUNT_DIR}" = /run/credentials/p3hlab1 ] \
  || blocked BLOCKED_LOCAL_LAB_SECRET_MOUNT
[ "${P3H_RELEASE_OWNER_REFERENCE}" = P3H-LAB1-RELEASE ] \
  || blocked BLOCKED_LOCAL_LAB_OWNER_REFERENCE
[ "${P3H_ROLLBACK_OWNER_REFERENCE}" = P3H-LAB1-ROLLBACK ] \
  || blocked BLOCKED_LOCAL_LAB_OWNER_REFERENCE
[ "${P3H_INCIDENT_OWNER_REFERENCE}" = P3H-LAB1-INCIDENT ] \
  || blocked BLOCKED_LOCAL_LAB_OWNER_REFERENCE
[ "${P3H_KEEP_STAGING_RUNNING}" = NO ] \
  || blocked BLOCKED_LOCAL_LAB_CLEANUP_POLICY

for secure_file in "${P3H_SERVER_ATTESTATION_FILE}" \
    "${P3H_SECRET_BACKEND_ATTESTATION_FILE}" "${P3H_SSH_IDENTITY_FILE}"; do
  [ -f "${secure_file}" ] && [ ! -L "${secure_file}" ] \
    || blocked BLOCKED_LOCAL_LAB_SECURE_FILE
  case "$(realpath "${secure_file}")" in
    "${LAB_ROOT}"/*) ;;
    *) blocked BLOCKED_LOCAL_LAB_SECURE_FILE_SCOPE ;;
  esac
done
[ -f "${P3H_CA_BUNDLE_FILE:-}" ] && [ ! -L "${P3H_CA_BUNDLE_FILE}" ] \
  || blocked BLOCKED_LOCAL_LAB_CA

grep -Fxq 'ENVIRONMENT_CLASS=CONTROLLED_STAGING' "${P3H_SERVER_ATTESTATION_FILE}"
grep -Fxq 'PRODUCTION_TRAFFIC=NO' "${P3H_SERVER_ATTESTATION_FILE}"
grep -Fxq 'PRODUCTION_DATABASE=NO' "${P3H_SERVER_ATTESTATION_FILE}"
grep -Fxq 'PRODUCTION_SECRETS=NO' "${P3H_SERVER_ATTESTATION_FILE}"
grep -Fxq 'APPROVAL_REFERENCE=P3H-LAB1-USER-AUTH-20260717' \
  "${P3H_SERVER_ATTESTATION_FILE}"
grep -Fxq 'SECRET_BACKEND_CLASS=SYSTEMD_CREDENTIALS' \
  "${P3H_SECRET_BACKEND_ATTESTATION_FILE}"
grep -Fxq 'PLAINTEXT_AT_REST=NO' "${P3H_SECRET_BACKEND_ATTESTATION_FILE}"
grep -Fxq 'SECRET_MOUNT_IS_RUNTIME_ONLY=YES' \
  "${P3H_SECRET_BACKEND_ATTESTATION_FILE}"

command -v limactl >/dev/null 2>&1 || blocked BLOCKED_LIMA_MISSING
limactl list --quiet | grep -Fxq "${VM_NAME}" || blocked BLOCKED_LAB_VM_MISSING
metadata_vm="$(sed -n 's/^VM_NAME=//p' "${METADATA_FILE}")"
metadata_target="$(sed -n 's/^TARGET_CLASS=//p' "${METADATA_FILE}")"
[ "${metadata_vm}" = "${VM_NAME}" ] && [ "${metadata_target}" = LOCAL_LIMA_LAB ] \
  || blocked BLOCKED_LAB_METADATA

CURRENT_STAGE=exact-source
SOURCE_HEAD="$(git -C "${ROOT_DIR}" rev-parse HEAD)"
current_branch="$(git -C "${ROOT_DIR}" branch --show-current)"
[ "${current_branch}" = "${EXPECTED_BRANCH}" ] || blocked BLOCKED_SOURCE_BRANCH
[ -z "$(git -C "${ROOT_DIR}" status --porcelain)" ] || blocked BLOCKED_DIRTY_WORKTREE
TEMP_ROOT="$(mktemp -d /private/tmp/trade-model-p3h-lab1-r1.XXXXXX)"
chmod 700 "${TEMP_ROOT}"
RAW_EVIDENCE="${TEMP_ROOT}/raw-evidence.txt"
: >"${RAW_EVIDENCE}"

known_hosts="${TEMP_ROOT}/known_hosts"
ssh_port="$(limactl list "${VM_NAME}" --format '{{.SSHLocalPort}}')"
[ "${ssh_port}" = "${P3H_SSH_PORT}" ] || blocked BLOCKED_LIMA_SSH_PORT_DRIFT
console_fingerprint="$(limactl shell "${VM_NAME}" sudo ssh-keygen \
  -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256 | awk 'NF >= 2 {print $2; exit}')"
[ "${console_fingerprint}" = "${P3H_SSH_HOST_KEY_SHA256}" ] \
  || blocked BLOCKED_CONSOLE_HOST_KEY_MISMATCH
ssh-keyscan -T 10 -p "${ssh_port}" 127.0.0.1 \
  >"${TEMP_ROOT}/known_hosts.candidates" 2>/dev/null
"${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
  "${TEMP_ROOT}/known_hosts.candidates" "${console_fingerprint}" "${known_hosts}" \
  >/dev/null || blocked BLOCKED_NETWORK_HOST_KEY_MISMATCH
chmod 600 "${known_hosts}"

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
  -p "${ssh_port}"
)
scp_options=(
  -o BatchMode=yes
  -o StrictHostKeyChecking=yes
  -o "UserKnownHostsFile=${known_hosts}"
  -o IdentitiesOnly=yes
  -o IdentityAgent=none
  -o ForwardAgent=no
  -o PasswordAuthentication=no
  -o KbdInteractiveAuthentication=no
  -o ConnectTimeout=10
  -i "${P3H_SSH_IDENTITY_FILE}"
  -P "${ssh_port}"
)

remote() {
  ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
    bash /opt/trade-model-p3h/current/deploy/p3h/lima/p3h-lab-r1-remote.sh "$@"
}

CURRENT_STAGE=remote-preflight
remote_preflight="${TEMP_ROOT}/remote-preflight.raw"
run_bounded_with_stdin 180 "${ROOT_DIR}/scripts/p3h-remote-preflight.sh" \
  ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  bash -s -- "${P3H_STAGING_HOSTNAME}" "${P3H_SECRET_BACKEND}" \
  "${P3H_SECRET_MOUNT_DIR}" "${P3H_TLS_MODE}" \
  >"${remote_preflight}" \
  || blocked BLOCKED_REMOTE_PREFLIGHT
if ! "${ROOT_DIR}/scripts/p3h-server-evidence-redact.sh" \
    "${remote_preflight}" "${TEMP_ROOT}/remote-preflight.sanitized" >/dev/null; then
  blocked BLOCKED_REMOTE_PREFLIGHT_EVIDENCE
fi
cat "${TEMP_ROOT}/remote-preflight.sanitized" >>"${RAW_EVIDENCE}"

CURRENT_STAGE=exact-source-archive
archive_file="${TEMP_ROOT}/trade-model-p3h-${SOURCE_HEAD}.tar"
git -C "${ROOT_DIR}" archive --format=tar --output="${archive_file}" "${SOURCE_HEAD}"
archive_context="${TEMP_ROOT}/archive-context"
mkdir -p "${archive_context}"
tar -xf "${archive_file}" -C "${archive_context}"
bash "${ROOT_DIR}/scripts/check-docker-context-safety.sh" \
  "${archive_context}" >/dev/null \
  || blocked BLOCKED_DOCKER_CONTEXT_SAFETY
archive_sha="$(shasum -a 256 "${archive_file}" | awk '{print $1}')"
remote_archive="/tmp/trade-model-p3h-${SOURCE_HEAD}.tar"
run_bounded 180 scp "${scp_options[@]}" "${archive_file}" \
  "${P3H_SSH_USER}@${P3H_SSH_HOST}:${remote_archive}" >/dev/null
remote_sha="$(ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  sha256sum "${remote_archive}" | awk '{print $1}')"
[ "${archive_sha}" = "${remote_sha}" ] || blocked BLOCKED_REMOTE_ARCHIVE_SHA
printf '%s\n' "SOURCE_ARCHIVE_SHA256: ${archive_sha}" >>"${RAW_EVIDENCE}"
printf '%s\n' "SOURCE_ARCHIVE_REMOTE_SHA256: ${remote_sha}" >>"${RAW_EVIDENCE}"

release_dir="/opt/trade-model-p3h/releases/${SOURCE_HEAD}"
ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  sudo install -d -m 0750 -o p3h-deploy -g p3h-deploy "${release_dir}"
ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  tar -xf "${remote_archive}" -C "${release_dir}"
ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  sudo ln -sfn "${release_dir}" /opt/trade-model-p3h/current
ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  rm -f "${remote_archive}"
REMOTE_PREPARED=1

CURRENT_STAGE=initial-deploy
run_remote_stage 12600 INITIAL_DEPLOY BLOCKED_INITIAL_DEPLOY

CURRENT_STAGE=secret-and-tls-rotation
run_remote_stage 900 ROTATE BLOCKED_ROTATION

CURRENT_STAGE=actual-vm-reboot
boot_id_before="$(ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  cat /proc/sys/kernel/random/boot_id)"
set +e
ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  sudo systemctl reboot >/dev/null 2>&1
set -e

went_down=0
for attempt in $(seq 1 90); do
  if ! ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" true \
      >/dev/null 2>&1; then
    went_down=1
    break
  fi
  sleep 1
done
[ "${went_down}" -eq 1 ] || blocked BLOCKED_VM_DID_NOT_REBOOT

came_up=0
for attempt in $(seq 1 300); do
  current_port="$(limactl list "${VM_NAME}" --format '{{.SSHLocalPort}}' 2>/dev/null || true)"
  if [ "${current_port}" = "${ssh_port}" ] \
      && ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" true \
        >/dev/null 2>&1; then
    came_up=1
    break
  fi
  sleep 1
done
[ "${came_up}" -eq 1 ] || blocked BLOCKED_VM_REBOOT_TIMEOUT

console_after="$(limactl shell "${VM_NAME}" sudo ssh-keygen \
  -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256 | awk 'NF >= 2 {print $2; exit}')"
[ "${console_after}" = "${console_fingerprint}" ] \
  || blocked BLOCKED_REBOOT_CONSOLE_HOST_KEY
ssh-keyscan -T 10 -p "${ssh_port}" 127.0.0.1 \
  >"${TEMP_ROOT}/known_hosts.after-reboot.candidates" 2>/dev/null
"${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
  "${TEMP_ROOT}/known_hosts.after-reboot.candidates" "${console_after}" \
  "${TEMP_ROOT}/known_hosts.after-reboot" >/dev/null \
  || blocked BLOCKED_REBOOT_NETWORK_HOST_KEY
boot_id_after="$(ssh "${ssh_options[@]}" "${P3H_SSH_USER}@${P3H_SSH_HOST}" \
  cat /proc/sys/kernel/random/boot_id)"
[ "${boot_id_after}" != "${boot_id_before}" ] || blocked BLOCKED_BOOT_ID_UNCHANGED

CURRENT_STAGE=post-reboot-verification
run_remote_stage 900 POST_REBOOT_VERIFY BLOCKED_POST_REBOOT_VERIFY

CURRENT_STAGE=evidence-redaction
evidence_dir="${EVIDENCE_ROOT}/${SOURCE_HEAD:0:12}"
mkdir -p "${evidence_dir}"
chmod 700 "${EVIDENCE_ROOT}" "${evidence_dir}"
summary_file="${evidence_dir}/p3h-lab1-summary.txt"
"${ROOT_DIR}/scripts/p3h-lab-evidence-redact.sh" \
  "${RAW_EVIDENCE}" "${summary_file}" >/dev/null
summary_sha="$(shasum -a 256 "${summary_file}" | awk '{print $1}')"
printf '%s\n' "${summary_sha}" >"${evidence_dir}/p3h-lab1-summary.sha256"
chmod 600 "${summary_file}" "${evidence_dir}/p3h-lab1-summary.sha256"

CURRENT_STAGE=resource-cleanup
run_bounded 300 remote CLEANUP "${SOURCE_HEAD}" >/dev/null \
  || blocked BLOCKED_REMOTE_RESOURCE_CLEANUP
P3H_LAB_DESTROY_CONFIRM=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1 \
  bash "${ROOT_DIR}/scripts/p3h-lab-destroy.sh" >/dev/null \
  || blocked BLOCKED_VM_RESOURCE_CLEANUP
CLEANUP_COMPLETE=1

echo "P3H_LAB_RESULT: PASS_LOCAL_DISPOSABLE_LINUX_VM_STAGING"
echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: PASS_LOCAL_VM"
echo "LINUX_VM: PASS"
echo "SYSTEMD_CREDENTIALS: PASS"
echo "SSH_PIN: PASS_CONSOLE_AND_NETWORK_MATCH"
echo "FLYWAY: PASS_V1_TO_V7"
echo "READONLY_ROLE: PASS"
echo "TLS: PASS_INTERNAL_CA"
echo "BACKUP_RESTORE: PASS"
echo "SECRET_ROTATION: PASS"
echo "VM_REBOOT: PASS"
echo "SECRET_LEAK_CANDIDATE_COUNT: 0"
echo "RESOURCE_CLEANUP: PASS"
echo "SANITIZED_EVIDENCE_SHA256: ${summary_sha}"
echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
echo "P3H_RESULT: PARTIAL_LOCAL_VM_EVIDENCE"
echo "P4_ALLOWED: NO"
echo "PRODUCTION_READINESS: BLOCKED"
echo "NEXT_TASK: Reviewer_P3H_Local_Linux_VM_Lab_Evidence_Review"
