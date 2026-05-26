# P259 Push Channel Java Authorization Gate

## 1. 阶段定位

P259 是 docs-only Java authorization gate。

P259 不写 Java，不新增测试，不修改 DTO。

## 2. P259 不授权内容

P259 不授权：

- push channel Java implementation
- Telegram Java skeleton
- email Java skeleton
- webhook Java skeleton
- app notification Java skeleton
- local notification Java skeleton
- delivery service
- channel adapter
- channel registry
- scheduler
- API / endpoint / controller
- dashboard wiring
- config / schema / mapper
- MarketQuoteClient / BinanceMarketQuoteClient wiring
- external message sending

## 3. P260 可能进入的最小前提

未来 P260 如需进入 push channel Java skeleton，必须另开授权，并且只能考虑：

- disabled-by-default skeleton
- fail-closed behavior
- no external network call
- no message sending
- no scheduler activation
- no API / dashboard wiring
- no MarketQuoteClient / BinanceMarketQuoteClient wiring
- no runtime / live / external data read
- no order / execution / auto-trading
- no Readiness upgrade
- no point generation
- no entry / stop / TP / RR

## 4. Java Skeleton 最小安全语义

任何未来 Java skeleton 都必须保留：

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `externalPushSent=false` unless a later delivery implementation is explicitly authorized
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`

任何未来 Java skeleton 都必须从 Watchlist Pool candidate 边界开始，不能从 Display Slots 或默认六币构造 batch universe。

Risk Action Guard 必须位于 delivery 前。

## 5. 结论

P259 不授权 Java。

P260 只有在新 Issue 和新授权门明确允许后，才可以进入 push channel Java skeleton。
