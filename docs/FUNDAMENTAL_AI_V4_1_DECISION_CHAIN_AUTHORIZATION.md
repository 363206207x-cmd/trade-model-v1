# Fundamental AI v4.1 Decision Chain Implementation Authorization

Status: `HISTORICAL_REFERENCE_ONLY / SUPERSEDED`

Superseded on `2026-08-14` by
`docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md`.
This document records the already-delivered backend authorization and is not
an ACTIVE Product Source or current implementation permission.

AUTHORIZATION_STATUS: `AUTHORIZED_PENDING_MERGED_MAIN`

AUTHORIZED_PACKAGE: `FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION`

PRODUCT_SOURCE: `FUNDAMENTAL_AI_V4_1_DECISION_CHAIN`

PRODUCT_DESIGN_STATUS: `FROZEN`

## Authorization Decision

The Product Owner explicitly authorizes one bounded OneShot backend package
for the frozen Fundamental AI v4.1 decision chain. This authorization becomes
effective only after this record is independently reviewed, merged to `main`,
and validated from a clean worktree whose local `main` matches `origin/main`.

Before merged-main effectivity:

- `IMPLEMENTATION_ALLOWED=false`
- `PR_CREATION_ALLOWED=false`

After merged-main effectivity, only an exact request for
`FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION` may resolve to:

- `IMPLEMENTATION_ALLOWED=true`
- `PR_CREATION_ALLOWED=true`

A differently named, broader, Mobile, Figma, trading, or unrelated package
must fail closed.

## Authorized Scope

The package may implement, integrate, persist, expose, and test only:

1. Asset Pool as the sole opportunity source, including default/user pool,
   search, fuzzy search, add, remove, restore default, and scan analysis.
2. Opportunity Discovery and the eight-state Opportunity state machine with a
   single transition entry point, transition audit, debounce, cooling, and Hot
   Reset/Confused/Invalidated precedence.
3. The existing AnalysisRun, EvidenceItem, ScoreItem, and DecisionBundle chain.
4. GPT_FINAL generation of ExecutionPlanCandidate only.
5. GEMINI_REVIEW candidate review only.
6. GROK_CHALLENGE adversarial risk challenge only.
7. Conflict Resolver output and Rule Validation as the Final confirmation
   authority.
8. Separate persisted ExecutionPlanCandidate and FinalExecutionPlan identities,
   with Candidate/Analysis trace links and no unvalidated Candidate exposure.
9. AI trace completion for input hash, model, output, token cost, latency,
   fallback, trace ID, and analysis ID.
10. Review-chain extension over the existing Review owner.
11. Tests, migrations, and API changes strictly required by this bounded
    backend contract.

## Frozen AI Permission Contract

| Actor | Authorized authority | Forbidden authority |
|---|---|---|
| GPT_FINAL | generate `ExecutionPlanCandidate` | generate FinalExecutionPlan, mutate state, bypass rules |
| GEMINI_REVIEW | review Candidate and return ReviewResult | generate a plan, mutate state |
| GROK_CHALLENGE | produce Risk Challenge/ChallengeResult | generate a plan, mutate state |
| Conflict Resolver | calculate conflict/downgrade/confused/rule-veto result | create UserPosition or execute a trade |
| Rule Validation | final risk/state validation and FinalExecutionPlan confirmation | automatic position mutation or order execution |

AI failure must use and audit the rule fallback. No AI role may replace the
rule-layer base direction or become a three-way voting system.

## Frozen Object Ownership

The implementation must follow
`docs/FUNDAMENTAL_AI_V4_1_OBJECT_OWNERSHIP_MAP.md`.

Reuse:

- AnalysisRun
- EvidenceItem
- ScoreItem
- DecisionBundle
- UserPosition
- PositionMonitorLog
- Review

Extend/reconcile the existing canonical owners for:

- Opportunity
- AITrace
- ConflictResolverResult
- ExecutionPlan
- DecisionBundle

Introduce one canonical persisted `ExecutionPlanCandidate` boundary only after
reconciling existing candidate assets. Existing objects may not be duplicated.

## Permanent Safety Boundary

The authorization does not permit:

- automatic open, close, partial close, reduce, add, reverse, or order;
- Candidate-to-Final bypass;
- FinalExecutionPlan-to-UserPosition conversion;
- AI state-machine mutation or rule bypass;
- an opportunity source outside Asset Pool;
- a second Analysis, Evidence, Score, Decision, ExecutionPlan, UserPosition,
  PositionMonitorLog, Review, AI trace, or conflict-resolver family;
- Mobile or Figma changes;
- frontend redesign;
- unrelated notification, external delivery, or trading expansion.

## Authorization Is Not Completion

This record changes implementation permission only. It creates no application
capability, API, schema, migration, UI, Figma, Mobile, test result, runtime
acceptance, product completion, or production-readiness claim.

NEXT_STEP: `FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION`
