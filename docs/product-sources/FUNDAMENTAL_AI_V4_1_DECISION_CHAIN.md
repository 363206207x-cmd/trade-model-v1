# Fundamental AI v4.1 Unified Product Source

Status: `PRODUCT_DESIGN_FROZEN`

Original business source: `/Users/xuchao/Documents/唯一产品开发方案_最终冻结版.docx`

Original business-source SHA-256: `91bcfbd154bc43b2176107bfc65a948271e10e3e9862027f3647dc13bf5e0900`

Final interaction source: `/Users/xuchao/Documents/Fundamental_AI_v4.1_最终交互逻辑与页面设计开发规格_冻结版.docx`

Final interaction-source SHA-256: `43ec787f3228ec05e4e81a3c07fce4c3969c38850d709efa7097a2a406c463d3`

Version/date: `v4.1 unified final freeze / 2026-08-14`

This file is the sole ACTIVE and AUTHORITATIVE v4.1 Product Source. It merges
all twenty chapters and Appendices A-D of the Product Owner-approved business
source with the final interaction, page, route, state and component contract.
It supersedes every earlier v4.1 interpretation while preserving historical
documents as evidence only. It freezes product semantics; it does not claim
implementation or runtime acceptance.

The following repository documents are normative annexes to this one source,
not competing Product Sources:

- `docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md`;
- `docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md`;
- `docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md`.

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
- explicit add, remove, top-up missing defaults and reset to defaults;
- manual and scheduled scan through the same Analysis Run chain;
- batch add, remove and scan;
- removing a Pool item retains historical analysis and review.

`Top up defaults` adds only missing default assets and preserves every custom
asset. `Reset to defaults` requires confirmation and replaces the continuing
observation set with defaults without deleting history or stopping existing
position monitoring. Removing an asset stops future continuous scans,
Opportunity Promotion, Top6 eligibility and new opportunity messages. Existing
Analysis, Opportunity, Candidate, Final and Review records remain. The current
Final becomes `TRACKING_STOPPED` with `needsRevalidation=true`; any existing
UserPosition continues independent monitoring.

### 3.3 Search preview and analysis mode

Every AnalysisRun has an explicit `analysisMode`:

- `ANALYSIS_PREVIEW` is for an asset not yet in the Pool. It may create an
  AnalysisRun, data-quality result, Evidence, Eight Scores, Multi-Timeframe
  result and Three-AI explanation. It must not create Opportunity, Candidate,
  Conflict Resolver, Rule Validation or Final records, and must not expose
  `candidateId`, Candidate Plan Mode, `finalPlanId`, Final Plan Mode, entry,
  stop or target fields. GPT synthesizes evidence and a direction hypothesis;
  Gemini reviews evidence quality and logic; Grok challenges blind spots and
  failure scenarios. Preview never changes the selected Home asset.
- `OPPORTUNITY_DECISION` is valid only when a real Opportunity exists. It may
  run the Candidate, Review, Challenge, Resolver, Rule Validation and Final
  chain.

Only explicit Pool add enables observing, scheduled scans, Opportunity
Discovery, ranking and opportunity messaging.

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

Top6 is deduplicated by asset, not by Opportunity. Each slot additionally
includes `primaryOpportunityId`, `primaryTimeframe`, `primaryPlanMode`,
`secondaryOpportunityCount` and `timeframeConflictState`. Opposing timeframes
are never silently averaged; they produce a conflict penalty or Confused
evaluation.

The selected Home asset is written to `?asset={symbol}`. Refresh, browser
history and cross-page return restore it. Ranking changes never auto-switch a
user-selected asset. If it exits Top6, the reading context remains with an
explicit exit reason until the user selects another asset.

## 4. Data, Evidence, Scores and Timeframes

Sources include market/candles/volume, liquidity/orders, OI/Funding/liquidation,
ETF/macro/news/regulation/events, on-chain/whale data and provider health.
Every EvidenceItem includes current fact/value, baseline change, direction,
strength, confidence, `sourceId`, `observedAt`, freshness and `analysisId`.

The eight scores are trend structure, capital momentum, leverage risk,
liquidity quality, sentiment temperature, event impact, macro environment and
overall confidence.

`data_quality_score` is 0-100:

- `85-100`: the data gate passes and the complete decision chain is allowed;
  this never guarantees an Opportunity or plan;
- `70-84`: confidence is downgraded at least once; Opportunity, Evidence,
  Conflict, Risk and Rule Validation determine Plan Mode;
- `<70`: `CONFIRMATION` is forbidden, but the complete chain may produce
  `PREPARATION`, `REDUCED`, `OBSERVATION` or `BLOCKED`. `REDUCED` is legal only
  when its key source and safety gates are complete; low quality is not a
  mechanical global pause;
- `STALE` or `SOURCE_UNAVAILABLE`: the response names affected sources and
  modules plus the exact revalidation condition.

Three AI is normally called only at quality >=85 plus material change, except
the fail-closed/degraded paths explicitly permitted above. All thresholds are
configuration. Missing configuration fails closed.

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

The minimum flow and output timing are:

- observing -> candidate when configured promotion criteria pass;
- candidate provides opportunity analysis and non-directional `OBSERVATION`;
  it may not produce an untriggered `CONFIRMATION`;
- candidate -> waiting_trigger when direction exists but trigger is pending;
- waiting_trigger may run GPT Candidate -> Gemini -> Grok -> Resolver -> Rule
  Validation and persist a Final `PREPARATION`; `PREPARATION` is not `NO_PLAN`;
- waiting_trigger -> triggered when trigger, quality, risk, execution
  feasibility and confused checks pass;
- triggered revalidates the existing Final `PREPARATION` and may produce
  `CONFIRMATION`, `REDUCED`, `PREPARATION`, `OBSERVATION`, `BLOCKED` or an
  invalidated lifecycle result; it does not start the first Candidate merely
  because the trigger fired;
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

In `ANALYSIS_PREVIEW`, the same role identities operate under the reduced
authority in Section 3.3 and cannot emit Candidate or Final semantics. In
`OPPORTUNITY_DECISION`, the frozen Candidate/Review/Challenge permissions
apply. The mode is persisted and queryable; UI labels never infer it.

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

Plan lifecycle is independent from Plan Mode and is exactly `CURRENT`,
`NEEDS_REVALIDATION`, `SUPERSEDED`, `TRACKING_STOPPED`, `INVALIDATED` or
`EXPIRED`. A new Final never silently overwrites an old Final. Versions and
supersession links remain queryable.

Candidate and Final have distinct objects, storage and identifiers. Final
references Candidate and Analysis. No API exposes an unvalidated Candidate as
Final.

## 12. Push Recheck, Plan Revalidation and Hot Reset

Push Recheck is owned by a `PushSnapshot`, starts when the user opens a
message, and preserves the original snapshot beside the current result. It is
a reminder/recheck result, never trading authorization, and always carries
`notTradeInstruction=true`.

Plan Revalidation is owned by `planId` and is triggered by exactly
`HOT_RESET`, `EVENT_WINDOW`, `DATA_REFRESH`, `EVIDENCE_CHANGED` or
`MANUAL_REVALIDATION`. It records `triggerType`, source plan/version and result
plan/version. Push Recheck and Plan Revalidation are distinct identities and
must never share a generic record as their owner.

Hot Reset scope is exactly `GLOBAL`, `MARKET`, `ASSET` or
`PROVIDER_DEPENDENCY`. It affects only its scope, never freezes every asset by
default, and does not stop Position Monitoring; affected positions receive
higher monitoring priority.

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

Only `CONFIRMATION` and `REDUCED` Finals expose a plan-linked `record actual
position` action. Plan values may prefill the form, but actual entry price,
quantity, leverage, actual stop/target and time require user confirmation.
Submission creates `SYSTEM_PLAN_POSITION`. An unplanned position is explicitly
`MANUAL_INDEPENDENT`. A UserPosition keeps the `finalPlanId` used at opening;
newer Finals are compared separately and never replace its monitoring
baseline.

## 14. Review and Rule Iteration

Review covers executed valid/invalid, missed valid/invalid,
pushed-not-filled, blocked-by-risk and user-deviation outcomes. Missed review
separates `missedReason` (`NOT_TRIGGERED`, `BLOCKED_BY_SYSTEM`,
`PUSHED_NOT_FILLED`, `USER_SKIPPED`) from `laterOutcome` (`VALID`, `INVALID`,
`INCONCLUSIVE`). At-time evidence and later results are independent. The
mandatory chain is:

`input snapshot -> evidence/scores/decision -> GPT input hash/Candidate -> Gemini -> Grok -> Resolver before/after -> Rule Validation/veto -> Final -> Push/Recheck -> UserPosition/Monitoring -> Outcome/Review/Rule feedback`.

Review attributes Candidate quality, Gemini/Grok findings, Resolver
adjustment, rule action, plan outcome, user deviation and monitor timing.
Metrics include evidence traceability, structured completeness, unsupported
conclusion rate (target 0), fabricated-fill rate (target 0), confidence
calibration, false positive/negative, Plan Mode effectiveness, downgrade
effectiveness, failure-path hit rate and missed-opportunity quality.

## 15. Page, Interaction and Runtime Freeze

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

### 15.1 State scopes and action glossary

State scopes are System, Macro/BTC environment, AnalysisRun, Opportunity,
Final Plan, UserPosition and Message/PushSnapshot. The global header label is
`Macro/BTC Environment` (Chinese: `大盘环境` or `BTC / 宏观环境`), never an
asset-level `finalMarketBias`.

Actions have one meaning each: Pool Scan, On-demand Analysis, Re-analysis,
Plan Revalidation, Push Recheck, Manual Review, Refresh Page Data, Retry,
Record Actual Position and Record Close. Labels, APIs and audit records may
not cross-map these actions.

### 15.2 Message and Telegram

One persisted Message is the sole business fact source for in-app read state,
dedupe, cooldown, expiry, current Recheck and channel-delivery status.
Telegram is a delivery channel, not a second message owner. Only these
high-value categories are eligible:

1. an Opportunity reaches `CONFIRMATION` or configured high-quality `REDUCED`;
2. major Opportunity/plan safety change: Confused, liquidity trap, scoped Hot
   Reset impact, invalidation, veto, drift, expiry or execution pause;
3. major active-position logic/risk change: entry logic weakened/invalidated,
   strong reversal, HIGH/EXTREME, material riskTrend increase, or proximity to
   the actual stop/target.

Ordinary price movement, minor confidence changes, ordinary `OBSERVATION`,
Preview, non-Final Candidate and duplicate noise are forbidden notifications.

### 15.3 Account risk and asynchronous work

Account-risk coverage is exactly `COMPLETE`, `PARTIAL` or `UNKNOWN`. If only
recorded positions are included, the UI explicitly displays that coverage.

Pool Scan, Preview, Re-analysis, Three AI, Plan Revalidation and Hot Reset
recalculation use one async-task contract: `taskId`, `taskType`, `targetId`,
`state` (`QUEUED`, `RUNNING`, `PARTIAL`, `SUCCEEDED`, `FAILED`, `CANCELLED`),
`stage`, `failureReason` and `retryAllowed`. Fake percentages are forbidden.

### 15.4 Frozen routed surfaces

The Desktop product has exactly fourteen routed responsibilities:

1. `/login` Login / Session Recovery;
2. `/dashboard?asset={symbol}` Home Dashboard;
3. `/asset-pool` Asset Pool;
4. `/positions` Position Center;
5. `/positions/{positionId}` Position Detail;
6. `/reviews` Review Center;
7. `/reviews/{reviewId}` Review Detail;
8. `/analysis` and `/analysis/{analysisId}` AI Analysis Preview/Decision;
9. `/messages` Message Center;
10. `/recheck/{pushSnapshotId}` Push Recheck;
11. `/plans/{planId}` Final Plan Detail;
12. `/calendar` Event Calendar;
13. `/audit/{traceId}` Full Audit Chain;
14. `/me` My / Settings.

Eleven shared overlays are frozen: Status/Recovery Drawer, Quick Asset Search,
Pool Asset Detail, Pool Batch Management, FinalPlanDetail Drawer, Actual
Position Modal, Close Position Modal, Audit Detail Drawer, Async Task Center,
Telegram Binding/Test and Event Detail. Their exact state inventory, data
owners and tests are in the normative page matrix annex.

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

Required extensions are `analysisMode`, `planLifecycleState`,
`revalidationTriggerType`, Home primary/secondary timeframe aggregation,
`timeframeConflictState`, Message channel status, account-risk coverage,
async-task state and Review `missedReason`/`laterOutcome`. New ownership is
legal only for genuinely independent Plan Revalidation, channel-delivery or
cross-domain AsyncTask records; it may not create a second Plan, Message,
Analysis, Opportunity, Position, Monitoring, Review, Home or Asset Pool stack.

Query-critical ID/state/enum/time/version/source/ranking fields are normalized.
Snapshots and raw structured content may use versioned JSON with traceable IDs.
AnalysisRun is the chain anchor. Critical writes use transactions or explicit
consistency policy.

## 17. API Contract

Responses use `code`, `msg`, `requestId`, `serverTime`, `data`; ISO-8601 zoned
time; `[]` for empty arrays; fixed enums; idempotent writes; analysis/rule/trace
metadata.

Required API groups cover Session, Pool, Opportunity/Top6/state history, on-demand
Analysis Preview, Three-AI/Trace, Candidate/Resolver/Validation/Final,
Position/Monitoring, Message/channel delivery, Push/Recheck, Plan
Revalidation, Event Calendar, Review, Audit and My/Settings.

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
- all 14 routes, 11 overlays, 70 route states and 81 Desktop acceptance frames;
- selected-asset URL persistence and no auto-switch after ranking changes;
- separate Push Recheck and Plan Revalidation ownership;
- Message sole ownership plus exact Telegram filtering;
- account-risk coverage and async task state without fake percentage;
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

The previous v4.1 authorization and ownership documents remain
`HISTORICAL_REFERENCE_ONLY / SUPERSEDED`. They are not active Product Sources
and cannot authorize or narrow this unified interaction implementation.

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
| candidate | opportunity analysis plus non-directional OBSERVATION | Candidate/Final CONFIRMATION before trigger readiness |
| waiting_trigger | Candidate -> reviews -> resolver -> validation -> Final PREPARATION | fake triggered state or PREPARATION-as-NO_PLAN |
| triggered | Plan Revalidation of existing PREPARATION into a validated current outcome | first-time Candidate shortcut or bypass Resolver/Validation |
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
- [ ] Push Recheck and Plan Revalidation are separate records and triggers.
- [ ] Fourteen routes, eleven overlays, fifty-four component families and
      eighty-one Desktop acceptance frames match the normative page matrix.
- [ ] Selected asset, plan lifecycle/version, Message ownership, Telegram
      filtering, account-risk coverage and async-task fail-closed rules hold.
- [ ] Review attributes GPT, Gemini, Grok, Resolver, Rule and user actions.
- [ ] Automatic open/close/add/reduce/reverse/order capability count is zero.

## Appendix E. Component Families and Desktop Acceptance Inventory

The fifty-four frozen component families are grouped as follows:

- Global (10): `AppShell`, `SideNav`, `PageHeader`, `SystemStatusBar`,
  `StateBadge`, `EmptyState`, `AsyncTaskIndicator`, `Drawer`, `Modal`,
  `AuditMetaDisclosure`;
- Asset/Opportunity (9): `AssetSearch`, `SearchResultItem`,
  `AssetPoolToolbar`, `AssetPoolTable`, `PoolScanStatus`, `OpportunityGrid`,
  `OpportunityCard`, `MultiTimeframeSummary`, `DataQualityGate`;
- Plan (8): `PlanSummaryCard`, `PlanModeHeader`, `PlanLifecycleBadge`,
  `EntryTriggerSection`, `InvalidationStopSection`, `TargetTrendSection`,
  `RiskLimitSection`, `FinalPlanDetail`;
- AI (8): `AnalysisModeBanner`, `AIWorkspace`, `AIRoleTabs`, `EvidenceList`,
  `MultiTimeframeMatrix`, `BeforeAfterDiff`, `FailurePathList`,
  `ConflictSummary`;
- Position/Review (8): `PositionRiskAggregate`, `PositionCard`,
  `PositionActualForm`, `PlanActualComparison`, `MonitorTimeline`, `ReviewCard`,
  `AtTimeLaterCompare`, `ResponsibilityChain`;
- Message/Recheck (5): `MessageListItem`, `ChannelDeliveryStatus`,
  `OriginalSnapshotCard`, `RecheckResultHero`, `RecheckActionBar`;
- Event/Settings/Audit (6): `EventCalendar`, `EventWindowBadge`,
  `TelegramBindingPanel`, `RiskPreferenceForm`, `ProviderStatusPanel`,
  `AuditChainStepper`.

Desktop acceptance consists of 70 routed states plus 11 overlay states, for 81
named frames. Route-state counts are R01 3, R02 8, R03 5, R04 4, R05 5, R06
3, R07 4, R08 6, R09 4, R10 10, R11 7, R12 4, R13 3 and R14 4. Mobile has
sixteen reserved adaptation scenarios only and is outside the current
implementation authorization.

## Appendix F. Prototype and End-to-End Flows

The frozen flows are: first use/session recovery; daily dynamic-opportunity
review; Preview then explicit Pool add; Final Plan then manual UserPosition;
Message then Push Recheck; scoped abnormal recovery; and manual close then
Review. Acceptance scenarios cover a ten-asset Pool with changing Top6,
multi-timeframe conflict, both analysis modes, waiting-trigger Preparation,
triggered revalidation, all five Plan Modes, Pool removal with continuing
position monitoring, three high-value message categories with dedupe, seven
Recheck result classes, Hot Reset/Confused recovery and close-to-review.
