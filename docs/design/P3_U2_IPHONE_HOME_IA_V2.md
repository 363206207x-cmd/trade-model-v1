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
Status Grid
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
| App header | 56 | 64 | 80 | Grows for Dynamic Type; non-sticky |
| Status grid | 128 | 144 | 208 | Two text rows per cell; content wraps |
| Alert/event row | 120 | 144 | 208 | Two panels side by side; each row wraps |
| Watch heading + pager | 184 | 216 | 280 | Card is content-driven; local horizontal scroll |
| Execution advice | 248 | 360 | none | Definition list grows; long rows expand |
| Position monitor | 260 | 420 | none | Three summaries, then existing detail route |
| AI review + consistency | 340 | 520 | none | One role only; evidence grows vertically |
| Bottom navigation | 56 | 60 | 72 | Fixed, plus `34pt` safe-area inset |

### 17PM first-screen cutoff

At standard text size, the first viewport contains:

1. app header;
2. seven-status grid;
3. alert/event row;
4. selected watch asset card and the next-card edge;
5. execution-advice header/status and approximately the first two definition
   rows.

The execution module intentionally continues below the fold. Position and AI
are not compressed into the first screen.

### 17PM second-screen cutoff

The second viewport completes execution advice, shows up to three position
summaries, and reveals the AI-review heading/consistency summary. Full AI
evidence may continue into a third viewport when content is long or Dynamic
Type is increased.

## 12PM Height Budget

The portrait screen is `926pt`. Subtracting the measured safe areas leaves
`845pt`; reserving the `58-64pt` bottom control band leaves approximately
`781-787pt` for content.

| Module | Min | Default target | Default max | Adaptation |
|---|---:|---:|---:|---|
| App header | 52 | 56 | 80 | Tighter spacing, same text size |
| Status grid | 128 | 144 | 224 | Four columns, seven cells, wrapping labels |
| Realtime alert | 76 | 96 | 160 | Full-width above event; one row visible in the default fixture, capacity remains two |
| Key event | 76 | 96 | 160 | Full-width below alert; one row visible in the default fixture, capacity remains two |
| Watch heading + pager | 176 | 204 | 280 | Card width 91%; P1 fields in expansion |
| Execution advice | 248 | 360 | none | Same semantics; long rows collapse to 3 lines |
| Position monitor | 240 | 360 | none | Two summaries, then existing detail route |
| AI review + consistency | 340 | 520 | none | Segments wrap internally if Dynamic Type grows |
| Bottom navigation | 56 | 60 | 72 | Fixed, plus `34pt` safe-area inset |

### 12PM first-screen cutoff

The first viewport contains the app header, seven-status grid, one visible row
from each stacked alert/event summary, and the selected asset card. Each
summary keeps a two-row data capacity, but the secondary row is outside the
default 12PM density budget. The implementation must not shrink text to force
additional modules into view.

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
| 12PM asset P1 fields | Collapsed | Native details block reveals confidence/latest price |
| Execution long rows | Up to three lines | Native details reveals full token/content |
| Position P1 fields | Collapsed | Per-position details block |
| AI evidence lists | First responsibility fields visible | Native details reveals remaining evidence fields |
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

The prototype uses scroll snapping and explicit card buttons. It does not
display three narrow cards simultaneously.

## Scroll And Focus Order

VoiceOver/keyboard order follows visual order:

1. fixture/state header;
2. seven status values;
3. alert rows;
4. event rows;
5. asset heading and three asset selectors;
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
| Status layout | 4 columns / 7 truthful cells | 4 columns / 7 truthful cells |
| Alert/event | Side by side | Stacked |
| Watch card width | 86% content width | 91% content width |
| Default asset fields | P0 + P1 | P0; P1 expanded |
| Position summaries | Up to 3 | Up to 2 |
| AI roles visible | Exactly 1 | Exactly 1 |
| Horizontal page overflow | None | None |
| Bottom safe-area reservation | 34pt | 34pt |

## Prototype Interaction Contract

The static prototype implements only local layout behavior:

- device preview switch;
- light/dark switch;
- standard/large text switch;
- three-asset selection;
- execution and AI context-label update;
- one-role-at-a-time AI tabs;
- native long-text expansion;
- in-page bottom-navigation anchors.

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
