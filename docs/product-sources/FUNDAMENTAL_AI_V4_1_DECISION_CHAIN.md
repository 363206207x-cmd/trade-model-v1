# Fundamental AI v4.1 Decision Chain

Status: `PRODUCT_DESIGN_FROZEN`

Original source: `/Users/xuchao/Desktop/Fundamental_AI_v4_1_Codex_v2.docx`

Original SHA-256: `0aea7af215045df2b49430bdbde601910825de5248f53b37de977c11927da2e7`

This repository record is the canonical text representation of the Product
Owner-approved Fundamental AI v4.1 decision-chain source. It freezes product
scope and authority. It does not claim implementation, runtime acceptance, or
production readiness.

## 1. Closed Decision Chain

The required backend chain is:

1. Asset Pool.
2. Opportunity Discovery and Opportunity State Machine.
3. Analysis Run.
4. Evidence.
5. Score.
6. Decision Bundle.
7. GPT_FINAL ExecutionPlanCandidate.
8. GEMINI_REVIEW Candidate Review.
9. GROK_CHALLENGE Risk Challenge.
10. Conflict Resolver.
11. Rule Validation.
12. Final Execution Plan.
13. Explicit manual UserPosition.
14. Position Monitoring.
15. Review.

The system never automatically opens, closes, reduces, adds, reverses, or
orders a position.

## 2. Asset Pool

Asset Pool is the only opportunity source. It must support a system default
pool, a user pool, full-market and fuzzy search, explicit add, explicit remove,
restore default, and scan analysis. The six Home focus assets are projections
from Asset Pool. A fixed BTC/ETH/SOL-only opportunity source or any path that
bypasses Asset Pool is forbidden.

## 3. Opportunity State Machine

The states are exactly:

- `observing`
- `candidate`
- `waiting_trigger`
- `triggered`
- `high_risk`
- `invalidated`
- `cooling`
- `confused`

All state changes use one canonical entry point. Every transition records
`from_state`, `to_state`, `reason`, `trigger_source`, `timestamp`,
`opportunity_id`, and `analysis_id`. Debounce and cooling are required.
Precedence is `Hot Reset > Confused > Invalidated > ordinary transition`.
`triggered` is an opportunity state and never means a position was opened.

## 4. AI Authority

### GPT_FINAL

GPT_FINAL is the `ExecutionPlanCandidate` generator. Its input includes the
asset analysis, evidence, eight scores, multi-timeframe state, rule-base
direction, data-quality score, confused score, and risk state. It cannot create
the FinalExecutionPlan, mutate the state machine, or bypass the rule layer.

### GEMINI_REVIEW

GEMINI_REVIEW reviews the candidate and returns a `ReviewResult`. It cannot
generate a plan or mutate the state machine.

### GROK_CHALLENGE

GROK_CHALLENGE supplies adversarial risk and counter-evidence through a
`ChallengeResult`. It cannot generate a plan or mutate the state machine.

### Rule Validation

Rule Validation owns final risk validation, state validation, and confirmation
of the FinalExecutionPlan. AI roles are not voters and cannot replace this
authority.

Every AI call records `trace_id`, `analysis_id`, `input_hash`, model, output,
token cost, and latency. AI failure falls back to the rule path and records the
fallback; it does not fabricate AI output.

## 5. Candidate, Conflict, and Final Plan

The mandatory sequence is:

`Rule Base Analysis -> GPT Candidate -> Gemini Review -> Grok Challenge -> Conflict Resolver -> Rule Validation -> Final Execution Plan`.

Candidate and Final are separate objects and separate persisted identities.
Final references `candidate_id` and `analysis_id`. No endpoint may return an
unvalidated Candidate as an executable Final plan.

Conflict Resolver consumes rule direction/confidence/risk, the GPT Candidate,
Gemini Review, Grok Challenge, data-quality score, confused score, and account
risk. Its result records conflict level and score, plan mode before/after,
confidence before/after, risk before/after, downgrade reason, confused
decision, and rule-veto reason.

Opportunity state is separate from execution permission. AI conflict may
adjust confidence, risk, plan mode, or Confused. It cannot permanently discard
the opportunity or directly break the state machine. Downgraded results retain
the original opportunity state, downgrade reason, and conflict reason. Only
Confused/BLOCKED may pause directional execution advice.

## 6. Ownership and Traceability

The implementation reuses existing `AnalysisRun`, `EvidenceItem`, `ScoreItem`,
`DecisionBundle`, `UserPosition`, `PositionMonitorLog`, and `Review` ownership.
It introduces or reconciles only the canonical owners needed for Opportunity,
ExecutionPlanCandidate, AITrace, and ConflictResolverResult, and extends the
existing ExecutionPlan and DecisionBundle contracts. Existing objects must not
be duplicated.

FinalExecutionPlan references Candidate and Analysis. UserPosition references
the Final plan only after an authenticated explicit manual action. AI records
reference Trace and Analysis. State-transition audit records reference
Opportunity and Analysis.

The audit chain preserves GPT input summary/hash, GPT Candidate, Gemini Review,
Grok Challenge, Conflict Resolver result, Rule Validation result, and every
final modification, downgrade, or veto reason. Every critical write carries a
`traceId`.

## 7. Position and Review Boundary

FinalExecutionPlan is a system decision artifact. It is never a UserPosition.
Only an explicit manual position record starts Position Monitoring. Review
consumes the traceable opportunity, analysis, plan, user-position, and monitor
chain without creating trading authority.

## 8. Permanent Prohibitions

- no automatic open;
- no automatic close;
- no automatic reverse;
- no automatic order;
- no plan-to-position conversion;
- no AI state-machine mutation;
- no AI bypass of rule validation;
- no Candidate exposed as a Final plan;
- no opportunity source outside Asset Pool;
- no duplicate canonical object family;
- no Figma or Mobile change in the backend implementation package.
