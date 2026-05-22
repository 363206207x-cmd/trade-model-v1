# BACKEND-P161 SourceTrace Service Wiring Authorization Gate

## 一、这一步是干嘛的

P161 是 SourceTrace（证据来源追踪）Service Wiring Authorization Gate（服务层接线授权门）。

P161 是 P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）的第二步：

- P160：SourceTrace Service Wiring Scope Audit（证据来源追踪服务层接线范围审计），已完成
- P161：SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门）
- P162：SourceTrace Service Minimal Wiring（证据来源追踪服务层最小接线）
- P163：SourceTrace Service Wiring Closure（证据来源追踪服务层接线收口）

本轮只写授权门文档，不写 Java，不新增测试，不接 Service（服务），不接 API（接口），不接 dashboard（页面），不读取真实运行时数据，不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 二、P160 审计结论

P160 发现当前没有一个完全合适、低风险、专门承接 `SourceTraceRuntimePopulationHelper` 的现有 Service（服务）。

`SourceAssembler` / `DefaultSourceAssembler` 虽然最接近 SourceTrace（证据来源追踪），但它们的输入是 `RuntimeKlineContextDTO` 和 `DerivativesRiskContextDTO`，不是 P158 的只读证据快照与只读候选结果。

直接修改 `DefaultSourceAssembler` 风险更高，可能误碰 Runtime（系统运行时）路径。

P160 建议更保守地新增一个极小 service wrapper（服务包装器）。该 wrapper（包装器）只调用 `SourceTraceRuntimePopulationHelper.populate(...)`，只返回 `SourceTraceDTO`，不接 controller / endpoint / API / dashboard（控制器 / 接口 / 页面），不接 mapper / schema / config（映射 / 数据库结构 / 配置）。

## 三、是否允许 P162 写代码

可以允许 P162 写最小 Service（服务）层代码，但 P162 必须极小。

P162 只能新增 service wrapper（服务包装器）和对应测试。P162 不能改已有业务 Service（服务），不能改 `DefaultSourceAssembler`，不能改 `SourceAssembler`，不能改 `PlanServiceImpl`，不能改 `BoundaryCandidateServiceImpl`，不能改 `DashboardController`，不能改 `dashboard.html`。

P162 不能接 API（接口），不能接 mapper / schema / config（映射 / 数据库结构 / 配置），不能读取真实 Runtime（系统运行时）数据。

## 四、P162 允许改哪些文件

P162 最多允许改 1-3 个文件。授权范围如下：

1. `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`
2. `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`
3. `src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`

如果 `service/planboundary` 目录不存在，P162 可以新建该目录。

P162 不允许改 `SourceTraceRuntimePopulationHelper.java`。如果 P162 测试发现 helper（辅助类）有明显缺口，必须停止并另开授权。

P162 不允许改 `SourceTraceDTO.java`。

P162 不允许改 controller / endpoint / API / dashboard / mapper / schema / config（控制器 / 接口 / 页面 / 映射 / 数据库结构 / 配置）。

P162 不允许改 `DefaultSourceAssembler` / `SourceAssembler`。

P162 不允许改 `PlanServiceImpl`。

P162 不允许改 `BoundaryCandidateServiceImpl`。

## 五、P162 允许做什么

未来 P162 只能做：

- 新增一个 service interface（服务接口）
- 新增一个 service impl（服务实现）
- service impl（服务实现）只调用 `SourceTraceRuntimePopulationHelper.populate(snapshot, result)`
- 输入只能是 `MarketReadOnlyEvidenceSnapshotDTO` 和 `MarketReadOnlyCandidateResultDTO`
- 输出只能是 `SourceTraceDTO`
- 保持 REVIEW_ONLY（只允许复核）
- 保持 `manualReviewRequired=true`（必须人工复核）
- 保持 `notTradeInstruction=true`（不是交易指令）
- 缺证据时保持 INCOMPLETE（证据不完整状态）
- 冲突 / 不安全时保持 BLOCKED（禁止推进状态）
- null input（空输入）必须 fail-closed（失败时保持关闭）

## 六、P162 禁止做什么

未来 P162 仍然禁止：

- 不允许接 controller / endpoint / API（控制器 / 接口）
- 不允许接 dashboard（页面）
- 不允许新增 mapper（映射）
- 不允许修改 schema（数据库结构）
- 不允许修改 config（配置）
- 不允许读取真实 runtime data（运行时数据）
- 不允许读取 live market data（实时行情）
- 不允许读取 external data（外部数据）
- 不允许调用 `BoundaryCandidateDTO.valid(...)`
- 不允许生成 VALID（有效候选状态）
- 不允许生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）
- 不允许升级 ExecutionPlan readiness（执行计划升级为可执行）
- 不允许接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）

## 七、仍然禁止的路径

- production candidate generation（生产候选交易计划生成）
- source-owned runtime candidate generation（运行时证据来源候选生成）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- production VALID mapping（生产环境映射为有效候选）
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard readiness mutation（页面显示可执行状态）
- dashboard.html changes（页面改动）
- controller / endpoint / API wiring（接口接线）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- runtime data reads（读取运行时数据）
- live market data reads（读取实时行情）
- external data integration（接外部数据）
- WebClient / RestTemplate（网络请求工具）
- order API（下单接口）
- execution API（执行接口）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 八、推荐下一步

推荐下一步为：

P162：SourceTrace Service Minimal Wiring（证据来源追踪服务层最小接线）

P162 才可以开始最小 Service（服务）层代码，但只能做 service wrapper（服务包装器），不能接 API（接口），不能接 dashboard（页面），不能生成交易点位，不能 auto-trading（自动交易）。

## 九、边界确认

P161 只完成一个 Authorization Gate（授权门）文档。

P161 删除 placeholder（占位文档）`docs/P161.md`。

P161 不新增 Java，不新增测试，不改 production Java（生产代码），不改现有测试，不改 `dashboard.html`（页面），不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / 数据库结构 / 配置 / 服务 / 映射）。

P161 不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不调用 `BoundaryCandidateDTO.valid(...)`，不升级 ExecutionPlan readiness（执行计划可执行状态），不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
