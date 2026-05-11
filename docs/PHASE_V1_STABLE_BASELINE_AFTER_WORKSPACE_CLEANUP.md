# PHASE V1 Stable Baseline After Workspace Cleanup

Date: 2026-05-11

## 1. Stable Point Summary

- Tracked working tree is clean.
- Staged area is empty.
- Untracked source files that polluted Maven compilation have been isolated outside the repository.
- Untracked docs remain in the repository for later document triage.
- The clean mainline can compile, test, start, and serve the core dashboard/API flows.

## 2. Key Commits

- `76df077 feat(market): add OKX fallback market data providers`
- `47162ed feat(position): add structured manual monitor advice`
- `6c37ea8 feat(dashboard): show manual position action advice`
- `6c2cc9c feat(dashboard): enrich latest decisions from base read models`
- `3ddca3a fix(position): avoid duplicate sync rows for manual positions`
- `d1cc050 feat(governance): classify governance missed opportunities`
- `b783d5f fix(query): move push recheck retry window to java time bound`
- `d3f2036 chore(gitignore): ignore local database and residual backups`
- `aab953a docs(workflow): add migration draft and homepage validation notes`
- `0896a32 refactor(query): simplify joined decision plan summary concat`
- `05ae498 refactor(hot-reset): clarify trigger threshold naming`

## 3. Verification Results

- `./mvnw -q -DskipTests compile`: PASS
- `./mvnw -q -DskipTests test-compile`: PASS
- `DashboardControllerTest`: PASS
- `DecisionResultMapperLatestPlanIntegrationTest`: PASS
- `DecisionServiceImplTest`: PASS
- `ManualPositionControllerTest`: PASS
- `PositionMonitorControllerTest`: PASS
- `PositionMonitorServiceImplTest`: PASS
- `PositionTradeResultServiceImplTest`: PASS
- `MissedOpportunityServiceImplTest`: PASS

## 4. Startup Smoke Results

- `/dashboard`: HTTP 200
- `/api/dashboard/summary`: HTTP 200
- `/api/dashboard/detail?symbol=BTCUSDT`: HTTP 200
- `/api/dashboard/detail?symbol=ETHUSDT`: HTTP 200
- `/api/dashboard/detail?symbol=SOLUSDT`: HTTP 200
- `/api/system/position-sync-status`: HTTP 200
- `/api/dashboard/refresh`: HTTP 200
- `/api/position-monitor/open`: HTTP 200

## 5. Watchlist Detail Samples

| Symbol | latestPrice | priceChangePct | sourceType | dataQualityScore | readModelTruthStatus | hasOpenPosition | positionSide | avgOpenPrice |
| --- | ---: | ---: | --- | ---: | --- | --- | --- | ---: |
| BTCUSDT | 80749.9 | -0.203795 | OKX_24H_FALLBACK | 85 | FULL | true | LONG | 63520.5 |
| ETHUSDT | 2310.98 | -0.849076 | OKX_24H_FALLBACK | 85 | FULL | true | SHORT | 3120.8 |
| SOLUSDT | 94.44 | 0.628663 | OKX_24H_FALLBACK | 85 | FULL | false |  |  |

## 6. Position Monitor Status

- `/api/position-monitor/open`: HTTP 200
- Current open row count: 0
- The `actionAdvice` backend and dashboard UI have already been committed.
- This smoke run had no open monitor row to display `actionAdvice`, but the endpoint was not broken.

## 7. Isolated Content

- Untracked source files under `src/main/java`, `src/test/java`, and `src/main/resources` were moved to:
  `/Users/xuchao/Documents/trade-model-v1-untracked-workspace/`
- Residual tracked-file backups are stored at:
  `/Users/xuchao/Documents/trade-model-v1-residual-backups/`
- Untracked docs remain inside the repository and should be triaged separately.

## 8. Deferred Tracks

- RuleEngine / PlanBoundary
- Push / Watchlist
- Opportunity
- TradeReview / ReviewCenter
- RuleImprovement
- `CODEX_AUTONOMOUS_TASK_QUEUE` / PHASE docs refresh

## 9. Next Step Recommendation

- Commit this stable point document first.
- Before entering the formal Watchlist / Push track, restore only the minimal required files from the external workspace.
- Do not restore all large-track source files in one batch.
