# Fundamental AI v4.1 B.1.2.1 Opportunity Selection Evidence

Implementation base: `820795152ed27ff42536977c438839a00d240412`.
Final implementation Head CI is recorded in the PR handoff after push.

This is isolated `UI_REVIEW_FIXTURE` evidence, not production or live-provider
proof. It reruns IA-01 only. Preview and Recheck were not executed.

## Closure status

| Gate | Status |
|---|---|
| `B12-P1-01` | `CLOSED_BY_OWNER_FREEZE_EXCEPTION` |
| `CARD_VISIBLE_CURRENT_LABEL_REQUIRED` | `NO` |
| `CARD_VISIBLE_CURRENT_LABEL_COUNT` | `0` |
| `PAGEHEADER_CURRENT_ASSET` | `PASS` |
| `ARIA_PRESSED` | `PASS` |
| `B12-P1-02` | `CLOSED` |
| `B12-P1-02_ROOT_CAUSE` | `UI_REVIEW_FIXTURE_STALE_IDENTITY` |
| `PRODUCTION_STALE_IDENTITY` | `NOT_VERIFIED` |
| `IA-01` | `PASS` |
| `IA-02` | `PASS_PRESERVED_NOT_RERUN` |
| `IA-03` | `NOT_VERIFIED_NOT_EXECUTED` |

## DOM and runtime table

| State | Selected symbol | URL asset | pressed=true | pressed=false | Card `当前` count | PageHeader | analysisId / traceId | Audit href | Console errors | Widths |
|---|---|---|---:|---:|---:|---|---|---|---:|---|
| Initial | BTCUSDT | absent, defaults BTC | 1 | 5 | 0 | `当前资产 · BTCUSDT` | BTC role identity retained | `/audit/ui-review-trace-btc-gpt_final?...` | 0 | 1440 / 1440 / 1440 |
| ETH selected | ETHUSDT | ETHUSDT | 1 | 5 | 0 | `当前资产 · ETHUSDT` | `null / null` for every unavailable role | none; `审计链尚未形成` | 0 | 1440 / 1440 / 1440 |
| ETH refreshed | ETHUSDT | ETHUSDT | 1 | 5 | 0 | `当前资产 · ETHUSDT` | `null / null` for every unavailable role | none; `审计链尚未形成` | 0 | 1440 / 1440 / 1440 |

Browser zoom was 100%, CSS viewport was 1440x900, and DPR was 2.
Horizontal overflow was zero. The Opportunity State badge and risk value
remained independent, including the formal `HIGH_RISK` fixture state.

## Identity table

| Asset | Role state | resultAvailable | analysisId | traceId | Visible audit state |
|---|---|---:|---|---|---|
| BTCUSDT | READY | true | `ui-review-analysis-btc` | legitimate per-role BTC trace | `查看完整审计链` |
| ETHUSDT and every other non-BTC asset | UNAVAILABLE | false | null | null | `审计链尚未形成`, no href |

The non-BTC result is proved by `UiReviewDashboardHomeServiceTest`; browser
evidence proves the rendered no-link state. No production stale-identity claim
is made.

## Selection and navigation

- Mouse selection changed BTC -> ETH with one selected outline and one
  `aria-pressed=true`.
- Keyboard Enter selected BTC; Space selected ETH.
- Refresh restored ETH from `/dashboard?asset=ETHUSDT`.
- Back landed on `/dashboard` with the BTC default matching the URL; Forward
  landed on `/dashboard?asset=ETHUSDT` with ETH selected. Neither action opened
  a legacy `/dashboard/*-detail` route.
- `replaceState` remains unchanged; no synthetic per-selection history entry
  was added.

## Network ledger

| Action | Method | Route | Status | Result |
|---|---|---|---:|---|
| Initial Home | GET | `/dashboard`, `/api/dashboard/home` | 200 | BTC selected |
| Select ETH | GET | `/api/dashboard/home?asset=ETHUSDT` | 200 | ETH selected; no write |
| Refresh ETH | GET | `/dashboard?asset=ETHUSDT`, `/api/dashboard/home?asset=ETHUSDT` | 200 | ETH restored |
| Keyboard Enter / Space | GET | `/api/dashboard/home?asset=BTCUSDT`, then ETHUSDT | 200 | Accessible selection preserved |

## Screenshot manifest

| File | Route / state | Viewport | Classification |
|---|---|---:|---|
| `01-btc-initial-home.png` | `/dashboard`, BTC selected | 1440x900 | `UI_REVIEW_FIXTURE` |
| `02-eth-selected-home.png` | `/dashboard?asset=ETHUSDT` | 1440x900 | `UI_REVIEW_FIXTURE` |
| `03-eth-refreshed-home.png` | same route after refresh | 1440x900 | `UI_REVIEW_FIXTURE` |
| `04-eth-audit-not-formed.png` | ETH AI workspace, no audit target | 1440x900 | `UI_REVIEW_FIXTURE` |

All screenshots are real 1440x900 PNGs transcoded from uncropped in-app
browser viewport captures without resize, crop, zoom, or content edits.

## Explicit boundary

- `MESSAGE_OPEN=NOT_EXECUTED`
- `RECHECK_BIND=NOT_EXECUTED`
- `RECHECK_F5=NOT_EXECUTED`
- `RECHECK_REFRESH=NOT_EXECUTED`
- `RECHECK_RETRY=NOT_EXECUTED`
- `RECHECK_ROW_DELTA=NOT_VERIFIED`
- Visible card-level `当前` badge added: NO.
- PageHeader current asset retained: YES.
- Knife C started: NO.
- PR merged: NO.
- `CURRENT_PHASE_DONE=NO`.
