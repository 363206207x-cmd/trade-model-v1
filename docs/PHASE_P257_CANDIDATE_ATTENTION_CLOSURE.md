# P257 Candidate Attention Closure

## 1. 阶段定位

P257 是 P256 Candidate Attention Review-Only Skeleton 的 closure。

P257 只记录 P256 已完成内容、测试证据和边界，不实现新功能。

## 2. P256 合并基准

- PR #631
- Issue #630
- merge commit: `1e52d2f`
- 标题：BACKEND-P256 Candidate Attention Review-Only Skeleton

## 3. P256 已完成内容

- 新增 `CandidateAttentionDTO`
- 新增 `CandidateAttentionStatusEnum`
- 新增 `CandidateAttentionRule`
- 新增 `DefaultCandidateAttentionRule`
- 新增 `DefaultCandidateAttentionRuleTest`
- 新增 P256 verification 文档
- 更新 `V1_CURRENT_STATE.md`
- 更新 `PROJECT_PROGRESS_INDEX.md`

## 4. P256 测试确认

P256 targeted test 已证明：

- null score fails closed
- blank symbol fails closed
- unsafe score fails closed
- non-review-only score fails closed
- safe reviewOnly score returns review-only candidate attention
- score reasons and blocking reasons are preserved
- all outputs preserve no-execution defaults
- DTO defensive copy
- enum has no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / PUSHED / PROMOTED
- no forbidden MarketQuoteClient / BinanceMarketQuoteClient / Scheduler / Controller / Push service / DataSource / JdbcTemplate / Scheduled fields or methods
- method names do not contain push / promote / readiness / order / execute / trade

## 5. P256 没有完成

- no Promote To Home
- no Opportunity Push
- no Readiness
- no point generation
- no entry / stop / TP / RR
- no order / execution / auto-trading
- no API / dashboard
- no MarketQuoteClient
- no scheduler
- no runtime / live / external data reads

## 6. 当前结论

P256 是 review-only Candidate Attention skeleton。

P256 不是 Push。

P256 不是 Promote。

P256 不是 Readiness。

P257 先定义 Opportunity Push authorization + Risk Action Guard gate。
