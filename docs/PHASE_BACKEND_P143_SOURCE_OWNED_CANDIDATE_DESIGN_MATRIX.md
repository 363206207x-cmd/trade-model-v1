# BACKEND-P143 Source-Owned Candidate Design Matrix

## Baseline

- Branch context: PR #400 / Issue #398.
- Duplicate issue note: Issue #399 is a duplicate and is ignored. Issue #398 is the formal mainline issue.
- Formal mainline title: BACKEND-P143 Source-Owned Candidate Design Matrix.
- PR title note: PR #400 uses a shortened title as a platform workaround; Issue #398 and this document preserve the formal mainline title.
- Baseline commit: `7f68a3a` (`P142 Source-Owned Input Contract (#397)`).
- Scope: documentation-only source-owned candidate design matrix.
- Line context: P143 continues the Production Wiring Preparation Line.
- Placeholder removed: `docs/P143.md`.

## Files Changed

- `docs/PHASE_BACKEND_P143_SOURCE_OWNED_CANDIDATE_DESIGN_MATRIX.md`
- Removed `docs/P143.md`

No Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data reader, live market data reader, external data integration, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Purpose

P143 translates the P142 source-owned candidate input contract into an implementation-planning matrix.

P143 does not add Java. P143 does not add runtime wiring. P143 does not generate real entry / stop / TP / RR values. P143 does not upgrade readiness. P143 does not create order, execution, scheduler, automation, or auto-trading behavior.

The matrix identifies, for each input family:

- P142 required fields
- existing fields/classes if any
- missing fields/classes
- required future tests
- `INCOMPLETE` triggers
- `BLOCKED` triggers
- rollback requirements
- still-blocked implementation paths
- current sufficiency judgment

## Controlling Source

P142 remains the controlling source for this matrix.

Every source-owned input family requires:

- owner
- source ref
- source timeframe
- source window
- freshness
- rule id
- rule version
- reason
- conflict state

Entry, stop, TP, and RR also require numeric source ownership.

SourceTrace audit completeness and Risk Action Guard boundaries remain mandatory.

## Preserved P141 / P142 Conclusions

P143 preserves these conclusions:

- runtime SourceTrace population remains missing
- source-owned candidate generation remains missing
- real entry / stop / TP / RR generation remains blocked
- BoundaryCandidateService production `VALID` path remains blocked
- ExecutionPlan readiness beyond review-only remains blocked
- latest price alone is not an entry source
- raw kline presence alone is not entry / stop / TP / RR source
- AI text is not source ownership
- dashboard / API / display text is not source ownership
- order / execution state is not source ownership

## Matrix Summary

| Family | Current Sufficiency Judgment | What Can Be Planned Next Without Implementation |
| --- | --- | --- |
| Entry | Partially represented, not sufficient for runtime source-owned candidate. | Documentation-only mapping from current entry fields to required owner/ref/window/freshness/rule/conflict fields. |
| Stop | Partially represented, not sufficient for runtime source-owned candidate. | Documentation-only mapping from stop fields to independent source ownership requirements. |
| Take-profit | Partially represented, not sufficient for runtime source-owned candidate. | Documentation-only TP ownership and ordering matrix. |
| Risk-reward | Partially represented, not sufficient for runtime source-owned candidate. | Documentation-only RR derivation and source-bundle contract. |
| Liquidity | Mostly skeleton/source-label level, not sufficient. | Documentation-only liquidity evidence and blocker decision table. |
| Multi-timeframe | Mostly skeleton/source-label level, not sufficient. | Documentation-only participating-timeframe evidence matrix. |
| Event | Mostly skeleton/source-label level, not sufficient. | Documentation-only event evidence status and no-risk-blocker matrix. |
| Wick | Mostly skeleton/source-label level, not sufficient. | Documentation-only wick confirmation matrix. |
| OHLCV / kline | Diagnostic context exists, but not boundary ownership. | Documentation-only mapping from persisted OHLCV readiness to input-evidence prerequisites. |
| Data quality | Field exists, ownership chain incomplete. | Documentation-only data quality owner/source/ref/window/rule matrix. |
| SourceTrace audit | DTO fields exist, runtime population incomplete. | Documentation-only SourceTrace audit completeness matrix. |
| Risk Action Guard | Rules documented, runtime decision table still missing. | Documentation-only Risk Action Guard decision table and test matrix. |

No row in this matrix is sufficient for production implementation today.

## Design Matrix

### Entry

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref, source timeframe, source window, freshness, rule id, rule version, reason, conflict state, numeric source ownership. |
| Existing fields/classes | `SourceTraceDTO.entryPriceSource`, `entrySourceType`, `entrySourceTimeframe`, `entrySourceReason`, `entrySourceRef`; `RuntimeKlineContextDTO` matching entry source fields; `BoundaryEntryDTO.entryPrice`, `numericSourceType`, `numericSourceValue`, `sourceTimeframe`, `reason`; entry ownership DTO/service skeletons and fail-closed tests. |
| Missing fields/classes | Production entry owner, typed source window, entry freshness status, entry rule id, entry rule version, explicit entry conflict state in runtime input, typed source ref audit envelope, runtime source-owned population path. |
| Required future tests | Entry source-owned input contract test, entry numeric source ownership test, missing owner/ref/timeframe/window/freshness/rule/reason/conflict fail-closed tests, latest-price-only blocked test, dashboard/API/display/AI substitution blocked tests. |
| `INCOMPLETE` triggers | Missing entry owner, ref, timeframe, window, observed time, decision-time relationship, freshness, rule id, rule version, reason, conflict state, numeric source ownership, SourceTrace audit, or runtime SourceTrace population. |
| `BLOCKED` triggers | Latest price used as entry by itself, raw kline used as entry by itself, unsupported entry owner/type, duplicated/ambiguous/fabricated entry ref, stale source window with unsafe evidence, dashboard/API/display/AI/order/execution substitution. |
| Rollback requirements | Remove future entry mapping/tests/design artifacts and restore missing-entry behavior to `INCOMPLETE` or `BLOCKED`; do not touch production Java in P143. |
| Still-blocked implementation paths | Real entry generation, runtime SourceTrace field population, production candidate generation, production `VALID`, ExecutionPlan readiness, controller/API/schema/config/service/mapper changes. |
| Sufficiency judgment | Partially represented, not sufficient for runtime source-owned candidate. |

### Stop

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref, source timeframe, source window, freshness, rule id, rule version, reason, conflict state, numeric source ownership. |
| Existing fields/classes | `SourceTraceDTO.stopPriceSource`, `stopSourceType`, `stopSourceTimeframe`, `stopSourceReason`, `stopSourceRef`; `RuntimeKlineContextDTO` matching stop source fields; `BoundaryStopDTO.stopPrice`, `numericSourceType`, `numericSourceValue`, `sourceTimeframe`, `reason`; stop ownership DTO/service skeletons. |
| Missing fields/classes | Production stop owner, typed source window, stop freshness status, stop rule id, stop rule version, explicit stop conflict state, independent stop source audit envelope, runtime source-owned population path. |
| Required future tests | Stop source-owned input contract test, stop numeric source ownership test, missing stop fields fail-closed tests, entry-derived-stop-only blocked test, raw-kline-only blocked test, unsupported stop owner/type blocked test. |
| `INCOMPLETE` triggers | Missing stop owner, ref, timeframe, window, freshness, rule id, rule version, reason, conflict state, numeric source ownership, SourceTrace audit, or runtime SourceTrace population. |
| `BLOCKED` triggers | Stop inferred only from entry, latest price, raw kline, dashboard/API/display/AI text, order/execution state, unsupported owner/type, fabricated ref, or unsafe stale source window. |
| Rollback requirements | Restore stop to missing-evidence fail-closed behavior and remove any future design/test artifact that implies real stop production. |
| Still-blocked implementation paths | Real stop generation, production source-owned candidate generation, production `VALID`, ExecutionPlan readiness, dashboard readiness, API/controller/schema/config/service/mapper changes. |
| Sufficiency judgment | Partially represented, not sufficient for runtime source-owned candidate. |

### Take-Profit

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner for every TP level, source ref, source timeframe, source window, freshness, rule id, rule version, reason, conflict state, numeric source ownership, ordering intent. |
| Existing fields/classes | `SourceTraceDTO.tpPriceSources`, `tpSourceType`, `tpSourceTimeframe`, `tpSourceReason`, `tpSourceRef`; `RuntimeKlineContextDTO` matching TP fields; `BoundaryTakeProfitLevelDTO.price`, `rr`, `numericSourceType`, `numericSourceValue`, `sourceTimeframe`, `sourceRef`; TP ownership DTO/service skeletons. |
| Missing fields/classes | TP owner per level/source family, typed TP source window, TP freshness status, TP rule id/version, explicit TP conflict state, mixed-source ownership rules, runtime source-owned population path. |
| Required future tests | TP source-owned input contract test, TP numeric source ownership tests, missing TP list/owner/ref/window/freshness/rule/reason/conflict fail-closed tests, TP ordering tests, raw-kline-only and display-text substitution blocked tests. |
| `INCOMPLETE` triggers | Missing TP owner, ref, timeframe, window, freshness, rule id, rule version, reason, conflict state, numeric source ownership, ordering evidence, SourceTrace audit, or runtime SourceTrace population. |
| `BLOCKED` triggers | TP generated from raw kline presence alone, unsupported source type, fabricated or ambiguous ref, unsafe stale source window, dashboard/API/display/AI substitution, or order/execution feedback loop. |
| Rollback requirements | Remove any TP design artifact that implies generated TP values and restore TP missing-evidence behavior to `INCOMPLETE`/`BLOCKED`. |
| Still-blocked implementation paths | Real TP generation, source-owned runtime candidate generation, production `VALID`, ExecutionPlan readiness, dashboard/API/schema/config/service/mapper changes. |
| Sufficiency judgment | Partially represented, not sufficient for runtime source-owned candidate. |

### Risk-Reward

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref or source bundle ref, source timeframe/window, freshness, rule id, rule version, reason, conflict state, `rrRuleRef`, numeric source ownership. |
| Existing fields/classes | `SourceTraceDTO.rrSource`, `rrRuleRef`; `RuntimeKlineContextDTO.rrSource`, `rrRuleRef`; `BoundaryTakeProfitLevelDTO.rr`; RR ownership DTO/service skeletons. |
| Missing fields/classes | Production RR owner, RR source bundle ref, RR source window, RR freshness status, RR rule id/version beyond string ref, RR conflict state, proof RR derives from owned entry/stop/TP, runtime source-owned population path. |
| Required future tests | RR source-owned input contract test, RR numeric source ownership test, RR derivation-from-owned-entry-stop-TP test, missing RR rule/ref/window/freshness fail-closed tests, copied-display/AI RR blocked tests. |
| `INCOMPLETE` triggers | Missing RR owner, source bundle ref, timeframe, window, freshness, rule id, rule version, reason, conflict state, `rrRuleRef`, numeric source ownership, owned entry/stop/TP prerequisites, SourceTrace audit. |
| `BLOCKED` triggers | RR copied from AI/display/API text, fixture-only token, latest price, raw kline, unsupported rule, stale unsafe source window, fabricated source bundle ref, or order/execution data. |
| Rollback requirements | Remove any future RR derivation artifact and restore RR to missing-evidence fail-closed behavior. |
| Still-blocked implementation paths | Real RR generation, source-owned candidate generation, production `VALID`, ExecutionPlan readiness, order/execution/automation paths. |
| Sufficiency judgment | Partially represented, not sufficient for runtime source-owned candidate. |

### Liquidity

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref, source timeframe/window, freshness, rule id, rule version, reason, conflict state. |
| Existing fields/classes | `SourceTraceDTO.liquiditySource`; `RuntimeKlineContextDTO.liquiditySource`; `DerivativesRiskContextDTO.liquidityStress`, `liquidityStressReason`; liquidity ownership DTO/service skeletons; Risk Action Guard display fields. |
| Missing fields/classes | Typed liquidity owner/ref/window/freshness/rule/version/conflict, source-owned deteriorating-liquidity decision table, runtime ownership population, liquidity-to-blocker audit trail. |
| Required future tests | Liquidity source-owned input contract test, missing liquidity evidence incomplete test, liquidity stress blocked test, deteriorating liquidity one-shot-exit blocked test, liquidity-as-opportunity blocked test. |
| `INCOMPLETE` triggers | Missing liquidity owner, ref, timeframe/window, freshness, rule id/version, reason, conflict state, or SourceTrace audit evidence. |
| `BLOCKED` triggers | Deteriorating liquidity treated as direct action, liquidity stress treated as opportunity, unsupported liquidity source, unsafe stale window, fabricated ref, missing liquidity treated as safe. |
| Rollback requirements | Restore liquidity to manual-review/fail-closed context and remove any future artifact that implies opportunity or direct action. |
| Still-blocked implementation paths | Production liquidity ownership wiring, runtime SourceTrace population, production candidate generation, ExecutionPlan readiness, order/execution/automation. |
| Sufficiency judgment | Mostly skeleton/source-label level, not sufficient. |

### Multi-Timeframe

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, participating source refs, source timeframe set, source window/aggregation window, freshness per timeframe, rule id, rule version, reason, convergence/conflict state. |
| Existing fields/classes | `SourceTraceDTO.multiTimeframeSource`; `RuntimeKlineContextDTO.multiTimeframeSource`; multi-timeframe ownership DTO/service skeletons. |
| Missing fields/classes | Participating timeframe list/ref structure, aggregation window, per-timeframe freshness, aggregation rule id/version, convergence/conflict state, runtime source-owned population path. |
| Required future tests | Multi-timeframe source-owned input contract test, missing participating timeframe incomplete test, stale timeframe incomplete/blocked tests, agreement-alone-not-complete test, conflict-state missing fail-closed test. |
| `INCOMPLETE` triggers | Missing owner, refs, timeframe set, aggregation window, freshness, rule id/version, reason, convergence/conflict state, or SourceTrace audit. |
| `BLOCKED` triggers | Multi-timeframe agreement used as readiness by itself, stale unsafe timeframe evidence, fabricated refs, unsupported aggregation source, conflict ignored. |
| Rollback requirements | Restore multi-timeframe evidence to source-label/fail-closed behavior and remove any future readiness implication. |
| Still-blocked implementation paths | Multi-timeframe runtime ownership, production candidate generation, production `VALID`, ExecutionPlan readiness, dashboard readiness. |
| Sufficiency judgment | Mostly skeleton/source-label level, not sufficient. |

### Event

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref, observation timeframe, source window, freshness, rule id, rule version, reason, conflict/blocker state. |
| Existing fields/classes | `SourceTraceDTO.eventSource`; `RuntimeKlineContextDTO.eventSource`; `DerivativesRiskContextDTO.eventWindowBlockers`; event ownership DTO/service skeletons. |
| Missing fields/classes | Typed event owner/ref/window/freshness/rule/version/status, event blocker decision table, missing-event evidence status, runtime SourceTrace event population. |
| Required future tests | Event source-owned input contract test, missing event evidence incomplete test, missing event treated-as-no-risk blocked test, event no-go blocked test, stale event window fail-closed test. |
| `INCOMPLETE` triggers | Missing event owner, ref, timeframe/window, freshness, rule id/version, reason, event evidence status, conflict/blocker state, or SourceTrace audit. |
| `BLOCKED` triggers | Missing event evidence displayed as no risk, known event no-go evidence, unsafe stale event window, unsupported event source, fabricated event ref. |
| Rollback requirements | Restore event state to missing-evidence or no-go fail-closed behavior and remove any future no-risk implication. |
| Still-blocked implementation paths | Runtime event ownership, external data fetches, source-owned candidate generation, production `VALID`, ExecutionPlan readiness. |
| Sufficiency judgment | Mostly skeleton/source-label level, not sufficient. |

### Wick

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | owner, source ref, source timeframe, source window, freshness, rule id, rule version, reason, conflict state. |
| Existing fields/classes | `SourceTraceDTO.wickSource`; `RuntimeKlineContextDTO.wickSource`; `DerivativesRiskContextDTO.wickConfirmationSources`; wick ownership DTO/service skeletons. |
| Missing fields/classes | Typed wick owner/ref/window/freshness/rule/version, confirmation state, conflict state versus trend/liquidity/event/multi-timeframe, runtime SourceTrace wick population. |
| Required future tests | Wick source-owned input contract test, wick-only confirmation tests, wick-only trend-reversal blocked test, stale wick evidence fail-closed test, missing confirmation incomplete test. |
| `INCOMPLETE` triggers | Missing wick owner, ref, timeframe/window, freshness, rule id/version, reason, confirmation/conflict state, or SourceTrace audit. |
| `BLOCKED` triggers | Wick-only evidence treated as trend reversal, unsupported wick source, stale unsafe wick window, fabricated ref, conflict ignored. |
| Rollback requirements | Restore wick to review-only/fail-closed context and remove any future reversal implication. |
| Still-blocked implementation paths | Runtime wick ownership, production candidate generation, production `VALID`, ExecutionPlan readiness, dashboard readiness. |
| Sufficiency judgment | Mostly skeleton/source-label level, not sufficient. |

### OHLCV / Kline

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | persisted source owner, source ref or batch/source trace id, symbol, timeframe, required closed-bar window, latest close time, ingestion time, freshness, stale reason code, missing fields, quality status, rule id, rule version, reason, continuity/conflict state. |
| Existing fields/classes | `RuntimeKlineContextDTO`, `RuntimeKlineItemDTO`, persisted OHLCV readiness diagnostics, `RuntimeKlineContextAssemblyServiceImpl`, dashboard runtime kline context adapter. |
| Missing fields/classes | Boundary ownership selection from kline data, rule id/version for boundary extraction, per-boundary source windows, conflict state connecting kline evidence to entry/stop/TP/RR, runtime source-owned candidate input object. |
| Required future tests | OHLCV/kline source-owned input contract test, raw-kline-only blocked tests, stale/non-contiguous/missing-readiness incomplete tests, boundary-extraction-not-implied tests, latest-price-only blocked tests. |
| `INCOMPLETE` triggers | Missing OHLCV/kline context, persisted readiness metadata, closed-bar window, freshness, stale reason, quality status, source ref, rule id/version, continuity/conflict state. |
| `BLOCKED` triggers | Raw kline presence used as entry/stop/TP/RR by itself, latest price used as entry by itself, stale unsafe window, unsupported source, fabricated batch/ref, external feed without contract. |
| Rollback requirements | Restore OHLCV/kline to diagnostic context only and remove any future boundary-ownership implication. |
| Still-blocked implementation paths | Runtime data reads, live market data reads, external data fetches, source-owned runtime candidate generation, real boundary value generation. |
| Sufficiency judgment | Diagnostic context exists, but not boundary ownership. |

### Data Quality

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | data quality owner, source ref, source timeframe/window, freshness, rule id, rule version, reason, conflict state, numeric data quality score. |
| Existing fields/classes | `SourceTraceDTO.dataQualityScore`, `dataQualityScoreSource`; `RuntimeKlineContextDTO.dataQualityScore`; `BoundarySourceFieldsDTO.dataQualityScore`; `BoundaryCandidateDTO.dataQualityScore`; market read-only snapshot/result data quality fields. |
| Missing fields/classes | Production data quality owner, source ref/window, freshness, rule id/version, conflict state, audit trail, consistent runtime copy into SourceTrace audit. |
| Required future tests | Data quality source-owned input contract test, missing score incomplete test, missing owner incomplete test, unrelated score substitution blocked test, stale/unsupported quality source fail-closed tests. |
| `INCOMPLETE` triggers | Missing score, owner, ref, timeframe/window, freshness, rule id/version, reason, conflict state, SourceTrace audit link. |
| `BLOCKED` triggers | Data quality copied from unrelated decision score, dashboard/API/display/AI text, unsupported external source, fabricated ref, stale unsafe quality evidence. |
| Rollback requirements | Restore missing/unsafe data quality to fail-closed context and remove any future quality-as-readiness implication. |
| Still-blocked implementation paths | Production data quality ownership chain, production candidate generation, production `VALID`, ExecutionPlan readiness. |
| Sufficiency judgment | Field exists, ownership chain incomplete. |

### SourceTrace Audit

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | audit owner, family source refs, timeframe coverage, source windows, freshness summary, rule id/version coverage, reasons, conflict summary, missing evidence summary, blocked evidence summary, rollback-safe evidence trail, `manualReviewRequired=true`, `notTradeInstruction=true`, `reviewMode=REVIEW_ONLY`. |
| Existing fields/classes | `SourceTraceDTO`, `SourceCompletenessContract`, `SourceTraceFallbackStatusEnum`, `missingFields`, `hasRequiredBoundarySources()`, SourceTrace production ownership audit/review DTOs, `DefaultSourceAssembler`, fail-closed SourceTrace services. |
| Missing fields/classes | Runtime audit owner, typed family-level audit envelope, complete runtime population from source-owned evidence, complete source window/rule/freshness/conflict coverage, rollback-safe evidence trail in runtime candidate input. |
| Required future tests | SourceTrace audit completeness tests, missing family audit incomplete tests, display/API/AI text substitution blocked tests, runtime population from source-owned evidence tests, no production `VALID` mapping guard tests. |
| `INCOMPLETE` triggers | Missing audit owner, family refs, windows, freshness summary, rule coverage, reasons, conflict summary, missing/blocked evidence summary, rollback trail, runtime SourceTrace source-owned population. |
| `BLOCKED` triggers | Runtime SourceTrace populated from dashboard/API/display/AI/order/execution data, fabricated audit refs, unsafe stale source window, missing event displayed as no risk, action surface appears. |
| Rollback requirements | Restore SourceTrace to incomplete/fail-closed behavior and remove any future runtime completion claim. |
| Still-blocked implementation paths | Runtime SourceTrace field population, full SourceTrace runtime completion, production completion, production adapter, production `VALID`, ExecutionPlan readiness. |
| Sufficiency judgment | DTO fields exist, runtime population incomplete. |

### Risk Action Guard

| Matrix Item | P143 Assessment |
| --- | --- |
| P142 required fields | stampede state, wick-only state, liquidity state, deteriorating-liquidity state, missing-event state, high-risk state, blocking reason, action flags false, review-only safety flags. |
| Existing fields/classes | Risk Action Guard docs, `DefaultRiskActionGuardDisplayAdapter`, Risk Action Guard display VO fields, `BoundaryCandidateServiceImpl` risk blocking checks, `DefaultExecutionPlanDisplayAdapter` risk readiness checks, fixture tests. |
| Missing fields/classes | Runtime decision table covering source-owned evidence, typed Risk Action Guard evidence object, source-owned relationship to liquidity/event/wick/multi-timeframe inputs, complete no-go/blocker audit trail. |
| Required future tests | Risk Action Guard blocked tests, liquidity stress blocked tests, stampede blocked tests, wick-only confirmation tests, missing event incomplete tests, high-risk-normal-liquidity review-only tests, no action-surface guard tests. |
| `INCOMPLETE` triggers | Missing Risk Action Guard state, missing liquidity/event/wick evidence status, missing high-risk context, missing blocking reason audit, incomplete source evidence needed to evaluate guard. |
| `BLOCKED` triggers | Stampede, deteriorating liquidity unsafe for direct action, wick-only reversal misuse, missing event treated as no risk, liquidity stress treated as opportunity, action flags enabled, order/execution/automation surface. |
| Rollback requirements | Restore all Risk Action Guard output to manual-review/fail-closed behavior and remove any action-facing interpretation. |
| Still-blocked implementation paths | Runtime Risk Action Guard decision table implementation, production candidate generation, ExecutionPlan readiness, dashboard readiness mutation, order/execution/scheduler/automation. |
| Sufficiency judgment | Rules documented, runtime decision table still missing. |

## Current Sufficiency Judgment

The current repository does not have sufficient fields for any family to support runtime source-owned candidate generation.

The repository does have useful partial structures:

- SourceTrace DTO fields and required-boundary-source checks
- RuntimeKline diagnostic context and persisted OHLCV readiness metadata
- Boundary candidate DTOs and fail-closed service checks
- Source ownership service skeletons
- Risk Action Guard display and blocking checks
- fixture and guard tests proving review-only/non-executable behavior

The repository is still missing the runtime source-owned input object, typed owner/ref/window/freshness/rule/conflict fields, runtime SourceTrace population from source-owned evidence, real entry/stop/TP/RR generation, and a complete Risk Action Guard decision table.

## What Can Be Planned Next Without Implementation

Future docs-only planning may define:

- field-by-field mapping from P142 contract terms to current DTO fields
- missing field proposal without adding Java
- source-owned candidate test plan
- Risk Action Guard decision table design
- SourceTrace audit evidence model
- rollback plan for future implementation

Future planning must still not add Java, test source, runtime reads, dashboard changes, controller/API/schema/config/service/mapper changes, production `VALID`, ExecutionPlan readiness, order, execution, scheduler, automation, or auto-trading.

## Future Tests To Define

Future separately authorized implementation planning should define:

- one source-owned input contract test per family
- numeric source ownership tests for entry / stop / TP / RR
- freshness and source-window fail-closed tests
- rule id / rule version fail-closed tests
- duplicate / ambiguous / fabricated source ref blocked tests
- latest-price-only blocked tests
- raw-kline-only blocked tests
- AI/dashboard/API/display text substitution blocked tests
- missing event evidence incomplete tests
- liquidity stress blocked tests
- stampede blocked tests
- wick-only confirmation tests
- Risk Action Guard blocked tests
- SourceTrace audit completeness tests
- no production `VALID` mapping guard tests
- no ExecutionPlan readiness upgrade guard tests
- no order / execution / scheduler / automation / auto-trading guard tests

P143 defines these future tests only as planning requirements. P143 does not add tests.

## INCOMPLETE Triggers

Future candidate output must remain `INCOMPLETE` when:

- source owner is missing
- source ref is missing
- source timeframe is missing
- source window is missing
- observed time is missing
- decision time relationship is missing
- freshness is missing
- source is stale without known no-go evidence
- OHLCV / kline context is missing
- persisted OHLCV readiness metadata is missing
- data quality score is missing
- data quality score owner is missing
- evidence completeness is insufficient
- SourceTrace is incomplete
- numeric source ownership is incomplete
- entry source reason is missing
- stop source reason is missing
- TP source reason is missing
- RR rule ref is missing
- rule id is missing
- rule version is missing
- conflict family state is missing
- liquidity evidence is missing
- multi-timeframe evidence is missing
- event evidence status is missing
- wick evidence status is missing
- rollback-safe evidence trail is missing
- runtime SourceTrace field is not populated from source-owned evidence

`INCOMPLETE` remains missing-evidence context only. It is not production `VALID`, not readiness, not executable state, not a trade instruction, and not an order or execution surface.

## BLOCKED Triggers

Future candidate output must remain `BLOCKED` when:

- forbidden input is present
- no-go evidence exists
- Risk Action Guard blocks action
- stampede condition exists
- deteriorating liquidity makes direct action unsafe
- wick-only evidence is being misread as trend reversal
- missing event evidence is being treated as no risk
- liquidity stress is being treated as opportunity
- stale source window appears with unsafe or contradictory evidence
- unsupported source owner is used
- unsupported source type is used
- source ref is duplicated, ambiguous, or fabricated
- dashboard/API/display text is used as source ownership
- AI text is used as source ownership
- latest price is used as entry source by itself
- raw kline item presence is used as entry / stop / TP / RR source by itself
- external data appears without a source ownership contract
- order / execution / automation surface appears

`BLOCKED` remains no-go / forbidden / Risk Action Guard blocked context only. It is not production `VALID`, not readiness, not executable state, not a trade instruction, and not an order or execution path.

## Risk Action Guard Boundaries

- Stampede must not become reverse / new-position / opportunity-push display.
- Wick-only must not become trend reversal.
- Deteriorating liquidity must not become one-shot market exit instruction.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.
- High risk alone must not mean direct stop loss, reverse, or new position.
- Risk high with normal liquidity may support review-only risk reduction context, not automatic action.
- Risk high with deteriorating liquidity must avoid one-shot market exit instruction.
- Risk high with stampede must block reverse / new position / opportunity push.
- Risk high with wick-only evidence must wait for confirmation.

## Recommended Next Step

Recommended next step after P143 is STOP unless a separately authorized issue exists.

If work continues, the safest next line is a documentation-only source-owned candidate test plan. That future issue should define exact future test classes, static/fixture scope, implementation no-go triggers, rollback, and still-blocked paths before any Java implementation is considered.

P143 does not authorize that future test plan. P143 does not authorize production implementation.

## Still-Blocked Paths

The following paths remain blocked after P143:

- production candidate generation
- source-owned runtime candidate generation
- real entry / stop / TP / RR value generation
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` calls in new production flows
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- dashboard implementation beyond P130 static skeleton
- `dashboard.html` changes beyond P130 static skeleton
- dashboard UI code beyond P130 static skeleton
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service registration
- Spring bean registration
- order API
- execution API
- scheduler / automation / auto-trading
- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion

## Rollback Expectations

Rollback for P143 is limited to:

- remove `docs/PHASE_BACKEND_P143_SOURCE_OWNED_CANDIDATE_DESIGN_MATRIX.md`
- restore `docs/P143.md` only if the PR is abandoned before merge

Rollback must not touch production Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data, live market data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

If a future change uses P143 to widen scope without authorization, rollback must restore the last approved P142 source-owned candidate input contract and keep all still-blocked paths blocked.

## Boundary Confirmations

- P143 is documentation-only design matrix work.
- P143 ignores duplicate Issue #399 and uses formal mainline Issue #398.
- P143 removes the placeholder `docs/P143.md`.
- P143 adds one source-owned candidate design matrix document.
- P143 does not modify production Java.
- P143 does not modify test source.
- P143 does not modify `dashboard.html`.
- P143 does not add dashboard UI code.
- P143 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P143 does not read runtime data.
- P143 does not read live market data.
- P143 does not fetch external data.
- P143 does not generate real entry / stop / TP / RR values.
- P143 does not upgrade ExecutionPlan readiness.
- P143 does not map to production `VALID`.
- P143 does not wire BoundaryCandidateService `VALID` production path.
- P143 does not call `BoundaryCandidateDTO.valid(...)`.
- P143 does not add order API.
- P143 does not add execution API.
- P143 does not add scheduler / automation / auto-trading.
- P143 does not authorize production implementation.
- P143 does not merge the PR.

## Validation

P143 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- design matrix coverage
- family-by-family gaps
- current sufficiency judgment
- future tests
- `INCOMPLETE` triggers
- `BLOCKED` triggers
- recommended next step
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #398 / BACKEND-P143

P143 stops here. It does not merge the PR.
