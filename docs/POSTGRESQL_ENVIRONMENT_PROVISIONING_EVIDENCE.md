# PostgreSQL Environment Provisioning Evidence

Package: PDR-PF10 PostgreSQL Environment Provisioning Evidence
Date: 2026-07-07
Current main commit reviewed: `040860ed7dacd2fff4c307432aee309983bb5813`
Branch: `codex/pdr-pf10-postgresql-environment-provisioning`
Production readiness: BLOCKED
Production deployment: cannot proceed
Docker availability result: `DOCKER_MISSING`
Migration smoke result: `SKIPPED_ENV_UNAVAILABLE`

This package records whether the local/server environment can support PostgreSQL migration smoke evidence after PDR-PF9 reported `BLOCKED_ENV_UNAVAILABLE`. It does not access production DB, does not run destructive DB operations, does not change Flyway SQL, and does not claim production readiness.

## Scope

This is a production-readiness remediation evidence package for PostgreSQL/Testcontainers environment readiness only.

Allowed evidence collected:

- Docker command availability.
- Docker daemon availability.
- Docker socket availability.
- Docker Desktop-style socket availability on macOS.
- Existing PostgreSQL/Flyway smoke test presence.
- Static Flyway migration SQL H2-pattern scan.

The PostgreSQL migration smoke was not run because Docker is not available. This follows the package command policy: run the targeted Maven/Testcontainers command only if Docker is available.

## Environment Checks Run

```bash
docker version
docker info
test -S /var/run/docker.sock || true
find src/test -iname '*Postgre*' -o -iname '*Flyway*'
ls -l /var/run/docker.sock ~/.docker/run/docker.sock 2>/dev/null || true
command -v docker || true
command -v timeout || true
if [ -S /var/run/docker.sock ]; then echo DOCKER_SOCKET_AVAILABLE; else echo DOCKER_SOCKET_UNAVAILABLE; fi
if [ -S "$HOME/.docker/run/docker.sock" ]; then echo DOCKER_DESKTOP_SOCKET_AVAILABLE; else echo DOCKER_DESKTOP_SOCKET_UNAVAILABLE; fi
rg -n "AUTO_INCREMENT|MERGE INTO|ON DUPLICATE|DATEADD|FORMATDATETIME|CLOB" src/main/resources/db/migration/*.sql
```

## Docker Availability Result

`DOCKER_MISSING`.

Command output summary:

```text
$ docker version
zsh:1: command not found: docker

$ docker info
zsh:1: command not found: docker

$ command -v docker || true
<no output>
```

Docker daemon availability: not checkable because Docker CLI is missing.

## Docker Socket Result

Standard Docker socket:

```text
DOCKER_SOCKET_UNAVAILABLE
```

Docker Desktop-style macOS socket:

```text
DOCKER_DESKTOP_SOCKET_UNAVAILABLE
```

Socket listing produced no socket paths for `/var/run/docker.sock` or `~/.docker/run/docker.sock`.

## Testcontainers Detection Result

`BLOCKED_BY_DOCKER_MISSING`.

PF10 did not run the Maven/Testcontainers migration smoke because Docker is missing and both socket checks are unavailable. Under the package command policy, the targeted smoke is only allowed after Docker availability is confirmed.

The prior PF9 bounded smoke already showed Testcontainers could not find a valid Docker environment. PF10 confirms the root environment blocker before any Maven/Testcontainers invocation: Docker CLI and sockets are absent.

## Existing PostgreSQL/Flyway Smoke Assets

Existing tests found:

```text
src/test/java/org/example/trademodel/postgresql
src/test/java/org/example/trademodel/postgresql/PostgreSqlFlywayMigrationSmokeTest.java
src/test/java/org/example/trademodel/mapper/PostgreSqlUpsertVariantGuardTest.java
src/test/java/org/example/trademodel/mapper/PostgreSqlDateFunctionVariantGuardTest.java
```

`PostgreSqlFlywayMigrationSmokeTest` remains the intended target for empty PostgreSQL Flyway migration proof when Docker/Testcontainers becomes available.

## Static Migration SQL Scan

Command:

```bash
rg -n "AUTO_INCREMENT|MERGE INTO|ON DUPLICATE|DATEADD|FORMATDATETIME|CLOB" src/main/resources/db/migration/*.sql
```

Result: no matches.

This is only static evidence. It does not prove PostgreSQL migration success.

## Migration Smoke Result

`SKIPPED_ENV_UNAVAILABLE`.

The bounded migration smoke command was not run because Docker availability was not confirmed.

Command intentionally not run:

```bash
./mvnw -q -Dtest=PostgreSqlFlywayMigrationSmokeTest test
```

Reason: Docker CLI is missing and Docker sockets are unavailable. Running a Testcontainers command without Docker would only reproduce the environment skip and would not produce Flyway V1/V2/V3 PostgreSQL success evidence.

## Flyway V1/V2/V3 Success Evidence

Not proven.

No PostgreSQL container ran. No Flyway migration executed against PostgreSQL. No `flyway_schema_history` rows were produced or inspected in this package.

## No Production Database Impact

- No production database was accessed.
- No production secrets were used.
- No destructive database operation was run.
- No backup, restore, migration, rollback, DDL, or DML operation was executed against a production database.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PF10 proves the current local environment is not provisioned for Docker/Testcontainers-backed PostgreSQL migration evidence. It does not prove PostgreSQL migration compatibility or production deployment readiness.

## Next Remediation Recommendation

Recommended next package: `PDR-PF11 Docker/Testcontainers Provisioning Or Server-Backed PostgreSQL Smoke`.

The next package should use one explicit evidence path:

1. Install/enable Docker or Docker Desktop in the local/server evidence environment, confirm Docker CLI, daemon, and socket availability, then run the bounded `PostgreSqlFlywayMigrationSmokeTest`; or
2. Use a controlled disposable PostgreSQL server environment and run Flyway V1/V2/V3 with redacted output and no production DB access.

After empty migration evidence is PASS, continue to current-state migration and rollback drill execution.

## Prohibited Items Preserved

- no auto-open
- no auto-close
- no auto-reverse
- no order execution
- no auto-trading
- no external push send
- no fake positions
- no fake review records
- no production-ready claim
