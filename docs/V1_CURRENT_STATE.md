# V1 Current State

This file is a source-of-truth summary. Completion is based only on merged `main`.

## Current Main

- Source branch baseline: `main`
- Current merged main: `4840a07 BACKEND-P292 MarketReadRequest Test-Only Wiring and Review-Only Assembler Slice (#705)`
- Current open workflow package in PR #711 / branch `p293`: `BACKEND-P293 MarketReadRequest Review-Only Output Assembler Slice`
- Current active mainline is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.
- P291A, P291C, and P292 are complete on main. P293 is the active Market Read block and does not count as merged main completion until its PR is merged.

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

## What P287-P292 Actually Completed

P287-P292 completed market-read request contract, DTO, validator, and test-only wiring groundwork only:

- P287: docs-only authorization gate for future `MarketReadRequestDTO`.
- P288: pure-data `MarketReadRequestDTO` skeleton plus targeted DTO test.
- P289: docs-only closure and authorization for future guard validator.
- P290: `MarketReadRequestGuardValidator` skeleton, validation result/status DTOs, and targeted validator test.
- P291A: workflow reset, progress source of truth, capability matrix, allowed review-only outputs, blocked capability registry, MVP reality roadmap, and drift guard checklist.
- P292: test-only `MarketReadRequestDTO` -> `MarketReadRequestGuardValidator` wiring and review-only validation output.

These packages are DTO / validator / skeleton / targeted-test / test-only wiring work.

They are not production market-read wiring.

They do not connect `MarketQuoteClient` / `BinanceMarketQuoteClient` into the new scan-chain.

They do not create runtime market reads, scan output, real scan loop, production ScanScore, production Candidate workflow, Opportunity Push execution, Readiness, point generation, order execution, or auto-trading.

## Current P293 Scope

P293 adds a review-only output assembler for MarketReadRequest.

P293 turns `MarketReadRequestDTO` plus `MarketReadRequestGuardValidationResult` into `MarketReadReviewOnlyOutputDTO`, so the validation result can be read as a manual-review output instead of remaining only a validator result.

P293 is not production market read.

P293 does not connect `MarketQuoteClient` / `BinanceMarketQuoteClient`.

P293 does not create scan output, score, Candidate, Push, Readiness, point generation, order execution, execution API, or auto-trading.

## MarketReadRequest Current Capability

- `MarketReadRequestDTO`: `3 TARGETED_TEST`
- `MarketReadRequestGuardValidator`: `3 TARGETED_TEST`
- `MarketReadRequest test-only wiring`: `4 TEST_ONLY_WIRING`
- `MarketReadRequest review-only output assembler`: `REVIEW_ONLY_OUTPUT_SKELETON` in active PR #711, not production assembler

## What Is Still Not Completed

The following remain incomplete for the new MVP chain:

- production MarketReadRequest assembler.
- scan-chain market read adapter connected to authorized review-only inputs.
- production/runtime market read for the new scan-chain.
- scan output from live market data.
- review-only ScanScore over real scan output.
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

Those legacy capabilities must not be described as completion of the P287-P290 market-read request scan-chain.

Any use of legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` in the new scan-chain requires a separate authorization package.

## Review-Only Output Clarification

P291A restores the principle that review-only does not mean no output.

The system may produce safe manual-review proposals, such as entry zone proposal, stop zone proposal, TP proposal, RR estimate, position size suggestion, leverage cap suggestion, invalidation condition, reduce-position suggestion, tighten-stop suggestion, move-stop suggestion, partial take-profit suggestion, wait-for-trigger state, plan-invalidated state, internal push preview, risk-downgraded candidate, and confused-with-recovery-condition state.

Those outputs must remain non-executable, manual-review required, and not trade instructions.

Automatic order, close, reverse, leverage change, execution, and auto-trading remain blocked.

## Current Recommendation

Do not describe P292 or P293 as production market read.

After P291C, new windows must follow `docs/SESSION_BOOTSTRAP.md` and answer with `docs/ANSWER_FORMAT_CONTRACT.md`.

After P293 merges, continue by business-chain MAX_SAFE_PACK into review-only MarketRead output / scan output.

Do not continue to P294 while the P293 PR remains open.
