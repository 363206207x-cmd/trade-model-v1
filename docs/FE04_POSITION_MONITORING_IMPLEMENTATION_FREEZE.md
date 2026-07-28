# FE-04 Position Monitoring Implementation Freeze

## 1. Registration Status

| Item | Value |
|---|---|
| Record date | `2026-07-28` |
| Figma file | `Trade Model Design System` |
| Figma baseline | `FROZEN` |
| Semantic contract | `docs/design/FE04_SEMANTIC_CONTRACT_V2.md` |
| Frontend status | `NOT_STARTED` |
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

## 6. Delivery Gate

This registration is effective project governance on clean, synced `main`.
FE-04 frontend remains `NOT_STARTED`; design registration is not frontend
implementation evidence.

The next bounded implementation package is FE-04A + FE-04B:

- FE-04A: `Frontend Shell & Navigation`;
- FE-04B: `Home Dashboard Integration`.

The package must not implement later FE-04 screens, expand APIs, or move
trading capability.

```text
FE04_FIGMA_BASELINE: FROZEN
FE04_FIGMA_BASELINE_REGISTRATION: REGISTERED_ON_MAIN
FE04_FRONTEND_STATUS: NOT_STARTED
FE04_IMPLEMENTATION_ALLOWED: FE-04A_AND_FE-04B_ONLY
NEXT_IMPLEMENTATION_PACKAGE: FE-04A_AND_FE-04B_FRONTEND_IMPLEMENTATION
```
