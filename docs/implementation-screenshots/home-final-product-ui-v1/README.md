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
| `iphone-large-complete-light.jpg` | 440x956 | READY / Light | Plan status, direction, confidence, risk, and entry summary above navigation |
| `iphone-large-complete-dark.jpg` | 440x956 | READY / Dark | Same first-screen hierarchy with dark contrast |
| `iphone-12-pro-max-complete-light.jpg` | 428x926 | READY / Light | Compact plan disclosure remains above navigation |
| `iphone-12-pro-max-complete-dark.jpg` | 428x926 | READY / Dark | Same compact hierarchy with dark contrast |
| `iphone-12-pro-max-top2-positions-light.jpg` | 428x926 | READY / Light | Default Top2 UserPosition summaries with details collapsed |
| `iphone-12-pro-max-top2-positions-dark.jpg` | 428x926 | READY / Dark | Default Top2 disclosure density with dark contrast |
| `iphone-large-partial-light.jpg` | 440x956 | PARTIAL | Partial data is explicit and plan remains fail closed |
| `iphone-large-missing-light.jpg` | 440x956 | MISSING | Missing asset context clears plan fields |
| `iphone-large-retry-light.jpg` | 440x956 | ERROR | Retry is visible; DOM test records recovery to READY |
| `iphone-12-pro-max-long-content-light.jpg` | 428x926 | READY / Long content | Text wrapping and no page-level overflow |

## DOM Acceptance

- Module order: system/asset status, alerts/events, focus assets, Execution Plan, responsive Top2/Top3 UserPosition, Three AI.
- Asset cards expose seven core fields and four subordinate status fields.
- Exact Execution Plan access remains fail closed for PARTIAL, MISSING, and ERROR.
- Execution Plan defaults to direction, asset confidence, asset risk, status, and entry summary; boundaries and provenance remain in a native disclosure.
- UserPosition defaults to compact summaries; 428px shows Top2 and 440px preserves Top3, independently of selected asset context.
- Asset, system, plan, and AI consistency state labels carry explicit scopes.
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
- Mobile uses a contained horizontal asset rail, collapsed secondary system status, compact decision disclosures, fixed five-item navigation, and device-specific position counts.
- Error, Missing, and Partial states remain visible and never borrow stale successful values.

## Figma

`SYNC_PENDING`: repository node IDs exist, but no resolvable Figma file key or node-specific URL is available in the workspace. This does not replace or weaken the formal product and semantic contracts used for this implementation.
