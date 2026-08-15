# Fundamental AI v4.1 Canonical Home Replacement Report

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

## Production Route

`GET /dashboard` now renders `workspace.html` with `data-page="home"`. The
superseded `dashboard.html` visual system is not a selectable production Home
path. Existing Dashboard APIs remain the data owners.

## Frozen Home Contract

| Contract | Current implementation |
|---|---|
| Primary navigation | exactly Home, Position, AI Analysis, Message, My |
| Secondary navigation | Asset Pool, Event Calendar, System Status |
| System status | one six-segment strip |
| Alerts/events | zero height when empty; compact rows when present |
| Dynamic Top6 | 3 columns x 2 rows at 1440; only actual opportunities |
| Zero opportunity | toolbar plus compact fail-closed empty state |
| Position / Plan | 60:40; stacks only below 1040px main-container width |
| AI / consistency | 76:24; one role visible at a time |
| Charts/fake data | none |

The selected asset updates the Final Plan and Three-AI context only. System
status, alerts, events and user positions keep their independent ownership.

## Evidence

- Before: `docs/evidence/v4_1_productized_ui/runtime/01-before-first-viewport.png`
  from the superseded candidate visual.
- After 1440x900:
  `docs/evidence/v4_1_final_p1_remediation/runtime/latest-authenticated-home-1440x900.png`.
- After full page:
  `docs/evidence/v4_1_final_p1_remediation/runtime/home-full-page-1440.png`.
- Zero opportunity:
  `docs/evidence/v4_1_final_p1_remediation/runtime/home-zero-opportunity-1440x900.png`.
- Additional widths: `home-1280x800.png`, `home-1600x1000.png`, and
  `home-1728x1117.png` in the same directory.

All current captures are authenticated runtime output. They are visual
evidence only and do not claim live-provider success.

## Canonical Figma

- File: `rdMYmsAvZYkXHJX8hdl7UN`
- Home frame: `573:20`
- Side navigation: `615:712`
- Six-segment status: `611:716`
- Top6 compact state: `612:712`
- Desktop state page: `559:7`

The Home product frame has zero visible component-taxonomy labels and zero
detached instances. Exact state nodes are listed in the Figma node map.

`LEGACY_HOME_PRODUCTION_PATH_REMOVED = PASS`

`SINGLE_DESKTOP_VISUAL_SYSTEM = PASS`
