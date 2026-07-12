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
- Gemini Interactions API: `POST https://generativelanguage.googleapis.com/v1/interactions`
- Gemini structured outputs: <https://ai.google.dev/gemini-api/docs/structured-output?lang=rest>
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
| Gemini | `gemini-3.5-flash` Interactions accepts canonical `models/...` names and JSON Schema response format | generateContent availability and extraction mismatch | Use one stable-v1 Interactions adapter with final-step extraction and strict V1 validation |
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
| GEMINI_REVIEW | gemini-3.5-flash | BALANCED | Consistency review through the stable Interactions structured-output contract |
| GROK_CHALLENGE | grok-4.5 | ADVERSARIAL_CHALLENGE | Contradiction detection and counter-challenge generation |

The configuration keys are trade-model.ai.model-strategy.gpt-final.priority, gemini-review.priority, and grok-challenge.priority. Provider status exposes configuredModel, effectiveModel, fallbackUsed, and fallbackReason without exposing API keys.

Model readiness is separate from credential presence:

- MODEL_CONFIGURED: an approved model route is selected, but no successful provider response has verified it in this process; `ready=false` and the reason is `MODEL_AVAILABILITY_UNVERIFIED`.
- MODEL_ACTIVE: the configured primary model has returned a successful contract-valid response in this process; `ready=true`, the reason is `MODEL_CALL_VERIFIED`, and `MODEL_AVAILABILITY_UNVERIFIED` is absent.
- MODEL_FALLBACK_ACTIVE: an approved GPT-5.5 or GPT-5.4 fallback was selected and its full response contract passed; `ready=true`, `fallbackUsed=true`, and the explicit fallback reason is retained.
- MODEL_UNAVAILABLE: configuration is invalid or no approved model remains available; `ready=false`.

The status endpoint does not report ready=true or MODEL_ACTIVE merely because an API key and model are configured. Missing or malformed model selection fails closed.

Readiness verification is deliberately process-local and is not persisted. After application restart, a configured provider starts again at `MODEL_CONFIGURED`; it can become `MODEL_ACTIVE` only after a new HTTP, extraction, JSON, and strict V1 role-contract success. No key, raw output, prompt, response body, or readiness secret is persisted.

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

### Canonical implementation

`GEMINI_REVIEW` now uses `gemini-3.5-flash`, canonicalized in the outgoing request as `models/gemini-3.5-flash`, and sends exactly one `POST /v1/interactions` request. The request sets `store=false`, `stream=false`, a bounded 256-token output, temperature 0, seed 42, low thinking, no thinking summary, no tools, and a JSON Schema response format for the four V1 role fields. There is no automatic generateContent fallback.

The production adapter parses only terminal `status=completed` Interaction resources. It selects the last `model_output` step, concatenates only nonblank `type=text` items inside that final step, and ignores earlier model-output text. Failed, cancelled, incomplete, missing-step, missing-text, malformed-JSON, and schema-invalid results fail closed. It never reads thought text, concatenates earlier outputs, strips prose, extracts JSON with regex, repairs output, or invents fields.

Interaction `id` maps directly to `providerRequestId`; missing IDs remain absent and add `GEMINI_INTERACTION_ID_MISSING`. Usage maps from `total_input_tokens`, `total_output_tokens`, and `total_tokens`. Raw payloads, output text, prompts, headers, and keys are never exposed.

The deterministic Gemini normalizer remains a final security boundary before the strict common V1 parser. Its expected normal path is identity normalization. It does not accept Markdown or natural language, repair malformed JSON, infer stance, fill missing values, or silently remove unknown/trading fields.

### Real evidence and remaining limitation

The operator evidence showed that `gemini-2.5-pro` was visible in model listing and declared generateContent support, but the minimal real generateContent request returned HTTP 404; it is therefore not the selected default. `gemini-3.5-flash` Interactions returned real HTTP 200 with text and usage. One external probe passed the V1 JSON contract and one did not, so repeatability remains unproven. This package implements the canonical request/extraction discrepancy offline and makes no live provider call. Gemini is not production-ready and overall production readiness remains BLOCKED.

The controlled Gemini smoke uses the exact production Interactions client, canonical model mapping, request builder, final-step extractor, normalizer, and strict parser. It has no second one-off response parser. The sanitized request diagnostic exposes only model, MIME type, schema presence, token limit, temperature, instruction/input lengths, stop-sequence presence, and tools presence.

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

Legacy Gemini diagnostic labels do not create alternate request or parser paths; any explicitly enabled Gemini smoke still uses the canonical production Interactions adapter and all existing external-call gates.

## One-Provider-One-Request Rule

- One provider target
- At most one HTTP POST
- Core runtime requires `AI_PROVIDER_SMOKE_HARNESS_ENTRY=I_CONFIRM_SINGLE_PROVIDER_SMOKE` before reading the selected key, constructing the provider client, or invoking transport
- No retry, loop, concurrency, fallback provider, or multi-role orchestration
- Controlled-smoke request and overall timeout: 30 seconds for Gemini structured-output validation; 15 seconds for OpenAI and xAI
- Production AI request timeout remains unchanged at 5 seconds
- Script watchdog: 60 seconds including Maven harness startup
- Maximum output: 128 tokens for OpenAI/xAI controlled smoke and 256 tokens for Gemini Interactions

## Sanitized Output Contract

Allowed output fields are:

    AI_PROVIDER:
    AI_MODEL:
    AI_AUTH_STATUS:
    AI_HTTP_STATUS_CLASS:
    AI_ERROR_CATEGORY:
    AI_PROVIDER_ERROR_REASON:
    AI_RESPONSE_PARSE_STATUS:
    AI_TOKEN_USAGE_PRESENT:
    AI_REQUEST_ID_PRESENT:
    AI_TIMEOUT_LIMIT_MS:
    AI_LATENCY_MS:
    AI_PROVIDER_LIVE_SMOKE:
    LIVE_PROVIDER_CALLS:
    REAL_KEYS_READ:
    PRODUCTION_READINESS:

The output never includes a key, key shape, authorization header, request body, prompt, raw response body, raw error body, raw headers, complete request ID, or provider summary. The shell maintains a mode-`600` temporary counter containing only `0` or `1`; it is deleted on exit. Process failures report the marker value, or `UNKNOWN_MAX_1` when the marker cannot be trusted, rather than silently claiming zero calls.

Authorized Gemini diagnostic mode emits only:

    AI_PROVIDER:
    AI_DIAGNOSTIC_MODE:
    AI_HTTP_STATUS_CLASS:
    AI_ERROR_CATEGORY:
    AI_RESPONSE_PARSE_STATUS:
    AI_LATENCY_MS:
    AI_PROVIDER_LIVE_SMOKE:
    LIVE_PROVIDER_CALLS:
    PRODUCTION_READINESS:

When a Gemini 2xx response fails the strict role-result parser, the harness may additionally emit `GEMINI_SCHEMA_DIAGNOSTIC`, `EXPECTED_FIELDS`, `ACTUAL_FIELDS`, `MISSING_FIELDS`, `UNEXPECTED_FIELDS`, and `TYPE_MISMATCH_FIELDS`. These lines contain only allowlisted or sanitized field names and JSON type names. They never retain or print field values, candidate text, the raw response, the prompt, headers, or credentials.

The diagnostic compares the candidate object with the exact V1 role fragment: required string fields `stance`, `conflictLevel`, and `summary`, plus required `reasonCodes` as an array of strings. Unknown fields, missing fields, wrong types, Markdown wrappers, and natural-language wrappers remain hard failures. The parser performs no extraction from prose and no automatic repair.

### Gemini provider normalization layer

Gemini candidate text passes through `GeminiRoleResultNormalizer` before the common V1 role-result parser. The normalizer accepts a direct role fragment or one deterministic single-object wrapper named `result` or `analysis`. It also maps only the explicit field aliases `conflict_level` to `conflictLevel` and `reason_codes` to `reasonCodes`; values are never inferred or filled.

Successful normalization emits only `stance`, `conflictLevel`, `reasonCodes`, and `summary`, then the unchanged common parser validates required fields, types, enum values, forbidden fields, and forbidden instruction text. Missing fields, wrong types, duplicate aliases, extra fields, unsafe trading fields, Markdown, natural-language wrappers, multiple JSON values, and malformed JSON fail closed. Unknown fields are never silently discarded, and no response repair or intent guessing is performed. OpenAI and xAI response paths are unchanged.

When JSON MIME output fails normalization or strict V1 validation, the controlled smoke emits only `GEMINI_SCHEMA_DIAGNOSTIC_STATUS`, `GEMINI_EXPECTED_FIELDS`, `GEMINI_ACTUAL_FIELDS`, `GEMINI_MISSING_FIELDS`, `GEMINI_UNEXPECTED_FIELDS`, and `GEMINI_TYPE_MISMATCH`. Lists contain sanitized field names or type relationships only. The raw candidate, field values, summary, reasoning, prompt, request body, headers, request ID, and API key are never retained in the diagnostic or printed. Successful Gemini responses emit none of these fields. The evidence distinguishes a deterministic alias gap from an unsupported wrapper or unsafe semantic mismatch, but it never changes mappings or repairs output automatically.

Schema evidence remains sanitized when the final Interactions output reaches the strict parser and fails. Only field names and type relationships may pass from `GeminiProviderClient` through `AiProviderReviewResult`, `AiProviderControlledSmokeResult`, and the shell allowlist. Interaction steps and text values are never emitted.

### Gemini Interactions contract failure diagnostics

Gemini Interactions failures preserve an explicit allowlisted reason instead of collapsing every terminal-state or extraction failure into `PROVIDER_RESPONSE_SCHEMA`. Supported reasons distinguish `in_progress`, `requires_action`, `failed`, `cancelled`, `incomplete`, missing status, missing final `model_output`, missing final text, invalid final JSON, strict V1 role-contract failure, and an invalid response envelope. A genuinely malformed provider JSON envelope still follows the generic `PROVIDER_RESPONSE_SCHEMA` fallback.

The controlled smoke may expose only the sanitized interaction status, interaction-ID presence, usage/token-field presence, step and model-output counts, final-output presence, final text-block count and length, final JSON parse status, V1 contract status, and the allowlisted failure reason. It never stores or emits the interaction ID, token values, final text, response body, prompt, request body, headers, API key, or generated field values. Successful interactions do not emit failure diagnostic lines. The typed failure is caught before the generic Jackson failure path, while parsing and V1 validation remain fail closed with no retry or response repair.

Both the normal and explicitly authorized diagnostic shell modes use exact-name `awk` allowlists for these interaction fields. Broad `GEMINI_.*` or interaction-wide patterns are not accepted. An offline synthetic-output test runs each real shell filter and proves that all approved interaction diagnostics survive while raw response, generated text, interaction-ID value, token-count value, authorization, and prompt lines are removed. A second regression test derives the emitted `GEMINI_INTERACTION_*` field names from the Java result and requires both shell allowlists to contain every emitted name.

`AI_HTTP_STATUS_CLASS` reports `TIMEOUT` when no HTTP response arrives because the request timed out. Otherwise it reports `1XX` through `5XX` for an HTTP response, or `NOT_AVAILABLE` when no status exists for another reason. `AI_ERROR_CATEGORY` is blank for success/skip and otherwise is one of `TIMEOUT`, `AUTH`, `MODEL_NOT_FOUND`, `RATE_LIMIT`, `PROVIDER_ERROR`, or `RESPONSE_SCHEMA`.

For Gemini non-2xx responses, the controlled smoke uses narrower categories: `INVALID_REQUEST`, `SCHEMA_UNSUPPORTED`, `MODEL_CAPABILITY_ERROR`, `AUTH`, `RATE_LIMIT`, `PROVIDER_INTERNAL_ERROR`, or `UNKNOWN_PROVIDER_ERROR`. `AI_PROVIDER_ERROR_REASON` is an allowlisted enum such as `GEMINI_HTTP_400_INVALID_REQUEST`, `GEMINI_STRUCTURED_OUTPUT_UNSUPPORTED`, or `GEMINI_HTTP_5XX_INTERNAL`. The classifier may inspect the standard error status/message in memory for a 400 response, but it never retains or emits that text. Raw response bodies, prompts, headers, request IDs, and credentials remain excluded.

The offline `GeminiProviderStructuredOutputContractTest` verifies the canonical model name, Interactions request schema, final-step-only extraction, multi-block final text, terminal status handling, ID/usage mapping, strict normalization, readiness transition, and absence of generateContent fallback. It uses fake transport only and makes no network call.

### Future repeatability evidence

No live Gemini request is run by this package. Any later operator-authorized repeatability check must use the same production Interactions adapter, one request per execution, no retry, and the sanitized smoke output. A single PASS does not prove repeatability or production readiness.

## Failure Classification

| Condition | Result |
|---|---|
| external gate closed | SKIPPED_EXTERNAL_CALLS_DISABLED |
| core harness confirmation absent or invalid | SKIPPED_HARNESS_ENTRY_MISSING |
| key absent | SKIPPED_MISSING_API_KEY |
| global or provider switch disabled | SKIPPED_PROVIDER_DISABLED |
| target not exactly allowlisted | FAIL_INVALID_TARGET |
| HTTP 401/403 | FAIL_AUTH |
| explicit billing/credits error, including an HTTP 429 quota/credit body | FAIL_BILLING_OR_CREDITS |
| HTTP 404/model missing | FAIL_MODEL_NOT_FOUND |
| remaining HTTP 429 without billing/quota/credit semantics | FAIL_RATE_LIMIT |
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
- The exact core harness-entry confirmation is checked before selected-key lookup and before transport construction.
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

For a later operator-authorized single-provider run, the selected key must already be present in the operator shell. Do not enter a key in a command, document, Codex, or shell history. Set only the non-secret gates for exactly one target, including `AI_PROVIDER_SMOKE_HARNESS_ENTRY=I_CONFIRM_SINGLE_PROVIDER_SMOKE`, then run the same script. The harness never sources a secret file.

For a later operator-authorized Gemini repeatability check, keep the existing key in the shell without displaying it and enable the existing global/Gemini/external-call gates for exactly one run. The script uses the production Interactions adapter, performs at most one request, has no retry or legacy fallback, and does not source or display the key.

## What PASS Proves

A single provider PASS proves only current-key authentication, selected-model account/region availability, endpoint compatibility, parser compatibility, and presence-only token/request trace reporting.

## What PASS Does Not Prove

It does not prove all providers work, sustained availability, cost safety, scheduler safety in a running application, decision quality, direction accuracy, profitability, or production readiness.

## Test Evidence

Fake transport tests cover all three contracts, GPT-5.6 fast/deep routing, GPT-5.5 and GPT-5.4 fallbacks, MODEL_UNAVAILABLE exhaustion, fallback audit metadata, GPT-4 rejection, model-readiness states, malformed-model fail-closed behavior, Gemini retirement avoidance, xAI Responses usage, controlled-smoke one-request enforcement, gate skips, invalid targets, 401, 403, billing, 404, 429, timeout, IO, malformed JSON, missing text, missing usage, missing request ID, redaction, default script behavior, scheduler switches, default-disabled configuration, rule-direction preservation, record-creation boundaries, and Dashboard role labels.

No fake transport test is live-provider evidence.

## Production Readiness

BLOCKED. This package creates a safe evidence tool but records no real provider result. Production deployment cannot proceed based on this package.
