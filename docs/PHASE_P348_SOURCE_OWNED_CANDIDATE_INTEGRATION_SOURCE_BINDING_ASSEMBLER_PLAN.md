# PHASE P348 Source-Owned Candidate Integration Source Binding Assembler Plan

## Summary

P348 is a docs-only assembler plan for future Source-Owned Candidate Integration Source Binding.

It prepares the future `SourceOwnedCandidateIntegrationSourceBindingAssembler` package without adding Java or tests.

## Capability Movement

`SOURCE_OWNED_CANDIDATE_INTEGRATION_SOURCE_BINDING_VALIDATOR_VERIFICATION -> SOURCE_OWNED_CANDIDATE_INTEGRATION_SOURCE_BINDING_ASSEMBLER_PLAN`

## Planned Scope

Future assembler will be allowed only to:

- accept explicit `AssemblyInput`;
- move explicit fields into `SourceOwnedCandidateIntegrationSourceBindingDTO`;
- choose the DTO factory from explicit status;
- invoke `SourceOwnedCandidateIntegrationSourceBindingValidator`;
- return context plus validation result.

The future result is not a candidate runtime, point proposal, push payload, final direction, or trade action.

## Explicit Non-Scope

P348 does not add Java, tests, assembler implementation, runtime wiring, source-owned candidate integration runtime, internal preview, service, dashboard, external channel, Push send, order, execution, or auto-trading.

P348 does not generate entry, stop, TP, RR, final direction, candidate runtime output, or executable trade action.

## L0-L7 Boundary

- L0 Source Binding Plan: completed by P344.
- L1 DTO: completed by P345.
- L2 Validator: completed by P346.
- L3 Assembler: not completed; P348 is plan only.
- L4 Verification: P347 completed only DTO + validator stage verification.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

## Safety Boundary

Future assembler must preserve incomplete-safe and fail-closed states.

It must not calculate status, infer direction, read market data, read latest price, read latest close, read Watchlist service, read DB, generate push payload, or create trade action.

## Production Runtime Progress

P348 does not raise Production Runtime Progress.

It does not mean source-owned candidate integration is implemented.

It does not mean review-only point candidates are available.

It does not mean entry / stop / TP / RR can be generated or used.

## Next Safe Package

`P349 SourceOwnedCandidateIntegrationSourceBindingAssembler Java Skeleton`
