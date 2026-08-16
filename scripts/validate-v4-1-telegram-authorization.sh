#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

current_package="FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_AUTHORIZATION"
exact_package="FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION"
authorization_id="PS-FUNDAMENTAL-AI-V4-1-TELEGRAM-AUTHORIZATION"

required_files=(
  docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
  docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md
  docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md
  docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_AUTHORIZATION_VALIDATION.md
)

for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "AUTHORIZATION_VALIDATION_BLOCKED missing=$file" >&2; exit 1; }
done

grep -Fq 'Section 15.2' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md
grep -Fq 'HIGH_PERMISSION_OPPORTUNITY' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md
grep -Fq 'OPPORTUNITY_PLAN_SAFETY_CHANGE' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md
grep -Fq 'POSITION_LOGIC_RISK_CHANGE' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_SOURCE_MAPPING.md
grep -Fq 'MessageDO' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md
grep -Fq 'ChannelDeliveryDO' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md
grep -Fq 'Duplicate Skeleton Gate' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_OWNERSHIP_MAP.md
grep -Fq 'TELEGRAM_LIVE_SECRET_REQUIRED_FOR_IMPLEMENTATION=false' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md
grep -Fq 'TELEGRAM_SECRET_FILE_READ_ALLOWED=false' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md
grep -Fq 'TELEGRAM_SECRET_REPOSITORY_WRITE_ALLOWED=false' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md
grep -Fq 'CAPABILITY_MOVEMENT=NONE' docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md

authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$authorization_count" == "1" ]] \
  || { echo "AUTHORIZATION_VALIDATION_BLOCKED authorization-registry-count=$authorization_count" >&2; exit 1; }

grep -Fq "current_package_phase: \"${current_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"${exact_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_target_runtime_remediation_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_target_runtime_remediation_implementation_status: "COMPLETE"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_authorization_status: "AUTHORIZED_PENDING_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_canonical_figma_desktop_implementation_allowed: false' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_mobile_implementation_allowed: false' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_canonical_figma_file_key: "NONE"' docs/CODEX_NEXT_TASK.yml

bash scripts/product-source-gate.sh >/dev/null

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

merged_output="$(state_for "$exact_package")"
grep -Fq "REQUESTED_PACKAGE: $exact_package" <<<"$merged_output"
grep -Fq 'REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE' <<<"$merged_output"
grep -Fq 'REPOSITORY_EDITS_ALLOWED: true' <<<"$merged_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'PR_CREATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_FILE_KEY: NONE' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_IMPLEMENTATION_STATUS: NOT_STARTED' <<<"$merged_output"

assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATON
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION_EXPANDED
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_AUTO_TRADING
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_MOBILE
assert_false_permissions FUNDAMENTAL_AI_V4_1_TELEGRAM_FIGMA

base_ref="$(git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
changed_files="$({ git diff --name-only "$base_ref"; git diff --name-only; git diff --cached --name-only; } | awk 'NF && !seen[$0]++')"
effective_branch="${GITHUB_HEAD_REF:-$(git branch --show-current)}"
if [[ "$effective_branch" == "codex/v4-1-telegram-high-value-alert-channel" ]]; then
  if grep -Eiq '(^|/)(figma|mobile)(/|\.|$)' <<<"$changed_files"; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED protected-design-scope-change" >&2
    exit 1
  fi
  if grep -Eiq 'auto[_ -]?(open|close|reverse|order|trade)' <<<"$(git diff "$base_ref" -- $changed_files 2>/dev/null || true)"; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED automatic-trading-scope" >&2
    exit 1
  fi
  echo "IMPLEMENTATION_EXACT_BRANCH_SCOPE: PASS"
else
  if grep -Eq '^(pom\.xml|src/(main|test)/|.*application[^/]*\.ya?ml$|.*\.java$|.*\.sql$|.*dashboard.*|.*mobile.*|.*figma.*)' <<<"$changed_files"; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED forbidden-authorization-change-scope" >&2
    printf '%s\n' "$changed_files" >&2
    exit 1
  fi
fi

echo "PRODUCT_SOURCE_MAPPING: PASS"
echo "OBJECT_OWNERSHIP_REUSE: PASS"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED: PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED: PASS"
echo "TYPO_EXPANDED_PACKAGES_BLOCKED: PASS"
echo "AUTO_TRADING_MOBILE_FIGMA_PACKAGES_BLOCKED: PASS"
echo "PRIVATE_SECRET_ACCESS_REQUIRED: NO"
echo "IMPLEMENTATION_STATUS: NOT_STARTED"
echo "CAPABILITY_MOVEMENT: NONE"
echo "AUTHORIZATION_VALIDATION: PASS"
