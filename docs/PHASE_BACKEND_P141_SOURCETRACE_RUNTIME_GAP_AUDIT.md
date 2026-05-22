# BACKEND-P141 SourceTrace Runtime Gap Audit

## Baseline

- Branch context: PR #395 / Issue #393.
- Duplicate issue note: Issue #394 was closed as duplicate / not planned. Issue #393 is the formal mainline issue.
- Formal mainline title: BACKEND-P141 SourceTrace Runtime Gap Audit.
- PR title note: PR #395 uses a shortened title as a platform workaround; Issue #393 and this document preserve the formal mainline title.
- Baseline commit: `3798b25` (`P140 Production Prep Scope (#392)`).
- Scope: documentation-only read-only SourceTrace runtime gap audit.
- Line context: P141 continues the Production Wiring Preparation Line started by P140.
- Placeholder removed: `docs/P141.md`.

## Files Changed

- `docs/PHASE_BACKEND_P141_SOURCETRACE_RUNTIME_GAP_AUDIT.md`
- Removed `docs/P141.md`

No Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data reader, live market data reader, external data integration, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Audit Coverage

P141 audits repository text only. It inspects existing docs and existing source/test files to identify what SourceTrace, source ownership, source reference, freshness, numeric source, RuntimeKlineContext, BoundaryCandidate, and ExecutionPlan-related surfaces exist today.

P141 does not authorize implementation. P141 does not authorize production wiring. P141 does not authorize runtime/live/external data reads. P141 does not authorize real entry / stop / TP / RR values. P141 does not authorize ExecutionPlan readiness upgrade. P141 does not authorize order, execution, scheduler, automation, or auto-trading.

Read-only audit targets included:

- `src/main/java/org/example/trademodel/dto/planboundary/`
- `src/test/java/org/example/trademodel/dto/planboundary/`
- `src/main/java/org/example/trademodel/service/`
- `src/main/java/org/example/trademodel/service/impl/`
- `src/test/java/org/example/trademodel/service/`
- `src/test/java/org/example/trademodel/service/impl/`
- ExecutionPlan-related service/display files that already reference SourceTrace readiness boundaries
- docs from P18-P140 relevant to SourceTrace, BoundaryCandidate, ExecutionPlan, PlanReadiness, read-only candidate generation, production preparation, and static guards

Representative read-only searches used for the audit:

```text
find src/main/java/org/example/trademodel/dto/planboundary -maxdepth 2 -type f | sort
find src/test/java/org/example/trademodel/dto/planboundary -maxdepth 2 -type f | sort
find src/main/java/org/example/trademodel/service -maxdepth 2 -type f | sort
find src/test/java/org/example/trademodel/service -maxdepth 2 -type f | sort
find docs -maxdepth 1 -type f | sort
rg -n "SourceTrace|sourceTrace|sourceOwner|sourceRef|sourceTimeframe|freshness|RuntimeKlineContext|BoundaryNumericSource|BoundaryCandidate|VALID|INCOMPLETE|BLOCKED|PlanReadiness|ExecutionPlan" src/main/java src/test/java docs
rg -n "assembleSourceTrace|evaluateBoundaryCandidate|BoundaryCandidateDTO\\.valid|hasRequiredBoundarySources|setEntryPriceSource|setStopPriceSource|setTpPriceSources|setRrSource" src/main/java/org/example/trademodel src/test/java/org/example/trademodel
```

The audit did not read runtime data, live market data, database data, exchange data, external data, or generated market values.

## Existing Artifacts

Existing SourceTrace / source ownership DTO artifacts include:

- `SourceTraceDTO`
- `SourceCompletenessContract`
- `SourceTraceFallbackStatusEnum`
- SourceTrace entry / stop / take-profit / risk-reward / liquidity / multi-timeframe / event / wick ownership result DTOs
- SourceTrace ownership status, review mode, and missing reason enums for those source families
- SourceTrace entry read-only display/API DTOs
- SourceTrace entry completion DTOs and positive completion contract DTOs
- SourceTrace entry production ownership review request/result/audit/consumer-isolation DTOs

Existing SourceTrace / ownership service artifacts include:

- `SourceAssembler`
- `DefaultSourceAssembler`
- `SourceTraceEntrySourceOwnershipService`
- `SourceTraceStopSourceOwnershipService`
- `SourceTraceTakeProfitSourceOwnershipService`
- `SourceTraceRiskRewardSourceOwnershipService`
- `SourceTraceLiquiditySourceOwnershipService`
- `SourceTraceMultiTimeframeSourceOwnershipService`
- `SourceTraceEventSourceOwnershipService`
- `SourceTraceWickSourceOwnershipService`
- fail-closed implementations for the ownership services
- entry ownership validator, completion contract, read-only assembler, display mapper, API response mapper, and production ownership review boundary skeletons

Existing runtime / boundary DTO artifacts include:

- `RuntimeKlineContextDTO`
- `RuntimeKlineItemDTO`
- `BoundaryCandidateDTO`
- `BoundaryEntryDTO`
- `BoundaryStopDTO`
- `BoundaryTakeProfitLevelDTO`
- `BoundarySourceFieldsDTO`
- `BoundaryStatusEnum`
- `DerivativesRiskContextDTO`
- read-only market candidate DTOs and inert read-only generator DTOs from the P114-P121 line

Existing service / display boundary artifacts include:

- `RuntimeKlineContextAssemblyServiceImpl`
- `BoundaryCandidateService`
- `BoundaryCandidateServiceImpl`
- `RuleEngineService`
- `PlanServiceImpl`
- `DefaultExecutionPlanDisplayAdapter`
- dashboard SourceTrace / PlanBoundary display adapters

Existing tests include SourceTrace DTO/ownership tests, fail-closed source ownership tests, BoundaryCandidate tests, RuntimeKline assembly tests, read-only candidate guard tests, and fixture-only P17/P18 tests that prove review-only behavior remains non-executable.

## Existing Field Representation

`SourceTraceDTO` already represents:

- anchor/source-label fields: `symbol`, `symbolSource`, `decisionId`, `decisionIdSource`, `analysisId`, `analysisIdSource`, `decisionCreateTime`, `decisionCreateTimeSource`, `timeframe`, `timeframeSource`
- runtime diagnostic fields: `runtimeKlineContextStatus`, `runtimeKlineContextSource`, `runtimeKlineReadinessStatus`, `runtimeKlineStaleReasonCode`, `runtimeKlineStaleReasonText`, `runtimeKlineReadinessMissingFields`
- quote/freshness/quality fields: `quoteLatestPrice`, `quoteLatestPriceSource`, `quotePriceUpdateTimeMs`, `quotePriceUpdateTimeSource`, `quoteFreshnessStatus`, `dataQualityScore`, `dataQualityScoreSource`
- entry ownership fields: `entryPriceSource`, `entrySourceType`, `entrySourceTimeframe`, `entrySourceReason`, `entrySourceRef`
- stop ownership fields: `stopPriceSource`, `stopSourceType`, `stopSourceTimeframe`, `stopSourceReason`, `stopSourceRef`
- TP ownership fields: `tpPriceSources`, `tpSourceType`, `tpSourceTimeframe`, `tpSourceReason`, `tpSourceRef`
- RR fields: `rrSource`, `rrRuleRef`
- other source-family fields: `liquiditySource`, `multiTimeframeSource`, `eventSource`, `wickSource`
- fail-closed fields: `fallbackStatus`, `missingFields`, `manualReviewRequired=true`, `notTradeInstruction=true`

`SourceTraceDTO.hasRequiredBoundarySources()` requires empty missing fields, null fallback status, entry / stop / TP / RR sources, and liquidity / multi-timeframe / event / wick sources before the DTO can report required boundary sources.

`RuntimeKlineContextDTO` already represents:

- diagnostic runtime fields: `symbol`, `timeframe`, `latestPrice`, `dataQualityScore`, `klineItems`
- entry / stop / TP / RR source fields matching the current SourceTrace boundary source shape
- liquidity / multi-timeframe / event / wick source strings
- persisted OHLCV readiness diagnostics: readiness status, stale reason code/text, and missing fields
- fail-closed fields: `fallbackStatus`, `missingFields`, `manualReviewRequired=true`, `notTradeInstruction=true`

`BoundaryCandidateDTO` already represents:

- `symbol`
- `timeframe`
- `boundaryStatus`
- `entry`
- `stop`
- `takeProfitLevels`
- `sourceFields`
- `dataQualityScore`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `blockingReasons`

`BoundaryEntryDTO`, `BoundaryStopDTO`, and `BoundaryTakeProfitLevelDTO` already represent numeric source type/value plus source timeframe/reason fields for candidate boundary values.

`BoundarySourceFieldsDTO` already represents source field labels, data source, data quality score, and evidence refs.

There is no production `BoundaryNumericSource` runtime class in the audited production DTO package. Numeric source ownership appears as fixture helper records in tests and as flattened production DTO fields such as `numericSourceType`, `numericSourceValue`, `entryPriceSource`, `stopPriceSource`, `tpPriceSources`, and `rrSource`.

## Runtime SourceTrace Population Finding

Runtime SourceTrace population is not complete today.

The current code has a `SourceAssembler` and `DefaultSourceAssembler`. The assembler can copy source fields from an already-populated `RuntimeKlineContextDTO` into a `SourceTraceDTO` and mark missing fields/fallback status when required source fields are absent. This is a fail-closed assembly boundary, not source-owned candidate generation.

The current `RuntimeKlineContextAssemblyServiceImpl` assembles persisted OHLCV diagnostic context from a persisted readiness result. It can populate symbol, timeframe, latest closed price, kline items, and persisted OHLCV readiness diagnostics. It does not generate source-owned entry / stop / TP / RR values. It does not populate entry / stop / TP / RR source ownership from persisted bars. It does not produce liquidity / multi-timeframe / event / wick ownership. It does not complete SourceTrace.

The current tests can manually provide complete fixture source fields and prove that a fixture path can reach `BoundaryStatusEnum.VALID` while still preserving `manualReviewRequired=true`, `notTradeInstruction=true`, review-only labels, and `canExecute=false`. That fixture proof is not runtime production population.

Current conclusion:

- SourceTrace fields exist.
- RuntimeKlineContext source fields exist.
- BoundaryCandidate gates exist.
- Fail-closed assembler logic exists.
- Fixture-only paths can populate complete source fields in tests.
- Production runtime SourceTrace field population remains missing.
- Source-owned runtime candidate generation remains missing.
- Real entry / stop / TP / RR generation remains blocked.
- Production `VALID` mapping remains blocked.
- ExecutionPlan readiness beyond review-only remains blocked.

## Source Ownership Completeness Gaps

Source owner / source reference / source timeframe / freshness are not complete enough for source-owned runtime candidate generation.

Current represented pieces:

- Entry, stop, and TP have source type, timeframe, reason, and ref fields.
- RR has numeric source and rule ref fields.
- RuntimeKline and SourceTrace carry safety flags and missing-field/fallback status.
- Persisted OHLCV readiness carries diagnostic stale/missing context.
- Boundary source fields can carry source labels and evidence refs.

Current missing or incomplete pieces:

- no exact runtime source-owned candidate input contract
- no production owner field for each numeric boundary family
- no typed source window start/end per boundary family
- no rule id and rule version fields per boundary family
- no freshness status per entry / stop / TP / RR family
- no conflict status per boundary family in `SourceTraceDTO`
- no typed source reference audit envelope attached to each numeric source
- no complete RR owner/type/timeframe/reason/ref/window/freshness representation in `SourceTraceDTO`
- no typed liquidity owner/ref/timeframe/window/freshness/conflict representation
- no typed multi-timeframe owner/ref/timeframe/window/freshness/conflict representation
- no typed event owner/ref/window/freshness/blocker representation
- no typed wick owner/ref/timeframe/window/freshness/conflict representation
- no production source-owner allowlist
- no runtime source reference completeness rule
- no runtime source timeframe completeness rule
- no runtime freshness policy for boundary-source fields
- no data quality score ownership chain for production candidate generation
- no runtime SourceTrace audit trail proving all fields came from source-owned evidence

The current `DefaultSourceAssembler` checks that `RuntimeKlineContextDTO.dataQualityScore` exists but does not copy it into `SourceTraceDTO.dataQualityScore`. That is another gap before any runtime SourceTrace completion claim can be made.

## Missing Fields For Source-Owned Candidate Generation

Before source-owned candidate generation can be considered, the project still needs a documented and tested runtime input contract for:

- source owner per entry / stop / TP / RR / liquidity / multi-timeframe / event / wick family
- source ref per family
- source timeframe per family
- source window start and end per family
- observed time and decision time relationship
- freshness status per family
- stale / future / inverted / unknown freshness downgrade rules
- rule id and rule version per generated numeric source
- reason taxonomy that remains review-only and non-instructional
- conflict family state and conflict resolution
- data quality score owner and source ref
- evidence completeness status
- no-go evidence status
- Risk Action Guard decision table
- rollback-safe evidence trail

Without those fields and rules, output must remain fail-closed and review-only.

## Missing Fields For Real Entry / Stop / TP / RR

Real entry / stop / TP / RR generation remains blocked because the current system lacks:

- production entry source owner
- production stop source owner
- production TP source owner
- production RR source owner
- numeric value owner per field
- numeric source ref per field
- numeric source timeframe per field
- numeric source window per field
- numeric rule id and rule version per field
- freshness owner per field
- data quality owner for numeric values
- complete source-to-value audit trail
- downgrade rules for missing, stale, conflicting, partial, or unsupported numeric evidence
- tests proving latest price and raw kline items cannot substitute for source-owned boundary values

`RuntimeKline.latestPrice` and `RuntimeKline.klineItems` remain diagnostic inputs only. They are not entry, stop, TP, RR, or readiness by themselves.

## BoundaryCandidate VALID Gaps

`BoundaryCandidateServiceImpl` contains a path that can call `BoundaryCandidateDTO.valid(...)` when all method inputs and SourceTrace checks are satisfied. That path is currently reachable in fixture/test conditions where inputs are manually supplied.

This is not a production `VALID` path because the production chain still lacks:

- runtime source-owned candidate input contract
- production-owned numeric sources
- runtime SourceTrace completion
- complete source owner/ref/timeframe/freshness/rule/audit trail
- Risk Action Guard decision table for runtime evidence
- production consumer isolation contract
- rollback path for production wiring
- separately authorized production implementation issue

Until those gaps are closed by separately authorized work, production `VALID` mapping remains blocked and calls to `BoundaryCandidateDTO.valid(...)` must not be added to new production flows.

## ExecutionPlan Readiness Gaps

ExecutionPlan-related code can preserve review-only status and fail closed when SourceTrace is missing or incomplete. Existing fixture tests prove complete fixture inputs remain review-only and non-executable.

ExecutionPlan readiness beyond review-only remains blocked because:

- runtime SourceTrace is not production-complete
- production candidate generation is not implemented
- source-owned entry / stop / TP / RR values are not implemented
- production BoundaryCandidate `VALID` mapping is not authorized
- Risk Action Guard runtime decision table is not complete
- order / execution / scheduler / automation / auto-trading remain blocked
- no future issue has authorized readiness mutation or executable state

`READY_REVIEW_ONLY` remains review-only. It is not trade-ready, not ready-to-trade, not executable, not production `VALID`, not dashboard readiness mutation, and not an order or execution surface.

## INCOMPLETE Requirements

Future output must remain `INCOMPLETE` when any required source-owned evidence is absent, partial, stale, ambiguous, or unaudited.

Required `INCOMPLETE` cases include:

- missing source owner
- missing source reference
- missing source timeframe
- missing source window
- missing observed time
- missing decision time relationship
- missing freshness
- stale source without known unsafe/no-go evidence
- future or clock-inverted source timestamp
- missing OHLCV / kline context
- missing persisted OHLCV readiness metadata
- missing data quality score
- missing data quality score owner
- insufficient evidence completeness
- incomplete SourceTrace
- incomplete numeric source ownership
- missing entry source reason
- missing stop source reason
- missing TP source reason
- missing RR rule ref
- missing rule id
- missing rule version
- missing conflict family state
- missing liquidity evidence
- missing multi-timeframe evidence
- missing event evidence status
- missing wick evidence status
- missing rollback-safe evidence trail
- runtime SourceTrace field not populated from source-owned evidence

`INCOMPLETE` remains missing-evidence context only. It is not production `VALID`, not readiness, not executable state, not a trade instruction, and not an order or execution surface.

## BLOCKED Requirements

Future output must remain `BLOCKED` when no-go, forbidden, unsafe, or action-surface evidence appears.

Required `BLOCKED` cases include:

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

## Risk Action Guard Boundary Confirmation

Risk Action Guard boundaries remain preserved:

- Stampede must not become reverse / new-position / opportunity-push display.
- Wick-only must not become trend reversal.
- Deteriorating liquidity must not become one-shot market exit instruction.
- Missing event evidence must not display as no risk.
- Liquidity stress must not display as opportunity.
- High risk alone must not mean direct stop loss, reverse, or new position.

These boundaries apply to future SourceTrace runtime field population, source-owned candidate generation, BoundaryCandidate service paths, ExecutionPlan display/readiness discussion, dashboard/display wording, docs, tests, and any production wiring preparation.

## Recommended Next Step

Recommended next step after P141 is STOP unless a separately authorized issue exists.

If work continues, the safest next line is a documentation-only source-owned candidate input contract and runtime SourceTrace field requirements matrix. That future issue should define the exact runtime fields, owner/ref/timeframe/freshness/rule requirements, Risk Action Guard decision table, expected tests, rollback path, and still-blocked implementation files before any Java change is considered.

P141 does not authorize that future issue. P141 does not authorize implementation.

## Still-Blocked Paths

The following paths remain blocked after P141:

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

Rollback for P141 is limited to:

- remove `docs/PHASE_BACKEND_P141_SOURCETRACE_RUNTIME_GAP_AUDIT.md`
- restore `docs/P141.md` only if the PR is abandoned before merge

Rollback must not touch production Java, test source, `dashboard.html`, controller, endpoint, API, schema, config, service, mapper, runtime data, live market data, external data, readiness, order, execution, scheduler, automation, or auto-trading paths.

If a future change uses P141 to widen scope without authorization, rollback must restore the last approved P140 production wiring preparation scope gate and keep all still-blocked paths blocked.

## Boundary Confirmations

- P141 is documentation-only read-only audit work.
- P141 removes the placeholder `docs/P141.md`.
- P141 adds one SourceTrace runtime gap audit document.
- P141 does not modify production Java.
- P141 does not modify test source.
- P141 does not modify `dashboard.html`.
- P141 does not add dashboard UI code.
- P141 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P141 does not read runtime data.
- P141 does not read live market data.
- P141 does not fetch external data.
- P141 does not generate real entry / stop / TP / RR values.
- P141 does not upgrade ExecutionPlan readiness.
- P141 does not map to production `VALID`.
- P141 does not wire BoundaryCandidateService `VALID` production path.
- P141 does not call `BoundaryCandidateDTO.valid(...)`.
- P141 does not add order API.
- P141 does not add execution API.
- P141 does not add scheduler / automation / auto-trading.
- P141 does not authorize production wiring implementation.
- P141 does not merge the PR.

## Validation

P141 is documentation-only, so Maven may be skipped because no Java or test source is modified. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- audit coverage
- existing artifacts
- missing runtime gaps
- `INCOMPLETE` requirements
- `BLOCKED` requirements
- Risk Action Guard boundary confirmation
- recommended next step
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #393 / BACKEND-P141

P141 stops here. It does not merge the PR.
