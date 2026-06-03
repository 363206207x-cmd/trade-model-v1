# PHASE P343 Source-Owned Candidate Integration Boundary Plan

## Purpose

P343 defines the L0 boundary before any future Source-Owned Candidate Integration work.

It confirms that P335-P342 source binding skeletons are closed only at L0-L4 and must not be treated as runtime-safe candidate generation.

## Capability Movement

`WATCHLIST_POOL_PROOF_SOURCE_BINDING_ABILITY_CLOSURE -> SOURCE_OWNED_CANDIDATE_INTEGRATION_BOUNDARY_PLAN`

## Verification Scope

P343 verifies the future candidate integration boundary:

- all required source bindings must be review-only;
- all required source bindings must be incomplete-safe / fail-closed;
- SourceTrace refs remain mandatory;
- RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuard, and WatchlistPoolProof remain mandatory;
- Watchlist Pool remains the maximum candidate / opportunity-push boundary;
- Display Slots are not Watchlist Pool proof;
- Risk Action Guard remains mandatory before candidate integration can become review-ready;
- executable semantics remain blocked.

## Explicit Non-Scope

P343 is docs-only.

It does not add Java, tests, service, controller, mapper, repository, scheduler, runtime wiring, dashboard runtime, market reads, latest price reads, latest close reads, external channel, Push send, executable point generation, final direction, order, execution, or auto-trading.

## Boundary Rules

Future candidate integration must be `INCOMPLETE` or `BLOCKED_FAIL_CLOSED` whenever required source bindings are missing, stale, untrusted, conflicted, blocked, or executable.

Future candidate integration may only output review-only readiness / unavailable / blocked / degraded labels and trace refs.

It must not output executable entry, stop, TP, RR, final direction, push send payload, external channel message, order intent, execution intent, or auto-trading action.

## Progress Boundary

P343 does not raise Production Runtime Progress.

It does not mean source-owned candidate integration is implemented, review-only numeric point candidate is available, dashboard preview is available, external push is authorized, or trading can execute.

## Next Safe Package

Next safe package:

`P344 Source-Owned Candidate Integration Source Binding Plan`

Do not jump to Java integration assembler, service runtime, dashboard runtime, executable point generation, external channel, Push send, order, execution, or auto-trading.
