# Phase P311 Executable Point Generation Pre-Approval Plan

P311 is a docs-only planning gate.

It follows P310, where the source-owned review-only point proposal became an internal review-only display object.

Capability movement:

`REVIEW_ONLY_POINT_PROPOSAL_CLOSURE_DISPLAY_GATE` -> `EXECUTABLE_POINT_GENERATION_PRE_APPROVAL_PLAN`

## Scope

P311 defines the approval boundary before any future source-owned numeric point proposal or executable point-generation-adjacent work.

P311 does not modify Java, tests, dashboard, resources, schema, config, or pom.

P311 does not implement runtime behavior.

P311 does not raise Production Runtime Progress.

## Required Future Boundary

Future numeric point work must pass:

1. SourceTrace completeness gate.
2. RuntimeKlineContext completeness gate.
3. DataQuality gate.
4. Multi-timeframe confirmation gate.
5. Liquidity / stampede / wick / strong-reversal Risk Action Guard.
6. Review-only numeric point proposal.
7. Manual review.

Even after those gates, the output must not become an order, execution instruction, external push, or auto-trading action.

## Required Risk Action Guard Rules

- High risk does not mean immediate stop.
- High risk does not mean immediate reverse.
- High risk does not mean immediate open.
- Strong reversal does not mean direct reverse.
- Wick / pin-bar behavior does not mean trend reversal.
- Stampede blocks opportunity push.
- Degraded liquidity should not produce a one-shot market-cut suggestion.
- Short-term wick behavior may only trigger risk reminder and wait-for-confirmation output.
- Point generation must happen after Risk Action Guard.
- Any later point output must remain review-only, not a trade instruction, and manual-review required.

## INCOMPLETE Boundary

Future point proposal work must remain `INCOMPLETE` when source trace, runtime kline context, data quality, timeframe coverage, multi-timeframe confirmation, liquidity state, stampede state, Risk Action Guard result, source-owned entry / stop / TP / RR trace, watchlistPoolProof, or manual review is missing.

## BLOCKED_FAIL_CLOSED Boundary

Future point proposal work must become `BLOCKED_FAIL_CLOSED` when stampede is confirmed, liquidity is severely degraded, strong reversal lacks multi-timeframe confirmation, proposed points imply direct reverse or executable instruction, external channel is attempted before authorization, order / execution / auto-trading is attempted, source trace is forged or missing while generation is attempted, data quality fails a hard threshold, or Risk Action Guard fails.

## Not Authorized

P311 does not authorize:

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

After P311, the next recommended package is Source-owned Numeric Point Proposal Plan or Point Generation Safety Gate Plan.

The next package should not directly implement real point generation.

External Channel Authorization remains a separate C-level package.
