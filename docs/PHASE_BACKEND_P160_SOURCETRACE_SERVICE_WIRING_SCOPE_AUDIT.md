# BACKEND-P160 SourceTrace Service Wiring Scope Audit

## 一、这一步是干嘛的

P160 是 SourceTrace（证据来源追踪）Service Wiring Scope Audit（服务层接线范围审计）。

P160 是 P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）的第一步：

- P160：SourceTrace Service Wiring Scope Audit（证据来源追踪服务层接线范围审计）
- P161：SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门）
- P162：SourceTrace Service Minimal Wiring（证据来源追踪服务层最小接线）
- P163：SourceTrace Service Wiring Closure（证据来源追踪服务层接线收口）

本轮只做文档审计，不写 Java，不新增测试，不接 Service（服务），不接 API（接口），不接 dashboard（页面），不读取真实 Runtime（系统运行时）数据，不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 二、P157-P159 已经完成什么

P157 定义了 P158 的 DTO（数据传输对象）Helper（辅助类）范围。

P158 新增了：

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
- `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelperTest.java`

P158 没有修改：

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`

P158 只把 `MarketReadOnlyEvidenceSnapshotDTO`（只读证据快照）和 `MarketReadOnlyCandidateResultDTO`（只读候选结果）映射进 `SourceTraceDTO`（证据来源追踪数据传输对象）。这个 Helper（辅助类）会写入 source owner（证据来源所有者）、source ref（证据来源引用）、source timeframe（证据来源周期）、source window（证据窗口）、freshness（新鲜度）、missing fields（缺失字段）、blocking reasons（禁止推进原因）和 reviewMode（复核模式）。

P159 已经收口确认：P158 没有接 service / mapper / controller / API / schema / config / dashboard（服务 / 映射 / 控制器 / 接口 / 数据库结构 / 配置 / 页面），也没有生成交易点位或 auto-trading（自动交易）。

## 三、本轮只读审计范围

P160 允许只读查看这些文件：

- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java`
- `src/test/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelperTest.java`
- `src/main/java/org/example/trademodel/dto/planboundary/SourceTraceDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceSnapshotDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTO.java`

P160 允许只读搜索这些候选区域，但不允许修改：

- `src/main/java/org/example/trademodel/service/`
- `src/main/java/org/example/trademodel/service/impl/`
- `src/main/java/org/example/trademodel/assembler/`
- `src/main/java/org/example/trademodel/controller/`

本轮审计发现 `src/main/java/org/example/trademodel/assembler/` 目录当前不存在。现有候选主要集中在 `service`、`service/impl`、`service/dashboard` 和 `controller`。

## 四、服务层接线问题审计

1. 当前是否已经存在适合承接 `SourceTraceRuntimePopulationHelper` 的 Service（服务）？

   当前没有一个完全合适、低风险、专门承接 P158 Helper（辅助类）的 Service（服务）。`SourceAssembler` / `DefaultSourceAssembler` 已经与 SourceTrace（证据来源追踪）相关，但它的输入是 `RuntimeKlineContextDTO` 和 `DerivativesRiskContextDTO`，不是 P158 的只读证据快照与只读候选结果。直接改它可能误碰 Runtime（系统运行时）路径。

2. 如果存在，哪个 Service（服务）最适合未来最小接线？

   如果未来必须复用现有 service 入口，`SourceAssembler` / `DefaultSourceAssembler` 是最接近 SourceTrace（证据来源追踪）的候选。但 P160 不建议默认修改它，因为它已经是 Spring Service（服务），并且更靠近 Runtime（系统运行时）来源组装。未来 P161 必须单独判断是否允许改它。

3. 如果不存在，未来是否应该新增一个极小 Service（服务）？

   是。更保守的下一步是未来 P161 授权一个极小的 service 层 wrapper（服务层包装器），只调用 `SourceTraceRuntimePopulationHelper.populate(...)`，只返回 `SourceTraceDTO`，不接 controller / endpoint / API / dashboard（控制器 / 接口 / 页面），不接 mapper / schema / config（映射 / 数据库结构 / 配置）。

4. Service（服务）层接线是否会误触发 API / dashboard / mapper / schema？

   如果只新增一个孤立 service wrapper（服务包装器），并且不注入 controller / dashboard adapter / mapper，就不会触发 API / dashboard / mapper / schema。若改 `DashboardController`、`DefaultDashboardSourceTraceDetailAdapter`、`PlanServiceImpl` 或 mapper，则会越界。

5. Service（服务）层接线是否会误生成 VALID（有效候选状态）？

   有风险，尤其不能接 `BoundaryCandidateServiceImpl`。该实现里存在 `BoundaryCandidateDTO.valid(...)` 路径，虽然已有阻断条件，但 P160-P162 不允许接入任何可能生成 VALID（有效候选状态）的路径。

6. Service（服务）层接线是否会误生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）？

   如果只包装 P158 Helper（辅助类），不会生成真实 entry / stop / TP / RR。若接入 `BoundaryCandidateServiceImpl`、`DefaultSourceAssembler` 的 runtime 边界字段，或任何 candidate generation（候选交易计划生成）路径，就有误生成交易点位的风险。

7. Service（服务）层接线是否会误升级 Readiness（是否允许进入下一步 / 可执行就绪）？

   有风险，尤其不能接 `PlanServiceImpl`。该服务会根据 `SourceTraceDTO` 更新 ExecutionPlan（执行计划）的 readiness（是否允许进入下一步）。P160-P162 不允许升级 ExecutionPlan readiness（执行计划可执行状态）。

8. Service（服务）层接线如何继续保持 REVIEW_ONLY（只允许复核）？

   未来最小 service 只能传递 P158 Helper（辅助类）已经固定的 `reviewMode=REVIEW_ONLY`，并继续保持 `manualReviewRequired=true` 和 `notTradeInstruction=true`。不得把 REVIEW_ONLY（只允许复核）转换成 VALID（有效候选状态）、ready（就绪）或 executable（可执行）。

9. 缺证据时如何继续保持 INCOMPLETE（证据不完整状态）？

   未来最小 service 必须保留 `MarketReadOnlyEvidenceSnapshotDTO` 和 `MarketReadOnlyCandidateResultDTO` 的缺字段判断，并把缺 source owner / source ref / source timeframe / source window / freshness（证据来源所有者 / 引用 / 周期 / 窗口 / 新鲜度）继续映射为 INCOMPLETE（证据不完整状态），不能推进成 VALID（有效候选状态）。

10. 冲突 / 不安全时如何继续保持 BLOCKED（禁止推进状态）？

    未来最小 service 必须保留 blockingReasons（禁止推进原因）和 `SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY`，把 conflicting source（证据来源冲突）、unsafe substitution（不安全替代数据）、stale unsafe source window（过期且不安全证据窗口）继续保持为 BLOCKED（禁止推进状态），不能推进成 REVIEW_ONLY_CANDIDATE（只读复核候选）、VALID（有效候选状态）、readiness（可执行就绪）或 executable surface（可执行入口）。

## 五、P160 结论

P160 不允许直接写代码。

P160 可以允许未来 P161 做 SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门）。

P161 必须明确未来 P162 最多允许改哪 1-3 个文件。P162 如果实现，也必须只做最小 Service（服务）层接线，只能把 P158 Helper（辅助类）包成一个安全的 REVIEW_ONLY（只允许复核）路径。

P162 不允许接 controller / endpoint / API / dashboard（控制器 / 接口 / 页面）。P162 不允许生成交易点位。P162 不允许 auto-trading（自动交易）。

## 六、推荐下一步

推荐下一步为：

P161：SourceTrace Service Wiring Authorization Gate（证据来源追踪服务层接线授权门）

P161 仍然不写代码。P161 只定义未来 P162 最小 service（服务）接线允许改哪些文件。P161 必须继续禁止 VALID（有效候选状态）、entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、readiness（是否允许进入下一步）、dashboard 可执行状态、API（接口）和 auto-trading（自动交易）。

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

## 八、边界确认

P160 只完成一个审计文档。

P160 删除 placeholder（占位文档）`docs/P160.md`。

P160 不新增 Java，不新增测试，不改 production Java（生产代码），不改现有测试，不改 dashboard.html（页面），不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / 数据库结构 / 配置 / 服务 / 映射）。

P160 不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不调用 `BoundaryCandidateDTO.valid(...)`，不升级 ExecutionPlan readiness（执行计划可执行状态），不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。
