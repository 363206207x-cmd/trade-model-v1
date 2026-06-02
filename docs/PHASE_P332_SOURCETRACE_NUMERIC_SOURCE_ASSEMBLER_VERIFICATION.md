# Phase P332 SourceTrace Numeric Source Assembler Verification

## Summary

P332 is a docs-only verification closure package for P331.

It verifies that P331 added only a plain Java SourceTrace numeric source read-model assembler skeleton and targeted tests.

## Capability Movement

`SOURCETRACE_NUMERIC_SOURCE_READ_MODEL_ASSEMBLER_JAVA_SKELETON -> SOURCETRACE_NUMERIC_SOURCE_ASSEMBLER_VERIFICATION`

## Verification Scope

P332 verifies:

- `SourceTraceNumericSourceReadModelAssembler` only moves explicit `AssemblyInput` fields into `SourceTraceNumericSourceContextDTO`;
- the assembler immediately calls `SourceTraceNumericSourceReadModelValidator`;
- the assembler returns both `context` and `validationResult`;
- null input remains `INCOMPLETE`;
- blocked input remains fail-closed;
- degraded input remains degraded review-only;
- review-only input remains review-only;
- forbidden source types remain rejected by validator;
- safety flags remain required true;
- RiskActionGuard ref remains required by validation;
- missing / stale / unknown conditions remain incomplete-safe;
- explicitly provided `BigDecimal` values are preserved;
- safe outputs do not produce executable semantics.

## Non-Scope

P332 does not add Java, tests, runtime source reads, RuntimeKlineContext wiring, DataQuality wiring, MultiTimeframe wiring, RiskActionGuard wiring, WatchlistPoolProof, service wiring, dashboard runtime, executable point generation, external channel, order, execution, or auto-trading.

## Required Checks

- `bash scripts/check-workflow-contract.sh`
- `git diff --name-only`
- `git diff --check`

## Next Safe Package

The next safe package should be SourceTrace Runtime / Source Binding Plan, not runtime wiring.
