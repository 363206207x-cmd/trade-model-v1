# V1 Current State（V1 当前状态）

本文件用于记录 Trade Model V1 当前阶段。以后每次 PR（合并请求）合并后，必须优先更新本文件或在下一次 progress index（进度索引）刷新中同步更新。

## 1. 当前本地 / 远端主线

当前 main（主分支）基准：

```text
bd1bfeb BACKEND-P280 Real Scan Input Contract DTO Closure and Guard Validator Authorization Scope Pack (#679)
```

说明：WORKFLOW-P1 已合并，P204 已合并，P205 已完成并合并，P206 已完成并合并，P207 已完成并合并，P208 已完成并合并，P209 已完成并合并，P210 已完成并合并，P211 已完成并合并，P212 已完成并合并，P213 已完成并合并，P214 已完成并合并，P215 已完成并合并，P216 已完成并合并，P217 已完成并合并，P218 已完成并合并，P219 已完成并合并，P220 已完成并合并，P221 已完成并合并，P222 已完成并合并，P223 已完成并合并，P224 已完成并合并，P225 已完成并合并，P226 已完成并合并，P227 已完成并合并，P228 已完成并合并，P229 已完成并合并，P230 已完成并合并，P231 已完成并合并，P232 已完成并合并，P233 已完成并合并，P234 已完成并合并，P235 已完成并合并，P236 已完成并合并，P237 已完成并合并，P238 已完成并合并，P239 已完成并合并，P240 已完成并合并，P241 已完成并合并，P242 已完成并合并，P243 已完成并合并，P244 已完成并合并，P245 已完成并合并，P246 已完成并合并，P247 已完成并合并，P248 已完成并合并，P249 已完成并合并，P250 已完成并合并，P251 已完成并合并，P252 已完成并合并，P253 已完成并合并，P254 已完成并合并，P255 已完成并合并，P256 已完成并合并，P257 已完成并合并，P258 已完成并合并，P259 已完成并合并，P260 已完成并合并，P261 已完成并合并，P262 已完成并合并，P263 已完成并合并，P264 已完成并合并，P265 已完成并合并，P266 已完成并合并，P267 已完成并合并，P268 已完成并合并，P269 已完成并合并，P270 已完成并合并，P271 已完成并合并，P272 已完成并合并，P273 已完成并合并，P274 已完成并合并，P275 已完成并合并，P276 已完成并合并，P277 已完成并合并，P278 已完成并合并，P279 已完成并合并，P280 已完成并合并。当前主线基准为 P280 合并后状态。

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
P252：Market Read Adapter Skeleton（行情读取适配器骨架）
P253：ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）
P254：ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）
P255：Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）
P256：Candidate Attention Review-Only Skeleton（候选关注只允许复核骨架）
P257：Opportunity Push Authorization and Risk Guard Gate Pack（机会推送授权与风险保护门包）
P258：Opportunity Push Review-Only Skeleton（机会推送只允许复核骨架）
P259：Opportunity Push Closure and External Channel Gate Pack（机会推送收口与外部通道授权门包）
P260：Push Channel Disabled No-Op Java Skeleton（推送通道禁用 no-op Java 骨架）
P261：Push Channel No-Op Closure and Delivery Wiring Gate（推送通道 no-op 收口与投递接线授权门）
P262：Audit-Only Delivery Wiring Java Skeleton（审计-only 投递接线 Java 骨架）
P263：Audit Envelope Closure and Delivery Pipeline Scope Pack（审计信封收口与投递流水线范围包）
P264：Audit Envelope Persistence No-Op Java Skeleton（审计信封持久化 no-op Java 骨架）
P265：Audit Persistence Closure and Audit Queue Authorization Scope Pack（审计持久化收口与审计队列授权范围包）
P266：Audit Queue No-Op Java Skeleton（审计队列 no-op Java 骨架）
P267：Audit Queue Closure and Delivery Pipeline Authorization Scope Pack（审计队列收口与投递流水线授权范围包）
P268：Delivery Pipeline Disabled No-Op Java Skeleton（投递流水线禁用 no-op Java 骨架）
P269：Delivery Pipeline Closure and Message Envelope Authorization Scope Pack（投递流水线收口与消息信封授权范围包）
P270：Message Envelope Disabled No-Op Java Skeleton（消息信封禁用 no-op Java 骨架）
P271：Message Envelope Closure and Provider Channel Authorization Scope Pack（消息信封收口与 provider channel 授权范围包）
P272：Provider Channel Disabled No-Op Java Skeleton（provider channel 禁用 no-op Java 骨架）
P273：Provider Channel Closure and External Channel Authorization Scope Pack（provider channel 收口与 external channel 授权范围包）
P274：External Channel Disabled No-Op Java Skeleton（external channel 禁用 no-op Java 骨架，本地验证例外合并）
P275：External Channel Closure and CI Exception Record Scope Pack（external channel 收口与 CI 例外记录范围包）
P276：Real Scan Score Candidate Return Scope Pack（real scan / real score / Candidate / Push preview planning 回归范围包）
P277：Real Scan Contract Audit and Market-Read Boundary Plan（真实扫描契约审计与 market-read 边界方案）
P278：Real Scan Input Contract DTO Authorization Gate（真实扫描输入契约 DTO 授权门）
P279：Real Scan Input Contract DTO Skeleton（真实扫描输入契约 DTO 骨架）
P280：Real Scan Input Contract DTO Closure and Guard Validator Authorization Scope Pack（真实扫描输入契约 DTO 收口与 Guard Validator 授权范围包）
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
- Market Read Adapter Skeleton（行情读取适配器骨架）。
- ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。
- ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。
- Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。
- Candidate Attention Review-Only Skeleton（候选关注只允许复核骨架）。
- Opportunity Push Authorization and Risk Guard Gate Pack（机会推送授权与风险保护门包）。
- Opportunity Push Review-Only Skeleton（机会推送只允许复核骨架）。
- Opportunity Push Closure and External Channel Gate Pack（机会推送收口与外部通道授权门包）。
- Push Channel Disabled No-Op Java Skeleton（推送通道禁用 no-op Java 骨架）。
- Push Channel No-Op Closure and Delivery Wiring Gate（推送通道 no-op 收口与投递接线授权门）。
- Audit-Only Delivery Wiring Java Skeleton（审计-only 投递接线 Java 骨架）。
- Audit Envelope Closure and Delivery Pipeline Scope Pack（审计信封收口与投递流水线范围包）。
- Audit Envelope Persistence No-Op Java Skeleton（审计信封持久化 no-op Java 骨架）。
- Audit Persistence Closure and Audit Queue Authorization Scope Pack（审计持久化收口与审计队列授权范围包）。
- Audit Queue No-Op Java Skeleton（审计队列 no-op Java 骨架）。
- Audit Queue Closure and Delivery Pipeline Authorization Scope Pack（审计队列收口与投递流水线授权范围包）。
- Delivery Pipeline Disabled No-Op Java Skeleton（投递流水线禁用 no-op Java 骨架）。
- Delivery Pipeline Closure and Message Envelope Authorization Scope Pack（投递流水线收口与消息信封授权范围包）。
- Message Envelope Disabled No-Op Java Skeleton（消息信封禁用 no-op Java 骨架）。
- Message Envelope Closure and Provider Channel Authorization Scope Pack（消息信封收口与 provider channel 授权范围包）。
- Provider Channel Disabled No-Op Java Skeleton（provider channel 禁用 no-op Java 骨架）。
- Provider Channel Closure and External Channel Authorization Scope Pack（provider channel 收口与 external channel 授权范围包）。
- External Channel Disabled No-Op Java Skeleton（external channel 禁用 no-op Java 骨架，本地验证例外合并）。
- External Channel Closure and CI Exception Record Scope Pack（external channel 收口与 CI 例外记录范围包）。
- Real Scan Score Candidate Return Scope Pack（real scan / real score / Candidate / Push preview planning 回归范围包）。
- Real Scan Contract Audit and Market-Read Boundary Plan（真实扫描契约审计与 market-read 边界方案）。
- Real Scan Input Contract DTO Authorization Gate（真实扫描输入契约 DTO 授权门）。
- Real Scan Input Contract DTO Skeleton（真实扫描输入契约 DTO 骨架）。
- Real Scan Input Contract DTO Closure and Guard Validator Authorization Scope Pack（真实扫描输入契约 DTO 收口与 Guard Validator 授权范围包）。

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
- RealScanInputContractGuardValidator skeleton（真实扫描输入契约保护校验器骨架）。
- production ScanScore computation（生产级扫描分数计算）。
- Candidate Attention production workflow（候选关注生产流程）。
- Promote To Home（提升到首页观察）运行时逻辑。
- Opportunity Push execution（机会推送执行）。
- delivery wiring（推送投递接线）。
- real audit persistence DB write / audit queue implementation（真实审计持久化 DB 写入 / 审计队列实现）。
- delivery pipeline（推送投递流水线）。
- real message envelope behavior（真实消息信封行为）。
- real provider channel（真实 provider channel）。
- real external channel behavior（真实外部通道行为）。
- message rendering / message sending（消息渲染 / 消息发送）。
- provider credentials / live provider calls（provider 凭证 / 实时 provider 调用）。
- provider selection / provider integration（provider 选择 / provider 集成）。
- external push channel（Telegram / email / webhook / app notification / local notification）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- ExecutionPlan readiness（执行计划可执行就绪）。
- order API（下单接口）。
- execution API（执行接口）。
- auto-trading（自动交易）不在 V1 范围内，保持关闭，只作为禁止越界安全边界。

## 4. 当前 open PR / 当前任务包（未合并请求）

当前已创建但尚未完成的 Draft PR：

```text
PR #681：BACKEND-P281 RealScanInputContractGuardValidator Skeleton
Branch：p281
Issue：#680
风险档位：B/C Java skeleton, targeted-test-only, no Spring annotation/controller/endpoint/API/scheduler/MarketQuoteClient/BinanceMarketQuoteClient/runtime data read/scan output/real scan loop/production ScanScore/Candidate workflow/Push execution/Readiness/point generation/trading path
状态：Draft PR（草稿合并请求）
```

P280 merged as `bd1bfeb`。P280 收口 P279 real scan input contract DTO skeleton，并授权 P281 可以新增 `RealScanInputContractGuardValidator` skeleton + targeted test。P279 新增 `RealScanInputContractDTO`、`RealScanInputContractStatusEnum`、`RealScanInputContractDTOTest`，且 P279 CI passed before merge。P279 只是 DTO-only / enum-only / targeted-test-only skeleton：`RealScanInputContractDTO` 默认 `manualReviewRequired=true`、`notTradeInstruction=true`，missing Watchlist Pool proof fails closed，non-watchlist input fails closed，valid-looking input remains review-only and not trade instruction，且 DTO 没有 trade action / order / execution / entry / stop / take profit / RR / provider / external channel / message sending / readiness fields。

P281 只能新增 GuardValidator interface / default implementation / targeted test / verification。P281 的 validator 只能验证 DTO safety / Watchlist Pool proof / fail-closed states / review-only flags，必须保持 `manualReviewRequired=true` 和 `notTradeInstruction=true`。P281 不读 market data，不调用 MarketQuoteClient，不创建 scan output，不计算 score，不创建 Candidate，不触发 Push，不升级 Readiness，也不生成 point。

## 5. 当前 open Issue（未关闭问题单）

```text
#680：BACKEND-P281 RealScanInputContractGuardValidator Skeleton
```

P204、P205、P206、P207、P208、P209、P210、P211、P212、P213、P214、P215、P216、P217、P218、P219、P220、P221、P222、P223、P224、P225、P226、P227、P228、P229、P230、P231、P232、P233、P234、P235、P236、P237、P238、P239、P240、P241、P242、P243、P244、P245、P246、P247、P248、P249、P250、P251、P252、P253、P254、P255、P256、P257、P258、P259、P260、P261、P262、P263、P264、P265、P266、P267、P268、P269、P270、P271、P272、P273、P274、P275、P276、P277、P278、P279、P280 和 WORKFLOW-P1 已合并，不再作为当前 open PR（未合并请求）处理。

## 6. 下一步推荐

当前优先级：

```text
完成 P281 RealScanInputContractGuardValidator Skeleton。
```

P281 属于 B/C Java skeleton，targeted-test-only。P281 只允许新增 GuardValidator interface / default implementation / targeted test / verification。P281 不加 Spring 注解，不改 DTO，不改 dashboard，不改 schema / config，不接 API，不新增 controller / endpoint / mapper / repository / scheduler，不接 MarketQuoteClient 或 BinanceMarketQuoteClient，不读取 runtime / live / external data，不创建 scan output，不创建 real scan loop，不实现 production ScanScore computation，不实现 Candidate Attention production workflow，不实现 Promote To Home runtime logic，不实现 Opportunity Push execution，不实现 external channel behavior，不处理 provider credentials，不做 live provider call，不实现 message rendering，不发送任何 message，不接 Telegram / email / webhook / app notification / local notification，不升级 Readiness，不生成 point generation 或 entry / stop / TP / RR，不接 order / execution / auto-trading。

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
- 接 Telegram / email / webhook / app notification / local notification。
- 发送任何外部或本地推送消息。
- 把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）。
- 把 Display Slots / 默认六币当成 batch universe。
- 默认六币固定推送。
- 非观察库资产进入机会推送候选。
- 在踩踏状态推送机会、反手或新开仓。
- 把短线插针当作趋势反转。
- 把强反转当作直接反手。

## 8. 合并后同步命令

每次 PR（合并请求）合并后，本地同步命令固定为：

```bash
cd /Users/xuchao/Documents/trade-model-v1 && git switch main && git pull origin main && git status && git log --oneline -5
```
