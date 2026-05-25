# V1 Current State（V1 当前状态）

本文件用于记录 Trade Model V1 当前阶段。以后每次 PR（合并请求）合并后，必须优先更新本文件或在下一次 progress index（进度索引）刷新中同步更新。

## 1. 当前本地 / 远端主线

当前 main（主分支）基准：

```text
9c6c58a BACKEND-P251 Market Score Candidate Push Readiness Scope Pack (#621)
```

说明：WORKFLOW-P1 已合并，P204 已合并，P205 已完成并合并，P206 已完成并合并，P207 已完成并合并，P208 已完成并合并，P209 已完成并合并，P210 已完成并合并，P211 已完成并合并，P212 已完成并合并，P213 已完成并合并，P214 已完成并合并，P215 已完成并合并，P216 已完成并合并，P217 已完成并合并，P218 已完成并合并，P219 已完成并合并，P220 已完成并合并，P221 已完成并合并，P222 已完成并合并，P223 已完成并合并，P224 已完成并合并，P225 已完成并合并，P226 已完成并合并，P227 已完成并合并，P228 已完成并合并，P229 已完成并合并，P230 已完成并合并，P231 已完成并合并，P232 已完成并合并，P233 已完成并合并，P234 已完成并合并，P235 已完成并合并，P236 已完成并合并，P237 已完成并合并，P238 已完成并合并，P239 已完成并合并，P240 已完成并合并，P241 已完成并合并，P242 已完成并合并，P243 已完成并合并，P244 已完成并合并，P245 已完成并合并，P246 已完成并合并，P247 已完成并合并，P248 已完成并合并，P249 已完成并合并，P250 已完成并合并，P251 已完成并合并。当前主线基准为 P251 合并后状态。

## 2. 当前已完成主线

近期已完成：

```text
P197：Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）
P198：Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）
P199：Watchlist Low-Frequency Scan / Opportunity Promote Minimal Wiring（观察库低频扫描 / 机会提升最小接线，docs-only）
P200：Watchlist Low-Frequency Scan / Opportunity Promote Closure（观察库低频扫描 / 机会提升收口）
P201：Project Progress Index Refresh After Watchlist Scan Promote Semantics（观察库扫描提升语义后项目总进度索引刷新）
P202：Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）
P203：Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）
P204：Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）
P205：Max Safe Docs Pack After P204 Scheduler Skeleton（P204 定时器骨架后的最大安全文档包）
P206：Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包）
P207：Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包）
P208：Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）
P209：Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包）
P210：Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）
P211：Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门）
P212：Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）
P213：Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）
P214：Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）
P215：Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）
P216：Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）
P217：Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）
P218：Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）
P219：Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门）
P220：Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架）
P221：Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门）
P222：Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案）
P223：Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案）
P224：Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架）
P225：Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门）
P226：Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案）
P227：Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门）
P228：Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现）
P229：Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门）
P230：DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计）
P231：DB Watchlist Pool Read Audit Closure and Java Authorization Gate（DB 观察库池读取审计收口与 Java 授权门）
P232：DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）
P233：DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）
P234：Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）
P235：Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）
P236：Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）
P237：Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）
P238：Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）
P239：Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）
P240：Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）
P241：Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）
P242：Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）
P243：Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）
P244：Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）
P245：Scheduler Trigger Authorization Gate（定时器触发授权门）
P246：Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）
P247：Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）
P248：Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）
P249：Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）
P250：Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）
P251：Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）
```

## 3. 当前项目真实状态

当前 Trade Model V1 已经完成大量 read-only（只读）、review-only（只允许复核）、fail-closed（失败关闭）基础。

当前已经完成：

- SourceTrace（证据来源追踪）只读展示。
- BoundaryCandidate（边界候选交易计划）只读候选展示。
- ExecutionPlan（执行计划）只允许复核展示。
- Risk Action Guard（风险动作保护器）只读风险展示。
- Position Monitor（持仓监控）强反转 / 移动止损只读展示。
- Dashboard Risk Reminder（首页风险提醒）只读展示。
- Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）语义边界。
- Low-Frequency Scan Scheduler（低频扫描定时器）范围审计和授权门。
- Low-Frequency Scan Scheduler（低频扫描定时器）disabled-by-default skeleton（默认关闭骨架）。
- Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）。
- Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）。
- Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门）。
- Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）。
- Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）。
- Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）。
- Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）。
- Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）。
- Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）。
- Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）。
- Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门）。
- Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架）。
- Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门）。
- Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案）。
- Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案）。
- Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架）。
- Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门）。
- Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案）。
- Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门）。
- Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现）。
- Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门）。
- DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计）。
- DB Watchlist Pool Read Audit Closure and Java Authorization Gate（DB 观察库池读取审计收口与 Java 授权门）。
- DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）。
- DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）。
- Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）。
- Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。
- Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。
- Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。
- Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。
- Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。
- Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。
- Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。
- Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。
- Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。
- Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。
- Scheduler Trigger Authorization Gate（定时器触发授权门）。
- Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。
- Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。
- Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。
- Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。
- Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。
- Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。

当前仍未完成：

- 真实低频扫描。
- Watchlist runtime data source（观察库运行时数据源）。
- production runtime source read（生产运行时数据源读取）。
- production read adapter（生产读取适配器）。
- production adapter implementation（生产适配器实现）。
- production read implementation（生产读取实现）。
- DB-backed Watchlist Pool read production implementation（数据库观察库池读取生产实现）。
- production Runtime Source Service wiring（生产运行时数据源服务接线）。
- Low-Frequency Watchlist Scan Orchestrator Java production wiring（低频观察库扫描编排器生产接线）。
- scheduler-triggered orchestrator（定时器触发编排器）。
- scheduler-triggered batch（定时器触发批量扫描）。
- Batch Watchlist Scan production wiring（批量观察库扫描生产接线）。
- real batch scan（真实批量扫描）。
- batch scheduler（批量扫描定时器）。
- real scan loop（真实扫描循环）。
- Production Source Assembler（生产数据源组装器）。
- MarketQuoteClient scan integration（行情客户端扫描接入）。
- runtime data reads（运行时数据读取）。
- scan loop（扫描循环）。
- ScanScore（扫描分数）。
- Candidate Attention（候选关注）。
- Promote To Home（提升到首页观察）运行时逻辑。
- Opportunity Push execution（机会推送执行）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ExecutionPlan readiness（执行计划可执行就绪）。
- order API（下单接口）。
- execution API（执行接口）。
- auto-trading（自动交易）不在 V1 范围内，保持关闭，只作为禁止越界安全边界。

## 4. 当前 open PR（未合并请求）

当前已创建但尚未完成的 PR：

```text
PR #623：BACKEND-P252 Market Read Adapter Skeleton
Branch：p252
Issue：#622
风险档位：B/C boundary read-only fail-closed market-read adapter skeleton, no real market client, no scheduler, no scan loop, no score, no API/dashboard wiring
状态：Draft PR（草稿合并请求）
```

P252 只允许完成最大安全 read-only fail-closed Market-read adapter skeleton（只读、失败关闭的行情读取适配器骨架）：新增 `WatchlistMarketReadAdapter`、`DefaultWatchlistMarketReadAdapter` 和 targeted test（目标测试），并更新 P252 verification（验证文档）、当前状态和进度索引。P252 不修改既有 Java / test / DTO / guard / validator / assembler / orchestrator，不接真实 MarketQuoteClient / BinanceMarketQuoteClient，不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不接 scheduler（定时器），不创建真实 scan loop（扫描循环），不创建真实扫描，不生成 ScanScore / Candidate Attention / Promote To Home / Push / readiness / entry-stop-TP-RR / trading。

P252 禁止：

- 修改既有 Java。
- 修改既有测试。
- 修改既有 DTO 文件。
- 修改 guard / validator 文件。
- 修改 assembler / orchestrator 文件。
- 改 dashboard.html。
- 改 schema（数据库结构）。
- 改 config（配置）。
- 接 API（接口）。
- 改既有 service / scheduler implementation（服务 / 定时器实现）。
- 改 mapper（数据库映射）。
- 接真实 MarketQuoteClient（行情客户端）。
- 接真实 BinanceMarketQuoteClient（币安行情客户端）。
- 接 scheduler（定时器）。
- 读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 创建真实 scan loop（真实扫描循环）。
- 扫描真实资产。
- 实现 MarketQuoteClient read（行情客户端读取）。
- 实现 scheduler-triggered read（定时器触发读取）。
- 实现 production read implementation（生产读取实现）。
- 实现 production Runtime Source Service wiring（生产运行时数据源服务接线）。
- 实现 Production Source Assembler（生产数据源组装器）。
- 修改 `schema.sql`。
- 生成 ScanScore（扫描分数）。
- 生成 Candidate Attention（候选关注）。
- 生成 Promote To Home（提升到首页观察）。
- 创建 opportunity push execution（机会推送执行）。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 readiness（可执行就绪）。
- auto-trading（自动交易）不在 V1 范围内，保持关闭，只作为禁止越界安全边界。

## 5. 当前 open Issue（未关闭问题单）

```text
#622：BACKEND-P252 Market Read Adapter Skeleton
```

P204、P205、P206、P207、P208、P209、P210、P211、P212、P213、P214、P215、P216、P217、P218、P219、P220、P221、P222、P223、P224、P225、P226、P227、P228、P229、P230、P231、P232、P233、P234、P235、P236、P237、P238、P239、P240、P241、P242、P243、P244、P245、P246、P247、P248、P249、P250、P251 和 WORKFLOW-P1 已合并，不再作为当前 open PR（未合并请求）处理。

## 6. 下一步推荐

当前优先级：

```text
完成 P252 Market Read Adapter Skeleton。
```

P252 属于 B/C boundary read-only fail-closed Market-read adapter skeleton（只读、失败关闭的行情读取适配器骨架）。本轮只新增 market-read adapter interface（接口）、默认 no-op adapter（无动作适配器）、targeted test（目标测试）和 P252 verification（验证文档），并更新当前状态和进度索引；不修改既有 Java / test / DTO / guard / validator / assembler / orchestrator，不改 dashboard，不改 schema，不改 config，不接 API，不接真实 MarketQuoteClient（行情客户端）或 BinanceMarketQuoteClient（币安行情客户端），不启用 scheduler（定时器），不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不创建真实 scan loop（扫描循环）或真实扫描，不实现 ScanScore（扫描分数），不创建 Candidate Attention workflow（候选关注流程）或 Promote To Home workflow（提升到首页观察流程）。

## 7. 当前禁止越界

除非后续 Issue（问题单）和 Authorization Gate（授权门）明确允许，否则禁止：

- 自动下单。
- 自动平仓。
- 自动反手。
- 自动修改止损。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 ExecutionPlan readiness（执行计划可执行就绪）。
- 接 order API（下单接口）。
- 接 execution API（执行接口）。
- auto-trading（自动交易）不在 V1 范围内，保持关闭，只作为禁止越界安全边界。
- 把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）。
- 默认六币固定推送。
- 非观察库资产进入机会推送候选。
- 在踩踏状态推送机会、反手或新开仓。
- 把短线插针当作趋势反转。

## 8. 合并后同步命令

每次 PR（合并请求）合并后，本地同步命令固定为：

```bash
cd /Users/xuchao/Documents/trade-model-v1 && git switch main && git pull origin main && git status && git log --oneline -5
```
