# Fundamental AI v4.1 Final Interaction Evidence

Status: `IMPLEMENTED_PENDING_INDEPENDENT_PRODUCT_AUDIT`

Canonical Figma: `rdMYmsAvZYkXHJX8hdl7UN`

Canonical Home node: `573:20`

Current acceptance evidence node: `599:4307`

Historical reusable visual anchors: `28:154`, `31:23`, `520:212`,
`523:748`, `35:97`. Node `519:3` is the rejected old P1-KB baseline and
is not a runtime target.

## Visual Scenario Index

| ID | State | Evidence |
|---:|---|---|
| 01 | 未选择资产 | `desktop-home-1440x900.jpg` |
| 02 | 等待分析 | `analysis-no-data-1440x900.jpg` |
| 03 | PREPARATION | Figma state matrix + plan contract tests |
| 04 | OBSERVATION | Figma state matrix + plan contract tests |
| 05 | BLOCKED | Figma state matrix + fail-closed tests |
| 06 | CONFIRMATION | Figma state matrix + final-only tests |
| 07 | REDUCED | Figma state matrix + final-only tests |
| 08 | Candidate 有、Final 无 | `plan-unavailable-1440x900.jpg` |
| 09 | Final 有、AI 不可用 | Three-AI partial/unavailable contract tests |
| 10 | GPT Candidate | Canonical AI Workspace state |
| 11 | Before / After | `figma-runtime-home-1440x900-side-by-side.png` |
| 12 | 1440 x 900 | `runtime/12-desktop-first-viewport-1440x900.png` |
| 13 | Full page | `runtime/13-desktop-full-page.png` |

## Runtime Captures

- `desktop-home-1280x800.jpg`
- `desktop-home-1440x900.jpg`
- `desktop-home-1600x1000.jpg`
- `desktop-home-1728x1117.jpg`
- `asset-pool-1440x900.jpg`
- `positions-empty-1440x900.jpg`
- `analysis-no-data-1440x900.jpg`
- `messages-empty-1440x900.jpg`
- `plan-unavailable-1440x900.jpg`
- `settings-1440x900.jpg`
- `postgresql-v1-v13-validation.txt`

Post-authorization exact-head captures use PNG and include both viewport and
full-page evidence at `1280x800`, `1440x900`, `1600x1000` and `1728x1117`.
The 1440 route sweep also refreshes Asset Pool, Position empty, Analysis empty,
Message empty, Final unavailable and Settings evidence. The legacy JPG files
remain historical evidence and are not used as the exact-head browser result.

`runtime-state-contact-sheet.png` combines the routed-state captures. The
browser run used the authenticated local application with production endpoints;
empty and unavailable states are genuine fail-closed states, not market or AI
fixtures.

## Evidence Boundary

Screenshots prove layout, navigation, interaction, empty-state and fail-closed
behavior only. They do not claim live provider accuracy. No market, AI,
position, message or audit value was fabricated for visual completeness.
