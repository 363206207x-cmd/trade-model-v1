# PHASE P323 Source-owned Numeric Point Candidate Assembler Plan

## Scope

P323 is a docs-only planning package for the Readiness / Point Mainline.

It follows P322 `REVIEW_ONLY_NUMERIC_POINT_ASSEMBLER_JAVA_SKELETON` and advances the state to `SOURCE_OWNED_NUMERIC_POINT_CANDIDATE_ASSEMBLER_PLAN`.

## Completed In This Phase

P323 defines the future Source-owned Numeric Point Candidate Assembler boundary:

- how it differs from `ReviewOnlyNumericPointProposalAssembler`;
- how it must use `ReviewOnlyNumericPointProposalDTO`;
- how it must call `NumericPointSafetyValidator`;
- which source-owned contexts may be read;
- which latest-price, display, AI prose, score, label, provider, DB write, external channel, and execution sources remain forbidden;
- entry candidate rules;
- stop candidate rules;
- TP candidate rules;
- RR candidate rules;
- `REVIEW_ONLY_NUMERIC_POINT_CANDIDATE`, degraded, `INCOMPLETE`, and `BLOCKED_FAIL_CLOSED` status rules;
- future Java fixture expectations.

## Explicit Non-Scope

P323 does not add Java.

P323 does not add tests.

P323 does not add DTO, validator, assembler, service, controller, mapper, repository, scheduler, schema, config, dashboard, or resources changes.

P323 does not implement numeric point generation.

P323 does not generate executable entry / stop / TP / RR.

P323 does not generate final direction.

P323 does not connect external channel, Push send, order, execution, or auto-trading.

## Safety Conclusion

P323 does not increase Production Runtime Progress.

P323 does not authorize real point generation.

P323 does not authorize external channel.

P323 does not authorize order / execution / auto-trading.

The next recommended package is Source-owned Numeric Point Candidate Assembler Java Skeleton or P323 verification, not real executable point generation.
