# BACKEND-P31 SourceTrace Entry Ownership Adapter DTO / Interface Skeleton Result

## Baseline

- Branch context: PR #154 / Issue #153.
- Baseline commit: `177c55d` (`docs: fix entry conflict metadata design`).
- Prior design dependency: P30A corrected the P30 conflict metadata contract before P31 implementation.

## Files Added

- `src/main/java/org/example/trademodel/dto/planboundary/EntryOwnershipRequest.java`
- `src/main/java/org/example/trademodel/dto/planboundary/RuleOwnedEntryCandidateDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/EntrySourceFreshnessDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/EntrySourceConflictDTO.java`
- `src/main/java/org/example/trademodel/service/SourceTraceEntryOwnershipAdapter.java`
- `src/test/java/org/example/trademodel/dto/planboundary/EntryOwnershipAdapterSkeletonDTOTest.java`
- `src/test/java/org/example/trademodel/service/SourceTraceEntryOwnershipAdapterTest.java`

The temporary `x31.txt` marker is removed from the final branch state.

## DTO / Interface Surfaces

### EntryOwnershipRequest

`EntryOwnershipRequest` is a request envelope for future SourceTrace entry ownership resolution. It keeps these objects separate:

- `RuntimeKlineContextDTO runtimeKlineContext`
- `RuleOwnedEntryCandidateDTO ruleOwnedEntryCandidate`
- `EntrySourceFreshnessDTO freshness`
- `EntrySourceConflictDTO conflict`
- `manualReviewRequired=true`
- `notTradeInstruction=true`

The default safety invariants remain true. P31 does not add executable behavior that can clear these flags.

### RuleOwnedEntryCandidateDTO

`RuleOwnedEntryCandidateDTO` carries the future rule-owned entry candidate metadata:

- `symbol`
- `decisionTimeframe`
- `candidateEntryBoundary`
- `entrySourceType`
- `entrySourceTimeframe`
- `entrySourceReason`
- `entrySourceRef`
- `ruleId`
- `ruleVersion`
- `sourceWindow`

This DTO does not calculate, infer, fetch, or populate real entry price values.

### EntrySourceFreshnessDTO

`EntrySourceFreshnessDTO` carries freshness metadata:

- `freshnessStatus`
- `staleReasonCode`
- `staleReasonText`
- `observedAtMs`
- `decisionCreateTimeMs`
- `missingFields`

Future Java implementation must fail closed when freshness metadata is missing, stale, incomplete, or internally inconsistent.

### EntrySourceConflictDTO

P31 applies the corrected P30A nullability design. The conflict flags are nullable `Boolean`, not primitive-only `boolean`:

- `conflictsWithStop`
- `conflictsWithTakeProfit`
- `conflictsWithRiskReward`
- `conflictsWithLiquidity`
- `conflictsWithMultiTimeframe`
- `conflictsWithEvent`
- `conflictsWithWick`

`null` means missing or unevaluated. A future implementation must fail closed when any required conflict flag is `null`. `false` means an explicit non-conflict was evaluated; it must not be confused with missing metadata.

The DTO also carries:

- `conflictReasons`
- `missingFields`

### SourceTraceEntryOwnershipAdapter

`SourceTraceEntryOwnershipAdapter` exposes only:

```java
SourceTraceEntrySourceOwnershipResult resolveEntryOwnership(EntryOwnershipRequest request);
```

No production implementation is added in P31.

## RuntimeKline Separation

P31 preserves the P29/P30 boundary that allows `RuntimeKlineContextDTO` to be present as request context while keeping the rule-owned entry candidate in a separate DTO. Future implementation must not derive entry ownership from:

- `latestPrice`
- raw `klineItems`
- quote snapshots
- AI text
- dashboard text
- execution/order state

## Fail-Closed Behavior

P31 is skeleton-only, but it freezes the future fail-closed contract:

- Missing request: fail closed.
- Missing runtime context: fail closed.
- Missing rule-owned candidate: fail closed.
- Missing candidate boundary or source metadata: fail closed.
- Missing freshness metadata: fail closed.
- Stale freshness metadata: fail closed.
- Missing conflict metadata: fail closed.
- Any nullable conflict flag equal to `null`: fail closed.
- Any explicit conflict flag equal to `true`: fail closed.
- Any attempt to convert the result into an order, close, reverse, execution, or auto-trading action: fail closed and remain manual review only.

## Still Unwired Fields

P31 does not populate SourceTrace entry fields and does not wire ownership into production readiness. These remain intentionally unwired:

- SourceTrace entry ownership completion path
- BoundaryCandidateService `VALID`
- ExecutionPlan readiness upgrades
- Entry/stop/take-profit/risk-reward/liquidity/multi-timeframe/event/wick executable values
- Dashboard rendering or schema persistence of completed SourceTrace entry ownership

## Boundary Confirmations

- Documentation, DTOs, interface, and focused skeleton tests only.
- No real entry price generation.
- No production adapter implementation.
- No `DefaultSourceTraceEntryOwnershipAdapter`.
- No change to `FailClosedSourceTraceEntrySourceOwnershipService`.
- No Java trading, order, close, reverse, execution, or auto-trading surface.
- No schema changes.
- No `dashboard.html` changes.
- No external data integration.
- No Coinglass, news, macro calendar, order API, or auto-trading changes.

## Tests

Focused tests are added for:

- DTO fields carrying the required skeleton metadata.
- `EntrySourceConflictDTO` flags defaulting to `null`, not `false`.
- Nullable conflict metadata distinguishing missing evaluation from explicit non-conflict.
- `EntryOwnershipRequest` keeping `RuntimeKlineContextDTO` separate from `RuleOwnedEntryCandidateDTO`.
- `SourceTraceEntryOwnershipAdapter` exposing only the entry ownership method.
- No required production adapter implementation.

Run:

```bash
./mvnw -q -Dtest=EntryOwnershipAdapterSkeletonDTOTest,SourceTraceEntryOwnershipAdapterTest test
```

## Risk Action Guard

This pack is not a trading implementation. It must remain manual-review-only and non-instructional:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- no entry/stop/take-profit/risk-reward generation
- no order placement
- no close/reverse action
- no auto-trading readiness upgrade
