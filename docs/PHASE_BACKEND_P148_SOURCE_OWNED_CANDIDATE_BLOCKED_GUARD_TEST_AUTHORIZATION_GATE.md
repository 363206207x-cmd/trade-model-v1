# BACKEND-P148 Source-Owned Candidate Blocked Guard Test Authorization Gate

## Baseline

- Branch context: PR #411 / Issue #410.
- Formal mainline title: BACKEND-P148 Source-Owned Candidate Blocked Guard Test Authorization Gate.
- Base main commit: `cdba210` (`BACKEND-P147 Source-Owned Candidate Incomplete Guard Closure (#409)`).
- Scope: documentation-only authorization gate.
- Placeholder removed: `docs/P148.md`.

P148 is an authorization gate. It authorizes only one future smallest `BLOCKED` guard test. P148 does not write Java, does not add tests, and does not connect production wiring.

## Preceding Closure

The preceding source-owned candidate guard line is closed through P147:

- P145 authorized one smallest `INCOMPLETE` guard test.
- P146 implemented `SourceOwnedCandidateIncompleteGuardTest`.
- P147 confirmed P145-P146 cover only `INCOMPLETE` fail-closed behavior.

Because P145-P147 only closed the missing-evidence / incomplete-field slice, P148 can authorize only the next smallest `BLOCKED` guard test. P148 cannot skip ahead into production wiring, runtime SourceTrace population, `VALID` mapping, readiness, or action surfaces.

## Authorized Future Test File

P148 authorizes only this future test file:

```text
src/test/java/org/example/trademodel/dto/planboundary/SourceOwnedCandidateBlockedGuardTest.java
```

P148 does not create that file. P148 does not authorize any other Java or test file.

The future test file must remain DTO / fixture level. It must not start Spring context, instantiate production services, call controllers, call APIs, use network, read runtime data, read live market data, fetch external data, or generate real entry / stop / TP / RR values.

## BLOCKED Semantics

`BLOCKED` is not `INCOMPLETE`.

Definitions:

- `INCOMPLETE` means evidence is missing, fields are missing, or the system cannot determine source-owned safety.
- `BLOCKED` means evidence exists, but it is explicitly forbidden, unsafe, contradictory, or not allowed to promote the candidate.

Required future behavior:

- `BLOCKED` cannot upgrade to `REVIEW_ONLY_CANDIDATE`.
- `BLOCKED` cannot upgrade to `VALID`.
- `BLOCKED` cannot generate readiness.
- `BLOCKED` cannot generate executable state.
- `BLOCKED` cannot generate trade instruction text.
- `BLOCKED` cannot generate order / execution / automation surface.

The future test must prove that `BLOCKED` is fail-closed action prevention, not a trade-plan advancement state.

## Future P149 Minimum BLOCKED Guard Scenarios

P148 authorizes a minimal future P149 `BLOCKED` status guard test for:

- conflicting source ownership blocks candidate promotion
- stale source window with unsafe evidence blocks candidate promotion
- unsafe substitution blocks candidate promotion
- explicit blocked status preserves fail-closed output

The unsafe substitution coverage is authorized only as a minimal `BLOCKED` status guard assertion inside `SourceOwnedCandidateBlockedGuardTest.java`. P148 does not authorize a broad substitution blocked suite, additional substitution files, dashboard/API/display substitution sweeps, or production integration checks. If broader substitution coverage is needed, it must be separately authorized after P149.

## Future Safety Posture

The future P149 test must confirm every tested `BLOCKED` output preserves:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`
- no trade instruction
- no executable surface
- no readiness surface
- no order surface
- no execution surface
- no automation surface

The future test must not assert production readiness, execution readiness, dashboard readiness, or trade action eligibility.

## Future P149 Forbidden Scope

Future P149 must not add or modify:

- production Java
- existing tests outside the one authorized test file
- `dashboard.html`
- controller Java
- endpoint Java
- API wiring
- schema files
- config files
- service files
- mapper files
- runtime data readers
- live market data readers
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- order files
- execution files
- scheduler files
- automation files
- auto-trading files

Future P149 must not:

- test or map `VALID`
- call `BoundaryCandidateDTO.valid(...)`
- wire BoundaryCandidateService `VALID` production path
- upgrade ExecutionPlan readiness
- mutate dashboard readiness
- generate real entry / stop / TP / RR values
- read runtime / live / external data
- add order / execution / scheduler / automation / auto-trading behavior

## Still-Blocked Paths

The following paths remain blocked after P148:

- production candidate generation
- source-owned runtime candidate generation
- real entry / stop / TP / RR value generation
- runtime data reads
- live market data reads
- external data fetches
- external data integration
- exchange clients
- `WebClient`
- `RestTemplate`
- production `VALID` mapping
- BoundaryCandidateService `VALID` production path
- `BoundaryCandidateDTO.valid(...)` production calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- `dashboard.html` changes
- dashboard implementation beyond P130 static skeleton
- dashboard UI code beyond P130 static skeleton
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service changes
- service registration
- Spring bean registration
- mapper changes
- order API
- execution API
- scheduler / automation / auto-trading
- production ownership review wiring
- production completion
- production adapter
- `DefaultSourceTraceEntryOwnershipAdapter`
- production `DefaultSourceTraceEntryCompletionContract`
- runtime SourceTrace field population
- full SourceTrace runtime completion

## Recommended Next Step

After P148, the next step can be P149: Source-Owned Candidate Blocked Guard Test.

P149 may create only:

```text
src/test/java/org/example/trademodel/dto/planboundary/SourceOwnedCandidateBlockedGuardTest.java
```

P149 still cannot connect production wiring. P149 still cannot generate real entry / stop / TP / RR values. P149 still cannot call `BoundaryCandidateDTO.valid(...)`. P149 still cannot map production `VALID`, upgrade ExecutionPlan readiness, mutate dashboard readiness, or add order / execution / scheduler / automation / auto-trading.

STOP remains a valid choice if P149 is not separately authorized.

## P148 Boundary Confirmations

- P148 is documentation-only authorization-gate work.
- P148 adds one authorization-gate document only.
- P148 removes `docs/P148.md`.
- P148 does not add Java.
- P148 does not add tests.
- P148 does not modify production Java.
- P148 does not modify existing tests.
- P148 does not modify `dashboard.html`.
- P148 does not add controller / endpoint / API / schema / config / service / mapper changes.
- P148 does not read runtime data.
- P148 does not read live market data.
- P148 does not fetch external data.
- P148 does not generate real entry / stop / TP / RR values.
- P148 does not test or map `VALID`.
- P148 does not call `BoundaryCandidateDTO.valid(...)`.
- P148 does not upgrade ExecutionPlan readiness.
- P148 does not add order API.
- P148 does not add execution API.
- P148 does not add scheduler / automation / auto-trading.
- P148 does not merge the PR.

P148 stops here.
