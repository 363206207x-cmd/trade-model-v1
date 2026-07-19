# Provider Call Orchestration Audit

## Audit Scope

- Baseline: `230528b0942737275a397323bcfff874541e2ea8`
- Package: `P3-CALL1`
- Mode: offline coordination and existing-contract adoption
- Real provider, CoinGlass, AI, Telegram, scheduler, position mutation, and order calls: disabled
- Production readiness: `BLOCKED`

## Existing Owner Audit

| Capability | Existing owner | Decision | P3-CALL1 treatment |
|---|---|---|---|
| Public OHLCV acquisition | `PublicOhlcvProvider` and routed Binance/Kraken adapters | `REUSE_AS_IS` | Coordinated adapter path only; no second HTTP owner |
| Authoritative OHLCV persistence | `PersistedOhlcvIngestionService` | `REUSE_AS_IS` | Remains the only writer to `tm_persisted_ohlcv_bar` |
| Persisted decision OHLCV | `AuthoritativePersistedDecisionOhlcvSource` | `REUSE_AS_IS` | No decision cutoff implementation added |
| Price/derivatives snapshots | Existing provider snapshot services | `ADAPT` | Canonical keys, common cache, budget, health, and query/refresh boundaries |
| Dashboard Home | `DashboardHomeServiceImpl` | `ADAPT` | Uses read-only snapshot `peek`; page load cannot refresh a provider |
| Decision/analysis | Existing Decision and Analysis services | `ADAPT` | Consume normalized snapshots/persisted OHLCV; no direct new client |
| Asset state | `tm_asset_state` and existing services | `REUSE_AS_IS` | No second state machine or table |
| Opportunity | Existing Opportunity owner | `REUSE_AS_IS` | New notification policy creates eligibility only, not records or trades |
| Candidate/waiting trigger | Existing AssetState plus runtime `AutoCandidateRegistry` | `ADAPT` | Auto-candidate storage is runtime-only; no migration |
| Position monitor | `PositionMonitorServiceImpl` | `ADAPT` | Uses coordinated snapshot refresh; no position mutation added |
| Push Recheck | `PushRecheckServiceImpl` | `ADAPT` | Uses coordinated snapshots and remains review-only/fail-closed |
| Execution plan | Existing plan services and mappers | `REUSE_AS_IS` | No executable permission or new plan owner |
| User position | Existing manual position owner | `REUSE_AS_IS` | No automatic create/update/close path |
| AI roles/conflict | `GPT_FINAL`, `GEMINI_REVIEW`, `GROK_CHALLENGE`, existing conflict resolver | `REUSE_AS_IS` | Due policy only; all new adapters are NoCall |
| Confused/Hot Reset | Existing policies/services | `REUSE_AS_IS` | Signals may affect per-asset profile; algorithms unchanged |
| Notification/Push skeleton | Existing no-op boundaries | `ADAPT` | Domain eligibility and in-memory test collector; no sender/outbox |
| Watchlist | Existing user config plus `ConfiguredWatchlistAssetSource` | `ADAPT` | Replaceable bounded configuration, not a fixed six-asset contract |
| Discovery | No formal bounded owner | `MISSING` | Added configured, bounded, offline `DiscoveryUniverseSource` only |
| Provider source/data quality status | Existing enums | `REUSE_AS_IS` | No duplicate quality or source-status family |
| Clock/time basis | Existing UTC policies and injected clocks | `REUSE_AS_IS` | Due, cooldown, TTL, and hysteresis calculations use `Instant`/`Clock` |

## Architecture Result

The supported path is:

```text
Business read/worker
  -> ProviderSnapshotQueryService or ProviderSnapshotRefreshService
  -> ProviderCallCoordinator
  -> explicit Provider Adapter boundary
```

Business services do not gain direct Binance, CoinGlass, OpenAI, Gemini, xAI,
news, or Telegram clients. The architecture guard covers Dashboard, Decision,
Analysis, Position Monitor, Push Recheck, Plan, Opportunity, and Review Center
source files.

## Reviewer Round 1 Correctness Closure

PR #1131 Reviewer Round 1 (`4730021827`) identified five runtime correctness
gaps. The branch closes them with offline fixtures and fail-closed contracts:

| Gate | Branch result | Contract |
|---|---|---|
| Physical call lifecycle | `PASS_BOUNDED` | A dedicated bounded `ProviderCallExecutor` owns adapter work. The physical task acquires and releases its concurrency lease and records every attempt. A logical timeout requests interruption but does not claim the physical call stopped or release Single Flight early. |
| Stable snapshot sharing | `PASS` | `ProviderSnapshotKey` excludes consumer TTL and `timeBucket`; cache, health, and Single Flight use the stable provider/dataset/instrument/timeframe/source identity. Consumer TTL only evaluates freshness. |
| Four-timeframe OHLCV | `PASS_4_OF_4` | `5m`, `15m`, `1h`, and `4h` have independent minimum-gap identities and budget attempts. One timeframe failure is recorded without rewriting another result. |
| Candidate confirmation | `PASS` | Consecutive confirmation follows `CandidateLogicIdentity`; changing market evidence updates `latestEvidenceHash` without resetting unchanged strategy/rule/direction/trigger logic. |
| Market identity | `PASS_END_TO_END` | Refresh services receive the scan plan's `CanonicalInstrumentId`. SPOT and PERPETUAL never share cache, audit, OHLCV, price, or derivatives identity; an unavailable perpetual price/OHLCV adapter fails as `NOT_CONFIGURED`, never as a spot fallback. |

Retries are physical attempts, not logical bookkeeping: every 5xx or timeout
attempt reserves budget and emits its own sanitized audit lifecycle. A timeout
retry cannot start until the preceding physical attempt has actually ended.
`401`, `403`, and `429` remain non-retryable; `429` preserves Retry-After.

These are branch-level offline test results. Only merged main establishes an
effective capability, and no real provider availability is claimed.

## Explicit Non-Claims

- No live provider readiness was proven.
- No dynamic exchange-wide discovery exists.
- No AI correctness or availability was proven.
- No notification was delivered.
- No trading capability was added.
- No live provider or AI call was made while closing Reviewer Round 1.
- Production readiness remains `BLOCKED`.
