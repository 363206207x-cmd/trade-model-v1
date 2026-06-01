# P299 Score-to-Candidate Handoff Review-Only Closure

P299 completes a review-only `ReviewOnlyScoreAssembly -> ReviewOnlyCandidateHandoff` assembler slice.

The output remains:

- review-only;
- not a trade instruction;
- manual-review required;
- fail-closed when input is blocked;
- blocker-preserving.

P299 does not connect provider, runtime data, DB writes, scheduler behavior, controller/API behavior, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

P299 does not generate a real Candidate.

P299 does not generate Push, Readiness, point generation, entry, stop, TP, or RR.

Capability level moves from `REVIEW_ONLY_SCORE_ASSEMBLY_SKELETON` to `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` inside the P299 PR.

The next step should be Candidate Attention review-only slice, not Push / Readiness / point.
