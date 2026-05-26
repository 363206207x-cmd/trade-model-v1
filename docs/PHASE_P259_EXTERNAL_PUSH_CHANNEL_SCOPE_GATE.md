# P259 External Push Channel Scope Gate

## 1. 阶段定位

P259 定义未来 external push channel scope gate。

P259 不连接任何外部通道，不发送任何消息，不新增 Java skeleton。

## 2. 未来可讨论通道

未来 external push channel 可在另行授权后考虑：

- Telegram
- email
- webhook
- app notification
- local notification

这些只是未来 scope 候选，不是 P259 实现内容。

## 3. P259 明确不做

- 不新增 Telegram bot / client / sender。
- 不新增 email sender。
- 不新增 webhook caller。
- 不新增 app notification sender。
- 不新增 local notification sender。
- 不新增 channel adapter / channel registry / delivery service。
- 不新增 API / controller / endpoint。
- 不新增 scheduler。
- 不修改 dashboard。
- 不新增 schema / mapper / config。
- 不接 MarketQuoteClient / BinanceMarketQuoteClient。
- 不读取 runtime / live / external data。
- 不发送任何测试消息或真实消息。

## 4. Candidate Scope

未来 external push channel 只能消费 Watchlist Pool 边界内的 review-only Opportunity Push candidate。

Display Slots（首页展示位）不能作为 batch universe。

默认六币不能作为 batch universe。

默认六币不能作为默认推送全集。

非 Watchlist Pool 资产不能进入 external push candidate。

## 5. Future Channel Requirements

未来如果另行授权 external push channel，至少必须先定义：

- opt-in / opt-out boundary
- disabled-by-default behavior
- channel allowlist
- recipient allowlist
- throttling requirements
- idempotency key requirements
- audit requirements
- failure handling and retry limits
- message template safety
- no trading instruction wording

P259 只记录这些要求，不实现节流、幂等、审计、重试或模板系统。

## 6. 结论

P259 只打开文档范围门。

任何 Telegram / email / webhook / app notification / local notification implementation 必须后续另开授权。
