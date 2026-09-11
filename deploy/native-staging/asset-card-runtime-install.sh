#!/usr/bin/env bash
set +x
set -euo pipefail
SCRIPT_DIRECTORY=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
source "$SCRIPT_DIRECTORY/asset-card-runtime-preflight.sh"
native_init "$@"; native_base_checks
[[ "$NATIVE_ACTION" = CHECK || "$NATIVE_ACTION" = APPLY ]] || native_fail ARGUMENT_INVALID
native_credential_metadata "$(native_path "$CREDENTIAL_SOURCE")"
if [[ "$NATIVE_ACTION" = CHECK ]]; then
  printf 'STATUS=PASS\nACTION=DRY_RUN\nROLLBACK_PLAN=RESTORE_PRIOR_CARD_DROPIN_OR_REMOVE_NEW_CARD_DROPIN\n'
  native_status; exit 0
fi
if [[ "$CARD_STARTUP_MODE" = ARMED ]]; then
  native_apply_authority INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY
else
  native_apply_authority INSTALL_ASSET_CARD_DROPIN_ONLY
fi
target=$(native_path "$CARD_DROPIN"); parent=$(dirname "$target")
if [[ -e "$target" || -L "$target" ]]; then
  native_meta "$target" "$ROOT_UID" 022
  [[ -f "$target" ]] || native_fail DROPIN_PATH_INVALID
  backup="$target.previous.$(date -u +%Y%m%dT%H%M%SZ).$$"
  [[ ! -e "$backup" ]] || native_fail BACKUP_COLLISION
  cp -p -- "$target" "$backup"
else backup=ABSENT; fi
stage=$(mktemp "$parent/.pending-card-dropin.XXXXXXXX")
cleanup() { [[ ! -f "$stage" ]] || rm -f -- "$stage"; }
trap cleanup EXIT
while IFS= read -r line || [[ -n "$line" ]]; do
  line=${line//@WRITER_JDBC_URL@/$WRITER_JDBC_URL}
  line=${line//@EXPECTED_DATABASE@/$EXPECTED_DATABASE}
  line=${line//@SERVICE_UID@/$SERVICE_UID}
  line=${line//@CARD_ENABLED@/$CARD_ENABLED}
  line=${line//@ARCHIVE_DIRECTORY@/$ARCHIVE_DIRECTORY}
  printf '%s\n' "$line"
done < "$SCRIPT_DIRECTORY/rine-logic-asset-card.conf.template" > "$stage"
native_collection_environment >> "$stage"
chmod 644 "$stage"
native_atomic_replace "$stage" "$target" || native_fail ATOMIC_DROPIN_SWITCH_FAILED
inventory="$target.rollback.$(date -u +%Y%m%dT%H%M%SZ).$$"
(umask 077; printf 'KIND=ASSET_CARD_DROPIN_ROLLBACK\nTARGET=%s\nPREVIOUS=%s\nMAIN_UNIT_UNCHANGED=YES\nRESTART_EXECUTED=NO\n' "$CARD_DROPIN" "$backup" > "$inventory")
printf 'STATUS=PASS\nACTION=CARD_DROPIN_INSTALLED\nCARD_STARTUP_MODE=%s\nROLLBACK_INVENTORY=CREATED\nRESTART_REQUIRED=YES\n' "$CARD_STARTUP_MODE"
native_status
