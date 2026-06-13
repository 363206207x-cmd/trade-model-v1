# V1 Next Minimal Runtime Slice Selection After Internal Push Preview / Notification Preview

## Scope

This A-risk package selects the 20th minimal, low-conflict, verifiable
`REVIEW_ONLY_RUNTIME partial` slice after Internal Push preview / notification
preview status visual closure.

Allowed changes:

- selection documentation
- source-of-truth documentation

Forbidden changes:

- Java business code, tests, dashboard business logic, schema/config/pom
- new DTO / Validator / Assembler / Orchestrator
- new service/domain/mapper/repository ownership family
- Push send, external channel, sendable message, provider payload
- PushSnapshot write
- Recheck execution or Replay execution
- Candidate generation, ranking, or scoring
- Decision generation
- Point generation
- final direction, entry, stop, TP, RR
- order, execution, auto-trading, Position Monitor execution
- missed-opportunity generation/write, review result generation
- paper order, simulated execution, paper PnL
- executable readiness, trading authorization
- position sizing, reduce/close/stop/reverse guidance
- recovery, repair, restart, auto-fix
- external API refresh, scheduler trigger, collector trigger, API client refresh
- Hot Reset execution/write, event generation, news fetch
- external AI call, Three AI provider orchestration, final arbiter behavior
- duplicate implementation of completed DecisionResult status
- duplicate implementation of completed Internal Push preview / notification preview status
- P359 / P360
- capability-level promotion

## Effective Baseline

- User-provided current main HEAD: `2c2972c docs(push): close internal push preview visual verification (#997)`.
- Source-of-truth baseline lag is not blocking. This package uses actual merged
  main as the effective execution baseline.
- Completed `REVIEW_ONLY_RUNTIME partial` slices before this selection: 19.
- Capability level remains `REVIEW_ONLY_RUNTIME partial`.

## Selection Result

Selected next slice:

`Recheck preview / recheck status`

Chinese label:

`复查预览 / 复查状态`

Next allowed action:

`Source Read for Recheck preview / recheck status`

Next branch:

`recheck-preview-recheck-status-source-read`

This selection does not authorize recheck execution, replay execution, scheduler
execution, Push send, external channel, PushSnapshot write, Candidate generation,
Decision generation, Point generation, final direction, entry/stop/TP/RR,
order/execution, auto-trading, Position Monitor execution, or new skeleton
owners.

## Candidate Comparison

| Candidate | Existing owner/read path | Main risk | Decision |
|---|---|---|---|
| Recheck preview / recheck status | `PushRecheckController`, `PushRecheckService`, `PushRecheckServiceImpl`, `PushRecheckScheduler`, `PushRecheckStatusContract`, `PushRecheckDispatchConfigService`, `PushRecheckLogMapper`, existing dashboard/recheck copy, and push/recheck tests are present as inventory targets. | Recheck execution, Replay execution, scheduler trigger, write-side status/log mutation, PushSnapshot write, Push send, and external channel adjacency. | Selected for source read only. The next package must classify execution/write/scheduler paths as forbidden boundary evidence and determine whether a pure read-only status slice is feasible. |
| Candidate preview / ranking status | Candidate attention / preview guard / ranking-era skeletons and tests exist. | Candidate generation, ranking, scoring, Point generation, entry/stop/TP/RR adjacency, and duplicate skeleton revival. | Rejected for this slot. |
| Position Monitor manual-input / monitor status | PositionSync and Position Monitor foundations exist. | Real-position monitoring, stop/moving-stop/reduce/close/reverse guidance, and Position Monitor execution. | Rejected. |
| Macro-news / event calendar status | Macro/news/event references exist through docs, evidence/score context, and the completed Hot Reset / Event Impact Source slice. | External API refresh, news fetch, event generation, scheduler/collector trigger. | Rejected. |
| Account risk downstream review-only display continuation | Account risk / account exposure status is already closed as slice 17. | Trading authorization, position sizing, reduce/close/stop/reverse guidance. | Rejected as completed-slice continuation risk. |
| Hot Reset / Event Impact downstream review-only display continuation | Hot Reset / Event Impact Source status is already closed as slice 18. | Hot Reset execution/write, event generation, external refresh, news fetch. | Rejected as completed-slice continuation risk. |
| Review / Replay downstream preview only | Review / Replay result status is already closed as slice 7. | Replay execution and duplicate Review/Replay status implementation. | Deferred. A future source read may revisit only if it proves a distinct missing owner path. |
| Data Source Health downstream continuation | Data Source Health dashboard/API status is already closed as slice 8. | Duplicate status expansion or refresh-trigger drift. | Deferred. |
| Existing dashboard/system placeholder with review-only owner path | No smaller, safer, unclosed placeholder was found during this selection pass. | Duplicate completed slice or unclear owner path. | Not selected. |
| Other smaller review-only runtime slice | No smaller non-duplicate source-read target was found. | N/A | Not selected. |

Explicit exclusions:

- `AI conflict / AI role convergence continuation`: excluded because #990 returned
  `NO-GO: duplicate with DecisionResult status`.
- `Internal Push preview / notification preview continuation`: excluded because
  it just closed as the 19th completed `REVIEW_ONLY_RUNTIME partial` slice.
- Push send / external channel: frozen.
- P359 / P360: frozen.

## Selection Reason

`Recheck preview / recheck status` is selected because it is the smallest
remaining non-completed candidate with identifiable existing owner assets and a
clear source-read question:

- Can existing recheck assets expose a review-only status/preview without
  executing recheck or replay?
- Can write/scheduler/log/update paths be kept as forbidden boundary evidence
  rather than runtime dependencies?
- Can the next design, if allowed later, reuse existing controller/service/test
  assets without new DTO / Validator / Assembler / Orchestrator or new ownership
  families?

The selection is intentionally conservative. It selects Source Read only, not
Design, Readiness Gate, Implementation, Verification, or Visual Closure.

## Rejected Candidate Details

- Candidate preview / ranking is lower priority because ranking/scoring language
  is too close to Candidate promotion and Point generation.
- Position Monitor is lower priority because it can easily become position
  execution, stop movement, close/reduce/reverse guidance, or real account action.
- Macro-news / event calendar remains too broad unless a future source read can
  prove a persisted local read model that avoids external refresh, news fetch,
  scheduler, collector, and event generation.
- Account risk and Hot Reset continuations are recent completed slices and would
  risk extending into action/execution behaviors.
- Review / Replay downstream preview is not selected now because Review / Replay
  result status is already a completed slice. The recheck source read must
  explicitly avoid duplicating that slice.
- Data Source Health downstream continuation is not selected because Data Source
  Health already closed and any continuation needs a separate distinct owner
  proof.
- Existing system placeholders do not present a smaller safer target than a
  source-read-only recheck inventory.

## Next Source Read Must Cover

The next source read should inspect at minimum:

- `PushRecheckController`
- `PushRecheckService`
- `PushRecheckServiceImpl`
- `PushRecheckScheduler`
- `PushRecheckStatusContract`
- `PushRecheckDispatchConfigService`
- `PushRecheckLogMapper`
- PushSnapshot related read/write owners as forbidden boundary evidence
- replay/recheck related service, mapper, and scheduler references
- dashboard recheck / internal push / review status surfaces
- review-page recheck display context, if present
- existing push/recheck/replay/dashboard tests
- existing review-only, manual-review, fail-closed, not-executable, and not-trading safety semantics

The source read must distinguish read-only recheck status evidence from:

- recheck execution
- replay execution
- scheduler trigger
- collector trigger
- Push send
- external channel
- PushSnapshot write
- Candidate generation / ranking / scoring
- Decision generation
- Point generation
- final direction / entry / stop / TP / RR
- order / execution / auto-trading
- Position Monitor execution

## Risk Notes

This target is safe only as an A-risk Source Read package.

Later Design must return NO-GO if it discovers that a future status requires:

- executing recheck or replay
- triggering a scheduler, collector, or API client refresh
- writing PushSnapshot, recheck logs, recheck status, review result, or any
  execution-adjacent state
- creating sendable messages or provider payloads
- connecting Push send or external channel
- generating Candidate, ranking, score, Decision, Point, final direction,
  entry/stop/TP/RR, order, execution, auto-trading, or Position Monitor action
- adding schema/config/pom
- adding DTO / Validator / Assembler / Orchestrator
- adding a new service/domain/mapper/repository ownership family

## Next Allowed Action

`Source Read for Recheck preview / recheck status`

Next branch:

`recheck-preview-recheck-status-source-read`

## Overreach Check

- No Java business code changed.
- No tests changed.
- No dashboard business logic changed.
- No schema/config/pom changed.
- No DTO / Validator / Assembler / Orchestrator added.
- No service/domain/mapper/repository ownership family added.
- No endpoint / panel behavior implemented.
- No Push send / external channel connected.
- No sendable message / provider payload generated.
- No PushSnapshot write connected.
- No Recheck / Replay execution connected.
- No Candidate generation / ranking / scoring connected.
- No Decision generation connected.
- No Point generation connected.
- No final direction / entry / stop / TP / RR generated.
- No order / execution / auto-trading connected.
- No Position Monitor execution connected.
- No external API refresh / scheduler / collector / API client refresh triggered.
- No Hot Reset execution/write, event generation, or news fetch connected.
- No external AI call, Three AI provider orchestration, or final arbiter behavior added.
- No capability-level promotion.
- P359 / P360 remain frozen.

## Capability Movement

No capability level movement.

Completed review-only runtime partial slices remain 19. This package is selection
only. The selected Recheck preview / recheck status slice has not completed a
runtime partial closure yet.

## #830 Audit

- New skeleton created: no.
- Cursor-era / V1 assets reused: yes, the selected next source read targets
  existing PushRecheck / recheck / dashboard / test assets.
- Duplicate reduction: yes, this excludes completed AI conflict, Internal Push,
  Account risk, Hot Reset, Review/Replay, and Data Source Health continuations
  unless a distinct future owner proof exists.
- Capability level raised: no.
- Service/runtime/dashboard/API connected: no implementation in this package;
  the selected next source read will inspect existing owner paths only.
- #830 compliance: yes, this selects an existing-owner source-read direction and
  keeps P359/P360, new wrapper families, external channels, execution, and
  trading frozen.
