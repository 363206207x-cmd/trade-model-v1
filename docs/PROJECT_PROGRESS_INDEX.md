# PROJECT_PROGRESS_INDEX

## P255 Current Scope

- P254 已完成：ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。
- P255 当前推进：Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。
- P255 是 docs-only gate pack（只改文档授权门包），不是实现。
- Runtime Source Service skeleton 已存在。
- Watchlist Scan Result Assembly skeleton 已存在。
- Low-Frequency Watchlist Scan Orchestrator skeleton 已存在。
- BatchWatchlistScanOrchestrator 已存在。
- WatchlistMarketReadAdapter 已存在。
- DefaultWatchlistMarketReadAdapter 已存在。
- WatchlistScanScoreDTO 已存在。
- WatchlistScanScoreRule 已存在。
- WatchlistScanScoreCalculator 已存在。
- Candidate Attention Java 仍未实现。
- Promote To Home Java 仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- Readiness / point generation（可执行就绪 / 点位生成）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- real MarketQuoteClient integration（真实行情客户端接入）仍未实现。
- real ScanScore computation（真实扫描分数计算）仍未实现。
- scheduler-triggered batch（定时器触发批量扫描）仍未实现。
- real scan loop（真实扫描循环）仍未实现。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。
- 进度百分比可谨慎小幅上调，但不能因为 P255 docs gate（文档授权门）而把 Candidate / Push / Readiness 进度大幅上调。

P201 是 Project Progress Index Refresh After Watchlist Scan Promote Semantics（观察库扫描提升语义后项目总进度索引刷新）。P205 追加吸收 P202-P204 Low-Frequency Scan Scheduler（低频扫描定时器）范围审计、授权门和最小骨架结果。P206 已完成 Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包）。P207 已完成 Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包）。P208 已完成 Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）。P209 已完成 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包）。P210 已完成 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）。P211 已完成 Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门）。P212 已完成 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）。P213 已完成 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）。P214 已完成 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）。P215 已完成 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）。P216 已完成 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）。P217 已完成 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）。P218 已完成 Watchlist Runtime Source Guard Validator Skeleton（运行时数据源保护校验器骨架）。P219 已完成 Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门）。P220 已完成 Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架）。P221 已完成 Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门）。P222 已完成 Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案）。P223 已完成 Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案）。P224 已完成 Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架）。P225 已完成 Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门）。P226 已完成 Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案）。P227 已完成 Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门）。P228 已完成 Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现）。P229 已完成 Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门）。P230 已完成 DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计）。P231 已完成 DB Watchlist Pool Read Audit Closure and Java Authorization Gate（DB 观察库池读取审计收口与 Java 授权门）。P232 已完成 DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）。P233 已完成 DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）。P234 已完成 Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）。P235 已完成 Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。P236 已完成 Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。

本索引来自 P164 全局扫描后的持续维护。P201 刷新 `docs/PROJECT_PROGRESS_INDEX.md`，并吸收 P197-P200 Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）read-only audit / docs-only semantics（只读审计 / 只改文档语义）闭环结果。P205 补齐 P202-P204 后的状态和工作流文档，并把最大安全任务包规则写入工作流契约和 Codex 模板。P206-P236 已逐步完成 runtime contract audit（运行时契约审计）、authorization gate（授权门）、DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）、test-only wiring skeleton（仅测试接线骨架）、runtime source contract（运行时数据源契约）、adapter plan（适配器方案）、adapter interface skeleton（适配器接口骨架）、fail-closed no-op Java adapter（失败关闭 no-op Java 适配器）、DB Watchlist Pool read skeleton（DB 观察库池读取骨架）、Runtime Source Service skeleton（运行时数据源服务骨架）和 Watchlist Scan Result Assembly Gate（观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。P249 只是 docs-only DTO / Java skeleton authorization gate，不是实现；P248 只是 docs-only batch Java authorization gate / batch envelope plan，不是 batch implementation；P247 只是 docs-only closure / batch scan authorization gate，不是 batch implementation；P246 只是 disabled scheduler wiring skeleton，不是 scheduler activation；P245 只是 docs-only scheduler trigger authorization gate，不是 scheduler implementation；P244 只是 docs-only scope audit / layered gate plan，不是实现；P243 只是 docs-only closure / Scheduler-Batch-Market authorization gate，不是实现；P242 只是最小 disabled-by-default single-symbol orchestrator skeleton，不是真实扫描；Runtime Source Service skeleton 已存在，Watchlist Scan Result Assembly skeleton 已存在，WatchlistLowFrequencyScanScheduler disabled-by-default skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在，但 Low-Frequency Watchlist Scan Orchestrator production wiring（低频观察库扫描编排器生产接线）、scheduler-triggered orchestrator（定时器触发编排器）、scheduler-triggered batch（定时器触发批量扫描）、BatchWatchlistScanOrchestrator（批量观察库扫描编排器）已存在、BatchWatchlistScanResultEnvelopeDTO（批量观察库扫描结果信封 DTO）已存在、Batch scan Java（批量扫描 Java）进入最小 skeleton 阶段、Batch result envelope Java（批量结果信封 Java）进入最小 DTO 阶段，但 batch scan（批量扫描）作为真实扫描、real scan loop（真实扫描循环）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、real ScanScore computation（真实扫描分数计算）、Candidate Attention workflow（候选关注流程）、Promote To Home workflow（提升到首页观察流程）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）和 readiness（可执行就绪）仍未实现。P246 已新增独立 disabled scheduler wiring skeleton 和 targeted test；P249 只新增 docs-only batch envelope DTO authorization gate / batch Java skeleton authorization gate / batch fail-closed rules 文档；P248 只新增 docs-only batch Java authorization gate / batch envelope plan 文档；P247 只新增 docs-only closure / batch scan authorization gate 文档，但不修改 DTO / guard / validator / assembler，不改 dashboard.html，不改 schema / config，不接 API，不接 MarketQuoteClient，不接 BinanceMarketQuoteClient，不接 scheduler（定时器），不接 batch scan（批量扫描），不读取 runtime / live / external data（运行时 / 实时 / 外部数据），不创建真实 scan loop（扫描循环），不实现真实扫描、ScanScore computation（扫描分数计算）、Candidate Attention workflow（候选关注流程）或 Promote To Home workflow（提升到首页观察流程），不创建 Opportunity Push execution（机会推送执行），不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），不升级 Readiness（可执行就绪）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭；auto-trading（自动交易）只作为禁止越界安全边界。

## 一、当前总进度结论

P250 已完成：Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成：Market Score Candidate Push Readiness Scope Pack。P252 已完成：Market Read Adapter Skeleton。P253 已完成：ScanScore DTO and Rule Skeleton。P254 已完成：ScanScore Calculation Review-Only Skeleton。P255 当前推进：Candidate Attention and Promote To Home Gate Pack。P253 已完成 review-only ScanScore DTO / rule skeleton（只允许复核的扫描分数 DTO / 规则骨架）。P255 是 docs-only gate pack（只改文档授权门包），不是实现。Runtime Source Service skeleton 已存在，Watchlist Scan Result Assembly skeleton 已存在，Low-Frequency Watchlist Scan Orchestrator skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在，BatchWatchlistScanOrchestrator 已存在，BatchWatchlistScanResultEnvelopeDTO 已存在，WatchlistMarketReadAdapter 已存在，DefaultWatchlistMarketReadAdapter 已存在，WatchlistScanScoreDTO 已存在，WatchlistScanScoreRule 已存在，WatchlistScanScoreCalculator 已存在。real MarketQuoteClient integration（真实行情客户端接入）仍未实现，BinanceMarketQuoteClient integration（币安行情客户端接入）仍未实现，scheduler-triggered batch（定时器触发批量扫描）仍未实现，real scan loop（真实扫描循环）仍未实现，scheduler activation（定时器激活）仍未实现，production ScanScore computation（生产级扫描分数计算）仍未实现，Candidate Attention workflow（候选关注流程）仍未实现，Promote To Home workflow（提升到首页观察流程）仍未实现，Opportunity Push execution（机会推送执行）仍未实现，Readiness / point generation（可执行就绪 / 点位生成）仍未实现，真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。进度百分比可谨慎小幅上调，但不能因为 P255 docs gate（文档授权门）而把 Candidate / Push / Readiness 进度大幅上调。

P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。P250 只是 minimal batch skeleton（最小批量骨架），不是真实扫描。P251 是 docs-only accelerated scope pack（只改文档提速范围包），不是实现。P252 是 read-only fail-closed market-read adapter skeleton（只读、失败关闭的行情读取适配器骨架），不是真实行情读取。P253 已完成 review-only ScanScore DTO / rule skeleton（只允许复核的扫描分数 DTO / 规则骨架）。P255 是 docs-only gate pack（只改文档授权门包），不是实现。Runtime Source Service skeleton 已存在，Watchlist Scan Result Assembly skeleton 已存在，Low-Frequency Watchlist Scan Orchestrator skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在，BatchWatchlistScanOrchestrator 可存在，BatchWatchlistScanResultEnvelopeDTO 可存在，WatchlistMarketReadAdapter 已存在，DefaultWatchlistMarketReadAdapter 已存在，WatchlistScanScoreDTO 已存在，WatchlistScanScoreRule 已存在，WatchlistScanScoreCalculator 已存在。real MarketQuoteClient integration（真实行情客户端接入）仍未实现，BinanceMarketQuoteClient integration（币安行情客户端接入）仍未实现，scheduler-triggered batch（定时器触发批量扫描）仍未实现，real scan loop（真实扫描循环）仍未实现，scheduler activation（定时器激活）仍未实现，production ScanScore computation（生产级扫描分数计算）仍未实现，Candidate Attention workflow（候选关注流程）仍未实现，Promote To Home workflow（提升到首页观察流程）仍未实现，Opportunity Push execution（机会推送执行）仍未实现，真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现，readiness（可执行就绪）仍未升级。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。进度百分比只允许谨慎微调，不能因为 P255 docs gate（文档授权门）而把 Candidate / Push / Readiness 进度大幅上调。

P232 已完成 DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）。P233 已完成 DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）。P234 已完成 Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）。P235 已完成 Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。P236 已完成 Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。P249 只是 docs-only DTO / Java skeleton authorization gate，不是实现；P248 只是 docs-only batch Java authorization gate / batch envelope plan，不是 batch implementation；P247 只是 docs-only closure / batch scan authorization gate，不是 batch implementation；P246 只是 disabled scheduler wiring skeleton，不是 scheduler activation；P245 只是 docs-only scheduler trigger authorization gate，不是 scheduler implementation；P244 只是 docs-only scope audit / layered gate plan，不是实现；P243 只是 docs-only closure / Scheduler-Batch-Market authorization gate，不是实现；P242 只是最小 disabled-by-default single-symbol orchestrator skeleton，不是真实扫描；Runtime Source Service skeleton 已存在，Watchlist Scan Result Assembly skeleton 已存在，WatchlistLowFrequencyScanScheduler disabled-by-default skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在，但 Low-Frequency Watchlist Scan Orchestrator production wiring（低频观察库扫描编排器生产接线）、scheduler-triggered batch（定时器触发批量扫描）、BatchWatchlistScanOrchestrator（批量观察库扫描编排器）已存在、BatchWatchlistScanResultEnvelopeDTO（批量观察库扫描结果信封 DTO）已存在、Batch scan Java（批量扫描 Java）进入最小 skeleton 阶段、Batch result envelope Java（批量结果信封 Java）进入最小 DTO 阶段，但 batch orchestrator（批量扫描编排器）生产接线、real scan loop（真实扫描循环）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、real ScanScore computation（真实扫描分数计算）、Candidate Attention workflow（候选关注流程）、Promote To Home workflow（提升到首页观察流程）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）和 readiness（可执行就绪）仍未实现；order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

当前项目不是“快完成自动交易”的状态，而且 V1 明确不做 auto-trading（自动交易）。更准确的状态是：安全地基、只读复核、失败关闭、SourceTrace（证据来源追踪）只读展示、BoundaryCandidate（边界候选交易计划）只读候选展示、ExecutionPlan（执行计划）review-only plan display（只允许复核展示）、Risk Action Guard（风险动作保护器）read-only risk display（只读风险展示）、Position Monitor Strong Reversal / Moving Stop review-only display（持仓强反转 / 移动止损只允许复核展示）、Dashboard Risk Reminder read-only display（首页风险提醒只读展示）、Watchlist Low-Frequency Scan / Opportunity Promote read-only audit / docs-only semantics（观察库低频扫描 / 机会提升只读审计 / 只改文档语义）以及 Low-Frequency Scan Scheduler disabled-by-default skeleton（低频扫描定时器默认关闭骨架）已经更完整；P206 已把 Watchlist runtime data source contract（观察库运行时数据源契约）、Watchlist scan result contract（观察库扫描结果契约）和 ScanScore rule definition（扫描分数规则定义）放进文档审计，P207 已把后续 skeleton（骨架）的授权门边界收紧，P208 已完成 DTO / enum / tests（数据传输对象 / 枚举 / 测试）骨架，P209 已完成收口和未来 guard / no-score / no-push（保护器 / 无分数 / 无推送）边界文档，P210 已完成 guard / validator / tests（保护器 / 校验器 / 测试）骨架，P211 已完成 docs-only closure / wiring gate（只改文档收口 / 接线授权门），P212 已完成 non-runtime test-only wiring / assembler skeleton（非运行时、仅测试级接线 / 组装器骨架），P213 已完成 docs-only closure / runtime source gate（只改文档收口 / 运行时数据源授权门），P214 已完成 docs-only runtime source contract definition（只改文档运行时数据源契约定义），P215 已完成 docs-only runtime source authorization gate / DTO skeleton plan（只改文档运行时数据源授权门 / 数据对象骨架方案），P216 已完成 pure DTO / enum / tests runtime source DTO skeleton（纯数据对象 / 枚举 / 测试的运行时数据源 DTO 骨架），P217 已完成 docs-only closure / guard boundary（只改文档收口 / 保护边界），P218 已完成 pure runtime source guard / validator / tests skeleton（纯运行时数据源保护器 / 校验器 / 测试骨架），P219 已完成 docs-only closure / wiring gate（只改文档收口 / 接线授权门），P220 已完成 non-runtime test-only wiring / assembler / tests skeleton（非运行时、仅测试接线 / 组装器 / 测试骨架），P221 已完成 docs-only closure / production read gate（只改文档收口 / 生产读取授权门），P222 已完成 docs-only adapter plan（只改文档适配器方案），P223 已完成 docs-only interface skeleton plan（只改文档接口骨架方案），P224 已完成 interface / DTO / tests skeleton（接口 / 数据对象 / 测试骨架），P225 已完成 docs-only closure / implementation gate（只改文档收口 / 实现授权门），P226 已完成 docs-only fail-closed no-op implementation plan / risk audit（只改文档的失败关闭 no-op 实现方案 / 风险审计），P227 已完成 docs-only Java authorization gate（只改文档 Java 授权门），P228 已完成 fail-closed no-op Java implementation（失败关闭 no-op Java 实现），P229 已完成 docs-only no-op closure / DB Watchlist Pool read authorization gate（只改文档 no-op 收口 / DB 观察库池读取授权门），P230 已完成 docs-only DB read plan / mapper audit / schema audit（只改文档 DB 读取方案 / Mapper 审计 / Schema 审计），P231 已完成 docs-only audit closure / Java authorization gate（只改文档审计收口 / Java 授权门）。`WatchlistRuntimeSourceDTO` Java 已存在，runtime source guard / validator（运行时数据源保护器 / 校验器）已存在，runtime source test-only wiring（运行时数据源仅测试接线）已存在，production read adapter plan（生产读取适配器方案）已存在，production adapter interface skeleton（生产适配器接口骨架）已存在，production fail-closed no-op implementation plan（生产失败关闭 no-op 实现方案）已存在，production fail-closed no-op Java（生产失败关闭 no-op Java）已存在，但它只是永远失败关闭的 no-op adapter，不是 production runtime source read（生产运行时数据源读取）或 production read implementation（生产读取实现）。DB-backed watchlist read（数据库观察库读取）虽进入最小 skeleton 阶段但 production read implementation（生产读取实现）仍未完成，`WatchlistRuntimeSourceService` / `DefaultWatchlistRuntimeSourceService` 已存在，但 production Runtime Source Service wiring（生产运行时数据源服务接线）、production source assembler（生产数据源组装器）、real low-frequency scan（真实低频扫描）、runtime source implementation（运行时数据源实现）、runtime data reads（运行时数据读取）、Watchlist runtime data source（观察库运行时数据源）、MarketQuoteClient integration（行情客户端接入）、service wiring（服务接线）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、scan loop（扫描循环）、real ScanScore computation（真实扫描分数计算）、Candidate Attention workflow（候选关注流程）、Promote To Home workflow（提升到首页观察流程）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、trading buttons（交易按钮）、production candidate generation（生产候选交易计划生成）、trading actions（交易动作）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）和 ExecutionPlan Readiness（执行计划可执行就绪）仍然没有闭环；order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P165-P170 已完成 SourceTrace read-only display（证据来源追踪只读展示）闭环。P172-P175 已完成 BoundaryCandidate read-only candidate display（边界候选只读候选展示）闭环。P177-P180 已完成 ExecutionPlan review-only plan display（执行计划只允许复核展示）闭环。P182-P185 已完成 Risk Action Guard / Position Monitor read-only risk display（风险动作保护 / 持仓监控只读风险展示）闭环。P187-P190 已完成 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）闭环。P192-P195 已完成 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）闭环。P197-P200 已完成 Watchlist Low-Frequency Scan / Opportunity Promote Audit Pack（观察库低频扫描 / 机会提升审计包）闭环。P202-P204 已完成 Low-Frequency Scan Scheduler（低频扫描定时器）范围审计、授权门和默认关闭最小骨架。P206-P234 已完成运行时契约审计、授权门、DTO skeleton、guard skeleton、test-only wiring、adapter interface、fail-closed no-op adapter、DB Watchlist Pool read skeleton 和 Runtime Source Service plan。P235 已完成 Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。P236 已完成 Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包），是 docs-only gate pack，不是实现。所以项目总进度只能小幅、谨慎上调或保持。这个上调只代表“边界更清楚、只读复核语义更完整、低频扫描入口有默认关闭骨架、运行时数据源服务骨架更清楚、WatchlistScanResultDTO 最小 review-only skeleton 边界更清楚、Orchestrator 分层边界更清楚、Batch envelope contract 更清楚”，不代表真实扫描器、实时数据读取、scheduler-triggered batch（定时器触发批量扫描）、batch scan Java（批量扫描 Java）、batch result envelope Java（批量结果信封 Java）、production Runtime Source Service wiring（生产运行时数据源服务接线）、Production Source Assembler（生产数据源组装器）、推送执行、交易计划生成、生产 `VALID`（有效候选状态）、真实点位、Readiness（可执行就绪）或交易动作完成；auto-trading（自动交易）不在 V1 范围内，保持关闭，只作为禁止越界安全边界。

| 项目线 | 当前真实进度 |
|---|---:|
| 项目总进度 | 72%-77% |
| 安全地基进度 | 89%-94% |
| Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升）语义进度 | 65%-75% |
| Low-Frequency Scan Scheduler（低频扫描定时器）骨架进度 | 15%-25% |
| SourceTrace（证据来源追踪）进度 | 58%-66% |
| BoundaryCandidate（边界候选交易计划）进度 | 42%-52% |
| ExecutionPlan（执行计划）进度 | 45%-55% |
| Risk Action Guard（风险动作保护器）进度 | 47%-57% |
| Position Monitor（持仓监控）进度 | 52%-62% |
| dashboard（首页工作台）展示进度 | 76%-84% |
| 真实生产接线进度 | 26%-34% |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位进度 | 10%-18% |
| auto-trading（自动交易） | 不在 V1 范围内，保持关闭，只作为禁止越界安全边界 |

这些百分比按“能否安全进入真实生产链路”估算，不按文档数量估算。P197-P200 让 Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升）的语义边界明显更清楚，因此该模块保持 65%-75%。P204 只新增 disabled-by-default scheduler skeleton（默认关闭定时器骨架），所以 Low-Frequency Scan Scheduler（低频扫描定时器）只能记录为 15%-25%，不能上调到“真实扫描接近完成”。Dashboard（首页工作台）保持 76%-84%，不因 P205 文档补齐明显上调。SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、真实生产接线和真实点位保持谨慎，因为真实交易计划链路仍缺 source-owned runtime candidate generation（运行时证据来源候选生成）、runtime data source（运行时数据源）、numeric source ownership（数值来源归属）、production risk action（生产风控动作）、production VALID（生产环境有效候选状态）、Readiness（可执行就绪）和交易动作闭环。auto-trading（自动交易）只作为禁止越界安全边界，不作为进度目标。

## 二、已完成线路

- P140-P163 SourceTrace（证据来源追踪）/ Production Wiring Preparation（生产接线准备）已完成：已经完成范围门、缺口审计、输入契约、设计矩阵、测试计划、`INCOMPLETE`（证据不完整）guard（保护）、`BLOCKED`（禁止推进）guard（保护）、SourceTrace runtime population（运行时证据来源填充）helper（辅助类）、SourceTrace service wrapper（服务包装器）和收口文档。
- P160-P163 SourceTrace Service Wiring Pack（证据来源追踪服务层接线包）已完成：`SourceTraceRuntimePopulationService` 和 `SourceTraceRuntimePopulationServiceImpl` 已存在，service wrapper（服务包装器）只调用 `SourceTraceRuntimePopulationHelper.populate(...)`。
- P165-P170 SourceTrace read-model / dashboard read-only display pack（证据来源追踪只读输出 / 首页只读展示包）已完成。
- P165 完成 SourceTrace Read Model / Controller Scope Audit（证据来源追踪只读输出范围审计）。
- P166 完成 SourceTrace Read Model Authorization Gate（证据来源追踪只读输出授权门）。
- P167 完成 detail read model / adapter（详情只读模型 / 适配器）最小只读接线。
- P168 完成 SourceTrace Dashboard Display Scope Gate（首页证据来源追踪展示范围门）。
- P169 完成 `dashboard.html` 只读展示。
- P170 完成 SourceTrace Read-Only Display Closure（证据来源追踪只读展示收口）。
- P172-P175 BoundaryCandidate Read-Only Candidate Display Pack（边界候选只读候选展示包）已完成。
- P172 完成 BoundaryCandidate Read-Only Candidate Display Scope Audit（边界候选只读候选展示范围审计）。
- P173 完成 BoundaryCandidate Read-Only Candidate Display Authorization Gate（边界候选只读候选展示授权门）。
- P174 完成 PlanBoundaryDisplay（计划边界展示）/ `DefaultPlanBoundaryDisplayAdapter` 最小只读接线。
- P174 对 `VALID`（有效候选状态）做安全降级，不作为 production VALID（生产环境有效候选状态）输出。
- P175 完成 BoundaryCandidate Read-Only Candidate Display Closure（边界候选只读候选展示收口）。
- P177-P180 ExecutionPlan Review-Only Plan Display Pack（执行计划只允许复核展示包）已完成。
- P177 完成 ExecutionPlan Review-Only Plan Display Scope Audit（执行计划只允许复核展示范围审计）。
- P178 完成 ExecutionPlan Review-Only Plan Display Authorization Gate（执行计划只允许复核展示授权门）。
- P179 完成 `DefaultExecutionPlanDisplayAdapter` / `DefaultExecutionPlanDisplayAdapterTest` 最小只读接线。
- P179 没有升级 Readiness（可执行就绪），没有生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比），没有改 `dashboard.html`，没有接 API（接口），没有接自动交易。
- P180 完成 ExecutionPlan Review-Only Plan Display Closure（执行计划只允许复核展示收口）。
- P182-P185 Risk Action Guard / Position Monitor Read-Only Risk Display Pack（风险动作保护 / 持仓监控只读风险展示包）已完成。
- P182 完成 Risk Action Guard / Position Monitor Scope Audit（风险动作保护和持仓监控范围审计）。
- P183 完成 Risk Action Guard / Position Monitor Authorization Gate（风险动作保护和持仓监控授权门）。
- P184 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P185 完成 Risk Action Guard / Position Monitor Closure（风险动作保护和持仓监控收口）。
- RiskActionGuardDisplay（风险动作保护展示）继续保持 `opportunityPushAllowed=false`、`reverseTradeAllowed=false`、`newPositionAllowed=false`、`marketOrderExitAllowed=false`、`manualRiskReviewRequired=true`、`notTradeInstruction=true`。
- P187-P190 Position Monitor Strong Reversal / Moving Stop Review-Only Pack（持仓强反转 / 移动止损只读复核包）已完成。
- P187 完成 Position Monitor Strong Reversal / Moving Stop Review-Only Scope Audit（持仓强反转 / 移动止损只读复核范围审计）。
- P188 完成 Position Monitor Strong Reversal / Moving Stop Authorization Gate（持仓强反转 / 移动止损授权门）。
- P189 完成 `DefaultRiskActionGuardDisplayAdapter` / `DefaultRiskActionGuardDisplayAdapterTest` 最小只读接线。
- P190 完成 Position Monitor Strong Reversal / Moving Stop Closure（持仓强反转 / 移动止损收口）。
- RiskActionGuardDisplay（风险动作保护展示）现在能更清楚展示 Strong Reversal（强反转）待确认、原入场逻辑疑似失效、Moving Stop（移动止损）需要人工复核、Strong Reversal（强反转）不等于反手或自动平仓、Moving Stop（移动止损）不等于自动改 Stop Loss（止损）、auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭、不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、不升级 Readiness（可执行就绪）。
- P189 没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损），没有生成真实 entry / stop / TP / RR，没有改 `dashboard.html`，没有接 API，没有升级 Readiness，没有接 auto-trading（自动交易）。
- P192-P195 Dashboard Risk Reminder Read-Only Display Pack（首页风险提醒只读展示包）已完成。
- P192 完成 Dashboard Risk Reminder Read-Only Display Scope Audit（首页风险提醒只读展示范围审计）。
- P193 完成 Dashboard Risk Reminder Read-Only Display Authorization Gate（首页风险提醒只读展示授权门）。
- P194 完成 `dashboard.html` 最小 Read-Only Display（只读展示）接线。
- P195 完成 Dashboard Risk Reminder Read-Only Display Closure（首页风险提醒只读展示收口）。
- Dashboard（首页工作台）现在能更集中展示 `riskActionAdvice`（风险动作建议）、`riskActionBlockingReason`（风险动作阻断原因）、`liquidityState`（流动性状态）、`stampedeDetected`（是否检测到踩踏）、`wickOnlyRisk`（是否仅插针风险）、`marketOrderExitAllowed`（是否允许市价退出）、`opportunityPushAllowed`（是否允许机会推送）、`reverseTradeAllowed`（是否允许反手）、`newPositionAllowed`（是否允许新开仓）、`manualRiskReviewRequired`（是否必须人工复核）和 `notTradeInstruction`（是否不是交易指令）。
- Dashboard（首页工作台）现在能集中展示“不是交易指令”“必须人工复核”“不连接 order API（下单接口）”“不触发自动交易动作”“不生成真实点位”“自动平仓 / 自动反手 / 自动改止损关闭”。
- P194 只修改 `src/main/resources/templates/dashboard.html`。P194 没有改 Java，没有改 `DashboardController.java` / `DashboardDetailResponseVO.java`，没有接 API（接口），没有新增按钮，没有创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作，没有自动修改 Stop Loss（止损）/ Moving Stop（移动止损），没有生成真实 entry / stop / TP / RR，没有升级 Readiness（可执行就绪），没有接 auto-trading（自动交易）。
- P197-P200 Watchlist Low-Frequency Scan / Opportunity Promote Audit Pack（观察库低频扫描 / 机会提升审计包）已完成。
- P197 完成 Watchlist Low-Frequency Scan / Opportunity Promote Scope Audit（观察库低频扫描 / 机会提升范围审计）。
- P198 完成 Watchlist Low-Frequency Scan / Opportunity Promote Authorization Gate（观察库低频扫描 / 机会提升授权门）。
- P199 完成 docs-only minimal wiring（只改文档的最小接线）。
- P200 完成 Watchlist Low-Frequency Scan / Opportunity Promote Closure（观察库低频扫描 / 机会提升收口）。
- 已明确语义：Display Slots（首页展示位）是首页展示优先级，不是推送全集。
- 已明确语义：Watchlist Pool（观察库池）才是推送候选最大边界。
- 已明确语义：首页默认 6 个资产不是后端推送全集，也不是唯一观察库。
- 已明确语义：观察库可以多于 6 个。
- 已明确语义：Low-Frequency Scan（低频扫描）未来只能从 Watchlist Pool（观察库池）开始。
- 已明确语义：Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核。
- 已明确语义：Opportunity Promote（机会提升）不是 Opportunity Push execution（机会推送执行）。
- 已明确语义：Opportunity Promote（机会提升）不是订单。
- 已明确语义：Opportunity Promote（机会提升）不是交易信号。
- 已明确语义：Opportunity Promote（机会提升）不是 Readiness（可执行就绪）。
- 已明确语义：Opportunity Promote（机会提升）不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 已明确语义：默认六币不能作为默认推送全集。
- 已明确语义：非观察库资产不能进入候选。
- P199 没有改 `dashboard.html`。
- P199 没有改 Java。
- P199 没有新增测试。
- P199 没有新增 API。
- P199 没有新增 schema / config / mapper / service（数据库结构 / 配置 / 映射 / 服务）。
- P199 没有接 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。
- P199 没有接 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。
- P199 没有接 `MarketQuoteClient`。
- P199 没有创建 Low-Frequency Scan scheduler（低频扫描定时器）。
- P199 没有创建 Opportunity Push execution（机会推送执行）。
- P199 没有生成真实 entry / stop / TP / RR。
- P199 没有升级 Readiness（可执行就绪）。
- P199 没有接自动交易。
- P202 完成 Low-Frequency Scan Scheduler Scope Audit（低频扫描定时器范围审计）。
- P203 完成 Low-Frequency Scan Scheduler Authorization Gate（低频扫描定时器授权门）。
- P204 完成 Low-Frequency Scan Scheduler Minimal Skeleton（低频扫描定时器最小骨架）。
- P204 只是 disabled-by-default scheduler skeleton（默认关闭定时器骨架）。
- P204 默认 `enabled=false`，`runScheduledScan()` 默认返回 `DISABLED`。
- P204 在 `enabled=true` 时仍返回 `NOT_IMPLEMENTED`，不进入真实扫描。
- P204 保留 `notTradeInstruction=true`（不是交易指令）和 `manualReviewRequired=true`（必须人工复核）。
- P204 不等于真实 Low-Frequency Scan（低频扫描）完成。
- P204 不等于 Watchlist runtime data source（观察库运行时数据源）完成。
- P204 不等于 MarketQuoteClient scan integration（行情客户端扫描接入）完成。
- P204 不等于 ScanScore（扫描分数）/ Candidate Attention（候选关注）/ Promote To Home（提升到首页观察）完成。
- P204 不等于 Opportunity Push execution（机会推送执行）完成。
- P204 不等于真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）完成。
- P204 不等于 Readiness（可执行就绪）升级。
- P204 不改变 auto-trading（自动交易）边界；auto-trading 不在 V1 范围内，保持关闭。
- P205 完成 Max Safe Docs Pack After P204 Scheduler Skeleton（P204 定时器骨架后的最大安全文档包）。
- P205 已把最大安全任务包规则写入 `docs/V1_OPERATOR_WORKFLOW_CONTRACT.md` 和 `docs/V1_CODEX_TASK_TEMPLATE.md`。
- P205 明确后续默认按同一风险档位、同一模块 / 业务轨道、同一验证方式且不跨授权门的最大安全任务包组织。
- P206 已完成 Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包）。
- P206 已完成：运行时契约 / 观察库运行时数据源 / 扫描结果契约 / ScanScore 规则定义审计已完成。
- P206 是 docs-only audit（只改文档审计），不是实现。
- P206 只审计 Watchlist runtime data source contract（观察库运行时数据源契约）、Watchlist scan result contract（观察库扫描结果契约）和 ScanScore rule definition（扫描分数规则定义）。
- P206 不实现真实 Low-Frequency Scan（低频扫描）。
- P206 不实现 Watchlist runtime data source（观察库运行时数据源）。
- P206 不接 MarketQuoteClient scan integration（行情客户端扫描接入）。
- P206 不实现 ScanScore（扫描分数）。
- P206 不实现 Candidate Attention（候选关注）。
- P206 不实现 Promote To Home execution（提升到首页观察执行）。
- P206 不实现 Opportunity Push execution（机会推送执行）。
- P206 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P206 不升级 Readiness（可执行就绪）。
- P206 不接 auto-trading（自动交易）；auto-trading 不在 V1 范围内，保持关闭，不作为进度目标。
- P207 已完成 Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包）。
- P207 是 docs-only authorization gate（只改文档授权门），不是实现。
- P207 不实现真实低频扫描。
- P207 不实现 Watchlist runtime data source（观察库运行时数据源）。
- P207 不接 MarketQuoteClient scan integration（行情客户端扫描接入）。
- P207 不创建 ScanResult DTO Java（扫描结果数据对象 Java 类）。
- P207 不实现 ScanScore implementation（扫描分数实现）。
- P207 不创建 Candidate Attention implementation（候选关注实现）。
- P207 不创建 Promote To Home execution（提升到首页观察执行）。
- P207 不创建 Opportunity Push execution（机会推送执行）。
- P207 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P207 不升级 readiness（可执行就绪）。
- P207 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P208 已完成 Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）。
- P208 是 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试），不是实现。
- P208 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P208 不接 MarketQuoteClient integration（行情客户端接入）。
- P208 不接 service wiring（服务接线）。
- P208 不改 scheduler behavior（定时器行为）。
- P208 不实现 real ScanScore implementation（真实扫描分数实现）。
- P208 不实现 Candidate Attention implementation（候选关注实现）。
- P208 不实现 Promote To Home execution（提升到首页观察执行）。
- P208 不创建 Opportunity Push execution（机会推送执行）。
- P208 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P208 不升级 readiness（可执行就绪）。
- P208 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P209 已完成 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包）。
- P209 是 docs-only closure / boundary（只改文档收口 / 边界），不是实现。
- P209 不写 Java。
- P209 不新增测试。
- P209 不修改 P208 DTO Java。
- P209 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P209 不接 MarketQuoteClient integration（行情客户端接入）。
- P209 不接 service wiring（服务接线）。
- P209 不改 scheduler behavior（定时器行为）。
- P209 不实现 real ScanScore computation（真实扫描分数计算）。
- P209 不实现 Candidate Attention workflow（候选关注流程）。
- P209 不实现 Promote To Home workflow（提升到首页观察流程）。
- P209 不创建 Opportunity Push execution（机会推送执行）。
- P209 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P209 不升级 readiness（可执行就绪）。
- P209 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P210 已完成 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）。
- P210 是 pure guard / validator / tests（纯保护器 / 校验器 / 测试），不是 runtime scan（运行时扫描）。
- P210 可以存在 guard skeleton（保护器骨架），但真实低频扫描仍未完成。
- P210 不修改 P208 DTO Java。
- P210 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P210 不接 MarketQuoteClient integration（行情客户端接入）。
- P210 不接 service wiring（服务接线）。
- P210 不改 scheduler behavior（定时器行为）。
- P210 不实现 real ScanScore computation（真实扫描分数计算）。
- P210 不实现 Candidate Attention workflow（候选关注流程）。
- P210 不实现 Promote To Home workflow（提升到首页观察流程）。
- P210 不创建 Opportunity Push execution（机会推送执行）。
- P210 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P210 不升级 readiness（可执行就绪）。
- P210 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P211 已完成 Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门）。
- P211 是 docs-only closure / wiring gate（只改文档收口 / 接线授权门），不是实现。
- P211 不写 Java。
- P211 不新增测试。
- P211 不修改 DTO / guard / validator（数据对象 / 保护器 / 校验器）。
- P211 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P211 不接 MarketQuoteClient integration（行情客户端接入）。
- P211 不接 service wiring（服务接线）。
- P211 不改 scheduler behavior（定时器行为）。
- P211 不实现 real ScanScore computation（真实扫描分数计算）。
- P211 不实现 Candidate Attention workflow（候选关注流程）。
- P211 不实现 Promote To Home workflow（提升到首页观察流程）。
- P211 不创建 Opportunity Push execution（机会推送执行）。
- P211 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P211 不升级 readiness（可执行就绪）。
- P211 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P212 已完成 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）。
- P212 是 non-runtime test-only wiring / assembler skeleton（非运行时、仅测试级接线 / 组装器骨架），不是 runtime scan（运行时扫描）。
- P212 不修改 DTO / guard / validator（数据对象 / 保护器 / 校验器）。
- P212 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P212 不接 MarketQuoteClient integration（行情客户端接入）。
- P212 不接 scheduler（定时器）。
- P212 不接 mapper / controller / dashboard（映射器 / 控制器 / 首页）。
- P212 不创建 scan loop（扫描循环）。
- P212 不实现 real ScanScore computation（真实扫描分数计算）。
- P212 不实现 Candidate Attention workflow（候选关注流程）。
- P212 不实现 Promote To Home workflow（提升到首页观察流程）。
- P212 不创建 Opportunity Push execution（机会推送执行）。
- P212 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P212 不升级 readiness（可执行就绪）。
- P212 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P213 已完成 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）。
- P213 是 docs-only closure / runtime source gate（只改文档收口 / 运行时数据源授权门），不是实现。
- P213 不写 Java。
- P213 不新增测试。
- P213 不修改 DTO / guard / assembler（数据对象 / 保护器 / 组装器）。
- P213 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P213 不接 MarketQuoteClient integration（行情客户端接入）。
- P213 不接 scheduler（定时器）。
- P213 不接 mapper / controller / dashboard（映射器 / 控制器 / 首页）。
- P213 不创建 scan loop（扫描循环）。
- P213 不实现 real ScanScore computation（真实扫描分数计算）。
- P213 不实现 Candidate Attention workflow（候选关注流程）。
- P213 不实现 Promote To Home workflow（提升到首页观察流程）。
- P213 不创建 Opportunity Push execution（机会推送执行）。
- P213 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P213 不升级 readiness（可执行就绪）。
- P213 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P214 已完成 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）。
- P214 只是 docs-only contract definition（只改文档契约定义），不是实现。
- P214 可文档化 runtime source contract（运行时数据源契约），但 runtime source（运行时数据源）仍未实现。
- P214 不写 Java。
- P214 不新增测试。
- P214 不修改 DTO / guard / assembler（数据对象 / 保护器 / 组装器）。
- P214 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P214 不读 DB-backed watchlist（数据库观察库读取）。
- P214 不接 MarketQuoteClient integration（行情客户端接入）。
- P214 不接 scheduler（定时器）。
- P214 不接 mapper / controller / dashboard（映射器 / 控制器 / 首页）。
- P214 不创建 scan loop（扫描循环）。
- P214 不实现 real ScanScore computation（真实扫描分数计算）。
- P214 不实现 Candidate Attention workflow（候选关注流程）。
- P214 不实现 Promote To Home workflow（提升到首页观察流程）。
- P214 不创建 Opportunity Push execution（机会推送执行）。
- P214 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P214 不升级 readiness（可执行就绪）。
- P214 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P215 已完成 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）。
- P215 只是 docs-only authorization gate / DTO skeleton plan（只改文档授权门 / 数据对象骨架方案），不是实现。
- P215 不写 Java。
- P215 不新增测试。
- P215 不修改 DTO / guard / assembler（数据对象 / 保护器 / 组装器）。
- P215 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P215 不接 MarketQuoteClient integration（行情客户端接入）。
- P215 不接 scheduler（定时器）。
- P215 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P215 不创建 scan loop（扫描循环）。
- P215 不实现 real ScanScore computation（真实扫描分数计算）。
- P215 不实现 Candidate Attention workflow（候选关注流程）。
- P215 不实现 Promote To Home workflow（提升到首页观察流程）。
- P215 不创建 Opportunity Push execution（机会推送执行）。
- P215 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P215 不升级 readiness（可执行就绪）。
- P215 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P216 已完成 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）。
- P216 只是 pure DTO / enum / tests（纯数据对象 / 枚举 / 测试），不是 runtime source implementation（运行时数据源实现）。
- P216 已新增 `WatchlistRuntimeSourceDTO` Java，但不等于 DB-backed watchlist read（数据库观察库读取）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、scan loop（扫描循环）或 runtime source service（运行时数据源服务）完成。
- P216 不修改既有 Java / test / DTO / guard / assembler（既有 Java / 测试 / 数据对象 / 保护器 / 组装器）。
- P216 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P216 不接 MarketQuoteClient integration（行情客户端接入）。
- P216 不接 scheduler（定时器）。
- P216 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P216 不创建 scan loop（扫描循环）。
- P216 不实现 real ScanScore computation（真实扫描分数计算）。
- P216 不实现 Candidate Attention workflow（候选关注流程）。
- P216 不实现 Promote To Home workflow（提升到首页观察流程）。
- P216 不创建 Opportunity Push execution（机会推送执行）。
- P216 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P216 不升级 readiness（可执行就绪）。
- P216 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P217 已完成 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）。
- P217 只是 docs-only closure / guard boundary（只改文档收口 / 保护边界），不是实现。
- P217 不写 Java。
- P217 不新增测试。
- P217 不修改 DTO / guard / validator / assembler（数据对象 / 保护器 / 校验器 / 组装器）。
- P217 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P217 不接 MarketQuoteClient integration（行情客户端接入）。
- P217 不接 scheduler（定时器）。
- P217 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P217 不创建 scan loop（扫描循环）。
- P217 不实现 real ScanScore computation（真实扫描分数计算）。
- P217 不实现 Candidate Attention workflow（候选关注流程）。
- P217 不实现 Promote To Home workflow（提升到首页观察流程）。
- P217 不创建 Opportunity Push execution（机会推送执行）。
- P217 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P217 不升级 readiness（可执行就绪）。
- P217 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P218 已完成 Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）。
- P218 只是 pure guard / validator / tests（纯保护器 / 校验器 / 测试），不是 runtime source implementation（运行时数据源实现）。
- P218 可新增 runtime source guard / validator（运行时数据源保护器 / 校验器）骨架，但不等于 DB-backed watchlist read（数据库观察库读取）虽进入最小 skeleton 阶段但 production read implementation（生产读取实现）仍未完成，runtime source service（运行时数据源服务）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、scan loop（扫描循环）或真实低频扫描完成。
- P218 不修改 P216 DTO Java，不修改既有 Java / test / DTO / guard / assembler（既有 Java / 测试 / 数据对象 / 保护器 / 组装器）。
- P218 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P218 不接 MarketQuoteClient integration（行情客户端接入）。
- P218 不接 scheduler（定时器）。
- P218 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P218 不创建 scan loop（扫描循环）。
- P218 不实现 real ScanScore computation（真实扫描分数计算）。
- P218 不实现 Candidate Attention workflow（候选关注流程）。
- P218 不实现 Promote To Home workflow（提升到首页观察流程）。
- P218 不创建 Opportunity Push execution（机会推送执行）。
- P218 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P218 不升级 readiness（可执行就绪）。
- P218 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P219 已完成 Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门）。
- P219 只是 docs-only closure / wiring gate（只改文档收口 / 接线授权门），不是实现。
- P219 不写 Java。
- P219 不新增测试。
- P219 不修改 DTO / guard / validator / assembler（数据对象 / 保护器 / 校验器 / 组装器）。
- P219 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P219 不接 MarketQuoteClient integration（行情客户端接入）。
- P219 不接 scheduler（定时器）。
- P219 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P219 不创建 scan loop（扫描循环）。
- P219 不实现 real ScanScore computation（真实扫描分数计算）。
- P219 不实现 Candidate Attention workflow（候选关注流程）。
- P219 不实现 Promote To Home workflow（提升到首页观察流程）。
- P219 不创建 Opportunity Push execution（机会推送执行）。
- P219 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P219 不升级 readiness（可执行就绪）。
- P219 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P220 已完成 Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架）。
- P220 只是 non-runtime test-only wiring / assembler / tests（非运行时、仅测试接线 / 组装器 / 测试），不是 runtime source implementation（运行时数据源实现）。
- P220 可新增 runtime source test-only wiring（运行时数据源仅测试接线）骨架，但不等于 DB-backed watchlist read（数据库观察库读取）虽进入最小 skeleton 阶段但 production read implementation（生产读取实现）仍未完成，runtime source service（运行时数据源服务）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、scan loop（扫描循环）或真实低频扫描完成。
- P220 不修改 P216 DTO Java，不修改 P218 guard / validator Java，不修改既有 Java / test / DTO / guard / assembler（既有 Java / 测试 / 数据对象 / 保护器 / 组装器）。
- P220 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。
- P220 不接 MarketQuoteClient integration（行情客户端接入）。
- P220 不接 scheduler（定时器）。
- P220 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P220 不创建 scan loop（扫描循环）。
- P220 不实现 real ScanScore computation（真实扫描分数计算）。
- P220 不实现 Candidate Attention workflow（候选关注流程）。
- P220 不实现 Promote To Home workflow（提升到首页观察流程）。
- P220 不创建 Opportunity Push execution（机会推送执行）。
- P220 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P220 不升级 readiness（可执行就绪）。
- P220 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P221 已完成 Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门）。
- P222 已完成 Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案）。
- P223 已完成 Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案）。
- P224 已完成 Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架）。
- P225 已完成 Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门）。P226 已完成 Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案）。P227 已完成 Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门）。P228 已完成 Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现）。P229 已完成 Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门）。P230 已完成 DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计）。P231 已完成 DB Watchlist Pool Read Audit Closure and Java Authorization Gate（DB 观察库池读取审计收口与 Java 授权门）。
- P231 只是 docs-only audit closure / Java authorization gate（只改文档审计收口 / Java 授权门），不是 DB read implementation。
- P231 记录 P230 审计结论：`DefaultWatchlistPoolRuntimeSourceReadAdapter` no-op 已存在，`RuleConfigService` / `RuleConfigMapper` / `tm_rule_config` 已存在，`push.watchlist.symbols` runtime config（运行时配置）未确认；本轮不修改 Java / test / DTO / guard / validator / assembler（Java / 测试 / 数据对象 / 保护器 / 校验器 / 组装器）。
- P231 不修改 dashboard / schema / config（首页 / 数据库结构 / 配置）。
- P231 不读取 DB runtime data（数据库运行时数据），不执行数据库查询，不运行服务，不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P231 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。
- P231 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。
- P231 不改 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。
- P231 不创建 scan loop（扫描循环）。
- P231 不实现 DB-backed watchlist read（数据库观察库读取）。
- P231 不实现真实 production adapter implementation（生产适配器实现）或 production read implementation（生产读取实现）。
- P231 不实现 real ScanScore computation（真实扫描分数计算）。
- P231 不实现 Candidate Attention workflow（候选关注流程）。
- P231 不实现 Promote To Home workflow（提升到首页观察流程）。
- P231 不创建 Opportunity Push execution（机会推送执行）。
- P231 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- P231 不升级 readiness（可执行就绪）。
- P231 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P232 已完成 DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）。
- P232 只是最小 DB Watchlist Pool config read skeleton（DB 观察库池配置读取骨架），不是真实扫描。
- P232 可新增 `RuleConfigWatchlistPoolReadAdapter` 和 `RuleConfigWatchlistPoolReadAdapterTest`。
- P232 只允许复用 `RuleConfigService` / `tm_rule_config` 语义读取 `push.watchlist.symbols`。
- P232 不修改既有 Java / test / DTO / guard / validator / assembler。
- P232 不改 dashboard / schema / config / mapper / API。
- P232 不接 MarketQuoteClient / BinanceMarketQuoteClient / scheduler。
- P232 不创建 scan loop、real scan、ScanScore、Candidate Attention、Promote To Home、Opportunity Push、entry / stop / TP / RR、readiness 或 trading action。
- P233 已完成 DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）。P234 已完成 Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）。P235 已完成 Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。P236 已完成 Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。
- P233 只是 docs-only closure / Runtime Source Service authorization gate（只改文档收口 / 运行时数据源服务授权门），不是实现。
- `RuleConfigWatchlistPoolReadAdapter` Java 已存在。
- DB-backed Watchlist Pool read skeleton（数据库观察库池读取骨架）已存在。
- `WatchlistRuntimeSourceService` / `DefaultWatchlistRuntimeSourceService` 已存在，但 production Runtime Source Service wiring（生产运行时数据源服务接线）仍未实现。
- Production Source Assembler（生产数据源组装器）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- scan loop（扫描循环）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P233 不因为 docs-only 收口而大幅上调真实生产接线进度。
- BoundaryCandidate（边界候选交易计划）DTO / valid factory（有效候选工厂）/ service skeleton（服务骨架）已完成，但这不等于 production VALID（生产环境有效候选状态）已经可生成。
- RuntimeKlineContext（运行时 K 线上下文）/ BoundaryCandidateService（边界候选服务）相关已完成，但这不等于真实交易点位完成。
- Dashboard（首页工作台）已完成 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、RiskActionGuard（风险动作保护展示）、PaperObservationDisplay（纸面观察展示）、Position Monitor（持仓监控）和 Dashboard Risk Reminder（首页风险提醒）只读展示。
- position sync（持仓同步）/ manual position（手动或模拟持仓）/ monitoring（监控）已完成基础能力，但当前更像同步、告警、记录和只读风险展示基础，不是自动平仓或自动反手。

## 三、正在推进线路

Watchlist Low-Frequency Scan / Opportunity Promote（观察库低频扫描 / 机会提升）语义边界已完成，Low-Frequency Scan Scheduler（低频扫描定时器）已有默认关闭最小骨架。P206-P240 已逐步完成 runtime contract audit（运行时契约审计）、authorization gate（授权门）、DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）、test-only wiring skeleton（仅测试接线骨架）、runtime source contract（运行时数据源契约）、adapter plan（适配器方案）、adapter interface skeleton（适配器接口骨架）、fail-closed no-op Java adapter（失败关闭 no-op Java 适配器）、DB Watchlist Pool read skeleton（DB 观察库池读取骨架）、Runtime Source Service skeleton（运行时数据源服务骨架）、Watchlist Scan Result Assembly skeleton（观察库扫描结果组装骨架）和 Low-Frequency Scan Orchestrator Gate（低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包），是 docs-only gate pack，不是实现。当前不应该把 no-op adapter（no-op 适配器）、P232 config read skeleton（配置读取骨架）、P235 service skeleton（服务骨架）、P239 assembly skeleton（组装骨架）或 P242 orchestrator skeleton（编排器骨架）误判为 production read implementation（生产读取实现）或真实扫描器，也不能把 P252 market-read adapter skeleton（行情读取适配器骨架）误推进成 MarketQuoteClient integration（行情客户端接入）、ScanScore implementation（扫描分数实现）、Candidate Attention workflow（候选关注流程）、Promote To Home workflow（提升到首页观察流程）、Opportunity Push execution（机会推送执行）、Readiness（可执行就绪）、scan loop（扫描循环）、真实点位或交易动作；auto-trading（自动交易）不在 V1 范围内，保持关闭。

如果目标是个人可用最快路径，建议进入：

1. Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）。
2. Manual Position Review Workflow Audit（手动持仓复核流程审计）。
3. Dashboard Personal Use Smoke Checklist（个人可用冒烟清单）。

如果目标是开始真正实现 Low-Frequency Scan（低频扫描），必须先进入：

1. P206 Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包，已完成）。
2. P207 Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包，已完成）。
3. P208 Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架，已完成）。
4. P209 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包，已完成）。
5. P210 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架，已完成）。
6. P211 Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门，已完成）。
7. P212 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架，已完成）。
8. P213 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门，已完成）。
9. P214 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包，已完成）。
10. P215 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案，已完成）。
11. P216 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架，已完成）。
12. P217 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界，已完成）。
13. P218 Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架，已完成）。
14. P219 Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门，已完成）。
15. P220 Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架，已完成）。
16. P221 Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门，已完成）。
17. P222 Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案，已完成）。
18. P223 Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案，已完成）。
19. P224 Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架，已完成）。
20. P225 Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门，已完成）。
21. P226 Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案，已完成）。
22. P227 Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门，已完成）。
23. P228 Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现，已完成）。
24. P229 Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门，已完成）。
25. P230 DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计，当前推进）。
26. 后续 production adapter implementation / runtime source implementation（生产适配器实现 / 运行时数据源实现）必须另开授权门，不能直接接真实扫描、DB、行情或 scheduler（定时器）。

如果目标是严谨后端交易候选，仍建议进入：

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。

无论走哪条路线，都不能直接进入扫描器、实时数据读取、机会推送执行、自动平仓、自动反手、自动修改止损或真实点位。

## 四、暂停线路

- real low-frequency scan（真实低频扫描）：仍未完成，仍暂停，原因是 P204 只完成默认关闭骨架，P208-P228 只推进 DTO / guard / wiring / adapter interface / fail-closed no-op 等安全骨架，P231 只是 docs-only audit closure / Java authorization gate（只改文档审计收口 / Java 授权门），不创建真实扫描器。
- Runtime data reads（运行时数据读取）：仍未实现，仍暂停，原因是 P231 不读取 DB runtime data（数据库运行时数据），不执行数据库查询，不运行服务，不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- Watchlist runtime data source（观察库运行时数据源）：仍未实现，仍暂停，原因是 P206 只审计数据源契约，P207 只做 authorization gate（授权门），P208 只做 DTO skeleton（数据对象骨架），P209 只做收口 / 边界，P210 只做 guard skeleton（保护器骨架），P211 只做 wiring gate（接线授权门），P212 只做 test-only wiring skeleton（仅测试接线骨架），P213 只做 runtime source gate（运行时数据源授权门），P214 只做 contract definition（契约定义），P215 只做 authorization gate / DTO skeleton plan（授权门 / 数据对象骨架方案），P216 只做 DTO skeleton（数据对象骨架），P217 只做 closure / guard boundary（收口 / 保护边界），P218 只做 guard skeleton（保护器骨架），P219 只做 closure / wiring gate（收口 / 接线授权门），P220 只做 test-only wiring skeleton（仅测试接线骨架），P221 只做 closure / production read gate（收口 / 生产读取授权门），P222 只做 adapter plan（适配器方案），P223 只做 interface skeleton plan（接口骨架方案），不实现运行时数据源。
- WatchlistRuntimeSourceDTO Java：P216 已完成 pure DTO / enum / tests skeleton（纯数据对象 / 枚举 / 测试骨架），但这不是 runtime source implementation（运行时数据源实现），不读取 DB、行情、runtime、live 或 external data（数据库 / 行情 / 运行时 / 实时 / 外部数据）。
- runtime source guard / validator（运行时数据源保护器 / 校验器）：P218 已完成 skeleton（骨架），但这不是 runtime source implementation（运行时数据源实现），也不是 production wiring（生产接线）。
- runtime source test-only wiring（运行时数据源仅测试接线）：P220 已完成 skeleton（骨架），但这不是 production runtime source wiring（生产运行时数据源接线），也不是 runtime source implementation（运行时数据源实现）。
- production runtime source read（生产运行时数据源读取）：仍未完成，仍暂停，原因是 P232 只允许最小 DB Watchlist Pool config read skeleton（DB 观察库池配置读取骨架），不接行情 / scheduler / scan loop（定时器 / 扫描循环）。
- production read adapter（生产读取适配器）：仍未实现，仍暂停，原因是 P223 只定义 interface skeleton plan（接口骨架方案），不写 Java。
- production adapter interface skeleton（生产适配器接口骨架）：已存在，但只是接口 / DTO / 测试骨架，不是 production read implementation（生产读取实现）。
- production fail-closed no-op implementation plan（生产失败关闭 no-op 实现方案）：P226 可文档化，但不是 Java implementation（Java 实现）。
- production fail-closed no-op Java（生产失败关闭 no-op Java）：P228 已新增 `DefaultWatchlistPoolRuntimeSourceReadAdapter`，但它只会返回 `INCOMPLETE` 或 `SOURCE_UNAVAILABLE`，不是 production read implementation（生产读取实现）。
- `DefaultWatchlistPoolRuntimeSourceReadAdapter` Java：P228 已存在为 fail-closed no-op adapter（失败关闭 no-op 适配器），不读取 DB、行情、runtime、live 或 external data（数据库 / 行情 / 运行时 / 实时 / 外部数据）。
- `NoOpWatchlistPoolRuntimeSourceReadAdapter` Java：仍未实现，原因是 P228 选择 `DefaultWatchlistPoolRuntimeSourceReadAdapter`，不同时实现两个 no-op adapter。
- production adapter implementation（生产适配器实现）：仍未实现，仍暂停，原因是 P228 只是 fail-closed no-op adapter（失败关闭 no-op 适配器），P232 只是 DB Watchlist Pool config read skeleton（DB 观察库池配置读取骨架），不是 Market / Scheduler production adapter implementation（行情 / 定时器生产适配器实现）。
- production runtime source wiring（生产运行时数据源接线）：仍未实现，仍暂停，原因是 P223 只允许 docs-only interface skeleton plan（只改文档接口骨架方案），不接生产服务链路。
- DB-backed watchlist read（数据库观察库读取）：进入最小 skeleton 阶段，但不是 production read implementation（生产读取实现），原因是 P232 只读取 `push.watchlist.symbols` 配置并保持 no-push / no-readiness / no-trading（无推送 / 无可执行就绪 / 无交易动作）。
- MarketQuoteClient integration（行情客户端接入）：仍未实现，仍暂停，原因是 P199/P200 明确不接 `MarketQuoteClient`，P215 也明确不接。
- runtime source service（运行时数据源服务）：仍未实现，仍暂停，原因是 P232 不接 service / mapper / controller / API（服务 / 映射器 / 控制器 / 接口），不改 mapper 或既有 service。
- service wiring（服务接线）：仍未实现，仍暂停，原因是 P215 不接 mapper / controller / scheduler / runtime service wiring（映射器 / 控制器 / 定时器 / 运行时服务接线）。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）：仍未实现，仍暂停，原因是 P204 只允许 disabled-by-default skeleton（默认关闭骨架），P215 不改 scheduler behavior（定时器行为）。
- scan loop（扫描循环）：仍未实现，仍暂停，原因是 P215 只做文档授权门和 DTO 方案，不创建运行时循环。
- Watchlist scan DTO skeleton（观察库扫描 DTO 骨架）：P208 已完成，但这只是不接线的数据对象骨架，不是运行时扫描结果生产链路。
- real ScanScore computation（真实扫描分数计算）：仍未实现，仍暂停，原因是 P206 只定义规则审计，P207 只定义 guard（保护器）边界，P208 不计算分数，P209 只写 no-score / no-push（无分数 / 无推送）文档边界，P210 只保持 no-score guard（无分数保护器），P215 不实现分数。
- Candidate Attention workflow（候选关注流程）：仍未实现，仍暂停，原因是 P206 只定义契约语义，P215 不创建运行时工作流。
- Promote To Home workflow（提升到首页观察流程）：仍未实现，仍暂停，原因是 P206 只允许人工复核语义，P215 不创建工作流。
- Opportunity Promote execution（机会提升执行）：仍暂停，原因是 Opportunity Promote（机会提升）当前只是提升到首页观察 / 人工复核语义。
- Opportunity Push execution（机会推送执行）：仍未实现，仍暂停，原因是没有推送执行授权，也没有交易动作授权。
- default-six opportunity push（默认六币机会推送）：仍禁止，原因是默认六币只是 Display Slots（首页展示位）空态 / 排序，不是推送全集。
- non-watchlist push candidate（非观察库推送候选）：仍禁止，原因是不在 Watchlist Pool（观察库池）的资产不能进入候选。
- trading actions（交易动作）：仍暂停，原因是只读展示和文档语义不授权平仓、反手、买入、卖出或下单。
- production risk action（生产风控动作）：仍暂停，原因是 Risk Action Guard（风险动作保护器）仍是只读展示，不是生产动作执行。
- auto close / auto reverse / auto buy / auto sell（自动平仓 / 自动反手 / 自动买入 / 自动卖出）：仍暂停，原因是没有授权交易动作和动作接口。
- auto stop modification（自动修改止损）：仍暂停，原因是移动止损和风险提醒仍然只能人工复核。
- Strong Reversal（强反转）automation（自动化）：仍暂停，原因是 P187-P190 完成的是只读展示，不是强反转自动识别或自动处理。
- Moving Stop（移动止损）automation（自动化）：仍暂停，原因是移动止损目前只能作为人工复核提醒，不能自动修改止损。
- ExecutionPlan readiness（执行计划可执行就绪）：仍未升级，仍暂停，原因是 P177-P180 完成的是 review-only display（只允许复核展示），不是可执行计划。
- BoundaryCandidate（边界候选交易计划）真实候选生成：仍暂停，原因是只读候选展示完成不等于真实来源接线和生产候选生成闭环。
- production VALID（生产环境有效候选状态）：仍暂停，原因是 `VALID`（有效候选状态）没有生产来源闭环。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）真实点位：仍暂停，原因是 numeric source ownership（数值来源归属）没有真实闭环。
- AI 多角色冲突处理落地：仍暂停，原因是已有 `AiConflictResolverService`，但多角色冲突处理还不是完整生产裁决链。
- auto-trading（自动交易）：不在 V1 范围内，保持关闭；它只作为禁止越界安全边界，不作为后续进度目标。

## 五、后期必须回来做的线路

- BoundaryCandidate（边界候选交易计划）真实来源接线：必须回来做，因为没有真实来源，`VALID`（有效候选状态）只能停留在 DTO / 测试或局部服务语义。
- ExecutionPlan Readiness（执行计划可执行就绪）：必须回来做，因为 P177-P180 只完成 review-only display（只允许复核展示），真实边界来源、真实点位和风险保护没有闭环前不能升级。
- Risk Action Guard（风险动作保护器）生产接线：必须回来做，因为 P182-P185 只完成只读风险展示，stampede（踩踏）、强反转、移动止损、流动性恶化这些风险还没有生产动作链路。
- Position Monitor（持仓监控）：必须回来做，因为 position sync（持仓同步）和只读风险展示只说明当前持仓可被记录和解释，不说明系统能安全处理强反转、移动止损或退出策略。
- Watchlist / Opportunity Promote（观察库 / 机会提升）后续：必须回来做，因为 P197-P200 只完成语义和安全边界，真实低频扫描、运行时数据源、扫描结果契约、扫描分数、机会提升执行和机会推送执行都还没有完成。
- Alert / Push Channel（告警 / 推送通道）后续：必须回来做，因为 Push / Recheck（推送 / 二次复核）已有状态和调度，但仍需要 review-only（只允许复核）调度审计，不能直接变成交易推送。
- AI 冲突处理：必须回来做，因为 `AiConflictResolverService` 只是规则化冲突分层，还没有形成多角色证据仲裁和生产降级链。
- PROJECT progress index（项目总进度索引）后续维护：必须回来做，因为项目阶段很多，如果索引不更新，后续很容易把 helper（辅助类）、DTO（数据传输对象）或 display（展示）误当成生产完成。

## 六、容易误判为完成但其实没完成的线路

- Watchlist Low-Frequency Scan（观察库低频扫描）语义完成，不等于低频扫描器完成。
- Low-Frequency Scan Scheduler（低频扫描定时器）骨架完成，不等于真实低频扫描完成。
- P204 disabled-by-default scheduler skeleton（默认关闭定时器骨架）不等于 Watchlist runtime data source（观察库运行时数据源）完成。
- P204 disabled-by-default scheduler skeleton（默认关闭定时器骨架）不等于 MarketQuoteClient scan integration（行情客户端扫描接入）完成。
- P204 disabled-by-default scheduler skeleton（默认关闭定时器骨架）不等于 ScanScore（扫描分数）、Candidate Attention（候选关注）或 Promote To Home（提升到首页观察）完成。
- P204 disabled-by-default scheduler skeleton（默认关闭定时器骨架）不等于 Opportunity Push execution（机会推送执行）完成。
- P204 disabled-by-default scheduler skeleton（默认关闭定时器骨架）不等于真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）完成或 Readiness（可执行就绪）升级；auto-trading（自动交易）不在 V1 范围内，保持关闭。
- P206 Low-Frequency Scan Runtime Contract Audit（低频扫描运行时契约审计）已完成，但不等于真实低频扫描完成。
- P206 Watchlist runtime data source contract（观察库运行时数据源契约）已审计，但不等于运行时数据源实现。
- P206 Watchlist scan result contract（观察库扫描结果契约）已审计，但不等于 ScanResult DTO Java（扫描结果数据对象 Java 类）完成。
- P206 ScanScore rule definition（扫描分数规则定义）已审计，但不等于 ScanScore implementation（扫描分数实现）。
- P206 Candidate Attention（候选关注）语义不等于候选关注运行时功能。
- P206 Promote To Home（提升到首页观察）语义不等于提升执行。
- P207 Watchlist Runtime Data Source Authorization Gate（观察库运行时数据源授权门）不等于运行时数据源实现。
- P207 ScanResult DTO skeleton authorization gate（扫描结果数据对象骨架授权门）不等于 ScanResult DTO Java（扫描结果数据对象 Java 类）完成。
- P207 ScanScore guard authorization gate（扫描分数保护器授权门）不等于 ScanScore implementation（扫描分数实现）。
- P207 docs-only authorization gate（只改文档授权门）不等于真实扫描、真实排序、候选关注、提升首页执行或机会推送执行。
- P208 Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）已完成，但不等于真实低频扫描完成。
- P208 WatchlistRuntimeSnapshotDTO（观察库运行时快照数据对象）不等于 Watchlist runtime data source（观察库运行时数据源）实现。
- P208 WatchlistScanResultDTO（观察库扫描结果数据对象）不等于 service wiring（服务接线）或 scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）。
- P208 WatchlistScanStatusEnum（观察库扫描状态枚举）不等于 real ScanScore implementation（真实扫描分数实现）。
- P208 DTO skeleton（数据对象骨架）不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P208 DTO skeleton（数据对象骨架）不接 MarketQuoteClient integration（行情客户端接入）。
- P208 DTO skeleton（数据对象骨架）不创建 Candidate Attention implementation（候选关注实现）、Promote To Home execution（提升到首页观察执行）或 Opportunity Push execution（机会推送执行）。
- P209 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包）不等于 guard / validator（保护器 / 校验器）已实现。
- P209 no-score / no-push safety gate（无分数 / 无推送安全门）只是文档边界，不等于运行时代码。
- P209 docs-only closure（只改文档收口）不等于 runtime data reads（运行时数据读取）、MarketQuoteClient integration（行情客户端接入）、service wiring（服务接线）或 scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）完成。
- P210 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）已完成，但不等于 runtime scan（运行时扫描）完成。
- P210 guard skeleton（保护器骨架）不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- P210 guard skeleton（保护器骨架）不接 MarketQuoteClient integration（行情客户端接入）。
- P210 guard skeleton（保护器骨架）不等于 service wiring（服务接线）或 scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）完成。
- P211 docs-only closure / wiring gate（只改文档收口 / 接线授权门）不等于非 runtime wiring（非运行时接线）已实现。
- P212 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）不等于 runtime scan（运行时扫描）完成。
- P212 test-only wiring skeleton（仅测试接线骨架）不等于 service wiring（服务接线）、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）或 scan loop（扫描循环）完成。
- P213 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）不等于 runtime source（运行时数据源）实现。
- P213 runtime source gate（运行时数据源授权门）不等于 MarketQuoteClient integration（行情客户端接入）完成。
- P213 runtime source gate（运行时数据源授权门）不等于 scheduler activation（定时器激活）或真实低频扫描完成。
- P214 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）不等于 runtime source（运行时数据源）实现。
- P214 runtime source contract（运行时数据源契约）不等于 DB-backed watchlist read（数据库观察库读取）完成。
- P214 freshness / staleness rules（新鲜度 / 过期状态规则）不等于 freshness calculation（新鲜度计算）实现。
- P214 fail-closed / observability rules（失败关闭 / 可观测性规则）不等于日志、指标、DB、API 或 dashboard 展示实现。
- P215 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）不等于 runtime source（运行时数据源）实现。
- P215 DTO skeleton plan（数据对象骨架方案）不等于 `WatchlistRuntimeSourceDTO` Java 已实现。
- P215 authorization gate（授权门）不等于 DB-backed watchlist read（数据库观察库读取）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）或 scan loop（扫描循环）完成。
- P215 方案文档不等于 runtime source service（运行时数据源服务）、API response（接口响应）、dashboard display（首页展示）或 observability logging / metrics（可观测性日志 / 指标）实现。
- P216 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）不等于 runtime source implementation（运行时数据源实现）。
- P216 `WatchlistRuntimeSourceDTO` Java 已存在，不等于 DB-backed watchlist read（数据库观察库读取）、MarketQuoteClient integration（行情客户端接入）、scheduler activation（定时器激活）、runtime source service（运行时数据源服务）或 scan loop（扫描循环）完成。
- P217 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）不等于 runtime source implementation（运行时数据源实现）已完成。
- P218 Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）不等于 runtime source wiring（运行时数据源接线）或 production runtime read（生产运行时读取）已完成。
- P217 runtime read still blocked（运行时读取仍阻断）不等于 DB / runtime / MarketQuoteClient / scheduler 禁令解除。
- DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）和 test-only wiring skeleton（仅测试接线骨架）可存在，但 runtime scan（运行时扫描）仍然被阻断。
- Opportunity Promote（机会提升）语义完成，不等于 Opportunity Push execution（机会推送执行）完成。
- Display Slots（首页展示位）不是推送候选。
- 默认六币不是默认推送全集。
- Watchlist Pool（观察库池）是最大候选边界，但不是交易候选，也不会进入自动交易；auto-trading（自动交易）不在 V1 范围内。
- 非观察库资产不能进入推送候选。
- Opportunity Promote（机会提升）只是提升到首页观察 / 人工复核。
- Opportunity Promote（机会提升）不是订单。
- Opportunity Promote（机会提升）不是交易信号。
- Opportunity Promote（机会提升）不是 Readiness（可执行就绪）。
- docs-only minimal wiring（只改文档的最小接线）不等于运行时功能。
- 没有 MarketQuoteClient scan integration（行情客户端扫描接入），不等于可扫实时行情。
- 没有 scheduler（定时器），不等于系统会自动扫描。
- Dashboard Risk Reminder（首页风险提醒）已完成只读展示，不等于首页能交易。
- 风险建议展示，不等于交易建议。
- 阻断原因展示，不等于可以执行。
- `marketOrderExitAllowed=false` 说明不允许市价退出。
- `opportunityPushAllowed=false` / `reverseTradeAllowed=false` / `newPositionAllowed=false` 说明机会推送 / 反手 / 新开仓仍关闭。
- `manualRiskReviewRequired=true`（必须人工复核）说明必须人工复核。
- `notTradeInstruction=true`（不是交易指令）说明不是交易指令。
- Dashboard（首页工作台）能集中看风险提醒，不等于能下单。
- Risk Reminder（风险提醒）只能解释为什么不能执行，不能触发执行。
- Risk Action Guard（风险动作保护器）已完成 read-only risk display（只读风险展示），不等于 production risk action（生产风控动作）。
- Position Monitor（持仓监控）有同步 / 告警 / 展示基础，不等于自动平仓。
- Position Monitor（持仓监控）已完成 Strong Reversal（强反转）/ Moving Stop（移动止损）只读展示，不等于 Strong Reversal（强反转）自动识别与自动处理。
- 高风险提示不等于立即止损。
- Strong Reversal（强反转）待确认，不等于直接反手。
- Strong Reversal（强反转）提示不等于自动平仓。
- 原入场逻辑疑似失效，不等于立即退出。
- Moving Stop（移动止损）需要人工复核，不等于自动修改止损。
- Stampede（踩踏）风险提示不等于 Opportunity Push（机会推送）。
- Wick-only Risk（仅插针风险）/ 插针风险不等于趋势反转。
- auto close / auto reverse / auto stop modification（自动平仓 / 自动反手 / 自动修改止损）均关闭，说明交易动作仍关闭。
- SourceTrace（证据来源追踪）已完成 Dashboard（首页工作台）只读展示，不等于真实运行时候选生成完成。
- BoundaryCandidate（边界候选交易计划）已完成 read-only candidate display（只读候选展示），不等于真实候选生成完成。
- `VALID`（有效候选状态）被安全降级，不等于 production VALID（生产环境有效候选状态）可用。
- ExecutionPlan（执行计划）已完成 review-only display（只允许复核展示），不等于可执行计划完成。
- `READY_REVIEW_ONLY`（只允许复核的就绪摘要）是只允许复核摘要，不是 Readiness（可执行就绪）已打开。
- `ENTRY_STOP_TP_RR_NOT_GENERATED` 出现，说明真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）未生成。
- helper（辅助类）/ service（服务）完成，不等于 Controller（控制器）/ API（接口）动作能力完成。
- DTO（数据传输对象）字段完成，不等于真实数据来源完成。
- Watchlist Pool（观察库池）完成，不等于低频扫描和机会提升执行完成。

## 七、禁止提前做的线路

- 自动交易。
- 下单接口。
- 自动平仓。
- 自动反手。
- 自动买入 / 自动卖出。
- 自动修改 Stop Loss（止损）/ Moving Stop（移动止损）。
- active Low-Frequency Scan scheduler（激活低频扫描定时器）。
- Watchlist runtime data source（观察库运行时数据源）。
- MarketQuoteClient scan integration（行情客户端扫描接入）。
- ScanScore implementation（扫描分数实现）。
- Candidate Attention implementation（候选关注实现）。
- Promote To Home execution（提升到首页观察执行）。
- Opportunity Promote execution（机会提升执行）。
- Opportunity Push execution（机会推送执行）。
- default-six opportunity push（默认六币机会推送）。
- non-watchlist asset -> push candidate（非观察库资产进入推送候选）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）在 SourceTrace（证据来源追踪）/ BoundaryCandidate（边界候选交易计划）来源未闭环前不能做。
- `VALID`（有效候选状态）在来源链路未闭环前不能做。
- Readiness（可执行就绪）在真实边界来源未闭环前不能升级。
- Dashboard（首页工作台）可执行状态在 Readiness（可执行就绪）未闭环前不能打开。
- order（下单）/ execution（执行）/ scheduler（定时器）/ automation（自动化）/ auto-trading（自动交易）不能借 Push / Recheck（推送 / 二次复核）、Watchlist（观察库）、Opportunity Promote（机会提升）、position sync（持仓同步）或 read-only display（只读展示）名义提前进入。

## 八、推荐下一阶段顺序

### 路线 A：个人可用最快路径

1. Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计）。
2. Manual Position Review Workflow Audit（手动持仓复核流程审计）。
3. Dashboard Personal Use Smoke Checklist（个人可用冒烟清单）。
4. Personal Paper Trading Observation Plan（个人纸面观察计划）。

### 路线 B：开始真实低频扫描前置

1. P206 Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包，已完成）。
2. P207 Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包，已完成）。
3. P208 Watchlist Scan Runtime DTO Skeleton 或 Watchlist Scan Result DTO Skeleton（观察库扫描运行时 / 扫描结果数据对象骨架，已完成）。
4. P209 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包，已完成）。
5. P210 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架，已完成）。
6. P211 Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门，已完成）。
7. P212 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架，已完成）。
8. P213 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门，已完成）。
9. P214 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包，已完成）。
10. P215 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案，已完成）。
11. P216 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架，已完成）。
12. P217 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界，已完成）。
13. 完成 P218 Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）。
14. Runtime source implementation（运行时数据源实现）后续必须另开授权门，不能因为 DTO skeleton（数据对象骨架）、guard boundary（保护边界）或 guard skeleton（保护器骨架）直接接 DB、行情、scheduler 或 scan loop（扫描循环）。
15. ScanScore Guard / Rule Definition 后续授权门（扫描分数保护器 / 规则定义后续授权门）。
16. Low-Cost AI Event Explanation Gate（低成本 AI 事件解释授权门）。
17. Three-AI Promote-To-Home Review Gate（三 AI 提升到首页复核授权门）。

### 路线 C：继续严谨后端交易候选

1. BoundaryCandidate Source Wiring Scope Audit（边界候选来源接线范围审计）。
2. Numeric Source Ownership Audit（数值来源归属审计）。
3. ExecutionPlan Readiness Scope Audit（执行计划可执行就绪范围审计）。
4. Risk Action Guard Production Wiring Scope Audit（风险动作保护器生产接线范围审计）。

推荐结论：

- 如果目标是个人可用，优先路线 A。
- 如果目标是开始做真实 Low-Frequency Scan（低频扫描），先走路线 B，但第一步仍然是审计，不是直接写 scheduler（定时器）。
- 如果目标是严格生产候选，优先路线 C。
- 无论哪条路线，都不能直接进入自动交易、自动平仓、自动反手、自动改止损、真实点位或 Opportunity Push execution（机会推送执行）。

## 九、模块进度表

| 模块 | 当前状态 | 完成度 | 证据 / 文件线索 | 下一步 |
|---|---|---:|---|---|
| Project Overall（项目总进度） | 安全地基、只读展示、SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、Dashboard Risk Reminder（首页风险提醒）、Watchlist / Opportunity Promote（观察库 / 机会提升）语义边界、P206-P240 runtime source contract / DTO / guard / wiring / adapter / DB Watchlist Pool read skeleton / service skeleton / scan result assembly skeleton / orchestrator gate 均已完成。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包），是 docs-only gate pack，不是实现；真实扫描器、真实候选、真实点位、Readiness（可执行就绪）、生产风控动作和交易动作未完成；auto-trading（自动交易）不在 V1 范围内，保持关闭 | 72%-77% | `PROJECT_PROGRESS_INDEX.md`、`PHASE_P249_BATCH_ENVELOPE_DTO_AUTHORIZATION_GATE.md`、`PHASE_P249_BATCH_JAVA_SKELETON_AUTHORIZATION_GATE.md`、`PHASE_P249_BATCH_FAIL_CLOSED_RULES.md` | P255 后仍不能接真实扫描、行情、scheduler、batch implementation、real scan loop、ScanScore、Candidate Attention、Push、readiness、production Runtime Source Service wiring 或 Production Source Assembler；service skeleton、assembly skeleton、orchestrator skeleton 和 batch envelope plan 不等于 production read implementation，production runtime source read、real scan 和 runtime source implementation 必须另开授权门 |
| Safety Foundation（安全地基） | 失败关闭、只读复核、不是交易指令、人工复核、禁止自动动作、最大安全任务包规则、首页集中风险提醒和观察库扫描提升语义边界继续增强；仍不能替代真实生产授权 | 88%-94% | `StaticNoTradeInstructionGuardTest.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、多份 P140-P206 文档 | 继续把新增能力先放进范围门和只读复核 |
| SourceTrace（证据来源追踪） | 只读输出 + Dashboard（首页工作台）只读展示已完成；真实候选 / 真实点位 / Readiness（可执行就绪）未完成，本轮不再上调 | 58%-66% | `SourceTraceRuntimePopulationHelper.java`、`SourceTraceRuntimePopulationServiceImpl.java`、`DefaultDashboardSourceTraceDetailAdapter.java`、`dashboard.html`、`PHASE_BACKEND_P170*` | 个人可用路线转告警推送审计；严谨路线继续 BoundaryCandidate 来源审计 |
| BoundaryCandidate（边界候选交易计划） | DTO（数据对象）/ service skeleton（服务骨架）/ read-only candidate display（只读候选展示）已完成；生产候选 / 真实点位 / production VALID（生产环境有效候选状态）未完成 | 42%-52% | `BoundaryCandidateDTO.java`、`BoundaryCandidateServiceImpl.java`、`BoundaryCandidateServiceImplTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultPlanBoundaryDisplayAdapterTest.java`、`PHASE_BACKEND_P175*` | 严谨路线做 source wiring audit（来源接线审计） |
| ExecutionPlan（执行计划） | 已完成 review-only display（只允许复核展示）；Readiness（可执行就绪）/ 真实点位未完成；auto-trading（自动交易）不在 V1 范围内，保持关闭 | 45%-55% | `PlanServiceImpl.java`、`ExecutionPlanVO.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapterTest.java`、`PHASE_BACKEND_P180_EXECUTION_PLAN_REVIEW_ONLY_DISPLAY_CLOSURE.md` | 继续禁止 Readiness（可执行就绪）升级，严谨路线再做 readiness scope audit（可执行就绪范围审计） |
| Risk Action Guard（风险动作保护器） | 已完成只读风险展示、持仓强反转 / 移动止损只读解释、首页风险提醒集中展示；生产风控动作 / 自动执行未完成，本轮不明显上调 | 47%-57% | `DefaultRiskActionGuardDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapterTest.java`、`dashboard.html`、`PHASE_BACKEND_P185_RISK_ACTION_GUARD_POSITION_MONITOR_CLOSURE.md`、`PHASE_BACKEND_P190_POSITION_MONITOR_STRONG_REVERSAL_MOVING_STOP_CLOSURE.md`、`PHASE_BACKEND_P195_DASHBOARD_RISK_REMINDER_READ_ONLY_DISPLAY_CLOSURE.md` | 个人路线做告警 / 推送只读调度审计；严谨路线做生产接线范围审计 |
| Position Monitor（持仓监控） | 同步 / 告警 / 记录基础 + 只读风险展示 + 强反转 / 移动止损只读展示已完成；强反转自动识别 / 自动处理 / 自动改止损 / 自动平仓未完成，本轮不明显上调 | 52%-62% | `PositionSyncService.java`、`PositionSyncScheduler.java`、`RealPositionMapper.java`、`MonitorAlertMapper.java`、`tm_real_position`、`tm_monitor_alert`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P190*` | 下一步只能走人工复核流程或风险提醒展示，继续禁止自动动作 |
| Dashboard（首页工作台） | SourceTrace（证据来源追踪）+ BoundaryCandidate（边界候选交易计划）+ ExecutionPlan（执行计划）+ RiskActionGuard（风险动作保护器）+ Position Monitor（持仓监控）+ Dashboard Risk Reminder（首页风险提醒）只读展示增强已完成；可执行状态未打开，本轮不明显上调 | 76%-84% | `DashboardController.java`、`dashboard.html`、`DashboardControllerTest.java`、`DefaultPlanBoundaryDisplayAdapter.java`、`DefaultExecutionPlanDisplayAdapter.java`、`DefaultRiskActionGuardDisplayAdapter.java`、`PHASE_BACKEND_P170*`、`PHASE_BACKEND_P175*`、`PHASE_BACKEND_P180*`、`PHASE_BACKEND_P185*`、`PHASE_BACKEND_P190*`、`PHASE_BACKEND_P195*` | 只允许继续做告警推送、人工复核或个人可用冒烟，不打开可执行状态 |
| Watchlist / Display Slots / Opportunity Promote（观察库 / 首页展示位 / 机会提升） | Display Slots / Watchlist Pool / Low-Frequency Scan / Opportunity Promote（首页展示位 / 观察库池 / 低频扫描 / 机会提升）语义边界已完成，P206-P240 已补 runtime contract、DTO、guard、test-only wiring、runtime source adapter、DB Watchlist Pool config read skeleton、Runtime Source Service skeleton、Watchlist Scan Result Assembly skeleton 和 Low-Frequency Scan Orchestrator Gate。P241 已完成 docs-only Low-Frequency Scan Orchestrator plan / scan loop boundary audit；P243 已完成 docs-only closure / Scheduler-Batch-Market authorization gate；P244 已完成 docs-only scope audit / layered gate plan；P245 已完成 docs-only scheduler trigger authorization gate；P246 已完成 disabled scheduler wiring skeleton；P247 已完成 docs-only closure / batch scan authorization gate；P251 已完成 docs-only accelerated scope pack。P252 已完成 read-only fail-closed market-read adapter skeleton。P253 已完成 review-only ScanScore DTO / rule skeleton，P254 已完成 review-only calculation skeleton，P255 当前推进 docs-only gate pack；`WatchlistRuntimeSourceDTO` Java 已存在，runtime source guard / validator（运行时数据源保护器 / 校验器）已存在，Runtime Source Service skeleton 已存在，Watchlist Scan Result Assembly skeleton 已存在，WatchlistLowFrequencyScanScheduler disabled-by-default skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在，但 production read implementation（生产读取实现）、production runtime source read（生产运行时数据源读取）、Low-Frequency Watchlist Scan Orchestrator skeleton 已存在，但 BatchWatchlistScanOrchestrator（批量观察库扫描编排器）已存在、BatchWatchlistScanResultEnvelopeDTO（批量观察库扫描结果信封 DTO）已存在、Batch scan Java（批量扫描 Java）进入最小 skeleton 阶段、Batch result envelope Java（批量结果信封 Java）进入最小 DTO 阶段，但 batch orchestrator（批量扫描编排器）生产接线、real scan loop、真实扫描器、实时数据、Production Source Assembler、scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、ScanScore computation（扫描分数计算）和推送执行仍未完成；auto-trading（自动交易）不在 V1 范围内，保持关闭 | 65%-75% | `PHASE_P240_LOW_FREQUENCY_SCAN_ORCHESTRATOR_AUTHORIZATION_GATE.md`、`PHASE_P241_LOW_FREQUENCY_SCAN_ORCHESTRATOR_PLAN.md`、`PHASE_P241_DISABLED_BY_DEFAULT_SCAN_LOOP_BOUNDARY_AUDIT.md`、`PHASE_P241_ORCHESTRATOR_JAVA_AUTHORIZATION_GATE.md` | P255 后仍不能直接接 MarketQuoteClient、真实 service wiring、scheduler、real scan loop、ScanScore、Candidate Attention、Promote To Home、Push、readiness 或 point generation |
| Low-Frequency Scan Scheduler（低频扫描定时器） | P202-P204 默认关闭 scheduler skeleton 已完成；P206-P240 已逐步补齐 runtime contract、DTO、guard、test-only wiring、runtime source adapter、DB Watchlist Pool read skeleton、Runtime Source Service skeleton、Watchlist Scan Result Assembly skeleton 和 Orchestrator gate。P241 已完成 docs-only orchestrator plan / scan loop boundary audit；P243 已完成 docs-only closure / Scheduler-Batch-Market authorization gate；P244 已完成 docs-only scope audit / layered gate plan；P245 已完成 docs-only scheduler trigger authorization gate；P246 已完成 disabled scheduler wiring skeleton；P247 已完成 docs-only closure / batch scan authorization gate；P251 已完成 docs-only accelerated scope pack。P252 已完成 read-only fail-closed market-read adapter skeleton。P253 已完成 review-only ScanScore DTO / rule skeleton，P254 已完成 review-only calculation skeleton，P255 当前推进 docs-only gate pack，不是真实扫描；Low-Frequency Watchlist Scan Orchestrator skeleton 已存在，但 real scan loop、MarketQuoteClient integration、scheduler behavior changes、real ScanScore computation、Candidate Attention、Promote To Home、Opportunity Push execution、真实 entry / stop / TP / RR 和 readiness 仍未实现 | 15%-25% | `WatchlistLowFrequencyScanScheduler.java`、`WatchlistLowFrequencyScanSchedulerTest.java`、`PHASE_P205_LOW_FREQUENCY_SCAN_SCHEDULER_MINIMAL_SKELETON_CLOSURE.md`、`PHASE_P240_LOW_FREQUENCY_SCAN_ORCHESTRATOR_AUTHORIZATION_GATE.md`、`PHASE_P241_DISABLED_BY_DEFAULT_SCAN_LOOP_BOUNDARY_AUDIT.md` | 仍不接真实扫描；任何 scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）、real scan loop、MarketQuoteClient integration、ScanScore、Candidate Attention、Push 或 readiness 必须另开授权 |
| Watchlist / Push（观察库 / 推送） | Push snapshot（推送快照）、Recheck（二次复核）、scheduler（定时器）和 ops overview（运维总览）存在；但它们不是 Watchlist Low-Frequency Scan（观察库低频扫描），不是 Opportunity Push execution（机会推送执行） | 45%-55% | `PushRecheckServiceImpl.java`、`PushRecheckScheduler.java`、`PushSnapshotService.java`、`PHASE_P11A_PUSH_RECHECK_NAMING_VERIFICATION.md` | 先做 Alert / Push Channel Review-Only Dispatch Audit（告警 / 推送通道只允许复核调度审计） |
| AI multi-agent（AI 多角色） | `AiConflictResolverService` 已有冲突分层；多角色生产仲裁链未落地 | 25%-35% | `AiConflictResolverService.java`、`AiConflictResolverServiceImpl.java`、`DecisionEngineServiceTest.java` | 定义多角色输入、冲突降级和人工复核边界 |
| Production Wiring（真实生产接线） | 真实来源、真实候选、生产风控动作、执行授权仍未闭环，不因 P197-P206 文档语义明显上调 | 26%-34% | P140-P206 文档和现有 service / adapter / dashboard 只读链路 | 先做 BoundaryCandidate 来源接线或数值来源归属审计 |
| entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比） | DTO 和 fixture（测试夹具）存在；真实数值来源未闭环，不因观察库语义完成而上调 | 10%-18% | `BoundaryEntryDTO.java`、`BoundaryStopDTO.java`、`BoundaryTakeProfitLevelDTO.java`、`StopTpRrSourceOwnedCandidateFixtureHelper.java` | 先做 numeric source ownership（数值来源归属）审计 |
| Auto-trading（自动交易） | 不在 V1 范围内，保持关闭；只作为禁止越界安全边界，不作为进度目标 | 不适用 | `StaticNoTradeInstructionGuardTest.java`、多份 P140-P206 禁止清单 | 继续禁止，不纳入 V1 推进路线 |

## 十、P206-P248 结论

P206 只刷新运行时契约审计相关文档和项目状态索引。

P206 不写代码。

P206 不新增测试。

P206 不改 `dashboard.html`。

P206 不接 API（接口）。

P206 不接 `MarketQuoteClient`。

P206 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P206 不创建真实 Low-Frequency Scan（低频扫描）。

P206 不创建 ScanResult DTO Java（扫描结果数据对象 Java 类）。

P206 不实现 ScanScore（扫描分数）。

P206 不实现 Candidate Attention（候选关注）。

P206 不实现 Promote To Home（提升到首页观察）。

P206 不创建 Opportunity Promote execution（机会提升执行）。

P206 不创建 Opportunity Push execution（机会推送执行）。

P206 不生成交易点位。

P206 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。

P206 不升级 Readiness（可执行就绪）。

P206 不接 auto-trading（自动交易）；auto-trading 不在 V1 范围内，保持关闭。

P206 本轮严格禁止并确认：

- 不新增 Java。
- 不新增测试。
- 不改 production Java。
- 不改现有测试。
- 不改 `dashboard.html`。
- 不新增 controller / endpoint / API / schema / config / service / mapper（控制器 / 接口 / API / 数据库结构 / 配置 / 服务 / 映射）。
- 不改 `PushRecheckScheduler` / `PushRecheckService` / `PushSnapshotService`。
- 不改 `RuleController` / `RuleConfigService` / `RuleConfigMapper`。
- 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不接 `MarketQuoteClient`。
- 不创建真实 Low-Frequency Scan scheduler（低频扫描定时器）。
- 不创建 ScanResult DTO Java（扫描结果数据对象 Java 类）。
- 不实现 ScanScore（扫描分数）。
- 不实现 Candidate Attention（候选关注）。
- 不实现 Promote To Home（提升到首页观察）。
- 不创建 Opportunity Push execution（机会推送执行）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建 close / reverse / buy / sell（平仓 / 反手 / 买入 / 卖出）动作。
- 不升级 ExecutionPlan readiness（执行计划可执行就绪）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P206 的核心结论是：Low-Frequency Scan Runtime Contract Audit Pack（低频扫描运行时契约审计包）只把 Watchlist runtime data source contract（观察库运行时数据源契约）、Watchlist scan result contract（观察库扫描结果契约）和 ScanScore rule definition（扫描分数规则定义）放进文档边界；但 real low-frequency scan（真实低频扫描）、runtime data source（运行时数据源）、MarketQuoteClient scan integration（行情客户端扫描接入）、scan scheduler activation（扫描定时器激活）、ScanScore implementation（扫描分数实现）、Candidate Attention implementation（候选关注实现）、Promote To Home execution（提升到首页观察执行）、Opportunity Promote execution（机会提升执行）、Opportunity Push execution（机会推送执行）、真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）、ExecutionPlan Readiness（执行计划可执行就绪）和 trading buttons（交易按钮）仍未完成；auto-trading（自动交易）不在 V1 范围内，保持关闭。

P207 已完成 Watchlist Runtime Data Source Authorization Gate Pack（观察库运行时数据源授权门包）。

P207 是 docs-only authorization gate（只改文档授权门），不是实现。

P207 只为未来 P208 pure DTO / enum / tests skeleton（纯数据传输对象 / 枚举 / 测试骨架）准备边界。

P207 不创建真实 Low-Frequency Scan（低频扫描）。

P207 不实现 Watchlist runtime data source（观察库运行时数据源）。

P207 不接 MarketQuoteClient scan integration（行情客户端扫描接入）。

P207 不创建 ScanResult DTO Java（扫描结果数据对象 Java 类）。

P207 不实现 ScanScore implementation（扫描分数实现）。

P207 不创建 Candidate Attention implementation（候选关注实现）。

P207 不创建 Promote To Home implementation / execution（提升到首页观察实现 / 执行）。

P207 不创建 Opportunity Push execution（机会推送执行）。

P207 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P207 不升级 readiness（可执行就绪）。

P207 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P207 不因为文档授权门而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P207 后仍未完成 / 未实现 / 未升级清单：

- 真实低频扫描仍未完成。
- Watchlist runtime data source（观察库运行时数据源）仍未实现。
- MarketQuoteClient scan integration（行情客户端扫描接入）仍未实现。
- ScanResult DTO Java（扫描结果数据对象 Java 类）仍未实现。
- ScanScore implementation（扫描分数实现）仍未实现。
- Candidate Attention（候选关注）仍未实现。
- Promote To Home execution（提升到首页观察执行）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P208 已完成 Watchlist Scan Runtime DTO Skeleton（观察库扫描运行时 DTO 骨架）。

P208 是 pure DTO / enum / tests（纯数据传输对象 / 枚举 / 测试），不是实现。

P208 新增的 DTO / enum 只能表达 fail-closed（失败关闭）、review-only（只允许复核）、manualReviewRequired=true（必须人工复核）和 notTradeInstruction=true（不是交易指令）。

P208 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P208 不实现 Watchlist runtime data source（观察库运行时数据源）。

P208 不接 MarketQuoteClient integration（行情客户端接入）。

P208 不接 service wiring（服务接线）。

P208 不改 scheduler behavior（定时器行为）。

P208 不实现 real ScanScore implementation（真实扫描分数实现）。

P208 不实现 Candidate Attention implementation（候选关注实现）。

P208 不实现 Promote To Home execution（提升到首页观察执行）。

P208 不创建 Opportunity Push execution（机会推送执行）。

P208 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P208 不升级 readiness（可执行就绪）。

P208 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P208 不因为 DTO skeleton（数据对象骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P208 后仍未完成 / 未实现 / 未升级清单：

- 真实低频扫描仍未完成。
- Runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- service wiring（服务接线）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- real ScanScore implementation（真实扫描分数实现）仍未实现。
- Candidate Attention implementation（候选关注实现）仍未实现。
- Promote To Home execution（提升到首页观察执行）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P209 已完成 Watchlist Scan DTO Closure and Guard Boundary Pack（观察库扫描 DTO 收口与保护边界包）。

P209 是 docs-only closure / boundary（只改文档收口 / 边界），不是实现。

P209 只记录 P208 DTO skeleton（数据对象骨架）的完成内容、测试确认和未完成边界。

P209 只定义未来 guard / validator（保护器 / 校验器）和 no-score / no-push gate（无分数 / 无推送安全门）的边界。

P209 不写 Java。

P209 不新增测试。

P209 不修改 P208 DTO Java。

P209 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P209 不实现 Watchlist runtime data source（观察库运行时数据源）。

P209 不接 MarketQuoteClient integration（行情客户端接入）。

P209 不接 service wiring（服务接线）。

P209 不改 scheduler behavior（定时器行为）。

P209 不实现 real ScanScore computation（真实扫描分数计算）。

P209 不实现 Candidate Attention workflow（候选关注流程）。

P209 不实现 Promote To Home workflow（提升到首页观察流程）。

P209 不创建 Opportunity Push execution（机会推送执行）。

P209 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P209 不升级 readiness（可执行就绪）。

P209 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P209 不因为文档收口而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P209 后仍未完成 / 未实现 / 未升级清单：

- DTO skeleton（数据对象骨架）已存在，但真实低频扫描仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- service wiring（服务接线）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P210 已完成 Watchlist Scan Guard Validator Skeleton（观察库扫描保护校验器骨架）。

P210 是 pure guard / validator / tests（纯保护器 / 校验器 / 测试），不是 runtime scan（运行时扫描）。

P210 可以新增 guard skeleton（保护器骨架），但不接 runtime service wiring（运行时服务接线）。

P210 不修改 P208 DTO Java。

P210 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P210 不实现 Watchlist runtime data source（观察库运行时数据源）。

P210 不接 MarketQuoteClient integration（行情客户端接入）。

P210 不接 mapper / controller / scheduler / dashboard（映射器 / 控制器 / 定时器 / 首页）。

P210 不改 scheduler behavior（定时器行为）。

P210 不实现 real ScanScore computation（真实扫描分数计算）。

P210 不实现 Candidate Attention workflow（候选关注流程）。

P210 不实现 Promote To Home workflow（提升到首页观察流程）。

P210 不创建 Opportunity Push execution（机会推送执行）。

P210 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P210 不升级 readiness（可执行就绪）。

P210 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P210 不因为 guard skeleton（保护器骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P210 后仍未完成 / 未实现 / 未升级清单：

- DTO skeleton（数据对象骨架）已存在。
- Guard skeleton（保护器骨架）可存在，但真实低频扫描仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- service wiring（服务接线）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P211 已完成 Watchlist Scan Guard Validator Closure and Wiring Gate（观察库扫描保护校验器收口与接线授权门）。

P211 只是 docs-only closure / wiring gate（只改文档收口 / 接线授权门），不是实现。

P211 只记录 P210 guard skeleton（保护器骨架）完成内容，定义未来非 runtime wiring（非运行时接线）边界，并确认 runtime scan（运行时扫描）仍被阻断。

P211 不写 Java。

P211 不新增测试。

P211 不修改 DTO / guard / validator（数据对象 / 保护器 / 校验器）。

P211 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P211 不实现 Watchlist runtime data source（观察库运行时数据源）。

P211 不接 MarketQuoteClient integration（行情客户端接入）。

P211 不接 service wiring（服务接线）。

P211 不接 mapper / controller / scheduler / dashboard（映射器 / 控制器 / 定时器 / 首页）。

P211 不改 scheduler behavior（定时器行为）。

P211 不实现 real ScanScore computation（真实扫描分数计算）。

P211 不实现 Candidate Attention workflow（候选关注流程）。

P211 不实现 Promote To Home workflow（提升到首页观察流程）。

P211 不创建 Opportunity Push execution（机会推送执行）。

P211 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P211 不升级 readiness（可执行就绪）。

P211 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P211 不因为文档收口而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P211 后仍未完成 / 未实现 / 未升级清单：

- DTO skeleton（数据对象骨架）和 guard skeleton（保护器骨架）已存在。
- real low-frequency scan（真实低频扫描）仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- service wiring（服务接线）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P212 已完成 Watchlist Scan Guard Test-Only Wiring Skeleton（观察库扫描保护仅测试接线骨架）。

P212 只是 non-runtime test-only wiring / assembler skeleton（非运行时、仅测试级接线 / 组装器骨架），不是 runtime scan（运行时扫描）。

P212 只允许新增 `WatchlistScanGuardWiringAssembler`、`DefaultWatchlistScanGuardWiringAssembler` 和 `DefaultWatchlistScanGuardWiringAssemblerTest`。

P212 DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）和 test-only wiring skeleton（仅测试接线骨架）可存在，但真实低频扫描仍未完成。

P212 不修改 P208 DTO Java。

P212 不修改 P210 guard / validator Java。

P212 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P212 不实现 Watchlist runtime data source（观察库运行时数据源）。

P212 不接 MarketQuoteClient integration（行情客户端接入）。

P212 不接 mapper / controller / scheduler / dashboard（映射器 / 控制器 / 定时器 / 首页）。

P212 不改 scheduler behavior（定时器行为）。

P212 不创建 scan loop（扫描循环）。

P212 不实现 real ScanScore computation（真实扫描分数计算）。

P212 不实现 Candidate Attention workflow（候选关注流程）。

P212 不实现 Promote To Home workflow（提升到首页观察流程）。

P212 不创建 Opportunity Push execution（机会推送执行）。

P212 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P212 不升级 readiness（可执行就绪）。

P212 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P212 不因为 test-only wiring skeleton（仅测试接线骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P212 后仍未完成 / 未实现 / 未升级清单：

- DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）和 test-only wiring skeleton（仅测试接线骨架）可存在。
- real low-frequency scan（真实低频扫描）仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- service wiring（服务接线）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- scan loop（扫描循环）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P213 当前推进 Watchlist Scan Test-Only Wiring Closure and Runtime Source Gate（观察库扫描仅测试接线收口与运行时数据源授权门）。

P213 只是 docs-only closure / runtime source gate（只改文档收口 / 运行时数据源授权门），不是实现。

P213 只记录 P212 test-only wiring skeleton（仅测试接线骨架）完成内容，并定义未来 runtime source（运行时数据源）授权门。

P213 DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）、test-only wiring skeleton（仅测试接线骨架）已存在，但真实低频扫描仍未完成。

P213 不写 Java。

P213 不新增测试。

P213 不修改 P208 DTO Java。

P213 不修改 P210 guard / validator Java。

P213 不修改 P212 assembler Java。

P213 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P213 不实现 Watchlist runtime data source（观察库运行时数据源）。

P213 不接 MarketQuoteClient integration（行情客户端接入）。

P213 不接 scheduler（定时器）。

P213 不接 mapper / controller / dashboard（映射器 / 控制器 / 首页）。

P213 不改 scheduler behavior（定时器行为）。

P213 不创建 scan loop（扫描循环）。

P213 不实现 real ScanScore computation（真实扫描分数计算）。

P213 不实现 Candidate Attention workflow（候选关注流程）。

P213 不实现 Promote To Home workflow（提升到首页观察流程）。

P213 不创建 Opportunity Push execution（机会推送执行）。

P213 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P213 不升级 readiness（可执行就绪）。

P213 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P213 不因为文档收口而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P213 后仍未完成 / 未实现 / 未升级清单：

- DTO skeleton（数据对象骨架）、guard skeleton（保护器骨架）和 test-only wiring skeleton（仅测试接线骨架）已存在。
- real low-frequency scan（真实低频扫描）仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- scan loop（扫描循环）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P214 已完成 Watchlist Runtime Source Contract Definition Pack（观察库运行时数据源契约定义包）。

P214 只是 docs-only contract definition（只改文档契约定义），不是实现。

P214 只定义未来 Watchlist runtime source contract（观察库运行时数据源契约）、freshness / staleness（新鲜度 / 过期状态）规则，以及 fail-closed / observability（失败关闭 / 可观测性）规则。

P214 runtime source contract（运行时数据源契约）可文档化，但 runtime source（运行时数据源）仍未实现。

P214 不写 Java。

P214 不新增测试。

P214 不修改 P208 DTO Java。

P214 不修改 P210 guard / validator Java。

P214 不修改 P212 assembler Java。

P214 不改 dashboard.html。

P214 不改 schema（数据库结构）。

P214 不改 config（配置）。

P214 不接 API（接口）。

P214 不接 mapper / controller / dashboard / runtime service wiring（映射器 / 控制器 / 首页 / 运行时服务接线）。

P214 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P214 不读 DB-backed watchlist（数据库观察库读取）。

P214 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P214 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P214 不创建 scan loop（扫描循环）。

P214 不创建 real low-frequency scan（真实低频扫描）。

P214 不实现 real ScanScore computation（真实扫描分数计算）。

P214 不实现 Candidate Attention workflow（候选关注流程）。

P214 不实现 Promote To Home workflow（提升到首页观察流程）。

P214 不创建 Opportunity Push execution（机会推送执行）。

P214 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P214 不升级 readiness（可执行就绪）。

P214 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P214 不因为文档契约定义而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P214 后仍未完成 / 未实现 / 未升级清单：

- runtime source contract（运行时数据源契约）可文档化，但 runtime source（运行时数据源）仍未实现。
- real low-frequency scan（真实低频扫描）仍未完成。
- runtime data reads（运行时数据读取）仍未实现。
- DB-backed watchlist read（数据库观察库读取）仍未实现。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- scan loop（扫描循环）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P215 已完成 Watchlist Runtime Source Authorization Gate and DTO Skeleton Plan（观察库运行时数据源授权门与数据对象骨架方案）。

P215 只是 docs-only authorization gate / DTO skeleton plan（只改文档授权门 / 数据对象骨架方案），不是实现。

P215 runtime source contract（运行时数据源契约）已文档化，但 runtime source（运行时数据源）仍未实现。

P215 只授权未来可能进入 pure DTO / enum / tests skeleton（纯数据对象 / 枚举 / 测试骨架）。

P215 不授权 P216 直接实现 runtime source（运行时数据源）。

P215 不写 Java。

P215 不新增测试。

P215 不修改 P208 DTO Java。

P215 不修改 P210 guard / validator Java。

P215 不修改 P212 assembler Java。

P215 不改 dashboard.html。

P215 不改 schema（数据库结构）。

P215 不改 config（配置）。

P215 不接 API（接口）。

P215 不接 mapper / service / controller / dashboard / runtime service wiring（映射器 / 服务 / 控制器 / 首页 / 运行时服务接线）。

P215 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P215 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P215 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P215 不创建 scan loop（扫描循环）。

P215 不创建 real low-frequency scan（真实低频扫描）。

P215 不实现 real ScanScore computation（真实扫描分数计算）。

P215 不实现 Candidate Attention workflow（候选关注流程）。

P215 不实现 Promote To Home workflow（提升到首页观察流程）。

P215 不创建 Opportunity Push execution（机会推送执行）。

P215 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P215 不升级 readiness（可执行就绪）。

P215 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P215 不因为 DTO skeleton plan（数据对象骨架方案）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P216 已完成 Watchlist Runtime Source DTO Skeleton（观察库运行时数据源 DTO 骨架）。

P216 只是 pure DTO / enum / tests（纯数据对象 / 枚举 / 测试），不是 runtime source implementation（运行时数据源实现）。

P216 可以新增 `WatchlistRuntimeSourceDTO` Java、`WatchlistRuntimeSourceStatusEnum`、`WatchlistRuntimeSourceTypeEnum`、`WatchlistRuntimeFreshnessStatusEnum` 和 `WatchlistRuntimeSourceDTOTest`。

P216 不修改既有 Java。

P216 不修改既有 test。

P216 不修改 P208 DTO Java。

P216 不修改 P210 guard / validator Java。

P216 不修改 P212 assembler Java。

P216 不改 dashboard.html。

P216 不改 schema（数据库结构）。

P216 不改 config（配置）。

P216 不接 API（接口）。

P216 不接 mapper / service / controller / dashboard / runtime service wiring（映射器 / 服务 / 控制器 / 首页 / 运行时服务接线）。

P216 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P216 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P216 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P216 不创建 scan loop（扫描循环）。

P216 不创建 real low-frequency scan（真实低频扫描）。

P216 不实现 real ScanScore computation（真实扫描分数计算）。

P216 不实现 Candidate Attention workflow（候选关注流程）。

P216 不实现 Promote To Home workflow（提升到首页观察流程）。

P216 不创建 Opportunity Push execution（机会推送执行）。

P216 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P216 不升级 readiness（可执行就绪）。

P216 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P216 不因为 DTO skeleton（数据对象骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P217 已完成 Watchlist Runtime Source DTO Closure and Guard Boundary（观察库运行时数据源 DTO 收口与保护边界）。

P217 只是 docs-only closure / guard boundary（只改文档收口 / 保护边界），不是实现。

P217 记录 P216 已完成内容和边界。

P217 定义未来 runtime source guard / validator（运行时数据源保护器 / 校验器）的授权门。

P217 明确 runtime read（运行时读取）仍阻断。

P217 不写 Java。

P217 不新增测试。

P217 不修改 P216 DTO Java。

P217 不修改 P210 guard / validator Java。

P217 不修改 P212 assembler Java。

P217 不改 dashboard.html。

P217 不改 schema（数据库结构）。

P217 不改 config（配置）。

P217 不接 API（接口）。

P217 不接 mapper / service / controller / dashboard / runtime service wiring（映射器 / 服务 / 控制器 / 首页 / 运行时服务接线）。

P217 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P217 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P217 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P217 不创建 scan loop（扫描循环）。

P217 不创建 real low-frequency scan（真实低频扫描）。

P217 不实现 real ScanScore computation（真实扫描分数计算）。

P217 不实现 Candidate Attention workflow（候选关注流程）。

P217 不实现 Promote To Home workflow（提升到首页观察流程）。

P217 不创建 Opportunity Push execution（机会推送执行）。

P217 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P217 不升级 readiness（可执行就绪）。

P217 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P217 不因为文档收口而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P218 已完成 Watchlist Runtime Source Guard Validator Skeleton（观察库运行时数据源保护校验器骨架）。

P218 只是 pure guard / validator / tests（纯保护器 / 校验器 / 测试），不是 runtime source implementation（运行时数据源实现）。

P218 不修改 P216 DTO Java。

P218 不修改既有 Java / test / DTO / guard / assembler（既有 Java / 测试 / 数据对象 / 保护器 / 组装器）。

P218 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P218 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P218 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P218 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。

P218 不创建 scan loop（扫描循环）。

P218 不创建 real low-frequency scan（真实低频扫描）。

P218 不实现 real ScanScore computation（真实扫描分数计算）。

P218 不实现 Candidate Attention workflow（候选关注流程）。

P218 不实现 Promote To Home workflow（提升到首页观察流程）。

P218 不创建 Opportunity Push execution（机会推送执行）。

P218 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P218 不升级 readiness（可执行就绪）。

P218 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P218 不因为 guard skeleton（保护器骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P219 已完成 Watchlist Runtime Source Guard Closure and Wiring Gate（观察库运行时数据源保护器收口与接线授权门）。

P219 只是 docs-only closure / wiring gate（只改文档收口 / 接线授权门），不是实现。

P219 不写 Java。

P219 不新增测试。

P219 不修改 DTO / guard / validator / assembler（数据对象 / 保护器 / 校验器 / 组装器）。

P219 不改 dashboard.html。

P219 不改 schema（数据库结构）。

P219 不改 config（配置）。

P219 不接 API（接口）。

P219 不接 mapper / service / controller / dashboard / runtime service wiring（映射器 / 服务 / 控制器 / 首页 / 运行时服务接线）。

P219 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P219 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P219 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P219 不创建 scan loop（扫描循环）。

P219 不创建 real low-frequency scan（真实低频扫描）。

P219 不实现 real ScanScore computation（真实扫描分数计算）。

P219 不实现 Candidate Attention workflow（候选关注流程）。

P219 不实现 Promote To Home workflow（提升到首页观察流程）。

P219 不创建 Opportunity Push execution（机会推送执行）。

P219 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P219 不升级 readiness（可执行就绪）。

P219 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P219 不因为文档收口而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P220 已完成 Watchlist Runtime Source Test-Only Wiring Skeleton（观察库运行时数据源仅测试接线骨架）。

P220 只是 non-runtime test-only wiring / assembler / tests（非运行时、仅测试接线 / 组装器 / 测试），不是 runtime source implementation（运行时数据源实现）。

P220 不修改 P216 DTO Java。

P220 不修改 P218 guard / validator Java。

P220 不修改既有 Java / test / DTO / guard / assembler（既有 Java / 测试 / 数据对象 / 保护器 / 组装器）。

P220 不读取 DB / runtime / live / external data（数据库 / 运行时 / 实时 / 外部数据）。

P220 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P220 不接 scheduler（定时器），不改变 scheduler behavior（定时器行为）。

P220 不接 mapper / service / controller / API / dashboard（映射器 / 服务 / 控制器 / 接口 / 首页）。

P220 不创建 scan loop（扫描循环）。

P220 不创建 real low-frequency scan（真实低频扫描）。

P220 不实现 real ScanScore computation（真实扫描分数计算）。

P220 不实现 Candidate Attention workflow（候选关注流程）。

P220 不实现 Promote To Home workflow（提升到首页观察流程）。

P220 不创建 Opportunity Push execution（机会推送执行）。

P220 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P220 不升级 readiness（可执行就绪）。

P220 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P220 不因为 test-only wiring skeleton（仅测试接线骨架）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P221 已完成 Watchlist Runtime Source Test-Only Wiring Closure and Production Read Gate（观察库运行时数据源仅测试接线收口与生产读取授权门）。

P222 已完成 Watchlist Production Runtime Source Read Adapter Plan（观察库生产运行时数据源读取适配器方案）。

P223 已完成 Production Runtime Source Adapter Interface Skeleton Plan（生产运行时数据源适配器接口骨架方案）。

P224 已完成 Production Runtime Source Adapter Interface Skeleton（生产运行时数据源适配器接口骨架）。

P225 已完成 Production Runtime Source Adapter Interface Closure and Implementation Gate（生产运行时数据源适配器接口收口与实现授权门）。P226 已完成 Production Adapter Fail-Closed No-Op Implementation Plan（生产适配器失败关闭 no-op 实现方案）。P227 已完成 Production Adapter Fail-Closed No-Op Java Authorization Gate（生产适配器失败关闭 no-op Java 授权门）。P228 已完成 Production Adapter Fail-Closed No-Op Java Implementation（生产适配器失败关闭 no-op Java 实现）。P229 已完成 Production Adapter No-Op Closure and DB Watchlist Read Gate（生产适配器 no-op 收口与 DB 观察库池读取授权门）。P230 已完成 DB Watchlist Pool Read Plan and Mapper Schema Audit（DB 观察库池读取方案与 Mapper / Schema 审计）。P231 已完成 DB Watchlist Pool Read Audit Closure and Java Authorization Gate（DB 观察库池读取审计收口与 Java 授权门）。P232 已完成 DB Watchlist Pool Read Java Skeleton（DB 观察库池读取 Java 骨架）。P233 已完成 DB Watchlist Pool Read Closure and Runtime Source Service Gate（DB 观察库池读取收口与运行时数据源服务授权门）。P234 已完成 Runtime Source Service Plan and Assembler Gate（运行时数据源服务方案与组装器授权门）。P235 已完成 Runtime Source Service Java Skeleton（运行时数据源服务 Java 骨架）。P236 已完成 Runtime Source Service Closure and Watchlist Scan Result Assembly Gate（运行时数据源服务收口与观察库扫描结果组装授权门）。P237 已完成 Watchlist Scan Result Assembly Plan and DTO Usage Audit（观察库扫描结果组装方案与 DTO 使用审计）。P238 已完成 Watchlist Scan Result Assembly Java Authorization Gate（观察库扫描结果组装 Java 授权门）。P239 已完成 Watchlist Scan Result Assembly Java Skeleton（观察库扫描结果组装 Java 骨架）。P240 已完成 Watchlist Scan Result Assembly Closure and Low-Frequency Scan Orchestrator Gate（观察库扫描结果组装收口与低频扫描编排器授权门）。P241 已完成 Low-Frequency Scan Orchestrator Plan and Scan Loop Boundary Audit（低频扫描编排器方案与扫描循环边界审计）。P242 已完成 Low-Frequency Watchlist Scan Orchestrator Java Skeleton（低频观察库扫描编排器 Java 骨架）。P243 已完成 Low-Frequency Scan Orchestrator Closure and Scheduler Batch Market Gate（低频扫描编排器收口与 Scheduler / Batch / Market 授权门）。P244 已完成 Scheduler Batch Market-Read Scope Audit（Scheduler / Batch / Market-read 范围审计）。P245 已完成 Scheduler Trigger Authorization Gate（定时器触发授权门）。P246 已完成 Disabled Scheduler Wiring Skeleton（默认关闭定时器接线骨架）。P247 已完成 Disabled Scheduler Wiring Closure and Batch Scan Authorization Gate（默认关闭定时器接线收口与批量扫描授权门）。P248 已完成 Batch Scan Java Authorization Gate and Batch Envelope Plan（批量扫描 Java 授权门与批量结果信封方案）。P249 已完成 Batch Envelope DTO Authorization Gate and Batch Java Skeleton Gate（批量结果信封 DTO 授权门与批量 Java 骨架授权门）。P250 已完成 Batch Watchlist Scan Java Skeleton（批量观察库扫描 Java 骨架）。P251 已完成 Market Score Candidate Push Readiness Scope Pack（Market-read、ScanScore、Candidate、Push、Readiness 范围包）。P252 已完成 Market Read Adapter Skeleton（行情读取适配器骨架）。P253 已完成 ScanScore DTO and Rule Skeleton（扫描分数 DTO 与规则骨架）。P254 已完成 ScanScore Calculation Review-Only Skeleton（扫描分数计算只允许复核骨架）。P255 当前推进 Candidate Attention and Promote To Home Gate Pack（候选关注与提升到首页授权门包）。

P249 只是 docs-only DTO / Java skeleton authorization gate，不是实现；P248 只是 docs-only batch Java authorization gate / batch envelope plan，不是 batch implementation；P247 只是 docs-only closure / batch scan authorization gate，不是 batch implementation；P246 只是 disabled scheduler wiring skeleton，不是 scheduler activation；P245 只是 docs-only scheduler trigger authorization gate，不是 scheduler implementation；P244 只是 docs-only scope audit / layered gate plan，不是实现；P243 只是 docs-only closure / Scheduler-Batch-Market authorization gate，不是实现；P242 只是最小 disabled-by-default single-symbol orchestrator skeleton，不是真实扫描。

`RuleConfigWatchlistPoolReadAdapter` Java 已存在。

DB-backed Watchlist Pool read skeleton（DB 观察库池读取骨架）已存在。

`WatchlistRuntimeSourceService` / `DefaultWatchlistRuntimeSourceService` 已存在，但 production Runtime Source Service wiring（生产运行时数据源服务接线）仍未实现。

Production Source Assembler（生产数据源组装器）仍未实现。

`WatchlistRuntimeSourceService` / `DefaultWatchlistRuntimeSourceService` 已存在。

`WatchlistScanResultDTO` 字段足够支持最小 review-only skeleton。

Watchlist Scan Result Assembly skeleton 已存在，WatchlistLowFrequencyScanScheduler disabled-by-default skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在。

Low-Frequency Watchlist Scan Orchestrator skeleton 已存在，WatchlistLowFrequencyScanScheduler disabled-by-default skeleton 已存在，DisabledLowFrequencyScanSchedulerWiring 已存在。

Scheduler-triggered orchestrator（定时器触发编排器）仍未实现；P246 只提供 disabled wiring skeleton。

batch scan（批量扫描）仍未实现。

batch orchestrator（批量扫描编排器）仍未实现。

real scan loop（真实扫描循环）仍未实现。

P249 只做 docs-only batch envelope DTO authorization gate / batch Java skeleton authorization gate（只改文档批量结果信封 DTO 授权门 / 批量 Java 骨架授权门），不是实现。

P249 不写 Java。

P249 不新增测试。

P249 不修改既有 Java / test。

P249 不修改 DTO 文件。

P249 不修改 guard / validator 文件。

P249 不修改 assembler 文件。

P249 不改 dashboard.html。

P249 不改 schema（数据库结构）。

P249 不改 config（配置）。

P249 不接 API（接口）。

P249 不改 mapper / controller / dashboard / production runtime service wiring（映射器 / 控制器 / 首页 / 生产运行时服务接线）。

P249 不读取 runtime / live / external data（运行时 / 实时 / 外部数据）。

P249 不接 MarketQuoteClient / BinanceMarketQuoteClient（行情客户端）。

P249 不启用 scheduler（定时器），不创建 scheduler activation（定时器激活）。

P249 不接 batch implementation（批量实现）。

P249 不创建 `BatchWatchlistScanOrchestrator`。

P249 不创建 `BatchWatchlistScanResultEnvelopeDTO`。

P249 不创建 batch scan Java（批量扫描 Java）。

P249 不创建 batch result envelope Java（批量结果信封 Java）。

P249 不创建 batch orchestrator（批量扫描编排器）。

P249 不创建真实 scan loop（扫描循环）。

P249 不创建 real low-frequency scan（真实低频扫描）。

P249 不实现 Production Source Assembler（生产数据源组装器）。

P249 不实现 real ScanScore computation（真实扫描分数计算）。

P249 不实现 Candidate Attention workflow（候选关注流程）。

P249 不实现 Promote To Home workflow（提升到首页观察流程）。

P249 不创建 Opportunity Push execution（机会推送执行）。

P249 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。

P249 不升级 readiness（可执行就绪）。

P249 不接 order API / execution API / auto-trading（下单接口 / 执行接口 / 自动交易）。order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

P249 不因为 batch envelope DTO authorization gate / batch Java skeleton authorization gate（批量结果信封 DTO 授权门 / 批量 Java 骨架授权门）而大幅上调真实生产接线进度；真实生产接线仍保持 26%-34%，真实点位仍保持 10%-18%。

P250 后仍未完成 / 未实现 / 未升级清单：

- runtime source contract（运行时数据源契约）已文档化。
- `WatchlistRuntimeSourceDTO` Java 已存在，runtime source guard / validator（运行时数据源保护器 / 校验器）已存在，runtime source test-only wiring（运行时数据源仅测试接线）已存在，production read adapter plan（生产读取适配器方案）已存在，production adapter interface skeleton（生产适配器接口骨架）已存在，production fail-closed no-op implementation plan（生产失败关闭 no-op 实现方案）已存在，P227 Java authorization gate（Java 授权门）已存在，P228 fail-closed no-op adapter（失败关闭 no-op 适配器）已存在，但 runtime source implementation（运行时数据源实现）仍未实现。
- production adapter interface skeleton（生产适配器接口骨架）已存在，但只是接口 / DTO / 测试骨架，不是 production read implementation。
- production fail-closed no-op Java（生产失败关闭 no-op Java）已存在，但只是 fail-closed no-op adapter（失败关闭 no-op 适配器），不是 production read implementation。
- `RuleConfigService` / `RuleConfigMapper` / `tm_rule_config` 已存在。
- `RuleConfigWatchlistPoolReadAdapter` 可存在，但只是最小 DB Watchlist Pool config read skeleton（DB 观察库池配置读取骨架）。
- Runtime Source Service skeleton（运行时数据源服务骨架）已存在：`WatchlistRuntimeSourceService` / `DefaultWatchlistRuntimeSourceService` 已存在。
- Watchlist Scan Result Assembly skeleton（观察库扫描结果组装骨架）已存在。
- Low-Frequency Watchlist Scan Orchestrator skeleton（低频观察库扫描编排器骨架）已存在，但 production wiring（生产接线）和 real scan loop（真实扫描循环）仍未实现。
- scheduler-triggered orchestrator（定时器触发编排器）仍未实现；P246 只提供 disabled wiring skeleton。
- scheduler-triggered batch（定时器触发批量扫描）仍未实现。
- `BatchWatchlistScanOrchestrator` 可存在，但只是最小 batch skeleton（批量骨架），不是 scheduler-triggered batch（定时器触发批量扫描）或真实扫描。
- `BatchWatchlistScanResultEnvelopeDTO` 可存在，但只是安全 envelope DTO（结果信封数据对象），不承载真实 ScanScore / Candidate / Push / Readiness / trading action。
- batch scan Java（批量扫描 Java）进入最小 skeleton 阶段，但不是真实扫描。
- Batch result envelope Java（批量结果信封 Java）进入最小 DTO 阶段，但不是生产批量扫描结果。
- batch scan（批量扫描）仍未实现为真实扫描。
- batch orchestrator（批量扫描编排器）仍未实现为生产编排器。
- real scan loop（真实扫描循环）仍未实现。
- Production Source Assembler（生产数据源组装器）仍未实现。
- `push.watchlist.symbols` runtime config（运行时配置）如果不存在或为空，仍必须 fail-closed（失败关闭）。
- `NoOpWatchlistPoolRuntimeSourceReadAdapter` Java 仍未实现，且 P228 选择 `DefaultWatchlistPoolRuntimeSourceReadAdapter`，不同时实现两个 no-op adapter。
- production adapter implementation（生产适配器实现）仍未实现。
- production read implementation（生产读取实现）仍未实现。
- production runtime source read（生产运行时数据源读取）仍未实现。
- production runtime source wiring（生产运行时数据源接线）仍未实现。
- DB-backed watchlist read（数据库观察库读取）进入最小 skeleton 阶段，但 production read implementation（生产读取实现）仍未完成。
- MarketQuoteClient integration（行情客户端接入）仍未实现。
- scheduler activation（定时器激活）/ scheduler behavior changes（定时器行为改变）仍未实现。
- scan loop（扫描循环）仍未实现。
- production ScanScore computation（生产级扫描分数计算）仍未实现。
- Candidate Attention workflow（候选关注流程）仍未实现。
- Promote To Home workflow（提升到首页观察流程）仍未实现。
- Opportunity Push execution（机会推送执行）仍未实现。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）仍未实现。
- readiness（可执行就绪）仍未升级。
- order API（下单接口）/ execution API（执行接口）/ auto-trading（自动交易）不在 V1 范围内，保持关闭。

后续继续推进必须以 `docs/PROJECT_PROGRESS_INDEX.md` 为准。任何后续阶段如果想打开 SourceTrace（证据来源追踪）、BoundaryCandidate（边界候选交易计划）、ExecutionPlan（执行计划）、Dashboard（首页工作台）、Risk Action Guard（风险动作保护器）、Position Monitor（持仓监控）、Watchlist（观察库）、Opportunity Promote（机会提升）或 Push（推送）的新能力，都必须先对照本索引确认它属于“已完成”“正在推进”“暂停”“后期必须回来做”还是“禁止提前做”。auto-trading（自动交易）不在 V1 范围内，保持关闭，不作为后续进度路线。
