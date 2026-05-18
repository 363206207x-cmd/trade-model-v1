# BACKEND-P44 Entry Completion Ownership Contract Fixture Pack

## Baseline

- Branch context: PR #185 / Issue #184.
- Baseline commit: `6841e48` (`docs: review entry completion fixture evidence`).
- Scope: maximum-safe fixture-only ownership contract pack for SourceTrace entry completion fields.
- Production adapter, production completion, readiness wiring, schema, dashboard, external integrations, order APIs, and auto-trading remain out of scope.

## Ownership Contract Coverage

### `sourceTraceEntryOwnershipCompletionPath`

- Required before any completed entry SourceTrace output can exist.
- Must remain missing/unwired in P44.
- Missing or unwired path keeps validation incomplete and completion output `COMPLETION_UNWIRED`.

### `entryPriceSource`

- Must be owned by a future rule-owned SourceTrace completion contract before it can be populated.
- `candidateEntryBoundary` may be fixture evidence for a candidate boundary, but it is not a completed `entryPriceSource`.
- No real entry price value is generated in P44.

### `entrySourceType`

- Must identify an allowed rule-owned source family.
- P44 fixture guards allow only the deterministic skeleton value `rule-owned-boundary`.
- Blank, unknown, or unsupported source types fail closed.

### `entrySourceTimeframe`

- Must match the runtime decision timeframe for the fixture contract.
- Blank, unknown, unsupported, or mismatched timeframes fail closed.

### `entrySourceReason`

- Must explain why the rule-owned candidate exists.
- Blank reason fails closed.
- Reason text is provenance only and cannot become readiness.

### `entrySourceRef`

- Must identify one unambiguous source reference.
- Blank, duplicate, or ambiguous references fail closed.
- A source ref does not become readiness without a completed SourceTrace ownership path.

### Candidate Boundary Semantics

- `candidateEntryBoundary` is required fixture metadata.
- It remains a rule-owned candidate boundary, not a real entry price and not a completed SourceTrace entry field.
- Missing boundary fails closed.

### Source Window / Rule ID / Rule Version / Source Ref Provenance

- `ruleId`, `ruleVersion`, and `sourceWindow` are required provenance fields in the P44 fixture contract.
- Missing provenance fails closed.
- Runtime symbol must match candidate symbol.
- Runtime timeframe must match candidate decision timeframe.

### Freshness Semantics

- `freshnessStatus=FRESH` is required for the fixture contract.
- Stale freshness status fails closed.
- Missing observed time or decision-create time fails closed.
- Observed time after decision-create time is treated as future/clock-inverted evidence and fails closed.

### Conflict Evidence Ownership

- Conflict flags remain nullable Boolean metadata.
- `null` means missing or unevaluated and fails closed.
- `true` means a conflict exists and fails closed.
- Explicit `false` does not substitute for SourceTrace completion.
- Stop, take-profit, risk-reward, liquidity, multi-timeframe, event, and wick conflict evidence must be owned by their respective future source families before completion can be considered.

### Event Absence Versus Missing Event Data

- Missing event data is not no event risk.
- Fixture evidence marked `MISSING_EVENT_DATA` fails closed through event conflict ownership.

### Liquidity Stress / Stampede Handling

- Liquidity stress and stampede evidence must block completion and require review.
- Fixture evidence marked `LIQUIDITY_STRESS` or `STAMPEDE` fails closed through liquidity conflict ownership.

### Multi-Timeframe Agreement Versus SourceTrace Completion

- Multi-timeframe agreement alone does not complete SourceTrace.
- Fixture evidence marked `MULTI_TIMEFRAME_AGREEMENT_ONLY` fails closed through multi-timeframe conflict ownership.

### Wick / Pin-Bar Evidence

- Wick or pin-bar evidence alone does not confirm trend reversal.
- Wick or pin-bar evidence alone does not complete SourceTrace.
- Fixture evidence marked `WICK_PIN_BAR_ONLY` fails closed through wick conflict ownership.

## Fixture Guard Coverage

P44 adds deterministic fixture-only guards proving:

- conflict flags explicitly `true` fail closed one at a time
- mixed null and false conflict flags fail closed
- freshness clock inversion fails closed
- future observed time fails closed
- stale freshness status fails closed
- runtime symbol mismatch with candidate symbol fails closed
- runtime timeframe mismatch with candidate decision timeframe fails closed
- unsupported `entrySourceType` fails closed
- unsupported `entrySourceTimeframe` fails closed
- duplicate or ambiguous `entrySourceRef` fails closed
- missing rule id, rule version, or source window fails closed
- liquidity stress and stampede block completion and require review
- missing event data fails closed and is not no event risk
- multi-timeframe agreement alone does not complete SourceTrace
- wick or pin-bar alone does not prove reversal or completion
- positive-looking fixture remains review-only until an explicit completed contract exists

## Implementation Decision

Implementation remains blocked after P44.

P44 defines additional ownership requirements and fixture guards, but it still does not create a positive completed SourceTrace contract, production adapter, production completion result, readiness transition, schema/dashboard persistence path, order path, or automation path.

## Still-Unwired Fields

These remain intentionally unwired after P44:

- `sourceTraceEntryOwnershipCompletionPath`
- `entryPriceSource`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- full SourceTrace completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- dashboard rendering or schema persistence of completed SourceTrace entry ownership
- production validation, readiness, order, automation, or external data paths
- external API, Coinglass, news, macro calendar, order API, and auto-trading paths

## Boundary Confirmations

- Fixture-only and deterministic only.
- No real entry, stop, take-profit, or risk-reward values are generated.
- No production entry ownership adapter is implemented.
- No `DefaultSourceTraceEntryOwnershipAdapter` is added.
- No production `DefaultSourceTraceEntryCompletionContract` is added.
- Resolver and assembler are not registered as production Spring services.
- Resolver and assembler are not wired into validation, readiness, dashboard, schema, order, or automation paths.
- Real SourceTrace fields are not populated.
- Full SourceTrace completion is not completed.
- BoundaryCandidateService `VALID` production path is not wired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are not modified.
- External data integration, order API, and auto-trading are not added.

## Tests

Run:

```bash
./mvnw -q -Dtest=EntryCompletionFixtureMatrixGuardTest test
./mvnw -q -Dtest=EntryCompletionValidationContextAssemblerTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryCompletionResolverTest test
./mvnw -q -Dtest=EntryOwnershipValidationCompletionContextTest test
./mvnw -q -Dtest=EntryCompletionOwnershipContractFixtureTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.

## Recommended Next Stage

The next safe stage should remain fixture-only unless explicitly authorized otherwise. A safe P45 boundary would be a contract-review freeze that decides whether P44 closed enough ownership gaps to design a positive completion contract, while still forbidding production adapter implementation and readiness wiring.
