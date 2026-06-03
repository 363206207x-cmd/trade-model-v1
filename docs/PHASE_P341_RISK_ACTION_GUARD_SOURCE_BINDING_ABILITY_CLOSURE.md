# PHASE P341 RiskActionGuard Source Binding Ability Closure

## Purpose

P341 closes the RiskActionGuard source binding skeleton at plan + DTO + validator + assembler + verification level.

The package remains review-only, incomplete-safe, and fail-closed.

## Capability Movement

`MULTITIMEFRAME_CONTEXT_SOURCE_BINDING_ABILITY_CLOSURE -> RISK_ACTION_GUARD_SOURCE_BINDING_ABILITY_CLOSURE`

## Completed Scope

P341 adds a plain Java RiskActionGuard source binding carrier, validator, assembler, targeted tests, and verification documentation.

The assembler only moves explicit input fields into the DTO and immediately runs `RiskActionGuardSourceBindingValidator`.

## Verification Scope

P341 verifies that:

- RiskActionGuard can carry SourceTrace refs, RuntimeKlineContext ref, DataQualityContext ref, and MultiTimeframeContext ref;
- RiskActionGuard is an action review layer, not an execution layer;
- high risk is not immediate stop, reverse, or open;
- strong reversal is not direct reverse;
- wick-only is not trend reversal;
- stampede is blocked fail-closed;
- degraded liquidity cannot become one-shot market cut semantics;
- high risk with normal liquidity can only become review-only degraded context with explicit reason;
- required refs and risk fields are incomplete-safe when missing;
- forbidden executable semantics are blocked fail-closed;
- no executable point values or executable actions are produced.

## Explicit Non-Scope

P341 does not connect:

- service;
- controller / mapper / repository / scheduler;
- dashboard;
- schema / config / pom;
- market data;
- external provider;
- RiskActionGuard runtime source;
- WatchlistPoolProof;
- source-owned candidate integration;
- internal preview;
- external channel;
- Push send;
- order / execution / auto-trading.

## Progress Boundary

P341 does not increase Production Runtime Progress.

It does not authorize real entry / stop / TP / RR, final direction, external push, executable action output, or trading.

## Next Safe Package

Next safe package:

`WatchlistPoolProof Source Binding Ability Closure`

Do not jump directly to service runtime, dashboard runtime, executable point generation, external channel, Push send, or order / execution / auto-trading.
