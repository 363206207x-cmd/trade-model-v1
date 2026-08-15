#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

current_package="FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION"
exact_package="FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION"
authorization_id="PS-FUNDAMENTAL-AI-V4-1-TARGET-RUNTIME-REMEDIATION-AUTHORIZATION"

required_files=(
  docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md
  docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md
  docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_OWNERSHIP_MAP.md
  docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md
  docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION_VALIDATION.md
)

for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "AUTHORIZATION_VALIDATION_BLOCKED missing=$file" >&2; exit 1; }
done

grep -Fq 'B01' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md
grep -Fq 'B02' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md
grep -Fq 'B03' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md
grep -Fq 'B04' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md
grep -Fq 'Increases duplicate skeleton surface: NO' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_OWNERSHIP_MAP.md
grep -Fq 'COINGLASS_LIVE_SECRET_REQUIRED_FOR_IMPLEMENTATION=false' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md
grep -Fq 'COINGLASS_SECRET_REPOSITORY_WRITE_ALLOWED=false' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md
grep -Fq 'CAPABILITY_MOVEMENT=NONE' docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md

authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$authorization_count" == "1" ]] \
  || { echo "AUTHORIZATION_VALIDATION_BLOCKED authorization-registry-count=$authorization_count" >&2; exit 1; }

grep -Fq "current_package_phase: \"${current_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"${exact_package}\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_final_interaction_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_final_interaction_implementation_status: "COMPLETE"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_target_runtime_remediation_authorization_status: "AUTHORIZED_PENDING_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_target_runtime_remediation_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
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
grep -Fq 'V4_1_TARGET_RUNTIME_REMEDIATION_IMPLEMENTATION_STATUS: NOT_STARTED' <<<"$merged_output"

assert_false_permissions FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION
assert_false_permissions FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATON
assert_false_permissions FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_EXPANDED
assert_false_permissions FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_AUTO_TRADING
assert_false_permissions FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_MOBILE
assert_false_permissions FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_FIGMA

base_ref="$(git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
changed_files="$({ git diff --name-only "$base_ref"; git diff --name-only; git diff --cached --name-only; } | awk 'NF && !seen[$0]++')"
effective_branch="${GITHUB_HEAD_REF:-$(git branch --show-current)}"
if [[ "$effective_branch" == "codex/v4-1-target-runtime-blocker-remediation" ]]; then
  implementation_path_allowed() {
    local file="$1"
    case "$file" in
      pom.xml | \
      scripts/generate-runtime-password.sh | \
      scripts/standard-release-postgresql-smoke.sh | \
      scripts/target-runtime-preflight.sh | \
      scripts/controlled-current-state-clone-rehearsal-p3.sh | \
      scripts/controlled-postgresql-flyway-v7-evidence.sh | \
      scripts/validate-v4-1-target-runtime-blocker-authorization.sh | \
      docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION.md | \
      docs/FUNDAMENTAL_AI_V4_1_RELEASE_JAR_FLYWAY_FIX.md | \
      docs/FUNDAMENTAL_AI_V4_1_PROVIDER_CAPABILITY_MATRIX.md | \
      docs/FUNDAMENTAL_AI_V4_1_AI_PROVIDER_READINESS.md | \
      docs/FUNDAMENTAL_AI_V4_1_AUTH_BOOTSTRAP_PREFLIGHT.md | \
      docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_REACCEPTANCE_HANDOFF.md | \
      docs/FUNDAMENTAL_AI_V4_1_TEST_REPORT.md | \
      docs/FUNDAMENTAL_AI_V4_1_REMAINING_GAPS.md | \
      docs/FUNDAMENTAL_AI_V4_1_ENVIRONMENT_VARIABLES.md | \
      docs/FUNDAMENTAL_AI_V4_1_DEPLOYMENT_RUNBOOK.md | \
      docs/FUNDAMENTAL_AI_V4_1_DEPLOYMENT_SMOKE_TEST.md | \
      src/main/resources/application.yml | \
      src/main/resources/application-prod.yml)
        return 0
        ;;
      src/main/java/org/example/trademodel/ai/*.java | \
      src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java | \
      src/main/java/org/example/trademodel/config/TargetRuntimePreflight.java | \
      src/main/java/org/example/trademodel/controller/AiOrchestratorController.java | \
      src/main/java/org/example/trademodel/controller/AssetPoolController.java | \
      src/main/java/org/example/trademodel/dto/assetpool/*.java | \
      src/main/java/org/example/trademodel/dto/ohlcv/PublicProviderErrorCode.java | \
      src/main/java/org/example/trademodel/localreal/LocalRealDataCoordinator.java | \
      src/main/java/org/example/trademodel/market/client/impl/*.java | \
      src/main/java/org/example/trademodel/providercall/coinglass/*.java | \
      src/main/java/org/example/trademodel/providercall/instrument/*.java | \
      src/main/java/org/example/trademodel/security/*.java | \
      src/main/java/org/example/trademodel/service/RealMarketDataFetcherService.java | \
      src/main/java/org/example/trademodel/service/readiness/ProviderReadinessServiceImpl.java | \
      src/main/java/org/example/trademodel/service/watchlistsource/*.java | \
      src/test/java/org/example/trademodel/actuator/*.java | \
      src/test/java/org/example/trademodel/ai/*.java | \
      src/test/java/org/example/trademodel/config/*.java | \
      src/test/java/org/example/trademodel/controller/*.java | \
      src/test/java/org/example/trademodel/localreal/*.java | \
      src/test/java/org/example/trademodel/market/client/impl/*.java | \
      src/test/java/org/example/trademodel/postgresql/*.java | \
      src/test/java/org/example/trademodel/providercall/*.java | \
      src/test/java/org/example/trademodel/providercall/coinglass/*.java | \
      src/test/java/org/example/trademodel/providercall/instrument/*.java | \
      src/test/java/org/example/trademodel/security/*.java | \
      src/test/java/org/example/trademodel/service/*.java | \
      src/test/java/org/example/trademodel/service/watchlistsource/*.java)
        return 0
        ;;
      *)
        return 1
        ;;
    esac
  }

  forbidden_files=""
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    if ! implementation_path_allowed "$file"; then
      forbidden_files+="${file}"$'\n'
    fi
  done <<<"$changed_files"
  if [[ -n "$forbidden_files" ]]; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED implementation-forbidden-change-scope" >&2
    printf '%s' "$forbidden_files" >&2
    exit 1
  fi
  if grep -Eiq '(^|/)(mobile|figma)(/|\.|$)|\.sql$|dashboard\.(html|js|css)$' <<<"$changed_files"; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED protected-scope-change" >&2
    exit 1
  fi
  echo "IMPLEMENTATION_EXACT_BRANCH_SCOPE: PASS"
else
  if grep -Eq '^(pom\.xml|src/(main|test)/|.*application[^/]*\.ya?ml$|.*\.java$|.*\.sql$|.*dashboard.*|.*mobile.*|.*figma.*)' <<<"$changed_files"; then
    echo "AUTHORIZATION_VALIDATION_BLOCKED forbidden-change-scope" >&2
    printf '%s\n' "$changed_files" >&2
    exit 1
  fi
fi

echo "PRODUCT_SOURCE_MAPPING: PASS"
echo "OBJECT_OWNERSHIP_REUSE: PASS"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED: PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED: PASS"
echo "OLD_TYPO_EXPANDED_PACKAGES_BLOCKED: PASS"
echo "AUTO_TRADING_MOBILE_FIGMA_PACKAGES_BLOCKED: PASS"
echo "IMPLEMENTATION_STATUS: NOT_STARTED"
echo "CAPABILITY_MOVEMENT: NONE"
echo "AUTHORIZATION_VALIDATION: PASS"
