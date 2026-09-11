#!/usr/bin/env bash
set +x
set -euo pipefail
SCRIPT_DIRECTORY=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
source "$SCRIPT_DIRECTORY/asset-card-runtime-preflight.sh"
native_init "$@"; native_base_checks
[[ "$NATIVE_ACTION" = CHECK || "$NATIVE_ACTION" = APPLY ]] || native_fail ARGUMENT_INVALID
if [[ -z "$NATIVE_SOURCE" ]]; then
  [[ "$NATIVE_ACTION" = CHECK ]] || native_fail SOURCE_REQUIRED
  printf 'STATUS=PASS\nACTION=DRY_RUN\nMODEL_STATUS=NOT_CONFIGURED\nCURRENT_MODEL_LINK=UNCHANGED\n'; native_status; exit 0
fi
native_candidate_path "$NATIVE_SOURCE"; native_meta "$NATIVE_SOURCE" "$ROOT_UID" 022
[[ -d "$NATIVE_SOURCE" && "$NATIVE_BUNDLE_SHA" =~ ^[0-9a-f]{64}$ ]] || native_fail MODEL_IDENTITY_INVALID
if [[ "$NATIVE_ACTION" = APPLY ]]; then native_apply_authority INSTALL_ASSET_CARD_MODEL_ONLY; fi
# Full real-manifest qualification is required even though checksum-only is a useful preflight.
output=$(native_probe --verify-bundle-only "$NATIVE_SOURCE" "$NATIVE_BUNDLE_SHA") || native_fail MODEL_NOT_QUALIFIED
grep -qx 'VERIFIED_BUNDLE_STATUS=PASS' <<< "$output" || native_fail MODEL_NOT_QUALIFIED
grep -qx 'DATA_KIND=REAL_HISTORICAL' <<< "$output" || native_fail MODEL_NOT_QUALIFIED
if [[ "$NATIVE_ACTION" = CHECK ]]; then
  printf 'STATUS=PASS\nACTION=DRY_RUN\nVERIFIED_BUNDLE_STATUS=PASS\nCURRENT_MODEL_LINK=UNCHANGED\n'; native_status; exit 0
fi
root=$(native_path "$MODEL_ROOT"); bundles="$root/bundles"; target="$bundles/$NATIVE_BUNDLE_SHA"
[[ -e "$bundles" ]] || mkdir -m 755 -- "$bundles"
native_meta "$bundles" "$ROOT_UID" 022
files=(manifest.json long.ubj short.ubj calibration.json thresholds.json risk-distributions.json validation.json)
stage=
cleanup() {
  if [[ -n "$stage" && "$stage" = "$bundles"/.pending.* && -d "$stage" && ! -L "$stage" ]]; then
    chmod 700 "$stage"
    for name in "${files[@]}"; do [[ ! -f "$stage/$name" ]] || rm -f -- "$stage/$name"; done
    rmdir -- "$stage" 2>/dev/null || true
  fi
}
trap cleanup EXIT
if [[ -e "$target" || -L "$target" ]]; then
  native_meta "$target" "$ROOT_UID" 022
  output=$(native_probe --verify-bundle-only "$target" "$NATIVE_BUNDLE_SHA") || native_fail IMMUTABLE_BUNDLE_MISMATCH
  grep -qx 'VERIFIED_BUNDLE_STATUS=PASS' <<< "$output" || native_fail IMMUTABLE_BUNDLE_MISMATCH
  grep -qx 'DATA_KIND=REAL_HISTORICAL' <<< "$output" || native_fail IMMUTABLE_BUNDLE_MISMATCH
else
  stage=$(mktemp -d "$bundles/.pending.XXXXXXXX")
  for name in "${files[@]}"; do
    native_meta "$NATIVE_SOURCE/$name" "$ROOT_UID" 022
    cp -- "$NATIVE_SOURCE/$name" "$stage/$name"; chmod 444 "$stage/$name"
  done
  output=$(native_probe --verify-bundle-only "$stage" "$NATIVE_BUNDLE_SHA") || native_fail COPIED_BUNDLE_MISMATCH
  grep -qx 'VERIFIED_BUNDLE_STATUS=PASS' <<< "$output" || native_fail COPIED_BUNDLE_MISMATCH
  grep -qx 'DATA_KIND=REAL_HISTORICAL' <<< "$output" || native_fail COPIED_BUNDLE_MISMATCH
  chmod 555 "$stage"; mv -- "$stage" "$target"; stage=
fi
pointer="$root/current"
if [[ -L "$pointer" ]]; then
  previous=$(readlink "$pointer")
  [[ "$previous" = bundles/* && "${previous#bundles/}" =~ ^[0-9a-f]{64}$ ]] || native_fail MODEL_POINTER_INVALID
  native_meta "$root/$previous" "$ROOT_UID" 022
  ln -s -- "$previous" "$root/.previous.$$"
  native_atomic_replace "$root/.previous.$$" "$root/previous" || native_fail ROLLBACK_POINTER_FAILED
elif [[ -e "$pointer" ]]; then native_fail MODEL_POINTER_INVALID; fi
ln -s -- "bundles/$NATIVE_BUNDLE_SHA" "$root/.current.$$"
native_atomic_replace "$root/.current.$$" "$pointer" || native_fail ATOMIC_MODEL_SWITCH_FAILED
printf 'STATUS=PASS\nACTION=IMMUTABLE_MODEL_SWITCHED\nROLLBACK=PREVIOUS_BUNDLE_RETAINED\nRESTART_REQUIRED=YES\n'
native_status
