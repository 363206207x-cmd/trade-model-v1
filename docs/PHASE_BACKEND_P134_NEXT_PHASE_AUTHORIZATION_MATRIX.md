# BACKEND-P134 Next Phase Authorization Matrix

## Baseline

- Branch context: PR #380 / Issue #379.
- Formal mainline title: BACKEND-P134 Next Phase Authorization Matrix.
- PR title note: PR #380 uses a shortened title as a platform workaround; Issue #379 and this document preserve the formal mainline title.
- Freeze baseline: `5d95e87` (`P133 Freeze Index (#378)`).
- Scope: documentation-only authorization matrix for future phase classification after the P133 read-only system freeze.
- Line context: P134 is part of the Global Freeze Line.
- Placeholder removed: `docs/P134.md`.

## Files Changed

- `docs/PHASE_BACKEND_P134_NEXT_PHASE_AUTHORIZATION_MATRIX.md`
- Removed `docs/P134.md`

No production Java, test source, dashboard HTML, dashboard UI code, controller, endpoint, API, schema, config, service, runtime, live-market, external-data, readiness, order, execution, scheduler, automation, or auto-trading files are changed.

## Matrix Purpose

P134 classifies possible future phases before any work starts. It defines allowed scopes, forbidden scopes, approval requirements, validation gates, rollback requirements, and stop conditions for each future phase type.

P134 does not start production wiring. P134 does not authorize production wiring implementation. P134 does not authorize readiness upgrades. P134 does not authorize real entry / stop / TP / RR generation. P134 does not authorize order, execution, scheduler, automation, or auto-trading.

## Authorization Matrix

| Future Phase Category | Allowed Files | Forbidden Files | Required Approvals | Required Tests / Validation | Rollback Requirements | No-Go Triggers | Currently Authorized |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1. STOP / no next phase | No files. | All code, docs, dashboard, runtime, API, schema, config, service, order, execution, scheduler, automation, and auto-trading files. | None. | None. | No rollback needed because no change is made. | Any file change. | Yes. STOP is always allowed. |
| 2. Documentation-only planning | Future docs file only, with exact issue scope. | Production Java, test source, `dashboard.html`, dashboard UI code, controller, endpoint, API, schema, config, service, runtime, data access, readiness, order, execution, scheduler, automation, and auto-trading files. | New issue and explicit documentation-only authorization. | `git diff --check`; `git diff --cached --check`; Maven may be skipped when no Java/test source changes exist. | Revert the planning document if it widens scope or implies implementation authorization. | Any code/runtime/dashboard/API/schema/config/service change, or language authorizing production wiring. | Conditionally allowed only with a new issue. |
| 3. Static read-only UI polish | Exact listed static display files only, normally limited to `src/main/resources/templates/dashboard.html` if separately authorized. | Production Java, controller, endpoint, API, schema, config, service, runtime, data access, readiness, order, execution, scheduler, automation, auto-trading, and any unlisted UI file. | New issue, exact file list, and explicit no-action-language approval. | `git diff --check`; `git diff --cached --check`; relevant static dashboard/template tests if present; grep/search for forbidden labels and action surfaces. | Revert to the last static skeleton if action language, wiring, readiness, or executable state appears. | Buy/sell/open/close/reverse/signal/trade-ready/ready-to-trade/executable/production VALID/auto-trading copy, buttons, links, forms, click handlers, fetch calls, API wiring, localStorage decision logic, or readiness mutation. | Not authorized by P134. Conditionally eligible only under a separate future issue. |
| 4. Static guard tests | Test source files under `src/test/java` only, with exact focused guard scope. | Production Java, runtime wiring, dashboard implementation, controller, endpoint, API, schema, config, service, external-data integration, order, execution, scheduler, automation, and auto-trading files. | New issue with exact test files and guard assertions. | Focused guard tests named by the issue; `./mvnw -q -DskipTests compile`; `./mvnw -q -DskipTests test-compile`; `git diff --check`; `git diff --cached --check`. | Revert the test change if it introduces helper productionization, runtime reads, production DTO valid mapping, readiness, or action surfaces. | Any runtime/live/external read, production `VALID` mapping, `BoundaryCandidateDTO.valid(...)`, `BoundaryCandidateService` `VALID` path, readiness upgrade, action surface, BigDecimal real trading values, or Spring/controller registration. | Not authorized by P134. Conditionally eligible only under a separate future issue. |
| 5. Production wiring preparation | Documentation-only preparation, authorization checklist, dry-run review plan, or explicitly approved test-only guard expansion. | Production wiring, service registration, Spring bean registration, controller/endpoint/API wiring, schema/config changes, runtime/live/external data reads, real trading values, readiness upgrade, order/execution/scheduler/automation/auto-trading. | Separate production-wiring-preparation authorization naming exact boundary, allowed files, still-blocked paths, tests, rollback, and reviewer checklist. | P133/P134 diff checks; all named C/D/E/read-only guard tests required by the future issue; compile/test-compile if any test source changes exist. | Revert to P133/P134 freeze point if preparation implies implementation, wires runtime paths, or weakens review-only invariants. | Any production wiring implementation, runtime/live/external read, production `VALID`, readiness, real values, dashboard/API/schema/config/service mutation, order/execution/automation path, or unclear approval. | Not authorized by P134. Separate authorization required. |
| 6. Production wiring implementation | None under P134. | All production wiring files, production Java, controller/endpoint/API/schema/config/service registration, runtime/live/external data access, real value generation, readiness, order, execution, scheduler, automation, and auto-trading files. | Not available under P134. A separate future issue must explicitly authorize implementation and name exact files, data sources, invariants, rollback, and acceptance tests. | Not applicable under P134. Future implementation would require a separately authorized full regression set before and after the change. | Do not start. If implementation appears under P134, revert immediately to the freeze point. | Any implementation of production candidate generation, production wiring, production `VALID`, readiness, real values, runtime data, live data, external fetches, order/execution/scheduler/automation/auto-trading. | No. Production wiring implementation is not authorized by P134. |

## Authorization Decisions

- STOP is always allowed.
- Documentation-only planning may be allowed with a new issue.
- Static read-only UI polish may be allowed only with an exact file list and no action language.
- Static guard tests may be allowed only if they do not create runtime wiring.
- Production wiring preparation may be allowed only with separate authorization.
- Production wiring implementation is not authorized by P134.
- Order / execution / automation / auto-trading is not authorized by P134.
- Real entry / stop / TP / RR generation is not authorized by P134.
- ExecutionPlan readiness upgrade is not authorized by P134.
- Dashboard implementation beyond the P130 static skeleton is not authorized by P134.

## Required Approval Fields For Future Phases

Any future phase that is not STOP must name:

- phase category
- exact issue number
- exact formal mainline title
- exact branch
- exact allowed files
- explicitly forbidden files
- still-blocked paths
- required tests and validation commands
- rollback point
- no-go triggers
- whether production wiring remains blocked
- whether readiness remains blocked
- whether real entry / stop / TP / RR generation remains blocked
- whether order / execution / scheduler / automation / auto-trading remains blocked

Broad language such as "continue", "wire it", "make production ready", "enable readiness", or "finish implementation" is not enough authorization.

## Required Validation Gates

Documentation-only phases must run:

```text
git diff --check
git diff --cached --check
```

Static read-only UI polish must add any issue-named static dashboard/template checks and must search for forbidden action labels.

Static guard tests must run the focused test class or classes named by the issue, plus:

```text
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
git diff --cached --check
```

Production wiring preparation must preserve P133/P134 freeze boundaries and run every guard or regression command named by the future issue.

Production wiring implementation has no validation gate under P134 because it is not authorized.

## Rollback Requirements

Rollback must return to the last freeze point when a no-go trigger appears. The default freeze point for this matrix is:

```text
5d95e87 P133 Freeze Index (#378)
```

Rollback is required if a future change:

- widens scope beyond its approved phase category
- modifies a forbidden file
- introduces action language or executable state
- weakens the mandatory invariants
- maps a read-only state to production `VALID`
- upgrades readiness
- generates real entry / stop / TP / RR values
- introduces runtime/live/external data reads
- introduces order / execution / scheduler / automation / auto-trading behavior
- introduces unclear production wiring or service registration

## Stop Conditions

The correct next action is STOP when:

- no separately authorized next issue exists
- the requested next phase does not name exact files
- approval language is broad or ambiguous
- required rollback path is missing
- required validation gates are missing
- any no-go trigger is present
- the task asks for production wiring implementation under P134
- the task asks for readiness, real trading values, order, execution, scheduler, automation, or auto-trading under P134

## Preserved P133 Freeze Invariants

The following invariants remain mandatory:

- `manualReviewRequired=true`
- `notTradeInstruction=true`
- `reviewMode=REVIEW_ONLY`

These invariants do not imply production readiness, executable state, dashboard readiness, ExecutionPlan readiness, order behavior, execution behavior, scheduler behavior, automation behavior, or auto-trading.

## Preserved Status Boundaries

The following status boundaries remain frozen:

- `REVIEW_ONLY_CANDIDATE` = review-only context only
- `INCOMPLETE` = missing-evidence context only
- `BLOCKED` = no-go / forbidden / Risk Action Guard blocked context only

None of these statuses may become production `VALID`, readiness, dashboard readiness, ExecutionPlan readiness, executable plan state, action plan, order plan, execution plan, scheduler plan, automation plan, or auto-trading under P134.

## Still-Blocked Paths

The following paths remain blocked:

- production candidate generation
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
- `BoundaryCandidateDTO.valid(...)` calls
- production `BoundaryStatusEnum.VALID` mapping
- ExecutionPlan readiness upgrade
- dashboard readiness mutation
- dashboard implementation beyond P130 static skeleton
- `dashboard.html` changes beyond P130 static skeleton
- dashboard UI code beyond P130 static skeleton
- controller / endpoint Java
- API wiring
- schema changes
- config changes
- service registration
- Spring bean registration
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

## Boundary Confirmations

- P134 is a documentation-only authorization matrix.
- P134 is part of the Global Freeze Line.
- P134 removes the placeholder `docs/P134.md`.
- P134 adds one authorization matrix document.
- P134 does not modify production Java.
- P134 does not modify test source.
- P134 does not modify `dashboard.html`.
- P134 does not add dashboard UI code.
- P134 does not add controller / endpoint / API / schema / config / service changes.
- P134 does not read runtime data.
- P134 does not read live market data.
- P134 does not fetch external data.
- P134 does not generate real entry / stop / TP / RR values.
- P134 does not upgrade ExecutionPlan readiness.
- P134 does not map to production `VALID`.
- P134 does not wire BoundaryCandidateService `VALID` production path.
- P134 does not call `BoundaryCandidateDTO.valid(...)`.
- P134 does not add order API.
- P134 does not add execution API.
- P134 does not add scheduler / automation / auto-trading.

## Validation

P134 is documentation-only, so Maven may be skipped. Validation is limited to:

```text
git diff --check
git diff --cached --check
```

## PR Body Checklist

The PR body must include:

- files changed
- validation performed
- authorization matrix coverage
- future phase categories
- authorization decisions
- required approvals / tests / rollback
- still-blocked paths
- boundary confirmations
- note that the PR short title is only a platform workaround; formal mainline is Issue #379 / BACKEND-P134

P134 stops here. It does not merge the PR.
