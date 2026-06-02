# PHASE P326 Source Context Integration Plan

## Scope

P326 is a docs-only source context integration plan for the Readiness / Point Mainline.

It follows P325 `SOURCE_OWNED_NUMERIC_POINT_CANDIDATE_ASSEMBLER_VERIFICATION` and advances the state to `SOURCE_CONTEXT_INTEGRATION_PLAN`.

P326 does not modify Java, tests, dashboard, resources, schema, config, service wiring, external channel, order, execution, or auto-trading.

## Planned Future Contexts

Future review-only numeric point candidate integration requires:

- `SourceTraceNumericSourceContext`;
- `RuntimeKlineContext`;
- `DataQualityContext`;
- `MultiTimeframeContext`;
- `RiskActionGuardContext`;
- `WatchlistPoolProof`;
- source-owned entry, stop, TP, and RR contexts.

Each context must include source refs, timestamps, symbol, market, timeframe, quality state, missing reason, or blocked reason.

## Integration Boundary

The future integration path may bind real source contexts into the existing DTO / validator / assembler skeletons.

It must not generate entry, stop, TP, RR, final direction, order intent, execution intent, or auto-trading behavior.

It must not derive point values from latest price alone, latest close alone, dashboard text, AI prose, score labels, or candidate labels.

It must not bypass `NumericPointSafetyValidator` or Risk Action Guard.

## Missing And Blocked Rules

Missing required contexts, stale sources, unresolved conflicts, missing watchlist proof, missing source refs, or insufficient data quality must remain `INCOMPLETE`.

Forged sources, hard threshold failures, confirmed stampede, severe OHLCV gaps, trade-inducing higher-timeframe conflicts, Risk Action Guard bypass, validator bypass, executable semantics, external channel attempts, Push send attempts, order attempts, execution attempts, and auto-trading attempts must be `BLOCKED_FAIL_CLOSED`.

## Conclusion

P326 is only the plan for future real source context integration.

It does not raise Production Runtime Progress.

It does not connect real source context.

It does not make entry / stop / TP / RR available as trading output.

It does not authorize dashboard runtime integration, external channel, order, execution, or auto-trading.

The next recommended package is SourceTrace Numeric Source Read Model Plan, RuntimeKlineContext Source Binding Plan, DataQualityContext Source Binding Plan, or Review-only Numeric Point Internal Preview Plan.
