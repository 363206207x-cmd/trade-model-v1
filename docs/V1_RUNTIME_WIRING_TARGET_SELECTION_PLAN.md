# V1 Runtime Wiring Target Selection Plan

This document is the first wiring-target selection after the #830 global audit, the duplicate skeleton freeze rule, and the Cursor artifact ownership map.

It is a stop-loss wiring selection artifact. It does not implement Java, tests, service wiring, runtime wiring, dashboard changes, push, external channel, point generation, final direction, order execution, or auto-trading.

## 1. Executive Summary

本任务只选择 wiring target，不做实现。

首选目标必须从 `docs/V1_CURSOR_ARTIFACT_INVENTORY_OWNERSHIP_MAP.md` 推荐项中选择。根据 ownership map 的结论，默认首选应评估并最终选择：

**B. PositionSync + Dashboard review-only status**

本文件比较四个候选：

- A. Watchlist + MarketQuote + Dashboard review-only status
- B. PositionSync + Dashboard review-only status
- C. Score/Evidence + Dashboard review-only status
- D. RuleConfig Watchlist + WatchlistPoolProof merge

最终选择 B。

原因很直接：PositionSync 已有 Cursor-era provider、fallback、service、scheduler、mapper、schema、dashboard/API 读取面；它不要求生成 candidate、point、final direction、push payload、AI 结论或交易动作。它是当前最短路径，可以把项目从“新骨架继续增殖”拉回“复用已有 runtime 资产并朝 REVIEW_ONLY_RUNTIME 走”。

本任务不继续 P359，不启动 P360，不新增 DTO / Validator / Assembler / Orchestrator，不新增普通 docs-only plan。

## 2. Selection Criteria

本轮 wiring target 必须满足：

- 复用 Cursor-era service / runtime / dashboard / API 资产；
- 不新增 DTO / Validator / Assembler；
- 不生成 candidate / point / final direction；
- 不接 external push；
- 不涉及 order / execution / auto-trading；
- 最小实现后能在 dashboard / API 产生 review-only status；
- 能提升或准备提升到 capability level 5 `REVIEW_ONLY_RUNTIME`；
- 能减少重复，或至少避免继续 P359 的 skeleton churn；
- 能被 fail-closed / review-only / manualReviewRequired / notTradeInstruction 规则包裹。

如果候选只能产生新的 carrier、wrapper、plan、validator 或 assembler，而不能靠近已有 service / runtime / dashboard / API，它不能作为第一目标。

## 3. Candidate Comparison

| Candidate | Cursor assets reused | Codex safety rules reused | Creates new skeleton | Connects real input | Dashboard/API path exists | Risk | Capability movement | Decision |
|---|---|---|---|---|---|---|---|---|
| A. Watchlist + MarketQuote + Dashboard review-only status | `RuleConfigWatchlistPoolReadAdapter`, `RuleConfigServiceImpl`, `MarketQuoteClient`, `BinanceMarketQuoteClient`, `RealMarketEnvironmentService`, `DashboardController` | Watchlist Pool boundary, Display Slots not proof, review-only / fail-closed, no push send | No | Yes, but crosses watchlist config plus market quote | Partial | B | Good second candidate, but broader than B because it touches watchlist membership and market read together | Not selected first |
| B. PositionSync + Dashboard review-only status | `PositionSyncService`, `PositionSyncScheduler`, `PositionProvider`, `SwitchablePositionProvider`, `BinancePositionProvider`, `SimulatedPositionProvider`, `RealPositionMapper`, `tm_real_position`, `DashboardController`, `dashboard.html`, `SystemController` | review-only, notTradeInstruction, manualReviewRequired, incomplete-safe, fail-closed, no close / reverse / open action | No | Yes, via simulated or Binance position provider result | Yes, `/api/system/position-sync-status` and dashboard position display already exist | A | Best near-term path from partial legacy runtime to future REVIEW_ONLY_RUNTIME | Selected |
| C. Score/Evidence + Dashboard review-only status | `EvidenceServiceImpl`, `ScoreServiceImpl`, `EvidenceController`, `ScoreController`, mappers, score/evidence tables, dashboard top evidence/score cards | review-only evidence / score safety, forbidden final direction, no candidate promotion | No | Partial, because evidence/score source quality varies | Yes | B | Useful, but more entangled with DecisionResult and candidate interpretation | Not selected first |
| D. RuleConfig Watchlist + WatchlistPoolProof merge | `RuleConfigWatchlistPoolReadAdapter`, `RuleConfigServiceImpl`, `RuleController`, `tm_rule_config`, watchlist runtime source classes | Watchlist Pool proof boundary, Display Slots not proof, stale/untrusted fail-closed | No | Yes, from rule config if source read is verified | Partial | A/B | Strong duplication reducer, but first needs a tighter source-read verification around current watchlist path | Not selected first |

## 4. Selected Target

Selected target:

**PositionSync + Dashboard review-only status**

Canonical owner:

- `PositionSyncService`
- `PositionSyncScheduler`
- `PositionProvider`
- `SwitchablePositionProvider`
- `BinancePositionProvider`
- `SimulatedPositionProvider`
- `RealPositionMapper`
- `tm_real_position`

Surface:

- `DashboardController`
- dashboard summary/detail read models
- `dashboard.html`
- `/api/system/position-sync-status`

Input:

- simulated position provider result; or
- Binance position provider result when credentials and runtime environment safely allow it.

Output:

- review-only position sync status.

Allowed future statuses:

- `REVIEW_ONLY_POSITION_SYNC_READY`
- `INCOMPLETE`
- `BLOCKED_FAIL_CLOSED`
- `SIMULATED_FALLBACK`

Forbidden output:

- trade action;
- close position;
- reverse position;
- open position;
- order;
- execution;
- leverage advice.

Explicit non-goals:

- no point;
- no candidate;
- no push;
- no AI;
- no final direction;
- no order / execution / auto-trading.

Why this is the safest first target:

- It has a real existing service/runtime path instead of only DTO skeletons.
- It already has a dashboard/API surface.
- It can be expressed as status visibility, not advice.
- It can reuse Codex fail-closed language without creating a new object family.
- It avoids the high-risk point/candidate/push/AI areas.

## 5. Non-selected Candidates

### Watchlist + MarketQuote + Dashboard

This remains valuable, but it is not the first target because it combines two ownership problems at once: watchlist membership proof and market quote read. It also sits close to candidate/push semantics, so a sloppy implementation could accidentally look like opportunity generation. It should come after a narrower runtime slice proves the workflow can move from planning to user-visible status without adding skeletons.

### Score/Evidence + Dashboard

This is also a strong candidate because `EvidenceServiceImpl` and `ScoreServiceImpl` already exist. It is not selected first because score/evidence output can be misread as directional judgment or candidate readiness. It needs a clearer merge map with DecisionResult before wiring is expanded.

### RuleConfig Watchlist + WatchlistPoolProof Merge

This is the best duplication-reduction candidate after PositionSync, but it should not be first because it needs targeted source-read verification around `RuleConfigWatchlistPoolReadAdapter`, `WatchlistRuntimeSourceService`, dashboard/API visibility, and proof freshness. It should be considered next only after the project proves it can complete one tiny runtime status wiring target without spawning new wrappers.

## 6. Implementation Boundary For Next Package

The next package must be:

**PositionSync/Dashboard Source Read Verification**

If source clarity is still insufficient, it may be narrowed further to:

**PositionSync/Dashboard Current Source Read Audit**

The next package cannot directly jump to Java implementation unless the source read proves the owner path is already clear enough and the implementation scope is strictly limited to existing service/runtime/dashboard/API review-only status.

The next package also cannot:

- create a new DTO;
- create a new Validator;
- create a new Assembler;
- continue P359;
- start P360;
- start Three AI;
- expand Position Monitor;
- expand Dashboard beyond the selected existing position status surface;
- implement Push;
- generate point values.

## 7. Capability-Level Movement

Current level:

- Partial legacy runtime exists, but the selected new track is still planning.

Target level after future implementation:

- `REVIEW_ONLY_RUNTIME`.

Does this package raise capability level:

- No.

Why this package is still worth doing:

- It selects one concrete target.
- It prevents more skeleton churn.
- It forces the next step to reuse Cursor-era owner assets.
- It aligns future progress with capability level rather than package count.

## 8. Freeze Rule Compliance

| Question | Answer |
|---|---|
| 是否创建新骨架 | No |
| 是否复用 Cursor-era 资产 | Yes |
| 是否减少重复 | Yes |
| 是否提升 capability level | No, prepares |
| 是否接 service/runtime/dashboard/API | No, selects target |
| 是否符合 #830 审计建议 | Yes |

This target complies with `docs/V1_DUPLICATE_SKELETON_FREEZE_RULE.md` because it directly selects canonical ownership and directly prepares an existing service/runtime/dashboard/API path for review-only runtime wiring.

## 9. Final Recommendation

当前首个落地目标是 **PositionSync + Dashboard review-only status**；它最安全，因为它复用已有 provider / service / scheduler / mapper / schema / dashboard/API 资产，只输出只读同步状态，不碰 candidate、point、push、AI、final direction 或交易动作。下一步只做 **PositionSync/Dashboard Source Read Verification**，P359、P360、新 DTO、新 Validator、新 Assembler、Three AI、Position Monitor expansion、Dashboard expansion、Push、point generation 继续冻结。
