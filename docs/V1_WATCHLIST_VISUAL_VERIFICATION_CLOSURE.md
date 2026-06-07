# V1 Watchlist Visual Verification Closure

# 1. Executive Summary

Watchlist visual verification 通过。`/dashboard` 能打开，`watchlistStatusPanel` 真实可见，Watchlist Pool 当前状态、空值 / fail-closed 状态、Display Slots 边界文案、默认六个币边界、只读不发送 Push 文案都能在浏览器中看到。

Display Slots / Watchlist Pool 边界清楚：Display Slots 只是首页展示位，不是候选池；默认六个币不是候选池；不在 Watchlist Pool 的资产不进入候选 / 推送 / 扫描 / 点位。当前仍然是 review-only，没有 Push、MarketQuote、Candidate、Point、Trading 语义。

当前 capability level 不改变，仍为 `REVIEW_ONLY_RUNTIME partial`。Watchlist slice 在 #855 implementation、#856 verification 和本次 visual closure 后，可以作为第二条止损后可见 runtime 小闭环收口。下一步应进入 `Next minimal runtime slice selection`，不是 P359 / P360。

# 2. Visual Verification Matrix

| Check | Result | Evidence |
|---|---|---|
| `/dashboard` browser open | PASS | Browser opened `http://127.0.0.1:8081/dashboard`; page title was `TRINE LOGIC (V1)`. |
| Watchlist status panel visible | PASS | `watchlistStatusPanel` existed, was visible, and after scroll had rect `x=312, y=293, width=950, height=134`; fully inside 1280x720 viewport. |
| Watchlist Pool current assets / empty / fail-closed visible | PASS | Panel displayed `WATCHLIST_CONFIG_MISSING`, current assets `—`, source `MISSING`, and fail-closed `是`. |
| Display Slots only homepage display copy visible | PASS | Panel displayed `Display Slots 只是首页展示位`. |
| Display Slots not candidate pool copy visible | PASS | Panel displayed `Display Slots 不是候选池`. |
| default six not candidate pool copy visible | PASS | Panel displayed `默认六个币不是候选池`. |
| review-only / no Push copy visible | PASS | Panel displayed `只读状态，不发送 Push`. |
| no candidate / point / trading action copy | PASS | Panel displayed `不在 Watchlist Pool 不进入候选/推送/扫描/点位`; no visible `placeOrder`, `createOrder`, `submitOrder`, `auto-trading`, `order execution`, `entry / stop / TP`, `final direction`, `MarketQuote`, `candidateRanking`, `closePosition`, `reversePosition`, or `openPosition` text in the panel. |
| no layout overlap | PASS | Browser check reported `overlapWithNextSection=false`; the Watchlist panel did not overlap the next dashboard section. |

# 3. Runtime / Test Recap

| Check | Result | Evidence |
|---|---|---|
| compile | PASS | `./mvnw -q -DskipTests compile` passed in this closure task. |
| test-compile | PASS | `./mvnw -q -DskipTests test-compile` passed in this closure task. |
| RuleControllerTest | PASS | `./mvnw -q -Dtest=RuleControllerTest test` passed in this closure task. |
| DashboardControllerTest | PASS | `./mvnw -q -Dtest=DashboardControllerTest test` passed in this closure task. |
| RuleConfigWatchlistPoolReadAdapterTest | PASS | `./mvnw -q -Dtest=RuleConfigWatchlistPoolReadAdapterTest test` passed in this closure task. |
| API smoke from #856 | PASS | #856 verified `/api/rule/push-watchlist` HTTP 200 with review-only fields including `reviewOnly=true` and `displaySlotsAreCandidatePool=false`. |
| dashboard smoke from #856 | PASS | #856 verified `/dashboard` HTTP 200 and Watchlist runtime DOM/copy presence. |

# 4. Boundary Confirmation

- no DTO / Validator / Assembler: confirmed; this closure adds no Java and no new skeleton object.
- no schema/config/pom: confirmed; this closure does not edit schema, config, or pom.
- no Push external channel: confirmed; the dashboard copy explicitly says only read-only status and no Push send.
- no MarketQuote: confirmed; no market quote provider or market data wiring is touched.
- no candidate generation: confirmed; Watchlist Pool is only a boundary/status view.
- no point generation: confirmed; no point, entry, stop, TP, RR, or final direction is generated.
- no final direction: confirmed.
- no order / execution / auto-trading: confirmed.
- P359 / P360 frozen: confirmed; neither package is revived or started.

# 5. Capability-Level Conclusion

Current level remains `REVIEW_ONLY_RUNTIME partial`.

PositionSync slice: `REVIEW_ONLY_RUNTIME partial`.

Watchlist slice: `REVIEW_ONLY_RUNTIME partial` after #855, #856, and this visual closure. The Watchlist API/dashboard status is useful and visible, but it is still a safe review-only boundary view.

This is still not Production Wiring, not Push, not MarketQuote, not candidate generation, and not point generation.

# 6. Next Step Decision

Decision: **A. Next minimal runtime slice selection**.

Reason: visual verification passed. The Watchlist panel is visible, required boundary copy is clear, layout overlap was not observed, and forbidden trading / Push / MarketQuote / candidate / point semantics were not visible. There is no reason to remain in a Watchlist closure follow-up.

Do not continue P359, P360, new DTO, new Validator, new Assembler, Three AI, Position Monitor expansion, Push external channel, MarketQuote wiring, point generation, order, execution, or auto-trading.

# 7. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Visual closure confirms Watchlist `REVIEW_ONLY_RUNTIME partial`
- 是否接 service/runtime/dashboard/API: Verification only; verifies #855/#856 minimal API/dashboard wiring
- 是否符合 #830 审计建议: Yes
