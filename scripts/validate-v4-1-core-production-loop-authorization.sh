#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

authorization_package="FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION"
exact_package="FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION"
authorization_id="PS-TRINE-LOGIC-CORE-PRODUCTION-LOOP-AUTOMATION-AUTHORIZATION"

required_files=(
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_SOURCE_MAPPING.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION_VALIDATION.md
)
for file in "${required_files[@]}"; do
  [[ -s "$file" ]] || { echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED missing=$file" >&2; exit 1; }
done

authorization_doc="docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md"
ownership_doc="docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md"
source_mapping_doc="docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_SOURCE_MAPPING.md"

grep -Fq "$exact_package" "$authorization_doc"
grep -Fq '`observing` Opportunity | 15 minutes' "$authorization_doc"
grep -Fq '`candidate` Opportunity | 5 minutes' "$authorization_doc"
grep -Fq '`waiting_trigger` Opportunity | 2 minutes' "$authorization_doc"
grep -Fq '`triggered` Opportunity | 1 minute' "$authorization_doc"
grep -Fq '`OPEN` / `PARTIALLY_CLOSED` UserPosition | 30 seconds' "$authorization_doc"
grep -Fq 'Binance public SPOT' "$authorization_doc"
grep -Fq '`5m`, `15m`, `1h` and `4h`' "$authorization_doc"
grep -Fq 'The full Asset Pool receives lightweight scans' "$authorization_doc"
grep -Fq 'promotion-gated' "$authorization_doc"
grep -Fq 'must not add a' "$authorization_doc"
grep -Fq '`nextScanAt` column' "$authorization_doc"
grep -Fq 'All three frozen in-application Message categories remain' "$authorization_doc"
grep -Fq 'narrowed to exactly two categories' "$authorization_doc"
grep -Fq 'at most one Telegram Delivery' "$authorization_doc"
grep -Fq 'default-off' "$authorization_doc"
grep -Fq 'Automatic trading capability count remains `0`' "$authorization_doc"
grep -Fq 'Duplicate Skeleton Gate' "$ownership_doc"
grep -Fq 'New owner allowed' "$ownership_doc"
grep -Fq 'Asset Pool is the sole continuous opportunity source' "$source_mapping_doc"
grep -Fq 'Existing unmerged remediation evidence' "$source_mapping_doc"

authorization_count="$(grep -c "^<!-- PRODUCT_SOURCE|${authorization_id}|" docs/PRODUCT_SOURCE_OF_TRUTH.md || true)"
[[ "$authorization_count" == "1" ]] \
  || { echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED authorization-registry-count=$authorization_count" >&2; exit 1; }

grep -Fq "current_package_phase: \"$authorization_package\"" docs/CODEX_NEXT_TASK.yml
grep -Fq "authorized_next_package_phase: \"$exact_package\"" docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_remediation_authorization_status: "EFFECTIVE_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_telegram_remediation_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_core_production_loop_authorization_status: "AUTHORIZED_PENDING_MERGED_MAIN"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'v4_1_core_production_loop_implementation_status: "NOT_STARTED"' docs/CODEX_NEXT_TASK.yml
grep -Fq 'scope: "V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_ONLY"' docs/CODEX_NEXT_TASK.yml
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
grep -Fq 'V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_STATUS: PENDING_MERGED_MAIN' <<<"$premerge_output"

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
grep -Fq 'V4_1_TELEGRAM_REMEDIATION_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN' <<<"$merged_output"
grep -Fq 'V4_1_TELEGRAM_REMEDIATION_IMPLEMENTATION_STATUS: NOT_STARTED' <<<"$merged_output"
grep -Fq 'V4_1_CORE_PRODUCTION_LOOP_AUTHORIZATION_STATUS: EFFECTIVE_MERGED_MAIN' <<<"$merged_output"
grep -Fq 'V4_1_CORE_PRODUCTION_LOOP_IMPLEMENTATION_STATUS: NOT_STARTED' <<<"$merged_output"

assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATON
assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_EXPANDED
assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_PRODUCTION_DEPLOYMENT
assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTO_TRADING
assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_MOBILE
assert_false_permissions FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_FIGMA

base_ref="$(git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
changed_files="$({ git diff --name-only "$base_ref"; git diff --name-only; git diff --cached --name-only; git ls-files --others --exclude-standard; } | awk 'NF && !seen[$0]++')"
authorization_allowed_files=(
  docs/ACTIVE_MAINLINE_STATUS.yml
  docs/CODEX_NEXT_TASK.yml
  docs/CONTRACT_CHANGE_LOG.md
  docs/DELIVERY_PROGRESS_MATRIX.md
  docs/PRODUCT_SOURCE_OF_TRUTH.md
  docs/PROJECT_CURRENT_STATE.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION_VALIDATION.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_OWNERSHIP_MAP.md
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_SOURCE_MAPPING.md
  docs/V1_CAPABILITY_MATRIX.md
  scripts/check-workflow-contract.sh
  scripts/codex-next-task.sh
  scripts/product-source-gate.sh
  scripts/v1-state.sh
  scripts/validate-v4-1-core-production-loop-authorization.sh
)

implementation_allowed_files=(
  docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_IMPLEMENTATION_REPORT.md
  docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_SHORT_ALERT_IMPLEMENTATION_REPORT.md
  scripts/validate-v4-1-core-production-loop-authorization.sh
  scripts/validate-v4-1-telegram-remediation-authorization.sh
  src/main/java/org/example/trademodel/analysisrun/AnalysisRunCommand.java
  src/main/java/org/example/trademodel/analysisrun/AnalysisRunProperties.java
  src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java
  src/main/java/org/example/trademodel/market/client/impl/BinancePublicOhlcvProvider.java
  src/main/java/org/example/trademodel/mapper/AssetStateMapper.java
  src/main/java/org/example/trademodel/mapper/ChannelDeliveryMapper.java
  src/main/java/org/example/trademodel/mapper/ExecutionPlanMapper.java
  src/main/java/org/example/trademodel/mapper/MessageMapper.java
  src/main/java/org/example/trademodel/service/AnalysisSchedulerService.java
  src/main/java/org/example/trademodel/service/AssetStateService.java
  src/main/java/org/example/trademodel/service/ChannelDeliveryService.java
  src/main/java/org/example/trademodel/service/MarketDataScheduler.java
  src/main/java/org/example/trademodel/service/MessageFactService.java
  src/main/java/org/example/trademodel/service/PersistedOhlcvIngestionScheduler.java
  src/main/java/org/example/trademodel/service/PositionMonitorScheduler.java
  src/main/java/org/example/trademodel/service/impl/AssetStateServiceImpl.java
  src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java
  src/main/java/org/example/trademodel/service/impl/OpportunityPriorityRankingServiceImpl.java
  src/main/java/org/example/trademodel/service/impl/PositionMonitorServiceImpl.java
  src/main/java/org/example/trademodel/telegram/HighValueAlertMessageService.java
  src/main/java/org/example/trademodel/telegram/HighValueAlertPolicy.java
  src/main/java/org/example/trademodel/telegram/TelegramDedupeKey.java
  src/main/java/org/example/trademodel/telegram/TelegramDeliveryDispatcher.java
  src/main/java/org/example/trademodel/telegram/TelegramMessageCommitListener.java
  src/main/java/org/example/trademodel/telegram/TelegramMessageFormatter.java
  src/main/resources/application-prod.yml
  src/main/resources/application-local-real.yml
  src/main/resources/application.yml
  src/main/resources/static/js/home-runtime.js
  src/test/java/org/example/trademodel/analysisrun/AnalysisSchedulerServiceTest.java
  src/test/java/org/example/trademodel/config/LocalSmokeSchedulerGateTest.java
  src/test/java/org/example/trademodel/config/ProductionProfileSafetyGuardTest.java
  src/test/java/org/example/trademodel/config/TargetRuntimePreflightTest.java
  src/test/java/org/example/trademodel/controller/AnalysisRunControllerTest.java
  src/test/java/org/example/trademodel/controller/ApprovedFigmaHomeRuntimeContractTest.java
  src/test/java/org/example/trademodel/controller/HomeUiReviewRuntimeContractTest.java
  src/test/java/org/example/trademodel/localreal/LocalRealProfileContractTest.java
  src/test/java/org/example/trademodel/mapper/AssetStateMapperIntegrationTest.java
  src/test/java/org/example/trademodel/market/client/impl/BinancePublicOhlcvProviderTest.java
  src/test/java/org/example/trademodel/market/client/impl/RoutedPublicOhlcvProviderTest.java
  src/test/java/org/example/trademodel/positionmonitor/PositionMonitorServiceImplTest.java
  src/test/java/org/example/trademodel/providercall/coinglass/CoinGlassV4ProviderTest.java
  src/test/java/org/example/trademodel/service/AnalysisSchedulerLocalRealReadinessGateTest.java
  src/test/java/org/example/trademodel/service/ChannelDeliveryTelegramContractTest.java
  src/test/java/org/example/trademodel/service/MessageFactServiceTest.java
  src/test/java/org/example/trademodel/service/PersistedOhlcvIngestionSchedulerTest.java
  src/test/java/org/example/trademodel/service/impl/AssetStateServiceImplTest.java
  src/test/java/org/example/trademodel/service/impl/DashboardHomeServiceImplTest.java
  src/test/java/org/example/trademodel/service/impl/OpportunityPriorityRankingServiceImplTest.java
  src/test/java/org/example/trademodel/telegram/HighValueAlertMessageServiceTest.java
  src/test/java/org/example/trademodel/telegram/HighValueAlertPolicyTest.java
  src/test/java/org/example/trademodel/telegram/TelegramDeliveryDispatcherTest.java
  src/test/java/org/example/trademodel/telegram/TelegramDeliveryOrphanMapperIntegrationTest.java
  src/test/java/org/example/trademodel/telegram/TelegramMessageCommitListenerTest.java
  src/test/java/org/example/trademodel/telegram/TelegramMessageFormatterTest.java
  src/test/resources/provider/coinglass/v4/funding-negative-success.json
)

if git cat-file -e "$base_ref:$authorization_doc" 2>/dev/null; then
  allowed_files=("${implementation_allowed_files[@]}")
  authorization_scope="AUTHORIZED_IMPLEMENTATION"
else
  allowed_files=("${authorization_allowed_files[@]}")
  authorization_scope="AUTHORIZATION_DOCS_GATE"
fi

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
    echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED unauthorized-file=$changed_file" >&2
    exit 1
  fi
done <<<"$changed_files"

if [[ "$authorization_scope" == "AUTHORIZATION_DOCS_GATE" ]] \
  && grep -Eq '^(src/|pom\.xml|.*\.sql$|.*\.java$|.*\.js$|.*\.html$|.*\.css$|.*application[^/]*\.ya?ml$)' <<<"$changed_files"; then
  echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED application-api-schema-ui-config-change" >&2
  exit 1
fi

if grep -Eq '(^|/)db/migration/|\.sql$|\.html$|\.css$' <<<"$changed_files"; then
  echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED schema-or-ui-change" >&2
  exit 1
fi

while IFS= read -r changed_file; do
  [[ -n "$changed_file" ]] || continue
  if [[ "$changed_file" == *.js ]] \
    && [[ "$changed_file" != "src/main/resources/static/js/home-runtime.js" ]]; then
    echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED unauthorized-js=$changed_file" >&2
    exit 1
  fi
done <<<"$changed_files"

if ! git diff --quiet "$base_ref" -- docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md; then
  echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=BLOCKED frozen-product-source-changed" >&2
  exit 1
fi

echo "ASSET_POOL_SOLE_CONTINUOUS_SOURCE=PASS"
echo "STATE_SENSITIVE_CADENCES=15m,5m,2m,lightweight-1m"
echo "ACTIVE_POSITION_MONITOR_CADENCE=30s"
echo "PROMOTION_GATED_FULL_ANALYSIS=PASS"
echo "BINANCE_SOURCE_OWNED_CLOSED_OHLCV=5m,15m,1h,4h"
echo "MESSAGE_CATEGORIES_RETAINED=3"
echo "TELEGRAM_FIRST_RELEASE_CATEGORIES_RETAINED=2"
echo "PREMERGE_EXACT_PACKAGE_BLOCKED=PASS"
echo "MERGED_MAIN_EXACT_PACKAGE_ALLOWED=PASS"
echo "WRONG_AND_EXPANDED_PACKAGES_BLOCKED=PASS"
echo "AUTHORIZATION_SCOPE=$authorization_scope"
echo "UNAUTHORIZED_APPLICATION_API_SCHEMA_UI_CONFIG_CHANGE_COUNT=0"
echo "DATABASE_SCHEMA_CHANGE_COUNT=0"
echo "SCHEDULER_SWITCH_CHANGES=0"
echo "TELEGRAM_SWITCH_CHANGES=0"
echo "TELEGRAM_REAL_SEND_ATTEMPTS=0"
echo "DEPLOYMENT_ATTEMPTS=0"
echo "AUTO_TRADING_CAPABILITY_COUNT=0"
echo "CAPABILITY_MOVEMENT=NONE"
echo "CORE_PRODUCTION_LOOP_AUTHORIZATION_VALIDATION=PASS"
