# CURSOR_RISK_HANDOFF_AND_UNFINISHED_SCOPE

## 1. 文档目的

本文件用于把 Cursor 过程中已讨论、已冻结或半完成的关键边界迁移给 Codex，避免后续自动实施时被覆盖、反向重构或忽略。

本文件不是新需求扩张，不要求本轮实现 Java。

本文件只做风险迁移、边界冻结、未完成项索引。

## 2. 当前最高优先级

当前最高治理边界仍是：

1. AI conflict。
2. confused state。
3. Push Recheck。
4. Missed Opportunity。
5. Hot Reset。

任何 Loader / candidate / VALID / mapper / Assembler / `plan_boundary_json` 后续实施，都必须先检查这些治理边界。

Loader context 足量不等于 candidate 可 VALID。

AI 冲突不能覆盖规则层。

confused 状态下禁止方向性执行建议和方向性推送。

## 3. Cursor 中已确定但容易被覆盖的核心边界

### 3.1 首页方向已冻结

首页不是行情堆叠页，也不是交易所下单终端。

首页方向已冻结为：

1. 重点资产监控。
2. 首页工作台 `homeWorkbench`。
3. 已开仓监控。
4. 执行建议。
5. AI 三方裁决。
6. 实时告警。
7. 关键事件。
8. 系统状态。

后续禁止：

1. 大改首页 UI。
2. 把首页改回传统行情面板。
3. 把核心信息挤进表格。
4. 删除 `homeWorkbench` 方向。
5. 只展示结论不展示证据。
6. 制造自动交易、自动执行、自动平仓暗示。

### 3.2 重点资产推送边界

V1 的可执行机会推送只允许基于“重点观察 / 观察列表中的资产”。

要求：

1. 不按默认六个币固定推送。
2. 只要资产处于观察列表，就可纳入推送候选。
3. 不在观察列表中的资产不允许推送。
4. 推送不是静态机会，点击必须 Push Recheck。
5. confused / high_risk / invalidated / cooling 必须阻断或等待。
6. `confusedScore >= 85` 禁止方向性推送。

未完成项：

1. 观察列表资产与 Push Snapshot 生成候选之间的正式过滤链路需要后续审计。
2. 不允许先按默认六币恢复推送逻辑。

### 3.3 持仓监控边界

系统建议不等于用户真实开仓。

达到入场条件不等于已经开仓。

只有用户手动录入真实持仓后，才进入持仓监控。

持仓监控只做：

1. 入场逻辑是否仍成立。
2. 反转风险是否升高。
3. AI 三方是否冲突。
4. 风险等级是否升高。
5. 是否建议人工处理。
6. 是否进入复盘。

持仓监控不做：

1. 自动平仓。
2. 自动反手。
3. 自动加仓。
4. 自动把计划失效等同为用户已平仓。
5. 自动把候选机会变成真实持仓。

平仓后：

1. 首页持仓监控清空。
2. 记录进入复盘。
3. 系统建议与用户实际执行必须分开。

### 3.4 强反转与反手开仓边界

强反转不是简单价格反向，而是原方向被新的证据链推翻。

强反转至少涉及：

1. 价格结构破坏。
2. 多周期收敛转向或严重冲突。
3. 八大评分明显恶化。
4. 资金 / 杠杆 / 流动性 / 事件冲击反向。
5. AI 三方裁决与原持仓方向冲突扩大。
6. confused 或 high_risk 状态出现。

当前阶段：

1. 可以提示“原计划失效 / 强反转风险升高 / 等待人工确认”。
2. 可以建议“收紧止损 / 减仓观察 / 记录平仓进入复盘”。
3. 不建议自动反手开仓。
4. 不允许自动反向下单。
5. 不允许把强反转直接等同为新方向可执行。

未完成项：

1. 强反转的正式 scoring / rule config / 状态机边界需要后续单独设计。
2. 移动止损位置的正式算法尚未完成。
3. 反手开仓不属于当前 V1 自动逻辑。

### 3.5 移动止损边界

移动止损不能凭主观价格随意给。

应基于：

1. 原执行计划止损区。
2. 用户实际止损价。
3. 结构位。
4. 波动率。
5. 多周期支撑 / 压力。
6. 当前风险等级。
7. 持仓方向。
8. 资金和流动性变化。

当前阶段可输出：

1. 建议收紧止损。
2. 建议移动止损到结构参考区。
3. 建议人工确认新的止损价。
4. 不自动修改用户止损。
5. 不自动平仓。

未完成项：

1. 移动止损价位算法未完成。
2. 用户实际止损与系统建议止损的偏离度计算未完整落地。
3. 需要后续设计：结构止损 / 保本止损 / 分批止盈后移动止损。

### 3.6 Entry / Stop / TP 数值边界

entry / stop / TP 数值不能为了完整而硬填。

必须仍允许 INCOMPLETE。

仍必须 INCOMPLETE 的情况包括：

1. 数据质量不足。
2. 多周期不收敛。
3. AI 冲突显著或极端。
4. confused / high_risk / invalidated / cooling。
5. 缺少结构位。
6. 缺少有效入场区。
7. 只有文字执行建议但没有结构化边界。
8. Loader context 足量但规则层未确认执行可行性。

当前已完成或已知状态：

1. `invalidPrice` 可以作为“结构失效参考价”。
2. `invalidPrice` 不等于 `stopLoss`。
3. entry / stopLoss / takeProfit PARTIAL 仍未完整落地。
4. STRUCTURED `plan_boundary_json` 未全面启用。
5. candidate 当前不得生产 VALID。

后续风险：

1. Codex 不得因为 Loader / Context 足量，就直接生成 entry / stop / TP。
2. 不得为了让 JSON 完整而伪造价格。
3. 不得把 `invalidPrice` 包装成 `stopLoss`。

### 3.7 AI 三方裁决边界

GPT / Gemini / Grok 不是三票投票制。

职责：

1. GPT_FINAL：最终裁决官。
2. GEMINI_REVIEW：冲突复核官。
3. GROK_CHALLENGE：快讯与反方挑战官。

AI 只能：

1. 增强。
2. 解释。
3. 复核。
4. 反证。
5. 摘要。
6. 降级建议。
7. 风险提示。

AI 不得：

1. 替代规则层。
2. 直接触发下单。
3. 直接触发平仓。
4. 直接把 candidate 变成 VALID。
5. 直接覆盖状态机。
6. 单独反对就让系统无限观望。

AI 冲突优先影响：

1. 置信度。
2. 风险等级。
3. 计划模式。
4. 是否进入 confused。

### 3.8 Push Recheck 与推送边界

推送发出后不是永久有效。

用户点击推送时必须重新确认。

Push Recheck 至少检查：

1. 当前价格是否仍在可接受入场区。
2. 价格漂移。
3. 滑点。
4. 数据质量。
5. 账户风险。
6. 当前状态是否 invalidated / cooling / high_risk / confused。
7. 原因层是否仍成立。
8. 推送是否过期。

当前已完成：

1. Push Snapshot confused / assetState 闸门已完成。
2. `confusedScore >= 85` 不生成方向性 push snapshot。
3. CONFUSED / HIGH_RISK / INVALIDATED / COOLING 不生成方向性 push snapshot。

未完成：

1. Push Recheck 当前状态重算是否完整，需要后续审计。
2. 观察列表过滤与推送候选关系需要后续审计。
3. 推送通道选择、延迟、可靠性未最终冻结。

### 3.9 Missed Opportunity 与 Opportunity Log 边界

普通 missed 和非普通机会事件必须分开。

当前已完成：

1. Missed Opportunity confused / assetState 闸门已完成。
2. `confusedScore >= 70` 不写普通 missed。
3. CONFUSED / HIGH_RISK / INVALIDATED / COOLING 不写普通 missed。

当前设计冻结：

1. `docs/PHASE_OPPORTUNITY_LOG_SERVICE_SCHEMA_DESIGN_V1.md`

后续不允许：

1. 把 BLOCKED_BY_CONFUSED 写成普通 missed。
2. 把 BLOCKED_BY_RISK_VALID 写成普通 missed。
3. 把 pushed-not-filled 当成 executed。
4. 把系统建议当成真实执行。

未完成：

1. OpportunityLogService 未实现。
2. `tm_opportunity_log` 未建表。
3. pushed-not-filled / blocked / executed 类型未接入复盘。
4. ReviewAggregate 未接 opportunity log。

### 3.10 Cursor / Codex 协作边界

Cursor 已参与过 UI、首页、持仓监控、entry / stop / TP、推送、AI 冲突等讨论。

Codex 后续自动实施时，不得覆盖这些已冻结边界。

Cursor 主要用于：

1. 人工查看 diff。
2. 搜索文件。
3. 必要时确认 UI 或前端细节。

Codex 主要用于：

1. 只读审计。
2. 最小实施。
3. 编译。
4. 指定测试。
5. 收口文档。

不允许 Codex：

1. 自行大重构。
2. 自行“完成全部 V1”。
3. 自行清理 git status。
4. 自行删除历史 docs。
5. 自行重写首页方向。
6. 自行把旧 Cursor 产物判定为无效并覆盖。

## 4. 当前已经完成的治理收口

1. `docs/PHASE_AI_CONFLICT_CONFUSED_GOVERNANCE_ALIGNMENT_CLOSURE.md`
2. `docs/PHASE_PUSH_SNAPSHOT_CONFUSED_GOVERNANCE_GATE_CLOSURE.md`
3. `docs/PHASE_MISSED_OPPORTUNITY_CONFUSED_GOVERNANCE_GATE_CLOSURE.md`
4. `docs/PHASE_OPPORTUNITY_LOG_SERVICE_SCHEMA_DESIGN_V1.md`
5. `docs/PHASE_RULE_ENGINE_RUNTIME_MULTI_TIMEFRAME_KLINE_LOADER_CLOSURE.md`

## 5. 当前未完成但不能丢的内容

1. 观察列表资产推送过滤。
2. Push Recheck 当前状态重算。
3. OpportunityLogService / `tm_opportunity_log`。
4. ReviewAggregate 接 opportunity log。
5. candidate service 是否继续接 Loader。
6. candidate 继续保持 INCOMPLETE 的门禁。
7. entry / stop / TP PARTIAL 规则。
8. STRUCTURED `plan_boundary_json` 门禁。
9. 强反转评分与状态机。
10. 移动止损价位算法。
11. 持仓监控中 AI conflict Level 3/4 的统一表达。
12. UI `homeWorkbench` 与首页冻结方向的持续保护。
13. 重点资产 / 观察列表与首页、推送、监控联动。
14. 推送通道选择与延迟策略。

## 6. 后续 Codex 每轮必须先检查

每轮如果涉及以下内容，必须先检查本文件：

1. 首页。
2. 推送。
3. 持仓监控。
4. AI 冲突。
5. confused。
6. candidate。
7. Loader。
8. VALID。
9. mapper。
10. Assembler。
11. `plan_boundary_json`。
12. entry / stop / TP。
13. 复盘。
14. Opportunity Log。

## 7. 本轮不做事项

1. 不改 Java。
2. 不改 HTML。
3. 不改 Mapper。
4. 不改 schema。
5. 不改配置。
6. 不运行编译。
7. 不运行测试。
8. 不接 Loader。
9. 不接 production VALID。
10. 不生成 `plan_boundary_json`。
11. 不清理 git status。
12. 不删除或覆盖旧 docs。
13. 不自动下单或平仓。
