# V1 Next Minimal Runtime Slice Selection

This document selects the next smallest review-only runtime slice after the completed PositionSync and Watchlist stop-loss slices. It is a selection / source-read-lite package only.

## 1. Executive Summary

当前已经完成两个可见的 runtime 小闭环：

- `PositionSync + Dashboard review-only status`: `REVIEW_ONLY_RUNTIME partial`。
- `Watchlist + RuleConfig + Dashboard/API review-only status`: `REVIEW_ONLY_RUNTIME partial`。

下一条最小 runtime slice 推荐选择：`MarketQuote freshness / fallback / dashboard API status`。

理由很直接：MarketQuote / BinanceMarketQuoteClient / DashboardServiceImpl / dashboard display 这些 Cursor-era 资产已经存在，dashboard 已经有行情展示尝试，但用户目前无法稳定区分“行情新鲜”“行情缺失”“provider fallback”“外部 provider 失败/不可用”。把下一步先收窄为 source read，可以确认现有 owner path 和 dashboard/API surface，而不会直接接 MarketQuote runtime、候选、点位或 Push。

这比继续 Push、Three AI、点位、P359/P360 更安全。那些方向要么靠近外部发送和交易解释，要么会重新进入 DTO / Validator / Assembler / wrapper 膨胀，要么会把已有 owner path 旁路掉。

下一步具体做：`Source Read for MarketQuote Freshness / Fallback Dashboard API Status`。该下一步仍然必须只读，不实现，不接 MarketQuote runtime，不生成 candidate / point / final direction。

## 2. Current Completed Runtime Slices

| Runtime slice | Capability level | User-visible output | Boundary |
|---|---:|---|---|
| PositionSync + Dashboard review-only status | REVIEW_ONLY_RUNTIME partial | Dashboard 显示 provider、fallback、simulated、freshness、last sync、open position count，并明确只读状态。 | 不是 Production Wiring，不是 Binance 交易授权，不是 Position Monitor 完整功能，不是交易建议。 |
| Watchlist + RuleConfig + Dashboard/API review-only status | REVIEW_ONLY_RUNTIME partial | `GET /api/rule/push-watchlist` 和 dashboard 显示 Watchlist Pool / Display Slots 边界、fail-closed、只读且不发送 Push。 | 不是 Production Watchlist，不是外部 Push，不是 MarketQuote wiring，不是候选生成，不是点位生成，不是交易能力。 |

两条已经完成的小闭环都只是 review-only。它们证明了“可见状态 + 明确安全边界”可以比继续堆骨架更有效，但它们不等于生产接线，不等于交易能力。

## 3. Candidate Slice Comparison

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Duplicate risk | Recommendation |
|---|---|---|---|---|---|---|
| MarketQuote freshness / fallback / dashboard API status | `MarketQuoteClient`, `BinanceMarketQuoteClient`, `RealMarketEnvironmentService`, `DashboardServiceImpl`, dashboard 行情展示路径。 | 用户能看懂行情来源是否新鲜、是否 fallback、是否缺失或 provider 失败，避免把空价格/旧价格误读为可用行情。 | Legacy runtime 和 dashboard read path 已存在，但 owner path、API surface、freshness/fallback 字段需要 source read。 | 中等；不能让 MarketQuote 进入 scan/candidate/point。 | 低到中；如果复用 existing owner path，能避免新 MarketRead wrapper。 | 推荐为下一条 source-read slice。 |
| Evidence / Score review-only runtime status | `EvidenceBriefVO`, `ScoreBriefVO`, dashboard detail fields, P295-P298 review-only skeletons。 | 能解释证据/分数状态，但更像内部分析链状态。 | 有 dashboard/detail read model，但 runtime owner 与 score 计算边界需要再梳理。 | 中等；容易继续 Evidence/Score skeleton。 | 高；P295-P298 已有多层 skeleton。 | 暂不作为第一优先。 |
| DecisionResult review-only dashboard/API status | `DecisionResultVO`, `DecisionService`, dashboard detail response。 | 能展示决策 read model 状态。 | Existing read model 较强。 | 中等；容易被误读成 final direction。 | 中等；会与 BoundaryCandidate / ExecutionPlan owner-path 继续缠绕。 | 暂缓，避免接近 final direction。 |
| ExecutionPlan / BoundaryCandidate review-only display continuation | `BoundaryCandidateService`, `BoundaryCandidateDTO`, `PlanService`, `ExecutionPlanVO/DO/Mapper`, display adapters。 | 对计划/边界展示有价值。 | Owner-path source read、merge design、tests-first coverage 已安全收口。 | 中等；接近 entry / stop / TP / RR 解释。 | 中等；继续做会拉回 point/proposal 方向。 | 不选；当前不应继续点位相邻链。 |
| Internal Push preview status only | Internal push preview / dashboard display gate artifacts exist in earlier chain. | 能展示内部 Push 预览状态。 | 旧链已有 review-only display，但外部 channel 边界敏感。 | 高；容易滑向 Push send / external channel。 | 中等；可能复活 candidate/push skeleton。 | 不选。 |
| Position Monitor manual-input source read | Legacy position monitor / manual position / sync foundations exist. | 对持仓可见性有价值。 | PositionSync slice 刚完成，monitor action 仍需单独边界。 | 高；容易被误读为 close/reverse/open action suggestion。 | 中等；会扩张 Position Monitor。 | 暂缓。 |

## 4. Recommended Next Slice

推荐下一条 slice：`MarketQuote freshness / fallback / dashboard API status`。

Owner path 候选：

```text
MarketQuoteClient / BinanceMarketQuoteClient
  -> RealMarketEnvironmentService / DashboardServiceImpl
  -> DashboardController / existing dashboard API response
  -> dashboard.html quote freshness / fallback status display
```

该 owner path 仍需下一步 source read 验证，不能在本包直接确认实现范围。source read 必须回答：

- `MarketQuoteClient` / `BinanceMarketQuoteClient` 是否已有 freshness / fallback / failure 语义；
- `RealMarketEnvironmentService` / `DashboardServiceImpl` 是否已经聚合行情来源状态；
- dashboard/API 是否已有可复用字段；
- 是否可以只展示 freshness / fallback / missing / blocked 状态；
- 是否能完全避免 Candidate / Point / Push / trading 语义。

这条 slice 有机会形成 review-only runtime 小闭环，因为用户已经在 dashboard 上看到行情相关输出，但缺少明确“可用/不可用/过期/fallback”的只读状态。下一步只做 source read，可以避免直接接 MarketQuote runtime，也不需要新 DTO / Validator / Assembler。

禁止边界：

- 不接 Push；
- 不接 Candidate；
- 不接 Decision；
- 不接 Point；
- 不生成 entry / stop / TP / RR；
- 不生成 final direction；
- 不接 order / execution / auto-trading；
- 不把 MarketQuote 作为 scan-chain/candidate-chain 的授权 runtime provider。

## 5. Rejected Options

- Push external channel: 暂不选。外部发送仍需单独授权，且容易从“状态展示”滑向发送能力。
- Three AI: 暂不选。它不是最小 runtime slice，会引入 provider orchestration、预算、fallback、final decision 风险。
- point generation: 暂不选。P359/P360 与 numeric point wrapper 已冻结，任何点位方向都会把项目拉回重复骨架或交易解释风险。
- ExecutionPlan / BoundaryCandidate continuation: 暂不选。owner-path safety track 已经收口，继续推进会靠近 entry / stop / TP / RR 和 ExecutionPlan runtime，不是当前最大安全推进。
- Position Monitor expansion: 暂不选。PositionSync 已完成可见状态，Position Monitor 扩张更接近 action suggestion，需要另一个严格 source-read gate。
- P359 / P360: 不选。P359 分支未合并，#829 closed unmerged，不计入完成；P360 仍禁止启动。没有证据表明恢复它们会减少重复而不是增加 runtime candidate wrapper。

## 6. Next Step Decision

Decision: **A. GO: Source Read for selected next minimal runtime slice**.

下一步命名为：

```text
Source Read for MarketQuote Freshness / Fallback Dashboard API Status
```

下一步必须保持 source-read only，不实现。它必须先确认 existing MarketQuote / dashboard owner path、freshness/fallback 字段、API/dashboard surface、test coverage 与禁止语义，再决定是否可以进入 minimal review-only design。

## 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, selection only
- 是否接 service/runtime/dashboard/API: No, selection only
- 是否符合 #830 审计建议: Yes
