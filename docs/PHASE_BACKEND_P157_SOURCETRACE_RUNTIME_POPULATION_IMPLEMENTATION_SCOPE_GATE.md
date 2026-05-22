# BACKEND-P157 SourceTrace Runtime Population Implementation Scope Gate

## 1. 这一步是干嘛的

P157 是 SourceTrace Runtime Population Implementation Scope Gate（运行时证据来源填充代码实现范围门）。

它只定义未来第一小段 Production Java（生产代码）允许改哪些文件、允许做什么、仍然禁止什么。

P157 本轮不是代码实现：

- 不写 Java。
- 不新增测试。
- 不接 Production Wiring（真正接入系统运行链路）。
- 不读取 Runtime（系统运行时）数据。
- 不读取 live market data（实时行情）。
- 不接 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 VALID（有效候选状态）。
- 不升级 readiness（可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P157 只是 Scope Gate（范围门），不是 Implementation（代码实现）。

## 2. P154-P156 已经完成了什么

P154-P156 已经完成 SourceTrace Runtime Population（运行时证据来源填充）从授权到测试夹具收口的最小闭环。

已完成内容：

- P154 授权 P155 做 Fixture（测试夹具）层验证。
- P155 新增 `SourceTraceRuntimePopulationFixtureTest.java`。
- P155 新增 `SourceTraceRuntimePopulationFixtureHelper.java`。
- P155 必要地修改 `SourceTraceDTO.java`。
- P156 已经收口确认：P155 只完成字段承载和 Fixture（测试夹具）验证。

P155 对 `SourceTraceDTO.java` 的修改让 SourceTrace（证据来源追踪）可以承载：

- source owner（证据来源所有者）
- source ref（证据来源引用）
- source timeframe（证据来源周期）
- source window（证据窗口）
- freshness（新鲜度）
- blockingReasons（禁止推进原因）
- reviewMode（复核模式）

P154-P156 没有完成以下内容：

- 没有接生产 service（服务）。
- 没有接 mapper（映射）。
- 没有接 controller（控制器）。
- 没有接 endpoint / API（接口）。
- 没有改 `dashboard.html`（页面）。
- 没有读取真实行情。
- 没有读取 Runtime（系统运行时）数据。
- 没有生成交易点位。
- 没有生成 VALID（有效候选状态）。
- 没有接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 3. 是否允许未来进入生产代码

P157 的保守结论是：可以允许未来单独开 P158，开始第一小段 Production Java（生产代码）实现。

但是 P158 必须极小。

P158 只允许把已有 DTO（数据传输对象）字段映射到 SourceTrace（证据来源追踪）。

P158 必须继续保持：

- 只能输出 REVIEW_ONLY（只允许复核）。
- 不允许生成 VALID（有效候选状态）。
- 不允许生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不允许升级 readiness（可执行就绪）。
- 不允许改 `dashboard.html`。
- 不允许接 controller / endpoint / API（控制器 / 接口 / API）。
- 不允许接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P158 如果需要超出 P157 允许的文件范围，必须停止并另开新的 Scope Gate（范围门）。

## 4. 未来 P158 允许改哪些文件

P157 授权未来 P158 最多允许改 1-3 个文件。

保守授权范围如下：

1. `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
2. `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelperTest.java`
3. 如确实必须，最多允许轻微修改：
   `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P158 的默认策略必须是：

- 默认不再改 `SourceTraceDTO.java`。
- 只有测试无法用现有字段完成时，才允许轻微修改 `SourceTraceDTO.java`。
- 不允许改 service（服务）。
- 不允许改 mapper（映射）。
- 不允许改 controller（控制器）。
- 不允许改 endpoint / API（接口）。
- 不允许改 schema（数据库结构）。
- 不允许改 config（配置）。
- 不允许改 `dashboard.html`。
- 不允许读取真实 Runtime（系统运行时）数据。
- 不允许接真实系统链路。

P158 的生产代码只能是 DTO 层 helper（辅助类），不能是服务链路。

## 5. 未来 P158 允许做什么

未来 P158 只能做这些事：

- 从 `MarketReadOnlyEvidenceSnapshotDTO`（只读证据快照）组装 SourceTrace（证据来源追踪）。
- 从 `MarketReadOnlyCandidateResultDTO`（只读候选结果）补充 blockingReasons（禁止推进原因）。
- 从 `MarketReadOnlyCandidateResultDTO`（只读候选结果）补充 reviewMode（复核模式）。
- 映射 source owner（证据来源所有者）。
- 映射 source ref（证据来源引用）。
- 映射 source timeframe（证据来源周期）。
- 映射 source window（证据窗口）。
- 映射 freshness（新鲜度）。
- 缺证据时保持 INCOMPLETE（证据不完整状态）。
- 冲突 / 不安全时保持 BLOCKED（禁止推进状态）。
- 始终保持 `manualReviewRequired=true`（必须人工复核）。
- 始终保持 `notTradeInstruction=true`（不是交易指令）。
- 始终保持 REVIEW_ONLY（只允许复核）。
- 不产生交易指令。
- 不产生 readiness（可执行就绪）。
- 不产生 order / execution / automation（下单 / 执行 / 自动化）字段或行为。

P158 只能把已有只读证据和只读候选结果放进 SourceTrace（证据来源追踪）。它不能创建新的交易判断。

## 6. 未来 P158 禁止做什么

未来 P158 仍然禁止：

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

P158 不能把 DTO helper（数据传输对象辅助类）伪装成完整 Production Wiring（真正接入系统运行链路）。

## 7. 仍然禁止的路径

以下路径在 P157 之后仍然禁止：

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

这些路径不能因为 P157 允许未来 P158 做 DTO 层 helper（辅助类）而被间接授权。

## 8. 推荐下一步

推荐下一步是：

```text
P158: SourceTrace Runtime Population Helper
```

中文解释：P158 是 SourceTrace Runtime Population Helper（运行时证据来源填充辅助类）。

P158 才可以开始非常小的 Production Java（生产代码）实现。

但是 P158 也只能做一个 DTO 层 helper（辅助类）。

P158 只能把已有只读证据快照和只读候选结果映射进 SourceTrace（证据来源追踪）。

P158 不能接 service（服务）链路。

P158 不能接接口。

P158 不能接页面。

P158 不能生成交易点位。

P158 不能自动交易。

## 9. P157 结论

P157 只完成 Scope Gate（范围门）文档。

P157 不写代码。

P157 不新增测试。

P157 不授权完整生产链路。

P157 只允许未来 P158 做一个极小的 DTO 层 helper（辅助类）。

P157 仍然不允许：

- VALID（有效候选状态）。
- readiness（可执行就绪）。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- dashboard 可执行状态。
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

后续如果要扩大到 service（服务）、API（接口）、dashboard（页面）或真实 Runtime（系统运行时）数据，必须另开新的授权门。

## 10. P157 边界确认

P157 本轮只完成一个 Scope Gate（范围门）文档：

- 新增 `docs/PHASE_BACKEND_P157_SOURCETRACE_RUNTIME_POPULATION_IMPLEMENTATION_SCOPE_GATE.md`。
- 删除 `docs/P157.md`。

P157 本轮确认：

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

P157 stops here. P157 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
