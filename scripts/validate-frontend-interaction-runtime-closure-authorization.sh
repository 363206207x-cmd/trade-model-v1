#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

exact_package="FRONTEND_INTERACTION_RUNTIME_CLOSURE"
authorization_package="FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION"

required_files=(
  docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION.md
  docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_SOURCE_MAPPING.md
  docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_OWNERSHIP_MAP.md
  docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION_VALIDATION.md
)
for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "FRONTEND_INTERACTION_AUTHORIZATION_VALIDATION=BLOCKED missing $file" >&2; exit 1; }
done

grep -Fq "$exact_package" docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION.md
grep -Fq 'Duplicate Skeleton Gate' docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_OWNERSHIP_MAP.md
grep -Fq 'PS-FUNDAMENTAL-AI-FRONTEND-INTERACTION-RUNTIME-CLOSURE-AUTHORIZATION' docs/PRODUCT_SOURCE_OF_TRUTH.md
grep -Fq 'frontend_interaction_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'frontend_interaction_implementation_status: "COMPLETE"' docs/CODEX_NEXT_TASK.yml

bash scripts/product-source-gate.sh --task-file docs/CODEX_NEXT_TASK.yml >/dev/null

premerge_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
  bash scripts/v1-state.sh --request-package "$exact_package")"
grep -Fq 'AUTHORIZATION_STATUS: BLOCKED' <<<"$premerge_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"

merged_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package "$exact_package")"
grep -Fq 'AUTHORIZATION_STATUS: BLOCKED' <<<"$merged_output"
grep -Fq "REQUESTED_PACKAGE: $exact_package" <<<"$merged_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'REPOSITORY_EDITS_ALLOWED: false' <<<"$merged_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"

set +e
wrong_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package FRONTEND_INTERACTION_RUNTIME_CLOSUR 2>&1)"
set -e
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$wrong_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"

current_package_phase="$(awk -F': ' '$1 == "current_package_phase" {gsub(/"/, "", $2); print $2; exit}' \
  docs/CODEX_NEXT_TASK.yml)"
changed_files="$({ git diff --name-only; git diff --cached --name-only; } | sort -u)"
if [[ "$current_package_phase" == "$authorization_package" ]] \
  && grep -Eq '^(src/|pom.xml|.*\.sql$)' <<<"$changed_files"; then
  echo "FRONTEND_INTERACTION_AUTHORIZATION_VALIDATION=BLOCKED application/API/schema change in authorization diff" >&2
  exit 1
fi

echo "FRONTEND_INTERACTION_AUTHORIZATION_VALIDATION=PASS"
