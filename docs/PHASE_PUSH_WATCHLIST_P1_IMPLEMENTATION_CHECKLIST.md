# Push Watchlist P1 Implementation Checklist

## 1. P1 Implementation Principles

- Do not restore the whole external workspace source track at once.
- Every commit must be a smallest closed package.
- Every commit must compile and have focused tests.
- Advance schema, mapper, service, controller, and tests in separate steps unless a smaller closed package requires merging adjacent steps.
- Restored files must come only from the minimum necessary external workspace files, or be rewritten by hand.
- Every restored hunk must exclude RuleEngine, PlanBoundary, Opportunity, TradeReview, ReviewCenter, and RuleImprovement content.
- No automatic order placement.
- No automatic position opening.
- No automatic position closing.
- No automatic reverse position actions.
- No exchange order API.

## 2. Recommended Commit Order

### Commit 1: Schema Audit Table Only

Goal:

- Add only `tm_push_watchlist_config_audit`.
- Add only necessary indexes.

Allowed file:

- `src/main/resources/schema.sql`

Allowed content:

- `CREATE TABLE IF NOT EXISTS tm_push_watchlist_config_audit`
- `idx_tm_push_watchlist_config_audit_time`
- Optional `idx_tm_push_watchlist_config_audit_rule_time`

Forbidden content:

- RuleEngine readonly audit.
- OpportunityLog.
- TradeReview.
- RuleImprovement.
- Push snapshot / recheck extra indexes.
- AccountRisk / MonitorAlert indexes.

Validation:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- schema expected grep.
- schema forbidden grep.

### Commit 2: Watchlist Audit Mapper / VO

Goal:

- Add audit insert and query ability.

Allowed files:

- `src/main/java/org/example/trademodel/vo/PushWatchlistConfigAuditVO.java`
- `src/main/java/org/example/trademodel/mapper/PushWatchlistConfigAuditMapper.java`
- Mapper integration test.

Notes:

- `PushWatchlistConfigAuditDO.java` path is currently uncertain. Prefer not to depend on a DO in this commit, or explicitly rebuild the DO if the final implementation needs it.
- Fields must align with the final schema.

Forbidden content:

- RuleConfigService write logic.
- Controller API.
- Push workflow changes.

Validation:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- Push watchlist audit mapper test.

### Commit 3: RuleConfig Mapper / Service Watchlist Read And Write

Goal:

- Read disabled configuration.
- Insert or update `push.watchlist.symbols`.
- Normalize, deduplicate, and validate symbols.
- Write audit in the same transaction.
- Call `reloadRules()`.

Allowed files:

- `src/main/java/org/example/trademodel/mapper/RuleConfigMapper.java`
- `src/main/java/org/example/trademodel/service/RuleConfigService.java`
- `src/main/java/org/example/trademodel/service/impl/RuleConfigServiceImpl.java`
- `src/main/java/org/example/trademodel/dto/PushWatchlistConfigRequest.java`
- `src/main/java/org/example/trademodel/vo/PushWatchlistConfigVO.java`
- Service tests.

Allowed hunks:

- `findByRuleKeyIncludingDisabled`
- `updateRuleConfigByKey`
- `insertRuleConfig`
- `getPushWatchlist`
- `updatePushWatchlist`
- `listPushWatchlistAudit`, if the service API is completed in this commit.

Forbidden content:

- RuleEngine runtime flags.
- PlanBoundary runtime/write flags.
- Push latest-price recheck.
- Asset-state gate.
- Stampede guard.

Validation:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- `RuleConfigServiceImpl` watchlist tests.
- `WatchlistPushEligibilityServiceImplTest`

### Commit 4: RuleController Watchlist API

Goal:

- `GET /api/rule/push-watchlist`
- `POST /api/rule/push-watchlist`
- Optional `GET /api/rule/push-watchlist/audit`

Allowed files:

- `src/main/java/org/example/trademodel/controller/RuleController.java`
- Controller tests.

Forbidden content:

- UI.
- Templates.
- RuleEngine / PlanBoundary endpoints.
- Push workflow changes.

Validation:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- RuleController watchlist API tests.

### Commit 5: P0 Regression And Smoke

Goal:

- Confirm P1 does not break P0 fail-closed behavior.

Tests:

- `WatchlistPushEligibilityServiceImplTest`
- `PushSnapshotServiceTest`
- `PushRecheckServiceImplTest`
- `DashboardControllerTest`
- `DecisionServiceImplTest`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- dashboard/API smoke.

## 3. P1 Schema Checklist

Fields:

- `audit_id`
- `rule_key`
- `before_symbols`
- `after_symbols`
- `before_enabled`
- `after_enabled`
- `changed_by`
- `change_reason`
- `source`
- `trace_id`
- `rule_version`
- `create_time`

Indexes:

- `idx_tm_push_watchlist_config_audit_time(create_time)`
- `idx_tm_push_watchlist_config_audit_rule_time(rule_key, create_time)`, optional.

Must exclude:

- `tm_rule_engine_candidate_readonly_audit`
- `tm_opportunity_log`
- `tm_position_trade_review`
- `tm_rule_improvement_suggestion`
- `idx_tm_account_risk_snapshot_symbol_create_time`
- `idx_tm_monitor_alert_status_created_at`
- `idx_tm_monitor_alert_type_created_at`
- `idx_tm_push_snapshot_status_expires_at`
- `idx_tm_push_recheck_log_status_create_time`

## 4. P1 API Checklist

### `GET /api/rule/push-watchlist`

Response:

- `ruleKey`
- `symbols`
- `enabled`
- `ruleValue`
- `source` / `ruleVersion`, optional.
- `updatedAt`, only if the existing table supports it.

### `POST /api/rule/push-watchlist`

Request:

- `symbols`
- `enabled`
- `operator`
- `reason`

Validation:

- `operator` is required.
- `reason` is required.
- `symbols` may be an empty array, meaning fail-closed.
- Symbols are trimmed and normalized to uppercase.
- Duplicate symbols are removed.
- Blank symbols are filtered out.
- The default six watched symbols must not be auto-filled.
- `reloadRules()` is called after write success.

### `GET /api/rule/push-watchlist/audit`

Response:

- `ruleKey`
- `beforeSymbols`
- `afterSymbols`
- `beforeEnabled`
- `afterEnabled`
- `changedBy`
- `changeReason`
- `source`
- `traceId`
- `ruleVersion`
- `createTime`

## 5. Transaction Checklist

- Rule config update and audit insert run in the same transaction.
- If audit write fails, the rule config update rolls back.
- If `reloadRules()` fails, the update should fail as a whole to avoid cache/database divergence.
- Empty watchlist, disabled watchlist, and exceptional states must remain fail-closed.
- No fallback to full push eligibility is allowed.

## 6. File Recovery Checklist

Project-external files that may be referenced:

- `PushWatchlistConfigRequest.java`
- `PushWatchlistConfigVO.java`
- `PushWatchlistConfigAuditVO.java`
- `PushWatchlistConfigAuditMapper.java`

Must be careful:

- `PushWatchlistConfigAuditDO.java` path and fields are currently uncertain. Do not use it blindly.
- RuleConfig residual backup may only provide precise watchlist hunks.
- Schema residual backup may only provide the `tm_push_watchlist_config_audit` hunk.

Do not restore:

- RuleEngine / PlanBoundary files.
- Opportunity / TradeReview / ReviewCenter files.
- RuleImprovement files.
- Latest-price recheck.
- Asset-state gate.
- Stampede guard.
- UI templates.

## 7. Test Checklist

Must cover:

- Normalize / dedupe.
- Missing config fail-closed.
- Disabled config fail-closed.
- Blank config fail-closed.
- Create new rule config.
- Update existing rule config.
- Audit written.
- Audit failure rollback.
- `reloadRules()` called.
- Eligibility sees updated config.
- Controller validation.
- `operator` / `reason` required.
- No default six symbols.
- P0 snapshot gate regression.
- P0 recheck gate regression.

## 8. Forbidden Grep Checklist

Before staging each commit, grep the staged diff for:

- `RuleEngine`
- `PlanBoundary`
- `Opportunity`
- `TradeReview`
- `ReviewCenter`
- `RuleImprovement`
- `latestPrice`
- `recheckWithLatestPrice`
- `MarketQuoteClient`
- `AssetStateMapper`
- `stampede`
- `自动下单`
- `自动开仓`
- `自动平仓`
- `自动反手`
- `order API`
- `apiKey`
- `secret`
- `password`

## 9. P1 Non-Goals

- No UI.
- No latest-price recheck.
- No asset-state gate.
- No stampede guard.
- No RuleEngine.
- No Opportunity.
- No TradeReview.
- No RuleImprovement.
- No automatic trading.
- No exchange order API.

## 10. Next Step

- Commit this checklist first.
- Then enter Commit 1: schema audit table only, starting with a read-only staging plan.
- Do not directly restore RuleConfig / controller / mapper large blocks.
