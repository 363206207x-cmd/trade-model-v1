# PostgreSQL / Flyway V7 Controlled Evidence P1

## Decision

- Evidence package: `Controlled PostgreSQL and Flyway V7 Evidence P1`
- Base main commit: `ace5e560d35f214499a06d5478361318d371ee65`
- Evidence branch: `codex/postgresql-flyway-v7-evidence-p1`
- Environment: disposable localhost Docker PostgreSQL only
- Fresh migration result: **PASS**
- V6 to V7 upgrade result: **PASS**
- Session-timezone result: **PASS_NOT_SKIPPED**
- Application startup smoke: **PASS**
- Production deployment readiness: **BLOCKED**

This package is controlled migration and compatibility evidence. It is not a
production migration, deployment approval, provider enablement, trading
authorization, or production-ready claim.

## Environment

| Component | Controlled evidence value |
| --- | --- |
| Host binding | `127.0.0.1:55432` |
| PostgreSQL | `16.14` |
| Docker Engine | `29.6.1`, Linux `arm64` |
| Docker Compose | `v5.2.0` |
| Image | `postgres:16-alpine` |
| Image digest | `postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc` |
| Java | `25.0.2` |
| Maven wrapper | `3.9.9` |
| Spring Boot | `3.3.5` |
| Flyway | `10.10.0` |

The disposable password and temporary application Basic Auth credential were
provided only as process environment values. They are redacted from this
report and are not present in repository files.

## Safety Controls

The evidence runner requires both explicit confirmations:

```text
CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL
CONTROLLED_POSTGRESQL_FLYWAY_RUN=I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB
```

`ControlledPostgreSqlFlywayV7EvidenceTest` accepts only the exact controlled
endpoint `jdbc:postgresql://127.0.0.1:55432/trade_model_v1_test`, checks the
temporary test user, and creates only these dedicated databases:

- `trade_model_v1_fresh_test`
- `trade_model_v1_upgrade_test`

The test refuses production-like URL indicators. It never reads a secret file,
calls a provider, sends a message, creates a real position, or executes an
order. Database recreation is limited to those two disposable database names.

## Commands

The controlled test command used redacted process environment values:

```bash
CONTROLLED_POSTGRESQL_JDBC_URL=jdbc:postgresql://127.0.0.1:55432/trade_model_v1_test \
CONTROLLED_POSTGRESQL_USERNAME=trade_model_test \
CONTROLLED_POSTGRESQL_PASSWORD='<redacted-disposable-value>' \
CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM=I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL \
CONTROLLED_POSTGRESQL_FLYWAY_RUN=I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB \
./mvnw -q \
  -Dtest=ControlledPostgreSqlFlywayV7EvidenceTest,ControlledPostgreSqlFlywaySmokeTest,PostgreSqlFlywayMigrationSmokeTest \
  test
```

The application smoke used the `flyway-migration` Maven profile, the migrated
fresh database, port `18081`, and explicit false values for every scheduler,
AI provider, market provider, external-provider call, and external-send path.

## Fresh V1 to V7

The test recreated an empty `trade_model_v1_fresh_test` database before
running Flyway.

Observed result:

- Empty schema detected.
- Seven migrations validated.
- V1, V2, V3, V4, V5, V6, and V7 applied in order.
- Final schema version: `v7`.
- `27` `tm_*` base tables found.
- Flyway validation successful with zero invalid migrations.
- A second `migrate()` call applied `0` migrations and reported the schema up
  to date.

Flyway history snapshot:

| Rank | Version | Description | Success |
| ---: | ---: | --- | --- |
| 1 | 1 | baseline schema tables | true |
| 2 | 2 | baseline schema indexes | true |
| 3 | 3 | scheme rule config defaults | true |
| 4 | 4 | ohlcv ingestion provenance | true |
| 5 | 5 | provider scan profile orchestration | true |
| 6 | 6 | derivatives business rule defaults | true |
| 7 | 7 | decision plan offset times | true |

Failed history rows: `0`.

## V7 PostgreSQL Types

The types were read from PostgreSQL `information_schema.columns`, not inferred
from Java entities or `schema.sql`.

| Table | Column | PostgreSQL type | Nullable |
| --- | --- | --- | --- |
| `tm_decision_result` | `valid_from` | `timestamp with time zone` | yes |
| `tm_decision_result` | `expires_at` | `timestamp with time zone` | yes |

The execution-plan safety fields remain `NOT NULL DEFAULT TRUE`:

- `manual_review_required`
- `not_trade_instruction`
- `not_executable`
- `not_auto_trading`
- `not_order_execution`
- `not_user_position_creation`

## V6 to V7 Upgrade

The test recreated `trade_model_v1_upgrade_test`, migrated only through V6,
and inserted one minimal historical Analysis Run, Decision Result, and
Execution Plan. No position, order, or trading record was created.

It then removed the Flyway target and applied V7.

Observed result:

- V1 through V6 initial migration: **PASS**, six migrations applied.
- V7-only upgrade: **PASS**, one migration applied.
- Historical Analysis, Decision, and Plan rows remained present.
- Historical `valid_from` and `expires_at` values remained `NULL`; V7 did not
  invent or truncate historical time data.
- `AnalysisRunMapper`, `DecisionResultMapper`, and `ExecutionPlanMapper` read
  the historical rows successfully.
- New V7 Decision rows written as `2026-07-14T20:00:00+08:00` and
  `2026-07-14T08:00:00-04:00` both read back as the same
  `2026-07-14T12:00:00Z` instant.
- Equivalent expiry offsets both read back as
  `2026-07-15T12:00:00Z`.
- `PositionPlanSourceResolver` resolved the exact historical Plan A by typed
  execution-plan reference; no latest-sibling fallback was used.

## Session Timezones and UTC Boundaries

The PostgreSQL test executed real transactions under all three session zones:

- `UTC`
- `Asia/Shanghai`
- `America/New_York`

For each session, a fixed application clock of
`2026-07-14T12:00:00Z` passed through
`MonitorAlertWriteServiceImpl -> MonitorAlertMapper.insert`.

All three sessions stored the same UTC-naive values:

```text
created_at     = 2026-07-14 12:00:00
updated_at     = 2026-07-14 12:00:00
cooldown_until = 2026-07-14 12:15:00
```

The shared Baseline window was:

```text
windowStartInclusive = 2026-07-14 11:30:00
asOfInclusive         = 2026-07-14 12:00:00
```

For Monitor Alert, Analysis Run, low-quality Analysis Run, Push Recheck, Hot
Reset, and Hot Reset trigger-type distribution, the real PostgreSQL result was:

| Row time | Result |
| --- | --- |
| `11:29:59` | excluded |
| `11:30:00` | included |
| `11:59:59` | included |
| `12:00:00` | included |
| `12:00:01` | excluded |

`RunBaselineServiceImpl` consumed the same window and returned three included
Analysis Runs, three low-quality Runs, three Rechecks, and three Hot Reset
events for every session timezone. Future rows were excluded.

## Mapper and Service Compatibility

The controlled PostgreSQL test performed real read/write calls through:

- `AnalysisRunMapper`
- `DecisionResultMapper`
- `ExecutionPlanMapper`
- `PushSnapshotMapper`
- `PushRecheckLogMapper`
- `MonitorAlertMapper`
- `HotResetEventMapper`
- `AssetStateMapper`
- `RunBaselineServiceImpl`
- `PositionPlanSourceResolver`

The application smoke exercised the Spring-wired
`DashboardHomeServiceImpl` through `/api/dashboard/home` and
`RunBaselineServiceImpl` through `/api/system/run-baseline`. PostgreSQL selected
the annotated `databaseId="postgresql"` variants, including PostgreSQL date
formatting and asset-state upsert SQL.

No H2-only SQL failure, Boolean conversion failure, generated-key failure,
reserved-word failure, or offset timestamp mapping failure was observed in the
controlled paths.

## Application Startup Smoke

The application started on `127.0.0.1:18081` using the controlled PostgreSQL
fresh database and `SPRING_FLYWAY_ENABLED=true`.

Startup evidence:

- PostgreSQL connection pool: started.
- Flyway: seven migrations validated.
- Current schema version: `7`.
- Repeated migration: no migration necessary.
- `GET /actuator/health`: HTTP 200, `status=UP`.
- `GET /api/dashboard/home`: HTTP 200, fail-closed empty-state payload.
- Dashboard AI state: `NOT_CALLED`.
- Dashboard safety flags: review only, manual review only, not executable, not
  auto-trading, not order execution, no external Push, and no position
  creation/mutation.
- `GET /api/system/run-baseline`: HTTP 200 with PostgreSQL-backed zero-count
  summaries.

All schedulers, AI providers, market providers, provider external calls, and
external send paths were explicitly disabled. A post-start database check
found zero Analysis, Decision, Execution Plan, User Position, Monitor Alert,
Push Snapshot, Push Recheck, and Hot Reset rows. Startup created only three
local Push Recheck dispatch configuration defaults.

## Test Accounting

Controlled PostgreSQL/Flyway targeted run:

- Tests: `6`
- Failures: `0`
- Errors: `0`
- Skipped: `0`
- PostgreSQL execution status: **PASS_NOT_SKIPPED**

Full repository regression accounting is recorded by the final validation run
for this branch:

- Tests: `3526`
- Failures: `0`
- Errors: `0`
- Skipped: `1`

The single skipped test was the environment-gated
`CoinGlassControlledSmokeTest`; no live CoinGlass call or key was enabled. All
four PostgreSQL test classes ran with zero skips, including the six migration
evidence tests listed above.

## Historical Time Strategy

The controlled evidence proves the current contracts for newly written data;
it does not manufacture certainty for historical rows.

- New Monitor Alert rows use explicit application UTC-naive `created_at`,
  `updated_at`, and `cooldown_until` values.
- Monitor Alert rows written before the UTC-naive writer change cannot be
  automatically proven to use UTC.
- Historical `analysis_time` and Hot Reset `event_time` values may contain a
  mixed wall-clock basis.
- V7 leaves historical Decision validity columns null instead of guessing an
  offset.
- Historical timestamps must not be bulk-shifted automatically.
- Before any production migration decision, an operator must sample historical
  rows, inspect time distributions around known events, identify the writer
  cutover time, choose an explicit retain/ignore/migrate policy, rehearse it on
  a backup, and obtain release-owner approval.

## Remaining Gates

Production readiness remains **BLOCKED**. This local disposable evidence does
not close:

1. Historical mixed-time-basis sampling and an approved remediation policy.
2. Production-like backup/restore and current-state migration rehearsal for the
   actual release dataset.
3. Controlled server smoke behind the release HTTPS/reverse-proxy path.
4. Real secret-store injection and credential-rotation evidence.
5. Provider policy/waiver completion and release-owner approval.

Production deployment cannot proceed from this evidence package alone.

## Next Task

`PostgreSQL Evidence Review and Historical Time Strategy P2`
（PostgreSQL 证据复审与历史时间策略 P2）
