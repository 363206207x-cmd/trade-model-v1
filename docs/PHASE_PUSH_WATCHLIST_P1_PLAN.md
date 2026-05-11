# Push Watchlist P1 Plan

## 1. P1 Background

- Push Watchlist P0 is complete and verified.
- P0 introduced read-only watchlist eligibility.
- P0 reads configuration through `RuleConfigService#getRuleConfigMap()`.
- P0 uses `push.watchlist.symbols` as the watchlist key.
- P0 fails closed for non-watchlist assets.
- P1 only adds configuration maintenance and audit. It does not change the Push decision or recheck main logic.

## 2. P1 Scope

P1 includes:

- `GET /api/rule/push-watchlist`
- `POST /api/rule/push-watchlist`
- `GET /api/rule/push-watchlist/audit`, recommended as part of the complete P1 audit loop.
- Write and update `push.watchlist.symbols` in `tm_rule_config`.
- Add `tm_push_watchlist_config_audit`.
- Call `reloadRules()` after successful update.
- Normalize, deduplicate, and validate watchlist symbols.
- Define the audit transaction boundary.

P1 excludes:

- UI.
- Latest-price recheck.
- Asset-state gate.
- Stampede guard.
- PushWatchlist management page.
- RuleEngine / PlanBoundary.
- Opportunity / OpportunityLog.
- TradeReview / ReviewCenter.
- RuleImprovement.
- Automatic order placement.
- Automatic position opening.
- Automatic position closing.
- Automatic reverse position actions.
- Exchange order API.

## 3. Configuration Storage Strategy

Recommended strategy:

- Continue using `tm_rule_config` as the source of truth.
- Continue using `push.watchlist.symbols`.
- Do not add a separate watchlist truth table.
- Do not treat the default six watched assets as an implicit watchlist.
- Disabled, missing, blank, or invalid configuration fails closed.
- After the update API writes `tm_rule_config`, it must call `reloadRules()`.

Reason not to add a dedicated watchlist table in P1:

- It introduces another source of truth.
- It expands schema, synchronization, and UI complexity.
- It exceeds the P1 goal of configuration maintenance plus audit.

## 4. Schema Strategy

P1 may add only:

- `tm_push_watchlist_config_audit`
- Necessary indexes, such as `create_time` or `(rule_key, create_time)`.

Recommended fields:

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

Deferred schema:

- Push snapshot indexes.
- Push recheck indexes.
- RuleEngine schema.
- Opportunity schema.
- TradeReview schema.
- RuleImprovement schema.

## 5. API Design

### `GET /api/rule/push-watchlist`

Response fields:

- `ruleKey`
- `symbols`
- `enabled`
- `ruleValue`
- `updatedAt`, only if the existing table supports it.
- `source` / `ruleVersion`, optional.

### `POST /api/rule/push-watchlist`

Request fields:

- `symbols`
- `enabled`
- `operator`
- `reason`

Requirements:

- `operator` is required.
- `reason` is required.
- Symbols are normalized to uppercase.
- Leading and trailing whitespace is trimmed.
- Duplicate symbols are removed.
- Blank symbols are removed.
- Empty symbol lists are allowed and mean fail-closed.
- The API writes `tm_rule_config`.
- The API writes audit.
- Audit failure rolls back the whole update.
- `reloadRules()` is called after a successful update.

### `GET /api/rule/push-watchlist/audit`

Recommended for P1. It returns the latest N audit records:

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

## 6. Transaction And Failure Strategy

- Configuration update and audit insert run in one transaction.
- If audit insert fails, the configuration update rolls back.
- `reloadRules()` timing must be explicit.
- Recommended P1 behavior: if `reloadRules()` fails, treat the whole update as failed to avoid cache/database divergence.
- Writing an empty watchlist never means all assets are eligible; it means fail-closed.
- Writing disabled configuration also fails closed.
- Any exceptional state must never fall back to full push eligibility.

## 7. Candidate File List

P1 may need to restore or add:

- `PushWatchlistConfigRequest.java`
- `PushWatchlistConfigVO.java`
- `PushWatchlistConfigAuditVO.java`
- `PushWatchlistConfigAuditDO.java`, or use only VO + mapper if the implementation chooses that route.
- `PushWatchlistConfigAuditMapper.java`
- `RuleConfigMapper.java` precise hunks:
  - `findByRuleKeyIncludingDisabled`
  - `updateRuleConfigByKey`
  - `insertRuleConfig`
- `RuleConfigService.java` precise hunks:
  - watchlist read
  - watchlist update
  - watchlist audit list
- `RuleConfigServiceImpl.java` precise watchlist-only implementation hunks.
- `RuleController.java` precise GET / POST / audit API hunks.
- `schema.sql` precise hunk:
  - only `tm_push_watchlist_config_audit`
  - only necessary indexes.

P1 tests:

- `RuleConfigServiceImpl` watchlist-only tests.
- `RuleController` watchlist API tests.
- `PushWatchlistConfigAuditMapper` integration tests.
- `WatchlistPushEligibilityServiceImpl` regression test.
- `PushSnapshotServiceTest` regression test.
- `PushRecheckServiceImplTest` regression test.

Must not restore:

- RuleEngine runtime flag hunks.
- PlanBoundary runtime/write flag hunks.
- Push latest-price recheck.
- Asset-state gate.
- Stampede guard.
- OpportunityLog.
- TradeReview / ReviewCenter.
- RuleImprovement.
- Templates / UI.

## 8. Candidate File Assessment

- `PushWatchlistConfigRequest`: reusable, but `operator` and `reason` validation must be explicit.
- `PushWatchlistConfigVO`: reusable.
- `PushWatchlistConfigAuditVO`: reusable, but fields must align with the final schema.
- `PushWatchlistConfigAuditDO`: needs completion or rewrite, especially around reason / trace / version fields.
- `PushWatchlistConfigAuditMapper`: can be reused precisely, but must follow the final schema.
- RuleConfig residual backup: only watchlist hunks may be reused. RuleEngine / PlanBoundary hunks must be excluded.

## 9. P1 Safety Boundary

- No automatic order placement.
- No automatic position opening.
- No automatic position closing.
- No automatic reverse position action.
- No exchange order API.
- Non-watchlist assets are not pushed.
- Disabled watchlist is not pushed.
- Empty watchlist is not pushed.
- Configuration errors are not pushed.
- `governance_missed` is not pushed.
- `HIGH_RISK`, `CONFUSED`, `INVALIDATED`, and `COOLING` do not directly become push opportunities.
- The default six watched assets must not become an implicit watchlist.

## 10. Test Strategy

After implementation, run at least:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- `RuleConfigServiceImpl` watchlist tests.
- `RuleController` watchlist API tests.
- `PushWatchlistConfigAuditMapper` tests.
- `WatchlistPushEligibilityServiceImplTest`
- `PushSnapshotServiceTest`
- `PushRecheckServiceImplTest`
- `DashboardControllerTest`
- dashboard/API smoke.

## 11. Risks

- Schema lands before matching implementation.
- Audit schema grows too wide and mixes unrelated tracks.
- Bad configuration accidentally enables all assets.
- `reloadRules()` diverges from database state.
- Too many files are restored from the external workspace.
- UI is introduced too early.
- RuleEngine / Opportunity / TradeReview code is mixed into P1.
- The default six watched assets are treated as an implicit watchlist.

## 12. Next Step Recommendation

- Commit this P1 plan document first.
- Then enter P1 minimum file recovery design.
- Consider implementation commits in small slices:
  - schema audit table only
  - mapper / service watchlist write only
  - controller API only
  - tests
- The final split can still be adjusted around the smallest closed package.
- Do not restore the whole external workspace source track at once.
