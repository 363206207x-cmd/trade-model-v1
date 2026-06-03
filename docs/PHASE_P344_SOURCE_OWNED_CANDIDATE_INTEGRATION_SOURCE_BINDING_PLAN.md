# PHASE P344 Source-Owned Candidate Integration Source Binding Plan

## Purpose

P344 refines the P343 boundary into a future source binding plan for Source-Owned Candidate Integration.

It defines recommended DTO fields, binding statuses, validator rules, assembler rules, fail-closed conditions, incomplete-safe conditions, and output boundaries for a later Java package.

## Capability Movement

`SOURCE_OWNED_CANDIDATE_INTEGRATION_BOUNDARY_PLAN -> SOURCE_OWNED_CANDIDATE_INTEGRATION_SOURCE_BINDING_PLAN`

## Scope

P344 is docs-only.

It does not create DTO, validator, assembler, tests, service wiring, source-owned candidate runtime, dashboard runtime, external channel, Push send, executable point generation, final direction, order, execution, or auto-trading.

## Source Binding Rules

Future source binding must accept only explicit inputs from:

- SourceTrace;
- RuntimeKlineContext;
- DataQualityContext;
- MultiTimeframeContext;
- RiskActionGuard;
- WatchlistPoolProof.

It must preserve review-only, not-trade-instruction, manual-review-required, incomplete-safe, and fail-closed semantics.

## Future Java Split

The next safe Java package is:

`P345 SourceOwnedCandidateIntegrationSourceBindingDTO Java Skeleton`

The DTO package must not include validator, assembler, service, runtime wiring, dashboard runtime, external channel, Push send, order, execution, or auto-trading.

## Progress Boundary

P344 is L0 source binding plan only.

It does not raise Production Runtime Progress and does not mean source-owned candidate integration is implemented or review-only point candidates can be generated.

## Next Safe Package

Next safe package:

`P345 SourceOwnedCandidateIntegrationSourceBindingDTO Java Skeleton`

Do not jump to Java integration assembler, service runtime, dashboard runtime, executable point generation, external channel, Push send, order, execution, or auto-trading.
