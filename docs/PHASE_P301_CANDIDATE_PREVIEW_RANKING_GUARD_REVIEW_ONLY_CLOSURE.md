# P301 Candidate Preview / Ranking Guard Review-Only Closure

P301 completes the `ReviewOnlyCandidateAttentionDTO` -> `ReviewOnlyCandidatePreviewGuardDTO` assembler slice.

The output keeps the same safety posture as the upstream chain:

- review-only
- not a trade instruction
- manual review required
- fail-closed when the input is blocked or missing

This package does not connect providers, runtime reads, DB writes, schedulers, APIs, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

This package does not generate a real Candidate, rank, score, or ranking result.

This package does not generate Push, Readiness, point generation, entry, stop, TP, or RR.

The capability level moves from `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON` to `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON`.

The next safe step should be Internal Push Preview / Recheck Handoff review-only slice, not Readiness / point.
