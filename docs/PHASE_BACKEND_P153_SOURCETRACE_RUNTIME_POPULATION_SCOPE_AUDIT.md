# BACKEND-P153 SourceTrace Runtime Population Scope Audit

## 1. 这一步是干嘛的

P153 是 SourceTrace Runtime Population Scope Audit（运行时证据来源填充范围审计）。

这一步只检查现有 DTO（数据传输对象）、测试、文档是否能支撑未来第一根最小 SourceTrace runtime population（运行时证据来源填充）。

P153 不做实现：

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

P153 的问题很窄：如果未来只补 SourceTrace（证据来源追踪）的 Runtime（系统运行时）Population（填充），现有对象和测试是否已经足够作为下一步授权依据。

## 2. 允许检查的范围

P153 只审计 P152 允许的 read-only audit（只读审计）范围。

本轮允许查看的路径：

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

本轮允许只读搜索的对象和测试：

- `SourceTraceDTO`
- `MarketReadOnlyEvidenceSnapshotDTO`
- `MarketReadOnlyCandidateResultDTO`
- `SourceOwnedCandidateIncompleteGuardTest`
- `SourceOwnedCandidateBlockedGuardTest`
- `BoundaryCandidateFixtureAssemblerHelper`
- `EntrySourceOwnedCandidateFixtureHelper`
- `StopTpRrSourceOwnedCandidateFixtureHelper`

P153 没有读取 Runtime（系统运行时）数据、live market data（实时行情）或 external data（外部数据）。

## 3. 已经能支撑什么

P153 审计确认：现有 DTO（数据传输对象）和 fixture（测试夹具）已经能表达一部分 SourceTrace（证据来源追踪）所需信息。

现有对象已经能表达：

- source owner（证据来源所有者）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 `sourceOwner`。
  - `BoundaryCandidateFixtureAssemblerHelper` 的 review field / summary 能表达 entry / stop / TP / RR 的 source owner。
- source ref（证据来源引用）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 `sourceRef`。
  - `SourceTraceDTO` 有 entry / stop / TP source ref 字段。
  - fixture helper 能表达 source ref 或 numeric source ref。
- source timeframe（证据来源周期）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 `sourceTimeframe`。
  - `SourceTraceDTO` 和 `RuntimeKlineContextDTO` 有 entry / stop / TP source timeframe 字段。
- source window（证据来源窗口）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 `sourceWindow`。
  - P146 / P149 测试已经覆盖 stale source window（过期证据窗口）相关 fail-closed 行为。
- freshness（新鲜度）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 `freshnessStatus`。
  - `MarketReadOnlyCandidateResultDTO` 能带出 freshness status。
  - `SourceTraceDTO` 和 `RuntimeKlineContextDTO` 能表达 quote / OHLCV freshness 或 stale reason 信息。
- missing fields（缺失字段）
  - `SourceTraceDTO` 有 `missingFields`。
  - `MarketReadOnlyEvidenceSnapshotDTO` 会解析缺失 source owner / source ref / source timeframe / source window / rule / freshness / evidence status。
  - `MarketReadOnlyCandidateResultDTO` 会把缺失字段转成 `snapshot_missing:*` blocking reason。
- blocking reasons（禁止推进原因）
  - `MarketReadOnlyEvidenceSnapshotDTO` 有 blocker evidence。
  - `MarketReadOnlyCandidateResultDTO` 有 `blockingReasons`。
  - `SourceOwnedCandidateBlockedGuardTest` 已证明冲突、不安全、禁止推进证据会保持 BLOCKED（禁止推进状态）。
- review-only（只允许复核）
  - `MarketReadOnlyEvidenceSnapshotDTO` 固定 `reviewMode=REVIEW_ONLY`。
  - `MarketReadOnlyCandidateResultDTO` 固定 `reviewMode=REVIEW_ONLY`。
  - `BoundaryCandidateFixtureAssemblerHelper` 输出 `REVIEW_ONLY`。
- `manualReviewRequired=true`（必须人工复核）
  - `SourceTraceDTO` 默认 true。
  - `RuntimeKlineContextDTO` 默认 true。
  - `MarketReadOnlyEvidenceSnapshotDTO` 固定 true。
  - `MarketReadOnlyCandidateResultDTO` 固定 true。
  - P146 / P149 测试都断言这个安全姿态。
- `notTradeInstruction=true`（不是交易指令）
  - `SourceTraceDTO` 默认 true。
  - `RuntimeKlineContextDTO` 默认 true。
  - `MarketReadOnlyEvidenceSnapshotDTO` 固定 true。
  - `MarketReadOnlyCandidateResultDTO` 固定 true。
  - P146 / P149 测试都断言不会产生 trade instruction（交易指令）。

现有测试已经支撑两类 fail-closed（失败时保持关闭）判断：

- `SourceOwnedCandidateIncompleteGuardTest` 证明缺少证据来源字段时输出 INCOMPLETE（证据不完整状态），不会推进到 REVIEW_ONLY_CANDIDATE、VALID（有效候选状态）或可执行状态。
- `SourceOwnedCandidateBlockedGuardTest` 证明证据冲突、不安全替代、显式 BLOCKED（禁止推进状态）时不会推进到 REVIEW_ONLY_CANDIDATE、VALID（有效候选状态）或可执行状态。

这说明：未来如果只补 SourceTrace runtime population（运行时证据来源填充），已有 DTO（数据传输对象）和测试可以作为安全边界参考。

## 4. 仍然缺什么

P153 审计确认：现在仍然没有真实 Runtime（系统运行时）Population（填充）逻辑。

仍然缺的内容如下：

- 还没有真实运行时填充 SourceTrace（证据来源追踪）的生产逻辑。
- 还没有定义哪个生产 service（服务）负责填 SourceTrace（证据来源追踪）。
- 还没有定义 source owner（证据来源所有者）从哪个真实对象来。
- 还没有定义 source ref（证据来源引用）从哪个真实对象来。
- 还没有定义 source timeframe（证据来源周期）从哪个真实对象来。
- 还没有定义 source window（证据来源窗口）从哪个真实对象来。
- 还没有定义 Runtime SourceTrace（运行时证据来源追踪）和 Candidate（候选交易计划）之间怎么连接。
- 还没有定义缺证据时如何从真实运行链路输出 INCOMPLETE（证据不完整状态）。
- 还没有定义冲突 / 不安全时如何从真实运行链路输出 BLOCKED（禁止推进状态）。
- 还没有定义未来第一根代码实现允许改哪 1-3 个文件。
- 还不能生成 VALID（有效候选状态）。
- 还不能生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 还不能升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 还不能打开 dashboard readiness（页面可执行状态）。

换句话说：现有对象可以表达审计字段，但还没有真实 Runtime（系统运行时）把这些字段填进去。

## 5. 是否允许下一步写代码

P153 的保守结论是：不允许直接写代码。

P153 之后仍不建议直接 production candidate generation（生产候选交易计划生成）。

如果继续，下一步应该是：

```text
P154: SourceTrace Runtime Population Authorization Gate
```

中文解释：P154 是 SourceTrace Runtime Population Authorization Gate（运行时证据来源填充授权门）。

P154 仍然应该只定义未来第一根最小代码实现允许改哪些文件，不直接写代码。

真正代码实现必须再下一步单独授权。P153 不把审计结论自动变成实现许可。

## 6. 推荐的未来第一根最小代码方向

P153 建议未来第一根最小代码实现只围绕：

```text
SourceTrace runtime population
```

中文解释：SourceTrace runtime population（运行时证据来源填充）。

未来实现如果被单独授权，也必须限制为：

- 只补 SourceTrace（证据来源追踪）。
- 只输出 REVIEW_ONLY（只允许复核）。
- 不生成 VALID（有效候选状态）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不升级 Readiness（是否允许进入下一步）。
- 不改 `dashboard.html`。
- 不接 controller / endpoint / API（控制器 / 接口 / API）。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

未来第一根最小代码实现的合理目标应该只是：

- 把已有 source owner（证据来源所有者）映射进 SourceTrace（证据来源追踪）。
- 把已有 source ref（证据来源引用）映射进 SourceTrace（证据来源追踪）。
- 把已有 timeframe（证据来源周期）映射进 SourceTrace（证据来源追踪）。
- 把已有 source window（证据来源窗口）映射进 SourceTrace（证据来源追踪）。
- 把 freshness（新鲜度）和 missing fields（缺失字段）保持为审计信息。
- 在缺证据时继续 fail closed（失败时保持关闭）为 INCOMPLETE（证据不完整状态）。
- 在冲突 / 不安全时继续 fail closed（失败时保持关闭）为 BLOCKED（禁止推进状态）。

这些目标仍然只是 review-only（只允许复核）证据追踪，不是交易计划生成。

## 7. 仍然禁止的路径

以下路径在 P153 之后仍然禁止：

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

这些路径不能因为 P153 完成审计而被间接授权。

## 8. P153 结论

P153 只完成审计文档。

P153 不授权写代码。

P153 不授权生成真实交易点位。

P153 不授权 VALID（有效候选状态）。

P153 不授权 Readiness（是否允许进入下一步）升级。

P153 不授权 dashboard readiness（页面可执行状态）。

P153 不授权 auto-trading（自动交易）。

P153 推荐下一步 P154 做授权门，而不是直接实现。

P153 的审计结论是：

- 现有 DTO（数据传输对象）已经能表达 source owner / source ref / source timeframe / source window / freshness / missing fields / blocking reasons / review-only 安全姿态。
- 现有 P146 / P149 测试已经证明 INCOMPLETE（证据不完整状态）和 BLOCKED（禁止推进状态）不会被误推进。
- 但是现有代码还没有真实 Runtime（系统运行时）SourceTrace（证据来源追踪）Population（填充）。
- 因此下一步只能先做 P154 authorization gate（授权门），继续定义未来第一根最小代码实现的文件边界。

## 9. P153 边界确认

P153 本轮只完成一个审计文档：

- 新增 `docs/PHASE_BACKEND_P153_SOURCETRACE_RUNTIME_POPULATION_SCOPE_AUDIT.md`。
- 删除 `docs/P153.md`。

P153 本轮确认：

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

P153 stops here. P153 不合并 PR，不进入 Production Wiring（真正接入系统运行链路）。
