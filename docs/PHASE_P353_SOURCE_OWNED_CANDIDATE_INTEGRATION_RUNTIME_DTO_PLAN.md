# PHASE P353 Source-Owned Candidate Integration Runtime DTO Plan

## Summary

P353 is the docs-only runtime DTO plan after the P351 runtime boundary and P352 runtime input contract.

It defines the future runtime candidate DTO name, recommended fields, runtime status values, forced safety flags, factory rules, forbidden execution-shaped fields, incomplete-safe boundaries, blocked fail-closed boundaries, and next Java DTO skeleton boundary.

It inherits the P352 follow-up fix that added `missingReason` to the runtime input contract for incomplete-state traceability.

## Capability Movement

`SOURCE_OWNED_CANDIDATE_INTEGRATION_RUNTIME_INPUT_CONTRACT_PLAN -> SOURCE_OWNED_CANDIDATE_INTEGRATION_RUNTIME_DTO_PLAN`

## Completed Scope

- Runtime DTO plan;
- future `SourceOwnedCandidateIntegrationRuntimeCandidateDTO` positioning;
- recommended DTO fields;
- `missingReason` as a future DTO field for incomplete-state traceability;
- recommended runtime status values;
- safety flag and factory rules;
- forbidden DTO output fields;
- incomplete-safe DTO boundaries;
- blocked fail-closed DTO boundaries;
- Watchlist Pool boundary inheritance;
- Risk Action Guard boundary inheritance;
- disabled-by-default inheritance.

## Explicit Non-Scope

P353 does not add Java, tests, runtime DTO implementation, runtime validators, runtime assemblers, runtime orchestrators, service wiring, internal preview, dashboard runtime, external channel, Push send, order, execution, or auto-trading.

P353 does not generate candidates, entry, stop, TP, RR, final direction, push payload, point proposal, or executable trade actions.

## L0-L7 Boundary

Source-Owned Candidate Integration Source Binding is complete through L4 after P350.

Source-Owned Candidate Integration Runtime remains:

- L0 Runtime Boundary Plan: completed in P351.
- Runtime Input Contract Plan: completed in P352.
- Runtime DTO Plan: P353 planning only.
- L1 Runtime DTO: not completed.
- L2 Runtime Validator: not completed.
- L3 Runtime Assembler / Orchestrator: not completed.
- L4 Runtime Verification: not completed.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

## Production Runtime Progress

P353 does not raise Production Runtime Progress.

It does not mean runtime DTO is implemented.

It does not mean source-owned candidate integration runtime is implemented.

It does not mean review-only point candidates are available.

It does not mean entry / stop / TP / RR can be generated or used.

## Next Safe Package

`P354 Source-Owned Candidate Integration Runtime DTO Java Skeleton`
