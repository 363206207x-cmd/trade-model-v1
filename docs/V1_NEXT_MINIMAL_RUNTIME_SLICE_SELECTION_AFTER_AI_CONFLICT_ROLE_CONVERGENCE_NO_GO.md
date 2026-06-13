# V1 Next Minimal Runtime Slice Selection After AI Conflict / Role Convergence NO-GO

## Scope

This package reselects the 19th minimal review-only runtime slice after `AI conflict / AI role convergence read-only status` returned `NO-GO: duplicate with DecisionResult status`.

This is selection only. It does not implement an endpoint, dashboard panel, Push behavior, Recheck behavior, Candidate behavior, Point behavior, external channel, order, execution, auto-trading, schema/config/pom change, new DTO / Validator / Assembler / Orchestrator, or new service/domain/mapper/repository ownership family.

Effective execution baseline:

- Previous merged package: PR #990.
- Actual main before this branch: `0700a44 docs(runtime): gate ai conflict status readiness (#990)`.
- Source of Truth baseline lag is not blocking; this package uses actual merged main as the effective execution baseline and updates source-of-truth docs inside this selection package.

## Selection Result

Selected next slice:

`Internal Push preview / notification preview status`

Next allowed action:

`Source Read for Internal Push preview / notification preview status`

Next branch:

`internal-push-preview-notification-preview-status-source-read`

## Why This Slice

This is the smallest remaining non-duplicate candidate with existing review-only assets and visible dashboard context.

Reusable evidence found during selection:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- `ReviewOnlyInternalPushPreviewAssemblerTest`
- `CandidatePushReviewOnlyMvpClosureTest`
- dashboard `internalPushPreviewDisplay`
- historical closure docs `PHASE_P303_PUSH_PREVIEW_BEFORE_EXTERNAL_CHANNEL_CLOSURE.md`
- historical closure docs `PHASE_P304_DASHBOARD_INTERNAL_PUSH_PREVIEW_DISPLAY_GATE_CLOSURE.md`
- no-op external channel safety policy assets such as `NoOpOpportunityPushExternalChannelPolicy`

The existing assets already express:

- review-only preview;
- manual review required;
- recheck required as a review condition, not recheck execution;
- Risk Action Guard required;
- fail-closed / blocked preservation;
- not trade instruction;
- external channel disabled;
- no Telegram / email / webhook / app notification / local notification connection;
- no readiness / point / entry / stop / TP / RR;
- no order / execution / auto-trading.

The next package must be source-read only. It must confirm whether these existing assets can support a future minimal runtime status without adding Push send, external channel, Recheck execution, Candidate generation, Point generation, or trading behavior.

## Candidate Comparison

| Candidate | Decision | Reason |
|---|---|---|
| Internal Push preview / notification preview status | Selected | Existing review-only DTO/assembler, dashboard display placeholder, closure tests, and disabled external-channel policy make this the smallest non-duplicate source-read target. It still needs strict source read because Push send / external channel / Recheck execution risks are high. |
| Recheck status / recheck preview | Rejected for now | `PushRecheckService`, `PushRecheckScheduler`, controller, log, and status update paths exist, but the direction is too close to recheck execution, replay, scheduler, and write-side state changes for the next minimal slice. |
| Candidate preview / ranking status | Rejected | Candidate preview assets exist, but the direction is close to Candidate generation, candidate ranking, Point generation, and entry/stop/TP/RR-adjacent semantics. |
| Position Monitor manual-input / monitor status | Rejected | Prior position-monitor scope docs show strong stop/reverse/moving-stop/close guidance risk. Too close to Position Monitor execution. |
| Macro-news / event calendar status | Rejected | External API refresh, news fetch, event generation, scheduler, and collector risks are higher than the selected internal preview source-read target. |
| Account risk downstream review-only display continuation | Rejected | Account risk / exposure status is already the 17th completed review-only runtime partial slice. Continuation risks reintroducing trading authorization, position sizing, or reduce/close/stop/reverse guidance. |
| Hot Reset / Event Impact downstream review-only display continuation | Rejected | Hot Reset / Event Impact Source status is already the 18th completed review-only runtime partial slice. Continuation risks Hot Reset execution/write, event generation, external refresh, and news fetch. |
| AI conflict continuation | Rejected by default | PR #990 returned `NO-GO: duplicate with DecisionResult status`. No AI conflict / role convergence implementation, endpoint, or panel may continue from that package. |
| Existing dashboard/system placeholder with review-only owner path | Not selected | No smaller non-duplicate placeholder was found during this selection that is safer than internal Push preview source read. |
| Other smaller review-only runtime slice | Not selected | The discovered no-op opportunity push safety family is adjacent to the selected internal Push preview direction and should be inventoried inside that source read rather than chosen as a separate immediate slice. |

## Selection Reason

`Internal Push preview / notification preview status` is selected because it is the strongest remaining candidate that:

- has existing owner/display assets;
- is already framed as review-only and manual-review-required;
- has clear fail-closed and disabled-channel semantics;
- can be investigated by source read without touching Java, tests, dashboard behavior, schema/config/pom, or external channels;
- avoids the #990 DecisionResult / AI conflict duplication boundary;
- avoids repeating completed Account risk and Hot Reset slices;
- remains smaller than Recheck execution, Candidate ranking, Position Monitor, or Macro-news/event-calendar work.

## Risk Notes

This slice is safe only as Source Read.

The next package must explicitly inventory and keep blocked:

- Push send;
- external channel;
- Telegram / email / webhook / app notification / local notification send;
- message rendering or sendable message generation;
- PushSnapshot write;
- Recheck execution;
- Replay execution;
- scheduler / collector / API client refresh;
- Candidate generation;
- candidate ranking / candidate score;
- Decision generation;
- Point generation;
- final direction;
- entry / stop / TP / RR;
- order / execution / auto-trading;
- Position Monitor execution.

If source read proves that a future status requires any of those behaviors, the next design/readiness path must return NO-GO.

## Next Source Read Must Cover

The next source read should inspect at minimum:

- `ReviewOnlyInternalPushPreviewDTO`
- `ReviewOnlyInternalPushPreviewAssembler`
- `ReviewOnlyInternalPushPreviewAssemblerTest`
- `CandidatePushReviewOnlyMvpClosureTest`
- dashboard `internalPushPreviewDisplay`
- `NoOpOpportunityPushExternalChannelPolicy`
- no-op opportunity push provider/channel/message/envelope/pipeline/persistence policy assets
- `PushSnapshotService`
- `PushSnapshotMapper`
- `TmPushSnapshotDO`
- `PushRecheckController`
- `PushRecheckService`
- `PushRecheckServiceImpl`
- `PushRecheckScheduler`
- `PushRecheckStatusContract`
- `PushRecheckDispatchConfigService`
- `PushRecheckLogMapper`
- historical P303/P304/P305 docs
- existing push/recheck/dashboard tests

The read must distinguish internal preview display evidence from Push send, notification send, external channel, and Recheck execution.

## Forbidden Scope

This selection package does not:

- change Java business code;
- change tests;
- change dashboard business logic;
- change schema/config/pom;
- add DTO / Validator / Assembler / Orchestrator;
- add service/domain/mapper/repository ownership family;
- implement Push preview status;
- implement notification preview status;
- send Push;
- connect external channel;
- execute Recheck / Replay;
- generate Candidate / Decision / Point;
- generate final direction / entry / stop / TP / RR;
- create order / execution / auto-trading behavior;
- execute Position Monitor;
- continue AI conflict after #990 NO-GO;
- continue P359 / P360;
- promote capability level.

## Capability Movement

No capability level movement.

Completed review-only runtime partial slices remain 18. The selected slice has not completed a runtime partial closure yet; it is only selected for Source Read.
