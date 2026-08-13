# Fundamental AI v4.1 Runtime Visual Validation

## Validation Boundary

This report covers the PR `#1179` Execution Plan semantic remediation on Desktop Home. The controlled browser uses the current production template and static assets through a read-only fixture. The actual-runtime check uses the authenticated Spring application. External providers and all schedulers were disabled; no live-provider result is claimed.

## Controlled Browser Validation

All 13 required evidence groups were reviewed at `1440 x 900`. Group 11 contains the required before/after pair.

| Gate | Result |
|---|---|
| Required scenario groups | `13 / 13 PASS` |
| Plan modes | `CONFIRMATION / PREPARATION / REDUCED / OBSERVATION / BLOCKED PASS` |
| Missing-Final states | `UNSELECTED / WAITING / CANDIDATE_ONLY PASS` |
| Candidate / Final isolation | PASS |
| Final with AI unavailable | PASS |
| Horizontal overflow | `0` |
| Text overflow | `0` |
| Top-level overlap | `0` |
| Console errors / warnings | `0 / 0` |
| Visible AI roles | `1` |
| Visible disclaimer copy | `0` |
| Raw enum in primary UI | `0` |
| Fake values or charts | `0` |

### Mode Semantics

- `PREPARATION` renders `等待触发` as a formal Final and does not become a no-plan state.
- `OBSERVATION` renders `当前仅观察` without entry, stop, target, default leverage, or default position values.
- `BLOCKED` renders `当前已阻断` with block/recovery semantics and no executable-looking price sections.
- `CONFIRMATION` renders all five structured sections.
- `REDUCED` renders the full plan and emphasizes downgrade, limits, and recovery.

### Asset-Switch Isolation

The controlled browser switched from BTC to ETH and verified:

```text
SYSTEM_STATUS_UNCHANGED=true
ALERTS_AND_EVENTS_UNCHANGED=true
POSITIONS_UNCHANGED=true
EXECUTION_PLAN_UPDATED=true
AI_WORKSPACE_UPDATED=true
STALE_ASSET_CONTENT_COUNT=0
```

## Actual Spring Runtime

The current branch started on `127.0.0.1:18802` with authentication enabled and a throwaway local user.

| Check | Result |
|---|---|
| login page / authenticated login | HTTP `200 / 302` |
| authenticated `/dashboard` | HTTP `200`, `716941` bytes |
| authenticated `/api/dashboard/home` | HTTP `200` |
| `/actuator/health` | `UP` |
| served CSS / semantic mapper | exact worktree SHA-256 match |
| in-app browser at `1440 x 900` | PASS |
| title / workspace | `执行计划` / one AI workspace |
| horizontal / text overflow | `0 / 0` |
| visible disclaimer / binary worth-opening | `0 / 0` |
| console errors / warnings | `0 / 0` |

The supplemental actual-runtime screenshot is `docs/evidence/v4_1_execution_plan_semantics/runtime/14-actual-spring-runtime.png`.

## Evidence

- Index: `docs/evidence/v4_1_execution_plan_semantics/README.md`
- Machine-readable results: `docs/evidence/v4_1_execution_plan_semantics/browser-qa.json`
- Full page: `docs/evidence/v4_1_execution_plan_semantics/runtime/13-desktop-full-page.png`

## Acceptance Boundary

Execution Plan semantic and browser/runtime validation is PASS for this candidate. Authenticated live-provider data acceptance was not part of this run and remains a separate target-runtime gate. The PR remains Draft and unmerged pending independent exact-Head frontend audit.
