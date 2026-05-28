# Project Progress Index

This index uses fixed progress口径. It does not count P-package quantity as progress.

Completion is based on merged `main` only.

Current merged main:

- `26e8943 BACKEND-P291A Workflow Reset and Progress Source of Truth Pack (#703)`

Current capability movement pack:

- `BACKEND-P292 MarketReadRequest Test-Only Wiring and Review-Only Assembler Slice`

P292 is capability movement, not closure-only. It moves `MarketReadRequest test-only wiring` from `0 NOT_STARTED` to `4 TEST_ONLY_WIRING`.

P292 only permits a small Skeleton / Test and MVP-chain improvement. It must not raise Production Runtime Progress.

## Fixed Progress Percentages

| Progress view | Current range | Why this range | Why it cannot be higher yet |
|---|---:|---|---|
| Total Progress | 58%-64% | Many review-only displays, contracts, DTOs, validators, no-op skeletons, and safety rules exist. | The full V1 chain still lacks completed market read -> scan output -> score -> candidate -> push preview -> execution advice -> monitor -> review closure. |
| MVP Progress | 53%-61% | Watchlist/display/review surfaces, skeletons, and the MarketReadRequest DTO -> GuardValidator test-only wiring slice exist. | The user-facing MVP loop is not yet closed end to end. |
| Production Runtime Progress | 28%-36% | Some legacy runtime components exist, including market clients, schedulers, dashboard services, and position foundations. | P292 does not add production wiring; the new scan-chain production runtime is not wired, and push/readiness/point/trading paths remain blocked. |
| Governance / Contract Progress | 86%-92% | Boundaries, gates, fail-closed rules, no-trade semantics, and review-only policy are extensive. | Governance drift occurred and needs P291A source-of-truth correction. |
| Skeleton / Test Progress | 73%-81% | DTO, validator, no-op, audit, queue, channel, score, candidate, market-read request skeletons/tests, and MarketReadRequest test-only wiring exist. | Review-only runtime wiring is incomplete in several key chain steps. |
| Product Usability Progress | 38%-48% | Dashboard and review-only displays exist. | Core actions still do not form a coherent review-only MVP workflow. |
| Execution Advice Progress | 30%-40% | ExecutionPlan review-only display and entry/stop/TP/RR design/test groundwork exist. | Runtime source-owned proposal generation remains incomplete. |
| Push / Monitoring Progress | 42%-55% | Push no-op/audit/channel skeletons and legacy position monitor foundations exist. | No external send, no full internal push preview chain, and no complete monitor action loop. |
| AI Arbitration Progress | 25%-35% | Role names and heuristic conflict logic exist. | Real GPT/Gemini/Grok orchestration, budget/cache/rate limits, fallback, and conflict downgrade closure are incomplete. |

## Progress Rules

Docs-only packages may improve Governance / Contract Progress, but must not significantly raise Production Runtime Progress.

Skeleton packages may improve Skeleton / Test Progress, but must not be described as production wiring.

Open PRs, branches, Issues, and draft work must not be counted as completed.

Legacy runtime `MarketQuoteClient` / `BinanceMarketQuoteClient` capability must not be treated as completion of the new scan-chain market-read request path.

## Current Capability Summary

The current project is strong in guardrails and skeletons but weaker in end-to-end product usefulness.

The next useful upgrades should move modules from:

- `TARGETED_TEST` to `TEST_ONLY_WIRING`;
- `TEST_ONLY_WIRING` to `REVIEW_ONLY_RUNTIME`;
- broad blocked states to allowed review-only downgrade outputs.

## Business Chain Priority

Use `docs/V1_MVP_REALITY_ROADMAP.md` as the roadmap.

Near-term priority after P291A:

1. review-only MarketRead output / scan output.
2. MarketReadRequest production-unsafe assembler remains blocked; only review-only/test-only assembly may proceed next.
3. review-only market read adapter path.
4. review-only ScanScore.
5. review-only Candidate.
6. internal Opportunity Push preview and Push Recheck.
7. review-only Execution Advice and entry / stop / TP / RR proposal.
8. manual position entry and monitor suggestions.
9. AI conflict downgrade and recovery conditions.
10. dashboard MVP smoke.
11. review / missed-valid logging.

## Blocked Capability Reference

Do not repeat long blocked lists in every new scope pack.

Reference `docs/V1_BLOCKED_CAPABILITY_REGISTRY.md` unless a package changes a specific boundary.

Blocked does not mean no useful review-only output. It means no automatic execution, no unauthorized production wiring, and no external send.
