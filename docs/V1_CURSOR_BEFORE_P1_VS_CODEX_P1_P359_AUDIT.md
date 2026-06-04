# V1 Cursor-before-P1 vs Codex-P1-to-P359 Global Usability / Duplication / Continuity Audit

任务名：V1 Cursor-before-P1 vs Codex-P1-to-P359 Global Usability / Duplication / Continuity Audit

审计结论基于当前仓库文件、merged main、git log、状态文档和只读源码扫描。边界按用户指定的 P 编号划分：Cursor 阶段是 P1 之前，Codex 阶段是 P1 到当前 P359。日期不是主边界，2026-05-25 不是主边界。

## 1. Executive Summary

| question | direct answer |
|---|---|
| Cursor 阶段是否产生过用户可用内容 | 是。Cursor-before-P1 已经留下 dashboard、DashboardController、manual position / position sync、MarketQuoteClient / Binance market quote、score / evidence / decision / execution plan、RuleConfig / Push Watchlist / Display Slots 等 runtime 或 dashboard 可触达资产。不是全都生产级，但不是纯文档。 |
| Codex P1-P359 是否主要在做骨架和安全边界 | 是，尤其 P287 之后更明显。大量工作是 docs-only plan、DTO、Validator、Assembler、Verification，主线文本反复声明“不接 service / runtime / dashboard / push / order”。 |
| Codex P1-P359 是否重复造了很多 DTO / Validator / Assembler | 是。SourceTrace、RuntimeKlineContext、DataQuality、MultiTimeframe、RiskActionGuard、WatchlistPoolProof、Source-Owned Candidate Integration、Runtime Candidate 等链条都走了 plan -> DTO -> Validator -> Assembler -> verification 的模式，其中不少与 Cursor 已有 dashboard/service/read-model 概念重叠。 |
| 当前项目有没有进入“包数推进但业务能力不提升”的状态 | 有。P320-P358 以后尤其明显：包数在增长，安全边界越来越厚，但没有把能力接到真实输入、service、dashboard 或用户可用 review-only runtime。 |
| 是否建议暂停 P359 | 建议暂停。当前 main 只到 P358，P359 在本地/远端分支上不应视为完成。继续 P359/P360 只能闭合又一层 DTO/Validator/Assembler 骨架，不能解决 runtime wiring gap。 |
| “两个月没有产生一个可用模块”的判断 | 部分成立，偏成立。不是完全白做，因为留下了安全契约、fail-closed 语义和测试资产；但按“用户可用模块”标准，近两个月主产出大多不是可用模块，而是安全骨架和文档闭环。 |

一句话：项目不是没有资产，但已经明显滑入“先把边界写完、再写下一层边界”的无底洞；最大问题不是没写代码，而是新骨架没有合并进 Cursor 已经存在的 runtime/dashboard/service 资产。

## 2. Audit Boundary / Methodology

### Boundary

- Cursor 阶段 = P1 之前的全部内容。
- Codex 阶段 = P1 到 P359。
- 不按日期划分。
- 不用 2026-05-25 作为主分界。
- 完成度只按 merged main 和当前仓库文件判断。
- open PR、本地分支、聊天记忆、未合并 branch 不算完成。

### P1 Cut Uncertainty

git log 里存在多个带 P1 的历史点，例如：

- `WORKFLOW-P1 固化 V1 Operator Workflow Contract (#527)`
- `BACKEND-P1 cloud trigger entry`
- `HOME-P1`
- `P1 derivatives risk context plan baseline`

所以如果只靠 `git log --grep="P1"`，无法机械确定唯一 P1 起点。本报告按用户给出的事实边界处理：P1 之前视为 Cursor，P1-P359 视为 Codex；遇到无法从 commit message 精准归属的内容，以文件形态、PR/P 编号文档和主线状态辅助判断。

### Source Of Truth And Gaps

已读取并用于判断的 source-of-truth 文件包括：

- `AGENTS.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/V1_CURRENT_STATE.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/V1_BLOCKED_CAPABILITY_REGISTRY.md`
- `docs/V1_ALLOWED_REVIEW_ONLY_OUTPUTS.md`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/WORKFLOW_GITHUB_AUTH_AND_HANDOFF_RULE.md`

用户要求读取的以下文件在当前仓库未找到，因此不能作为 repo 内规则证据：

- `docs/CODEX_PHASE_EXECUTION_RULE.md`
- `docs/CODEX_PRE_COMMIT_AUDIT_CHECKLIST.md`
- `docs/CODEX_LOCAL_TASK_QUEUE.md`
- `docs/V1_FRAMEWORK_LOCK.md`

状态文档存在滞后：例如部分状态文件仍写 P358 / `c693481`，但当前 main 已是 `701a019 docs(point): define source-owned candidate integration runtime assembler plan (#828)`。这说明状态文档不是完全同步的单一真相，必须用 git main 交叉校验。

### Commands And Evidence

已执行只读审计命令：

- `git status --short`
- `git branch --show-current`
- `git log --oneline --decorate --all --max-count=500`
- `git log --oneline --decorate --all --reverse | head -300`
- `git log --oneline --decorate --all --grep="P1" --all`
- `git log --oneline --decorate --all --grep="P359" --all`
- `bash scripts/v1-state.sh`
- `find src/main/java/org/example/trademodel -type f | sort`
- `find src/test/java/org/example/trademodel -type f | sort`
- `find src/main/resources -type f | sort`
- `find docs -maxdepth 1 -type f | sort`
- `grep -R "class .*DTO|class .*Validator|class .*Assembler|class .*Service|class .*Controller|class .*Scheduler|interface .*Service|@Service|@RestController|@Controller|@Mapper|@Scheduled" -n src/main/java/org/example/trademodel | head -700`
- `grep -R "does not add service|does not add runtime|does not generate real|does not connect|not production|skeleton|review-only|test-only" -n docs | head -700`
- `grep -R "dashboard|position|manual position|push|candidate|readiness|point|score|evidence|MarketQuoteClient|Binance|OKX|DecisionResult|ExecutionPlan|PositionSync" -n src/main/java src/main/resources docs | head -1000`

注意：上述 grep 命令使用基础 grep，`|` 被当作普通字符，部分命令输出不充分；为避免误判，审计额外使用 `rg` 扫描了同等语义，并读取关键 Java / SQL 文件进行交叉验证。

## 3. Cursor-before-P1 Deliverables

| area | Cursor-era files/classes | what Cursor-era appears to have completed | current usability | evidence from file/class names or docs | whether Codex later rebuilt overlapping skeleton | risk / gap |
|---|---|---|---|---|---|---|
| Dashboard homepage | `DashboardController`, `templates/dashboard.html`, dashboard JS/static files | 有 dashboard 页面、summary/detail API、刷新接口和多种展示块 | usable / partial | `DashboardController` 暴露 `/dashboard`、`/api/dashboard/summary`、`/api/dashboard/detail`；`dashboard.html` 存在 | 是，Codex 后续大量新增 dashboard/status/readiness/point review-only 文档和 read-model 包 | 旧 dashboard 可用性依赖真实服务质量；新骨架没有充分回接 dashboard |
| Dashboard detail / read model | Dashboard display adapter、Score/Evidence/Decision read model classes | dashboard detail 已能展示 score、evidence、decision、plan/readiness 等信息 | partial | commits 包含 `add score-eight read model to dashboard detail`、`add plan readiness read model`、`add review summary` | 是，Codex 后续重做 Evidence/Score/Readiness/Point review-only chain | 容易形成两套展示模型：旧 dashboard 能看，新 skeleton 更安全但不可用 |
| Position sync / manual position | `PositionSyncService`, `PositionSyncScheduler`, `BinancePositionProvider`, `SwitchablePositionProvider`, position monitor services | 有持仓同步、手动/模拟 provider、Binance provider、状态落库和 dashboard 使用路径 | usable / partial | `PositionSyncService` 是 Spring `@Service`；`BinancePositionProvider` 可读取 Binance futures position risk | Codex 后续多次规划 Position Monitor / RiskActionGuard / runtime candidate safety，但没有替换这条可用链 | Binance 凭证/网络/生产安全依赖配置；需审计实盘边界 |
| Market quote adapter | `MarketQuoteClient`, `BinanceMarketQuoteClient`, `RealMarketEnvironmentService`, market services | 有 market quote client 和 Binance quote implementation；历史 log 显示 OKX fallback | partial | `BinanceMarketQuoteClient` 使用 `java.net.http.HttpClient` 访问 Binance ticker；commit 有 `add OKX fallback market data providers` | 是，Codex 后续 RuntimeKlineContext / SourceTrace / DataQuality skeleton 反复声明不读取行情 | 旧 market adapter 没接入新 source-owned candidate chain；OKX 当前源码证据不如 Binance 清晰 |
| Manual position / monitor advice | monitor service、position display helper、manual monitor docs/classes | 有结构化手动监控建议和 dashboard monitor UI | partial | commits 包含 `add structured manual monitor advice`、`add P-track dashboard monitor UI` | 是，Codex 后续 RiskActionGuard/Position Monitor 规划可能重复 | 旧能力可能偏展示/建议；后续安全语义未统一接入 |
| AI decision / multi-agent | AI decision docs/services/read models | 已有 AI decision / multi-agent 相关历史内容 | unknown / partial | docs 和 commit 历史出现 AI decision、多 agent、decision result | 未来 Three-AI 规划与旧 AI decision 可能重复 | 当前不应继续开 Three-AI 新包，先 inventory |
| Score / evidence / decision result | `EvidenceService`, `ScoreService`, `DecisionService`, mappers, DTOs, schema tables | 有 evidence、score、decision result 的服务、mapper、表结构和 dashboard 入口 | usable / partial | `schema.sql` 有 `tm_evidence_item`、`tm_score_item`、`tm_decision_result`、`tm_execution_plan`; `ScoreServiceImpl` 有实际评分逻辑 | 是，Codex P1-P359 中 MarketRead/Evidence/Score review-only chain 大量重建 | 旧评分是否足够可信另说，但它比新 DTO skeleton 更接近用户可用 |
| schema / database tables | `schema.sql`, mapper XML/Java, entity classes | 已有大量表：analysis run、evidence、score、decision、execution plan、market snapshot、real position、push snapshot、rule config 等 | partial | `schema.sql` 明确存在多张业务表 | Codex 后续多数包严禁改 schema，只在 DTO 层扩展 | 新对象不落库、不接 mapper，造成模型膨胀 |
| RuleConfig / Push Watchlist / Display Slots | `RuleConfigService`, `RuleController`, Watchlist adapters, display slot docs/classes | 已有 rule config API、push watchlist、display slots/dashboard 控制 | usable / partial | commits 有 `expose watchlist rule APIs`、`show push watchlist status`、`manage display slots locally`；source 有 RuleConfig watchlist adapter | 是，Codex P299-P305、P342、P351-P358 反复重写 Watchlist Pool 边界 | WatchlistPoolProof 新链未接旧 RuleConfig/Watchlist runtime |
| Risk Action Guard | risk display adapter、risk action docs/classes | 已有风险动作 guard 或展示边界概念 | partial | current source/docs 有 `RiskActionGuard` 相关 display/source-binding classes | 是，Codex P317/P341/P358 重复强化风险高不等于执行动作 | 安全语义是资产，但重复层太多，需合并到一个 owner |
| Hot reset / missed opportunity / review | missed opportunity、review、hot reset docs/classes/controllers | 有复盘、missed opportunity、review dashboard/API 相关内容 | partial | controllers include `MissedOpportunityController`, `ReviewController`; schema/docs 有 missed/review 相关内容 | Codex 后续 verification/docs-only 继续强调 review-only，但未明显提升可用复盘 | 应保留旧可触达功能，避免再写纯 verification 包 |
| Execution plan / boundary candidate | `ExecutionPlan*`, `BoundaryCandidate*`, plan services | Cursor 阶段已有 execution plan / boundary candidate 读写或展示模型 | partial | commits 有 `add boundary candidate DTOs/service/factory`; schema has `tm_execution_plan` | 是，Codex NumericPointProposal、SourceOwnedCandidateIntegration、RuntimeCandidate 等与其目标重叠 | 最大重复区：旧 plan/candidate 与新 point/runtime candidate 并行存在 |

## 4. Codex P1-P359 Deliverables

| module | package range | deliverable type | capability level | user-usable | connects real runtime/service | duplicates Cursor-era work | recommendation |
|---|---:|---|---|---|---|---|---|
| Workflow / GitHub automation | P1, P336B/R2 and related workflow packages | workflow docs/scripts, status commands, handoff rules | operational support | partial | yes, for workflow only | no direct business overlap | keep; do not let workflow packaging replace product progress |
| MarketRead review-only chain | early-mid Codex packages | review-only docs, DTO/validator/test and dashboard-safe boundaries | L0-L4 mostly | no / partial | limited | overlaps Cursor market quote adapter and dashboard market displays | merge into existing market service/read model; freeze new wrappers |
| Evidence / Score review-only chain | Codex evidence/score packages | read-only DTO/validator/verification | L0-L4 | partial at best | limited; Cursor services already exist | overlaps `EvidenceService`, `ScoreService`, schema tables | keep safety rules, map to existing service; stop creating new carriers |
| Candidate / Push review-only chain | P287-P305 region and earlier push packages | internal push preview/no-op/external blocked docs and code | some L4-L5 internal preview, external blocked | partial | internal-only / disabled | overlaps Cursor push watchlist/rule config/display slots | freeze external push; reconcile with old watchlist APIs |
| Readiness / Point skeleton chain | P306-P334 region | readiness, point proposal, safety validators, source-owned point plans | L0-L4 | no | no real point runtime | overlaps execution plan/boundary candidate/dashboard plan readiness | merge concepts; do not keep parallel point proposal family |
| Numeric point proposal chain | P318-P334 approximate | DTO/validator/assembler/docs for review-only numeric point | L0-L4 | no | no market read, no runtime | overlaps Cursor execution plan/boundary candidate | freeze until one point model owner is chosen |
| SourceTrace numeric source chain | P330-P335 approximate | SourceTrace numeric context DTO/validator/assembler/verification | L0-L4 | no / test-only | no production runtime | overlaps Cursor SourceTrace runtime/display/source trace services | merge with existing SourceTrace; avoid second source trace universe |
| RuntimeKlineContext source binding | P335-P338 | plan, DTO, validator, assembler, verification | L0-L4 | no | no real kline runtime | overlaps existing RuntimeKlineContextDTO/AssemblyService/PersistedOhlcv classes | keep rules, wire or delete duplicate DTO later |
| DataQuality source binding | P339 | DTO/validator/assembler/tests/docs | L0-L4 | no | no runtime | partly overlaps existing score/evidence/data quality concepts | freeze further DataQuality wrappers until runtime input selected |
| MultiTimeframe source binding | P340 | DTO/validator/assembler/tests/docs | L0-L4 | no | no runtime | overlaps older trend/score/multi-timeframe decision hints | keep as safety contract only; do not expand |
| RiskActionGuard source binding | P341 | DTO/validator/assembler/tests/docs | L0-L4 | no | no runtime | overlaps Cursor risk action guard/display/position monitor semantics | consolidate with existing guard owner |
| WatchlistPoolProof source binding | P342 | DTO/validator/assembler/tests/docs | L0-L4 | no | no Watchlist service/runtime | overlaps RuleConfig / Push Watchlist / Display Slots | merge with RuleConfig watchlist adapter; freeze new proof layers |
| Source-Owned Candidate Integration source binding | P343-P350 | boundary plan, source-binding DTO/validator/assembler/verification | L0-L4 | no | no service/runtime/dashboard | overlaps boundary candidate/execution plan/point proposal | freeze after P350; no new source-binding skeletons |
| Source-Owned Candidate Integration runtime DTO / Validator / Assembler plan | P351-P358, P359 branch unmerged | runtime boundary/input/DTO plan, DTO, validator, verification, assembler plan; P359 local branch adds assembler skeleton but not merged | Runtime L0-L2 on main; L3 only planned on main | no | no runtime/service; P359 still not merged | overlaps source-owned candidate source binding and old boundary candidate | pause P359; do not continue P360 before de-dup/wiring plan |

## 5. Cursor vs Codex Overlap Map

| overlapping area | Cursor artifact | Codex artifact | duplicate type | recommendation |
|---|---|---|---|---|
| Dashboard / read-only display | `DashboardController`, `dashboard.html`, dashboard detail adapters | many review-only docs and new read-only DTO/status chains | wrapper / parallel model | keep Cursor dashboard as user surface; merge Codex safety states into it only after ownership map |
| Market data | `MarketQuoteClient`, `BinanceMarketQuoteClient`, market environment services | RuntimeKlineContext source binding, SourceTrace numeric source, DataQuality plans | safer replacement / disconnected wrapper | keep Cursor adapter; wire through safe read adapter later; stop pretending new DTOs read market |
| Position / monitor | `PositionSyncService`, Binance/manual position provider, monitor UI | RiskActionGuard source binding, future Position Monitor planning | same purpose / safer wrapper | keep Cursor runtime; merge guard semantics into monitor after audit |
| Decision / score / evidence | `DecisionService`, `ScoreService`, `EvidenceService`, schema tables | Evidence/Score review-only chains and source-owned candidate runtime status | parallel scoring / wrapper | keep service-backed chain; map new validation rules onto existing DTOs |
| Execution plan / point display | `ExecutionPlan*`, `BoundaryCandidate*`, plan readiness dashboard | Numeric point proposal, SourceOwnedCandidateIntegrationRuntimeCandidateDTO | same purpose / unclear replacement | freeze new point/candidate DTOs; decide single canonical candidate/point owner |
| Push / Watchlist | RuleConfig watchlist APIs, display slots, push snapshots | P299-P305 internal push preview/no-op; P342 WatchlistPoolProof; P351-P358 watchlist boundary propagation | safer replacement / wrapper | keep Watchlist Pool boundary; merge with RuleConfig runtime source; do not create more proof DTOs |
| Risk Action Guard | existing risk guard/display/position-risk concepts | P317, P341, P358 risk action guard rules | repeated safety policy | keep one policy document and one validator owner; delete/merge duplicate language later |
| AI decision | older AI decision / multi-agent docs/classes | future Three-AI planning candidates | unclear duplicate | freeze Three-AI until old AI decision inventory is complete |
| Position monitor | manual position monitor and sync services | later Position Monitor roadmap and runtime candidate risk fields | same purpose / extension | keep Cursor monitor; only extend after service/dashboard target is chosen |
| SourceTrace | existing source trace DTO/service/read-only review flow | SourceTrace numeric source context DTO/Validator/Assembler | safer replacement / parallel source model | merge or adapter-map; avoid double SourceTrace status |
| RuntimeKline | existing RuntimeKlineContextDTO/AssemblyService/PersistedOhlcv | RuntimeKlineContextSourceBindingDTO/Validator/Assembler | safer wrapper but disconnected | either wire wrapper to existing assembly service or mark duplicate |

## 6. Usable Capability Audit

User-usable means: real input source, runtime wiring, stable dashboard/API display, review-only result based on real data, fail-closed behavior, no hand-built test DTO required, and no misleading trade instruction.

| module | current level | usable | why | main blocker | action |
|---|---|---|---|---|---|
| Dashboard homepage | L5-ish legacy review-only runtime | yes / partial | `DashboardController` and `dashboard.html` exist; real services feed parts of it | status drift and multiple read-model families | wire selected canonical runtime slice |
| Display Slots | L5-ish local/dashboard capability | partial | local display slot controls and docs exist | not equivalent to Watchlist Pool proof | keep, but never use as watchlist membership proof |
| Watchlist Pool | L4-L5 partial | partial | RuleConfig/watchlist APIs and adapters exist | new WatchlistPoolProof not wired to old RuleConfig service | refactor/wire after inventory |
| Position sync / manual position | L6-ish legacy production wiring | partial | position providers and sync service exist | exchange credentials/safety and dashboard consistency | keep and audit before expanding |
| Market quote adapter / Binance / OKX fallback | L6-ish for Binance; OKX unknown in current source | partial | Binance quote client exists; OKX visible in history but current code evidence weaker | not connected to new source-owned runtime | keep Binance adapter; inventory OKX |
| Evidence / Score | L5-ish legacy service-backed review | partial | services, mappers, schema and scoring logic exist | new review-only chain duplicates rather than consolidates | merge safety gates with existing score/evidence services |
| Decision Result | L5-ish legacy service-backed review | partial | decision result table/service/dashboard path exists | final semantics and new candidate runtime not unified | merge with candidate/point owner |
| Execution Plan | L4-L5 partial | partial | table/classes and boundary candidate service/factory history | overlaps numeric point proposal and runtime candidate | choose one canonical plan/candidate model |
| Candidate / Push Preview | L4-L5 internal/no-op | partial | push snapshots, no-op/external blocked docs, internal preview assets | external push blocked; runtime candidate not wired | keep preview-only; freeze external push |
| Readiness | L0-L4 mostly | no / partial | many readiness DTO/docs exist | not connected to real runtime decision flow | merge into dashboard/decision read model |
| Point Proposal | L0-L4 | no | numeric point proposal skeleton does not read market or create executable points | no runtime input/service/dashboard | freeze |
| Source-Owned Candidate Integration Runtime | main: L0-L2 + assembler plan; p359 unmerged L3 | no | DTO/validator exists, assembler only planned on main | no service/runtime/dashboard; p359 not merged | pause P359 |
| Risk Action Guard | legacy partial plus Codex L0-L4 source binding | partial / no for new chain | old guard concepts exist; new DTO chain not wired | duplicate policy owners | consolidate |
| Three-AI Decision | planning/unknown | no | no stable runtime evidence in current source-of-truth | likely overlaps old AI decision | freeze |
| Position Monitor | legacy partial | partial | manual monitor UI/sync exists | new runtime monitor plan not reconciled | refactor around existing services |
| Review / missed opportunity | legacy partial | partial | controllers/services/docs exist | not unified with new candidate runtime | keep, inventory |
| RuleConfig / Push Watchlist APIs | legacy partial | partial | service/controller/schema paths exist | not reconciled with WatchlistPoolProof | wire one canonical watchlist proof source |

## 7. Skeleton Inflation Analysis

Approximate counts from current repo and git history:

| metric | approximate evidence | meaning |
|---|---:|---|
| Java files under `src/main/java/org/example/trademodel` | 480 | The repo is not small; it already had substantial runtime/service surface before late Codex skeletons. |
| DTO files under `src/main/java/org/example/trademodel/dto` | about 157 | DTO surface is large. Adding new DTOs without ownership mapping increases ambiguity. |
| Validator-like files | about 19 | Validator count grew heavily in point/source-binding chain. |
| Assembler-like files | about 37 | Assembler layer is now large and often not wired to service. |
| Service/Scheduler-like files | about 181 | Cursor/early code already contains many real Spring services/schedulers. |
| P docs under `docs/P*.md` | about 707 | Package documentation volume is very high. |
| Phase docs under `docs/PHASE_P*.md` | about 373 | Verification/phase documentation is a dominant artifact type. |
| git commits matching docs/plan/verification/closure/gate/audit | about 728 | The project has a heavy docs/process tail. |
| git commits matching DTO/Validator/Assembler/Skeleton | about 244 | Skeleton work is a major share of visible progress. |

### What This Means

- Codex P1-P359 contains real work, but the later flow repeatedly splits one conceptual feature into plan, DTO, validator, assembler, verification, boundary plan, runtime input plan, runtime DTO plan, runtime DTO, runtime validator plan, runtime validator, validator verification, assembler plan, assembler skeleton, and so on.
- This is over-decomposition. It is safer than reckless runtime hacking, but it has become a productivity trap.
- P343-P358 alone shows the pattern clearly: Source-Owned Candidate Integration has source binding plan/DTO/validator/assembler/verification, then runtime boundary/input/DTO/validator/verification/assembler plan. Main still has no runtime wiring.
- Future packages should not keep using the default plan + DTO + validator + assembler + verification split unless the package directly wires an existing user surface.

### Packages That Should Have Been Scope Packs

- RuntimeKlineContext source binding P335-P338 could have been one closure pack.
- Source-Owned Candidate Integration P343-P350 could have been one source binding closure pack, or deferred until ownership mapping.
- Runtime Candidate P351-P358 should not have started before a wiring target was selected.
- WatchlistPoolProof and RiskActionGuard source binding should have been merged into the existing Watchlist/RuleConfig and risk guard inventory before new DTO families were added.

## 8. Runtime Wiring Gap

| gap | current state | consequence |
|---|---|---|
| market data read | legacy Binance MarketQuoteClient exists; new source-owned runtime does not read it | new runtime candidate cannot use real market input |
| source context read | SourceTrace/RuntimeKline/DataQuality/MultiTimeframe wrappers exist, but not unified into one production source context | source binding stays test/manual-input only |
| service layer | existing services exist, but new point/source-owned chains are mostly plain Java DTO/validator/assembler | no user-visible runtime capability |
| scheduler / scan loop | legacy schedulers exist; new source-owned candidate runtime does not have scan loop | no autonomous review-only candidate generation |
| dashboard runtime | dashboard exists; new runtime candidate objects are not displayed | user cannot see the new work |
| candidate runtime | source-owned runtime DTO/validator exists, assembler only planned on main | no candidate runtime output |
| point runtime | numeric point proposal skeleton exists, but no real entry/stop/TP/RR runtime | no point module usable by user |
| push runtime | external push remains blocked; internal preview/no-op exists | no real push capability, by design |
| position monitor runtime | legacy partial exists; new runtime candidate chain not integrated | duplicate monitor/guard semantics |
| three-AI runtime | not implemented as runtime-safe usable capability | high duplication risk with old AI decision |

### Would P359-P360 Solve This?

No. P359/P360 would close Runtime Candidate Assembler/Verification, but that still only creates context + validationResult from explicit input. It does not connect market data, source contexts, services, dashboard, scheduler, watchlist runtime, or candidate runtime generation.

If P359 is merged anyway, P360 must be the last skeleton-closing package and then the project must stop creating new skeletons. But the better decision is to pause P359 now and perform de-duplication plus wiring planning before another object family becomes “completed” but unused.

## 9. Stop / Continue Decision

### Option A: Pause P359, Do De-dup / Wiring Plan First

Recommended.

Reason:

- Current main has P358 only; P359 is not completed on merged main.
- The next skeleton does not reduce the real blocker.
- The real blocker is not lack of a Runtime Assembler class; it is lack of a chosen canonical path from real input -> service -> dashboard/API review-only output.
- Pausing now prevents another unowned object from becoming permanent.

### Option B: Finish P359/P360, Then Stop

Not recommended as the primary path.

Reason:

- It may make the current runtime DTO/validator/assembler family aesthetically complete.
- It still does not create a user-usable module.
- It continues exactly the behavior under audit: package completion without capability completion.

### Option C: Immediate Delete / Rollback Duplicate Objects

Not recommended as the first move.

Reason:

- Some Codex skeletons encode useful safety rules and tests.
- Deleting before ownership mapping risks losing safety assets.
- The right first step is freeze + inventory + merge map, then delete/refactor with evidence.

Decision: choose Option A. Pause P359. Do not start P360. Start a duplicate and runtime wiring stop-loss track.

## 10. Next 5 Actions

| action name | type | why needed | does it create new skeleton | does it reduce duplication | does it move capability toward REVIEW_ONLY_RUNTIME | risk level |
|---|---|---|---|---|---|---|
| Global Duplicate Skeleton Freeze Rule | docs / audit | Stop automatic plan/DTO/validator/assembler/verification packages until runtime target is chosen | No | Yes | Indirectly, by stopping churn | A |
| Cursor Artifact Inventory + Ownership Map | audit | Identify which existing Cursor services/controllers/tables are canonical owners | No | Yes | Yes, it identifies usable runtime assets | A |
| Runtime Wiring Target Selection Plan | docs / wiring plan | Pick one user-visible slice to wire first, such as Watchlist -> MarketRead -> Score -> Dashboard review-only status | No | Yes | Yes | A/B |
| Source-Owned Runtime vs Existing Point Proposal Merge Map | refactor plan | Decide whether RuntimeCandidate, NumericPointProposal, BoundaryCandidate, ExecutionPlan are one concept or separate | No | Yes | Yes | B |
| Minimal Review-Only Runtime Integration Plan | wiring | Define exact path from existing service output to dashboard/API, disabled-by-default and fail-closed | No | Yes | Yes | B |

These five actions intentionally avoid “another DTO skeleton.” The next useful work is ownership and wiring, not another package number.

## 11. What To Freeze Immediately

| item | freeze now | unfreeze condition |
|---|---|---|
| new DTO | Yes | Only after duplicate ownership map proves no existing DTO/model can own the concept |
| new Validator | Yes | Only when attached to a selected runtime/service/dashboard path |
| new Assembler | Yes | Only when it wires existing inputs, not hand-built test inputs |
| new docs-only plan | Yes | Only if it directly enables a de-dup/refactor/wiring decision |
| Three AI | Yes | Only after old AI decision inventory and exact non-duplicate runtime role |
| Position Monitor expansion | Yes | Only after existing PositionSync/manual monitor ownership map |
| Dashboard expansion | Yes | Only after selecting one canonical runtime slice to expose |
| Push external channel | Yes | Only after internal review-only pipeline is stable and explicit approval exists |
| executable point generation | Yes | Only after review-only runtime chain is proven, still with non-trading boundaries |
| order / execution / auto-trading | Permanent freeze | Should remain forbidden unless project charter changes explicitly and separately |

Freeze does not mean delete. It means stop adding parallel abstractions until the existing assets are reconciled.

## 12. Final Decision For User

项目已经有无底洞趋势，但还没到不可救。前两个月不是全白做，Codex 留下了安全边界、fail-closed 规则、测试和流程资产；但最大浪费很明确：反复给 Cursor 已经存在的 dashboard/service/decision/watchlist/point 概念套新的 DTO / Validator / Assembler / docs-only 壳，却没有把它们接回真实输入和用户可见界面。

真正有价值的资产是 Cursor 留下的 runtime/dashboard/service/schema 基底，以及 Codex 留下的安全规则和 fail-closed 测试语义。真正该停的是继续 P359/P360 这种“再补一个 assembler / verification”的惯性。下一步必须冻结新骨架，做重复对象清点、能力所有权合并、最小 review-only runtime 接线方案。当前 GPT + Codex 工作流还能继续，但必须改规则：不允许再用包数证明进展，只允许用“真实输入 -> service/runtime -> dashboard/API review-only 输出”的闭环证明进展。
