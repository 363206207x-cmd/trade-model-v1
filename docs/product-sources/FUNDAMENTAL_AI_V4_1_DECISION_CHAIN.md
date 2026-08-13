# Fundamental AI v4.1 Unified Final Contract

Status: `PRODUCT_DESIGN_FROZEN`

Original source: `/Users/xuchao/Documents/唯一产品开发方案_最终冻结版.docx`

Original SHA-256: `91bcfbd154bc43b2176107bfc65a948271e10e3e9862027f3647dc13bf5e0900`

Version/date: `v4.1 final freeze / 2026-08-12`

This file is the canonical repository representation of all twenty chapters
and Appendices A-D of the Product Owner-approved final source. It supersedes
the previous v4.1 repository representation. It freezes product semantics; it
does not claim implementation or runtime acceptance.

## 1. Product Position and Principles

Fundamental AI is a personal, multi-source evidence-driven trading decision
closed loop. AI generates a candidate, rules own final validation, an explicit
manual position starts monitoring, and Review closes the loop.

- Every direction, risk, opportunity and plan is traceable to real data,
  evidence, scores, AI calls and a rule version.
- Structured output is mandatory; a one-line black-box conclusion is invalid.
- AI understands evidence and proposes a Candidate. Rules own data quality,
  safety, state and final veto.
- A system plan is never a UserPosition. Monitoring starts only after an
  authenticated explicit manual position action.
- AI disagreement downgrades bias intensity, confidence, risk and Plan Mode;
  it does not silently erase an opportunity.
- Missing, stale or untrusted data fails closed. Empty arrays and null are
  valid when accompanied by exact state; fabricated content is forbidden.

The frozen formula is:

`multi-source data/evidence + GPT Candidate + Gemini Review + Grok Challenge + Conflict Resolver + Rule Validation = Final Execution Plan`.

Automatic open, close, add, reduce, reverse or exchange order is forbidden.
Candidate-as-Final, plan-as-position, AI rule bypass and AI fact fabrication
are forbidden.

## 2. Complete Business Loop

`Asset Pool -> scan/on-demand analysis -> Opportunity Discovery + State Machine -> Analysis Run -> Evidence -> Eight Scores -> Decision Bundle -> Rule Base Result -> GPT Candidate -> Gemini Review -> Grok Challenge -> Conflict Resolver -> Rule Validation -> Final Execution Plan -> explicit manual action -> UserPosition -> Position Monitoring -> Review/Missed Opportunity/Rule iteration`.

The following are independent concepts:

| Concept | Question | Canonical owner |
|---|---|---|
| Market Bias | Which direction and strength does the market support? | MarketBiasResult / Decision |
| Opportunity State | Where is this asset in the opportunity lifecycle? | Opportunity / AssetState |
| Plan Mode | Is participation permitted and at what intensity? | PlanModeResult / Final plan |
| Execution Plan | If participation is allowed, how? | Candidate / Final plan |
| Position Monitoring | After a real open, does the original logic remain valid? | UserPosition / PositionMonitorLog |

## 3. Asset Pool and Dynamic Home Top6

### 3.1 Sole continuous source

Asset Pool is the only source for continuous opportunity discovery, Candidate
Promotion, Home Top6, push and missed-opportunity records. It supports more
than six assets. Six is only Home projection capacity. Defaults enter the Pool
but never receive permanent Home slots. Fixed BTC/ETH/SOL paths or any
opportunity path bypassing the Pool violate the contract.

### 3.2 Pool capabilities

- full-market search and fuzzy symbol/name/alias search;
- explicit add, remove and restore default;
- manual and scheduled scan through the same Analysis Run chain;
- batch add, remove and scan;
- removing a Pool item retains historical analysis and review.

### 3.3 Search preview

A searched asset may start an on-demand Analysis Run and Three-AI explanation.
This is `Analysis Preview`: it does not add the asset to the Pool, create a
persistent Opportunity or Candidate, or enter Home Top6. Only explicit add
enables observing, scheduled scans, Opportunity Discovery and ranking.

### 3.4 Dynamic ranking

`all Pool assets -> latest trusted Analysis + Opportunity -> configured Opportunity Priority Ranking -> dynamic Home Top6`.

Ranking inputs include opportunity score, final confidence, final risk, final
Plan Mode, data quality, freshness and conflict penalty. Weights, penalties and
thresholds are configuration. `invalidated`, `cooling`, `confused` and
`BLOCKED` never enter the positive Top6. Fewer than six eligible assets returns
the actual count; there is no default or fake backfill. Ties use freshness,
analysis time and opportunity stability.

Each projection includes `assetId`, `symbol`, `name`, `opportunityScore`,
`finalMarketBias`, `finalPlanMode`, `confidence`, `riskLevel`,
`opportunityState`, `analysisId` and `rankingReason`.

## 4. Data, Evidence, Scores and Timeframes

Sources include market/candles/volume, liquidity/orders, OI/Funding/liquidation,
ETF/macro/news/regulation/events, on-chain/whale data and provider health.
Every EvidenceItem includes current fact/value, baseline change, direction,
strength, confidence, `sourceId`, `observedAt`, freshness and `analysisId`.

The eight scores are trend structure, capital momentum, leverage risk,
liquidity quality, sentiment temperature, event impact, macro environment and
overall confidence.

`data_quality_score` is 0-100. Below 85 downgrades confidence at least one
level. Below 70 is a circuit breaker and cannot produce a confirmation plan.
Three AI is normally called only at quality >=85 plus material change. All
thresholds are configuration. Missing configuration fails closed.

Timeframe responsibilities are separate: 4h direction, 1h structure, 15m
trigger, 5m microstructure/liquidity filter. Weights and convergence threshold
are configuration; the accepted baseline is 40/30/20/10 and difference <=15%
with at least three aligned timeframes.

## 5. Market Bias, Opportunity State and Plan Mode

### 5.1 Market Bias (exactly eight)

`STRONG_BULLISH`, `BULLISH`, `WEAK_BULLISH`, `RANGE`, `WEAK_BEARISH`,
`BEARISH`, `STRONG_BEARISH`, `WAIT`.

Rules first produce `ruleMarketBias`, `ruleConfidence`, `ruleRisk`,
`rulePlanMode` and `ruleCanExecute`. GPT may propose only a same-family
intensity downgrade. Cross-family reversal requires a new rule analysis with
new evidence or Hot Reset. Every bias before/after and adjustment reason is
persisted.

### 5.2 Opportunity State (exactly eight)

`observing`, `candidate`, `waiting_trigger`, `triggered`, `high_risk`,
`invalidated`, `cooling`, `confused`.

### 5.3 Plan Mode (exactly five)

`CONFIRMATION`, `PREPARATION`, `REDUCED`, `OBSERVATION`, `BLOCKED`.

Bias, opportunity state and Plan Mode are separate fields. A bullish bias does
not imply execution permission. For example, `BULLISH + candidate +
OBSERVATION` is valid.

## 6. Opportunity State Machine

All state writes use one canonical StateService. State identity includes Pool
owner/user, asset or symbol, and timeframe. Every transition records
`opportunityId`, `analysisId`, timeframe, from/to state, reason, trigger source,
rule version, timestamp and trace ID.

The minimum flow is:

- observing -> candidate when configured promotion criteria pass;
- candidate -> waiting_trigger when direction exists but trigger is pending;
- waiting_trigger -> triggered when trigger, quality, risk, execution
  feasibility and confused checks pass;
- triggered -> high_risk when risk rises without complete invalidation;
- any planned state -> invalidated on a formal invalidation condition;
- invalidated/high_risk -> cooling;
- any state -> confused when the joint threshold is reached;
- cooling -> observing after the configured window;
- confused -> observing/candidate only, never directly triggered.

Debounce and cooling are isolated by owner + symbol/asset + timeframe. Cooling
cannot enter candidate/waiting_trigger/triggered. Promotion threshold, minimum
dwell and cooling time are configuration; missing config fails closed.
Priority is `Hot Reset > Confused > Invalidated > ordinary transition`.
Hot Reset invalidates immediate candidate/trigger validity, marks existing
plans for revalidation and recalculates driver, execution feasibility,
confused score and risk.

## 7. Three-AI Authority and Call Gate

GPT_FINAL is the Candidate generator only. It consumes Analysis, both evidence
sets, eight scores, multi-timeframe state, rule result, data quality, confused
score and account-risk snapshot. Missing mandatory input creates a failed
AITrace and rule fallback, never a successful Candidate.

Gemini_REVIEW reviews the Candidate only and returns `approve`, `downgrade`,
`reject_candidate` or `risk_warning`. Grok_CHALLENGE supplies adversarial
failure paths and risk only. Neither may generate a plan or mutate state.

Calls use configurable cache, quota, concurrency, token budget and timeout.
Success, failure, timeout, missing provider, fallback and cache hit all create
queryable AITrace. Rule fallback never impersonates a role success.

## 8. Three-AI Explanation Contract

The Execution Plan area contains only validated Final results. The Three-AI
workspace is the evidence/audit explanation surface.

Every role output includes `analysisId`, `traceId`, `roleState` and
`generatedAt`. Role state is exactly `READY`, `PARTIAL`, `FALLBACK`,
`UNAVAILABLE` or `ERROR`.

Formal array collection state is exactly `FOUND`, `NONE_FOUND`,
`INSUFFICIENT_DATA`, `SOURCE_UNAVAILABLE` or `STALE`. Grok's failure-path
collection additionally supports `NO_VERIFIABLE_FAILURE_PATH`. Role state
never substitutes for collection state. Every formal array has its own state.

### 8.1 GPT_FINAL

Required structure:

- core judgment: market bias, opportunity state and text;
- `supportingEvidence` plus `supportingEvidenceState`;
- `opposingEvidence` plus `opposingEvidenceState`;
- separate 4h/1h/15m/5m explanation;
- bias adjustment before/after/reason;
- Candidate summary.

Each evidence entry includes evidenceId, type, source, current value/change,
strength, confidence, observedAt, freshness and analysisId.

### 8.2 Gemini_REVIEW

Required structure:

- `evidenceGaps` + state;
- `logicConflicts` + state;
- `underestimatedRisks` + state;
- downgrade suggestion before/after/reason/recovery condition;
- exact review result enum.

### 8.3 Grok_CHALLENGE

Required structure:

- `failurePaths` + state;
- `opposingScenarios` + state;
- `externalEventRisks` + state;
- `microstructureRisks` + state;
- `watchIndicators` + state.

A failure path has trigger condition, causal path, observation window,
validation indicators and source references when available. No verifiable path
returns `[]` plus `NO_VERIFIABLE_FAILURE_PATH`; it is never fabricated.

### 8.4 AI Consistency

AI Consistency contains only conflict level, final Market Bias, final Plan
Mode, main reason, recovery condition and data state. It is not a fourth role,
contains no vote or percentage, and creates no new business conclusion.

Empty arrays must remain present. Missing, stale, unavailable, failed and
fallback states are explicit. Backend and frontend may not infer or fill role
content from summary, examples or another module.

## 9. Conflict Levels, Confused and Opportunity Preservation

The frozen levels are:

- `LEVEL_1_CONSISTENT`: normally CONFIRMATION;
- `LEVEL_2_MINOR_DISAGREEMENT`: REDUCED or PREPARATION;
- `LEVEL_3_SIGNIFICANT_DISAGREEMENT`: PREPARATION or OBSERVATION;
- `LEVEL_4_EXTREME_CONFLICT`: BLOCKED and confused.

Conflict Resolver persists score, bias/mode/confidence/risk before and after,
adjustment reason, downgrade reason, recovery condition, confused decision and
rule-veto reason. Confused score >=70 enters confused; >=85 blocks directional
push. The exact threshold is configuration.

Opportunity State is not execution permission. Level 2/3 adjusts bias
intensity, confidence, risk, Plan Mode and recovery condition but retains the
Opportunity. Level 4, confused or rule veto may block direction, while still
retaining Opportunity, Candidate, before/after and reasons for Review. A single
Gemini/Grok objection never deletes an Opportunity.

## 10. ExecutionPlanCandidate

Mandatory inputs include Analysis, Evidence, Scores, Decision, multi-timeframe
state, rule result, data quality, confused score, account risk and source
lineage.

Candidate fields include candidate/opportunity/analysis identity, Candidate
mode and bias, opportunity type, entry logic/zone, trigger condition, stop
logic/zone, target logic/zones, add/reduce/abandon conditions, invalidation,
leverage/position suggestions, validity, analysis/trigger timeframes, holding
horizon, source references, AI trace, version and timestamps.

Every numeric entry/stop/target/RR boundary requires a traceable source.
Absent source cannot validate. Candidate is never an executable Final.

## 11. Rule Validation and Final Execution Plan

Rule Validation owns data quality, state, direction-family, risk, account risk,
source, timeframe, validity, execution feasibility and final veto. It is not an
AI role and is not recorded as an AITrace.

Final contains:

- plan/candidate/opportunity/analysis/asset/trace/ruleVersion identity;
- rule/final bias, Candidate/final mode and adjustment reason;
- opportunity type, recommended action, entry, trigger, stop and targets;
- entry/stop/target/add/reduce/abandon/invalidation logic;
- leverage, position size, risk, RR and account-risk snapshot;
- analysis/trigger timeframes, valid-from/until and holding horizon;
- validation, veto, downgrade, data-quality and source status.

Candidate and Final have distinct objects, storage and identifiers. Final
references Candidate and Analysis. No API exposes an unvalidated Candidate as
Final.

## 12. Push Recheck and Validity

Push/Recheck is a reminder and revalidation result, never trading
authorization. It records snapshot, recheck state, reason, current plan
validity and `notTradeInstruction=true`. It cannot open, close, add, reduce,
reverse or order.

## 13. UserPosition and Position Monitoring

`SYSTEM_PLAN_POSITION` requires a valid Final plan ID.
`MANUAL_INDEPENDENT` explicitly records no system plan. Null source semantics
are forbidden. Only authenticated explicit manual creation forms a position.

Position Monitoring preserves asset/direction/entry/mark/PnL/open time,
position risk, risk trend/reason, monitoring conclusion, suggested action,
entry logic state, reversal state and monitor time. Risk level and risk trend
are independent. Only VERIFIED + FRESH results enter Home success state;
Pending, Stale and Invalid fail closed without fake values. Closed positions
leave Home and enter history/Review.

## 14. Review and Rule Iteration

Review covers executed valid/invalid, missed valid/invalid,
pushed-not-filled, blocked-by-risk and user-deviation outcomes. The mandatory
chain is:

`input snapshot -> evidence/scores/decision -> GPT input hash/Candidate -> Gemini -> Grok -> Resolver before/after -> Rule Validation/veto -> Final -> Push/Recheck -> UserPosition/Monitoring -> Outcome/Review/Rule feedback`.

Review attributes Candidate quality, Gemini/Grok findings, Resolver
adjustment, rule action, plan outcome, user deviation and monitor timing.
Metrics include evidence traceability, structured completeness, unsupported
conclusion rate (target 0), fabricated-fill rate (target 0), confidence
calibration, false positive/negative, Plan Mode effectiveness, downgrade
effectiveness, failure-path hit rate and missed-opportunity quality.

## 15. Home and Interaction Freeze

Home order is system status, alert/event, dynamic Top6, Position Monitoring
(about 70%) plus Final Execution Plan (about 30%), then single Three-AI
workspace plus AI Consistency.

Top6 cards show trusted price, final bias, opportunity score, confidence, risk,
opportunity state and Plan Mode. Clicking updates Final plan and Three-AI only.
Remove acts on Pool membership. Search is real input. No fake chart.

Execution Plan shows Final only. Confused/BLOCKED shows conflict source, block
reason and recovery condition. Three-AI is one workspace with three tabs and
one active role. Asset Pool lists all assets and supports search/add/remove/
restore/batch scan; preview precedes add and never creates persistent
opportunity.

## 16. Canonical Object Ownership and Persistence

Reuse Asset/Market data, AnalysisRun, InputSnapshot, EvidenceItem, ScoreItem,
DecisionBundle, UserPosition, PositionMonitorLog, ReviewResult,
DataSourceHealth, RuleConfig and AI call-log ownership. Extend Asset Pool
relationship/view, DecisionBundle, ExecutionPlan, AI orchestration, Home
projection and Review. Maintain one canonical Opportunity/StateLog,
ExecutionPlanCandidate, ConflictResolverResult and Final owner.

Minimum identifiers and relations:

- AssetPoolItem: poolItemId/userId/assetId/symbol/name/source/watch state/time/version;
- Opportunity: opportunityId/poolItemId/assetId/analysisId/timeframe/state/score/confidence/risk/time/ruleVersion;
- state log: opportunity/analysis/timeframe/from/to/reason/trigger/time/trace;
- Candidate: candidate/opportunity/analysis/mode/bias/type/plan logic/zones/conditions/risk/trace/time/version;
- Resolver: resolver/candidate/level/score/bias/mode/confidence/risk before/after/reasons/confused/veto;
- Final: plan/candidate/opportunity/analysis/final bias/mode/plan/risk/validity/validation/rule/source;
- AITrace: trace/analysis/candidate/role/model/input/output/status/error/fallback/cost/latency/time;
- UserPosition: position/user/source/final plan/symbol/direction/entry/size/leverage/user stop/target/open/close;
- Monitor: position/analysis/mark source/time/logic/reversal/risk/trend/reason/conclusion/action/trust/time;
- Review: review/analysis/plan/position/opportunity/outcome/deviation/AI-rule assessment/feedback/time.

Query-critical ID/state/enum/time/version/source/ranking fields are normalized.
Snapshots and raw structured content may use versioned JSON with traceable IDs.
AnalysisRun is the chain anchor. Critical writes use transactions or explicit
consistency policy.

## 17. API Contract

Responses use `code`, `msg`, `requestId`, `serverTime`, `data`; ISO-8601 zoned
time; `[]` for empty arrays; fixed enums; idempotent writes; analysis/rule/trace
metadata.

Required API groups cover Pool, Opportunity/Top6/state history, on-demand
Analysis Preview, Three-AI/Trace, Candidate/Resolver/Validation/Final,
Position/Monitoring, Push/Recheck and Review.

Home Top6 has Opportunity/ranking lineage. Execution Plan returns Final or an
explicit non-Final state. Three-AI fields never fallback across roles or
modules. Formal arrays always exist with their collection state. Position
success fields are hidden when trust fails. Frontend never synthesizes role
content from summary or examples.

## 18. Scheduling, Idempotency, Cache, Quota and Audit

Triggers include schedule, candle update, event and manual scan. Idempotency is
symbol + timeframe + analysis time + rule version. Locking or equivalent avoids
duplicate Analysis, promotion and plan generation. Scan frequency is
state-sensitive and configured. AI cache, cost, token budget, rate limit,
concurrency, timeout and fallback are recorded.

Critical actions carry trace ID, request ID, analysis ID, symbol, timeframe,
rule version and server time.

AITrace owns only GPT/Gemini/Grok calls. ConflictResolverResult and Rule
Validation/Final validation are independent owners. Aggregate audit query joins
them by analysisId, candidateId and traceId; Resolver or validation must never
be impersonated as an AI role trace.

## 19. Test and Capability Audit

The final regression includes:

- Pool >6, fuzzy search, add/remove/restore/batch scan, preview isolation;
- dynamic Top6 ranking changes, no fixed symbol, no fake backfill;
- sole opportunity source, eight states, timeframe debounce, cooling, Hot Reset
  and complete transition log;
- eight biases, five modes, same-family downgrade, reversal veto, before/after;
- role isolation, mandatory input, success/timeout/error/fallback/cache trace;
- role/collection state matrix and zero fabricated evidence/failure paths;
- Candidate/Final storage and API isolation, Resolver and Validation mandatory;
- entry/stop/target/RR source gate;
- system/manual position source, trusted monitor, independent position risk and
  closed removal;
- executed/missed/blocked review and full AI/rule/user responsibility chain;
- Push Recheck non-authorization;
- zero automatic trading capability.

Implementation is followed by one unified independent capability audit and
merged-main/runtime validation. It is not complete merely because focused
tests pass.

## 20. Reuse, Extension and Removal

Reuse validated providers, analysis/evidence/score/decision, P2 position and
monitoring, review foundation and Home structure. Extend existing canonical
owners. Add an owner only for a genuinely independent semantic object. Do not
create a second stack. Fixed opportunity sources, generic one-field AI output,
AI-direct Final, Candidate-as-plan and semantic fallback must leave production
paths. Existing code is not deleted without dead-code evidence.

## Appendix A. Market Bias x Plan Mode

Common legal combinations (not mechanical defaults):

| Bias | Common modes |
|---|---|
| STRONG_BULLISH | CONFIRMATION / REDUCED / PREPARATION / BLOCKED |
| BULLISH | CONFIRMATION / REDUCED / PREPARATION / OBSERVATION / BLOCKED |
| WEAK_BULLISH | PREPARATION / REDUCED / OBSERVATION / BLOCKED |
| RANGE | OBSERVATION / BLOCKED |
| WEAK_BEARISH | PREPARATION / REDUCED / OBSERVATION / BLOCKED |
| BEARISH | CONFIRMATION / REDUCED / PREPARATION / OBSERVATION / BLOCKED |
| STRONG_BEARISH | CONFIRMATION / REDUCED / PREPARATION / BLOCKED |
| WAIT | OBSERVATION / BLOCKED |

## Appendix B. Opportunity State Output Boundary

| State | Allowed | Forbidden |
|---|---|---|
| observing | observation summary | directional plan |
| candidate | candidate opportunity and AI explanation | untriggered confirmation plan |
| waiting_trigger | PREPARATION plus trigger/recovery condition | fake triggered state |
| triggered | Candidate generation and Final validation | bypass Resolver/Validation |
| high_risk | warning plus REDUCED/OBSERVATION/BLOCKED | normal high-intensity participation |
| invalidated | invalidation reason and Review | retaining old valid plan |
| cooling | cooling reason and remaining time | candidate/waiting/triggered |
| confused | conflict/recovery plus BLOCKED | directional plan or push |

## Appendix C. Standard Output Rules

Example values are documentation only and never production defaults. Role
objects obey Chapter 8; arrays are always present and may be empty only with
their own collection state. Every success field traces to real Evidence,
Score, Rule or source. `notTradeInstruction=true` is mandatory on advice.

## Appendix D. Final Acceptance Checklist

- [ ] Pool supports >6 and dynamic Home Top6.
- [ ] Search preview runs Three AI without persistent Opportunity before add.
- [ ] Eight biases, eight opportunity states and five modes are independent.
- [ ] GPT creates Candidate only; Gemini/Grok do not create plans; rules are final.
- [ ] Final-only plan area and complete structured Three-AI workspace.
- [ ] Every formal role array has exact collection state and no fabricated fill.
- [ ] AI Consistency is a compact non-voting summary.
- [ ] Conflict downgrades rather than silently erases Opportunity.
- [ ] Candidate/Final, Resolver and Validation ownership and query chain are complete.
- [ ] Final plan and manual UserPosition remain separate.
- [ ] Position Monitoring trust/semantic contract is unchanged and fail closed.
- [ ] Push Recheck is not trading authorization.
- [ ] Review attributes GPT, Gemini, Grok, Resolver, Rule and user actions.
- [ ] Automatic open/close/add/reduce/reverse/order capability count is zero.
