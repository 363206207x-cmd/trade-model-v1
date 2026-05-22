# BACKEND-P172 BoundaryCandidate Read-Only Candidate Display Scope Audit

Issue: #458
PR: #459
Branch: `p172`
Base: `0ad2086 BACKEND-P171 Project Progress Index Refresh After SourceTrace Display (#457)`

本文件是 P172 的唯一交付物。P172 只做 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Scope Audit（范围审计），不写 Java，不新增测试，不接 Dashboard（首页工作台），不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。

## 一、这一步是干嘛的

P172 是 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Scope Audit（范围审计）。

P172 是 P172-P175 最大安全任务包的第一步。这个任务包只允许审计和推进 BoundaryCandidate（边界候选交易计划）的只读展示路径，不允许把它变成生产候选生成、交易点位生成或执行入口。

本轮边界固定如下：

- 不写 Java。
- 不新增测试。
- 不接 Dashboard（首页工作台）。
- 不修改 `dashboard.html`。
- 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）。
- 不新增 schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不生成生产 `VALID`（有效候选状态）。
- 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

P172 只回答一个问题：未来是否可以把 BoundaryCandidate（边界候选交易计划）的状态、缺失字段、阻断原因和人工复核模式放进只读展示路径。

## 二、P171 / PROJECT_PROGRESS_INDEX 的依据

P172 以 `docs/PROJECT_PROGRESS_INDEX.md` 为总索引依据，不重新扩大项目范围。

P171 已刷新项目总索引，确认 P165-P170 已经完成 SourceTrace（证据来源追踪）read-only display（只读展示）闭环。也就是说，SourceTrace（证据来源追踪）已经从 service wrapper（服务包装层）推进到 Dashboard Detail（首页详情）Read Model（只读输出模型），并在 `dashboard.html` 中完成只读展示。

`docs/PROJECT_PROGRESS_INDEX.md` 同时给出三个重要限制：

- SourceTrace（证据来源追踪）只读输出和 Dashboard（首页工作台）只读展示已完成，但真实生产候选、生产 `VALID`（有效候选状态）、真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）、Readiness（可执行就绪）、自动交易仍未完成。
- 个人可用最快路径可以进入 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）方向。
- BoundaryCandidate（边界候选交易计划）真实候选生成仍暂停，生产 `VALID`（有效候选状态）未授权，真实点位来源仍未闭环。

因此 P172 只能审计只读展示，不能审计真实候选生成。P172 的正确结论不是“BoundaryCandidate（边界候选交易计划）可以交易”，而是“未来可以在受限范围内展示它为什么仍需人工复核、为什么不能推进”。

## 三、当前 BoundaryCandidate 能力

只读扫描确认，BoundaryCandidate（边界候选交易计划）相关文件已经存在，但能力边界必须分开看。

### 1. DTO 和状态枚举

当前已有：

- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryCandidateDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryStatusEnum.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryEntryDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryStopDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundaryTakeProfitLevelDTO.java`
- `src/main/java/org/example/trademodel/dto/planboundary/BoundarySourceFieldsDTO.java`

`BoundaryCandidateDTO`（边界候选交易计划数据对象）已经有字段承载 symbol（交易标的）、timeframe（周期）、entry（入场）、stop（止损）、takeProfitLevels（止盈层级）、sourceFields（来源字段）、dataQualityScore（数据质量分）、manualReviewRequired（必须人工复核）、notTradeInstruction（不是交易指令）和 blockingReasons（禁止推进原因）。

`BoundaryStatusEnum`（边界状态枚举）当前包含：

- `VALID`（有效候选状态）。
- `INCOMPLETE`（证据不完整）。
- `WATCH_ONLY`（仅观察）。
- `INVALID`（无效）。

这说明 DTO（数据对象）层已经能表达候选形状和候选状态，但 DTO 字段存在不等于真实数据来源完成，也不等于生产候选生成完成。

### 2. valid factory（有效候选工厂）

`BoundaryCandidateDTO.valid(...)` 已存在。它要求 entry（入场）、stop（止损）、takeProfitLevels（止盈层级）、sourceFields（来源字段）、dataQualityScore（数据质量分）等输入都齐套，然后设置 `VALID`（有效候选状态），同时仍保持：

- `manualReviewRequired = true`，表示必须人工复核。
- `notTradeInstruction = true`，表示不是交易指令。
- `blockingReasons` 为空。

这个 factory（工厂方法）存在，只能说明 DTO（数据对象）可以表达“有效候选状态”。它不能被 P172 解释成生产 `VALID`（有效候选状态）已经授权，也不能被未来展示层直接当成可执行交易。

### 3. service / impl

当前已有：

- `src/main/java/org/example/trademodel/service/BoundaryCandidateService.java`
- `src/main/java/org/example/trademodel/service/impl/BoundaryCandidateServiceImpl.java`

`BoundaryCandidateServiceImpl`（边界候选交易计划服务实现）会检查以下缺失或不安全情况：

- symbol（交易标的）或 timeframe（周期）缺失。
- entry（入场）/ stop（止损）/ takeProfitLevels（止盈层级）/ sourceFields（来源字段）/ dataQualityScore（数据质量分）缺失。
- SourceTrace（证据来源追踪）缺失、fallbackStatus（失败关闭状态）存在、missingFields（缺失字段）存在。
- entry（入场）、stop（止损）、TP（止盈）、RR（盈亏比）的来源字段缺失。
- Risk Action Guard（风险动作保护器）发现 stampede（踩踏风险）、wick-only risk（仅插针风险）、交易动作标记或流动性缺失。

当存在 blockingReasons（禁止推进原因）时，service（服务）会返回 `INCOMPLETE`（证据不完整）或 `WATCH_ONLY`（仅观察），并保持 manualReviewRequired（必须人工复核）和 notTradeInstruction（不是交易指令）。

当所有输入都齐套时，当前 service（服务）可以调用 `BoundaryCandidateDTO.valid(...)` 生成 `VALID`（有效候选状态）。但这仍然只能说明“已有服务逻辑和单元测试覆盖完整输入场景”，不能说明生产路径已经授权生成真实候选。P171 总索引已经明确：生产 `VALID`（有效候选状态）和真实点位仍未完成。

### 4. read-only candidate 保护语义

当前已有 MarketReadOnly（市场只读）候选线：

- `MarketReadOnlyEvidenceSnapshotDTO`（市场只读证据快照）。
- `MarketReadOnlyCandidateResultDTO`（市场只读候选结果）。
- `MarketReadOnlyCandidateStatusEnum`（市场只读候选状态枚举）。
- `MarketReadOnlyCandidateGenerator`（市场只读候选生成器接口）。
- `InertMarketReadOnlyCandidateGenerator`（惰性市场只读候选生成器）。

`MarketReadOnlyCandidateStatusEnum`（市场只读候选状态枚举）包含：

- `INCOMPLETE`（证据不完整）。
- `BLOCKED`（禁止推进）。
- `REVIEW_ONLY_CANDIDATE`（只允许复核的候选）。

这些 read-only（只读）对象没有开放生产 Controller（控制器）、生产 endpoint（接口入口）或真实交易执行面。它们更像是审计材料和保护壳。

### 5. 测试证据

只读扫描确认已有测试覆盖缺证据和不安全输入不能推进：

- `MarketReadOnlyMissingEvidenceFailClosedTest` 证明缺少证据、来源归属、规则、新鲜度、数据质量等字段时保持 `INCOMPLETE`（证据不完整），不会变成 `REVIEW_ONLY_CANDIDATE`（只允许复核的候选），更不会变成 `VALID`（有效候选状态）。
- `MarketReadOnlyForbiddenInputBlockedTest` 证明 forbidden input（禁止输入）、no-go evidence（禁止推进证据）、Risk Action Guard（风险动作保护器）阻断时返回 `BLOCKED`（禁止推进）。
- `MarketReadOnlyNoRuntimeNoProductionValidGuardTest` 证明 MarketReadOnly（市场只读）线不允许出现 runtime（运行时）、live（实时）、external（外部）读取，也不允许出现 `BoundaryCandidateDTO.valid(...)` 或生产 `VALID`（有效候选状态）。
- `BoundaryCandidateServiceImplTest` 证明 service（服务）在 SourceTrace（证据来源追踪）缺失、边界来源缺失、Risk Action Guard（风险动作保护器）不安全时返回 `INCOMPLETE`（证据不完整）或 `WATCH_ONLY`（仅观察），并保持 manualReviewRequired（必须人工复核）和 notTradeInstruction（不是交易指令）。

这些测试说明保护语义已经存在，但它们不等于生产候选生成完成，也不等于 Dashboard（首页工作台）可以展示可执行交易。

## 四、当前 Dashboard / display 现状

只读扫描确认，Dashboard（首页工作台）detail（详情）路径已经存在，并且比 summary（汇总）路径更适合未来只读候选展示。

### 1. DashboardController（首页控制器）

`src/main/java/org/example/trademodel/controller/DashboardController.java` 当前已有：

- `/dashboard` 页面。
- `/api/dashboard/summary` 汇总接口。
- `/api/dashboard/detail` 详情接口。

`/api/dashboard/detail` 返回 `DashboardDetailResponseVO`（首页详情响应对象）。它是单标的详情路径，影响面小，适合人工复核。P172 不允许改 Controller（控制器），也不建议未来 P174 默认修改 Controller（控制器）。

### 2. DashboardDetailResponseVO（首页详情响应对象）

`DashboardDetailResponseVO` 当前已有这些承载对象：

- `PlanBoundaryDisplayVO`（计划边界展示对象）。
- `ExecutionPlanDisplayVO`（执行计划展示对象）。
- `RiskActionGuardDisplayVO`（风险动作保护展示对象）。
- `PaperObservationDisplayVO`（纸面观察展示对象）。
- `SourceTraceDTO`（证据来源追踪数据对象）。
- `RuntimeKlineContextDTO`（运行时 K 线上下文数据对象）。
- `DerivativesRiskContextDTO`（衍生品风险上下文数据对象）。

其中 `PlanBoundaryDisplayVO` 已有：

- `planBoundaryStatus`（计划边界状态）。
- `sourceTraceStatus`（证据来源追踪状态）。
- `backendConnectionStatus`（后端接线状态）。
- `incompleteReasons`（证据不完整原因）。
- `blockingReasons`（禁止推进原因）。
- `manualReviewRequired`（必须人工复核）。
- `notTradeInstruction`（不是交易指令）。

这说明未来如果要做 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示），最自然的承载位置是 detail（详情）里的 PlanBoundaryDisplay（计划边界展示）区域，而不是 summary（汇总）或 action API（动作接口）。

### 3. PlanBoundary / ExecutionPlan display adapters

当前已有：

- `DefaultPlanBoundaryDisplayAdapter`（默认计划边界展示适配器）。
- `DefaultPlanBoundarySourceTraceAdapter`（默认计划边界证据来源追踪适配器）。
- `DefaultExecutionPlanDisplayAdapter`（默认执行计划展示适配器）。

`DefaultPlanBoundaryDisplayAdapter` 当前保持 fail-closed（失败关闭）：缺少 decision（决策结果）、analysisId（分析编号）、read model（只读模型）不完整或仅有文本执行计划字段时，展示为 `BACKEND_PENDING`（后端未接入）或 `INCOMPLETE`（证据不完整），并强制 manualReviewRequired（必须人工复核）和 notTradeInstruction（不是交易指令）。

`DefaultPlanBoundarySourceTraceAdapter` 当前会把 SourceTrace（证据来源追踪）输入不可用表达为 `INCOMPLETE`（证据不完整），并写入 `SOURCE_TRACE_INPUT_NOT_AVAILABLE`、`BOUNDARY_CANDIDATE_DTO_MISSING`、`RUNTIME_KLINE_CONTEXT_DTO_MISSING` 等原因。

`DefaultExecutionPlanDisplayAdapter` 当前最多把完整边界和 SourceTrace（证据来源追踪）映射为 `READY_REVIEW_ONLY`（只允许复核的就绪摘要），不是交易执行。它也会在 SourceTrace（证据来源追踪）缺失或失败关闭时保持 `INCOMPLETE`（证据不完整）或 `WATCH_ONLY`（仅观察）。

### 4. dashboard.html（首页页面）

`src/main/resources/templates/dashboard.html` 当前已有：

- SourceTrace（证据来源追踪）只读展示片段。
- PlanBoundary（计划边界）和 ExecutionPlan（执行计划）相关详情展示。
- 多处文案明确“不是交易指令”“需要人工复核”。

页面中也出现了提醒：`BoundaryCandidate VALID`（边界候选有效状态）不等于交易动作，`ExecutionPlan readiness`（执行计划可执行就绪）不自动执行。

这说明 Dashboard（首页工作台）已有展示位置，但 P172 不能直接改页面。未来最安全路径仍然是 detail read-only display（详情只读展示），不是 summary（汇总），不是 action API（动作接口），也不是 Dashboard（首页工作台）可执行状态。

## 五、是否允许未来进入只读候选展示

P172 的保守结论是：允许未来进入 P173，但 P172 本身不允许直接写代码。

可以允许未来 P173 做 BoundaryCandidate（边界候选交易计划）Read-Only Candidate（只读候选）Display（展示）Authorization Gate（授权门）。

P173 必须明确 P174 最多允许改哪些文件。P173 不能用“范围审计已通过”作为理由直接扩大到 Controller（控制器）、API（接口）、schema（数据库结构）、mapper（映射器）或生产候选生成。

如果未来 P174 实现，也只能做 read-only display（只读展示）：

- 只能展示 BoundaryCandidate（边界候选交易计划）的状态、缺失字段、禁止推进原因、人工复核模式。
- 不能生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- 不能生成生产 `VALID`（有效候选状态）。
- 不能调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- 不能升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- 不能新增买入、卖出、平仓、反手等交易动作按钮。
- 不能接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## 六、未来 P173/P174 最安全方向

P173/P174 的最安全方向如下：

1. 优先展示 BoundaryCandidate（边界候选交易计划）的状态、missing fields（缺失字段）、blocking reasons（禁止推进原因）、review mode（复核模式）。
2. 优先复用现有 detail display（详情展示）/ PlanBoundaryDisplay（计划边界展示）区域。
3. 不先改 summary（汇总）接口。
4. 不新增 action API（动作接口）。
5. 不新增交易按钮。
6. 如果现有 `PlanBoundaryDisplayVO`（计划边界展示对象）足够，P173 可以授权 P174 最小字段展示或最小 adapter（适配器）接线。
7. 如果现有对象不够，应先定义 read-only display DTO（只读展示数据对象）或 adapter（适配器），并把字段限制为状态、原因和人工复核语义。
8. 继续保持 `REVIEW_ONLY`（只允许复核）。
9. 继续保持 `INCOMPLETE`（证据不完整）/ `BLOCKED`（禁止推进）/ `WATCH_ONLY`（仅观察）这样的失败关闭语义。
10. 不映射生产 `VALID`（有效候选状态）。
11. 不映射 ExecutionPlan（执行计划）Readiness（可执行就绪）。
12. 不把真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）展示成可执行点位。

P174 的正确形态应接近“用户能看到为什么候选还不能推进”，而不是“用户能看到一组可下单点位”。

## 七、仍然禁止的路径

以下路径在 P172 之后仍然禁止，不能被 P173/P174 放开：

- production candidate generation（生产候选交易计划生成）。
- source-owned runtime candidate generation（运行时证据来源候选生成）。
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）。
- production `VALID` mapping（生产环境映射为有效候选）。
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid 工厂）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）upgrade（升级）。
- Dashboard（首页工作台）readiness mutation（页面显示可执行状态）。
- Dashboard（首页工作台）trading action buttons（页面交易动作按钮）。
- Controller（控制器）/ endpoint（接口入口）/ API（接口）action wiring（动作接线）。
- schema（数据库结构）/ config（配置）/ mapper（映射器）changes（改动）。
- runtime data reads（读取运行时数据）。
- live market data reads（读取实时行情）。
- external data integration（接外部数据）。
- WebClient / RestTemplate（网络请求工具）。
- order API（下单接口）。
- execution API（执行接口）。
- scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

这些禁线继续保留，是因为 BoundaryCandidate（边界候选交易计划）当前还没有完成真实来源闭环。只读展示可以帮助人工复核，但不能替代来源闭环、数值归属、风险动作保护和执行计划就绪审计。

## 八、推荐下一步

推荐下一步为：

P173：BoundaryCandidate Read-Only Candidate Display Authorization Gate（边界候选只读候选展示授权门）。

P173 仍然不写代码。P173 只定义 P174 最小只读候选展示允许改哪些文件、允许展示哪些字段、禁止哪些语义。

P173 必须继续禁止：

- 生产 `VALID`（有效候选状态）。
- 真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- ExecutionPlan（执行计划）Readiness（可执行就绪）。
- Dashboard（首页工作台）交易按钮。
- action API（动作接口）。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。

## P172 边界确认

- P172 只新增本审计文档。
- P172 删除 `docs/P172.md` placeholder（占位文档）。
- P172 不新增 Java。
- P172 不新增测试。
- P172 不修改 production Java（生产 Java）。
- P172 不修改现有测试。
- P172 不修改 `dashboard.html`。
- P172 不新增 Controller（控制器）/ endpoint（接口入口）/ API（接口）/ schema（数据库结构）/ config（配置）/ service（服务）/ mapper（映射器）。
- P172 不读取 runtime（运行时）/ live（实时）/ external（外部）数据。
- P172 不生成真实 entry（入场）/ stop（止损）/ TP（止盈）/ RR（盈亏比）。
- P172 不调用 `BoundaryCandidateDTO.valid(...)` 进入生产路径。
- P172 不升级 ExecutionPlan（执行计划）Readiness（可执行就绪）。
- P172 不接 order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）。
