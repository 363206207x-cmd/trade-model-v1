# AGENTS.md — Trade Model V1 Codex 工作总规则

## 1. 项目定位

本项目是 Trade Model V1：多源证据驱动的交易决策闭环系统。

本项目不是自动交易机器人，不直接下单，不自动平仓，不承诺收益。

核心链路固定为：

原始数据接入 → 原始证据生成 → 证据标准化 → 八大评分 → 综合决策 → 执行计划 → 实时监控 → 结果复盘 → 规则修正与版本迭代。

所有开发必须服务于三件事：

1. 可追踪：输入、证据、评分、决策、执行计划、监控、复盘都可追溯。
2. 可约束：统一规则层、风险边界、数据质量、AI 调用策略可控。
3. 可迭代：复盘结果能反向修正规则和版本。

## 2. 当前最高治理边界

AI conflict / confused state / Push Recheck / Missed Opportunity / Hot Reset 是当前最高治理边界。

任何涉及以下内容的开发，必须先检查这些治理边界：

- 方向输出
- 候选计划
- 执行建议
- 推送
- 持仓监控
- 复盘

治理边界未冻结前，不允许推进：

- 生产 `VALID`
- mapper
- Assembler
- `plan_boundary_json`
- 方向性推送
- 自动执行链路

## 3. AI 与规则层关系

规则层先产出基础方向、证据、评分、风险和状态。

AI 只能用于：

- 增强
- 解释
- 复核
- 反证
- 摘要
- 降级建议
- 风险提示

AI 不得替代规则层，不得直接触发下单、平仓、`triggered`、`VALID` 或可执行计划。

AI 不是三票投票制。不允许任一 AI 单独反对就无限观望。不允许 Gemini 或 Grok 直接覆盖规则状态机。

AI 冲突四级处理固定为：

1. Level 1：一致。保留方向，确认型计划可用。
2. Level 2：轻微分歧。保留方向，置信度降一级，风险或计划模式降级。
3. Level 3：显著分歧。不输出确认型执行建议，只允许 candidate / waiting_trigger / 预备型计划，并给恢复条件。
4. Level 4：极端分歧。进入 confused，禁止方向性执行建议。

confused state 固定规则：

- `confused_score >= 70` 进入 confused。
- `confused_score >= 85` 禁止方向性推送。
- confused 状态下禁止标准看多 / 看空执行计划。
- confused 状态下 `triggered` 必须降级为 `observing` / `candidate` / `waiting_trigger` / `warning_only`。
- 退出 confused 后只能进入 `observing` 或 `candidate`，不能直接回 `triggered`。
- Loader context 足量不等于 candidate 可 `VALID`。
- 高 confused 下不能让生产 candidate 进入 `VALID`。

Push Recheck 固定规则：

- 推送不是静态机会，点击时必须重新核验。
- Push Recheck 必须检查价格漂移、滑点、数据质量、账户风险、状态、原因层、是否过期。
- confused / high_risk / invalidated / cooling 应阻断或等待。
- 必须支持 `CONFUSED_BLOCKED`。
- 推送失败、漂移、失效、二次确认结果必须可追踪、可审计、可复盘。

Missed Opportunity 固定规则：

- 未执行但正确的机会必须记录，避免幸存者偏差。
- 推送未成交但正确、风险阻断但正确、已执行正确 / 错误都要纳入复盘视角。
- Missed Opportunity 不得绕过 confused / high_risk / invalidated / existing position 边界。

Hot Reset 固定规则：

- 极端环境突变时允许 Hot Reset 高于常规慢状态机。
- Hot Reset 只能触发重新计算、状态重建、风险提示和审计记录。
- Hot Reset 不得直接触发自动下单或自动平仓。
- Hot Reset 之后不允许直接回到 `triggered`。

## 4. 持仓监控与 UI 边界

系统建议不等于用户真实开仓。达到入场条件不等于已经开仓。只有用户手动录入真实持仓后，才进入持仓监控。

AI 三方重大冲突是持仓监控重新分析和风险升级触发源。计划失效只能提示人工处理，不能自动假设用户已平仓。

平仓后首页监控清空，记录进入复盘。系统建议与用户实际执行必须分开。

UI 是决策工作台，不是交易所下单终端。所有视觉和文案必须服务于：

- 证据可追踪
- 结论可解释
- 计划可执行
- 结果可复盘

UI 不允许制造自动交易、自动执行、自动平仓的暗示。不允许只展示结论不展示证据。风险提示必须有文字原因。

## 5. Codex 每轮固定流程

每次任务必须按以下顺序执行：

1. 先读取 AGENTS.md。
2. 再读取 docs/V1_FRAMEWORK_LOCK.md。
3. 再读取 docs/CODEX_PHASE_EXECUTION_RULE.md。
4. 再读取 docs/PHASE_POSITION_MONITOR_FREEZE_INDEX.md，如果该文件存在。
5. 先做只读审计，不允许直接改代码。
6. 输出当前阶段、已有实现、部分实现、后端-only、前端-only、冲突、未实现。
7. 输出下一步最小交付物。
8. 等用户明确确认后，才允许进入实施。
9. 实施时每轮只做一个最小闭环。
10. 实施后必须运行或说明验收命令。
11. 最后必须更新或新增收口文档。
12. 必须说明本轮没有做什么。

涉及方向、候选、推送、持仓监控、复盘前，必须先审计 AI conflict / confused / Push Recheck / Missed Opportunity / Hot Reset 是否受影响。

如果护栏未覆盖完整方案，必须先更新护栏再实施 Java。

## 6. 严禁事项

严禁：

- 自动下单。
- 自动平仓。
- 自动执行。
- 把“达到开仓条件”当成“用户已经开仓”。
- 没有用户手动录入方向和开仓价时展示真实持仓监控。
- 伪造 Binance、AI、持仓、复盘、风控结果。
- 把模拟数据包装成真实数据。
- 用单一 AI 意见替代证据链。
- 用单一综合分直接输出多空。
- 绕过证据、评分、决策、执行计划、监控、复盘链路。
- 在 AI conflict / confused / Push Recheck / Missed Opportunity / Hot Reset 治理边界未冻结前推进生产 VALID、mapper、Assembler、plan_boundary_json、方向性推送或自动执行链路。
- 让 AI 直接触发下单、平仓、triggered、VALID 或可执行计划。
- 让 Loader context 足量直接推出 candidate VALID。
- 为了通过编译删除核心逻辑。
- 大规模重构。
- 修改无关模块。
- 未经要求修改首页 UI。
- 未经要求修改数据库结构。
- 未经要求修改接口 JSON 契约。
- 未经要求新增依赖。
- 未经要求引入平台化能力。
- 写入项目目录外文件。

## 7. 当前项目运行命令

项目路径：

/Users/xuchao/Documents/trade-model-v1

编译：

./mvnw clean compile

运行：

./mvnw spring-boot:run

访问：

http://localhost:8081/dashboard

端口检查：

lsof -i :8081

## 8. 每轮完成后必须输出

每轮完成后必须输出：

1. 修改文件清单。
2. 每个文件为什么改。
3. 是否符合 V1_FRAMEWORK_LOCK。
4. 是否触碰严禁事项。
5. 编译是否通过。
6. 测试是否通过，如未运行必须说明原因。
7. 页面或接口如何验收。
8. 本轮没有做什么。
9. 下一步最小动作。
10. 是否影响 AI conflict。
11. 是否影响 confused state。
12. 是否影响 Push Recheck。
13. 是否影响 Missed Opportunity。
14. 是否影响 Hot Reset。

## 9. 失败处理规则

如果编译失败：

1. 只允许修复本轮引入的问题。
2. 不允许扩大修改范围。
3. 不允许重构无关模块。
4. 不允许为了编译通过删除核心逻辑。
5. 修复后必须重新说明修改文件和原因。
