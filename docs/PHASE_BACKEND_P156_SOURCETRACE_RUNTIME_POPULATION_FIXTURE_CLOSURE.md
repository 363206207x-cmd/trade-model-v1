# BACKEND-P156 SourceTrace Runtime Population Fixture Closure

## 1. 这一步是干嘛的

P156 是 SourceTrace Runtime Population Fixture Closure（运行时证据来源填充测试夹具收口）。

它的作用是确认 P154-P155 这一小段已经完成：

- P154 做 Authorization Gate（授权门）。
- P155 做最小 Fixture（测试夹具）层验证。
- P156 做 Closure（收口），说明这一小段完成了什么、没有完成什么、后面还禁止什么。

P156 本轮只做文档收口：

- 不写 Java。
- 不新增测试。
- 不接真实系统运行链路。
- 不读取 Runtime（系统运行时）数据。
- 不读取 live market data（实时行情）。
- 不接 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 VALID（有效候选状态）。
- 不升级 readiness（可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 2. P154 做了什么

P154 是 SourceTrace Runtime Population Authorization Gate（运行时证据来源填充授权门）。

P154 只授权未来 P155 做最小 Fixture（测试夹具）层验证。

P154 明确未来 P155 最多允许：

1. 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureTest.java`
2. 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureHelper.java`
3. 必要时修改 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P154 的边界：

- P154 没有写代码。
- P154 没有新增测试。
- P154 没有接真实系统运行链路。
- P154 没有授权 service（服务）生产链路。
- P154 没有授权 mapper（映射）、controller（控制器）、endpoint / API（接口）、schema（数据库结构）、config（配置）或 `dashboard.html`（页面）。
- P154 没有授权 VALID（有效候选状态）、readiness（可执行就绪）、真实交易点位或自动交易。

## 3. P155 做了什么

P155 按 P154 授权完成了最小 SourceTrace Runtime Population Fixture Test（运行时证据来源填充测试夹具）。

P155 实际改动：

- 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureTest.java`
- 新增 `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationFixtureHelper.java`
- 修改 `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P155 修改 `SourceTraceDTO.java` 的原因：

- 让 SourceTrace（证据来源追踪）能承载 source owner（证据来源所有者）。
- 让 SourceTrace（证据来源追踪）能承载 source ref（证据来源引用）。
- 让 SourceTrace（证据来源追踪）能承载 source timeframe（证据来源周期）。
- 让 SourceTrace（证据来源追踪）能承载 source window（证据窗口）。
- 让 SourceTrace（证据来源追踪）能承载 freshness status（新鲜度状态）。
- 让 SourceTrace（证据来源追踪）能承载 blockingReasons（禁止推进原因）。
- 让 SourceTrace（证据来源追踪）能承载 reviewMode（复核模式）。

这些只是 DTO（数据传输对象）字段承载能力，不是 Production Wiring（真正接入系统运行链路）。

P155 没有把这些字段接到生产 service（服务）、API（接口）、页面或真实行情读取。

## 4. P155 验证了什么

P155 验证了 SourceTrace（证据来源追踪）可以承载基础 Runtime（系统运行时）证据来源信息。

P155 已验证：

- SourceTrace（证据来源追踪）可以承载 source owner（证据来源所有者）。
- SourceTrace（证据来源追踪）可以承载 source ref（证据来源引用）。
- SourceTrace（证据来源追踪）可以承载 timeframe（周期）。
- SourceTrace（证据来源追踪）可以承载 source window（证据窗口）。
- SourceTrace（证据来源追踪）可以承载 freshness（新鲜度）。
- missing fields（缺失字段）能进入 SourceTrace（证据来源追踪）。
- blocking reasons（禁止推进原因）能进入 SourceTrace（证据来源追踪）。
- 缺证据时保持 INCOMPLETE（证据不完整状态）。
- 冲突 / 不安全 / 过期不安全证据时保持 BLOCKED（禁止推进状态）。
- `manualReviewRequired=true`（必须人工复核）。
- `notTradeInstruction=true`（不是交易指令）。
- `reviewMode=REVIEW_ONLY`（只允许复核）。
- 不出现 order / execution / automation（下单 / 执行 / 自动化）字段或行为。
- 不生成 VALID（有效候选状态）。
- 不生成 readiness（可执行就绪）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P155 的测试只证明字段承载和 fail-closed（失败时保持关闭）姿态，不证明生产候选交易计划已经可用。

## 5. P155 没有做什么

P155 明确没有做以下内容：

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
- 没有升级 ExecutionPlan readiness（执行计划可执行状态）。
- 没有接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P155 也没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 6. P156 的结论

P154-P155 这一小段完成。

完成的是：

- SourceTrace（证据来源追踪）字段承载能力。
- Runtime（系统运行时）Population（填充）的 Fixture（测试夹具）验证。
- REVIEW_ONLY（只允许复核）安全姿态验证。
- INCOMPLETE（证据不完整状态）fail-closed 验证。
- BLOCKED（禁止推进状态）fail-closed 验证。

没有完成的是：

- 真实系统运行时接线。
- 真实生产 service（服务）填充 SourceTrace（证据来源追踪）。
- 真实 Candidate（候选交易计划）生成。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）生成。
- 页面可执行状态。
- 交易指令。
- 自动交易。

因此，P156 的结论是：P154-P155 的 Fixture（测试夹具）层闭环完成，但还不是 Production Wiring（真正接入系统运行链路）。

## 7. 推荐下一步

推荐下一步是：

```text
P157: SourceTrace Runtime Population Implementation Scope Gate
```

中文解释：P157 是 SourceTrace Runtime Population Implementation Scope Gate（运行时证据来源填充代码实现范围门）。

P157 仍然不直接写代码。

P157 要规定未来真正第一小段代码实现到底允许改哪些文件。

P157 必须决定是否允许从 Fixture（测试夹具）进入真正 production Java（生产代码）。

P157 仍然必须禁止：

- VALID（有效候选状态）。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实生成。
- readiness（可执行就绪）。
- dashboard 可执行状态。
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 8. 仍然禁止的路径

以下路径在 P156 之后仍然禁止：

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

这些路径不能因为 P154-P155 完成 Fixture（测试夹具）层验证而被间接授权。

## 9. P156 边界确认

P156 本轮只完成一个 Closure（收口）文档：

- 新增 `docs/PHASE_BACKEND_P156_SOURCETRACE_RUNTIME_POPULATION_FIXTURE_CLOSURE.md`。
- 删除 `docs/P156.md`。

P156 本轮确认：

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

P156 stops here. P156 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
