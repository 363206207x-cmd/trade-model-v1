# BACKEND-P159 SourceTrace Runtime Population Helper Closure

## 1. 这一步是干嘛的

P159 是 SourceTrace Runtime Population Helper Closure（运行时证据来源填充辅助类收口）。

它的作用是确认 P157-P158 这一小段已经完成：

- P157 定义 Implementation Scope Gate（代码实现范围门）。
- P158 新增最小 DTO（数据传输对象）层 Helper（辅助类）和测试。
- P159 做 Closure（收口），说明这一小段完成了什么、没有完成什么、后面还禁止什么。

P159 本轮只做文档收口：

- 不写 Java。
- 不新增测试。
- 不接真实系统运行链路。
- 不读取 Runtime（系统运行时）数据。
- 不读取 live market data（实时行情）。
- 不读取 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 VALID（有效候选状态）。
- 不升级 readiness（可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 2. P157 做了什么

P157 是 SourceTrace Runtime Population Implementation Scope Gate（运行时证据来源填充代码实现范围门）。

P157 只授权未来 P158 做极小 DTO（数据传输对象）层 Helper（辅助类）。

P157 明确 P158 只允许：

1. 新增 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
2. 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelperTest.java`
3. 必要时轻微修改 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P157 的边界：

- P157 没有写代码。
- P157 没有新增测试。
- P157 没有接真实系统运行链路。
- P157 没有授权 service（服务）层。
- P157 没有授权 mapper（映射）、controller（控制器）、endpoint / API（接口）、schema（数据库结构）、config（配置）或 `dashboard.html`（页面）。
- P157 没有授权 VALID（有效候选状态）、readiness（可执行就绪）、真实交易点位或自动交易。

## 3. P158 做了什么

P158 按 P157 授权完成了 SourceTrace Runtime Population Helper（运行时证据来源填充辅助类）。

P158 实际改动：

- 新增 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
- 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelperTest.java`
- 未修改 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P158 的 Helper（辅助类）只做 DTO（数据传输对象）层映射：

- 从 `MarketReadOnlyEvidenceSnapshotDTO`（只读证据快照）读取已有证据字段。
- 从 `MarketReadOnlyCandidateResultDTO`（只读候选结果）读取 blockingReasons（禁止推进原因）和 reviewMode（复核模式）。
- 组装到 `SourceTraceDTO`（证据来源追踪数据传输对象）。

P158 没有把 Helper（辅助类）接到 service（服务）、controller（控制器）、endpoint / API（接口）、dashboard（页面）或真实数据读取链路。

## 4. P158 验证了什么

P158 已验证：

- `SourceTraceRuntimePopulationHelper` 可以从 `MarketReadOnlyEvidenceSnapshotDTO`（只读证据快照）组装 `SourceTraceDTO`。
- 可以从 `MarketReadOnlyCandidateResultDTO`（只读候选结果）补充 blockingReasons（禁止推进原因）。
- 可以从 `MarketReadOnlyCandidateResultDTO`（只读候选结果）补充 reviewMode（复核模式）。
- 可以映射 source owner（证据来源所有者）。
- 可以映射 source ref（证据来源引用）。
- 可以映射 source timeframe（证据来源周期）。
- 可以映射 source window（证据窗口）。
- 可以映射 freshness（新鲜度）。
- 缺证据时保持 INCOMPLETE（证据不完整状态）。
- 冲突 / 不安全时保持 BLOCKED（禁止推进状态）。
- 始终保持 `manualReviewRequired=true`（必须人工复核）。
- 始终保持 `notTradeInstruction=true`（不是交易指令）。
- 始终保持 REVIEW_ONLY（只允许复核）。
- 不产生交易指令。
- 不产生 readiness（可执行就绪）。
- 不产生 order / execution / automation（下单 / 执行 / 自动化）字段或行为。

P158 证明的是 DTO Helper（数据传输对象辅助类）层映射能力，不是生产服务链路。

## 5. P158 没有做什么

P158 明确没有做以下内容：

- 没有新增 service（服务）。
- 没有新增 mapper（映射）。
- 没有新增 controller（控制器）。
- 没有新增 endpoint / API（接口）。
- 没有修改 schema（数据库结构）。
- 没有修改 config（配置）。
- 没有修改 `dashboard.html`（页面）。
- 没有读取 runtime data（运行时数据）。
- 没有读取 live market data（实时行情）。
- 没有读取 external data（外部数据）。
- 没有调用 `BoundaryCandidateDTO.valid(...)`。
- 没有生成 VALID（有效候选状态）。
- 没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 没有升级 ExecutionPlan readiness（执行计划可执行状态）。
- 没有接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P158 也没有把 SourceTrace（证据来源追踪）输出展示到 dashboard（页面）。

## 6. P159 的结论

P157-P158 这一小段完成。

完成的是：

- DTO Helper（数据传输对象辅助类）层的 SourceTrace（证据来源追踪）映射能力。
- 从只读证据快照到 SourceTrace（证据来源追踪）的字段映射。
- 从只读候选结果到 SourceTrace（证据来源追踪）的 blockingReasons（禁止推进原因）和 reviewMode（复核模式）映射。
- REVIEW_ONLY（只允许复核）安全姿态保持。
- INCOMPLETE（证据不完整状态）fail-closed（失败时保持关闭）保持。
- BLOCKED（禁止推进状态）fail-closed（失败时保持关闭）保持。

还没有完成的是：

- service（服务）层接线。
- endpoint / API（接口）。
- dashboard（页面）展示。
- 真实 Runtime（系统运行时）数据读取。
- 真实行情读取。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）生成。
- 交易指令生成。
- 自动交易。

因此，P159 的结论是：P157-P158 的 DTO Helper（数据传输对象辅助类）层闭环完成，但还不是 Production Wiring（真正接入系统运行链路）。

## 7. 推荐下一步

推荐下一步是：

```text
P160: SourceTrace Service Wiring Scope Audit
```

中文解释：P160 是 SourceTrace Service Wiring Scope Audit（证据来源追踪服务层接线范围审计）。

P160 仍然不直接写代码。

P160 要检查是否允许从 DTO Helper（数据传输对象辅助类）进入 service（服务）层。

P160 要决定未来 service（服务）层最小接线允许改哪些文件。

P160 仍然必须禁止：

- VALID（有效候选状态）。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实生成。
- readiness（可执行就绪）。
- dashboard 可执行状态。
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 8. 仍然禁止的路径

以下路径在 P159 之后仍然禁止：

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

这些路径不能因为 P157-P158 完成 DTO Helper（数据传输对象辅助类）层验证而被间接授权。

## 9. P159 边界确认

P159 本轮只完成一个 Closure（收口）文档：

- 新增 `docs/PHASE_BACKEND_P159_SOURCETRACE_RUNTIME_POPULATION_HELPER_CLOSURE.md`。
- 删除 `docs/P159.md`。

P159 本轮确认：

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

P159 stops here. P159 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
