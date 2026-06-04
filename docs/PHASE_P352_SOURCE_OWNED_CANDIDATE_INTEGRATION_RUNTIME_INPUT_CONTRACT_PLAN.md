# PHASE P352 Source-Owned Candidate Integration Runtime Input Contract Plan

## Summary

P352 is the docs-only runtime input contract plan after the P351 runtime boundary.

It defines allowed future input sources, forbidden runtime reads, required input fields, incomplete input conditions, blocked fail-closed input conditions, and next safe package boundaries.

The runtime input contract includes `missingReason` for incomplete-state traceability.

## Capability Movement

`SOURCE_OWNED_CANDIDATE_INTEGRATION_RUNTIME_BOUNDARY_PLAN -> SOURCE_OWNED_CANDIDATE_INTEGRATION_RUNTIME_INPUT_CONTRACT_PLAN`

## Completed Scope

- Runtime input contract planning;
- allowed source binding input sources;
- forbidden runtime input sources;
- required input field set;
- incomplete input rules;
- blocked fail-closed input rules;
- Watchlist Pool boundary;
- Risk Action Guard boundary;
- disabled-by-default inheritance.

## Explicit Non-Scope

P352 does not add Java, tests, runtime DTOs, validators, orchestrators, service wiring, internal preview, dashboard runtime, external channel, Push send, order, execution, or auto-trading.

P352 does not generate candidates, entry, stop, TP, RR, final direction, push payload, or executable trade actions.

## L0-L7 Boundary

Source-Owned Candidate Integration Source Binding is complete through L4 after P350.

Source-Owned Candidate Integration Runtime remains:

- L0 Runtime Boundary Plan: complete in P351.
- Runtime Input Contract Plan: P352 planning.
- L1 Runtime DTO: not completed.
- L2 Runtime Validator: not completed.
- L3 Runtime Assembler / Orchestrator: not completed.
- L4 Runtime Verification: not completed.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

## Production Runtime Progress

P352 does not raise Production Runtime Progress.

It does not mean runtime DTO is implemented.

It does not mean source-owned candidate integration runtime is implemented.

It does not mean review-only point candidates are available.

It does not mean entry / stop / TP / RR can be generated or used.

## Next Safe Package

`P353 Source-Owned Candidate Integration Runtime DTO Plan`
