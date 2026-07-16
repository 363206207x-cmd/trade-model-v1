#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/p3h/docker-compose.p3h.yml"

if [ -z "${P3H_SECRET_MOUNT_DIR:-}" ] && [ -n "${CREDENTIALS_DIRECTORY:-}" ]; then
  export P3H_SECRET_MOUNT_DIR="$(realpath "${CREDENTIALS_DIRECTORY}")"
fi

docker compose -f "${COMPOSE_FILE}" down

project_name="${P3H_COMPOSE_PROJECT_NAME:-trade-model-p3h}"
while IFS= read -r secret_volume; do
  [ -n "${secret_volume}" ] || continue
  docker volume rm "${secret_volume}" >/dev/null
done < <(docker volume ls --quiet \
  --filter "label=com.docker.compose.project=${project_name}" \
  --filter "label=com.docker.compose.volume=p3h_materialized_secrets")

echo "P3H_COMPOSE_STOP: PASS"
