# OpenAI GPT-5 Model Routing Contract

## Scope

- Package: AI-1.1
- Verification date: 2026-07-11
- Role: `GPT_FINAL`
- Live provider calls: 0
- Real API keys read: 0
- Production readiness: BLOCKED

This document verifies the configured model identifiers, Responses API mapping, review-only JSON contract, and bounded GPT-5 fallback route. It does not prove account entitlement or live model availability.

## Official Sources

Only first-party OpenAI documentation was used:

- Model catalog: <https://developers.openai.com/api/docs/models>
- GPT-5.6 Luna: <https://developers.openai.com/api/docs/models/gpt-5.6-luna>
- GPT-5.6 Sol: <https://developers.openai.com/api/docs/models/gpt-5.6-sol>
- GPT-5.5: <https://developers.openai.com/api/docs/models/gpt-5.5>
- GPT-5.4: <https://developers.openai.com/api/docs/models/gpt-5.4>
- Responses create contract: <https://developers.openai.com/api/reference/resources/responses/methods/create>
- Structured Outputs: <https://developers.openai.com/api/docs/guides/structured-outputs>
- Reasoning controls: <https://developers.openai.com/api/docs/guides/reasoning>
- Request identifiers: <https://developers.openai.com/api/reference/overview#debugging-requests>

## Official Model Contract Audit

| Configured ID | Exact official API ID | Availability/status | Responses API | Structured Outputs | Usage and request ID contract | Result |
|---|---|---|---|---|---|---|
| `gpt-5.6-luna` | Yes | GPT-5.6 catalog availability is preview for select trusted partners; model page is not marked deprecated | Supported | Supported | Responses exposes usage fields; HTTP exposes `x-request-id` | VERIFIED_PREVIEW_ACCESS_REQUIRED |
| `gpt-5.6-sol` | Yes | GPT-5.6 catalog availability is preview for select trusted partners; model page is not marked deprecated | Supported | Supported | Responses exposes usage fields; HTTP exposes `x-request-id` | VERIFIED_PREVIEW_ACCESS_REQUIRED |
| `gpt-5.5` | Yes | Official default model page; not marked preview or deprecated | Supported | Supported | Responses exposes usage fields; HTTP exposes `x-request-id` | VERIFIED |
| `gpt-5.4` | Yes | Official default model page; not marked preview or deprecated | Supported | Supported | Responses exposes usage fields; HTTP exposes `x-request-id` | VERIFIED |

No configured ID is `MODEL_ID_UNVERIFIED`, so defaults remain unchanged. Exact model publication does not prove that the operator account is entitled to GPT-5.6 preview access. Until a successful controlled response occurs, readiness remains `MODEL_CONFIGURED` rather than `MODEL_ACTIVE`.

## GPT_FINAL Routing

### FAST_DECISION_MODEL

- Purpose: normal candidate, waiting-trigger, execution-plan, and position-logic review checkpoints.
- Priority: latency before maximum reasoning depth.
- Approved model: `gpt-5.6-luna`.
- Contract status: official Responses and Structured Outputs support; account availability remains unverified without a live call.

### DEEP_REASONING_MODEL

- Purpose: AI conflict, confused state, Hot Reset, extreme events, high risk, multi-timeframe contradiction, and rule/evidence conflict.
- Priority: strongest reasoning in the configured GPT-5.6 route.
- Approved model: `gpt-5.6-sol`.
- Contract status: official Responses and Structured Outputs support; account availability remains unverified without a live call.

### Fallback chain

| Level | Model | Outcome |
|---|---|---|
| 0 | configured GPT-5.6 fast or reasoning model | Primary route |
| 1 | `gpt-5.5` | `OPENAI_FALLBACK_GPT55` |
| 2 | `gpt-5.4` | `OPENAI_FALLBACK_GPT54` |
| 3 | none | `MODEL_UNAVAILABLE` / `OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE` |

There is no GPT-4, GPT-4.1-mini, GPT-4o, or older-family fallback. Authentication, billing, and rate-limit failures do not silently advance the model chain.

Every fallback result records `originalModel`, `selectedModel`, `fallbackLevel`, `fallbackReason`, timestamp, and `traceId`.

## Configuration And Readiness Contract

The role-aware properties are:

- `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL`
- `TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL`
- `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT55_MODEL`
- `TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT54_MODEL`

Provider status exposes `configuredModel`, `effectiveModel`, `modelStrategy`, `fallbackUsed`, and `fallbackReason`. It never exposes the API key.

| State | Meaning |
|---|---|
| `MODEL_CONFIGURED` | Route configuration is valid, but no successful provider response has verified the selected model in this process |
| `MODEL_ACTIVE` | The primary model returned a successful contract-valid response in this process |
| `MODEL_FALLBACK_ACTIVE` | An approved fallback returned a successful contract-valid response |
| `MODEL_UNAVAILABLE` | Configuration is invalid or all acceptable models are exhausted |

An API key or model name by itself does not produce `MODEL_ACTIVE` and does not make `ready=true`.

## Responses API Contract

The OpenAI client sends:

- `model`
- `instructions`
- `input`
- `max_output_tokens`
- `reasoning.effort` for GPT-5 models

The client reads:

- top-level `output_text`
- `output[].content[].text` as the documented content fallback
- `usage.input_tokens`
- `usage.output_tokens`
- `usage.total_tokens`
- `x-request-id`, with the response body `id` as a presence fallback

OpenAI documents `x-request-id` as the server-generated request identifier and `X-Client-Request-Id` as an optional caller-generated trace header. V1 currently records the server ID when returned and preserves its own routing `traceId` separately.

All four configured models officially support Structured Outputs. The current V1 HTTP client does not claim provider-side strict JSON Schema enforcement because it does not send `text.format`. Instead, it requires a JSON-only review response and enforces the allowlisted schema in `AiProviderResponseParser`. Enabling `text.format` is a separate contract enhancement and must retain the same fail-closed parser.

## Parser Fail-Closed Rules

The parser rejects:

- missing or blank response text
- malformed JSON or non-object JSON
- unknown fields outside `stance`, `conflictLevel`, `reasonCodes`, and `summary`
- direction or state-machine override fields
- execution, order, position, push, or provider-payload fields
- prompt-injection text
- order submission/creation text
- user-position creation text

Rejected responses become `INVALID_RESPONSE`; they cannot create an execution plan, position, order, state transition, external push, or Telegram message.

## Verification Status

- Exact model IDs: VERIFIED
- GPT-5.6 account entitlement: MODEL_AVAILABLE_UNKNOWN / preview access not live-tested
- Responses request mapping: VERIFIED_BY_OFFLINE_TEST
- Output, usage, and request-ID mapping: VERIFIED_BY_OFFLINE_TEST
- Parser safety: VERIFIED_BY_OFFLINE_TEST
- GPT-4 fallback: ABSENT
- Live availability: NOT_TESTED
- Default smoke: `SKIPPED_EXTERNAL_CALLS_DISABLED`, `LIVE_PROVIDER_CALLS: 0`, `REAL_KEYS_READ: 0`
- Production readiness: BLOCKED
