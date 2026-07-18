#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1
VM_NAME=trade-model-p3h-staging-lab
LAB_ROOT="${HOME}/.local/share/trade-model-p3h-lab1"
LAB_MARKER="${LAB_ROOT}/lab-owned-by-p3h-lab1"
LOCAL_NTP_LABEL=org.example.trademodel.p3h-lab1-ntp
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BOUNDED_PROCESS_RUNNER="${ROOT_DIR}/scripts/p3h-bounded-process.py"
CLEANUP_TOTAL_TIMEOUT_SECONDS=300
LIMA_STOP_TIMEOUT_SECONDS=120
LIMA_DELETE_TIMEOUT_SECONDS=120

if [ "${P3H_LAB_DESTROY_CONFIRM:-}" != "${EXPECTED_CONFIRMATION}" ]; then
  echo "P3H_LAB_DESTROY: BLOCKED_CONFIRMATION"
  exit 2
fi

if [ "${P3H_OFFLINE_TIMEOUT_TEST:-}" = I_CONFIRM_OFFLINE_TIMEOUT_STUBS ]; then
  CLEANUP_TOTAL_TIMEOUT_SECONDS=8
  LIMA_STOP_TIMEOUT_SECONDS=1
  LIMA_DELETE_TIMEOUT_SECONDS=1
fi

command -v python3 >/dev/null 2>&1 \
  || { echo "P3H_LAB_DESTROY: BLOCKED_PYTHON_MISSING"; exit 2; }
[ -f "${BOUNDED_PROCESS_RUNNER}" ] \
  || { echo "P3H_LAB_DESTROY: BLOCKED_BOUNDED_RUNNER_MISSING"; exit 2; }

if [ "${P3H_DESTROY_PROCESS_TREE_SUPERVISED:-}" != YES ]; then
  export P3H_DESTROY_PROCESS_TREE_SUPERVISED=YES
  cleanup_supervisor_start="$(date +%s)"
  set +e
  python3 "${BOUNDED_PROCESS_RUNNER}" \
    --timeout-seconds "${CLEANUP_TOTAL_TIMEOUT_SECONDS}" \
    --global-start-epoch "${cleanup_supervisor_start}" \
    --global-timeout-seconds "${CLEANUP_TOTAL_TIMEOUT_SECONDS}" \
    --stage LAB_RESOURCE_CLEANUP \
    --operation-class COMPLETE_CLEANUP_PROCESS_TREE \
    --poll-seconds 1 --heartbeat-seconds 60 --term-grace-seconds 1 \
    -- bash "${BASH_SOURCE[0]}" "$@"
  cleanup_supervisor_status=$?
  set -e
  if [ "${cleanup_supervisor_status}" -eq 124 ] \
      || [ "${cleanup_supervisor_status}" -eq 125 ] \
      || [ "${cleanup_supervisor_status}" -eq 143 ]; then
    echo "P3H_LAB_DESTROY: FAIL_CLEANUP_TIMEOUT"
    echo "LAB_VM_CLEANUP: FAIL"
    echo "RESOURCE_CLEANUP: FAIL"
  fi
  exit "${cleanup_supervisor_status}"
fi

CLEANUP_START_EPOCH="$(date +%s)"
cleanup_failed=0
vm_cleanup=PASS
ntp_cleanup=PASS
file_cleanup=PASS

run_cleanup_bounded() {
  local timeout_seconds="$1"
  local operation_class="$2"
  shift 2
  python3 "${BOUNDED_PROCESS_RUNNER}" \
    --timeout-seconds "${timeout_seconds}" \
    --global-start-epoch "${CLEANUP_START_EPOCH}" \
    --global-timeout-seconds "${CLEANUP_TOTAL_TIMEOUT_SECONDS}" \
    --stage LAB_RESOURCE_CLEANUP \
    --operation-class "${operation_class}" \
    --poll-seconds 1 --heartbeat-seconds 60 --term-grace-seconds 1 \
    -- "$@"
}

refresh_vm_list() {
  VM_LIST=""
  if ! command -v limactl >/dev/null 2>&1; then
    return 0
  fi
  set +e
  VM_LIST="$(run_cleanup_bounded 20 LIMA_LIST limactl list --quiet 2>/dev/null)"
  list_status=$?
  set -e
  if [ "${list_status}" -ne 0 ]; then
    echo "P3H_LAB_DESTROY: FAIL_LIMA_LIST"
    vm_cleanup=FAIL
    cleanup_failed=1
    return 1
  fi
}

vm_is_present() {
  printf '%s\n' "${VM_LIST:-}" | grep -Fxq "${VM_NAME}"
}

if command -v launchctl >/dev/null 2>&1 \
    && launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1; then
  [ -f "${LAB_MARKER}" ] \
    && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}" \
    || { echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_LOCAL_NTP_PROCESS"; exit 2; }
  local_ntp_contract="$(launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}")"
  case "${local_ntp_contract}" in
    *"${LAB_ROOT}/local-ntp-server.py"*P3H-LAB1-USER-AUTH-20260717*) ;;
    *) echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_LOCAL_NTP_PROCESS"; exit 2 ;;
  esac
  if ! launchctl remove "${LOCAL_NTP_LABEL}"; then
    ntp_cleanup=FAIL
    cleanup_failed=1
  fi
  for _attempt in $(seq 1 10); do
    launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1 || break
    sleep 1
  done
  if launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1; then
    echo "P3H_LAB_DESTROY: FAIL_LOCAL_NTP_REMAINS"
    ntp_cleanup=FAIL
    cleanup_failed=1
  fi
fi

refresh_vm_list || true
if vm_is_present; then
  if [ ! -f "${LAB_MARKER}" ] \
      || ! grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}"; then
    echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_VM"
    exit 2
  fi
  if ! run_cleanup_bounded "${LIMA_STOP_TIMEOUT_SECONDS}" LIMA_STOP \
      limactl stop "${VM_NAME}" >/dev/null 2>&1; then
    echo "P3H_LAB_DESTROY: FAIL_LIMA_STOP"
    vm_cleanup=FAIL
    cleanup_failed=1
  fi
  if ! run_cleanup_bounded "${LIMA_DELETE_TIMEOUT_SECONDS}" LIMA_DELETE \
      limactl delete --force "${VM_NAME}" >/dev/null 2>&1; then
    echo "P3H_LAB_DESTROY: FAIL_LIMA_DELETE"
    vm_cleanup=FAIL
    cleanup_failed=1
  fi
fi

refresh_vm_list || true
if vm_is_present; then
  echo "P3H_LAB_DESTROY: FAIL_VM_REMAINS"
  vm_cleanup=FAIL
  cleanup_failed=1
fi

case "${LAB_ROOT}" in
  "${HOME}/.local/share/trade-model-p3h-lab1") ;;
  *) echo "P3H_LAB_DESTROY: BLOCKED_PATH_SCOPE"; exit 2 ;;
esac
if [ -d "${LAB_ROOT}" ] && [ "${vm_cleanup}" = PASS ]; then
  [ -f "${LAB_MARKER}" ] \
    && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}" \
    || { echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_DIRECTORY"; exit 2; }
  rm -rf "${LAB_ROOT}"
fi
if [ -e "${LAB_ROOT}" ]; then
  echo "P3H_LAB_DESTROY: FAIL_FILES_REMAIN"
  file_cleanup=FAIL
  cleanup_failed=1
fi
if command -v launchctl >/dev/null 2>&1 \
    && launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1; then
  ntp_cleanup=FAIL
  cleanup_failed=1
fi

if [ "${cleanup_failed}" -ne 0 ]; then
  echo "P3H_LAB_DESTROY: FAIL"
  echo "LAB_VM_CLEANUP: ${vm_cleanup}"
  echo "LAB_SECRET_CLEANUP: ${file_cleanup}"
  echo "LAB_LOCAL_NTP_CLEANUP: ${ntp_cleanup}"
  echo "RESOURCE_CLEANUP: FAIL"
  exit 2
fi

echo "P3H_LAB_DESTROY: PASS"
echo "LAB_VM_CLEANUP: PASS"
echo "LAB_VM_REMAINS: 0"
echo "LAB_SECRET_ROOT_REMAINS: 0"
echo "LAB_LOCAL_NTP_REMAINS: 0"
echo "RESOURCE_CLEANUP: PASS"
