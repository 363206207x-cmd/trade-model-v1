# Global Audit Gap Closure Progress Report

## Post-1066 Status Closure

- PR #1066 merged into `main`.
- Merge commit: `694c68d8418a207ac54c825f6c8e7e63f0853859`.
- Post-merge local validation passed:
  - `./mvnw test -q` PASS
  - `bash scripts/v1-delivery-check.sh` PASS
  - `V1_STATE_RESULT` PASS
  - `WORKTREE_CLEAN` Yes
  - `MAIN_SYNC` OK
  - `OPEN_PR_STATUS` NONE
  - `BLOCKERS` none
- Review + AI conflict package: current round DONE / usable.
- Position monitor package: current round DONE / usable.
- Project real status: V1 local acceptance-ready, not production-ready.
- Production readiness remains BLOCKED.
- Next business phase allowed: YES.
- Current progress numbers:
  - Review + AI conflict handling: 86%
  - Position monitoring: 83%
  - Overall project real progress: 74%
- Prohibited items remain:
  - no auto-open
  - no auto-close
  - no auto-reverse
  - no order execution
  - no auto-trading
  - no external push send
  - no fake positions
  - no fake review records

## Scope

This package closes selected V1 gaps from the replay / AI conflict scheme and the position monitor scheme. It remains a read-only / review-only trading decision assistant. It does not introduce exchange order execution or external push sending.

## 复盘与 AI 冲突处理完成度

- `tm_rule_config` now has a scheme config contract for `confused_state_config`, `ai_conflict_config`, `push_recheck_config`, `missed_opportunity_config`, and `hot_reset_config`.
- Hot Reset threshold decisions now read the four trigger thresholds from `RuleConfigService` through `RuleConfigContractService`.
- Hot Reset config read failure is fail-closed: it returns `HOT_RESET_CONFIG_NOT_READY` and does not update asset state, invalidate decisions/plans/pushes, or trigger rebuild.
- Push Recheck keeps the existing current-price API and adds a readonly quote fallback through `MarketQuoteClient` when no current price is provided.
- Push Recheck writes explicit `fail_reason_json` codes for `PRICE_REQUIRED`, `QUOTE_UNAVAILABLE`, `DRIFTED`, `INVALIDATED`, `RISK_BLOCKED`, and `CONFUSED_BLOCKED` where applicable.
- Review Center now exposes readonly diagnostics statuses instead of leaving source readiness semantically unknown.

## 持仓监控完成度

- Added a default-off `PositionMonitorScheduler` for batch scanning open user positions only.
- Scheduled position monitoring only calls `monitorOpenUserPositions()` and only writes monitor logs through the existing monitor path.
- `PositionMonitorResultDTO` now stabilizes display-facing fields: `entryLogicStatus`, `directionSupportStatus`, `reversalStatus`, `riskLevel`, `suggestedManualAction`, `pnlPct`, `pnlAmount`, and `accountImpactPct`.
- Dashboard Home now calculates manual-position current price / floating PnL from real monitor log price or readonly `MarketQuoteClient` data.
- LONG and SHORT PnL are calculated separately from real UserPosition fields.
- Leverage is only used for exposure display (`accountImpactPct`) and never creates an action.

## 项目整体真实进度

- V1 remains local/acceptance-ready with production readiness still gated by deployment and release evidence.
- Backend review/readiness semantics are stronger for Hot Reset, Push Recheck, Review Center, and manual position monitoring.
- Full AI conflict scheme remains partially implemented: display and core four-level conflict logic exist, but not every critical scheme layer is fully persisted/replayable end to end.

## 已完成证据

- `RuleConfigContractServiceTest` verifies all five scheme config groups are read from `RuleConfigService` map.
- `HotResetPolicyTest` verifies configured thresholds change Hot Reset trigger behavior.
- `HotResetServiceImplTest` verifies missing Hot Reset config fails closed with no business writes or rebuild.
- `PushRecheckServiceImplTest` verifies missing current price can be resolved through `MarketQuoteClient` and that risk-blocked failures use a canonical fail reason.
- `PositionMonitorSchedulerTest` verifies the scheduler is default-off and only runs the open-position monitor batch when enabled.
- `DashboardHomeServiceImplTest` verifies manual LONG/SHORT position PnL without ExecutionPlan-to-position fallback.
- `ReviewCenterServiceImplTest` verifies Review Center diagnostics expose READY/EMPTY readonly source status.

## 未完成缺口

- Flyway defaults cover the scheme config keys, but existing live databases still need normal migration execution before production use.
- AI conflict and Confused thresholds are contract-seeded and auditable, but deeper runtime rewiring of every score boundary can be split into later packages if stricter dynamic tuning is required.
- Critical-event persistence is present for several flows, but a universal critical event ledger across all 10 scheme layers remains a future backend package.
- Missed Opportunity config is contract-seeded for audit/readiness; deeper threshold-driven evaluation tuning remains future work.

## 不属于 V1 的禁止项

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records

## PR Description Safety Statement

This PR preserves the V1 safety boundary: no auto-open, no auto-close, no auto-reverse, no order execution, no auto-trading, no external push send, no fake positions, and no fake review records.
