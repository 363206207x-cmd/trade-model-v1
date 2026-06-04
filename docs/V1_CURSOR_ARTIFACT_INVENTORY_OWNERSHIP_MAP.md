# V1 Cursor Artifact Inventory + Ownership Map

本文件是 #830 审计和 `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md` 之后的第一份 ownership map。

它只做只读盘点和归属决策，不新增 Java，不修改测试，不接 service/runtime/dashboard/API，不继续 P359，不启动 P360。

## 1. Executive Summary

Cursor-before-P1 阶段留下的可盘活资产不是零散草稿，而是一批已经接近 runtime 的 Spring / MyBatis / dashboard / schema / provider 资产：

- Dashboard surface: `DashboardController`、`dashboard.html`、dashboard detail/summary API、display adapters。
- Runtime data/service: `DecisionServiceImpl`、`PositionSyncService`、`RealMarketEnvironmentService`、`EvidenceServiceImpl`、`ScoreServiceImpl`、`RuleConfigServiceImpl`。
- Provider/schema: `BinanceMarketQuoteClient`、`BinancePositionProvider`、`SwitchablePositionProvider`、`schema.sql` 中的 decision/evidence/score/execution/position/push/review/rule tables。
- Review surfaces: `ReviewServiceImpl`、`MissedOpportunityServiceImpl`、`PushRecheckController`、RiskActionGuard dashboard adapter。

接近用户可用的资产主要是 Dashboard + Decision summary/detail、PositionSync + dashboard readonly fields、MarketQuote + market environment、Evidence/Score read model、RuleConfig watchlist read。它们有 controller/service/mapper/schema/provider 或 dashboard 接口，比 Codex 后续 DTO/Validator/Assembler skeleton 更接近 `REVIEW_ONLY_RUNTIME`。

Partial 的资产包括 Watchlist low frequency scan、Push preview/recheck、BoundaryCandidate/ExecutionPlan、RiskActionGuard、Position Monitor。它们已有安全边界或显示面，但生产 runtime 串联不完整，不能直接作为可执行建议。

Codex skeleton 的处理原则：冻结默认扩张，保留其 fail-closed / review-only / notTradeInstruction / manualReviewRequired 规则，后续只能作为 safety adapter 或 merge input，不能再当 canonical runtime owner。P359 不应继续，P360 不应启动。

本任务不提升业务 capability level。它降低重复风险，为下一步 `Runtime Wiring Target Selection Plan` 服务。首选最小 runtime wiring 目标是：

**B. PositionSync + Dashboard review-only status**

理由很硬：它已有 provider、fallback、service、mapper、scheduler、schema、dashboard summary/detail 字段，且不需要生成 candidate、point、push 或 final direction。它比 P359 更接近可见、可测、可止损的 `REVIEW_ONLY_RUNTIME`。

## 2. Ownership Map Method

本次 owner 判断不按日期，不按包数，不按聊天记忆。只按 merged main 文件和只读命令结果判断。

一个 canonical owner 优先满足：

- 已存在于 Cursor-era 或 merged main。
- 有 service / controller / mapper / schema / dashboard / provider 连接。
- 能接真实输入或已有 runtime 数据。
- 能进入 dashboard/API。
- 能被 fail-closed / review-only 规则包裹。
- 比新 DTO skeleton 更接近用户可用。

如果只有 DTO / Validator / Assembler，而没有 service/runtime/dashboard/API，则默认不是 canonical owner，只能作为 safety adapter 候选。

## 3. Cursor Artifact Inventory

| capability area | candidate owner files/classes | evidence from source | current usability | connected to service/runtime | connected to dashboard/API | safety gaps | duplicate Codex skeletons | recommended owner decision |
|---|---|---|---|---|---|---|---|---|
| Dashboard homepage | `src/main/java/org/example/trademodel/controller/DashboardController.java`, `src/main/resources/templates/dashboard.html` | `/dashboard` returns `dashboard`; summary/detail APIs exist; template renders decision, sourceTrace, runtimeKline, execution, risk guard, position shell text. | partial | yes | yes | Runtime slices are uneven; some cards still show backend pending / placeholders. | P304 dashboard push display gate; P310 point display gate; many display DTOs. | keep as owner |
| Dashboard APIs | `DashboardController`, `DashboardSummaryResponseVO`, `DashboardDetailResponseVO` | `/api/dashboard/summary`, `/api/dashboard/detail`, `/api/dashboard/refresh` aggregate Decision, Evidence, Score, market env, RiskActionGuard display. | partial | yes | yes | Need canonical field ownership and no misleading point/candidate claims. | P304, P310, P351-P358 runtime candidate plans. | keep as owner |
| dashboard.html | `src/main/resources/templates/dashboard.html` | Template contains runtimeKline, sourceTrace, riskActionGuard, executionPlan, position, internal push preview, no-order warnings. | partial | partial | yes | It displays many placeholders and can mask backend incompleteness. | Multiple Codex display skeletons. | keep as owner |
| Display Slots | `DashboardController`, `dashboard.html`, `DecisionServiceImpl` summary rows | Dashboard can show default/summary assets, but display slots do not prove watchlist membership. | partial | partial | yes | Must not be treated as watchlist pool proof. | WatchlistPoolProof source binding family. | keep as owner for UI only |
| Watchlist Pool / RuleConfig | `RuleConfigServiceImpl`, `RuleConfigMapper`, `RuleController`, `RuleConfigWatchlistPoolReadAdapter`, `schema.sql: tm_rule_config` | Adapter reads `push.watchlist.symbols`; RuleController reloads rules; schema has `tm_rule_config`. | partial | yes | API partial | Need runtime ownership and proof freshness; controller only reloads. | P342 WatchlistPoolProof; P343-P358 candidate source/runtime chains. | keep RuleConfig adapter as owner; merge Codex proof rules into owner |
| MarketQuoteClient | `MarketQuoteClient`, `BinanceMarketQuoteClient`, `RealMarketEnvironmentService`, `DecisionServiceImpl` | Binance public 24h ticker client exists; DecisionService fetches latest price; market env maps quote to review fields. | partial | yes | yes through dashboard detail/summary | Live provider failure fallback must stay fail-closed; not authorized as new scan-chain proof by itself. | MarketRead DTO/guard/output skeletons P287-P294. | keep as owner for quote read; Codex marketread wrappers freeze |
| BinanceMarketQuoteClient | `market/client/impl/BinanceMarketQuoteClient.java` | `@Service`, Java `HttpClient`, Binance `/api/v3/ticker/24hr`, parses `lastPrice`, `priceChangePercent`. | partial | yes | yes via services | External dependency can fail; no exchange fallback found in this read. | MarketRead adapter/request skeletons. | keep as owner |
| OKX fallback / market provider if present | no confirmed OKX class in required grep output | Required grep found Binance clients, no concrete OKX owner surfaced. | unknown | unknown | unknown | Need targeted source read before claiming. | Market read fallback docs/skeletons may imply provider abstraction. | UNKNOWN - requires targeted source read |
| PositionSyncService | `PositionSyncService`, `PositionSyncScheduler`, `RealPositionMapper`, `PositionProvider`, `SwitchablePositionProvider`, `BinancePositionProvider`, `SimulatedPositionProvider`, `schema.sql: tm_real_position` | Sync fetches positions, upserts/ closes missing rows, exposes sync status; scheduler runs; provider defaults simulated and falls back. | partial, near usable | yes | partial via DecisionService/dashboard fields | Needs explicit review-only dashboard status and no trade-action interpretation. | Position monitor / RiskActionGuard / source-owned runtime skeletons overlap conceptually. | keep as owner |
| Manual position / simulated position | `SimulatedPositionProvider`, `SwitchablePositionProvider`, `PositionProviderResult`, `PositionSnapshot` | Provider type defaults to SIMULATED; Binance credentials missing falls back to simulated. | partial | yes | partial | Simulated state must be labeled clearly; not account proof. | Future Position Monitor plans would overlap. | keep as owner |
| Position monitor / monitor advice | `MonitorController`, `MonitorServiceImpl`, `MonitorAlertMapper`, `schema.sql: tm_monitor_alert` | Monitor alerts can be read; `/api/monitor/status` is a thin status endpoint. | partial | partial | partial | Advice/action layer not fully wired; avoid expansion now. | P341 RiskActionGuard source binding; future position monitor plans. | freeze expansion; keep alert read owner |
| EvidenceService | `EvidenceServiceImpl`, `EvidenceController`, `EvidenceItemMapper`, `schema.sql: tm_evidence_item` | Builds evidence from market environment, hot reset, funding/OI, price structure; controller can build. | partial | yes | yes through dashboard top evidence and `/api/evidence/build` | Build API may be request-driven; persisted/runtime chain ownership needs selection. | P295-P296 evidence skeletons. | keep as owner |
| ScoreService | `ScoreServiceImpl`, `ScoreController`, `ScoreItemMapper`, `schema.sql: tm_score_item` | Builds trend/credibility/funding/leverage/liquidity/sentiment/macro/event scores; controller exists. | partial | yes | yes through dashboard top scores and `/api/score/*` | Some scores are heuristic; must remain review-only. | P297-P298 score skeletons. | keep as owner |
| DecisionService / DecisionResult | `DecisionServiceImpl`, `DecisionResultMapper`, `DecisionResultVO`, `schema.sql: tm_decision_result` | Dashboard summary/detail reads latest decisions, latest quote, open position, push/missed state. | partial, near usable | yes | yes | Must distinguish displayed decision from final direction/trade instruction. | Candidate/runtime/point skeletons. | keep as owner |
| ExecutionPlan | `PlanServiceImpl`, `ExecutionPlanDO`, `ExecutionPlanMapper`, `DefaultExecutionPlanDisplayAdapter`, `ExecutionPlanVO`, `schema.sql: tm_execution_plan` | Plan defaults advisory, sets manual review and not-trade flags, applies source trace and risk guard readiness. | partial | yes | yes through dashboard display adapter | It has entry/stop/TP field names and must not be revived as executable plan. | P307-P325 numeric point/readiness skeletons. | keep as review-only owner; freeze executable use |
| BoundaryCandidate | `BoundaryCandidateServiceImpl`, `BoundaryCandidateDTO`, `DefaultPlanBoundaryDisplayAdapter` | Service validates source trace, entry, stop, TP, dataQuality, risk guard and returns fallback if incomplete. | partial | yes | yes through dashboard adapters | Touches numeric boundary fields; high misuse risk before ownership plan. | Numeric Point / SourceTrace / RuntimeKline skeleton chains. | keep as owner after targeted merge map, not first wiring target |
| Review / missed opportunity | `ReviewServiceImpl`, `ReviewController`, `ReviewPageController`, `MissedOpportunityServiceImpl`, `MissedOpportunityController`, `schema.sql: tm_review_result`, `tm_missed_opportunity`, `tm_rule_version_log` | Review save/update writes audit log; missed opportunity records worth-opening/no-position cases. | partial | yes | yes/API | Needs usability flow validation; do not auto-correct rules. | Review-only closure docs and future monitor plans. | keep as owner |
| RiskActionGuard | `DefaultRiskActionGuardDisplayAdapter`, `PlanServiceImpl`, `BoundaryCandidateServiceImpl`, dashboard template | Fail-closed display adapter disables push/reverse/new position/market exit and forces manual review. | partial | partial | yes | No full runtime guard context owner; some fields are display-derived. | P317 numeric plan; P341 source binding; P358 runtime assembly plan. | keep dashboard adapter as current owner; merge Codex rules later |
| AI decision / multi-agent if present | `AiConflictResolverServiceImpl`, `AiConflictResolverService`, `DecisionContext`, `AiRoleEnum` | Heuristic conflict resolver exists; no external GPT/Gemini/Grok provider orchestration in required read. | partial | yes, heuristic only | partial via decision fields | Three-AI runtime would duplicate and expand risk; freeze. | Future Three AI planning would overlap. | freeze expansion; keep heuristic owner |
| schema.sql tables | `src/main/resources/schema.sql` | Tables for analysis, evidence, score, decision, execution plan, market env, OHLCV, rule config, real position, push, account risk, monitor, missed, review, asset state, hot reset. | partial | yes | yes via mappers/controllers | Schema breadth is large; ownership must be chosen before adding objects. | Codex skeleton families often ignore existing tables. | keep schema as ownership anchor |
| mapper / repository layer | `mapper/*Mapper.java` | MyBatis mappers exist for evidence, score, decision, execution, real position, rule config, push, review, monitor, market env, OHLCV. | partial | yes | yes via services/controllers | Need targeted owner map per capability before wiring. | Codex DTO-only families bypass mappers. | keep as owner anchors |

## 4. Codex Skeleton Families To Reconcile

| skeleton family | likely Cursor owner | overlap type | recommended action | freeze | wire later |
|---|---|---|---|---|---|
| MarketRead review-only chain | `MarketQuoteClient`, `BinanceMarketQuoteClient`, `RealMarketEnvironmentService`, `DecisionServiceImpl` | safety wrapper | Freeze new wrappers; merge only if it gates existing quote read. | Yes | Yes, only as guard around existing quote owner |
| Evidence / Score review-only chain | `EvidenceServiceImpl`, `ScoreServiceImpl`, `EvidenceController`, `ScoreController` | duplicate/safety wrapper | Keep services as canonical; use Codex rules as test/safety adapters. | Yes | Yes, through existing services |
| Candidate / Push review-only chain | `PushRecheckController`, `PushRecheckService`, `PushSnapshotMapper`, dashboard preview sections | adapter candidate | Freeze external/channel wrappers; only internal review preview may be merged. | Yes | Later, after watchlist owner is selected |
| Readiness / Point skeleton chain | `PlanServiceImpl`, `BoundaryCandidateServiceImpl`, dashboard plan/risk adapters | duplicate/safety wrapper | Freeze. Do not add point wrappers. Merge into existing plan/boundary only after owner map. | Yes | Later, not first |
| Numeric Point Proposal chain | `BoundaryCandidateServiceImpl`, `PlanServiceImpl`, `ExecutionPlanDisplayAdapter` | duplicate/high-risk wrapper | Freeze until source/runtime ownership is proven. | Yes | Maybe, after review-only runtime exists |
| SourceTrace Numeric Source chain | `SourceTraceDTO`, SourceTrace ownership services, dashboard source trace adapter | safety wrapper | Keep as adapter candidate; do not add new source trace objects. | Yes | Later |
| RuntimeKlineContext source binding | `RuntimeKlineContextAssemblyServiceImpl`, `PersistedOhlcvQueryServiceImpl`, dashboard runtime kline adapter | duplicate/safety wrapper | Merge only into existing runtime kline service if needed. | Yes | Later |
| DataQuality source binding | `ScoreServiceImpl`, `EvidenceServiceImpl`, market environment and data-quality score fields | unclear adapter | Freeze; do not treat as owner until service mapping is written. | Yes | Later |
| MultiTimeframe source binding | `DecisionEngineService`, `DecisionContext`, existing multi-timeframe flags | unclear adapter | Freeze; source-read needed before owner decision. | Yes | Later |
| RiskActionGuard source binding | `DefaultRiskActionGuardDisplayAdapter`, `PlanServiceImpl`, `BoundaryCandidateServiceImpl` | safer replacement / duplicate | Keep Codex rules as canonical safety text; owner remains dashboard/plan guard. | Yes | Yes |
| WatchlistPoolProof source binding | `RuleConfigWatchlistPoolReadAdapter`, `RuleConfigServiceImpl`, `WatchlistRuntimeSourceService` | safety wrapper | Merge proof rules into RuleConfig watchlist owner; freeze new proof DTOs. | Yes | Yes |
| Source-Owned Candidate Integration source binding | `BoundaryCandidateServiceImpl`, `DecisionServiceImpl`, existing candidate/push services | duplicate wrapper | Freeze. It is not owner until it wires existing service/dashboard. | Yes | Only if it reduces duplication |
| Source-Owned Candidate Integration runtime DTO / Validator / Assembler plan | no Cursor runtime owner proven; likely overlaps Point/Boundary/Decision | duplicate wrapper | Do not continue P359/P360. Re-evaluate after wiring target plan. | Yes | No by default |

## 5. Canonical Owner Decisions

| Capability | Canonical Owner | Codex Skeleton Role | Decision |
|---|---|---|---|
| Dashboard surface | `DashboardController` + `dashboard.html` + dashboard display adapters | Safety labels and display contracts only | Keep Cursor owner |
| Market quote | `MarketQuoteClient` + `BinanceMarketQuoteClient` + `RealMarketEnvironmentService` | Guard/adapter candidate around existing quote read | Keep Cursor owner |
| Watchlist membership | `RuleConfigWatchlistPoolReadAdapter` + `RuleConfigServiceImpl` + `tm_rule_config` | WatchlistPoolProof rules should merge into this owner | Keep Cursor owner, merge Codex proof rules |
| Risk guard | `DefaultRiskActionGuardDisplayAdapter` + `PlanServiceImpl` | Codex RiskActionGuard rules are safety constraints | Keep Cursor owner, merge rules |
| Evidence / Score | `EvidenceServiceImpl` + `ScoreServiceImpl` + mappers/controllers | Codex evidence/score wrappers are adapters/tests | Keep Cursor owner |
| Decision Result | `DecisionServiceImpl` + `DecisionResultMapper` + dashboard APIs | Codex candidate/runtime wrappers must not replace it | Keep Cursor owner |
| Execution Plan | `PlanServiceImpl` + `ExecutionPlanDisplayAdapter` + `ExecutionPlanMapper` | Codex point/readiness rules as guard text only | Keep review-only Cursor owner |
| Point Candidate | `BoundaryCandidateServiceImpl` for existing boundary review; no executable owner | Codex numeric point skeletons frozen as safety adapters | Keep Cursor boundary owner; freeze numeric expansion |
| Runtime Candidate | UNKNOWN - requires targeted source read | Codex source-owned runtime skeletons are not canonical | Freeze P359/P360 |
| Position Sync | `PositionSyncService` + `PositionSyncScheduler` + `PositionProvider` + `RealPositionMapper` | Codex safety rules can wrap display/status only | Keep Cursor owner |
| Position Monitor | `MonitorServiceImpl` + `MonitorAlertMapper` + dashboard alert display | Future monitor skeletons frozen | Keep alert owner; freeze expansion |
| Push Preview | `PushRecheckController` + `PushRecheckService` + `PushSnapshotMapper` + dashboard internal preview | Codex push preview wrappers are safety constraints | Keep internal owner; freeze external channel |

## 6. What To Freeze

Freeze immediately:

- P359 / P360.
- new DTO.
- new Validator.
- new Assembler.
- new docs-only plan.
- Three AI.
- Position Monitor expansion.
- Dashboard expansion.
- Push external channel.
- executable point generation.
- new source binding family.
- new candidate family.
- new point family.
- order / execution / auto-trading.

解冻条件：

- 先完成 `Runtime Wiring Target Selection Plan`。
- 明确 canonical owner。
- 证明会复用 Cursor-era service/runtime/dashboard/API。
- 证明会减少重复对象。
- 证明目标 capability level 真实朝 `REVIEW_ONLY_RUNTIME` 前进。
- 证明不新增普通 skeleton/wrapper。

## 7. What To Keep

应该保留：

- Cursor runtime/dashboard/service/schema/provider assets。
- `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md`。
- Codex fail-closed / review-only / blocked capability rules。
- Codex targeted tests where useful。
- Codex skeletons only as safety adapters, not canonical runtime owners unless proven。

尤其应保留并优先盘活：

- `PositionSyncService` / `PositionSyncScheduler` / `PositionProvider` / `RealPositionMapper` / `tm_real_position`。
- `DashboardController` / `dashboard.html` / `DashboardSummaryResponseVO` / `DashboardDetailResponseVO`。
- `MarketQuoteClient` / `BinanceMarketQuoteClient` / `RealMarketEnvironmentService`。
- `RuleConfigWatchlistPoolReadAdapter` / `RuleConfigServiceImpl` / `tm_rule_config`。
- `EvidenceServiceImpl` / `ScoreServiceImpl` / corresponding mappers。

## 8. First Minimal Runtime Wiring Candidate

首选：

**B. PositionSync + Dashboard review-only status**

为什么最小：

- 已有 `PositionSyncService`，不是新 skeleton。
- 已有 `PositionSyncScheduler`，有 runtime loop。
- 已有 `SwitchablePositionProvider`，默认 SIMULATED，Binance credential missing 时 fallback。
- 已有 `BinancePositionProvider`，可读真实 futures position risk，但缺凭证时 fail-safe fallback。
- 已有 `RealPositionMapper` 和 `tm_real_position`。
- 已有 dashboard summary/detail 中的 position fields。
- 不需要生成候选、点位、final direction、push payload 或交易动作。

复用 Cursor 资产：

- `PositionSyncService`
- `PositionSyncScheduler`
- `PositionProvider`
- `SwitchablePositionProvider`
- `BinancePositionProvider`
- `SimulatedPositionProvider`
- `RealPositionMapper`
- `DashboardController`
- `dashboard.html`

复用 Codex 安全规则：

- review-only。
- notTradeInstruction。
- manualReviewRequired。
- incomplete-safe。
- fail-closed / fallback reason visible。
- no order / execution / auto-trading。

是否接近 `REVIEW_ONLY_RUNTIME`：

- 是，最接近。这个目标只需要把已有 runtime 状态在 dashboard/API 中明确成 review-only status，而不是开新对象族。

为什么不是 P359：

- P359 只会继续 Source-Owned Candidate Integration Runtime Assembler skeleton。它不接 service，不接 dashboard，不接 provider，不解决 runtime wiring gap。

为什么不是三 AI / 持仓监控 / 点位：

- 三 AI 会新增 provider/orchestration 风险。
- 持仓监控 expansion 会引入动作建议和风险动作语义。
- 点位会碰 entry / stop / TP / RR，误导风险更高。
- PositionSync status 是已有 runtime read model，最小、最稳、最能止损。

备选第二目标：

**D. RuleConfig Watchlist + WatchlistPoolProof merge**

它也减少重复，但需要先确认 WatchlistRuntimeSourceService 到 dashboard/API 的最小可见路径，因此放在 PositionSync 之后。

## 9. Next 5 Actions

| action name | type | creates new skeleton | reuses Cursor-era asset | reduces duplication | raises capability level | connects service/runtime/dashboard/API | risk level |
|---|---|---|---|---|---|---|---|
| Runtime Wiring Target Selection Plan | wiring-plan | No | Yes | Yes | prepares | planned | A |
| PositionSync/Dashboard Source Read Verification | audit | No | Yes | Yes | prepares | No | A |
| Cursor-to-Codex Safety Adapter Merge Map for PositionSync | ownership | No | Yes | Yes | prepares | planned | A/B |
| Minimal Review-Only Position Runtime Wiring Design | wiring-plan | No | Yes | Yes | prepares | planned | B |
| Single-Slice Implementation Readiness Gate | audit | No | Yes | Yes | prepares | planned | B |

下一步必须是 `Runtime Wiring Target Selection Plan`，除非该计划前置的更小 source read 发现 PositionSync/Dashboard 不成立。

## 10. Final Recommendation

当前最该盘活的是 `PositionSyncService + Dashboard` 这条已有 runtime/status/display 资产；当前最该冻结的是 P359/P360 和所有新 DTO/Validator/Assembler/plan；下一步必须从“包数推进”改成“选一个已有 owner，把 capability 推向 REVIEW_ONLY_RUNTIME”。这符合 #830 审计和 freeze rule。
