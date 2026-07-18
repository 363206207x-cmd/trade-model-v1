#!/usr/bin/env bash
set -euo pipefail

umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXPECTED_BRANCH=codex/p3h-local-vm-staging-lab1
EXPECTED_CONFIRMATION=I_CONFIRM_BUILD_EXACT_HEAD_APPLICATION_ARTIFACT
BUILD_TIMEOUT_SECONDS=1800
BOUNDED_RUNNER="${ROOT_DIR}/scripts/p3h-bounded-process.py"
RUNTIME_DOCKERFILE=deploy/p3h/Dockerfile.runtime.p3h
TEMP_ROOT=""

blocked() {
  printf '%s\n' "P3H_ARTIFACT_BUILD: $1" >&2
  exit 2
}

cleanup() {
  local exit_status=$?
  trap - EXIT
  if [ -n "${TEMP_ROOT}" ] && [ -d "${TEMP_ROOT}" ]; then
    rm -rf "${TEMP_ROOT}"
  fi
  exit "${exit_status}"
}
trap cleanup EXIT

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

file_size_bytes() {
  stat -f '%z' "$1" 2>/dev/null || stat -c '%s' "$1"
}

directory_mode() {
  stat -f '%Lp' "$1" 2>/dev/null || stat -c '%a' "$1"
}

case "${P3H_EXPECTED_HEAD:-}" in
  ''|*[!0-9a-f]*) blocked BLOCKED_EXPECTED_HEAD ;;
esac
[ "${#P3H_EXPECTED_HEAD}" -eq 40 ] || blocked BLOCKED_EXPECTED_HEAD
[ "${P3H_ARTIFACT_BUILD_CONFIRM:-}" = "${EXPECTED_CONFIRMATION}" ] \
  || blocked BLOCKED_CONFIRMATION
[ -n "${P3H_ARTIFACT_OUTPUT_DIR:-}" ] || blocked BLOCKED_OUTPUT_DIRECTORY

current_branch="$(git -C "${ROOT_DIR}" branch --show-current)"
local_head="$(git -C "${ROOT_DIR}" rev-parse HEAD)"
remote_head="$(git -C "${ROOT_DIR}" rev-parse "origin/${EXPECTED_BRANCH}" 2>/dev/null || true)"
[ "${current_branch}" = "${EXPECTED_BRANCH}" ] || blocked BLOCKED_SOURCE_BRANCH
[ "${local_head}" = "${P3H_EXPECTED_HEAD}" ] || blocked BLOCKED_LOCAL_HEAD_MISMATCH
[ "${remote_head}" = "${P3H_EXPECTED_HEAD}" ] || blocked BLOCKED_REMOTE_HEAD_MISMATCH
[ -z "$(git -C "${ROOT_DIR}" status --porcelain --untracked-files=normal)" ] \
  || blocked BLOCKED_DIRTY_WORKTREE

if [ -e "${P3H_ARTIFACT_OUTPUT_DIR}" ]; then
  [ -d "${P3H_ARTIFACT_OUTPUT_DIR}" ] && [ ! -L "${P3H_ARTIFACT_OUTPUT_DIR}" ] \
    || blocked BLOCKED_OUTPUT_DIRECTORY
else
  mkdir -p "${P3H_ARTIFACT_OUTPUT_DIR}"
fi
chmod 700 "${P3H_ARTIFACT_OUTPUT_DIR}"
[ "$(directory_mode "${P3H_ARTIFACT_OUTPUT_DIR}")" = 700 ] \
  || blocked BLOCKED_OUTPUT_DIRECTORY_PERMISSIONS
if find "${P3H_ARTIFACT_OUTPUT_DIR}" -mindepth 1 -maxdepth 1 -print -quit \
    | grep -q .; then
  blocked BLOCKED_OUTPUT_DIRECTORY_NOT_EMPTY
fi

command -v python3 >/dev/null 2>&1 || blocked BLOCKED_PYTHON_MISSING
command -v unzip >/dev/null 2>&1 || blocked BLOCKED_UNZIP_MISSING
[ -f "${BOUNDED_RUNNER}" ] || blocked BLOCKED_BOUNDED_RUNNER_MISSING

java_home="${P3H_JAVA17_HOME:-}"
if [ -z "${java_home}" ] && [ -x /usr/libexec/java_home ]; then
  java_home="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
fi
if [ -z "${java_home}" ] && [ -n "${JAVA_HOME:-}" ]; then
  java_home="${JAVA_HOME}"
fi
[ -x "${java_home}/bin/java" ] || blocked BLOCKED_JAVA17_MISSING
java_version="$(${java_home}/bin/java -version 2>&1 \
  | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
case "${java_version}" in
  17|17.*) ;;
  *) blocked BLOCKED_JAVA17_REQUIRED ;;
esac

TEMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/trade-model-p3h-artifact.XXXXXX")"
chmod 700 "${TEMP_ROOT}"
source_archive="${TEMP_ROOT}/source.tar"
source_root="${TEMP_ROOT}/source"
build_log="${TEMP_ROOT}/maven-build.log"
jar_entries="${TEMP_ROOT}/jar-entries.txt"
jar_payload="${TEMP_ROOT}/jar-payload.bin"
jar_manifest="${TEMP_ROOT}/jar-manifest.txt"
mkdir -p "${source_root}"
chmod 700 "${source_root}"
: >"${build_log}"
chmod 600 "${build_log}"

git -C "${ROOT_DIR}" archive --format=tar \
  --output="${source_archive}" "${P3H_EXPECTED_HEAD}"
tar -xf "${source_archive}" -C "${source_root}"
[ -f "${source_root}/mvnw" ] && [ -f "${source_root}/${RUNTIME_DOCKERFILE}" ] \
  || blocked BLOCKED_ARCHIVED_SOURCE_CONTRACT

build_started="$(date +%s)"
set +e
(
  cd "${source_root}"
  env JAVA_HOME="${java_home}" PATH="${java_home}/bin:${PATH}" \
    python3 "${BOUNDED_RUNNER}" \
      --timeout-seconds "${BUILD_TIMEOUT_SECONDS}" \
      --global-start-epoch "${build_started}" \
      --global-timeout-seconds "${BUILD_TIMEOUT_SECONDS}" \
      --stage APPLICATION_ARTIFACT_BUILD_ON_HOST \
      --operation-class MAVEN_PACKAGE \
      --poll-seconds 15 --heartbeat-seconds 60 --term-grace-seconds 15 \
      -- ./mvnw -B -ntp -DskipTests package
) >"${build_log}" 2>&1
build_status=$?
set -e
case "${build_status}" in
  0) ;;
  124|125) blocked BLOCKED_MAVEN_BUILD_TIMEOUT ;;
  *) blocked BLOCKED_MAVEN_BUILD ;;
esac

jar_candidates=()
while IFS= read -r candidate; do
  jar_candidates+=("${candidate}")
done < <(find "${source_root}/target" -maxdepth 1 -type f -name '*.jar' \
  ! -name 'sources.jar' ! -name '*-sources.jar' \
  ! -name 'javadoc.jar' ! -name '*-javadoc.jar' \
  ! -name 'original-*.jar' -print)
[ "${#jar_candidates[@]}" -eq 1 ] || blocked BLOCKED_EXECUTABLE_JAR_COUNT
app_jar="${jar_candidates[0]}"
[ -f "${app_jar}" ] && [ ! -L "${app_jar}" ] && [ -s "${app_jar}" ] \
  || blocked BLOCKED_EXECUTABLE_JAR
unzip -tq "${app_jar}" >/dev/null 2>&1 || blocked BLOCKED_INVALID_JAR_STRUCTURE
unzip -Z1 "${app_jar}" >"${jar_entries}"
chmod 600 "${jar_entries}"
grep -Fxq 'META-INF/MANIFEST.MF' "${jar_entries}" \
  || blocked BLOCKED_SPRING_BOOT_JAR_CONTRACT
grep -Eq '^BOOT-INF/' "${jar_entries}" || blocked BLOCKED_SPRING_BOOT_JAR_CONTRACT
grep -Eq '^org/springframework/boot/loader/.+Launcher\.class$' "${jar_entries}" \
  || blocked BLOCKED_SPRING_BOOT_JAR_CONTRACT
if grep -Eiq '(^|/)(\.env($|\.)|trade-model\.local-secret$|\.pgpass$|id_(rsa|dsa|ecdsa|ed25519)($|\.)|[^/]*\.(pem|key|p12|pfx|dump|backup)$|[^/]*\.sql\.(gz|xz|bz2)$)' \
    "${jar_entries}"; then
  blocked BLOCKED_FORBIDDEN_JAR_ENTRY
fi
unzip -p "${app_jar}" META-INF/MANIFEST.MF >"${jar_manifest}"
chmod 600 "${jar_manifest}"
grep -Eq '^Main-Class: org\.springframework\.boot\.loader\..*JarLauncher' \
  "${jar_manifest}" || blocked BLOCKED_SPRING_BOOT_JAR_CONTRACT
unzip -p "${app_jar}" >"${jar_payload}"
chmod 600 "${jar_payload}"
if LC_ALL=C grep -aEq -- '-----BEGIN ([A-Z0-9]+ )?PRIVATE KEY-----|(^|[^A-Z0-9_])(OPENAI_API_KEY|GEMINI_API_KEY|XAI_API_KEY|APP_ADMIN_PASSWORD|SPRING_DATASOURCE_PASSWORD)=[A-Za-z0-9]' \
    "${jar_payload}"; then
  blocked BLOCKED_SECRET_MATERIAL_IN_JAR
fi

app_jar_sha="$(sha256_file "${app_jar}")"
app_jar_size="$(file_size_bytes "${app_jar}")"
case "${app_jar_sha}" in ''|*[!0-9a-f]*) blocked BLOCKED_JAR_SHA256 ;; esac
[ "${#app_jar_sha}" -eq 64 ] || blocked BLOCKED_JAR_SHA256
case "${app_jar_size}" in ''|*[!0-9]*) blocked BLOCKED_JAR_SIZE ;; esac
[ "${app_jar_size}" -gt 0 ] || blocked BLOCKED_JAR_SIZE
maven_version="$(sed -n 's#^distributionUrl=.*/apache-maven/\([^/]*\)/.*#\1#p' \
  "${source_root}/.mvn/wrapper/maven-wrapper.properties")"
case "${maven_version}" in ''|*[!0-9A-Za-z._+-]*) blocked BLOCKED_MAVEN_VERSION ;; esac

context="${TEMP_ROOT}/artifact-context"
mkdir -p "${context}"
chmod 700 "${context}"
install -m 0600 "${app_jar}" "${context}/app.jar"
install -m 0600 "${source_root}/${RUNTIME_DOCKERFILE}" \
  "${context}/Dockerfile.runtime.p3h"
metadata="${context}/artifact-metadata.txt"
{
  printf '%s\n' "SOURCE_HEAD=${P3H_EXPECTED_HEAD}"
  printf '%s\n' "APP_JAR_SHA256=${app_jar_sha}"
  printf '%s\n' "APP_JAR_SIZE_BYTES=${app_jar_size}"
  printf '%s\n' "JAVA_VERSION=${java_version}"
  printf '%s\n' "MAVEN_VERSION=${maven_version}"
} >"${metadata}"
chmod 600 "${metadata}"

archive_name="p3h-application-artifact-${P3H_EXPECTED_HEAD}.tar"
artifact_archive="${P3H_ARTIFACT_OUTPUT_DIR}/${archive_name}"
COPYFILE_DISABLE=1 tar -cf "${artifact_archive}" -C "${context}" \
  app.jar Dockerfile.runtime.p3h artifact-metadata.txt
chmod 600 "${artifact_archive}"
expected_listing="${TEMP_ROOT}/expected-listing.txt"
actual_listing="${TEMP_ROOT}/actual-listing.txt"
printf '%s\n' app.jar Dockerfile.runtime.p3h artifact-metadata.txt >"${expected_listing}"
tar -tf "${artifact_archive}" >"${actual_listing}"
cmp -s "${expected_listing}" "${actual_listing}" \
  || blocked BLOCKED_ARTIFACT_ARCHIVE_CONTENTS
archive_sha="$(sha256_file "${artifact_archive}")"
case "${archive_sha}" in ''|*[!0-9a-f]*) blocked BLOCKED_ARCHIVE_SHA256 ;; esac
[ "${#archive_sha}" -eq 64 ] || blocked BLOCKED_ARCHIVE_SHA256

printf '%s\n' 'P3H_ARTIFACT_BUILD: PASS_EXACT_HEAD'
printf '%s\n' "APP_ARTIFACT_SOURCE_HEAD: ${P3H_EXPECTED_HEAD}"
printf '%s\n' "APP_JAR_SHA256: ${app_jar_sha}"
printf '%s\n' "APP_JAR_SIZE_BYTES: ${app_jar_size}"
printf '%s\n' "APP_ARTIFACT_ARCHIVE_SHA256: ${archive_sha}"
