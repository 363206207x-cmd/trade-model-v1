# Fundamental AI v4.1 PR #1187 Final Target Runtime Closure

Status: `FINAL_HTTP451_CLOSURE_COMPLETE_PENDING_ONE_EXACT_REAUDIT`

## Baseline And Scope

- Main baseline: `b1b49a0de4090fd93a12b14e18c1c980669d0162`
- Final HTTP 451 closure baseline:
  `e82ba8888da596ac67c871b4cb4b03b2ec5191b3`
- Branch: `codex/v4-1-target-runtime-blocker-remediation`
- PR: `#1187`, Draft, open, unmerged
- Authorized package:
  `FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`

## Root-cause Sweep

The sweep covers B01 release/database, B02 every public market-provider entry,
B03 AI readiness, and B04 auth/bootstrap. The complete entry-point/owner/gate/
fail-closed/test mapping is in
`FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_ROOT_CAUSE_MATRIX.md`.

The original provider capability P1 is closed by the existing
`ProviderCapabilityRegistry` becoming the
single pre-call decision owner. Routed primary and fallback, coordinated
OHLCV, provider scan, quote, derivatives, controller, Push Recheck and Position
Monitoring paths are gated. Capability directory calls are separate from data
calls, exact identity is mandatory, and blocked states have zero data calls.

P1-002 is closed by removing all implicit CoinGlass RPM defaults. One canonical
configuration-state contract is reused by runtime service, client, provider
health, preflight and rate budget. Missing data remains null/empty with a typed
fail-closed state; it is never fabricated as zero.

Independent finding `P1-001-451-QUOTE-DERIVATIVES` is closed across the full
production sweep:

- Current price, Binance funding and Binance open interest preserve actual HTTP
  451 as `REGION_RESTRICTED`, write the exact dataset observation before
  returning, and suppress the next exact external call.
- CoinGlass open interest, funding, liquidation and long/short use the same
  classifier and registry write contract.
- OHLCV keeps its existing routed write/gate behavior.
- Push Recheck, Position Monitoring, Decision and dashboard paths consume the
  gated owners and do not create a direct provider bypass.
- Primary fallback requires its own exact `SUPPORTED` decision. Unsupported or
  mismatched fallbacks are not called.
- Funding/OI restrictions remain dataset-specific and never become zero or
  Evidence.

The detailed matrix is
`docs/FUNDAMENTAL_AI_V4_1_PR1187_HTTP451_FINAL_CLOSURE.md`.

## Validation Evidence

| Gate | Result |
|---|---|
| Root-cause matrix | COMPLETE |
| Focused provider/CoinGlass contracts | PASS |
| HTTP 451 focused contracts | 34 tests, PASS |
| B03 AI readiness and B04 auth/bootstrap focused contracts | PASS |
| Full Maven/H2 | 4626 total, 4612 passed, 14 skipped, 0 failed/errors |
| Product Source Gate | PASS |
| Workflow Contract | PASS |
| Authorization validator | PASS |
| Standard Java 17 JAR | PASS |
| JAR Flyway core/PostgreSQL/V1-V13 content | PASS |
| PostgreSQL 16 empty V1-V13 | 13/13 PASS |
| Existing V13 restart | PASS |
| Checksum/migration readiness fail closed | PASS |
| Packaged login/Session/logout | PASS |
| Production implicit CoinGlass RPM default | 0 |
| Production direct provider bypass | 0 |

The ordinary Maven run skipped 14 explicitly environment-gated tests: 12
controlled P3/PostgreSQL actions, one Testcontainers smoke when Docker was not
visible to that Maven process, and one live CoinGlass smoke without its opt-in
environment variable. No HTTP 451 contract was skipped. The required standard
release path was separately executed by the disposable PostgreSQL 16 script:
empty V1-V13, existing V13 restart, checksum/migration fail closed, and packaged
login/Session/logout all passed.

## Frozen Boundaries

- Schema/API product contract/Figma/Desktop/Mobile changed: NO.
- Data Quality algorithm/threshold and Candidate promotion threshold changed:
  NO.
- Market Bias, Opportunity State, Plan Mode, Three-AI authority,
  Candidate/Final isolation, Final/UserPosition isolation changed: NO.
- Fake market/evidence/AI values introduced: NO.
- Automatic open/close/add/reduce/reverse/order/trading capability: 0.
- Live CoinGlass or AI secret used: NO.

## Remaining Gate

PR #1187 must remain Draft. After the final closure commit is pushed and its
exact-head CI passes, one independent HTTP 451 closure re-audit is the next
allowed action. Merge, deployment and live-secret target-runtime acceptance are
outside this task.
