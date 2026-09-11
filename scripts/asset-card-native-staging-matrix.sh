#!/usr/bin/env bash
# TEST_FIXTURE_ONLY interoperability of the candidate standard JAR. No deployment or network.
# Build the clean candidate first: mvn -Passet-card-native-evidence clean verify
# The embedded, build-generated full revision and dirty=false must match --candidate-sha.
set -euo pipefail
export PYTHONDONTWRITEBYTECODE=1

fail() { printf 'STATUS=FAIL\nCODE=%s\nPRODUCTION_MODEL_READY=NO\n' "$1"; exit "${2:-2}"; }
not_executed() { printf 'STATUS=NOT_EXECUTED\nCODE=%s\nNATIVE_STATUS=NOT_EXECUTED\nPRODUCTION_MODEL_READY=NO\n' "$1"; exit 78; }
app_jar='' candidate_sha='' python_bin='' linux_image=''
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || fail INVALID_ARGUMENTS
  case "$1" in
    --app-jar) [[ -z "$app_jar" ]] || fail DUPLICATE_ARGUMENT; app_jar="$2" ;;
    --candidate-sha) [[ -z "$candidate_sha" ]] || fail DUPLICATE_ARGUMENT; candidate_sha="$2" ;;
    --python) [[ -z "$python_bin" ]] || fail DUPLICATE_ARGUMENT; python_bin="$2" ;;
    --linux-image) [[ -z "$linux_image" ]] || fail DUPLICATE_ARGUMENT; linux_image="$2" ;;
    *) fail INVALID_ARGUMENTS ;;
  esac
  shift 2
done
[[ "$candidate_sha" =~ ^[0-9a-f]{40}$ && -f "$app_jar" && -x "$python_bin" ]] || fail INVALID_ARGUMENTS
[[ -z "$linux_image" || "$linux_image" =~ ^sha256:[0-9a-f]{64}$ ]] || fail IMMUTABLE_CACHED_IMAGE_REQUIRED
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
repo_dir="$(cd -- "$script_dir/.." && pwd -P)"
[[ "$(git -C "$repo_dir" rev-parse HEAD)" == "$candidate_sha" ]] || fail CANDIDATE_HEAD_MISMATCH
[[ -z "$(git -C "$repo_dir" status --porcelain -- src pom.xml scripts/test_asset_card_model.py scripts/asset_card_model.py scripts/asset-card-native-staging-matrix.sh)" ]] || fail UNCOMMITTED_CANDIDATE_INPUTS
app_jar="$(cd -- "$(dirname -- "$app_jar")" && pwd -P)/$(basename -- "$app_jar")"
python_bin="$(cd -- "$(dirname -- "$python_bin")" && pwd -P)/$(basename -- "$python_bin")"
hash_file() { if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'; else shasum -a 256 "$1" | awk '{print $1}'; fi; }
jar_sha="$(hash_file "$app_jar")"
command -v unzip >/dev/null 2>&1 || not_executed JAR_PROVENANCE_READER_UNAVAILABLE
jar_identity="$(unzip -p "$app_jar" BOOT-INF/classes/git.properties 2>/dev/null)" || fail MISSING_BUILD_PROVENANCE
[[ "$(printf '%s\n' "$jar_identity" | awk -F= '$1=="git.commit.id.full" {print $2}')" == "$candidate_sha" \
  && "$(printf '%s\n' "$jar_identity" | awk -F= '$1=="git.dirty" {print $2}')" == false ]] || fail CANDIDATE_BUILD_MISMATCH

if [[ -n "$linux_image" ]]; then
  command -v docker >/dev/null 2>&1 || not_executed NO_LOCAL_LINUX_EXECUTOR
  platform="$(docker image inspect --platform linux/amd64 --format '{{.Os}}/{{.Architecture}}' "$linux_image" 2>/dev/null)" || not_executed IMAGE_NOT_CACHED
  [[ "$platform" == linux/amd64 ]] || not_executed LINUX_AMD64_IMAGE_REQUIRED
else
  [[ "$(uname -s)" == Linux && "$(uname -m)" == x86_64 ]] || not_executed LINUX_X86_64_REQUIRED
  command -v java >/dev/null 2>&1 || not_executed JAVA17_REQUIRED
fi

card_tmp="$(mktemp -d "${TMPDIR:-/tmp}/asset-card-native-matrix.XXXXXXXX")"
cleanup() {
  # Only this invocation's freshly-created, private test directory is recoverably disposable.
  if [[ -n "${card_tmp:-}" && -d "$card_tmp" && ! -L "$card_tmp" && "$(basename -- "$card_tmp")" == asset-card-native-matrix.* ]]; then
    chmod -R u+rwX "$card_tmp"
    rm -rf -- "$card_tmp"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
"$python_bin" "$script_dir/test_asset_card_model.py" --generate-native-fixture "$card_tmp/fixture" "$candidate_sha" "$jar_sha" || fail FIXTURE_GENERATION_FAILED 3
manifest_sha="$(hash_file "$card_tmp/fixture/native-fixture.json")"
chmod 444 "$card_tmp/fixture/long.ubj" "$card_tmp/fixture/short.ubj" "$card_tmp/fixture/native-fixture.json"
chmod 555 "$card_tmp/fixture"
mkdir "$card_tmp/native"
printf 'DATA_KIND=TEST_FIXTURE_ONLY\nCANDIDATE_SHA=%s\nJAR_SHA256=%s\nMANIFEST_SHA256=%s\nMODEL_MODE=SHADOW\nPRODUCTION_MODEL_READY=NO\n' "$candidate_sha" "$jar_sha" "$manifest_sha"
set +e
if [[ -n "$linux_image" ]]; then
  printf 'EXECUTION_ENVIRONMENT=DISPOSABLE_LINUX_AMD64\nLINUX_IMAGE=%s\n' "$linux_image"
  gomp_version="$(docker run --rm --pull=never --platform linux/amd64 --network none --read-only --user 65534:65534 \
    --cap-drop ALL --security-opt no-new-privileges --entrypoint dpkg-query "$linux_image" -W '-f=${Version}' libgomp1 2>/dev/null)"
  [[ "$gomp_version" =~ ^[0-9A-Za-z.+:~_-]+$ ]] || gomp_version=UNKNOWN
  printf 'LINUX_LIBGOMP_PACKAGE_VERSION=%s\n' "$gomp_version"
  docker run --rm --pull=never --platform linux/amd64 --network none --read-only --user 65534:65534 \
    --cap-drop ALL --security-opt no-new-privileges --pids-limit 128 \
    --tmpfs /tmp:rw,nosuid,exec,size=256m,mode=1777 \
    --mount "type=bind,src=$app_jar,dst=/candidate/app.jar,readonly" \
    --mount "type=bind,src=$card_tmp/fixture,dst=/fixture,readonly" \
    --entrypoint java "$linux_image" \
    -Djava.io.tmpdir=/tmp -Dloader.main=org.example.trademodel.assetcard.AssetCardNativeRuntimeProbe \
    -cp /candidate/app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
    --predict-fixture --app-jar /candidate/app.jar --app-jar-sha256 "$jar_sha" \
    --bundle-dir /fixture --manifest-sha256 "$manifest_sha" --candidate-sha "$candidate_sha"
  native_exit=$?
else
  printf 'EXECUTION_ENVIRONMENT=LOCAL_LINUX_X86_64\n'
  gomp_version="$(dpkg-query -W '-f=${Version}' libgomp1 2>/dev/null)"
  [[ "$gomp_version" =~ ^[0-9A-Za-z.+:~_-]+$ ]] || gomp_version=UNKNOWN
  printf 'LINUX_LIBGOMP_PACKAGE_VERSION=%s\n' "$gomp_version"
  (
    cd -- "$card_tmp"
    java -Djava.io.tmpdir="$card_tmp/native" -Dloader.main=org.example.trademodel.assetcard.AssetCardNativeRuntimeProbe \
      -cp "$app_jar" org.springframework.boot.loader.launch.PropertiesLauncher \
      --predict-fixture --app-jar "$app_jar" --app-jar-sha256 "$jar_sha" \
      --bundle-dir "$card_tmp/fixture" --manifest-sha256 "$manifest_sha" --candidate-sha "$candidate_sha"
  )
  native_exit=$?
fi
set -e
printf 'NATIVE_PROCESS_EXIT=%s\nSTAGING_ACCEPTANCE=NOT_EXECUTED\nPRODUCTION_MODEL_READY=NO\n' "$native_exit"
exit "$native_exit"
