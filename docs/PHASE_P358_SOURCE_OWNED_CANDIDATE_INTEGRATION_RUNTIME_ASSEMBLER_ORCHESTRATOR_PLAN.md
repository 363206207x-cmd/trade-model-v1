# PHASE P358 Source-Owned Candidate Integration Runtime Assembler / Orchestrator Plan

## Summary

P358 defines the docs-only plan for the future Source-Owned Candidate Integration Runtime Assembler / Orchestrator.

It follows the P357 Runtime DTO + Validator verification.

It does not add Java, tests, Runtime Assembler implementation, Runtime Orchestrator implementation, service, dashboard runtime, source-owned candidate runtime, point generation, push sending, external channel, order, execution, or auto-trading.

## Recommended Minimal Implementation Name

The recommended smallest future class name is:

`SourceOwnedCandidateIntegrationRuntimeCandidateAssembler`

The broader `SourceOwnedCandidateIntegrationRuntimeCandidateOrchestrator` name should be reserved only if future work truly coordinates multiple runtime steps beyond explicit DTO assembly and validation.

## Planned Assembly Shape

Future assembly should:

- receive explicit `SourceOwnedCandidateIntegrationRuntimeAssemblyInput`;
- move explicit input fields into `SourceOwnedCandidateIntegrationRuntimeCandidateDTO`;
- choose DTO factory by explicit requested runtime status;
- call `SourceOwnedCandidateIntegrationRuntimeCandidateValidator.validate(...)`;
- return `AssembledSourceOwnedCandidateIntegrationRuntimeCandidate` containing context and validation result;
- preserve review-only, not-trade-instruction, manual-review-required, incomplete-safe, and fail-closed boundaries.

## Planned Non-Scope

The future assembler / orchestrator must not:

- read market data;
- read latest price or latest close;
- read Watchlist service;
- read rule config or audit table;
- read DB;
- call service;
- generate point proposal;
- generate final direction;
- generate push payload;
- generate trade action;
- bypass validator;
- mutate failed DTOs into valid DTOs.

## Planned Status Rules

Future assembly must preserve:

- input null, unsupported requestedRuntimeStatus, missing refs, stale source, incomplete source, low completeness, or only latest price / close / dashboard text / AI prose -> `INCOMPLETE`;
- blocked source validation, untrusted source, disabled safety flags, RiskActionGuard blocks, WatchlistPoolProof blocks, Display Slots used as proof, or executable semantics -> `BLOCKED_FAIL_CLOSED`;
- degraded status only with degraded reasons and without blocked conditions -> `REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED`;
- full review-only valid only when all required refs, source trust, source safety flags, WatchlistPoolProof, RiskActionGuard, RuntimeKline, DataQuality, and MultiTimeframe gates pass.

Review-only valid status is not a point, direction, push payload, or trade action.

## Watchlist And Risk Boundaries

Watchlist Pool remains the maximum candidate boundary.

Display Slots and the default six home assets are not Watchlist Pool Proof.

Risk high does not mean immediate stop, reverse, or open.

Strong reversal does not mean direct reverse.

Wick-only does not mean trend reversal.

Stampede forbids opportunity push, reverse, and new open.

Liquidity degradation forbids one-shot market cut semantics.

## Disabled-By-Default Boundary

Future runtime assembler / orchestrator work must remain disabled by default.

It must not affect production decisions, dashboard, Push, point proposal, or current decision result by default.

When enabled, it may only output review-only runtime candidate status.

## L0-L7 Runtime State

Source-Owned Candidate Integration Runtime:

- L0 Runtime Boundary Plan: completed by P351.
- Runtime Input Contract Plan: completed by P352.
- Runtime DTO Plan: completed by P353.
- L1 Runtime DTO: completed by P354.
- Runtime Validator Plan: completed by P355.
- L2 Runtime Validator: completed by P356.
- Runtime Validator Verification: completed by P357.
- Runtime Assembler / Orchestrator Plan: P358.
- L3 Runtime Assembler / Orchestrator: not completed.
- L4 Runtime Verification: not completed.
- L5 Internal wiring: not completed.
- L6 Dashboard preview: not completed.
- L7 Runtime-safe usable: not completed.

P358 does not increase Production Runtime Progress.

## Next Safe Package

P359 should be `Source-Owned Candidate Integration Runtime Assembler / Orchestrator Java Skeleton`.

Do not jump directly to service runtime, dashboard runtime, executable point generation, external channel, Push send, order, execution, or auto-trading.
