# Fundamental AI v4.1 Test Report

Status: `AUDIT_REMEDIATION_LOCAL_VALIDATION_PASS`

## Full Maven Validation

Command: `./mvnw test -q`

Result:

- suites: `398`
- tests: `4394`
- passed: `4380`
- failures: `0`
- errors: `0`
- skipped: `14`

## v4.1 Focused Coverage

The principal new/extended contract suites contain `66` passing tests and no
failure or skip:

- AI response/authority/input safety and failure traces: `13`
- Candidate/Resolver/Final persistence: `4`
- role-specific AI orchestration: `4`
- Rule Validation: `5`
- decision-chain orchestration: `5`
- persistent Asset Pool: `4`
- market catalog search: `2`
- conflict resolver: `15`
- Opportunity state machine: `14`

Additional full-suite coverage verifies Analysis/Evidence/Score/Decision
integration, mapper dialect variants, Hot Reset canonical transition use,
manual UserPosition linkage, Review provenance, Dashboard compatibility, and
legacy behavior.

## Required Scenario Coverage

| Requirement | Evidence result |
|---|---|
| Asset Pool search/fuzzy/add/remove/restore/scan | `PASS` |
| Asset Pool is the Opportunity source gate | `PASS` |
| eight states, transition audit, Cooling recovery | `PASS` |
| debounce isolation by symbol and timeframe | `PASS` |
| GPT cannot create Final | `PASS` |
| Gemini/Grok cannot generate plans | `PASS` |
| Candidate cannot bypass Rule Validation | `PASS` |
| Candidate and Final identities differ | `PASS` |
| Confused produces blocked plan through canonical transition | `PASS` |
| blocked plan alone does not fabricate Confused | `PASS` |
| explicit manual/system-plan UserPosition source | `PASS` |
| system-plan UserPosition requires validated Final | `PASS` |
| AI success/failure/timeout trace completeness | `PASS` |
| canonical Conflict Level 1-4 and Plan Mode mapping | `PASS` |
| no automatic trading capability | `PASS` |

## PostgreSQL V11 Migration

`PostgreSqlFlywayMigrationSmokeTest` includes V11 coverage for historical state
normalization, six defaults, new tables/indexes, Candidate -> Resolver -> Final,
invalid Final rejection, and manual-only UserPosition linkage.

Local controlled result: `PASS` against disposable PostgreSQL `16.14`.

- migrations validated: `11`;
- historical path: empty -> V8 fixture -> V9/V10/V11;
- V11 historical AssetState timeframe normalization: `PASS`;
- canonical Conflict Level database constraint: `PASS`;
- UserPosition source/Final association constraints: `PASS`;
- tests: `1`, failures: `0`, errors: `0`, skipped: `0`.

The test was executed through its controlled PostgreSQL target because the
local Testcontainers client could not negotiate directly with the installed
Docker Desktop API. This is an environment adapter detail; the complete test
body ran against a real disposable PostgreSQL 16 database.

## Contract Gates

- Product Source Gate: `PASS`.
- Workflow Contract: `PASS`.
- Diff whitespace validation: `PASS`.
