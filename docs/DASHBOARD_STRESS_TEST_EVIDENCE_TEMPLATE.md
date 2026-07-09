# Dashboard Stress Test Evidence Template

Package: future dashboard stress execution package
Status: template only

`STRESS_TEST_EXECUTED: TBD`

`PRODUCTION_READINESS: BLOCKED`

## Run Context

- Branch:
- Main/base commit:
- Local app URL:
- App auth: username present / password redacted
- Command run:
- Start time:
- End time:
- Operator:

## Safety Confirmation

- Local-only target:
- No production server accessed:
- No production DB accessed:
- No provider endpoint called:
- No write endpoint called:
- No external Push sent:
- No Telegram sent:
- No order execution:
- No auto-open:
- No auto-close:
- No auto-reverse:
- No auto-trading:
- No fake positions:
- No fake review records:
- No secrets printed or committed:

## Configuration

- `STRESS_TARGET_BASE_URL`:
- Endpoints:
- `STRESS_CONCURRENCY_LEVELS`:
- `STRESS_REQUESTS_PER_ENDPOINT`:
- `STRESS_TIMEOUT_SECONDS`:
- Output directory:

## Results Summary

| Endpoint | Concurrency | Total | Success | Failure | Status distribution | Avg latency | Max latency | Approx p95 | Stop reason |
|---|---:|---:|---:|---:|---|---:|---:|---:|---|
| `/actuator/health` |  |  |  |  |  |  |  |  |  |
| `/dashboard` |  |  |  |  |  |  |  |  |  |
| `/api/dashboard/home` |  |  |  |  |  |  |  |  |  |

## Failure Log Summary

- Connection refused:
- Timeout:
- 401 / 403:
- 5xx:
- Unexpected provider call evidence:
- Production-ready claim evidence:

## Decision

- Local dashboard stress result: PASS / FAIL / STOPPED / SKIPPED
- Production readiness decision: BLOCKED
- Follow-up required:
