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
releases it in `finally`. A waiting caller timing out requests interruption and
returns `PROVIDER_TIMEOUT`, but it cannot release that lease or remove the
Single Flight while an uninterruptible adapter is still running. Client-level
connect/read/request timeouts remain mandatory when real HTTP adapters are
introduced.

## Single Flight and Failure

One stable `ProviderSnapshotKey` has one owner lifecycle. Waiters with different
consumer TTLs or request time buckets share that lifecycle. Registration ends
only after the physical chain completes; logical caller timeout is not physical
completion. Different instruments, timeframes, market types, or source versions
remain independent.

`401/403` do not retry. `429` records Retry-After. `5xx` and timeout retries are
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
