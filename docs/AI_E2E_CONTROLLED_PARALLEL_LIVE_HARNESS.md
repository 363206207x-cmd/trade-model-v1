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

The isolated datasource uses the same H2 MySQL compatibility mode as the authoritative local
schema contract:

```text
jdbc:h2:mem:ai_parallel_controlled_smoke;DB_CLOSE_DELAY=-1;MODE=MySQL
```

SQL initialization remains `always`; the harness does not bypass `schema.sql` or connect to the
normal application database.

## Call Audit

The script creates a mode-600 temporary marker. A test-only transport decorator records each
provider immediately before its formal transport call and rejects a second attempt. A normal run
reports `0` or `1` per provider. A failed or watchdog-terminated process reports
the exact readable values from that marker. Only a missing, malformed, or unreadable provider value
becomes `UNKNOWN_MAX_1`; any aggregate containing an unknown value becomes `UNKNOWN_MAX_3`.

## Failure Diagnostics

Two additional mode-600 markers record the last allowlisted harness stage and whether the watchdog
actually terminated Maven. Failure output includes only:

- a fixed failure category such as `SPRING_CONTEXT_FAILURE`, `DATABASE_INITIALIZATION_FAILURE`,
  `CALL_COUNT_AUDIT_FAILURE`, or `WATCHDOG_TIMEOUT`
- process state `SUCCESS`, `FAILURE`, or `WATCHDOG`
- an allowlisted stage from `PRECHECK` through `OUTPUT_EMITTED`
- recovered per-provider counts and their safe aggregate

Classification reads the captured Maven output locally for fixed framework class names. It never
prints that file, exception messages, stack traces, SQL, datasource details, prompts, provider
responses, headers, or credentials.

## Startup Failure Closure

The first controlled parallel run stopped at `SPRING_STARTING` with
`DATABASE_INITIALIZATION_FAILURE`. Its recovered provider attempt counts were `0/0/0`, so no live
provider retry was needed or performed during this repair.

Offline regression reproduces authoritative `schema.sql` failure under the former H2 PostgreSQL
compatibility mode and proves that the isolated MySQL compatibility mode reaches `SPRING_READY`,
creates `tm_ai_call_log`, makes zero transport calls, and shuts down the bounded executor cleanly.

The cleanup lifecycle now uses script-level path and child-process variables plus a named EXIT
handler. It is safe under `set -u`, preserves the original exit status, handles partial temporary
file initialization, and removes only known nonblank temporary paths.

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
