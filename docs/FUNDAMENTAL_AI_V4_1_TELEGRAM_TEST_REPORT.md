# Fundamental AI v4.1 Telegram Test Report

Status: `PASS_PENDING_EXACT_HEAD_CI_AND_INDEPENDENT_AUDIT`

Date: 2026-08-16

Branch: `codex/v4-1-telegram-high-value-alert-channel`

Implementation baseline after authorization merge:
`21ab98ad4155f2bc5f7792d2290d00f8481b00db`

## Results

| Validation | Result |
|---|---|
| Focused Telegram contract suite | PASS |
| Maven full suite, Java 17 | `4671` tests, `0` failures, `0` errors, `14` skipped, `4657` passed |
| Java 17 clean package | PASS |
| Product Source Gate | PASS |
| Workflow Contract | PASS |
| Exact-package authorization validator | PASS |
| Standard JAR Flyway content | PASS |
| PostgreSQL 16 standard release smoke | PASS |
| PostgreSQL V1 to V14 | `14/14_PASS` |
| PostgreSQL existing V14 restart | PASS |
| PostgreSQL V13 to V14 historical migration | PASS |
| PostgreSQL checksum/migration failure | PASS_FAIL_CLOSED |
| Packaged JAR login/Session/CSRF/logout | PASS |
| Telegram live application call | NOT RUN; deferred until audited code is merged |

The Maven Testcontainers checks were skipped by their existing no-Docker
contract because the local Testcontainers socket strategy was unavailable.
Independent real PostgreSQL 16 evidence was obtained through the repository's
standard disposable-container smoke and a controlled V13-to-V14 migration
run. No external Telegram request was made.

## Contract Coverage

- High-permission CONFIRMATION and configured qualified REDUCED acceptance.
- PREPARATION, OBSERVATION, BLOCKED, Preview, Candidate-only, unvalidated,
  expired, confused, and source/freshness failure rejection.
- Hot Reset and Push Recheck safety-change mapping.
- VERIFIED/FRESH material position change acceptance and untrusted rejection.
- Message-before-delivery and AFTER_COMMIT queueing.
- Delivery uniqueness, cooldown suppression, severity escalation, manual
  requeue, pre-send expiry suppression, atomic claim, retry, and crash recovery.
- HTTP 200, 400, 401, 403, 429, 5xx, timeout, invalid JSON, and `ok=false`.
- Public HTTPS recheck/position links and rejection of unsafe links.
- Authenticated secret-free status API and runtime-preflight output.
- V14 status migration, historical duplicate retention, due-row backfill, and
  active uniqueness.
- Position Monitoring, Push Recheck, authentication, and decision-chain
  regression coverage through the full suite.

## Secret And Live-State Statement

`TELEGRAM_DIRECT_CONNECTIVITY=PASS_USER_VERIFIED` is recorded from operator
evidence only. The private environment file was not read, printed, copied,
hashed, or loaded. Mock provider tests are not represented as live Telegram
acceptance.

`TELEGRAM_LIVE_APPLICATION_ACCEPTANCE=DEFERRED_UNTIL_MERGED_MAIN`.
