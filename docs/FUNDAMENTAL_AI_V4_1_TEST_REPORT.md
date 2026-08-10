# Fundamental AI v4.1 Test Report

Status: `LOCAL_VALIDATION_PASS_AND_CI_POSTGRESQL_PASS`

## Full Maven Validation

Command: `./mvnw test -q`

Result:

- suites: `398`
- tests: `4382`
- passed: `4368`
- failures: `0`
- errors: `0`
- skipped: `14`

## v4.1 Focused Coverage

The principal new/extended contract suites contain `57` passing tests and no
failure or skip:

- AI response/authority/input safety: `5`
- Candidate/Resolver/Final persistence: `4`
- role-specific AI orchestration: `2`
- Rule Validation: `5`
- decision-chain orchestration: `5`
- persistent Asset Pool: `4`
- market catalog search: `2`
- AI trace persistence: `6`
- conflict resolver: `14`
- Opportunity state machine: `10`

Additional full-suite coverage verifies Analysis/Evidence/Score/Decision
integration, mapper dialect variants, Hot Reset canonical transition use,
manual UserPosition linkage, Review provenance, Dashboard compatibility, and
legacy behavior.

## Required Scenario Coverage

| Requirement | Evidence result |
|---|---|
| Asset Pool search/fuzzy/add/remove/restore/scan | `PASS` |
| Asset Pool is the Opportunity source gate | `PASS` |
| eight states, audit, precedence, debounce, cooling | `PASS` |
| GPT cannot create Final | `PASS` |
| Gemini/Grok cannot generate plans | `PASS` |
| Candidate cannot bypass Rule Validation | `PASS` |
| Candidate and Final identities differ | `PASS` |
| Confused produces blocked plan through canonical transition | `PASS` |
| blocked plan alone does not fabricate Confused | `PASS` |
| manual UserPosition and Final remain separate | `PASS` |
| AI trace input/output/model/token/cost/latency/fallback | `PASS` |
| no automatic trading capability | `PASS` |

## PostgreSQL V11 Migration

`PostgreSqlFlywayMigrationSmokeTest` includes V11 coverage for historical state
normalization, six defaults, new tables/indexes, Candidate -> Resolver -> Final,
invalid Final rejection, and manual-only UserPosition linkage.

Local result: `SKIPPED_DOCKER_UNAVAILABLE` because no Docker socket or
controlled PostgreSQL server is available. This is not reported as a
local PostgreSQL migration PASS. H2 schema and persistence constraints pass
locally.

Draft PR CI result: `PASS`. GitHub Actions run `31437240898` connected to the
Docker socket, started PostgreSQL 16 through Testcontainers, and ran
`PostgreSqlFlywayMigrationSmokeTest` with `1` test, `0` failures, `0` errors,
and `0` skipped.
