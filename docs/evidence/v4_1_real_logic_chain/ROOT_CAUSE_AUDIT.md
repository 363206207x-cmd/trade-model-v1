# TRINE LOGIC v4.1 Real Logic Chain Root-Cause Audit

Audit baseline: `b6904ba7c12759ab8a89da40991f9809cdc73e24`

Staging deployed SHA: `b6904ba7c12759ab8a89da40991f9809cdc73e24`

Method: read-only source inspection plus read-only PostgreSQL queries on Staging.
Protected data: the existing three groups / twenty-five suspected duplicate positions were not changed.

## Frozen findings

### A. Direction calculation and time identity

- The active normalization contract is `V41-NORM-WREP-1`.
- `MarketBiasPolicy` assigns 4h weight `0.57` and 1h weight `0.43`. The 15m and 5m scores do not contribute to the structural direction score: 15m is the trigger timeframe and 5m is the micro-risk/liquidity filter.
- A missing 4h or 1h core input returns `WAIT / INSUFFICIENT_DATA`. A 4h/1h sign conflict with both absolute scores at least 35 returns `WAIT / MULTI_TIMEFRAME_CONFLICT`.
- The weighted structural thresholds are: `>=70 STRONG_BULLISH`, `>=35 BULLISH`, `>=15 WEAK_BULLISH`, `[-14,14] RANGE`, `(-35,-14] WEAK_BEARISH`, `(-70,-35] BEARISH`, and `<=-70 STRONG_BEARISH`.
- Normalized scores use a 200-bar lookback, require at least 60 samples, winsorize at 2.5/97.5 percentiles, then rank to `[-100,100]`.
- The current `DecisionEngine` convergence path requires same-sign 4h and 1h core scores but does not apply the configured `maximumTrendScoreDifference=15`; the older full convergence API does. Strong-label qualification is therefore not using the complete configured convergence contract.
- `AnalysisSchedulerService.lightweightAssessment` tracks only the configured 5m decision close. A new closed 1h or 4h candle is not a first-class trigger/idempotency identity, so an observing asset can miss a required structural recalculation.
- Full analysis is additionally gated by `coinGlassReady`, even though derivatives context is optional for the core direction calculation. CoinGlass availability can therefore prevent core structural recalculation.
- The decision explanation persisted by `AnalysisAssemblerServiceImpl` omits `multiTimeframeDetails`, `priceAtDecision`, and `directionCalculatedAt`. Home reconstructs 5m/1h/4h bars at read time and combines them with a newer quote. The UI can show the two timestamps, but the evidence is not an immutable same-run snapshot.
- A validation failure clears `finalConfidence` while the non-final card can retain `validatedMarketBias`. This produces a strong direction with no visible trusted confidence.

Frozen root cause:

```text
DIRECTION_ROOT_CAUSE=
SCHEDULER_5M_ONLY_IDENTITY + OPTIONAL_COINGLASS_HARD_GATE +
INCOMPLETE_STRONG_CONVERGENCE_CHECK + NON_PERSISTED_DECISION_PROVENANCE +
NON_FINAL_STRONG_DIRECTION_WITH_NULL_FINAL_CONFIDENCE_PROJECTION
```

### B. ETH 2391 to later high-price phase

The first matching Staging record is:

```text
analysisId=ana-c55bd070fb1d4da08e599f56ec158212
decisionId=dec-6a01380cd6234bd098f7552f7caf0305
traceId=trace-e5782a152cf84dbf86dfe310072cbb24
analysisTime=2026-09-03T10:49:58Z
priceAtDecision=2391.61
1hClosedAt=2026-09-03T09:59:59.999Z
4hClosedAt=2026-09-03T07:59:59.999Z
ruleMarketBias=STRONG_BEARISH
validatedMarketBias=STRONG_BEARISH
projectedMarketBias=WAIT
ruleConfidence=LOW
finalConfidence=NULL
riskLevel=EXTREME
```

Subsequent stored runs were observed at about 2395.54, 2405.95, and 2407.40 with a retained bearish rule label; at 2511.22 the engine detected `MULTI_TIMEFRAME_CONFLICT` and projected `WAIT`; at 2501.33 it recalculated `STRONG_BULLISH`; a later stored run around 2518.18 remained strong bullish. This proves new runs were created and the structural result eventually changed. It does not prove a cache returned the original 2391 decision as a new decision.

The defect is that the intervening strong bearish label did not carry visible trusted confidence, immutable timeframe evidence, or a persisted invalidation condition. The scheduler also does not guarantee a run for each new 1h/4h close. Thus the card could not prove that a strong conclusion was current and still valid even when the quote was newer.

Frozen classification:

```text
ETH_INCIDENT_ROOT_CAUSE=
DIRECTION_WEIGHT_OR_THRESHOLD_DEFECT + INVALIDATION_NOT_EVALUATED +
PROJECTION_OR_CACHE_DEFECT

NO_NEW_ANALYSIS_RUN=PARTIAL_TRIGGER_DEFECT
NEW_RUN_CREATED_BUT_UI_STALE=NOT_PROVEN
OLD_DIRECTION_NEW_PRICE_MIX=NOT_PROVEN_IN_PERSISTED_ROWS
HYSTERESIS_LOCKED_DIRECTION=NOT_PROVEN
PROVIDER_OR_TTL_FAILURE=CONTRIBUTING_GATE_ONLY
```

### C. Direction, plan, on-demand task and three-AI chain

- In the last 24 hours, all six tracked assets produced real analysis, decision and plan rows. Recent plans were `BLOCKED / RULE_VALIDATION_BLOCKED / final_plan=false`; none was a final validated plan.
- Home's mapper joins only `final_plan=true`, `PASS`, `FINAL_VALIDATED`. A real blocked plan is therefore discarded and rendered as a blank plan rather than a visible blocked result with reasons and recovery conditions.
- `DecisionChainServiceImpl.blockedPlan` clears entry, stop and target parameters. That is correct safety behavior, but the projection also loses the remaining blocked-plan status and reasons.
- Seven runs in the sampled three-day window had successful GPT, Gemini and Grok role calls for the same analysis. Their candidates were rejected and resolver results reported extreme conflict; validation then blocked the plan for opportunity/permission/plan-mode/trigger reasons. This proves that “blank three AI” is not equivalent to “AI never ran”.
- Later calls were truthfully blocked by `DAILY_BUDGET_EXCEEDED` under the configured 5 USD daily cap, by asset/role frequency limits, or failed evidence/parse/source-reference contracts. The principal defect is loss of these explicit role states in the selected Home/Analysis projection, compounded by excessive repeated scheduling.
- Clicking an existing pooled asset currently changes selection only. Asset-search analysis uses `ANALYSIS_PREVIEW`, creates a fresh `tm_async_task` row on every click, and returns analysis/trace data without a stable task identity. The controller marks the request succeeded once the background run is merely queued. Refresh cannot restore or poll the task, while only the deeper analysis-run idempotency key prevents some duplicate runs.

Frozen breakpoints:

```text
PLAN_CHAIN_BREAKPOINT=BLOCKED_PLAN_FILTERED_BY_FINAL_ONLY_HOME_JOIN
THREE_AI_CHAIN_BREAKPOINT=REAL_ROLE_RESULTS_AND_FAILURES_NOT_PROJECTED_WITH_ONE_SELECTED_RUN_IDENTITY
ON_DEMAND_CHAIN_BREAKPOINT=SELECTION_OR_PREVIEW_ONLY_WITH_NON_IDEMPOTENT_NON_RECOVERABLE_TASK_ROW
```

### D. Manual position monitoring

- Staging contains 28 open ETH manual-independent positions; every row has no final plan identity. The protected suspected duplicates were observed only and not changed.
- No `tm_position_monitor_log` row exists for those positions.
- `TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED=false` in the deployed runtime, while global schedulers are enabled. Position creation does not enqueue an initial monitor run.
- Symbol normalization correctly maps `ETH` to the Binance `ETHUSDT` market identity, so the observed result is not explained by the short symbol alone.
- `PositionMonitorServiceImpl` formally allows `MANUAL_INDEPENDENT` without a final plan, but it then requires full fresh 5m/15m/1h/4h windows plus a recent verified Decision, eight scores and multi-timeframe evidence before keeping the source verified. CoinGlass is mostly fail-soft, but missing analysis context still suppresses otherwise valid Binance quote monitoring.
- The projection requires logic/reversal/risk/action fields to call a result trusted. Manual positions with no original plan should report logic `NOT_APPLICABLE`, while price/PnL monitoring continues as `PARTIAL`; the current projection can instead report the whole source unavailable.
- In-process claims prevent simultaneous duplication only inside one process. Monitor logs have no stable database run key, so repeated/concurrent scheduler execution is not persistently idempotent.

Frozen root cause:

```text
POSITION_MONITOR_ROOT_CAUSE=
DISABLED_RUNTIME_SCHEDULER + NO_IDEMPOTENT_INITIAL_MONITOR +
MANUAL_BASE_PRICE_MONITOR_WRONGLY_GATED_BY_FULL_ANALYSIS_EVIDENCE +
NO_DATABASE_MONITOR_RUN_IDEMPOTENCY
```

## Repair contract frozen from this audit

1. A new 1h or 4h close must create or select one idempotent analysis identity and re-evaluate direction, confidence, risk and invalidation without making optional CoinGlass a core-direction prerequisite.
2. Strong direction projection requires immutable same-decision provenance and a trusted visible confidence; otherwise it must be stale/revalidation/conflict rather than current strong direction.
3. Every directional result must expose a plan result. Unsafe/incomplete chains remain non-executable `BLOCKED` or waiting modes with exact causes and recovery conditions; no fake price levels are added.
4. Asset clicks must return and restore one idempotent job/run identity. Preview remains preview; an eligible pooled directional asset follows the opportunity-decision chain.
5. Each of the three AI roles, resolver and rule validation must be visible as a real success, pending, failure or blocked state bound to the same analysis/decision identity.
6. A manual-independent position requires only a verified fresh Binance quote for base monitoring. Missing optional analysis/CoinGlass/AI context yields `PARTIAL`, and no-plan logic is `NOT_APPLICABLE`.
7. Initial and scheduled monitor execution must share a stable database idempotency key; closed positions are excluded.
8. No automatic trade or Telegram send is introduced, and protected duplicate positions remain unchanged.
