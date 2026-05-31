# V1 Current State

This file is a source-of-truth summary. Completion is based only on merged `main`.

## Current Main

- Source branch baseline: `main`
- Current merged main: `58f69ef BACKEND-P291F Active Mainline Status Refresh Pack (#719)`
- Market Read Mainline has completed through `a61a86b BACKEND-P294 Review-Only MarketRead Output and Scan Output Slice (#713)`.
- Current active mainline is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.
- Current open business-chain package is PR #721 / branch `p295`: `BACKEND-P295 Review-Only Scan Output to Evidence / Score Entry Slice`.
- P295 is a review-only Evidence / Score entry slice. It is not real score calculation and does not count as merged main completion until its PR is merged.

## Source-Of-Truth Rule

Only merged `main` counts as completed.

Open Issues, open PRs, local branches, draft PRs, and chat memory do not count as completed progress.

Progress must be read together with:

- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
- `docs/V1_CAPABILITY_MATRIX.md`
- `docs/PROJECT_PROGRESS_INDEX.md`
- `docs/V1_MVP_REALITY_ROADMAP.md`
- `docs/ACTIVE_MAINLINE_STATUS.yml`
- `docs/SESSION_BOOTSTRAP.md`

## What P287-P294 Actually Completed

P287-P294 completed market-read request contract, DTO, validator, test-only wiring, review-only output, and review-only scan output skeleton only:

- P287: docs-only authorization gate for future `MarketReadRequestDTO`.
- P288: pure-data `MarketReadRequestDTO` skeleton plus targeted DTO test.
- P289: docs-only closure and authorization for future guard validator.
- P290: `MarketReadRequestGuardValidator` skeleton, validation result/status DTOs, and targeted validator test.
- P291A: workflow reset, progress source of truth, capability matrix, allowed review-only outputs, blocked capability registry, MVP reality roadmap, and drift guard checklist.
- P292: test-only `MarketReadRequestDTO` -> `MarketReadRequestGuardValidator` wiring and review-only validation output.
- P293: review-only output assembler from `MarketReadRequestDTO` + guard result to `MarketReadReviewOnlyOutputDTO`.
- P294: review-only scan output skeleton from `MarketReadReviewOnlyOutputDTO` to `MarketReadReviewOnlyScanOutputDTO`.

These packages are DTO / validator / skeleton / targeted-test / test-only wiring / review-only output work.

They are not production market-read wiring.

They do not connect `MarketQuoteClient` / `BinanceMarketQuoteClient` into the new scan-chain.

They do not create runtime market reads, production scan output, real scan loop, production ScanScore, Evidence generation, production Candidate workflow, Opportunity Push execution, Readiness, point generation, order execution, or auto-trading.

## Current Workflow Automation Scope

P291D is merged on main and adds workflow command automation.

P291E is merged on main and adds `bash scripts/v1.sh` as the single terminal entry point.

P291F is merged on main and refreshes the active mainline to Evidence / Score Mainline after P294.

These workflow/source-of-truth packages are not production runtime progress.

Use `docs/WORKFLOW_COMMAND_AUTOMATION.md` for command usage.

## Current P295 Scope

P295 is open on branch `p295` and adds a review-only Evidence / Score entry skeleton pending merge.

P295 turns `MarketReadReviewOnlyScanOutputDTO` into `ReviewOnlyEvidenceScoreEntryDTO`, so review-only scan output can become a safe manual-review entry envelope for later evidence / score work.

P295 is not real evidence generation.

P295 is not score calculation.

P295 does not calculate the eight score dimensions.

P295 does not connect `MarketQuoteClient` / `BinanceMarketQuoteClient`.

P295 does not create production scan output, real EvidenceItem, real ScoreItem, Candidate, Push, Readiness, point generation, order execution, execution API, or auto-trading.

## Current Next Mainline

The current mainline is Evidence / Score Mainline.

The current block is Review-only Scan Output to Evidence / Score Entry.

Evidence / Score entry is in P295 only and is not completed on main until the PR merges.

Evidence generation and score calculation are not completed.

Candidate, Push, Readiness, point generation, order execution, execution API, and auto-trading are not completed.

## MarketReadRequest Current Capability

- `MarketReadRequestDTO`: `3 TARGETED_TEST`
- `MarketReadRequestGuardValidator`: `3 TARGETED_TEST`
- `MarketReadRequest test-only wiring`: `4 TEST_ONLY_WIRING`
- `MarketReadRequest review-only output assembler`: `REVIEW_ONLY_OUTPUT_SKELETON`, completed after P293, not production assembler
- `Review-only MarketRead scan output`: `REVIEW_ONLY_SCAN_OUTPUT_SKELETON`, completed after P294, not production scan output
- `Review-only Evidence / Score entry`: `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` pending / completed in P295 PR, not real evidence generation or score calculation

## What Is Still Not Completed

The following remain incomplete for the new MVP chain:

- production MarketReadRequest assembler.
- scan-chain market read adapter connected to authorized review-only inputs.
- production/runtime market read for the new scan-chain.
- production scan output from live market data.
- Evidence normalization over review-only entry output.
- Evidence generation.
- Score calculation over real evidence.
- production ScanScore over live scan output.
- review-only Candidate from scored scan output.
- internal Opportunity Push preview from candidate output.
- Push Recheck integration for preview expiry/drift handling.
- review-only Execution Advice from a complete candidate chain.
- runtime entry / stop / TP / RR proposal chain.
- manual position entry to monitoring loop closure.
- position-monitor downgrade/action suggestion loop.
- real GPT / Gemini / Grok arbitration.
- missed-valid opportunity logging and feedback loop.
- dashboard MVP smoke over the full review-only chain.

## Legacy Runtime Clarification

Legacy market and monitor components exist in the repository, including market clients, dashboard services, scheduled recheck, and position monitoring foundations.

Those legacy capabilities must not be described as completion of the P287-P294 market-read request scan-chain.

Any use of legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` in the new scan-chain requires a separate authorization package.

## Review-Only Output Clarification

P291A restores the principle that review-only does not mean no output.

The system may produce safe manual-review proposals, such as entry zone proposal, stop zone proposal, TP proposal, RR estimate, position size suggestion, leverage cap suggestion, invalidation condition, reduce-position suggestion, tighten-stop suggestion, move-stop suggestion, partial take-profit suggestion, wait-for-trigger state, plan-invalidated state, internal push preview, risk-downgraded candidate, and confused-with-recovery-condition state.

Those outputs must remain non-executable, manual-review required, and not trade instructions.

Automatic order, close, reverse, leverage change, execution, and auto-trading remain blocked.

## Current Recommendation

Use `bash scripts/v1.sh` for daily workflow.

Review P295 before any merge decision.

After P295 merges, continue to an Evidence normalization review-only slice.

Do not describe P295 as real evidence generation or real score calculation.

Do not describe Candidate, Push, Readiness, or point generation as completed.
