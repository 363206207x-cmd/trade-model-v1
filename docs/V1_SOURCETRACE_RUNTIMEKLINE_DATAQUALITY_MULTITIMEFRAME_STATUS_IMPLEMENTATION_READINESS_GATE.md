# V1 SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Status Implementation Readiness Gate

## 1. Executive Summary

本包只做 Implementation readiness gate（实现前就绪门），不做 Implementation（实现）。结论是：**GO to B-risk minimal implementation**。

允许进入下一步的原因：

- 现有 `/api/dashboard/detail` owner path（归属路径）已经存在，并且已经输出 `sourceTrace` 与 `runtimeKlineContext`。
- SourceTrace（来源追踪）和 RuntimeKline（运行时 K线）已有 dashboard detail adapter（仪表盘详情适配器）与 fail-closed（失败关闭）语义。
- Persisted OHLCV readiness（持久化 OHLCV 就绪状态）已有 DB read-only（数据库只读）查询路径。
- DataQuality（数据质量）与 MultiTimeframe（多周期）只能作为 partial metadata（部分元数据）展示，不能解释成交易折扣、方向判断、点位或候选。
- 下一步实现可以保持最小范围：优先复用 `/api/dashboard/detail`，如确需独立接口，最多新增一个 minimal read-only Map endpoint（最小只读 Map 接口）。

下一步实现仍必须是 REVIEW_ONLY_RUNTIME partial（只读运行时部分完成），不得提升为 Production Wiring（生产接线）。

## 2. Readiness Decision

| Question | Decision | Evidence / Reason |
|---|---|---|
| 是否允许进入 B-risk Implementation | GO | Source Read 与 Design 均确认现有 dashboard detail owner path 可复用。 |
| 是否必须复用 `/api/dashboard/detail` owner path | Yes, preferred | 这是现有 SourceTrace / RuntimeKline dashboard owner path。 |
| 是否允许 dedicated endpoint | Yes, only if minimal | 只允许一个 thin `Map<String,Object>` read-only endpoint，例如 `/api/dashboard/source-runtime-data-quality-status?symbol=BTCUSDT`。 |
| 是否允许 dashboard status panel | Yes, minimal only | 只允许 DOM/copy/status 展示，不允许新增 dashboard business logic。 |
| 是否允许新增 DTO / Validator / Assembler / Orchestrator | No | 冻结重复骨架，现有 owner path 已足够表达状态。 |
| 是否允许新增 source-binding family | No | Source-binding skeletons 是历史/冻结资产，不得复活为 owner path。 |
| 是否允许 schema/config/pom | No | 本 slice 不需要结构、配置或依赖变更。 |
| 是否允许 scheduler / collector / API client refresh | No | 状态只能读取现有数据，不能刷新或采集。 |
| 是否允许 external refresh | No | 任何外部刷新需求都是 NO-GO。 |
| 是否允许 Candidate / Decision generation / Point | No | 本 slice 只展示状态，不生成任何候选、决策或点位。 |
| 是否允许 final direction / entry / stop / TP / RR | No | 多周期与数据质量只能是诊断元数据，不是方向或点位来源。 |
| 是否允许 Push send / external channel | No | 不接推送发送或外部通道。 |
| 是否允许 order / execution / auto-trading | No | 不接订单、执行或自动交易。 |

## 3. Allowed Implementation Files

如果下一步 B-risk implementation（中风险实现）执行，最多允许修改：

| File | Allowed change |
|---|---|
| `src/main/java/org/example/trademodel/controller/DashboardController.java` | 仅当 `/api/dashboard/detail` 不足以表达紧凑状态时，新增一个 minimal read-only `Map` endpoint。不得新增 service/domain owner。 |
| `src/main/resources/templates/dashboard.html` | 新增最小 status panel、DOM id、review-only / fail-closed / not-refresh / not-generation safety copy。 |
| `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java` | 覆盖 endpoint safety flags、fail-closed states、refresh/generation boundary、forbidden executable fields absent、dashboard DOM/copy。 |
| Existing adapter tests | 仅允许极小断言现有 owner path；不得扩展业务语义。 |
| Implementation report docs | 记录实现范围、复用资产、测试和边界。 |
| Source-of-truth docs | 更新当前阶段、下一步、能力层级和禁止范围。 |

不得修改其他 Java ownership family、mapper/schema/config/pom，也不得新增 DTO / Validator / Assembler / Orchestrator。

## 4. Required Safety Fields

下一步实现若新增 dedicated status endpoint，必须返回并测试：

| Field | Required value |
|---|---:|
| `reviewOnly` | `true` |
| `notCandidateSignal` | `true` |
| `notDecisionGeneration` | `true` |
| `notPointSignal` | `true` |
| `notFinalDirection` | `true` |
| `notEntryStopTpRr` | `true` |
| `notTradingSignal` | `true` |
| `notExecutable` | `true` |
| `notSchedulerTrigger` | `true` |
| `notCollectorTrigger` | `true` |
| `notApiClientRefresh` | `true` |
| `notExternalRefresh` | `true` |
| `notSourceBindingGeneration` | `true` |
| `displaySlotsAreCandidatePool` | `false` |

这些字段是边界声明，不是交易能力。

## 5. Required Status Mapping

下一步实现必须覆盖并测试以下状态映射：

| Status | Required source | Fail-closed? | Implementation allowed? |
|---|---|---:|---:|
| `SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY` | Existing SourceTrace + RuntimeKline diagnostics both readable. | No | Yes |
| `SOURCE_TRACE_MISSING_FAIL_CLOSED` | `sourceTrace` missing/null/untrusted. | Yes | Yes |
| `SOURCE_TRACE_PARTIAL_REVIEW_ONLY` | SourceTrace incomplete / has missing fields or blocking reasons. | No | Yes |
| `RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY` | `runtimeKlineContext` readable for diagnostics. | No | Yes |
| `RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED` | RuntimeKline context missing/null. | Yes | Yes |
| `PERSISTED_OHLCV_READY_REVIEW_ONLY` | Persisted OHLCV readiness is fresh enough for diagnostics. | No | Yes |
| `PERSISTED_OHLCV_STALE_REVIEW_ONLY` | Persisted OHLCV stale/partial/unknown/invalid. | No | Yes |
| `PERSISTED_OHLCV_MISSING_FAIL_CLOSED` | Persisted OHLCV missing. | Yes | Yes |
| `DATA_QUALITY_PARTIAL_REVIEW_ONLY` | DataQuality metadata present but partial. | No | Yes |
| `DATA_QUALITY_BLOCKED_FAIL_CLOSED` | DataQuality required but missing/blocked. | Yes | Yes |
| `MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY` | MultiTimeframe metadata indicates alignment. | No | Yes |
| `MULTITIMEFRAME_CONFLICT_REVIEW_ONLY` | MultiTimeframe metadata indicates conflict. | No | Yes |
| `MULTITIMEFRAME_MISSING_FAIL_CLOSED` | MultiTimeframe metadata missing when needed. | Yes | Yes |
| `REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would require refresh / collector / API client. | Yes | Yes, as blocked status only |
| `GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED` | Status would require Candidate / Decision / Point / source-binding generation. | Yes | Yes, as blocked status only |

## 6. DataQuality / MultiTimeframe Boundary

DataQuality（数据质量）只能表示：

- readiness（就绪）；
- stale（过期）；
- partial（部分可读）；
- blocked（阻断 / 失败关闭）。

它不能表示：

- 交易折扣；
- position size（仓位）；
- leverage（杠杆）；
- 执行优先级；
- 自动交易条件。

MultiTimeframe（多周期）只能表示：

- alignment（一致）；
- conflict（冲突）；
- missing（缺失）；
- partial metadata（部分元数据）。

它不能表示：

- final direction（最终方向）；
- entry / stop / TP / RR；
- Candidate ranking（候选排名）；
- Point generation（点位生成）；
- trading signal（交易信号）。

## 7. Refresh / Generation Boundary

下一步实现必须保持：

- no scheduler trigger（不触发调度）；
- no collector trigger（不触发采集器）；
- no API client refresh（不触发接口客户端刷新）；
- no external refresh（不触发外部刷新）；
- no source-binding generation（不生成来源绑定）；
- no Candidate generation（不生成候选）；
- no Decision generation（不生成决策）；
- no Point generation（不生成点位）；
- no final direction / entry / stop / TP / RR；
- no Push send / external channel；
- no order / execution / auto-trading。

任何实现路径若需要上述动作，必须改判 NO-GO。

## 8. Required Tests

下一步 implementation 必须补 targeted tests（定向测试）：

- controller/API smoke test for the minimal status endpoint if a dedicated endpoint is added；
- dashboard template / DOM test for `sourceRuntimeDataQualityStatusPanel` and related status ids if a panel is added；
- safety fields test；
- missing SourceTrace fail-closed test；
- missing RuntimeKline fail-closed test；
- persisted OHLCV stale/missing test；
- DataQuality partial/blocked test；
- MultiTimeframe conflict/missing test；
- refresh boundary blocked test；
- generation boundary blocked test；
- forbidden executable fields absent test；
- forbidden path / forbidden semantics grep check。

Existing adapter tests may receive only tiny owner-path assertions if needed.

## 9. No-Go Conditions

Implementation must be NO-GO if it requires:

- new DTO / Validator / Assembler / Orchestrator；
- new source-binding family；
- schema/config/pom changes；
- scheduler / collector / API client refresh；
- external refresh；
- Candidate generation；
- Decision generation；
- Point generation；
- final direction / entry / stop / TP / RR；
- Push send / external channel；
- order / execution / auto-trading；
- treating MultiTimeframe as direction judgment；
- treating DataQuality as trading execution discount；
- bypassing or replacing the existing dashboard detail owner path。

## 10. Final Recommendation

Readiness decision: **GO**.

Next allowed action:

- `Minimal Review-Only SourceTrace / RuntimeKline / DataQuality / MultiTimeframe Aggregate Runtime Wiring Implementation`

Next branch:

- `minimal-review-only-sourcetrace-runtimekline-dataquality-multitimeframe-status-runtime-wiring-implementation`

Risk:

- `B`, because Java/controller, dashboard template, and tests may change if implementation proceeds.

Capability movement:

- None in this readiness package. The project remains `REVIEW_ONLY_RUNTIME partial`.

## 11. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era / existing assets: Yes
- 是否减少重复: Yes, by keeping `/api/dashboard/detail` and existing adapters as owner path and rejecting frozen source-binding families
- 是否提升 capability level: No, readiness only
- 是否接 service/runtime/dashboard/API: No implementation; readiness permits only future minimal review-only wiring
- 是否符合 #830 审计建议: Yes
