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

## Controlled Live Evidence - PASS_3_OF_3

Operator-controlled evidence was collected on clean `main` at commit
`eacc224f23f8a63a1294bed4813a0aec5c5614bf`. All runs used the formal
`AiDecisionOrchestratorService`, the bounded three-thread executor, the fixed review-only fixture,
an isolated in-memory H2 database, disabled business schedulers, and disabled OpenAI internal model
fallback. No retry was permitted.

Final result order was deterministic in every run:

```text
GPT_RULE_REVIEW,GEMINI_CONSISTENCY_REVIEW,GROK_ADVERSARIAL_CHALLENGE
```

### Orchestration Runs

| Run | Result | Mode | Latency ms | Submitted | Completed | Success | Timeout | Failed | Partial fallback | Global deadline exceeded |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | PASS | AI_ASSISTED | 9,716 | 3 | 3 | 3 | 0 | 0 | false | false |
| 2 | PASS | AI_ASSISTED | 10,550 | 3 | 3 | 3 | 0 | 0 | false | false |
| 3 | PASS | AI_ASSISTED | 7,239 | 3 | 3 | 3 | 0 | 0 | false | false |

### Provider Runs

| Run | OpenAI HTTP / parse / latency / calls | Gemini HTTP / parse / latency / calls | xAI HTTP / parse / latency / calls |
| --- | --- | --- | --- |
| 1 | 2XX / PASS / 4,677 ms / 1 | 2XX / PASS / 9,681 ms / 1 | 2XX / PASS / 4,497 ms / 1 |
| 2 | 2XX / PASS / 5,236 ms / 1 | 2XX / PASS / 10,523 ms / 1 | 2XX / PASS / 6,939 ms / 1 |
| 3 | 2XX / PASS / 4,180 ms / 1 | 2XX / PASS / 7,206 ms / 1 | 2XX / PASS / 4,216 ms / 1 |

Each run recorded `LIVE_PROVIDER_CALLS: 3` and `REAL_KEYS_READ: 3`. Across all three runs:

- repeatability: `PASS_3_OF_3`
- total external calls: `9`
- OpenAI calls: `3`
- Gemini calls: `3`
- xAI calls: `3`
- failures: `0`
- timeouts: `0`
- partial fallbacks: `0`
- global deadline exceeded: `0`

### Aggregate Latency

| Measurement | Average | Observed range |
| --- | ---: | ---: |
| Orchestration | approximately 9,168 ms | 7,239-10,550 ms |
| OpenAI | approximately 4,698 ms | 4,180-5,236 ms |
| Gemini | approximately 9,137 ms | 7,206-10,523 ms |
| xAI | approximately 5,217 ms | 4,216-6,939 ms |

### Evidence Boundary

This evidence proves that all three configured credentials worked during the controlled runs, all
three endpoints were reachable, all responses passed their strict adapters/parsers, bounded
parallel submission worked, deterministic ordering was preserved, the global deadline was not
exceeded, and every provider was invoked exactly once per run. End-to-end orchestration completed
in approximately 7.2-10.6 seconds without failure, timeout, retry, or partial fallback.

The runs used no trading, order, position mutation, `ExecutionPlan` creation, Push, or Telegram
behavior. The evidence-closure package itself performs no live call and reads no secret.

This evidence does not prove long-term availability, production scheduling stability, concurrency
under real system load, monthly API cost, sustained rate-limit/quota behavior, AI decision
correctness, directional accuracy, profitability, or production deployment readiness.

Production readiness remains `BLOCKED`. Remaining gates include sustained soak testing,
cost-budget validation, rate-limit and quota validation, a real business-chain E2E beyond the fixed
review fixture, and an approved production rollout and rollback plan.
