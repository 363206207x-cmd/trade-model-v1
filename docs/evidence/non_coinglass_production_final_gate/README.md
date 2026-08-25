# Non-CoinGlass Production Final Gate Evidence

Date: 2026-08-23
Start Head: `b12e0796d77f3d2b5db59eadc4ac0ff8c5bf2d81`
Implementation Head: `2ec8e649039a37af99bc0fbc17930774206670cf`
Audited Evidence Baseline: `95a4b4ad0e18cf6141ab7a01537e69c45c8ea067`
Docs-only Final Head: recorded after commit in the PR Description and final task result; not self-referenced here
PR #1195: open, Draft, unmerged

## Scope and change

Phase A changes only the production-safe Kraken defaults, their contract tests,
and the superseding audit wording. It does not change Provider selection,
market logic, AI semantics, schema, UI, Telegram, Position Monitoring, or any
trading capability.

Changed files in the Phase A implementation and evidence package:

- `src/main/resources/application-prod.yml`
- `src/test/java/org/example/trademodel/config/TargetRuntimePreflightTest.java`
- `src/test/java/org/example/trademodel/config/ProductionProfileSafetyGuardTest.java`
- `docs/TRINE_LOGIC_V4_1_NON_COINGLASS_BLOCKER_CLOSURE.md`
- `docs/TRINE_LOGIC_V4_1_PRODUCTION_READINESS_WITHOUT_COINGLASS_AUDIT.md`
- `docs/evidence/non_coinglass_blocker_closure/README.md`
- `docs/evidence/non_coinglass_production_final_gate/README.md`
- `docs/RELEASE_OWNER_DECISION_REGISTER.md`
- `docs/FUNDAMENTAL_AI_V4_1_GLOBAL_ALIGNMENT_MATRIX.md`

| Setting | Before | After |
|---|---:|---:|
| `TRADE_MODEL_KRAKEN_OHLCV_ENABLED` default | `true` | `false` |
| `TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED` default | `true` | `false` |
| `TRADE_MODEL_BINANCE_OHLCV_ENABLED` default | `false` | `false` |
| `TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED` default | `false` | `false` |
| `TRADE_MODEL_OHLCV_FALLBACK_ENABLED` default | `false` | `false` |

An authorized deployment must explicitly inject:

```text
TRADE_MODEL_KRAKEN_OHLCV_ENABLED=true
TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED=true
TRADE_MODEL_BINANCE_OHLCV_ENABLED=false
TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED=false
TRADE_MODEL_OHLCV_FALLBACK_ENABLED=false
```

No secret value is part of this contract.

## Phase A contract matrix

| Contract | Evidence type | Result |
|---|---|---|
| All prod OHLCV external paths default off | `AUTOMATED_TEST` | PASS |
| Missing either Kraken opt-in blocks preflight/ingestion | `AUTOMATED_TEST` | PASS |
| Both explicit Kraken flags permit the release provider gate | `AUTOMATED_TEST` | PASS |
| Either Binance release flag blocks | `AUTOMATED_TEST` | PASS |
| Disabled scheduler performs zero Provider calls | `AUTOMATED_TEST` | PASS |
| Kraken failure fails closed with zero Binance calls | `AUTOMATED_TEST` | PASS |
| Existing persisted Kraken/live evidence | `LOCAL` / `LIVE_PROVIDER` | RETAINED, not restated as staging |
| Gemini endpoint/model/client | `LOCKED_REGRESSION` | unchanged |
| Gemini live connectivity | `LIVE_PROVIDER` | BLOCKED_ACCOUNT_OR_REGION |
| CoinGlass live path | `NOT_EXECUTED` | missing private key |

## Validation accounting

- Directed tests (`LOCAL`): 288 tests, 0 failures, 0 errors, 0 skipped.
- Full Maven (`LOCAL`): 4,793 tests, 0 failures, 0 errors, 14 controlled skips.
- Preserved implementation-Head CI at `2ec8e649`: 938 tests, 0 failures, 0 errors, 0 skipped.
- Docs-only Final Head exact CI is recorded from its actual GitHub workflow log
  in the PR Description and final task result after this document is committed.
- Required CI categories are `quality-gate` and `workflow-contract`;
  duplicate `quality-gate` runs count as one category.
- Product Source Gate: PASS before modification; rerun on final clean Head.
- Workflow Contract: rerun on final clean Head.
- `git diff --check`: rerun on final clean Head.

## Authorized staging check

Only configuration presence and validity may be inspected. No P3H value,
secret, path content, credential, cookie, or authorization header is recorded.
All 13 required remote staging inputs were not set, so
no deploy, mutation, remote database action, or target runtime claim is made.

| Gate | Status | Evidence type |
|---|---|---|
| Staging authorization | `NOT_VERIFIED_MISSING_CONFIGURATION` | `NOT_VERIFIED` |
| Kraken staging runtime | `NOT_VERIFIED` | `NOT_VERIFIED` |
| Staging user paths/full close | `NOT_VERIFIED` | `NOT_VERIFIED` |
| Legal-source Recheck runtime | `NOT_VERIFIED_NO_LEGAL_SOURCE` | `NOT_VERIFIED` |
| PostgreSQL staging upgrade/least privilege | `NOT_VERIFIED` | `NOT_VERIFIED` |
| Backup/restore/rollback | `NOT_VERIFIED` | `NOT_VERIFIED` |
| HTTPS/session/secrets/schedulers/observability | `NOT_VERIFIED` | `NOT_VERIFIED` |

Repair loop used: `NO`.

## Final status

```text
IMPLEMENTED_ON_DRAFT_BRANCH=YES
EFFECTIVE_ON_MAIN=NO
KRAKEN_SAFE_DEFAULT_IMPLEMENTATION=PASS_PRESERVED
KRAKEN_PROD_DEFAULT=DISABLED
KRAKEN_RELEASE_REQUIREMENT=EXPLICIT_DEPLOYMENT_INJECTION
KRAKEN_RELEASE_SOURCE_POLICY=PASS
KRAKEN_LOCAL_RUNTIME=PASS_LOCAL
KRAKEN_STAGING_RUNTIME=NOT_VERIFIED
BINANCE_RELEASE_POLICY=DISABLED_DUE_451
OPENAI_LIVE_CALL=PASS
GEMINI_LIVE_CALL=BLOCKED_ACCOUNT_OR_REGION
XAI_LIVE_CALL=PASS
THREE_AI_PROVIDER_CALLS=PARTIAL
THREE_AI_PROVIDER_CONNECTIVITY=PARTIAL
THREE_AI_NON_COINGLASS_LINEAGE=BLOCKED_GEMINI_ACCOUNT_OR_REGION
THREE_AI_COMPLETE_RELEASE_CHAIN=BLOCKED_BY_GEMINI_AND_COINGLASS
STAGING_AUTHORIZATION=NOT_VERIFIED_MISSING_CONFIGURATION
POSITION_FULL_CLOSE_E2E=NOT_VERIFIED_NO_AUTHORIZED_STAGING
RECHECK_REAL_PATH=NOT_VERIFIED_NO_LEGAL_SOURCE
POSTGRESQL_STAGING_UPGRADE=NOT_VERIFIED
POSTGRESQL_LEAST_PRIVILEGE=NOT_VERIFIED
DATABASE_BACKUP=NOT_VERIFIED
DATABASE_RESTORE=NOT_VERIFIED
HTTPS_PROXY_SESSION=NOT_VERIFIED
SECRET_INJECTION=NOT_VERIFIED
SECRET_ROTATION_DRILL=NOT_VERIFIED
SCHEDULER_STAGING_RUNTIME=NOT_VERIFIED
OBSERVABILITY_STAGING_RUNTIME=NOT_VERIFIED
COINGLASS_LIVE_CALL=NOT_EXECUTED_MISSING_PRIVATE_KEY
COINGLASS_SNAPSHOT_FRESHNESS=NOT_VERIFIED
COINGLASS_AI_RUN_CONSUMPTION=NOT_VERIFIED
NON_COINGLASS_READINESS=BLOCKED
PRODUCTION_READINESS=BLOCKED_MULTIPLE
CURRENT_PHASE_DONE=NO
MERGE=NO
DEPLOYMENT_ALLOWED=NO
```
