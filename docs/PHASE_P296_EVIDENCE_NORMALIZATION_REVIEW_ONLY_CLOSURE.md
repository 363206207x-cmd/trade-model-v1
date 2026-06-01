# P296 Evidence Normalization Review-Only Closure

P296 completes a review-only `ReviewOnlyEvidenceScoreEntry -> ReviewOnlyNormalizedEvidence` assembler slice.

The output remains:

- review-only;
- not a trade instruction;
- manual-review required;
- fail-closed when input is blocked;
- blocker-preserving.

P296 does not connect provider, runtime data, DB writes, scheduler behavior, controller/API behavior, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

P296 does not generate real `EvidenceItem`.

P296 does not calculate score and does not generate `ScoreItem`.

Capability level moves from `REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON` to `REVIEW_ONLY_EVIDENCE_NORMALIZATION_SKELETON` inside the P296 PR.

The next step should be a Score input / precheck review-only slice, not Candidate / Push.
