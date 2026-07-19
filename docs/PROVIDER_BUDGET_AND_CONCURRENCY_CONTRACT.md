# Provider Budget and Concurrency Contract

## Admission Layers

Every refresh is admitted through:

1. global requests-per-minute budget;
2. per-provider requests-per-minute budget;
3. per-provider/dataset/canonical-instrument minimum gap;
4. global provider concurrency;
5. independent AI concurrency;
6. provider circuit and Retry-After state;
7. single flight for the stable snapshot key;
8. bounded executor admission.

Default internal budget is 80% of advertised capacity and emergency reserve is
20%. The normal priority thresholds reject in this order:

```text
P3_DISCOVERY -> P1_WATCHLIST -> P2_CANDIDATE -> P0_POSITION
```

Only a system-selected `EMERGENCY` profile may consume emergency reserve.
Normal discovery cannot consume it. CoinGlass budget exhaustion does not stop
the separate Binance position-price safety path.

## Concurrency

Default maximums are eight provider calls and three AI calls. Higher-priority
slots are reserved. P3 is rejected before P2/P0 when capacity is tight.
`ProviderCallExecutor` is a dedicated bounded `ThreadPoolExecutor`: worker count
comes from `maxConcurrentProviderCalls`, queue capacity comes from
`maxQueuedCalls`, named provider threads are used, rejection is fail-closed,
and application shutdown explicitly terminates the executor. Provider adapter
work never runs on the ForkJoin common pool.

Every physical attempt owns its concurrency lease inside the worker and
releases it in `finally`. The owner request fixes the physical attempt timeout;
its supervisor alone may request interruption. A waiting caller timeout is a
separate logical deadline: it returns stale fallback or `PROVIDER_TIMEOUT` to
that caller only and cannot set the physical timeout flag, cancel or retry the
flight, release its lease, or remove Single Flight. An interrupted waiter
preserves its interrupt flag without cancelling the shared call. Client-level
connect/read/request timeouts remain mandatory when real HTTP adapters are
introduced.

Each submitted attempt has one atomic execution phase:

```text
QUEUED -> LOCAL_ADMISSION -> REMOTE_IN_FLIGHT -> COMPLETED
```

The timeout supervisor competes with those transitions by compare-and-set. A
timeout that wins in `QUEUED` becomes `PROVIDER_EXECUTOR_QUEUE_TIMEOUT`; a
timeout that wins in `LOCAL_ADMISSION` becomes
`PROVIDER_PRE_REMOTE_TIMEOUT`. Both are local admission outcomes. They call no
adapter, cause no provider-health or circuit failure, and cannot enter timeout
retry. A pure queue timeout also consumes no attempt budget and writes no
physical-attempt start audit. Only a timeout that wins after
`REMOTE_IN_FLIGHT` may become `PROVIDER_TIMEOUT` and retain the existing remote
transport, bounded retry, health, and circuit behavior.

Cancellation of a still-`QUEUED` physical task is owned by
`ProviderCallExecutor`. The task state first wins `NEW ->
CANCELLED_BEFORE_START`; the executor then cancels and removes that exact
control `FutureTask` from the bounded work queue inside the admission boundary.
The logical completion is published after the lock is released. Consequently
`queuedCalls` reflects the reclaimed slot immediately, repeated queue timeouts
cannot accumulate dead controls, and reserved priority capacity remains
available to a later `P0_POSITION` request. A running task receives only an
interrupt request and never removes a neighbouring queued control.

## Single Flight and Failure

One stable `ProviderSnapshotKey` has one owner lifecycle. Waiters with different
consumer TTLs or request time buckets share that lifecycle. Registration ends
only after the physical chain completes; logical caller timeout is not physical
completion. Different instruments, timeframes, market types, or source versions
remain independent.

The completed physical result is shared once; each logical waiter result is
rewrapped from a caller-TTL cache lookup. Therefore every waiter owns its trace,
priority, profile fields, frequency-matrix version, TTL, metadata, and
request-result audit. Rewrapping performs zero additional adapter calls, budget
reservations, circuit settlements, remote-health updates, retries, or
physical-attempt audits. If a successful physical result has already crossed
retention before caller rewrap, the waiter fails closed instead of returning
the owner's metadata.

Local admission, queue, budget, minimum-gap, concurrency, disabled, and
not-configured outcomes are local coordination state. They remain fail-closed
and audited, but do not increment the remote provider circuit or mark remote
provider health down. Repeated P3 discovery pressure therefore cannot open a
provider circuit that blocks a later P0 position refresh.

`401/403` are remote auth/configuration errors and do not retry or share the
5xx failure counter. `429` records Retry-After and remote degraded status but
does not open the provider circuit. Network/transport failure, physical
timeout, remote `5xx`, malformed response, and invalid remote payload are
remote circuit failures. `5xx` and timeout retries are
bounded and may start only after the previous physical attempt ends. Every
attempt receives a distinct attempt ID, budget reservation, concurrency lease,
and start/end audit event. Timeout and 5xx retries therefore consume real RPM;
retry accounting can never bypass the budget.

Each physical attempt also owns one idempotent `ProviderCircuitPermit`.
When an OPEN circuit cools down, the permit carries the exclusive HALF_OPEN
probe token. Remote success or `EMPTY_CONFIRMED` closes the circuit; remote
transport/physical timeout/`5xx`/invalid payload reopens it; `429` and auth
responses settle reachability without counting an availability failure; and a
local admission, budget, minimum-gap, concurrency, configuration, or shutdown
rejection releases the token without claiming a remote attempt. The next
request can therefore probe after local pressure clears or Retry-After expires.
A caller wait timeout or interrupt never receives or settles this permit.
Repeated permit settlement is a no-op, and no completed path may leave a
HALF_OPEN token claimed.

The per-symbol minimum-gap key contains provider, dataset, canonical
instrument, timeframe, and source version. Four due OHLCV requests (`5m`,
`15m`, `1h`, `4h`) can consume four independent budget attempts in one scan;
duplicates for the same timeframe remain blocked. SPOT and PERPETUAL are also
independent identities.

Health, circuit, budget usage, rejected priority, and sanitized reason codes
are observable. Secrets, auth headers, prompts, and full responses are never
recorded.

Tests use fixtures only; no live quota was consumed. Production readiness
remains `BLOCKED`.

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
SHARED_FLIGHT_PHYSICAL_RESULT: SHARED_ONCE
SHARED_FLIGHT_CALLER_RESULT: PER_CALLER_REWRAPPED
WAITER_TRACE_OWNERSHIP: CALLER_OWN
WAITER_PROFILE_AUDIT: CALLER_OWN
WAITER_TTL: CALLER_OWN
WAITER_ADDITIONAL_PROVIDER_CALLS: 0
WAITER_ADDITIONAL_BUDGET_RESERVATIONS: 0
WAITER_ADDITIONAL_CIRCUIT_SETTLEMENTS: 0
WAITER_ADDITIONAL_REMOTE_HEALTH_MUTATIONS: 0
```
