# V1 Evidence / Score Runtime Wiring Implementation Readiness Gate

This document records the implementation readiness gate for the `Evidence / Score review-only runtime status` slice. It does not implement Java, tests, dashboard changes, schema changes, endpoint changes, Push, Candidate, Decision, Point, or trading behavior.

## 1. Executive Summary

允许进入最小 implementation，但只能进入 **Minimal Review-Only Evidence / Score Runtime Wiring Implementation**，不能自动合并，不能扩大为 Candidate / Decision / Point / Push / Trading。

如果进入最小 implementation，允许新增或复用的 endpoint 只能是一个最小只读 Evidence / Score runtime status endpoint。现有 `POST /api/evidence/build`、`POST /api/score/build`、`GET /api/score/list` 和 dashboard detail output 已存在，但它们不是 dedicated review-only status endpoint；因此 future implementation 可以在 existing Evidence / Score / Dashboard controller owner path 中新增最小只读 status endpoint，或在 readiness implementation 时证明某个 existing endpoint 足够复用。

不允许新增 DTO / Validator / Assembler。未来 endpoint 必须优先返回 map / existing object / existing VO，不能创建新的 Evidence / Score wrapper owner。

不允许改 schema。`tm_evidence_item`、`tm_score_item`、`EvidenceItemMapper`、`ScoreItemMapper` 已经是 owner path 资产，未来最小实现只允许读取这些既有路径，不允许改表结构。

允许最小 dashboard status/copy/DOM，但只能用于显示 Evidence status、Score status、counts、top summary、source trace partial/complete、review-only、not candidate、not decision、not point、Watchlist / MarketQuote boundary。不能大改 dashboard，不能新增复杂评分卡片。

不允许接 Push / Candidate / Decision / Point / Trading，不生成候选 / 点位 / 方向，不生成 entry / stop / TP / RR，不调用 order / execution。

最小 implementation 允许候选文件类型：

- existing Evidence / Score / Dashboard controller only if minimal read-only status endpoint is missing;
- existing `EvidenceService` / `ScoreService` owner path only if absolutely necessary for existing read methods;
- existing mapper read path only if absolutely necessary and without schema changes;
- `src/main/resources/templates/dashboard.html` only if minimal safe status/copy/DOM is required;
- existing controller/dashboard tests;
- source-of-truth docs.

当前 capability level 不提升，本包是 readiness gate only。下一步应该进入：`Minimal Review-Only Evidence / Score Runtime Wiring Implementation`。

## 2. Implementation Permission Matrix

| Area | Allowed? | Allowed files | Reason | Guardrail |
|---|---|---|---|---|
| EvidenceService / Evidence owner path | Yes, minimal only | Existing `EvidenceService` / `EvidenceServiceImpl` if absolutely necessary | Existing owner path reads persisted Evidence top items and build path already exists. | Read-only status only; no new Evidence wrapper owner; no Candidate / Decision / Point output. |
| ScoreService / Score owner path | Yes, minimal only | Existing `ScoreService` / `ScoreServiceImpl` if absolutely necessary | Existing owner path reads persisted Score top items and build/list path already exists. | Score values must remain explanatory, not ranking/readiness/trading signal. |
| mapper / schema | Mapper read path only | Existing `EvidenceItemMapper` / `ScoreItemMapper` only if necessary | Existing `tm_evidence_item` / `tm_score_item` tables and mappers already support top item reads. | No schema changes; no new mapper family; no write behavior. |
| controller/API | Yes, minimal read-only endpoint if missing | Existing `EvidenceController`, `ScoreController`, or `DashboardController` only | Dedicated review-only status endpoint/panel is missing, and build/list/detail endpoints are not a status endpoint. | Endpoint must be read-only and return map / existing object / existing VO; no Push, Candidate, Decision, Point, direction, order, or execution calls. |
| dashboard.html | Yes, minimal copy/status only | `src/main/resources/templates/dashboard.html` | A user-visible review-only status panel/copy may be needed to prevent score/evidence misread. | No large layout change, no complex scoring card, no Display Slots promotion, no trading action copy. |
| controller/dashboard tests | Yes | Existing controller tests, `DashboardControllerTest`, or dashboard static tests if present | Future implementation must lock endpoint fields and safety copy. | Targeted only; no broad feature tests; no production behavior expansion. |
| source-of-truth docs | Yes | Existing source-of-truth docs | Must record capability and guardrails. | No docs-only drift beyond implementation scope. |
| schema.sql | No | None | Existing schema is sufficient for minimal read-only status. | Any schema need means NO-GO for this implementation. |
| config / pom | No | None | No dependency/config change is required. | Any config/pom need means NO-GO. |
| DTO / Validator / Assembler | No | None | Freeze rule blocks new skeleton/wrapper families; map / existing object is enough. | Any new DTO / Validator / Assembler means NO-GO. |
| Push / Candidate / Decision / Point / Trading | No | None | Evidence / Score status is only review-only runtime visibility. | No external channel, no candidate generation, no decision generation, no point generation, no final direction, no order/execution. |

## 3. Minimal Endpoint Readiness

已有可复用 endpoint: partial。`POST /api/evidence/build`、`POST /api/score/build`、`GET /api/score/list` 和 dashboard detail output already exist, but they are build/list/detail surfaces and are not a dedicated review-only runtime status endpoint.

可以新增最小 review-only endpoint: Yes, if future implementation keeps it read-only and inside existing Evidence / Score / Dashboard controller owner path.

可以不用新 DTO: Yes. Future endpoint may return map / existing object / existing VO only.

Endpoint 必须只读。

Endpoint 不能发送 Push。

Endpoint 不能生成 Candidate。

Endpoint 不能生成 Decision。

Endpoint 不能生成 Point。

Endpoint 不能生成方向。

Endpoint 不能调用 order / execution。

Allowed minimal endpoint fields should stay within #867 design:

- `status`
- `symbol`
- `evidenceCount`
- `scoreCount`
- `evidenceAvailable`
- `scoreAvailable`
- `evidenceTopItems`
- `scoreTopItems`
- `sourceTraceComplete`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `notCandidateSignal = true`
- `notDecisionSignal = true`
- `notPointSignal = true`
- `watchlistBounded = true`
- `marketQuoteChecked = true`

Forbidden endpoint fields:

- candidate ranking
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state

## 4. Minimal Dashboard Readiness

允许最小 dashboard status/copy/DOM。

Dashboard 只能显示 Evidence status / Score status / counts / top summary / source trace / review-only / not candidate / not decision / not point / Watchlist-MarketQuote boundary。

不能大改 layout。

不能新增复杂评分卡片。

不能全市场扫描。

不能把 Display Slots 当候选池。

Dashboard copy must make clear:

- Evidence / Score 是只读状态；
- Score 不是 candidate ranking；
- Evidence / Score 不是 Decision；
- Evidence / Score 不是 Point；
- Evidence / Score 不是交易信号；
- Watchlist Pool 和 MarketQuote freshness/fallback boundary still apply.

## 5. Required Test Scope For Implementation

未来最小 implementation 必须新增/修改的测试：

- controller endpoint test for Evidence / Score runtime status；
- dashboard static test for Evidence / Score labels；
- no DTO / Validator / Assembler check；
- no Push / Candidate / Decision / Point / Trading semantics check；
- Watchlist / MarketQuote boundary copy check if dashboard touched；
- endpoint read-only field check for `reviewOnly = true`、`notTradingSignal = true`、`notCandidateSignal = true`、`notDecisionSignal = true`、`notPointSignal = true`；
- missing / incomplete Evidence or Score should fail closed for Candidate / Push / Decision / Point / Trading.

## 6. No-Go Conditions

以下情况不能 implementation：

- 需要新 DTO / Validator / Assembler；
- 需要大改 schema；
- 需要直接接 Push；
- 需要生成 Candidate；
- 需要生成 Decision；
- 需要生成 Point；
- 需要生成方向或交易动作；
- 需要全市场扫描；
- 需要绕过 Watchlist Pool；
- 需要绕过 MarketQuote freshness/fallback status；
- dashboard 没有可安全插入位置；
- API 字段不足且必须新建复杂 endpoint；
- 需要把 Score 值展示成 candidate ranking、readiness、direction、point readiness 或 trading signal；
- 需要写入 Evidence / Score state，而不是只读展示。

## 7. Go / No-Go Decision

Decision: **A. GO: Minimal Review-Only Evidence / Score Runtime Wiring Implementation**.

理由：

- #866 confirms Evidence / Score owner path exists: `EvidenceService` / `EvidenceItemMapper` / `tm_evidence_item` and `ScoreService` / `ScoreItemMapper` / `tm_score_item`.
- #867 fixes the future status mapping and explicitly blocks DTO / Validator / Assembler, Push, Candidate, Decision, Point, and Trading.
- Existing build/list/detail endpoints are not a dedicated status endpoint, but a minimal read-only endpoint can be added inside existing controller owner path without a new DTO.
- Dashboard detail assets already expose `evidenceTopItems` / `scoreTopItems`, so future dashboard work can be limited to a small status/copy/DOM surface.
- Tests can be targeted to controller status fields, dashboard labels, and forbidden semantics.

Implementation 最多允许改这些具体类型文件：

- existing Evidence / Score / Dashboard controller file only for a minimal read-only status endpoint;
- existing EvidenceService / ScoreService only if necessary for existing read-path reuse;
- existing dashboard template only for minimal status/copy/DOM;
- existing controller/dashboard tests;
- source-of-truth docs.

明确禁止：

- new DTO / Validator / Assembler / Orchestrator;
- schema/config/pom;
- Push external channel;
- Candidate / Decision / Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- P359 / P360.

下一步进入 implementation，但不得自动合并。

## 8. Capability-Level Statement

当前 level: `REVIEW_ONLY_RUNTIME partial`。

本包是否提升 level: No, readiness gate only。

未来 Evidence / Score 最小实现目标：`REVIEW_ONLY_RUNTIME partial for Evidence / Score slice`。

不等于 Production Wiring。

不等于 Push。

不等于 Candidate generation。

不等于 Decision generation。

不等于 Point generation。

不等于 Trading。

## 9. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, readiness gate only
- 是否接 service/runtime/dashboard/API: No, readiness only
- 是否符合 #830 审计建议: Yes

## 10. Final Recommendation

可以进入 `Minimal Review-Only Evidence / Score Runtime Wiring Implementation`；允许的最小改动仅限 existing Evidence / Score / Dashboard controller 的只读 status endpoint、existing EvidenceService / ScoreService / mapper read path 的必要复用、最小 dashboard status/copy/DOM、targeted tests 和 source-of-truth docs；禁止 Push、Candidate、Decision、Point、P359/P360、交易动作、schema/config/pom 和新 DTO / Validator / Assembler，因为 #866/#867 已证明现有 Cursor-era owner path 足够承载最小只读 runtime status。
