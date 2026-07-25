# P3-U2 iPhone Home IA v2

## Scope

This document freezes module order, responsive behavior, height envelopes,
scroll ownership, and safe-area treatment for the P3-U2 static wireframe. It
does not authorize production implementation.

## Measured Device Geometry

Measurements were taken from UIKit inside the installed iOS 26.5
CoreSimulator runtime. Pixel dimensions from each Simulator device profile
were divided by its scale and checked against `UIScreen.main.bounds` and the
root view's `safeAreaInsets`.

| Device | Logical portrait size | Safe area top/right/bottom/left | Role |
|---|---|---|---|
| iPhone 17 Pro Max | `440 x 956pt` | `62 / 0 / 34 / 0pt` | Primary |
| iPhone 12 Pro Max | `428 x 926pt` | `47 / 0 / 34 / 0pt` | Secondary |

The prototype uses these measured values only in device-preview mode. A future
production WebView must continue to consume native safe-area insets rather
than hard-code model names.

## Information Order

```text
App Header
Status Band
Realtime Alert / Key Event
Watch Asset Pager (3 assets)
Execution Advice (selected asset)
Position Monitor (independent)
AI Evidence Review + Consistency (selected asset)
Fixed Bottom Navigation
```

The page has one vertical scroll owner. The watch pager is the only horizontal
scroll area. AI evidence and long text expand within normal document flow.

## 17PM Height Budget

The portrait screen is `956pt`. Subtracting the measured top (`62pt`) and
bottom (`34pt`) safe areas leaves `860pt`. The fixed navigation's visible
control band is `58-64pt`, leaving about `796-802pt` for the first content
viewport. These are target envelopes, not fixed heights.

| Module | Min | Default target | Default max | Growth rule |
|---|---:|---:|---:|---|
| App header | 50 | 56 | 72 | Grows for Dynamic Type; non-sticky |
| Status band | 82 | 104 | 132 | Seven text values; no per-value card surface |
| Alert/event row | 80 | 96 | 136 | Two compact disclosures side by side |
| Watch heading + pager | 160 | 184 | 224 | One radio-group card; local horizontal scroll |
| Execution advice | 140 | 184 | none | Compact status/direction/entry; complete fields expand |
| Position monitor | 360 | 520 | none | Three compact disclosures; full-page route unresolved |
| AI review + consistency | 400 | 560 | none | Compact consistency and one role; complete fields expand |
| Bottom navigation | 56 | 60 | 72 | Fixed, plus `34pt` safe-area inset |

### 17PM first-screen cutoff

At standard text size, the first viewport contains:

1. app header;
2. seven-status band;
3. alert/event row;
4. selected watch asset card and the next-card edge;
5. execution-advice title, backend status, direction, and entry zone.

The execution disclosure may continue below the fold, but a title-only cutoff
is a failure. The reviewed fixture fits the complete collapsed execution
summary and the beginning of position monitoring above the bottom navigation.
Position details and AI are not compressed into the first screen.

### 17PM second-screen cutoff

The second viewport completes execution advice and begins the compact position
list. Full position and AI evidence continues in document flow; its default
height is reduced through disclosure rather than type scaling.

## 12PM Height Budget

The portrait screen is `926pt`. Subtracting the measured safe areas leaves
`845pt`; reserving the `58-64pt` bottom control band leaves approximately
`781-787pt` for content.

| Module | Min | Default target | Default max | Adaptation |
|---|---:|---:|---:|---|
| App header | 48 | 54 | 72 | Tighter spacing, same text size |
| Status band | 82 | 104 | 148 | Four columns, seven truthful values |
| Realtime alert | 56 | 72 | 128 | Full-width compact disclosure above event |
| Key event | 56 | 72 | 128 | Full-width compact disclosure below alert |
| Watch heading + pager | 160 | 184 | 232 | Card width 91%; one radio control |
| Execution advice | 140 | 184 | none | Same compact summary; complete fields expand |
| Position monitor | 260 | 360 | none | Two compact disclosures; route unresolved |
| AI review + consistency | 400 | 560 | none | Segments wrap internally if Dynamic Type grows |
| Bottom navigation | 56 | 60 | 72 | Fixed, plus `34pt` safe-area inset |

### 12PM first-screen cutoff

The first viewport contains the app header, seven-status band, one compact
disclosure from each stacked alert/event panel, and the selected asset card.
Each disclosure keeps two-row data capacity. The implementation must not
shrink text to force additional modules into view.

### 12PM second-screen cutoff

The second viewport completes execution advice and shows up to two position
summaries. AI begins below that point. This is an intentional reflow, not a
scaled copy of 17PM.

## Module Sizing Rules

1. Use intrinsic content height, `min-height`, and layout gaps based on the 8pt
   grid.
2. No module receives a fixed height that can clip Dynamic Type.
3. Summary cards use an 8px maximum corner radius.
4. Body text starts at 17px; supporting text starts at 13-15px and never drops
   below 13px in the static prototype.
5. Interactive targets are at least 44 x 44pt.
6. Long Chinese text uses normal line breaks. Long enum/code tokens use
   `overflow-wrap:anywhere` without ellipsis.
7. No desktop table appears at any breakpoint.

## Expand/Collapse Strategy

| Content | Default | Expanded behavior |
|---|---|---|
| Asset selector | P0 + P1 in one radio control | No nested interactive expansion |
| Execution advice | Status, direction, entry zone | Native details reveals every remaining confirmed field |
| Position fields | Risk, entry logic, direction support, reversal, current advice | Per-position details reveals remaining confirmed fields |
| AI evidence | Compact role conclusion plus 1-2 evidence fields | Native details reveals every remaining role field |
| Consistency score/asset block | Compact text summary | Details block; score remains `--` if missing |

Expansion never changes business state and never requests new data.

## Sticky Decisions

- Bottom navigation: **fixed** and padded by the measured/actual bottom safe
  area.
- App header: **not sticky**; preserving content height outranks persistent
  branding.
- Status grid: **not sticky**.
- Asset pager: **not sticky**.
- AI segmented control: **not page-sticky** in v2; it remains at the role
  section top to avoid covering evidence while scrolling.
- No nested vertical scroll containers.

## Watch Pager Geometry

### 17PM

- page horizontal padding: `16pt`;
- content width: `408pt`;
- selected card width target: `351pt` (`86%` of content width);
- inter-card gap: `12pt`;
- next card edge remains visible;
- three cards total.

### 12PM

- page horizontal padding: `16pt`;
- content width: `396pt`;
- selected card width target: `360pt` (`91%` of content width);
- inter-card gap: `12pt`;
- P1 fields move to expansion rather than reducing font size.

The prototype uses scroll snapping and one accessible radio button per card.
It does not display three narrow cards simultaneously or nest a disclosure in
the selection control.

## Scroll And Focus Order

VoiceOver/keyboard order follows visual order:

1. fixture/state header;
2. seven status values;
3. alert rows;
4. event rows;
5. asset heading and one three-item radio group;
6. execution advice;
7. independent positions;
8. consistency summary;
9. AI role segmented control and active role fields;
10. bottom navigation.

When an asset is selected, focus remains on the selected asset control. The
execution and AI context labels update through an `aria-live="polite"` region.
Position markup and text are not replaced.

## Light And Dark Tokens

The palette is neutral and operational:

- light background: cool gray-white;
- dark background: near black with neutral elevated surfaces;
- blue: selected/navigation context;
- red: explicit high-risk/block state only;
- amber: warning/partial state only;
- green: explicit connected/valid state only.

No gradient, orb, decorative illustration, heavy shadow, or dominant
single-hue theme is used.

## Dynamic Type

The prototype's “放大一档” control increases the root text scale to 112.5%.
Validation passes only when:

- all module heights grow with content;
- no field overlaps another;
- status labels and enum tokens wrap;
- segmented controls retain 44pt targets;
- page width remains bounded;
- fixed navigation does not cover the final content.

## Responsive Acceptance Lines

| Check | 17PM | 12PM |
|---|---|---|
| Whole-page scaling | Forbidden | Forbidden |
| Status layout | 4 columns / 7 truthful band values | 4 columns / 7 truthful band values |
| Alert/event | Side by side | Stacked |
| Watch card width | 86% content width | 91% content width |
| Default asset fields | P0 + P1 | P0 + P1 |
| Position summaries | Up to 3 | Up to 2 |
| AI roles visible | Exactly 1 | Exactly 1 |
| Horizontal page overflow | None | None |
| Bottom safe-area reservation | 34pt | 34pt |

## Rendered Measurement Evidence

The measurements below come from the checked-in localhost fixture in capture
mode at standard text size. Capture mode uses truthful empty states and retains
each exact binding in `data-field-token`; it does not use whole-page scaling.

| Module/check | 17PM Light | 17PM Dark | 12PM Light | 12PM Dark |
|---|---:|---:|---:|---:|
| Header height | `68.3pt` | `68.3pt` | `68.3pt` | `68.3pt` |
| Top-status height | `132.6pt` | `132.6pt` | `132.6pt` | `132.6pt` |
| Alert/event height | `95.6pt` | `95.6pt` | `178.2pt` | `178.2pt` |
| Watch-asset height | `204.4pt` | `204.4pt` | `204.4pt` | `204.4pt` |
| Execution default height | `234.4pt` | `234.4pt` | `234.4pt` | `234.4pt` |
| Position default height | `865.2pt` | `865.2pt` | `610.1pt` | `610.1pt` |
| AI default height | `733.4pt` | `733.4pt` | `733.4pt` | `733.4pt` |
| Execution top Y | `563.0pt` | `563.0pt` | `630.6pt` | `630.6pt` |
| Bottom-nav top Y | `861.0pt` | `861.0pt` | `831.0pt` | `831.0pt` |
| Execution core above nav | `YES` | `YES` | `YES` | `YES` |
| Horizontal page overflow | `NONE` | `NONE` | `NONE` | `NONE` |
| Visible position summaries | `3` | `3` | `2` | `2` |

At Dynamic Type +1, 17PM remains overflow-free with its execution core above
the nav. 12PM remains overflow-free and preserves every field in document
flow; it is not required to force the execution core into that smaller first
viewport. All visible controls remain at least `44 x 44pt` in both sizes.

## Prototype Interaction Contract

The static prototype implements only local layout behavior:

- device preview switch;
- light/dark switch;
- standard/large text switch;
- three-asset radio selection;
- execution and AI context-label update;
- one-role-at-a-time AI tabs;
- native long-text expansion;
- home scroll-owner reset;
- in-page position navigation;
- sanitized `/review/dashboard` target capture.

It performs zero network requests and contains no form submission, storage,
cookie, Session, CSRF, Provider, AI, Telegram, Push, order, position mutation,
or trading path.

## Delivery Boundaries

- Foundation PR #1134 remains at
  `168ef18c7ad148d960902c913f6ddb4b53318e14`.
- Production Swift changed: `NO`.
- Production Java changed: `NO`.
- Dashboard template changed: `NO`.
- Real iPhone validation: `NOT_RUN`.
- Production readiness: `BLOCKED`.
