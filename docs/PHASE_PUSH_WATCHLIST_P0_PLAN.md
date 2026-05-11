# Watchlist / Push P0 Plan

## 1. P0 Background

- Clean HEAD already has baseline PushSnapshot and PushRecheck capabilities.
- Pending recheck retry window was moved from SQL DATEADD to Java-side `retryBeforeTime` in `b783d5f`.
- Clean HEAD does not yet have a watchlist eligibility gate.
- The project external workspace contains Push / Watchlist candidate files, but P0 does not restore them blindly.

## 2. P0 Product Boundary

- Only key watchlist assets may enter Push.
- Non-watchlist assets must fail closed.
- Push is only a manual review reminder, not a trading signal.
- `governance_missed` is not a push candidate.
- `HIGH_RISK`, `CONFUSED`, `INVALIDATED`, and `COOLING` must not directly become opportunity pushes.
- Stampede state forbids reversal, new opening, and opportunity push.
- P0 does not automatically place orders.
- P0 does not automatically open positions.
- P0 does not automatically close positions.
- P0 does not automatically reverse positions.
- P0 does not connect to exchange order APIs.

## 3. P0 Config Source

- P0 reads only the existing `tm_rule_config`.
- Recommended rule key: `push.watchlist.symbols`.
- Missing config, disabled config, blank config, parse failure, or service exception must fail closed.
- P0 does not add a config maintenance API.
- P0 does not add an audit table.
- P0 does not add or modify schema.
- P0 does not modify `application.yml`.

## 4. P0 Minimal Capability

- `WatchlistPushEligibilityService` only decides whether a symbol is allowed to enter Push.
- `PushSnapshotService` must check watchlist eligibility before capture.
- `PushRecheckServiceImpl` must check watchlist eligibility again before recheck can proceed.
- Non-watchlist assets must not write a push snapshot and must not enter a push or recheck executable chain.
- Logs or reason payloads should state `blocked_by_watchlist` or an equivalent reason.
- P0 keeps the existing PushRecheck state-machine flow.
- P0 does not implement the latest-price recheck block.
- P0 does not implement the asset-state gate block.
- P0 does not implement the stampede guard block.

## 5. P0 Minimal File Candidates

P0 required candidates:

- `WatchlistPushEligibilityService.java`
- `WatchlistPushEligibilityServiceImpl.java`

P0 needs fresh implementation or precise hunks from backup:

- `PushSnapshotService.java` watchlist gate hunk
- `PushRecheckServiceImpl.java` recheck gate hunk
- Corresponding minimal test hunks

P0 does not restore:

- `PushWatchlistConfigRequest.java`
- `PushWatchlistConfigVO.java`
- `PushWatchlistConfigAuditVO.java`
- `PushWatchlistConfigAuditDO.java`
- `PushWatchlistConfigAuditMapper.java`
- Push watchlist management API
- `tm_push_watchlist_config_audit` schema
- latest price recheck
- asset-state gate
- stampede guard
- non-execution copy large block
- RuleEngine / PlanBoundary
- OpportunityLog
- TradeReview / ReviewCenter
- RuleImprovement

## 6. P0 Schema Strategy

- P0 does not change schema.
- P0 does not need `tm_push_watchlist_config_audit`.
- P0 does not need push snapshot or push recheck indexes.
- Existing schema residual remains deferred.
- Later config write or audit work may introduce schema changes in a separate track.

## 7. P0 Test Strategy

After implementation, at minimum run:

- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- `WatchlistPushEligibilityServiceImplTest`
- `PushSnapshotService` watchlist gate test
- `PushRecheckServiceImpl` watchlist gate test
- Non-watchlist fail-closed test
- Missing / disabled / invalid / exception config fail-closed tests
- Protection test proving `governance_missed` does not enter push candidates
- Dashboard smoke only as regression, not as the primary P0 Push test

## 8. P0 Risks

- Accidentally treating a default six-symbol list as the watchlist.
- Accidentally treating `governance_missed` as an opportunity push.
- Restoring too many project external files and mixing in RuleEngine, Opportunity, or TradeReview.
- Applying schema first and creating implementation mismatch.
- Expanding too early into config write or audit behavior.

## 9. Next Execution Recommendation

- Commit this plan document first.
- Then enter P0 minimal file recovery plan design.
- Restore only the minimum necessary files or hunks.
- Do not restore the whole project external workspace in one step.
