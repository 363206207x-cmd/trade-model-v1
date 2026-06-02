# PHASE P324 Source-owned Numeric Point Candidate Assembler Java Skeleton

## Scope

P324 is a minimal Java assembler / targeted-test package for the Readiness / Point Mainline.

It follows P323 `SOURCE_OWNED_NUMERIC_POINT_CANDIDATE_ASSEMBLER_PLAN` and advances the state to `SOURCE_OWNED_NUMERIC_POINT_CANDIDATE_ASSEMBLER_JAVA_SKELETON`.

## Completed In This Phase

P324 adds:

- a plain Java `SourceOwnedNumericPointCandidateAssembler`;
- nested source-owned context classes for entry, stop, TP, and RR;
- nested `SourceOwnedAssembledNumericPoint`;
- source-owned context to `ReviewOnlyNumericPointProposalAssembler.AssemblyInput` conversion;
- mandatory `ReviewOnlyNumericPointProposalAssembler` invocation;
- mandatory `NumericPointSafetyValidator` validation through the P322 assembler;
- trusted-source fail-closed handling;
- targeted tests for null, complete, missing refs, missing point fields, degraded, untrusted source, forbidden semantics, assembler invocation, explicit BigDecimal preservation, and forbidden dependency boundaries.

## Explicit Non-Scope

P324 does not add service Java.

P324 does not add controller, mapper, repository, scheduler, schema, config, dashboard, or resource changes.

P324 does not implement numeric point generation.

P324 does not generate executable entry / stop / TP / RR.

P324 does not generate final direction.

P324 does not connect external channel, Push send, order, execution, or auto-trading.

## Safety Conclusion

P324 does not increase Production Runtime Progress.

P324 does not authorize real point generation.

P324 does not authorize external channel.

P324 does not authorize order / execution / auto-trading.

The next recommended package is Source-owned Numeric Point Candidate Assembler Verification or Review-only Numeric Point Internal Preview Plan, not real point generation.
