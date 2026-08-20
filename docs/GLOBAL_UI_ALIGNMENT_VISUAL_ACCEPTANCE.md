# Global UI Alignment Visual Acceptance

Date: 2026-08-21
Runtime: `ui-review`, Java 17, local H2, external providers/AI/Telegram/schedulers disabled
Baseline: `af58dc7e7ae6c33f87daf78986fbfafc538a8340`

## Owner Blocker Closure

| Gate | Result | Runtime evidence |
|---|---|---|
| OpportunityCard labels and values | PASS | `最终偏向` / `计划模式`; no ellipsis or clipping at 1440, 1280, 1080 |
| 1080 account summary | PASS | `极高·3笔·部分覆盖`, `scrollWidth == clientWidth` |
| Position trusted layout | PASS | 1440 grid resolves to 187.52 / 238.67 / 238.67 / 187.52px |
| Position fail-closed states | PASS | One state message; opening facts retained; monitor-owned fields absent |
| Lifecycle copy | PASS | Old direct-to-confirmation copy count 0; corrected copy appears in GPT and Final Plan |
| Shared AppShell | PASS | Home/task rails are dark 64px; task-page global status bar count 0 |
| Messages empty state | PASS | Exact approved title and explanation |
| Settings clean/dirty | PASS | Clean hidden+disabled; dirty visible, enabled, blue, unique primary action |
| Final lifecycle actions | PASS | CURRENT/NEEDS_REVALIDATION enabled; unavailable hidden+disabled |
| Browser console errors | PASS | 0 |

## Home Geometry

| Evidence | Viewport | Top6 grid | Decision layout | Horizontal overflow | Critical clipping |
|---|---:|---|---|---:|---:|
| `home-1440-top.png` | 1440 x 900 | 6 x 1, about 214.67px each | 918.40 / 393.59px | 0 | 0 |
| `home-1280.png` | 1280 x 900 | 3 x 2, 384px each | 806.40 / 345.59px | 0 | 0 |
| `home-1080.png` | 1080 x 900 | 3 x 2 | responsive stacked modules | 0 | 0 |

The approved Home module order and responsive behavior are unchanged. The
collapsed rail exposes icons only; user-visible rail label count is 0.

## Monitor Trust States

| Evidence | State | Opening facts | State message count | Mark/PnL | Judgment/Conclusion |
|---|---|---|---:|---|---|
| `position-verified-fresh.png` | VERIFIED + FRESH | retained | 0 | visible | visible |
| `position-pending.png` | PENDING | retained | 1: `等待监控数据` | absent | absent |
| `position-stale.png` | STALE | retained | 1: `监控数据已过期` | absent | absent |
| `position-invalid.png` | INVALID | retained | 1: `当前不可查看` | absent | absent |
| `position-source-unavailable.png` | SOURCE_UNAVAILABLE | retained | 1: `监控来源不可用` | absent | absent |

## Task Pages And Lifecycle

- `gpt-home.png`: Result -> Why -> Evidence -> Candidate Summary, with
  resolver-owned Conflict Summary and corrected lifecycle copy.
- `positions-page.png`: no copied Home SystemStatusBar; compact empty-state CTA.
- `messages-page.png`: exact high-value-message empty state; no Telegram CTA.
- `settings-clean.png` and `settings-dirty.png`: clean/dirty save-button contract.
- `plan-current-final.png`: validated CURRENT Final with complete fields.
- `plan-needs-revalidation.png`: validated NEEDS_REVALIDATION state.
- `plan-unavailable.png`: invalid ID fail closed; revalidation hidden; Candidate
  does not populate Final Detail.

All 16 PNG files are stored under
`docs/evidence/global_ui_alignment/owner_blocker_closure/`.
The browser-read DOM measurements are stored alongside them in
`browser-qa.json` and are locked by the focused UI contract suite.

## Runtime DOM Assertions

- Critical text: `scrollWidth <= clientWidth` and computed
  `text-overflow != ellipsis` at 1440, 1280, and 1080.
- Nontrusted Position rows contain only `开仓价` and `开仓时间` facts plus one
  exact trust-state message; judgment and conclusion node counts are 0.
- Task-page SystemStatusBar count is 0 and inactive task indicator is hidden.
- Settings clean/dirty visibility, disabled state, class, and primary color were
  asserted after a real change event.
- Plan action visibility and enabled state were asserted against CURRENT,
  NEEDS_REVALIDATION, and unavailable lifecycle responses.
- Horizontal document overflow and browser console error counts are 0.

TEXT_CLIPPING_COUNT: 0
OWNER_VISUAL_BLOCKERS: 0
