# Fundamental AI 三 AI + CoinGlass 语义审计闭环

## Scope / 范围

本次只修复既有 v4.1 决策链与 Desktop Home 的语义、真实数据输入和展示缺口。登录、Telegram、Figma、Mobile、Schema、业务阈值、自动开仓/平仓/加减仓/反手、下单与自动交易均不在范围内。

## Product Contract Mapping / 产品契约映射

- 权威来源：`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`。
- 决策链：多源证据 → GPT Candidate → Gemini Review → Grok Challenge → Conflict Resolver → Rule Validation → Final Execution Plan。
- GPT 只形成候选；Gemini 只复核可信度、缺口、冲突与低估风险；Grok 只验证失败路径；只有 Resolver + Rule Validation 后的结果可进入 Final。
- 三 AI 必须联合 K 线多周期、成交量、CoinGlass 未平仓量、加权资金费率、强平和多空比，不得把回答缩减成 K 线技术分析。
- CoinGlass 不可用、部分可用或过期时必须明说，且不得声称衍生品确认。

## Three-AI User Semantics / 三 AI 用户语义

### GPT 候选判断

先回答：方向是什么、现在该做什么。随后用白话说明哪些现货与衍生品证据支持，哪些证据限制，以及下一条可验证触发条件。页面必须明确“不是最终计划”。

### Gemini 可信度复核

先回答：候选可以维持、需要降级、候选不成立，还是存在明显风险。随后说明缺什么、哪里冲突、哪个风险被低估、止损和来源是否可靠，以及怎样恢复。

### Grok 失败压力测试

先回答：候选最可能怎样失败。随后给出触发 → 演化 → 失效链路，并列出需要继续观察的价格、成交量、未平仓量、资金费率、拥挤度、强平、微观结构和外部事件指标。

## CoinGlass Interpretation / CoinGlass 解读边界

- 价格上涨 + OI 上升 + 成交量确认：可作为新增多头参与的确认证据。
- 价格下跌 + OI 上升 + 成交量确认：可作为新增空头参与的确认证据。
- 价格上涨 + OI 下降：更接近空头回补，不能当成新多头确认。
- 价格下跌 + OI 下降：更接近去杠杆，不能当成新空头确认。
- 极端资金费率 + 同方向拥挤：提高挤压和反向波动风险。
- 强平是被迫成交，不是独立方向证据。
- 多空比是拥挤度证据，不等于资金流入，也不能单独证明方向。

## Data Source Mapping / 数据源映射

| 数据 | 现有来源 | 进入三 AI 的方式 | Home 表达 |
|---|---|---|---|
| 4h / 1h / 15m / 5m K 线与成交量 | 持久化真实 OHLCV | 通用 evidence + multi-timeframe | 多周期结构与触发条件 |
| OI 与 1m/5m/15m/1h 变化 | CoinGlass v4 snapshot | `derivativesContext.datasetReadings.openInterest` + 可追踪衍生品 evidence | 未平仓量结构 |
| 加权资金费率与极值 | CoinGlass v4 snapshot | `derivativesContext.datasetReadings.funding` | 资金费率风险 |
| 多空比与来源 | CoinGlass v4 snapshot | `derivativesContext.datasetReadings.longShortRatio` | 多空拥挤 |
| 多/空强平 1m/5m/15m/1h | CoinGlass v4 snapshot | `derivativesContext.datasetReadings.liquidation` | 强平风险 |
| 数据状态、时间、缺失/降级原因 | provider snapshot + business assessment | 独立上下文，不能被通用证据 20 条窗口截掉 | 独立显示数据时间与可用状态 |

Home 的 CoinGlass 条带是页面读取时的最新缓存风险参照，并独立显示数据时间；它不会反向改写已经生成的 AI 结论。AI 结论使用的是该次 Analysis Run 传入并记录 trace 的衍生品快照。

## Stop-Loss Audit / 止损审计

正式链路已经具备真实止损生成：市场结构边界提取 → 来源追踪 → Candidate `stopLogic/stopZone/stopSource/stopReason` → Rule Validation → Final Plan。缺失原因不是后端没生成，而是 Home renderer 只展示“入场 / 失效 / 目标”，漏掉了独立 `stopZone/stopLoss`。本次已增加独立“止损”字段，且继续与“失效条件”分开显示。

## Position Close Audit / 持仓关闭审计

首页“查看详情”已进入 `/positions/{positionId}`，详情页已有“记录平仓”，提交 owner-scoped `manual-close` 后持仓变为 CLOSED，并进入 Review。该路径已存在且符合“只允许用户手动平仓”，本次不创建第二套关闭流程。

## Findings / 审计发现

```text
FINDING_ID: 3AI-COINGLASS-001
BLOCKER_CLASS: REAL_DATA_INTEGRITY_BLOCKER
DIRECT_PRODUCT_IMPACT: CoinGlass 派生证据追加在通用 evidence 后，前 20 条窗口可能把它排除，三 AI 无法稳定取得或引用衍生品证据。
REPRODUCTION_EVIDENCE: DecisionChainServiceImpl.evidenceFacts(...).limit(20)，且此前没有 derivativesContext；AI trace validator 也只索引通用 evidence。
BLOCKS_CURRENT_STAGE: YES
```

```text
FINDING_ID: 3AI-SEMANTIC-002
BLOCKER_CLASS: PRODUCT_SEMANTIC_BLOCKER
DIRECT_PRODUCT_IMPACT: 三角色提示词没有要求联合 K 线与 CoinGlass，也没有要求白话结论优先，用户看到的是技术字段而不是可理解的判断。
REPRODUCTION_EVIDENCE: 旧 system instruction 只有角色权限描述；Home 使用 GPT Candidate / Market Bias / Before → After 等技术文案。
BLOCKS_CURRENT_STAGE: YES
```

```text
FINDING_ID: 3AI-UI-003
BLOCKER_CLASS: REAL_DATA_INTEGRITY_BLOCKER
DIRECT_PRODUCT_IMPACT: DashboardHomeVO 已返回 derivatives，但 Home runtime 未消费；Gemini 拒绝枚举与受控 Fixture 的等级/调整值不符合结构化合同。
REPRODUCTION_EVIDENCE: 旧 renderAi 未引用 home.derivatives；旧 UI 只接受 REJECT；Fixture 使用 84%、PREPARATION 和 LOW → MEDIUM 代替合同枚举。
BLOCKS_CURRENT_STAGE: YES
```

```text
FINDING_ID: FINAL-STOP-004
BLOCKER_CLASS: PRODUCT_SEMANTIC_BLOCKER
DIRECT_PRODUCT_IMPACT: Final Plan 已有真实、可追踪止损，但首页不展示，用户无法区分止损与一般失效条件。
REPRODUCTION_EVIDENCE: 旧 renderPlan 只绑定 entry、invalidCondition、target，未绑定 stopZone/stopLoss。
BLOCKS_CURRENT_STAGE: YES
```

## Closure / 修复闭环

- CoinGlass 四类数据、状态、时间、缺失/降级原因和派生证据以独立上下文进入三个 AI；派生证据同时进入输出 trace allowlist。
- 三角色提示词固定为简体中文、结论优先、白话解释，并冻结 OI/价格、资金费率/拥挤、强平、多空比的解释边界。
- Home 展示 CoinGlass 实况、数据时间、四类摘要与结论影响；正常/提醒/风险/信息分别使用绿/橙/红/蓝语义色。
- GPT、Gemini、Grok 首页第一视觉分别回答“候选结论”“能否相信”“最可能怎样失败”。
- 修正 Gemini `REJECT_CANDIDATE` 及 Fixture 中的结构化等级、调整枚举和可追踪证据。
- Final Plan 独立展示止损和失效条件；持仓监控判断字段居中，风险等级继续按语义色区分。
- 未新增 DTO、Assembler、Orchestrator 或第二套运行时所有者。

## Delivery Boundaries / 交付边界

```text
PRODUCT_WORK_RATIO: 90%
NON_PRODUCT_WORK_RATIO: 10%
STOP_RULE_TRIGGERED: NO

是否创建新骨架: No
是否复用 Cursor-era 资产: Yes
是否减少重复: Yes
是否提升 capability level: Yes — 既有真实数据从后端业务调整提升到三 AI 可解释输入与 Home 可见语义
是否接 service/runtime/dashboard/API: Yes
是否符合 #830 审计建议: Yes
```

## External Primary References / 外部主来源

- CoinGlass API v4 getting started: <https://docs.coinglass.com/reference/getting-started-with-your-api>
- CoinGlass endpoint overview: <https://docs.coinglass.com/reference/endpoint-overview>
- Global long/short account ratio: <https://docs.coinglass.com/reference/global-longshort-account-ratio>
- Aggregated liquidation history: <https://docs.coinglass.com/reference/aggregated-liquidation-history>
