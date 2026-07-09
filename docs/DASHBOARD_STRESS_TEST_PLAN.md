# Dashboard Stress Test Plan

Package: P1 Dashboard Stress Test Plan & Harness Preparation
Branch: `codex/p1-dashboard-stress-test-plan-harness`
Base main: `29d3237a` or newer
Status date: 2026-07-10

`STRESS_TEST_EXECUTED: NO`

`STRESS_TEST_EXECUTION_ALLOWED_IN_THIS_PR: NO`

`PRODUCTION_READINESS: BLOCKED`

## Scope

This package prepares a safe, reproducible, local-only dashboard stress-test plan and guarded harness. It does not execute the stress test. The follow-up execution package must be explicitly approved before traffic is generated.

The default target is local only: `http://localhost:8081`.

Allowed read-only target endpoints:

1. `GET /actuator/health`
2. `GET /dashboard`
3. `GET /api/dashboard/home`

## Excluded Scope

This package does not access a production server, production database, provider endpoint, real secret store, or external network dependency. It does not call write endpoints and does not call `POST` endpoints. It does not trigger scheduler, Push/Recheck, Telegram, provider refresh, order execution, auto-open, auto-close, auto-reverse, or auto-trading behavior.

## Harness Guard

The local harness is `scripts/dashboard-stress-local.sh`.

Default behavior is dry-run. Actual stress traffic is refused unless all of the following are true in a future approved execution package:

1. `DASHBOARD_STRESS_CONFIRM=YES`.
2. `STRESS_TARGET_BASE_URL` is a local loopback HTTP URL such as `http://localhost:8081` or `http://127.0.0.1:8081`.
3. `APP_ADMIN_USERNAME` is present.
4. `APP_ADMIN_PASSWORD` is present and redacted from output.
5. The endpoint list is restricted to the three allowed GET endpoints.

The harness prints the target URL, endpoint list, concurrency plan, request plan, output directory, and confirmation state. It never prints the password.

## Configuration

Default configuration:

```bash
STRESS_TARGET_BASE_URL=http://localhost:8081
STRESS_CONCURRENCY_LEVELS="1,5,10,20"
STRESS_REQUESTS_PER_ENDPOINT=100
STRESS_OUTPUT_DIR=build/stress-dashboard
STRESS_TIMEOUT_SECONDS=10
```

Dry-run command for this preparation package:

```bash
bash scripts/dashboard-stress-local.sh --dry-run
```

Future execution command, only after explicit approval:

```bash
DASHBOARD_STRESS_CONFIRM=YES \
STRESS_TARGET_BASE_URL=http://localhost:8081 \
APP_ADMIN_USERNAME=<local-admin-user> \
APP_ADMIN_PASSWORD=<redacted-local-password> \
bash scripts/dashboard-stress-local.sh
```

## Concurrency Ramp

The default ramp is `1,5,10,20` concurrent workers. Each endpoint receives `STRESS_REQUESTS_PER_ENDPOINT` requests at each concurrency level.

The execution package may lower the ramp for a smaller machine. It must not increase the ramp against any non-local target.

## Metrics

The harness records per endpoint and concurrency level:

- timestamp
- target URL
- total requests
- success count
- failure count
- status distribution
- average latency
- maximum latency
- approximate p95 latency
- stop reason, if any

## Evidence Format

Future execution evidence is written under `build/stress-dashboard/` and ignored by Git:

- `summary.txt`
- `endpoint-results.csv`
- `failures.log`
- `environment.txt`

Generated evidence must not be committed unless a later package explicitly creates a redacted documentation summary.

## Stop Conditions

The stress run must stop when any of the following is observed:

1. any configured 5xx threshold breach
2. repeated `401` / `403` indicating auth misconfiguration
3. connection refused
4. timeout spike or curl connection failure
5. JVM crash or app process exits
6. local machine overload
7. unexpected provider call evidence
8. external network dependency evidence
9. response text that incorrectly claims production readiness

## Success Criteria

A future approved run can be considered locally successful only when:

1. `GET /actuator/health` returns 200.
2. `GET /dashboard` returns 200 with expected authenticated access behavior.
3. `GET /api/dashboard/home` returns 200 with the expected read-only dashboard shape.
4. No 5xx responses occur.
5. No write endpoint is called.
6. No provider endpoint, production server, production database, or external Push/Telegram path is called.
7. No order, auto-open, auto-close, auto-reverse, or auto-trading behavior appears.
8. No fake position or fake review record is created.
9. Production readiness remains BLOCKED.

## Next-Step Execution Checklist

Before any future execution package runs the harness:

1. Confirm the branch and worktree state.
2. Start the app locally on `localhost:8081` with intended auth credentials.
3. Confirm no provider-live, Push/Recheck, Telegram, order, execution, or scheduler side effects are enabled.
4. Run `bash scripts/dashboard-stress-local.sh --dry-run` and inspect the plan.
5. Set `DASHBOARD_STRESS_CONFIRM=YES` only after explicit user approval.
6. Run the harness locally only.
7. Summarize redacted results in a separate evidence package.

## Safety Boundary

This is a dashboard read-only local stress plan. It is not production readiness, not production deployment, not provider smoke, and not a trading capability.
