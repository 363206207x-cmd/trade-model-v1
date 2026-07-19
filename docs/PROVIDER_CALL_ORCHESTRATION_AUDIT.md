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
| Physical call lifecycle | `PASS_BOUNDED` | A dedicated bounded `ProviderCallExecutor` owns adapter work. The physical task acquires and releases its concurrency lease and records every attempt. Only the physical attempt timeout supervisor requests interruption; a logical caller timeout only ends that caller's wait. |
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

## Reviewer Round 2 Focused Closure

PR #1131 Reviewer Round 2 (`4730247371`) identified three remaining runtime
correctness gaps. They are closed on this branch with offline deterministic
tests:

| Gate | Branch result | Contract |
|---|---|---|
| Shared-flight cancellation ownership | `PASS` | The owner request fixes each physical attempt timeout. A joined caller's wait timeout or thread interrupt returns only for that caller and cannot cancel, retry, remove, or release the shared physical flight. Physical timeout supervision remains interruptible and a bounded retry may finish after the original caller returns. |
| Local/remote failure isolation | `PASS` | `ProviderFailureClassifier` separates local admission, budget, concurrency, and configuration outcomes from remote rate-limit, auth, transport, server, and payload failures. Local rejection remains audited but does not increment the provider circuit or mark remote health down. `429` applies Retry-After without circuit failure; real transport/5xx/payload failures remain circuit inputs. |
| Market-aware OHLCV due state | `PASS` | The dedicated persisted-bar lookup binds symbol, timeframe, persisted provider, and `provider_market_type`. `SPOT` rows cannot suppress `USDT_PERP` refresh or produce perpetual `READY`; each of `5m`, `15m`, `1h`, and `4h` retains canonical identity, provider, market type, and source version. |

The current perpetual OHLCV adapter remains unconfigured. In the absence of a
matching valid `USDT_PERP` persisted bar, all four timeframes enter
`CoordinatedOhlcvSnapshotService` and return
`PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED`; no spot fallback is used.

## Reviewer Round 3 Final Focused Closure

PR #1131 Reviewer Round 3 (`4730325751`) identified one final circuit lifecycle
gap. It is closed on this branch with an explicit, one-shot
`ProviderCircuitPermit` and offline deterministic tests:

| Gate | Branch result | Contract |
|---|---|---|
| HALF_OPEN permit ownership | `PASS` | Each physical attempt acquires its own circuit permit. Only a HALF_OPEN owner carries the unique probe token; logical callers and Single Flight waiters never own it. |
| Local rejection release | `PASS` | Executor, concurrency, budget, per-symbol gap, local configuration, pre-attempt cancellation, and shutdown cancellation of queued work release the probe without incrementing remote failures or closing the circuit as success. |
| Remote reachable settlement | `PASS` | `429` applies Retry-After and `401/403` auth responses record remote reachability; both settle the probe without joining the 5xx availability counter. |
| Remote terminal state | `PASS` | Success and `EMPTY_CONFIRMED` close the circuit. Physical timeout, transport failure, `5xx`, malformed response, and invalid payload reopen it. |
| Idempotency and retry | `PASS` | A permit settles once. Every retry reacquires circuit permission for its own physical attempt; an OPEN circuit cannot be bypassed by a background retry. |

No completed fixture path retains `HALF_OPEN + probeClaimed`; permanent
HALF_OPEN count is zero. This remains branch-level offline evidence pending the
final merge-readiness review.

## Explicit Non-Claims

- No live provider readiness was proven.
- No dynamic exchange-wide discovery exists.
- No AI correctness or availability was proven.
- No notification was delivered.
- No trading capability was added.
- No live provider or AI call was made while closing Reviewer Rounds 1, 2, or 3.
- Production readiness remains `BLOCKED`.
