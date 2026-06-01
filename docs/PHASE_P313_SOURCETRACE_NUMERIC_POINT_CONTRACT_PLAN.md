# PHASE P313 SourceTrace Numeric Point Contract Plan

## Scope

P313 is a docs-only planning gate for the Readiness / Point Mainline.

It follows P312 `SOURCE_OWNED_NUMERIC_POINT_PROPOSAL_PLAN` and advances the documentation state to `SOURCETRACE_NUMERIC_POINT_CONTRACT_PLAN`.

## Completed In This Phase

P313 defines the future numeric point SourceTrace contract for:

- entry source traces;
- stop source traces;
- TP / ladder source traces;
- RR source traces;
- freshness and stale handling;
- `INCOMPLETE` handling;
- `BLOCKED_FAIL_CLOSED` handling;
- fixture matrix expectations;
- Risk Action Guard integration.

## Explicit Non-Scope

P313 does not change Java.

P313 does not change tests.

P313 does not change dashboard runtime or `dashboard.html`.

P313 does not create SourceTrace Java DTOs.

P313 does not implement numeric point proposal.

P313 does not generate executable point values.

P313 does not generate executable entry / stop / TP / RR.

P313 does not connect external channel, Push send, order, execution, or auto-trading.

## SourceTrace Contract Closure

Future numeric point values must be source-owned and trace-bound. Missing, stale, unknown, conflicted, forged, or untrusted source traces must keep the proposal incomplete or fail-closed.

No future source trace can be derived only from latest price, dashboard text, candidate label, score label, AI prose, or manual fill.

Risk Action Guard reference, runtime kline context reference, data quality score, upstream snapshot reference, and Watchlist Pool proof are mandatory contract inputs.

## Safety Conclusion

P313 does not increase Production Runtime Progress.

P313 does not authorize real point generation.

P313 does not authorize external channel.

P313 does not authorize order / execution / auto-trading.

The next recommended package is RuntimeKlineContext Numeric Point Contract Plan or Numeric Point Safety Validator Plan, not real point generation.
