# V1 Next Minimal Runtime Slice Selection After RiskActionGuard

## 1. Executive Summary

Current merged main: `bab2325 docs(risk): record risk action guard visual closure (#935)`.

This package selects the next minimal `REVIEW_ONLY_RUNTIME partial` slice after RiskActionGuard read-only status closure. It is selection only: no Java business code, tests, dashboard business logic, schema/config/pom, endpoint, panel, Push, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO/Validator/Assembler/Orchestrator, Position Monitor execution, replay/recheck, P359, or P360.

Completed review-only runtime slices: 11.

Selected next slice: `Alert fatigue / notification policy status`.

Next allowed action: `Source Read for Alert fatigue / notification policy status`.

Next branch: `alert-fatigue-notification-policy-status-source-read`.

Capability movement: none. The project remains `REVIEW_ONLY_RUNTIME partial`.

## 2. Completed Runtime Slices

| # | Completed slice | Capability |
|---:|---|---|
| 1 | PositionSync + Dashboard review-only status | REVIEW_ONLY_RUNTIME partial |
| 2 | Watchlist + RuleConfig + Dashboard/API review-only status | REVIEW_ONLY_RUNTIME partial |
| 3 | MarketQuote freshness / fallback / dashboard API status | REVIEW_ONLY_RUNTIME partial |
| 4 | Evidence / Score review-only runtime status | REVIEW_ONLY_RUNTIME partial |
| 5 | DecisionResult review-only dashboard/API status | REVIEW_ONLY_RUNTIME partial |
| 6 | ExecutionPlan / BoundaryCandidate review-only runtime status | REVIEW_ONLY_RUNTIME partial |
| 7 | Review / Replay result status | REVIEW_ONLY_RUNTIME partial |
| 8 | Data Source Health dashboard/API status | REVIEW_ONLY_RUNTIME partial |
| 9 | RuleConfig runtime audit / rule explainability | REVIEW_ONLY_RUNTIME partial |
| 10 | Missed Opportunity / Review Archive status | REVIEW_ONLY_RUNTIME partial |
| 11 | RiskActionGuard read-only status | REVIEW_ONLY_RUNTIME partial |

All completed slices are review-only. None is Production Wiring, Push, Candidate generation, Decision generation, Point generation, order/execution, or auto-trading.

## 3. Candidate Slice Comparison

| Candidate slice | Existing assets | User-visible value | Runtime/API readiness | Risk | Recommendation |
|---|---|---|---|---|---|
| Position Monitor manual-input / monitor status | `MonitorService`, position sync history, dashboard alert foundations, older Position Monitor docs | Useful, but action wording can drift into close/reverse/open/move stop guidance | Partial; source read would need strict execution boundary review | High, because it is close to position execution semantics | Defer |
| Internal Push preview / recheck status | `ReviewOnlyInternalPushPreviewDTO`, Push recheck service/log/schema, dashboard preview display | User-visible, but crosses Push/recheck boundary | Existing assets are real but coupled to Push preview/recheck semantics | High, because Push external-channel boundary is nearby | Defer |
| Candidate preview / ranking status | Candidate preview guard DTO/assembler/tests and historical candidate preview docs | User-visible, but names imply candidate/ranking | Existing assets exist but are candidate-adjacent | High, because Candidate generation/ranking semantics are explicitly frozen | Defer |
| Three AI / AI conflict status | dashboard AI conflict KPI, `aiRoleResults`, DecisionResult AI role fields | Useful status surface | Partial; provider orchestration and final-bias semantics need careful separation | Medium/high due provider and decision-language coupling | Defer |
| SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate | SourceTrace/detail adapters, runtime kline context card, source-health snippets, source-binding skeleton history | Useful quality picture | Existing pieces are spread across source context and point-adjacent skeletons | Medium/high because many assets are point/source-binding skeletons | Defer |
| Account risk / system health / macro-news status | `/api/system/health`, `SystemHealthService`, account-risk snapshot assets, macro/news event evidence | Useful operations view | System health is already endpoint-level; account/macro assets are mixed with Push/recheck or external context | Medium; account/macro can imply trading readiness or external refresh | Defer |
| Alert fatigue / notification policy status | `tm_monitor_alert`, `MonitorAlertMapper`, `MonitorService.getRecentAlerts`, `MonitorAlertWriteServiceImpl` cooldown/suppression logic, dashboard alert center, review page alert rendering, `alert-explain.js` | High: lets users see whether alerts are being suppressed/throttled without sending anything | Good source-read candidate; existing owner path and dashboard copy already exist | Lowest among candidates if strictly read-only and no Push send | Select |
| Other smaller safer discovered slice | No smaller lower-risk owner path was found during this selection pass | N/A | N/A | N/A | Not selected |

## 4. Selected Next Slice

Selected slice: `Alert fatigue / notification policy status`.

Owner-path candidates for source read:

- `src/main/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImpl.java`
- `src/main/java/org/example/trademodel/service/MonitorService.java`
- `src/main/java/org/example/trademodel/service/impl/MonitorServiceImpl.java`
- `src/main/java/org/example/trademodel/mapper/MonitorAlertMapper.java`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/static/js/alert-explain.js`
- review page alert rendering assets, if present
- `tm_monitor_alert` schema references, only for inventory in source read

Why this slice now:

- It is smaller than Position Monitor execution or account-risk expansion.
- It has existing alert owner assets, including recent-alert reads and suppression/cooldown state.
- It is user-visible through dashboard alert surfaces and review page alert rendering.
- It can be framed as status-only: recent alert count, open/suppressed counts, cooldown/suppression visibility, policy availability, and fail-closed unknown states.
- It does not require Push send, external channel, Candidate generation, Decision generation, Point generation, or trading actions.
- It naturally follows RiskActionGuard because both are guardrail/visibility surfaces, but this slice remains notification-policy status, not executable risk action.

## 5. Rejected Options

Position Monitor manual-input / monitor status is deferred because even manual-input/monitor wording can drift into reduce/close/reverse/open/move-stop action semantics. It should not be the next slice until alert-policy read-only visibility is closed.

Internal Push preview / recheck status is deferred because it sits next to Push send and recheck execution. Source read may be needed later, but the next minimal slice should not reopen external-channel risk.

Candidate preview / ranking status is deferred because Candidate generation and ranking semantics are explicitly frozen. A status-only slice would still need careful proof that it does not become a candidate pool.

Three AI / AI conflict status is deferred because provider role/conflict fields are useful but close to decision-language and final-bias semantics.

SourceTrace / RuntimeKline / DataQuality / MultiTimeframe aggregate is deferred because it overlaps point/source-binding skeleton history and could accidentally revive DTO/Validator/Assembler or point-generation tracks.

Account risk / system health / macro-news status is deferred because system health already has an endpoint, while account and macro/news surfaces risk external refresh or trading-readiness interpretation.

## 6. Next Step Definition

Next action: `Source Read for Alert fatigue / notification policy status`.

Next branch: `alert-fatigue-notification-policy-status-source-read`.

Risk: A.

Allowed changes for next action:

- source-read docs
- source-of-truth docs

Forbidden in next action:

- Java business code
- tests
- dashboard business logic
- schema/config/pom
- endpoint/panel implementation
- external API refresh
- scheduler/collector/API client trigger
- Push send or external channel
- Candidate generation
- Decision generation
- Point generation
- final direction
- entry/stop/TP/RR
- order/execution/auto-trading
- new DTO/Validator/Assembler/Orchestrator
- Position Monitor execution
- replay/recheck execution
- P359/P360

## 7. Source Read Questions

The next source read must answer:

- Which `MonitorAlert` owner path is canonical for alert fatigue / notification policy status?
- Which fields represent open, suppressed, cooldown, suppression reason, type, severity, and freshness?
- Is `MonitorService.getRecentAlerts` sufficient as a read-only source, or is a dedicated status endpoint needed later?
- Can dashboard alert center / review page alert rendering be reused without new dashboard business logic?
- Does any status surface imply Push send, external notification, order action, or trading instruction?
- What fail-closed states are possible when alert owner data is missing, stale, or ambiguous?
- Are new DTO / Validator / Assembler / schema changes avoidable?

## 8. Freeze Rule Compliance

- New skeleton created: No
- Cursor-era / V1 assets reused: Yes, via existing monitor alert and dashboard alert owner paths
- Duplicate reduction: Yes, by selecting an existing alert owner path before adding any new surface
- Capability level movement: No, selection only
- Service/runtime/dashboard/API connected: No, selection only
- #830 audit recommendation compliance: Yes

## 9. Final Recommendation

GO to `Source Read for Alert fatigue / notification policy status`.

The next package must remain source-read only. It must prove whether the existing monitor alert owner path can support a minimal review-only notification-policy status without Push send, external channel, Candidate generation, Decision generation, Point generation, trading action, schema/config/pom, or new DTO/Validator/Assembler/Orchestrator.
