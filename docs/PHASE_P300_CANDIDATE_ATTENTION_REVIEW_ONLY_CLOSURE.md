# P300 Candidate Attention Review-Only Closure

P300 completes the `ReviewOnlyCandidateHandoffDTO` -> `ReviewOnlyCandidateAttentionDTO` assembler slice.

The output keeps the same safety posture as the upstream chain:

- review-only
- not a trade instruction
- manual review required
- fail-closed when the input is blocked or missing

This package does not connect providers, runtime reads, DB writes, schedulers, APIs, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

This package does not generate a real Candidate.

This package does not generate candidate rank, candidate score, Promote To Home, Push, Readiness, point generation, entry, stop, TP, or RR.

The capability level moves from `REVIEW_ONLY_CANDIDATE_HANDOFF_SKELETON` to `REVIEW_ONLY_CANDIDATE_ATTENTION_SKELETON`.

The next safe step should be Candidate Preview / Ranking Guard review-only slice, not Push / Readiness / point.
