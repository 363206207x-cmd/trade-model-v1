#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${ROOT_DIR}/.runtime/trade-model-v1-local-real.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "LOCAL_REAL_DATA_STOP: ALREADY_STOPPED"
  exit 0
fi

pid="$(tr -dc '0-9' <"${PID_FILE}")"
if [[ -z "${pid}" ]] || ! kill -0 "${pid}" 2>/dev/null; then
  rm -f "${PID_FILE}"
  echo "LOCAL_REAL_DATA_STOP: STALE_PID_CLEANED"
  exit 0
fi

project_process=false
if command -v lsof >/dev/null 2>&1; then
  cwd="$(lsof -a -p "${pid}" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -n 1)"
  [[ "${cwd}" == "${ROOT_DIR}" ]] && project_process=true
fi
if [[ "${project_process}" != true ]]; then
  command_line="$(ps -p "${pid}" -o command= 2>/dev/null || true)"
  [[ "${command_line}" == *"trade-model-v1"* ]] && project_process=true
fi
if [[ "${project_process}" != true ]]; then
  rm -f "${PID_FILE}"
  echo "LOCAL_REAL_DATA_STOP: PID_NOT_PROJECT_PROCESS"
  exit 0
fi

kill "${pid}"
for _ in {1..20}; do
  if ! kill -0 "${pid}" 2>/dev/null; then
    rm -f "${PID_FILE}"
    echo "LOCAL_REAL_DATA_STOP: PASS"
    exit 0
  fi
  sleep 1
done

echo "LOCAL_REAL_DATA_STOP: FAIL_GRACEFUL_TIMEOUT"
exit 1
