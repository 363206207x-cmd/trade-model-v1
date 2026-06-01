# P302 Internal Push Preview / Recheck Handoff Review-Only Closure

P302 completes the `ReviewOnlyCandidatePreviewGuardDTO` -> `ReviewOnlyInternalPushPreviewDTO` assembler slice.

The output keeps the same safety posture as the upstream chain:

- review-only
- not a trade instruction
- manual review required
- fail-closed when the input is blocked or missing
- `recheckRequired = true`
- `riskActionGuardRequired = true`

This package does not connect providers, runtime reads, DB writes, schedulers, APIs, `MarketQuoteClient`, or `BinanceMarketQuoteClient`.

This package does not generate a real Push, external channel, Readiness, point generation, entry, stop, TP, or RR.

This package does not render a sendable message and does not send any message.

The capability level moves from `REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_SKELETON` to `REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK_SKELETON`.

The next safe step should be Push preview closure before external channel, not direct external push.
