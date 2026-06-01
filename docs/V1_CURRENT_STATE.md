# V1 Current State

This file is a source-of-truth summary. Completion is based only on merged `main`.

## Current Main

- Source branch baseline: `main`
- Current merged main: `bf14ec0 BACKEND-P300 Candidate Attention Review-Only Slice (#735)`
- Evidence / Score Mainline has completed through `24e120b BACKEND-P295 Review-Only Scan Output to Evidence / Score Entry Slice (#721)`.
- Workflow automation also includes `2efdd6b BACKEND-P291G Workflow Auto-Decision Runner Pack (#723)`, `58f69ef BACKEND-P291F Active Mainline Status Refresh Pack (#719)`, and `ba9cd2c BACKEND-P291E Workflow One-Command Runner Pack (#717)`.
- Market Read Mainline has completed through `a61a86b BACKEND-P294 Review-Only MarketRead Output and Scan Output Slice (#713)`.
- Evidence / Score Mainline has completed a review-only entry envelope through P295, review-only evidence normalization through P296, review-only score input / precheck through P297, and review-only score assembly through P298.
- Candidate / Push Mainline has completed review-only score-to-candidate handoff through P299 and review-only candidate attention through P300.
- Current active mainline is machine-readable in `docs/ACTIVE_MAINLINE_STATUS.yml`.
- Current open business-chain package is PR #737 / branch `p301`: `BACKEND-P301 Candidate Preview / Ranking Guard Review-Only Slice`.
- P301 is review-only candidate preview / ranking guard. It is not a real Candidate and does not count as merged main completion until its PR is merged.

Default workflow is GPT + Codex + GitHub-native.
（默认工作流是 GPT + Codex + GitHub 原生。）

Terminal scripts are fallback only except local main sync after merge.
（终端脚本除合并后同步 main 外，只作为兜底。）

Codex must output PR number and stop.
（Codex 必须输出 PR 编号并停止。）

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

If these sources disagree, merged `main` wins and the docs must be corrected.

## What P287-P300 Actually Completed

P287-P300 completed market-read request contract, DTO, validator, test-only wiring, review-only output, review-only scan output, review-only Evidence / Score entry, review-only evidence normalization, review-only score input / precheck, review-only score assembly, review-only candidate handoff, and review-only candidate attention skeleton only:

- P287: docs-only authorization gate for future `MarketReadRequestDTO`.
- P288: pure-data `MarketReadRequestDTO` skeleton plus targeted DTO test.
- P289: docs-only closure and authorization for future guard validator.
- P290: `MarketReadRequestGuardValidator` skeleton, validation result/status DTOs, and targeted validator test.
- P291A: workflow reset, progress source of truth, capability matrix, allowed review-only outputs, blocked capability registry, MVP reality roadmap, and drift guard checklist.
- P292: test-only `MarketReadRequestDTO` -> `MarketReadRequestGuardValidator` wiring and review-only validation output.
- P293: review-only output assembler from `MarketReadRequestDTO` + guard result to `MarketReadReviewOnlyOutputDTO`.
- P294: review-only scan output skeleton from `MarketReadReviewOnlyOutputDTO` to `MarketReadReviewOnlyScanOutputDTO`.
- P295: review-only Evidence / Score entry skeleton from `MarketReadReviewOnlyScanOutputDTO` to `ReviewOnlyEvidenceScoreEntryDTO`.
- P296: review-only evidence normalization skeleton from `ReviewOnlyEvidenceScoreEntryDTO` to `ReviewOnlyNormalizedEvidenceDTO`.
- P297: review-only score input / precheck skeleton from `ReviewOnlyNormalizedEvidenceDTO` to `ReviewOnlyScoreInputPrecheckDTO`.
- P298: review-only score assembly skeleton from `ReviewOnlyScoreInputPrecheckDTO` to `ReviewOnlyScoreAssemblyDTO`.
- P299: review-only candidate handoff skeleton from `ReviewOnlyScoreAssemblyDTO` to `ReviewOnlyCandidateHandoffDTO`.
- P300: review-only candidate attention skeleton from `ReviewOnlyCandidateHandoffDTO` to `ReviewOnlyCandidateAttentionDTO`.

These packages are DTO / validator / skeleton / targeted-test / test-only wiring / review-only output work.

They are not production market-read wiring.

They do not connect `MarketQuoteClient` / `BinanceMarketQuoteClient` into the new scan-chain.

They do not create runtime market reads, production scan output, real scan loop, production ScanScore, Evidence generation, production Candidate workflow, Opportunity Push execution, Readiness, point generation, order execution, or auto-trading.

## Current P301 Scope

P301 is open on branch `p301` and adds a review-only candidate preview / ranking guard skeleton pending merge.

P301 turns `ReviewOnlyCandidateAttentionDTO` into `ReviewOnlyCandidatePreviewGuardDTO`, so review-only candidate attention can become safe manual-review candidate preview / ranking guard context for later Internal Push Preview / Recheck Handoff review-only work.

P301 is not a real Candidate.

P301 does not generate candidate rank or candidate score.

P301 does not generate a real ranking result.

P301 does not generate Promote To Home.

P301 does not generate Opportunity Push.

P301 does not generate Readiness.

P301 does not generate point generation, entry, stop, TP, RR, final direction, long-short signal, order intent, execution intent, or auto-trading.

P301 does not connect `MarketQuoteClient` / `BinanceMarketQuoteClient`.

P301 does not create production scan output, real EvidenceItem, real ScoreItem, real Candidate, candidate rank, candidate score, real ranking result, Promote To Home, Push, Readiness, point generation, order execution, execution API, or auto-trading.

## Current Workflow Scope

P291D, P291E, and P291G are merged on main and provide terminal helpers.

P291H changes the default priority: GitHub-native workflow first, terminal scripts fallback only except local main sync after merge.

Workflow/source-of-truth packages are not production runtime progress.

Use `docs/GITHUB_NATIVE_WORKFLOW.md` and `docs/WORKFLOW_COMMAND_AUTOMATION.md` for workflow rules.

## Current Next Mainline

The current mainline is Candidate / Push Mainline.

The current block is Candidate Preview / Ranking Guard Review-Only Slice.

Evidence / Score entry is completed at `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` after P295.

Evidence normalization is completed at `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` after P296.

Score input / precheck is completed at `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` after P297.

Review-only score assembly is completed at `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` after P298.

Score-to-Candidate handoff is completed at `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` after P299.

Candidate Attention is completed at `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` after P300.

Candidate Preview / Ranking Guard is in P301 only and is not completed on main until the PR merges.

Evidence generation and score calculation are not completed.

Candidate, Push, Readiness, point generation, order execution, execution API, and auto-trading are not completed.

## MarketReadRequest Current Capability

- `MarketReadRequestDTO`: `3 TARGETED_TEST`
- `MarketReadRequestGuardValidator`: `3 TARGETED_TEST`
- `MarketReadRequest test-only wiring`: `4 TEST_ONLY_WIRING`
- `MarketReadRequest review-only output assembler`: `REVIEW_ONLY_OUTPUT_SKELETON`, completed after P293, not production assembler
- `Review-only MarketRead scan output`: `REVIEW_ONLY_SCAN_OUTPUT_SKELETON`, completed after P294, not production scan output
- `Review-only Evidence / Score entry`: `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON`, completed after P295, not real evidence generation or score calculation
- `Review-only evidence normalization`: `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON`, completed after P296, not real evidence generation or score calculation
- `Review-only score input / precheck`: `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON`, completed after P297, not real score calculation or ScoreItem generation
- `Review-only score assembly`: `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON`, completed after P298, not real score calculation, final score, direction, or ScoreItem generation
- `Review-only candidate handoff`: `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON`, completed after P299, not real Candidate, Candidate Attention, Push, Readiness, or point generation
- `Review-only candidate attention`: `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON`, completed after P300, not real Candidate, candidate rank, candidate score, Push, Readiness, or point generation
- `Review-only candidate preview / ranking guard`: `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` pending / completed in P301 PR, not real Candidate, candidate rank, candidate score, real ranking result, Push, Readiness, or point generation

## What Is Still Not Completed

The following remain incomplete for the new MVP chain:

- production MarketReadRequest assembler.
- scan-chain market read adapter connected to authorized review-only inputs.
- production/runtime market read for the new scan-chain.
- production scan output from live market data.
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

Those legacy capabilities must not be described as completion of the P287-P301 market-read / evidence-entry / score assembly / candidate handoff / candidate attention / candidate preview guard scan-chain.

Any use of legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` in the new scan-chain requires a separate authorization package.

## Review-Only Output Clarification

P291A restores the principle that review-only does not mean no output.

The system may produce safe manual-review proposals, such as entry zone proposal, stop zone proposal, TP proposal, RR estimate, position size suggestion, leverage cap suggestion, invalidation condition, reduce-position suggestion, tighten-stop suggestion, move-stop suggestion, partial take-profit suggestion, wait-for-trigger state, plan-invalidated state, internal push preview, risk-downgraded candidate, and confused-with-recovery-condition state.

Those outputs must remain non-executable, manual-review required, and not trade instructions.

Automatic order, close, reverse, leverage change, execution, and auto-trading remain blocked.

## Current Recommendation

Use GPT + Codex + GitHub-native workflow by default.

Review P301 before any merge decision.

Do not describe P295 as real evidence generation or real score calculation.

Do not describe P296 as real evidence generation, persisted evidence, or real score calculation.

Do not describe P297 as real scoring, ScoreItem generation, or score calculation.

Do not describe P298 as real scoring, ScoreItem generation, score calculation, final score, direction, or Candidate handoff completion.

Do not describe P299 as real Candidate, Candidate Attention, Promote To Home, Push, Readiness, or point generation.

Do not describe P300 as real Candidate, candidate rank, candidate score, Promote To Home, Push, Readiness, or point generation.

Do not describe P301 as real Candidate, candidate rank, candidate score, real ranking result, Promote To Home, Push, Readiness, or point generation.

Do not describe Candidate, Push, Readiness, or point generation as completed.
