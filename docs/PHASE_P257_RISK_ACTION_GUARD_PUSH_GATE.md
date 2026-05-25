# P257 Risk Action Guard Push Gate

## 1. 阶段定位

P257 定义 Push 前置 Risk Action Guard。

P257 不实现 Risk Action Guard Java。

## 2. Risk Action Guard 规则

Push 前置保护必须写入以下规则：

- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- 风险高但流动性正常：只能考虑减仓、移动止损、降低杠杆等人工审核建议。
- 风险高且流动性恶化：不建议市价一次性砍仓，优先分批降风险、等待流动性恢复、只降杠杆。
- 风险高且存在踩踏：进入极端压力锁定，禁止反手、禁止新开仓、禁止机会推送。
- 风险高但仅短线插针：不直接判定趋势反转，不生成反向开仓计划，只做短线风险提醒和等待确认。
- 强反转不等于直接反手。
- 插针不等于趋势反转。
- 踩踏状态禁止机会推送。

## 3. Push gate 必须保持

- Candidate Attention 之后。
- Risk Action Guard 之前不允许 Push。
- stampede / extreme stress 禁止 Push。
- liquidity deterioration 禁止直接执行类 Push。
- wick-only 不允许趋势反转 Push。
- `manualReviewRequired=true`。
- `notTradeInstruction=true`。

## 4. 结论

P258 如果进入 Java，也只能是 review-only Push skeleton。

Risk Action Guard 生产实现必须另开授权门，或在 Push skeleton 中作为 fail-closed placeholder。
