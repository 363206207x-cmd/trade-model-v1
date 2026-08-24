# TRINE LOGIC v4.1 Production Readiness Evidence

Evidence package: Production Readiness Final Gate excluding a CoinGlass live call.

> **Current correction:** this package is historical evidence for Head
> `a39c3979`. The follow-up implementation/evidence is indexed at
> `docs/evidence/non_coinglass_blocker_closure/README.md`. Telegram identity is
> authorized, Binance is disabled for release, the controlled Three-AI harness
> now starts and completes with partial real-provider results, and Gemini is
> blocked by account/location/region policy. Remote P3H staging remains
> unavailable. Current status: `PRODUCTION_READINESS=BLOCKED_MULTIPLE`.

- Implementation Head: `a39c3979f57f31e61ff56924c0135dce8570a44f`
- Final evidence Head: the exact PR #1195 Head containing this file; recorded in the PR description and final audit response after the single evidence commit
- PR: `#1195`, branch `codex/frontend-interaction-runtime-closure`, base `2698ca50ae2a9e125e4848865c3151f1adade4a3`
- Release scope: `PRIVATE_SINGLE_USER_WEB`
- CoinGlass: `NOT_EXECUTED_MISSING_PRIVATE_KEY`; no CoinGlass live endpoint was called
- Evidence date: 2026-08-23 Asia/Shanghai

## Evidence Labels

- `LOCAL`: isolated local application runtime.
- `UI_REVIEW_FIXTURE`: rendering/mapping proof only; not real data.
- `AUTOMATED_TEST`: deterministic contract or regression test.
- `LIVE_PROVIDER`: real provider call with sanitized output.
- `NOT_VERIFIED`: required target-runtime evidence was unavailable.
- `NOT_EXECUTED_KNOWN_GAP`: deliberately excluded CoinGlass live call.

## Changed-File Audit

At the implementation Head, `origin/main...HEAD` contains 252 files: 49 production, 38 tests, 158 docs (148 existing evidence files), and 7 other workflow/config files. No schema/Flyway, authentication production class, mobile-only file, automatic-trading file, exchange-write file, or legacy dashboard implementation file changed. One Telegram production class changed: `HighValueAlertMessageService` clears `currentRecheckId` in two message construction paths. It does not add delivery capability, but remains an out-of-release-scope production modification and is reported as P1.

Generated inventory: `changed-files-at-implementation-head.txt`.

## Build And Test

| Evidence | Result |
|---|---|
| Java 17 compile | PASS |
| Directed contract tests | 159 passed, 0 failed, 0 skipped |
| Timestamp Node matrix | PASS |
| Frontend state matrix | PASS |
| JavaScript syntax | PASS |
| Product Source Gate | PASS |
| Workflow Contract | PASS |
| `git diff --check` | PASS |
| Local full Maven | 4,786 tests, 0 failures, 0 errors, 14 skipped |
| Implementation-Head CI profile | 938 tests, 0 failures/errors/skips |
| GitHub required categories at implementation Head | `quality-gate` PASS, `workflow-contract` PASS |

The 14 local skips are controlled Docker/PostgreSQL/CoinGlass or fingerprint fixtures. PostgreSQL was validated separately with the standard release smoke; CoinGlass was not called.

## Normal Runtime

`LOCAL` / H2 in-memory:

- `./scripts/run-local.sh` started successfully with Java 17.
- Existing login, failed login, CSRF logout, and post-logout denial passed.
- `/dashboard`, `/positions`, `/analysis`, `/messages`, and `/me` returned authenticated HTTP 200.
- Unknown position API returned 404.
- Home returned zero assets and zero positions, null Header/Status data time, and empty execution-plan state; no fixture IDs or fake Final/evidence/position appeared.
- Browser console error count 0; horizontal overflow count 0 at 1080 and 1440.
- H2 success is not PostgreSQL production proof.

Screenshot: `normal-runtime-home-1440.png` (`LOCAL`).

## UI-Review Isolation

`UI_REVIEW_FIXTURE`:

- `./scripts/run-local.sh --ui-review` logged `UI_REVIEW_FIXTURE=ENABLED`.
- Home Top3, all three active positions, details 7101/7102/7103, unknown-ID 404, and O07 binding were verified.
- No manual-close POST and no external provider call occurred.
- The fixture screenshot is rendering/mapping evidence only and is not live-provider or production-data proof.

Screenshot: `ui-review-fixture-home-1440.png` (`UI_REVIEW_FIXTURE`).

## User Paths

No explicitly authorized staging URL, account, or database identity was available. Selection, Preview, formal analysis, persistence across restart, cross-user runtime, and controlled full-close staging E2E are `NOT_VERIFIED`. Automated owner-scope and interaction tests passed but do not replace staging runtime evidence.

## Market Data

`LIVE_PROVIDER` / isolated local-real database:

- Kraken persisted 2,424 closed OHLCV bars across six assets and four timeframes; latest closed-bar time was present and classified fresh.
- A restart with providers and schedulers disabled preserved the 2,424 bars. Header time, Status time, and persisted latest closed-bar time were identical and carried an explicit offset.
- Restart external HTTP marker count was 0; startup time did not replace market-data time.
- Binance public futures time returned HTTP 451 in this environment. Aggregate enabled-provider readiness remained degraded and no analysis completed.
- CoinGlass was not called.

Therefore `LIVE_MARKET_DATA_WITHOUT_COINGLASS=BLOCKED`, despite the Kraken/timestamp sub-path passing.

## Three-AI Providers

`LIVE_PROVIDER` single-provider smokes:

| Provider | Result | Latency | Notes |
|---|---:|---:|---|
| OpenAI/GPT | PASS | 6,040 ms | 2xx, parsed, usage and request ID present |
| Gemini | BLOCKED | 1,735 ms | HTTP 400 invalid request; no parsed output |
| xAI/Grok | PASS | 6,697 ms | 2xx, parsed, usage and request ID present |

The controlled parallel application orchestrator stopped during Spring database initialization before any provider call. It produced no analysis/trace lineage. Thus independent OpenAI and xAI connectivity passed, but `THREE_AI_PROVIDER_CALLS` and `THREE_AI_NON_COINGLASS_LINEAGE` are blocked. Preview live runtime was not verified. External news/macro/ETF/context remains `OWNER_DECISION_REQUIRED`.

## Recheck

Normal runtime contained no legitimate in-app `PUSH_SNAPSHOT` Message. No raw `pushId` was fabricated. `RECHECK_REAL_PATH=NOT_VERIFIED_NO_LEGAL_SOURCE`; an Owner release decision is required.

## PostgreSQL And Flyway

`LOCAL` disposable PostgreSQL 16 / standard release JAR:

- Flyway V1 to V14: 14/14 PASS.
- Existing V14 restart: PASS.
- Session auth, form login, CSRF, and production smoke: PASS.
- Checksum and migration failures failed closed.

No authorized staging copy, upgrade migration, full release-object write audit, or least-privilege runtime-role proof was available. `POSTGRESQL_MIGRATION=BLOCKED_STAGING_UPGRADE_AND_LEAST_PRIVILEGE_NOT_VERIFIED`.

## Backup, Restore, And Rollback

No authorized staging source/recovery database was configured. Backup and restore scripts were inspected but not executed against a target. `DATABASE_BACKUP`, `DATABASE_RESTORE`, and rollback drill are `NOT_VERIFIED`.

## Server, HTTPS, And Session

No authorized staging server or HTTPS entry point was configured. Local HTTP authentication and CSRF passed, but reverse proxy, TLS, secure cookie behavior, rate limiting through the target proxy, service restart, and target persistence are `NOT_VERIFIED`.

## Secret Management

- Tracked high-confidence secret candidate count: 0.
- Private runtime secret value occurrences in temporary audit logs: 0.
- Tracked private-key files: 0.
- Credentials were loaded without printing values.
- Target secret injection and credential rotation drill: `NOT_VERIFIED`.

## Schedulers And Freshness

Production schedulers default disabled and require explicit environment authorization. One local Kraken OHLCV ingestion cycle passed. No authorized staging cycles existed for analysis refresh, position monitoring, internal messages, or complete Header freshness. No automatic-trading scheduler was enabled.

## Observability

Local health/readiness, application/provider/database logging, trace lookup code, and redaction tests are present. Real-server disk/volume visibility, backup result visibility, incident procedure execution, and target scheduler failure visibility are `NOT_VERIFIED`.

## Legacy Routes

Formal Home navigation links only `/dashboard`, `/positions`, `/analysis`, `/messages`, `/me`, and approved supporting routes. It links none of the legacy routes. Anonymous requests to every listed formal and legacy route redirected to login. Legacy pages remain in the repository and their release policy requires Owner decision.

## Owner Decisions

Missing explicit records: release owner, target server, deployment window, rollback owner, incident owner, Recheck policy, external-provider policy, legacy-route policy, and final Go/No-Go.

## Consolidated Blockers

1. P1: One Telegram production class is modified although Telegram is outside this release scope.
2. P1: Binance public provider returns HTTP 451; aggregate live market readiness remains degraded.
3. P1: Gemini live request returns HTTP 400.
4. P1: Three-AI orchestrator/lineage harness fails during database initialization.
5. P1: No authorized staging user-path and full-close E2E.
6. P1: No legitimate in-app Recheck source path.
7. P1: No staging PostgreSQL upgrade/least-privilege proof.
8. P1: No staging backup/restore/rollback drill.
9. P1: No authorized real server, HTTPS/proxy/session proof.
10. P1: No target secret injection or rotation drill.
11. P1: Required staging scheduler cycles and production observability remain unverified.
12. P1: Required release-owner decisions are absent.
13. Known external gap: CoinGlass private key missing; live call, freshness, persistence, and AI consumption not executed.

P0 findings: none. No fake data was accepted as live evidence, and automatic-trading capability count remains 0.

## Decision

- `NON_COINGLASS_READINESS=BLOCKED`
- `PRODUCTION_READINESS_AUDIT_DONE=YES`
- Historical value at the audited Head: `PRODUCTION_READINESS=BLOCKED_BY_COINGLASS_PRIVATE_KEY`
- Superseding current value: `PRODUCTION_READINESS=BLOCKED_MULTIPLE`
- Additional non-CoinGlass P1 blockers are listed above.
- `DEPLOYMENT_ALLOWED=NO`
- `CURRENT_PHASE_DONE=NO`
- `MERGE=NO`
