# AI-E2E-1 Controlled Parallel Live Harness

## Purpose

This package adds an operator-authorized, review-only harness for the production
`AiDecisionOrchestratorService`. It exercises the dedicated bounded executor and the OpenAI,
Gemini, and xAI adapters in one checkpoint without changing Rule Engine direction authority.

The harness is evidence tooling, not a scheduler or deployment gate. Production readiness remains
`BLOCKED`.

## Default Behavior

Running the script without explicit authorization performs no network request:

```bash
bash scripts/ai-parallel-orchestrator-controlled-smoke.sh
```

Expected evidence:

```text
AI_PARALLEL_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED
LIVE_PROVIDER_CALLS: 0
REAL_KEYS_READ: 0
PRODUCTION_READINESS: BLOCKED
```

## Live Gates

An operator must provide all of the following through the local process environment:

- `AI_PARALLEL_SMOKE_ENABLE_EXTERNAL_CALLS=true`
- `AI_PARALLEL_SMOKE_HARNESS_ENTRY=I_CONFIRM_THREE_PROVIDER_PARALLEL_SMOKE`
- global AI and all three provider enable flags set to `true`
- non-empty `OPENAI_API_KEY`, `GEMINI_API_KEY`, and `XAI_API_KEY`

Do not commit or print those values. The script does not source a secret file.

The live path disables all business schedulers, overrides inherited Spring profile and datasource
settings with an isolated in-memory H2 database, disables the OpenAI internal model fallback for
call-count clarity, and preserves the merged timeout contract:
OpenAI 10 seconds, Gemini 25 seconds, xAI 10 seconds, and an overall 30-second deadline.

## Call Audit

The script creates a mode-600 temporary marker. A test-only transport decorator records each
provider immediately before its formal transport call and rejects a second attempt. A normal run
reports `0` or `1` per provider. A failed or watchdog-terminated process reports
`UNKNOWN_MAX_1`; it never converts an uncertain run into a false zero-call result.

## Fixed Fixture

The input is a deterministic `BTCUSDT` `15m` review fixture with a `BULLISH` rule bias, medium
confidence and risk, `worthOpening=false`, and non-empty evidence. It contains no entry, stop,
take-profit, leverage, order, or position instruction.

## Safety Boundary

- maximum one request per provider and three total
- no retry, parallel fallback, scheduler, Push, or Telegram send
- no `UserPosition`, `ExecutionPlan`, or order creation
- no production database connection
- output contains status classes, parse status, counts, and timing only
- no key, prompt, header, raw response, model answer, or interaction identifier is printed
- production readiness remains `BLOCKED`
