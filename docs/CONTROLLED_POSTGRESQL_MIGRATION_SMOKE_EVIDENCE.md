# Controlled PostgreSQL Migration Smoke Evidence

Package: PDR-PF11 Controlled PostgreSQL Migration Smoke Evidence
Date: 2026-07-07
Current main commit reviewed: `cef6547fde9d4727d4f5681f2113bd2b2a6f3626`
Branch: `codex/pdr-pf11-controlled-postgresql-migration-smoke`
Production readiness: BLOCKED
Production deployment: cannot proceed
Docker availability result: `DOCKER_MISSING`
Migration smoke result: `BLOCKED_ENV_UNAVAILABLE`
Flyway V1/V2/V3 PostgreSQL success: not proven

This package attempts to obtain trustworthy bounded PostgreSQL Flyway V1/V2/V3 migration success evidence using Docker/Testcontainers or a controlled non-production PostgreSQL environment. The current environment still lacks Docker, so the PostgreSQL migration smoke was not run. It does not access production DB, does not run destructive operations, does not change Flyway SQL, and does not claim production readiness.

## Scope

Controlled PostgreSQL migration smoke evidence only.

Allowed evidence collected:

- Docker command availability.
- Docker daemon availability.
- Docker socket availability on macOS-compatible paths.
- Existing PostgreSQL/Flyway smoke test presence.
- Decision on whether the bounded migration smoke may run.

No Java business logic, schema.sql, Flyway SQL, application config, trading behavior, external push send, fake position, or fake review behavior changed.

## Commands Run

Safe environment checks:

```bash
command -v docker || true
docker version || true
docker info || true
test -S /var/run/docker.sock || true
test -S ~/.docker/run/docker.sock || true
find src/test -iname '*Postgre*' -o -iname '*Flyway*'
if command -v docker >/dev/null 2>&1; then echo DOCKER_COMMAND_AVAILABLE; else echo DOCKER_COMMAND_MISSING; fi
if [ -S /var/run/docker.sock ]; then echo DOCKER_SOCKET_AVAILABLE; else echo DOCKER_SOCKET_UNAVAILABLE; fi
if [ -S "$HOME/.docker/run/docker.sock" ]; then echo DOCKER_DESKTOP_SOCKET_AVAILABLE; else echo DOCKER_DESKTOP_SOCKET_UNAVAILABLE; fi
command -v timeout || true
rg -n "AUTO_INCREMENT|MERGE INTO|ON DUPLICATE|DATEADD|FORMATDATETIME|CLOB" src/main/resources/db/migration/*.sql
```

## Docker Availability Result

`DOCKER_MISSING`.

Command output summary:

```text
$ command -v docker || true
<no output>

$ docker version || true
zsh:1: command not found: docker

$ docker info || true
zsh:1: command not found: docker

$ if command -v docker >/dev/null 2>&1; then echo DOCKER_COMMAND_AVAILABLE; else echo DOCKER_COMMAND_MISSING; fi
DOCKER_COMMAND_MISSING
```

Docker daemon result: `DOCKER_DAEMON_UNAVAILABLE` by dependency, because the Docker CLI is missing and daemon status cannot be queried.

## Docker Socket Result

Standard Docker socket:

```text
DOCKER_SOCKET_UNAVAILABLE
```

Docker Desktop-style macOS socket:

```text
DOCKER_DESKTOP_SOCKET_UNAVAILABLE
```

The raw `test -S` checks returned no output, consistent with unavailable sockets.

## Testcontainers Detection Result

`BLOCKED_BY_DOCKER_MISSING`.

The controlled smoke did not invoke Testcontainers because Docker CLI and sockets are absent. Under the command policy, the bounded Maven/Testcontainers smoke is allowed only after Docker/Testcontainers availability is confirmed.

The existing test remains present and is still the intended local Testcontainers proof path when Docker is provisioned.

## Existing PostgreSQL/Flyway Smoke Assets

Existing tests found:

```text
src/test/java/org/example/trademodel/postgresql
src/test/java/org/example/trademodel/postgresql/PostgreSqlFlywayMigrationSmokeTest.java
src/test/java/org/example/trademodel/mapper/PostgreSqlUpsertVariantGuardTest.java
src/test/java/org/example/trademodel/mapper/PostgreSqlDateFunctionVariantGuardTest.java
```

`PostgreSqlFlywayMigrationSmokeTest` remains the bounded target for future empty PostgreSQL Flyway migration proof.

## Static Migration SQL Scan

Command:

```bash
rg -n "AUTO_INCREMENT|MERGE INTO|ON DUPLICATE|DATEADD|FORMATDATETIME|CLOB" src/main/resources/db/migration/*.sql
```

Result: no matches.

This is static SQL evidence only. It does not prove PostgreSQL migration success.

## Bounded Migration Smoke Result

`BLOCKED_ENV_UNAVAILABLE`.

The bounded migration smoke command was not run because Docker/Testcontainers availability was not confirmed.

Command intentionally not run:

```bash
./mvnw -q -Dtest=PostgreSqlFlywayMigrationSmokeTest test
```

Reason: Docker CLI is missing and both Docker socket locations are unavailable. Running the Maven/Testcontainers smoke without Docker would not produce a trustworthy Flyway V1/V2/V3 PostgreSQL success log.

## Flyway V1/V2/V3 Success Evidence

Not proven.

No PostgreSQL container ran. No controlled non-production PostgreSQL server was provided. No Flyway migration executed against PostgreSQL. No `flyway_schema_history` rows were produced or inspected in this package.

## No Production Database Impact

- No production database was accessed.
- No production secrets were used.
- No destructive database operation was run.
- No backup, restore, migration, rollback, DDL, or DML operation was executed against a production database.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed.

PF11 confirms that the controlled PostgreSQL migration smoke cannot run in the current environment because Docker/Testcontainers is still unavailable. It does not prove PostgreSQL migration compatibility or production deployment readiness.

## Next Remediation Recommendation

Recommended next package: `PDR-PF12 Server-Backed Disposable PostgreSQL Migration Smoke`.

The next package should use one explicit evidence path:

1. Start Docker Desktop / Docker daemon and confirm Docker CLI plus socket availability before running the bounded Testcontainers smoke; or
2. Provide a controlled disposable PostgreSQL server and run Flyway V1/V2/V3 with redacted output and no production DB access.

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
