# Project Progress Index

This index fixes progress口径 for Trade Model V1.

Current merged `main` at P291B creation:

- `26e8943 BACKEND-P291A Workflow Reset and Progress Source of Truth Pack (#703)`

Open PR #705 P292 is not counted as completed until merged.

## Fixed Progress Percentages

| Progress view | Current range | Counts | Does not count |
|---|---:|---|---|
| Total Progress | 58%-64% | merged contracts, review-only displays, DTOs, validators, no-op skeletons, safety rules | open PRs, production market-read not yet wired, full MVP loop incomplete |
| MVP Progress | 52%-60% | Watchlist/display/review surfaces and several review-only skeletons | P292 until merged, scan output/score/Candidate/Push/Execution Advice loop incomplete |
| Production Runtime Progress | 28%-36% | legacy runtime components, legacy market clients, legacy schedulers, dashboard services, position foundations | new scan-chain production market read, production Push, production Readiness, production points |
| Governance / Contract Progress | 87%-93% | source-of-truth rules, gates, fail-closed policy, no-trade semantics, drift guard | production behavior |
| Skeleton / Test Progress | 72%-80% | DTOs, validators, no-op skeletons, targeted tests | P292 test-only wiring until merged, runtime behavior |
| Product Usability Progress | 38%-48% | dashboard/review surfaces and review-only display patterns | docs-only and skeleton work without user-visible output |
| Execution Advice Progress | 30%-40% | ExecutionPlan review-only display and entry/stop/TP/RR design/test groundwork | runtime review-only proposal chain and production point generation |
| Push / Monitoring Progress | 42%-55% | push no-op/audit/channel skeletons and legacy monitor foundations | external send, full internal preview chain, monitor action loop |
| AI Arbitration Progress | 25%-35% | role names and heuristic conflict logic | real GPT/Gemini/Grok orchestration, budget/cache/rate limits, provider fallback |

## Progress Movement Rules

Docs-only work may raise Governance / Contract Progress only slightly.

Docs-only work must not significantly raise Product Usability Progress or Production Runtime Progress.

Skeleton work may raise Skeleton / Test Progress only.

Skeleton must not be written as production wiring.

Targeted test must not be written as runtime behavior.

Test-only wiring does not equal production.

Review-only runtime can meaningfully raise MVP Progress when it improves user-visible safe output.

Production wiring can raise Production Runtime Progress only when a real runtime path is merged and bounded.

## Current Main Interpretation

P291A merged source-of-truth structure, but P291B is needed to fill it operationally.

P292 is open and should remain excluded from progress until merged.

The next capability movement after P291B should be P292 review/merge if the PR remains valid.

## Business Chain Priority

Use `docs/V1_MVP_REALITY_ROADMAP.md`.

Do not use P-package count as progress.

Do not repeat closure-only packages when a capability-level movement is available.
