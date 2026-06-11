# V1 Next Minimal Runtime Slice Selection After Data Source Health Closure

## 1. Current Merged Main

- Current actual main: `d9f7817 chore(workflow): fix status summary baseline`
- Current business closure baseline: `c6b35b5 docs(health): record data source health visual closure`
- Current capability level: `REVIEW_ONLY_RUNTIME partial`
- Current package type: selection / source-of-truth update only
- Capability movement: none

This package does not implement the selected slice. It only chooses the next smallest review-only runtime target and prepares the next source-read handoff.

## 2. Completed Slices

Completed `REVIEW_ONLY_RUNTIME partial` slices: 8.

1. PositionSync + Dashboard review-only status
2. Watchlist + RuleConfig + Dashboard/API review-only status
3. MarketQuote freshness / fallback / dashboard API
4. Evidence / Score review-only runtime status
5. DecisionResult review-only dashboard/API status
6. ExecutionPlan / BoundaryCandidate review-only runtime status
7. Review / Replay result status review-only runtime status
8. Data Source Health dashboard/API status review-only runtime status

All eight slices are review-only. They are not Production Wiring, not Push, not Candidate generation, not Decision generation, not Point generation, and not Trading.

## 3. Candidate Next Slices Considered

| Candidate | Pros | Risks | V1 boundary conflict points | Selection result |
|---|---|---|---|---|
| Push / Alert preview | User-visible and historically relevant to internal preview work. | High wording and workflow risk: even a preview can be mistaken as Push send or external alerting. | Push, external channel, send state, and notification action semantics are explicitly blocked for this stage. | Rejected for now. |
| Candidate preview | Natural after score/decision/plan surfaces and user-visible. | Too close to Candidate generation and ranking semantics. | Candidate generation, candidate ranking, candidate pool, promotion, and Push handoff are blocked. | Rejected for now. |
| Position monitor preview | There are legacy position foundations and PositionSync is already visible. | May imply monitoring expansion, action suggestions, close/reverse hints, or account/provider writes. | Position Monitor expansion is explicitly frozen as a default next step; must not imply execution advice or auto-trading. | Rejected for now. |
| Account risk snapshot preview | Potentially useful as a read-only safety surface. | May require account/balance/provider ownership, new aggregation, or external account refresh. | External refresh, account action, position sizing, leverage, order/execution, and risk-action outputs are blocked. | Rejected for now. |
| Macro / News event review-only status | Could add context visibility and source-health style status. | Likely needs external event/news provider reads or scheduler/collector ownership. | External API refresh, scheduler, collector, API client trigger, and news-driven signal semantics are blocked. | Rejected for now. |
| RuleConfig runtime audit / rule explainability | Reuses existing Watchlist + RuleConfig assets and already-completed dashboard/API boundary. Small, explainable, user-visible, and safe. | Must avoid becoming Candidate eligibility, Push audit, or rule-driven signal generation. | Needs strict read-only wording: explain configured rules and runtime boundary only; no Candidate, Point, Decision generation, or trading action. | Selected. |
| System health aggregate / operational readiness | Broadly useful after Data Source Health. | Too broad immediately after Data Source Health; may pull in unrelated operational readiness and imply Production Wiring readiness. | Operational readiness can be mistaken as production readiness, external refresh, or all-system health gate. | Rejected for now. |

## 4. Selected Next Slice

Selected next slice: `RuleConfig runtime audit / rule explainability`.

Next source-read branch: `ruleconfig-runtime-audit-rule-explainability-source-read`.

Risk level: `A` for source-read docs and source-of-truth updates only.

Why this slice now:

- It is the smallest low-conflict follow-up after Data Source Health because it can inspect existing rule configuration and already-established Watchlist / RuleConfig review-only boundaries.
- It should reuse existing Cursor-era / V1 assets around RuleConfig, Watchlist Pool, dashboard status/copy, and any current audit or rule display helpers.
- It is user-visible without needing new trading semantics: the user can see whether rule configuration is readable, bounded, and explainable.
- It does not require Push, Candidate generation, Decision generation, Point generation, entry / stop / TP / RR, order/execution, auto-trading, external API refresh, scheduler, collector, schema/config/pom, or new DTO / Validator / Assembler.
- It naturally follows the eight completed review-only runtime slices by making the already-used rule boundary easier to audit before any more execution-adjacent package is considered.

## 5. Why Not The Others

- Push / Alert preview is deferred because even an internal preview risks being interpreted as sendable Push or external-channel behavior.
- Candidate preview is deferred because it risks reopening Candidate generation, ranking, and promotion semantics before the source-read boundary is re-established.
- Position monitor preview is deferred because Position Monitor expansion remains a frozen default track and could imply action advice or execution-adjacent behavior.
- Account risk snapshot preview is deferred because it may require account/provider ownership, position size, leverage, or balance refresh semantics.
- Macro / News event review-only status is deferred because it likely needs external news/event data refresh, scheduler, collector, or API-client ownership.
- System health aggregate / operational readiness is deferred because Data Source Health just closed and a broader operational readiness aggregate could be confused with Production Wiring readiness.

## 6. Next Source Read Task Definition

Next allowed action: `Source Read for RuleConfig runtime audit / rule explainability`.

The next package must remain source-read only. It should inventory existing RuleConfig / Watchlist / dashboard / API / test assets and decide whether a minimal review-only runtime audit / explainability status can enter design.

The source read must answer:

- Which RuleConfig owner path exists?
- Is there an existing runtime audit or rule explainability path?
- Is there an existing dashboard/API/status surface that can be reused?
- Can the slice remain review-only and fail-closed?
- Can it avoid new DTO / Validator / Assembler?
- Can it avoid schema/config/pom changes?
- Can it avoid Push, Candidate generation, Decision generation, Point generation, final direction, entry / stop / TP / RR, order/execution, auto-trading, external refresh, scheduler, collector, and API-client trigger?

## 7. Forbidden Scope

This selection package and the next source-read package must not:

- modify Java business code;
- modify tests;
- modify dashboard business logic;
- modify schema/config/pom;
- connect Push or external channel;
- generate Candidate;
- generate Decision;
- generate Point;
- generate final direction;
- generate entry / stop / TP / RR;
- connect order / execution / auto-trading;
- add DTO / Validator / Assembler;
- continue P359 / P360;
- implement RuleConfig audit / explainability;
- run a selected-module implementation.

## 8. Capability Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`
- This selection package level movement: none
- Selected next source-read package movement: none until a future implementation is designed, gated, implemented, verified, and visually closed
- Still not Production Wiring
- Still not Push
- Still not Candidate generation
- Still not Decision generation
- Still not Point generation
- Still not Trading
