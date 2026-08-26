# TRINE LOGIC Core Production Loop Automation Authorization

Status: `AUTHORIZED_PENDING_MERGED_MAIN`

Authorization package:
`FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION`

Authorization type: `DOCS_GATE_ONLY`

This record is subordinate to the sole authoritative v4.1 Product Source,
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`. It grants no
runtime capability on this branch. The exact implementation package becomes
eligible only after this authorization is reviewed, merged, and validated on
clean synchronized main.

## Product Contract

The successor may connect the existing production loop without creating a
parallel business stack:

`Asset Pool -> lightweight scan -> Opportunity state gate -> trusted full Analysis -> Decision chain -> validated Final -> explicit manual UserPosition -> Position Monitoring -> Review`.

Asset Pool remains the only continuous opportunity source. Opportunity State,
Plan Mode, Final lifecycle and UserPosition remain separate. A triggered
opportunity never creates or mutates a position.

## Authorized Scheduling Contract

| Subject | Configured cadence | Authorized work |
|---|---:|---|
| `observing` Opportunity | 15 minutes | Lightweight source-owned scan and promotion check |
| `candidate` Opportunity | 5 minutes | Lightweight refresh; full analysis only after the promotion/material-change gate |
| `waiting_trigger` Opportunity | 2 minutes | Trigger/readiness recheck; full decision chain only when its existing gate requires it |
| `triggered` Opportunity | 1 minute | Lightweight trigger and Final-revalidation readiness check only; not unconditional full Analysis or Three-AI execution |
| `OPEN` / `PARTIALLY_CLOSED` UserPosition | 30 seconds | Trusted mark/stop/target/risk/reversal monitoring through the existing PositionMonitor owner |

Cadences are configuration, not database state. The successor must not add a
`nextScanAt` column or another scheduler table merely to represent them. It
must reuse existing Analysis, Opportunity transition, scheduler/idempotency,
UserPosition and PositionMonitor records. Missing cadence configuration fails
closed.

The full Asset Pool receives lightweight scans. Expensive Evidence, score,
Three-AI, Resolver, Rule Validation and Final work is promotion-gated and
checkpoint-triggered. The one-minute triggered path is never permission to
rerun every expensive stage continuously.

## Provider And Truth Contract

The authorized market path is Binance public SPOT, read-only, source-owned,
closed OHLCV for `5m`, `15m`, `1h` and `4h`. A Binance request may not combine
or silently fall back to another provider. Open candles, fixtures and mock data
must not be represented as real. Provider/source, observed time, close time and
freshness remain explicit.

CoinGlass external context remains required by the frozen source for a fully
qualified confirmation chain where that source is mandatory. Missing external
context degrades or blocks according to existing quality/rule gates; it must
not be invented or silently waived.

## Position Monitoring Boundary

Only real authenticated `OPEN` or `PARTIALLY_CLOSED` UserPosition records enter
the 30-second monitor loop. Monitoring may persist trusted monitor facts and
user-facing suggestions through existing owners. It must not automatically
open, close, add, reduce, reverse or submit an order. `CLOSED` positions do not
enter the active loop.

## Telegram Integration Boundary

Closed, unmerged PR #1201 and preserved Head
`b158b7a89a4fdb9bd2254a210ecd258e26032161` are audit/recovery evidence for
the successor implementation only. They are not current merged code and may
not be copied as an authoritative rule set. The successor must review and
integrate only compatible work into the existing Message -> ChannelDelivery ->
Telegram pipeline; it must not create a parallel Telegram stack.

All three frozen in-application Message categories remain. Owner first-release
Telegram Delivery remains narrowed to exactly two categories:

1. validated `CONFIRMATION` Final short alerts;
2. trusted material active-position change short alerts.

Opportunity/plan safety changes remain in-app only. For one user, one `planId`
and the `CONFIRMATION` category, at most one Telegram Delivery may be sent for
the lifetime of that Final. Rechecks, snapshots, analyses, time buckets or
severity must not create duplicate confirmation delivery identity.

The existing three Telegram switches remain default-off. This authorization
does not activate them, access a secret or send a message.

## Runtime Opt-In And Safety

All production automation is explicit opt-in. Repository defaults remain off.
The successor must preserve source trust, quality gates, idempotency, bounded
leases/retries, traceability and fail-closed behavior. It may not enable a
scheduler merely because a production profile is active.

Automatic trading capability count remains `0`. No exchange private API,
order endpoint, order intent, automatic position mutation, Push Recheck trade
authorization or plan-as-position behavior is authorized.

## Product-First Mapping

### Product Contract Mapping

This authorization operationalizes frozen Sections 2-6, 13, 15.2 and 18. It
does not change the eight Opportunity states, five Plan Modes, AI authority,
Final validation, manual-position boundary or Message ownership.

### Design / Interaction Mapping

No route, page, component, Figma, Desktop or Mobile interaction changes are
included. Runtime states continue to use the existing truthful fail-closed UI.

### Data Source Mapping

Reuse AssetPool/PoolItem, Opportunity/StateLog, AnalysisRun, Evidence, Score,
DecisionBundle, Candidate, Resolver, Rule Validation/Final, UserPosition,
PositionMonitorLog, Review, Message and ChannelDelivery owners. Binance closed
OHLCV remains source-owned. No second owner or fabricated value is allowed.

### Current Implementation Gap

Merged main does not recognize this exact package or freeze the production
cadences and integration boundaries above. PR #1201 is closed and unmerged, so
its content is not effective. The successor needs one bounded authorization
before runtime implementation and independent audit.

## Permission After Merged-Main Effectivity

- repository edits: `true`, exact package only;
- implementation: `true`, exact package only;
- Draft PR creation: `true`, exact package only;
- Canonical Figma Desktop implementation: `false`;
- Mobile implementation: `false`;
- Staging deployment in this authorization task: `false`;
- Production deployment: `false`;
- automatic trading: `false`.

Reserved successor branch:
`codex/core-production-loop-automation`.

## Stop Conditions

Stop if implementation requires a duplicate owner, fabricated data, a reduced
quality/trust gate, automatic position mutation, order execution, a schema
column solely for cadence, a second Telegram pipeline, modification of the
frozen Product Source, Figma/Mobile work, secret access, or deployment inside
this docs/gate authorization task.

## Capability Movement

`NONE`. This record grants future permission only. It does not prove runtime
implementation, provider readiness, live Telegram delivery, Staging acceptance
or Production readiness.
