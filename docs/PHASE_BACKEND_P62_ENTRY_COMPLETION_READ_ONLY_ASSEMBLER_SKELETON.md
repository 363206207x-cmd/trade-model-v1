# BACKEND-P62 Entry Completion Read-Only Assembler Skeleton

## Baseline

- Branch context: PR #221 / Issue #220.
- Baseline commit: `09d3841` (`docs: design entry completion read-only boundary`).
- Scope: read-only assembler Java skeleton, read-only request DTO, focused tests, and result documentation.
- P62 keeps production wiring unchanged and does not add production completion, production adapters, readiness wiring, schema/dashboard persistence, external integrations, order APIs, or auto-trading.

## Files Changed

- Added `SourceTraceEntryReadOnlyCompletionRequest`.
- Added `SourceTraceEntryReadOnlyCompletionAssembler`.
- Added `SourceTraceEntryReadOnlyCompletionAssemblerTest`.
- Added `docs/PHASE_BACKEND_P62_ENTRY_COMPLETION_READ_ONLY_ASSEMBLER_SKELETON.md`.
- Removed placeholder `docs/P62.md`.

## Read-Only Assembler Behavior

The assembler is an unwired read-only skeleton:

- starts from `SourceTraceEntryPositiveCompletionContractDTO` fail-closed defaults
- accepts explicitly provided internal read-only inputs only
- copies mutable request evidence defensively
- maps safe metadata into the DTO only as review metadata
- keeps real entry price unset
- keeps `sourceTraceEntryCompleted=false`
- keeps `completionReady=false`
- keeps output `REVIEW_ONLY`
- keeps `manualReviewRequired=true`
- keeps `notTradeInstruction=true`
- downgrades null, missing, stale, ambiguous, runtime-like, or unsafe inputs fail-closed
- remains unregistered as a Spring service
- implements no production boundary interfaces
- is not wired into resolver, validation, readiness, dashboard, schema, order, automation, or external data paths

For complete safe read-only input, the assembler may set `POSITIVE_DESIGN_REVIEW_ONLY` metadata, but still returns `COMPLETION_UNWIRED` with missing fields:

- `readOnlyCompletionProductionPathUnwired`
- `entryPriceSource`

## Fail-Closed Guard Coverage

Focused tests cover:

- null input
- missing completion path
- missing source type / timeframe / reason / ref
- missing provenance fields
- missing freshness fields
- missing conflict evidence
- stale freshness
- future observed time / clock inversion
- runtime-like tags: latest price, raw kline, AI text, dashboard text, external data, order data, execution data
- duplicate or ambiguous source refs
- liquidity stress and stampede
- missing event data
- multi-timeframe agreement only
- wick / pin-bar evidence only
- true conflict flags one at a time
- defensive copying of mutable read-only evidence
- no order / execution / close / reverse / auto-trading / trade-ready method names
- no Spring service/component annotations
- no production boundary interface implementation
- production adapter and production completion contract remain absent

## Default Safety Behavior

P62 preserves:

- fail-closed DTO defaults
- `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- no real entry price generation
- no real stop / take-profit / risk-reward generation
- no runtime SourceTrace field population
- no BoundaryCandidateService `VALID`
- no ExecutionPlan readiness upgrade

## Still-Blocked Production Paths

These remain blocked after P62:

- production completion implementation
- production entry ownership adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- assembler registration as a Spring service
- resolver wiring
- validation wiring
- readiness wiring
- dashboard wiring
- schema or database persistence wiring
- order wiring
- automation wiring
- external data wiring
- runtime SourceTrace field population
- full SourceTrace runtime completion
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrade
- schema changes
- `dashboard.html` changes
- external data integration
- order API
- auto-trading
- real entry, stop, take-profit, or risk-reward value generation

## Verification

Required verification for P62:

```bash
./mvnw -q -Dtest=SourceTraceEntryReadOnlyCompletionAssemblerTest test
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionFixtureFactoryMapperTest test
./mvnw -q -Dtest=SourceTraceEntryPositiveCompletionContractDTOTest test
./mvnw -q -Dtest=EntryCompletionPositiveContractFixtureSkeletonTest test
./mvnw -q -Dtest=EntryCompletionOwnershipContractFixtureTest test
./mvnw -q -Dtest=EntryCompletionFixtureMatrixGuardTest test
./mvnw -q -Dtest=FailClosedSourceTraceEntryOwnershipValidatorTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

## Boundary Confirmations

- Read-only skeleton only.
- No Spring service registration added.
- No resolver wiring added.
- No validation wiring added.
- No readiness wiring added.
- No dashboard/schema wiring added.
- No order, automation, or external data wiring added.
- No production completion implemented.
- No production adapter added.
- No `DefaultSourceTraceEntryOwnershipAdapter` added.
- No production `DefaultSourceTraceEntryCompletionContract` added.
- Runtime SourceTrace fields are not populated.
- Full SourceTrace is not completed in runtime.
- BoundaryCandidateService `VALID` production path remains unwired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are unchanged.
- External data integration, order API, and auto-trading are not added.
- Real entry, stop, take-profit, and risk-reward values are not generated.
- Placeholder `docs/P62.md` was removed.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
