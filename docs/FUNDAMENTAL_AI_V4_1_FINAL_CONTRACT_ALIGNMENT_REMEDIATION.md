# Fundamental AI v4.1 Final Contract Alignment Remediation

Status: `COMPLETE_PENDING_INDEPENDENT_FINAL_REAUDIT`

## Scope

This remediation aligns PR #1177 with all chapters 1-20 and Appendices A-D of
`唯一产品开发方案_最终冻结版.docx`. B01-B07 were treated as the minimum known
blockers, not the maximum implementation scope.

No Figma, Mobile, automatic trading, exchange order, automatic position
mutation, or duplicate canonical business stack was added.

## Implementation Summary

### Asset Pool, Opportunity and Home

- Asset Pool is the only source for persistent discovery and Opportunity
  promotion.
- Search preview runs Analysis and Three-AI explanation without adding the
  asset, persisting Opportunity/Candidate/Final, or entering Top 6.
- Home Top 6 is ranked from eligible, fresh Opportunity + Analysis + validated
  Final data with configured weights and deterministic tie-breaking.
- Invalidated, cooling, confused and BLOCKED records are excluded; fewer than
  six results are not backfilled.

### State and decision contracts

- Opportunity has one canonical mutation service, eight states, timeframe
  debounce, cooling and Hot Reset precedence.
- Market Bias uses the exact eight values; Plan Mode uses the exact five values.
- Opportunity State, Market Bias and execution permission are independent.
- Same-family intensity downgrade is auditable; cross-family AI reversal is
  vetoed by rules.

### Three-AI and anti-hallucination

- GPT creates Candidate content only; Gemini reviews Candidate; Grok challenges
  it. None can create Final or mutate state.
- Every role result includes Analysis/Trace identity, exact role state and
  generation time.
- Every formal array has its own collection state.
- GPT evidence and Gemini/Grok findings require verifiable references.
- External-event empty, unavailable, stale and found claims are validated
  against actual source coverage and freshness.
- Success, failure, timeout, fallback and cache-hit role calls remain queryable.

### Candidate, Resolver, Validation and Final

- Candidate and Final remain separate objects and storage records.
- Candidate carries complete rule, evidence, score, decision, risk, validity,
  source and plan-body lineage.
- Conflict Resolver owns before/after bias, confidence, risk and Plan Mode,
  conflict data, downgrade/veto reasons and recovery condition.
- Rule Validation owns final safety and source checks; it is not an AI trace.
- Final requires validated sources, account risk, execution feasibility,
  validity, Candidate/Analysis/Resolver links and Rule Validation PASS.

### Position, monitoring and review

- Final never creates UserPosition. Position creation remains a user action with
  explicit independent-manual or validated-system-plan provenance.
- Existing P2 per-position monitoring and verified/fresh trust rules remain
  intact.
- Review records preserve Candidate, Resolver, Validation, Final and role/rule
  responsibility for executed and missed outcomes.

## PostgreSQL Corrections Found During Real Validation

The real PostgreSQL 16 run found two migration-only defects that H2 could not
expose:

1. historical `tm_asset_state` timestamps were initially backfilled only when
   an Asset identity matched; time backfill is now independent and unknown
   `asset_id` remains null rather than being fabricated;
2. the V11 Candidate Plan Mode constraint was initially removed after enum
   conversion; it is now removed before conversion and replaced by the exact
   five-mode V12 constraint.

The clean V1-to-V12 rerun passed after both corrections.

## Files And Owners

The implementation extends existing owners across:

- AI schema/parser/payload/orchestration and trace query;
- Asset Pool, state machine, ranking and Dashboard projection;
- Candidate, Resolver, Rule Validation and Final Plan;
- Analysis/Evidence/Score/Decision and Review provenance;
- V12 migration and mirrored local schema;
- focused and full regression tests.

The detailed clause-to-file-to-test mapping is in
`docs/FUNDAMENTAL_AI_V4_1_FINAL_CONTRACT_MAPPING.md`.

## Validation

- full Maven: `4497` tests, `4484` passed in the final combined report set,
  `0` failures, `0` errors, `13` environment-only skips;
- isolated PostgreSQL 16.14 migration: `1/1` passed, V1 through V12, no skip;
- Product Source Gate: `PASS`;
- Workflow Contract: `PASS`;
- diff whitespace: `PASS`;
- automatic trading capability count: `0`.

## Outcome

All final-contract mapping rows are `PASS` on the candidate branch.

`READY_FOR_INDEPENDENT_FINAL_REAUDIT`
