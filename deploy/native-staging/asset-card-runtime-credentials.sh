#!/usr/bin/env bash
set +x
set -euo pipefail
SCRIPT_DIRECTORY=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
source "$SCRIPT_DIRECTORY/asset-card-runtime-preflight.sh"
native_init "$@"; native_base_checks
target=$(native_path "$CREDENTIAL_SOURCE")
case "$NATIVE_ACTION" in
  CHECK)
    native_credential_metadata "$target"
    printf 'STATUS=PASS\nACTION=CHECK_ONLY\nCREDENTIAL_METADATA=PASS\nFRESH_CONNECTION=NOT_EXECUTED\n'
    native_status; exit 0;;
  PREPARE|ROTATE) native_apply_authority PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY;;
  *) native_fail ARGUMENT_INVALID;;
esac
[[ -n "$NATIVE_CANDIDATE" && "$NATIVE_CANDIDATE" != "$target" ]] || native_fail CANDIDATE_REQUIRED
native_candidate_path "$NATIVE_CANDIDATE"; native_credential_metadata "$NATIVE_CANDIDATE"
# A prepared protected candidate is renamed, not copied through a plaintext temporary file.
[[ "$(dirname "$NATIVE_CANDIDATE")" = "$(dirname "$target")" ]] || native_fail CANDIDATE_MUST_SHARE_PROTECTED_DIRECTORY
if [[ "$NATIVE_ACTION" = ROTATE ]]; then native_credential_metadata "$target";
else [[ ! -e "$target" && ! -L "$target" ]] || native_fail EXISTING_CREDENTIAL_USE_ROTATE; fi
output=$("$NATIVE_JAVA" -Dloader.main=org.example.trademodel.assetcard.AssetCardDataSourceConfiguration -cp "$NATIVE_JAR" \
  org.springframework.boot.loader.launch.PropertiesLauncher verify --jdbc-url "$WRITER_JDBC_URL" --expected-database "$EXPECTED_DATABASE" \
  --credential-file "$NATIVE_CANDIDATE" --credential-owner "$ROOT_UID" 2>/dev/null) || native_fail FRESH_CREDENTIAL_VERIFICATION_FAILED
grep -qx 'ASSET_CARD_WRITER_VERIFY=PASS' <<< "$output" || native_fail FRESH_CREDENTIAL_VERIFICATION_FAILED
native_credential_metadata "$NATIVE_CANDIDATE"
if [[ "$NATIVE_ACTION" = ROTATE ]]; then
  backup="$target.previous.$(date -u +%Y%m%dT%H%M%SZ).$$"
  [[ ! -e "$backup" && ! -L "$backup" ]] || native_fail BACKUP_COLLISION
  ln -- "$target" "$backup" 2>/dev/null || native_fail OLD_CREDENTIAL_BACKUP_FAILED
fi
native_atomic_replace "$NATIVE_CANDIDATE" "$target" || native_fail ATOMIC_CREDENTIAL_SWITCH_FAILED
printf 'STATUS=PASS\nACTION=CREDENTIAL_SWITCHED\nFRESH_CONNECTION=PASS\nRESTART_REQUIRED=YES\nHOT_ROTATION=NOT_CLAIMED\n'
native_status
