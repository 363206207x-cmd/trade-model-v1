#!/usr/bin/env bash
# Shared local file checks and read-only preflight. This file is also a library for the three bounded installers.
set +x
set -euo pipefail

native_fail() { printf 'STATUS=FAIL\nREASON_CODE=%s\n' "$1"; exit 2; }
native_sha() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum -- "$1" | awk '{print $1}';
  else shasum -a 256 -- "$1" | awk '{print $1}'; fi
}
native_uid() { stat -c '%u' "$1" 2>/dev/null || stat -f '%u' "$1"; }
native_gid() { stat -c '%g' "$1" 2>/dev/null || stat -f '%g' "$1"; }
native_mode() { stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1"; }
native_size() { stat -c '%s' "$1" 2>/dev/null || stat -f '%z' "$1"; }
native_no_links() {
  local path="$1" part current= rest
  [[ "$path" = /* && "$path" != *'/../'* && "$path" != */.. && "$path" != *'/./'* && "$path" != *'//'* ]] || native_fail PATH_INVALID
  rest=${path#/}
  while [[ -n "$rest" ]]; do
    part=${rest%%/*}; current="$current/$part"
    [[ ! -L "$current" ]] || native_fail SYMLINK_FORBIDDEN
    if [[ "$rest" == */* ]]; then rest=${rest#*/}; else rest=; fi
  done
}
native_meta() {
  local path="$1" owner="$2" mask="${3:-022}" mode parent
  native_no_links "$path"
  [[ -e "$path" && "$(native_uid "$path")" = "$owner" ]] || native_fail OWNER_OR_PATH_INVALID
  mode=$(native_mode "$path")
  [[ "$mode" =~ ^[0-7]{3,4}$ ]] || native_fail PERMISSION_INVALID
  (( (8#$mode & 8#$mask) == 0 )) || native_fail PERMISSION_INVALID
  # An attacker-writable ancestor could replace an otherwise protected file between checks.
  parent=$path
  while [[ "$parent" != / && "$parent" != "${NATIVE_ROOT:-}" ]]; do
    parent=$(dirname "$parent")
    [[ "$(native_uid "$parent")" = "$owner" ]] || native_fail OWNER_OR_PATH_INVALID
    mode=$(native_mode "$parent"); (( (8#$mode & 8#022) == 0 )) || native_fail PERMISSION_INVALID
  done
}
native_path() { printf '%s%s' "$NATIVE_ROOT" "$1"; }
native_init() {
  NATIVE_ROOT=; NATIVE_MANIFEST=; NATIVE_ACTION=CHECK; NATIVE_CONFIRM=; NATIVE_CANDIDATE=; NATIVE_SOURCE=; NATIVE_BUNDLE_SHA=
  OWNER_PREVIEW_USER_ID=NONE
  while (( $# )); do
    case "$1" in
      --manifest|--test-root|--confirm|--candidate|--source|--bundle-sha256)
        (( $# >= 2 )) || native_fail ARGUMENT_INVALID
        case "$1" in
          --manifest) NATIVE_MANIFEST=$2;; --test-root) NATIVE_ROOT=$2;; --confirm) NATIVE_CONFIRM=$2;;
          --candidate) NATIVE_CANDIDATE=$2;; --source) NATIVE_SOURCE=$2;; --bundle-sha256) NATIVE_BUNDLE_SHA=$2;;
        esac; shift 2;;
      --apply) NATIVE_ACTION=APPLY; shift;; --prepare) NATIVE_ACTION=PREPARE; shift;; --rotate) NATIVE_ACTION=ROTATE; shift;;
      --check|--dry-run) NATIVE_ACTION=CHECK; shift;;
      --help) printf '%s\n' '--manifest FILE [--test-root TEMPORARY_FIXTURE_ROOT] [operation-specific confirmation]'; exit 0;;
      *) native_fail ARGUMENT_INVALID;;
    esac
  done
  [[ -n "$NATIVE_MANIFEST" && -f "$NATIVE_MANIFEST" && ! -L "$NATIVE_MANIFEST" ]] || native_fail MANIFEST_INVALID
  if [[ -n "$NATIVE_ROOT" ]]; then
    [[ -d "$NATIVE_ROOT" && ! -L "$NATIVE_ROOT" ]] || native_fail TEST_ROOT_INVALID
    NATIVE_ROOT=$(cd "$NATIVE_ROOT" && pwd -P)
    case "$NATIVE_ROOT" in /private/tmp/*|/tmp/*|/private/var/folders/*/T/*) ;; *) native_fail TEST_ROOT_INVALID;; esac
    native_meta "$NATIVE_ROOT" "$(id -u)" 077
    native_meta "$NATIVE_ROOT/.asset-card-test-root" "$(id -u)" 077
    [[ "$(< "$NATIVE_ROOT/.asset-card-test-root")" = TEST_FIXTURE_ONLY ]] || native_fail TEST_ROOT_INVALID
    case "$NATIVE_MANIFEST" in "$NATIVE_ROOT"/*) ;; *) native_fail TEST_ROOT_INVALID;; esac
    native_meta "$NATIVE_MANIFEST" "$(id -u)" 022
  else
    native_meta "$NATIVE_MANIFEST" 0 022
  fi
  local line key value seen='|' required
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* && "$line" != *$'\r'* ]] || native_fail MANIFEST_INVALID
    key=${line%%=*}; value=${line#*=}
    case "$key" in
      MANIFEST_KIND|TARGET_ARCH|SERVICE_NAME|APP_JAR|APP_JAR_SHA256|MAIN_UNIT|MAIN_UNIT_SHA256|SCHEDULER_DROPIN|SCHEDULER_DROPIN_SHA256|READY_SCRIPT|READY_SCRIPT_SHA256|RELEASE_METADATA|RELEASE_METADATA_FORMAT|RELEASE_METADATA_SHA256|MODEL_ROOT|CARD_DROPIN|CREDENTIAL_SOURCE|CREDENTIAL_ID|ROOT_UID|SERVICE_UID|WRITER_JDBC_URL|EXPECTED_DATABASE|WRITER_ROLE|MODEL_MODE|PRODUCTION_MODEL_READY|CARD_STARTUP_MODE|COLLECTION_WINDOW_ID|COLLECTION_STARTS_AT|COLLECTION_ENDS_AT|COLLECTION_SYMBOLS|COLLECTION_STATE_DIRECTORY|ARCHIVE_DIRECTORY|SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE|SHARED_IP_WEIGHT_LIMIT_PER_MINUTE|SHARED_IP_HEADROOM_CONFIRMED_AT|MAXIMUM_REST_REQUESTS|MAXIMUM_REST_WEIGHT|MAXIMUM_CONNECTION_ATTEMPTS|MAXIMUM_CONTROL_MESSAGES|MAXIMUM_NEW_ROWS|MAXIMUM_DATABASE_GROWTH_BYTES|MAXIMUM_WAL_GROWTH_BYTES|MINIMUM_FREE_BYTES|STORAGE_SAME_FILESYSTEM_VERIFIED|DATABASE_FILESYSTEM_DEVICE) ;;
      OWNER_PREVIEW_USER_ID|SERVICE_GID) ;;
      *) native_fail MANIFEST_INVALID;;
    esac
    [[ "$seen" != *"|$key|"* && -n "$value" && "$value" != REPLACE_* && "$value" != *'$'* && "$value" != *'`'* ]] || native_fail MANIFEST_INVALID
    seen="$seen$key|"; printf -v "$key" '%s' "$value"
  done < "$NATIVE_MANIFEST"
  for required in MANIFEST_KIND TARGET_ARCH SERVICE_NAME APP_JAR APP_JAR_SHA256 MAIN_UNIT MAIN_UNIT_SHA256 SCHEDULER_DROPIN SCHEDULER_DROPIN_SHA256 READY_SCRIPT READY_SCRIPT_SHA256 RELEASE_METADATA RELEASE_METADATA_FORMAT RELEASE_METADATA_SHA256 MODEL_ROOT CARD_DROPIN CREDENTIAL_SOURCE CREDENTIAL_ID ROOT_UID SERVICE_UID WRITER_JDBC_URL EXPECTED_DATABASE WRITER_ROLE MODEL_MODE PRODUCTION_MODEL_READY CARD_STARTUP_MODE COLLECTION_WINDOW_ID COLLECTION_STARTS_AT COLLECTION_ENDS_AT COLLECTION_SYMBOLS COLLECTION_STATE_DIRECTORY ARCHIVE_DIRECTORY SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE SHARED_IP_WEIGHT_LIMIT_PER_MINUTE SHARED_IP_HEADROOM_CONFIRMED_AT MAXIMUM_REST_REQUESTS MAXIMUM_REST_WEIGHT MAXIMUM_CONNECTION_ATTEMPTS MAXIMUM_CONTROL_MESSAGES MAXIMUM_NEW_ROWS MAXIMUM_DATABASE_GROWTH_BYTES MAXIMUM_WAL_GROWTH_BYTES MINIMUM_FREE_BYTES STORAGE_SAME_FILESYSTEM_VERIFIED DATABASE_FILESYSTEM_DEVICE; do
    [[ "$seen" == *"|$required|"* ]] || native_fail MANIFEST_INVALID
  done
  [[ "$TARGET_ARCH" = x86_64 && "$SERVICE_NAME" = rine-logic.service && "$APP_JAR" = /opt/rine-logic/current/app.jar
    && "$MAIN_UNIT" = /etc/systemd/system/rine-logic.service
    && "$SCHEDULER_DROPIN" = /etc/systemd/system/rine-logic.service.d/20-core-loop-schedulers.conf
    && "$READY_SCRIPT" = /usr/local/sbin/rine-logic-wait-ready
    && "$CARD_DROPIN" = /etc/systemd/system/rine-logic.service.d/40-asset-card.conf
    && "$MODEL_ROOT" = /opt/rine-logic/models/asset-card
    && "$CREDENTIAL_SOURCE" = /etc/rine-logic/credentials/asset-card-db-password
    && "$CREDENTIAL_ID" = asset-card-db-password && "$WRITER_ROLE" = rine_asset_card_writer
    && "$MODEL_MODE" = SHADOW && "$PRODUCTION_MODEL_READY" = NO ]] || native_fail MANIFEST_IDENTITY_INVALID
  [[ "$ROOT_UID" =~ ^[0-9]+$ && "$SERVICE_UID" =~ ^[0-9]+$ && "$SERVICE_UID" != "$ROOT_UID" \
    && "$seen" == *'|SERVICE_GID|'* && "${SERVICE_GID:-}" =~ ^[0-9]+$ ]] || native_fail OWNER_INVALID
  OWNER_PREVIEW_USER_IDS=
  if [[ "$OWNER_PREVIEW_USER_ID" != NONE ]]; then
    [[ "$OWNER_PREVIEW_USER_ID" =~ ^[1-9][0-9]{0,18}$ ]] || native_fail OWNER_PREVIEW_ID_INVALID
    if (( ${#OWNER_PREVIEW_USER_ID} == 19 )); then
      [[ "$OWNER_PREVIEW_USER_ID" < 9223372036854775808 ]] || native_fail OWNER_PREVIEW_ID_INVALID
    fi
    OWNER_PREVIEW_USER_IDS=$OWNER_PREVIEW_USER_ID
  fi
  [[ "$EXPECTED_DATABASE" =~ ^[A-Za-z0-9_]+$ && "$WRITER_JDBC_URL" =~ ^jdbc:postgresql://[A-Za-z0-9.:-]+/[A-Za-z0-9_]+$ ]] || native_fail DATABASE_IDENTITY_INVALID
  [[ "$WRITER_JDBC_URL" = */"$EXPECTED_DATABASE" && "$RELEASE_METADATA_FORMAT" =~ ^[A-Z0-9_]+$ ]] || native_fail DATABASE_IDENTITY_INVALID
  # Bind the independently observed native release format, never an arbitrary file.
  [[ "$RELEASE_METADATA" = /opt/rine-logic/current/deployment-metadata.txt && "$RELEASE_METADATA_FORMAT" = KEY_VALUE_V1 ]] || native_fail RELEASE_METADATA_INVALID
  for key in APP_JAR_SHA256 MAIN_UNIT_SHA256 SCHEDULER_DROPIN_SHA256 READY_SCRIPT_SHA256 RELEASE_METADATA_SHA256; do
    value=${!key}; [[ "$value" =~ ^[0-9a-f]{64}$ ]] || native_fail MANIFEST_INVALID
  done
  if [[ -n "$NATIVE_ROOT" ]]; then
    [[ "$MANIFEST_KIND" = TEST_FIXTURE_ONLY && "$ROOT_UID" = "$(id -u)" ]] || native_fail TEST_ROOT_INVALID
    case "$NATIVE_MANIFEST" in "$NATIVE_ROOT"/*) ;; *) native_fail TEST_ROOT_INVALID;; esac
  else
    [[ "$MANIFEST_KIND" = NATIVE_SYSTEMD_JAR_RUNTIME && "$ROOT_UID" = 0 ]] || native_fail MANIFEST_IDENTITY_INVALID
  fi
  native_meta "$NATIVE_MANIFEST" "$ROOT_UID" 022
  native_collection_plan_checks
}
native_epoch() {
  local instant="$1" epoch roundtrip
  [[ "$instant" =~ ^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]Z$ ]] || native_fail COLLECTION_CLOCK_INVALID
  epoch=$(date -u -d "$instant" +%s 2>/dev/null) || epoch=$(date -ju -f '%Y-%m-%dT%H:%M:%SZ' "$instant" +%s 2>/dev/null) || native_fail COLLECTION_CLOCK_INVALID
  [[ "$epoch" =~ ^[0-9]+$ ]] || native_fail COLLECTION_CLOCK_INVALID
  roundtrip=$(date -u -d "@$epoch" '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null) || roundtrip=$(date -ur "$epoch" '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null) || native_fail COLLECTION_CLOCK_INVALID
  [[ "$roundtrip" = "$instant" ]] || native_fail COLLECTION_CLOCK_INVALID
  printf '%s\n' "$epoch"
}
native_positive_budget() {
  local value="$1" maximum="$2"
  [[ "$value" =~ ^[1-9][0-9]{0,10}$ ]] && (( value <= maximum )) || native_fail COLLECTION_BUDGET_INVALID
}
native_collection_plan_checks() {
  local start end confirmed now symbol seen=',' registered=','
  [[ "$CARD_STARTUP_MODE" = PREPARED || "$CARD_STARTUP_MODE" = ARMED ]] || native_fail COLLECTION_MODE_INVALID
  [[ "$COLLECTION_STATE_DIRECTORY" = /var/lib/rine-logic-asset-card/collection && "$ARCHIVE_DIRECTORY" = /var/lib/rine-logic-asset-card/archive ]] || native_fail COLLECTION_PATH_INVALID
  native_positive_budget "$MAXIMUM_REST_REQUESTS" 288; native_positive_budget "$MAXIMUM_REST_WEIGHT" 72000
  native_positive_budget "$MAXIMUM_CONNECTION_ATTEMPTS" 32; native_positive_budget "$MAXIMUM_CONTROL_MESSAGES" 128
  native_positive_budget "$MAXIMUM_NEW_ROWS" 1100000; native_positive_budget "$MAXIMUM_DATABASE_GROWTH_BYTES" 3221225472
  native_positive_budget "$MAXIMUM_WAL_GROWTH_BYTES" 8589934592
  [[ "$MINIMUM_FREE_BYTES" = 21474836480 ]] || native_fail COLLECTION_BUDGET_INVALID
  CARD_ENABLED=false
  [[ "$CARD_STARTUP_MODE" = ARMED ]] || return 0
  [[ "$COLLECTION_WINDOW_ID" =~ ^[A-Za-z0-9_-]{8,80}$ && "$COLLECTION_WINDOW_ID" != NOT_CONFIGURED ]] || native_fail COLLECTION_WINDOW_ID_INVALID
  start=$(native_epoch "$COLLECTION_STARTS_AT"); end=$(native_epoch "$COLLECTION_ENDS_AT"); confirmed=$(native_epoch "$SHARED_IP_HEADROOM_CONFIRMED_AT")
  (( end > start && end - start <= 28800 && confirmed <= start && confirmed >= start - 300 )) || native_fail COLLECTION_WINDOW_INVALID
  now=$(date -u +%s)
  (( end > now )) || native_fail COLLECTION_WINDOW_EXPIRED
  (( confirmed <= now )) || native_fail COLLECTION_QUOTA_UNVERIFIED
  [[ "$SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE" =~ ^[1-9][0-9]{2,3}$ && "$SHARED_IP_WEIGHT_LIMIT_PER_MINUTE" =~ ^[1-9][0-9]{2,5}$ ]] || native_fail COLLECTION_QUOTA_UNVERIFIED
  (( SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE >= 250 && SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE <= 1000 && SHARED_IP_WEIGHT_LIMIT_PER_MINUTE >= SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE * 2 )) || native_fail COLLECTION_QUOTA_UNVERIFIED
  [[ "$COLLECTION_SYMBOLS" =~ ^[A-Z0-9]+(,[A-Z0-9]+)*$ ]] || native_fail COLLECTION_SYMBOLS_INVALID
  registered=',AAVEUSDT,ADAUSDT,ALGOUSDT,APTUSDT,ARBUSDT,ASTERUSDT,AVAXUSDT,BCHUSDT,BNBUSDT,BTCUSDT,DOGEUSDT,DOTUSDT,ETCUSDT,ETHUSDT,FETUSDT,FILUSDT,HBARUSDT,HYPERUSDT,ICPUSDT,INJUSDT,LINKUSDT,LTCUSDT,NEARUSDT,OPUSDT,POLUSDT,RUNEUSDT,SEIUSDT,SOLUSDT,SUIUSDT,TAOUSDT,TRXUSDT,UNIUSDT,VETUSDT,XLMUSDT,XRPUSDT,ZECUSDT,'
  local -a symbols; IFS=',' read -r -a symbols <<< "$COLLECTION_SYMBOLS"
  (( ${#symbols[@]} >= 1 && ${#symbols[@]} <= 36 )) || native_fail COLLECTION_SYMBOLS_INVALID
  for symbol in "${symbols[@]}"; do
    [[ "$registered" = *",$symbol,"* && "$seen" != *",$symbol,"* ]] || native_fail COLLECTION_SYMBOLS_INVALID
    seen="$seen$symbol,"
  done
  [[ "$STORAGE_SAME_FILESYSTEM_VERIFIED" = YES && "$DATABASE_FILESYSTEM_DEVICE" =~ ^[0-9]+$ ]] || native_fail DATABASE_STORAGE_EVIDENCE_REQUIRED
  CARD_ENABLED=true
}
native_service_directory() {
  local path="$1" owner="$SERVICE_UID" parent parent_group=0
  if [[ -n "$NATIVE_ROOT" ]]; then
    owner=$ROOT_UID # Fake-root tests cannot change host account ownership.
    parent_group=$(id -g)
  fi
  native_no_links "$path"
  [[ -d "$path" && "$(native_uid "$path")" = "$owner" \
    && "$(native_gid "$path")" = "$SERVICE_GID" && "$(native_mode "$path")" = 700 ]] || native_fail COLLECTION_DIRECTORY_UNSAFE
  parent=$(dirname "$path")
  native_meta "$parent" "$ROOT_UID" 022
  [[ "$(native_gid "$parent")" = "$parent_group" && "$(native_mode "$parent")" = 755 ]] || native_fail COLLECTION_PARENT_UNSAFE
}
native_collection_storage_checks() {
  [[ "$CARD_STARTUP_MODE" = ARMED ]] || return 0
  local directory device
  for directory in "$COLLECTION_STATE_DIRECTORY" "$ARCHIVE_DIRECTORY"; do
    directory=$(native_path "$directory"); native_service_directory "$directory"
    device=$(stat -c '%d' "$directory" 2>/dev/null) || device=$(stat -f '%d' "$directory")
    [[ "$device" = "$DATABASE_FILESYSTEM_DEVICE" ]] || native_fail DATABASE_FILESYSTEM_DECLARATION_MISMATCH
  done
  printf 'DATABASE_FILESYSTEM_EVIDENCE=REVIEWED_DECLARATION_ONLY\nVISIBLE_COLLECTION_DIRECTORIES_DEVICE_MATCH=YES\n'
}
native_collection_environment() {
  [[ "$CARD_STARTUP_MODE" = ARMED ]] || return 0
  local pair
  for pair in "ID=$COLLECTION_WINDOW_ID" "STARTS_AT=$COLLECTION_STARTS_AT" "ENDS_AT=$COLLECTION_ENDS_AT" "SYMBOLS=$COLLECTION_SYMBOLS" "STATE_DIRECTORY=$COLLECTION_STATE_DIRECTORY" \
    "SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE=$SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE" "SHARED_IP_WEIGHT_LIMIT_PER_MINUTE=$SHARED_IP_WEIGHT_LIMIT_PER_MINUTE" "SHARED_IP_HEADROOM_CONFIRMED_AT=$SHARED_IP_HEADROOM_CONFIRMED_AT" \
    "MAXIMUM_REST_REQUESTS=$MAXIMUM_REST_REQUESTS" "MAXIMUM_REST_WEIGHT=$MAXIMUM_REST_WEIGHT" "MAXIMUM_CONNECTION_ATTEMPTS=$MAXIMUM_CONNECTION_ATTEMPTS" "MAXIMUM_CONTROL_MESSAGES=$MAXIMUM_CONTROL_MESSAGES" \
    "MAXIMUM_NEW_ROWS=$MAXIMUM_NEW_ROWS" "MAXIMUM_DATABASE_GROWTH_BYTES=$MAXIMUM_DATABASE_GROWTH_BYTES" "MAXIMUM_WAL_GROWTH_BYTES=$MAXIMUM_WAL_GROWTH_BYTES" "MINIMUM_FREE_BYTES=$MINIMUM_FREE_BYTES"; do
    printf 'Environment="TRADE_MODEL_ASSET_CARD_COLLECTION_WINDOW_%s"\n' "$pair"
  done
}
native_deployment_metadata() {
  local path="$1" line key value seen='|' merged= artifact= deployed= year month day last_day=31
  [[ $(native_size "$path") -le 512 ]] || native_fail RELEASE_METADATA_INVALID
  [[ $(LC_ALL=C tr -d '\012\040-\176' < "$path" | wc -c | tr -d '[:space:]') = 0 ]] || native_fail RELEASE_METADATA_INVALID
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == *=* ]] || native_fail RELEASE_METADATA_INVALID
    key=${line%%=*}; value=${line#*=}
    [[ "$seen" != *"|$key|"* ]] || native_fail RELEASE_METADATA_INVALID
    seen="$seen$key|"
    case "$key" in
      MERGED_MAIN_SHA) [[ "$value" =~ ^[0-9a-f]{40}$ ]] || native_fail RELEASE_METADATA_INVALID; merged=$value;;
      ARTIFACT_SHA256) [[ "$value" =~ ^[0-9a-f]{64}$ && "$value" = "$APP_JAR_SHA256" ]] || native_fail RELEASE_METADATA_INVALID; artifact=$value;;
      DEPLOYED_AT) deployed=$value;;
      *) native_fail RELEASE_METADATA_INVALID;;
    esac
  done < "$path"
  [[ -n "$merged" && -n "$artifact" && "$deployed" =~ ^([0-9]{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]Z$ ]] || native_fail RELEASE_METADATA_INVALID
  year=$((10#${BASH_REMATCH[1]})); month=$((10#${BASH_REMATCH[2]})); day=$((10#${BASH_REMATCH[3]}))
  case "$month" in
    4|6|9|11) last_day=30;;
    2) last_day=28; if (( year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) )); then last_day=29; fi;;
  esac
  (( year > 0 && day <= last_day )) || native_fail RELEASE_METADATA_INVALID
}
native_base_checks() {
  local pair path expected actual parent
  for pair in "$MAIN_UNIT|$MAIN_UNIT_SHA256" "$SCHEDULER_DROPIN|$SCHEDULER_DROPIN_SHA256" "$READY_SCRIPT|$READY_SCRIPT_SHA256"; do
    path=$(native_path "${pair%%|*}"); expected=${pair#*|}
    native_meta "$path" "$ROOT_UID" 022
    [[ -f "$path" && "$(native_sha "$path")" = "$expected" ]] || native_fail BASE_IDENTITY_MISMATCH
  done
  # The external release system owns the existing current symlink. Bind its resolved JAR bytes,
  # verify the resolved path's root ownership, and never replace that external pointer.
  path=$(native_path "$APP_JAR"); [[ -f "$path" ]] || native_fail JAR_IDENTITY_MISMATCH
  actual=$(cd "$(dirname "$path")" && pwd -P)/$(basename "$path")
  case "$actual" in "$(native_path /opt/rine-logic/)"*) ;; *) native_fail JAR_IDENTITY_MISMATCH;; esac
  native_meta "$actual" "$ROOT_UID" 022
  [[ "$(native_sha "$actual")" = "$APP_JAR_SHA256" ]] || native_fail JAR_IDENTITY_MISMATCH
  NATIVE_JAR=$actual
  # A protected external current-directory symlink is allowed only when both
  # files resolve beside each other in the same root-owned release directory.
  path=$(native_path "$RELEASE_METADATA"); [[ -f "$path" ]] || native_fail RELEASE_METADATA_INVALID
  actual=$(cd "$(dirname "$path")" && pwd -P)/$(basename "$path")
  [[ "$(dirname "$actual")" = "$(dirname "$NATIVE_JAR")" ]] || native_fail RELEASE_METADATA_INVALID
  native_meta "$actual" "$ROOT_UID" 022
  [[ "$(native_sha "$actual")" = "$RELEASE_METADATA_SHA256" ]] || native_fail BASE_IDENTITY_MISMATCH
  native_deployment_metadata "$actual"
  for parent in "$(native_path "$MODEL_ROOT")" "$(native_path "$(dirname "$CARD_DROPIN")")" "$(native_path "$(dirname "$CREDENTIAL_SOURCE")")"; do
    native_meta "$parent" "$ROOT_UID" 022
  done
  native_collection_storage_checks
  NATIVE_JAVA=$(native_path /usr/bin/java)
  if [[ -n "$NATIVE_ROOT" ]]; then native_meta "$NATIVE_JAVA" "$ROOT_UID" 022; fi
}
native_credential_metadata() {
  local path="$1" mode
  native_no_links "$path"
  [[ -f "$path" && -s "$path" && "$(native_uid "$path")" = "$ROOT_UID" ]] || native_fail CREDENTIAL_METADATA_INVALID
  mode=$(native_mode "$path")
  [[ "$mode" = 600 || "$mode" = 400 ]] || native_fail CREDENTIAL_METADATA_INVALID
  [[ $(native_size "$path") -le 4096 ]] || native_fail CREDENTIAL_METADATA_INVALID
}
native_candidate_path() {
  native_no_links "$1"
  if [[ -n "$NATIVE_ROOT" ]]; then case "$1" in "$NATIVE_ROOT"/*) ;; *) native_fail TEST_ROOT_INVALID;; esac; fi
}
native_apply_authority() {
  [[ "$NATIVE_CONFIRM" = "$1" ]] || native_fail EXPLICIT_CONFIRMATION_REQUIRED
  if [[ -z "$NATIVE_ROOT" ]]; then
    [[ $(id -u) = 0 ]] || native_fail ROOT_REQUIRED
    native_platform_checks
  fi
}
native_platform_checks() {
  [[ $(uname -s) = Linux && $(uname -m) = x86_64 ]] || native_fail NATIVE_PLATFORM_MISMATCH
  "$NATIVE_JAVA" -version 2>&1 | grep -Eq '^.*version "17[.\"]' || native_fail JAVA17_REQUIRED
  if [[ "$CARD_STARTUP_MODE" = ARMED ]]; then
    # Existing OS interpreter, metadata-only: no installation, secret read, network or package imports.
    [[ -x /usr/bin/python3 ]] || native_fail SYSTEMD_CREDENTIAL_METADATA_RUNTIME_UNAVAILABLE
    /usr/bin/python3 -I -S -B -c 'import os, struct, sys; assert sys.version_info.major == 3 and hasattr(os, "getxattr")' \
      >/dev/null 2>&1 || native_fail SYSTEMD_CREDENTIAL_METADATA_RUNTIME_UNAVAILABLE
  fi
  if [[ -x /sbin/ldconfig ]]; then /sbin/ldconfig -p 2>/dev/null | grep -F 'libgomp.so.1' >/dev/null || native_fail OPENMP_UNAVAILABLE;
  else native_fail OPENMP_UNAVAILABLE; fi
}
native_probe() {
  "$NATIVE_JAVA" -Dloader.main=org.example.trademodel.assetcard.AssetCardNativeRuntimeProbe -cp "$NATIVE_JAR" \
    org.springframework.boot.loader.launch.PropertiesLauncher "$1" --app-jar "$NATIVE_JAR" --app-jar-sha256 "$APP_JAR_SHA256" \
    --bundle-dir "$2" --manifest-sha256 "$3" 2>/dev/null
}
native_status() { printf 'MODEL_MODE=SHADOW\nPRODUCTION_MODEL_READY=NO\nSYSTEMD_CHANGE_EXECUTION=NO\n'; }
native_atomic_replace() {
  # GNU and BSD spell non-dereferencing rename differently. Never follow an existing model pointer.
  mv -fT -- "$1" "$2" 2>/dev/null || mv -fh -- "$1" "$2" 2>/dev/null
}
native_preflight_main() {
  native_init "$@"; [[ "$NATIVE_ACTION" = CHECK ]] || native_fail READ_ONLY_PREFLIGHT
  native_base_checks; native_credential_metadata "$(native_path "$CREDENTIAL_SOURCE")"
  if [[ -n "$NATIVE_ROOT" ]]; then
    printf 'STATUS=PASS\nCODE=LOCAL_SOURCE_FIXTURE_CHECKED\nNATIVE_STATUS=NOT_EXECUTED\n'; native_status; return
  fi
  native_platform_checks
  local pointer directory sha output
  pointer=$(native_path "$MODEL_ROOT/current")
  if [[ -L "$pointer" ]]; then
    directory=$(readlink "$pointer"); [[ "$directory" = bundles/* && "${directory#bundles/}" =~ ^[0-9a-f]{64}$ ]] || native_fail MODEL_POINTER_INVALID
    sha=${directory#bundles/}; directory=$(native_path "$MODEL_ROOT/$directory")
    output=$(native_probe --verify-manifest-checksums "$directory" "$sha") || native_fail MODEL_CHECKSUM_INVALID
    grep -qx 'CHECKSUM_STATUS=PASS' <<< "$output" || native_fail MODEL_CHECKSUM_INVALID
    printf 'MODEL_CHECKSUM_STATUS=PASS\n'
  elif [[ -e "$pointer" ]]; then native_fail MODEL_POINTER_INVALID;
  else printf 'MODEL_STATUS=NOT_CONFIGURED\n'; fi
  printf 'STATUS=PASS\nCODE=READ_ONLY_PREFLIGHT_CHECKED\nNATIVE_STATUS=NOT_EXECUTED\n'; native_status
}
if [[ "${BASH_SOURCE[0]}" = "$0" ]]; then native_preflight_main "$@"; fi
