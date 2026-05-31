# Project Progress Index

This index uses fixed progress口径. It does not count P-package quantity as progress.

Completion is based on merged `main` only.

Current merged main:

- `58f69ef BACKEND-P291F Active Mainline Status Refresh Pack (#719)`
- `a61a86b BACKEND-P294 Review-Only MarketRead Output and Scan Output Slice (#713)`

Current active capability movement:

- `BACKEND-P295 Review-Only Scan Output to Evidence / Score Entry Slice` in PR #721 / branch `p295`, pending merge.
- The active mainline is Evidence / Score Mainline.
- The active block is Review-only Scan Output to Evidence / Score Entry.

P291D and P291E are merged on main. They add workflow command automation and a one-command runner, but they do not raise Market Read business-chain capability or Production Runtime Progress.

P291F is merged on main. It refreshes the active mainline status after P294, but it does not raise business-chain runtime progress.

P292 is merged on main. It moved `MarketReadRequest test-only wiring` to `4 TEST_ONLY_WIRING`.

P293 is merged on main. It moved the MarketReadRequest path from `TEST_ONLY_WIRING` toward `REVIEW_ONLY_OUTPUT_SKELETON` by turning guard validation results into a readable review-only output DTO.

P294 is merged on main. It moved the MarketRead path to `REVIEW_ONLY_SCAN_OUTPUT_SKELETON` by turning `MarketReadReviewOnlyOutputDTO` into a safe review-only scan output skeleton.

P295 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_SCAN_OUTPUT_SKELETON` toward `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` by turning review-only scan output into a safe evidence / score entry envelope.

P295 does not raise Production Runtime Progress. It is not real evidence generation, score calculation, production scan output, Candidate, Push, Readiness, point generation, or trading behavior.

P291C does not raise feature progress. It enforces session bootstrap, answer format, PR template, workflow contract script, and CI workflow-contract check.

Current active mainline status is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.

## Fixed Progress Percentages

| Progress view | Current range | Why this range | Why it cannot be higher yet |
|---|---:|---|---|
| Total Progress | 58%-64% | Many review-only displays, contracts, DTOs, validators, no-op skeletons, workflow automation, and safety rules exist. | The full V1 chain still lacks completed evidence normalization -> score -> candidate -> push preview -> execution advice -> monitor -> review closure. |
| MVP Progress | 55%-63% | Watchlist/display/review surfaces, skeletons, the MarketReadRequest DTO -> GuardValidator test-only wiring slice, P293 review-only output assembler, P294 review-only scan output skeleton, and active P295 evidence / score entry skeleton exist. | Evidence normalization, real score calculation, Candidate, Push, Readiness, point generation, and the user-facing MVP loop are not complete. |
| Production Runtime Progress | 28%-36% | Some legacy runtime components exist, including market clients, schedulers, dashboard services, and position foundations. | P294, P295, P291D, P291E, and P291F do not add production wiring; the new scan-chain production runtime is not wired, and push/readiness/point/trading paths remain blocked. |
| Governance / Contract Progress | 88%-94% | Boundaries, gates, fail-closed rules, no-trade semantics, review-only policy, P291D command automation, and P291E one-command runner are extensive. | Future windows still need to use the automation consistently. |
| Skeleton / Test Progress | 75%-83% | DTO, validator, no-op, audit, queue, channel, score, candidate, market-read request skeletons/tests, MarketReadRequest test-only wiring, review-only scan output skeleton, and active P295 evidence / score entry skeleton exist. | Evidence normalization and real score calculation are not complete. |
| Product Usability Progress | 40%-50% | Dashboard and review-only displays exist, and MarketRead review-only scan output now has a safe entry envelope in P295. | Core actions still do not form a coherent review-only MVP workflow. |
| Execution Advice Progress | 30%-40% | ExecutionPlan review-only display and entry/stop/TP/RR design/test groundwork exist. | Runtime source-owned proposal generation remains incomplete. |
| Push / Monitoring Progress | 42%-55% | Push no-op/audit/channel skeletons and legacy position monitor foundations exist. | No external send, no full internal push preview chain, and no complete monitor action loop. |
| AI Arbitration Progress | 25%-35% | Role names and heuristic conflict logic exist. | Real GPT/Gemini/Grok orchestration, budget/cache/rate limits, fallback, and conflict downgrade closure are incomplete. |

## Progress Rules

Docs-only packages may improve Governance / Contract Progress, but must not significantly raise Production Runtime Progress.

Skeleton packages may improve Skeleton / Test Progress, but must not be described as production wiring.

Open PRs, branches, Issues, and draft work must not be counted as completed.

Codex output must not be counted as completed.

Legacy runtime `MarketQuoteClient` / `BinanceMarketQuoteClient` capability must not be treated as completion of the new scan-chain market-read request path.

P294 must not be described as production scan output, score, Evidence, Candidate, Push, Readiness, point generation, or production market read.

P295 must not be described as real evidence generation or score calculation.

Evidence generation / Score calculation must not be described as completed until a separate merged package adds that layer.

Candidate, Push, Readiness, and point generation must not be described as completed.

## Current Capability Summary

The current project is strong in guardrails and skeletons but weaker in end-to-end product usefulness.

The next useful upgrades should move modules from:

- `TARGETED_TEST` to `TEST_ONLY_WIRING`;
- `TEST_ONLY_WIRING` to `REVIEW_ONLY_RUNTIME`;
- broad blocked states to allowed review-only downgrade outputs.

## Business Chain Priority

Use `docs/V1_MVP_REALITY_ROADMAP.md` as the roadmap.

Use `docs/SESSION_BOOTSTRAP.md` at every new window.

Near-term priority after P295:

1. Evidence normalization review-only slice.
2. review-only ScanScore over normalized evidence / entry output.
3. review-only Candidate.
4. internal Opportunity Push preview and Push Recheck.
5. review-only Execution Advice and entry / stop / TP / RR proposal.
6. manual position entry and monitor suggestions.
7. AI conflict downgrade and recovery conditions.
8. dashboard MVP smoke.
9. review / missed-valid logging.

## Blocked Capability Reference

Do not repeat long blocked lists in every new scope pack.

Reference `docs/V1_BLOCKED_CAPABILITY_REGISTRY.md` unless a package changes a specific boundary.

Blocked does not mean no useful review-only output. It means no automatic execution, no unauthorized production wiring, and no external send.
