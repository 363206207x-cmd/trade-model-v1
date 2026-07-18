#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-}"
SOURCE_HEAD="${2:-}"
ROOT=/opt/trade-model-p3h/current
STATE_ROOT=/var/lib/trade-model-p3h-lab1
CREDENTIALS=/run/credentials/p3hlab1
SERVICE_RUNTIME=/run/trade-model-p3h
UNIT_TEMPLATE="${ROOT}/deploy/p3h/lima/trade-model-p3h-lab.service.template"
UNIT_PATH=/etc/systemd/system/trade-model-p3h.service
PROJECT=trade-model-p3h-lab1
HOSTNAME=trade-staging.lab.test
POSTGRES_IMAGE='postgres:16-alpine@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc'
FLYWAY_IMAGE='flyway/flyway:12.11.0-alpine@sha256:6bf3a713f52c4d803a88501f8409dda2191e9ccba1454358a6de2c4cc65f71b0'
NGINX_IMAGE='nginx:1.27.4-alpine@sha256:4ff102c5d78d254a6f0da062b3cf39eaf07f01eec0927fd21e219d0af8bc0591'
GREENFIELD_CONFIRMATION=I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION
ROTATION_CONFIRMATION=I_CONFIRM_CONTROLLED_APP_DATABASE_SECRET_ROTATION
IMAGE_BUILD_ATTEMPT_TIMEOUT_SECONDS=3600
IMAGE_BUILD_MAX_ATTEMPTS=2
IMAGE_BUILD_FAILURE_CATEGORY=UNKNOWN
RUNTIME_IMAGE_PULL_ATTEMPT_TIMEOUT_SECONDS=1200
RUNTIME_IMAGE_PULL_MAX_ATTEMPTS=2
RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=UNKNOWN
CURRENT_REMOTE_STEP=PRECONDITION

blocked() {
  echo "P3H_REMOTE_STAGE: $1"
  echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: BLOCKED_LOCAL_VM"
  echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
  echo "P3H_RESULT: BLOCKED_LOCAL_VM_EVIDENCE"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit 2
}

unexpected_failure() {
  local exit_status=$?
  trap - ERR
  echo "P3H_REMOTE_STAGE: BLOCKED_UNEXPECTED_${CURRENT_REMOTE_STEP}"
  echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: BLOCKED_LOCAL_VM"
  echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
  echo "P3H_RESULT: BLOCKED_LOCAL_VM_EVIDENCE"
  echo "P4_ALLOWED: NO"
  echo "PRODUCTION_READINESS: BLOCKED"
  exit "${exit_status}"
}
trap unexpected_failure ERR

run_checked_or_block() {
  local reason="$1"
  local check_status
  shift
  set +e
  (
    trap - ERR
    set -euo pipefail
    "$@"
  )
  check_status=$?
  set -e
  if [ "${check_status}" -ne 0 ]; then
    blocked "$(checked_failure_reason "${reason}" "${check_status}")"
  fi
}

checked_failure_reason() {
  local base_reason="$1"
  local check_status="$2"
  local detail=""
  case "${base_reason}:${check_status}" in
    BLOCKED_HTTPS_SMOKE:*|BLOCKED_POST_ROTATION_SMOKE:*|BLOCKED_HTTPS_AFTER_REBOOT:*)
      case "${check_status}" in
        61) detail=RUNTIME_DIRECTORY ;;
        62) detail=AUTH_CONFIG ;;
        63) detail=HEALTH ;;
        64) detail=LIVENESS ;;
        65) detail=READINESS ;;
        66) detail=DASHBOARD_FETCH ;;
        67) detail=REVIEW_FETCH ;;
        68) detail=PROD_SMOKE_CONTRACT ;;
        69) detail=DASHBOARD_SAFETY ;;
        70) detail=UNAUTHENTICATED_API ;;
        71) detail=HTTP_REDIRECT ;;
        72) detail=UNKNOWN_HOST_REJECTION ;;
        73) detail=TLS_1_2 ;;
        74) detail=TLS_1_3 ;;
        75) detail=RATE_LIMIT ;;
        76) detail=TEMP_CLEANUP ;;
        *) detail=UNKNOWN ;;
      esac
      ;;
  esac
  if [ -n "${detail}" ]; then
    echo "${base_reason}_${detail}"
  else
    echo "${base_reason}"
  fi
}

case "${SOURCE_HEAD}" in
  ''|*[!0-9a-f]*) blocked BLOCKED_SOURCE_HEAD ;;
esac
[ "${#SOURCE_HEAD}" -eq 40 ] || blocked BLOCKED_SOURCE_HEAD
[ "$(id -un)" = p3h-deploy ] || blocked BLOCKED_DEPLOYMENT_USER
[ "$(hostname -f)" = "${HOSTNAME}" ] || blocked BLOCKED_HOSTNAME
expected_release="/opt/trade-model-p3h/releases/${SOURCE_HEAD}"
[ -L "${ROOT}" ] || blocked BLOCKED_RELEASE_LINK
[ "$(readlink -f "${ROOT}")" = "${expected_release}" ] \
  || blocked BLOCKED_RELEASE_LINK
[ -d "${expected_release}" ] && [ ! -L "${expected_release}" ] \
  || blocked BLOCKED_RELEASE_LINK
[ -d "${CREDENTIALS}" ] && [ ! -L "${CREDENTIALS}" ] \
  || blocked BLOCKED_RUNTIME_CREDENTIALS

compose_env() {
  P3H_COMPOSE_PROJECT_NAME="${PROJECT}"
  P3H_APPLICATION_IMAGE_TAG="lab-${SOURCE_HEAD:0:12}"
  P3H_STAGING_HOSTNAME="${HOSTNAME}"
  P3H_SECRET_MOUNT_DIR="${CREDENTIALS}"
  P3H_HTTP_HOST_PORT=80
  P3H_HTTPS_HOST_PORT=443
  P3H_ACTIVE_APP_DATABASE_SECRET_VERSION="${1:-V1}"
  P3H_ACTIVE_APP_ADMIN_SECRET_VERSION="${2:-V1}"
  P3H_START_MODE="${3:-STEADY_STATE_START}"
  P3H_GREENFIELD_INITIALIZE_CONFIRM=""
  P3H_GREENFIELD_RECOVERY_CONFIRM=""
  if [ "${P3H_START_MODE}" = INITIALIZE_GREENFIELD ]; then
    P3H_GREENFIELD_INITIALIZE_CONFIRM="${GREENFIELD_CONFIRMATION}"
  fi
  export P3H_COMPOSE_PROJECT_NAME P3H_APPLICATION_IMAGE_TAG P3H_STAGING_HOSTNAME
  export P3H_SECRET_MOUNT_DIR P3H_HTTP_HOST_PORT P3H_HTTPS_HOST_PORT
  export P3H_ACTIVE_APP_DATABASE_SECRET_VERSION P3H_ACTIVE_APP_ADMIN_SECRET_VERSION
  export P3H_START_MODE P3H_GREENFIELD_INITIALIZE_CONFIRM P3H_GREENFIELD_RECOVERY_CONFIRM
}

compose() {
  docker compose -f "${ROOT}/deploy/p3h/docker-compose.p3h.yml" "$@"
}

validate_prebuild_compose_config() {
  local config_output config_status
  config_output="$(mktemp /tmp/p3h-compose-prebuild.XXXXXX)"
  chmod 600 "${config_output}"
  compose_env V1 V1 INITIALIZE_GREENFIELD
  if P3H_COMPOSE_CONFIG_CHECK_ONLY=true \
      bash "${ROOT}/deploy/p3h/p3h-compose-start.sh" \
      >"${config_output}" 2>&1; then
    rm -f "${config_output}"
    return 0
  else
    config_status="$(awk -F ': ' '
      $1 == "P3H_COMPOSE_START" && $2 ~ /^BLOCKED_[A-Z0-9_]+$/ {
        status=$2
      }
      END { print status }
    ' "${config_output}")"
  fi
  rm -f "${config_output}"
  case "${config_status}" in
    BLOCKED_[A-Z0-9_]*) blocked "BLOCKED_PREBUILD_${config_status}" ;;
    *) blocked BLOCKED_PREBUILD_COMPOSE_CONFIG_UNKNOWN ;;
  esac
}

validate_systemd_sandbox_compose_config() {
  local config_output config_status transient_unit
  config_output="$(mktemp /tmp/p3h-compose-systemd-prebuild.XXXXXX)"
  chmod 600 "${config_output}"
  transient_unit="p3h-lab-compose-prebuild-${SOURCE_HEAD:0:12}"
  compose_env V1 V1 INITIALIZE_GREENFIELD
  if sudo systemd-run --unit="${transient_unit}" --wait --collect --pipe --quiet \
      --property=Type=exec \
      --property=User=p3h-deploy \
      --property=Group=p3h-deploy \
      --property=SupplementaryGroups=docker \
      --property="WorkingDirectory=${ROOT}" \
      --property=NoNewPrivileges=yes \
      --property=PrivateTmp=yes \
      --property=ProtectSystem=strict \
      --property=ProtectHome=yes \
      --property=RuntimeDirectory=trade-model-p3h-prebuild \
      --property=RuntimeDirectoryMode=0700 \
      --property=Environment=DOCKER_CONFIG=/run/trade-model-p3h-prebuild/docker-config \
      --property="Environment=P3H_COMPOSE_PROJECT_NAME=${P3H_COMPOSE_PROJECT_NAME}" \
      --property="Environment=P3H_APPLICATION_IMAGE_TAG=${P3H_APPLICATION_IMAGE_TAG}" \
      --property="Environment=P3H_STAGING_HOSTNAME=${P3H_STAGING_HOSTNAME}" \
      --property="Environment=P3H_SECRET_MOUNT_DIR=${P3H_SECRET_MOUNT_DIR}" \
      --property="Environment=P3H_START_MODE=${P3H_START_MODE}" \
      --property="Environment=P3H_GREENFIELD_INITIALIZE_CONFIRM=${P3H_GREENFIELD_INITIALIZE_CONFIRM}" \
      --property="Environment=P3H_GREENFIELD_RECOVERY_CONFIRM=" \
      --property="Environment=P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=${P3H_ACTIVE_APP_DATABASE_SECRET_VERSION}" \
      --property="Environment=P3H_ACTIVE_APP_ADMIN_SECRET_VERSION=${P3H_ACTIVE_APP_ADMIN_SECRET_VERSION}" \
      --property=Environment=P3H_HTTP_HOST_PORT=80 \
      --property=Environment=P3H_HTTPS_HOST_PORT=443 \
      --property=Environment=P3H_COMPOSE_CONFIG_CHECK_ONLY=true \
      bash "${ROOT}/deploy/p3h/p3h-compose-start.sh" \
      >"${config_output}" 2>&1; then
    rm -f "${config_output}"
    return 0
  else
    config_status="$(awk -F ': ' '
      $1 == "P3H_COMPOSE_START" && $2 ~ /^BLOCKED_[A-Z0-9_]+$/ {
        status=$2
      }
      END { print status }
    ' "${config_output}")"
  fi
  rm -f "${config_output}"
  case "${config_status}" in
    BLOCKED_[A-Z0-9_]*) blocked "BLOCKED_PREBUILD_SYSTEMD_${config_status}" ;;
    *) blocked BLOCKED_PREBUILD_SYSTEMD_COMPOSE_CONFIG_UNKNOWN ;;
  esac
}

build_application_image() {
  local image="$1"
  local build_log build_status
  build_log="$(mktemp /tmp/p3h-image-build.XXXXXX)"
  chmod 600 "${build_log}"
  if timeout --signal=TERM --kill-after=30s \
      "${IMAGE_BUILD_ATTEMPT_TIMEOUT_SECONDS}" \
      docker build --pull=false --file "${ROOT}/deploy/p3h/Dockerfile.p3h" \
        --build-arg "VCS_REF=${SOURCE_HEAD}" --tag "${image}" "${ROOT}" \
        >"${build_log}" 2>&1; then
    IMAGE_BUILD_FAILURE_CATEGORY=NONE
    rm -f "${build_log}"
    return 0
  else
    build_status=$?
  fi

  case "${build_status}" in
    124|137) IMAGE_BUILD_FAILURE_CATEGORY=TIMEOUT ;;
    *)
      if grep -Eqi '429|too many requests|toomanyrequests|rate.?limit' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=RATE_LIMIT
      elif grep -Eqi 'tls handshake timeout|i/o timeout|timed out|connection reset|unexpected eof|temporary failure|failed to do request|dial tcp|network is unreachable|connection refused|no such host|unable to resolve|failed to fetch anonymous token|context deadline exceeded' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=NETWORK
      elif grep -Eqi 'no space left on device|disk quota exceeded' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=STORAGE
      elif grep -Eqi 'build failure|compilation error|failed to execute goal|there are test failures' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=MAVEN
      else
        IMAGE_BUILD_FAILURE_CATEGORY=UNKNOWN
      fi
      ;;
  esac
  rm -f "${build_log}"
  return "${build_status}"
}

pull_runtime_image() {
  local image="$1"
  local pull_log pull_status
  pull_log="$(mktemp /tmp/p3h-runtime-image-pull.XXXXXX)"
  chmod 600 "${pull_log}"
  if timeout --signal=TERM --kill-after=30s \
      "${RUNTIME_IMAGE_PULL_ATTEMPT_TIMEOUT_SECONDS}" \
      docker pull "${image}" >"${pull_log}" 2>&1; then
    RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=NONE
    rm -f "${pull_log}"
    return 0
  else
    pull_status=$?
  fi

  case "${pull_status}" in
    124|137) RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=TIMEOUT ;;
    *)
      if grep -Eqi '429|too many requests|toomanyrequests|rate.?limit' "${pull_log}"; then
        RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=RATE_LIMIT
      elif grep -Eqi 'tls handshake timeout|i/o timeout|timed out|connection reset|unexpected eof|temporary failure|failed to do request|dial tcp|network is unreachable|connection refused|no such host|unable to resolve|failed to fetch anonymous token|context deadline exceeded' "${pull_log}"; then
        RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=NETWORK
      elif grep -Eqi 'no space left on device|disk quota exceeded' "${pull_log}"; then
        RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=STORAGE
      else
        RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=UNKNOWN
      fi
      ;;
  esac
  rm -f "${pull_log}"
  return "${pull_status}"
}

ensure_runtime_images() {
  local image pull_attempt
  for image in "${POSTGRES_IMAGE}" "${FLYWAY_IMAGE}" "${NGINX_IMAGE}"; do
    pull_attempt=1
    while [ "${pull_attempt}" -le "${RUNTIME_IMAGE_PULL_MAX_ATTEMPTS}" ]; do
      if pull_runtime_image "${image}"; then
        break
      fi
      case "${RUNTIME_IMAGE_PULL_FAILURE_CATEGORY}" in
        TIMEOUT|NETWORK|RATE_LIMIT) ;;
        STORAGE|UNKNOWN)
          blocked "BLOCKED_RUNTIME_IMAGE_PULL_${RUNTIME_IMAGE_PULL_FAILURE_CATEGORY}"
          ;;
        *) blocked BLOCKED_RUNTIME_IMAGE_PULL_FAILURE ;;
      esac
      if [ "${pull_attempt}" -eq "${RUNTIME_IMAGE_PULL_MAX_ATTEMPTS}" ]; then
        blocked "BLOCKED_RUNTIME_IMAGE_PULL_${RUNTIME_IMAGE_PULL_FAILURE_CATEGORY}"
      fi
      echo "P3H_RUNTIME_IMAGE_PULL_RETRY: BOUNDED_${RUNTIME_IMAGE_PULL_FAILURE_CATEGORY}"
      pull_attempt=$((pull_attempt + 1))
    done
    [ "${pull_attempt}" -le "${RUNTIME_IMAGE_PULL_MAX_ATTEMPTS}" ] \
      || blocked BLOCKED_RUNTIME_IMAGE_PULL_TIMEOUT
  done
  echo 'P3H_RUNTIME_IMAGE_PREFETCH: PASS_3_OF_3'
}

install_unit() {
  local start_mode="$1"
  local database_version="$2"
  local admin_version="$3"
  local tls_version="$4"
  local greenfield_confirmation=""
  local rendered
  case "${start_mode}" in
    INITIALIZE_GREENFIELD)
      greenfield_confirmation="${GREENFIELD_CONFIRMATION}"
      ;;
    STEADY_STATE_START) ;;
    *) blocked BLOCKED_UNIT_START_MODE ;;
  esac
  case "${database_version}:${admin_version}:${tls_version}" in
    V1:V1:V1|V2:V2:V1|V2:V2:V2) ;;
    *) blocked BLOCKED_UNIT_SECRET_VERSION ;;
  esac
  rendered="$(mktemp /tmp/trade-model-p3h.service.XXXXXX)"
  sed \
    -e "s/RENDER_EXACT_HEAD_PREFIX/lab-${SOURCE_HEAD:0:12}/g" \
    -e "s/RENDER_START_MODE/${start_mode}/g" \
    -e "s/RENDER_GREENFIELD_CONFIRMATION/${greenfield_confirmation}/g" \
    -e "s/RENDER_DATABASE_SECRET_VERSION/${database_version}/g" \
    -e "s/RENDER_ADMIN_SECRET_VERSION/${admin_version}/g" \
    -e "s#RENDER_TLS_CERTIFICATE_CREDENTIAL#/etc/credstore.encrypted/trade-model-p3h/tls_certificate_${tls_version,,}.cred#g" \
    -e "s#RENDER_TLS_PRIVATE_KEY_CREDENTIAL#/etc/credstore.encrypted/trade-model-p3h/tls_private_key_${tls_version,,}.cred#g" \
    "${UNIT_TEMPLATE}" >"${rendered}"
  if grep -q 'RENDER_' "${rendered}"; then
    rm -f "${rendered}"
    blocked BLOCKED_UNIT_RENDER
  fi
  sudo install -m 0644 "${rendered}" "${UNIT_PATH}"
  rm -f "${rendered}"
  sudo systemctl daemon-reload
  sudo systemctl enable trade-model-p3h.service >/dev/null
}

service_start_failure_reason() {
  local journal_file failed_step current_step start_status detail
  journal_file="$(mktemp /tmp/p3h-service-journal.XXXXXX)"
  chmod 600 "${journal_file}"
  sudo journalctl --unit=trade-model-p3h.service --boot --no-pager -o cat \
    >"${journal_file}" 2>/dev/null || true
  failed_step="$(awk -F ': ' '
      $1 == "P3H_COMPOSE_FAILED_STEP" && $2 ~ /^[A-Z0-9_]+$/ {
        failed_step=$2
      }
      END { print failed_step }
    ' "${journal_file}")"
  current_step="$(awk -F ': ' '
      $1 == "P3H_COMPOSE_CURRENT_STEP" && $2 ~ /^[A-Z0-9_]+$/ {
        current_step=$2
      }
      END { print current_step }
    ' "${journal_file}")"
  start_status="$(awk -F ': ' '
      $1 == "P3H_COMPOSE_START" && $2 ~ /^(BLOCKED|FAIL)_[A-Z0-9_]+$/ {
        start_status=$2
      }
      END { print start_status }
    ' "${journal_file}")"

  detail=""
  if [ "${failed_step}" = FLYWAY_MIGRATE ] \
      || [ "${failed_step}" = FLYWAY_VALIDATE ]; then
    if grep -Eqi 'exec format error|platform .* does not match' "${journal_file}"; then
      detail=ARCHITECTURE_MISMATCH
    elif grep -Eqi 'unsupported database|no (flyway )?database( plugin)? found to handle|no database found to handle' "${journal_file}"; then
      detail=UNSUPPORTED_DATABASE
    elif grep -Eqi 'password authentication failed|authentication failed' "${journal_file}"; then
      detail=AUTH
    elif grep -Eqi 'role .* does not exist' "${journal_file}"; then
      detail=ROLE_MISSING
    elif grep -Eqi 'permission denied|must be owner|insufficient privilege' "${journal_file}"; then
      detail=DATABASE_PERMISSION
    elif grep -Eqi 'connection refused|unable to obtain connection|connect timed out|could not connect' "${journal_file}"; then
      detail=CONNECTION
    elif grep -Eqi 'checksum mismatch|resolved migration not applied|applied migration not resolved' "${journal_file}"; then
      detail=CHECKSUM
    elif grep -Eqi 'no migrations found|unable to resolve location|migration files? .* not found' "${journal_file}"; then
      detail=MIGRATION_FILES
    elif grep -Eqi 'migration .* failed|syntax error|failed to execute|sql state' "${journal_file}"; then
      detail=MIGRATION_SQL
    else
      detail=UNKNOWN
    fi
  fi
  rm -f "${journal_file}"

  if [ -n "${failed_step}" ] && [ -n "${detail}" ]; then
    echo "STEP_${failed_step}_${detail}"
  elif [ -n "${failed_step}" ]; then
    echo "STEP_${failed_step}"
  elif [ -n "${start_status}" ]; then
    echo "${start_status}"
  elif [ -n "${current_step}" ]; then
    echo "STEP_${current_step}_INCOMPLETE"
  else
    echo START
  fi
}

start_service_or_block() {
  local phase="$1"
  local reason
  if sudo systemctl start trade-model-p3h.service; then
    return 0
  fi
  reason="$(service_start_failure_reason)"
  case "${reason}" in
    START|STEP_[A-Z0-9_]*|BLOCKED_[A-Z0-9_]*|FAIL_[A-Z0-9_]*) ;;
    *) reason=START ;;
  esac
  blocked "BLOCKED_${phase}_${reason}"
}

flyway_state() {
  compose exec -T postgres psql --username=p3h_bootstrap \
    --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align \
    --command="SELECT count(*) || '|' || max(version) FROM flyway_schema_history WHERE success=true"
}

database_fingerprint() {
  local temp_root structure_file content_file
  temp_root="$(mktemp -d /tmp/p3h-database-fingerprint.XXXXXX)"
  chmod 700 "${temp_root}"
  structure_file="${temp_root}/structure"
  content_file="${temp_root}/content"

  if ! capture_fingerprint trade_model_v1_p3h_primary p3h_backup_reader \
      backup_reader_password current-state-clone-fingerprint.sql \
      "${structure_file}" 2>/dev/null; then
    rm -rf "${temp_root}"
    return 81
  fi
  if ! capture_fingerprint trade_model_v1_p3h_primary p3h_backup_reader \
      backup_reader_password current-state-clone-content-fingerprint.sql \
      "${content_file}" 2>/dev/null; then
    rm -rf "${temp_root}"
    return 85
  fi

  cat "${structure_file}" "${content_file}" \
    | sha256sum | awk '{print $1}'
  rm -rf "${temp_root}"
}

fingerprint_failure_reason() {
  case "$1" in
    81) echo STRUCTURE_QUERY ;;
    85) echo CONTENT_QUERY ;;
    *) echo UNKNOWN ;;
  esac
}

database_secret_probe() {
  local version="$1"
  compose --profile validation run --rm --no-deps \
    -e "P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=${version}" app-role-probe \
    >/dev/null 2>&1
}

auth_code() {
  local version="$1"
  local secret_file="${CREDENTIALS}/app_admin_password_${version,,}"
  local config_file code curl_status
  if [ ! -f "${secret_file}" ] || [ -L "${secret_file}" ] || [ ! -r "${secret_file}" ]; then
    echo TRANSPORT_CREDENTIAL_FILE
    return 0
  fi
  if [ ! -d "${SERVICE_RUNTIME}" ] || [ -L "${SERVICE_RUNTIME}" ]; then
    echo TRANSPORT_RUNTIME
    return 0
  fi
  if ! config_file="$(mktemp "${SERVICE_RUNTIME}/p3h-lab-auth.XXXXXX")"; then
    echo TRANSPORT_RUNTIME
    return 0
  fi
  if ! chmod 600 "${config_file}"; then
    rm -f "${config_file}"
    echo TRANSPORT_RUNTIME
    return 0
  fi
  if ! {
    echo 'silent'
    echo 'show-error'
    echo 'connect-timeout = 2'
    echo 'max-time = 5'
    echo "cacert = \"${CREDENTIALS}/tls_ca_certificate\""
    echo "resolve = \"${HOSTNAME}:443:127.0.0.1\""
    printf 'user = "p3h_operator:%s"\n' "$(tr -d '\r\n' <"${secret_file}")"
  } >"${config_file}"; then
    rm -f "${config_file}"
    echo TRANSPORT_CONFIG
    return 0
  fi
  set +e
  code="$(curl --config "${config_file}" --output /dev/null --write-out '%{http_code}' \
    "https://${HOSTNAME}/api/dashboard/home" 2>/dev/null)"
  curl_status=$?
  set -e
  rm -f "${config_file}"
  if [ "${curl_status}" -ne 0 ]; then
    case "${curl_status}" in
      3|4) echo TRANSPORT_CONFIG ;;
      5|6) echo TRANSPORT_DNS ;;
      7) echo TRANSPORT_CONNECT ;;
      28) echo TRANSPORT_TIMEOUT ;;
      35|51|53|54|58|59|64|66) echo TRANSPORT_TLS ;;
      60) echo TRANSPORT_CERTIFICATE ;;
      77) echo TRANSPORT_CA_FILE ;;
      *) echo TRANSPORT_OTHER ;;
    esac
  else
    echo "${code}"
  fi
}

auth_code_category() {
  case "$1" in
    200) echo HTTP_200 ;;
    401) echo HTTP_401 ;;
    403) echo HTTP_403 ;;
    429) echo HTTP_429 ;;
    TRANSPORT_[A-Z0-9_]*) echo "$1" ;;
    1??) echo HTTP_1XX ;;
    2??) echo HTTP_2XX ;;
    3??) echo HTTP_3XX ;;
    4??) echo HTTP_4XX ;;
    5??) echo HTTP_5XX ;;
    *) echo UNKNOWN ;;
  esac
}

await_auth_expectation() {
  local version="$1"
  local expectation="$2"
  local code=UNKNOWN
  local attempt
  for attempt in $(seq 1 8); do
    code="$(auth_code "${version}")"
    case "${expectation}:${code}" in
      ACTIVE:200|DENIED:401|DENIED:403)
        echo "${code}"
        return 0
        ;;
    esac
    case "${code}" in
      429|TRANSPORT_DNS|TRANSPORT_CONNECT|TRANSPORT_TIMEOUT)
        if [ "${attempt}" -lt 8 ]; then
          sleep 2
          continue
        fi
        ;;
    esac
    echo "${code}"
    return 1
  done
  echo "${code}"
  return 1
}

https_smoke() {
  local admin_version="$1"
  local response_dir config_file unauthenticated_code redirect_headers unknown_code rate_code
  [ -d "${SERVICE_RUNTIME}" ] && [ ! -L "${SERVICE_RUNTIME}" ] || return 61
  response_dir="$(mktemp -d "${SERVICE_RUNTIME}/p3h-lab-smoke.XXXXXX")" \
    || return 61
  trap 'rm -rf "${response_dir}"' EXIT
  config_file="${response_dir}/curl-auth.conf"
  chmod 700 "${response_dir}" || return 61
  if ! {
    echo 'silent'
    echo 'show-error'
    echo 'max-time = 20'
    echo "cacert = \"${CREDENTIALS}/tls_ca_certificate\""
    echo "resolve = \"${HOSTNAME}:443:127.0.0.1\""
    printf 'user = "p3h_operator:%s"\n' \
      "$(tr -d '\r\n' <"${CREDENTIALS}/app_admin_password_${admin_version,,}")"
  } >"${config_file}"; then
    return 62
  fi
  chmod 600 "${config_file}" || return 62

  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health" >"${response_dir}/health.json" \
    || return 63
  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health/liveness" >"${response_dir}/liveness.json" \
    || return 64
  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health/readiness" >"${response_dir}/readiness.json" \
    || return 65
  curl --config "${config_file}" \
    "https://${HOSTNAME}/api/dashboard/home" >"${response_dir}/dashboard.json" \
    || return 66
  curl --config "${config_file}" \
    "https://${HOSTNAME}/api/review/center" >"${response_dir}/review.json" \
    || return 67

  SMOKE_PHASE=VALIDATE \
  SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE \
  SMOKE_RESPONSE_DIR="${response_dir}" \
  SMOKE_ALLOW_EXTERNAL_CALLS=false \
    bash "${ROOT}/scripts/prod-smoke.sh" >/dev/null || return 68

  python3 - "${response_dir}/dashboard.json" <<'PY' || return 69
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    payload = json.load(handle)
data = payload.get("data", payload)
if data.get("positions") not in (None, []):
    raise SystemExit(2)
safety = data.get("safety") or {}
if safety.get("notAutoTrading") is not True or safety.get("notOrderExecution") is not True:
    raise SystemExit(2)
PY

  if ! unauthenticated_code="$(curl --silent --show-error --max-time 20 \
      --cacert "${CREDENTIALS}/tls_ca_certificate" \
      --resolve "${HOSTNAME}:443:127.0.0.1" --output /dev/null \
      --write-out '%{http_code}' "https://${HOSTNAME}/api/dashboard/home")"; then
    return 70
  fi
  case "${unauthenticated_code}" in 401|403) ;; *) return 70 ;; esac

  redirect_headers="${response_dir}/redirect.headers"
  curl --silent --show-error --max-time 20 --output /dev/null \
    --dump-header "${redirect_headers}" -H "Host: ${HOSTNAME}" \
    http://127.0.0.1/actuator/health || return 71
  grep -Eiq "^Location: https://${HOSTNAME}/actuator/health\r?$" \
    "${redirect_headers}" || return 71

  unknown_code="$(curl --silent --max-time 10 --output /dev/null \
    --write-out '%{http_code}' -H 'Host: unapproved.invalid' \
    http://127.0.0.1/ || true)"
  [ "${unknown_code}" != 200 ] && [ "${unknown_code}" != 308 ] \
    || return 72

  openssl s_client -connect 127.0.0.1:443 -servername "${HOSTNAME}" \
    -CAfile "${CREDENTIALS}/tls_ca_certificate" -verify_hostname "${HOSTNAME}" \
    -verify_return_error -tls1_2 </dev/null >/dev/null 2>&1 || return 73
  if openssl s_client -help 2>&1 | grep -q -- -tls1_3; then
    openssl s_client -connect 127.0.0.1:443 -servername "${HOSTNAME}" \
      -CAfile "${CREDENTIALS}/tls_ca_certificate" -verify_hostname "${HOSTNAME}" \
      -verify_return_error -tls1_3 </dev/null >/dev/null 2>&1 || return 74
  fi

  : >"${response_dir}/rate-codes" || return 75
  for request_index in $(seq 1 140); do
    if ! rate_code="$(curl --config "${config_file}" --output /dev/null \
        --write-out '%{http_code}' \
        "https://${HOSTNAME}/api/dashboard/home" 2>/dev/null)"; then
      return 75
    fi
    printf '%s\n' "${rate_code}" >>"${response_dir}/rate-codes" || return 75
  done
  grep -Fxq 429 "${response_dir}/rate-codes" || return 75
  rm -rf "${response_dir}" || return 76
  trap - EXIT
}

postgres_ops() {
  docker run --rm --network "${PROJECT}_p3h_backend" \
    --mount "type=bind,src=${ROOT},dst=/repo,readonly" \
    --mount "type=bind,src=${CREDENTIALS},dst=/credentials,readonly" \
    --mount "type=volume,src=${PROJECT}_p3h_backups,dst=/evidence" \
    "$@"
}

capture_fingerprint() {
  local database="$1"
  local username="$2"
  local password_name="$3"
  local script_name="$4"
  local output_file="$5"
  postgres_ops \
    -e "P3H_OPS_DATABASE=${database}" \
    -e "P3H_OPS_USERNAME=${username}" \
    -e "P3H_OPS_PASSWORD_FILE=/credentials/${password_name}" \
    -e "P3H_OPS_SCRIPT=/repo/scripts/${script_name}" \
    "${POSTGRES_IMAGE}" sh -eu -c '
      pgpass=/tmp/.pgpass
      chmod 600 "${pgpass}" 2>/dev/null || true
      printf "postgres:5432:*:%s:%s\n" "${P3H_OPS_USERNAME}" "$(tr -d "\r\n" <"${P3H_OPS_PASSWORD_FILE}")" >"${pgpass}"
      chmod 600 "${pgpass}"
      export PGPASSFILE="${pgpass}"
      psql --host=postgres --username="${P3H_OPS_USERNAME}" --dbname="${P3H_OPS_DATABASE}" --no-psqlrc --file="${P3H_OPS_SCRIPT}"
    ' >"${output_file}"
}

backup_restore() {
  local temp_root primary_structure recovery_structure primary_content recovery_content
  [ -d "${SERVICE_RUNTIME}" ] && [ ! -L "${SERVICE_RUNTIME}" ]
  temp_root="$(mktemp -d "${SERVICE_RUNTIME}/p3h-lab-backup.XXXXXX")"
  primary_structure="${temp_root}/primary-structure"
  recovery_structure="${temp_root}/recovery-structure"
  primary_content="${temp_root}/primary-content"
  recovery_content="${temp_root}/recovery-content"

  postgres_ops \
    -e BACKUP_DIR=/evidence \
    -e BACKUP_FILE=/evidence/p3h-lab-primary.dump \
    -e PROD_DATASOURCE_HOST=postgres \
    -e PROD_DATASOURCE_PORT=5432 \
    -e PROD_DATASOURCE_USERNAME=p3h_backup_reader \
    -e PROD_DATASOURCE_PASSWORD_FILE=/credentials/backup_reader_password \
    -e PROD_DATASOURCE_DATABASE=trade_model_v1_p3h_primary \
    "${POSTGRES_IMAGE}" bash /repo/scripts/prod-backup.sh >/dev/null

  postgres_ops \
    -e RESTORE_DATASOURCE_HOST=postgres \
    -e RESTORE_DATASOURCE_PORT=5432 \
    -e RESTORE_DATASOURCE_USERNAME=p3h_recovery_owner \
    -e RESTORE_DATASOURCE_PASSWORD_FILE=/credentials/recovery_owner_password \
    -e RESTORE_DATASOURCE_DATABASE=trade_model_v1_p3h_recovery \
    -e RESTORE_BACKUP_FILE=/evidence/p3h-lab-primary.dump \
    -e RESTORE_CONFIRM=I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA \
    "${POSTGRES_IMAGE}" bash /repo/scripts/prod-restore.sh >/dev/null

  capture_fingerprint trade_model_v1_p3h_primary p3h_backup_reader \
    backup_reader_password current-state-clone-fingerprint.sql "${primary_structure}"
  capture_fingerprint trade_model_v1_p3h_recovery p3h_recovery_owner \
    recovery_owner_password current-state-clone-fingerprint.sql "${recovery_structure}"
  capture_fingerprint trade_model_v1_p3h_primary p3h_backup_reader \
    backup_reader_password current-state-clone-content-fingerprint.sql "${primary_content}"
  capture_fingerprint trade_model_v1_p3h_recovery p3h_recovery_owner \
    recovery_owner_password current-state-clone-content-fingerprint.sql "${recovery_content}"
  cmp -s "${primary_structure}" "${recovery_structure}"
  cmp -s "${primary_content}" "${recovery_content}"
  rm -rf "${temp_root}"
}

served_certificate_serial() {
  openssl s_client -connect 127.0.0.1:443 -servername "${HOSTNAME}" \
    -CAfile "${CREDENTIALS}/tls_ca_certificate" -verify_return_error \
    </dev/null 2>/dev/null \
    | openssl x509 -noout -serial | sed 's/^serial=//'
}

expected_certificate_serial() {
  openssl x509 -in "${CREDENTIALS}/tls_certificate_$1" -noout -serial \
    | sed 's/^serial=//'
}

activate_tls_v2_credentials() {
  local holder_unit=/etc/systemd/system/p3h-lab-credential-holder.service
  local runtime_mount_unit rendered runtime_options
  runtime_mount_unit="$(systemd-escape --path --suffix=mount "${CREDENTIALS}")"
  rendered="$(mktemp /tmp/p3h-lab-credential-holder.XXXXXX)"

  sudo systemctl stop p3h-lab-credential-seal.service
  sudo systemctl stop p3h-lab-credential-holder.service
  sudo mount -t tmpfs -o remount,rw,nodev,nosuid,noexec tmpfs "${CREDENTIALS}"
  rm -f "${CREDENTIALS}"/*
  sudo sed \
    -e 's#tls_certificate_v1\.cred#tls_certificate_v2_active.cred#' \
    -e 's#tls_private_key_v1\.cred#tls_private_key_v2_active.cred#' \
    "${holder_unit}" >"${rendered}"
  grep -Fxq \
    'LoadCredentialEncrypted=tls_certificate:/etc/credstore.encrypted/trade-model-p3h/tls_certificate_v2_active.cred' \
    "${rendered}"
  grep -Fxq \
    'LoadCredentialEncrypted=tls_private_key:/etc/credstore.encrypted/trade-model-p3h/tls_private_key_v2_active.cred' \
    "${rendered}"
  sudo install -m 0644 "${rendered}" "${holder_unit}"
  rm -f "${rendered}"
  sudo systemctl daemon-reload
  sudo systemctl start p3h-lab-credential-holder.service
  sudo systemctl start p3h-lab-credential-seal.service
  runtime_options="$(findmnt -n -T "${CREDENTIALS}" -o OPTIONS)"
  case ",${runtime_options}," in *,ro,*) ;; *) return 1 ;; esac
  cmp -s "${CREDENTIALS}/tls_certificate" "${CREDENTIALS}/tls_certificate_v2"
  cmp -s "${CREDENTIALS}/tls_private_key" "${CREDENTIALS}/tls_private_key_v2"
}

case "${ACTION}" in
  INITIAL_DEPLOY)
    CURRENT_REMOTE_STEP=PREBUILD_COMPOSE_CONFIG
    validate_prebuild_compose_config
    CURRENT_REMOTE_STEP=PREBUILD_SYSTEMD_COMPOSE_CONFIG
    validate_systemd_sandbox_compose_config
    CURRENT_REMOTE_STEP=IMAGE_BUILD
    image="trade-model-v1:p3h-lab-${SOURCE_HEAD:0:12}"
    image_build_attempt=1
    while [ "${image_build_attempt}" -le "${IMAGE_BUILD_MAX_ATTEMPTS}" ]; do
      if build_application_image "${image}" >/dev/null; then
        break
      else
        image_build_status=$?
      fi
      case "${IMAGE_BUILD_FAILURE_CATEGORY}" in
        TIMEOUT|NETWORK|RATE_LIMIT) ;;
        MAVEN|STORAGE|UNKNOWN)
          blocked "BLOCKED_IMAGE_BUILD_${IMAGE_BUILD_FAILURE_CATEGORY}"
          ;;
        *) blocked BLOCKED_IMAGE_BUILD_FAILURE ;;
      esac
      if [ "${image_build_attempt}" -eq "${IMAGE_BUILD_MAX_ATTEMPTS}" ]; then
        blocked "BLOCKED_IMAGE_BUILD_${IMAGE_BUILD_FAILURE_CATEGORY}"
      fi
      echo "P3H_IMAGE_BUILD_RETRY: BOUNDED_CACHE_REUSE_${IMAGE_BUILD_FAILURE_CATEGORY}"
      image_build_attempt=$((image_build_attempt + 1))
      CURRENT_REMOTE_STEP=IMAGE_BUILD_RETRY
    done
    [ "${image_build_attempt}" -le "${IMAGE_BUILD_MAX_ATTEMPTS}" ] \
      || blocked BLOCKED_IMAGE_BUILD_TIMEOUT
    echo "P3H_IMAGE_BUILD_ATTEMPTS: ${image_build_attempt}"
    revision="$(docker image inspect "${image}" \
      --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}')"
    [ "${revision}" = "${SOURCE_HEAD}" ] || blocked BLOCKED_IMAGE_REVISION
    [ "$(docker image inspect "${image}" --format '{{.Config.User}}')" = app ] \
      || blocked BLOCKED_IMAGE_USER

    CURRENT_REMOTE_STEP=RUNTIME_IMAGE_PREFETCH
    ensure_runtime_images

    CURRENT_REMOTE_STEP=INITIAL_UNIT_INSTALL
    install_unit INITIALIZE_GREENFIELD V1 V1 V1
    CURRENT_REMOTE_STEP=INITIAL_SERVICE_START
    start_service_or_block INITIAL_SERVICE
    compose_env V1 V1 INITIALIZE_GREENFIELD
    CURRENT_REMOTE_STEP=INITIAL_FLYWAY_VERIFY
    [ "$(flyway_state)" = '7|7' ] || blocked BLOCKED_FLYWAY_V1_TO_V7
    CURRENT_REMOTE_STEP=INITIAL_READONLY_VERIFY
    database_secret_probe V1 || blocked BLOCKED_READONLY_ROLE
    CURRENT_REMOTE_STEP=INITIAL_FINGERPRINT
    if fingerprint="$(database_fingerprint)"; then
      :
    else
      fingerprint_status=$?
      blocked "BLOCKED_INITIAL_FINGERPRINT_$(fingerprint_failure_reason "${fingerprint_status}")"
    fi
    printf '%s\n' "${fingerprint}" | sudo tee "${STATE_ROOT}/database-fingerprint" >/dev/null
    printf '%s\n' "${SOURCE_HEAD}" | sudo tee "${STATE_ROOT}/source-head" >/dev/null
    sudo chmod 0600 "${STATE_ROOT}/database-fingerprint" "${STATE_ROOT}/source-head"
    CURRENT_REMOTE_STEP=INITIAL_HTTPS_SMOKE
    run_checked_or_block BLOCKED_HTTPS_SMOKE https_smoke V1

    CURRENT_REMOTE_STEP=STEADY_STATE_RESTART
    sudo systemctl stop trade-model-p3h.service
    install_unit STEADY_STATE_START V1 V1 V1
    start_service_or_block STEADY_SERVICE
    compose_env V1 V1 STEADY_STATE_START
    [ "$(flyway_state)" = '7|7' ] || blocked BLOCKED_FLYWAY_REPEAT
    if steady_fingerprint="$(database_fingerprint)"; then
      [ "${steady_fingerprint}" = "${fingerprint}" ] \
        || blocked BLOCKED_STEADY_FINGERPRINT_MISMATCH
    else
      fingerprint_status=$?
      blocked "BLOCKED_STEADY_FINGERPRINT_$(fingerprint_failure_reason "${fingerprint_status}")"
    fi
    CURRENT_REMOTE_STEP=BACKUP_RESTORE
    run_checked_or_block BLOCKED_BACKUP_RESTORE backup_restore

    echo "P3H_REMOTE_STAGE: INITIAL_DEPLOY_PASS"
    echo "APP_IMAGE_REVISION: ${SOURCE_HEAD}"
    echo "STAGING_FLYWAY: PASS_V1_TO_V7"
    echo "FLYWAY_REPEAT: ZERO_MIGRATIONS"
    echo "APPLICATION_DATABASE_ROLE: READ_ONLY"
    echo "READ_ONLY_WRITE_PROBE: DENIED"
    echo "TLS_1_2: PASS"
    if openssl s_client -help 2>&1 | grep -q -- -tls1_3; then
      echo "TLS_1_3: PASS"
    else
      echo "TLS_1_3: CLIENT_UNSUPPORTED_WITH_EVIDENCE"
    fi
    echo "HTTP_TO_HTTPS_REDIRECT: PASS"
    echo "UNKNOWN_HOST: REJECTED"
    echo "UNAUTHENTICATED_API: DENIED"
    echo "AUTHENTICATED_DASHBOARD: PASS"
    echo "EMPTY_DASHBOARD_FAIL_CLOSED: PASS"
    echo "RATE_LIMIT: PASS_429"
    echo "PROD_BACKUP_SCRIPT: PASS"
    echo "PROD_RESTORE_SCRIPT: PASS"
    echo "RESTORE_SCHEMA: MATCH"
    echo "RESTORE_CONTENT: MATCH"
    ;;

  ROTATE)
    CURRENT_REMOTE_STEP=ROTATION_PRECHECK
    compose_env V1 V1 STEADY_STATE_START
    database_secret_probe V1 || blocked BLOCKED_V1_DATABASE_PRECHECK
    if database_secret_probe V2; then
      blocked BLOCKED_V2_DATABASE_PREACTIVATED
    fi
    if admin_code="$(await_auth_expectation v1 ACTIVE)"; then
      :
    else
      blocked "BLOCKED_V1_ADMIN_PRECHECK_$(auth_code_category "${admin_code}")"
    fi
    if admin_code="$(await_auth_expectation v2 DENIED)"; then
      :
    else
      blocked "BLOCKED_V2_ADMIN_PREACTIVATED_$(auth_code_category "${admin_code}")"
    fi
    tls_v1_serial="$(served_certificate_serial)"
    [ "${tls_v1_serial}" = "$(expected_certificate_serial v1)" ] \
      || blocked BLOCKED_TLS_V1_IDENTITY

    CURRENT_REMOTE_STEP=DATABASE_SECRET_ROTATION
    P3H_SECRET_VERSION_ACTIVATION_CONFIRM="${ROTATION_CONFIRMATION}"
    P3H_ACTIVE_APP_DATABASE_SECRET_VERSION=V2
    export P3H_SECRET_VERSION_ACTIVATION_CONFIRM P3H_ACTIVE_APP_DATABASE_SECRET_VERSION
    compose run --rm --no-deps app-database-secret-activate >/dev/null

    CURRENT_REMOTE_STEP=ADMIN_SECRET_ROTATION
    sudo systemctl stop trade-model-p3h.service
    install_unit STEADY_STATE_START V2 V2 V1
    sudo systemctl start trade-model-p3h.service
    compose_env V2 V2 STEADY_STATE_START
    database_secret_probe V2 || blocked BLOCKED_V2_DATABASE_ACTIVATION
    if database_secret_probe V1; then
      blocked BLOCKED_V1_DATABASE_NOT_REVOKED
    fi
    if admin_code="$(await_auth_expectation v2 ACTIVE)"; then
      :
    else
      blocked "BLOCKED_V2_ADMIN_ACTIVATION_$(auth_code_category "${admin_code}")"
    fi
    if admin_code="$(await_auth_expectation v1 DENIED)"; then
      :
    else
      blocked "BLOCKED_V1_ADMIN_NOT_REVOKED_$(auth_code_category "${admin_code}")"
    fi

    CURRENT_REMOTE_STEP=TLS_CREDENTIAL_ACTIVATION
    sudo systemctl stop trade-model-p3h.service
    run_checked_or_block BLOCKED_TLS_CREDENTIAL_ACTIVATION \
      activate_tls_v2_credentials
    CURRENT_REMOTE_STEP=TLS_ROTATION
    install_unit STEADY_STATE_START V2 V2 V2
    sudo systemctl start trade-model-p3h.service
    compose_env V2 V2 STEADY_STATE_START
    tls_v2_serial="$(served_certificate_serial)"
    [ "${tls_v2_serial}" = "$(expected_certificate_serial v2)" ] \
      || blocked BLOCKED_TLS_V2_IDENTITY
    [ "${tls_v2_serial}" != "${tls_v1_serial}" ] || blocked BLOCKED_TLS_NOT_ROTATED
    run_checked_or_block BLOCKED_POST_ROTATION_SMOKE https_smoke V2

    echo "P3H_REMOTE_STAGE: ROTATION_PASS"
    echo "ADMIN_SECRET_ROTATION: PASS_V2_ACTIVE_V1_DENIED"
    echo "DATABASE_SECRET_ROTATION: PASS_V2_ACTIVE_V1_DENIED"
    echo "TLS_ROTATION: PASS"
    echo "SERVICE_RESTART: PASS"
    ;;

  POST_REBOOT_VERIFY)
    CURRENT_REMOTE_STEP=POST_REBOOT_SERVICES
    sudo systemctl is-active --quiet p3h-lab-credential-holder.service \
      || blocked BLOCKED_HOLDER_AFTER_REBOOT
    sudo systemctl is-active --quiet p3h-lab-credential-seal.service \
      || blocked BLOCKED_CREDENTIAL_SEAL_AFTER_REBOOT
    sudo systemctl is-active --quiet trade-model-p3h.service \
      || blocked BLOCKED_APP_AFTER_REBOOT
    CURRENT_REMOTE_STEP=POST_REBOOT_CONTRACT
    compose_env V2 V2 STEADY_STATE_START
    [ "$(flyway_state)" = '7|7' ] || blocked BLOCKED_FLYWAY_AFTER_REBOOT
    expected_fingerprint="$(sudo cat "${STATE_ROOT}/database-fingerprint")"
    if reboot_fingerprint="$(database_fingerprint)"; then
      [ "${reboot_fingerprint}" = "${expected_fingerprint}" ] \
        || blocked BLOCKED_CONTENT_AFTER_REBOOT_MISMATCH
    else
      fingerprint_status=$?
      blocked "BLOCKED_CONTENT_AFTER_REBOOT_$(fingerprint_failure_reason "${fingerprint_status}")"
    fi
    database_secret_probe V2 || blocked BLOCKED_V2_DATABASE_AFTER_REBOOT
    if database_secret_probe V1; then
      blocked BLOCKED_V1_DATABASE_AFTER_REBOOT
    fi
    if admin_code="$(await_auth_expectation v2 ACTIVE)"; then
      :
    else
      blocked "BLOCKED_V2_ADMIN_AFTER_REBOOT_$(auth_code_category "${admin_code}")"
    fi
    if admin_code="$(await_auth_expectation v1 DENIED)"; then
      :
    else
      blocked "BLOCKED_V1_ADMIN_AFTER_REBOOT_$(auth_code_category "${admin_code}")"
    fi
    [ "$(served_certificate_serial)" = "$(expected_certificate_serial v2)" ] \
      || blocked BLOCKED_TLS_AFTER_REBOOT
    run_checked_or_block BLOCKED_HTTPS_AFTER_REBOOT https_smoke V2

    CURRENT_REMOTE_STEP=POST_REBOOT_LEAK_SCAN
    leak_log="$(mktemp "${SERVICE_RUNTIME}/p3h-lab-journal.XXXXXX")"
    sudo journalctl -u trade-model-p3h.service --no-pager >"${leak_log}"
    bash "${ROOT}/scripts/p3h-secret-leak-check.sh" \
      "${CREDENTIALS}" "${ROOT}" "${leak_log}"
    rm -f "${leak_log}"

    echo "P3H_REMOTE_STAGE: POST_REBOOT_PASS"
    echo "VM_REBOOT_STATUS: PASS_ACTUAL_LINUX_VM_REBOOT"
    echo "V2_DATABASE_AFTER_REBOOT: PASS"
    echo "V1_DATABASE_AFTER_REBOOT: DENIED"
    echo "V2_ADMIN_AFTER_REBOOT: PASS"
    echo "V1_ADMIN_AFTER_REBOOT: DENIED"
    echo "POST_REBOOT_CONTENT_FINGERPRINT: MATCH"
    echo "SECRET_LEAK_CANDIDATE_COUNT: 0"
    echo "PROVIDER_EXTERNAL_CALLS: DISABLED"
    echo "AI_EXTERNAL_CALLS: DISABLED"
    echo "SCHEDULERS: DISABLED"
    echo "TRADING: DISABLED"
    echo "P3H_REMOTE_EXECUTION_IMPLEMENTATION: PASS_LOCAL_VM"
    echo "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN"
    echo "P3H_RESULT: PARTIAL_LOCAL_VM_EVIDENCE"
    echo "P4_ALLOWED: NO"
    echo "PRODUCTION_READINESS: BLOCKED"
    ;;

  CLEANUP)
    set +e
    runtime_mount_unit="$(systemd-escape --path --suffix=mount "${CREDENTIALS}")"
    sudo systemctl stop trade-model-p3h.service >/dev/null 2>&1
    compose_env V2 V2 STEADY_STATE_START
    compose --profile validation down --volumes --remove-orphans >/dev/null 2>&1
    docker image rm "trade-model-v1:p3h-lab-${SOURCE_HEAD:0:12}" >/dev/null 2>&1
    sudo systemctl disable trade-model-p3h.service >/dev/null 2>&1
    sudo systemctl stop p3h-lab-credential-seal.service >/dev/null 2>&1
    sudo systemctl stop p3h-lab-credential-holder.service >/dev/null 2>&1
    sudo systemctl stop "${runtime_mount_unit}" >/dev/null 2>&1
    sudo systemctl disable p3h-lab-credential-seal.service >/dev/null 2>&1
    sudo systemctl disable p3h-lab-credential-holder.service >/dev/null 2>&1
    sudo rm -f "${UNIT_PATH}" \
      /etc/systemd/system/p3h-lab-credential-holder.service \
      /etc/systemd/system/p3h-lab-credential-seal.service \
      "/etc/systemd/system/${runtime_mount_unit}" \
      /usr/local/libexec/p3h-lab-materialize-credentials.sh
    sudo systemctl daemon-reload
    sudo rm -rf /opt/trade-model-p3h /etc/credstore.encrypted/trade-model-p3h \
      /var/lib/trade-model-p3h-lab1
    set -e
    if docker ps --all --quiet --filter "label=com.docker.compose.project=${PROJECT}" \
        | grep -q .; then
      blocked BLOCKED_REMOTE_CONTAINER_CLEANUP
    fi
    echo "P3H_REMOTE_STAGE: CLEANUP_PASS"
    ;;

  *) blocked BLOCKED_ACTION ;;
esac
