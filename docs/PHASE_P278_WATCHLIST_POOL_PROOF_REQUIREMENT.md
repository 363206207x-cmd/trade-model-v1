# P278 Watchlist Pool Proof Requirement

Watchlist Pool proof is required before any future real scan input can be considered eligible.

## Candidate Boundary

The scan universe can only originate from Watchlist Pool membership. The future input contract may carry proof such as symbol identity, source, watchlist config version, and membership evidence, but P278 does not implement that DTO or any membership validator.

Display Slots / 默认六币 cannot be scan universe or batch universe.

Display Slots are presentation priority only. The default six assets are not a backend batch universe, not a scan universe, and not an implicit push universe.

## Fail-Closed Rule

If Watchlist Pool proof is missing, stale, ambiguous, or inconsistent, the future scan input contract must fail closed. It may be review-only and audit-only where applicable, but it must not become production ScanScore input, Candidate workflow input, Push execution input, Readiness input, point generation input, order input, or execution input.

Risk Action Guard must remain before delivery / Push / Readiness.
