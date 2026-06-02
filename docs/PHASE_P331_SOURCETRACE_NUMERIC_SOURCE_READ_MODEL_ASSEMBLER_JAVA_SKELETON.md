# Phase P331 SourceTrace Numeric Source Read Model Assembler Java Skeleton

## Summary

P331 adds a plain Java SourceTrace read-model assembler skeleton and targeted tests.

The assembler only turns explicit source trace read-model input into `SourceTraceNumericSourceContextDTO`, then immediately calls `SourceTraceNumericSourceReadModelValidator`.

## Capability Movement

`SOURCETRACE_NUMERIC_SOURCE_VALIDATOR_VERIFICATION -> SOURCETRACE_NUMERIC_SOURCE_READ_MODEL_ASSEMBLER_JAVA_SKELETON`

## Added Java Scope

- `SourceTraceNumericSourceReadModelAssembler`
- `SourceTraceNumericSourceReadModelAssemblerTest`

## Safety Boundary

P331 does not calculate, infer, derive, or generate any point value.

P331 does not read market data, latest price, latest close, score labels, AI prose, dashboard text, or external providers.

P331 does not connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, service runtime, dashboard runtime, external channel, order, execution, or auto-trading.

## Required Verification

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- `./mvnw -q -Dtest=SourceTraceNumericSourceReadModelAssemblerTest test`

## Next Safe Package

The next safe package should be SourceTrace Numeric Source Assembler Verification.

It should not directly become RuntimeKlineContext wiring, service runtime, dashboard runtime, executable point generation, external push, order, execution, or auto-trading.
