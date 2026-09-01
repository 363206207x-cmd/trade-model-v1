#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

exact_package="MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE"
authorization_package="MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE_AUTHORIZATION"

required_files=(
  docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION.md
  docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_SOURCE_MAPPING.md
  docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md
  docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION_VALIDATION.md
)
for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "MULTI_USER_AUTHORIZATION_VALIDATION=BLOCKED missing $file" >&2; exit 1; }
done

grep -Fq "$exact_package" docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION.md
grep -Fq 'Duplicate Skeleton Gate' docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md
grep -Fq 'GLOBAL_SHARED' docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md
grep -Fq 'USER_OWNED' docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md
grep -Fq 'OWNER_ONLY' docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_OWNERSHIP_MAP.md
grep -Fq 'PS-TRINE-LOGIC-MULTI-USER-ACCOUNT-REGISTRATION-AUTHORIZATION' docs/PRODUCT_SOURCE_OF_TRUTH.md

bash scripts/product-source-gate.sh --task-file docs/CODEX_NEXT_TASK.yml >/dev/null

current_package_phase="$(awk -F': ' '$1 == "current_package_phase" {gsub(/"/, "", $2); print $2; exit}' docs/CODEX_NEXT_TASK.yml)"
if [[ "$current_package_phase" == "$authorization_package" ]]; then
  grep -Fq "authorized_next_package_phase: \"$exact_package\"" docs/CODEX_NEXT_TASK.yml

  premerge_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
    bash scripts/v1-state.sh --request-package "$exact_package")"
  grep -Fq 'AUTHORIZATION_STATUS: BLOCKED' <<<"$premerge_output"
  grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"

  merged_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
    bash scripts/v1-state.sh --request-package "$exact_package")"
  grep -Fq 'AUTHORIZATION_STATUS: AUTHORIZED' <<<"$merged_output"
  grep -Fq "AUTHORIZED_PACKAGE: $exact_package" <<<"$merged_output"
  grep -Fq 'IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
  grep -Fq 'REPOSITORY_EDITS_ALLOWED: true' <<<"$merged_output"
  grep -Fq 'PR_CREATION_ALLOWED: true' <<<"$merged_output"
  grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
  grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"

  set +e
  wrong_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
    bash scripts/v1-state.sh --request-package MULTI_USER_ACCOUNT_REGISTRATION_CLOSUR 2>&1)"
  set -e
  grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$wrong_output"
  grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"
else
  grep -Fq 'multi_user_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
  grep -Fq 'multi_user_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
  grep -Fq 'multi-user authorization is effective on merged main' docs/PROJECT_CURRENT_STATE.md
fi

changed_files="$({ git diff --name-only; git diff --cached --name-only; } | sort -u)"
if [[ "$current_package_phase" == "$authorization_package" ]] \
  && grep -Eq '^(src/|pom.xml|.*\.sql$|.*\.java$|.*\.js$|.*\.html$|.*\.css$)' <<<"$changed_files"; then
  echo "MULTI_USER_AUTHORIZATION_VALIDATION=BLOCKED application/API/schema/UI change in authorization diff" >&2
  exit 1
fi

echo "MULTI_USER_AUTHORIZATION_VALIDATION=PASS"
