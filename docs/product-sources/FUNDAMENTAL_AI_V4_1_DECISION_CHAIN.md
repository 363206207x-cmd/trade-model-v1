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
- `docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md`;
- `docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md`
  (`SHA-256 4d3e937be4534d69e07d34fcf3fe08c4cd5a63ed0bda58b4961ffe6249d26d61`).

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

`data_quality_score` is 0-100 and has one versioned calculation owner:

`DQ = Completeness*0.30 + Freshness*0.25 + ProviderHealth*0.20 +
CrossSourceConsistency*0.15 + SampleAdequacy*0.10`.

- `85-100`: the data gate permits the complete decision chain; this never
  guarantees an Opportunity or plan;
- `70-84`: validated direction may exist, but `CONFIRMATION` is forbidden;
  Opportunity, Evidence, Conflict, Risk and Rule Validation determine whether
  the result is `PREPARATION`, `REDUCED`, `OBSERVATION` or `BLOCKED`;
- `<70`: no validated direction is produced and no directional Final is
  allowed;
- mandatory stale data caps DQ at 69; mandatory unavailable data fails closed;
- optional unavailable data affects only the declared scores and permissions;
- `STALE` or `SOURCE_UNAVAILABLE` names the affected sources/modules and the
  exact revalidation condition.

Three AI is called only when the configured DQ and checkpoint gates permit it.
DQ failure produces zero role calls. A rule fallback may preserve a fail-closed
non-directional result, but it cannot impersonate AI success or create a
directional Final. Missing configuration fails closed.

Timeframe responsibilities are separate: 4h direction, 1h structure, 15m
trigger, 5m microstructure/liquidity filter. Weights and convergence threshold
are configuration; the accepted baseline is 40/30/20/10 and difference <=15%
with at least three aligned timeframes.

### 4.1 Owner-final executable calculation contract

The final versioned machine contract is:

| Contract | Version |
|---|---|
| calculation DAG | `V41-DAG-2026-08-31` |
| normalization | `V41-NORM-WREP-1` |
| direction engine | `V41-DIRECTION-4H1H-1` |
| eight-score engine | `V41-SCORE-1` |
| data quality | `V41-DQ-1` |
| provider matrix | `V41-PROVIDER-MATRIX-1` |
| plan source gate | `V41-PLAN-SOURCE-1` |
| Home ranking | `V41-HOME-RANK-1` |
| Telegram eligibility | `V41-TELEGRAM-3C-1` |

The only legal calculation order is:

`Provider/raw -> DQ -> eight scores -> 4h/1h structural score ->
ruleMarketBias -> EvidenceReliability -> OpportunityScore -> RiskScore ->
validatedMarketBias -> Candidate -> deterministic plan boundaries -> GPT ->
Gemini -> Grok -> Conflict Resolver -> Rule Validation -> FinalConfidence ->
finalMarketBias -> Final/BLOCKED -> Home ranking -> Message/Telegram`.

`OpportunityScore` never consumes `FinalConfidence`. One production normalizer
owns all score inputs: Winsorized Rolling Empirical Percentile over 200 closed
samples, minimum 60, winsor bounds 2.5/97.5, output 0..100, isolated by provider,
venue, asset and timeframe. Missing samples return
`null/INSUFFICIENT_SAMPLE`; a synthetic neutral 50 is forbidden.

The 4h/1h structural score is `normalized4h*0.57 + normalized1h*0.43`.
Thresholds are +70/+35/+15 and -15/-35/-70; -14..14 is `RANGE`. `WAIT`
is a data/conflict state rather than a numeric band. Direction has three
separate maturity fields: internal eight-state `ruleMarketBias`, six-state
`validatedMarketBias` only after DQ/source/structure/non-confused gates, and
`finalMarketBias` only after Candidate, all three role calls, Resolver and Rule
Validation. RANGE, WAIT and missing validated direction cannot start a
directional plan.

The eight score owners are TrendStructure, CapitalFlow, LeverageRisk,
LiquidityQuality, SentimentTemperature, EventImpact, MacroEnvironment and
EvidenceReliability. LLM output cannot create or replace them. The formulas are:

`OpportunityScore = TrendStructure*0.30 + CapitalFlow*0.20 +
LiquidityQuality*0.15 + SentimentAlignment*0.10 + EventAlignment*0.10 +
MacroAlignment*0.05 + EvidenceReliability*0.10 - LeverageRiskPenalty -
ConflictPenalty - StalePenalty`.

`RiskScore = LeverageRisk*0.35 + LiquidityRisk*0.25 + EventRisk*0.20 +
ConflictAndExecutionRisk*0.20`.

`FinalConfidence = DQ*0.35 + MultiTimeframeConsistency*0.30 +
EvidenceCoverage*0.20 + CrossSourceConsistency*0.15 - ConflictPenalty`, and is
calculated only after Rule Validation.

Legal opportunity-state results are exact: observing has no directional Final;
candidate is plan generation in progress without user-visible Final parameters;
waiting_trigger permits `PREPARATION` only; triggered permits
`CONFIRMATION/REDUCED/PREPARATION/OBSERVATION/BLOCKED`; high_risk permits
`REDUCED/OBSERVATION/BLOCKED`; confused permits `BLOCKED` only; invalidated has
no active plan; cooling permits no new directional plan. In particular,
`waiting_trigger + REDUCED` and `high_risk + CONFIRMATION` are illegal.

The existing FinalExecutionPlan remains the only Final owner.
`CONFIRMATION/REDUCED` require complete sourced execution parameters;
`PREPARATION` requires complete trigger, invalidation and conditional
parameters; `OBSERVATION/BLOCKED` keep directional execution parameters null.
Only `CONFIRMATION/REDUCED` may offer manual position recording. No Final
creates a UserPosition.

Tier 1 Home ranking is
`DirectionStrength*0.30 + FinalConfidence*0.25 +
OneHourOpportunityQuality*0.20 + FourHourTrendAlignment*0.10 +
ExecutionFeasibility*0.10 + Freshness*0.05 - RiskPenalty - ConflictPenalty`.
Tie order is lower risk, higher FinalConfidence, higher 1h quality, newer data,
then symbol. Replacement requires a five-point lead unless the incumbent loses
eligibility. Tier 2 may fill only from the current user's real Asset Pool and
recent real analysis and never outranks Tier 1.

AI cache identity is symbol + timeframe + evidenceHash with a five-minute TTL.
Each analysis permits at most three role calls and one retry per role. Per-run,
per-asset, hourly, daily cost/token/call and concurrency limits are mandatory
configuration and fail closed when exhausted. Runtime acceptance must aggregate
one same-run chain from provider observation through persisted closed OHLCV,
analysis/scores/direction maturity, Candidate/role traces/Resolver/Validation,
Final, Home and Message eligibility with `fixture=false`; HTTP 200 alone is not
acceptance evidence.

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
plus Final Execution Plan, then single Three-AI workspace plus AI Consistency.
The earlier Position Monitoring / Final Execution Plan ratio `60:40` is
superseded. The frozen current desktop ratio is `70:30`; responsive stacking
may adapt presentation but cannot change this business layout contract.

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

## Appendix G. Owner-Final Web Live Direction and Risk Amendment

Status: `ACTIVE / OWNER_AUTHORIZED / 2026-09-06`

This amendment is part of this sole v4.1 Product Source. It supersedes only
the earlier direction-weight, normalization, rule-plan dependency, live-home,
risk-display, and Mobile-authorization details that conflict with the exact
`V41_WEB_LIVE_DIRECTION_RISK_CLOSURE` Owner instruction. It does not create a
second Analysis, Decision, Plan, Position, Monitor, Message, or frontend stack.

- Package: `V41_WEB_LIVE_DIRECTION_RISK_CLOSURE`
- Branch: `codex/v4-1-web-live-direction-risk-closure`
- Starting full SHA: `b87cb1878405a6fe8693add1036251d6631e2520`
- Official target: `https://trinelogic.com/dashboard`
- Direction: `V41-DIRECTION-4H1H-2`, using closed 4h and 1h bars only and
  `DirectionScore = 0.55 * Trend4H + 0.45 * State1H`. Trend and state use
  ATR/MAD/robust-volatility-normalized slope, structure, center, momentum,
  pullback, and acceleration features; the prior fixed-start 200-bar return
  percentile is not a primary direction feature.
- Shock gate: `V41-REALTIME-SHOCK-1`. Structural direction remains immutable
  between closed-bar recalculations; mark/index price, trade, 1m/5m, depth,
  liquidation, OI, funding, and crowding facts may warn, suspend, invalidate,
  or require revalidation but never manufacture a formal opposite direction.
- Confidence: `V41-CONFIDENCE-CALIBRATED-1`, the walk-forward probability that
  the next four hours reaches +1 ATR in the structural direction before -0.75
  ATR. It is capped at 75% for data quality 85-94 and 95% for quality >=95;
  quality below 85 or insufficient samples cannot show a valid confidence.
- Risk: `V41-RISK-VECTOR-1`, with independent asset risk items for chase,
  rapid move, trend reversal, crowding, liquidation, liquidity, event, and
  data risk. Each item owns score, severity, evidence, source, observation
  time, and recovery condition. Home shows the two highest concrete items.
- Plan: `V41-STRUCTURAL-PLAN-2`. Each of the six non-WAIT direction states owns
  a traceable conditional rule plan independent of AI availability. WAIT and
  multi-timeframe conflict expose truthful recovery conditions without fake
  prices. A suspended or invalidated plan is historical and is never revived;
  recovery requires a new AnalysisRun, Decision, snapshot, and plan version.
- Live Home: `V41-HOME-SSE-1`. An authenticated SSE stream carries monotonic,
  versioned price, direction, risk, plan, position-monitor, provider, and
  system events. The web client applies only newer events, polls after a
  disconnect at 15 seconds, reconciles a full snapshot every 60 seconds, and
  refreshes on focus without changing selected asset, AI tab, or open form.
- Position Monitor: `V41-POSITION-RISK-VECTOR-1`. Normal read-only monitoring
  runs every 30 seconds and urgent shock facts trigger immediate monitoring.
  Manual close remains an idempotent Owner action and is never automatic.
- This correction accepts only the 1440 x 900 desktop `trinelogic.com` browser
  route. The 390 x 844 mobile-web route is paused and its files are outside
  this package. Native iOS, Android, macOS, Windows, Electron, WebView shells,
  and every automatic trade action remain forbidden.
- Telegram keeps the existing Message/ChannelDelivery owner. Strong-direction
  complete-plan opportunities and material position/safety state changes use
  distinct idempotency keys; weak directions, fabricated facts, and duplicate
  same-severity messages are rejected.

Acceptance requires real Binance, CoinGlass, database, three-AI, Staging, and
official-domain browser evidence. Unit tests, fixtures, or a safely empty UI
cannot by themselves close this package.

## Appendix H. Owner Desktop Home Runtime and Priority Supplement

Status: `ACTIVE / OWNER_AUTHORIZED / 2026-09-08`.
This supplement records the Owner-approved desktop-web continuation only.
It takes priority solely over conflicting system-only Top6/no-user-pin ranking,
Telegram delivery of safety/Hot Reset/REDUCED plans, and mandatory empty-risk
placeholders. All other existing product contracts remain effective.

1. Home is "重点资产", showing at most six assets.
2. User-pinned assets come first, in the user's exact saved pin order.
3. Fill remaining slots with unpinned eligible opportunities only: effective
   direction, trusted fresh data, not RANGE/WAIT/timeframe conflict, not
   INVALIDATED/COOLING/CONFUSED/BLOCKED, and LOW or MEDIUM risk only. Sort by
   direction strength, risk, confidence, Final Plan Mode, Opportunity Score,
   freshness and analysis time. This changes selection, not any score formula.
4. When fewer than six assets qualify, show the actual count. Ordinary
   observation assets must not backfill the list.
5. Clicking/selecting an asset must not change Home membership or its order.
6. A CURRENT Final Execution Plan must use the same real Analysis, Decision,
   Trace and Plan identity chain and show actual entry, trigger, stop, targets,
   invalidation conditions and validity period. Do not invent identities or
   prices, borrow a different run, or label an incomplete rule plan as Final.
7. Real risks show "主要风险 · 等级 +N", where N counts additional actual risks.
   No risk items means hiding the entire row; unknown risk shows only "风险 —".
8. Telegram outbound is limited to two categories: currently valid,
   final-validated CONFIRMATION plans with STRONG_BULLISH or STRONG_BEARISH;
   and material risk changes for active positions with VERIFIED + FRESH
   monitoring. Existing identity, source, freshness, ownership, risk and
   deduplication safeguards remain required; no notification category expands.
9. Hot Reset and all other plan safety changes remain in-app Message/audit
   facts but must not create Telegram ChannelDelivery. Queue creation, orphan
   recovery and pre-HTTP dispatch must exclude them, including old pending
   safety messages, without deleting historical in-app Messages.
10. Pinning, reordering, refreshing, ordinary analysis and Provider status
    must not trigger Telegram.

This does not change direction, confidence or risk algorithms/thresholds.
The existing 28 Owner positions remain untouched. No APP/mobile-web work,
Production deployment, trades, real Telegram test send or real AI invocation
is authorized by this supplement. Registration is not business completion.

## Appendix J. Owner-authorized V42 directional risk and runtime closure

Status: `OWNER_AUTHORIZED / REGISTRATION_CANDIDATE / 2026-09-11`.
Package: `V42_ASSET_CARD_DIRECTIONAL_RISK_AND_RUNTIME_CLOSURE`.
Authorization package: `TRINE_LOGIC_V4_2_ASSET_CARD_DIRECTIONAL_RISK_AND_RUNTIME_CLOSURE_AUTHORIZATION`.
Source PR: `#1295`; audited source head: `2921a4a98254a4bd88f3138ed4eb2e0487b3956b`.
Registration branch: `codex/v4-2-asset-card-directional-risk-runtime-authorization`.
Implementation branch: `codex/v4-1-asset-card-live-signal-closure`.
Starting merged-main baseline: `2c71f1cd36ea7da6b7c5cf7d4d737aa4a70099b2`.
Scope: `HOME_ASSET_CARD_ONLY`.

Appendix J is an additive V42 registration preserving the existing 49 exact
asset-card paths, including `Dockerfile`, and adding the Owner's 15 exact
native systemd/JAR source and local-test paths (64 total).
It does not replace Appendix I, any V41 package, a
predecessor state, or generic gate policy. Business implementation remains
blocked until this registration is merged to `origin/main`; this registration
itself has no merge or deployment execution permission.

### J.1 Direction-to-side identity

The three bullish card directions (`STRONG_BULLISH`, `BULLISH`,
`WEAK_BULLISH`) map to `LONG`; the three bearish directions
(`STRONG_BEARISH`, `BEARISH`, `WEAK_BEARISH`) map to `SHORT`; `RANGE` and
`WATCH` map to `NON_DIRECTIONAL`. This mapping is an identity projection and
does not alter the frozen direction, confidence, or threshold formulas.

### J.2 Signed, independent risk semantics

The eight existing risk types and their `NONE`/`LOW`/`MEDIUM`/`HIGH` grades
remain unchanged. A directional metric is interpreted as adverse to the
current side, never by taking an absolute value: funding, long/short ratio,
liquidation imbalance, order-book imbalance, structural distance and returns
retain their sign. `LONG` assesses long-side adverse evidence, `SHORT`
assesses short-side adverse evidence, and `NON_DIRECTIONAL` never inherits a
previous side. CHASE, SHOCK, REVERSAL, CROWDING, LIQUIDATION and directional
LIQUIDITY therefore use the current side; DATA is side-neutral; EVENT is
directional only where reliable event direction exists and otherwise remains
two-sided. Opportunity score, direction strength and confidence are not risk
inputs. `UNKNOWN` is not converted to `LOW`, `NONE` or zero.

Every risk result is bound to `riskBasisSide`, `riskBasisDirection`,
`riskBasisSignalAsOf`, `riskMarketAsOf` and `riskVersion`, in addition to its
own evidence status, value, unit, source, observation time and reason. A side
change atomically recomputes all risk evidence; if the new snapshot cannot be
completed, risk is temporarily `—`/unknown and old-side evidence is rejected.
Known high risk cannot be hidden by an unknown item, and no aggregate grade is
copied into individual risk items.

### J.3 Runtime publication and failure domains

Closed Spot bars are processed once per `symbol + closed5mAt` identity, using
an idempotent per-symbol or closed-bar batch rather than a global serial lock.
All displayed assets at one boundary publish within the existing 15-second
budget or record an explicit timeout state. Real Binance Spot trade price,
depth and closed-bar work use separate queues or shards with reconnect,
backoff, queue-depth, dropped-frame and processing-latency evidence. Dynamic
subscriptions must not empty the displayed set during asset changes or a
23-hour stream rollover, and official sequence rules govern depth rebuilds.

PRICE, SIGNAL, RISK, PERSISTENCE and RECOVERY health/failure domains are
independent. A risk failure yields unknown risk only; a signal failure does
not erase a valid price; only a genuine Spot-price failure clears price. A
source-loss safety projection also emits DATA `HIGH` evidence. Effective risk
changes alone allocate a version, persist and publish SSE; health-check clock
changes do not create per-second risk events. Field-only events never invoke a
whole-Home reload and reject stale symbol/snapshot/threshold identities.

### J.4 Training and provenance identity

`takerBuySellRatio` is either a verified same-source feature or is removed
from the feature version before retraining. Historical observations preserve
instrument identity, source version, unit, observed/available/expiry times,
and Java/Python apply the same point-in-time freshness checks. Training
manifests name Binance Spot and each actual CoinGlass source separately.
Immutable trade observations and mature four-hour labels retain the real
future 1m/5m path and horizon trade; raw OI, depth, liquidation and trade
counts are never shared across assets without point-in-time normalization.
Missing feature combinations remain outside confidence coverage and show `—`.

Long and short XGBoost models and their Beta calibrators remain independent,
with temporal purged walk-forward splits and embargo. Confidence retains the
frozen meaning: the calibrated probability that the selected side reaches
`+1 ATR` before `-0.75 ATR` within four hours. Each published bundle carries
trained-through, valid-until, data/feature/model/calibration versions and a
threshold version. Without real training, calibration, stratified validation,
sample counts and Brier/ECE/LogLoss evidence, model mode remains `SHADOW` and
`PRODUCTION_MODEL_READY` remains `NO`; CANARY and ACTIVE are forbidden.

### J.5 Snapshot, storage and deployment boundary

Card results, risk side/version identity, SSE payloads and browser projection
must share one atomic snapshot and compare `expectedSnapshotVersion` before
publishing. Spot bars, feature history, observations and labels use an
explicit retention/archive policy; no existing business table or position is
altered. Any writer is limited to asset-card-owned storage and does not widen
generic database roles. Runtime model loading is read-only, checksummed and
manifest-bound; final Staging acceptance targets `NATIVE_SYSTEMD_JAR_RUNTIME`.
External Binance access has its own explicit safety switch and cannot be
enabled by the card flag alone.

The existing root Dockerfile runtime-stage `libgomp1` allowance is preserved:
`--no-install-recommends`, clear apt lists, no build-stage, Java/base-image
series, USER or ENTRYPOINT changes. Docker evidence is container-only
corroboration, not native Staging acceptance; Docker Hub EOF is not the final
blocker for the actual systemd/JAR chain. No host installation, additional
system package, apt upgrade or deployment is authorized in this stage.

The Owner selects an explicitly locked external base chain plus managed
asset-card-only attachments, not replacement of the public release system or
the withdrawn isolated `deploy/staging/*` files. The non-secret runtime
manifest must bind `TARGET_ARCH=x86_64`, `SERVICE_NAME=rine-logic.service`,
`APP_JAR=/opt/rine-logic/current/app.jar`, the SHA-256 of the main unit,
`20-core-loop-schedulers.conf` and `/usr/local/sbin/rine-logic-wait-ready`,
the current release-metadata format/path, model root and card drop-in target.
Do not read or record active.env, ai.env or other credential contents, hash
secret files, or commit passwords, tokens, host keys or environment values.
Any base-unit, scheduler-drop-in or readiness-script identity mismatch blocks
installation. Manifest placeholders are not verified runtime evidence.

Keep the default application DataSource/JdbcTemplate unchanged for existing
business reads. Use an independent explicitly named card DataSource and
JdbcTemplate for card-owned writes; existing OHLCV reads still use the default
read-only path. Missing connection, invalid credentials or insufficient
permissions fails closed with zero fallback writes through the default pool.
Bound pool size, timeouts and lifetimes, and release old connections on close
or rotation. Passwords may only be read from protected systemd credential
files; no password in argv, logs, exceptions, APIs or ordinary environment
variables.

The dedicated writer's exact effective privilege matrix is:

| Card table | Allowed | Forbidden |
|---|---|---|
| `tm_asset_card_snapshot` | SELECT, INSERT, UPDATE | DELETE |
| `tm_asset_card_spot_bar` | SELECT, INSERT, DELETE | UPDATE |
| `tm_asset_card_feature_history` | SELECT, INSERT, DELETE | UPDATE |

No access to other business tables or sequences; no DDL, CREATE, TEMP,
TRUNCATE, REFERENCES, TRIGGER, role inheritance/SET ROLE, database ownership
or schema ownership. Effective permissions, including PUBLIC and memberships,
must be checked without changing historical roles, old ACLs or existing data.
V24 remains limited to its three card tables: no fourth table or V25 is added.
Role/bootstrap/verify SQL is defined and tested only in disposable local
databases in this stage; it must not execute on the real server.

The card drop-in must not replace the main unit, modify active.env/ai.env or
the core scheduler drop-in. It exposes only the dedicated credential file and
read-only model directory; the service user cannot alter models, manifests or
credentials. SHADOW remains default; CANARY/ACTIVE never enable implicitly.
No script implicitly daemon-reloads, restarts or deploys.

Credential tooling checks only by default. Preparation/rotation requires an
independent exact confirmation string; reject symlinks, broad permissions,
wrong ownership and empty files. Do not leak or retain passwords in temporary
files. Verify new credentials using a separate fresh connection; failure
preserves usable old credentials and fails closed. Output `RESTART_REQUIRED`
when connection-pool restart is needed, never claim unperformed hot rotation.

Install models in immutable bundle-SHA directories, validating the manifest,
complete file list and every model SHA. Reject symlinks, path traversal,
duplicate assets and inconsistent identities. Switch atomically without
overwriting the old bundle and retain the rollback version. No qualified real
model means no current-production-model link. Installation is DRY_RUN by
default, verifies target ownership/permissions/real paths and base-chain SHAs,
and prepares a rollback inventory without restarting. Actual installation of
drop-in, credentials or models requires later independent deployment approval.

Read-only preflight checks x86_64, Java 17, libgomp.so.1, JAR SHA, base-unit
identity, directory permissions, model SHA and credential metadata. It cannot
open, print or hash secret contents. The non-Web standard-JAR native probe
loads real XGBoost/libgomp and separate long/short UBJ/Beta inputs, predicts
fixed float32 features, and reports version, model SHA, maximum error and
PASS/FAIL without secrets. It cannot connect to a real database, Binance,
CoinGlass, AI or Telegram. Delete temporary models after tests. Architecture,
native-library, version or SHA mismatch fails closed; readiness 200 alone is
not native prediction PASS. `MODEL_MODE=SHADOW` and
`PRODUCTION_MODEL_READY=NO` remain unchanged.

### J.6 Acceptance and action boundary

Acceptance must prove side-opposite risk semantics, non-directional RANGE/WATCH
without numeric confidence, one result per closed bar under 128-asset pressure,
field-isolated failures/SSE, stale identity rejection, bounded Spot-to-DOM and
risk latency, native model loading, reconnect recovery, and unchanged Home
geometry. The 28 existing Owner positions, execution plans, Three-AI,
Telegram, trading, native clients, mobile web, Production and unrelated Home
modules remain outside scope. No real AI or Telegram call is part of this
registration.

The registration gate permits only the seven exact contract/YAML/script paths
listed in the task handoff, with no wildcard or directory grant. Preserve all
original 49 paths and add exactly these 15 source/local-test paths: 64 unique
implementation paths, fingerprint `4262f151a513d7bec00bcc9f0614531d8cae537f`.

```text
src/main/java/org/example/trademodel/assetcard/AssetCardDataSourceConfiguration.java
src/main/java/org/example/trademodel/assetcard/AssetCardNativeRuntimeProbe.java
src/test/java/org/example/trademodel/assetcard/AssetCardDataSourceConfigurationTest.java
src/test/java/org/example/trademodel/assetcard/AssetCardNativeRuntimeProbeTest.java
deploy/native-staging/README.md
deploy/native-staging/rine-logic-asset-card.conf.template
deploy/native-staging/asset-card-role-bootstrap.sql
deploy/native-staging/asset-card-role-verify.sql
deploy/native-staging/asset-card-runtime-credentials.sh
deploy/native-staging/asset-card-model-install.sh
deploy/native-staging/asset-card-runtime-preflight.sh
deploy/native-staging/asset-card-runtime-manifest.template
deploy/native-staging/asset-card-runtime-install.sh
scripts/asset-card-native-staging-matrix.sh
src/test/java/org/example/trademodel/postgresql/NativeStagingAssetCardInfrastructureContractTest.java
```

Tests must prove unchanged default read-only access, zero writer fallback,
every allowed/denied table privilege and unchanged old ACLs; secret symlink,
permission, failed-rotation and redaction cases; read-only models, bad SHA or
identity, rollback, and installation refusal on base-chain SHA mismatch.
Require x86_64 standard-JAR long/short XGBoost plus Beta prediction, Shell
matrix, focused/full Maven, Python/frontend matrices and exact-head CI.
Container-only evidence must never be reported as native Staging acceptance.
After the exact amended gate merges into `origin/main` and implementation
permission is verified, continue source definitions and local/disposable tests
on the same PR #1295 branch only, then stop before business merge. No real
server changes, database-role/ACL operations, credential/model installation,
daemon-reload, restart or deployment. `BUSINESS_PR_MERGE=NO`,
`STAGING_DEPLOYMENT=NO`, `REAL_DATABASE_PERMISSION_CHANGE=NO`,
`SYSTEMD_CHANGE_EXECUTION=NO` and `PRODUCTION_MODEL_READY=NO` remain explicit.

`MERGE_AUTHORIZATION=NO`, `DEPLOY_AUTHORIZATION=NO`,
`MODEL_MODE=SHADOW`, and `BUSINESS_IMPLEMENTATION_BEFORE_GATE_PASS=NO` remain
machine-enforced. Eligibility flags do not execute a merge; only a later
Owner-approved action may do so after this exact registration is merged and
the gate is effective on `origin/main`.

<!-- END APPENDIX J V42 -->

## Appendix I. Owner Asset-Card Live Signal Contract

Status: `OWNER_AUTHORIZED / REGISTRATION_CANDIDATE / 2026-09-10`.
Package: `V41_ASSET_CARD_LIVE_SIGNAL_CLOSURE`.
Branch: `codex/v4-1-asset-card-live-signal-closure`.
Starting merged main: `094b70a8ed31891999da0814fae5add09e2c4e08`.

This records the Owner's complete Asset Card Live Direction, Calibrated
Confidence and Explainable Risk Closure instruction. Only the existing Home
asset-card surface may adopt the following new computation and display
contract. Earlier card-only five-direction definitions, weighted/proxy/fallback
confidence, opportunity-derived risk, copied component risk, and price-event
full-Home reload semantics are superseded here. All other contracts remain
effective. This is not permission to change the Canonical decision chain.

### I.1 Ownership and isolation

Add a separate `cardSignal` / `AssetCardSnapshot`, never overwrite
`finalMarketBias`, `finalConfidence`, `riskLevel`, Analysis, Decision, or Plan.
The snapshot owns symbol, assetName, spotPrice and price observation time;
signal(direction, status, calibratedConfidence, pLong, pShort, oneHourState,
fourHourTrend, signalAsOf); risk(overallLevel, items, riskAsOf); cardAsOf;
monotonic snapshotVersion; featureVersion, modelVersion and calibrationVersion.
Probabilities and versions are audit metadata, not additional visible fields.
Persist UTC timestamps and reject older symbol/snapshot versions.

Reuse authenticated Home/SSE transport, Pool reads, stored market/evidence
reads and existing test infrastructure. Add only genuinely independent card
snapshots, features, model/bundle and risk owners. Do not modify Pool
eligibility, saved pin order, automatic ranking/fill, search, other Home
regions, Final Plan, PositionMonitor, Three-AI, Telegram, login/Cloudflare,
Analysis pages, native clients or mobile-specific files. User positions are
immutable for this task. No automatic trading or real AI/Telegram call.

### I.2 Real data and model

The only displayed current price is Binance Spot real trade price. Futures
Mark Price, CoinGlass price, stale analysis prices and mixed-venue inputs are
not substitutes. Dynamically subscribe to symbols needed by actual cards and
the existing observation pool without changing pool membership or ordering.
Keep the shared futures stream and its existing plan/monitor consumers intact.

Train separate XGBoost Long Success and Short Success binary models offline.
The horizon is four hours; ATR is fixed at signal time. Long success is first
+1 ATR before -0.75 ATR, with timeout a failure; short is symmetric. If one
closed 5m bar touches both, resolve with closed 1m ordering. Remaining ambiguous
samples are excluded from training, calibration and testing, never assigned a
convenient outcome. Version the ATR/feature definitions in the model bundle.

Features use closed Spot 5m/15m/1h/4h OHLCV, momentum, ATR/volatility, volume,
slope, structure, taker trades, actual spread/depth, and time-aligned existing
CoinGlass OI, funding, long/short, liquidation and available taker evidence.
Both observation time and availability time must not exceed signal time.
Missing historical inputs remain missing. Current snapshots cannot be copied
backwards into history; fixtures cannot train a production model.

Fit separate long/short Beta calibrators on an independent calibration split:
`logit(p_calibrated) = a*ln(p_raw) - b*ln(1-p_raw) + c`, clipping endpoints
safely. Neither calibrator nor threshold selection may use the final test set.
Use temporal purged walk-forward splits and embargo for overlapping 4h labels.
Spring Boot Java 17 only loads verified, checksummed XGBoost model bundles;
there is no runtime training or resident Python service.

Publish features, both models/calibrators, thresholds, provenance and validation
report as one atomic version. Strength/probability-gap thresholds come from
out-of-sample validation, with strict strong > normal > weak ordering,
adequate probability-band samples and fee/slippage-adjusted positive edge.
Do not invent thresholds or use 60 samples as a production-readiness shortcut.
Release gates require calibrated Brier better than raw and base-rate baselines,
correct binned ECE, non-collapsed calibration curves, LogLoss and per-asset,
market-regime and volatility strata, plus independent evidence for each tier.
Missing data, model, calibrator or validation fails closed; no old weighted,
65/70/90 proxy, AI-derived or alternate numerical confidence is reachable.

### I.3 Eight directions and clocks

The exact card directions are STRONG_LONG (强偏多), LONG (偏多), WEAK_LONG
(弱偏多), STRONG_SHORT (强偏空), SHORT (偏空), WEAK_SHORT (弱偏空), RANGE
(震荡), WATCH (观望). RANGE is a supported range state; WATCH requires
sufficient data but no stable advantage or unresolved conflict. Missing data
is not WATCH: display `方向：— · 数据不足`.

Only calibrated pLong/pShort supplies an integer confidence percentage for
the corresponding directional family. RANGE, WATCH, invalidated, missing or
unvalidated results show `—`, never text confidence or a second formula.
Each closed 5m triggers one card inference, with publication within 15 seconds.
Require two consecutive 5m confirmations before switching direction; update
1h/4h features only on their closes. Structural breach, extreme 1m/5m movement,
liquidity deterioration or core-source loss may invalidate within 1-5 seconds.
Invalidation retains the previous direction labelled 已失效, hides confidence
and shows high risk; it cannot manufacture an opposite direction.

The background line is exactly one of 1小时机会/观察/冲突/数据不足 and
4小时趋势偏多/偏空/震荡/数据不足, from closed data. It is not another
confidence formula. Price has its own time. cardAsOf changes only with an
effective direction/confidence/risk change, not price ticks or unchanged polls.

### I.4 Independent risks

Eight types are CHASE, SHOCK, REVERSAL, CROWDING, LIQUIDATION, LIQUIDITY,
EVENT, DATA. Every item owns assessmentStatus ASSESSED/UNKNOWN, level
NONE/LOW/MEDIUM/HIGH, evidenceValue, source, asOf and reason. UNKNOWN is not LOW.

CHASE uses structural-center/ATR distance and extension; SHOCK uses real
1m/5m volatility, ATR and historical percentiles; REVERSAL uses 5m/1h/4h
conflict and invalidation; CROWDING uses funding/long-short/OI; LIQUIDATION uses
directional liquidation amounts and imbalance; LIQUIDITY uses actual spread,
10/25bps depth and order-book anomalies; EVENT reuses existing macro/news/event
facts; DATA uses source health, freshness, missingness and identity consistency.
Thresholds are versioned asset-history distributions, rolling percentiles and
explicit hard safety conditions. No opportunity score, 24h-range liquidity
proxy, aggregate-to-item copy or claim that raw CoinGlass facts are risk grades.

Overall risk is HIGH if any assessed item is HIGH, otherwise MEDIUM if one is
MEDIUM. LOW requires all necessary assessments complete without medium/high;
otherwise show `—`. Unknown items cannot conceal a known high risk. Display
at most three medium/high items ordered by severity, invalidation effect and
freshness; hide the detail row when there are none. Real-time risk reacts in
1-5 seconds; CoinGlass evidence is reconciled around 60 seconds subject to its
existing provider limits; event risk updates on evidence arrival.

### I.5 Existing card rendering and events

Preserve the current card dimensions, position, grid and whole-Home layout
(Owner's UI Freeze 1.2 constraint); do not redesign a screen or add charts,
buttons, thick shadows, glass effects or long explanations. Show symbol/name,
Spot price, direction, abnormal-only state, integer confidence or `—`, overall
risk, up to three real active risks and the closed-timeframe background line.
Remove card-only 24h/undefined percentage changes, 更新于, normal 有效 labels,
source/version text and unsupported risk names. The lower-right time is only
HH:mm:ss in the user's timezone, without date/zone text. Use tabular figures,
stable numeric layout and subtle field transitions, not whole-card flashing.
Directional greens/reds have strong/normal/muted levels; RANGE is the frozen
neutral/blue-gray, WATCH gray; risk medium orange, high red and unknown gray.

ASSET_CARD_PRICE patches price only; ASSET_CARD_SIGNAL patches direction,
status, confidence and timeframe fields; ASSET_CARD_RISK patches risk fields;
ASSET_CARD_HEALTH patches abnormal data status. Signal/risk changes carry the
effective card clock. No card event invokes loadHome, reloads other cards,
positions, plans or AI, changes membership, or reorders a slot. Throttle price
rendering to 1-2 seconds. Use one 60-second read-only card reconciliation and
15-second disconnected fallback; stop fallback on SSE recovery. Reject stale
responses/events by symbol and snapshotVersion.

### I.6 Readiness, modes and acceptance

ASSET_CARD_MODEL_MODE is LEGACY (rollback only, no numerical fallback), SHADOW
(compute/audit without displaying unvalidated model results), CANARY (explicit
symbols only) or ACTIVE. Default new facilities to SHADOW. Missing historical
data may permit infrastructure completion, but confidence stays `—` and
PRODUCTION_MODEL_READY remains NO. Record actual sample counts and provenance;
unknown coverage/counts are UNKNOWN, not fabricated zero or invented metrics.

Tests must cover all eight directions, independent calibration, split/label
leakage and ambiguity, missing data, confirmation/invalidation, independent
risks/aggregation, clocks, versions, field-only events and unchanged downstream
chain/pins/eligibility. Require focused/frontend/data tests, full Maven,
product/machine/workflow gates, diff checks and exact-head CI. No model or
business completion can be inferred from docs, DTOs, fixtures or tests alone.

No merge or deployment permission is granted by this registration. Gate-only
review/PR may proceed; implementation remains blocked until exact registration
is merged main, and business merge and Staging deployment require separate
Owner approval. Deployment must not be inferred from earlier packages.
When authorized, real Staging acceptance requires 30 seconds of Spot price
observations (p95 <=2s), closed-5m result <=15s, risk/source timestamps, SSE
disconnect/recovery, zero full-Home reload storm, 1440x900 screenshot and narrow
window checks, unchanged geometry and zero downstream side effects. Without
that evidence CURRENT_PHASE_DONE=NO and FINAL_GATE=FAIL.

### I.7 Owner-corrected merge eligibility and actual-action boundary

For this registration, current_package_merge_allowed=true and
authorized_next_package_merge_allowed=true describe eligibility after all
required gates pass; neither grants permission to execute a merge now.
Owner withdraws the earlier merge=false exception design. Do not change the
generic machine_gate_policy_check, other packages or global merge/deploy rules.

Only exact identity recognition for V41_ASSET_CARD_LIVE_SIGNAL_CLOSURE and its
authorization package is added: exact branch, 40-character baseline SHA,
permission tuple, canonical Appendix I, path counts and fingerprints.
Keep worktree identity and origin/main effectivity checks. Never assign main
effectivity or copy a predecessor's permission/status to unlock this package.

This step may validate, commit, push and open the contract/gate-only Draft PR,
then must stop for separate Owner merge approval. MERGE_EXECUTED=NO and
DEPLOY_EXECUTED=NO. Staging and Production deployment are not authorized.
Business implementation remains blocked until this exact authorization
contract and gate have merged into origin/main and the gate passes. The local
candidate passing checks does not activate the future implementation scope.
