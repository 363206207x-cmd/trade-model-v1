# P277 Watchlist Pool Scan Universe Boundary

P277 defines the scan universe boundary for future real scan planning.

The scan universe can only be Watchlist Pool.

## Boundary Rules

Future real scan planning must keep these rules:

- Watchlist Pool is the maximum scan candidate universe.
- A symbol must prove Watchlist Pool membership before scan output planning.
- Display Slots are display priority only.
- 默认六币 are not a backend batch universe.
- Display Slots / 默认六币 cannot be used as a batch universe.
- Display Slots / 默认六币 cannot become the default scan universe.
- Display Slots / 默认六币 cannot become the default push universe.

## What This Blocks

P277 blocks any interpretation that would scan:

- homepage Display Slots as the whole batch universe
- default six coins as the whole batch universe
- non-watchlist assets as scan candidates
- ad hoc external symbols without Watchlist Pool proof

## Not Implementation

P277 does not add Java, DTOs, tests, scan loops, market-read adapters, API endpoints, scheduler wiring, dashboard wiring, or external data reads.

## Safety Rules

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。插针不等于趋势反转。强反转不等于直接反手。
