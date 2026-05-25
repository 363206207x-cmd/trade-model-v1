# P255 Push / Readiness / Point Generation Still Blocked

## 1. 阶段定位

本文明确 Push / Readiness / point generation 仍阻断。P255 不解除任何实现禁令，不创建机会推送执行，不升级 readiness，不生成真实点位。

## 2. 当前已有但仍不是执行链路

当前已有以下骨架或 DTO，但它们仍不是执行链路：

- `BatchWatchlistScanOrchestrator`
- `WatchlistMarketReadAdapter`
- `WatchlistScanScoreDTO`
- `WatchlistScanScoreRule`
- `WatchlistScanScoreCalculator`

## 3. 仍未实现

- Candidate Attention Java
- Promote To Home Java
- Opportunity Push
- Readiness
- entry / stop / TP / RR
- real market read
- real ScanScore
- scheduler-triggered batch
- real scan loop

## 4. Risk Action Guard 提醒

- 风险高不能直接等于立即止损、立即反手或立即开仓。
- 强反转不等于直接反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。
- Risk Action Guard 必须位于 Candidate / Push / Readiness 之前。

## 5. 结论

后续 Push / Readiness / point generation 必须另开授权门。auto-trading 不在 V1 范围内。
