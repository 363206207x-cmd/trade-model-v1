# PHASE P222 Watchlist Pool Read Boundary

## 1. Phase Position

This document defines the production read universe boundary.

P222 does not read any data.

## 2. Watchlist Pool Is The Only Scan Universe

Future production read work must preserve these rules:

- Watchlist Pool is the only candidate boundary.
- Display Slots are only homepage display positions.
- the default fixed-six assets are only display defaults, not the scan universe.
- non-watchlist assets are not scanned.
- non-watchlist assets do not enter opportunity candidates.
- non-watchlist assets do not enter push candidates.

## 3. Read Failure Rules

Future production read work must fail closed:

- watchlist membership unknown => `INCOMPLETE`
- DB read unavailable => `SOURCE_UNAVAILABLE`
- watchlist config missing => `INCOMPLETE`
- stale watchlist source => `STALE_REVIEW_ONLY` / `INCOMPLETE`
- conflicting source => `REVIEW_ONLY`

## 4. Advancement Boundary

Watchlist Pool read does not mean:

- Candidate Attention.
- Promote To Home.
- Opportunity Push.
- ScanScore.
- ExecutionPlan readiness.

## 5. Conclusion

Any future read implementation must pass through the Watchlist Pool boundary first.

The system must not bypass Watchlist Pool to increase opportunity count.
