# P257 Opportunity Push Scope Gate

## 1. 阶段定位

P257 定义 Opportunity Push scope gate。

P257 不实现 Opportunity Push。

## 2. Opportunity Push 前置条件

未来任何 Opportunity Push 都必须满足：

- 必须来自 Watchlist Pool。
- 必须经过 Candidate Attention review-only。
- 必须通过 Risk Action Guard。
- 必须通过 stampede / liquidity gate。
- 必须保留 `manualReviewRequired=true`。
- 必须保留 `notTradeInstruction=true`。
- 不能生成 order / execution。
- 不能生成 entry / stop / TP / RR。
- 不能升级 readiness。

## 3. Opportunity Push 输出语义

Opportunity Push 只能表达“值得人工关注的提醒”。

Opportunity Push 不能表达“交易指令”。

Opportunity Push 不能表达“立即开仓”。

Opportunity Push 不能表达“自动下单”。

Opportunity Push 不能表达“已就绪”。

Opportunity Push 不能表达“反手”。

## 4. Push 仍禁止

- no order / execution
- no auto-trading
- no readiness
- no point generation
- no reverse instruction
- no forced close instruction
- no direct leverage advice execution

## 5. 结论

P258 可考虑 Opportunity Push review-only Java skeleton。

P257 不授权实现。
