# v4.1 Latest Approved Desktop UI Evidence

## Provenance

- Branch: `codex/v4-1-frontend-runtime-alignment`
- PR: `#1179` (Draft)
- Starting implementation head: `490919d6f8c763ffaac634cfbffd02ad8eaf66c4`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Runtime source: current `dashboard.html`, `dashboard-latest.css`, and `frontend-contract.js`
- Runtime source provenance: SHA-256 values in `browser-qa.json`, recomputed by the frontend contract test
- Runtime transport: local read-only `dashboard-visual-acceptance-fixture.py`
- External calls: `0`
- Runtime writes: rejected
- Primary viewport: `1440 x 900`
- Captured: `2026-08-13T18:32:31+08:00`

The runtime images are current-code browser captures. `Starting implementation head` identifies lineage; exact source hashes identify the working-tree source loaded for capture and remain valid across later documentation-only commits. The controlled fixture supplies deterministic contract states and is not live market, provider, AI, or trading evidence. The frontend does not invent replacement values for missing backend data.

## Figma Sources

| File | Figma node |
|---|---|
| `figma/asset-card-set-28-154.png` | `28:154` |
| `figma/execution-plan-card-31-23.png` | `31:23` |
| `figma/position-row-set-520-212.png` | `520:212` |
| `figma/position-state-set-523-748.png` | `523:748` |
| `figma/three-ai-set-35-97.png` | `35:97` (`35:4`, `35:35`, `35:66`) |

Node `519:3` is the rejected old P1-KB baseline and was not used as the implementation target.

## Runtime Index

| # | File | Evidence |
|---|---|---|
| 01 | `runtime/01-desktop-1440x900-light.png` | Light first viewport |
| 02 | `runtime/02-desktop-full-page-light.png` | Full Desktop Home, 1440 x 2453 |
| 03 | `runtime/03-desktop-dark.png` | Dark theme |
| 04 | `runtime/04-dynamic-top6-six.png` | Six authoritative ranked assets |
| 05 | `runtime/05-dynamic-top6-less-than-six.png` | Five assets, no default fill |
| 06 | `runtime/06-search-input.png` | Real search input and suggestions |
| 07 | `runtime/07-asset-pool-open.png` | Asset Pool open, ten managed assets |
| 08 | `runtime/08-position-no-position.png` | No Position |
| 09 | `runtime/09-position-open-top3.png` | Open Position Top3 |
| 10 | `runtime/10-execution-final.png` | Validated Final Plan |
| 11 | `runtime/11-execution-blocked.png` | Blocked/non-Final fail closed |
| 12 | `runtime/12-gpt-tab.png` | GPT_FINAL |
| 13 | `runtime/13-gemini-tab.png` | GEMINI_REVIEW |
| 14 | `runtime/14-grok-tab.png` | GROK_CHALLENGE |
| 15 | `runtime/15-ai-consistency.png` | Compact consistency summary |
| 16 | `runtime/16-ai-partial-failure.png` | AI timeout/partial failure |
| 17 | `runtime/17-empty-evidence.png` | Empty structured evidence |
| 18 | `runtime/18-asset-switch-no-stale.png` | BTC to ETH context switch without stale content |
| 19 | `runtime/19-figma-vs-implementation.png` | Approved Figma component sources vs browser runtime |
| 20 | `runtime/20-before-after.png` | Previous candidate vs latest approved UI |

The full-page image is deterministically composed from three overlapping current-browser viewport captures because the in-app screenshot surface caps a single capture height. The fixed shell appears once; main content is not fabricated or rescaled.
