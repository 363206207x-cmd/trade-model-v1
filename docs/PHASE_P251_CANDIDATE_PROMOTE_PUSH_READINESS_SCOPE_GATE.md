# PHASE P251 Candidate Promote Push Readiness Scope Gate

## 1. Phase Positioning

P251 defines the future boundary for Candidate Attention / Promote To Home / Opportunity Push / Readiness / point generation.

P251 does not implement these features.

## 2. Candidate Attention Boundary

Candidate Attention:

- can only happen after ScanScore and guard.
- remains review-only.
- is not push.
- is not readiness.
- is not entry / stop / TP / RR.
- is not a trade instruction.

## 3. Promote To Home Boundary

Promote To Home:

- is only a dashboard visibility lift.
- cannot expand Watchlist Pool.
- cannot replace batch universe.
- cannot trigger orders.
- requires an independent authorization gate.

## 4. Opportunity Push Boundary

Opportunity Push:

- can only be based on Watchlist Pool.
- must be protected by Risk Action Guard / stampede / liquidity gates.
- must not trigger during stampede / extreme stress.
- must not generate trade instructions.
- requires an independent authorization gate.

## 5. Readiness / Point Generation Boundary

Readiness:

- must happen after dataQuality, sourceTrace, boundary candidate, and risk guard.
- entry / stop / TP / RR must have source ownership.
- high risk does not mean direct reverse.
- wick-only movement does not mean trend reversal.
- stampede state forbids opportunity push.
- Readiness / point generation requires an independent authorization gate.

## 6. Conclusion

Future work must not merge Candidate / Promote / Push / Readiness into a single Java implementation.

Docs-only planning may combine these gates. Java implementation must remain layered.
