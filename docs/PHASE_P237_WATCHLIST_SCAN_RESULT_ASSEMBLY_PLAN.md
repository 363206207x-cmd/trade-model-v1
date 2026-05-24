# P237 Watchlist Scan Result Assembly Plan

## 1. 阶段定位

P237 是 Watchlist Scan Result Assembly 的方案文档。

P237 不实现 Java。

P237 不接 `MarketQuoteClient`。

P237 不启用 scheduler。

P237 不创建真实扫描。

P237 不生成 `ScanScore`。

## 2. 未来 Assembly 唯一职责

未来 Watchlist Scan Result Assembly 只能：

- 只消费 `RuntimeSourceReadResultDTO` / `WatchlistRuntimeSourceDTO`。
- 只输出 review-only / blocked / incomplete scan result skeleton。
- 只表达 source 是否可用于人工复核。
- 只表达 missing / unavailable / stale / blocked / review-only。
- 不计算分数。
- 不创建机会。
- 不触发推送。
- 不升级 readiness。
- 不生成点位。

## 3. 未来 Assembly 推荐数据流

```text
RuntimeSourceReadResultDTO / WatchlistRuntimeSourceDTO
-> WatchlistScanGuardValidator
-> WatchlistScanResultDTO
-> REVIEW_ONLY / BLOCKED / INCOMPLETE output
```

该数据流只允许表达 review-only（只允许复核）、blocked（阻断）或 incomplete（不完整）结果。

该数据流不得解释为真实 scan loop（扫描循环）、真实低频扫描、行情读取或机会生成。

## 4. 未来状态映射方案

未来状态映射可以按以下方式设计，但 P237 不实现：

- `RuntimeSourceReadResultDTO.INCOMPLETE` -> `WatchlistScanResultDTO.INCOMPLETE`
- `RuntimeSourceReadResultDTO.SOURCE_UNAVAILABLE` -> `WatchlistScanResultDTO.BLOCKED` 或 `WatchlistScanResultDTO.INCOMPLETE`
- `RuntimeSourceReadResultDTO.STALE_REVIEW_ONLY` -> `WatchlistScanResultDTO.REVIEW_ONLY` with stale reason
- `RuntimeSourceReadResultDTO.AVAILABLE_REVIEW_ONLY` -> `WatchlistScanResultDTO.REVIEW_ONLY`
- non-watchlist -> `BLOCKED_NOT_WATCHLIST`
- missing source -> `MISSING_RUNTIME_SOURCE`
- guard failure -> `GUARD_BLOCKED`
- exception -> `INCOMPLETE` / `SOURCE_UNAVAILABLE`

说明：

- 当前 `WatchlistScanResultDTO` 已有 `INCOMPLETE`、`BLOCKED_NOT_WATCHLIST` 和 `REVIEW_ONLY` 语义。
- `SOURCE_UNAVAILABLE` 可先通过 `INCOMPLETE` 或阻断原因表达，不得升级为可推送状态。
- `STALE_REVIEW_ONLY` 只能进入人工复核，不得生成分数或机会。
- `AVAILABLE_REVIEW_ONLY` 也只能表示可人工复核，不等于 Candidate Attention、Promote To Home、Push 或 readiness。

## 5. 未来 Assembly 仍禁止

未来 Assembly 仍禁止：

- 不输出真实 `ScanScore`。
- 不输出 Candidate Attention。
- 不输出 Promote To Home。
- 不输出 Opportunity Push。
- 不输出 entry / stop / TP / RR。
- 不升级 readiness。
- 不创建 trading action。
- 不调用 `MarketQuoteClient`。
- 不调用 `BinanceMarketQuoteClient`。
- 不被 scheduler 调用。
- 不进入 scan loop。

## 6. 结论

P237 不授权 P238 直接写 assembly Java。

P238 应先做 Java authorization gate 或最小 skeleton plan。
