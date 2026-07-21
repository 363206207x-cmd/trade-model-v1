# P3-U2 iPhone Home Field Mapping

## Contract Status

- Design base: `168ef18c7ad148d960902c913f6ddb4b53318e14`
- Mapping contract: `P3-U2-IPHONE-HOME-IA-V2`
- Confirmed display fields: **102**
- Unresolved fields: **1**
- Prototype data mode: `STATIC_LAYOUT_FIXTURE`
- Production Java, Swift, and Dashboard template changes: **none**

The machine-readable companion is
[`field-map.json`](p3-u2-iphone-home-ia-v2/field-map.json). Every row there
contains the required source object, value contract, priority, location, empty
state, rename permission, mapping status, and evidence file. This document is
the human review view of the same contract.

## Source Priority

1. Current backend and Dashboard implementation.
2. Current static and service tests.
3. Current DTO, VO, and enum contracts.
4. Current status sources.
5. Product proposal text.
6. Historical screenshots.

No mobile field may be promoted from a lower-priority source when the current
code does not expose it.

## Mapping Summary

| Module | Confirmed | Unresolved | Mobile rule |
|---|---:|---:|---|
| A. Top status | 7 | 1 | Render seven confirmed text cells; do not invent an eighth cell. |
| B. Realtime alert | 4 | 0 | At most two rows. |
| C. Key event | 4 | 0 | At most two rows; current service returns at most one. |
| D. Watch asset | 15 | 0 | Three assets; P0/P1 on the pager, P2 in expansion/detail. |
| E. Execution advice | 14 | 0 | Follows selected asset; fail-closed fields stay empty. |
| F. Position monitor | 17 | 0 | Independent of selected asset; manual positions only. |
| G. AI evidence review | 30 | 0 | One role visible at a time; roles keep distinct fields. |
| H. Adjudication consistency | 8 | 0 | Embedded in the AI header, not a fourth AI role. |
| I. Bottom navigation | 3 | 0 | Existing route or in-page section target only. |

## A. Top Status

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 市场趋势 | `systemState.marketTrend.valueLabel` | `SystemStateVO.StatusCardVO` | Backend market-bias mapping | 市场趋势 | P0 | 首页摘要 | 等待同步 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 风险等级 | `systemState.riskLevel.valueLabel` | `SystemStateVO.StatusCardVO` | Backend risk mapping | 风险等级 | P0 | 首页摘要 | 等待同步 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 数据质量分 | `systemState.dataQuality.valueLabel` | `SystemStateVO.StatusCardVO` | Numeric only if supplied | 数据质量分 | P0 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| AI 冲突等级 | `systemState.aiConflict.valueLabel` | `SystemStateVO` + `ConsistencyVO` | AI not applicable is `不适用` | AI 冲突等级 | P0 | 首页摘要 | 不适用 | No | CONFIRMED | `DashboardControllerTest` |
| 待复核机会 | `systemState.pendingReview.valueLabel` | `SystemStateVO` + `LightSystemStatusVO` | Non-negative count; zero is 暂无 | 待复核机会 | P0 | 首页摘要 | 暂无 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 冲突阻断 | `systemState.confused.valueLabel` | `SystemStateVO` + `LightSystemStatusVO` | Directional block count | 冲突阻断 | P0 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 热重置 | `systemState.hotReset.valueLabel` | `SystemStateVO.StatusCardVO` | 已触发 / 未触发 | 热重置 | P0 | 首页摘要 | 等待同步 | No | CONFIRMED | `dashboard.html` |
| 持仓风险（候选第八项） | none | none | Current code has no eighth top-status field | none | P0 | 不渲染 | none | No | **UNRESOLVED** | `DashboardHomeVO.SystemStateVO` |

### Top-status discrepancy

`DashboardHomeVO.SystemStateVO`, `buildSystemState`, the template, and its
contract tests all expose exactly seven status cells. The product task names
“持仓风险” as a candidate eighth item, but no current backend field or test
contract confirms it. IA v2 therefore renders seven cells and records the
candidate as `UNRESOLVED_FIELD`; it does not reuse a position risk field or
derive a new aggregate.

## B. Realtime Alert

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 告警级别 | `alerts[].level` | `AlertRowVO` | Backend alert level; text remains visible | 级别 | P0 | 首页摘要 | 暂无告警 | Yes | CONFIRMED | `DashboardHomeVO` |
| 资产 | `alerts[].symbol` | `AlertRowVO` | Backend display symbol | 资产 | P0 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 告警内容 | `alerts[].message` | `AlertRowVO` | Sanitized backend message | 告警内容 | P0 | 首页摘要 | 暂无告警 | Yes | CONFIRMED | `dashboard.html` |
| 时间 | `alerts[].time` | `AlertRowVO` | Backend time string, no client timezone guess | 时间 | P1 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |

## C. Key Event Window

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 事件类型 | `events[].type` | `EventRowVO` | Current home value is `EXTERNAL_CONTEXT` | 事件类型 | P2 | 展开区 | 暂无关键事件 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 关键事件 | `events[].label` | `EventRowVO` | External-context label only | 关键事件 | P0 | 首页摘要 | 暂无关键事件 | No | CONFIRMED | `dashboard.html` |
| 影响等级 | `events[].impactLevel` | `EventRowVO` | Backend risk value | 影响等级 | P0 | 首页摘要 | 状态待同步 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 事件窗口 | `events[].timeWindow` | `EventRowVO` | Backend window/time; no client countdown | 事件窗口 | P1 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |

## D. Watch Asset

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 资产 | `assets[].symbol` | `AssetVO` | Display symbol | 资产 | P0 | 首页摘要 | 等待首轮分析 | No | CONFIRMED | `DashboardHomeVO` |
| 综合评分 | `assets[].compositeScore` | `AssetVO` | Rounded analysis average if present | 综合评分 | P0 | 首页摘要 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 方向 | `assets[].marketBiasLabel` | `AssetVO` | Backend product mapping | 方向 | P0 | 首页摘要 | 等待同步 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 风险等级 | `assets[].riskLabel` | `AssetVO` | Backend product mapping | 风险等级 | P0 | 首页摘要 | 状态待同步 | No | CONFIRMED | `DashboardHomeVO` |
| 是否值得开仓 | `assets[].worthOpening` | `AssetVO` | Boolean review evidence, never authorization | 是否值得开仓 | P0 | 首页摘要 | 等待同步 | No | CONFIRMED | `DashboardHomeControllerTest` |
| 置信度 | `assets[].confidenceLabel` | `AssetVO` | 高 / 中 / 低 when recognized | 置信度 | P1 | 17PM 摘要 / 12PM 展开 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 最新价 | `assets[].latestPrice` | `AssetVO` | Persisted closed 5m OHLCV close | 最新价 | P1 | 17PM 摘要 / 12PM 展开 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 资产状态 | `assets[].assetStateLabel` | `AssetVO` | Eight existing enum labels | 资产状态 | P1 | 展开区 | 状态待同步 | No | CONFIRMED | `DashboardHomeControllerTest` |
| 当前结论 | `assets[].currentConclusion` | `AssetVO` | Backend product copy | 当前结论 | P1 | 展开区 | 等待分析 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 数据状态 | `assets[].dataFreshness` | `AssetVO` | `FRESH/PARTIAL/NO_DATA/UNAVAILABLE` | 数据状态 | P2 | 详情页 | 等待同步 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 四周期新鲜度 | `assets[].timeframeFreshness` | `AssetVO` | 5m / 15m / 1h / 4h map | 周期数据状态 | P2 | 详情页 | 暂无数据 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 数据来源 | `assets[].sourceProvider` | `AssetVO` | Backend provider label | 数据来源 | P2 | 详情页 | 等待同步 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 不可用原因 | `assets[].unavailableReason` | `AssetVO` | Raw code is diagnostic-only | 数据说明 | P2 | 详情页 | 暂无说明 | Yes | CONFIRMED | `DashboardControllerTest` |
| 证据数 | `assets[].evidenceCount` | `AssetVO` | Analysis evidence count | 证据数量 | P2 | 详情页 | `--` | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 分析时间 | `assets[].latestAnalysisTime` | `AssetVO` | Decision create time | 分析时间 | P2 | 详情页 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |

Only the first seven rows may appear on a watch card. IA v2 keeps exactly
three cards. P2 rows are deliberately excluded from the home card.

## E. Execution Advice

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 执行建议状态 | `executionSuggestion.statusLabel` | `ExecutionSuggestionVO` | Backend fail-closed copy | 执行建议状态 | P0 | 首页摘要 | 当前暂无完整执行计划 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 阻断原因 | `executionSuggestion.blockedReason` | `ExecutionSuggestionVO` | Backend reason only | 当前说明 | P0 | 首页摘要 | 暂无说明 | Yes | CONFIRMED | `DashboardHomeControllerTest` |
| 方向 | `executionSuggestion.direction` | `ExecutionSuggestionVO` | Populated only for valid review context | 方向 | P0 | 首页摘要 | 暂无 | No | CONFIRMED | `dashboard.html` |
| 入场区间 | `executionSuggestion.entryZone` | `ExecutionSuggestionVO` | Concrete source-gated boundary | 入场区间 | P0 | 长行 | 暂无 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 止损价 | `executionSuggestion.stopLoss` | `ExecutionSuggestionVO` | System review boundary, not user stop | 止损价 | P0 | 首页摘要 | 暂无 | No | CONFIRMED | `dashboard.html` |
| 阶梯止盈 | `executionSuggestion.takeProfitRules` | `ExecutionSuggestionVO` | Review text; no parsing into orders | 阶梯止盈 | P0 | 长行 | 暂无 | No | CONFIRMED | `dashboard.html` |
| 杠杆 | `executionSuggestion.leverageSuggestion` | `ExecutionSuggestionVO` | Review suggestion only | 杠杆 | P1 | 首页摘要 | 暂无 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 仓位建议 | `executionSuggestion.positionSuggestion` | `ExecutionSuggestionVO` | Never creates/mutates a position | 仓位建议 | P0 | 长行 | 暂无 | No | CONFIRMED | `ExecutionPlanVO` |
| 有效期 | `executionSuggestion.validPeriod` | `ExecutionSuggestionVO` | Backend validity display | 有效期 | P0 | 首页摘要 | 暂无 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 开始时间 | `executionSuggestion.validFrom` | `ExecutionSuggestionVO` | Structured `OffsetDateTime` | 开始时间 | P2 | 展开区 | `--` | Yes | CONFIRMED | `DashboardHomeVO` |
| 到期时间 | `executionSuggestion.expiresAt` | `ExecutionSuggestionVO` | Backend blocks at/after expiry | 到期时间 | P2 | 展开区 | `--` | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 失效条件 | `executionSuggestion.invalidCondition` | `ExecutionSuggestionVO` | Review condition only | 失效条件 | P0 | 长行 | 暂无 | No | CONFIRMED | `dashboard.html` |
| 原执行计划说明 | `executionSuggestion.originalPlanLabel` | `ExecutionSuggestionVO` | Only for verified original-plan identity | 原执行计划 | P1 | 持仓历史展开 | 暂无可关联的原执行计划 | Yes | CONFIRMED | `DashboardHomeServiceImplTest` |
| 计划来源身份 | `sourceAnalysisId/sourceExecutionPlanId/sourceTraceId` | `ExecutionSuggestionVO` | Diagnostic only | 来源诊断 | P2 | 受控诊断层 | 不可验证 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |

Execution advice follows `selectedSymbol`. It never becomes a UserPosition,
order, or automatic action. User stop values remain separate from system plan
boundaries.

## F. Position Monitor

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 持仓标识 | `positions[].positionId` | `PositionVO` | Exact selection identity, diagnostic/interaction only | 持仓标识 | P2 | 交互属性 | `--` | Yes | CONFIRMED | `DashboardHomeControllerTest` |
| 资产 / 方向 | `positions[].symbol/directionLabel` | `PositionVO` | Active MANUAL positions only | 资产 / 方向 | P0 | 首页摘要 | 暂无持仓 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 用户开仓价 | `positions[].entryPrice` | `PositionVO` | Real UserPosition entry | 用户开仓价 | P0 | 首页摘要 | `--` | No | CONFIRMED | `UserPositionDTO` |
| 当前价 | `positions[].currentPrice` | `PositionVO` | Monitor/fresh read-only quote | 当前价 | P1 | 展开区 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 浮动盈亏 | `positions[].pnlPct/floatingPnl` | `PositionVO` | Read-only derived display | 浮动盈亏 | P1 | 展开区 | `--` | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 入场逻辑 | `positions[].entryLogicStatusLabel` | `PositionVO` | Backend status mapping | 入场逻辑 | P0 | 首页摘要 | 等待首次监控 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 方向支持 | `positions[].directionSupportStatusLabel` | `PositionVO` | Backend status mapping | 方向支持 | P0 | 首页摘要 | 等待首次监控 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 反转状态 | `positions[].reversalStatusLabel` | `PositionVO` | Backend status mapping | 反转状态 | P0 | 首页摘要 | 等待首次监控 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 风险等级 | `positions[].riskLevelLabel` | `PositionVO` | Position-monitor risk, not asset risk | 风险等级 | P0 | 首页摘要 | 等待首次监控 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 当前建议 | `positions[].suggestedManualActionText` | `PositionVO` | Manual review only | 当前建议 | P0 | 首页摘要 | 等待监控 | No | CONFIRMED | `PositionMonitorResultDTO` |
| 监控结论 | `positions[].monitorConclusion` | `PositionVO` | Latest monitor product label | 监控结论 | P1 | 展开区 | 等待首次监控 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 最近监控 | `positions[].lastMonitorAt` | `PositionVO` | Persisted monitor-log time | 最近监控 | P1 | 首页摘要 | 等待首次监控 | Yes | CONFIRMED | `DashboardControllerTest` |
| 下次监控 | `positions[].nextMonitorAt` | `PositionVO` | Missing after a prior monitor means no schedule | 下次验证 | P0 | 首页摘要 | 等待首次监控 / 暂无下次监控排期 | Yes | CONFIRMED | `DashboardControllerTest` |
| 用户止损 | `positions[].userStopLoss` | `PositionVO` | User fact, separate from system suggestion | 用户止损 | P1 | 展开区 | `--` | No | CONFIRMED | `UserPositionDTO` |
| 用户止盈 | `positions[].userTakeProfit` | `PositionVO` | User fact, separate from system suggestion | 用户止盈 | P1 | 展开区 | `--` | No | CONFIRMED | `UserPositionDTO` |
| 持仓状态 | `positions[].positionStatusLabel` | `PositionVO` | Active manual position label | 持仓状态 | P1 | 展开区 | 状态待同步 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 人工处理 | `positionId/positionStatus` | Existing manual Dashboard flow | Static prototype never writes | 人工处理 | P0 | 详情入口 | 暂无可处理持仓 | No | CONFIRMED | `dashboard.html` |

Position cards are never re-scoped when a watch asset changes. Unknown raw
status values display `状态待同步`; raw values belong only in controlled
diagnostics.

## G. AI Evidence Review

### Shared role state

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| AI 运行状态 | `aiDecision.runStatusLabel` | `AiDecisionVO` | Backend aggregate status mapping | AI 运行状态 | P0 | AI 头部 | 未调用 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| AI 决策模式 | `aiDecision.decisionModeLabel` | `AiDecisionVO` | AI 辅助复核 / 无可裁决结论 / 仅规则判断 | 复核模式 | P1 | AI 头部 | 仅规则判断 | Yes | CONFIRMED | `DashboardHomeServiceImpl` |
| 角色 | `tabs[].roleLabel` | `AiTabVO` | Exact three-role mapping | 角色 | P0 | Segmented Control | `--` | No | CONFIRMED | `DashboardHomeServiceImplTest` |
| 运行状态 | `tabs[].runStatusLabel` | `AiTabVO` | Role status only; no cross-role clone | 运行状态 | P0 | 当前角色 | 未调用 | No | CONFIRMED | `DashboardControllerTest` |
| 状态说明 | `tabs[].statusMessage` | `AiTabVO` | Unavailable role renders status only | 状态说明 | P0 | 当前角色 | 本轮未调用该角色 | No | CONFIRMED | `DashboardHomeServiceImpl` |

### GPT_FINAL / 最终裁决官

| Web label | Backend field | Contract | Mobile location | Empty | Status |
|---|---|---|---|---|---|
| 最终倾向 | `finalMarketBias` | Synthesis final market bias | 当前角色 | 暂无 | CONFIRMED |
| 置信度 | `finalConfidence` | Synthesis confidence | 当前角色 | 暂无 | CONFIRMED |
| 风险等级 | `finalRiskLevel` | Synthesis risk | 当前角色 | 暂无 | CONFIRMED |
| AI 计划模式 | `finalPlanMode` | Review mode, not execution | 当前角色 | 暂无 | CONFIRMED |
| 是否值得开仓 | `worthOpening` | Explicit Boolean only | 当前角色 | 等待同步 | CONFIRMED |
| 最终结论 | `finalConclusion` | Sanitized summary | 当前角色 | 等待同步 | CONFIRMED |
| 核心支持证据 | `coreSupportingEvidence` | SUPPORT reason labels only | 展开区 | 暂无该角色证据 | CONFIRMED |
| 核心反证 | `coreCounterEvidence` | CHALLENGE reason labels only | 展开区 | 暂无该角色证据 | CONFIRMED |
| 裁决摘要 | `decisionSummary` | Sanitized summary | 当前角色 | 等待同步 | CONFIRMED |
| 降级 / 阻断原因 | `downgradeReason` | Backend mapped reason | 当前角色 | 暂无该角色证据 | CONFIRMED |

All rows are P0 except the two evidence lists and `decisionSummary`, which are
P1. Evidence: `DashboardHomeVO`, `DashboardHomeServiceImpl`,
`DashboardHomeServiceImplTest`, and `DashboardControllerTest`.

### GEMINI_REVIEW / 冲突复核官

| Web label | Backend field | Contract | Priority | Empty | Status |
|---|---|---|---|---|---|
| 复核意见 | `reviewVerdict` | Backend stance label | P0 | 等待同步 | CONFIRMED |
| 发现的冲突 | `detectedContradictions` | CHALLENGE reason labels | P0 | 暂无该角色证据 | CONFIRMED |
| 证据不足点 | `weakEvidence` | Backend list only | P1 | 暂无该角色证据 | CONFIRMED |
| 逻辑漏洞 | `logicGaps` | Backend list only | P1 | 暂无该角色证据 | CONFIRMED |
| 是否建议降级 | `downgradeRecommendation` | Explicit backend value only | P0 | 等待同步 | CONFIRMED |
| 风险调整建议 | `riskAdjustmentSuggestion` | Review-only backend value | P1 | 等待同步 | CONFIRMED |
| 是否需要人工复核 | `manualReviewRequired` | Explicit backend value only | P0 | 等待同步 | CONFIRMED |
| 复核结论 | `reviewConclusion` | Sanitized summary; ABSTAIN has fixed copy | P0 | 等待同步 | CONFIRMED |

### GROK_CHALLENGE / 反方挑战官

| Web label | Backend field | Contract | Priority | Empty | Status |
|---|---|---|---|---|---|
| 反方论点 | `challengeThesis` | Sanitized summary | P0 | 等待同步 | CONFIRMED |
| 突发新闻 / 事件风险 | `eventRisks` | Backend list only; no invented news | P0 | 暂无该角色证据 | CONFIRMED |
| 情绪反转风险 | `sentimentReversalRisks` | Backend list only | P1 | 暂无该角色证据 | CONFIRMED |
| 微观结构陷阱 | `microstructureTraps` | Backend list only | P1 | 暂无该角色证据 | CONFIRMED |
| 流动性 / 插针 / 挤仓风险 | `liquidityRisks` | Backend list only | P1 | 暂无该角色证据 | CONFIRMED |
| 反向证据 | `counterEvidence` | CHALLENGE reason labels | P0 | 暂无该角色证据 | CONFIRMED |
| 反方挑战结论 | `challengeConclusion` | Sanitized summary | P0 | 等待同步 | CONFIRMED |

## H. Adjudication Consistency

| Web label | Backend field | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| 一致性适用性 | `consistency.aiApplicable` | `ConsistencyVO` | False precedes all conflict checks | 裁决一致性 | P0 | AI 头部 | 不适用 | Yes | CONFIRMED | `DashboardControllerTest` |
| 一致性等级 | `consistency.consistencyLevel` | `ConsistencyVO` | Backend product label | 一致性等级 | P0 | AI 头部 | 不适用 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 一致性评分 | `consistency.consistencyScore` | `ConsistencyVO` | Numeric only when actual score exists | 一致性评分 | P2 | AI 展开 | `--` | No | CONFIRMED | `DashboardControllerTest` |
| 冲突等级 | `consistency.level` | `ConsistencyVO` | Raw level with backend mapping | 冲突等级 | P0 | AI 头部 | 不适用 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 是否进入冲突阻断 | `consistency.confused` | `ConsistencyVO` | Explicit synthesis Boolean | 是否进入冲突阻断 | P0 | AI 头部 | 否 | No | CONFIRMED | `DashboardControllerTest` |
| 资产方向阻断 | `consistency.directionalPushBlocked` | `ConsistencyVO` | Independent from AI applicability | 资产方向阻断 | P1 | AI 展开 | 否 | No | CONFIRMED | `DashboardControllerTest` |
| 降级原因 | `consistency.downgradeReason` | `ConsistencyVO` | Backend mapped reason | 降级原因 | P0 | AI 头部 | 暂无降级原因 | No | CONFIRMED | `DashboardHomeServiceImpl` |
| 一句话摘要 | `consistency.consistencySummary` | `ConsistencyVO` | Backend role-count summary | 一句话摘要 | P0 | AI 头部 | 等待 AI 三角色结果同步后生成一致性结论 | No | CONFIRMED | `DashboardHomeServiceImpl` |

The score has no ring visualization. Current service output is `null`, so the
prototype displays `--` and cannot fabricate a 0-100 value.

## I. Bottom Navigation

| Web label | Backend/target | Source object | Value contract | Mobile label | Priority | Location | Empty | Rename | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| Dashboard | `/dashboard` | Existing authenticated route | Existing iOS root route | 首页 | P0 | Fixed bottom nav | 首页 | Yes | CONFIRMED | `BackendConfiguration.swift` |
| 持仓监控 | `positions[]` / `#position-monitor` | Home read model | Prototype in-page anchor only | 持仓 | P0 | Fixed bottom nav | 暂无持仓 | Yes | CONFIRMED | `dashboard.html` |
| 复盘中心 | `/review/dashboard` | Existing Dashboard link | Existing authenticated route | 复盘 | P0 | Fixed bottom nav | 复盘 | Yes | CONFIRMED | `dashboard.html` |

No fourth navigation destination is introduced. The static anchor adds no
backend route and performs no write.

## Display Isolation Rules

1. Watch-asset selection changes the request context for execution advice,
   AI evidence, and consistency only.
2. Position monitor rows never change when a watch asset is selected.
3. Execution-plan stop values never replace user-entered stop values.
4. `ExecutionSuggestionVO` never becomes a `UserPosition`.
5. Missing AI role data never borrows another role's content.
6. Unknown enum values display `状态待同步` or `未知状态`; raw values remain
   available only to diagnostics.
7. The static wireframe contains field tokens only and no market, event,
   position, or AI evidence values.

## Unresolved Field

| Field | Evidence | Impact | Minimal next action |
|---|---|---|---|
| `top.holdingRisk` | No field in `SystemStateVO`; no builder assignment; no template card; tests assert seven current cards | IA v2 renders seven truthful status cells, leaving no fake eighth value | Product/backend owner must define a source object and test contract before a future design can render it |
