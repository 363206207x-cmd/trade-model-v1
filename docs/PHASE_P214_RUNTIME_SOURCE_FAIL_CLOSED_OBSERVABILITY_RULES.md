# P214 Runtime Source Fail-Closed Observability Rules

## 1. 阶段定位

P214 只定义 fail-closed（失败关闭）与 observability（可观测性）规则。

P214 不实现日志。

P214 不实现 DB（数据库）。

P214 不实现 API（接口）。

P214 不实现 metric（指标）。

P214 不实现 runtime service（运行时服务）。

## 2. Fail-Closed 规则

未来 runtime source（运行时数据源）必须遵守以下 fail-closed（失败关闭）规则：

- non-watchlist（非观察库资产）=> `BLOCKED_NOT_WATCHLIST`
- unknown membership（成员关系未知）=> `INCOMPLETE`
- missing source（来源缺失）=> `INCOMPLETE`
- stale source（来源过期）=> `REVIEW_ONLY` / `INCOMPLETE`
- partial data（部分数据）=> no-score（无分数）
- data quality unknown（数据质量未知）=> no-score（无分数）
- source conflict（来源冲突）=> `REVIEW_ONLY`
- runtime source unavailable（运行时数据源不可用）=> `INCOMPLETE`

任何 fail-closed 输出都必须保持 `manualReviewRequired=true` 和 `notTradeInstruction=true`。

## 3. Observability 字段候选

以下字段只定义，不实现：

- `sourceStatus`
- `blockingReasons`
- `missingFields`
- `staleFields`
- `sourceRef`
- `ruleVersion`
- `evaluatedAt`
- `notTradeInstruction`
- `manualReviewRequired`
- `noScoreReason`
- `noPushReason`
- `noReadinessReason`

这些字段只用于未来解释为什么不能自动推进，不授权写日志、写指标、写 DB、接 API 或展示到 dashboard（首页）。

## 4. 不允许

P214 不允许：

- 不写 log implementation（日志实现）。
- 不写 metric implementation（指标实现）。
- 不写 DB table（数据库表）。
- 不写 API response（接口响应）。
- 不写 dashboard display（首页展示）。
- 不写 runtime service（运行时服务）。

## 5. 结论

P214 只定义 observability（可观测性）字段候选。

后续实现 observability（可观测性）必须另开授权门。
