#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

authorization_package="FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_REMEDIATION_AUTHORIZATION"
exact_package="FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION"
authorization_id="PS-TRINE-LOGIC-TELEGRAM-TWO-CATEGORY-REMEDIATION-AUTHORIZATION"

required_files=(
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_SOURCE_MAPPING.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_OWNERSHIP_MAP.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION_VALIDATION.md
)
for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=BLOCKED missing=$file" >&2; exit 1; }
done

authorization_doc="docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md"
ownership_doc="docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_OWNERSHIP_MAP.md"

grep -Fq "$exact_package" "$authorization_doc"
grep -Fq 'three in-application Message categories remain available' "$authorization_doc"
grep -Fq 'Only these two categories may be connected' "$authorization_doc"
grep -Fq 'CONFIRMATION' "$authorization_doc"
grep -Fq 'suppress `REDUCED`' "$authorization_doc"
grep -Fq 'must not create Telegram ChannelDelivery' "$authorization_doc"
grep -Fq 'VERIFIED + FRESH' "$authorization_doc"
grep -Fq 'existing three Telegram switches with defaults off' "$authorization_doc"
grep -Fq 'no automatic position scheduler' "$authorization_doc"
grep -Fq 'no Telegram switch activation or real Telegram send' "$authorization_doc"
grep -Fq 'no Staging or Production deployment' "$authorization_doc"
grep -Fq 'Duplicate Skeleton Gate' "$ownership_doc"
grep -Fq 'New Message owner: `NO`' "$ownership_doc"
grep -Fq 'New ChannelDelivery owner: `NO`' "$ownership_doc"

authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$authorization_count" == "1" ]] \
  || { echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=BLOCKED authorization-registry-count=$authorization_count" >&2; exit 1; }

grep -Fq "current_package_phase: \"$authorization_package\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"$exact_package\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_implementation_status: "COMPLETE"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_remediation_authorization_status: "AUTHORIZED_PENDING_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_remediation_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'scope: "V4_1_TELEGRAM_TWO_CATEGORY_REMEDIATION_ONLY"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_canonical_figma_desktop_implementation_allowed: false' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_mobile_implementation_allowed: false' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_canonical_figma_file_key: "NONE"' docs/CODEX_NEXT_TASK.yml

bash scripts/product-source-gate.sh --task-file docs/CODEX_NEXT_TASK.yml >/dev/null

state_for() {
  local package="$1"
  V1_WORKFLOW_SELF_TEST=1 \
    V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
    bash scripts/v1-state.sh --request-package "$package" 2>&1 || true
}

assert_false_permissions() {
  local package="$1" output
  output="$(state_for "$package")"
  grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$output"
  grep -Fq 'REPOSITORY_EDITS_ALLOWED: false' <<<"$output"
  grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$output"
  grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$output"
  grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$output"
  grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$output"
}

premerge_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
  bash scripts/v1-state.sh --request-package "$exact_package" 2>&1 || true)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$premerge_output"
grep -Fq 'REPOSITORY_EDITS_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS: PENDING_MERGED_MAIN' <<<"$premerge_output"

merged_output="$(state_for "$exact_package")"
grep -Fq "REQUESTED_PACKAGE: $exact_package" <<<"$merged_output"
grep -Fq 'REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE' <<<"$merged_output"
grep -Fq 'AUTHORIZATION_STATUS: AUTHORIZED' <<<"$merged_output"
grep -Fq 'REPOSITORY_EDITS_ALLOWED: true' <<<"$merged_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'PR_CREATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_FILE_KEY: NONE' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_IMPLEMENTATION_STATUS: COMPLETE' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_REMEDIATION_IMPLEMENTATION_STATUS: NOT_STARTED' <<<"$merged_output"

assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATON
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION_EXPANDED
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_PRODUCTION_DEPLOYMENT
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_AUTO_TRADING
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_MOBILE
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_FIGMA

base_ref="$(git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
changed_files="$({ git diff --name-only "$base_ref"; git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard; } | awk 'NF && !seen[$0]++')"
allowed_files=(
  docs/ACTIVE_MAINLINE_STATUS.yml
  docs/CODEX_NEXT_TASK.yml
  docs/CONTRACT_CHANGE_LOG.md
  docs/DELIVERY_PROGRESS_MATRIX.md
  docs/PRODUCT_SOURCE_OF_TRUTH.md
  docs/PROJECT_CURRENT_STATE.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION_VALIDATION.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_OWNERSHIP_MAP.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_SOURCE_MAPPING.md
  docs/V1_CAPABILITY_MATRIX.md
  scripts/check-workflow-contract.sh
  scripts/codex-next-task.sh
  scripts/product-source-gate.sh
  scripts/v1-state.sh
  scripts/validate-multi-user-account-registration-authorization.sh
  scripts/validate-v4-1-telegram-remediation-authorization.sh
)
while IFS= read -r changed_file; do
  [[ -n "$changed_file" ]] || continue
  allowed="NO"
  for allowed_file in "${allowed_files[@]}"; do
    if [[ "$changed_file" == "$allowed_file" ]]; then
      allowed="YES"
      break
    fi
  done
  if [[ "$allowed" != "YES" ]]; then
    echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=BLOCKED unauthorized-file=$changed_file" >&2
    exit 1
  fi
done <<<"$changed_files"

if grep -Eq '^(src/|pom\.xml|.*\.sql$|.*\.java$|.*\.js$|.*\.html$|.*\.css$|.*application[^/]*\.ya?ml$)' <<<"$changed_files"; then
  echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=BLOCKED application-api-schema-ui-config-change" >&2
  exit 1
fi

if ! git diff --quiet "$base_ref" -- docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md; then
  echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=BLOCKED frozen-section-15-2-source-changed" >&2
  exit 1
fi

echo "THREE_IN_APP_MESSAGE_CATEGORIES_RETAINED=PASS"
echo "OWNER_FIRST_RELEASE_TELEGRAM_CATEGORIES=2"
echo "SAFETY_CHANGE_TELEGRAM_DELIVERY=BLOCKED"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED=PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED=PASS"
echo "WRONG_AND_EXPANDED_PACKAGES_BLOCKED=PASS"
echo "APPLICATION_API_SCHEMA_CONFIG_CHANGE_COUNT=0"
echo "TELEGRAM_REAL_SEND_ATTEMPTS=0"
echo "CAPABILITY_MOVEMENT=NONE"
echo "TELEGRAM_REMEDIATION_AUTHORIZATION_VALIDATION=PASS"
