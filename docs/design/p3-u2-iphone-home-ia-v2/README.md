# P3-U2 iPhone Home IA v2 Prototype

This directory contains a static, browser-rendered wireframe for the P3-U2
iPhone home information architecture. It is a design contract, not an iOS or
Dashboard implementation.

## Open Locally

Serve the repository root with any local static HTTP server, then open:

```text
/docs/design/p3-u2-iphone-home-ia-v2/index.html
```

The prototype has no dependency installation, build step, CDN, backend call,
cookie, storage, or form submission.

## Preview Parameters

| Parameter | Values | Purpose |
|---|---|---|
| `device` | `17pm`, `12pm` | Applies the measured device layout contract |
| `theme` | `light`, `dark` | Selects the local color token set |
| `text` | `standard`, `large` | Applies standard text or Dynamic Type +1 |
| `capture` | `1` | Hides desktop controls and fills the browser viewport |

Example:

```text
index.html?capture=1&device=17pm&theme=dark&text=standard
```

Use a `440 x 956` browser viewport for 17PM and a `428 x 926` viewport for
12PM. The corresponding measured safe areas are `62 / 0 / 34 / 0pt` and
`47 / 0 / 34 / 0pt`.

## Rendered Evidence

The checked-in viewport captures were rendered directly from `index.html`:

- `screenshots/iphone-17-pro-max-light.png`
- `screenshots/iphone-17-pro-max-dark.png`
- `screenshots/iphone-12-pro-max-light.png`
- `screenshots/iphone-12-pro-max-dark.png`

The 12PM fixture shows one row in each stacked alert/event panel to preserve
the first-screen asset task; the contract and DOM retain a maximum capacity of
two rows. This is responsive density behavior, not whole-page scaling.

## Interaction Contract

- Select one of three watch assets to update only the execution-advice and AI
  context labels.
- Position-monitor markup remains unchanged when the selected asset changes.
- Select one AI role at a time through the segmented control.
- Expand native details blocks for long fields and secondary evidence.
- Switch device, theme, and text size from the desktop control panel.
- Use `window.P3U2Prototype.getState()` for non-production browser checks.

## Field Integrity

Every displayed value is a `{fieldName}` structural token. The visible
`STATIC_LAYOUT_FIXTURE` marker confirms that the page contains no live market
data or generated business evidence.

Current source code confirms seven top status fields. The task's proposed
eighth field, `top.holdingRisk`, has no current backend field and is retained
as `UNRESOLVED` in `field-map.json`; it is deliberately not rendered.

## Safety Boundaries

- Production Java changed: `NO`
- Production Swift changed: `NO`
- Dashboard template changed: `NO`
- PR #1134 changed: `NO`
- Network requests: `NONE`
- Provider or AI calls: `NONE`
- Orders, position mutation, Push, Telegram, or trading: `NONE`
- Production readiness: `BLOCKED`

The screenshots in `screenshots/` must be captured from this actual static
prototype. Generated or illustrative replacement images are not accepted.
