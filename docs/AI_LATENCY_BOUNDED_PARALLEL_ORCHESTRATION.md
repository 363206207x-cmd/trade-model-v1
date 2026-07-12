# AI Latency Bounded Parallel Orchestration

## Scope

AI-LATENCY-1 replaces sequential review-provider calls with one bounded parallel checkpoint. It does not change Rule Engine direction authority, create an executable plan, mutate a position, place an order, or send Push/Telegram messages.

Production readiness remains **BLOCKED**.

## Real Latency Evidence

Controlled operator evidence available before this package:

| Provider role | Model | Observed latency |
|---|---|---:|
| OpenAI GPT_FINAL | `gpt-5.6-luna` | approximately 4.6s |
| Gemini GEMINI_REVIEW | `gemini-3.5-flash` | 13.416s, 13.771s, 23.274s |
| xAI GROK_CHALLENGE | `grok-4.5` | approximately 5.4s |

Mean observed Gemini latency was approximately 16.820s. These observations are controlled evidence, not production SLO evidence.

## Why Sequential Orchestration Failed

The previous implementation called providers one after another under a 5-second request timeout and an 8-second overall timeout. A slow first or second provider consumed the remaining deadline, so later completed providers could be skipped and Gemini could not normally finish within its observed latency range.

The old `request-timeout-ms` and `overall-timeout-ms` properties remain available for compatibility with existing clients and status consumers. The bounded orchestrator uses the new provider-specific contract below.

## Provider-Specific Timeout Contract

```yaml
trade-model:
  ai:
    provider-timeouts:
      openai-ms: ${TRADE_MODEL_AI_OPENAI_TIMEOUT_MS:10000}
      gemini-ms: ${TRADE_MODEL_AI_GEMINI_TIMEOUT_MS:25000}
      xai-ms: ${TRADE_MODEL_AI_XAI_TIMEOUT_MS:10000}
      overall-ms: ${TRADE_MODEL_AI_OVERALL_TIMEOUT_MS:30000}
```

Provider timeouts must be between 1,000 and 30,000 milliseconds. The overall deadline must be between 5,000 and 60,000 milliseconds. A provider timeout may not exceed the overall deadline. Invalid overall configuration fails every provider closed with `ORCHESTRATOR_TIMEOUT_CONFIG_INVALID`; an invalid provider setting fails that provider closed with `PROVIDER_TIMEOUT_CONFIG_INVALID`. No invalid value becomes an unlimited wait.

## Executor Bounds

`AiProviderExecutor` is a single application-managed executor with:

- fixed worker count: 3
- bounded queue capacity: 3
- abort/reject policy that fails closed
- traceable task thread names: `ai-provider-openai`, `ai-provider-gemini`, and `ai-provider-xai`
- explicit `@PreDestroy` shutdown and interrupt handling

It does not use the common ForkJoinPool, a cached pool, an unbounded queue, or a new pool per checkpoint.

## Global Deadline and Cancellation

UsageGuard and call-log start run before a provider task is submitted. Eligible providers are submitted without waiting for an earlier provider response. The orchestrator consumes completed futures by completion order but stores accepted results by role.

At a provider deadline, only that task is cancelled and marked `PROVIDER_TIMEOUT`. At the global deadline, all unfinished tasks are cancelled and marked `ORCHESTRATOR_OVERALL_TIMEOUT`. Cancellation uses interruption and never causes a retry. Each provider client receives its own timeout and is called at most once.

Each submitted invocation has a synchronized terminal-state gate. The first accepted completion or timeout closes its call log exactly once. A late provider response cannot replace the terminal timeout result or complete the same log again.

## Deterministic Result Ordering

Completion time does not control API order. Final `providerResults` always use:

1. `GPT_RULE_REVIEW`
2. `GEMINI_CONSISTENCY_REVIEW`
3. `GROK_ADVERSARIAL_CHALLENGE`

## Partial Fallback

- all eligible providers succeed: `AI_ASSISTED`
- one or two providers succeed: `PARTIAL_FALLBACK`
- no provider succeeds: `RULE_ONLY_FALLBACK`

A failed or timed-out AI review does not change the Rule Engine direction. All review-only, manual-review-only, non-executable, non-trading, non-order, non-position-mutation, and rule-direction-preserved flags remain true.

## Call-Log Consistency

Every submitted provider has its own call ID and provider/model/analysis/trace metadata. `startCall` occurs before submission and `completeCall` is terminal-gated. Guard-blocked calls use the existing skipped-log path. A call-log error is isolated to that provider and does not cancel other provider tasks.

## Sanitized Checkpoint Metrics

`AiOrchestratorResult` exposes only aggregate metadata:

- `orchestrationStartedAt`
- `orchestrationCompletedAt`
- `orchestrationLatencyMs`
- `providerSubmittedCount`
- `providerCompletedCount`
- `providerTimeoutCount`
- `providerFailedCount`
- `providerSuccessCount`
- `globalDeadlineExceeded`
- `partialFallbackUsed`

No API key, Authorization header, prompt, raw response, or complete generated output is added to these metrics.

## What This Package Proves

Offline fake-provider tests prove bounded parallel submission, provider-specific timeout propagation, global cancellation, deterministic ordering, partial fallback, timeout isolation, late-result rejection, one-call/no-retry behavior, call-log terminal gating, bounded queue configuration, clean executor shutdown, disabled zero-call behavior, and safety flags.

## What This Package Does Not Prove

This package does not call real providers and does not prove real parallel-provider stability, production latency, cost-budget suitability, sustained load behavior, or end-to-end three-role decision quality. It does not start schedulers or access a production database.

## Production Readiness

**BLOCKED.** Real bounded-parallel operation, cost controls under concurrency, long-running stability, and full three-role business E2E evidence remain incomplete. Production deployment cannot proceed based on this package.
