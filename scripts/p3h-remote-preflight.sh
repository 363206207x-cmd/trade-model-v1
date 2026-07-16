#!/usr/bin/env bash
set -euo pipefail

expected_hostname="${1:-}"
secret_backend="${2:-}"
secret_mount_dir="${3:-}"
tls_mode="${4:-}"

if [ -z "${expected_hostname}" ] || [ -z "${secret_backend}" ] \
    || [ -z "${secret_mount_dir}" ] || [ -z "${tls_mode}" ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_ARGUMENTS"
  exit 2
fi
if [ "$(id -u)" -eq 0 ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_ROOT_USER"
  exit 2
fi
if [ "$(hostname -f 2>/dev/null || hostname)" != "${expected_hostname}" ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_HOSTNAME_MISMATCH"
  exit 2
fi
case "${secret_mount_dir}" in
  /run/*) ;;
  *) echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_MOUNT"; exit 2 ;;
esac
case "${tls_mode}" in
  PUBLIC_CA|INTERNAL_CA) ;;
  *) echo "REMOTE_PREFLIGHT: BLOCKED_TLS_MODE"; exit 2 ;;
esac

for command_name in docker systemctl openssl uname df awk grep ss timedatectl stat sudo; do
  command -v "${command_name}" >/dev/null 2>&1 \
    || { echo "REMOTE_PREFLIGHT: BLOCKED_TOOL_MISSING"; exit 2; }
done
docker info >/dev/null 2>&1 \
  || { echo "REMOTE_PREFLIGHT: BLOCKED_DOCKER_DAEMON"; exit 2; }
docker compose version >/dev/null 2>&1 \
  || { echo "REMOTE_PREFLIGHT: BLOCKED_DOCKER_COMPOSE"; exit 2; }

timezone="$(timedatectl show -p Timezone --value 2>/dev/null || true)"
ntp="$(timedatectl show -p NTPSynchronized --value 2>/dev/null || true)"
if [ "${timezone}" != "UTC" ] || [ "${ntp}" != "yes" ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_TIME_BASIS"
  exit 2
fi

if docker ps -a --format '{{.Names}}' | grep -Ev '^(|trade-model-p3h($|-))' | grep -q .; then
  echo "REMOTE_PREFLIGHT: BLOCKED_UNKNOWN_TRADE_MODEL_WORKLOAD"
  exit 2
fi
if ss -lnt 2>/dev/null | awk '{print $4}' | grep -Eq '(^|:)5432$|(^|:)8081$'; then
  echo "REMOTE_PREFLIGHT: BLOCKED_INTERNAL_PORT_EXPOSED"
  exit 2
fi
if [ -e /opt/trade-model-p3h/current ] || [ -e /var/lib/trade-model-p3h/postgresql ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_EXISTING_TARGET_DATA"
  exit 2
fi
if [ ! -d "${secret_mount_dir}" ] || [ -L "${secret_mount_dir}" ]; then
  echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_MOUNT"
  exit 2
fi

mount_mode="$(stat -c '%a' "${secret_mount_dir}")"
case "${mount_mode}" in
  700|710|720|730|740|750) ;;
  *) echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_MOUNT_PERMISSIONS"; exit 2 ;;
esac

required_secret_files=(
  postgres_admin_password
  flyway_password
  app_database_password_v1
  app_database_password_v2
  app_admin_password_v1
  app_admin_password_v2
  binance_nonfunctional_key
  binance_nonfunctional_secret
  tls_certificate
  tls_private_key
)
if [ "${tls_mode}" = "INTERNAL_CA" ]; then
  required_secret_files+=(tls_ca_certificate)
fi

deployment_user="$(id -un)"
for secret_name in "${required_secret_files[@]}"; do
  secret_path="${secret_mount_dir}/${secret_name}"
  if [ ! -f "${secret_path}" ] || [ -L "${secret_path}" ] || [ ! -s "${secret_path}" ]; then
    echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_FILE"
    exit 2
  fi
  secret_mode="$(stat -c '%a' "${secret_path}")"
  secret_owner="$(stat -c '%U' "${secret_path}")"
  if ! [[ "${secret_mode}" =~ ^[0-7]{3,4}$ ]]; then
    echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_FILE_PERMISSIONS"
    exit 2
  fi
  secret_permissions=$((8#${secret_mode}))
  if (( (secret_permissions & 0337) != 0 )); then
    echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_FILE_PERMISSIONS"
    exit 2
  fi
  if [ "${secret_owner}" != "root" ] && [ "${secret_owner}" != "${deployment_user}" ]; then
    echo "REMOTE_PREFLIGHT: BLOCKED_SECRET_FILE_OWNER"
    exit 2
  fi
done

echo "REMOTE_PREFLIGHT: PASS"
echo "LINUX_DISTRIBUTION: $(. /etc/os-release && printf '%s' "${ID:-unknown}")"
echo "KERNEL_RELEASE: $(uname -r)"
echo "CPU_ARCHITECTURE: $(uname -m)"
echo "SYSTEMD_VERSION: $(systemctl --version | awk 'NR == 1 {print $2}')"
echo "DOCKER_ENGINE_VERSION: $(docker version --format '{{.Server.Version}}')"
echo "DOCKER_COMPOSE_VERSION: $(docker compose version --short)"
echo "OPENSSL_VERSION: $(openssl version | awk '{print $2}')"
echo "TIMEZONE: UTC"
echo "TIME_SYNCHRONIZED: YES"
echo "SECRET_BACKEND_CLASS: ${secret_backend}"
echo "SECRET_MOUNT: RUNTIME_ONLY"
echo "SECRET_FILE_CONTRACT: PASS_NAMES_OWNERS_PERMISSIONS"
echo "SUDO_NONINTERACTIVE: $(sudo -n true >/dev/null 2>&1 && echo AVAILABLE || echo UNAVAILABLE)"
echo "AVAILABLE_DISK_KB: $(df -Pk /opt 2>/dev/null | awk 'NR == 2 {print $4}')"
echo "AVAILABLE_MEMORY_KB: $(awk '/MemAvailable:/ {print $2}' /proc/meminfo)"
