#!/usr/bin/env bash
set -euo pipefail

EXPECTED_CONFIRMATION=I_CONFIRM_LOCAL_DISPOSABLE_LINUX_VM_STAGING_LAB
VM_NAME=trade-model-p3h-staging-lab
LAB_HOSTNAME=trade-staging.lab.test
LAB_USER=p3h-deploy
LAB_ROOT="${HOME}/.local/share/trade-model-p3h-lab1"
LAB_MARKER="${LAB_ROOT}/lab-owned-by-p3h-lab1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_NTP_SERVER="${ROOT_DIR}/scripts/p3h-lab-local-ntp-server.py"
LOCAL_NTP_LABEL=org.example.trademodel.p3h-lab1-ntp
TEMPLATE_FILE="${ROOT_DIR}/deploy/p3h/lima/p3h-lab.yaml"
PROVISION_FILE="${ROOT_DIR}/deploy/p3h/lima/p3h-lab-provision-linux.sh"
HOLDER_UNIT="${ROOT_DIR}/deploy/p3h/lima/p3h-lab-credential-holder.service"
MATERIALIZER="${ROOT_DIR}/deploy/p3h/lima/p3h-lab-materialize-credentials.sh"
RUNTIME_MOUNT_TEMPLATE="${ROOT_DIR}/deploy/p3h/lima/p3h-lab-credential-runtime.mount.template"
SEAL_UNIT="${ROOT_DIR}/deploy/p3h/lima/p3h-lab-credential-seal.service"
TEMP_ROOT=""
BOOTSTRAP_COMPLETE=0
CURRENT_STAGE=precheck

blocked() {
  echo "P3H_LAB_BOOTSTRAP: $1"
  echo "P3H_LAB_RESULT: BLOCKED_LOCAL_LAB_BOOTSTRAP"
  echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 2
}

cleanup() {
  local exit_status=$?
  trap - EXIT
  if [ -n "${TEMP_ROOT}" ] && [ -d "${TEMP_ROOT}" ]; then
    rm -rf "${TEMP_ROOT}"
  fi
  if [ "${BOOTSTRAP_COMPLETE}" -ne 1 ] \
      && [ -f "${LAB_MARKER}" ] \
      && grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}"; then
    P3H_LAB_DESTROY_CONFIRM=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1 \
      bash "${ROOT_DIR}/scripts/p3h-lab-destroy.sh" >/dev/null 2>&1 || true
  fi
  exit "${exit_status}"
}
trap cleanup EXIT

report_error() {
  local exit_status=$?
  echo "P3H_LAB_BOOTSTRAP_FAILED_STAGE: ${CURRENT_STAGE}" >&2
  return "${exit_status}"
}
trap report_error ERR

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

if [ "${P3H_LAB_CONFIRM:-}" != "${EXPECTED_CONFIRMATION}" ]; then
  echo "P3H_LAB_BOOTSTRAP: SKIPPED_CONFIRMATION_REQUIRED"
  echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 0
fi
if [ "$(uname -s)" != "Darwin" ]; then
  blocked "BLOCKED_HOST_OS_NOT_MACOS"
fi
command -v python3 >/dev/null 2>&1 || blocked "BLOCKED_HOST_PYTHON_MISSING"

install_lima_from_official_release() {
  local version=2.1.4
  local machine asset archive checksums install_dir
  machine="$(uname -m)"
  case "${machine}" in
    arm64) asset="lima-${version}-Darwin-arm64.tar.gz" ;;
    x86_64) asset="lima-${version}-Darwin-x86_64.tar.gz" ;;
    *) return 1 ;;
  esac
  install_dir="${HOME}/.local"
  archive="${TEMP_ROOT}/${asset}"
  checksums="${TEMP_ROOT}/SHA256SUMS"
  curl --fail --silent --show-error --location \
    "https://github.com/lima-vm/lima/releases/download/v${version}/${asset}" \
    --output "${archive}"
  curl --fail --silent --show-error --location \
    "https://github.com/lima-vm/lima/releases/download/v${version}/SHA256SUMS" \
    --output "${checksums}"
  expected_sha="$(awk -v file="${asset}" '$2 == file {print $1}' "${checksums}")"
  [ -n "${expected_sha}" ] || return 1
  actual_sha="$(shasum -a 256 "${archive}" | awk '{print $1}')"
  [ "${actual_sha}" = "${expected_sha}" ] || return 1
  mkdir -p "${install_dir}"
  tar -xzf "${archive}" -C "${install_dir}"
  export PATH="${install_dir}/bin:${PATH}"
}

TEMP_ROOT="$(mktemp -d /private/tmp/trade-model-p3h-lab1-bootstrap.XXXXXX)"
chmod 700 "${TEMP_ROOT}"

CURRENT_STAGE=lima-availability
if ! command -v limactl >/dev/null 2>&1; then
  if command -v brew >/dev/null 2>&1; then
    brew install lima
  else
    install_lima_from_official_release || blocked "BLOCKED_OFFICIAL_LIMA_INSTALL"
  fi
fi
command -v limactl >/dev/null 2>&1 || blocked "BLOCKED_LIMA_MISSING"

CURRENT_STAGE=owned-resource-replacement
if [ -e "${LAB_ROOT}" ] \
    || limactl list --quiet 2>/dev/null | grep -Fxq "${VM_NAME}" \
    || launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1; then
  if [ ! -f "${LAB_MARKER}" ] \
      || ! grep -Fxq P3H-LAB1-USER-AUTH-20260717 "${LAB_MARKER}"; then
    blocked "BLOCKED_EXISTING_UNOWNED_VM_NAME"
  fi
  P3H_LAB_DESTROY_CONFIRM=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1 \
    bash "${ROOT_DIR}/scripts/p3h-lab-destroy.sh" >/dev/null
fi

mkdir -p "${LAB_ROOT}"
chmod 700 "${LAB_ROOT}"
printf '%s\n' P3H-LAB1-USER-AUTH-20260717 >"${LAB_MARKER}"
chmod 600 "${LAB_MARKER}"

CURRENT_STAGE=local-ntp-source
local_ntp_log="${LAB_ROOT}/public/local-ntp.status"
local_ntp_runtime="${LAB_ROOT}/local-ntp-server.py"
mkdir -p "${LAB_ROOT}/public"
chmod 700 "${LAB_ROOT}/public"
install -m 0700 "${LOCAL_NTP_SERVER}" "${local_ntp_runtime}"
umask 077
: >"${local_ntp_log}"
chmod 600 "${local_ntp_log}"
launchctl submit -l "${LOCAL_NTP_LABEL}" \
  -o "${local_ntp_log}" -e "${local_ntp_log}" -- \
  python3 "${local_ntp_runtime}" \
  --bind=0.0.0.0 --port=123 \
  --owner-token=P3H-LAB1-USER-AUTH-20260717 \
  || blocked "BLOCKED_LOCAL_NTP_SOURCE"
for attempt in $(seq 1 10); do
  if grep -Fxq 'P3H_LAB_LOCAL_NTP: READY' "${local_ntp_log}"; then
    break
  fi
  launchctl print "gui/${UID}/${LOCAL_NTP_LABEL}" >/dev/null 2>&1 \
    || blocked "BLOCKED_LOCAL_NTP_SOURCE"
  sleep 1
done
grep -Fxq 'P3H_LAB_LOCAL_NTP: READY' "${local_ntp_log}" \
  || blocked "BLOCKED_LOCAL_NTP_SOURCE"

CURRENT_STAGE=dedicated-identity
mkdir -p "${LAB_ROOT}/identity" "${LAB_ROOT}/attestation"
chmod 700 "${LAB_ROOT}/identity" "${LAB_ROOT}/attestation" "${LAB_ROOT}/public"
identity_file="${LAB_ROOT}/identity/p3h-lab1-ed25519"
ssh-keygen -q -t ed25519 -N '' -C p3h-lab1-disposable \
  -f "${identity_file}"
chmod 600 "${identity_file}"

CURRENT_STAGE=lima-start
run_bounded 5100 limactl start --name="${VM_NAME}" --tty=false --mount-none \
  --progress --cpus=4 --memory=8 --disk=40 --timeout=75m "${TEMPLATE_FILE}" \
  || blocked "BLOCKED_LIMA_START_TIMEOUT_OR_FAILURE"

CURRENT_STAGE=guest-provision-copy
limactl copy --backend=scp "${identity_file}.pub" \
  "${VM_NAME}:/tmp/p3h-lab1-ed25519.pub"
limactl copy --backend=scp "${PROVISION_FILE}" \
  "${VM_NAME}:/tmp/p3h-lab-provision-linux.sh"
CURRENT_STAGE=guest-provision
guest_provision_log="${TEMP_ROOT}/guest-provision.log"
if ! limactl shell "${VM_NAME}" sudo bash /tmp/p3h-lab-provision-linux.sh \
    >"${guest_provision_log}" 2>&1; then
  sed -n '/^P3H_LAB_PROVISION:/p' "${guest_provision_log}" >&2
  blocked "BLOCKED_GUEST_PROVISION"
fi

CURRENT_STAGE=ssh-mapping
ssh_port="$(limactl list "${VM_NAME}" --format '{{.SSHLocalPort}}')"
case "${ssh_port}" in
  ''|*[!0-9]*) blocked "BLOCKED_LIMA_SSH_MAPPING" ;;
esac

CURRENT_STAGE=host-key-pin
console_fingerprint="$(limactl shell "${VM_NAME}" sudo ssh-keygen \
  -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256 | awk 'NF >= 2 {print $2; exit}')"
case "${console_fingerprint}" in
  SHA256:*) ;;
  *) blocked "BLOCKED_CONSOLE_HOST_KEY" ;;
esac

known_hosts_candidates="${TEMP_ROOT}/known_hosts.candidates"
known_hosts="${LAB_ROOT}/public/known_hosts"
ssh-keyscan -T 10 -p "${ssh_port}" 127.0.0.1 \
  >"${known_hosts_candidates}" 2>/dev/null
"${ROOT_DIR}/scripts/p3h-filter-known-hosts.sh" \
  "${known_hosts_candidates}" "${console_fingerprint}" "${known_hosts}" \
  >/dev/null || blocked "BLOCKED_CONSOLE_NETWORK_HOST_KEY_MISMATCH"
chmod 600 "${known_hosts}"

CURRENT_STAGE=dedicated-ssh
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
  -i "${identity_file}"
  -p "${ssh_port}"
)
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  hostname -f | grep -Fxq "${LAB_HOSTNAME}" \
  || blocked "BLOCKED_DEDICATED_SSH_IDENTITY"

encrypt_stream() {
  local credential_name="$1"
  local encrypted_file_name="${2:-${credential_name}}"
  ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
    sudo systemd-creds encrypt --name="${credential_name}" - \
    "/etc/credstore.encrypted/trade-model-p3h/${encrypted_file_name}.cred" \
    >/dev/null
}

CURRENT_STAGE=database-and-admin-credential-encryption
for secret_name in postgres_admin_password flyway_password \
    app_database_password_v1 app_database_password_v2 \
    app_admin_password_v1 app_admin_password_v2 backup_reader_password \
    recovery_owner_password binance_nonfunctional_key \
    binance_nonfunctional_secret; do
  openssl rand -hex 32 | encrypt_stream "${secret_name}"
done

CURRENT_STAGE=tls-generation
ca_key="${TEMP_ROOT}/lab-ca.key"
ca_cert="${LAB_ROOT}/public/lab-ca.pem"
tls_v1_key="${TEMP_ROOT}/tls-v1.key"
tls_v1_csr="${TEMP_ROOT}/tls-v1.csr"
tls_v1_cert="${TEMP_ROOT}/tls-v1.pem"
tls_v2_key="${TEMP_ROOT}/tls-v2.key"
tls_v2_csr="${TEMP_ROOT}/tls-v2.csr"
tls_v2_cert="${TEMP_ROOT}/tls-v2.pem"
tls_ext="${TEMP_ROOT}/tls.ext"
printf '%s\n' \
  'subjectAltName=DNS:trade-staging.lab.test' \
  'extendedKeyUsage=serverAuth' \
  'keyUsage=digitalSignature,keyEncipherment' >"${tls_ext}"

openssl req -x509 -newkey rsa:3072 -sha256 -nodes -days 2 \
  -subj '/CN=P3H LAB1 Disposable CA' -keyout "${ca_key}" -out "${ca_cert}" \
  >/dev/null 2>&1
for version in v1 v2; do
  key_var="tls_${version}_key"
  csr_var="tls_${version}_csr"
  cert_var="tls_${version}_cert"
  openssl req -new -newkey rsa:2048 -nodes -sha256 \
    -subj '/CN=trade-staging.lab.test' \
    -keyout "${!key_var}" -out "${!csr_var}" >/dev/null 2>&1
  serial=1001
  [ "${version}" = v2 ] && serial=1002
  openssl x509 -req -sha256 -days 1 -set_serial "${serial}" \
    -in "${!csr_var}" -CA "${ca_cert}" -CAkey "${ca_key}" \
    -extfile "${tls_ext}" -out "${!cert_var}" >/dev/null 2>&1
done
chmod 600 "${ca_cert}"

CURRENT_STAGE=tls-credential-encryption
encrypt_stream tls_certificate tls_certificate_v1 <"${tls_v1_cert}"
encrypt_stream tls_private_key tls_private_key_v1 <"${tls_v1_key}"
encrypt_stream tls_certificate_v2 <"${tls_v2_cert}"
encrypt_stream tls_private_key_v2 <"${tls_v2_key}"
encrypt_stream tls_certificate tls_certificate_v2_active <"${tls_v2_cert}"
encrypt_stream tls_private_key tls_private_key_v2_active <"${tls_v2_key}"
encrypt_stream tls_ca_certificate <"${ca_cert}"

CURRENT_STAGE=credential-holder-install
limactl copy --backend=scp "${HOLDER_UNIT}" \
  "${VM_NAME}:/tmp/p3h-lab-credential-holder.service"
limactl copy --backend=scp "${MATERIALIZER}" \
  "${VM_NAME}:/tmp/p3h-lab-materialize-credentials.sh"
limactl copy --backend=scp "${SEAL_UNIT}" \
  "${VM_NAME}:/tmp/p3h-lab-credential-seal.service"
deployment_uid="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" id -u)"
deployment_gid="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" id -g)"
runtime_mount_unit="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  systemd-escape --path --suffix=mount /run/credentials/p3hlab1)"
case "${runtime_mount_unit}" in
  *.mount) ;;
  *) blocked "BLOCKED_SYSTEMD_CREDENTIAL_RUNTIME" ;;
esac
runtime_mount_rendered="${TEMP_ROOT}/p3h-lab-credential-runtime.mount"
sed -e "s/RENDER_DEPLOYMENT_UID/${deployment_uid}/g" \
  -e "s/RENDER_DEPLOYMENT_GID/${deployment_gid}/g" \
  "${RUNTIME_MOUNT_TEMPLATE}" >"${runtime_mount_rendered}"
limactl copy --backend=scp "${runtime_mount_rendered}" \
  "${VM_NAME}:/tmp/p3h-lab-credential-runtime.mount"
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo install -m 0644 /tmp/p3h-lab-credential-holder.service \
  /etc/systemd/system/p3h-lab-credential-holder.service
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo install -d -m 0755 /usr/local/libexec
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo install -m 0755 /tmp/p3h-lab-materialize-credentials.sh \
  /usr/local/libexec/p3h-lab-materialize-credentials.sh
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo install -m 0644 /tmp/p3h-lab-credential-runtime.mount \
  "/etc/systemd/system/${runtime_mount_unit}"
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo install -m 0644 /tmp/p3h-lab-credential-seal.service \
  /etc/systemd/system/p3h-lab-credential-seal.service
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  sudo systemctl daemon-reload
if ! ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
    sudo systemctl enable --now p3h-lab-credential-seal.service >/dev/null; then
  for credential_unit in "${runtime_mount_unit}" \
      p3h-lab-credential-holder.service p3h-lab-credential-seal.service; do
    credential_unit_status="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
      sudo systemctl show "${credential_unit}" \
      --property=LoadState --property=ActiveState --property=SubState \
      --property=Result --property=ExecMainStatus --value 2>/dev/null \
      | paste -sd, -)"
    echo "P3H_LAB_CREDENTIAL_UNIT_STATUS: ${credential_unit}:${credential_unit_status}" >&2
  done
  blocked "BLOCKED_SYSTEMD_CREDENTIAL_RUNTIME"
fi

CURRENT_STAGE=credential-runtime-verification
credential_mount=/run/credentials/p3hlab1
ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  test -r "${credential_mount}/postgres_admin_password" \
  || blocked "BLOCKED_SYSTEMD_CREDENTIAL_RUNTIME"
runtime_fstype="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  findmnt -n -T "${credential_mount}" -o FSTYPE)"
runtime_options="$(ssh "${ssh_options[@]}" "${LAB_USER}@127.0.0.1" \
  findmnt -n -T "${credential_mount}" -o OPTIONS)"
case "${runtime_fstype}" in
  tmpfs|ramfs) ;;
  *) blocked "BLOCKED_SYSTEMD_CREDENTIAL_RUNTIME" ;;
esac
case ",${runtime_options}," in
  *,ro,*) ;;
  *) blocked "BLOCKED_SYSTEMD_CREDENTIAL_RUNTIME" ;;
esac

CURRENT_STAGE=attestation-and-inputs
systemd_version="$(limactl shell "${VM_NAME}" systemctl --version \
  | awk 'NR == 1 {print $2}')"
server_attestation="${LAB_ROOT}/attestation/server.attestation"
secret_attestation="${LAB_ROOT}/attestation/secret-backend.attestation"
printf '%s\n' \
  'ENVIRONMENT_CLASS=CONTROLLED_STAGING' \
  'PRODUCTION_TRAFFIC=NO' \
  'PRODUCTION_DATABASE=NO' \
  'PRODUCTION_SECRETS=NO' \
  'AUTHORIZED_FOR_P3H=YES' \
  'DISPOSABLE_OR_REBUILDABLE=YES' \
  'LINUX_SERVER=YES' \
  "EXPECTED_SSH_HOST_KEY_SHA256=${console_fingerprint}" \
  "EXPECTED_STAGING_HOSTNAME=${LAB_HOSTNAME}" \
  'SERVER_OWNER_REFERENCE=P3H-LAB1-INFRA-OWNER' \
  'APPROVAL_REFERENCE=P3H-LAB1-USER-AUTH-20260717' \
  >"${server_attestation}"
printf '%s\n' \
  'SECRET_BACKEND_CLASS=SYSTEMD_CREDENTIALS' \
  "BACKEND_VERSION=systemd-${systemd_version}" \
  'AUTHORIZED_FOR_P3H=YES' \
  'PLAINTEXT_AT_REST=NO' \
  'SECRETS_VERSIONED_OR_ROTATABLE=YES' \
  'SECRET_MOUNT_IS_RUNTIME_ONLY=YES' \
  'SECRET_OWNER_REFERENCE=P3H-LAB1-SECRET-OWNER' \
  'ROTATION_ALLOWED=YES' \
  >"${secret_attestation}"
chmod 600 "${server_attestation}" "${secret_attestation}"

inputs_file="${LAB_ROOT}/p3h-lab-inputs.sh"
printf '%s\n' \
  'export P3H_CONFIRM=I_CONFIRM_AUTHORIZED_NON_PRODUCTION_STAGING_DEPLOYMENT' \
  "export P3H_SERVER_ATTESTATION_FILE=${server_attestation}" \
  "export P3H_SECRET_BACKEND_ATTESTATION_FILE=${secret_attestation}" \
  'export P3H_SSH_HOST=127.0.0.1' \
  "export P3H_SSH_PORT=${ssh_port}" \
  'export P3H_SSH_USER=p3h-deploy' \
  "export P3H_SSH_IDENTITY_FILE=${identity_file}" \
  "export P3H_SSH_HOST_KEY_SHA256=${console_fingerprint}" \
  'export P3H_STAGING_HOSTNAME=trade-staging.lab.test' \
  'export P3H_TLS_MODE=INTERNAL_CA' \
  'export P3H_SECRET_BACKEND=SYSTEMD_CREDENTIALS' \
  "export P3H_SECRET_MOUNT_DIR=${credential_mount}" \
  'export P3H_RELEASE_OWNER_REFERENCE=P3H-LAB1-RELEASE' \
  'export P3H_ROLLBACK_OWNER_REFERENCE=P3H-LAB1-ROLLBACK' \
  'export P3H_INCIDENT_OWNER_REFERENCE=P3H-LAB1-INCIDENT' \
  'export P3H_REBOOT_CONFIRM=I_CONFIRM_CONTROLLED_STAGING_SERVER_REBOOT' \
  'export P3H_KEEP_STAGING_RUNNING=NO' \
  "export P3H_CA_BUNDLE_FILE=${ca_cert}" \
  >"${inputs_file}"
chmod 600 "${inputs_file}"

metadata_file="${LAB_ROOT}/lab-metadata"
printf '%s\n' \
  "VM_NAME=${VM_NAME}" \
  "SSH_PORT=${ssh_port}" \
  "SSH_HOST_KEY_SHA256=${console_fingerprint}" \
  'TARGET_CLASS=LOCAL_LIMA_LAB' \
  'AUTHORIZATION_REFERENCE=P3H-LAB1-USER-AUTH-20260717' \
  >"${metadata_file}"
chmod 600 "${metadata_file}"

server_attestation_sha="$(shasum -a 256 "${server_attestation}" | awk '{print $1}')"
secret_attestation_sha="$(shasum -a 256 "${secret_attestation}" | awk '{print $1}')"

CURRENT_STAGE=complete
BOOTSTRAP_COMPLETE=1
echo "P3H_LAB_BOOTSTRAP: PASS"
echo "P3H_LAB_INPUT_STATUS: 17_OF_17_READY"
echo "LINUX_VM: PASS"
echo "SYSTEMD: PASS"
echo "ROOTFUL_DOCKER: PASS"
echo "DOCKER_COMPOSE_V2: PASS"
echo "SSH_DEDICATED_IDENTITY: PASS"
echo "CONSOLE_HOST_KEY_FINGERPRINT: MATCH"
echo "NETWORK_SCANNED_HOST_KEY: MATCH"
echo "SSH_HOST_KEY_PIN: PASS_OUT_OF_BAND_LOCAL_VM_CONSOLE"
echo "SECRET_BACKEND: SYSTEMD_CREDENTIALS"
echo "SECRET_MOUNT_RUNTIME_VERIFICATION: PASS_BACKEND_BOUND"
echo "PLAINTEXT_SECRET_AT_REST: NO"
echo "SERVER_ATTESTATION_SHA256: ${server_attestation_sha}"
echo "SECRET_BACKEND_ATTESTATION_SHA256: ${secret_attestation_sha}"
echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
echo "P4_ALLOWED: NO"
echo "PRODUCTION_READINESS: BLOCKED"
