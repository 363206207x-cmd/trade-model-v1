# PHASE P342 WatchlistPoolProof Source Binding Ability Closure

## Purpose

P342 closes the WatchlistPoolProof source binding skeleton at plan + DTO + validator + assembler + verification level.

The package remains review-only, incomplete-safe, and fail-closed.

## Capability Movement

`RISK_ACTION_GUARD_SOURCE_BINDING_ABILITY_CLOSURE -> WATCHLIST_POOL_PROOF_SOURCE_BINDING_ABILITY_CLOSURE`

## Completed Scope

P342 adds a plain Java WatchlistPoolProof source binding carrier, validator, assembler, targeted tests, and verification documentation.

The assembler only moves explicit input fields into the DTO and immediately runs `WatchlistPoolProofSourceBindingValidator`.

## Verification Scope

P342 verifies that:

- Watchlist Pool is the maximum candidate / opportunity-push boundary;
- Display Slots and default home assets are not Watchlist Pool proof;
- default display does not authorize push candidacy;
- missing membership is blocked fail-closed;
- disabled or empty Watchlist Pool is blocked fail-closed;
- stale or not-fresh proof cannot pass silently;
- missing audit ref with valid membership is incomplete or degraded;
- promoted-to-home and low-frequency-scan flags remain review-only labels;
- forbidden executable semantics are blocked fail-closed;
- no executable point values, push sends, or executable actions are produced.

## Explicit Non-Scope

P342 does not connect:

- service;
- controller / mapper / repository / scheduler;
- dashboard;
- schema / config / pom;
- market data;
- Watchlist runtime;
- Watchlist service;
- rule config;
- audit table;
- external provider;
- source-owned candidate integration;
- internal preview;
- external channel;
- Push send;
- order / execution / auto-trading.

## Progress Boundary

P342 does not increase Production Runtime Progress.

It does not authorize real entry / stop / TP / RR, final direction, external push, executable action output, or trading.

## Next Safe Package

Next safe package:

`Source-Owned Candidate Integration Boundary Plan`

or:

`Source-Owned Candidate Integration Source Binding Plan`

Do not jump directly to service runtime, dashboard runtime, executable point generation, external channel, Push send, or order / execution / auto-trading.
