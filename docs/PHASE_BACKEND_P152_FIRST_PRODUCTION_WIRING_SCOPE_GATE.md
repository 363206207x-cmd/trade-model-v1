# BACKEND-P152 First Production Wiring Scope Gate

## 1. 这一步是干嘛的

P152 是 First Production Wiring Scope Gate（第一根真实接线范围门）。

它只定义下一步如果开始靠近 Production Wiring（真正接入系统运行链路），应该从哪里开始、先审计什么、仍然禁止什么。

P152 本轮不是实现任务：

- 不写 Java。
- 不新增测试。
- 不接 Production Wiring（真正接入系统运行链路）。
- 不读取 Runtime（系统运行时）数据。
- 不读取实时行情。
- 不接外部数据。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 Candidate（候选交易计划）。
- 不生成 VALID（有效候选状态）。
- 不升级 Readiness（是否允许进入下一步）。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

P152 的唯一作用是把后续工作继续缩小，避免从文档和测试线直接跳到完整真实运行链路。

## 2. 为什么现在还不能直接接完整链路

P151 已经确认：P140-P150 只证明了两类基础安全状态不会被误推进。

- INCOMPLETE（证据不完整状态）不会被误推进。
- BLOCKED（禁止推进状态）不会被误推进。

这很重要，但还不等于可以直接进入完整 Production Wiring（真正接入系统运行链路）。

现在仍然缺以下定义：

- 真实 Runtime（系统运行时）里的 SourceTrace（证据来源追踪）怎么填。
- 谁提供 source owner（证据来源所有者）。
- 谁提供 source ref（证据来源引用）。
- 谁提供 timeframe（证据来源周期）。
- 谁提供 window（证据来源窗口）。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）的真实证据来源从哪里来。
- 哪个 service（服务）负责第一根真实接线。
- 第一根真实接线如何保持 REVIEW_ONLY（只允许复核）。
- 第一根真实接线如何继续让缺证据输出 INCOMPLETE（证据不完整状态）。
- 第一根真实接线如何继续让冲突或不安全证据输出 BLOCKED（禁止推进状态）。

因此现在不能直接做这些事：

- 不能直接生成 VALID（有效候选状态）。
- 不能直接生成真实交易点位。
- 不能直接生成真实 Candidate（候选交易计划）。
- 不能直接打开 dashboard readiness（页面可执行就绪状态）。
- 不能直接升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不能接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

## 3. 第一根真实接线建议从哪里开始

P152 建议第一根真实接线只围绕：

```text
SourceTrace runtime population
```

中文解释：SourceTrace runtime population（运行时证据来源填充）。

意思是：先让系统在 Runtime（系统运行时）能把“这个结论来自哪里”填出来。

这个建议只补 SourceTrace（证据来源追踪），不生成交易点位。

第一根真实接线即使未来被单独授权，也必须保持这些限制：

- 只允许输出 REVIEW_ONLY（只允许复核）。
- 仍然不允许 VALID（有效候选状态）。
- 仍然不允许 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实生成。
- 仍然不允许 Readiness（是否允许进入下一步）升级。
- 仍然不允许 dashboard readiness（页面可执行就绪状态）打开。
- 仍然不允许 order / execution / auto-trading（下单 / 执行 / 自动交易）。

第一根真实接线的目标不是让系统“可以交易”，而是让系统先能说明：

- 这个 review-only（只允许复核）结论来自哪个 source owner（证据来源所有者）。
- 这个结论引用了哪个 source ref（证据来源引用）。
- 这个结论属于哪个 timeframe（证据来源周期）。
- 这个结论覆盖哪个 source window（证据来源窗口）。
- 这个结论的新鲜度 freshness（新鲜度）是什么。
- 这个结论为什么仍然只能 REVIEW_ONLY（只允许复核）。

## 4. 下一步允许读取哪些文件

P152 只为未来 P153 定义 read-only audit（只读审计）范围。

未来 P153 可以只读审计以下路径：

- `src/main/java/org/example/trademodel/dto/planboundary/`
- `src/test/java/org/example/trademodel/dto/planboundary/`
- `docs/PHASE_BACKEND_P140*`
- `docs/PHASE_BACKEND_P141*`
- `docs/PHASE_BACKEND_P142*`
- `docs/PHASE_BACKEND_P143*`
- `docs/PHASE_BACKEND_P144*`
- `docs/PHASE_BACKEND_P145*`
- `docs/PHASE_BACKEND_P146*`
- `docs/PHASE_BACKEND_P147*`
- `docs/PHASE_BACKEND_P148*`
- `docs/PHASE_BACKEND_P149*`
- `docs/PHASE_BACKEND_P150*`
- `docs/PHASE_BACKEND_P151*`
- `docs/PHASE_BACKEND_P152*`

未来 P153 可以只读搜索以下对象或测试：

- `SourceTraceDTO`
- `MarketReadOnlyEvidenceSnapshotDTO`
- `MarketReadOnlyCandidateResultDTO`
- `SourceOwnedCandidateIncompleteGuardTest`
- `SourceOwnedCandidateBlockedGuardTest`
- `BoundaryCandidateFixtureAssemblerHelper`
- `EntrySourceOwnedCandidateFixtureHelper`
- `StopTpRrSourceOwnedCandidateFixtureHelper`

P152 不授权 P153 读取 Runtime（系统运行时）数据、live market data（实时行情）或 external data（外部数据）。

## 5. 下一步是否允许改文件

P152 的保守结论是：P153 仍然建议只做文档授权或只读审计，不直接改 Java。

P153 可以回答这些问题：

- 现有 DTO 是否已经足够描述 SourceTrace（证据来源追踪）。
- 现有测试是否已经能证明缺证据和禁止推进时 fail closed（失败时保持关闭）。
- 如果未来要做第一根最小代码实现，应该只允许改哪 1-3 个文件。
- 未来代码实现是否必须先增加一个更窄的授权门。

如果未来真的进入第一根最小代码实现，必须另开新步骤授权。不能在 P153 默认改代码。

## 6. P152 明确禁止的东西

以下路径在 P152 之后仍然禁止：

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

这些路径不能因为 P152 存在而被间接授权。

## 7. 推荐下一步

推荐下一步是：

```text
P153: SourceTrace Runtime Population Scope Audit
```

中文解释：P153 是 SourceTrace Runtime Population Scope Audit（运行时证据来源填充范围审计）。

P153 应该只看哪些现有 DTO / 测试 / 文档已经能支撑 SourceTrace runtime population（运行时证据来源填充）。

P153 仍然不写生产代码。

P153 仍然不新增运行时接线。

P153 仍然不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P153 仍然不生成 VALID（有效候选状态）。

P153 仍然不升级 Readiness（是否允许进入下一步）。

P153 仍然不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P153 的目标是决定：未来第一根最小代码实现到底允许改哪 1-3 个文件，以及这些文件如何保持 REVIEW_ONLY（只允许复核）。

## 8. P152 结论

P152 不授权直接写代码。

P152 不授权直接生产 Candidate（候选交易计划）。

P152 不授权直接接 Production Wiring（真正接入系统运行链路）。

P152 不授权真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）生成。

P152 不授权 VALID（有效候选状态）。

P152 不授权 ExecutionPlan readiness（执行计划是否允许进入下一步）升级。

P152 不授权 dashboard readiness（页面可执行就绪状态）打开。

P152 只建议后续把第一根真实接线缩小到 SourceTrace runtime population（运行时证据来源填充）。

后续必须继续一小步一小步做：

- 先审计。
- 再授权。
- 再实现。
- 每一步都继续保留 REVIEW_ONLY（只允许复核）、INCOMPLETE（证据不完整状态）、BLOCKED（禁止推进状态）的安全边界。

## 9. P152 边界确认

P152 本轮只完成一个范围门文档：

- 新增 `docs/PHASE_BACKEND_P152_FIRST_PRODUCTION_WIRING_SCOPE_GATE.md`。
- 删除 `docs/P152.md`。

P152 本轮确认：

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

P152 stops here. P152 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
