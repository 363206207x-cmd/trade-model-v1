# OI 最小接入契约（冻结）

> **状态：** 冻结（文档契约；真接入代码须与此一致）  
> **权威：** 与市场环境冻结索引 `PHASE10_DASHBOARD_FREEZE_INDEX.md`、`PHASE5_FIELD_TRUTH_MATRIX.md`、`PROJECT_SPEC.md` 及「**市场环境快照 + run级 DQ 联合契约（冻结）**」并列；语义冲突时以 **PHASE10「联合契约」**为阶段裁判，本文仅约束 **OI 这一条增量**。

---

## 边界声明（必须保留）

**OI 第一刀 = 衍生品微观结构的最小真实附录，不是完整 OI 模块。**

本契约阶段及下一刀「最小真实接入」允许的范围内：**仅**将 OI 作为可与 Funding 对齐的 USDⓈ-M 单向量，以 **summary 附录句式**可读呈现；**不**承担独立策略引擎语义。

---

## 本阶段冻结范围（做了什么 / 没做什么）

| 冻结项 | 说明 |
| --- | --- |
| endpoint / 字段 / symbol / failure / summary 句式 / evidence 归类 / sourceType **决策矩阵** | **已冻结** |
| **真实 HTTP 接入主链** | **未冻结为已实现**——须单独 PR，且读本契约 |
| **快照表新列** | **禁止**作为 OI 第一刀 |
| **`environmentType` / `riskMode` / `leverageSuggestion`** | **禁止**由 OI 改写（第一刀） |
| **decision 主路径接线** | **禁止** |
| **OI 历史 / 变化率 / 模型** | **禁止**与本最小契约同步启动 |
| **并行 ETF / 宏观 / `tm_data_source_health` 全链** | **禁止**与本最小契约并行作为同一刀 |

---

## 1. Endpoint 与读取字段

| 项 | 冻结值 |
| --- | --- |
| HTTP | `GET` |
| Host（公开 REST） | Binance USDⓈ-M Futures：`https://fapi.binance.com` |
| Path | `/fapi/v1/openInterest` |
| Query | `symbol={BASEUSDT}`，示例 `BTCUSDT` |
| **必读字段** | JSON 响应中的 **`openInterest`**（字符串数值，解析为十进制） |
| **可选字段** | `symbol`（回显校验）、`time`（毫秒时间戳，审计对齐） |

**说明：** 最小接入**不**使用 `openInterestHist` 或任何「变化率」专用接口作为第一刀必填路径；若未来启用，须另立契约版本。

---

## 2. Symbol 映射（与现货 / Funding 同源）

**冻结规则：** 统一使用 `org.example.trademodel.market.util.BinanceUsdtSymbol.toUsdtPair(assetSymbol)`，产出与 **现货 `BinanceMarketQuoteClient`**、**Funding `BinanceUsdtMPerpFundingClient`** 一致的 `BASEUSDT`。**禁止**为 OI 另起映射表或潜规则。

---

## 3. 单位与口径（防漂移）

| 层级 | 冻结规则 |
| --- | --- |
| **真值** | 以 Binance **官方文档对 `/fapi/v1/openInterest` → `openInterest`** 的定义为唯一权威；契约实现须在注释或 README 引用 **文档版本或固定 URL + 摘录日期**。 |
| **程序内** | 保留 API 原始数值的可追溯形式（如 `BigDecimal`）；**禁止**在未写入契约的情况下隐式换算单位。 |
| **summary 人类可读** | 附录句中必须包含 **明确单位表述**（与官方一致的自然语言复述，例如「未平仓量（USDⓈ-M 合约口径，API 字段 `openInterest`）」——具体措辞以实现时对照官方为准）。 |

**冻结：** 第一刀 **不**将「变化率」「历史分位」「名义金额派生」列为必填；若日后增加「名义价值」派生（如配合 `markPrice`），须 **新版本契约**。

---

## 4. Failure / 回退（对齐 Funding）

**冻结：** 与 `RealMarketEnvironmentService` 对 Funding 的策略一致：**best-effort**。

| 情形 | 行为 |
| --- | --- |
| HTTP 非 2xx / 超时 / 解析失败 / 缺字段 | **不抛出**至 assemble 失败；**不追加** OI 附录 |
| 现货 24h 已成功 | summary 仍以现货（及已成功之 Funding 附录）为准 |
| OI 失败 | **不降级**整张市场环境为 fallback（除非现货本身失败） |

日志须带 `symbol`、状态码或可解析错误摘要（粒度与 Funding 客户端同级即可）。

---

## 5. `source_type`（`tm_market_environment_snapshot.source_type`）组合决策表

**冻结原则：** 「仅 Funding」与「Funding + OI」必须在快照上 **可区分**；**禁止**在未扩展枚举的情况下长期靠读 `summary` 文本区分二者。

**现有已实现枚举（代码常量名，落库字符串一致）：**

- `BINANCE_24H_HEURISTIC` — 现货 24h 成功，Funding **未**并入 summary  
- `BINANCE_SPOT_PERP_MIN_HEURISTIC` — 现货成功且 Funding **已**并入  
- `PLACEHOLDER_FALLBACK` — 现货链失败等占位回退  

**契约矩阵（语义 —— 接入实现时须新增对应常量并写入本表「落库值」列）：**

| 现货 24h | Funding 并入 | OI 附录成功 | 语义 | 落库 `source_type`（接入时钉死） |
| --- | --- | --- | --- | --- |
| 否 | — | — | 占位回退 | `PLACEHOLDER_FALLBACK` |
| 是 | 否 | 否 | 仅现货启发式 | `BINANCE_24H_HEURISTIC` |
| 是 | 是 | 否 | 现货 + Funding（当前已有） | `BINANCE_SPOT_PERP_MIN_HEURISTIC` |
| 是 | 否 | 是 | 现货 + OI，无 Funding | `BINANCE_USDM_OI_MIN_HEURISTIC` |
| 是 | 是 | 是 | 现货 + Funding + OI | `BINANCE_SPOT_PERP_OI_MIN_HEURISTIC` |

**说明：** 「建议名」仅供实现评审；唯一真值为 **`AnalysisAssemblerServiceImpl`（或继任装配类）中的字符串常量** 与本表同步更新。

**DQ：** OI 接入后的 run 级 DQ 可否沿用「非 fallback 不改条数档」哲学，须在接入 PR 中与 `estimateDataQualityScore` JavaDoc **对齐复核**；本契约不先行改 DQ 算法。

---

## 6. Summary 附录句式（最小）

**冻结结构要素（顺序可调，要素不可缺）：**

1. 标明 **USDⓈ-M / 未平仓**  
2. 给出 **规范化后的数值**（与 API 一致精度策略）  
3. **单位 / 口径**短语（与 §3 一致）  
4. **来源**：Binance 公开 `openInterest` 启发式（与 Funding「启发式」层级对齐）

**示例骨架（非字面强制，接入时固化为单模板方法）：**

> ` … USDⓈ-M 未平仓量约 {数值} {单位说明}（API: openInterest）；Binance 启发式。`

全书须可与 Funding 附录 **拼接在同一 `summary` 末尾**且不冲突。

---

## 7. Future evidence 与 DQ 哲学（未实现前先冻结语义）

| 项 | 冻结 |
| --- | --- |
| **证据类型** | **`风险`（`EvidenceTypeConstants.RISK`）** |
| **禁止** | 归入 **`杠杆`** —— 当前「杠杆」证据语义位已由 `leverageSuggestion` 窄模板占用 |
| **叙事** | 拥挤度 / 持仓存量 / 衍生品风险密度（与第二维「振幅」风险行 **模板区分**） |
| **DQ carve-out** | 若未来产生结构化 evidence 行，须与第二维 / Funding / 杠杆窄模板一样，用 **固定前后缀** 供 `effectiveEvidenceCountForDataQuality` 窄匹配；**接入 PR 须同时补 JavaDoc + 契约一句** |

**实现补记（当前最小切口）：** OI 最小 evidence 已进入 evidence 链（`风险` / `NEUTRAL` / `MARKET_HEURISTIC`，description 与 `buildOpenInterestAppendix(...)` 同源）；该模板在 run 级 DQ 中按**环境解释性锚点**执行 carve-out，不计入 `effectiveEvidenceCountForDataQuality`。

---

## 8. 修订记录

| 日期 | 修订 |
| --- | --- |
| 2026-04-21 | 初版冻结（契约文档；不接主链） |
| 2026-04-21 | 主链最小接入：`source_type` 落库值钉死；`RealMarketEnvironmentService` + `BinanceUsdtMOpenInterestClient` |
