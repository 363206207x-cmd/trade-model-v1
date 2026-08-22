# B.1.2.2 Position Runtime Closure Evidence

- Runtime source head: `38b1faa7df3d374da25edc6b8723ad0297b198ae`
- Start head: `c698f2e3d6dd6397fc2a4fb6e15f8dc26f341d98`
- Mode: `UI_REVIEW_FIXTURE`
- External call count: `0`
- Browser viewport: `1280 x 720`
- Zoom: `100%`
- DPR: `2`
- Console error count: `0`
- Horizontal overflow count: `0`
- UI-review manual-close POST count: `0`

## Browser Evidence

| File | URL / state |
| --- | --- |
| `01-home-top3.png` | `/dashboard`; Home Top3 contains 7101, 7102, 7103 and no close action |
| `02-positions-active-list.png` | `/positions?tab=active`; all three active positions and no close action |
| `03-position-7101-detail.png` | `/positions/7101?returnTo=%2Fdashboard%3Fasset%3DBTCUSDT` |
| `04-position-7102-detail.png` | `/positions/7102?returnTo=%2Fdashboard%3Fasset%3DBTCUSDT` |
| `05-position-7103-detail.png` | `/positions/7103?returnTo=%2Fdashboard%3Fasset%3DBTCUSDT` |
| `06-active-detail-close-action.png` | Active 7103 detail; right-side close action visible and bound to 7103 |
| `07-o07-open-7101.png` | O07 open for BTCUSDT / 7101; no submit performed |
| `08-return-home-context.png` | Returned to `/dashboard?asset=BTCUSDT`; selected asset preserved |
| `09-network-dom-identity.md` | API, DOM identity, close safety, and normal-profile isolation record |

All PNG files are screenshots from the running application launched with
`./scripts/run-local.sh --ui-review` at the runtime source head above.
