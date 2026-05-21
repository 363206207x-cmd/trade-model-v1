# BACKEND-P123 No Runtime / No Live / No Production VALID Guard Expansion

## Baseline

- Branch context: PR #356 / Issue #355.
- Formal mainline title: BACKEND-P123 No Runtime / No Live / No Production VALID Guard Expansion.
- PR title note: PR #356 uses a shortened title as a platform workaround; Issue #355 and this document preserve the formal mainline title.
- Baseline commit: `829d0cc` (`chore: add P123 placeholder`), based on `747bed1` (`P122 Authorization Checklist (#354)`).
- Scope: documentation-only guard expansion for the D line, Production Authorization Preparation / Safety Gate.
- Placeholder removed: `docs/P123.md`.

## Files Changed

- `docs/PHASE_BACKEND_P123_NO_RUNTIME_NO_LIVE_NO_PRODUCTION_VALID_GUARD_EXPANSION.md`
- Removed `docs/P123.md`

No production Java, test source, runtime, dashboard, schema, config, controller, endpoint, service registration, readiness, order, execution, scheduler, automation, auto-trading, exchange-client, or external-data files are changed.

## Scope Statement

P123 strengthens the P122 authorization checklist before any future production-adjacent work.

P123 does not authorize production wiring. P123 does not authorize production candidate generation. P123 does not authorize order, execution, scheduler, automation, or auto-trading. P123 does not authorize runtime data reads, live market reads, external data fetches, production `VALID`, ExecutionPlan readiness upgrades, dashboard mutation, schema/config/controller changes, endpoint Java, service registration, or Spring bean registration.

The D line remains Production Authorization Preparation / Safety Gate only.

## Guard Expansion Coverage

### Expanded No-Runtime / No-Live / No-External-Data Gate

Future PRs must prove the read-only generator line still consumes already-ingested evidence only.

Blocked indicators:

- runtime data access
- live market data access
- external data fetches
- external data integration
- exchange clients
- exchange-specific client markers
- `WebClient`
- `RestTemplate`

Any appearance of these indicators in production, test helper, fixture, config, mapper, controller, dashboard, or schema paths is a no-go trigger unless a later issue explicitly authorizes that exact file and exact behavior.

### Expanded No Production VALID Gate

Future PRs must prove the line does not map review-only output to production validity.

Blocked indicators:

- production `VALID` mapping
- `BoundaryStatusEnum.VALID`
- `BoundaryCandidateDTO.valid(...)`
- status conversion from `REVIEW_ONLY_CANDIDATE` to production `VALID`
- readiness or completion language that implies production validity

Complete already-ingested snapshots can only become `REVIEW_ONLY_CANDIDATE` unless a separately authorized future line changes the contract.

### Expanded No BoundaryCandidateService VALID Path Gate

Future PRs must prove the read-only generator line does not wire into BoundaryCandidateService production paths.

Blocked indicators:

- `BoundaryCandidateService`
- BoundaryCandidateService `VALID` path
- resolver, mapper, assembler, or service wiring that passes read-only output into production candidate validation
- any production path that treats read-only output as executable candidate state

### Expanded No ExecutionPlan Readiness Gate

Future PRs must prove no read-only output upgrades ExecutionPlan readiness.

Blocked indicators:

- ExecutionPlan readiness surface
- trade-ready surface
- ready-to-trade equivalents
- any readiness flag derived from read-only candidate output

Review-only context must not imply readiness.

### Expanded No Dashboard / Schema / Config / Controller / Endpoint Mutation Gate

Future PRs must prove there is no UI, API, schema, or config mutation.

Blocked indicators:

- `dashboard.html` changes
- dashboard mutation
- schema mutation
- config mutation
- controller Java
- endpoint Java
- request mapping annotations
- any API path that exposes read-only generator output as production-ready state

### Expanded No Service Registration / Spring Bean Registration Gate

Future PRs must prove the read-only generator remains non-Spring and non-wired unless a later line explicitly authorizes registration.

Blocked indicators:

- `@Service`
- `@Component`
- `@Repository`
- Spring bean registration
- configuration class registration
- any injection path that makes the generator runtime-active

### Expanded No Order / Execution / Scheduler / Automation / Auto-Trading Gate

Future PRs must prove there is no trading action surface.

Blocked indicators:

- order API
- execution API
- scheduler behavior
- automation behavior
- auto-trading behavior
- buy / sell / open / close / reverse / signal language that implies action
- any path that can become a trade instruction

Risk Action Guard output remains review-only and cannot become a direct action instruction.

## Required Grep / Search Patterns For Future PR Review

Future PR reviewers must search changed files and relevant boundary files for these patterns:

```text
runtimeData
liveMarket
externalFetch
exchangeClient
binance
okx
coinglass
restTemplate
webClient
BoundaryCandidateDTO.valid
BoundaryStatusEnum.VALID
BoundaryCandidateService
ExecutionPlan readiness
dashboard.html
@Controller
@RestController
@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@PatchMapping
@Service
@Component
@Repository
order
execution
scheduler
automation
autoTrading
auto-trading
buy
sell
open
close
reverse
signal
```

Suggested review workflow:

```text
rg -n "runtimeData|liveMarket|externalFetch|exchangeClient|binance|okx|coinglass|restTemplate|webClient" <changed-files>
rg -n "BoundaryCandidateDTO\\.valid|BoundaryStatusEnum\\.VALID|BoundaryCandidateService|ExecutionPlan readiness" <changed-files>
rg -n "dashboard\\.html|@Controller|@RestController|@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping" <changed-files>
rg -n "@Service|@Component|@Repository" <changed-files>
rg -n "order|execution|scheduler|automation|autoTrading|auto-trading|buy|sell|open|close|reverse|signal" <changed-files>
```

Matches are not automatically defects, but every match is a required review item. Any match that introduces a blocked path is a no-go trigger.

## Required CI / Focused Test Command Set

Future production-adjacent proposals must run this command set before authorization and again after any authorized change:

```text
./mvnw -q -Dtest=MarketReadOnlyNoRuntimeNoProductionValidGuardTest test
./mvnw -q -Dtest=MarketReadOnlyFixtureSnapshotReviewOnlyCandidateTest test
./mvnw -q -Dtest=MarketReadOnlyForbiddenInputBlockedTest test
./mvnw -q -Dtest=MarketReadOnlyMissingEvidenceFailClosedTest test
./mvnw -q -Dtest=InertMarketReadOnlyCandidateGeneratorTest test
./mvnw -q -Dtest=MarketReadOnlyEvidenceSnapshotDTOTest test
./mvnw -q -Dtest=MarketReadOnlyCandidateResultDTOTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

If any command fails, the future PR must stop until the failure is resolved within the authorized scope or rolled back.

## No-Go Triggers

Any of the following triggers requires stopping the phase and applying the documented rollback plan:

- runtime data read
- live market data read
- external data fetch
- external data integration
- exchange client
- `WebClient`
- `RestTemplate`
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` call
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` change
- schema change
- config change
- controller Java
- endpoint Java
- service registration
- Spring bean registration
- order API
- execution API
- scheduler behavior
- automation behavior
- auto-trading behavior
- buy / sell / open / close / reverse / signal behavior
- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion
- production candidate generation
- real entry / stop / TP / RR value generation

## Rollback Requirements

Future PRs must document rollback before implementation begins.

Rollback must:

- identify the last approved freeze point
- identify the exact files that can be reverted
- remove any no-go trigger introduced by the future PR
- restore inert, non-Spring, non-wired, review-only behavior
- restore `manualReviewRequired=true`
- restore `notTradeInstruction=true`
- restore `reviewMode=REVIEW_ONLY`
- restore status behavior where complete snapshots can only become `REVIEW_ONLY_CANDIDATE`, missing evidence becomes `INCOMPLETE`, and forbidden / no-go / Risk Action Guard blockers become `BLOCKED`

If rollback cannot be applied cleanly, the future PR must remain blocked and must not proceed into production-adjacent behavior.

## Still-Blocked Paths

The following paths remain blocked after P123:

- production candidate generation
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
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard mutation
- `dashboard.html` changes
- schema changes
- config changes
- controller / endpoint Java
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

## Boundary Confirmations

- P123 is documentation-only guard expansion.
- P123 removes the placeholder `docs/P123.md`.
- P123 adds one guard expansion document.
- P123 remains within the D line, Production Authorization Preparation / Safety Gate.
- P123 does not authorize production wiring.
- P123 does not authorize order, execution, scheduler, automation, or auto-trading.
- P123 does not modify production Java.
- P123 does not modify test source.
- P123 does not implement production candidate generation.
- P123 does not generate real entry / stop / TP / RR values.
- P123 does not read runtime data.
- P123 does not read live market data.
- P123 does not fetch external data.
- P123 does not add exchange clients, `WebClient`, or `RestTemplate`.
- P123 does not wire BoundaryCandidateService `VALID` production path.
- P123 does not call `BoundaryCandidateDTO.valid(...)`.
- P123 does not map to production `BoundaryStatusEnum.VALID`.
- P123 does not upgrade ExecutionPlan readiness.
- P123 does not modify `dashboard.html`.
- P123 does not modify schema.
- P123 does not modify config.
- P123 does not add controller / endpoint Java.
- P123 does not add service registration.
- P123 does not add order API.
- P123 does not add execution API.
- P123 does not add scheduler / automation / auto-trading.

## Validation

P123 is documentation-only, so Maven was skipped. Validation is limited to git diff checks confirming the placeholder removal and guard expansion document only.
