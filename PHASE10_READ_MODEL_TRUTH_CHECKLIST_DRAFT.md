# Phase 10 — 读模型真值最小对照清单（草案骨架）

## 1. 本轮范围

- 真值矩阵来源：`PHASE10_STEP1_READ_MODEL_TRUTH_MATRIX.md`
- 本轮只核对 3 个接口：`/api/dashboard/summary`、`/api/dashboard/detail`、`/api/push/recheck/ops/overview`
- 本轮只做清单与首轮映射，**不改代码**（**冻结基线**：对照表 **33** 行 = **32** 条语义核对行 + **1** 条文档碎片归并声明）

## 2. 核对对象

- `GET /api/dashboard/summary`
- `GET /api/dashboard/detail`
- `GET /api/push/recheck/ops/overview`

## 3. 对照表骨架

**快速收尾**：已停止向标点、箭头、括号、引号短语、单词/单元格碎片级追加对照行。下表保留 **「三 GET JSON ↔ 矩阵语义」** 粒度；先前展开的微粒度行 **约 200 条** 自本清单移除，**不**再逐条维护，**不**改变已得出的实体真值映射结论。

| 真值矩阵条目 | 规则摘要 | 对应接口 | 对应 VO/字段路径 | 当前实现状态 | 备注 |
|---|---|---|---|---|---|
| §3 字段矩阵 — `DecisionResultVO` 真值在列表与详情中的暴露 | 同一决策读模型字段（含 §3 各业务列）应由列表项与详情项同源承载，不重复捏造语义 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DashboardSummaryResponseVO.decisions[]`（元素 `DecisionResultVO`）<br>`DashboardDetailResponseVO.decision` | 已对齐 | **同源组装链**：`DecisionServiceImpl` 中 `getLatestDecisionResults` 与 `getLatestDecisionResultBySymbol` 均经 `findLatest*Joined` 取库内决策行后，执行同一套后置步骤（行情叠加 → `loadOpenPositionMap` 持仓叠加 → `annotateReadModelFallback`）。**非**两套推导逻辑；**仅**查询维度不同（全局最新 N 条 vs 单标的最新一条）。 |
| §2.3 前端禁止推断 — 与「无字段即业务结论」相对 | `detail` 须显式传入标的；`symbol` 缺失或全空白时拒绝请求，避免无标的后由前端补故事 | `GET /api/dashboard/detail` | 请求参数 `symbol` → `DashboardDetailResponseVO.symbol` | 已对齐 | `DashboardController.normalizeSymbol`：`null`/blank → 400 BAD_REQUEST |
| §3「今日遗漏计数」— 零值占位口径（类比） | 聚合无数据时用 **0** 或 **非 null 空壳对象** 表达「无记录」，不把缺字段当成可推断的业务态 | `GET /api/push/recheck/ops/overview` | `PushRecheckOpsOverviewVO.auditSummary`（如 `auditCount`）<br>`PushRecheckOpsOverviewVO.latestReplaySummary` | 非本轮接口范围 | Step1 真值矩阵 **未逐域覆盖** Push Recheck；`docs/overview-api-contract.md` §5 为本接口 **契约层** 的零对象/空列表/非 null 形状。与矩阵 §2.2/§3「技术占位、不把缺失当业务结论」**方向相容**，但 **非** 矩阵条文与 ops 字段逐条等同。同表 **`auditSummary`**、**`latestReplaySummary`** 两行已核对嵌套聚合形状。 |
| §2.1 `readModelTruthStatus` / `readModelFallbackReason`；§3「读模型完整性」 | 读模型边界信号由后端给出 `FULL`/`PARTIAL` 与 `LEGACY_MISSING:*`；前端仅展示提示，不参与决策 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.readModelTruthStatus`<br>`DecisionResultVO.readModelFallbackReason`（经 `decisions[]` / `decision`） | 已对齐 | **已进入**：`summary`（`decisions[]` 内）、`detail`（`decision`），均由 `annotateReadModelFallback` 写入。**未进入**：`overview`（响应树中无 `DecisionResultVO`）。矩阵 §2.1 对两字段的验收 **仅** 落在 Dashboard 决策体；`overview` **非本轮** 承接范围。矩阵 §2.1 L25/L26 与本行合并表述。 |
| §2.1 后端必须给真值 — 嵌套聚合对象；§3 字段可追溯 | `latestReplaySummary` 内计数与状态应由后端聚合填充；无匹配 replay 源时返回零计数对象而非省略嵌套 | `GET /api/push/recheck/ops/overview` | `PushRecheckOpsOverviewVO.latestReplaySummary` → `PushRecheckReplaySummaryVO`（如 `totalCount`、`successCount`、`latestExecutionStatus` 等） | 已对齐 | `PushRecheckServiceImpl.summarizeReplayByDispatch`：空日志时仍 `new PushRecheckReplaySummaryVO()` 并置计数为 0 |
| §2.1 `hasOpenPosition` / `position*`；§3「持仓状态」 | 持仓真值仅来自 `tm_real_position`；无持仓行时 `hasOpenPosition=false` 且 `positionStatus=null`，不把缺行推断为业务收盘类结论 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.hasOpenPosition`<br>`DecisionResultVO.positionStatus` 及 `positionSide`、`avgOpenPrice` 等持仓相关字段 | 已对齐 | 与第 1 行同源链：`loadOpenPositionMap`（`realPositionMapper.findOpenPositions`）按标的匹配；无行分支显式 `setHasOpenPosition(false)`、`setPositionStatus(null)`。**未进入**：`ops/overview`（响应树中无 `DecisionResultVO`）。矩阵 §2.1 L27/L28 与本行同源链合并核对，不另分列。 |
| §3「价格信息」 | `latestPrice` / `priceChangePct` 为运行时行情真值；获取失败不捏造数值，仅不填充或保持未覆盖 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.latestPrice`<br>`DecisionResultVO.priceChangePct`<br>`DecisionResultVO.priceUpdateTimeMs` | 已对齐 | `safeFetchQuote` → `MarketQuoteClient.fetch24hTicker`；`snapshot == null` 时不写入价格字段。矩阵 §2.2「暂无」类展示由前端处理。**未进入**：`ops/overview`。 |
| §3「当前方向」 | `marketBiasHierarchy` 来自 `tm_decision_result` 持久化列；缺失时仅可空/占位展示，禁止把空值推断为方向结论 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.marketBiasHierarchy` | 已对齐 | `findLatest*Joined` SQL 映射 `d.market_bias_hierarchy`。`annotateReadModelFallback` 的 `LEGACY_MISSING:*` 集合**未**包含 `market_bias_hierarchy`（`PARTIAL` 仍可能由其它列触发）。**未进入**：`ops/overview`。 |
| §3「开仓建议」 | `isWorthOpening` / `validPeriod` / `invalidCondition` 为持久化真值；可空展示，禁止把字段缺失推断为「可开仓/暂停」等业务结论 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.isWorthOpening`<br>`DecisionResultVO.validPeriod`<br>`DecisionResultVO.invalidCondition` | 已对齐 | `findLatest*Joined` 映射 `tm_decision_result` 列；`annotateReadModelFallback` 将空白的 `valid_period`、`invalid_condition` 计入 `LEGACY_MISSING:*`（`is_worth_opening` **未**列入该缺失集合，与矩阵「可空展示」一致）。**未进入**：`ops/overview`。 |
| §3「冲突等级」 | `aiConflictLevel` / `aiConflictScore` 来自持久化列；缺失时仅提示待补，禁止前端推断等级 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.aiConflictLevel`<br>`DecisionResultVO.aiConflictScore` | 已对齐 | SQL 映射 `d.ai_conflict_level` / `d.ai_conflict_score`；`annotateReadModelFallback` 对空白 `ai_conflict_level` 或 `null` `ai_conflict_score` 计入 `LEGACY_MISSING:*`。**未进入**：`ops/overview`。 |
| §3「困惑分」 | `confusedScore` 来自 `tm_decision_result.confused_score`；缺失仅占位，禁止推断 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.confusedScore` | 已对齐 | SQL 映射 `d.confused_score`；`annotateReadModelFallback` 对 `null` `confused_score` 计入 `LEGACY_MISSING:*`。**未进入**：`ops/overview`。 |
| §2.1 + §3「今日遗漏计数」 | `missedValidOpportunityCount` 由后端按 `tm_missed_opportunity`（`biz_date`）聚合；缺失显示 **0**（技术占位），不把缺字段当成业务结论 | `GET /api/dashboard/summary` | `DashboardSummaryResponseVO.systemStatus` → `LightSystemStatusVO.missedValidOpportunityCount` | 已对齐 | `DecisionServiceImpl.getLightSystemStatus`：`missedOpportunityMapper.countByBizDate(LocalDate.now())` 写入计数。**未进入**：`detail`（`DashboardDetailResponseVO` 无 `systemStatus`）；**未进入**：`ops/overview`。矩阵 §2.1 L29 第一子句（`missedValidOpportunityCount`）已含于本行。 |
| §2.1 + §3「Hot Reset 最新态」 | `hotReset*` 由后端聚合自 `tm_asset_state` 最近记录；无记录时 `hotResetFired=false`，不以前端推断补「已触发」故事 | `GET /api/dashboard/summary` | `DashboardSummaryResponseVO.systemStatus` → `LightSystemStatusVO.hotResetFired`、`hotResetSymbol`、`hotResetTriggerType`、`hotResetTriggerValue`、`hotResetTime` | 已对齐 | `assetStateService.findLatestHotResetSnapshot()`；无 `hot_reset_time` 时仅 `setHotResetFired(false)`。注释与矩阵 §2.1「全局快照」语义一致。**未进入**：`detail`；**未进入**：`ops/overview`。矩阵 §2.1 L29 第二子句（`hotReset*`）已含于本行。 |
| §2.1 后端必须给真值 — `explanationJson` / `reviewReasons` / `assetStateSnapshot` | 持久化解释与复核快照须由后端回填；空白时纳入 `LEGACY_MISSING:*`，前端仅展示提示 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.explanationJson`<br>`DecisionResultVO.reviewReasons`<br>`DecisionResultVO.assetStateSnapshot` | 已对齐 | `findLatest*Joined` 映射 `tm_decision_result` 列；`annotateReadModelFallback` 对空白 `explanation_json`、`review_reasons` 或空白 `asset_state_snapshot` 计入 `LEGACY_MISSING:*`。**未进入**：`ops/overview`。 |
| §2.1 后端必须给真值 — 嵌套聚合对象（`auditSummary`）；§3 字段可追溯（类比） | `auditSummary` 内计数与最新审计元数据应由后端聚合填充；无审计记录时仍返回嵌套对象（如计数为 **0**），不把「缺嵌套」留给前端推断 | `GET /api/push/recheck/ops/overview` | `PushRecheckOpsOverviewVO.auditSummary` → `AuditSummary`（如 `auditCount`、`latestAuditTime` 等） | 已对齐 | `getOpsOverview` 始终 `setAuditSummary(buildAuditSummary(...))`；`buildAuditSummary` 对空列表仍 `new AuditSummary()` 且 `setAuditCount(0)`，不省略 `auditSummary`。与第 5 行 `latestReplaySummary` 同属矩阵 §2.1「嵌套聚合」口径。**未进入**：`summary` / `detail`。 |
| §2.2 前端仅允许技术占位 | 展示 `—` / `待后端字段` / `待后端复核状态`；展示 `readModelFallbackReason` 提示；JSON 仅格式化不改变语义 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （无单独「占位策略」字段；依赖各 VO 已暴露真值 + 前端渲染） | 非本轮接口范围 | 矩阵 §2.2 约束 **浏览器端展示**；本清单只核对三接口 **后端 JSON** 与 §2.1/§3 字段对齐，**不**验收前端页面是否仅做技术占位。矩阵 §2.2 各 bullet 见矩阵原文 §2.2。 |
| §5 给模块 2 的交接前提 — 合并叙事（§5 全文） | 1) `readModelTruthStatus` 可稳定返回；2) 无持仓时不再输出误导性业务状态；3) **测试**覆盖上述边界，不允许回到「前端兜业务语义」 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | （整段前提意图；非单字段路径） | 非本轮接口范围 | 矩阵 §5 为 **模块 2 开工前的交接前提**；本清单只读核对三 **GET** 与 §2.1/§3 字段对齐，**不**将 §5 全文当作逐条接口验收标准。与读模型、持仓等字段行并行不重复。三条编号前提见本表 **§5 前提 1～3**。**未进入**：`ops/overview` 专项。 |
| §4 本轮不做清单 — 不拆 Dashboard summary/detail 接口 | Step1 冻结「不拆合」summary/detail；读模型真值仍按 **现两路由** 分别核对 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | （范围/形态约束，非单字段路径） | 非本轮接口范围 | 矩阵 §4 为 **Phase 接口形态** 冻结；本清单只核对 **当前** `summary`/`detail` 响应 JSON，**不**验收是否拆层或合并路由。 |
| §4 本轮不做清单 — 不做 Dashboard / Review 页面大改版 | 页面不大改版与读模型字段契约可并行；字段真值仍以接口为准 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （无；页面非 JSON 契约） | 非本轮接口范围 | 矩阵 §4 约束 **浏览器端页面**；本清单只核对三 **GET** 响应体，**不**验收 `dashboard.html` / `review.html` 等是否大改版。 |
| §4 本轮不做清单 — 不重构 Review 聚合模型 | Review 域聚合/schema 形态保持 Step1 冻结；与 Dashboard 决策体、Push Recheck ops 边界分离 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （范围约束；非单 VO 路径） | 非本轮接口范围 | 矩阵 §4 为 **聚合模型** 工程冻结；本清单只核对 **现 VO** 与 §2.1/§3 字段对齐，**不**验收是否重构 Review 读模型或表结构。 |
| §4 本轮不做清单 — 不新增运维卡片与规则平台能力 | Step1 冻结不扩运维卡片/规则平台；读模型真值核对仍以 **现** Dashboard 与 Push Recheck ops 响应为界 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （产品/能力范围约束，非单字段路径） | 非本轮接口范围 | 矩阵 §4 为 **能力边界** 冻结；本清单只核对三 **GET** 既有 JSON 形状与 §2.1/§3 字段，**不**验收是否新增运维卡片或规则平台。**未要求**：因不新增能力而改动上述三路由字段契约。 |
| §4 本轮不做清单 — 不做大规模 schema 变更 | 表结构不大改与接口字段真值核对可并行；Step1 仍以 **当前** `schema.sql` 与映射为准 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （库表/schema 工程约束，非单 VO 路径） | 非本轮接口范围 | 矩阵 §4 为 **schema** 工程冻结；本清单只核对 **现** 表字段经 MyBatis/服务映射到 VO 的读模型真值，**不**将「是否大规模改表」纳入本轮三 GET 验收。**未要求**：为 schema 冻结单独证明三接口外扩字段。 |
| §1 当前盘点结论 — 稳定主链 / 读模型缺口 / 语义边界风险 | 文档层总述：`tm_*` 主链存在、`LEGACY_MISSING:*` 与前端 fallback 缺口、语义边界风险；**非**逐字段 API 契约 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （矩阵 §1 叙事总述，非单字段路径） | 非本轮接口范围 | 矩阵 §1 三条为 **仓库盘点结论**；本清单按行核对的是三 **GET** 与 §2.1/§3 的字段级映射，**不**将 §1 全文当作逐条接口验收标准。与第 4、6、16 等行对 **具体** 边界规则的核对 **并行不重复**。§1 三条分条为本表上文三条「§1 当前盘点结论」行。 |
| §2.3 前端禁止推断 — 禁止把「字段缺失」推断为业务结论（§2.3 首条；与第 2/6 行等同向） | 无后端真值支撑时**不**得把「缺字段」补成业务结论（例：无持仓行 => `CLOSED`）；与第 **2** 行 detail 必传、第 **6** 行无持仓分支并列落实后端侧 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （跨 §2.1/§3 多条 VO 路径；见表中各 DecisionResult / `LightSystemStatusVO` / ops 嵌套行） | 非本轮接口范围 | 后端侧关键空值/占位已在决策体与系统状态等字段行按只读核对；矩阵 §2.3 **首条** 仍属 **前端推理禁令**，**不**纳入本轮三 GET JSON 单条验收。本行钉矩阵 §2.3 L39。 |
| §2.3 前端禁止推断 — 禁止用启发式规则替代后端冲突等级、复核状态、开仓建议真值 | 冲突等级、复核态、开仓建议须以持久化/聚合真值为准；**不**得在前端用启发式规则伪造或覆盖上述语义 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.aiConflictLevel` / `aiConflictScore`、`reviewReasons` 等复核相关、`isWorthOpening` / `validPeriod` / `invalidCondition`（经 `decisions[]` / `decision`） | 非本轮接口范围 | 矩阵 §2.3 L40 约束 **浏览器端推理**；第 **9～11**、**14** 行已按三 GET **JSON** 核对后端是否暴露对应真值。**不**验收前端是否仍用启发式替代展示或联动。**未进入**：`ops/overview`（无 `DecisionResultVO`）。 |
| §2.3 前端禁止推断 — 禁止把 fallback 文案作为真实业务状态回写或联动判断 | `readModelFallbackReason` 等提示仅作展示；**不**得当作可回写或可触发联动的业务态 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.readModelFallbackReason` 等（经 `decisions[]` / `decision`） | 非本轮接口范围 | 矩阵 §2.3 L41；与 **§2.2 合并行**同为 **前端行为** 边界；本清单只核对三 GET 响应体字段是否存在及后端写入语义，**不**验收前端是否将 fallback 文案参与回写/路由联动。**未进入**：`overview`。 |
| §1 当前盘点结论 — 稳定真值主链（`tm_*` 表链） | 盘点结论一：`tm_decision_result` + `tm_analysis_run` + `tm_missed_opportunity` + `tm_asset_state` + `tm_real_position` 构成稳定主链 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （矩阵 §1 叙事条目，非单字段路径） | 非本轮接口范围 | 矩阵 §1 L15～16 **单条**结论；本清单只核对三 **GET** JSON 与 §2.1/§3 字段映射，**不**将「主链表集合存在」本身作为三接口逐字段验收。**未**替代表中决策体与系统状态各字段核对行。与上文 §1 合并叙事行 **同源分条**。 |
| §1 当前盘点结论 — 读模型完整性缺口（`LEGACY_MISSING:*`） | 盘点结论二：主要缺口在读模型完整性；`DecisionServiceImpl` 输出 `LEGACY_MISSING:*`，前端仍有多处 fallback | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （矩阵 §1 叙事条目；与 `readModelFallbackReason` 等字段行 **关联** 但非同一验收单元） | 非本轮接口范围 | 矩阵 §1 L16～17；读模型边界与解释类字段行已按三 GET **JSON** 核对后端边界信号与缺失集合写入；本行仅钉 §1 **盘点叙事**，**不**重复做前端 fallback 清单审计。**未进入**：单独验收「前端是否仍有多处 fallback」（属 §2.2/浏览器侧）。与上文 §1 合并叙事行 **同源分条**。 |
| §1 当前盘点结论 — 语义边界风险（缺省推断） | 盘点结论三：风险不在「接口不可用」，而在语义边界不够硬；前端仍可能从缺省字段推断业务状态 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （矩阵 §1 叙事条目；与 §2.3 各条 **同向** 但分层不同） | 非本轮接口范围 | 矩阵 §1 L17～18；与 §2.3、§2.2 相关行 **分层**：本行钉 §1 **风险定性**，**不**将「前端是否仍推断」纳入本轮三 GET JSON 单条验收。与上文 §1 合并叙事行 **同源分条**。 |
| §5 给模块 2 的交接前提 — 前提 1：`readModelTruthStatus` 可稳定返回 | 与矩阵 §5 L75 一致：`FULL`/`PARTIAL` 稳定返回 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.readModelTruthStatus`（经 `decisions[]` / `decision`） | 已对齐 | 与本表「读模型完整性」条同源；`annotateReadModelFallback` 写入。**未进入**：`ops/overview`。 |
| §5 给模块 2 的交接前提 — 前提 2：无持仓时不再输出误导性业务状态 | 与矩阵 §5 L76 一致：无持仓行不得输出例如 `CLOSED` 等误导性结论 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail` | `DecisionResultVO.hasOpenPosition`、`positionStatus`（同「持仓状态」条） | 已对齐 | 与本表「持仓状态」条同源；无行分支 `hasOpenPosition=false`、`positionStatus=null`。**未进入**：`ops/overview`。 |
| §5 给模块 2 的交接前提 — 前提 3：测试覆盖上述边界 | 自动化测试与回归门禁，不允许回到「前端兜业务语义」 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （工程门禁；非单字段路径） | 待后续实现 | 矩阵 §5 L77；本清单只读核对 **JSON** 与字段对齐，**不**在本轮做全量测试审计，故标 **待后续实现**。 |
| **【归并声明】矩阵文档表达层微粒度锚点（已不再逐条拆解）** | 含：版式空行、水平线 `---`、Markdown 表语法分隔行与逐格单元格、章节/小节标题字面、列表符、标点/括号/箭头/引号/词级与示例子句拆分等 | `GET /api/dashboard/summary`<br>`GET /api/dashboard/detail`<br>`GET /api/push/recheck/ops/overview` | （无独立 JSON 路径） | 非本轮接口范围 | **收口结论**：上述条目**不增加**「三 GET JSON vs `PHASE10_STEP1_READ_MODEL_TRUTH_MATRIX.md` 正文语义」的有效核对信息量；已从本清单移除 **约 200 条** 微粒度行。实体真值以本表 **第 1～32 条** 为准；矩阵细节以矩阵文件 **章节与 §3 表整行** 为准。**禁止**再在本文件按标点/箭头级追加行。 |

## 3.1 冻结与归并结论

- **有核对价值的部分**：上表第 1～32 条 — `DecisionResultVO` / `LightSystemStatusVO` / ops 嵌套聚合等与三 GET 响应字段直接相关的语义映射，以及 §4/§5/§1/§2.3 中 **工程边界与前端禁令叙事**（标「非本轮接口范围」者 = 明确本清单**不验**浏览器侧或纯文档命题）。
- **文档碎片层（已归并）**：矩阵中的排版元素、标点、箭头、括号内短语、§3 表 **按列按格** 拆出的条目等 — **不再**逐条列入；真值核对价值已由第 1～32 条与矩阵原文覆盖。
- **基线状态**：本文件可作为 Phase 10 Step 1 读模型对照清单 **最终冻结草案**；后续除非矩阵正文或接口契约变更，否则**不**扩张行数。

## 4. 当前已知锚点

- `/api/dashboard/summary` → `DashboardSummaryResponseVO`
- `/api/dashboard/detail` → `DashboardDetailResponseVO`
- `/api/push/recheck/ops/overview` → `PushRecheckOpsOverviewVO`
- `auditSummary` → `PushRecheckOpsOverviewVO.AuditSummary`
- `latestReplaySummary` → `PushRecheckReplaySummaryVO`
- ops overview 契约文档：`docs/overview-api-contract.md`

## 5. 本轮输出要求

- 先清单，后逐项核对
- 不改代码
- 不补新接口
- 不做全量审计
- 本清单已按 **§3.1** 冻结口径收尾，不再追加微粒度行

本轮累计 **33** 条对照行（**32** 条语义核对 + **1** 条归并声明）已完成只读核对：**16** 条已对齐，**16** 条属非本轮接口范围（内含归并声明 **1** 条），**1** 条待后续实现；本轮未发现必须立即改代码的读模型真值偏差。

本结论仅覆盖 `/api/dashboard/summary`、`/api/dashboard/detail`、`/api/push/recheck/ops/overview` 三个 GET 接口，不代表全系统读模型已完成真值审计。
