#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-}"
SOURCE_HEAD="${2:-}"
R1_START_EPOCH="${3:-$(date +%s)}"
R1_GLOBAL_TIMEOUT_SECONDS="${4:-10800}"
ROOT=/opt/trade-model-p3h/current
STATE_ROOT=/var/lib/trade-model-p3h-lab1
CREDENTIALS=/run/credentials/p3hlab1
SERVICE_RUNTIME=/run/trade-model-p3h
HTTPS_SMOKE_STEP_FILE="${SERVICE_RUNTIME}/p3h-lab-https-smoke-step"
UNIT_TEMPLATE="${ROOT}/deploy/p3h/lima/trade-model-p3h-lab.service.template"
UNIT_PATH=/etc/systemd/system/trade-model-p3h.service
PROJECT=trade-model-p3h-lab1
HOSTNAME=trade-staging.lab.test
POSTGRES_IMAGE='postgres:16-alpine@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc'
FLYWAY_IMAGE='flyway/flyway:12.11.0-alpine@sha256:6bf3a713f52c4d803a88501f8409dda2191e9ccba1454358a6de2c4cc65f71b0'
NGINX_IMAGE='nginx:1.27.4-alpine@sha256:4ff102c5d78d254a6f0da062b3cf39eaf07f01eec0927fd21e219d0af8bc0591'
GREENFIELD_CONFIRMATION=I_CONFIRM_EMPTY_GREENFIELD_INITIALIZATION
ROTATION_CONFIRMATION=I_CONFIRM_CONTROLLED_APP_DATABASE_SECRET_ROTATION
IMAGE_BUILD_ATTEMPT_TIMEOUT_SECONDS=2700
IMAGE_BUILD_MAX_ATTEMPTS=1
IMAGE_BUILD_FAILURE_CATEGORY=UNKNOWN
RUNTIME_IMAGE_PULL_ATTEMPT_TIMEOUT_SECONDS=1200
RUNTIME_IMAGE_PULL_ALL_TIMEOUT_SECONDS=2400
RUNTIME_IMAGE_PULL_MAX_ATTEMPTS=1
RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=UNKNOWN
NO_PROGRESS_TIMEOUT_SECONDS=900
PROGRESS_PROBE_TIMEOUT_SECONDS=5
POLL_INTERVAL_SECONDS=15
HEARTBEAT_INTERVAL_SECONDS=60
TERM_GRACE_SECONDS=15
BOUNDED_DOCKER_FAILURE=UNKNOWN
ACTIVE_DOCKER_PROCESS_GROUP=""
CURRENT_REMOTE_STEP=PRECONDITION
PROGRESS_FINGERPRINT=""
PROGRESS_PROBE_STATUS=NOT_RUN
PROGRESS_PROBE_OUTPUT=""

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

emit_heartbeat() {
  local stage="$1"
  local stage_elapsed="$2"
  local process_state="$3"
  local operation_class="$4"
  echo "P3H_LAB_STAGE: ${stage}"
  echo "STAGE_ELAPSED_SECONDS: ${stage_elapsed}"
  echo "GLOBAL_ELAPSED_SECONDS: $(( $(date +%s) - R1_START_EPOCH ))"
  echo "PROCESS_STATE: ${process_state}"
  echo "DOCKER_OPERATION_CLASS: ${operation_class}"
}

terminate_process_group() {
  local process_group_leader="$1"
  kill -TERM -- "-${process_group_leader}" >/dev/null 2>&1 || true
  for _attempt in $(seq 1 "${TERM_GRACE_SECONDS}"); do
    kill -0 -- "-${process_group_leader}" >/dev/null 2>&1 || break
    sleep 1
  done
  if kill -0 -- "-${process_group_leader}" >/dev/null 2>&1; then
    kill -KILL -- "-${process_group_leader}" >/dev/null 2>&1 || true
  fi
  wait "${process_group_leader}" >/dev/null 2>&1 || true
}

cleanup_active_docker_group() {
  local exit_status=$?
  trap - EXIT
  if [ -n "${ACTIVE_DOCKER_PROCESS_GROUP}" ]; then
    terminate_process_group "${ACTIVE_DOCKER_PROCESS_GROUP}"
  fi
  exit "${exit_status}"
}
trap cleanup_active_docker_group EXIT

run_progress_probe() {
  local timeout_category="$1"
  local command_category="$2"
  local probe_status
  shift 2
  if PROGRESS_PROBE_OUTPUT="$(timeout --signal=TERM --kill-after=2s \
      "${PROGRESS_PROBE_TIMEOUT_SECONDS}s" "$@" 2>/dev/null)"; then
    probe_status=0
    PROGRESS_PROBE_STATUS=PASS
    return 0
  else
    probe_status=$?
  fi
  case "${probe_status}" in
    124|137) PROGRESS_PROBE_STATUS="${timeout_category}" ;;
    *) PROGRESS_PROBE_STATUS="${command_category}" ;;
  esac
  PROGRESS_PROBE_OUTPUT=""
  return 1
}

capture_docker_progress() {
  local target_image="$1"
  local image_id docker_usage buildkit_kb content_kb

  if run_progress_probe PROBE_TIMEOUT DOCKER_IMAGE_NOT_AVAILABLE \
      docker image inspect "${target_image}" --format '{{.Id}}'; then
    image_id="${PROGRESS_PROBE_OUTPUT:-NONE}"
  elif [ "${PROGRESS_PROBE_STATUS}" = DOCKER_IMAGE_NOT_AVAILABLE ]; then
    # A missing target image is expected while a build is in progress. Verify
    # daemon health separately before treating this sample as valid.
    if ! run_progress_probe PROBE_TIMEOUT DOCKER_DAEMON_UNAVAILABLE docker info; then
      return 1
    fi
    image_id=NONE
  else
    return 1
  fi

  if ! run_progress_probe PROBE_TIMEOUT DOCKER_DAEMON_UNAVAILABLE \
      docker system df --format '{{.Type}}|{{.TotalCount}}|{{.Size}}'; then
    return 1
  fi
  docker_usage="$(printf '%s' "${PROGRESS_PROBE_OUTPUT}" | sha256sum | awk '{print $1}')"

  run_progress_probe FILESYSTEM_PROBE_TIMEOUT FILESYSTEM_PROBE_TIMEOUT \
    sudo du -sk /var/lib/docker/buildkit || return 1
  buildkit_kb="$(printf '%s\n' "${PROGRESS_PROBE_OUTPUT}" | awk 'NR == 1 {print $1}')"
  run_progress_probe FILESYSTEM_PROBE_TIMEOUT FILESYSTEM_PROBE_TIMEOUT \
    sudo du -sk /var/lib/docker/containerd || return 1
  content_kb="$(printf '%s\n' "${PROGRESS_PROBE_OUTPUT}" | awk 'NR == 1 {print $1}')"

  PROGRESS_FINGERPRINT="$(printf '%s|%s|%s|%s\n' \
    "${image_id:-NONE}" "${docker_usage:-NONE}" \
    "${buildkit_kb:-0}" "${content_kb:-0}" \
    | sha256sum | awk '{print $1}')"
  PROGRESS_PROBE_STATUS=PASS
  PROGRESS_PROBE_OUTPUT=""
  return 0
}

run_docker_bounded() {
  local stage="$1"
  local operation_class="$2"
  local timeout_seconds="$3"
  local target_image="$4"
  local output_file="$5"
  local operation_pid started now elapsed last_progress next_heartbeat
  local previous_fingerprint current_fingerprint process_state operation_status
  local last_probe_status=NOT_RUN
  shift 5

  BOUNDED_DOCKER_FAILURE=UNKNOWN
  started="$(date +%s)"
  last_progress="${started}"
  next_heartbeat="${HEARTBEAT_INTERVAL_SECONDS}"
  previous_fingerprint=""
  if capture_docker_progress "${target_image}"; then
    previous_fingerprint="${PROGRESS_FINGERPRINT}"
  else
    last_probe_status="${PROGRESS_PROBE_STATUS}"
    echo "P3H_PROGRESS_PROBE_STATUS: ${PROGRESS_PROBE_STATUS}"
  fi
  setsid "$@" >"${output_file}" 2>&1 &
  operation_pid="$!"
  ACTIVE_DOCKER_PROCESS_GROUP="${operation_pid}"

  while kill -0 "${operation_pid}" >/dev/null 2>&1; do
    sleep "${POLL_INTERVAL_SECONDS}"
    if ! kill -0 "${operation_pid}" >/dev/null 2>&1; then
      break
    fi
    now="$(date +%s)"
    elapsed=$((now - started))
    if [ $((now - R1_START_EPOCH)) -ge "${R1_GLOBAL_TIMEOUT_SECONDS}" ]; then
      BOUNDED_DOCKER_FAILURE=GLOBAL_TIMEOUT
      terminate_process_group "${operation_pid}"
      ACTIVE_DOCKER_PROCESS_GROUP=""
      return 125
    fi
    process_state=RUNNING_NO_CHANGE
    if capture_docker_progress "${target_image}"; then
      current_fingerprint="${PROGRESS_FINGERPRINT}"
      if [ -n "${previous_fingerprint}" ] \
          && [ "${current_fingerprint}" != "${previous_fingerprint}" ]; then
        previous_fingerprint="${current_fingerprint}"
        last_progress="${now}"
        process_state=RUNNING_PROGRESS
      elif [ -z "${previous_fingerprint}" ]; then
        # Establishing a baseline after failed probes is not real progress.
        previous_fingerprint="${current_fingerprint}"
      fi
      last_probe_status=PASS
    else
      process_state="RUNNING_${PROGRESS_PROBE_STATUS}"
      if [ "${PROGRESS_PROBE_STATUS}" != "${last_probe_status}" ]; then
        echo "P3H_PROGRESS_PROBE_STATUS: ${PROGRESS_PROBE_STATUS}"
      fi
      last_probe_status="${PROGRESS_PROBE_STATUS}"
    fi
    if [ "${elapsed}" -ge "${next_heartbeat}" ]; then
      emit_heartbeat "${stage}" "${elapsed}" "${process_state}" "${operation_class}"
      next_heartbeat=$((next_heartbeat + HEARTBEAT_INTERVAL_SECONDS))
    fi
    if [ $((now - last_progress)) -ge "${NO_PROGRESS_TIMEOUT_SECONDS}" ]; then
      BOUNDED_DOCKER_FAILURE=NO_PROGRESS_TIMEOUT
      terminate_process_group "${operation_pid}"
      ACTIVE_DOCKER_PROCESS_GROUP=""
      return 126
    fi
    if [ "${elapsed}" -ge "${timeout_seconds}" ]; then
      BOUNDED_DOCKER_FAILURE=STAGE_TIMEOUT
      terminate_process_group "${operation_pid}"
      ACTIVE_DOCKER_PROCESS_GROUP=""
      return 124
    fi
  done

  set +e
  wait "${operation_pid}"
  operation_status=$?
  set -e
  ACTIVE_DOCKER_PROCESS_GROUP=""
  return "${operation_status}"
}

bounded_build_preflight() {
  local memory_mb root_disk_gb docker_disk_gb registry_status maven_status
  memory_mb="$(awk '/MemAvailable:/ {print int($2 / 1024); exit}' /proc/meminfo)"
  root_disk_gb="$(df -Pk / | awk 'NR == 2 {print int($4 / 1024 / 1024)}')"
  docker_disk_gb="$(df -Pk /var/lib/docker | awk 'NR == 2 {print int($4 / 1024 / 1024)}')"
  echo "VM_AVAILABLE_MEMORY_MB: ${memory_mb}"
  echo "VM_AVAILABLE_DISK_GB: ${root_disk_gb}"
  [ "${memory_mb}" -ge 4096 ] || blocked BLOCKED_VM_AVAILABLE_MEMORY
  [ "${root_disk_gb}" -ge 15 ] && [ "${docker_disk_gb}" -ge 15 ] \
    || blocked BLOCKED_VM_AVAILABLE_DISK
  systemctl is-active --quiet docker.service || blocked BLOCKED_DOCKER_DAEMON
  command -v setsid >/dev/null 2>&1 || blocked BLOCKED_PROCESS_GROUP_SUPPORT
  timeout --signal=TERM --kill-after=2s 10s \
    getent ahosts registry-1.docker.io >/dev/null 2>&1 \
    || blocked BLOCKED_DNS_RESOLUTION
  timeout --signal=TERM --kill-after=2s 10s \
    getent ahosts repo.maven.apache.org >/dev/null 2>&1 \
    || blocked BLOCKED_DNS_RESOLUTION
  registry_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --connect-timeout 5 --max-time 10 https://registry-1.docker.io/v2/ \
    2>/dev/null || true)"
  case "${registry_status}" in 200|401) ;; *) blocked BLOCKED_REGISTRY_CONNECTIVITY ;; esac
  maven_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --connect-timeout 5 --max-time 10 https://repo.maven.apache.org/maven2/ \
    2>/dev/null || true)"
  case "${maven_status}" in 200|301|302) ;; *) blocked BLOCKED_MAVEN_CONNECTIVITY ;; esac
  echo "DOCKER_DAEMON: ACTIVE"
  echo "DNS_RESOLUTION: PASS"
  echo "REQUIRED_REGISTRY_CONNECTIVITY: PASS_BOUNDED"
  echo "MAVEN_REPOSITORY_CONNECTIVITY: PASS_BOUNDED"
}

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
  local recorded_step=""
  if [ -f "${HTTPS_SMOKE_STEP_FILE}" ] \
      && [ ! -L "${HTTPS_SMOKE_STEP_FILE}" ]; then
    IFS= read -r recorded_step <"${HTTPS_SMOKE_STEP_FILE}" || recorded_step=""
  fi
  rm -f "${HTTPS_SMOKE_STEP_FILE}" >/dev/null 2>&1 || true
  case "${recorded_step}" in
    RUNTIME_DIRECTORY|AUTH_CONFIG|HEALTH|LIVENESS|READINESS|DASHBOARD_FETCH|REVIEW_FETCH|PROD_SMOKE_CONTRACT|DASHBOARD_SAFETY|UNAUTHENTICATED_API|HTTP_REDIRECT|UNKNOWN_HOST_REJECTION|TLS_1_2|TLS_1_3|RATE_LIMIT|TEMP_CLEANUP) ;;
    *) recorded_step="" ;;
  esac
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
        *)
          if [ -n "${recorded_step}" ]; then
            detail="${recorded_step}_EXIT_${check_status}"
          else
            detail="UNKNOWN_EXIT_${check_status}"
          fi
          ;;
      esac
      ;;
  esac
  if [ -n "${detail}" ]; then
    echo "${base_reason}_${detail}"
  else
    echo "${base_reason}"
  fi
}

mark_https_smoke_step() {
  local step="$1"
  case "${step}" in
    RUNTIME_DIRECTORY|AUTH_CONFIG|HEALTH|LIVENESS|READINESS|DASHBOARD_FETCH|REVIEW_FETCH|PROD_SMOKE_CONTRACT|DASHBOARD_SAFETY|UNAUTHENTICATED_API|HTTP_REDIRECT|UNKNOWN_HOST_REJECTION|TLS_1_2|TLS_1_3|RATE_LIMIT|TEMP_CLEANUP) ;;
    *) return 1 ;;
  esac
  [ ! -L "${HTTPS_SMOKE_STEP_FILE}" ] || return 1
  printf '%s\n' "${step}" >"${HTTPS_SMOKE_STEP_FILE}" || return 1
  chmod 600 "${HTTPS_SMOKE_STEP_FILE}" || return 1
}

case "${SOURCE_HEAD}" in
  ''|*[!0-9a-f]*) blocked BLOCKED_SOURCE_HEAD ;;
esac
[ "${#SOURCE_HEAD}" -eq 40 ] || blocked BLOCKED_SOURCE_HEAD
case "${R1_GLOBAL_TIMEOUT_SECONDS}" in
  ''|*[!0-9]*) blocked BLOCKED_GLOBAL_TIMEOUT_CONTRACT ;;
esac
[ "${R1_GLOBAL_TIMEOUT_SECONDS}" -gt 0 ] \
  && [ "${R1_GLOBAL_TIMEOUT_SECONDS}" -le 10800 ] \
  || blocked BLOCKED_GLOBAL_TIMEOUT_CONTRACT
[ $(( $(date +%s) - R1_START_EPOCH )) -lt "${R1_GLOBAL_TIMEOUT_SECONDS}" ] \
  || blocked BLOCKED_GLOBAL_TIMEOUT_EXCEEDED
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
  if run_docker_bounded APPLICATION_IMAGE_BUILD APPLICATION_IMAGE_BUILD \
      "${IMAGE_BUILD_ATTEMPT_TIMEOUT_SECONDS}" "${image}" "${build_log}" \
      docker build --pull=false --file "${ROOT}/deploy/p3h/Dockerfile.p3h" \
        --build-arg "VCS_REF=${SOURCE_HEAD}" --tag "${image}" "${ROOT}"; then
    IMAGE_BUILD_FAILURE_CATEGORY=NONE
    rm -f "${build_log}"
    return 0
  else
    build_status=$?
  fi

  case "${build_status}" in
    124|137) IMAGE_BUILD_FAILURE_CATEGORY=TIMEOUT ;;
    125) IMAGE_BUILD_FAILURE_CATEGORY=GLOBAL_TIMEOUT ;;
    126) IMAGE_BUILD_FAILURE_CATEGORY=NO_PROGRESS_TIMEOUT ;;
    *)
      if grep -Eqi '429|too many requests|toomanyrequests|rate.?limit' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=RATE_LIMIT
      elif grep -Eqi 'tls handshake timeout|i/o timeout|timed out|connection reset|connection reset by peer|connection timed out|read timed out|unexpected eof|premature eof|unexpected end of stream|remote host terminated|temporary failure|failed to do request|dial tcp|network is unreachable|connection refused|no such host|unable to resolve|failed to fetch anonymous token|failed to fetch oauth token|could not transfer artifact|transfer failed|status code: 5[0-9][0-9]|502 bad gateway|503 service unavailable|504 gateway timeout|context deadline exceeded' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=NETWORK
      elif grep -Eqi 'no space left on device|disk quota exceeded' "${build_log}"; then
        IMAGE_BUILD_FAILURE_CATEGORY=STORAGE
      elif grep -Eqi 'build failure|compilation failure|compilation error|cannot find symbol|failed to execute goal|there are test failures|non-resolvable parent pom|could not resolve dependencies' "${build_log}"; then
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
  if run_docker_bounded RUNTIME_IMAGE_PULL RUNTIME_IMAGE_PULL \
      "${RUNTIME_IMAGE_PULL_ATTEMPT_TIMEOUT_SECONDS}" "${image}" "${pull_log}" \
      docker pull "${image}"; then
    RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=NONE
    rm -f "${pull_log}"
    return 0
  else
    pull_status=$?
  fi

  case "${pull_status}" in
    124|137) RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=TIMEOUT ;;
    125) RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=GLOBAL_TIMEOUT ;;
    126) RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=NO_PROGRESS_TIMEOUT ;;
    *)
      if grep -Eqi '429|too many requests|toomanyrequests|rate.?limit' "${pull_log}"; then
        RUNTIME_IMAGE_PULL_FAILURE_CATEGORY=RATE_LIMIT
      elif grep -Eqi 'tls handshake timeout|i/o timeout|timed out|connection reset|connection reset by peer|connection timed out|read timed out|unexpected eof|premature eof|unexpected end of stream|remote host terminated|temporary failure|failed to do request|dial tcp|network is unreachable|connection refused|no such host|unable to resolve|failed to fetch anonymous token|failed to fetch oauth token|status code: 5[0-9][0-9]|502 bad gateway|503 service unavailable|504 gateway timeout|context deadline exceeded' "${pull_log}"; then
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
  local image pull_started pull_elapsed
  pull_started="$(date +%s)"
  for image in "${POSTGRES_IMAGE}" "${FLYWAY_IMAGE}" "${NGINX_IMAGE}"; do
    pull_elapsed=$(( $(date +%s) - pull_started ))
    [ "${pull_elapsed}" -lt "${RUNTIME_IMAGE_PULL_ALL_TIMEOUT_SECONDS}" ] \
      || blocked BLOCKED_RUNTIME_IMAGE_PULL_ALL_TIMEOUT
    if ! pull_runtime_image "${image}"; then
      blocked "BLOCKED_RUNTIME_IMAGE_PULL_${RUNTIME_IMAGE_PULL_FAILURE_CATEGORY}"
    fi
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
  mark_https_smoke_step RUNTIME_DIRECTORY || return 61
  response_dir="$(mktemp -d "${SERVICE_RUNTIME}/p3h-lab-smoke.XXXXXX")" \
    || return 61
  trap 'rm -rf "${response_dir}"' EXIT
  config_file="${response_dir}/curl-auth.conf"
  chmod 700 "${response_dir}" || return 61
  mark_https_smoke_step AUTH_CONFIG || return 62
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

  mark_https_smoke_step HEALTH || return 63
  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health" >"${response_dir}/health.json" \
    || return 63
  mark_https_smoke_step LIVENESS || return 64
  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health/liveness" >"${response_dir}/liveness.json" \
    || return 64
  mark_https_smoke_step READINESS || return 65
  curl --silent --show-error --max-time 20 \
    --cacert "${CREDENTIALS}/tls_ca_certificate" \
    --resolve "${HOSTNAME}:443:127.0.0.1" \
    "https://${HOSTNAME}/actuator/health/readiness" >"${response_dir}/readiness.json" \
    || return 65
  mark_https_smoke_step DASHBOARD_FETCH || return 66
  curl --config "${config_file}" \
    "https://${HOSTNAME}/api/dashboard/home" >"${response_dir}/dashboard.json" \
    || return 66
  mark_https_smoke_step REVIEW_FETCH || return 67
  curl --config "${config_file}" \
    "https://${HOSTNAME}/api/review/center" >"${response_dir}/review.json" \
    || return 67

  mark_https_smoke_step PROD_SMOKE_CONTRACT || return 68
  SMOKE_PHASE=VALIDATE \
  SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE \
  SMOKE_RESPONSE_DIR="${response_dir}" \
  SMOKE_ALLOW_EXTERNAL_CALLS=false \
    bash "${ROOT}/scripts/prod-smoke.sh" >/dev/null || return 68

  mark_https_smoke_step DASHBOARD_SAFETY || return 69
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

  mark_https_smoke_step UNAUTHENTICATED_API || return 70
  if ! unauthenticated_code="$(curl --silent --show-error --max-time 20 \
      --cacert "${CREDENTIALS}/tls_ca_certificate" \
      --resolve "${HOSTNAME}:443:127.0.0.1" --output /dev/null \
      --write-out '%{http_code}' "https://${HOSTNAME}/api/dashboard/home")"; then
    return 70
  fi
  case "${unauthenticated_code}" in 401|403) ;; *) return 70 ;; esac

  mark_https_smoke_step HTTP_REDIRECT || return 71
  redirect_headers="${response_dir}/redirect.headers"
  curl --silent --show-error --max-time 20 --output /dev/null \
    --dump-header "${redirect_headers}" -H "Host: ${HOSTNAME}" \
    http://127.0.0.1/actuator/health || return 71
  grep -Eiq "^Location: https://${HOSTNAME}/actuator/health\r?$" \
    "${redirect_headers}" || return 71

  mark_https_smoke_step UNKNOWN_HOST_REJECTION || return 72
  unknown_code="$(curl --silent --max-time 10 --output /dev/null \
    --write-out '%{http_code}' -H 'Host: unapproved.invalid' \
    http://127.0.0.1/ || true)"
  [ "${unknown_code}" != 200 ] && [ "${unknown_code}" != 308 ] \
    || return 72

  mark_https_smoke_step TLS_1_2 || return 73
  openssl s_client -connect 127.0.0.1:443 -servername "${HOSTNAME}" \
    -CAfile "${CREDENTIALS}/tls_ca_certificate" -verify_hostname "${HOSTNAME}" \
    -verify_return_error -tls1_2 </dev/null >/dev/null 2>&1 || return 73
  mark_https_smoke_step TLS_1_3 || return 74
  if openssl s_client -help 2>&1 | grep -q -- -tls1_3; then
    openssl s_client -connect 127.0.0.1:443 -servername "${HOSTNAME}" \
      -CAfile "${CREDENTIALS}/tls_ca_certificate" -verify_hostname "${HOSTNAME}" \
      -verify_return_error -tls1_3 </dev/null >/dev/null 2>&1 || return 74
  fi

  mark_https_smoke_step RATE_LIMIT || return 75
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
  mark_https_smoke_step TEMP_CLEANUP || return 76
  rm -rf "${response_dir}" || return 76
  rm -f "${HTTPS_SMOKE_STEP_FILE}" || return 76
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
  BUILD_APPLICATION_IMAGE)
    CURRENT_REMOTE_STEP=BOUNDED_BUILD_PREFLIGHT
    bounded_build_preflight
    CURRENT_REMOTE_STEP=PREBUILD_COMPOSE_CONFIG
    validate_prebuild_compose_config
    CURRENT_REMOTE_STEP=PREBUILD_SYSTEMD_COMPOSE_CONFIG
    validate_systemd_sandbox_compose_config
    CURRENT_REMOTE_STEP=IMAGE_BUILD
    image="trade-model-v1:p3h-lab-${SOURCE_HEAD:0:12}"
    if ! build_application_image "${image}"; then
      blocked "BLOCKED_IMAGE_BUILD_${IMAGE_BUILD_FAILURE_CATEGORY}"
    fi
    echo "P3H_IMAGE_BUILD_ATTEMPTS: 1"
    echo "P3H_IMAGE_BUILD_RETRY_COUNT: 0"
    revision="$(docker image inspect "${image}" \
      --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}')"
    [ "${revision}" = "${SOURCE_HEAD}" ] || blocked BLOCKED_IMAGE_REVISION
    [ "$(docker image inspect "${image}" --format '{{.Config.User}}')" = app ] \
      || blocked BLOCKED_IMAGE_USER
    echo "P3H_REMOTE_STAGE: APPLICATION_IMAGE_BUILD_PASS"
    echo "APP_IMAGE_REVISION: ${SOURCE_HEAD}"
    ;;

  PULL_RUNTIME_IMAGES)
    CURRENT_REMOTE_STEP=RUNTIME_IMAGE_PREFETCH
    ensure_runtime_images
    echo "P3H_REMOTE_STAGE: RUNTIME_IMAGE_PULL_PASS"
    ;;

  INITIAL_DEPLOY)
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

    echo "P3H_REMOTE_STAGE: INITIAL_DEPLOY_PASS"
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
    ;;

  BACKUP_RESTORE)
    CURRENT_REMOTE_STEP=BACKUP_RESTORE
    run_checked_or_block BLOCKED_BACKUP_RESTORE backup_restore
    echo "P3H_REMOTE_STAGE: BACKUP_RESTORE_PASS"
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
