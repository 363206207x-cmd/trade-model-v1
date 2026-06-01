# Phase P312 Source-owned Numeric Point Proposal Plan

P312 is a docs-only plan.

It follows P311 `EXECUTABLE_POINT_GENERATION_PRE_APPROVAL_PLAN`.

Capability movement:

`EXECUTABLE_POINT_GENERATION_PRE_APPROVAL_PLAN` -> `SOURCE_OWNED_NUMERIC_POINT_PROPOSAL_PLAN`

## Scope

P312 defines the future shape of a source-owned review-only numeric point proposal.

It does not modify Java, tests, dashboard, resources, schema, config, or pom.

It does not implement runtime behavior.

It does not raise Production Runtime Progress.

## Planned Future Object

The future object may be named `ReviewOnlyNumericPointProposalDTO`.

P312 does not create that object.

The future object must keep:

- `reviewOnly = true`
- `notTradeInstruction = true`
- `manualReviewRequired = true`
- `recheckRequired = true`
- `riskActionGuardRequired = true`
- `sourceTraceRequired = true`
- `runtimeKlineContextRequired = true`
- `incompleteSafe = true`
- `failClosed = true` when blocked

## Planned Field Families

Future numeric point proposal work must define source-owned, nullable, incomplete-safe field families for:

- entry / entry zone;
- stop / stop zone;
- TP / TP ladder;
- RR / risk reward calculation.

Each numeric field must have source trace metadata. Missing trace must keep the object `INCOMPLETE`.

## Source Trace Metadata

Every numeric source trace must include owner, contract id, symbol, timeframe, source type, source value or range, observed time, created time, freshness status, data quality score, source reason, source ref, upstream snapshot ref, runtime kline context ref, and Risk Action Guard ref.

## INCOMPLETE Boundary

Numeric point proposal must remain `INCOMPLETE` when entry / stop / TP / RR source is missing, source trace is stale, runtime kline context is missing, data quality is below threshold, timeframe conflict exists, liquidity or stampede state is unknown, wick or strong reversal confirmation is missing, Risk Action Guard has not executed, manual review is missing, watchlistPoolProof is missing, a required source is inferred from display text, or a value is fabricated from score / label / latest price only.

## BLOCKED_FAIL_CLOSED Boundary

Numeric point proposal must become `BLOCKED_FAIL_CLOSED` when it implies buy / sell / long / short, order intent, execution intent, auto-trading, failed Risk Action Guard, confirmed stampede, severely degraded liquidity, forged source trace, hard data-quality failure, manual-review bypass, or external-channel attempt before authorization.

## Risk Action Guard Boundary

Risk Action Guard must run before numeric point generation.

High risk does not mean immediate stop, reverse, or open.

Strong reversal does not mean direct reverse.

Wick / pin-bar behavior does not mean trend reversal.

Stampede blocks opportunity push.

Degraded liquidity should not produce a one-shot market-cut suggestion.

Even review-only numeric points cannot become trading actions.

## Not Authorized

P312 does not authorize:

- Java DTO creation;
- numeric point proposal implementation;
- executable point generation;
- executable entry / stop / TP / RR;
- final direction or long-short signal;
- dashboard runtime integration;
- external channel;
- push send;
- order;
- execution;
- auto-trading.

## Next Step

After P312, the next recommended package is SourceTrace Numeric Point Contract Plan or Numeric Point Safety Validator Plan.

The next package should not directly implement real point generation.
