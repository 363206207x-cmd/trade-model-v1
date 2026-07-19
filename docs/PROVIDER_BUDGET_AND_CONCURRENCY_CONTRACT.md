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

## Single Flight and Failure

One stable `ProviderSnapshotKey` has one owner lifecycle. Waiters with different
consumer TTLs or request time buckets share that lifecycle. Registration ends
only after the physical chain completes; logical caller timeout is not physical
completion. Different instruments, timeframes, market types, or source versions
remain independent.

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
