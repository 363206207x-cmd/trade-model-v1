# Phase 5 Step 1 - Field Truth Matrix

## Scope Freeze

This document freezes the Phase 5 Step 1 field contract for two primary battleground pages:

- `Dashboard` (`src/main/resources/templates/dashboard.html`)
- `Review` (`src/main/resources/static/js/review-page.js`)

No business logic changes are included in this step.

## Step 1 Non-Goals

- Do not change Java business logic.
- Do not change main pipeline rules.
- Do not change page layout or UI redesign.
- Do not add new feature APIs.
- Do not run schema migration.
- Do not perform broad refactor.
- Do not implement Step 2 / Step 3 code.

## Contract Columns (Final)

All rows in this matrix use the following columns:

| Column | Meaning |
| --- | --- |
| Page Module | Dashboard / Review sub-module |
| Page Display Field | Field shown on page |
| FE Read Function / Variable | Frontend read path |
| API / VO Field | API payload or VO field |
| Backend Assembly Location | Service / Controller / Mapper location |
| DB Field / Source Table | Physical column(s) and table(s) |
| Truth Source Note | Final semantic source |
| Current State | `truth` / `proxy` / `fallback` / `heuristic` / `missing` |
| Phase 5 Target State | Desired end-state in Phase 5 |
| Action Guidance | Step 2/3 execution guidance |
| Priority | P0 / P1 / P2 |
| FE Inference Ban | Whether frontend inference is banned (`YES`/`NO`) |
| Owner Step | Expected convergence step (Step 2 or Step 3) |

> Extra columns added for execution control: `FE Inference Ban`, `Owner Step`.

## Dashboard Truth Matrix

### Data quality score — boundary (run-level vs Push/Recheck)

- **Primary truth for run-level score:** `tm_analysis_run.data_quality_score` (one row per analysis run; join key **`analysis_id`**).
- **`tm_decision_result` does not carry this column.** Dashboard run-level quality must not be read from the decision row as if it were persisted there.
- **Push / Recheck chain (do not mix with run-level score):** `tm_push_snapshot.data_quality_score_snapshot` and `tm_push_recheck_log.current_data_quality_score` belong to push/recheck observability; they are **not** interchangeable with `tm_analysis_run.data_quality_score` semantics.
- **Dashboard display rule:** If surfaced on Dashboard, align the **run-level** score to the decision row’s **`analysis_id`** via **`tm_analysis_run`** (same id), not via Push snapshot columns.
- **Joint narrative freeze:** canonical wording for market-environment snapshot + run-level DQ + three-line quality boundaries → `PHASE10_DASHBOARD_FREEZE_INDEX.md` §「市场环境快照 + run级 DQ 联合契约（冻结）」.
- **Run-level DQ (PHASE10 authoritative):** discrete outputs **35 / 55 / 85** only, `PLACEHOLDER_FALLBACK` cap at **≤55**; Review/Alert **&lt;60** is **implementation-only** and equivalent to “not 85” under current discreteness—see `PHASE10_DASHBOARD_FREEZE_INDEX.md` §「run级 DQ 当前实现现状」and `AnalysisAssemblerServiceImpl#estimateDataQualityScore` JavaDoc.

### Market environment module (`MarketEnvironmentVO`) — boundary (current repo)

This block records the **truth-chain status** of the standalone market-environment object. It does **not** add matrix rows; do not treat the following as Phase 5 Step 1 Dashboard/Review contract rows until separately specified.

- **Main-chain minimum wiring (current):** `AnalysisAssemblerServiceImpl` now calls **`RealMarketEnvironmentService.tryBuildFromRealQuote()`** on the primary **`assemble`** path.
- **Current real-time input scope:** the only live input for this slice is **24h quote heuristic** from Binance ticker (`MarketQuoteClient.fetch24hTicker` path).
- **Success / fallback contract:** success maps into five-field **`MarketEnvironmentVO`** (`environmentType`, `riskMode`, `trendFriendliness`, `leverageSuggestion`, `summary`); failure falls back to placeholder summary on the same chain.
- **Persistence anchor:** **`tm_market_environment_snapshot`** is now the market-environment fact anchor aligned by **`analysis_id`**.
- **Funding/OI snapshot minimum structured columns (current):** `last_funding_rate`, `perp_funding_applied`, `last_open_interest`, `oi_applied` are persisted from same-run `MarketEnvironmentVO` only (no summary backfill/inference); nullable under best-effort semantics.
- **OI delta minimum truth (current):** `open_interest_delta` (`openInterestDelta`) is persisted as same-run market-environment upstream truth using previous snapshot baseline (same `symbol + timeframe`, current-minus-previous; null when unavailable); see `PHASE10_DASHBOARD_FREEZE_INDEX.md`.
- **Funding/OI joint-derived tag (current):** `derivatives_crowding_state` is persisted from same-run `MarketEnvironmentVO.derivativesCrowdingState` as a minimal discrete replay anchor (`NEUTRAL` / `CROWDED_LONG` / `CROWDED_SHORT`), not a full multi-source state engine.
- **Dashboard detail read boundary:** Dashboard detail **prefers snapshot by `analysis_id`** and exposes **`marketEnvironmentMini`** (`summary`, `environmentType`, `riskMode`, `sourceType`); only when snapshot is missing does it fallback to heuristic/fallback chain.
- **Review aggregate read boundary:** Review aggregate reads the same snapshot fact source via **`ReviewAggregateVO.marketEnvironment.*`** (same `analysis_id` alignment), consistent with Dashboard detail's snapshot-first contract.
- **Current minimum collaboration chain:** the current repo has formed a minimal explain-chain handoff: **`market environment + evidence -> score -> decision`** (assemble path), rather than independent parallel display only.
- **Non-goal guardrail (this stage):** this is still **not** the full PROJECT_SPEC market-environment module delivery (non-multi-source, non-independent module UI).
- **Semantic split:** **`marketBiasHierarchy`** (`tm_decision_result.market_bias_hierarchy` → `DecisionResultVO.marketBiasHierarchy`) is a **decision output** (“direction” / bias ladder), **not** the **`MarketEnvironmentVO`** market-environment module truth. Do **not** merge them into “market environment module is done”.
- **Dual-source phase conclusion & Funding-vs-snapshot (frozen wording):** spot 24h + optional Funding 最小双源收口、第二维快照列 **vs** Funding 暂不列化等——见 `PHASE10_DASHBOARD_FREEZE_INDEX.md` **§五、市场环境双源阶段结论（拍板）**。

### systemHealth map (Dashboard summary)

- **`databaseStatus`：** 数据库连通探测。
- **`schedulerStatus`：** 持仓同步活动状态，**不等同**全站分析调度状态。
- **`cpuUsage`：** 负载启发式，**不等同**标准 CPU%。
- **`memoryUsage`：** JVM 堆占用，**不等同**整机内存。

### Execution plan layer — implementation boundary (current repo)

These notes avoid conflating **`tm_execution_plan`**, **`DecisionResultVO`**, **`executionPlanSummary`**, and Dashboard display slots.

- **`tm_execution_plan` (narrow table, persisted):** `plan_id`, `analysis_id`, `recommended_action`, `entry_zone`, `stop_loss`, `take_profit_rules`, `leverage_suggestion`, `position_suggestion`, `create_time` (see `schema.sql`).
- **Cross-reference (execution minimal truthization):** `invalid_condition` has been persisted into `tm_execution_plan` as execution-object minimal truth (same-source mirror from decision), while latest-plan / Review read-through stays out of this cut.
- **Execution account risk minimal truth (write-chain only):** `tm_execution_plan.account_risk_json` stores a 7-key risk-constraint snapshot (`riskAllowed`, `riskReasonCode`, `riskReasonText`, `positionExposure`, `maxAllowedExposure`, `snapshotSource`, `snapshotVersion`) sourced from `tm_account_risk_snapshot`; this is execution object minimal truthization, not latest-plan/Review read-through.
- **Dashboard exposure from the plan table (`DecisionResultMapper` → `DecisionResultVO` → Dashboard s4):** The **latest plan row per `analysis_id`** is selected in a single subquery (`ROW_NUMBER() ... ORDER BY create_time DESC, plan_id DESC`, `rn = 1`). From that same join/row, Dashboard summary/detail APIs already map at least: **`recommended_action` → `recommendedAction`**, **`entry_zone` → `entryZone`**, **`stop_loss` → `stopLoss`**, **`take_profit_rules` → `takeProfitRules`**, **`leverage_suggestion` → `leverageSuggestion`**, **`position_suggestion` → `positionSuggestion`**. Tiles may surface `recommendedAction` prominently; **`executionBlock()` (s4)** consumes the full set above for the “执行建议” tab (plus decision-side fields below).
- **Execution object minimum truth extension (`plan_mode`):** `tm_execution_plan.plan_mode` is now persisted as execution-side object truth (`ADVISORY` / `SEMI_STRUCTURED`) and carried through latest-plan read chain; current scope is backend truth-chain availability, not mandatory Dashboard rendering.
- **`valid_period` / `invalid_condition`:** Persisted on **`tm_decision_result`**, not on `tm_execution_plan`. They are the same fields used for read-model completeness signaling.
- **`executionPlanSummary`:** **Derived in SQL** by concatenating `valid_period` and `invalid_condition` in `DecisionResultMapper` — it is **not** a column on `tm_execution_plan` and **not** a stored column on `tm_decision_result`. It is **only** a short derived summary for “执行要点” labeling; **do not** treat it as the full execution-plan narrative or as a substitute for narrow-table plan fields.
- **Current first truth-cut source boundary (`validPeriod` / `invalidCondition`):** `invalidCondition` is no longer fixed placeholder text (e.g. "价格跌破当前支撑位"); current value comes from **`DecisionBundleVO.pushInvalidationSummary`**. `validPeriod` is no longer a fixed time-window constant; current value comes from decision **`createTime ~ pushExpiresAt`**. Both fields are nullable and may remain `null` when source data is absent. `executionPlanSummary` stays a derived field, but its upstream is no longer fixed placeholder constants.
- **Join semantics (latest plan):** `findLatestDecisionResultsJoined` / `findLatestDecisionResultBySymbolJoined` join through a derived table that assigns **`rn = 1`** to the **latest** `tm_execution_plan` row per `analysis_id` (by `create_time DESC`, tie-break `plan_id DESC`). All plan-narrow fields on `DecisionResultVO`, including **`takeProfitRules`**, come from **that same row**.
- **Review aggregate:** `ReviewPlanSummary` still carries the full narrow plan snapshot for **`GET /api/review/aggregate`** / 复盘 Plan 摘要（kv 列表）。Dashboard s4 **已与之对齐窄表字段层面**（同源 `tm_execution_plan` 语义）；复盘侧仍可保留更完整的 **plan 区块编排 / 上下文**，不等同于 Dashboard 仅需完整“执行解释模块”。

**`take_profit_rules` — Dashboard display contract (implemented on s4):** (`tm_execution_plan.take_profit_rules`, `TEXT`)  
- Primary form: **multi-line text**. If overlong: **collapse / expand**.  
- **Null / empty / whitespace:** show **—**. Literal placeholder **`暂无`:** treat as empty, show **—**.  
- **Do not** concatenate with **`executionPlanSummary`**. Semantics: **plan-table** field, **not** a decision-summary string.

### Score detail (top3) — implementation boundary (current repo)

- **`tm_score_item` has formed a minimal real scoring chain:** score rows are persisted on the analysis save path and anchored by `analysis_id`, no longer interpreted as fixed constant-score semantics.
- **Current four real score items:** **趋势结构分**, **综合可信度分**, **资金推动分**, and **杠杆风险分**.
- **Trend-structure score boundary:** **趋势结构分 = `marketEnvironment.summary` lightweight rules + minimum price-structure evidence signal**.
- **Comprehensive credibility boundary:** **综合可信度分 = market-environment field completeness + lightweight consistency rules over price-structure evidence**.
- **Detail read chain is already wired:** `analysis_id -> scoreTopItems(top3)` is available on `/api/dashboard/detail`.
- **Review aggregate / page boundary (current):** `ReviewAggregateVO` exposes **`scoreTopItems`** sourced by the **same service/mapper semantics** as Dashboard detail (`ScoreBriefVO`: `scoreType`, `scoreValue`). The Review decision section renders **`评分明细（前3条）`** as a minimal read-only list aligned with Dashboard `s2`; **brief fields remain top3-only** (`ScoreItemMapper.selectTop3BriefByAnalysisId`), **no** expanded `description`/`direction`/`weight` columns in this read path.
- **Dashboard `s2`承接边界（当前）：** `scoreTopItems(top3)` currently carries the **minimal display of the four real score items**.
- **Top3 ordering semantics:** current top3 uses **`score_id DESC`**, which represents record-order recency approximation, **not** importance ranking.
- **Delivery boundary:** this remains a minimal item-set read/display chain only (non-eight-score completion state), and must **not** be interpreted as a complete scoring explanation layer or full eight-score module delivery (Review included). Funding-driven score is a minimal real score expansion only; it does not mean the eight-score framework is complete, and it is not hard-wired into the decision main path. Leverage-risk score is a minimal real score expansion only; it does not mean risk modeling is complete, and it is not hard-wired into the decision main path.

### Evidence detail (top3) — implementation boundary (current repo)

- **证据模块口径权威入口：** 详见 **`PHASE10_DASHBOARD_FREEZE_INDEX.md` §六、证据五类覆盖阶段结论（拍板）**（含 Hot Reset 驱动的最小事件注记、价格结构日内启发式代理及 score 消费边界；勿沿用旧「默认证据」叙事）。
- **`tm_evidence_item` write truth chain exists:** evidence rows are persisted on the analysis save path and anchored by `analysis_id`.
- **Evidence governance boundary (current):** evidence is no longer “type-only light governance”; a minimum controlled skeleton is now in place for **`evidence_type + direction + source`**.
- **Controlled value sets (current):**
  - `evidence_type`: `价格结构`, `杠杆`, `资金`, `事件`, `风险`
  - `direction`: `BULLISH`, `BEARISH`, `NEUTRAL`
  - `source`: `SYSTEM_GENERATED`, `MARKET_HEURISTIC`, `MANUAL_INPUT`
- **Write-chain guardrails (current):** all three fields are constrained by generation-side controlled defaults and persistence-side normalize/fallback before insert.
- **Detail read chain is already wired:** `analysis_id -> evidenceTopItems(top3)` is available on `/api/dashboard/detail`.
- **Dashboard display scope (current):** evidence details are exposed only in Dashboard detail section `s2` as **“证据明细（前3条）”**. **`evidenceTopItems` now performs minimal perceivable consumption of the three governed fields:** each row surfaces **`evidenceType`**, **`description`**, **`direction`** (primary label), and **`source`** (secondary muted cue). This is **not** “type + description only”.
- **Review aggregate / page boundary (current):** `ReviewAggregateVO` exposes **`evidenceTopItems`** with the **same `EvidenceBriefVO` semantics** as Dashboard detail (`EvidenceService.listTopEvidenceBriefByAnalysisId`). The Review decision section renders **`结构化证据（前3条）`** as a minimal read-only list (**direction** as primary cue, **source** as secondary muted cue), **alongside** — **not replacing** — `evidenceSummary` / `reviewReasons` parsed from **`ReviewDecisionSummary`** (`tm_decision_result`). **`evidenceSummary` remains decision-summary text**, not the structured explanation layer title.
- **Delivery boundary:** read-model remains minimal **top3** only on **both** Dashboard detail `s2` and Review; Review has advanced from **summary-only consumption** to **minimal structured replay** (`evidenceTopItems` + `scoreTopItems`), but this is **still not** a complete evidence/score interpretation module (**no** extra columns beyond brief; **no** modular review “evidence/score hub”).
- **Five-type coverage phase conclusion (frozen):** which of the five `evidence_type` values are placeholder vs real anchors vs not generated yet, and the score/decision boundary — see `PHASE10_DASHBOARD_FREEZE_INDEX.md` **§六、证据五类覆盖阶段结论（拍板）**. **Run-level DQ:** current-minimum leverage rows that match the narrow template are **carved out** from `effectiveEvidenceCountForDataQuality` like second-dimension risk and Funding — authoritative wording in `PHASE10_DASHBOARD_FREEZE_INDEX.md` **§二、run级 DQ 当前契约**.

| Page Module | Page Display Field | FE Read Function / Variable | API / VO Field | Backend Assembly Location | DB Field / Source Table | Truth Source Note | Current State | Phase 5 Target State | Action Guidance | Priority | FE Inference Ban | Owner Step |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Dashboard / Execution plan | `recommendedAction` | `resolveRecommendedActionPresentation()` / s4 context | `DecisionResultVO.recommendedAction` | `DecisionServiceImpl` summary/detail → `DecisionResultMapper.findLatestDecisionResultsJoined()` / `findLatestDecisionResultBySymbolJoined()` | **`tm_execution_plan.recommended_action`** (latest-plan subquery, `rn = 1`) | One of several plan-table fields from the **same latest-plan row** | truth | keep truth | Keep JOIN semantics aligned with other plan narrow fields | P1 | YES | Step 3 |
| Dashboard / Execution plan | `validPeriod` | `renderDetail()`, `executionBlock()` | `DecisionResultVO.validPeriod` | same mapper chain as decision row | **`tm_decision_result.valid_period`** | Decision column, not `tm_execution_plan` | truth | keep truth | Keep labels aligned with matrix (“有效期” vs execution block slot); do not treat as plan `entry_zone` | P1 | YES | Step 3 |
| Dashboard / Execution plan | `invalidCondition` | same | `DecisionResultVO.invalidCondition` | same | **`tm_decision_result.invalid_condition`** | Decision column, not plan table | truth | keep truth | same | P1 | YES | Step 3 |
| Dashboard / Execution plan | `executionPlanSummary` (“执行要点”) | `executionBlock()` | `DecisionResultVO.executionPlanSummary` | **`DecisionResultMapper` SQL concat** | **derived:** `valid_period` ∥ `invalid_condition` | Not stored; **not** a full execution playbook; **not** a substitute for plan narrow fields | truth (derived) | keep derived or split UI | **Do not** equate with “完整执行计划解释”; s4 顶部边界文案提示用户勿将本 tab 视为完整方案 | P2 | YES | Step 3 |
| Dashboard / Execution plan | `entryZone`, `stopLoss`, `leverageSuggestion`, `positionSuggestion` | `executionBlock()` kv grid | `DecisionResultVO` (same names) | same mapper chain as `recommendedAction` | **`tm_execution_plan`** (same **latest-plan** row) | Persisted narrow fields; shown in s4 grid with `recommendedAction` / `takeProfitRules` from **one** plan row | truth | keep truth | Same row as `takeProfitRules`; do not over-read grid as entire playbook | P2 | YES | Step 3 |
| Dashboard / Execution plan | `takeProfitRules` | `executionBlock()` **independent multi-line block** | `DecisionResultVO.takeProfitRules` | same latest-plan subquery as `entryZone` / `stopLoss` | **`tm_execution_plan.take_profit_rules`** | **Must not** concatenate with `executionPlanSummary`; **plan-table** semantics | truth | keep truth | PHASE5 contract: multi-line primary form; empty → **—** | P2 | YES | Step 3 |
| Dashboard / Layer2 Tile | `aiConflict` label/score | `resolveAiConflict()` | `DecisionResultVO.aiConflictLevel` / `aiConflictScore` | `DecisionServiceImpl.getLatestDecisionResults()` + `DecisionResultMapper.findLatestDecisionResultsJoined()` | `tm_decision_result.ai_conflict_level`, `ai_conflict_score` | Decision read-model persisted columns are truth | fallback + heuristic on legacy rows | backend truth only | Keep `readModelFallbackReason` as migration signal, then remove FE derivation path | P0 | YES | Step 3 |
| Dashboard / Layer2 Tile | `confused` / `confusedScore` | `resolveConfusedState()` | `DecisionResultVO.confusedScore` (and optional `isConfused`) | same as above | `tm_decision_result.confused_score` | Persisted decision confused score is truth for dashboard decision row | fallback when missing | backend truth only | Stop FE defaulting to semantic guess; only display backend value or explicit unavailable state | P0 | YES | Step 3 |
| Dashboard / Layer2 Tile | `readModelTruthStatus` / read-model tile (Dashboard; not Push `recheckStatus`) | `resolveReadModelTruthTilePresentation()` | `DecisionResultVO.readModelTruthStatus`, `readModelFallbackReason` | `DecisionServiceImpl` summary/detail chains → `annotateReadModelFallback()` | `tm_decision_result` persisted columns → completeness boundary | FULL/PARTIAL boundary from backend; Push/recheck row truth stays on Review aggregate | truth | keep truth | Maintain boundary semantics; forbid FE inventing review-like recheck conclusions; Review Push `recheckStatus` remains Review matrix | P0 | YES | Step 3 |
| Dashboard / Layer2 Tile | `openingSuggestion` | `resolveOpeningSuggestion()` | currently none in `DecisionResultVO` | FE-only placeholder; no backend assembly | none | No backend truth yet | fallback | explicit backend field or explicit N/A contract | Keep "pending backend" text; forbid FE composition as truth | P0 | YES | Step 3 |
| Dashboard / Layer1 Stats | `reverseSignalCount` | `countReverseSignals()` | `LightSystemStatusVO.reverseSignalCount` (`systemStatus`) | `DecisionServiceImpl.getLightSystemStatus()` → `DecisionResultMapper.countOpenSymbolsWithReverseSignal()` | `tm_real_position` OPEN + `tm_decision_result` latest bias per symbol | Backend aggregate: open symbols whose latest bias opposes position side | truth | keep truth | Keep SQL/VO semantics aligned; forbid FE inferring this count | P1 | YES | Step 3 |
| Dashboard / Layer1 Stats | `pendingCount` | `countPending()` | `LightSystemStatusVO.pendingCount` (`systemStatus`) | `DecisionServiceImpl.getLightSystemStatus()` → `PushSnapshotMapper.countPendingRecheckBacklog()` | `tm_push_snapshot` (mapper-defined backlog filter) | Backend aggregate pending push/recheck backlog count | truth | keep truth | Keep backlog definition per mapper/service; forbid FE inferring backlog size | P1 | YES | Step 3 |
| Dashboard / Layer1 Stats | `confusedCount` | `countConfused()` | `LightSystemStatusVO.confusedCount` (`systemStatus`) | `DecisionServiceImpl.getLightSystemStatus()` → `AssetStateMapper.countSymbolsWhereConfusedScorePositive()` | `tm_asset_state` (`confused_score` > 0 symbols) | Global confused-symbol count from asset state (distinct from per-row tile `confusedScore`) | truth | keep truth | Keep asset-state aggregation semantics; forbid FE inferring this global count | P1 | YES | Step 3 |
| Dashboard / Layer1 Card | `hotReset*` (`hotResetFired`, `hotResetSymbol`, `hotResetTriggerType`, `hotResetTriggerValue`, `hotResetTime`) | `readHotReset()` | `LightSystemStatusVO.hotReset*` | `DecisionServiceImpl.getLightSystemStatus()` -> `AssetStateService.findLatestHotResetSnapshot()` | `tm_asset_state.hot_reset_flag`, `hot_reset_trigger_type`, `hot_reset_trigger_value`, `hot_reset_time`, `symbol` | Global latest row semantics, not analysis-scoped event log | truth | keep current global semantics explicit | Maintain semantics note: "latest row snapshot" not "this analysis event" | P0 | NO | Step 3 |
| Dashboard / Layer1 Card | `missedValidOpportunityCount` | `missedOpportunityCount()` | `LightSystemStatusVO.missedValidOpportunityCount` | `DecisionServiceImpl.getLightSystemStatus()` | `tm_missed_opportunity.biz_date` count | Daily count by biz_date is current truth | truth | keep truth | Keep timezone/date semantics documented; no FE guess | P0 | NO | Step 2 |
| Dashboard / Layer1 Alerts | alert level/message/time | `renderLayer1()` alert list | `/api/dashboard/refresh alerts[]` (from `MonitorAlertDO`) | `DashboardController.refreshDashboard()` + `MonitorServiceImpl.getRecentAlerts()` + `MonitorAlertMapper.selectRecent()` | `tm_monitor_alert.alert_level`, `alert_message`, `created_at`, `status` | Alert fact rows from monitor table | truth | keep truth | Preserve OPEN/SUPPRESSED semantics in display copy only | P0 | NO | Step 2 |

## Review Truth Matrix

> **Aggregate boundary:** `ReviewAggregateVO` does not embed a full `ReviewStateVO`-style raw review row (authoritative fields remain under `/api/review/state` and `/api/review/save`). `governanceSummary` and parts of `reviewClosure` are **read-only projections** sourced from `tm_review_result`. `GET /api/review/aggregate/{analysisId}/summary` returns `ReviewAggregateSummaryVO`, which **omits** `governanceSummary`.

> **`tm_rule_version_log` (review save path):** Each successful `/api/review/save` **appends** one row (`change_category`=`REVIEW_FEEDBACK_SAVED`) for **append-only audit**, correlating with the run's `rule_version`. **Not** rule-config mutation and **not** automatic publication of a new rule version.

> **Market environment (review aggregate):** `ReviewAggregateVO.marketEnvironment.*` is sourced from `tm_market_environment_snapshot` aligned by `analysis_id`, consistent with Dashboard detail's snapshot-first read contract.

> **Structured top3 on Review aggregate (current):** Full `GET /api/review/aggregate/{analysisId}` includes **`evidenceTopItems`** and **`scoreTopItems`**, same **`analysis_id`** brief contracts as Dashboard detail (`EvidenceBriefVO` / `ScoreBriefVO`). Review page (`review-page.js`) surfaces **「结构化证据（前3条）」** and **「评分明细（前3条）」** as **read-only** blocks; decision-row **`evidenceSummary` / `reviewReasons`** remain in the **“解释与证据（结构化速览）”** / raw area and are **not** deprecated.

`adjustment_suggestion` 的推荐填写格式详见 `PROJECT_SPEC.md`「（五）adjustment_suggestion 填写约定」小节；可选填写的错误阶段词汇见同文件「（四）复盘归因分类标准」。

| Page Module | Page Display Field | FE Read Function / Variable | API / VO Field | Backend Assembly Location | DB Field / Source Table | Truth Source Note | Current State | Phase 5 Target State | Action Guidance | Priority | FE Inference Ban | Owner Step |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Review / Decision | Decision summary core fields | `renderDecision()` | `ReviewAggregateVO.ReviewDecisionSummary.*` | `ReviewAggregateServiceImpl.toDecisionSummary()` | `tm_decision_result.*` | Decision row snapshot at decision create time | truth | keep truth | Continue raw + structured dual display, no semantic rewrite | P0 | NO | Step 2 |
| Review / Decision | `explanationJson` | `formatExplanationJson()` + raw block | `decision.explanationJson` | same as above | `tm_decision_result.explanation_json` | Raw JSON text from DB is truth | truth | keep truth | Only pretty-print allowed, no FE synthesis | P0 | YES | Step 2 |
| Review / Decision | `reviewReasons` | `parseReviewReasons()` + raw block | `decision.reviewReasons` | same as above | `tm_decision_result.review_reasons` | Raw JSON/string reasons from DB is truth | proxy (parser output) + truth (raw) | raw truth + parser as read helper | Preserve parser as display helper only, not new source | P0 | YES | Step 2 |
| Review / Decision | `evidenceSummary` | `toListByDelimiters()` + raw | `decision.evidenceSummary` | same as above | `tm_decision_result.evidence_summary` | Raw summary text is truth | proxy (list split) + truth (raw) | raw truth + helper projection | Do not treat split list as canonical structure | P1 | YES | Step 2 |
| Review / Decision | `assetStateSnapshot` | `parseJsonObject()` + raw | `decision.assetStateSnapshot` | same as above | `tm_decision_result.asset_state_snapshot` | Decision-time asset snapshot string is truth | proxy preview + truth raw | keep truth | Keep preview bounded to read-only visualization | P0 | YES | Step 2 |
| Review / Push-Recheck | Push snapshot fields | `renderPushRecheck()` | `pushRecheck[].push.*` | `ReviewAggregateServiceImpl.toPushSummary()` | `tm_push_snapshot.*` | Push snapshot row is truth | truth | keep truth | No FE deduction from Push to decision outcome | P0 | NO | Step 2 |
| Review / Push-Recheck | Recheck fields (`recheckStatus`, `failReasonJson`, `current*`) | same | `pushRecheck[].rechecks.*` | `ReviewAggregateServiceImpl.toRecheckSummary()` | `tm_push_recheck_log.*` | Recheck log rows are truth | truth | keep truth | JSON pretty-print only | P0 | NO | Step 2 |
| Review / Missed | Missed rows + reason view | `renderMissed()` | `missed[].reasonJson`, `missed[].reasonView` | `ReviewAggregateServiceImpl.toMissedList()` + `MissedReasonViewParser.parse()` | `tm_missed_opportunity.reason_json` (+ parsed projection) | Raw `reason_json` is truth; `reasonView` is read projection | truth + proxy | keep dual view, raw as canonical | Do not overwrite raw semantics with parsed hints | P0 | YES | Step 2 |
| Review / Hot Reset | Hot reset current-row block | `renderHotReset()` | `hotReset.*` | `ReviewAggregateServiceImpl.toHotReset()` | main: `tm_asset_state.*`; relation hint: `tm_hot_reset_event.*` | Current-row snapshot semantics explicitly separated from event log | truth | keep explicit boundary | Keep semantic explanation fields as contract guardrail | P0 | NO | Step 2 |
| Review / Alerts | Alert facts/status | `renderAlerts()` | `alerts[].*` | `ReviewAggregateServiceImpl.toAlertList()` + `MonitorAlertMapper.listByAnalysisId()` | `tm_monitor_alert.*` | Alert row facts are truth | truth | keep truth | No FE-level status reclassification | P0 | NO | Step 2 |
| Review / Save-State | review save/state fields (`errorType`, `actualOutcome`, `adjustmentSuggestion`) | `applyReviewState()` + `wireSaveOnce()` | `ReviewStateVO.*` via `/api/review/state` and `/api/review/save` | `ReviewController` + `ReviewServiceImpl.saveOrUpdate()/getStateByAnalysisId()` | `tm_review_result.error_type`, `actual_outcome`, `adjustment_suggestion`, timestamps | User-authored review result row is truth | truth | keep truth | Preserve upsert semantics and avoid coupling to aggregate read-model | P0 | NO | Step 2 |

## Frontend Inference Ban List (Frozen)

These fields must not be inferred by frontend as business truth after Phase 5 convergence:

- Dashboard: `aiConflict*`, `confused*`, `recheckStatus`, `openingSuggestion`, `reverseSignalCount`, `pendingCount`, `confusedCount`
- Review: `explanationJson`, `reviewReasons`, `evidenceSummary`, `assetStateSnapshot`, `missed.reasonJson`

Rule:

- Frontend may format, parse, and summarize for readability.
- Frontend must not synthesize new business semantics when backend truth is absent.

## Third Battleground Decision

No third page is included in Step 1.

Reason:

- `Dashboard` and `Review` already cover all required field families.
- Push/Recheck, Missed, Hot Reset, Alerts, and review save/state are all contained under `Review` or `Dashboard` API chain.
- Adding extra page scope now increases risk of scope creep without increasing Step 1 contract value.

## Step 2 / Step 3 Handoff

- Step 2 (account risk truth upgrade): prioritize rows tagged `Owner Step = Step 2`, especially Review Push-Recheck and review save/state consistency checks.
- Step 3 (Dashboard de-fallback): prioritize rows tagged `Owner Step = Step 3`, especially Dashboard fallback/heuristic rows marked `P0/P1`.

## Minimal Verification Checklist

A reviewer can treat this document as executable if all checks pass:

1. Every critical page field family has a row with FE path, VO/API path, backend assembly, and DB source.
2. Every row has a current state and Phase 5 target state.
3. Every fallback/proxy row has explicit action guidance.
4. FE inference-ban fields are explicitly listed and tied to matrix rows.
5. Step ownership is labeled (`Step 2` or `Step 3`) for direct follow-up execution.
