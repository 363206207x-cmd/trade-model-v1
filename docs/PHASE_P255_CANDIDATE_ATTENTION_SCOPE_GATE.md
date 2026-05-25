# P255 Candidate Attention Scope Gate

## 1. 阶段定位

P255 定义 Candidate Attention scope gate。P255 不实现 Candidate Attention，不写 Java，不创建 workflow，不接 Push / Readiness / point generation。

## 2. Candidate Attention 前置条件

Candidate Attention 后续如果进入实现，必须满足：

- 必须有 safe review-only ScanScore。
- 必须来自 Watchlist Pool。
- 必须通过 guard / fail-closed 检查。
- 必须保持 `manualReviewRequired=true`。
- 必须保持 `notTradeInstruction=true`。
- 不得触发 Push。
- 不得触发 Readiness。
- 不得生成 entry / stop / TP / RR。

## 3. Candidate Attention 输出语义

Candidate Attention 只能表达“值得人工关注”。

Candidate Attention 不能表达：

- “可执行交易”
- “立即开仓”
- “自动推送”
- “提升首页”
- “已就绪”

## 4. Candidate Attention 仍禁止

- no push
- no readiness
- no point generation
- no order / execution
- no auto-trading
- no reversal instruction
- no risk action execution

## 5. 结论

P256 可考虑 Candidate Attention review-only Java skeleton。P255 不授权实现。
