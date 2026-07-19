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

## Snapshot Retention Hard-Bound Final Closure

PR #1131 review comment `3610213592` identified that a long consumer freshness
TTL could be evaluated before dataset retention. The focused branch fix keeps
the stable snapshot key and sharing behavior while making retention the first
lookup gate:

| Gate | Branch result | Contract |
|---|---|---|
| Dataset retention | `PASS_OFFLINE_PENDING_REVIEW` | `DATASET_RETENTION` is the hard upper bound. `now >= staleUntil` returns `UNAVAILABLE` and conditionally removes only the matching entry. |
| Lookup order | `PASS_OFFLINE_PENDING_REVIEW` | Retention is checked before consumer freshness. `effectiveFreshUntil = min(requestedFreshUntil, staleUntil)`. |
| Long consumer TTL | `PASS_OFFLINE_PENDING_REVIEW` | A consumer TTL cannot keep a snapshot fresh or readable beyond retention. `metadata.expiresAt` is capped by the same boundary. |
| Short consumer TTL | `PASS_OFFLINE_PENDING_REVIEW` | A shorter TTL makes the snapshot `STALE_READABLE` while it remains inside retention. |
| Sharing and fallback | `PRESERVED_OFFLINE_PENDING_REVIEW` | Different consumer TTLs still share one stable key; existing cross-profile, cross-bucket stale fallback, and Single Flight fixtures remain in the focused suite. |
| Exact boundary | `PASS_OFFLINE_PENDING_REVIEW` | Lookup and purge agree at `staleUntil - 1ns`, `staleUntil`, and `staleUntil + 1ns`; the exact boundary is expired. |

The fix is not effective mainline evidence until review and merge. No provider
call was enabled or executed by this closure.

## Read-Only Plan and Execution-State Separation Closure

PR #1131 review comment `3610342417` identified that status reads and the real
scan scheduler shared a plan-building path that evaluated mutable profile
transitions. The branch now gives the two paths explicit contracts:

| Gate | Branch result | Contract |
|---|---|---|
| Read-only universe | `PASS_OFFLINE_PENDING_REVIEW` | `currentUniverse()` reads existing transition state with `current()` only. It cannot create state, advance recovery, change a profile, or write transition audit. |
| Read-only plan | `PASS_OFFLINE_PENDING_REVIEW` | `currentPlan()` is the only plan used by Dashboard, runtime-status, and single-asset profile reads. One hundred repeated reads produce zero transition mutations and zero audit rows. |
| Execution universe | `PASS_OFFLINE_PENDING_REVIEW` | `evaluateUniverseForExecution(scanCycleTraceId)` is the only universe path that calls `evaluate()`. A required cycle trace deterministically scopes each instrument evaluation. |
| Execution plan | `PASS_OFFLINE_PENDING_REVIEW` | `planForExecution(scanCycleTraceId)` is Scheduler-only. Every relevant canonical instrument is evaluated at most once per real scan cycle and all due datasets reuse that effective profile. |
| Recovery ownership | `PASS_OFFLINE_PENDING_REVIEW` | Recovery confirmation advances only through real Scheduler scan cycles. Dashboard and runtime-status reads cannot substitute for recovery cycles. |
| Audit ownership | `PASS_OFFLINE_PENDING_REVIEW` | Transition audit remains limited to a changed transition reached from the real execution path. Disabled schedulers, disabled provider calls, missing refresh ports, reads, and unchanged evaluations write zero rows. |
| Last effective reason | `PASS_OFFLINE_PENDING_REVIEW` | `current()` returns the reason and rule version from the last real evaluation without recomputing thresholds or mutating runtime state. |

The execution-path test raises BTC to `HIGH`, performs 100 read-only plan
queries, and then requires two real scan cycles to satisfy recovery hysteresis
and step down to `STANDARD`. Only the actual rise and downgrade write audit
rows. This remains branch-level offline evidence pending re-review and merge;
no provider, AI, notification, order, or trading call was enabled.

## Transition-State Publication Closure

PR #1131 review `4730635925` identified that read-only transition queries did
not share `evaluate()`'s synchronization boundary. The focused fix publishes
the mutable transition state through the same service monitor:

| Gate | Branch result | Contract |
|---|---|---|
| Transition publication | `PASS_OFFLINE_PENDING_REVIEW` | `evaluate()`, `current()`, and `currentProfile()` are synchronized on the same service object. |
| Partial evaluation visibility | `0` | A deterministic test blocks changed-transition audit while `evaluate()` holds the monitor; both read methods wait until the complete evaluation is published. |
| Snapshot coherence | `PASS_OFFLINE_PENDING_REVIEW` | Profile, reason, effective time, next downgrade time, and rule version are copied and returned under one lock acquisition. |
| Completed-state visibility | `PASS_OFFLINE_PENDING_REVIEW` | Thirty-two concurrent readers, each repeating 100 reads after execution completion, observe the same complete state. |
| Read mutation | `0` | Concurrent reads do not create state, evaluate thresholds, advance recovery, change timing, or modify the profile. |
| Read-only audit | `0` | Concurrent `current()` and `currentProfile()` calls add no transition audit rows. |

```text
TRANSITION_STATE_PUBLICATION: SAME_MONITOR_AS_EVALUATE
READ_ONLY_STATE_VISIBILITY: COHERENT_AFTER_COMPLETED_EXECUTION
MIXED_TRANSITION_SNAPSHOT_COUNT: 0
CURRENT_METHOD_MUTATIONS: 0
CURRENT_PROFILE_METHOD_MUTATIONS: 0
READ_ONLY_TRANSITION_AUDIT_ROWS: 0
```

The lock protects transition evaluation, transition state, result snapshot,
and the existing changed-transition audit boundary only. It does not include
Provider HTTP work, cache/snapshot refresh, Dashboard rendering, AI,
notifications, or trading. This is branch-level offline evidence pending final
re-review and merge.

## Runtime-Status Resilience and Audit-Before-Publication Closure

Two P2 review threads opened after the prior exact-Head pass. The focused
branch closure keeps status reads available when optional universe
configuration is invalid and makes changed transition publication depend on
successful audit persistence:

| Gate | Branch result | Contract |
|---|---|---|
| Optional watchlist count | `PASS_OFFLINE_PENDING_REVIEW` | A `RuntimeException` or null optional list yields count `0`; it does not fail the runtime-status response or expose configuration content. |
| Optional discovery count | `PASS_OFFLINE_PENDING_REVIEW` | A `RuntimeException` or null optional list yields count `0`; budgets, health, current plan, and existing transition state remain readable. |
| Read ownership | `PASS_OFFLINE_PENDING_REVIEW` | Repeated degraded count reads still use `currentPlan/current`, invoke no execution plan or transition evaluation, and perform no Provider or AI call. |
| Transition staging | `PASS_OFFLINE_PENDING_REVIEW` | Evaluation copies every published State field and mutates only the staged copy. Readers cannot observe it before publication. |
| Audit order | `PASS_OFFLINE_PENDING_REVIEW` | A changed transition writes its audit before `states.put`; the insert count must be exactly one. |
| Audit failure | `PASS_OFFLINE_PENDING_REVIEW` | Exception, zero rows, or an unexpected row count throws and preserves the entire previous published State. |
| Scheduler fail-closed | `PASS_OFFLINE_PENDING_REVIEW` | Execution-plan failure propagates before dataset iteration, so transition-audit failure produces zero refresh calls. |

```text
RUNTIME_STATUS_OPTIONAL_UNIVERSE_FAILURE: HTTP_200_WITH_ZERO_COUNT
OPTIONAL_UNIVERSE_COUNT_FAILURE: FAIL_SOFT_READ_ONLY
TRANSITION_STATE_PUBLICATION_ORDER: AUDIT_SUCCESS_THEN_STATE_PUBLISH
UNAUDITED_TRANSITION_PUBLICATION_COUNT: 0
AUDIT_INSERT_COUNT_REQUIRED: EXACTLY_1
AUDIT_FAILURE_STATE_RESULT: PREVIOUS_STATE_PRESERVED
AUDIT_FAILURE_PROVIDER_REFRESH_COUNT: 0
```

These are deterministic offline branch tests pending final reviewer re-review
and merged-main activation. They do not prove live Provider availability or
production readiness.

## Queued-Attempt Timeout Classification Closure

PR #1131 thread `PRRT_kwDOSc6fQc6SF53A` identified that the physical timeout
supervisor could fire while work was still queued and then misclassify local
executor pressure as a remote transport timeout. The branch now gives every
attempt an atomic phase and makes timeout classification depend on which phase
wins the compare-and-set race:

| Gate | Branch result | Contract |
|---|---|---|
| Attempt phase | `PASS_OFFLINE_PENDING_REVIEW` | `QUEUED -> LOCAL_ADMISSION -> REMOTE_IN_FLIGHT`; timeout and completion are one-winner atomic transitions. |
| Queue timeout | `LOCAL_ADMISSION` | `PROVIDER_EXECUTOR_QUEUE_TIMEOUT`; adapter, attempt budget, physical-start audit, remote retry, health failure, and circuit failure counts are zero. |
| Pre-remote timeout | `LOCAL_ADMISSION` | `PROVIDER_PRE_REMOTE_TIMEOUT`; the adapter-start CAS fails closed, no timeout retry or remote failure is recorded, and any existing completion audit keeps the local reason. |
| Remote timeout | `REMOTE_TRANSPORT` | `PROVIDER_TIMEOUT` is possible only after the adapter-start transition wins. Existing interruption, lease ownership, retry budget, health, and circuit tests remain passing. |
| HALF_OPEN | `PASS_OFFLINE_PENDING_REVIEW` | A queue/pre-remote timeout releases the permit without a remote attempt, so a later recovery probe remains available. |
| Race integrity | `PASS_100_ATOMIC_ITERATIONS` | Every controlled race yields either local timeout with zero adapter calls or remote timeout with exactly one adapter call; no mixed outcome is accepted. |
| Cancelled queue control | `PASS_OFFLINE_PENDING_REVIEW` | A `NEW` task that wins cancellation is cancelled and its exact control is removed from the bounded work queue under the admission boundary; running tasks never remove queued neighbours. |
| Repeated queue pressure | `PASS_20_TIMEOUTS` | With the worker still blocked, every completed queue timeout leaves `queuedCalls=0`, actual queue growth `0`, Single Flight cleared, and the P0 reserved slot available. |

```text
ATTEMPT_PHASE_MODEL: QUEUED_LOCAL_ADMISSION_REMOTE_IN_FLIGHT
QUEUE_TIMEOUT_CLASSIFICATION: LOCAL_ADMISSION
PRE_REMOTE_TIMEOUT_CLASSIFICATION: LOCAL_ADMISSION
REMOTE_TIMEOUT_CLASSIFICATION: REMOTE_TRANSPORT_ONLY_AFTER_ADAPTER_START
QUEUE_TIMEOUT_PROVIDER_HEALTH_FAILURE_COUNT: 0
QUEUE_TIMEOUT_CIRCUIT_FAILURE_COUNT: 0
QUEUE_TIMEOUT_RETRY_COUNT: 0
QUEUE_TIMEOUT_ADAPTER_CALL_COUNT: 0
QUEUE_TIMEOUT_BUDGET_ATTEMPTS: 0
CANCELLED_BEFORE_START_QUEUE_REMOVAL: IMMEDIATE_EXACT_CONTROL_REMOVAL
CANCELLED_QUEUE_SLOT_RECLAMATION: PASS
REPEATED_QUEUE_TIMEOUT_QUEUE_GROWTH: 0
P0_RESERVED_SLOT_AFTER_CANCELLED_P3: AVAILABLE
SINGLE_FLIGHT_AND_EXECUTOR_QUEUE_CLEANUP: CONSISTENT
PR_STATE_AT_QUEUE_SLOT_CLOSURE: OPEN_DRAFT_UNMERGED
QUEUE_SLOT_CLOSURE_HANDOFF: SUPERSEDED_BY_CALLER_REWRAP_AND_RETENTION_REVIEW
```

These are offline branch results. No real Provider or AI call was made, and
the closure is not effective mainline evidence until review and merge.

## Per-Caller Shared-Flight and Retention-Expiry Closure

Threads `PRRT_kwDOSc6fQc6SGsJ7` and `PRRT_kwDOSc6fQc6SGsJ9` identified two
caller-boundary gaps after the queue-slot closure: joined waiters returned the
owner's caller metadata, and refreshed metadata could advertise an expiry later
than cache retention. The focused branch fix preserves stable-key sharing while
separating physical and caller results:

| Gate | Branch result | Contract |
|---|---|---|
| Physical flight | `SHARED_ONCE` | One Provider call/retry chain, budget chain, circuit-permit lifecycle, health mutation path, and cache write remain owner-only. |
| Caller result | `PER_CALLER_REWRAPPED` | A waiter rereads cache with its own fresh TTL and receives its own trace-bearing metadata and request-result audit. |
| Caller audit | `PASS_CALLER_OWN` | Priority, base/effective profile, reason codes, and frequency-matrix version come from each caller; physical-attempt start/end audits remain one owner chain. |
| Waiter fallback/failure | `PASS_FAIL_CLOSED` | Stale-readable data uses waiter metadata; an unavailable cache uses only the shared physical status/error and never returns owner caller metadata. |
| READY expiry | `PASS_RETENTION_BOUNDED` | `min(fetchTime + callerFreshTtl, fetchTime + datasetRetention)`. |
| EMPTY_CONFIRMED expiry | `PASS_RETENTION_BOUNDED` | Uses the same bound as READY. |
| Cache/peek/waiter expiry | `PASS_RETENTION_BOUNDED` | Cache hit, stale fallback, read-only peek, and waiter rewrap cannot advertise a time beyond retention. |
| Short-owner sharing | `PASS` | The owner's short returned expiry does not permanently cap a later caller before retention. |
| Overflow | `PASS_FAIL_CLOSED` | Caller-expiry overflow falls back to the valid retention boundary. |

```text
SHARED_FLIGHT_PHYSICAL_RESULT: SHARED_ONCE
SHARED_FLIGHT_CALLER_RESULT: PER_CALLER_REWRAPPED
WAITER_TRACE_OWNERSHIP: CALLER_OWN
WAITER_PROFILE_AUDIT: CALLER_OWN
WAITER_TTL: CALLER_OWN
WAITER_PROVIDER_CALL_COUNT: 0_ADDITIONAL
WAITER_REMOTE_HEALTH_MUTATION_COUNT: 0_ADDITIONAL
WAITER_CIRCUIT_SETTLEMENT_COUNT: 0_ADDITIONAL
READY_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
EMPTY_CONFIRMED_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
CACHE_HIT_EXPIRY: MIN_CALLER_TTL_AND_DATASET_RETENTION
EXPIRY_AFTER_RETENTION_COUNT: 0
PR_STATE: OPEN_DRAFT_UNMERGED
NEXT_TASK: Reviewer P3-CALL1 Caller Rewrap and Retention Expiry Final Re-review
```

These are deterministic offline branch results pending Reviewer re-review and
merged-main activation. No real Provider, AI, notification, order, position
mutation, or trading action was run.

## Explicit Non-Claims

- No live provider readiness was proven.
- No dynamic exchange-wide discovery exists.
- No AI correctness or availability was proven.
- No notification was delivered.
- No trading capability was added.
- No live provider or AI call was made while closing Reviewer Rounds 1, 2, 3,
  the snapshot-retention review comment, the read-only plan review comment, or
  the transition-state publication review.
- Production readiness remains `BLOCKED`.
