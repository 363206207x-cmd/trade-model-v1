# TRINE LOGIC Core Production Loop Automation Ownership Map

Status: `AUTHORIZATION_OWNERSHIP_FREEZE`

| Responsibility | Canonical existing owner | New owner allowed |
|---|---|---|
| Pool membership and scan universe | AssetPool / PoolItem | NO |
| Opportunity lifecycle and transition audit | Opportunity / canonical StateService / StateLog | NO |
| Analysis idempotency and trace | AnalysisRun / existing scheduler-idempotency owner | NO |
| Evidence, eight scores and decision | Existing EvidenceItem / ScoreItem / DecisionBundle | NO |
| GPT/Gemini/Grok call audit | AITrace | NO |
| Conflict and Final validation | ConflictResolverResult and RuleValidation/Final validation | NO |
| Final plan | FinalExecutionPlan | NO |
| Real position and manual lifecycle | UserPosition | NO |
| Position monitoring | PositionMonitor / PositionMonitorLog | NO |
| Review | Existing Review/OpportunityLog owners | NO |
| In-application notification fact | Message | NO |
| Telegram delivery lifecycle | ChannelDelivery plus existing Telegram provider | NO |
| Market candles | Persisted source-owned OHLCV owner | NO |

## Duplicate Skeleton Gate

- New Asset Pool, Opportunity, Analysis, Final, Position, Monitor, Review,
  Message, ChannelDelivery or Telegram object family: `BLOCKED`.
- A cadence-only `nextScanAt` database column/table: `BLOCKED`.
- A second scheduler may not own state, analysis, delivery or position facts.
- Closed PR #1201 remains non-effective audit evidence and cannot become a
  parallel runtime stack.
- Runtime coordination may extend existing scheduler/configuration boundaries
  only when the exact implementation package is effective on merged main.

## Safety Ownership

Rule Validation retains Final confirmation authority. User action retains
position-creation and close authority. PositionMonitor outputs facts and
suggestions only. Message remains the sole notification fact owner.
Automatic trading capability count: `0`.
