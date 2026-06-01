# P297 Score Input / Precheck Review-Only Closure

P297 completes a review-only `ReviewOnlyNormalizedEvidence -> ReviewOnlyScoreInputPrecheck` assembler slice.

The output remains:

- review-only;
- not a trade instruction;
- manual-review required;
- fail-closed when input is blocked;
- blocker-preserving.

P297 does not connect provider, runtime data, DB writes, scheduler behavior, controller/API behavior, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

P297 does not generate real `ScoreItem`.

P297 does not calculate score.

Capability level moves from `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` to `REVIEW_ONLY_SCORE_INPUT_PRECHECK_SKELETON` inside the P297 PR.

The next step should be Review-only score assembly, not Candidate / Push.
