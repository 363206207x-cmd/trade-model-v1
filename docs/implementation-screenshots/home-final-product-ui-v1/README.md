# Home Final Product UI V1 - Visual Acceptance

## Evidence Source

- Rendered from the production Desktop and Mobile Home templates.
- Served by `scripts/dashboard-visual-acceptance-fixture.py` with deterministic read-only responses.
- Production default data is unchanged; fixtures only select response states.
- Detailed DOM assertions and viewport records are in `visual-acceptance.json`.

## Desktop

| Screenshot | Viewport | State | Acceptance focus |
|---|---:|---|---|
| `desktop-complete-light.jpg` | 1440x1000 | READY / Light | Primary decision hierarchy and first-fold density |
| `desktop-complete-dark.jpg` | 1440x1000 | READY / Dark | Dark surfaces, contrast, and status hierarchy |
| `desktop-partial-light.jpg` | 1440x1000 | PARTIAL | Data-quality warning and fail-closed plan |
| `desktop-missing-light.jpg` | 1440x1000 | MISSING | Missing context without stale success data |
| `desktop-confused-high-risk-light.jpg` | 1440x1000 | READY / Confused | Confused and high-risk prominence |
| `desktop-plan-unavailable-light.jpg` | 1440x1000 | Plan MISSING | Unavailable exact plan is not presented as advice |
| `desktop-top3-positions-light.jpg` | 1440x1000 | READY | Top3 UserPosition remains separate from the plan |
| `desktop-viewport-1440x1000-light.jpg` | 1440x1000 | READY | Reference desktop viewport |

## Mobile

| Screenshot | Viewport | State | Acceptance focus |
|---|---:|---|---|
| `iphone-large-complete-light.jpg` | 440x956 | READY / Light | iPhone Large product hierarchy |
| `iphone-large-complete-dark.jpg` | 440x956 | READY / Dark | iPhone Large dark contrast |
| `iphone-12-pro-max-complete-light.jpg` | 428x926 | READY / Light | 12 Pro Max density and 91% asset card |
| `iphone-12-pro-max-complete-dark.jpg` | 428x926 | READY / Dark | 12 Pro Max dark hierarchy |
| `iphone-large-partial-light.jpg` | 440x956 | PARTIAL | Partial data is explicit and plan remains fail closed |
| `iphone-large-missing-light.jpg` | 440x956 | MISSING | Missing asset context clears plan fields |
| `iphone-large-retry-light.jpg` | 440x956 | ERROR | Retry is visible; DOM test records recovery to READY |
| `iphone-12-pro-max-long-content-light.jpg` | 428x926 | READY / Long content | Text wrapping and no page-level overflow |

## DOM Acceptance

- Module order: system/asset status, alerts/events, focus assets, Execution Plan, Top3 UserPosition, Three AI.
- Asset cards expose seven core fields and four subordinate status fields.
- Exact Execution Plan access remains fail closed for PARTIAL, MISSING, and ERROR.
- Top3 UserPosition is independent of selected asset context.
- Both mobile viewports have five navigation items and a 44px minimum visible control height.
- Mobile visible text has a 12px minimum computed size.
- Desktop and mobile document roots and bodies have no page-level horizontal overflow.
- Light and dark themes preserve risk and state contrast.
- `prefers-reduced-motion` disables nonessential transitions and animation.

## Visual Review

- The selected asset and decision summary form one primary status surface instead of a grid of equal-weight cards.
- Alerts and events use a compact operational band, preserving space for the asset and plan workflow.
- Execution Plan is the primary decision module; UserPosition is visually parallel but semantically independent.
- Core asset numbers lead; data quality, multi-timeframe state, Confused, update time, and source badges stay secondary.
- Mobile uses a contained horizontal asset rail, compact status composition, fixed five-item navigation, and device-specific card widths.
- Error, Missing, and Partial states remain visible and never borrow stale successful values.

## Figma

`SYNC_PENDING`: repository node IDs exist, but no resolvable Figma file key or node-specific URL is available in the workspace. This does not replace or weaken the formal product and semantic contracts used for this implementation.
