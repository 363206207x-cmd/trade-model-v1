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
- OpenAI GPT-5.6 guide: <https://developers.openai.com/api/docs/guides/latest-model>
- OpenAI GPT-4.1 mini compatibility fallback: <https://developers.openai.com/api/docs/models/gpt-4.1-mini>
- OpenAI Responses create: <https://developers.openai.com/api/reference/resources/responses/methods/create>
- OpenAI API errors: <https://developers.openai.com/api/docs/guides/error-codes>
- OpenAI request IDs: <https://platform.openai.com/docs/api-reference/debugging-requests>
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
| OpenAI | gpt-5.6-sol is the current flagship for complex reasoning; GPT-4.1 mini remains a compatibility option | Quality gap | Change GPT_FINAL default to gpt-5.6-sol; retain explicit, visible compatibility fallback only |
| Gemini | Gemini 1.5 Flash was shut down; gemini-3.5-flash is stable GA | Previous default unusable | Change default model; retain generateContent mapping |
| xAI | grok-4.5 supports Responses and Chat; xAI recommends Responses and labels Chat deprecated | Endpoint deprecation risk | Change default model and migrate to POST /v1/responses |

## OpenAI Decision

### Current implementation

The previous default used gpt-4.1-mini with POST /v1/responses, Bearer authentication, model, instructions, input, max_output_tokens, and temperature.

### Official current contract

The current OpenAI model catalog identifies gpt-5.6-sol, with alias gpt-5.6, as the flagship model for complex reasoning and coding. It supports Responses and configurable reasoning effort through max. GPT-4.1 mini remains available as a compatibility fallback, but it is no longer the quality-first default for final adjudication.

### Required change

GPT_FINAL defaults to gpt-5.6-sol through TRADE_MODEL_AI_OPENAI_MODEL. The client keeps POST /v1/responses and uses high reasoning effort for GPT-5 family models. Request-ID mapping prefers x-request-id, with response id as a presence fallback.

### No change reason

An operator may activate the gpt-4.1-mini compatibility fallback only after primary-model unavailability is established by setting TRADE_MODEL_AI_OPENAI_COMPATIBILITY_FALLBACK_ACTIVE=true. The configured model remains visible, the effective model changes explicitly, fallbackUsed becomes true, and fallbackReason is OPENAI_MODEL_FALLBACK_COMPATIBILITY. No automatic retry or silent downgrade occurs.

## AI Role Model Strategy

AI is checkpoint-driven rather than a high-frequency chat surface. Model selection therefore follows role responsibility instead of applying one low-cost assumption to every provider.

| Role | Provider default | Priority | Responsibility |
|---|---|---|---|
| GPT_FINAL | gpt-5.6-sol | QUALITY_FIRST | Final adjudication, instruction following, and conflict resolution |
| GEMINI_REVIEW | gemini-3.5-flash | BALANCED | Low-latency consistency review and structured-output checking |
| GROK_CHALLENGE | grok-4.5 | ADVERSARIAL_CHALLENGE | Contradiction detection and counter-challenge generation |

The configuration keys are trade-model.ai.model-strategy.gpt-final.priority, gemini-review.priority, and grok-challenge.priority. Provider status exposes configuredModel, effectiveModel, fallbackUsed, and fallbackReason without exposing API keys.

Model readiness is separate from credential presence:

- MODEL_CONFIGURED: a model is selected while the provider remains disabled.
- MODEL_AVAILABLE_UNKNOWN: provider and model configuration exist, but no live availability proof exists.
- MODEL_FALLBACK_ACTIVE: the explicit OpenAI compatibility fallback is active.
- MODEL_NOT_CONFIGURED: the primary model is blank or the fallback selection is invalid.

The status endpoint does not report ready=true merely because an API key and model are configured. Missing or malformed model selection fails closed.

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

It must return SKIPPED_EXTERNAL_CALLS_DISABLED and LIVE_PROVIDER_CALLS: 0.

For a later operator-authorized single-provider run, the selected key must already be present in the operator shell. Do not enter a key in a command, document, Codex, or shell history. Set only the non-secret gates for exactly one target, then run the same script. The harness never sources a secret file.

## What PASS Proves

A single provider PASS proves only current-key authentication, selected-model account/region availability, endpoint compatibility, parser compatibility, and presence-only token/request trace reporting.

## What PASS Does Not Prove

It does not prove all providers work, sustained availability, cost safety, scheduler safety in a running application, decision quality, direction accuracy, profitability, or production readiness.

## Test Evidence

Fake transport tests cover all three contracts, the quality-first GPT default, explicit compatibility fallback and reason code, model-readiness states, missing-model fail-closed behavior, Gemini retirement avoidance, xAI Responses usage, one-request enforcement, no retry, gate skips, invalid targets, 401, 403, billing, 404, 429, timeout, IO, malformed JSON, missing text, missing usage, missing request ID, redaction, default script behavior, scheduler switches, default-disabled configuration, rule-direction preservation, record-creation boundaries, and Dashboard role labels.

No fake transport test is live-provider evidence.

## Production Readiness

BLOCKED. This package creates a safe evidence tool but records no real provider result. Production deployment cannot proceed based on this package.
