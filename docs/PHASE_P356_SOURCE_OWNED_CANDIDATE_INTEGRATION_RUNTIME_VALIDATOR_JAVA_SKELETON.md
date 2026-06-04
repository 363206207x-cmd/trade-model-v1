# PHASE P356 Source-Owned Candidate Integration Runtime Validator Java Skeleton

## Summary

P356 adds the Source-Owned Candidate Integration Runtime Validator Java skeleton and targeted tests.

This package completes L2 Runtime Validator only.

It does not implement Runtime Assembler, Runtime Orchestrator, service wiring, dashboard runtime, source-owned candidate runtime, point generation, push sending, external channel, order, execution, or auto-trading.

## Files Added

- `src/main/java/org/example/trademodel/validator/point/SourceOwnedCandidateIntegrationRuntimeCandidateValidator.java`
- `src/test/java/org/example/trademodel/validator/point/SourceOwnedCandidateIntegrationRuntimeCandidateValidatorTest.java`
- `docs/P356.md`
- `docs/PHASE_P356_SOURCE_OWNED_CANDIDATE_INTEGRATION_RUNTIME_VALIDATOR_JAVA_SKELETON.md`

## Validator Contract

`SourceOwnedCandidateIntegrationRuntimeCandidateValidator` validates only explicit `SourceOwnedCandidateIntegrationRuntimeCandidateDTO` data.

It checks:

- null context and null runtime status;
- forced review-only safety flags;
- required upstream refs;
- observedAt;
- source binding completeness score;
- upstream trust and safety flag summaries;
- source blocked / incomplete / degraded flags;
- RiskActionGuard blocked / stampede states;
- WatchlistPoolProof membership and freshness;
- incomplete reason requirements;
- blocked reason requirements;
- degraded reason requirements;
- forbidden executable semantics in public string and list fields.

It returns a safe `ValidationResult` with immutable reasons and forced review-only safety flags.

## Status Boundary

Validator statuses are:

- `INCOMPLETE`;
- `BLOCKED_FAIL_CLOSED`;
- `REVIEW_ONLY_RUNTIME_CANDIDATE_VALID`;
- `REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED`.

`REVIEW_ONLY_RUNTIME_CANDIDATE_VALID` is only a review-only status validation result.

It is not a point, not a direction, not a push payload, not an action, and not a trade instruction.

## Non-Scope

P356 does not:

- add Runtime Assembler;
- add Runtime Orchestrator;
- connect service runtime;
- connect dashboard runtime;
- read market data;
- read latest price or latest close;
- read Watchlist service, rule config, or audit table;
- generate candidate runtime output;
- generate entry / stop / TP / RR;
- generate final direction;
- send Push;
- connect external channel;
- connect order, execution, or auto-trading.

## L0-L7 Runtime State

Source-Owned Candidate Integration Source Binding is complete through L0-L4.

Source-Owned Candidate Integration Runtime is now:

- L0 Runtime Boundary Plan: completed by P351.
- Runtime Input Contract Plan: completed by P352.
- Runtime DTO Plan: completed by P353.
- L1 Runtime DTO: completed by P354.
- Runtime Validator Plan: completed by P355.
- L2 Runtime Validator: completed by P356.
- L3 Runtime Assembler / Orchestrator: not completed.
- L4 Runtime Verification: not completed.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

P356 does not increase Production Runtime Progress.

## Next Safe Package

P357 should be `Source-Owned Candidate Integration Runtime Validator Verification`.

Do not jump directly to Runtime Assembler, Runtime Orchestrator, service runtime, dashboard runtime, executable point generation, external channel, Push send, order, execution, or auto-trading.
