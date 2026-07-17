#!/usr/bin/env bash
set -euo pipefail

expected_hostname=trade-staging.lab.test
deployment_user=p3h-deploy
public_key_file=/tmp/p3h-lab1-ed25519.pub
lab_marker=P3H-LAB1-USER-AUTH-20260717
current_stage=precheck

report_error() {
  local exit_status=$?
  echo "P3H_LAB_PROVISION: BLOCKED_${current_stage}" >&2
  return "${exit_status}"
}
trap report_error ERR

if [ "$(id -u)" -ne 0 ] || [ "$(uname -s)" != "Linux" ]; then
  echo "P3H_LAB_PROVISION: BLOCKED_NOT_LINUX_ROOT"
  exit 2
fi
if [ ! -s "${public_key_file}" ] || [ -L "${public_key_file}" ]; then
  echo "P3H_LAB_PROVISION: BLOCKED_MISSING_DEDICATED_PUBLIC_KEY"
  exit 2
fi

export DEBIAN_FRONTEND=noninteractive
current_stage=PACKAGE_INSTALL
apt-get update -q
apt-get install -y -q --no-install-recommends \
  ca-certificates curl jq openssh-server openssl python3 sudo

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

current_stage=SSH_POLICY
install -d -m 0755 /etc/ssh/sshd_config.d
printf '%s\n' \
  'PasswordAuthentication no' \
  'KbdInteractiveAuthentication no' \
  'PermitRootLogin no' \
  > /etc/ssh/sshd_config.d/90-p3h-lab1.conf
sshd -t
systemctl restart ssh.service

current_stage=UTC_AND_NTP
timedatectl set-timezone UTC
systemctl enable --now systemd-timesyncd.service docker.service ssh.service
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
  echo "P3H_LAB_PROVISION: BLOCKED_NTP_NOT_SYNCHRONIZED" >&2
  exit 2
fi

current_stage=FINAL_CONTRACT
[ "$(hostname -f)" = "${expected_hostname}" ]
[ "$(timedatectl show -p Timezone --value)" = "UTC" ]
systemctl is-active --quiet docker.service
systemctl is-active --quiet ssh.service
docker info >/dev/null
docker compose version >/dev/null
systemd-creds --help >/dev/null

current_stage=COMPLETE
echo "P3H_LAB_PROVISION: PASS"
echo "HOSTNAME: ${expected_hostname}"
echo "TIMEZONE: UTC"
echo "TIME_SYNCHRONIZED: YES"
echo "DEPLOYMENT_USER: DEDICATED_NON_ROOT"
echo "PASSWORD_AUTHENTICATION: DISABLED"
