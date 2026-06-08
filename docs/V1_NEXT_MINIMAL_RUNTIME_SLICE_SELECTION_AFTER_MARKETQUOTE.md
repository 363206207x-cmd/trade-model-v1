# V1 Next Minimal Runtime Slice Selection After MarketQuote Closure

# 1. Executive Summary

当前已完成三个只读 runtime 小闭环：

- PositionSync + Dashboard review-only status；
- Watchlist + RuleConfig + Dashboard/API review-only status；
- MarketQuote freshness / fallback / dashboard API status。

下一条最小 runtime slice 推荐：Evidence / Score review-only runtime status。

理由：Evidence / Score 是 MarketQuote 之后最自然的用户可见链路。当前源码已存在 `EvidenceController`、`ScoreController`、`EvidenceService`、`ScoreService`、`EvidenceItemMapper`、`ScoreItemMapper`、`EvidenceBriefVO`、`ScoreBriefVO`，并且 dashboard detail 已有 `evidenceTopItems` / `scoreTopItems` 输出路径。它可以把“行情状态已可见”之后的“证据/评分是否存在、是否只读、是否可解释、是否安全”做成最小可验证状态，而不需要接 Push、Candidate、Point 或 Trading。

它是最大安全推进，因为下一步只允许做 source read，先确认 owner path、API/dashboard surface、数据缺失/降级状态和重复 skeleton 风险，再决定是否进入 design。不能直接实现。

为什么不是 Push：Push 外部通道仍是高风险扩张，当前只能保持内部/只读边界，不能发送。

为什么不是 Three AI：Three AI 不属于当前最小 runtime 闭环，且会引入 provider、预算、fallback 和裁决链路风险。

为什么不是点位：entry / stop / TP / RR、final direction、point generation 仍明确冻结，不能作为下一步。

为什么不是 Position Monitor expansion：Position Monitor 相关基础存在，但 expansion 容易滑向 close / reverse / execution-adjacent 行为；当前更适合等待上游 Evidence / Score / Decision 只读状态更清楚后再读。

为什么不是 P359/P360：P359 分支存在但未合并，PR #829 已关闭未合并；P360 不允许启动。二者继续冻结。

下一步具体做：Source Read for Evidence / Score Review-Only Runtime Status。该下一步必须保持 source-read only，不实现功能。

# 2. Current Completed Runtime Slices

- PositionSync + Dashboard review-only status: REVIEW_ONLY_RUNTIME partial
- Watchlist + RuleConfig + Dashboard/API review-only status: REVIEW_ONLY_RUNTIME partial
- MarketQuote freshness / fallback / dashboard API status: REVIEW_ONLY_RUNTIME partial

以上三个 slice 都是 review-only；都不是 Production Wiring；都不是交易能力；都不允许自动生成候选、点位、方向、Push 或订单执行。

# 3. Candidate Slice Comparison

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| Evidence / Score review-only runtime status | `EvidenceController`, `ScoreController`, `EvidenceService`, `ScoreService`, `EvidenceItemMapper`, `ScoreItemMapper`, `EvidenceBriefVO`, `ScoreBriefVO`, dashboard detail `evidenceTopItems` / `scoreTopItems`, service and mapper tests. | 高。用户能看到证据和评分是否存在、是否为空、是否只读、是否安全，承接 MarketQuote 之后的解释层。 | 中。已有 controller/service/mapper/dashboard detail path，但 status mapping、空数据降级、source/readiness 边界仍需 source read。 | 中。Score 容易被误读为方向或候选强度，必须加 review-only / not trading signal 边界。 | 中高。P295-P298 已有 review-only skeleton，需要确认 canonical owner，避免新 wrapper。 | 推荐。下一步做 source read only。 |
| DecisionResult review-only dashboard/API status | `DecisionService`, `DecisionResultVO`, mapper/schema、dashboard detail decision fields 已存在。 | 中高。可显示决策 read-model 状态。 | 中。已有 read-model owner 结论，但容易与 Evidence/Score/ExecutionPlan 混合。 | 中高。DecisionResult 更接近结论，容易被误解为 final direction。 | 中。需先明确 Evidence/Score 输入状态再推进。 | 暂缓，等 Evidence / Score 状态 owner 更清楚。 |
| ExecutionPlan / BoundaryCandidate review-only display continuation | `BoundaryCandidateService` / DTO、`PlanService` / `ExecutionPlanVO/DO/Mapper`、display adapters、owner-path tests 已完成。 | 中。可以继续改善展示，但不是新的 runtime slice。 | 高，但 #842-#848 已安全收口。 | 中高。容易回到 point / execution display expansion。 | 中。继续推进会重复刚收口的 owner-path工作。 | 暂不选。保持冻结和已有测试锁。 |
| Internal Push preview status only | 旧 Candidate / Push review-only链和 dashboard internal preview display 有资产。 | 中。能显示内部预览状态。 | 中。已有历史资产但外部通道禁止。 | 高。容易滑向 Push send / external channel。 | 中。与 P302-P305 旧链重复。 | 暂不选。禁止外部 Push。 |
| Position Monitor manual-input source read | PositionSync / monitor 基础、manual position 概念和历史文档存在。 | 中。用户可见，但容易涉及持仓动作。 | 中低。需要更细 source read。 | 高。可能触发 close / reverse / execution-adjacent 误解。 | 中。应等上游 review-only状态更稳。 | 暂不选。 |
| Data Source Health dashboard status | MarketQuote source health、source trace、dashboard 状态已有部分资产。 | 中。可解释数据源健康。 | 中。刚完成 MarketQuote status，可能复用。 | 低中。主要是状态显示。 | 中。与 MarketQuote source health 重叠，短期收益较小。 | 暂缓，避免刚完成 MarketQuote 后重复。 |
| Review / Replay result status | Review/replay/log 概念、`replayFromLogId` 等字段存在。 | 中。可见复盘结果，但更偏反馈闭环。 | 低中。需要源读确认。 | 中。可能关联 recheck / push chain。 | 中。不是 MarketQuote 后最自然的下一跳。 | 暂不选。 |

# 4. Recommended Next Slice

推荐下一条 slice：Evidence / Score review-only runtime status。

owner path 候选：

```text
tm_evidence_item / tm_score_item
→ EvidenceItemMapper / ScoreItemMapper
→ EvidenceService / ScoreService
→ EvidenceController / ScoreController and dashboard detail evidenceTopItems / scoreTopItems
→ future minimal review-only Evidence / Score runtime status
```

现有 API / dashboard 可能复用：`EvidenceController`、`ScoreController`、dashboard detail response 中的 `evidenceTopItems` / `scoreTopItems` 已经提供入口，但下一步 source read 必须确认 endpoint 方法、返回字段、空数据行为、dashboard DOM / detail 位置、测试覆盖，以及是否已有 status/copy 可以复用。

是否能形成 review-only runtime 小闭环：可以作为候选，但必须先 source read。最小目标不是计算新分数，也不是生成 Candidate，而是显示 Evidence / Score 数据是否存在、是否为空、是否 stale/unknown（如有字段）、是否只读、是否不能当交易信号。

是否不需要新 DTO / Validator / Assembler：当前推荐方向是复用现有 VO / service / mapper / dashboard detail；下一步 source read 必须验证是否可以不新增 DTO / Validator / Assembler。

是否不接 Push / Candidate / Point / Trading：是。Evidence / Score status 只能是 review-only availability/status，不允许接 Candidate、Push、Point、Decision final direction、entry / stop / TP / RR 或 order/execution。

为什么这一步是 MarketQuote 之后的自然后续：MarketQuote 已经让行情 source / freshness / fallback 可见；Evidence / Score 是对行情与分析材料的下一层解释状态。先把 Evidence / Score 的只读状态和安全边界显示清楚，可以减少后续 DecisionResult / ExecutionPlan 被误读为交易结论的风险。

# 5. Rejected Options

- Push external channel：暂不选。外部发送仍未授权，任何 Push send / external channel 都会越界。
- Three AI：暂不选。它不是当前最小 runtime slice，且会引入 provider orchestration、fallback、预算、缓存和最终裁决风险。
- point generation：暂不选。entry / stop / TP / RR、RR、final direction 与点位生成仍冻结。
- ExecutionPlan / BoundaryCandidate continuation：暂不选。#842-#848 已完成 owner/source-read/design/test/readiness 收口，继续推进容易回到执行建议或点位展示扩张。
- Position Monitor expansion：暂不选。它接近持仓动作建议，必须等只读上游状态更稳后再进行 manual-input source read。
- P359 / P360：继续冻结。P359 未合并且 #829 closed unmerged；P360 不允许启动。

# 6. Next Step Decision

Decision: A. GO: Source Read for selected next minimal runtime slice.

下一步命名：Source Read for Evidence / Score Review-Only Runtime Status。

下一步必须保持 source-read only，不实现；只能确认 Evidence / Score owner path、API/dashboard surface、status 字段、空数据/缺失/只读语义、测试资产和 duplicate skeleton 风险。

# 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes
