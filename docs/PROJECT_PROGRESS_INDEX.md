# Project Progress Index

This index uses fixed progress口径. It does not count P-package quantity as progress.

Completion is based on merged `main` only.

Current merged main:

- `9e060a3 BACKEND-P307 Review-only Readiness Gate Skeleton (#749)`
- `78d4f83 BACKEND-P306 Readiness / Point Boundary Planning Gate (#747)`
- `ffaf52b BACKEND-P305 Candidate / Push Review-Only MVP Closure (#745)`
- `86954dd BACKEND-P304 Dashboard / Internal Push Preview Display Gate (#743)`
- `db5e38a BACKEND-P303 Push Preview Closure Before External Channel (#741)`
- `4de4905 BACKEND-P302 Internal Push Preview / Recheck Handoff Review-Only Slice (#739)`
- `819c17d BACKEND-P301 Candidate Preview / Ranking Guard Review-Only Slice (#737)`
- `bf14ec0 BACKEND-P300 Candidate Attention Review-Only Slice (#735)`
- `4b54233 BACKEND-P299 Score-to-Candidate Handoff Review-Only Slice (#733)`
- `ad3c045 BACKEND-P298 Review-Only Score Assembly Slice (#731)`
- `95760cb BACKEND-P297 Score Input / Precheck Review-Only Slice (#729)`
- `8665c24 BACKEND-P296 Evidence Normalization Review-Only Slice (#727)`
- `69440a7 BACKEND-P291H GitHub-Native Workflow Simplification Pack (#725)`
- `24e120b BACKEND-P295 Review-Only Scan Output to Evidence / Score Entry Slice (#721)`
- `2efdd6b BACKEND-P291G Workflow Auto-Decision Runner Pack (#723)`
- `58f69ef BACKEND-P291F Active Mainline Status Refresh Pack (#719)`
- `ba9cd2c BACKEND-P291E Workflow One-Command Runner Pack (#717)`
- `a61a86b BACKEND-P294 Review-Only MarketRead Output and Scan Output Slice (#713)`

Current active capability movement:

- P295 is merged on main.
- It moved the chain from `REVIEW_ONLY_SCAN_OUTPUT_SKELETON` to `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON`.
- P296 is merged on main.
- It moved the chain from `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` to `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON`.
- P297 is merged on main.
- It moved the chain from `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` to `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON`.
- P298 is merged on main.
- It moved the chain from `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` to `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON`.
- P299 is merged on main.
- It moved the chain from `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` to `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON`.
- P300 is merged on main.
- It moved the chain from `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` to `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON`.
- P301 is merged on main.
- It moved the chain from `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` to `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON`.
- P302 is merged on main.
- It moved the chain from `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` to `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON`.
- P303 is merged on main.
- It moved the chain from `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON` to `REVIEW_ONLY_PUSH_PREVIEW_CLOSURE`.
- P304 is merged on main.
- It moved the chain from `REVIEW_ONLY_PUSH_PREVIEW_CLOSURE` to `DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE`.
- P305 is merged on main.
- It closed the P299-P304 chain from `DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE` to `CANDIDATE_PUSH_REVIEW_ONLY_MVP_CLOSURE`.
- P306 is merged on main.
- It moved the chain from `CANDIDATE_PUSH_REVIEW_ONLY_MVP_CLOSURE` to `READINESS_POINT_BOUNDARY_PLAN`.
- P307 is merged on main.
- It moved the chain from `READINESS_POINT_BOUNDARY_PLAN` to `REVIEW_ONLY_READINESS_GATE_SKELETON`.
- `BACKEND-P308 Review-only Point Boundary / Proposal Gate` is in PR #751 / branch `p308`, pending merge.
- P308 is a review-only point boundary gate skeleton from `REVIEW_ONLY_READINESS_GATE_SKELETON` toward `REVIEW_ONLY_POINT_BOUNDARY_GATE_SKELETON`.
- The active mainline is Readiness / Point Mainline.
- The active block is Review-only Point Boundary / Proposal Gate.
- The next required action is `review_pr_751`.

P291D, P291E, P291F, P291G, and P291H are workflow/source-of-truth packages. They do not raise Market Read business-chain capability or Production Runtime Progress.

P292 is merged on main. It moved `MarketReadRequest test-only wiring` to `4 TEST_ONLY_WIRING`.

P293 is merged on main. It moved the MarketReadRequest path from `TEST_ONLY_WIRING` toward `REVIEW_ONLY_OUTPUT_SKELETON` by turning guard validation results into a readable review-only output DTO.

P294 is merged on main. It moved the MarketRead path to `REVIEW_ONLY_SCAN_OUTPUT_SKELETON` by turning `MarketReadReviewOnlyOutputDTO` into a safe review-only scan output skeleton.

P295 is merged on main. It moved the chain to `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` by turning review-only scan output into a safe Evidence / Score entry envelope.

P295 does not raise Production Runtime Progress. It is not real evidence generation, score calculation, production scan output, Candidate, Push, Readiness, point generation, or trading behavior.

P296 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` toward `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` by turning review-only Evidence / Score entry into safe normalized evidence skeleton output.

P296 does not raise Production Runtime Progress. It is not real evidence generation, persisted evidence, score calculation, production scan output, Candidate, Push, Readiness, point generation, or trading behavior.

P297 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` toward `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` by turning review-only normalized evidence into safe score input / precheck skeleton output.

P297 does not raise Production Runtime Progress. It is not real ScoreItem generation, score calculation, production scan output, Candidate, Push, Readiness, point generation, or trading behavior.

P298 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` toward `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` by turning review-only score input precheck into safe score assembly skeleton output.

P298 does not raise Production Runtime Progress. It is not real ScoreItem generation, score calculation, final score, direction, Candidate, Push, Readiness, point generation, or trading behavior.

P299 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` toward `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` by turning review-only score assembly into safe candidate handoff skeleton output.

P299 does not raise Production Runtime Progress. It is not real Candidate generation, Candidate Attention production workflow, Promote To Home runtime logic, Push, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P300 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` toward `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` by turning review-only candidate handoff into safe candidate attention skeleton output.

P300 does not raise Production Runtime Progress. It is not real Candidate generation, candidate rank, candidate score, Promote To Home runtime logic, Push, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P301 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` toward `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` by turning review-only candidate attention into safe candidate preview / ranking guard skeleton output.

P301 does not raise Production Runtime Progress. It is not real Candidate generation, candidate rank, candidate score, real ranking result, Promote To Home runtime logic, Push, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P302 is capability movement, not closure-only. It moves the chain from `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` toward `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON` by turning review-only candidate preview guard into safe internal push preview / recheck handoff skeleton output.

P302 does not raise Production Runtime Progress. It is not real Push, external channel behavior, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P303 is push preview closure capability movement. It moves the chain from `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON` toward `REVIEW_ONLY_PUSH_PREVIEW_CLOSURE` by closing internal push preview as non-sendable, recheck-required, and Risk Action Guard required before any external channel.

P303 does not raise Production Runtime Progress. It is not real Push, external channel authorization, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P304 is dashboard / internal preview display gate capability movement. It moves the chain from `REVIEW_ONLY_PUSH_PREVIEW_CLOSURE` toward `DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE` by showing internal push preview safety state on the dashboard.

P304 may only make a small Candidate / Push Mainline, MVP chain, and Product Usability lift. It does not raise Production Runtime Progress and is not real Push, external channel authorization, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P305 is Candidate / Push review-only MVP closure. It closes the P299-P304 chain from review-only score assembly through candidate handoff, candidate attention, candidate preview guard, internal push preview, and dashboard/internal display gate.

P305 may only make a small Candidate / Push Mainline, MVP chain, and Product Usability lift. It does not raise Production Runtime Progress and is not real Candidate, real Push, external channel authorization, Telegram/email/webhook/app/local notification, sendable message rendering, message sending, Readiness, point generation, entry / stop / TP / RR, or trading behavior.

P306 is a Readiness / Point planning gate, not runtime capability. It defines the boundary rules before Review-only Readiness Gate and Review-only Point Proposal work.

P306 may only make a small Governance / Contract Progress lift. It does not raise Production Runtime Progress and must not describe Readiness, point generation, entry / stop / TP / RR, external channel, order execution, or auto-trading as completed.

P307 is a review-only readiness gate skeleton. It moves internal push preview output into a non-executable readiness gate output while preserving recheck-required, Risk Action Guard required, review-only, manual-review, and not-trade-instruction flags.

P307 may only make a small Readiness / Point Mainline and MVP chain lift. It does not raise Production Runtime Progress and must not describe point generation, entry / stop / TP / RR, external channel, order execution, execution API, or auto-trading as completed.

P308 is a review-only point boundary gate skeleton. It moves review-only readiness gate output into a non-executable point boundary gate output while preserving recheck-required, Risk Action Guard required, review-only, manual-review, and not-trade-instruction flags.

P308 may only make a small Readiness / Point Mainline and MVP chain lift. It does not raise Production Runtime Progress and must not describe executable point generation, entry / stop / TP / RR, external channel, order execution, execution API, or auto-trading as completed.

P291H is workflow simplification. It changes workflow priority to GitHub-native first and terminal scripts fallback only. It does not raise business-chain runtime progress.

Current active mainline status is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.

## Fixed Progress Percentages

| Progress view | Current range | Why this range | Why it cannot be higher yet |
|---|---:|---|---|
| Total Progress | 58%-64% | Many review-only displays, contracts, DTOs, validators, no-op skeletons, workflow automation, and safety rules exist. | The full V1 chain still lacks source-owned point proposal -> execution advice -> monitor -> review closure. |
| MVP Progress | 63%-71% | Watchlist/display/review surfaces, skeletons, the MarketReadRequest DTO -> GuardValidator test-only wiring slice, P293 review-only output assembler, P294 review-only scan output skeleton, P295 evidence / score entry skeleton, P296 evidence normalization skeleton, P297 score input / precheck skeleton, P298 score assembly skeleton, P299 candidate handoff skeleton, P300 candidate attention skeleton, P301 candidate preview guard skeleton, P302 internal push preview skeleton, P303 push preview closure, P304 dashboard display gate, P305 review-only MVP closure, P306 planning, P307 review-only readiness gate skeleton, and active P308 point boundary gate skeleton exist. | Real Push, external channel, executable Readiness, executable point generation, and the user-facing MVP loop are not complete. |
| Production Runtime Progress | 28%-36% | Some legacy runtime components exist, including market clients, schedulers, dashboard services, and position foundations. | P294-P304 and workflow packs do not add production wiring; the new scan-chain production runtime is not wired, and push/readiness/point/trading paths remain blocked. |
| Governance / Contract Progress | 90%-96% | Boundaries, gates, fail-closed rules, no-trade semantics, review-only policy, command automation, one-command runner, auto-decision diagnostics, GitHub-native workflow rules, and active P306 readiness / point planning rules are extensive. | Future windows still need to follow GitHub-native workflow, stale PR / Issue hygiene, and the P306 readiness / point boundary. |
| Skeleton / Test Progress | 84%-92% | DTO, validator, no-op, audit, queue, channel, score, candidate, market-read request skeletons/tests, MarketReadRequest test-only wiring, review-only scan output skeleton, P295 evidence / score entry skeleton, P296 evidence normalization skeleton, P297 score input / precheck skeleton, P298 score assembly skeleton, P299 candidate handoff skeleton, P300 candidate attention skeleton, P301 candidate preview guard skeleton, P302 internal push preview skeleton, P303 closure guard tests, P304 dashboard guard tests, P305 closure tests, P307 readiness gate skeleton tests, and active P308 point boundary gate skeleton tests exist. | Real Push / external channel workflow, executable point generation, and executable readiness are not complete. |
| Product Usability Progress | 42%-52% | Dashboard and review-only displays exist, MarketRead review-only scan output now has a safe entry envelope after P295, and P304 makes internal push preview safety visible in the dashboard while P305 closes the read-only candidate/push MVP loop. | Core actions still do not form an executable MVP workflow, and external send remains blocked. |
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

P296 must not be described as real evidence generation, persisted evidence, or score calculation.

P297 must not be described as real scoring, ScoreItem generation, or score calculation.

P298 must not be described as real scoring, ScoreItem generation, score calculation, final score, direction, or Candidate handoff completion.

P299 must not be described as real Candidate, Candidate Attention, Promote To Home, Push, Readiness, or point generation.

P300 must not be described as real Candidate, candidate rank, candidate score, Promote To Home, Push, Readiness, or point generation.

P301 must not be described as real Candidate, candidate rank, candidate score, real ranking result, Promote To Home, Push, Readiness, or point generation.

P302 must not be described as real Push, external channel behavior, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

P303 must not be described as external channel authorization, real Push, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

P304 must not be described as external channel authorization, real Push, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

P305 must not be described as real Candidate, real Push, external channel authorization, sendable message rendering, message sending, Readiness, point generation, or trading behavior.

P306 must not be described as real Readiness, point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

P307 must not be described as executable Readiness, point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

P308 must not be described as executable point generation, entry / stop / TP / RR, external channel authorization, order execution, execution API, or auto-trading.

Evidence generation, ScoreItem generation, and Score calculation must not be described as completed until separate merged packages add those layers.

Real Candidate, real Push, executable Readiness, external channel, and point generation must not be described as completed.

## Current Capability Summary

The current project is strong in guardrails and skeletons but weaker in end-to-end product usefulness.

The next useful upgrades should move modules from:

- `TARGETED_TEST` to `TEST_ONLY_WIRING`;
- `TEST_ONLY_WIRING` to `REVIEW_ONLY_RUNTIME`;
- broad blocked states to allowed review-only downgrade outputs.

## Business Chain Priority

Use `docs/V1_MVP_REALITY_ROADMAP.md` as the roadmap.

Use `docs/SESSION_BOOTSTRAP.md` at every new window.

Near-term priority after P308:

1. Source-owned Review-only Point Proposal Skeleton.
2. External Channel Authorization Gate only as a separate C-level package.
3. Readiness / Point specialty planning follow-up before any point generation.
4. Dashboard smoke / internal preview closure.
5. Review-only Execution Advice and entry / stop / TP / RR proposal only after source-owned gates.
6. manual position entry and monitor suggestions.
7. AI conflict downgrade and recovery conditions.
8. review / missed-valid logging.

## Blocked Capability Reference

Do not repeat long blocked lists in every new scope pack.

Reference `docs/V1_BLOCKED_CAPABILITY_REGISTRY.md` unless a package changes a specific boundary.

Blocked does not mean no useful review-only output. It means no automatic execution, no unauthorized production wiring, and no external send.
