# Provider Budget and Concurrency Contract

## Admission Layers

Every refresh is admitted through:

1. global requests-per-minute budget;
2. per-provider requests-per-minute budget;
3. per-provider/dataset/canonical-instrument minimum gap;
4. global provider concurrency;
5. independent AI concurrency;
6. provider circuit and Retry-After state;
7. single flight for the final request key.

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
slots are reserved. P3 is rejected before P2/P0 when capacity is tight. The
current implementation chooses bounded, zero-wait rejection with an explicit
reason instead of an unbounded executor queue; `maxQueuedCalls` remains an
observable upper-bound configuration for a future bounded queue and is not a
claim that queuing occurs today.

## Single Flight and Failure

One `ProviderRequestKey` has one owner call. Waiters share the result. Success,
failure, and timeout all remove the in-flight registration; a later request may
retry. Different instruments, timeframes, market types, or source versions are
independent.

`401/403` do not retry. `429` records Retry-After. `5xx` and timeout retries are
bounded. Health, circuit, budget usage, rejected priority, and sanitized reason
codes are observable. Secrets, auth headers, and full responses are never
recorded.

Tests use fixtures only; no live quota was consumed. Production readiness
remains `BLOCKED`.
