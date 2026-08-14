# Fundamental AI v4.1 Final P1 Remediation Test Report

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

## Full Maven

Command: `./mvnw test -q`

- tests: `4556`
- passed: `4542`
- failures: `0`
- errors: `0`
- skipped: `14`
- suites: `422`
- exit code: `0`

The conditional external PostgreSQL test is one of the isolated-run skips. It
was then executed separately against a fresh disposable PostgreSQL instance.

## Java 17 Release Artifact

Command: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./mvnw -q -DskipTests package`

- Java release build: PASS;
- exit code: `0`;
- static resources and templates packaged with the application artifact.

## Focused Remediation

The focused ten-class run completed with `40` tests, `0` failures, `0` errors
and `0` skipped. It covers:

- canonical Home structure and Desktop interaction contracts;
- Java-computed Push Recheck cutoff and real H2 mapper execution;
- scheduler success/failure visibility;
- Analysis Preview persistence isolation;
- Asset Pool top-up/reset/`TRACKING_STOPPED` semantics;
- V13 contract and database-dialect guards.

## PostgreSQL 16.15

Controlled target: fresh disposable database `trade_model_v1_test` on a local
PostgreSQL 16.15 container. Credentials were redacted from all output.

- empty schema -> Flyway V1 through V13: PASS;
- migrations validated/applied: `13/13`;
- `tm_*` base tables: `38`;
- V11/V12/V13 core tables and V13 constraints: PASS;
- Push Recheck cutoff SQL executed with `cutoffAt`: PASS;
- recent retry excluded; older and first-attempt snapshots selected: PASS;
- failures/errors/skips: `0/0/0`.

The first controlled run exposed a stale smoke assertion that still expected
the V9 table/version totals. The test guard was corrected to the current V13
contract, numeric Flyway-version ordering was fixed, and a new empty-database
run passed.

## Browser And Visual Contract

- authenticated canonical Home captures at 1280, 1440, 1600 and wide Desktop;
- 1440 full page and zero-opportunity state;
- authenticated Analysis Preview and Asset Pool operation states;
- horizontal overflow: 0;
- one visible AI role: PASS;
- legacy Home active structure: 0;
- raw enum/component taxonomy primary copy: 0.

These browser states use controlled data and are not live-provider evidence.

## Governance Gates

The following are required again immediately before push:

- Product Source Gate;
- Workflow Contract;
- final-interaction authorization validation;
- duplicate skeleton guard;
- `git diff --check`;
- secret scan;
- exact-head PR CI.

CI run IDs and the exact pushed head are recorded in the PR handoff after push.

`MAVEN_FULL = PASS`

`POSTGRESQL_V1_V13 = PASS`

`PUSH_RECHECK_H2_SCHEDULER = PASS`

`PUSH_RECHECK_POSTGRESQL = PASS`
