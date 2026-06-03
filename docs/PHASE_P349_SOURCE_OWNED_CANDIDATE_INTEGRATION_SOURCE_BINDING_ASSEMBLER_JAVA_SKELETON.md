# PHASE P349 Source-Owned Candidate Integration Source Binding Assembler Java Skeleton

## Summary

P349 adds the minimal Java assembler skeleton for Source-Owned Candidate Integration Source Binding.

It moves explicit `AssemblyInput` fields into `SourceOwnedCandidateIntegrationSourceBindingDTO`, immediately invokes `SourceOwnedCandidateIntegrationSourceBindingValidator`, and returns context plus validation result.

## Capability Movement

`SOURCE_OWNED_CANDIDATE_INTEGRATION_SOURCE_BINDING_ASSEMBLER_PLAN -> SOURCE_OWNED_CANDIDATE_INTEGRATION_SOURCE_BINDING_ASSEMBLER_JAVA_SKELETON`

## Completed Scope

- `SourceOwnedCandidateIntegrationSourceBindingAssembler`;
- `SourceOwnedCandidateIntegrationSourceBindingAssemblerTest`;
- docs/status updates.

## Explicit Non-Scope

P349 does not add runtime wiring, source-owned candidate integration runtime, internal preview, service, dashboard, external channel, Push send, order, execution, or auto-trading.

P349 does not generate entry, stop, TP, RR, final direction, candidate runtime output, or executable trade action.

## L0-L7 Boundary

- L0 Source Binding Plan: completed by P344.
- L1 DTO: completed by P345.
- L2 Validator: completed by P346.
- L3 Assembler: completed by P349.
- L4 Full Verification: not completed.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

## Production Runtime Progress

P349 does not raise Production Runtime Progress.

It does not mean source-owned candidate integration is implemented.

It does not mean review-only point candidates are available.

It does not mean entry / stop / TP / RR can be generated or used.

## Next Safe Package

`P350 SourceOwnedCandidateIntegrationSourceBindingAssembler Verification`
