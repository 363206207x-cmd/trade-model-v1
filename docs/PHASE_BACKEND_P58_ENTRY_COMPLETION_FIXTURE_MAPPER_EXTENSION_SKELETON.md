# BACKEND-P58 Entry Completion Fixture Mapper Extension Skeleton

## Baseline

- Branch context: PR #213 / Issue #212.
- Baseline commit: `ad4cb12` (`docs: authorize entry completion fixture mapper extension`).
- Scope: fixture-only / test-scope extension for deterministic synthetic evidence shapes.
- P58 keeps production code unchanged and does not add production completion, production adapters, readiness wiring, schema/dashboard persistence, external integrations, order APIs, or auto-trading.

## Files Changed

- Extended `SourceTraceEntryPositiveCompletionFixtureInput` under test sources.
- Extended `SourceTraceEntryPositiveCompletionFixtureFactory` under test sources.
- Extended `SourceTraceEntryPositiveCompletionFixtureMapper` under test sources.
- Expanded `SourceTraceEntryPositiveCompletionFixtureFactoryMapperTest`.
- Added `docs/PHASE_BACKEND_P58_ENTRY_COMPLETION_FIXTURE_MAPPER_EXTENSION_SKELETON.md`.
- Removed placeholder `docs/P58_PLACEHOLDER.md`.

## Fixture Mapper Extension Behavior

P58 adds fixture-only synthetic evidence shape support:

- `fixtureOnlyEvidenceShape`
- `fixtureOnlyEvidenceRefs`

These values are deterministic synthetic metadata for tests only. They are recorded as fixture-only missing-field markers so output remains visibly non-production:

- `fixtureOnlyEvidenceShape:<shape>`
- `fixtureOnlyEvidenceRef:<ref>`

The extension preserves the existing mapper behavior:

- output starts from `SourceTraceEntryPositiveCompletionContractDTO` fail-closed defaults
- synthetic fixture output may carry `POSITIVE_FIXTURE_READY` metadata only
- fixture metadata does not imply runtime SourceTrace completion
- fixture metadata does not imply BoundaryCandidateService `VALID`
- fixture metadata does not imply ExecutionPlan readiness
- runtime-like source tags downgrade fail-closed
- mutable input and output evidence remain defensively copied

## Default Safety Behavior

The fixture input/factory/mapper chain continues to preserve:

- `REVIEW_ONLY`
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `sourceTraceEntryCompleted=false`
- `completionReady=false`
- fail-closed default DTO state for default factory output
- non-production fixture-only downgrade reason for synthetic metadata
- unsafe downgrade reason for runtime-like source tags

New synthetic evidence shape metadata is explicitly fixture-only and non-production. It does not become entry ownership, runtime readiness, dashboard/schema persistence, order readiness, automation readiness, or auto-trading readiness.

## Runtime-Like Source Tag Downgrade

P58 keeps existing downgrade coverage and adds fixture-only coverage for additional runtime-like tags:

- `REAL_ENTRY_PRICE_RUNTIME`
- `STOP_TP_RUNTIME`
- `RISK_REWARD_RUNTIME`
- `BOUNDARY_CANDIDATE_VALID_RUNTIME`
- `EXECUTION_PLAN_RUNTIME`

These tags downgrade to incomplete / unsafe / fail-closed output. The mapper does not populate entry source fields when runtime-like source tags are present.

## Defensive-Copy Safety

P58 confirms defensive-copy behavior for:

- mutable `missingFields`
- mutable `sourceTags`
- mutable `fixtureOnlyEvidenceRefs`
- mapped DTO `missingFields`

Mutable fixture inputs cannot mutate stored fixture input state or mapped DTO output after construction.

## Test-Scope Boundary

The extension remains under test sources only. The helpers:

- have no Spring service/component annotations
- implement no production resolver, adapter, validator, assembler, readiness, dashboard, schema, order, automation, or external data boundary
- expose no order / execution / close / reverse / auto-trading / trade-ready method names
- do not add or require production adapter implementation
- do not add production completion contract implementation

## Still-Blocked Production Paths

These remain blocked after P58:

- production completion implementation
- production entry ownership adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- DTO/factory/mapper registration as Spring services
- resolver wiring
- assembler wiring
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

Required verification for P58:

```bash
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

- Fixture-only / test-scope only.
- Production Java unchanged.
- No production completion implemented.
- No production adapter added.
- No `DefaultSourceTraceEntryOwnershipAdapter` added.
- No production `DefaultSourceTraceEntryCompletionContract` added.
- DTO/factory/mapper are not registered as Spring services.
- DTO/factory/mapper are not wired into resolver, assembler, validation, readiness, dashboard, schema, order, automation, or external data paths.
- Runtime SourceTrace fields are not populated.
- Full SourceTrace is not completed in runtime.
- BoundaryCandidateService `VALID` production path remains unwired.
- ExecutionPlan readiness is not upgraded.
- Schema and `dashboard.html` are unchanged.
- External data integration, order API, and auto-trading are not added.
- Real entry, stop, take-profit, and risk-reward values are not generated.
- Placeholder `docs/P58_PLACEHOLDER.md` was removed.

## Risk Action Guard

- High risk does not directly mean close, reverse, or open.
- Wick / pin-bar evidence does not confirm trend reversal.
- Liquidity stress / stampede must block opportunity push and require review.
- Missing event data is not no event risk.
- Multi-timeframe agreement alone does not complete SourceTrace.
