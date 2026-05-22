# P182 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）

## 一、这一步是干嘛的

P182 是 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）。

P182 是 P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）的第一步。

本轮只做审计文档，不写 Java，不新增测试，不接 Dashboard（首页工作台）新功能，不新增 Controller（控制器）、endpoint（接口端点）或 API（接口）。本轮只判断未来 Risk Action Guard（风险动作保护器）和 Position Monitor（持仓监控）是否可以进入 Read-Only Risk Display（只读风险展示）路径。

本轮不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。本轮不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。本轮不升级 Readiness（可执行就绪）。本轮不接 order（下单）、execution（执行）、scheduler（定时器）、automation（自动化）或 auto-trading（自动交易）。

## 二、P181 / PROJECT_PROGRESS_INDEX 的依据

`docs/PROJECT_PROGRESS_INDEX.md` 在 P181 已刷新，当前总索引给出的结论是：

- SourceTrace（证据来源追踪）read-only display（只读展示）已完成。
- BoundaryCandidate（边界候选交易计划）read-only candidate display（只读候选展示）已完成。
- ExecutionPlan（执行计划）review-only plan display（只允许复核展示）已完成。
- PROJECT_PROGRESS_INDEX（项目总进度索引）推荐个人可用最快路径进入 Risk Action Guard / Position Monitor（风险动作保护和持仓监控）只读风险审计。
- 同时，PROJECT_PROGRESS_INDEX 明确 Risk Action Guard（风险动作保护器）生产接线仍暂停，Position Monitor（持仓监控）的 Strong Reversal（强反转）/ Moving Stop（移动止损）仍暂停，auto-trading（自动交易）仍禁止。

因此，P182 只能审计 Read-Only Risk Display（只读风险展示）。P182 不能审计自动平仓、自动反手、自动买卖或真实风控动作执行。

## 三、必须纳入的 Risk Action Guard 核心规则

Risk Action Guard（风险动作保护器）不是交易动作执行器。它的第一职责是防止风险信息被误解释成交易指令。

- 风险高不能直接等同于立即止损、立即反手或立即开仓。
- 必须先判断 Liquidity（流动性）状态、是否存在 Stampede（踩踏）、是否只是 Wick-only Risk（仅插针风险）、是否有多周期确认。
- 风险高但流动性正常：只能提示可减仓、Moving Stop（移动止损）、降低杠杆，并且仍然是 REVIEW_ONLY（只允许复核）。
- 风险高且 Liquidity Deterioration（流动性恶化）：不建议市价一次性砍仓，优先分批降风险、等待流动性恢复、只降杠杆。
- 风险高且存在 Stampede（踩踏）：进入极端压力锁定，禁止反手，禁止新开仓，禁止机会推送，第一优先级是保护本金。
- 风险高但仅 Wick-only Risk（仅插针风险）：不直接判定趋势反转，不生成反向开仓计划，只做短线风险提醒和等待确认。
- Strong Reversal（强反转）不等于直接反手。
- 插针不等于趋势反转。
- Stampede（踩踏）状态禁止 opportunity push（机会推送）。

这些规则只能进入只读解释层，不能成为自动执行条件。

## 四、当前 Risk Action Guard 能力

只读扫描确认，当前已有 Risk Action Guard（风险动作保护器）相关展示能力：

- `src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java` 中已有 `RiskActionGuardDisplayVO`。
- `RiskActionGuardDisplayVO` 已有 `riskActionGuardStatus`、`riskActionGuardStatusLabel`、`riskActionAdvice`、`riskActionBlockingReason`、`liquidityState`、`stampedeDetected`、`wickOnlyRisk`、`opportunityPushAllowed`、`reverseTradeAllowed`、`newPositionAllowed`、`marketOrderExitAllowed`、`manualRiskReviewRequired`、`notTradeInstruction` 等字段。
- `src/main/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapter.java` 已存在，定位是 fail-closed（失败关闭）的 dashboard display adapter（首页展示适配器）。
- `src/test/java/org/example/trademodel/service/dashboard/DefaultRiskActionGuardDisplayAdapterTest.java` 已存在，覆盖输入缺失、PlanBoundary（计划边界）未 VALID（有效候选状态）、ExecutionPlan（执行计划）不是 READY_REVIEW_ONLY（只允许复核的就绪摘要）、高风险缺少流动性上下文、Stampede（踩踏）和 Wick-only Risk（仅插针风险）保留但动作继续禁止等场景。
- `DefaultRiskActionGuardDisplayAdapter` 会强制 `opportunityPushAllowed=false`、`reverseTradeAllowed=false`、`newPositionAllowed=false`、`marketOrderExitAllowed=false`、`manualRiskReviewRequired=true`、`notTradeInstruction=true`。
- `RiskActionGuardDisplayVO` 的 `liquidityState`、`stampedeDetected`、`wickOnlyRisk` 已能承载 Liquidity（流动性）、Stampede（踩踏）和 Wick-only Risk（仅插针风险）相关只读信息。

当前能力的边界也很明确：

- 这些能力只是 display（展示）、adapter（适配器）和 fail-closed（失败关闭）保护。
- 它们不等于生产风控动作。
- 它们不等于自动降杠杆、自动平仓、自动移动止损或自动反手。
- 它们不读取 runtime / live / external data（运行时 / 实时 / 外部数据）来生成动作。
- 它们不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

## 五、当前 Position Monitor 能力

只读扫描确认，当前已有 Position Monitor（持仓监控）相关基础能力，但主要是同步、展示、告警和记录，不是自动动作：

- `src/main/java/org/example/trademodel/service/PositionSyncService.java` 已存在，用于同步持仓快照。
- `src/main/java/org/example/trademodel/service/PositionSyncScheduler.java` 已存在，用于定时触发持仓同步。这里的 scheduler（定时器）是同步用途，不是交易动作触发器。
- `src/main/java/org/example/trademodel/position/PositionProvider.java`、`SimulatedPositionProvider.java`、`BinancePositionProvider.java`、`SwitchablePositionProvider.java` 已存在，用于提供持仓快照。P182 只读查看代码，没有调用 Binance（币安）或外部实时数据。
- `src/main/java/org/example/trademodel/controller/MonitorController.java` 已存在，但当前只提供监控状态读取语义，不是交易动作接口。
- `src/main/java/org/example/trademodel/service/MonitorService.java` 和 `src/main/java/org/example/trademodel/service/impl/MonitorServiceImpl.java` 已存在，用于读取 recent alerts（最近告警）。
- `src/main/java/org/example/trademodel/service/MonitorAlertWriteService.java` 和 `MonitorAlertWriteServiceImpl.java` 已存在，用于写入 `tm_monitor_alert`（监控告警表）。
- `src/main/java/org/example/trademodel/entity/MonitorAlertDO.java`、`src/main/java/org/example/trademodel/mapper/MonitorAlertMapper.java`、`src/main/java/org/example/trademodel/mapper/RealPositionMapper.java` 已存在。
- `src/main/resources/schema.sql` 中已有 `tm_real_position`（真实持仓表）和 `tm_monitor_alert`（监控告警表）。

需要避免误判：

- `RealPositionMapper.closeMissingOpenPositions(...)` 是把同步快照里消失的持仓在数据库中标为 CLOSED（已关闭），不是向交易所发送平仓订单。
- `PositionSyncScheduler` 是定时同步，不是 scheduler-triggered action（定时器触发交易动作）。
- `MonitorAlertWriteService` 写入告警，不是执行交易。
- 当前未发现 Strong Reversal（强反转）或 Moving Stop（移动止损）的真实生产动作逻辑。
- 强反转 / 移动止损仍未实现。

## 六、当前 Dashboard / display 现状

当前 Dashboard（首页工作台）已有风险和持仓相关只读展示基础：

- `DashboardDetailResponseVO` 已有 `riskActionGuardDisplay`、`executionPlanDisplay`、`planBoundaryDisplay`、`paperObservationDisplay`、`sourceTrace`、`runtimeKlineContext`、`derivativesRiskContext` 等字段。
- `dashboard.html` 已有 Risk Action Guard（风险动作保护器）占位和只读展示区域。
- `dashboard.html` 已展示 `opportunityPushAllowed`、`reverseTradeAllowed`、`newPositionAllowed`、`liquidityState`、`manualRiskReviewRequired`、`notTradeInstruction` 等只读语义。
- `dashboard.html` 已有 “非交易指令”“必须人工复核”“不连接 order API”“不触发自动交易动作”“高风险不等于直接止损或反手”“短线插针不等于趋势反转”等文案。
- `dashboard.html` 的主工作台和决策卡片已有“已开仓监控”区域，但它只描述已有持仓字段，不生成平仓、反手或新开仓建议。
- 当前最安全展示路径是 Dashboard Detail（首页详情）的 Read-Only Risk Display（只读风险展示），不是 summary（摘要），不是 action API（动作接口），也不是交易按钮。

如果未来页面出现“复查”“记录平仓”这类入口，也必须解释为人工复核或人工记录入口，不能解释为自动执行、自动平仓或自动反手。

## 七、是否允许未来进入只读风险展示

P182 不允许直接写代码。

可以允许未来 P183 做 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）。P183 必须明确 P184 最多允许改哪些文件。

如果 P184 实现，也只能做 read-only risk display（只读风险展示）：

- P184 不能创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- P184 不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P184 不能升级 ExecutionPlan Readiness（执行计划可执行就绪）。
- P184 不能新增交易动作按钮。
- P184 不能接 order API（下单接口）或 execution API（执行接口）。
- P184 不能接 scheduler（定时器）触发交易动作。
- P184 不能接 automation（自动化）或 auto-trading（自动交易）。

## 八、未来 P183/P184 最安全方向

P183/P184 的最安全方向应该是只读展示风险状态，而不是生成动作：

- 优先展示风险状态、风险原因、Liquidity（流动性）状态、Stampede（踩踏）状态、Wick-only Risk（仅插针风险）状态、人工复核模式。
- 优先展示持仓风险提醒、Strong Reversal（强反转）待确认、Moving Stop（移动止损）建议的只读说明。
- 优先复用现有 Dashboard Detail（首页详情）/ RiskActionGuardDisplay（风险动作保护展示）/ Position Monitor（持仓监控）区域。
- 不先改 summary（摘要）。
- 不新增 action API（动作接口）。
- 不新增交易按钮。
- 如果已有 display DTO（展示对象）可以承载字段，P183 可以授权 P184 做最小字段展示。
- 如果没有合适对象，应先定义 read-only risk display DTO（只读风险展示对象）或 adapter（适配器）。
- 保持 REVIEW_ONLY（只允许复核）/ INCOMPLETE（证据不完整）/ BLOCKED（禁止推进）/ WATCH_ONLY（仅观察）。
- 不把任何风险提醒映射成自动执行。
- 不把 Strong Reversal（强反转）映射成反手。
- 不把 Moving Stop（移动止损）映射成自动改止损。

## 九、仍然禁止的路径

以下路径仍然禁止，不能借 P182-P185 的名义提前进入：

- auto close position（自动平仓）
- auto reverse position（自动反手）
- auto buy / auto sell（自动买入 / 自动卖出）
- market order execution（市价执行）
- order API（下单接口）
- execution API（执行接口）
- scheduler-triggered action（定时器触发动作）
- risk high -> immediate stop loss（风险高直接止损）
- risk high -> reverse position（风险高直接反手）
- wick-only -> trend reversal（仅插针等于趋势反转）
- stampede -> opportunity push（踩踏状态推送机会）
- production candidate generation（生产候选交易计划生成）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard trading action buttons（页面交易动作按钮）
- controller / endpoint / API action wiring（控制器 / 接口动作接线）
- schema / config / mapper changes（数据库 / 配置 / 映射改动）
- runtime / live / external data reads（运行时 / 实时 / 外部数据读取）
- WebClient / RestTemplate（网络请求工具）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 十、推荐下一步

推荐下一步为：

P183：Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）

P183 仍然不写代码。P183 只定义 P184 最小 Read-Only Risk Display（只读风险展示）允许改哪些文件。

P183 必须继续禁止自动平仓、自动反手、自动买入、自动卖出、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、Readiness（可执行就绪）、交易按钮和 auto-trading（自动交易）。

P182 的结论是：风险保护和持仓监控可以进入下一步只读展示授权审计，但只能作为人工复核材料，不能作为交易动作入口。
