# Fundamental AI v4.1 Visual Density and Proportion Contract

Status: `FROZEN_IMPLEMENTATION_CONTRACT`

Canonical Product Source:
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

This contract records the approved Desktop composition measurements used by
Canonical Figma and runtime acceptance. It is subordinate to the canonical
Product Source and does not add business fields, owners, routes, or actions.

## 1. Desktop Baseline

| Property | Contract |
|---|---|
| Acceptance viewport | `1440 x 900` |
| Supported validation viewports | `1280 x 800`, `1600 x 1000`, `1728 x 1117` |
| Sidebar | `224px`; allowed range `216-232px` |
| Main grid | 12 columns |
| Grid gutter | `16px` |
| Main padding | `24px` |
| Spacing system | `8px` base |
| Page header | `48-56px` |

Layouts must not compress typography to preserve a ratio. When usable main
width is below `1040px`, the Position/Plan pair stacks while remaining a
Desktop surface. This contract does not authorize Mobile implementation.

## 2. Home Vertical Rhythm

| Section | Contract |
|---|---|
| System status | `56-64px`; hard maximum `72px` |
| Alert/Event | empty `0px`; with content `40-48px` |
| Section gap | `16-24px`, aligned to the 8px system |
| Card internal padding | `16px` default, `12px` compact |

The first viewport prioritizes system context, dynamic opportunities and a
clear entry into the Position/Final decision area. Empty sections collapse;
they do not reserve decorative space.

## 3. Dynamic Top6

- Dynamic Top6 is the first visual-priority business section.
- Layout is `3 columns x 2 rows` at the 1440px baseline.
- Card height is `120-136px`; hard maximum `148px`.
- Zero eligible opportunities uses one compact state of `120-144px`.
- Fewer than six opportunities renders the real count only.
- Cards contain no chart, mini trend, K-line, fabricated price, or fake fill.
- One slot represents one asset and preserves primary/secondary timeframe
  lineage and conflict state.

## 4. Position and Final Plan

| Region | Target | Allowed |
|---|---:|---:|
| Position Monitoring | 60% | 58-62% |
| Final Execution Plan | 40% | 38-42% |

Position Monitoring is a real UserPosition summary. Final Execution Plan is a
validated system recommendation. The two regions remain visually and
semantically independent. Plan values cannot masquerade as position facts.

## 5. Three AI and Conflict

| Region | Target | Allowed |
|---|---:|---:|
| Single Three-AI workspace | 76% | 72-80% |
| Conflict summary | 24% | 20-28% |

Only one AI role is visible at a time. Home shows at most three to four list
entries per role group; full arrays enter the analysis or audit detail. AI
Consistency remains a compact non-voting summary and may be shorter than the
workspace.

## 6. Color, Type, Number, and Action Hierarchy

- At least 90% of the surface is neutral.
- Unavailable, pending, stale, and unknown states never use success green.
- Red and green do not fill complete cards.
- Financial values use tabular figures and right alignment where compared.
- Raw enum values are not primary user-facing copy.
- Each page state has at most one solid primary action.
- Missing data is represented by exact empty, stale, unavailable, partial, or
  failed state text; it is never replaced by a successful-looking default.

## 7. Layout Quality Gates

The following must be zero at every required Desktop viewport:

- horizontal overflow;
- text overflow;
- incoherent overlap;
- stale selected-asset content;
- fabricated runtime value;
- multiple visible AI roles;
- more than one solid primary action per page state.

Canonical Figma and runtime screenshots must use the same route, state,
viewport, and data provenance before visual delta is evaluated.
