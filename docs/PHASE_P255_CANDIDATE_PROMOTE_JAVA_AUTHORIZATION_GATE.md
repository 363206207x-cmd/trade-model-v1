# P255 Candidate / Promote Java Authorization Gate

## 1. 阶段定位

P255 是 Candidate / Promote Java authorization gate。P255 不写 Java，不新增测试，不实现 Candidate Attention，也不实现 Promote To Home。

## 2. P256 可考虑内容

P256 可考虑最小 Candidate Attention review-only Java skeleton：

- `CandidateAttentionDTO`
- `CandidateAttentionStatusEnum`
- `CandidateAttentionRule`
- `DefaultCandidateAttentionRule`
- targeted test

P256 必须保持：

- 只能 review-only。
- 不能 push。
- 不能 readiness。
- 不能 promote。
- 不能 point generation。
- 不能 trading action。

## 3. 后续 Promote Java 必须另开

- `PromoteToHomeDTO` / rule / service 必须另开授权门。
- Promote 不能和 Candidate Attention Java skeleton 混成一个执行动作。
- Dashboard read-only integration 必须另开授权门。

## 4. P256 必须保持

- review-only
- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `opportunityPushAllowed=false`
- `promoteToHomeAllowed=false`
- `readinessUpgraded=false`
- `tradingActionCreated=false`
- `entryStopTpRrGenerated=false`
- no order / execution / auto-trading

## 5. 结论

P256 可以进入 Candidate Attention review-only skeleton。P256 不是 Push，不是 Promote，不是 Readiness。
