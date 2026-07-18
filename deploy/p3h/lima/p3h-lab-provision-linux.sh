#!/usr/bin/env bash
set -euo pipefail

expected_hostname=trade-staging.lab.test
deployment_user=p3h-deploy
public_key_file=/tmp/p3h-lab1-ed25519.pub
lab_marker=P3H-LAB1-USER-AUTH-20260717
current_stage=precheck
failure_category=BLOCKED_GUEST_APT_UPDATE
provision_started_epoch="$(date +%s)"
provision_log="$(mktemp /var/tmp/p3h-lab-provision.XXXXXX)"
chmod 0600 "${provision_log}"

report_error() {
  local exit_status=$?
  trap - ERR
  echo "P3H_LAB_PROVISION: ${failure_category}" >&2
  echo "P3H_LAB_PROVISION_STAGE: ${current_stage}" >&2
  echo "P3H_LAB_PROVISION_ELAPSED_SECONDS: $(( $(date +%s) - provision_started_epoch ))" >&2
  echo "P3H_LAB_PROVISION_EXIT_CODE: ${exit_status}" >&2
  rm -f "${provision_log}"
  exit "${exit_status}"
}
trap report_error ERR

run_quiet() {
  failure_category="$1"
  current_stage="$2"
  shift 2
  "$@" >>"${provision_log}" 2>&1
}

blocked() {
  local category="$1"
  local exit_status="${2:-2}"
  trap - ERR
  echo "P3H_LAB_PROVISION: ${category}" >&2
  echo "P3H_LAB_PROVISION_STAGE: ${current_stage}" >&2
  echo "P3H_LAB_PROVISION_ELAPSED_SECONDS: $(( $(date +%s) - provision_started_epoch ))" >&2
  echo "P3H_LAB_PROVISION_EXIT_CODE: ${exit_status}" >&2
  rm -f "${provision_log}"
  exit "${exit_status}"
}

if [ "$(id -u)" -ne 0 ] || [ "$(uname -s)" != "Linux" ]; then
  blocked BLOCKED_NOT_LINUX_ROOT
fi
if [ ! -s "${public_key_file}" ] || [ -L "${public_key_file}" ]; then
  blocked BLOCKED_MISSING_DEDICATED_PUBLIC_KEY
fi

export DEBIAN_FRONTEND=noninteractive
run_quiet BLOCKED_GUEST_DNS GUEST_DNS \
  timeout --signal=TERM --kill-after=2s 15s \
  getent ahosts deb.debian.org
run_quiet BLOCKED_GUEST_APT_UPDATE GUEST_APT_UPDATE apt-get update -q
run_quiet BLOCKED_GUEST_APT_UPDATE GUEST_BASE_PACKAGE_INSTALL \
  apt-get install -y -q --no-install-recommends \
  ca-certificates curl jq openssh-server openssl python3 sudo

run_quiet BLOCKED_GUEST_DNS DOCKER_REPOSITORY_DNS \
  timeout --signal=TERM --kill-after=2s 15s \
  getent ahosts download.docker.com
run_quiet BLOCKED_DOCKER_REPOSITORY DOCKER_REPOSITORY_KEY \
  install -m 0755 -d /etc/apt/keyrings
run_quiet BLOCKED_DOCKER_REPOSITORY DOCKER_REPOSITORY_KEY_DOWNLOAD \
  curl --fail --silent --show-error --location \
  https://download.docker.com/linux/debian/gpg \
  --output /etc/apt/keyrings/docker.asc
run_quiet BLOCKED_DOCKER_REPOSITORY DOCKER_REPOSITORY_KEY_MODE \
  chmod a+r /etc/apt/keyrings/docker.asc
. /etc/os-release
printf '%s\n' \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian ${VERSION_CODENAME} stable" \
  >/etc/apt/sources.list.d/docker.list
run_quiet BLOCKED_DOCKER_REPOSITORY DOCKER_REPOSITORY_UPDATE apt-get update -q
run_quiet BLOCKED_DOCKER_PACKAGE_INSTALL DOCKER_PACKAGE_INSTALL \
  apt-get install -y -q --no-install-recommends \
  docker-ce docker-ce-cli containerd.io docker-buildx-plugin \
  docker-compose-plugin
run_quiet BLOCKED_DOCKER_DAEMON_START DOCKER_DAEMON_START \
  systemctl enable --now docker.service
run_quiet BLOCKED_DOCKER_DAEMON_START DOCKER_DAEMON_VERIFY docker info
run_quiet BLOCKED_DOCKER_COMPOSE_MISSING DOCKER_COMPOSE_VERIFY docker compose version

failure_category=BLOCKED_HOST_IDENTITY
current_stage=HOST_IDENTITY
hostnamectl set-hostname "${expected_hostname}"
python3 - "${expected_hostname}" <<'PY'
from pathlib import Path
import sys

hostname = sys.argv[1]
hosts = Path("/etc/hosts")
lines = [line for line in hosts.read_text(encoding="utf-8").splitlines()
         if "trade-staging.lab.test" not in line]
lines.append(f"127.0.1.1 {hostname} trade-staging")
hosts.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

failure_category=BLOCKED_DEPLOYMENT_USER
current_stage=DEPLOYMENT_USER
if ! id "${deployment_user}" >/dev/null 2>&1; then
  useradd --create-home --shell /bin/bash "${deployment_user}"
fi
usermod --lock "${deployment_user}"
usermod --append --groups docker "${deployment_user}"

install -d -m 0700 -o "${deployment_user}" -g "${deployment_user}" \
  "/home/${deployment_user}/.ssh"
install -m 0600 -o "${deployment_user}" -g "${deployment_user}" \
  "${public_key_file}" "/home/${deployment_user}/.ssh/authorized_keys"

printf '%s ALL=(ALL) NOPASSWD: ALL\n' "${deployment_user}" \
  >"/etc/sudoers.d/90-${deployment_user}-p3h-lab1"
chmod 0440 "/etc/sudoers.d/90-${deployment_user}-p3h-lab1"
visudo --check --file="/etc/sudoers.d/90-${deployment_user}-p3h-lab1" >/dev/null

failure_category=BLOCKED_SSH_POLICY
current_stage=SSH_POLICY
install -d -m 0755 /etc/ssh/sshd_config.d
printf '%s\n' \
  'PasswordAuthentication no' \
  'KbdInteractiveAuthentication no' \
  'PermitRootLogin no' \
  > /etc/ssh/sshd_config.d/90-p3h-lab1.conf
sshd -t
systemctl restart ssh.service

failure_category=BLOCKED_UTC_AND_NTP
current_stage=UTC_AND_NTP
timedatectl set-timezone UTC
systemctl enable --now systemd-timesyncd.service ssh.service
install -d -m 0755 /etc/systemd/timesyncd.conf.d
cat >/etc/systemd/timesyncd.conf.d/90-p3h-lab1.conf <<'EOF'
[Time]
NTP=host.lima.internal
FallbackNTP=
PollIntervalMinSec=4
PollIntervalMaxSec=32
EOF
timedatectl set-ntp true
systemctl restart systemd-timesyncd.service

install -d -m 0750 -o "${deployment_user}" -g "${deployment_user}" \
  /opt/trade-model-p3h /opt/trade-model-p3h/releases
install -d -m 0700 /etc/credstore.encrypted/trade-model-p3h
install -d -m 0700 /var/lib/trade-model-p3h-lab1
printf '%s\n' "${lab_marker}" >/var/lib/trade-model-p3h-lab1/ownership-marker
chmod 0600 /var/lib/trade-model-p3h-lab1/ownership-marker

rm -f "${public_key_file}"

for attempt in $(seq 1 90); do
  if [ "$(timedatectl show -p NTPSynchronized --value 2>/dev/null || true)" = "yes" ]; then
    break
  fi
  sleep 1
done

if [ "$(timedatectl show -p NTPSynchronized --value 2>/dev/null || true)" != "yes" ]; then
  blocked BLOCKED_NTP_NOT_SYNCHRONIZED
fi

failure_category=BLOCKED_FINAL_CONTRACT
current_stage=FINAL_CONTRACT
[ "$(hostname -f)" = "${expected_hostname}" ]
[ "$(timedatectl show -p Timezone --value)" = "UTC" ]
systemctl is-active --quiet docker.service
systemctl is-active --quiet ssh.service
docker info >/dev/null
docker compose version >/dev/null
systemd-creds --help >/dev/null

current_stage=COMPLETE
rm -f "${provision_log}"
echo "P3H_LAB_PROVISION: PASS"
echo "P3H_LAB_PROVISION_STAGE: COMPLETE"
echo "P3H_LAB_PROVISION_ELAPSED_SECONDS: $(( $(date +%s) - provision_started_epoch ))"
echo "P3H_LAB_PROVISION_EXIT_CODE: 0"
