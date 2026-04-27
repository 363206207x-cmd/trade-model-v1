# Phase 10 Step 1 - Read Model Truth Matrix

## 文档目的

冻结 Phase 10 Step 1 的读模型真值边界，防止继续把业务语义放在前端 fallback 中扩散。

本轮只做边界收敛，不做 Dashboard summary/detail 拆层。

---

## 1) 当前盘点结论

基于当前仓库代码（`DecisionServiceImpl`、`DecisionResultVO`、`LightSystemStatusVO`、`DashboardController`、`dashboard.html`、`review.html`、`review-page.js`）：

- 稳定真值主链已存在：`tm_decision_result` + `tm_analysis_run` + `tm_missed_opportunity` + `tm_asset_state` + `tm_real_position`；`tm_push_snapshot` 已纳入 system 层聚合主链（pending backlog，对应 `pendingCount`）。
- 主要缺口集中在读模型完整性，且主要指 DecisionResult 行级读模型与 `LEGACY_MISSING:*`（非指 `LightSystemStatusVO` 的 `confusedCount` / `pendingCount` / `reverseSignalCount` 未由后端聚合提供）：`DecisionServiceImpl` 会输出 `LEGACY_MISSING:*`，前端仍有多处 fallback 分支。
- 风险点不是“接口不可用”，而是“语义边界不够硬”：前端仍可能从缺省字段推断业务状态。

---

## 2) 边界规则（Step 1 冻结版）

### 2.1 后端必须给真值（Required Truth）

- `DecisionResultVO.readModelTruthStatus`：`FULL` / `PARTIAL`
- `DecisionResultVO.readModelFallbackReason`：`LEGACY_MISSING:*`（仅在 `PARTIAL` 时）
- `DecisionResultVO.hasOpenPosition`：真实持仓是否存在
- `DecisionResultVO.position*`：仅来自 `tm_real_position`（有则填，无则空）
- `LightSystemStatusVO`：`missedValidOpportunityCount` / `hotReset*` 由后端聚合直接给值；`confusedCount`（`tm_asset_state` 困惑 symbol 计数）、`pendingCount`（`tm_push_snapshot` 待复核积压）、`reverseSignalCount`（OPEN 持仓与每 symbol 最新决策方向对照）亦由后端聚合为真值，前端禁止自行推算或补缺。

### 2.2 前端仅允许技术占位（Technical Placeholder Only）

- 展示 `—` / `待后端字段` / `待后端复核状态`
- 展示 `readModelFallbackReason` 的提示文案
- JSON 文本的格式化显示（不改变语义）

### 2.3 前端禁止继续推断（Forbidden Inference）

- 禁止把“字段缺失”推断成“业务结论”（例如：无持仓行 => `CLOSED`）
- 禁止用启发式规则替代后端冲突等级、复核状态、开仓建议真值
- 禁止把 fallback 文案作为真实业务状态回写或联动判断

---

## 3) 字段矩阵（页面 -> VO -> 表 -> 来源 -> fallback 策略）

| 页面字段 | VO 字段 | 表字段/来源 | 真值类型 | fallback 策略 |
|---|---|---|---|---|
| 当前方向 | `marketBiasHierarchy` | `tm_decision_result.market_bias_hierarchy` | 后端真值 | 仅显示 `—`，禁止推断 |
| 开仓建议 | `isWorthOpening` / `validPeriod` / `invalidCondition` | `tm_decision_result` | 后端真值 | 可空展示，不可推断“可开仓/暂停” |
| 冲突等级 | `aiConflictLevel` / `aiConflictScore` | `tm_decision_result` | 后端真值 | 缺失仅提示待补，不可推断等级 |
| 困惑分 | `confusedScore` | `tm_decision_result.confused_score` | 后端真值 | 缺失仅占位 |
| 持仓状态 | `hasOpenPosition` + `positionStatus` | `tm_real_position` | 后端真值 | 无行时 `hasOpenPosition=false` 且 `positionStatus=null` |
| 价格信息 | `latestPrice` / `priceChangePct` | `MarketQuoteClient` | 后端运行时真值 | 获取失败仅展示“暂无” |
| 读模型完整性 | `readModelTruthStatus` / `readModelFallbackReason` | `DecisionServiceImpl` 计算 | 后端边界信号 | 前端仅提示，不参与业务决策 |
| 今日遗漏计数 | `missedValidOpportunityCount` | `tm_missed_opportunity` 按 biz_date 统计 | 后端真值 | 缺失显示 0（技术占位） |
| Hot Reset 最新态 | `hotReset*` | `tm_asset_state` 最近记录 | 后端真值 | 无记录显示未触发 |

---

## 4) 本轮不做清单（硬限制）

- 不拆 Dashboard summary/detail 接口
- 不做 Dashboard / Review 页面大改版
- 不重构 Review 聚合模型
- 不新增运维卡片与规则平台能力
- 不做大规模 schema 变更

---

## 5) 给模块 2 的交接前提

模块 2（Dashboard 查询层分层）开始前，至少满足：

1. `DecisionResultVO.readModelTruthStatus` 可稳定返回（`FULL/PARTIAL`）
2. 无持仓时不再输出误导性业务状态（例如 `CLOSED`）
3. 测试覆盖上述边界，不允许回归到“前端兜业务语义”
