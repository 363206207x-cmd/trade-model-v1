# AI Role Structured Contract Alignment

## Scope

This package closes `GAP-P0-001` by aligning the real AI review producer, decision persistence, Dashboard Home consumer, and role-specific UI read model.

It does not call live providers, change rule authority, create positions, create orders, send Push/Telegram, or add trading behavior.

## Old Producer Format

`DecisionEngineService` previously persisted `AiOrchestratorResult.toSanitizedSummary()` plus extra plain text. The value was semicolon-delimited text such as `orchestrationMode=...; providers=...`.

`DashboardHomeServiceImpl` attempted to parse the same column as arbitrary JSON and recursively guessed role aliases. Production rows therefore could not populate the role panels, while tests could pass by injecting JSON shapes the producer never emitted.

## New Schema

Serialization uses `AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V1`:

```json
{
  "schemaVersion": "v1",
  "analysisId": "...",
  "traceId": "...",
  "ruleVersion": "...",
  "orchestrationMode": "AI_ASSISTED",
  "orchestrationReasonCodes": [],
  "roles": {
    "GPT_FINAL": {
      "role": "GPT_FINAL",
      "provider": "OPENAI",
      "sourceRole": "GPT_RULE_REVIEW",
      "callStatus": "SUCCESS",
      "stance": "SUPPORT",
      "conflictLevel": "NONE",
      "reasonCodes": [],
      "summary": "...",
      "manualReviewRequired": true
    },
    "GEMINI_REVIEW": {},
    "GROK_CHALLENGE": {}
  },
  "synthesis": {
    "finalMarketBias": "BULLISH",
    "finalConfidence": "MEDIUM",
    "finalRiskLevel": "HIGH",
    "worthOpening": false,
    "conflictLevel": "LEVEL_3_SIGNIFICANT_DIVERGENCE",
    "conflictScore": 68,
    "confidenceAdjustment": "MEDIUM",
    "riskAdjustment": "RAISED",
    "planModeAdjustment": "PREPARE_ONLY",
    "confused": false,
    "downgradeReason": "ROLE_REASON_CODE"
  },
  "safety": {
    "reviewOnly": true,
    "manualReviewOnly": true,
    "notTradeInstruction": true,
    "notExecutable": true,
    "notAutoTrading": true,
    "notOrderExecution": true,
    "notUserPositionCreation": true,
    "notPositionMutation": true,
    "notStateMachineOverride": true,
    "ruleDirectionPreserved": true
  }
}
```

Only role fields already produced by `AiProviderReviewResult` are serialized: provider, source role, call status, stance, conflict level, reason codes, summary, and fallback state. Unsupported event/news/microstructure evidence remains empty.

Rule direction, adjusted confidence, risk, plan mode, worth-opening result, conflict, and confused state belong to `synthesis`; they are not represented as provider-generated evidence.

## Ownership

- Typed contract: `AiRoleResultsPayload`
- Serialization/deserialization/sanitization owner: `AiRoleResultsCodec`
- Real producer: `DecisionEngineService`
- Persistence field: `tm_decision_result.ai_role_results` (`TEXT` in H2 and PostgreSQL Flyway baseline)
- Persistence mapper: `DecisionResultMapper`
- Read model: `DecisionResultVO.aiRoleResults`
- Consumer: `DashboardHomeServiceImpl`
- API/UI model: `DashboardHomeVO.AiDecisionVO`, including `schemaVersion`
- Renderer: existing role-specific Dashboard Home panels

## Consumer Rules

- Only `schemaVersion=v1` with an object-valued `roles` node is accepted.
- Role keys must be exactly `GPT_FINAL`, `GEMINI_REVIEW`, or `GROK_CHALLENGE`.
- The embedded role name must match its map key.
- Missing roles remain empty; no content is copied from another role.
- Role reason codes become support/counter evidence only when the real stance supports that interpretation.
- Unsupported role-specific evidence remains empty.
- The Home read path never calls the AI orchestrator or provider clients.

## Legacy Behavior

- Blank values produce empty role panels.
- Semicolon/plain-text rows are classified as legacy and do not become role evidence.
- Malformed JSON fails closed.
- JSON without the supported schema version fails closed.
- Existing rows are not rewritten by this package.

## Sanitization

The persisted payload excludes provider request IDs, raw provider payloads, token counts, costs, headers, credentials, and request bodies.

The codec redacts:

- bearer values;
- authorization values;
- API keys;
- provider secrets;
- passwords and access tokens;
- OpenAI, Gemini, and xAI key-shaped strings.

Summaries and identifiers are length bounded; reason codes are normalized and capped.

## Tests

- `realProducerCreatesVersionedStructuredAiRolePayload`
- `fullRolePayloadIsSanitized`
- `structuredAiRolePayloadPersistsAndLoadsWithoutLoss`
- `producerToDashboardHomeIntegrationRendersAllThreeRoles`
- malformed/unsupported/legacy fail-closed coverage
- missing-role no-cloning coverage
- Home read has no AI orchestrator/provider call
- rule direction/state/position safety coverage
- full Dashboard and repository regression suite

## Local Runtime Check

A local application instance was started on port `8081` with all schedulers disabled and a disposable H2 file database containing deterministic role payloads only.

- `BNBUSDT` returned `schemaVersion=v1` and three distinct role tabs.
- GPT exposed only `GPT_SUPPORT_ONLY` and the GPT summary.
- Gemini exposed only `GEMINI_CONTRADICTION_ONLY` and the Gemini review summary.
- Grok exposed only `GROK_COUNTER_ONLY` and the Grok challenge summary.
- A legacy plain-text `BTCUSDT` row returned no schema version and no evidence in any role tab.
- Dashboard HTML retained all three role labels and honest waiting/empty states.
- The Home read path made no AI orchestrator or provider call.

## Result

- `AI_ROLE_PRODUCER_CONTRACT: PASS`
- `AI_ROLE_PERSISTENCE_CONTRACT: PASS`
- `AI_ROLE_HOME_CONSUMER_CONTRACT: PASS`
- `PROVIDER_CALLS_DURING_HOME_READ: 0`
- `GAP_P0_001: CLOSED`
- `PRODUCTION_READINESS: BLOCKED`
