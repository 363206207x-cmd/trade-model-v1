# PHASE_P11B_PRODUCTION_DRY_RUN_VERIFICATION_PLAN

## 1. Document Purpose

This document defines the P11B production-like dry-run verification plan for Trade Model V1.

The goal is to validate fallback, review-only, and fail-closed behavior in a near-production scenario without connecting external APIs, generating executable plans, or producing automated trading actions.

This document is planning and verification guidance only.

It does not modify:

- DTOs
- SourceTrace
- DerivativesRiskContext
- BoundaryCandidateService
- ExecutionPlan readiness
- RiskActionGuard
- Push / Recheck / Watchlist behavior
- schema
- dashboard
- config

It does not connect:

- Coinglass
- external derivative APIs
- order APIs

It does not generate:

- actual ExecutionPlan execution
- order placement
- automated opening
- automated closing
- automated reversing

## 2. Dry-Run Goal

P11B validates whether the already implemented safety chain behaves correctly under near-production inputs:

- SourceTrace complete / missing variants
- DerivativesRiskContext complete / missing variants
- BoundaryCandidateService VALID / fallback variants
- ExecutionPlan display review-only / fallback variants
- RiskActionGuard safe / fail-closed variants
- Push / Recheck / Watchlist non-trading variants

The dry-run must prove that:

- missing sources cannot produce a trade-ready state
- high-risk sources cannot produce a trade-ready state
- BoundaryCandidate `VALID` remains review-only
- ExecutionPlan remains advisory / display-only unless future verified gates explicitly allow otherwise
- Push / Recheck / Watchlist cannot trigger real trading through naming such as `VALID_EXECUTABLE` or `valid=true`

## 3. Input Fixture Strategy

P11B should use local fixtures, mocks, or controlled test objects only.

Allowed fixture sources:

- in-memory DTO builders in tests
- local test JSON fixtures
- mock SourceTraceDTO
- mock DerivativesRiskContextDTO
- mock RuntimeKlineContextDTO
- mock RiskActionGuard state
- mock Push / Recheck records

Forbidden fixture sources:

- Coinglass network data
- live exchange derivatives APIs
- live liquidation feeds
- live order endpoints
- production order state mutation

## 4. Verification Scope

| Area | Required Verification | Expected Result |
|---|---|---|
| BoundaryCandidateService | SourceTrace complete and RiskActionGuard safe | May return BoundaryCandidate `VALID`, but only as review candidate. |
| BoundaryCandidateService | entry / stop / TP / RR source missing | Must return `INCOMPLETE` or `WATCH_ONLY`; must not infer VALID. |
| BoundaryCandidateService | liquidity / event / wick / multi-timeframe source missing | Must return `WATCH_ONLY` or safe fail-closed fallback. |
| SourceTrace | all boundary sources complete | `hasRequiredBoundarySources()` may pass. |
| SourceTrace | any required source missing | completeness check must fail. |
| DerivativesRiskContext | OI / Funding / liquidation / leverage / long-short source missing | Must mark missing fields and force fallback in consumers. |
| ExecutionPlanDisplayAdapter | complete source + safe guard | May render advisory / review-only display. |
| ExecutionPlanDisplayAdapter | missing source | Must remain `INCOMPLETE`, `WATCH_ONLY`, or review-only fallback. |
| RiskActionGuard | high-risk / stampede / liquidity missing | Must fail closed or force review-only fallback. |
| Push / Recheck / Watchlist | positive recheck wording present | Must not create order, executable plan, or auto-trading action. |

## 5. BoundaryCandidateService Dry-Run Cases

| Case | SourceTrace | DerivativesRiskContext | RiskActionGuard | Expected BoundaryCandidate Result |
|---|---|---|---|---|
| Complete safe case | complete | complete or non-blocking | safe | `VALID`, `manualReviewRequired=true`, `notTradeInstruction=true` |
| Missing entry source | missing entry | any | safe or unknown | `INCOMPLETE` / `WATCH_ONLY` |
| Missing stop source | missing stop | any | safe or unknown | `INCOMPLETE` / `WATCH_ONLY` |
| Missing TP source | missing TP | any | safe or unknown | `INCOMPLETE` / `WATCH_ONLY` |
| Missing RR source | missing RR | any | safe or unknown | `INCOMPLETE` / `WATCH_ONLY` |
| Missing liquidity source | complete boundary source | missing liquidity | unknown / fail-closed | `WATCH_ONLY` / safe fail-closed |
| Event blocker active | complete boundary source | event active | fail-closed | `WATCH_ONLY` / safe fail-closed |
| Wick-only uncertainty | complete boundary source | wick not confirmed | fail-closed | `WATCH_ONLY` |
| Stampede state | complete boundary source | high stress | fail-closed | no opportunity semantics; fallback only |

BoundaryCandidate `VALID` must never mean:

- open position
- close position
- reverse position
- place order
- executable plan

## 6. SourceTrace / DerivativesRiskContext Traceability Checks

P11B should verify traceability for:

- entry numeric source
- stop numeric source
- TP numeric source
- RR source
- liquidity source
- multi-timeframe source
- event window blocker
- wick confirmation source
- OI history
- Funding history
- liquidation cluster
- leverage distribution
- long / short ratio
- liquidity stress

Missing data must be explicitly represented as missing, unknown, incomplete, or safe fail-closed.

Missing data must not be silently converted into:

- positive score
- execution readiness
- opportunity push
- trade instruction

## 7. ExecutionPlan Display And Readiness Checks

ExecutionPlan dry-run verification must confirm:

- display-only is not executable
- DTO-only state is not executable
- missing SourceTrace is not executable
- missing DerivativesRiskContext is not executable
- RiskActionGuard fail-closed state is not executable
- complete source plus safe guard remains advisory / review-only unless future gates explicitly change it

Expected fallback outputs:

| Missing / Risk Area | Required ExecutionPlan Behavior |
|---|---|
| entry source missing | `INCOMPLETE` / review-only |
| stop source missing | `INCOMPLETE` / review-only |
| TP source missing | `INCOMPLETE` / review-only |
| RR missing | `WATCH_ONLY` / review-only |
| liquidity source missing | `SAFE_FAIL_CLOSED_ONLY` / review-only |
| event blocker unknown | `WATCH_ONLY` / safe fail-closed |
| wick confirmation unknown | `WATCH_ONLY` / safe fail-closed |
| high-risk guard triggered | fail-closed / review-only |
| stampede state | block new-open / reverse / opportunity semantics |

## 8. RiskActionGuard Dry-Run Checks

RiskActionGuard must preserve:

- high risk does not mean direct stop
- high risk does not mean direct reverse
- wick / spike does not mean trend reversal
- stampede state blocks new-open, reverse, and opportunity push semantics
- liquidity worsening does not mean one-shot market close
- complete source plus high risk still remains review-only

Required high-risk scenarios:

| Scenario | Expected Behavior |
|---|---|
| liquidity stress high | fail closed or review-only fallback |
| liquidation cluster missing | safe fail-closed |
| leverage distribution missing | safe fail-closed |
| long / short ratio missing | safe fail-closed or watch-only |
| event window active | block or downgrade to watch-only |
| wick-only signal | no trend reversal inference |
| stampede state | no new-open, reverse, or opportunity push |

## 9. Push / Recheck / Watchlist Dry-Run Checks

Push / Recheck / Watchlist must not treat naming as execution permission.

P11B should verify:

- `VALID_EXECUTABLE` does not trigger order placement
- `RECHECK_VALID_EXECUTABLE` does not trigger order placement
- `valid=true` does not mean executable trading permission
- `PASS` review tag does not bypass SourceTrace or RiskActionGuard
- `successCount` does not mean trade success
- `executionStatus` means job status only, not trade execution status
- Watchlist does not emit new-open / close / reverse action from Push / Recheck status alone

Expected behavior:

| Push / Recheck Signal | Required Interpretation |
|---|---|
| `VALID_EXECUTABLE` | legacy positive recheck naming; review-only |
| `RECHECK_VALID_EXECUTABLE` | legacy push status; review-only |
| `valid=true` | candidate still valid for review; not trade permission |
| `PASS` | review label only |
| replay success | recheck success only |
| job execution completed | recheck job completed only |

## 10. Suggested Test Matrix

Future tests may cover the following, without connecting external APIs:

| Test Group | Scenario | Expected Result |
|---|---|---|
| BoundaryCandidateService | complete source + safe guard | `VALID`, review-only flags true |
| BoundaryCandidateService | missing entry source | fallback |
| BoundaryCandidateService | missing stop source | fallback |
| BoundaryCandidateService | missing TP source | fallback |
| BoundaryCandidateService | missing RR source | fallback |
| SourceTrace | all required sources present | completeness true |
| SourceTrace | each required source missing one by one | completeness false |
| DerivativesRiskContext | missing OI / Funding / liquidation / leverage | missing fields recorded; consumers fallback |
| ExecutionPlanDisplayAdapter | missing SourceTrace | review-only fallback |
| ExecutionPlanDisplayAdapter | complete source + high risk | review-only fallback |
| RiskActionGuard | stampede / liquidity stress / event window | fail closed |
| PushRecheckService | `VALID_EXECUTABLE` status | no order, no executable plan |
| Watchlist | positive recheck status | no new-open, close, reverse, or opportunity action |

Suggested future command set:

```bash
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
./mvnw -q -Dtest=BoundaryCandidateServiceImplTest test
./mvnw -q -Dtest=DefaultExecutionPlanDisplayAdapterTest test
./mvnw -q -Dtest=PlanServiceImplTest test
./mvnw -q -Dtest=RuleEngineServiceSourceTraceTest test
./mvnw -q -Dtest=PushRecheckServiceImplTest test
```

These commands are suggestions for a later implementation / verification task. This document does not run or modify tests.

## 11. Acceptance Criteria

P11B dry-run planning is complete when this document confirms:

- production-like dry-run uses only local fixtures or mocks
- no Coinglass or external API is connected
- no actual order API is connected
- no actual ExecutionPlan execution is generated
- BoundaryCandidate `VALID` remains review-only
- ExecutionPlan remains advisory / display-only under missing source or high risk
- SourceTrace missing data triggers fallback
- DerivativesRiskContext missing data triggers fallback
- RiskActionGuard fail-closed scenarios remain blocked
- Push / Recheck / Watchlist naming does not become execution permission

## 12. Current Conclusion

P11B should validate the system in a near-production dry-run style without changing production behavior.

The expected safe result is:

- complete source + safe risk context may reach review-ready outputs
- missing source falls back to `INCOMPLETE`, `WATCH_ONLY`, or `SAFE_FAIL_CLOSED_ONLY`
- high-risk or unknown-risk states remain fail-closed or review-only
- Push / Recheck legacy executable wording remains non-trading
- no external API, execution plan, or automated trading action is produced

Before staging this document, ensure no other document is already staged unless the next task explicitly includes it.
