# FE-04 Position Monitoring Implementation Freeze

## 1. Registration Status

| Item | Value |
|---|---|
| Record date | `2026-07-28` |
| Figma file | `Trade Model Design System` |
| Figma baseline | `FROZEN` |
| Semantic contract | `docs/design/FE04_SEMANTIC_CONTRACT_V2.md` |
| Frontend status | `IN_PROGRESS_PARTIAL` |
| Governance effectivity | `REGISTERED_ON_MAIN` |
| Code/API/schema/Figma change in this package | `NONE` |
| Capability movement | `NONE` |

The Figma design is already frozen. This document registers its exact node
identity in repository governance. Registration does not implement a page,
make a partial API complete, authorize a new API, or mark FE-04 `DONE`.

## 2. Frozen Frames

### Mobile

| Screen | Frame ID |
|---|---|
| Home | `296:2` |
| Position Monitor | `296:3` |
| AI Analysis | `296:4` |
| Message Center | `296:5` |
| Push Detail | `296:6` |
| Profile | `296:7` |

### Desktop

| Screen | Frame ID |
|---|---|
| Dashboard | `296:8` |
| Position Monitoring | `296:9` |
| AI Analysis | `296:10` |
| Message Center | `296:11` |
| Profile | `296:12` |

## 3. Frozen Components

| Component | Node ID |
|---|---|
| Asset Card | `28:154` |
| Execution Plan Card | `31:23` |
| AI Role Card | `35:97` |
| Position Monitor Card | `32:26` |
| Message Card | `299:54` |
| Push Detail Card | `300:234` |

## 4. Frozen Navigation

Mobile Navigation V2 contains exactly five primary entries:

1. 首页
2. 持仓
3. AI分析
4. 消息
5. 我的

The Asset Card body changes only the selected asset context. Analysis Detail
navigation requires the asset's authoritative `analysisId`. Position remains an
independent UserPosition context and must not change with asset selection.
Review remains contextual and is not a sixth primary entry.

## 5. Implementation Boundary

The registered baseline covers the five-tab mobile shell, desktop navigation,
Home integration, Position Monitoring, AI Analysis entry, Message Center,
Push Detail, and Profile. Runtime implementation must remain within the
capabilities already classified by the frontend contract audit.

The following remain mandatory:

- `ExecutionPlan != UserPosition`;
- exact `positionId`, authoritative `analysisId`, and owner-scoped reads;
- exactly `GPT_FINAL`, `GEMINI_REVIEW`, and `GROK_CHALLENGE`;
- unavailable search, watch persistence, message data, Push detail, or settings
  remain disabled or fail closed;
- no fabricated AI content, evidence, scores, timeframe data, messages,
  monitoring history, or settings-save result;
- no auto-open, auto-close, auto-reverse, auto-reduce, order, or trading action.

## 6. Delivery Gate Through FE-04D First Package

This registration is effective project governance on clean, synced `main`.
FE-04A Shell & Navigation and FE-04B Home Dashboard Integration are effective
on merged main `aaf905b4f74ecafcf514aa34d7c06361461a0eb4`
through PR #1146. FE-04C read-only Position Monitoring is effective on merged
main `cc39f0c6315812b1178427c29b8b422da511ba0d` through PR #1148.
Overall FE-04 remains `IN_PROGRESS_PARTIAL`; these merged slices do not prove
FE-04 complete.

The effective FE-04C capability is limited to:

- Mobile Position Monitoring;
- Desktop Position Monitoring;
- the frozen Position Monitor Card projection;
- exact string-preserved `positionId`;
- owner-scoped GET-only position, summary, and monitor-log reads;
- existing monitor-log display;
- loading, empty, error, partial, missing, and fail-closed states.

The implementation inherits the P3-H3 ownership foundation and uses exact
`positionId` with no symbol, latest-position, time, or numeric-coercion
fallback. Monitor-log read failure remains unavailable and must not be
represented as `WAITING_MONITOR`. A unified read DTO, closed-position history,
and human-handling history remain `PARTIAL` and stay fail closed.

FE-04C does not add edit, manual close, partial-close UI, replay, trade
execution, automatic action, or an automatic call to the write-type
monitor-run endpoint. It does not expand APIs or move trading capability.

The FE-04D AI Analysis first package is effective on merged main
`66362746fe3bd932061087bcd3496c5273cc218b` through PR #1151. Its implemented
scope is limited to:

- the AI Analysis tab using the registered Mobile `296:4` and Desktop `296:10`
  frames;
- the existing selected-asset context;
- exact authoritative nullable `analysisId` navigation;
- returned summaries for exactly `GPT_FINAL`, `GEMINI_REVIEW`, and
  `GROK_CHALLENGE`;
- FE-03 Analysis Detail reuse by exact `analysisId`;
- loading, empty, error, partial, missing, and fail-closed states.

It also isolates unavailable AI roles, preserves loading/empty/error/partial/
missing fail-closed states, and prevents stale Dashboard cache from restoring
AI, execution-plan, or FE-03-link state after refresh failure.

It does not implement market-asset search, watch-asset writes, fake search
results, fabricated scores, evidence, multi-timeframe data, or AI content, AI
capability expansion, API/schema/Figma changes, external send, or trading
capability. The FE-04E Message/Push Contract Foundation is
`EFFECTIVE_MERGED_MAIN` on
`5ad8ddb24a8253180b3e2b0a34fec66b9928ace8` through PR #1154.

The FE-04E UI readiness re-evaluation exposed a server-side P1 privacy
prerequisite. PR #1155 now contains an
`OPPORTUNITY_PUBLIC_PROJECTION` candidate and is pending exact-head review.
Message/Push UI remains `NOT_STARTED` and is not authorized to begin before
this candidate is reviewed, merged to clean/synced main, and governance is
re-evaluated.

`OPPORTUNITY` is frozen as
`AUTHENTICATED_SHARED_PUBLIC_PROJECTION`: only exact message/source/opportunity
identity, safe allowlisted public opportunity status, public timestamp, and
public description may cross the API boundary. The server projection does not
select or serialize UserPosition data, account risk, position risk, Recheck
risk fields, `failReasonJson`, or private risk reasons. Frontend filtering is
not a privacy boundary.

`POSITION_RISK` is frozen as `OWNER_SCOPED_PRIVATE_PROJECTION`. It may expose
current-user position risk, monitoring risk, and private risk reason only
through exact owner-scoped reads. Public and private source records must remain
different DTO variants.

System notifications, Telegram, external send, automatic notification,
fabricated unread/message counts or message/Push data, mutation, backend/API/
schema/Figma change, AI expansion, and trading capability remain blocked.
Figma example values without a real returned field must be hidden or rendered
fail closed.

```text
FE04_FIGMA_BASELINE: FROZEN
FE04_FIGMA_BASELINE_REGISTRATION: REGISTERED_ON_MAIN
FE04_FRONTEND_STATUS: IN_PROGRESS_PARTIAL
FE04A_STATUS: EFFECTIVE_MERGED_MAIN
FE04B_STATUS: EFFECTIVE_MERGED_MAIN
FE04_AB_MERGE_COMMIT: aaf905b4f74ecafcf514aa34d7c06361461a0eb4
FE04C_STATUS: EFFECTIVE_MERGED_MAIN
FE04C_MERGE_COMMIT: cc39f0c6315812b1178427c29b8b422da511ba0d
FE04C_CAPABILITY: READONLY_POSITION_MONITORING_ONLY
FE04D_STATUS: EFFECTIVE_MERGED_MAIN
FE04D_IMPLEMENTATION_STATUS: COMPLETE_FIRST_PACKAGE
FE04D_MERGE_COMMIT: 66362746fe3bd932061087bcd3496c5273cc218b
FE04D_AUTHORIZATION: FIRST_PACKAGE_EFFECTIVE_NO_EXPANSION
FE04D_SEARCH_STATUS: PARTIAL_DISABLED_IN_FIRST_PACKAGE
FE04D_WATCH_ASSET_STATUS: BLOCKED_NO_AUTHENTICATED_WRITE_CONTRACT
FE04D_API_READINESS: PARTIAL_FAIL_CLOSED
FE04E_STATUS: CONTRACT_FOUNDATION_EFFECTIVE_MERGED_MAIN
FE04E_CONTRACT_MAIN_HEAD: 5ad8ddb24a8253180b3e2b0a34fec66b9928ace8
FE04E_IMPLEMENTATION_STATUS: UI_NOT_STARTED
FE04E_UI_READINESS_STATUS: PASS
FE04E_UI_STATUS: NOT_STARTED_BLOCKED_PENDING_PUBLIC_PROJECTION_MERGED_MAIN
FE04E_NEXT_PACKAGE: OPPORTUNITY_PUBLIC_PROJECTION_EXACT_HEAD_REVIEW
FE04E_AUTHORIZATION: EXACT_HEAD_REVIEW_ONLY
FE04E_MESSAGE_SOURCE_STATUS: PASS_OPPORTUNITY_AND_POSITION_RISK_ONLY
FE04E_OPPORTUNITY_ACCESS: AUTHENTICATED_SHARED_PUBLIC_PROJECTION
FE04E_OPPORTUNITY_CONTRACT: AUTHENTICATED_SHARED_PUBLIC_PROJECTION
FE04E_OPPORTUNITY_UI_PROJECTION: SERVER_SIDE_PUBLIC_OPPORTUNITY_ALLOWLIST
FE04E_OPPORTUNITY_FORBIDDEN_FIELDS: USER_POSITION_ACCOUNT_RISK_POSITION_RISK_FAIL_REASON_JSON_PRIVATE_RISK_REASON
FE04E_POSITION_RISK_ACCESS: OWNER_SCOPED_PRIVATE_PROJECTION_CROSS_USER_BLOCKED
FE04E_MESSAGE_IDENTITY_STATUS: PASS_STRING_SAFE_AUTHORITATIVE_IDENTITY
FE04E_PUSH_DETAIL_STATUS: SOURCE_SPECIFIC_PUBLIC_PRIVATE_PROJECTION_PENDING_EXACT_HEAD_REVIEW
FE04E_TELEGRAM_BOUNDARY_STATUS: PASS_EXTENSION_NOT_CONNECTED
FE04E_API_READINESS: PUBLIC_PROJECTION_IMPLEMENTED_PENDING_EXACT_HEAD_REVIEW
FE04E_BACKEND_PRIVACY_SANITIZATION: SERVER_SIDE_PUBLIC_PROJECTION_IMPLEMENTED_PENDING_REVIEW
FE04E_FIGMA_STATUS: PASS_REGISTERED_BASELINE
FE04E_FAIL_CLOSED_STATUS: PASS_EXPLICIT_READY_EMPTY_ERROR_MISSING_PARTIAL
FE04E_CAPABILITY_BOUNDARY_STATUS: PASS_NO_SEND_NO_TRADING
FE04_IMPLEMENTATION_ALLOWED: false
NEXT_ALLOWED_ACTION: FE-04E_OPPORTUNITY_PUBLIC_PROJECTION_EXACT_HEAD_REVIEW_ONLY
```
