# BACKEND-P163 SourceTrace Service Wiring Closure

## 一、这一步是干嘛的

P163 是 SourceTrace（证据来源追踪）Service Wiring Closure（服务层接线收口）。

P163 是 P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）的最后一步：

- P160：SourceTrace Service Wiring Scope Audit（证据来源追踪服务层接线范围审计），已完成
- P161：SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门），已完成
- P162：SourceTrace Service Minimal Wiring（证据来源追踪服务层最小接线），已完成
- P163：SourceTrace Service Wiring Closure（证据来源追踪服务层接线收口）

本轮只写收口文档，不写 Java，不新增测试，不接 API（接口），不接 dashboard（页面），不读取真实运行时数据，不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 二、P160 做了什么

P160 是 SourceTrace Service Wiring Scope Audit（证据来源追踪服务层接线范围审计）。

P160 审计后确认：不建议直接改 `DefaultSourceAssembler` / `SourceAssembler`。它们虽然接近 SourceTrace（证据来源追踪），但输入是 `RuntimeKlineContextDTO` 和 `DerivativesRiskContextDTO`，不是 P158 的只读证据快照与只读候选结果，直接修改可能误碰 Runtime（系统运行时）路径。

P160 建议使用极小 service wrapper（服务包装器），只包装 `SourceTraceRuntimePopulationHelper.populate(...)`，只返回 `SourceTraceDTO`。

P160 没有写代码，没有新增测试，没有接 Service（服务）/ API（接口）/ dashboard（页面）。

## 三、P161 做了什么

P161 是 SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门）。

P161 授权 P162 最多改 3 个文件：

1. `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`
2. `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`
3. `src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`

P161 没有写代码，没有新增测试，没有接 API（接口）/ dashboard（页面）/ mapper（映射）/ schema（数据库结构）/ config（配置）。

## 四、P162 做了什么

P162 实际新增了：

- `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java`
- `src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java`
- `src/test/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceTest.java`

P162 的实际能力：

- service interface（服务接口）只暴露 `populate(snapshot, result)`
- service impl（服务实现）只调用 `SourceTraceRuntimePopulationHelper.populate(snapshot, result)`
- 输入只允许 `MarketReadOnlyEvidenceSnapshotDTO` 和 `MarketReadOnlyCandidateResultDTO`
- 输出只允许 `SourceTraceDTO`
- null input（空输入）必须 fail-closed（失败时保持关闭）
- 缺证据保持 INCOMPLETE（证据不完整状态）
- 冲突 / 不安全保持 BLOCKED（禁止推进状态）
- 保持 REVIEW_ONLY（只允许复核）
- 保持 `manualReviewRequired=true`（必须人工复核）
- 保持 `notTradeInstruction=true`（不是交易指令）

## 五、P162 没有做什么

P162 没有修改 `SourceTraceRuntimePopulationHelper.java`。

P162 没有修改 `SourceTraceDTO.java`。

P162 没有修改 `DefaultSourceAssembler` / `SourceAssembler`。

P162 没有修改 `PlanServiceImpl`。

P162 没有修改 `BoundaryCandidateServiceImpl`。

P162 没有新增 controller / endpoint / API（控制器 / 接口）。

P162 没有新增 mapper（映射）。

P162 没有修改 schema（数据库结构）。

P162 没有修改 config（配置）。

P162 没有修改 `dashboard.html`（页面）。

P162 没有读取 runtime data（运行时数据）、live market data（实时行情）或 external data（外部数据）。

P162 没有调用 `BoundaryCandidateDTO.valid(...)`。

P162 没有生成 VALID（有效候选状态）。

P162 没有生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P162 没有升级 ExecutionPlan readiness（执行计划升级为可执行）。

P162 没有接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 六、P160-P163 这组完成了什么

P160-P163 把 SourceTrace（证据来源追踪）从 DTO Helper（数据传输对象辅助类）推进到最小 service wrapper（服务包装器）。

这只是安全包装层，不是完整生产链路。现在可以由 Service（服务）调用 Helper（辅助类）返回 `SourceTraceDTO`，但仍然只允许 REVIEW_ONLY（只允许复核）。

这组仍然不能生成交易指令，不能生成交易点位，不能让 dashboard（页面）显示可执行状态，不能 auto-trading（自动交易）。

## 七、P163 的结论

P160-P163 这一组完成。

完成的是 SourceTrace（证据来源追踪）最小 service wrapper（服务包装器）层。

还不是 controller / API（控制器 / 接口）接线。

还不是 dashboard（页面）展示。

还不是真实行情或运行时数据读取。

还不是 Candidate（候选交易计划）生成。

还不是 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）生成。

还不是 auto-trading（自动交易）。

## 八、推荐下一步

推荐下一步不是继续功能扩展，而是：

P164：Project Progress Global Index（项目总进度全局索引）

P164 要做全局扫描，扫描 `docs/`、`src/main/java/`、`src/test/java/`、schema / config / dashboard（数据库结构 / 配置 / 页面），汇总已完成、正在推进、暂停、后期必须回来做、容易误判完成、禁止提前做的线路。

这是之前约定好的：P160-P163 完成后，立刻做全局总进度索引，不继续开新功能。

## 九、仍然禁止的路径

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

## 十、边界确认

P163 只完成一个 Closure（收口）文档。

P163 删除 placeholder（占位文档）`docs/P163.md`。

P163 不新增 Java，不新增测试，不改 production Java（生产代码），不改现有测试，不改 `dashboard.html`（页面），不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / 数据库结构 / 配置 / 服务 / 映射）。

P163 不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不调用 `BoundaryCandidateDTO.valid(...)`，不升级 ExecutionPlan readiness（执行计划可执行状态），不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
