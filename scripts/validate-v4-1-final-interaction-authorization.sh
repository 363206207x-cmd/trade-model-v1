#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

exact_package="FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION"
current_package="FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_AUTHORIZATION"
canonical_id="PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN"
authorization_id="PS-FUNDAMENTAL-AI-V4-1-FINAL-INTERACTION-AUTHORIZATION"
canonical_figma_key="rdMYmsAvZYkXHJX8hdl7UN"
visual_contract="docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md"
visual_contract_hash="4d3e937be4534d69e07d34fcf3fe08c4cd5a63ed0bda58b4961ffe6249d26d61"

required_files=(
  docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
  docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md
  docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_RECONCILIATION_REPORT.md
  docs/FUNDAMENTAL_AI_V4_1_CANONICAL_FIGMA_AUTHORIZATION_SCOPE_RECONCILIATION.md
  "$visual_contract"
)

for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "AUTHORIZATION_VALIDATION_BLOCKED missing=$file" >&2; exit 1; }
done

grep -Fq 'fourteen routed responsibilities' docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
grep -Fq 'Total component families: `54`' docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
grep -Fq 'Desktop acceptance total: `81`' docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
grep -Fq 'Duplicate Skeleton Gate' docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md
grep -Fq 'may not create a second Plan, Message' docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
grep -Fq "SHA-256 $visual_contract_hash" docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
grep -Fq '70:30' "$visual_contract"
grep -Fq '`SUPERSEDED`' "$visual_contract"
grep -Fq '| Position Monitoring | 60% | 58-62% |' "$visual_contract"
grep -Fq '| Final Execution Plan | 40% | 38-42% |' "$visual_contract"
grep -Fq "Canonical Figma binding: file key \`$canonical_figma_key\`" docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md
if command -v sha256sum >/dev/null 2>&1; then
  actual_visual_hash="$(sha256sum "$visual_contract" | awk '{print $1}')"
else
  actual_visual_hash="$(shasum -a 256 "$visual_contract" | awk '{print $1}')"
fi
[[ "$actual_visual_hash" == "$visual_contract_hash" ]] \
  || { echo "AUTHORIZATION_VALIDATION_BLOCKED visual-contract-hash" >&2; exit 1; }

canonical_count="$(grep -c "^<!-- PRODUCT_SOURCE|${canonical_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
legacy_active_count="$(grep -c '^<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN-AUTHORIZATION|' docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$canonical_count" == "1" && "$authorization_count" == "1" && "$legacy_active_count" == "0" ]] \
  || { echo "AUTHORIZATION_VALIDATION_BLOCKED unique-source-registry" >&2; exit 1; }

grep -Fq "current_package_phase: \"${current_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"${exact_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_final_interaction_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'pr_1179_head: "62ba9702e54b268ef27158bcff7e33422e23015e"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'pr_1179_disposition: "REUSABLE_PENDING_AUTHORIZATION_RECONCILIATION"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_canonical_figma_desktop_implementation_allowed: true' docs/CODEX_NEXT_TASK.yml
grep -Fq 'authorized_next_package_mobile_implementation_allowed: false' docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_canonical_figma_file_key: \"$canonical_figma_key\"" docs/CODEX_NEXT_TASK.yml

bash scripts/product-source-gate.sh >/dev/null

premerge_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_pending \
  bash scripts/v1-state.sh --request-package "$exact_package" 2>&1)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$premerge_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$premerge_output"

merged_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package "$exact_package")"
grep -Fq "AUTHORIZED_PACKAGE: $exact_package" <<<"$merged_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'PR_CREATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: true' <<<"$merged_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$merged_output"
grep -Fq "CANONICAL_FIGMA_FILE_KEY: $canonical_figma_key" <<<"$merged_output"

wrong_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package "${exact_package}_WRONG" 2>&1)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$wrong_output"
grep -Fq 'IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"
grep -Fq 'PR_CREATION_ALLOWED: false' <<<"$wrong_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$wrong_output"

mobile_output="$(V1_WORKFLOW_SELF_TEST=1 V1_HANDOFF_SELF_TEST_SCENARIO=authorization_merged_validated \
  bash scripts/v1-state.sh --request-package FUNDAMENTAL_AI_V4_1_MOBILE 2>&1 || true)"
grep -Fq 'RESOLUTION_STATUS: BLOCKED' <<<"$mobile_output"
grep -Fq 'CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false' <<<"$mobile_output"
grep -Fq 'MOBILE_IMPLEMENTATION_ALLOWED: false' <<<"$mobile_output"

echo "UNIQUE_ACTIVE_PRODUCT_SOURCE: PASS"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED: PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED: PASS"
echo "WRONG_PACKAGE_FAIL_CLOSED: PASS"
echo "EXACT_PACKAGE_CANONICAL_FIGMA_ALLOWED: PASS"
echo "MOBILE_IMPLEMENTATION_FORBIDDEN: PASS"
echo "VISUAL_CONTRACT_REGISTERED: PASS"
echo "OLD_70_30_SUPERSEDED: PASS"
echo "PR_1179_CANDIDATE_HEAD_REGISTERED: PASS"
echo "PAGE_ROUTE_COMPONENT_LINKS: PASS"
echo "DUPLICATE_SKELETON_STATUS: PASS"
echo "AUTHORIZATION_VALIDATION: PASS"
