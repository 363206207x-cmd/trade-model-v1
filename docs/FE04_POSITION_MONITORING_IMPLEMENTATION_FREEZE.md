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

## 6. Delivery Gate After FE-04C

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

FE-04D AI Analysis is the next separately gated package, but only its
read-only readiness audit is allowed after this governance alignment is merged
to clean, synced `main`. The audit must verify the frozen AI Analysis frame,
existing API/DTO capability, authoritative navigation identity, exactly three
AI roles, and all fail-closed states. FE-04D implementation remains blocked
until that audit explicitly passes.

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
FE04D_STATUS: NOT_STARTED_READINESS_GATE_PENDING
FE04_IMPLEMENTATION_ALLOWED: NONE_PENDING_FE04D_READINESS_GATE
NEXT_ALLOWED_ACTION: FE-04D_AI_ANALYSIS_READINESS_GATE_ONLY
```
