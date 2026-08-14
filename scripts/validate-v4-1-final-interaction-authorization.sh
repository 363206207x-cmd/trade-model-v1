#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

exact_package="FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION"
current_package="FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION"
canonical_id="PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN"
authorization_id="PS-FUNDAMENTAL-AI-V4-1-FINAL-INTERACTION-AUTHORIZATION"

required_files=(
  docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
  docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_RECONCILIATION_REPORT.md
)

for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "AUTHORIZATION_VALIDATION_BLOCKED missing=$file" >&2; exit 1; }
done

grep -Fq 'fourteen routed responsibilities' docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
grep -Fq 'Total component families: `54`' docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
grep -Fq 'Desktop acceptance total: `81`' docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
grep -Fq 'Duplicate Skeleton Gate' docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md
grep -Fq 'may not create a second Plan, Message' docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md

canonical_count="$(grep -c "^<!-- PRODUCT_SOURCE|${canonical_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
legacy_active_count="$(grep -c '^<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN-AUTHORIZATION|' docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$canonical_count" == "1" && "$authorization_count" == "1" && "$legacy_active_count" == "0" ]] \
  || { echo "AUTHORIZATION_VALIDATION_BLOCKED unique-source-registry" >&2; exit 1; }

grep -Fq "current_package_phase: \"${current_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"${exact_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_final_interaction_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'pr_1179_head: "198fc0ff545240a1b89dbbbfb1a3e642648d4f45"' docs/CODEX_NEXT_TASK.yml

bash scripts/product-source-gate.sh >/dev/null

premerge_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
  bash scripts/v1-state.sh --request-package "$exact_package" 2>&1)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$premerge_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$premerge_output"

merged_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package "$exact_package")"
grep -Fq "AUTHORIZED_PACKAGE: $exact_package" <<<"$merged_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'PR_CREATION_ALLOWED: true' <<<"$merged_output"

wrong_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package "${exact_package}_WRONG" 2>&1)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$wrong_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$wrong_output"

echo "UNIQUE_ACTIVE_PRODUCT_SOURCE: PASS"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED: PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED: PASS"
echo "WRONG_PACKAGE_FAIL_CLOSED: PASS"
echo "PR_1179_AUDITED_HEAD_REGISTERED: PASS"
echo "PAGE_ROUTE_COMPONENT_LINKS: PASS"
echo "DUPLICATE_SKELETON_STATUS: PASS"
echo "AUTHORIZATION_VALIDATION: PASS"
