# Fundamental AI v4.1 Desktop UI Visual System

## Product Direction

The Desktop Home is a restrained professional decision workspace: neutral surfaces, clear hierarchy, sparse semantic color, no dashboard-wall treatment, and no simulated market visualization.

## Tokens

| Token | Purpose |
|---|---|
| `--canvas` | application canvas |
| `--surface-primary` | primary module surface |
| `--surface-secondary` | grouped secondary content |
| `--surface-selected` | selected context |
| `--border-subtle` / `--border-strong` | hierarchy borders |
| `--text-primary` / `--text-secondary` / `--text-tertiary` | text hierarchy |
| `--accent-primary` | selection, links, primary command |
| `--state-positive` | trusted/success only |
| `--state-warning` | waiting/partial/fallback |
| `--state-danger` | blocked/invalid/high risk |
| `--state-neutral` | ordinary no-data state |
| `--state-unavailable` | unavailable/missing source |

## Geometry And Density

- Module radius: `10-12px`
- Normal module padding: `16px`
- Section gap: `20-24px`
- Position / Final width: approximately `70 / 30`
- AI Workspace / adjustment summary: approximately `80 / 20`
- Borders are subtle; large black outlines and nested-card walls are prohibited.

## Color Semantics

Green never means merely selected; it means trusted or positive. Yellow marks wait, partial, fallback, or caution. Red marks blocked, invalid, or high risk. Gray marks unavailable, missing, and no-data. Blue is reserved for selection, links, and the primary command.

## Progressive Disclosure

Primary surfaces contain the decision, current state, and user action boundary. Evidence lists show the first two items. Additional evidence and Provider/Trace/Fallback/raw-state metadata are collapsed. This keeps the Home scannable without deleting the complete role contract.

## Theme Verification

Light and dark themes use the same semantic hierarchy. Evidence: `docs/evidence/v4_1_productized_ui/runtime/19-desktop-light.png` and `20-desktop-dark.png`.
