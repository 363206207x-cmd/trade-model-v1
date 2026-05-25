# P257 Readiness Point Generation Still Blocked

## 1. 阶段定位

本文明确 Readiness / point generation 仍阻断。

P257 不解除任何实现禁令。

## 2. 当前已有但仍不是执行链路

- `CandidateAttentionDTO`
- `CandidateAttentionRule`
- `WatchlistScanScoreCalculator`
- `BatchWatchlistScanOrchestrator`

以上对象只能作为 review-only / fail-closed / skeleton 基础，不构成 Push、Readiness、point generation 或交易执行链路。

## 3. 仍未实现

- Opportunity Push Java
- external push channel
- Readiness
- point generation
- entry / stop / TP / RR
- execution plan readiness upgrade
- real market read
- real ScanScore
- scheduler-triggered batch
- real scan loop

## 4. 结论

后续 Push / Readiness / point generation 必须另开授权门。

auto-trading 不在 V1 范围内。
