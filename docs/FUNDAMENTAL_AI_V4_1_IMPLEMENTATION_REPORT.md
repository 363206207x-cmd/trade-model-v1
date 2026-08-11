# Fundamental AI v4.1 Decision Chain Implementation Report

Status: `AUDIT_REMEDIATION_COMPLETE_PENDING_INDEPENDENT_REAUDIT`

Package: `FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION`

Baseline: `fb2722c7daa3acaa528131928222fcbbdc079081`

## Implemented Chain

`Asset Pool -> Opportunity -> AnalysisRun -> Evidence -> Score -> DecisionBundle -> GPT Candidate -> Gemini Review -> Grok Challenge -> Conflict Resolver -> Rule Validation -> Final ExecutionPlan -> manual UserPosition -> PositionMonitor -> Review`

## Capability Result

### Asset Pool

- persistent system defaults and authenticated user overrides;
- real market-catalog search with fuzzy symbol/base-asset matching;
- explicit add, remove, restore default, and scan operations;
- scheduler, watchlist, discovery universe, Dashboard focus assets, and
  decision-chain source gate consume Asset Pool symbols;
- a symbol outside Asset Pool cannot create Opportunity, Candidate, or AI calls.

### Opportunity

- exact states: `OBSERVING`, `CANDIDATE`, `WAITING_TRIGGER`, `TRIGGERED`,
  `HIGH_RISK`, `INVALIDATED`, `COOLING`, `CONFUSED`;
- all runtime writes use `AssetStateService.transition`;
- every actual/suppressed priority transition records from/to/reason/source,
  time, Opportunity, Analysis, and Trace;
- ordinary debounce is 30 seconds per symbol and timeframe, and cooling is 15
  minutes;
- Invalidated and Confused recover through Cooling; active Cooling blocks
  direct Candidate/Waiting/Triggered promotion until expiry;
- precedence is Hot Reset > Confused > Invalidated > ordinary;
- Opportunity state and execution permission remain separate.

### Three AI Roles

- GPT_FINAL can generate Candidate fields but cannot create Final, mutate
  Opportunity, create UserPosition, or order;
- GEMINI_REVIEW and GROK_CHALLENGE have strict review/challenge-only schemas;
- provider routing is role-specific, with per-provider timeout and usage guard;
- malformed, oversized, unauthorized, unknown-field, or unavailable results
  fail closed to an explicit rule fallback;
- AI audit stores full canonical input hash, bounded sanitized input summary,
  accepted output or bounded invalid output, model, tokens, cost, latency, and
  fallback state.
- missing-provider, timeout, exception, and log-failure paths retain terminal
  trace evidence while the rule fallback continues.

### Candidate, Conflict, And Final

- Candidate and Final are separate Java/persistence objects and identities;
- conflict level uses the frozen Level 1-4 enum and persistence constraint;
- Gemini/Grok can only downgrade confidence/plan mode or raise risk;
- plan blocking does not automatically mutate Opportunity to Confused;
- Rule Validation checks source/data/state/rule direction, Candidate authority,
  plan boundaries, confidence/risk monotonicity, and automatic-trading text;
- only a Rule Validation PASS row is marked Final;
- no controller exposes `ExecutionPlanCandidate` as Final.

### Position And Review Boundary

- existing UserPosition and PositionMonitor remain the canonical owners;
- a plan never creates a position;
- every new position declares `MANUAL_POSITION` or `SYSTEM_PLAN_POSITION`;
- a system-plan position requires a same-symbol, validated Final, while an
  explicit manual position carries no Final plan;
- Review reuses the existing owner and carries Final/Candidate/Trace provenance.

### Home / Dashboard

- Home focus assets come from Asset Pool;
- existing Home structures consume Final chain data and Three-AI role payload;
- no new trading module, Figma behavior, or Mobile behavior was introduced.

## Safety Result

- Automatic open: `ABSENT`
- Automatic close: `ABSENT`
- Automatic reverse: `ABSENT`
- Automatic order: `ABSENT`
- Candidate-as-Final path: `BLOCKED`
- Plan-to-UserPosition conversion: `ABSENT`
- AI state mutation: `BLOCKED`
- AI rule-direction bypass: `BLOCKED`

## Delivery State

The four audit blockers are remediated and PostgreSQL V11 passes against a
disposable PostgreSQL 16.14 database. An open PR is not effective completion.
Only an independent re-audit, reviewed merged main, and clean/synced main
validation can make the package effective.

`PRODUCT_WORK_RATIO = 95%`

`NON_PRODUCT_WORK_RATIO = 5%`

`STOP_RULE_TRIGGERED = NO`
