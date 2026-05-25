# PHASE P251 ScanScore Scope Gate

## 1. Phase Positioning

P251 defines the ScanScore scope gate.

P251 does not implement ScanScore.

## 2. Future ScanScore Candidates

Future ScanScore work may consider:

- ScanScore DTO / enum / rule skeleton.
- ScanScore calculation authorization gate.
- Score only after runtime source read and batch scan skeleton.
- Score must remain review-only initially.
- Score cannot trigger push / readiness / point generation by itself.
- Score must have blockingReasons / sourceTrace / dataQuality flags.

## 3. Future ScanScore Must Preserve

Future ScanScore must keep:

- no push by itself.
- no readiness by itself.
- no entry / stop / TP / RR by itself.
- no trading action.
- no auto-trading.
- fail-closed when data missing.
- fail-closed when source stale.
- score output must be explainable and bounded.

## 4. Future ScanScore Forbidden Scope

Future ScanScore must not:

- be combined with MarketQuoteClient in the same PR.
- be combined with scheduler in the same PR.
- directly push.
- directly promote to home.
- directly generate point values.
- directly upgrade readiness.

## 5. Conclusion

P252 / P253 can consider Market-read or ScanScore authorization gates.

They must remain independent.
