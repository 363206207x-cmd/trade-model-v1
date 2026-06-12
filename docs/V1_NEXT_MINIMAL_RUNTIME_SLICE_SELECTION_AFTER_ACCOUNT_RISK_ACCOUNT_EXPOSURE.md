# V1 Next Minimal Runtime Slice Selection After Account Risk / Account Exposure

## Scope

This A-risk package selects the 18th minimal, low-conflict, verifiable `REVIEW_ONLY_RUNTIME partial` slice after Account risk / account exposure status closure.

Allowed changes:

- selection documentation
- source-of-truth documentation

Forbidden changes:

- Java business code, tests, dashboard business logic, schema/config/pom
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- Push send, external channel, replay/recheck execution
- Candidate generation, Decision generation, Point generation
- final direction, entry, stop, TP, RR
- order, execution, auto-trading, Position Monitor execution
- missed-opportunity generation/write, review result generation
- paper order, simulated execution, paper PnL
- executable readiness, trading authorization
- position sizing, reduce/close/stop/reverse guidance
- recovery, repair, restart, auto-fix
- external API refresh, scheduler trigger, collector trigger
- capability-level promotion

## Effective Baseline

- User-provided current main HEAD: `4c829d8 docs(risk): close account exposure visual verification`
- Source-of-truth baseline lag is not blocking. This package uses actual merged main as the effective execution baseline.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this selection: 17.
- Capability level remains `REVIEW_ONLY_RUNTIME partial`.

## Candidate Comparison

| Candidate | Existing owner/read path | Main risk | Decision |
|---|---|---|---|
| Macro-news / event calendar status | Broad macro/news evidence exists in score/evidence paths; local event-impact and Hot Reset assets also exist. | Broad macro-news/event-calendar scope can require external API refresh, news fetch, or event generation. | Reject broad scope; select a narrower local-source slice: Hot Reset / Event Impact Source review-only status source read. |
| Three AI / AI conflict status | Dashboard/schema already expose AI role/conflict context. | DecisionEngine and AI role outputs sit close to final bias, final score, final direction, and decision generation. | Reject for now. Needs stricter source read before any runtime slice. |
| Internal Push preview / notification preview status | Existing internal push preview dashboard placeholder and review-only assembler/test assets exist. | PushSnapshot write side, Push send, external channel, sendable message, and notification channel risks are nearby. | Reject for now. |
| Recheck status / recheck preview | Existing PushRecheck read/log/overview assets exist. | POST trigger, scheduler, replay, and recheck execution paths are nearby. | Reject for now. |
| Candidate preview / ranking status | Watchlist scan / candidate assets exist. | Candidate generation, ranking, scoring, point, and duplicate skeleton risks are high. | Reject for now. |
| Position Monitor manual-input / monitor status | Position provider, sync, monitor, and alert assets exist. | Real position provider, monitor execution, stop/close/reverse/action guidance risks are high. | Reject for now. |
| Account risk downstream display continuation | Just closed account risk / exposure status with review-only dashboard/API visibility. | Continuation can drift into trading authorization, position sizing, or reduce/close/stop/reverse guidance. | Reject for now. |
| Existing dashboard/system placeholder | Several placeholders exist, many already closed as completed review-only slices. | No smaller unfinished placeholder was found than local Hot Reset / Event Impact Source assets. | Superseded by selected local event-impact source read. |
| Other smaller source-discovered slice | Existing `HotResetService`, `HotResetEventMapper`, `tm_hot_reset_event`, `EventImpactInputVO`, and fail-closed source-trace event-source ownership assets exist. | Must avoid Hot Reset execution/write behavior, event generation, external API refresh, Decision generation, Candidate, Point, and trading. | Select. |

## Selected Next Slice

Selected next slice:

`Hot Reset / Event Impact Source review-only status`

This is intentionally narrower than broad Macro-news / event calendar status. The next package must perform source read only and confirm whether the existing local event-impact / Hot Reset / source-trace event-source ownership assets can support a minimal review-only status slice.

## Reusable Evidence To Source Read Next

The next source-read package should inventory, at minimum:

- `HotResetService`
- `HotResetServiceImpl`
- `HotResetEventDO`
- `HotResetEventMapper`
- `EventImpactInputVO`
- `tm_hot_reset_event`
- `tm_asset_state` hot-reset / event-impact fields, if referenced
- `SourceTraceEventSourceOwnershipService`
- `FailClosedSourceTraceEventSourceOwnershipService`
- `SourceTraceEventSourceOwnershipResult`
- `SourceTraceEventSourceOwnershipStatusEnum`
- `SourceTraceEventSourceMissingReasonEnum`
- `SourceTraceEventSourceReviewModeEnum`
- `SourceTraceEventSourceOwnershipServiceTest`
- any dashboard / review-page / score / evidence display context mentioning event impact, event source, hot reset, macro risk, or abnormal event metadata

## Selection Reason

Hot Reset / Event Impact Source review-only status is the smallest safe next candidate because:

- it has existing local owner assets instead of requiring external macro-news fetches;
- the current fail-closed source-trace event-source ownership skeleton already carries review-only / manual-review / not-trade-instruction semantics;
- the source-read scope can be limited to status visibility and owner-path inventory;
- it can explicitly reject Hot Reset execution, event generation, external refresh, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, and trading;
- it keeps the project inside `REVIEW_ONLY_RUNTIME partial`.

## Risk Notes

- Broad Macro-news / event calendar remains deferred because it can imply external API refresh, news scraping, event creation, or scheduler/collector behavior.
- Hot Reset wording is execution-adjacent. The next source read must prove whether a status-only read model exists and must keep `executeHotReset` / asset-state writes out of scope.
- `EventImpactInputVO` and score/evidence event fields must remain display/read context only. They cannot become signal generation, final direction, entry/stop/TP/RR, or trading instruction.
- Existing fail-closed source-trace event-source ownership assets are promising but may still be too skeletal for implementation; source read must document gaps before any design.

## Next Allowed Action

`Source Read for Hot Reset / Event Impact Source review-only status`

Next branch:

`hot-reset-event-impact-source-status-source-read`

## Overreach Check

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No DTO / Validator / Assembler / Orchestrator added.
- No service/domain/mapper/repository ownership family added.
- No Push send / external channel connected.
- No replay / recheck execution connected.
- No Candidate / Decision generation / Point / final direction / entry / stop / TP / RR generated.
- No order / execution / auto-trading connected.
- No Position Monitor execution connected.
- No external API refresh / scheduler / collector trigger connected.
- No capability-level promotion.

## #830 Audit

- New skeleton created: no.
- Cursor-era / existing assets reused: yes, selection points to existing Hot Reset / Event Impact / SourceTrace event-source ownership assets for source read.
- Duplicate reduction: yes, selection avoids broad new macro-news/event-calendar ownership and requires existing-owner source read first.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no, selection only.
- #830 compliance: yes, this package selects a canonical-source read direction without adding duplicate skeletons or continuing P359/P360.
