# PHASE P315 DataQuality Numeric Point Contract Plan

## Scope

P315 is a docs-only planning gate for the Readiness / Point Mainline.

It follows P314 `RUNTIME_KLINE_CONTEXT_NUMERIC_POINT_CONTRACT_PLAN` and advances the documentation state to `DATA_QUALITY_NUMERIC_POINT_CONTRACT_PLAN`.

## Completed In This Phase

P315 defines the future numeric point DataQuality contract for:

- minimum data-quality context fields;
- `dataQualityScore` semantics;
- hard and warning thresholds;
- SourceTrace quality;
- RuntimeKlineContext quality;
- OHLCV completeness quality;
- freshness / stale quality;
- liquidity, stampede, wick, event, and abnormal data quality;
- multi-timeframe consistency quality;
- `INCOMPLETE` handling;
- `BLOCKED_FAIL_CLOSED` handling;
- fixture matrix expectations;
- Risk Action Guard integration.

## Explicit Non-Scope

P315 does not change Java.

P315 does not change tests.

P315 does not change dashboard runtime or `dashboard.html`.

P315 does not create DataQuality Java DTOs.

P315 does not implement numeric point proposal.

P315 does not generate executable point values.

P315 does not generate executable entry / stop / TP / RR.

P315 does not connect external channel, Push send, order, execution, or auto-trading.

## DataQuality Contract Closure

Future numeric point values must be bound to high enough quality SourceTrace and RuntimeKlineContext inputs.

Scores below 70 remain incomplete. Hard threshold failure, forged source trace, severe OHLCV gap, confirmed stampede, severely degraded liquidity, confirmed abnormal data, blocking event risk, or trade-inducing multi-timeframe conflict must fail closed.

Warning threshold failure may only downgrade to review-only / recheck-required output; it cannot enable execution.

## Safety Conclusion

P315 does not increase Production Runtime Progress.

P315 does not authorize real point generation.

P315 does not authorize external channel.

P315 does not authorize order / execution / auto-trading.

The next recommended package is MultiTimeframe Numeric Point Contract Plan or Numeric Point Safety Validator Plan, not real point generation.
