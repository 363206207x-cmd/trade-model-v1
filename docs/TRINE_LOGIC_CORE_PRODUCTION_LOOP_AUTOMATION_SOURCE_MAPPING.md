# TRINE LOGIC Core Production Loop Automation Source Mapping

Status: `AUTHORIZATION_MAPPING_ONLY`

| Frozen requirement | Authoritative source | Existing owner to reuse | Successor acceptance boundary |
|---|---|---|---|
| Asset Pool is the sole continuous opportunity source | v4.1 Sections 3 and 6 | AssetPool / PoolItem / Opportunity | No fixed-symbol or out-of-pool promotion path |
| State-sensitive scheduling | v4.1 Sections 6 and 18 | Existing scheduler, AnalysisRun and Opportunity StateLog | observing 15m, candidate 5m, waiting_trigger 2m, triggered lightweight 1m |
| Promotion-gated expensive analysis | v4.1 Sections 4, 7-11 and 18 | Analysis/Evidence/Score/Decision/AI/Resolver/Validation owners | Full pool stays lightweight; no unconditional Three-AI loop |
| Source-owned closed multi-timeframe market facts | v4.1 Section 4 | Persisted OHLCV and provider source gate | Binance public SPOT 5m/15m/1h/4h only; no cross-provider fallback |
| Manual position separation | v4.1 Section 13 | UserPosition | Trigger/Final never creates or mutates a position |
| Active-position monitoring | v4.1 Section 13 | PositionMonitor / PositionMonitorLog | OPEN/PARTIALLY_CLOSED 30s; CLOSED excluded; no automatic action |
| One Message fact owner and bounded Telegram outlet | v4.1 Section 15.2 plus merged narrowing authorization | Message / ChannelDelivery / Telegram provider | Three in-app categories; two Telegram categories; no parallel stack |
| Confirmation lifetime idempotency | Owner-authorized runtime boundary | Existing Message/ChannelDelivery dedupe owner | user + planId + CONFIRMATION sends at most once per Final lifetime |
| Existing unmerged remediation evidence | Owner-preserved PR #1201 Head `b158b7a89a4fdb9bd2254a210ecd258e26032161` | No effective owner; audit evidence only | Review compatibility; do not treat branch code as merged truth |
| No automatic trading | v4.1 permanent boundary | Existing safety guards | Automatic trading capability count remains zero |

No source authorizes a new page, Figma/Mobile change, `nextScanAt` schema
field, private exchange API, deployment, secret read or Telegram send in this
authorization package.
