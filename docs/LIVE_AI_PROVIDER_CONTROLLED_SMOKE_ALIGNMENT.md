# LIVE-AI-1 Controlled AI Provider Alignment

## Scope

- Package: LIVE-AI-1
- Audit date: 2026-07-11
- Base commit: 5d35bba2f36b04a062f1626a833674dc82c63861
- Providers: OpenAI, Google Gemini, xAI
- Live provider calls made by this package: 0
- Real keys read by this package: 0
- Production readiness: BLOCKED

This package aligns current model and HTTP contracts and adds a single-provider, single-request smoke harness. It does not run a live smoke, start Spring, connect a database, invoke the AI orchestrator, run a scheduler, create business records, send external notifications, or authorize trading.

## Official Source URLs

Only first-party documentation was used:

- OpenAI current model catalog: <https://developers.openai.com/api/docs/models>
- OpenAI GPT-5.6 Sol: <https://developers.openai.com/api/docs/models/gpt-5.6-sol>
- OpenAI GPT-5.6 Terra: <https://developers.openai.com/api/docs/models/gpt-5.6-terra>
- OpenAI GPT-5.6 Luna: <https://developers.openai.com/api/docs/models/gpt-5.6-luna>
- OpenAI GPT-5.5: <https://developers.openai.com/api/docs/models/gpt-5.5>
- OpenAI GPT-5.4: <https://developers.openai.com/api/docs/models/gpt-5.4>
- OpenAI Responses create: <https://developers.openai.com/api/reference/resources/responses/methods/create>
- OpenAI API errors: <https://developers.openai.com/api/docs/guides/error-codes>
- OpenAI request IDs: <https://developers.openai.com/api/reference/overview#debugging-requests>
- Gemini lifecycle: <https://ai.google.dev/gemini-api/docs/deprecations>
- Gemini 3.5 Flash: <https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash>
- Gemini generateContent: <https://ai.google.dev/api/generate-content>
- Gemini authentication: <https://ai.google.dev/gemini-api/docs/api-key>
- xAI Grok 4.5: <https://docs.x.ai/developers/grok-4-5>
- xAI Responses and Chat reference: <https://docs.x.ai/developers/rest-api-reference/inference/chat>
- xAI Responses comparison: <https://docs.x.ai/developers/model-capabilities/text/comparison>
- xAI reasoning controls: <https://docs.x.ai/developers/model-capabilities/text/reasoning>

## Current Provider Contract

| Provider | Previous default | Previous endpoint | Authentication | Default enabled |
|---|---|---|---|---|
| OpenAI | gpt-4.1-mini | POST /v1/responses | Authorization: Bearer | No |
| Gemini | gemini-1.5-flash | POST /v1beta/models/{model}:generateContent | x-goog-api-key | No |
| xAI | grok-3-mini | POST /v1/chat/completions | Authorization: Bearer | No |

The parser accepts only the review-only schema. It rejects direction overrides, execution fields, order fields, position mutations, and prompt-injection text.

## Official Contract Audit

| Provider | Official current contract | Assessment | Required change |
|---|---|---|---|
| OpenAI | GPT-5.6 Luna targets speed-sensitive workloads, GPT-5.6 Sol is the family frontier model, and GPT-5.5/GPT-5.4 support Responses and reasoning | Single-model gap | Route normal checkpoints to Luna, complex conflicts to Sol, then fall back only to GPT-5.5 and GPT-5.4 |
| Gemini | Gemini 1.5 Flash was shut down; gemini-3.5-flash is stable GA | Previous default unusable | Change default model; retain generateContent mapping |
| xAI | grok-4.5 supports Responses and Chat; xAI recommends Responses and labels Chat deprecated | Endpoint deprecation risk | Change default model and migrate to POST /v1/responses |

## OpenAI Decision

### Current implementation

The implementation uses POST /v1/responses, Bearer authentication, model, instructions, input, max_output_tokens, and GPT-5 reasoning controls. The earlier single-model compatibility strategy is removed.

### Official current contract

The official catalog identifies gpt-5.6-sol as the GPT-5.6 frontier model and recommends gpt-5.6-luna for speed- and cost-sensitive workloads. Both support reasoning and Responses, while the catalog currently limits GPT-5.6 preview availability to select trusted partners. GPT-5.5 and GPT-5.4 are reasoning-capable Responses models whose model pages are not marked preview or deprecated, and they form the only approved fallback tiers.

### Required change

GPT_FINAL uses role-aware environment properties for a fast model, a reasoning model, and two bounded fallback tiers. The client keeps POST /v1/responses and high reasoning effort. Request-ID mapping prefers x-request-id, with response id as a presence fallback.

### No change reason

No GPT-4-family fallback exists. Invalid configuration or exhaustion of GPT-5.6, GPT-5.5, and GPT-5.4 returns MODEL_UNAVAILABLE with OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE.

## AI Role Model Strategy

AI is checkpoint-driven rather than a high-frequency chat surface. Model selection therefore follows role responsibility instead of applying one low-cost assumption to every provider.

| Role | Provider default | Priority | Responsibility |
|---|---|---|---|
| GPT_FINAL | routed GPT-5.6/5.5/5.4 | QUALITY_FIRST | Final adjudication, instruction following, and conflict resolution |
| GEMINI_REVIEW | gemini-3.5-flash | BALANCED | Low-latency consistency review and structured-output checking |
| GROK_CHALLENGE | grok-4.5 | ADVERSARIAL_CHALLENGE | Contradiction detection and counter-challenge generation |

The configuration keys are trade-model.ai.model-strategy.gpt-final.priority, gemini-review.priority, and grok-challenge.priority. Provider status exposes configuredModel, effectiveModel, fallbackUsed, and fallbackReason without exposing API keys.

Model readiness is separate from credential presence:

- MODEL_CONFIGURED: an approved model route is selected, but no successful provider response has verified it in this process.
- MODEL_ACTIVE: the configured primary model has returned a successful contract-valid response in this process.
- MODEL_FALLBACK_ACTIVE: an approved GPT-5.5 or GPT-5.4 fallback was selected.
- MODEL_UNAVAILABLE: configuration is invalid or no approved model remains available.

The status endpoint does not report ready=true or MODEL_ACTIVE merely because an API key and model are configured. Missing or malformed model selection fails closed.

The detailed OpenAI model/API contract verification is recorded in `docs/OPENAI_GPT5_MODEL_ROUTING_CONTRACT.md`.

## GPT_FINAL Model Routing Strategy

### Approved model range

Only official GPT-5.6, GPT-5.5, and GPT-5.4 model IDs are accepted. GPT-4.x, GPT-4o, GPT-5.3 and older model families are rejected by configuration validation and runtime routing.

### Fast decision model

`gpt-5.6-luna` is the default FAST_DECISION_MODEL. OpenAI recommends Luna for speed- and cost-sensitive workloads, and it retains GPT-5.6 reasoning and Responses support. It handles candidate review, waiting-trigger review, normal execution-plan review, and normal position-logic review.

### Deep reasoning model

`gpt-5.6-sol` is the default DEEP_REASONING_MODEL and the official GPT-5.6 frontier tier. It is selected for AI conflict, confused state, Hot Reset, extreme events, high risk, multi-timeframe contradiction, blocked external context, and rule/evidence conflict.

### Fallback chain

The only fallback chain is `gpt-5.5` followed by `gpt-5.4`. A timeout or model/provider unavailability may advance one tier. Authentication, billing, and rate-limit failures do not trigger model fallback. Exhausting GPT-5.4 returns MODEL_UNAVAILABLE; no lower model is selected.

Every fallback result records originalModel, selectedModel, fallbackLevel, fallbackReason, modelRoutingTimestamp, and modelRoutingTraceId. Persisted call-log response summaries retain the same routing evidence. Reason codes include OPENAI_PRIMARY_UNAVAILABLE, OPENAI_FAST_MODEL_TIMEOUT, OPENAI_REASONING_MODEL_UNAVAILABLE, OPENAI_FALLBACK_GPT55, OPENAI_FALLBACK_GPT54, and OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE.

Environment overrides are `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL`, `TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL`, `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT55_MODEL`, and `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT54_MODEL`. Overrides remain subject to the same allowlist.

## Gemini Decision

### Current implementation

The client uses generateContent with x-goog-api-key, systemInstruction, contents, generationConfig.maxOutputTokens, generationConfig.temperature, candidate text, usageMetadata, and responseId/header trace mapping.

### Official current contract

Gemini 1.5 Flash was shut down on 2025-09-29. Gemini 2.5 Flash remains stable but has a documented 2026-10-16 shutdown date and Google recommends Gemini 3.5 Flash. Gemini 3.5 Flash is stable, GA, and non-preview.

### Required change

The default changes to gemini-3.5-flash. The endpoint and mapper remain aligned. TRADE_MODEL_AI_GEMINI_MODEL override remains supported.

## xAI Decision

### Current implementation

The previous client used grok-3-mini and Chat Completions.

### Official current contract

grok-4.5 is documented for Responses and Chat Completions. xAI recommends Responses and marks Chat Completions deprecated. The Responses shape uses model, input, max_output_tokens, output content, and usage input/output/total tokens.

### Required change

The default changes to grok-4.5 and the client moves to POST /v1/responses. The request uses instructions, input, max_output_tokens, reasoning effort low, and store false. Temperature is omitted for this reasoning model rather than relying on undocumented compatibility. TRADE_MODEL_AI_XAI_MODEL override remains supported.

### Deprecation risk

The official page notes region-dependent API-console availability. Publication does not prove operator account entitlement. A later controlled smoke is required.

## Controlled Smoke Design

The harness consists of AiProviderControlledSmoke, AiProviderControlledSmokeResult, scripts/ai-provider-controlled-smoke.sh, and AiProviderControlledSmokeTest.

It directly instantiates one provider client. It does not create a Spring context and cannot run the orchestrator, decision engine, schedulers, mappers, repositories, controllers, or business services.

## External Call Gates

All conditions must be true before one request is possible:

1. AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=true
2. AI_PROVIDER_SMOKE_TARGET is exactly OPENAI, GEMINI, or XAI
3. TRADE_MODEL_AI_ENABLED=true
4. The selected TRADE_MODEL_AI_PROVIDER_ENABLED flag is true
5. The selected key already exists in the current shell
6. The script-only internal harness marker is present

ALL, MULTI, THREE, wildcard, comma-separated, blank, and unknown targets fail closed. There is no fallback to another provider.

## One-Provider-One-Request Rule

- One provider target
- At most one HTTP POST
- No retry, loop, concurrency, fallback provider, or multi-role orchestration
- Request timeout: 5 seconds
- Script watchdog: 60 seconds including Maven harness startup
- Maximum output: 128 tokens

## Sanitized Output Contract

Allowed output fields are:

    AI_PROVIDER:
    AI_MODEL:
    AI_AUTH_STATUS:
    AI_HTTP_STATUS_CLASS:
    AI_RESPONSE_PARSE_STATUS:
    AI_TOKEN_USAGE_PRESENT:
    AI_REQUEST_ID_PRESENT:
    AI_LATENCY_MS:
    AI_PROVIDER_LIVE_SMOKE:
    LIVE_PROVIDER_CALLS:
    REAL_KEYS_READ:
    PRODUCTION_READINESS:

The output never includes a key, key shape, authorization header, request body, prompt, raw response body, raw error body, raw headers, complete request ID, or provider summary.

## Failure Classification

| Condition | Result |
|---|---|
| external gate closed | SKIPPED_EXTERNAL_CALLS_DISABLED |
| key absent | SKIPPED_MISSING_API_KEY |
| global or provider switch disabled | SKIPPED_PROVIDER_DISABLED |
| target not exactly allowlisted | FAIL_INVALID_TARGET |
| HTTP 401/403 | FAIL_AUTH |
| explicit billing/credits error | FAIL_BILLING_OR_CREDITS |
| HTTP 404/model missing | FAIL_MODEL_NOT_FOUND |
| HTTP 429 | FAIL_RATE_LIMIT |
| timeout | FAIL_TIMEOUT |
| 2xx malformed or missing review text | FAIL_RESPONSE_SCHEMA |
| other non-2xx | FAIL_PROVIDER_HTTP |
| network IO | FAIL_PROVIDER_IO |
| uncategorized failure | FAIL_UNEXPECTED |

Every result includes PRODUCTION_READINESS: BLOCKED.

## Secret Safety

- *.local-secret remains ignored.
- No local secret file is read, sourced, copied, modified, or tracked.
- Key presence is checked only after all non-secret gates pass.
- Tests use test-openai-key, test-gemini-key, and test-xai-key.
- No command example puts a key in shell history.
- Raw provider error content is never returned or logged.

## Scheduler Safety

The shell forces global, Push Recheck, Position Sync, Position Monitor, Market Data, OHLCV ingestion, Watchlist, Analysis, and Provider Scan scheduler switches to false. The harness does not start Spring, so no scheduler bean is created.

## Database Safety

The harness has no datasource, mapper, service, repository, or persistence dependency. It does not connect H2 or PostgreSQL and does not write AiCallLog or any business record.

## Trading Safety Boundary

The fixed request is schema-only, contains no real market or user data, and asks for no direction, entry, stop, target, leverage, position, order, or execution. Existing boundaries remain true:

- reviewOnly
- manualReviewOnly
- notTradeInstruction
- notExecutable
- notAutoTrading
- notOrderExecution
- notUserPositionCreation
- notPositionMutation
- notStateMachineOverride
- ruleDirectionPreserved

## Local Execution Procedure

The default validation is:

    bash scripts/ai-provider-controlled-smoke.sh

It must return SKIPPED_EXTERNAL_CALLS_DISABLED, LIVE_PROVIDER_CALLS: 0, and REAL_KEYS_READ: 0.

For a later operator-authorized single-provider run, the selected key must already be present in the operator shell. Do not enter a key in a command, document, Codex, or shell history. Set only the non-secret gates for exactly one target, then run the same script. The harness never sources a secret file.

## What PASS Proves

A single provider PASS proves only current-key authentication, selected-model account/region availability, endpoint compatibility, parser compatibility, and presence-only token/request trace reporting.

## What PASS Does Not Prove

It does not prove all providers work, sustained availability, cost safety, scheduler safety in a running application, decision quality, direction accuracy, profitability, or production readiness.

## Test Evidence

Fake transport tests cover all three contracts, GPT-5.6 fast/deep routing, GPT-5.5 and GPT-5.4 fallbacks, MODEL_UNAVAILABLE exhaustion, fallback audit metadata, GPT-4 rejection, model-readiness states, malformed-model fail-closed behavior, Gemini retirement avoidance, xAI Responses usage, controlled-smoke one-request enforcement, gate skips, invalid targets, 401, 403, billing, 404, 429, timeout, IO, malformed JSON, missing text, missing usage, missing request ID, redaction, default script behavior, scheduler switches, default-disabled configuration, rule-direction preservation, record-creation boundaries, and Dashboard role labels.

No fake transport test is live-provider evidence.

## Production Readiness

BLOCKED. This package creates a safe evidence tool but records no real provider result. Production deployment cannot proceed based on this package.
