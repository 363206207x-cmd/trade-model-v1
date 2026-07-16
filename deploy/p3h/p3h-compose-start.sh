#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"

required_nonsecret=(P3H_APPLICATION_IMAGE_TAG P3H_STAGING_HOSTNAME P3H_SECRET_MOUNT_DIR)
for input_name in "${required_nonsecret[@]}"; do
  if [ -z "${!input_name:-}" ]; then
    echo "P3H_COMPOSE_START: BLOCKED_MISSING_INPUT"
    exit 2
  fi
done

compose=(docker compose -f "${COMPOSE_FILE}")
if ! "${compose[@]}" config --quiet; then
  echo "P3H_COMPOSE_START: BLOCKED_COMPOSE_CONFIG"
  exit 2
fi

cleanup_failed_start() {
  "${compose[@]}" stop proxy app >/dev/null 2>&1 || true
}
trap cleanup_failed_start ERR

"${compose[@]}" up --detach --wait --wait-timeout 300 proxy
"${compose[@]}" --profile validation run --rm --no-deps app-role-probe

trap - ERR
echo "GREENFIELD_BOOTSTRAP_ORDER: PASS"
echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
echo "P3H_COMPOSE_START: PASS"
