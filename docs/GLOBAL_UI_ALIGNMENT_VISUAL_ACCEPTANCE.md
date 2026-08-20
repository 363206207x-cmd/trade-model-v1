# Global UI Alignment Visual Acceptance

Date: 2026-08-21
Runtime: `ui-review`, Java 17, local H2, external providers/AI/Telegram/schedulers disabled

## Home Geometry

| Evidence | Viewport | Top6 grid | Decision layout | Horizontal overflow | Text overflow |
|---|---:|---|---|---:|---:|
| `01-home-1440-top.png` | 1440 x 900 | 6 x 1, about 214.67px each | 918.40 / 393.59px | 0 | 0 |
| `02-home-1440-full.png` | 1440 full page | 6 x 1 | 7fr / 3fr | 0 | 0 |
| `03-home-1280.png` | 1280 x 900 | 3 x 2, 384px each | 806.40 / 345.59px | 0 | 0 |
| `04-home-1080.png` | 1080 x 900 | 3 x 2 | Plan at y=478, Position at y=738 | 0 | 0 |

The narrow decision layout preserves the required Plan then Position order.
The 1440 Position row resolves to 187.52 / 238.67 / 238.67 / 187.52px,
matching the frozen 22 / 28 / 28 / 22 semantic structure.

## Monitor Trust States

| Evidence | Trust state | Opening facts retained | Monitor-owned facts |
|---|---|---|---|
| `05-position-pending.png` | PENDING | Yes | `等待验证` |
| `06-position-stale.png` | STALE | Yes | `数据已过期` |
| `07-position-invalid.png` | INVALID | Yes | `来源无效` |
| `08-position-source-unavailable.png` | SOURCE_UNAVAILABLE | Yes | `来源不可用` |

All four states contain one real-position row and fail closed for mark price,
PnL, risk, judgment, conclusion, and suggested action.

## Three AI And Task Pages

- `09-ai-gpt.png`, `10-ai-gemini.png`, `11-ai-grok.png`: one workspace,
  one selected role, persistent resolver-owned Conflict Summary.
- ArrowLeft/ArrowRight and Home/End selection behavior passed with roving tab state.
- `12-positions.png`: frozen Positions IA, unique empty-state primary CTA.
- `13-analysis.png`: Preview-first analysis IA.
- `14-messages.png`: in-app messages only.
- `15-settings.png`: risk preference and asset-pool/data-source scope only.
- `16-plan-detail.png`: reusable focused detail shell.

## Runtime Checks

- Fresh browser console errors/warnings: **0**.
- UI-review external capability calls: **0**; all such capabilities are disabled
  by the profile and the rendered UI uses local application resources only.
- Whole-card risk coloring: **0**.
- Horizontal overflow: **0** across captured routes.
- Text overflow: **0** across captured routes.
- Current UI Telegram copy: **0**.
