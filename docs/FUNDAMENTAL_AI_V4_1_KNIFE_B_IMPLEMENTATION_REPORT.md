# Fundamental AI v4.1 Knife B.1 Implementation Report

## Scope And Truth State

Knife B.1 closes the verified residuals in KB-02, KB-03, KB-04 and KB-07 on `codex/frontend-interaction-runtime-closure`. The implementation base is `1a3363f3f05ec22352477097971965dae4785bc2`; PR #1195 remains Draft and unmerged. The exact handoff Head is reported by Git/PR after the commit rather than embedded as a self-referential value in this file.

This package changes no schema, Flyway migration, Mobile/Figma surface, CoinGlass chain, login contract, decision state machine or automatic-trading capability. `HighValueAlertMessageService` changes are limited to Message/PushSnapshot identity fields.

## Contract Mapping

| Package | Old binding | New binding | Automated evidence | Browser evidence | Status |
|---|---|---|---|---|---|
| KB-01 regression | Home Top3 risked being reused as the workspace list | owner-scoped workspace continues to query every active Position and each Position's own latest monitor | Position projection and controller tests | UI-review Home Top3 | PASS |
| KB-02 lifecycle | legacy empty copy; CLOSED detail could request current monitor and render self-detail semantics | exact empty copy; OPEN/PARTIALLY_CLOSED remain active; CLOSED is historical, preserves real facts and does not query current monitor | Position projection/frontend tests | empty Active and History tabs; data-rich CLOSED browser record unavailable | PASS |
| KB-03 mode/roles | Preview could inherit Candidate semantics; Gemini/Grok formal fields were incomplete; duplicate panels could remain | superseded by Knife B.1.1 executable role/mode/collection gates and correct `/analysis/{id}` evidence | production-function Node matrix plus role codec/query tests | Analysis-route Opportunity, Preview and Unknown UI-review evidence | PASS_IN_B1_1 |
| KB-04 Push Recheck | `currentRecheckId=pushId`; historical record acted as a permanent gate; bind could POST; execution ERROR was not durable | superseded by Knife B.1.1 atomic core transaction, independent ERROR transaction and after-commit safety-message handling | real Spring/H2 transaction integration tests plus owner/read boundary tests | data-rich Recheck browser state remains unavailable | PASS_IN_B1_1 |
| KB-05 regression | explicit Final-to-Position boundary | unchanged: only user submission creates UserPosition | existing O06 tests | not changed | PASS |
| KB-06 | no auditable partial-close event/quantity producer | unchanged; no persistence invented | repository ownership audit | not applicable | BLOCKED_BY_MISSING_PERSISTENCE_SOURCE |
| KB-07 context/audit | Message Plan/Analysis links lost group; returnTo used broad prefixes | route-specific internal allowlist; Message/Recheck/Position context preserved; audit uses real trace route | frontend contract tests | valid internal context preserved; four hostile shapes rejected | PASS |
| KB-08 regression | legacy route matrix | unchanged, no bulk redirect or retirement | legacy route matrix tests | not changed | PASS |

## Position Lifecycle

- Active states: `OPEN`, `PARTIALLY_CLOSED`.
- History state: `CLOSED`; links use `/positions/{positionId}`.
- Empty copy: `暂无已录入持仓`.
- CLOSED detail obtains the owner-scoped Position projection first, renders opening/closing facts, hides close action, ends the current-monitor timeline and never fetches old monitor logs as a current judgment.
- Non-trusted active monitoring still keeps identity, source, entry price, opened time and detail access while presenting one fail-closed monitor state.

## Analysis Mode And Three AI

- Mode ownership remains `analysis.analysisMode`; asset selection no longer changes it.
- Preview GPT shows direction hypothesis, reason and support/opposition only.
- Preview Gemini shows only formal evidence/conflict/confidence review collections.
- Preview Grok shows formal opposing scenarios, external/microstructure risk and watch indicators, without Opportunity failure paths.
- Opportunity GPT promotes `ExecutionPlanCandidate` fields and states `Candidate · 非 Final`.
- Opportunity Gemini requires `reviewResult`; non-APPROVE output consumes `downgradeSuggestion.before/after/reason/recoveryCondition`.
- Opportunity Grok requires `failurePathState`; each path consumes trigger condition, causal path and invalidating evidence. `FOUND` with an empty list is data-incomplete, not no-path.
- Unknown/missing mode renders only the fail-closed role state. Auxiliary failure/diff panels are stable hidden nodes, not removed nodes and not duplicate role output.

## Push Recheck Ownership And Safety

Canonical production chain:

`Authenticated user -> owned MessageDO(messageId) -> sourceType=PUSH_SNAPSHOT/sourceId(pushId) -> TmPushSnapshotDO -> canonical pushSnapshotId -> PUSH_OPEN TmPushRecheckLogDO(recheckId)`

- New messages persist `currentRecheckId=null`; successful OPEN/Retry updates it to the actual `recheckId`.
- `currentRecheckId` is not used to resolve PushSnapshot. Legacy `currentRecheckId==pushId` is ignored.
- Message click performs the owner-scoped POST; Recheck bind, F5 and `刷新当前对比` perform read-only GET.
- A completed historical OPEN does not permanently block a later explicit Message OPEN.
- In-process duplicate OPEN is coalesced by owner + PushSnapshot. Cross-instance idempotency remains partial because schema/distributed locking was outside authorization.
- Provider/config/derivatives execution exceptions on `PUSH_OPEN` persist `executionStatus=ERROR`; business outcomes such as INVALIDATED remain `COMPLETED`.
- Retry accepts only ERROR and creates a new attempt against the same PushSnapshot. Re-analysis creates an independent AnalysisRun.
- The fake-SCHEDULED gate, raw user pushId gate and manual/replay gates remain closed.

`FRESHNESS = NOT_VERIFIED`: the frozen model has no reuse duration field. No time window was invented; every new legal Message OPEN may create an attempt, while read/bind/F5 never does.

## Return Context Security

The frontend allowlist accepts only exact internal shapes for Dashboard, Messages, Recheck detail, Plan detail, Position list/detail, Analysis list/detail and Audit detail. External schemes, scheme-relative URLs, backslashes, encoded slash/backslash/percent attacks and non-allowlisted routes fail to the page-specific safe default.

Browser runtime verified one legal context and rejected external, `//`, backslash and double-encoded attack inputs. Audit links use `/audit/{traceId}` and preserve the current Home/Analysis context; a missing audit record fails closed.

## Validation

- Java 17 compile: PASS.
- Extended Knife B.1 directed regression: PASS.
- JavaScript syntax: PASS.
- Product Source Gate: PASS.
- Browser: authenticated standard release JAR + isolated UI-review profile; 1,440 and 1,080; horizontal overflow 0; user-visible text clipping 0; console errors 0.
- Full Maven: 4,749 tests, 0 failures, 0 errors, 14 skipped because Docker/Testcontainers was unavailable.
- Workflow Contract: PASS.
- Exact-head GitHub CI: `PENDING_COMMIT_AND_PUSH`.

Evidence and truth labels are indexed at `docs/evidence/knife_b_1/README.md`. No UI-review fixture is represented as live-provider evidence.

## Remaining Boundaries

- `FRESHNESS = NOT_VERIFIED`.
- `CROSS_INSTANCE_IDEMPOTENCY = PARTIAL`.
- Four-position, CLOSED-detail and Recheck browser states = `NOT_VERIFIED_BROWSER_DATA_BOUNDARY`; automated evidence is present.
- `KB-06 = BLOCKED_BY_MISSING_PERSISTENCE_SOURCE`.
- CoinGlass live and Owner final live-runtime acceptance were not run.

## Knife B.1.1 residual closure

Evidence source Head: `c376950f9ce7c0f2d7eae75c8eb861ca9ae38255`.

| Group | Result | Decisive evidence |
|---|---|---|
| B1.1-01 Analysis semantic gates | PASS | four production functions in `frontend-contract.js`; Maven-executed Node matrix; `/analysis/{id}` UI-review captures |
| B1.1-02 Recheck core transaction | PASS | atomic COMPLETED + PushSnapshot + Message binding; rollback-first independent ERROR; eight transaction integration cases |
| B1.1-03 Home aggregate | PASS | all-position aggregate remains separate from Top3 display; four-position tests and Home captures |
| B1.1-04 evidence truthfulness | PASS | old Home evidence corrected; new evidence classification and limits recorded in `docs/evidence/knife_b_1_1/README.md` |

The safety message runs after core commit. Its failure preserves the core
COMPLETED result and records `SAFETY_MESSAGE_FAILED` in
`tm_push_recheck_log.execution_error_code`; therefore
`SAFETY_MESSAGE_CHAIN=PARTIAL`, not PASS.

## B.1.2.3.1 state semantic ownership residual closure

- Data time now follows
  `PersistedOhlcvBarMapper.selectLatestClosedBar().closeTimeMs ->`
  `LocalRealDataStatusService.latestClosedBarAt -> DashboardHomeServiceImpl ->`
  both System Status Data and PageHeader `updatedAt`.
- `LocalRealReadinessService.updatedAt`, application start time, current time,
  and Provider CONNECTED are not timestamp fallbacks.
- `riskLevel=HIGH/EXTREME` remains a risk field and can only produce
  `当前风险较高/当前风险极高`; only `opportunityState=HIGH_RISK` owns
  `高风险观察`.
- No enum, state machine, risk algorithm, schema, Position, close action,
  brand, layout, Three-AI, Recheck, authentication, or Telegram behavior was
  changed.
- LOCAL RUN: Java 17 focused owner matrices `108/108`; full Maven `4782`
  tests, `0` failures, `0` errors, `14` skipped under the existing
  Docker/Testcontainers-unavailable policy.
- Exact-head GitHub CI is reported separately in the final PR canonical
  comment as one `quality-gate` and one `workflow-contract`; this report does
  not label local Maven as CI or claim an aggregate CI count.

Evidence: `docs/evidence/b1_2_3_1/README.md`.

## Phase Status

- `KNIFE_B_1_IMPLEMENTATION_DONE = NO` (historical package did not close the verified residuals).
- `KNIFE_B_1_1_IMPLEMENTATION_DONE = YES` after local validation; exact-head CI is reported separately.
- `KNIFE_B_IMPLEMENTATION_DONE = NO`.
- `CURRENT_PHASE_DONE = NO`.
- `GLOBAL_SEMANTIC_RUNTIME_DONE = NO`.
- `LIVE_RUNTIME_ACCEPTANCE_DONE = NO`.
- `READY_FOR_MERGE = NO`.
- `MERGE_EXECUTED = NO`.
