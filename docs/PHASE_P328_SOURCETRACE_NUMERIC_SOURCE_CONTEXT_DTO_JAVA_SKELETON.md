# PHASE P328 SourceTraceNumericSourceContextDTO Java Skeleton

## Scope

P328 adds a minimal plain Java DTO skeleton and targeted DTO tests for the future SourceTrace numeric source read model.

It follows P327 `SOURCETRACE_NUMERIC_SOURCE_READ_MODEL_PLAN` and advances the state to `SOURCETRACE_NUMERIC_SOURCE_CONTEXT_DTO_JAVA_SKELETON`.

## Completed

- Added `SourceTraceNumericSourceContextDTO`.
- Added nested enums for source type, numeric field role, freshness, and SourceTrace status.
- Added factories for incomplete, blocked fail-closed, review-only, and degraded contexts.
- Forced review-only, not-trade-instruction, manual-review-required, and incomplete-safe flags to true.
- Forced fail-closed when the DTO status is blocked.
- Added targeted tests for safety flags, factory reason requirements, enum coverage, explicit numeric field carry, null-safe numeric fields, dependency boundaries, and forbidden executable semantics.

## Safety Boundary

The DTO is only a carrier.

It does not validate SourceTrace completeness.

It does not assemble source-owned candidate input.

It does not connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, or RiskActionGuardContext.

It does not calculate entry, stop, TP, RR, final direction, order intent, execution intent, or auto-trading behavior.

## Non-Scope

P328 does not add validators, assemblers, services, controllers, mappers, repositories, schedulers, dashboard runtime, resources, schema, config, pom changes, external channel, Push send, order, execution, or auto-trading.

P328 does not make SourceTrace runtime reads available.

P328 does not make entry / stop / TP / RR available as trading output.

## Conclusion

P328 is a DTO-only Java/test skeleton.

It does not raise Production Runtime Progress.

It does not authorize executable point generation or any trade action.

The recommended next package is `SourceTraceNumericSourceReadModelValidator` Java Skeleton or SourceTrace Numeric Source DTO Verification, not RuntimeKlineContext wiring, service runtime, dashboard runtime, external push, order, execution, or auto-trading.
