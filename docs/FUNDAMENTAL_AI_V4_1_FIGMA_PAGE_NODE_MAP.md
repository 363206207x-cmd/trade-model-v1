# Fundamental AI v4.1 Canonical Figma Page And Node Map

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

File key: `rdMYmsAvZYkXHJX8hdl7UN`. No second Figma file was created.

## Canonical Pages

| Page | Node |
|---|---|
| 00 Cover / Source | `559:2` |
| 01 Foundations | `559:3` |
| 02 Components | `559:4` |
| 03 Overlays | `559:5` |
| 04 Desktop Pages | `559:6` |
| 05 Desktop States | `559:7` |
| 06 Prototype Flows | `559:8` |
| 07 Acceptance Evidence | `559:9` |
| 99 Historical / Superseded | `559:10` |

## Routed Desktop Frames

| Route | Node |
|---|---|
| R01 Login | `572:2` |
| R02 Home | `573:20` |
| R03 Asset Pool | `578:140` |
| R04 Position Center | `579:194` |
| R05 Position Detail | `580:242` |
| R06 Review Center | `581:290` |
| R07 Review Detail | `581:2731` |
| R08 AI Analysis | `581:2790` |
| R09 Message Center | `582:452` |
| R10 Push Recheck | `583:506` |
| R11 Final Plan Detail | `584:560` |
| R12 Event Calendar | `584:3030` |
| R13 Full Audit Chain | `584:3089` |
| R14 My / Settings | `584:3154` |

## Canonical Home Nodes

- final SideNav: `615:712`
- primary nav rows: `615:715` through `615:719`
- primary nav labels: `616:3`, `616:5`, `616:7`, `616:9`, `616:11`
- secondary navigation: `616:13`
- safety footer: `616:17`
- six-segment status strip: `611:716`
- status cells: `611:717` through `611:722`
- compact Top6 empty state: `612:712`

Superseded SideNav/System Status nodes are hidden. Component taxonomy labels
are hidden on the final Home and state product frames.

## Required Eighteen Desktop States

| State | Node |
|---|---|
| READY_WITH_TOP6 | `618:842` |
| ZERO_OPPORTUNITY | `618:1024` |
| FEWER_THAN_SIX | `618:1110` |
| WAITING_DATA | `618:1304` |
| SOURCE_UNAVAILABLE | `618:1390` |
| SELECTED_ASSET_EXITED_TOP6 | `618:1476` |
| CONFIRMATION | `619:1150` |
| PREPARATION | `619:1264` |
| REDUCED | `619:1378` |
| OBSERVATION | `619:1492` |
| BLOCKED | `619:1606` |
| CONFUSED | `619:1720` |
| HOT_RESET | `619:1862` |
| POSITION_TOP3 | `619:5442` |
| NO_POSITION | `619:5548` |
| AI_READY | `619:5654` |
| AI_PARTIAL | `619:5790` |
| AI_UNAVAILABLE | `619:5926` |

State count: `18/18`; detached instances: `0`; missing state names: `0`.

## Shared Overlays And Components

Overlays: `O01 568:2`, `O02 568:25`, `O03 568:55`, `O04 568:78`,
`O05 568:108`, `O06 568:131`, `O07 568:154`, `O08 568:177`,
`O09 568:200`, `O10 568:223`, `O11 568:246`.

The existing 54 component families on page `559:4` remain the owners. No
production instance named `TMV1/...` was detached or replaced with an ad-hoc
frame during the remediation.

## Acceptance Evidence

- Acceptance group: `599:4307`
- controlled acceptance instances: `599:4308`, `599:4315`, `599:4322`
- Scenario: `SCN-V41-04`
- provenance: `BROWSER_CONTROLLED`
- statement: controlled fixture, not production/live provider evidence

The READY cards contain controlled scenario values only. No controlled value
is the production default and no simulated chart exists.
