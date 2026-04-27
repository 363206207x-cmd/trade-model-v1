# Dashboard 真值收口阶段冻结成果索引（草案）

> 状态：草案  
> 用途：阶段冻结 / 交接索引 / 后续主线切换前的收口记录  
> 范围：本轮 Dashboard 真值收口、Layer1 system 聚合补齐、两批最小测试闭环、最小文档同步收口
> 交叉引用（基线升版清单）：`PHASE10_BASELINE_PROMOTION_AND_FREEZE_LIST.md`（冻结 / 未冻结双清单 + 下一主线拍板）

---

## V1 当前实现状态结论（阶段总控）

> **读前边界：** 「阶段性已实现」仅描述当前仓库相对原始 V1 框架（总览见 `PROJECT_SPEC.md`）**已落地且可引用**的范围；**明确不等于**「V1 全框架完成」。勿将 Dashboard / Review 冻结进度或解释层最小闭环误读为全规格交付。

### 一、阶段性已实现的部分

- **端到端分析主链可跑通并落库**：assemble → 证据 → 评分 → 决策 → 计划 → 核心表写入，并形成与市场环境快照、Push 快照、监控告警触发等在同一事务语义下的阶段性闭环。
- **Dashboard 决策读模型 + execution 窄表真值**：结论字段与「每 `analysis_id` 最新一条」`tm_execution_plan` 行贯通；执行 Tab（s4）边界已冻结为计划表节选 + 决策侧派生摘要，而非完整方案叙事。
- **Review 聚合事实入口**：`/api/review/aggregate` 聚合运行、决策、计划、Push/Recheck、Missed、Hot Reset、告警等；与 Dashboard detail 同源挂载结构化 evidence/score brief 与市场环境快照。
- **复盘落库与审计追加**：`tm_review_result` 用户结论与 `/api/review/save` 追加 `tm_rule_version_log`（审计链），符合「追加留痕、不自动改规则」的现阶段边界。
- **evidence 三字段治理骨架 + 前后端最小消费**：`evidence_type` / `direction` / `source` 生成与写库 normalize；Dashboard `s2` 与复盘页对 **top3** 结构化证据只读承接。
- **score 进入 8/8（新增事件冲击分）+ top3 展示**：`tm_score_item` 非纯占位；当前至少「趋势结构分」「综合可信度分」「资金推动分」「杠杆风险分」「流动性质量分」「情绪温度分」「宏观环境分」「事件冲击分」与主链挂钩；Dashboard / Review **同源 top3 brief**。本轮仅新增“事件冲击分”的写分与回放，不新增 decision 硬闸门；事件第二刀输入契约已实现并进入观察态（Implemented + Verified + Observation）。
- **事件第二刀输入契约已实现并回归通过**：已按 `tm_hot_reset_event` 落地 `eventImpactInput`（`eventFactHit` / `eventFactCount` / `eventLatestTime` / `eventReasonCode` / `eventTriggerType` / `eventVersion` / `eventTraceId`），并保持“不改 UI / 不改公式 / 不改 decision 主路径”；阶段状态：**Implemented + Verified + Observation**。归档见：`PHASE10_EVENT_SECOND_CUT_IMPLEMENTATION_REGRESSION_RECORD.md`。
- **market environment 最小双源环境快照闭环**：Binance 24h 启发式主链接入 + `tm_market_environment_snapshot` 按 `analysis_id` 锚定；Dashboard detail / Review **snapshot-first**；与决策 `market_bias_hierarchy` **语义隔离**已固定。
- **data quality / systemHealth / AI 展示等口径冻结成果**：run 级 `data_quality_score` 与 Push/Recheck 质量字段分离；`systemHealth` 首屏承载范围与 Layer1 三项聚合真值化；AI 区以落库 `ai_role_results` 等为展示真值、去前端虚构。

### 二、已推进但仍属最小实现

- **市场环境模块**：单一 24h heuristic，**非**多源融合，**非**独立大模块 UI 终态。
- **原始证据模块**：读模型 **top3 / brief**，**非**完整证据解释层；支持/反对分列、五类证据完备叙事等仍属扩展面。
- **证据评分模块**：**已进入 8/8（新增事件冲击分） + top3**，**非**八大评分；排序与重要性_ranking 仍为阶段性近似。
- **综合决策工业化部分**：`multi_tf_convergence`、`ai_role_results` 等**展示与落库字段存在**，多周期 / 多 AI **规格级调度、缓存、配额、成本审计表等工业层不齐**。
- **执行计划深层解释**：窄表字段贯通；模板维度、plan 侧账户风险 JSON、免责声明与完整执行叙事等**未收口为规格对象完整态**。
- **监控与复盘运营化**：告警与复盘聚合已有事实链；规格中的防疲劳全栈、状态机独占、回测与运营级叙事等**仍属缺口面**。
- **数据质量 / systemHealth 全意表达**：run 上有分与首屏冻结范围已成立；数据源扣分矩阵、全链路健康运营仪表盘等**非当前完成态**。

### 三、与原始框架仍有本质缺口的部分

- **数据基础层全栈**：多源采集、缺失/延迟/异常结构化健康矩阵、规格级缓存与成本策略等与当前仓库形态不对齐的部分。
- **八大评分 + DQ 硬规则闭环**：熔断、折扣与决策模板刚性约束的**规格级闭环**未齐。
- **多周期收敛算法层**：独立可审计算法与冲突明细落库等，与「仅有字段承载」区分。
- **三 AI 工业化**：分级触发、Redis 缓存、配额、限流、fallback、**持久化成本审计**等全套工程化。
- **执行计划对象契约完整态**：`plan_mode`、`execution_disclaimer`、`account_risk_json`、`template_type` 等与窄表并存对象契约（以 `schema.sql` 与写入链为准）。
- **规格三页前端全貌**：当前实施重心在 Dashboard / Review 等战场页迭代，**非**蓝图三页信息架构全集。

### 四、V1 当前实现状态 Top 5

1. **决策 + 计划窄表读模型已收口为前台真值**（含 latest-plan 语义与 s4 边界）。
2. **Review 聚合 API 已成为复盘事实主入口**（前端只读格式化，矩阵列 FE 推理禁区）。
3. **market environment 快照已与 decision 偏置解耦**，避免环境与倾向混称。
4. **证据 / 评分已形成最小传导链**，**明确远非**八大评分与完整解释层完成。
5. **任何进度百分比仅锚定 Dashboard / 解释层冻结轮**；**不得**等同于 V1 全框架完成度。

### 最容易被误判“已完成”的部分

下列现象**均不等于**对应模块在 `PROJECT_SPEC.md` 中的规格终态完成，沟通与排期时需主动降权：

- **top3 证据 / top3 评分**：仍是 brief 与条数裁剪，不是全量证据模块或八大评分模块。
- **`tm_execution_plan` 前台贯通**：是**窄表**读模型真值化，不是执行计划深层解释模块或完整 playbook。
- **`multi_tf_convergence` / `ai_role_results` 字段存在**：不等于多周期收敛算法层与三 AI 工业化已按规格闭环。
- **`tm_user_config` 表存在**：不等于用户配置已在决策 / 计划前 **全域、可验证** 生效。
- **`data_quality_score` 已落 run 表**：不等于数据源健康矩阵、扣分溯源与熔断规则的运营级闭环已完成。

### 下一阶段优先主线建议

- **第一优先：市场环境 + 输入事实质量主轴**——在不解冻当前读模型边界的前提下，把上游输入从「单源 24h 启发式」演进为可配置、可扩展、可对齐快照语义的事实锚；**原因**：它是 **evidence → score → decision** 的上游锚点，最能降低全链路「假完整」误判。
- **第二优先：复盘 / Push / recheck / recovery 语义深化**——与已冻结的 Dashboard「读模型齐套 / 系统积压」分工衔接，补齐叙事与权威入口，避免复核态与执行态混读。

### 五、市场环境双源阶段结论（拍板）

> **当前状态结论（固定口径）：** **当前市场环境主线 = 最小多附录衍生品环境闭环；不是完整市场环境模块。**
>
> **三层边界（对外复述统一口径）：**
> 1. **阶段性已实现：** analysis_id 对齐主链稳定；现货 24h + 第二维 + Funding + OI（最小附录）事实闭环可回放；`source_type` 可区分、可展示、可复盘；evidence / DQ / `source_type` 语义基本统一；环境语义与决策语义保持隔离。
> 2. **仍属最小实现：** 非多源仲裁引擎、非完整 DQ 健康矩阵、非完整环境判定引擎；Funding / OI 尚未进入环境核心判定字段；score / decision 尚未将新源作为刚性主路径输入。
> 3. **最易误判（需显式禁止）：** **输入源变多 + 有 evidence + `source_type` 可区分 ≠ 市场环境模块完成。**

> **定位（与上文第 21 行）：** **「最小双源环境快照闭环」**指 **快照锚 + 现货 24h 与（可选）Funding 的最小解释链**（非旧称「单源」）；**行情拉取主链**仍 **单路** Binance 24h。**非**规格多源融合终态；细节见下列各项。

- **本阶段可称已收口（可对外描述）：** 现货 24h 主链接入；**第二维**（`range_pct_24h` / `volatility_regime` 与 VO 同源，**已**进 `tm_market_environment_snapshot`）；**Funding** 经 **VO → `summary` 附录 → 结构化 evidence（`buildFundingAppendix` 模板）→ `source_type` 挡位（如 `BINANCE_SPOT_PERP_MIN_HEURISTIC`）→ run 级 DQ **carve-out**，形成稳定语义链；`summary` 与 `sourceType` 仍承担可读与来源标签。
- **仍属最小实现、不得误判为「完整市场环境模块」：** 非可配置多源仲裁、非独立大环境 UI、非 OI/第三源等；**非** `tm_data_source_health` 全链。
- **Funding / OI 最小快照同构（已落地）：** `tm_market_environment_snapshot` 已新增并按 VO 同源写入四列：`last_funding_rate`、`perp_funding_applied`、`last_open_interest`、`oi_applied`；保持 nullable 与 best-effort 语义，仅用于结构化复盘/查询增强，**不**改变 decision 主路径、`environmentType` / `riskMode` 与现有 summary/evidence 语义。
- **OI 变化维度最小真值（第一刀）：** `open_interest_delta`（VO/DO: `openInterestDelta`）已按“同 symbol+timeframe 最近前一 snapshot”基准进入 market environment 生成与 snapshot 固化链；仅在 `oiApplied=true` 且当前/前值 OI 均非空时写入 `current - previous`，其余为 `null`（best-effort，不阻断主链）。
- **OI/Funding 联合派生最小标签（已落地）：** `derivativesCrowdingState` 已作为 OI/Funding 联合派生离散标签进入 market environment 生成与 `tm_market_environment_snapshot` 固化链（`NEUTRAL` / `CROWDED_LONG` / `CROWDED_SHORT`），用于上游真值补强与回放锚点，**不**代表多源状态引擎完成态。
- **`derivativesCrowdingState`（二刀 B，与实现对齐）：** Funding 符号定方向；`openInterestDelta` 仅作轻过滤（`null` 保持 Funding-only，`0` 或异号 → `NEUTRAL`）；在 `enrichOpenInterestDeltaFromPreviousSnapshot` 之后于 assemble 主链写入 VO 并落快照；`tryBuildFromRealQuote` 单独路径可不携带该字段终值。
- **再接下一源（如 OI）前建议：** 以本段为边界锚点，避免与「完整模块」混称。
- **OI 最小接入契约（冻结文档）：** 见仓库根目录 **`OI_MINIMAL_ACCESS_CONTRACT.md`**（endpoint、字段、symbol、单位、failure、`source_type` 组合表、summary 句式、future evidence=`风险`、边界声明）。**真接入须先遵守该文档**；未完成契约核对前不接主链。

### 六、证据五类覆盖阶段结论（拍板）

> **最易误判：** 「五类 `evidence_type` 枚举已齐 + Dashboard/Review **top3** 已展示」**不等于**「五类证据模块已完成」。

> **当前阶段总括（与实现对齐）：** 证据模块 = **单路行情环境链上的最小结构化注解 + 读模型回放**（`EvidenceServiceImpl` ← `RealMarketEnvironmentService` / Binance 24h + Funding + OI，并补入 Hot Reset 驱动的最小事件注记）；**不是** `PROJECT_SPEC` 意义上的完整五类证据中台。**勿将「最小真实闭环」误称为「完整五类证据模块」。**

1. **生成覆盖：** 当前 **`EvidenceServiceImpl`** 已稳定产出价格结构、风险、资金、杠杆，并新增 **Hot Reset 驱动的最小事件证据**（`evidenceType=事件`、`direction=NEUTRAL`、`source=SYSTEM_GENERATED`）；该事件切口仅为最小注记，不等于事件系统完成态。
2. **价格结构：** 已为 **最小真实代理**——基于 **`MarketEnvironmentVO.priceChangePercent24h`**（与 Binance 24h ticker **同源赋值**，不从 `summary` 反推）生成 **日内启发式价格结构代理**（`MARKET_HEURISTIC`；缺标量时 `SYSTEM_GENERATED` 说明）；**仍非**规格级 K 线结构/形态/插针/流动性扫荡。**旧口径「默认证据占位行」已过期，文档与对外叙事勿再引用。**
2.1 **macro evidence 第一刀（最小注记）已入链：** 仅基于 `tm_market_environment_snapshot` 白名单字段（`volatility_regime` + `range_pct_24h`，可选 `derivatives_crowding_state`）追加单条 `evidenceType=宏观`、`direction=NEUTRAL`、`source=SYSTEM_GENERATED` 的窄模板注记；不读取 `summary/source_type`，不等于宏观上游系统完成。**C 第二刀 A1（已落地）补充：宏观条在主 assemble 链按同 run `MarketEnvironmentVO` 同源白名单字段生成（`volatilityRegime` + `rangePct24h`，可选 `derivativesCrowdingState`），并与同 run snapshot 对应列保持同源一致。**
3. **风险 / 资金：** **风险**行承载 **第二维振幅/波动体制**（`MARKET_HEURISTIC`，与 `RealMarketEnvironmentService` **同源**）；**资金**行在 Funding 并入时与 **`buildFundingAppendix` 同源**——二者均为 **环境主链真实锚点**（run 级 DQ 对二者模板条 **carve-out**，见 `AnalysisAssemblerServiceImpl#effectiveEvidenceCountForDataQuality`）。
4. **杠杆 / 事件：** **杠杆**已 **最小接入**：由 `MarketEnvironmentVO.leverageSuggestion`（`low_leverage` / `moderate_leverage`，与 `RealMarketEnvironmentService` 同源）生成 **单行**证据（`MARKET_HEURISTIC`）；run 级 DQ 对该 **窄模板**行与第二维 / Funding **同类 carve-out**，**不计入**抬档用有效 evidence 条数（见 §二 run 级 DQ 当前实现现状）。**事件**依赖外部 feed 或单独契约前 **保持后置**，**不适于**与五类文档收口并行硬扩占位。
5. **与 score / decision 边界：** 当前证据链主要增强 **解释链与复盘可读**（含 top3 brief）；**综合决策 / 八大评分规格矩阵** **未**将五类证据作为 **刚性主路径输入**；`ScoreServiceImpl` 仍为 **轻规则**（如对「价格结构」证据 **优先读取 `direction`**，再 fallback 描述关键词；趋势分另综合 `marketEnvironment.summary` 等）。**禁止**将「有证据行」误读为「五类矩阵已驱动决策引擎」。

### 七、综合决策模块当前状态结论（拍板）

> **当前状态结论（固定口径）：** **当前综合决策模块 = 决策结果主输出链已落库并被前台稳定承接的最小闭环；不是完整综合决策模块。**
>
> **三层边界（对外复述统一口径）：**
> 1. **阶段性已实现：** decision 读模型主链完整；`ai_conflict` / `confused` / `review_reasons` / `asset_state_snapshot` 最小真值链已稳定；`multi_tf_convergence`、`ai_role_results` 字段已可见且可回放；decision 与 run级 DQ / execution 的语义边界已冻结并可审计。
> 2. **仍属最小实现：** 当前输入仍是轻规则最小集，非八大评分刚性整合；多周期与多 AI 仍主要停留在字段承载与展示层，尚未形成规格级调度、约束、审计、成本与回退一体化闭环。
> 3. **最易误判（需显式禁止）：** **字段很多 ≠ 综合决策模块完成；`multi_tf_convergence` 可见 ≠ 多周期收敛模块完成；`ai_role_results` 可见 ≠ 三 AI 决策工业化完成。**

### 八、评分模块当前状态结论（拍板）

> **当前状态结论（固定口径）：** **当前评分模块 = 已从 5/8 进入 8/8（新增事件冲击分）并可在 Dashboard / Review 同源回放的最小评分闭环；不是完整评分模块。**
>
> **三层边界（对外复述统一口径）：**
> 1. **阶段性已实现：** `tm_score_item` 已形成主链真实写入；当前评分已进入 8/8（新增事件冲击分）；`score -> decision` 最小传导链已成立；`scoreTopItems(top3)` 已在 Dashboard / Review 同源承接回放；评分输入已真实接入 `marketEnvironment` 与价格结构 evidence（`direction` 优先、description 关键词 fallback）。
> 2. **仍属最小实现：** 当前已进入 8/8 分项与轻规则打分，非八大评分矩阵；非完整评分解释层；run 级 DQ 与评分折扣/熔断尚未形成规格级硬闭环；top3 仍是 brief 回放，非完整评分解释界面。
> 3. **最易误判（需显式禁止）：** **已进入 8/8 ≠ 八大评分完成；`scoreTopItems` 可回放 ≠ 评分解释层完成；有综合可信度分 ≠ 数据质量折扣与熔断闭环完成。资金推动分 = 最小真实评分扩展，不等于八大评分体系完成，且当前不硬接 decision 主路径。杠杆风险分 = 最小真实评分扩展，不等于风险建模完成，且当前不硬接 decision 主路径。流动性质量分 = 最小真实评分扩展，不等于完整流动性模型完成，且当前不硬接 decision 主路径。情绪温度分 = 最小真实评分扩展（仅写分+回放），不等于情绪与事件模块完成，且当前不硬接 decision 主路径。宏观环境分 = 基于现有宏观白名单字段的单项增量，不等于宏观模块完成，也不改变当前 decision 主路径。事件冲击分 = 基于现有 event evidence 命中的单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径；事件第二刀当前仅完成输入契约实现与回归（Implemented + Verified + Observation），不等于事件系统完成。scoreTopItems(top3) = 同源 brief 裁剪视图（按记录顺序），不代表按 scoreValue 排序，不代表评分优先级排序，不等于完整评分解释模块。**
> 4. **score 第一条最小刚性闸门（第一刀）：** decision 主路径可按单一趋势结构分阈值接线（`trendStructureScore < 50 -> isWorthOpening=false`）作为最小输入扩展；该变更仅作用于 `isWorthOpening`，**不等于**评分驱动决策体系完成。

### 九、执行计划模块当前状态结论（拍板）

> **当前状态结论（固定口径）：** **当前执行计划模块 = latest-plan 窄表字段已落库并在 Dashboard / Review 稳定承接的最小执行闭环；不是完整执行计划模块。**
>
> **三层边界（对外复述统一口径）：**
> 1. **阶段性已实现：** `tm_execution_plan` 窄表写入链稳定；`recommendedAction` / `entryZone` / `stopLoss` / `takeProfitRules` / `leverageSuggestion` / `positionSuggestion` 已按 `analysis_id` latest-plan 语义贯通前台；Dashboard `s4` 与 Review Plan 已同源承接；`decision` 侧 `validPeriod` / `invalidCondition` 与计划侧窄表边界已冻结。
> 1.1 **对象真值化最小增量（第一刀）：** `plan_mode` 已作为 execution 对象最小真值字段落库（`tm_execution_plan`），由 execution 生成阶段产出；该字段当前以对象真值链打通为目标，**不要求立即前台上屏**，且**不等于**完整执行计划模块完成。
> 1.2 **对象真值化最小增量（第三刀）：** `invalid_condition` 已作为 execution 对象失效边界最小真值字段落库（`tm_execution_plan`），写入时与 decision 侧同源镜像承接，当前不要求 latest-plan / Review 读链透传。
> 2. **仍属最小实现：** 当前仍是窄表节选与派生摘要承接，非完整执行解释层；`executionPlanSummary` 仅为决策侧派生摘要，非完整执行叙事；模板化对象、账户风险 JSON、免责声明、监控一致化等规格对象尚未收口为完整契约。
> 3. **最易误判（需显式禁止）：** **前台字段贯通 ≠ 执行计划模块完成；`executionPlanSummary` 可见 ≠ 完整执行方案完成；`takeProfitRules` 独立上屏 ≠ 执行深层解释模块完成。**
>
> 补充：`account_risk_json` 已作为 execution 对象风险约束快照最小真值字段落库（`tm_execution_plan`），当前仅打通 execution 写链，不等于完整执行风控对象完成。

### 十、复盘 / 监控模块当前状态结论（拍板）

> **当前状态结论（固定口径）：** **当前复盘 / 监控模块 = Review 聚合事实入口与 Push/Recheck 等状态链已可查询、可回放、可审计的最小运营闭环；不是完整复盘与监控模块。**
>
> **三层边界（对外复述统一口径）：**
> 1. **阶段性已实现：** `ReviewAggregate` 已形成复盘事实主入口；run / decision / plan / marketEnvironment / evidenceTopItems / scoreTopItems 与 Push/Recheck、Missed、Hot Reset、Alerts 已可同链查询与回放；复盘保存与 `tm_rule_version_log` 追加留痕已建立可审计链路。
> 2. **仍属最小实现：** 当前以事实聚合与只读复盘为主，非完整运营化复盘中台；Push/Recheck 与 run级 DQ、systemHealth 语义虽已分线冻结，但未形成规格级全栈监控编排、防疲劳治理闭环与状态机运营体系完成态。
> 3. **最易误判（需显式禁止）：** **Review 聚合可见且链路较强 ≠ 复盘模块完成；Push/Recheck/Missed/Hot Reset 可见 ≠ 监控模块完成；有留痕审计链 ≠ 规则闭环自动治理完成。**

---

## 市场环境快照 + run级 DQ 联合契约（冻结）

> **本节为规范性权威入口：** 市场环境快照、run 级 DQ、Push/Recheck 质量字段、`systemHealth` **三线边界**，以及下一阶段**两处允许代码入口 / 反模式**，**均以本节为准**；`PHASE5_FIELD_TRUTH_MATRIX.md`、`PROJECT_SPEC.md` **仅一行级交叉引用**，勿在三份文档各写一长套口径（避免漂移）。
>
> **契约优先于功能扩张：** 本节冻结「当前是什么」「边界在哪」；下一阶段增量须遵守本节**允许入口**与**反模式**；不得以交付名义绕过语义。**算法 / 库 / schema / Dashboard·Review 展示未变前提下，**run 级 DQ **真实生成规则**以本节「run级 DQ 当前实现现状」及 `AnalysisAssemblerServiceImpl#estimateDataQualityScore` JavaDoc 为准。

### 一、市场环境快照当前契约

- **当前来源：** **24h heuristic**，由 `RealMarketEnvironmentService.tryBuildFromRealQuote()` 主链接入（Binance ticker 类路径）；可选 **USDⓈ-M `lastFundingRate`**（`premiumIndex`）在 summary 追加一句，成功时 `source_type` 为 **`BINANCE_SPOT_PERP_MIN_HEURISTIC`**，否则仍为 **`BINANCE_24H_HEURISTIC`**。
- **当前真值锚点：** **`tm_market_environment_snapshot`**，按 **`analysis_id`** 对齐；与 assemble 主链写入同源。**第二维结构化列：** `range_pct_24h`、`volatility_regime` 与 `MarketEnvironmentVO` 同源（`RealMarketEnvironmentService`：`computeRangePercent24h` / `describeVolatilityRegime`），assemble 写入；high/low/last 不足时列为 NULL。
- **当前展示承接：** Dashboard detail **snapshot-first**（无快照才回落启发式/占位）；Review **`ReviewAggregateVO.marketEnvironment.*`** 与 Dashboard **同源**；Dashboard **`marketEnvironmentMini`**（如 `summary`、`environmentType`、`riskMode`、`sourceType`）为极小承接块。
- **当前非目标：** **非**多源融合；**非**完整独立市场环境模块 UI；**非**完整环境判定引擎。

### 二、run级 DQ 当前契约

- **真值锚点：** **`tm_analysis_run.data_quality_score`**（键 **`analysis_id`**；**不在** `tm_decision_result` 落列）。
- **当前语义：** 本次分析主链上，对**输入可信度**的 **阶段性整数估计**（assemble 路径内生成）；**不等于**规格终态的数据源扣分矩阵全自动结论。
- **当前非目标：** **不等于** `tm_data_source_health` 健康矩阵成品；**不等于** Push/Recheck 链质量字段；**不等于** `systemHealth`；**不等于**结论可信度（与综合可信度分区分见 `PROJECT_SPEC.md`）。

#### run级 DQ 当前实现现状（代码级，防误读）

- **估计位置：** `AnalysisAssemblerServiceImpl#estimateDataQualityScore` → 写入 **`tm_analysis_run.data_quality_score`**（assemble 主链）。
- **当前实质：** **阶段性整数估计**；先按 **evidence / score 列表条数** 落入三档 **35 / 55 / 85**（不读元素内容；run 级 DQ 的 evidence 条数采用有效计数，**不把**第二维、Funding、及**当前最小杠杆窄模板**等环境解释性锚点条视为独立输入增强，见 `AnalysisAssemblerServiceImpl#estimateDataQualityScore`）；再按与 **`tm_market_environment_snapshot.source_type` 同源**的 `marketEnvSourceType`：**`PLACEHOLDER_FALLBACK` 时结果封顶不超过 55**，**`BINANCE_24H_HEURISTIC` / `BINANCE_SPOT_PERP_MIN_HEURISTIC` 时不改条数档**（二者均属非 fallback）。不读 Push/Recheck、`systemHealth`。
- **Funding evidence：** 若 Funding 已进入结构化 evidence 链（`evidenceType=资金` 等与 `RealMarketEnvironmentService#buildFundingAppendix` 同源模板），同样作为**环境解释性锚点**，**不计入** run 级 DQ 有效条数（与第二维振幅 carve-out 并列，见 `AnalysisAssemblerServiceImpl#effectiveEvidenceCountForDataQuality`）。
- **杠杆 evidence（最小切口）：** `evidenceType=杠杆`、`direction=NEUTRAL`、`source=MARKET_HEURISTIC`，且 `description`（trim 后）与 `EvidenceServiceImpl` 当前两档固定文案（`LEVERAGE_EVIDENCE_DESCRIPTION_LOW` / `LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE`）**完全一致**时，视为**环境解释性锚点**，**不计入**有效条数；**非模板**杠杆行（未来其它来源或改写描述）仍**计入**，避免「按类型一刀切」误伤。
- **OI evidence（最小切口）：** `evidenceType=风险`、`direction=NEUTRAL`、`source=MARKET_HEURISTIC`，且 `description`（trim 后）与 `RealMarketEnvironmentService#buildOpenInterestAppendix` 同源固定模板（前后缀窄匹配）一致时，视为**环境解释性锚点**，**不计入**有效条数；**非模板** OI 风险行（如变化率/阈值/历史比较）仍**计入**，避免误伤后续升级信号。
- **离散输出与 &lt;60：** 当前实现下估计器 **仅** 可能产出 **35、55、85**（`PLACEHOLDER_FALLBACK` 封顶时 **≤55**，无 56–84 等中间刻度）。在此离散集合上，代码侧 `ReviewReasonsBuilder` / `MonitorAlertWriteServiceImpl` 的 **&lt;60** **等价于**「非 85 / 非顶格档」。
- **与规格叙事：** **`PROJECT_SPEC.md`** 中的 **85 降档 / 70 熔断** 等规则 **尚未**在决策引擎主路径据此字段接线；前述 **&lt;60** 仅为 **当前实现语义**，**不等于**规格 **70 / 85** 阈值体系，**亦不代表**已接入决策主路径熔断。勿将 run 表整数等同于「规格级熔断分」或「数据源质量矩阵」。
- **run级 DQ 最小刚性闸门（第一刀）：** decision 主路径已新增 **`dataQualityScore < 60 -> isWorthOpening=false`** 的最小刚性约束；该变更仅作用于 `isWorthOpening`，**不等于**完整 DQ 熔断体系完成。

### 三、三线质量边界

分别回答的问题与**禁止混读**如下：

| 线 | 回答什么问题 | 禁止混读 |
| --- | --- | --- |
| **run级 DQ** | **本次 analysis run** 主链输入质量的整数估计 | 勿从 Push 列回填；勿从决策行读作 DQ |
| **Push/Recheck 质量字段** | **推送/二次校验观测链**上的快照或复扫当次质量（如 `tm_push_snapshot.data_quality_score_snapshot`、`tm_push_recheck_log.current_data_quality_score`） | **不得**与 run 级 DQ 互换语义或混算同一指标 |
| **systemHealth** | Dashboard **首屏已冻结**的系统窄语义（如 DB 连接、scheduler 代理等） | **不是**输入数据质量；**不是** run DQ 别名 |

### 四、下一阶段代码增量允许入口

**仅允许优先从下列两处进入**（除非另行契约解冻）：

1. **`RealMarketEnvironmentService` → `tm_market_environment_snapshot`**（第二路输入、`source_type` 深化等均须落在此链路语义内）。
2. **`assemble` 内 run级 DQ 估计逻辑** → **`tm_analysis_run.data_quality_score`**（仅由此主链写入 run 表）。

**禁止的反模式：**

- 从 **Push / Recheck** **回填** run级 DQ。
- **前端**拼业务「质量」结论替代后端真值（遵守 `PHASE5_FIELD_TRUTH_MATRIX.md` FE Inference Ban）。
- **先行** **`tm_data_source_health` 全表链**（契约未稳前横切过大）。
- **先行** **大屏式** DQ / 数据健康 UI（冻结期内喧宾夺主）。

### 五、下一阶段第一批 / 第二批 / 禁止先做

- **第一批：** 本节在全团队可读；仅在**第四节两处入口**上做**最小增量**（如快照侧可命名来源、DQ 估计过程可叙述/可审计），**不**并行全表健康链与大屏扩张。
- **第二批：** `tm_data_source_health` 或等价数据源健康矩阵、DQ 与规则硬闭环、多源市场环境实质接入——须以本节稳定为前提**单独评审**。
- **禁止先做：** 违反第四节反模式；用行级 recheck/recovery **替代**本节三线边界；以 UI **倒逼**语义。

---

## V1 解释层阶段结论（冻结）

### 一、已收口的解释层部分

- Decision 读模型主链
- Review 聚合事实链
- market bias vs market environment 语义隔离
- data quality 口径分离
- systemHealth 首屏冻结范围

### 二、已推进但仍属最小实现

- Evidence
- Score
- Market environment
- Execution 深层解释
- data quality 的运营化承接
- systemHealth 的全意健康表达

### 三、转入下一阶段

- 完整证据解释模块
- 八大评分与评分解释层
- 多源市场环境模块
- execution 深层解释
- data health 全链路运营化
- Dashboard 行级 recheck / recovery 真值化

### 四、V1 解释层冻结索引 Top 5

1) Decision 读模型：关键结论位与有效/失效字段以 DB 真值为准，Dashboard 去伪存真已完成阶段目标
2) Review 聚合：复盘页以聚合 API 为事实入口，前端只做只读格式化，不补业务语义
3) Evidence / Score 结构化最小复盘：`evidence_type` / `direction` / `source` 治理骨架已完成；Dashboard `s2` 与 **Review 聚合 + 复盘页**均已承接同源 **`evidenceTopItems(top3)`、`scoreTopItems(top3)`**（仍为 brief/top3）；`evidenceSummary` 仍为决策摘要文本（≠ 结构化解释层），与结构化块 **并存不替代**
4) Market environment：snapshot 锚定已完成，并与 decision 偏置语义硬隔离
5) 口径分离：run 级 data quality 与 Push/Recheck 质量分离；systemHealth 首屏范围已冻结

---

## 1. 冻结名称

**Dashboard 真值收口阶段冻结（当前轮）**

---

## 2. 冻结结论

本轮已完成 Dashboard 从“展示壳子”向“真值驱动前台”的关键收口：

- 首页/详情首屏阶段完成
- AI 解释区完成去伪存真
- Layer1 三项核心 system 聚合真值已补齐
- 关键字段已完成两批最小测试闭环
- `PHASE5_FIELD_TRUTH_MATRIX.md` 与 `PHASE10_STEP1_READ_MODEL_TRUTH_MATRIX.md` 已完成最小文档同步收口
- 冻结提示：Dashboard 对 `validPeriod` / `invalidCondition` 统一按“当前真实来源 + 可空”口径解读，不再按旧占位常量口径理解

---

## 3. 本轮核心实现收口

### 3.1 首页 / 详情首屏

本轮已完成以下收口项：

- `conclusionSummary` 上移到首页币块
- `evidenceSummary` 上移到详情首屏
- `isWorthOpening` 与 `recommendedAction` 语义拆分
- `recommendedAction` 完成 DB → Mapper → VO → API → 页面全链路贯通
- `readModelTruthStatus` 上屏
- Tile 第四枚标签改为读模型齐套语义
- `executionBlock` 去除对不存在 `recheckStatus` 的依赖，改为依赖已存在真值字段
- **`executionBlock`（s4）文档同步口径：** 已承接 **latest-plan** 窄表字段（含 **`takeProfitRules`** 独立多行块）；**`executionPlanSummary`** 仍为决策侧派生摘要，不与止盈规则拼接；完整模板化 / 账户风险 / 监控叙事仍属后续（见下文 §「当前阶段冻结结论页补充」）

### 3.2 AI 区

本轮已完成以下真值化动作：

- 删除 `humanAiRoles`
- 删除前端虚构三角色话术
- 删除前端二次抽取的“最终裁决 / 综合判断分数”
- 改为直接展示 `aiRoleResults`
- 增加“以下为落库原始输出，未做角色重写”提示
- `aiPlanMode` 上屏

### 3.3 Layer1 system 聚合

本轮已完成三项 system 聚合真值补齐：

- `confusedCount`
- `pendingCount`
- `reverseSignalCount`

### 3.4 Layer1 语义收紧

本轮已完成以下语义收紧动作：

- `confusedCount` 缺失时不再显示为 `0`
- 高风险数 / 已采纳数明确标为 **本批摘要**
- `API 状态` 收窄为 **数据库连接**
- `正常` 收窄为 **可连接**

---

## 4. 本轮后端真值补齐

### 4.1 confusedCount

**口径：**

- 来源：`tm_asset_state`
- 条件：`confused_score > 0`
- 含义：当前困惑态 symbol 数

**链路：**

- `AssetStateMapper.countSymbolsWhereConfusedScorePositive()`
- → `DecisionServiceImpl.getLightSystemStatus()`
- → `LightSystemStatusVO.confusedCount`
- → summary / refresh / dashboard

---

### 4.2 pendingCount

**口径：**

- 来源：`tm_push_snapshot`
- 条件：
  - `push_status in ('CAPTURED', 'RECHECK_VALID_WAITING')`
  - `expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP`
- 含义：当前积压待复核数

**链路：**

- `PushSnapshotMapper.countPendingRecheckBacklog()`
- → `DecisionServiceImpl.getLightSystemStatus()`
- → `LightSystemStatusVO.pendingCount`

---

### 4.3 reverseSignalCount

**口径：**

- 当前 `OPEN` 持仓 symbol
- 每 symbol 取最新一条决策
- `LONG + BEARISH` / `SHORT + BULLISH` 计为反向
- 按 symbol 去重计数

**链路：**

- `DecisionResultMapper.countOpenSymbolsWithReverseSignal()`
- → `DecisionServiceImpl.getLightSystemStatus()`
- → `LightSystemStatusVO.reverseSignalCount`

---

## 5. 本轮测试闭环

### 5.1 第一批最小测试闭环

已覆盖：

- summary/detail 同源
- `readModelTruthStatus / readModelFallbackReason`
- `hasOpenPosition / positionStatus`
- `missedValidOpportunityCount / hotReset*`
- detail 缺参 `400`
- ops overview 空壳结构

### 5.2 第二批最小测试闭环

已覆盖：

- `marketBiasHierarchy`
- `isWorthOpening`
- `recommendedAction`
- `aiConflictLevel`
- `aiConflictScore`
- `aiPlanMode`
- `confusedScore`

### 5.3 本轮新增 system 聚合测试

已覆盖：

- `confusedCount`
- `pendingCount`
- `reverseSignalCount`

### 5.4 测试状态

- 本轮相关测试已本地执行
- `mvn test` 通过

---

## 6. 本轮同步完成的文档

### 6.1 已完成同步

- `PHASE5_FIELD_TRUTH_MATRIX.md`
- `PHASE10_STEP1_READ_MODEL_TRUTH_MATRIX.md`

### 6.2 已修正方向

- Layer1 三项 system 聚合旧状态
- Dashboard `recheckStatus` 旧叙事
- `LightSystemStatusVO` 的 system 真值范围
- §1 总述中对 system 缺口的误读风险

---

## 7. 本轮未纳入冻结范围

以下内容本轮明确未做：

- score 相关冻结已更新为“8/8 最小评分闭环 + top3 最小展示”，不等于八大评分模块完成
- Dashboard 决策体真正的 `recheckStatus` 贯通
- `confusedRecoveryHint` 真值字段化
- 三 AI 角色结构化拆栏
- `aiPlanMode` 中文映射
- 数据质量分上屏
- 独立市场环境模块
- 更重的集成测试 / E2E
- checklist 结构扩张
- 更多 Phase 文档的大面积回写

---

## 8. 当前系统状态判断

**总体进度：93%～95%**

### 分项判断

- 读模型真值边界：完成
- Dashboard 首屏收口：阶段完成
- AI 区真值化：完成
- Layer1 核心 system 聚合：完成
- 两批最小测试闭环：完成
- 文档同步两刀：完成

### 阶段判断

本轮结束后，Dashboard 已从“展示壳子 + 前端启发式补语义”推进到：

- 关键结论位真值化
- AI 区去伪存真
- Layer1 核心 system 聚合真值化
- 关键字段已有自动化门禁
- 核心矩阵文档已追平当前实现

---

## 9. 冻结后的后续方向建议

### 9.1 建议优先级 1

**复盘链路更深层真值贯通**

建议方向包括：

- Dashboard 是否需要真正的 `recheckStatus`
- recovery 提示是否真值化
- Review / Push / Dashboard 的 recheck 语义统一

### 9.2 建议优先级 2

**冻结成果索引正式入库**

建议将本草案整理为正式阶段记录文档，用于：

- 阶段交接
- 后续审计
- 新主线切换前的冻结基线

---

## 10. 可直接引用的冻结结论

**Dashboard 首页/详情首屏已完成阶段性收口；AI 解释区已完成去伪存真；Layer1 三项核心 system 聚合真值已补齐；关键字段已完成两批最小测试闭环；`PHASE5_FIELD_TRUTH_MATRIX.md` 与 `PHASE10_STEP1_READ_MODEL_TRUTH_MATRIX.md` 已完成最小文档同步收口。**

---

## error_stage 暂不加列冻结说明

本轮对 `tm_review_result` / `tm_analysis_run` / `tm_rule_version_log` 的离线统计，在**当前工作环境**中对应库为**空库**，无法形成有效样本。

因此：**不以本轮统计结果**作为依据推动 `error_stage` 入库字段化（第二刀加列）；现阶段**暂不推进**该方向。

当前仍采用：

1. **`error_type` 受控词表**
2. **`adjustment_suggestion` 模板化**
3. **`error_stage` 仅作可选填写词汇**（不进库、不升格为必填或结构字段）

后续**仅当**取得真实复盘数据并完成可复核的离线统计后，才重新评估是否在库内新增 `error_stage`（或等价）列。

---

## recheckStatus 语义冻结说明（草案）

### 1. 结论

当前阶段，**Dashboard 不承接行级 `recheckStatus` 真值**。  
Dashboard 的主语义保持为：

- **读模型边界**：`readModelTruthStatus` / `readModelFallbackReason`
- **系统聚合**：如 `pendingCount`、`confusedCount`、`reverseSignalCount`

行级 `recheckStatus` 真值仍归属：

- **Push / Recheck 专用链路**
- **Review 聚合链路**

### 2. 语义边界

以下三类概念必须严格区分：

#### 2.1 读模型齐套状态
用于回答：

- 当前决策读模型是否齐套
- 是否存在缺字段 / fallback
- 当前页面是否能基于现有真值展示执行区块

对应字段：

- `readModelTruthStatus`
- `readModelFallbackReason`

该语义属于 **Dashboard 决策读模型主链**。

#### 2.2 行级复扫状态
用于回答：

- 某条 push / recheck 日志的单次复扫结果是什么
- 最近一次 recheck 是否通过 / 失效 / 漂移 / 待确认

对应真值链：

- `tm_push_recheck_log.recheck_status`
- PushRecheck API
- ReviewAggregate 中的 recheck summaries

该语义属于 **Push / Review 链路**，**不直接进入 Dashboard 决策体**。

#### 2.3 系统级待复核积压
用于回答：

- 当前系统中还有多少条待复核积压项

对应字段：

- `pendingCount`

其来源是 **system 聚合真值**，不是单条 `recheck_status` 的替代品。

### 3. 冻结决定

为避免语义混淆，当前阶段冻结如下：

1. **Dashboard tile 不展示行级 `recheckStatus`**
2. **Dashboard executionBlock 不以 `recheckStatus` 作为主闸门**
3. **Dashboard 继续以 `readModelTruthStatus` 表达“读模型是否齐套”**
4. **Dashboard 继续以 `pendingCount` 表达“系统待复核积压规模”**
5. **行级 `recheckStatus` 仍以 Review / Push API 为唯一主入口**

### 4. 原因

作出上述冻结决定的原因如下：

- `readModelTruthStatus` 与 `recheck_status` 语义不同，不能混用
- Dashboard 当前主路径已完成从旧 recheck 占位叙事向读模型真值叙事的收口
- Push / Review 已具备行级 `recheckStatus` 真值链，无需在 Dashboard 再复制第二条权威入口
- 若将行级 `recheckStatus` 直接塞入 tile 或 executionBlock，容易重新引入“复核已通过/未通过”的伪语义耦合

### 5. 后续若要解冻的前提

只有在满足以下前提时，才考虑重新评估 Dashboard 是否承接 `recheckStatus`：

- 已明确 **按 symbol / 按 analysis / 按 push** 的归属规则
- 已明确 Dashboard 仅展示“说明性状态”还是“参与执行语义”
- 已明确 Review / Push / Dashboard 三处谁是权威入口、谁是引用展示

在上述条件未满足前，保持本冻结说明有效。

## recovery / confused recovery 语义冻结说明（草案）

### 1. 结论

当前阶段，**Dashboard 不承接“恢复条件”类业务真值字段**。  
Dashboard 目前与 recovery 相关的展示，分为两类：

- **真值数值链**：如 `confusedScore`、`confusedCount`、Push/Recheck 链上的 `currentConfusedScore`
- **说明性文案链**：如「恢复条件」「冲突来源」等阅读辅助文案

其中：

- **数值链属于后端真值**
- **说明性文案链当前不属于业务真值**

### 2. 语义边界

以下三类内容必须严格区分：

#### 2.1 困惑状态真值
用于回答：

- 当前这条决策是否处于困惑态
- 当前系统里有多少 symbol 处于困惑态
- Push/Recheck 过程中当前困惑分是多少

对应真值链包括：

- `tm_decision_result.confused_score`
- `tm_asset_state.confused_score`
- `tm_push_snapshot.confused_score_snapshot`
- `tm_push_recheck_log.current_confused_score`
- `DecisionResultVO.confusedScore`
- `LightSystemStatusVO.confusedCount`
- Review / Push 聚合中的 confusedScore 相关字段

该语义属于 **后端真值链**。

#### 2.2 recovery 说明性文案
用于回答：

- 从阅读角度，当前界面建议“等待什么条件恢复”
- 当前冲突说明里，用什么一句话帮助用户理解后续观察点

当前实现中的典型形式包括：

- 固定短句
- 文案层提示
- 启发式拼接说明

该语义当前属于 **Dashboard 展示层说明**，**不属于落库业务真值**。

#### 2.3 行级复扫 / 复核语义
用于回答：

- 某条 push / recheck 日志本身的复扫状态
- 某次复核是否通过、阻断、漂移、待确认

该语义对应：

- `tm_push_recheck_log.recheck_status`
- Push / Review 专用链路

该语义与 recovery 文案、困惑分值不同，不能混用。

### 3. 当前冻结决定

为避免混淆，当前阶段冻结如下：

1. **Dashboard 中“恢复条件”文案不视为业务真值**
2. **`confusedRecoveryHint` / `confused_recovery_hint` 当前视为未实现契约，不作为真实后端字段使用**
3. **Dashboard 继续展示 confused 数值类真值，但不将一句话 recovery 文案误表述为引擎输出**
4. **行级复扫 / 复核真值仍归 Push / Review 链路，不与 Dashboard recovery 文案混同**
5. **若未来要做 recovery 真值字段化，优先评估落在决策层，而不是 system 聚合层**

### 4. 原因

作出上述冻结决定的原因如下：

- 当前仓库已具备 confused 数值真值链，但**没有**稳定落库的 recovery 文案字段
- `confusedRecoveryHint` 在前端有预留读取路径，但 Java VO、数据库、API 均未提供对应真值
- 若把当前固定 recovery 文案误当成真值，会与已冻结的“禁止前端伪造业务结论”原则冲突
- `LightSystemStatusVO` 适合表达系统级数量统计，不适合承接单条决策的恢复条件说明
- Push / Review 上已有行级复扫真值链，Dashboard 不应再用 recovery 文案复制第二套近似语义

### 5. 后续若要解冻的前提

只有在满足以下前提时，才考虑将 recovery 说明从展示文案升级为真值字段：

- 已明确 recovery 的粒度：按 symbol、按 analysis、按 decision，还是按 push/recheck
- 已明确该字段仅作说明，还是会参与执行/阻断语义
- 已明确它应落在哪个对象：`DecisionResultVO`、review 结果，还是其它独立对象
- 已具备稳定可审计的后端写入来源，而不是前端静态句或启发式拼接

在上述前提未满足前，保持本冻结说明有效。

---

## systemHealth（Dashboard）当前阶段冻结说明

### 1. 当前 Dashboard `systemHealth` 的上屏范围

当前阶段，Dashboard **仅**将以下字段作为首屏健康区的常显输入：

| 字段 | 上屏口径（当前阶段） |
|------|----------------------|
| `databaseStatus` | **数据库连接**可用性（与 Layer1「API 状态收窄为数据库连接」叙事一致） |
| `schedulerStatus` | **持仓同步**相关活动的**轻量代理**：仅表达「是否在发生同步类活动」，不作为完整调度器健康结论 |

### 2. 当前不上屏的字段列表

以下字段 **当前明确不上屏**（不作为 Dashboard 首屏 `systemHealth` 常显项）：

- `cpuUsage`
- `memoryUsage`
- `schedulerStatusDetail`
- `databaseStatusDetail`

### 3. 为什么不上屏

| 字段 | 原因 |
|------|------|
| `cpuUsage` / `memoryUsage` | 当前命名与采集口径下，数值**容易被误读**（例如进程视角 vs 容器 vs 宿主机、峰值/均值、是否与交易主路径相关），不适合作为用户扫一眼即采信的首屏「系统健康」结论。 |
| `schedulerStatusDetail` | **Detail 形态**偏诊断与排障，信息密度高，**不适合首屏常驻**。 |
| `databaseStatusDetail` | 同上；连接类问题更适合在 drill-down / 运维视图中展开，而非首屏常显。 |

补充（与上屏范围的关系）：`schedulerStatus` **之所以仍上屏**，是因为当前阶段只把它当作**持仓同步活动的轻量代理**，刻意避免把它扩展成「全量调度健康度」叙事；重语义仍应走专门链路或后续独立呈现，而不是挤在首屏一句里。

### 4. 后续若要解冻的前提

仅当以下条件之一组被满足并经评审后，才可考虑将对应项**解冻上屏**或调整展示形态：

1. **`cpuUsage` / `memoryUsage`**
   - 已明确**采集语义**（采集点、归属边界、采样周期、是否与交易关键路径对齐）。
   - 已明确**展示名与说明文案**，避免用户将「宿主/容器空闲率」误读为「策略引擎健康度」等。
   - 必要时：调整指标定义或改为**非首屏**的二级区 / 运维页专用展示。

2. **`schedulerStatusDetail` / `databaseStatusDetail`**
   - 已明确**不应作为首屏常显**的定位；若展示，应有**独立入口**（展开面板、详情 Tab、运维 Dashboard 等），并约定何时从 summary 升级为 detail。
   - 若 product 坚持首屏出现，需另定 **UI 承载**（可折叠、限长、告警态才展开等），而非直接长文本常显。

在未满足上述前提前，保持本冻结说明有效：首屏 `systemHealth` **只承上表「上屏范围」**，其余字段视为**刻意冻结**。

---

## 当前阶段冻结结论页补充（极简）

### 1. 本阶段已收口

- review
- execution plan（当前阶段口径）：Dashboard s4 已贯通 **`recommendedAction` / `entryZone` / `stopLoss` / `takeProfitRules` / `leverageSuggestion` / `positionSuggestion`**（同源 latest-plan 行）；顶部含 **边界文案**（节选 + 派生摘要，非完整方案）；**不等于** execution 深层解释模块已完结
- data quality（当前展示与口径层）
- systemHealth（当前冻结口径）
- decision / execution 边界

### 2. 已推进但未完全冻结

- evidence（Dashboard `s2` + Review 页「结构化证据」已达三字段最小可感知消费；读模型仍 top3；完整证据解释层仍开放）
- score（Dashboard `s2` + Review 页「评分明细」已同源 top3 brief；八大评分与全解释层仍开放）
- execution plan **深层解释层**（窄表字段 + `takeProfitRules` 已在 Dashboard s4 **单点真值贯通**，但**仍非**完整执行解释模块；模板 / 账户风险 / 监控等 **仍未**与本 tab 一致化收口）

### 3. 下一阶段入口

- market environment（优先）
- score 全模块解释层
- execution 深解释层
- evidence / score **完整**解释层与更大范围 brief 字段（Review 已完成**最小**结构化承接，不等于模块完结）
- 冻结提示：market environment 当前已完成最小承接闭环——主链最小启发式切面（24h quote heuristic）+ `tm_market_environment_snapshot`（`analysis_id` 对齐事实锚点）+ Dashboard detail snapshot 优先（未命中 fallback heuristic）+ Review aggregate snapshot 读取；但这仍不代表完整市场环境模块交付（非多源、非独立大模块 UI）

### 4. 本阶段冻结索引 Top 5

1) validPeriod / invalidCondition 已真实化第一刀且允许 null
2) executionPlanSummary 为派生字段（**仅** valid/invalid 拼接摘要，**非**完整执行计划解释）
3) Dashboard s4 已完成 decision / execution 语义拆标；**顶部边界文案**约束「计划表节选 + 派生摘要」，勿将本 tab 视为完整执行方案
4) Dashboard s4 已贯通 **latest-plan** 窄表字段：`recommendedAction`、`entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion`（**同源 `rn = 1` 行**）；**`takeProfitRules`** 以 **独立多行块**展示且 **不与** `executionPlanSummary` 拼接；Review Plan 仍为更完整承接语境，但窄表字段与 Dashboard **已对齐**
5) run 级 data quality 与 push/recheck 质量口径已分离；systemHealth 首屏承载范围已冻结（Layer1 口径另见上文 §systemHealth）

> **说明：** 上列第 4、5 点为 **execution 线文档同步后**的冻结索引表述；**execution 深层解释**（模板化、账户风险 JSON、监控告警一致化等）仍属 **§2 / §3「未完全冻结 / 下一阶段」**，未因窄表贯通而宣告完结。

### 5. market environment 阶段收口补充（极小）

- 已收口：heuristic 主链接入 + `tm_market_environment_snapshot` 落库 + Dashboard detail snapshot-first（`analysis_id` 对齐）+ Review aggregate 同源读取 + Dashboard `s1` `marketEnvironmentMini` 极小独立块。
- 仍最小实现：当前仍是单一 24h heuristic，非多源，且非完整独立市场环境模块 UI。
- 下一阶段：多源融合、独立模块化 UI、规则治理化（阈值/映射可配置）。

### 6. evidence / score / market environment 协同链阶段收口补充（极小）

- 已收口：最小协同链已成立——`market environment`（heuristic -> snapshot）+ evidence（`analysis_id -> evidenceTopItems(top3)`）-> score（`analysis_id -> scoreTopItems(top3)`）-> decision（结论字段链）。
- 已收口补充（evidence 三字段）：evidence 已完成最小受控骨架——`evidence_type`（`价格结构/杠杆/资金/事件/风险`）+ `direction`（`BULLISH/BEARISH/NEUTRAL`）+ `source`（`SYSTEM_GENERATED/MARKET_HEURISTIC/MANUAL_INPUT`），并具备生成侧受控与写库前 normalize/fallback 兜底。
- **已收口补充（evidence 展示消费，Dashboard `s2`）：** `evidenceTopItems` 已进入 **三字段最小可感知消费**：除 `evidenceType` / `description` 外，`direction` 作为主标签、`source` 作为次级 muted 信息上屏；**不再沿用**「Dashboard 仅 type + description」或「direction/source 全局未消费」的旧口径。
- **已收口补充（Review 最小结构化复盘）：** `ReviewAggregateVO` 已挂载 **`evidenceTopItems`、`scoreTopItems`**（与 Dashboard detail **同源** brief）；复盘页已增加只读 **「结构化证据（前3条）」「评分明细（前3条）」**；**请勿再按旧口径**将 Review 理解为「仅 `evidenceSummary`/`reviewReasons`」或「结构化解释仅在 Dashboard detail」。**`evidenceSummary`** 仍在决策摘要路径保留，**不等于**结构化证据解释层。
- **未收口（事实边界）：** 读模型仍为 **top3**；brief **未**扩 `description`/`direction`/`weight`（score）等额外列；**未**做大模块化复盘信息架构；证据/评分线**仍非**完整解释模块。
- 下一阶段：证据 / score **完整**解释层治理化扩展、score 维度扩展、market environment 多源化。

### 7. score 已进入 8/8（新增事件冲击分）阶段收口补充（极小）

- 已收口：score 已从 5/8 进入 8/8（新增「事件冲击分」）并上屏最小承接（Dashboard `s2` 的 `scoreTopItems(top3)`）；**Review 聚合 + 复盘页「评分明细（前3条）」**已与 Dashboard **同源 brief** 对齐。本轮仅新增“事件冲击分”的写分与回放，不新增 decision 硬闸门；事件第二刀输入契约已实现并进入观察态（Implemented + Verified + Observation）。scoreTopItems(top3) = 同源 brief 裁剪视图（按记录顺序），不代表按 scoreValue 排序，不代表评分优先级排序，不等于完整评分解释模块。
- 未收口：八大评分与完整解释层未完成。
- 下一阶段：按维度增量扩展，不做一次性铺开。
