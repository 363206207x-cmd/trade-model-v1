# P255 ScanScore Calculation Closure

## 1. 阶段定位

P255 是 P254 ScanScore Calculation Review-Only Skeleton 的 closure。P255 只记录 P254 已完成内容、测试证明和仍然保持的边界，不实现新功能。

## 2. P254 合并基准

- PR：#627
- Issue：#626
- merge commit：3278a2f
- 标题：BACKEND-P254 ScanScore Calculation Review-Only Skeleton

## 3. P254 已完成内容

- 新增 `WatchlistScanScoreCalculator`
- 新增 `DefaultWatchlistScanScoreCalculator`
- 新增 `DefaultWatchlistScanScoreCalculatorTest`
- 新增 P254 verification 文档
- 更新 `docs/V1_CURRENT_STATE.md`
- 更新 `docs/PROJECT_PROGRESS_INDEX.md`

## 4. P254 测试确认

P254 targeted test 已确认：

- missing scoreRule fails closed
- null batchEnvelope fails closed
- blank symbol fails closed
- scoreRule returns null fails closed
- scoreRule throws fails closed
- unsafe score result fails closed
- safe reviewOnly score result is returned
- all outputs preserve no-execution defaults
- calculator only calls rule once
- no forbidden fields / methods for MarketQuoteClient / BinanceMarketQuoteClient / Scheduler / Controller / Push service / DataSource / JdbcTemplate / Scheduled
- no method names containing push / readiness / order / execute / trade

## 5. P254 没有完成

- no production score computation
- no MarketQuoteClient
- no BinanceMarketQuoteClient
- no runtime / live / external data reads
- no scheduler
- no scan loop
- no real scan
- no Candidate Attention
- no Promote To Home
- no Opportunity Push
- no readiness
- no entry / stop / TP / RR
- no order / execution / auto-trading

## 6. 当前结论

P254 是 review-only ScanScore calculation skeleton，不是真实评分。P254 不授权 P256 直接做 Push / Readiness / 点位。P255 先定义 Candidate / Promote scope 和 Java authorization gate。
