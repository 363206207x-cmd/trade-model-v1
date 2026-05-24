# P214 Runtime Source Freshness Staleness Rules

## 1. 阶段定位

P214 只定义 freshness / staleness（新鲜度 / 过期状态）规则。

P214 不实现时间判断。

P214 不读取系统时间。

P214 不读取外部数据。

P214 不接 MarketQuoteClient（行情客户端）、DB（数据库）或 scheduler（定时器）。

## 2. Freshness 状态候选

Freshness 状态候选仅在文档层定义：

- `FRESH`
- `STALE`
- `EXPIRED`
- `UNKNOWN`
- `NOT_AVAILABLE`

这些状态只表达未来数据源是否可用于人工复核，不表示允许推送或交易。

## 3. Staleness 阻断规则

Staleness（过期状态）阻断规则：

- `UNKNOWN` => `INCOMPLETE`
- `NOT_AVAILABLE` => `INCOMPLETE`
- `EXPIRED` => `BLOCKED` / `INCOMPLETE`
- `STALE` => `REVIEW_ONLY` / `INCOMPLETE`
- `FRESH` 也不等于允许推送或交易。

即使数据为 `FRESH`，也必须继续通过 no-score / no-push / no-readiness / no-trading（无分数 / 无推送 / 无可执行就绪 / 无交易动作）边界。

## 4. 时间字段候选

以下字段只定义，不实现：

- `sourceUpdatedAt`
- `receivedAt`
- `evaluatedAt`
- `maxAgeSeconds`
- `ageSeconds`

P214 不计算这些字段，不读取当前系统时间，不比较时间差，不创建时间判断 helper（辅助类）。

## 5. 安全边界

stale data（过期数据）必须保持以下安全边界：

- stale data 不生成 ScanScore（扫描分数）。
- stale data 不触发 Candidate Attention workflow（候选关注流程）。
- stale data 不触发 Promote To Home workflow（提升到首页观察流程）。
- stale data 不允许 Opportunity Push execution（机会推送执行）。
- stale data 不升级 readiness（可执行就绪）。
- stale data 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 6. 结论

P214 不实现 freshness calculation（新鲜度计算）。

后续 freshness calculation（新鲜度计算）必须另开授权门。
