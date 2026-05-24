# P209 No-Score / No-Push Safety Gate

## 1. 阶段定位

No-score / no-push gate（无分数 / 无推送安全门）是未来扫描链路最重要安全边界。

它的目标是阻断自动化，而不是阻断人工复核。

P209 只写文档，不实现。

## 2. 必须阻断自动化的情况

未来任何实现遇到以下情况，都必须阻断自动化：

- 非观察库资产 => `BLOCKED_NOT_WATCHLIST`。
- unknown watchlist membership（未知观察库成员关系）=> `INCOMPLETE`。
- missing data（缺失数据）=> `INCOMPLETE` / `REVIEW_ONLY`。
- stale data（过期数据）=> `REVIEW_ONLY` / `INCOMPLETE`。
- partial evidence（部分证据）=> no score（无分数）。
- source trace incomplete（证据来源追踪不完整）=> no score（无分数）。
- data quality below threshold（数据质量低于阈值）=> no score（无分数）。
- stampede state（踩踏状态）=> no opportunity push（无机会推送）。
- wick-only short-term risk without confirmation（仅插针短期风险且未确认）=> no promote execution（无提升执行）。
- unresolved multi-timeframe conflict（多周期冲突未解决）=> `REVIEW_ONLY`。

## 3. 必须保持关闭的字段

未来 no-score / no-push gate（无分数 / 无推送安全门）必须保持：

- `opportunityPushAllowed=false`。
- `entryStopTpRrGenerated=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。
- `notTradeInstruction=true`。
- `manualReviewRequired=true`。

## 4. 关键语义

- Score unavailable（分数不可用）不应阻断人工查看。
- Score unavailable（分数不可用）必须阻断自动化、推送执行、readiness（可执行就绪）、交易动作。
- Candidate Attention（候选关注）不等于交易信号。
- Promote To Home（提升到首页观察）不等于 Opportunity Push execution（机会推送执行）。
- Review-only（只允许复核）不等于 ExecutionPlan readiness（执行计划可执行就绪）。
- No-score（无分数）不等于系统不可用，只是不能自动推进。

## 5. 结论

后续任何实现都必须先通过 no-score / no-push gate（无分数 / 无推送安全门）。

不允许为了提高机会数量而绕过 fail-closed（失败关闭）。
