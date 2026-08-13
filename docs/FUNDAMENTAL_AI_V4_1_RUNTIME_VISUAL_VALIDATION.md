# Fundamental AI v4.1 Runtime Visual Validation

## Controlled Browser Validation

The real production template and static assets were served through the read-only controlled acceptance transport at `1440 x 900`. The transport rejects writes and performs no external calls.

| Gate | Result |
|---|---|
| 21 required screenshots | PASS |
| Horizontal overflow | `0` |
| Text overflow | `0` |
| Top-level overlap | `0` |
| Visible AI roles | `1` |
| Candidate visible as Final | `false` |
| Search Add/Analyze disabled before selection | PASS |
| Search Add/Analyze enabled after explicit selection | PASS |
| Three distinct Top6 empty states | PASS |
| Light/dark visual hierarchy | PASS |
| Fake chart/K-line/percentage/vote | `0` |

## Scenario Coverage

The browser matrix covers Pool empty, Pool populated with no eligible opportunities, ranking unavailable, dynamic Top6, Final present/absent, GPT/Gemini/Grok, partial and unavailable AI, conflict/final adjustment, no position, Top3, search result selection, light, dark, and full page.

## Actual Spring Runtime Validation

The current application started successfully from the working branch on `127.0.0.1` with authentication enabled and a throwaway local user. Schedulers, external providers, push, and automatic trading were disabled.

| Check | Result |
|---|---|
| login page / authenticated login | HTTP `200` / `302`; local user session established |
| authenticated `/dashboard` | HTTP `200`; complete 711,507-byte response; current brand and module markers present |
| `/actuator/health` | `UP` |
| served `dashboard-latest.css` vs worktree | SHA-256 exact match |
| served `frontend-contract.js` vs worktree | SHA-256 exact match |
| authenticated `/api/dashboard/home` | HTTP `200`; current Dashboard contract returned |
| real application browser navigation | BLOCKED by in-app browser URL policy |
| authenticated real-provider scenario | NOT EXECUTED |

The browser policy block is not a product failure and is not reported as PASS. No alternate browser surface or URL-policy workaround was used.

## Acceptance Boundary

Controlled UI behavior and authenticated local HTTP/template rendering are ready for independent frontend audit. Real browser inspection of the Spring runtime and authenticated real-provider validation remain pending in an approved target runtime.
