# BACKEND-P154 SourceTrace Runtime Population Authorization Gate

## 1. 这一步是干嘛的

P154 是 SourceTrace Runtime Population Authorization Gate（运行时证据来源填充授权门）。

它只定义未来第一根最小代码实现允许改哪些文件、允许验证什么、仍然禁止什么。

P154 本轮不做实现：

- 不写 Java。
- 不新增测试。
- 不接 Production Wiring（真正接入系统运行链路）。
- 不读取 Runtime（系统运行时）数据。
- 不读取 live market data（实时行情）。
- 不接 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 VALID（有效候选状态）。
- 不升级 Readiness（是否允许进入下一步）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P154 只是 Authorization Gate（授权门），不是代码实现。

## 2. P153 的审计结论

P153 已经确认：现有 DTO（数据传输对象）可以表达 SourceTrace（证据来源追踪）相关的基础字段。

已经能表达的内容包括：

- source owner（证据来源所有者）
- source ref（证据来源引用）
- timeframe（周期）
- source window（证据窗口）
- freshness（新鲜度）
- missing fields（缺失字段）
- blocking reasons（禁止推进原因）
- review-only（只允许复核）
- `manualReviewRequired=true`（必须人工复核）
- `notTradeInstruction=true`（不是交易指令）

P153 也确认：

- P146 的 `SourceOwnedCandidateIncompleteGuardTest` 已经证明 INCOMPLETE（证据不完整状态）不会被误推进。
- P149 的 `SourceOwnedCandidateBlockedGuardTest` 已经证明 BLOCKED（禁止推进状态）不会被误推进。
- 现有测试已经覆盖 review-only（只允许复核）、manual review（人工复核）、not trade instruction（不是交易指令）的安全姿态。

但是 P153 同时确认：

- 还没有真实 Runtime SourceTrace population（系统运行时证据来源填充）逻辑。
- 还没有定义哪个生产 service（服务）负责填 SourceTrace（证据来源追踪）。
- 还没有定义 source owner / source ref / timeframe / source window 从哪个真实对象来。
- 还没有定义 Runtime（系统运行时）SourceTrace（证据来源追踪）和 Candidate（候选交易计划）之间怎么连接。

所以 P154 只能授权未来最小代码实现的范围，不能直接写代码。

## 3. 是否允许未来最小代码实现

P154 的保守结论是：可以允许未来单独开 P155 做一个最小代码实现授权后的实现。

但是 P155 必须非常小。

P155 只能围绕：

```text
SourceTrace runtime population
```

中文解释：SourceTrace runtime population（运行时证据来源填充）。

未来 P155 必须继续遵守：

- 不能生成 VALID（有效候选状态）。
- 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不能升级 Readiness（是否允许进入下一步）。
- 不能改 `dashboard.html`。
- 不能接 controller / endpoint / API（控制器 / 接口 / API）。
- 不能接 service（服务）生产链路。
- 不能接 mapper（映射）。
- 不能接 schema / config（数据库结构 / 配置）。
- 不能接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P155 只能证明现有 DTO（数据传输对象）字段可以承载 Runtime（系统运行时）证据来源信息，并且继续保持 REVIEW_ONLY（只允许复核）。

## 4. 未来 P155 允许改哪些文件

P154 授权未来 P155 最多允许改 1-3 个文件。

保守授权范围如下：

1. `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureTest.java`
2. `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureHelper.java`
3. 如确实必须，最多允许修改：
   `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P155 的默认策略必须是：

- 优先只做 test fixture（测试夹具）层。
- 如果不需要改 `SourceTraceDTO.java`，就不改。
- 不允许改 service（服务）。
- 不允许改 mapper（映射）。
- 不允许改 controller（控制器）。
- 不允许改 endpoint / API（接口）。
- 不允许改 schema（数据库结构）。
- 不允许改 config（配置）。
- 不允许改 `dashboard.html`。

如果 P155 发现必须修改上述授权范围之外的文件，P155 必须停止，并另开新的授权门。

## 5. 未来 P155 允许做什么

未来 P155 只能做这些事：

- 用已有 DTO（数据传输对象）字段组装 SourceTrace（证据来源追踪）。
- 验证 source owner（证据来源所有者）能进入 SourceTrace（证据来源追踪）。
- 验证 source ref（证据来源引用）能进入 SourceTrace（证据来源追踪）。
- 验证 timeframe（周期）能进入 SourceTrace（证据来源追踪）。
- 验证 source window（证据窗口）能进入 SourceTrace（证据来源追踪）。
- 验证 freshness（新鲜度）能进入 SourceTrace（证据来源追踪）。
- 验证缺证据时仍保持 INCOMPLETE（证据不完整状态）。
- 验证冲突或不安全时仍保持 BLOCKED（禁止推进状态）。
- 验证 `manualReviewRequired=true`（必须人工复核）。
- 验证 `notTradeInstruction=true`（不是交易指令）。
- 验证 review-only（只允许复核）。
- 验证不会出现 order / execution / automation（下单 / 执行 / 自动化）字段。

P155 的输出只能证明字段承载能力和 fail-closed（失败时保持关闭）安全姿态，不能证明生产候选交易计划已经可用。

## 6. 未来 P155 禁止做什么

未来 P155 仍然禁止：

- 不允许新增 service（服务）。
- 不允许新增 mapper（映射）。
- 不允许新增 controller（控制器）。
- 不允许新增 endpoint / API（接口）。
- 不允许改 schema（数据库结构）。
- 不允许改 config（配置）。
- 不允许改 `dashboard.html`。
- 不允许读取 runtime data（运行时数据）。
- 不允许读取 live market data（实时行情）。
- 不允许读取 external data（外部数据）。
- 不允许生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不允许调用 `BoundaryCandidateDTO.valid(...)`。
- 不允许生成 VALID（有效候选状态）。
- 不允许升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不允许接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P155 不能把 test fixture（测试夹具）能力伪装成 Production Wiring（真正接入系统运行链路）。

## 7. 仍然禁止的路径

以下路径在 P154 之后仍然禁止：

- production candidate generation（生产候选交易计划生成）
- source-owned runtime candidate generation（运行时证据来源候选生成）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- production VALID mapping（生产环境映射为有效候选）
- BoundaryCandidateService VALID production path（边界候选服务生产有效路径）
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard readiness mutation（页面显示可执行状态）
- `dashboard.html` changes（页面改动）
- controller / endpoint / API wiring（接口接线）
- schema / config / service / mapper changes（数据库 / 配置 / 服务 / 映射改动）
- runtime data reads（读取运行时数据）
- live market data reads（读取实时行情）
- external data integration（接外部数据）
- exchange clients（交易所客户端）
- `WebClient` / `RestTemplate`（网络请求工具）
- order API（下单接口）
- execution API（执行接口）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

这些路径不能因为 P154 授权未来 P155 测试夹具层验证而被间接授权。

## 8. 推荐下一步

推荐下一步是：

```text
P155: SourceTrace Runtime Population Fixture Test
```

中文解释：P155 是 SourceTrace Runtime Population Fixture Test（运行时证据来源填充测试夹具）。

P155 才可以开始最小代码实现。

但是 P155 也只能做 test fixture（测试夹具）层，不接真实系统运行链路。

P155 的目标是证明现有 DTO（数据传输对象）字段可以承载 Runtime（系统运行时）证据来源信息。

P155 仍然不能生成真实交易点位。

P155 仍然不能生成 VALID（有效候选状态）。

P155 仍然不能升级 Readiness（是否允许进入下一步）。

P155 仍然不能接 auto-trading（自动交易）。

## 9. P154 结论

P154 只完成授权门文档。

P154 不写代码。

P154 不新增测试。

P154 不授权 production candidate generation（生产候选交易计划生成）。

P154 不授权 VALID（有效候选状态）。

P154 不授权 Readiness（是否允许进入下一步）。

P154 不授权 dashboard readiness（页面可执行状态）。

P154 不授权 auto-trading（自动交易）。

P154 只允许后续 P155 做最小 test fixture（测试夹具）层验证。

P154 不允许 P155 默认进入 service / mapper / controller / API / schema / config / dashboard / runtime data / live data / external data / order / execution / automation 路径。

## 10. P154 边界确认

P154 本轮只完成一个授权门文档：

- 新增 `docs/PHASE_BACKEND_P154_SOURCETRACE_RUNTIME_POPULATION_AUTHORIZATION_GATE.md`。
- 删除 `docs/P154.md`。

P154 本轮确认：

- 不新增 Java。
- 不新增测试。
- 不改 production Java（生产代码）。
- 不改现有测试。
- 不改 `dashboard.html`。
- 不新增 controller / endpoint / API / schema / config / service / mapper。
- 不读取 runtime data（运行时数据）。
- 不读取 live market data（实时行情）。
- 不读取 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不调用 `BoundaryCandidateDTO.valid(...)`。
- 不升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P154 stops here. P154 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
