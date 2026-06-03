# PHASE P339 DataQualityContext Source Binding Ability Closure

## Purpose

P339 closes the DataQualityContext source binding skeleton at plan + DTO + validator + assembler + verification level.

The package remains review-only, incomplete-safe, and fail-closed.

## Capability Movement

`RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_ASSEMBLER_AND_VERIFICATION -> DATA_QUALITY_CONTEXT_SOURCE_BINDING_ABILITY_CLOSURE`

## Completed Scope

P339 adds a plain Java DataQualityContext source binding carrier, validator, assembler, targeted tests, and verification documentation.

The assembler only moves explicit input fields into the DTO and immediately runs `DataQualityContextSourceBindingValidator`.

## Verification Scope

P339 verifies that:

- DataQualityContext can carry SourceTrace refs and RuntimeKlineContext ref;
- data quality score and completeness scores are explicit fields;
- hard threshold failure is blocked fail-closed;
- score below 70 is incomplete;
- score from 70 through 84 is degraded only with explicit degraded reason;
- score 85 or above can remain review-only;
- missing refs are incomplete;
- forbidden executable semantics are blocked fail-closed;
- no executable point values are produced.

## Explicit Non-Scope

P339 does not connect:

- service;
- controller / mapper / repository / scheduler;
- dashboard;
- schema / config / pom;
- market data;
- external provider;
- DataQuality runtime source;
- MultiTimeframeContext;
- RiskActionGuard runtime;
- WatchlistPoolProof;
- external channel;
- Push send;
- order / execution / auto-trading.

## Progress Boundary

P339 does not increase Production Runtime Progress.

It does not authorize real entry / stop / TP / RR, final direction, external push, or trading.

## Next Safe Package

Next safe package:

`MultiTimeframeContext Source Binding Ability Closure`

Do not jump directly to service runtime, dashboard runtime, executable point generation, external channel, Push send, or order / execution / auto-trading.
