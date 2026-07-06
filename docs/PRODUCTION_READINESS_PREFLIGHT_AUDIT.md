# Production Readiness Preflight Audit

Audit date: 2026-07-07
Branch: `codex/production-readiness-preflight-audit`
Current main commit audited: `815fef522f379864a07a8df487e24affa0631be2`
Current main evidence: PR #1066 and PR #1067 are merged into `main`.

## Current Validated Status

Preflight validation was run from a clean branch created from the current `main` commit above.

- `./mvnw test -q`: PASS
- `bash scripts/v1-delivery-check.sh`: PASS
- `bash scripts/v1-state.sh`: PASS
- `WORKTREE_CLEAN`: Yes before report generation
- `MAIN_SYNC`: OK
- `OPEN_PR_STATUS`: NONE before this audit PR
- `NEXT_BUSINESS_PHASE_ALLOWED`: YES
- `CAN_CONTINUE_NEXT_PACKAGE`: YES
- `BLOCKERS`: none for starting the next scoped package
- `PRODUCTION_DEPLOYMENT_READINESS`: BLOCKED

Important validation note: PostgreSQL Testcontainers smoke is designed to skip when Docker is unavailable. The full Maven run passed, but local production readiness still cannot claim real PostgreSQL migration evidence unless Docker-backed or server-backed migration evidence is supplied.

## Production Readiness Decision

Production readiness: BLOCKED.

Recommendation: production deployment cannot proceed.

Reason: the repository is local acceptance-ready and can continue scoped packages, but production deployment still lacks explicit proof for real server deployment, production database migration/rollback, secrets handling, provider live smoke, hardened deployment access, and complete release-gate evidence. No production-ready claim is allowed unless every production gate is explicitly proven.

## Pass / Fail Checklist

| # | Check | Status | Evidence / Finding |
|---|---|---|---|
| 1 | Production profile safety | PASS | `ProductionProfileSafetyGuard` rejects H2 memory DB, blank datasource credentials, enabled H2 console, simulated provider, missing Binance credentials, unsafe admin password, public bind without opt-in, and sensitive actuator exposure. Tests cover these guardrails. |
| 2 | Datasource config and secret handling | FAIL | `application-prod.yml` requires datasource and Binance secrets from environment, which is good, but there is no secrets manager integration, credential rotation evidence, or real server secret injection proof. |
| 3 | Schema migration from empty DB | PARTIAL | Flyway V1/V2/V3 migrations exist and `PostgreSqlFlywayMigrationSmokeTest` validates empty PostgreSQL migration when Docker/Testcontainers is available. Current local run skipped Docker, so production gate remains unproven. |
| 4 | Schema migration from current main state | FAIL | There is no completed migration-from-existing-live-state evidence, no real current production DB baseline, no backfill/compatibility rehearsal, and no restore drill. |
| 5 | Scheduler default states | PDR-PF2 POLICY ADDED | PDR-PF2 adds `docs/PRODUCTION_SCHEDULER_POLICY.md`, production default-off scheduler flags in `application-prod.yml`, and `ProductionProfileSafetyGuard` validation for missing scheduler policy/classifications and unsafe opt-in. Production deployment remains BLOCKED until later release-gate evidence. |
| 6 | Position Monitor scheduler default-off | PASS | `trade-model.schedulers.position-monitor.enabled` defaults to false, and `PositionMonitorSchedulerTest` proves the monitor batch does not run unless explicitly enabled. |
| 7 | No auto-open / auto-close / auto-reverse / order execution / auto-trading | PASS | Static and service tests preserve review-only/manual-review/no-order/no-auto-trading boundaries. No production trading behavior was added by the preflight audit. |
| 8 | MarketQuoteClient failure behavior | PASS | `MarketControllerTest.quoteStatusEndpointFailsClosedWhenQuoteMissing` returns `MARKETQUOTE_MISSING_FAIL_CLOSED`, `QUOTE_UNAVAILABLE`, review-only, and not-trading-signal fields. Live provider proof remains missing. |
| 9 | AI provider unavailable / budget blocked / timeout fallback behavior | PASS | `AiUsageGuard` fail-closes disabled, not-configured, rate-limit, budget, and log-unavailable states. `AiDecisionOrchestratorServiceImplTest` covers disabled global fallback, provider failure partial fallback, provider timeout, and overall timeout fallback. |
| 10 | Push Recheck quote unavailable fail-closed behavior | PARTIAL | `PushRecheckServiceImpl` maps missing quote paths to `PRICE_REQUIRED` or `QUOTE_UNAVAILABLE` and invalidates review-only. Tests cover quote fallback success and invalid current price; add a focused quote-unavailable test before production release. |
| 11 | Review Center readonly behavior | PASS | `ReviewCenterServiceImplTest.emptySourcesReturnEmptyArraysWithoutSyntheticRows` proves empty arrays without synthetic records; mapping test uses existing readonly sources. |
| 12 | Dashboard Home no fake position / no fake review behavior | PASS | `DashboardHomeServiceImplTest` proves open manual UserPosition is the source for home positions and LONG/SHORT PnL is calculated from real UserPosition plus monitor/quote data, without ExecutionPlan-to-position fallback. Review Center tests prove no fake review rows. |
| 13 | Delivery-check and v1-state consistency | PASS | `bash scripts/v1-delivery-check.sh` PASS and `bash scripts/v1-state.sh` PASS on the clean branch before report generation. Both report `MAIN_SYNC: OK`; v1-state reports `PRODUCTION_DEPLOYMENT_READINESS: BLOCKED`. |
| 14 | Production-readiness docs no longer contain stale PDR-only scope | PDR-PF1 CLEANUP | `PROJECT_CURRENT_STATE.md` and `GLOBAL_AUDIT_PROGRESS_REPORT.md` were corrected by #1067. PDR-PF1 updates `ACTIVE_MAINLINE_STATUS.yml`, `CODEX_NEXT_TASK.yml`, and `V1_PROGRESS_SOURCE_OF_TRUTH.md` so PDR-M7 is historical evidence, not the only currently allowed work. |
| 15 | Production readiness remains BLOCKED unless every gate is proven | PASS | Current source-of-truth and delivery matrix still keep `PRODUCTION_DEPLOYMENT_READINESS: BLOCKED`; this audit does not loosen that gate. |

## Blocker List

1. Real production PostgreSQL connection is not proven.
2. Empty PostgreSQL migration evidence is conditional on Docker/Testcontainers or real server execution; current local run cannot prove it.
3. Migration from current main/live state is not rehearsed and has no rollback evidence.
4. Backup and restore drill evidence is missing.
5. Secrets manager integration, credential rotation, and redacted server-side secret injection evidence are missing.
6. HTTPS/reverse-proxy hardening, audit logging, rate limiting, and real server auth smoke evidence are missing.
7. Production scheduler policy is addressed by PDR-PF2 guard/config/docs, but production deployment still needs merged evidence and a later release-gate run.
8. Live provider proof is missing for Binance public market data and optional AI/external-context providers.
9. Push Recheck quote-unavailable behavior needs a focused test-only guard, even though implementation fail-closes.
11. Metrics dashboards, log aggregation, alerting, and operational incident evidence are missing.
12. No completed production release-gate evidence bundle exists.

## Required Remediation Packages

1. `PDR-PF1 Status Source Cleanup`: DONE/effective on merged main; stale production-readiness docs no longer imply old PDR-only scope.
2. `PDR-PF2 Production Scheduler Policy`: current package; defines production scheduler defaults, required env overrides, fail-closed guard validation, and Position Monitor default-off status.
3. `PDR-PF3 PostgreSQL Migration Evidence`: run Flyway migrations against empty PostgreSQL with retained logs and prove V1/V2/V3 success.
4. `PDR-PF4 Current-State Migration + Rollback Drill`: rehearse migration from a current-main-like database state, including backup, restore, and rollback verification evidence.
5. `PDR-PF5 Secrets and Access Hardening`: add or document secrets manager integration, credential rotation, HTTPS/reverse-proxy hardening, audit logging, and rate limiting.
6. `PDR-PF6 Provider Live Smoke Evidence`: collect redacted server-side evidence for Binance public data and any explicitly enabled AI/external providers.
7. `PDR-PF7 Push Recheck Quote-Unavailable Guard`: add a focused test-only guard proving no current price plus unavailable quote writes `QUOTE_UNAVAILABLE` and remains review-only/fail-closed.
8. `PDR-PF8 Production Release Gate Closure`: only after all evidence is present, run the release-gate review and decide whether production readiness can move out of BLOCKED.

## Prohibited Items

The following remain prohibited in V1 and in all production-readiness packages unless a future explicit contract changes the scope:

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim without complete release-gate evidence

## Final Recommendation

Production deployment should not proceed.

The next work can proceed only as a scoped remediation package. After PDR-PF2 is merged/effective, the safest next package is `PDR-PF3 PostgreSQL Migration Evidence`, followed by current-state migration/rollback evidence. Runtime trading behavior, order execution, external push sending, fake records, and production-ready claims must remain blocked.
