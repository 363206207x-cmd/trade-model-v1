#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1
VM_NAME=trade-model-p3h-staging-lab
LAB_ROOT="${HOME}/.local/share/trade-model-p3h-lab1"
LAB_MARKER="${LAB_ROOT}/lab-owned-by-p3h-lab1"
LOCAL_NTP_LABEL=org.example.trademodel.p3h-lab1-ntp

if [ "${P3H_LAB_DESTROY_CONFIRM:-}" != "${EXPECTED_CONFIRMATION}" ]; then
  echo "P3H_LAB_DESTROY: BLOCKED_CONFIRMATION"
  exit 2
fi

if launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1; then
  [ -f "${LAB_MARKER}" ] \
    && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}" \
    || { echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_LOCAL_NTP_PROCESS"; exit 2; }
  local_ntp_contract="$(launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}")"
  case "${local_ntp_contract}" in
    *"${LAB_ROOT}/local-ntp-server.py"*P3H-LAB1-USER-AUTH-20260717*) ;;
    *) echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_LOCAL_NTP_PROCESS"; exit 2 ;;
  esac
  launchctl remove "${LOCAL_NTP_LABEL}"
  for attempt in $(seq 1 10); do
    launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1 || break
    sleep 1
  done
  launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1 \
    && { echo "P3H_LAB_DESTROY: FAIL_LOCAL_NTP_REMAINS"; exit 2; }
fi

if command -v limactl >/dev/null 2>&1 \
    && limactl list --quiet | grep -Fxq "${VM_NAME}"; then
  if [ ! -f "${LAB_MARKER}" ] \
      || ! grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}"; then
    echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_VM"
    exit 2
  fi
  limactl stop "${VM_NAME}" >/dev/null 2>&1 || true
  limactl delete --force "${VM_NAME}"
fi

case "${LAB_ROOT}" in
  "${HOME}/.local/share/trade-model-p3h-lab1") ;;
  *) echo "P3H_LAB_DESTROY: BLOCKED_PATH_SCOPE"; exit 2 ;;
esac
if [ -d "${LAB_ROOT}" ]; then
  [ -f "${LAB_MARKER}" ] \
    && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}" \
    || { echo "P3H_LAB_DESTROY: BLOCKED_UNOWNED_DIRECTORY"; exit 2; }
  rm -rf "${LAB_ROOT}"
fi

if command -v limactl >/dev/null 2>&1 \
    && limactl list --quiet | grep -Fxq "${VM_NAME}"; then
  echo "P3H_LAB_DESTROY: FAIL_VM_REMAINS"
  exit 2
fi
[ ! -e "${LAB_ROOT}" ] || { echo "P3H_LAB_DESTROY: FAIL_FILES_REMAIN"; exit 2; }
launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1 \
  && { echo "P3H_LAB_DESTROY: FAIL_LOCAL_NTP_REMAINS"; exit 2; }

echo "P3H_LAB_DESTROY: PASS"
echo "LAB_VM_REMAINS: 0"
echo "LAB_SECRET_ROOT_REMAINS: 0"
echo "LAB_LOCAL_NTP_REMAINS: 0"
echo "RESOURCE_CLEANUP: PASS"
