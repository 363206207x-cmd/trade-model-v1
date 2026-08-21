# Fundamental AI v4.1 Knife B.1 Evidence Index

## Evidence Rules

- `UI_REVIEW_FIXTURE`: isolated `ui-review` runtime, authenticated local user, external providers and schedulers disabled. It is visual evidence only and is not live-provider evidence.
- `AUTOMATED_TEST`: controlled owner, lifecycle, trust, lineage, error and security evidence. It is not a browser or production-runtime claim.
- `NOT_VERIFIED_BROWSER_DATA_BOUNDARY`: the isolated runtime does not persist the required four-position or Push Recheck records, so no screenshot is claimed.
- No fixture is described as `LIVE` in this package.

## Browser Evidence

| Evidence | Classification | File | Result |
|---|---|---|---|
| Home at 1,440 | UI_REVIEW_FIXTURE | `knife-b1-home-1440.png` | Home Top3, Opportunity and single-workspace regression visible |
| Home at 1,080 | UI_REVIEW_FIXTURE | `knife-b1-home-1080.png` | document horizontal overflow 0; visible text clipping 0 |
| Analysis unknown mode | UI_REVIEW_FIXTURE | `knife-b1-analysis-unknown.png` | fail closed; no Candidate or failure-path copy rendered |
| Historical Home Gemini tab | UI_REVIEW_FIXTURE | `knife-b1-opportunity-gemini.png` | Home single-workspace layout only; this is not `/analysis/{id}` evidence and does not prove the formal Review Result contract |
| Historical Home Grok tab | UI_REVIEW_FIXTURE | `knife-b1-opportunity-grok.png` | Home single-workspace layout only; this is not `/analysis/{id}` evidence and does not prove the formal Failure Path contract |
| Positions active | UI_REVIEW_FIXTURE | `knife-b1-positions-active.png` | exact empty copy `暂无已录入持仓` |
| Positions history | UI_REVIEW_FIXTURE | `knife-b1-positions-history.png` | real tab action updates URL to `?tab=history` |
| Full audit destination | UI_REVIEW_FIXTURE | `knife-b1-audit-return.png` | real `/audit/{traceId}` target and encoded Home return context; missing audit record fails closed |

Browser session facts: authenticated isolated runtime, standard release JAR, HTTP 200, console error count 0. At 1,440 and 1,080 the document horizontal overflow count was 0. The only 1x1 overflow candidate was the intentionally screen-reader-only Search label, so user-visible text clipping count was 0.

## Contract Evidence

| Requirement | Classification | Evidence |
|---|---|---|
| Home Top3 while `/positions` returns all four active positions | AUTOMATED_TEST | `PositionMonitoringProjectionServiceTest#fourthPositionUsesItsOwnLatestMonitorInsteadOfHomeTopThree` plus Home contract regression |
| Fourth position owns its own latest monitor | AUTOMATED_TEST | same service test; mapper invocation is per `positionId` |
| `OPEN` and `PARTIALLY_CLOSED` active; `CLOSED` history only | AUTOMATED_TEST | `PositionMonitoringProjectionServiceTest#partiallyClosedLifecycleRemainsInActiveProjection` plus the owner-scoped history query contract |
| CLOSED detail preserves facts and does not query current monitor | AUTOMATED_TEST | `PositionMonitoringProjectionServiceTest#closedDetailPreservesFactsWithoutReadingOldMonitorAsCurrent`; frontend contract test |
| Cross-user Position/Recheck access is 404/fail closed | AUTOMATED_TEST | existing owner-scoped controller tests; `WorkspacePushRecheckServiceTest` mismatch cases |
| Preview GPT/Gemini/Grok mode ownership | AUTOMATED_TEST | `KnifeBFrontendContractTest#analysisUsesFormalModeStructuredRolesAndConflictGate` and structured AI schema tests |
| Opportunity GPT Candidate, Gemini downgrade, Grok failure path | AUTOMATED_TEST | historical evidence is limited to frontend/structured semantic tests; the old Home screenshots do not prove `/analysis/{id}` role output |
| Unknown Analysis mode fail closed | UI_REVIEW_FIXTURE + AUTOMATED_TEST | `knife-b1-analysis-unknown.png`; frontend contract test |
| No duplicate failure panel outside role tab | UI_REVIEW_FIXTURE + AUTOMATED_TEST | runtime `#analysisFailures` client rect count 0; frontend contract test |
| Production owner chain | AUTOMATED_TEST | `HighValueAlertMessageServiceTest#productionMessageIdentityResolvesOwnedSnapshotAndWritesRealRecheckOnlyAfterOpen` |
| New Message `currentRecheckId=null`; real id written only after OPEN | AUTOMATED_TEST | HighValueAlert test plus `WorkspacePushRecheckServiceTest#firstOpenCreatesOnePushOpenAndKeepsAllIdsDistinct` |
| Page bind, F5/read and refresh are GET-only | AUTOMATED_TEST | `WorkspacePushRecheckServiceTest#reloadAndReadOnlyGetNeverCreateOpen`; frontend contract test |
| Completed historical OPEN is not a permanent gate | AUTOMATED_TEST | `WorkspacePushRecheckServiceTest#completedOldOpenDoesNotPermanentlyBlockNewExplicitMessageOpen` |
| In-process duplicate OPEN reuses one attempt | AUTOMATED_TEST | `WorkspacePushRecheckServiceTest#concurrentDoubleOpenReusesSingleInProcessAttempt` |
| Infrastructure failure persists ERROR | AUTOMATED_TEST | `PushRecheckServiceImplTest#ownedPushOpenInfrastructureFailurePersistsRealErrorForExplicitRetry` |
| ERROR-only Retry creates a new same-target attempt | AUTOMATED_TEST | `WorkspacePushRecheckServiceTest#errorRetryCreatesNewAttemptForSameTarget` |
| Business invalidation remains COMPLETED and cannot use ERROR Retry | AUTOMATED_TEST | Push Recheck implementation and focused tests |
| Fake SCHEDULED, raw user pushId and manual/replay remain denied | AUTOMATED_TEST | `PushRecheckAccessBoundaryKnifeBTest` |
| Message/Plan/Analysis/Recheck/Position return context | AUTOMATED_TEST | `KnifeBFrontendContractTest#recheckAndHomeAuditLinksPreserveObjectAndReturnIdentity` |
| Open-redirect protection | UI_REVIEW_FIXTURE + AUTOMATED_TEST | valid `/messages?group=position` preserved; external scheme, `//`, backslash and double-encoded input all fell back to `/dashboard`; frontend contract test |

## Explicit Boundaries

- `FRESHNESS = NOT_VERIFIED`: no frozen freshness duration or cross-open reuse field exists; no duration was invented. Each new legal Message OPEN may create a new attempt, while F5/read never does.
- `CROSS_INSTANCE_IDEMPOTENCY = PARTIAL`: in-process owner + PushSnapshot coalescing is verified; no new schema or distributed lock was authorized.
- `KB-06 = BLOCKED_BY_MISSING_PERSISTENCE_SOURCE`: no auditable partial-close event producer exists.
- Four-position, CLOSED-detail and Recheck/ERROR browser captures are `NOT_VERIFIED_BROWSER_DATA_BOUNDARY`; their claims remain automated-test evidence.
- `LIVE_RUNTIME_ACCEPTANCE_DONE = NO`.
- The old `knife-b1-opportunity-gemini.png` and `knife-b1-opportunity-grok.png` are retained as historical Home facts; superseding Analysis-route evidence is indexed in `../knife_b_1_1/README.md`.
