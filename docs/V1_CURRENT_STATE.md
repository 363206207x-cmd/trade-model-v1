# V1 Current State

This file is a source-of-truth summary. Completion is based only on merged `main`.

## Current Main

- Source branch baseline: `main`
- Current merged main: `06b5cfe BACKEND-P293 MarketReadRequest Review-Only Output Assembler Slice (#711)`
- Current workflow automation package in PR #715 / branch `p291d`: `BACKEND-P291D Workflow Command Automation Pack`
- Current active mainline is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.
- P291A, P291C, P292, and P293 are complete on main.
- P291D is scripts/docs automation only. It does not advance the Market Read business chain.

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

## What P287-P293 Actually Completed

P287-P293 completed market-read request contract, DTO, validator, test-only wiring, and review-only output groundwork only:

- P287: docs-only authorization gate for future `MarketReadRequestDTO`.
- P288: pure-data `MarketReadRequestDTO` skeleton plus targeted DTO test.
- P289: docs-only closure and authorization for future guard validator.
- P290: `MarketReadRequestGuardValidator` skeleton, validation result/status DTOs, and targeted validator test.
- P291A: workflow reset, progress source of truth, capability matrix, allowed review-only outputs, blocked capability registry, MVP reality roadmap, and drift guard checklist.
- P292: test-only `MarketReadRequestDTO` -> `MarketReadRequestGuardValidator` wiring and review-only validation output.
- P293: review-only output assembler from `MarketReadRequestDTO` + guard result to `MarketReadReviewOnlyOutputDTO`.

These packages are DTO / validator / skeleton / targeted-test / test-only wiring work.

They are not production market-read wiring.

They do not connect `MarketQuoteClient` / `BinanceMarketQuoteClient` into the new scan-chain.

They do not create runtime market reads, scan output, real scan loop, production ScanScore, production Candidate workflow, Opportunity Push execution, Readiness, point generation, order execution, or auto-trading.

## Current P291D Scope

P291D adds workflow command automation.

P291D adds scripts for status checks, PR review input, merge-sync after explicit approval, safe checks, session bootstrap, and next-pack context.

P291D is not business-chain progress.

P291D does not modify Java, tests, DTOs, dashboard, schema, config, resources, services, controllers, mappers, repositories, schedulers, APIs, market clients, runtime reads, scan output, score, Candidate, Push, Readiness, point generation, order execution, execution API, or auto-trading.

Use `docs/WORKFLOW_COMMAND_AUTOMATION.md` for command usage.

## MarketReadRequest Current Capability

- `MarketReadRequestDTO`: `3 TARGETED_TEST`
- `MarketReadRequestGuardValidator`: `3 TARGETED_TEST`
- `MarketReadRequest test-only wiring`: `4 TEST_ONLY_WIRING`
- `MarketReadRequest review-only output assembler`: `REVIEW_ONLY_OUTPUT_SKELETON`, completed after P293, not production assembler

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

Do not describe P291D as Market Read business-chain progress.

After P291C, new windows must follow `docs/SESSION_BOOTSTRAP.md` and answer with `docs/ANSWER_FORMAT_CONTRACT.md`.

After P291D merges, use the command automation scripts for daily workflow and continue the business chain only through a separately authorized package.

Do not continue to P294 inside P291D.
