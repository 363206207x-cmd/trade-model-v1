# Push Watchlist P0 Verification

## 1. Verification Target

- Commit: `fdbcb16 feat(push): gate push candidates by watchlist`
- Capability: read-only watchlist eligibility
- Config key: `push.watchlist.symbols`
- Behavior: non-watchlist assets fail closed

## 2. P0 Capability Boundary

- Read only from `tm_rule_config`.
- Does not change `schema.sql`.
- Does not change `application.yml`.
- Does not restore `PushWatchlistConfig` or audit flows.
- Does not restore latest-price recheck.
- Does not restore asset-state gate.
- Does not restore stampede guard.
- Does not restore RuleEngine, Opportunity, TradeReview, or RuleImprovement tracks.
- Does not place orders automatically.
- Does not open positions automatically.
- Does not close positions automatically.
- Does not reverse positions automatically.
- Does not connect to any exchange order API.

## 3. Test Results

- `compile`: PASS
- `test-compile`: PASS
- `WatchlistPushEligibilityServiceImplTest`: PASS
- `PushSnapshotServiceTest`: PASS
- `PushRecheckServiceImplTest`: PASS
- `DashboardControllerTest`: PASS
- `DecisionServiceImplTest`: PASS
- `ManualPositionControllerTest`: PASS

## 4. Runtime Smoke Results

- `/dashboard`: 200
- `/api/dashboard/summary`: 200
- `BTCUSDT` detail: 200
- `ETHUSDT` detail: 200
- `SOLUSDT` detail: 200
- `/api/system/position-sync-status`: 200, `freshnessStatus=FRESH`
- `/api/dashboard/refresh`: 200
- `/api/position-monitor/open`: 200, `data=[]`

## 5. Summary Result

- `systemStatus.status=OK`
- `databaseStatus=UP`
- `schedulerStatus=RUNNING`
- `decisions=12`
- `openPositionCount=2`

## 6. Watched Asset Detail Samples

### BTCUSDT

- `latestPrice=81787.4`
- `priceChangePct=0.575385`
- `sourceType=OKX_24H_FALLBACK`
- `dataQualityScore=85`
- `readModelTruthStatus=FULL`
- `hasOpenPosition=true`

### ETHUSDT

- `latestPrice=2335`
- `priceChangePct=-0.635338`
- `sourceType=OKX_24H_FALLBACK`
- `dataQualityScore=85`
- `readModelTruthStatus=FULL`
- `hasOpenPosition=true`

### SOLUSDT

- `latestPrice=97.68`
- `priceChangePct=3.005378`
- `sourceType=OKX_24H_FALLBACK`
- `dataQualityScore=85`
- `readModelTruthStatus=FULL`
- `hasOpenPosition=false`

## 7. Runtime Boundary Confirmation

- Missing `push.watchlist.symbols` fails closed.
- The default six watched assets are not implicitly treated as push-eligible.
- `governance_missed` is not a push candidate.
- Push P0 does not affect the dashboard main path.
- Push P0 does not affect watched asset market data input.
- Push P0 does not affect the position monitor API.

## 8. Deferred Items

- Push watchlist configuration maintenance API
- Push watchlist audit schema and mapper
- Latest-price recheck
- Asset-state gate
- Stampede guard
- Push P1 / P2
- RuleEngine / Opportunity / TradeReview / RuleImprovement

## 9. Next Step Recommendation

- Commit this verification document first.
- Before P1, design the configuration maintenance and audit plan.
- Do not restore the whole external workspace source track at once.
