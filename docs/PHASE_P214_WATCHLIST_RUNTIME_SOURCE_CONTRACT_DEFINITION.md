# P214 Watchlist Runtime Source Contract Definition

## 1. 阶段定位

P214 只定义未来 Watchlist runtime source contract（观察库运行时数据源契约）。

P214 不实现 runtime source（运行时数据源）。

P214 不接行情。

P214 不读 DB / API / external data（数据库 / 接口 / 外部数据）。

P214 不启用 scheduler（定时器）。

P214 不创建 scan loop（扫描循环）。

## 2. Runtime Source 输入字段候选

以下字段只作为未来概念定义，不在 P214 实现：

- `symbol`
- `watchlistMember`
- `watchlistSource`
- `sourceType`
- `sourceRef`
- `sourceUpdatedAt`
- `receivedAt`
- `freshnessStatus`
- `staleStatus`
- `dataQualityStatus`
- `missingFields`
- `staleFields`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`

这些字段只能用于表达数据来源、安全状态和人工复核边界，不能被解释为真实扫描、推送执行或交易信号。

## 3. SourceType 文档候选

SourceType（来源类型）候选仅在文档层定义：

- `WATCHLIST_CONFIG`
- `DB_WATCHLIST_READ`
- `CACHE_SNAPSHOT`
- `MARKET_QUOTE_CLIENT`
- `SCHEDULER_TRIGGER`
- `MANUAL_REVIEW_INPUT`
- `UNKNOWN`

`MARKET_QUOTE_CLIENT` 和 `SCHEDULER_TRIGGER` 只是未来来源类型候选，不代表 P214 授权接入 MarketQuoteClient（行情客户端）或 scheduler（定时器）。

## 4. 强制安全默认

未来任何 runtime source contract（运行时数据源契约）都必须保持：

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`，如出现。
- `readinessUpgraded=false`，如出现。
- `tradingActionCreated=false`，如出现。
- `entryStopTpRrGenerated=false`，如出现。

这些默认值只能向人工复核开放，不允许自动推送、自动升级 readiness（可执行就绪）或创建交易动作。

## 5. 必须阻断

以下情况必须 fail-closed（失败关闭）或 review-only（只允许复核）：

- non-watchlist asset（非观察库资产）。
- unknown watchlist membership（观察库成员关系未知）。
- missing `sourceRef`（来源引用缺失）。
- stale `sourceUpdatedAt`（来源更新时间过期）。
- `dataQualityStatus` unknown（数据质量状态未知）。
- `sourceType UNKNOWN`（来源类型未知）。
- runtime source missing（运行时数据源缺失）。
- MarketQuoteClient unavailable（行情客户端不可用）。
- scheduler trigger unknown（定时器触发来源未知）。

阻断状态不得生成 ScanScore（扫描分数）、Candidate Attention workflow（候选关注流程）、Promote To Home workflow（提升到首页观察流程）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）或 readiness（可执行就绪）。

## 6. 结论

P214 只是 contract definition（契约定义）。

P214 不授权 P215 直接读 DB / 行情 / scheduler（数据库 / 行情 / 定时器）。

P215 若继续推进，应先做 runtime source authorization gate / DTO skeleton（运行时数据源授权门 / 数据对象骨架），而不是直接实现。
