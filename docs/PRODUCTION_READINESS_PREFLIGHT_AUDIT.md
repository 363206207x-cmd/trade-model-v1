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

Important validation note: PostgreSQL Testcontainers smoke is designed to skip when Docker is unavailable. PDR-PF3 later recorded empty PostgreSQL migration evidence as BLOCKED_TIMEOUT after an approximately 1h27m interrupted run. Local production readiness still cannot claim real PostgreSQL migration evidence unless Docker-backed or server-backed migration evidence is supplied.

### P3-H Current Addendum (2026-07-16)

The P3-H offline harness now has deterministic Greenfield Bootstrap, role
separation, fixed non-root Secret materialization, strict attestation,
systemd-credential, runtime-mount, Host-header, TLS-target, and TLS 1.3
contracts. An explicitly enabled disposable local Compose run completed as
`PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE`, including Flyway V1-V7, denied
application writes, Secret non-exposure checks, and cleanup. Round 2 also
proves explicit initialize/steady modes, retained-volume and reboot-like
restarts with zero repeated migrations and matching fingerprints, V2 active
Secret preservation with V1 denied, strict object inventory, exact SSH-line
pinning, exact Git archive image attribution, and injected-failure cleanup.
Round 3 adds confirmed partial-initialization recovery, core/full state
verification, measured cleanup that stops PostgreSQL while preserving its
volume, fail-closed role membership checks, exact default ACL/Sequence
privileges, and post-reboot V2 admin/database success with V1 denial. This is local
template evidence only. No authorized server or real Secret Store was
accessed; `REAL_STAGING_STATUS` remains
`BLOCKED_MISSING_AUTHORIZED_INPUT`, P4 is not allowed, and Production
Deployment Readiness remains `BLOCKED`.

## Production Readiness Decision

Production readiness: BLOCKED.

Recommendation: production deployment cannot proceed.

Reason: the repository is local acceptance-ready and can continue scoped packages, but production deployment still lacks explicit proof for real server deployment, production database migration/rollback, secrets handling, provider live smoke, hardened deployment access, and complete release-gate evidence. No production-ready claim is allowed unless every production gate is explicitly proven.

## Pass / Fail Checklist

| # | Check | Status | Evidence / Finding |
|---|---|---|---|
| 1 | Production profile safety | PASS | `ProductionProfileSafetyGuard` rejects H2 memory DB, blank datasource credentials, enabled H2 console, simulated provider, missing Binance credentials, unsafe admin password, public bind without opt-in, and sensitive actuator exposure. Tests cover these guardrails. |
| 2 | Datasource config and secret handling | PDR-PF5 DONE / PF6 EVIDENCE CURRENT | `application-prod.yml` requires datasource and Binance secrets from environment and `ProductionProfileSafetyGuard` fails closed for missing/unsafe values. PDR-PF5 defines secrets manager, rotation, HTTPS/reverse-proxy, audit/access logging, and rate limiting requirements, but no real server secret injection proof exists yet. PDR-PF6 records provider smoke defaults and no-call evidence without accessing secrets. |
| 3 | Schema migration from empty DB | LIVE4 CONTROLLED PASS | Flyway V1/V2/V3 migrations exist. Operator-provided controlled evidence records disposable local Docker PostgreSQL 16.14, Flyway validated 3 migrations, applied V1/V2/V3, final schema version v3, and `CONTROLLED_POSTGRESQL_FLYWAY_RESULT: PASS`. This proves the empty migration gate only for the disposable controlled DB, not full production readiness. |
| 4 | Schema migration from current main state | LIVE7 CLEAN RESTORE PASS | LIVE7 records PostgreSQL 16 container-native backup `PASS`, restore `PASS_CLEAN`, restored `tm_*` table count `27`, and Flyway success count `3`. This resolves the prior `transaction_timeout` warning for disposable local restore evidence, but remains local/disposable only; production readiness stays BLOCKED. |
| 5 | Scheduler default states | PDR-PF2 POLICY ADDED | PDR-PF2 adds `docs/PRODUCTION_SCHEDULER_POLICY.md`, production default-off scheduler flags in `application-prod.yml`, and `ProductionProfileSafetyGuard` validation for missing scheduler policy/classifications and unsafe opt-in. Production deployment remains BLOCKED until later release-gate evidence. |
| 6 | Position Monitor scheduler default-off | PASS | `trade-model.schedulers.position-monitor.enabled` defaults to false, and `PositionMonitorSchedulerTest` proves the monitor batch does not run unless explicitly enabled. |
| 7 | No auto-open / auto-close / auto-reverse / order execution / auto-trading | PASS | Static and service tests preserve review-only/manual-review/no-order/no-auto-trading boundaries. No production trading behavior was added by the preflight audit. |
| 8 | MarketQuoteClient failure behavior | PASS | `MarketControllerTest.quoteStatusEndpointFailsClosedWhenQuoteMissing` returns `MARKETQUOTE_MISSING_FAIL_CLOSED`, `QUOTE_UNAVAILABLE`, review-only, and not-trading-signal fields. Live provider proof remains missing. |
| 9 | AI provider unavailable / budget blocked / timeout fallback behavior | PASS | `AiUsageGuard` fail-closes disabled, not-configured, rate-limit, budget, and log-unavailable states. `AiDecisionOrchestratorServiceImplTest` covers disabled global fallback, provider failure partial fallback, provider timeout, and overall timeout fallback. |
| 10 | Push Recheck quote unavailable fail-closed behavior | PDR-PF7 PASS SAFETY GUARD | `PushRecheckServiceImpl` maps missing quote paths to `PRICE_REQUIRED` or `QUOTE_UNAVAILABLE` and invalidates review-only. PDR-PF7 adds focused tests for empty quote, null lastPrice, quote exception, missing symbol, and valid currentPrice no-fallback behavior. This is safety evidence only, not production deployment approval. |
| 11 | Review Center readonly behavior | PASS | `ReviewCenterServiceImplTest.emptySourcesReturnEmptyArraysWithoutSyntheticRows` proves empty arrays without synthetic records; mapping test uses existing readonly sources. |
| 12 | Dashboard Home no fake position / no fake review behavior | PASS | `DashboardHomeServiceImplTest` proves open manual UserPosition is the source for home positions and LONG/SHORT PnL is calculated from real UserPosition plus monitor/quote data, without ExecutionPlan-to-position fallback. Review Center tests prove no fake review rows. |
| 13 | Delivery-check and v1-state consistency | PASS | `bash scripts/v1-delivery-check.sh` PASS and `bash scripts/v1-state.sh` PASS on the clean branch before report generation. Both report `MAIN_SYNC: OK`; v1-state reports `PRODUCTION_DEPLOYMENT_READINESS: BLOCKED`. |
| 14 | Production-readiness docs no longer contain stale PDR-only scope | PDR-PF1 CLEANUP | `PROJECT_CURRENT_STATE.md` and `GLOBAL_AUDIT_PROGRESS_REPORT.md` were corrected by #1067. PDR-PF1 updates `ACTIVE_MAINLINE_STATUS.yml`, `CODEX_NEXT_TASK.yml`, and `V1_PROGRESS_SOURCE_OF_TRUTH.md` so PDR-M7 is historical evidence, not the only currently allowed work. |
| 15 | Production readiness remains BLOCKED unless every gate is proven | PASS | Current source-of-truth and delivery matrix still keep `PRODUCTION_DEPLOYMENT_READINESS: BLOCKED`; this audit does not loosen that gate. |

## Blocker List

1. Real production PostgreSQL connection is not proven.
2. Empty PostgreSQL migration evidence is now proven for a disposable local Docker PostgreSQL controlled DB by PDR-LIVE4, but this is one gate only and does not prove current-state migration, rollback, restore, provider, secrets/access, server smoke, or release-owner approval.
3. Current-state migration/restore evidence is improved by LIVE7 PostgreSQL 16-aligned disposable local backup/restore validation, but the evidence is not production-like.
4. Backup evidence is `PASS`; clean restore evidence is `PASS_CLEAN`; restored validation is `PASS` with 27 `tm_*` tables and 3 successful Flyway migrations. Production-grade current-state migration/rollback evidence remains missing.
5. Secrets manager integration and credential rotation are `DOCUMENTED_WITH_PLAN` after LIVE14; real secret-store injection, redacted server-side secret proof, and actual rotation drill evidence remain missing.
6. Real server smoke is `SKIPPED_MISSING_CONTROLLED_SERVER` after LIVE15 because no controlled server endpoint was present and no server was contacted.
7. LIVE16 final conditional readiness review keeps the decision `BLOCKED` / `DO NOT DEPLOY` because the skipped/missing/planned gates cannot be treated as PASS.
8. HTTPS/reverse-proxy hardening is `DOCUMENTED_WITH_CONFIG` after LIVE13 template evidence; LIVE12 moves application-level access logging, auth audit logging, and rate limiting to `GUARD_PASS`, while real HTTPS proxy auth smoke evidence remains `MISSING_EVIDENCE`.
7. Production scheduler policy is addressed by PDR-PF2 guard/config/docs, but production deployment still needs merged evidence and a later release-gate run.
8. Provider proof is partial: LIVE8 records controlled Binance public market data `PASS`; LIVE9 records OpenAI, Gemini, and xAI/Grok as `SKIPPED_MISSING_SECRET`; external-context provider live proof remains missing because keys/configuration were absent and no live external-context harness exists.
9. Push Recheck quote-unavailable behavior is locked by PDR-PF7 focused tests; production readiness still remains blocked until all release gates are proven.
10. PDR-PF8 release-gate closure records the aggregate decision as BLOCKED / DO NOT DEPLOY because migration, rollback, secrets/access, provider live smoke, and release evidence remain incomplete.
11. PDR-PF9/PF10/PF11 evidence identifies Docker/Testcontainers unavailability as the local blocker. LIVE4 records controlled local Docker PostgreSQL Flyway PASS evidence for the empty migration gate only; current-state migration and rollback evidence remain missing.
12. Metrics dashboards, log aggregation, alerting, and operational incident evidence are missing.
13. No completed production release-gate evidence bundle exists.

## Required Remediation Packages

1. `PDR-PF1 Status Source Cleanup`: DONE/effective on merged main; stale production-readiness docs no longer imply old PDR-only scope.
2. `PDR-PF2 Production Scheduler Policy`: DONE/effective on merged main; defines production scheduler defaults, required env overrides, fail-closed guard validation, and Position Monitor default-off status.
3. `PDR-PF3 PostgreSQL Migration Evidence`: DONE/effective on merged main by PR #1071; records migration-file review and BLOCKED_TIMEOUT evidence after an approximately 1h27m interrupted run. Resolve Docker/Testcontainers availability or rerun in a server-backed PostgreSQL environment before claiming PASS.
4. `PDR-PF4 Current-State Migration + Rollback Drill`: DONE/effective on merged main by PR #1072; defines safe backup, restore, current-state migration rehearsal, rollback decision tree, and evidence bundle requirements without production DB access or destructive DB operations.
5. `PDR-PF5 Secrets and Access Hardening`: DONE/effective on merged main by PR #1073; defines existing secret guards, missing hardening evidence, required env vars, secrets manager/rotation plan, HTTPS/reverse-proxy checklist, audit/access logging checklist, rate limiting checklist, and actuator policy without real secret access.
6. `PDR-PF6 Provider Live Smoke Evidence`: DONE/effective on merged main by PR #1074; records provider smoke defaults, safe no-call evidence, result per provider, redaction policy, and remaining blockers. Collect redacted server-side live evidence only in a separately approved safe environment.
7. `PDR-PF7 Push Recheck Quote-Unavailable Guard`: DONE/effective on merged main by PR #1075; adds focused guard tests proving no current price plus unavailable quote writes `QUOTE_UNAVAILABLE` or `PRICE_REQUIRED` and remains review-only/fail-closed.
8. `PDR-PF8 Production Release Gate Closure`: DONE/effective on merged main by PR #1076; aggregates PF1-PF7 evidence and records the release-gate decision as BLOCKED / DO NOT DEPLOY because every production gate is not proven.
9. `PDR-PF9 PostgreSQL Migration Evidence Recovery`: DONE/effective on merged main by PR #1077; bounded targeted smoke completed quickly but skipped because Docker/Testcontainers is unavailable, so result is BLOCKED_ENV_UNAVAILABLE and not PASS.
10. `PDR-PF10 PostgreSQL Environment Provisioning Evidence`: DONE/effective on merged main by PR #1078; Docker CLI and sockets are missing, Testcontainers cannot be considered available, and migration smoke is SKIPPED_ENV_UNAVAILABLE.
11. `PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence`: DONE/effective on merged main by PR #1079; Docker CLI and sockets are still missing, so bounded migration smoke is BLOCKED_ENV_UNAVAILABLE and no Flyway V1/V2/V3 success log exists.
12. `PDR-LIVE1 Controlled Live Dependency Acceptance`: DONE/effective on merged main by PR #1080; controlled DB is SKIPPED_MISSING_CONTROLLED_DB, Binance public smoke is SKIPPED_DISABLED, AI/external providers are SKIPPED_MISSING_SECRET, focused production safety/scheduler/Push Recheck tests pass, and production readiness remains BLOCKED.
13. `PDR-LIVE2 Controlled PostgreSQL Evidence Setup`: DONE/effective on merged main by PR #1081; Docker is DOCKER_MISSING, controlled DB env is SKIPPED_MISSING_CONTROLLED_DB, no-op setup helper is added, no migration runs, and production readiness remains BLOCKED.
14. `PDR-LIVE3 Controlled PostgreSQL Flyway Smoke Runner`: DONE/effective on merged main by PR #1082; guarded runner and test-only external smoke path are added, missing env returns SKIPPED_MISSING_CONTROLLED_DB, and production readiness remains BLOCKED.
15. `PDR-LIVE4 Controlled PostgreSQL Flyway Evidence Run`: DONE/effective on merged main by PR #1083; records operator-provided disposable local PostgreSQL 16.14 Flyway V1/V2/V3 PASS evidence with final schema version v3, while production readiness remains BLOCKED.
16. `PDR-LIVE5 Controlled Current-State Migration + Restore Drill Evidence`: DONE/effective on merged main by PR #1084; adds a guarded no-op/default helper and records backup `SKIPPED_MISSING_CONTROLLED_DB`, restore `SKIPPED_MISSING_RECOVERY_DB`, and current-state migration rehearsal `SKIPPED`.
17. `PDR-LIVE6 Controlled Backup Restore Evidence Run`: DONE/effective on merged main by PR #1085; records backup `PASS`, restore `PASS_WITH_WARNING` due `transaction_timeout`, restored `tm_*` table count `27`, and Flyway success count `3`.
18. `PDR-LIVE7 PostgreSQL 16-aligned Clean Restore Evidence`: DONE/effective on merged main by PR #1086; records PostgreSQL 16 container-native backup `PASS`, restore `PASS_CLEAN`, restored `tm_*` table count `27`, and Flyway success count `3`.
19. `PDR-LIVE8 Controlled Provider Live Smoke Evidence Run`: DONE/effective on merged main by PR #1087; records bounded provider smoke evidence with Binance public `PASS`, AI providers `SKIPPED_MISSING_SECRET`, external context `SKIPPED_MISSING_SECRET`, no secret printing, no order placement, and no external Push send.
20. `PDR-LIVE9 Controlled AI Provider Smoke Evidence Run`: DONE/effective on merged main by PR #1088; records OpenAI/Gemini/xAI skipped due missing secrets, no secret printing, no AI endpoint call, no order placement, and no external Push send.
21. `PDR-LIVE10 Secrets HTTPS Access Logging Rate Limit Evidence`: DONE/effective on merged main by PR #1089; records production guard/auth/actuator/repo-secret hygiene `GUARD_PASS`, secrets manager/access logging/auth audit/rate limit `MISSING_EVIDENCE`, HTTPS/reverse proxy `DOCUMENTED_NOT_EVIDENCED`, and production readiness remains BLOCKED.
22. `PDR-LIVE11 Release Evidence Bundle + Remaining Blockers Closure`: DONE/effective on merged main by PR #1090; aggregates controlled PostgreSQL, provider, AI-provider, and security/access evidence into `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md` and records production readiness as BLOCKED / DO NOT DEPLOY.
23. `PDR-LIVE12 Access Logging Auth Audit Rate Limit Evidence Remediation`: DONE/effective on merged main by PR #1091; adds controlled access logging, authentication audit logging, sensitive-data redaction, rate-limit guard evidence, and prod fail-closed rate-limit validation while production readiness remains BLOCKED.
24. `PDR-LIVE13 HTTPS Reverse Proxy Evidence`: DONE/effective on merged main by PR #1092; records HTTPS/reverse-proxy template evidence as `DOCUMENTED_WITH_CONFIG`, keeps real proxy/auth smoke missing, and keeps production readiness BLOCKED.
25. `PDR-LIVE14 Secrets Manager Credential Rotation Evidence`: DONE/effective on merged main by PR #1093; records secrets manager and credential rotation as `DOCUMENTED_WITH_PLAN`, keeps real secret-store injection and rotation drill evidence missing, and keeps production readiness BLOCKED.
26. `PDR-LIVE15 Real Server Smoke Evidence Plan / Gate`: DONE/effective on merged main by PR #1094; records controlled real-server smoke as `SKIPPED_MISSING_CONTROLLED_SERVER`, adds a safe default-skip smoke wrapper, and keeps production readiness BLOCKED.
27. `PDR-LIVE16 Final Conditional Readiness Review`: DONE/effective on merged main by PR #1095; aggregates LIVE1-LIVE15 evidence and keeps readiness `BLOCKED` / deployment `DO NOT DEPLOY`.
28. `PDR-LIVE17 AI External Provider Release Policy Evidence`: DONE/effective on merged main by PR #1096; records AI/external provider release-policy status and requires release-owner decisions before skipped missing-secret providers can be waived or disabled for a target release.
29. `PDR-LIVE18 Release Owner Decision Register / Waiver Policy`: current package; records the decision register for remaining gates and approves no waiver without explicit owner evidence.

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

The next work can proceed only as a scoped remediation package. PDR-LIVE7 records PostgreSQL 16-aligned clean local restore evidence, but it remains local/disposable only. PDR-LIVE8 records controlled Binance public provider `PASS`, PDR-LIVE9 records exact AI provider skipped reasons due missing secrets, PDR-LIVE10 records secrets/HTTPS/access evidence gaps, PDR-LIVE11 records the current release evidence bundle as BLOCKED / DO NOT DEPLOY, PDR-LIVE15 records real-server smoke as SKIPPED_MISSING_CONTROLLED_SERVER, PDR-LIVE16 keeps the final conditional readiness decision BLOCKED, and PDR-LIVE17 records that missing AI/external providers require release-owner decisions before they can be treated as optional/disabled for a target release, and PDR-LIVE18 records the release-owner decision register while approving no waiver. AI provider PASS evidence or explicit waiver, external provider proof or explicit waiver, real secret-store injection / rotation drill evidence, real HTTPS reverse-proxy smoke, real server smoke PASS evidence, named rollback/incident ownership, and release-owner approval remain incomplete. Proceed only to the next explicitly scoped remediation package such as actual owner decision capture, controlled real-server PASS evidence, or controlled provider/secrets evidence. Runtime trading behavior, order execution, external push sending, fake records, and production-ready claims must remain blocked.
