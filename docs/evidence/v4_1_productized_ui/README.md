# Fundamental AI v4.1 Productized Desktop UI Evidence

## Evidence Boundary

- Branch: `codex/v4-1-frontend-runtime-alignment`
- PR: `#1179` (Draft, unmerged)
- Starting Head: `3c485d40f9668f6835328bf8f917fde62d73ebc1`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Browser source: current `dashboard.html`, `dashboard-latest.css`, and `frontend-contract.js`
- Controlled transport: local read-only `dashboard-visual-acceptance-fixture.py`
- Viewport: `1440 x 900`
- External calls: `0`
- Runtime writes: rejected

The controlled fixture proves rendering, interaction, responsive containment, and fail-closed presentation. It does not prove live provider data. Before images are historical comparison inputs; after images are generated from the current worktree.

## Figma Provenance

Latest approved component sources remain `28:154`, `31:23`, `520:212`, `523:748`, `35:97`, `35:4`, `35:35`, and `35:66` in file `rdMYmsAvZYkXHJX8hdl7UN`.

Node `519:3` is the rejected old P1-KB baseline and is not the implementation target.

## Runtime Evidence Index

| # | File | Scenario | Result |
|---:|---|---|---|
| 01 | `runtime/01-before-first-viewport.png` | Previous first viewport | Comparison baseline |
| 02 | `runtime/02-after-first-viewport.png` | Productized first viewport | PASS |
| 03 | `runtime/03-before-ai-empty.png` | Previous AI empty state | Comparison baseline |
| 04 | `runtime/04-after-ai-empty.png` | Compact AI unavailable state | PASS |
| 05 | `runtime/05-asset-pool-empty.png` | Asset Pool has no assets | PASS |
| 06 | `runtime/06-asset-pool-10-no-opportunities.png` | Pool has 10 assets, ranking has no eligible result | PASS |
| 07 | `runtime/07-dynamic-top6.png` | Dynamic Top6 projection | PASS |
| 08 | `runtime/08-final-execution-plan.png` | Validated Final Execution Plan | PASS |
| 09 | `runtime/09-final-plan-absent.png` | No validated Final Plan | PASS |
| 10 | `runtime/10-ai-gpt.png` | GPT candidate explanation | PASS |
| 11 | `runtime/11-ai-gemini.png` | Gemini evidence and risk review | PASS |
| 12 | `runtime/12-ai-grok.png` | Grok failure paths and stress test | PASS |
| 13 | `runtime/13-ai-partial.png` | Partial role result and unavailable collection | PASS |
| 14 | `runtime/14-ai-unavailable.png` | Role timeout/unavailable | PASS |
| 15 | `runtime/15-conflict-final-adjustment.png` | Conflict and final adjustment summary | PASS |
| 16 | `runtime/16-position-empty.png` | No active UserPosition | PASS |
| 17 | `runtime/17-position-top3.png` | Position Monitoring Top3 | PASS |
| 18 | `runtime/18-asset-search-selected.png` | Search result selected; Add/Analyze enabled | PASS |
| 19 | `runtime/19-desktop-light.png` | Light theme | PASS |
| 20 | `runtime/20-desktop-dark.png` | Dark theme | PASS |
| 21 | `runtime/21-desktop-full-page.png` | Current full Desktop Home | PASS |

## Before / After Index

- First viewport: `01` -> `02`
- AI unavailable: `03` -> `04`
- Previous implementation evidence remains under `docs/evidence/v4_1_latest_ui/`; it is comparison material only.

## Actual Spring Runtime Boundary

The current Spring application was started on `127.0.0.1` from this worktree with authentication enabled and a throwaway local user. Schedulers, external provider calls, push, and automatic trading were disabled. HTTP validation returned:

- login page: `200`; authenticated login: `302` to `/dashboard`;
- authenticated `/dashboard`: `200`, complete 711,507-byte response with current productized HTML markers;
- `/actuator/health`: `UP`;
- served CSS SHA-256 equals the worktree CSS SHA-256;
- served semantic mapper SHA-256 equals the worktree mapper SHA-256;
- authenticated `/api/dashboard/home`: `200`, current Dashboard contract returned.

The in-app browser rejected navigation to the local runtime port under its URL security policy. No real-Spring browser screenshot, browser console trace, or authenticated real-provider claim is made. This remains an explicit independent runtime acceptance item.
