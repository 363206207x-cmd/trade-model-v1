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
cookie, storage, or form submission. Capture mode performs one same-directory
request for the checked-in `field-map.json`; it makes no external request.

## Preview Parameters

| Parameter | Values | Purpose |
|---|---|---|
| `device` | `17pm`, `12pm` | Applies the measured device layout contract |
| `theme` | `light`, `dark` | Selects the local color token set |
| `text` | `standard`, `large` | Applies standard text or Dynamic Type +1 |
| `capture` | `1` | Hides desktop controls, fills the viewport, and renders safe empty-state copy while retaining exact `data-field-token` bindings |

Example:

```text
index.html?capture=1&device=17pm&theme=dark&text=standard
```

Use a `440 x 956` browser viewport for 17PM and a `428 x 926` viewport for
12PM. The corresponding measured safe areas are `62 / 0 / 34 / 0pt` and
`47 / 0 / 34 / 0pt`.

## Capture Readiness Gate

Capture tooling must wait for a terminal DOM contract state before taking a
screenshot:

```js
const terminalState = page.locator(
  'html[data-capture-contract="ready"], html[data-capture-contract="error"]'
);
await terminalState.waitFor({ state: "attached" });
if (await page.locator("html").getAttribute("data-capture-contract") !== "ready") {
  throw new Error("Capture contract is not ready");
}
```

The document marks capture mode as `loading` before the stylesheet is loaded,
so raw template tokens never receive a visible first paint. Successful
field-map normalization changes the state to `ready`. A load or validation
failure removes the template DOM, renders a bounded fail-closed unavailable
state, and changes the state to `error`; screenshot generation must stop.
`window.P3U2PrototypeReady` is also exposed as a convenience when the host
allows extending `window`, but the DOM attribute is the authoritative gate.

## Rendered Evidence

The checked-in viewport captures were rendered directly from `index.html`:

- `screenshots/iphone-17-pro-max-light.png`
- `screenshots/iphone-17-pro-max-dark.png`
- `screenshots/iphone-12-pro-max-light.png`
- `screenshots/iphone-12-pro-max-dark.png`
- `screenshots/iphone-17-pro-max-first-screen.png`
- `screenshots/iphone-17-pro-max-position-collapsed.png`
- `screenshots/iphone-17-pro-max-ai-collapsed.png`
- `screenshots/iphone-17-pro-max-large-text.png`

Both fixtures show one compact alert/event summary; native disclosure retains
capacity for a second row. Capture mode renders the exact per-field
`emptyState` from `field-map.json` instead of manufacturing market or AI
evidence. Every replaced value retains its exact path in `data-field-token`.
The 12PM panels stack independently rather than scaling the 17PM layout.

## Browser Acceptance Evidence

The localhost fixture was checked in the in-app browser with the real logical
viewports. Measurements are CSS pixels, which equal logical points for this
prototype.

| Check | Result |
|---|---|
| 17PM first screen | Header, seven-status band, alert/event, selected asset, complete execution compact summary, and the start of position monitoring visible |
| Execution core vs bottom nav | Core bottom `732.4pt`; bottom-nav top `861pt`; visible `YES` |
| Asset switch | `assets[1]` updates execution and AI; position markup remains byte-for-byte unchanged |
| AI role switch | Exactly one `tabpanel` visible |
| Home navigation | `.app-scroll` moved from `797.5` to `0`; focus returned to `#app-title` |
| Review navigation | Sanitized target `/review/dashboard`; fixture URL unchanged |
| Disclosures | Execution, position, consistency, and active-role disclosures each opened and closed |
| Accessibility structure | 3 radios; 0 listboxes; 0 options; 0 nested interactive controls |
| Touch targets | 18 visible targets checked; 0 below `44 x 44pt` |
| Horizontal scrolling | Page overflow `NONE`; only `.asset-pager` remains horizontally scrollable |
| CSS/resources | One stylesheet loaded with 138 CSSOM rules; local script loaded and executed without browser log errors |
| Long tokens | Longest visible normal-preview token: 51 characters; no page overflow at standard or large text |

Exact module measurements are recorded in
`../P3_U2_IPHONE_HOME_IA_V2.md`.

## Interaction Contract

- Select one of three watch assets through a radio group to update only the
  execution-advice and AI context labels. Every token uses `assets[index]`.
- Position-monitor markup remains unchanged when the selected asset changes.
- Select one AI role at a time through the segmented control.
- Expand native details blocks for complete execution, position, consistency,
  and role evidence.
- “首页” resets the internal `.app-scroll` owner and focuses the page title.
- “复盘” targets `/review/dashboard`; the fixture records the sanitized target
  without issuing a network request.
- No authenticated full-position page route currently exists. The view-all
  control is a disabled `CONTRACT_UNRESOLVED` design placeholder, not a false
  self-link.
- Switch device, theme, and text size from the desktop control panel.
- Use `document.P3U2Prototype.getState()` and
  `document.P3U2Prototype.runContractChecks()` for non-production browser checks
  when the host permits extending `document`. A `window.P3U2Prototype` alias is
  likewise exposed when the host permits extending `window`; locked-down hosts
  can assert the same DOM contract directly.

The watch pager contains one focusable control per card. It does not mix
`listbox`, `option`, and nested `summary` controls. All visible controls retain
at least a `44 x 44pt` target.

## Field Integrity

Normal preview mode displays `{fieldName}` structural tokens. Capture mode
uses safe empty-state copy and retains each exact path in `data-field-token`.
The visible `STATIC_LAYOUT_FIXTURE` marker confirms that the page contains no
live market data or generated business evidence.

Current source code confirms seven top status fields. The task's proposed
eighth field, `top.holdingRisk`, has no current backend field and is retained
as `UNRESOLVED` in `field-map.json`; it is deliberately not rendered.

`finalPlanMode` belongs to
`aiDecision.tabs[GPT_FINAL].finalPlanMode`. `ConsistencyVO` does not own that
field, so the consistency summary does not render it.

## Safety Boundaries

- Production Java changed: `NO`
- Production Swift changed: `NO`
- Dashboard template changed: `NO`
- PR #1134 changed: `NO`
- Network requests: `LOCAL_FIELD_MAP_ONLY`
- Provider or AI calls: `NONE`
- Orders, position mutation, Push, Telegram, or trading: `NONE`
- Production readiness: `BLOCKED`

The screenshots in `screenshots/` must be captured from this actual static
prototype. Generated or illustrative replacement images are not accepted.
