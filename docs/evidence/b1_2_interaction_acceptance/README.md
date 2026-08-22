# Fundamental AI v4.1 B.1.2 Browser Interaction Acceptance

Evidence source Head: `4abab07cef34632992a07aba7a0933d198b06611`.

This package is browser evidence only. It does not modify production code,
fixtures, tests, schemas, or product behavior. The `ui-review` profile is
deterministic fixture evidence, disables external providers, and is not live
provider evidence.

## Runtime baseline

| Mode | Command | HOME_URL | LOGIN_URL | Result |
|---|---|---|---|---|
| Normal | `./scripts/run-local.sh` | `http://localhost:8087/dashboard` | `http://localhost:8087/login` | Authenticated `/dashboard`, `/analysis`, and `/messages` returned 200. No analysis was started. |
| UI review | `./scripts/run-local.sh --ui-review` | `http://localhost:8087/dashboard` | `http://localhost:8087/login` | Deterministic interaction evidence below. `UI_REVIEW_EXTERNAL_CALL_COUNT=0`. |

Browser zoom was 100%. The primary CSS viewport was `1440x900` with
`devicePixelRatio=2`, `innerWidth=1440`, `clientWidth=1440`, and
`scrollWidth=1440`. The in-app browser stored uncropped viewport captures at
1440x900 physical pixels. Browser console error count and horizontal overflow
delta were both zero.

## Acceptance result

| Item | Status | Result |
|---|---|---|
| IA-01 Home opportunity selection | `PASS` | Revalidated by B.1.2.1: BTCUSDT -> ETHUSDT selection, URL persistence, refresh, replaceState Back/Forward semantics, Plan/AI fail-closed state, and one `aria-pressed=true` all passed. The visible card-level `当前` requirement is superseded by the dated Owner exception. |
| IA-02 Analysis Preview | `PASS` | Formal `/analysis` search and Preview worked. PREVIEW stayed isolated from Opportunity/Candidate/Final semantics, one role rendered at a time, unknown mode failed closed, and no role tab or first visual intersected the sticky Header. |
| IA-03 Messages -> Recheck | `NOT_VERIFIED` | The current authenticated user had zero active Messages, therefore no legal `PUSH_SNAPSHOT` owner chain was available. No naked pushId route was constructed and screenshots 8-13 were not fabricated. |
| `B1_2_INTERACTION_ACCEPTANCE_DONE` | `NO` | IA-03 remains `NOT_VERIFIED_NOT_EXECUTED`; the B.1.2.1 correction does not execute Recheck. |

The fixed phase statements remain:
`KNIFE_B_1_1_IMPLEMENTATION_DONE=YES`, `CURRENT_PHASE_DONE=NO`,
`GLOBAL_SEMANTIC_RUNTIME_DONE=NO`, `LIVE_RUNTIME_ACCEPTANCE_DONE=NO`,
`KNIFE_C_STARTED=NO`, and `MERGE=NO`.

## IA-01 observations

- Before: selected asset `BTCUSDT`, URL `/dashboard`, one selected card.
- After one ETH card click: selected asset `ETHUSDT`, URL
  `/dashboard?asset=ETHUSDT`, one selected card, PageHeader/Plan/AI/Conflict
  all showed ETHUSDT or the correct no-Final/fail-closed state.
- Refresh reproduced the same ETHUSDT URL and page state.
- Back/Forward restored the same ETHUSDT Home context and did not enter a
  legacy dashboard detail route.
- After B.1.2.1, the selected ETHUSDT unavailable roles have no analysisId or
  traceId. The UI says `审计链尚未形成`, sets `aria-disabled=true`, and has no
  usable audit href.
- The card state has `.is-selected` and `aria-pressed=true` but no visible
  card-level `当前` text, as required by the dated Owner exception.
- No AnalysisRun, Position, Final Plan, or external-provider request was
  created by selection.

## IA-02 observations

- Search selected `BTCUSDT` from the formal `/analysis` page.
- Preview request: `POST
  /api/asset-pool/search/BTCUSDT/analysis-preview?timeframe=5m`, status 200.
- Result URL: `/analysis/ui-review-preview-analysis`.
- `analysisId=ui-review-preview-analysis` and
  `analysisMode=PREVIEW` (`按需分析预览`).
- GPT rendered `方向假设 / 弱偏多 / 按需分析预览 · 非 Opportunity`.
- Gemini rendered evidence-gap, logical-conflict, and confidence-review
  groups only; Candidate review and Before -> After were absent.
- Grok rendered reverse scenario, external event risk, microstructure risk,
  and observation metrics only; Opportunity Failure Path was absent.
- Role Tab switches made zero network requests and zero business writes.
- Opportunity/Candidate/Final IDs and Final-only entry, stop, and target
  values were absent. Preview did not add a Top6 asset or create a
  UserPosition.
- The controlled unknown-mode route failed closed and did not default to
  Opportunity.
- Sticky measurement at `scrollY=323`: Header bottom `52`, role tabs top/bottom
  `298.91/340.91`, active role top/bottom `302.91/336.91`; intersection was
  false.
- No UNAVAILABLE, ERROR, or FALLBACK role fixture was exercised:
  `ROLE_UNAVAILABLE_GATE_BROWSER=NOT_VERIFIED`.

## IA-03 owner-chain boundary

The authenticated Message query returned zero active rows. Consequently:

| Field | Value |
|---|---|
| Message group | `NOT_AVAILABLE` |
| messageId | `NOT_AVAILABLE` |
| source type | `NOT_AVAILABLE` |
| pushSnapshotId | `NOT_AVAILABLE` |
| pushId | `NOT_AVAILABLE` |
| recheckId | `NOT_AVAILABLE` |
| Message OPEN | `NOT_EXECUTED` |
| Recheck bind | `NOT_EXECUTED` |
| Recheck F5 | `NOT_EXECUTED` |
| Recheck refresh | `NOT_EXECUTED` |
| Recheck Retry | `NOT_EXECUTED` |
| Recheck row delta | `NOT_VERIFIED` |
| ERROR Retry browser | `NOT_VERIFIED` |

No Recheck route was opened because doing so without a Message-owned
`PUSH_SNAPSHOT` would violate the owner-chain contract.

## Network interaction ledger

| Step | User action | Method | URL | Status | Object ID | Read/Write | Result |
|---|---|---|---|---:|---|---|---|
| IA-01.0 | Open Home | GET | `/dashboard` and `/api/dashboard/home` | 200 | `BTCUSDT` | Read | BTCUSDT selected. |
| IA-01.1 | Click ETH card once | GET | `/api/dashboard/home` | 200 | `ETHUSDT` | Read | URL became `/dashboard?asset=ETHUSDT`; exactly one selected card. |
| IA-01.2 | Refresh Home | GET | `/dashboard?asset=ETHUSDT` and `/api/dashboard/home` | 200 | `ETHUSDT` | Read | ETHUSDT restored. |
| IA-01.3 | Inspect related audit control | NONE | N/A | N/A | unavailable | None | ETH unavailable roles have no analysisId/traceId; control says `审计链尚未形成`, is disabled, and has no href. No audit request was made. |
| IA-02.0 | Search BTC | GET | `/api/asset-pool/search?query=BTC&limit=20` | 200 | `BTCUSDT` | Read | Existing asset selected. |
| IA-02.1 | Click Start Preview once | POST | `/api/asset-pool/search/BTCUSDT/analysis-preview?timeframe=5m` | 200 | `ui-review-preview-analysis` | Preview action | Preview result only; no Opportunity/Candidate/Final/UserPosition. |
| IA-02.2 | Open Preview route | GET | `/analysis/ui-review-preview-analysis` | 200 | `ui-review-preview-analysis` | Read | PREVIEW page rendered. |
| IA-02.3 | Click GPT/Gemini/Grok tabs | NONE | N/A | N/A | role only | Client state | One role visible; no request. |
| IA-02.4 | Open controlled unknown mode | GET | `/analysis/ui-review-analysis-unknown` | 200 | `ui-review-analysis-unknown` | Read | Fail closed. |
| IA-03.0 | Open Messages | GET | `/messages` and `/api/workspace/messages` | 200 | none | Read | Active Message count 0. |
| IA-03.1 | Message OPEN | NOT EXECUTED | N/A | N/A | unavailable | None | `NOT_VERIFIED`; no legal owner chain. |
| IA-03.2 | Recheck bind/F5/refresh/Retry/return | NOT EXECUTED | N/A | N/A | unavailable | None | `NOT_VERIFIED`; no route fabricated. |

The ledger is correlated from the actual browser action, application access
log method/path/status, and rendered result. No external provider request was
observed.

## Screenshot manifest

| File | Route / state | Viewport | scrollY | Classification | Acceptance item |
|---|---|---:|---:|---|---|
| `01-home-before.png` | `/dashboard`, BTCUSDT selected | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-01 before |
| `02-home-after-eth.png` | `/dashboard?asset=ETHUSDT`, after click | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-01 after |
| `03-home-refresh-eth.png` | same route after refresh | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-01 refresh |
| `04-analysis-search-ready.png` | `/analysis`, BTC search result selected | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-02 start |
| `05-preview-gpt.png` | Preview, GPT | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-02 GPT |
| `06-preview-gemini.png` | Preview, Gemini | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-02 Gemini |
| `07-preview-grok.png` | Preview, Grok | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-02 Grok |
| `08-analysis-unknown.png` | unknown analysis mode | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-02 mode gate |
| `09-messages-current-state.png` | `/messages`, no active Message | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | IA-03 evidence gap |
| `10-eth-audit-entry-fail-closed.png` | Historical pre-B.1.2.1 ETH wrong-owner fail-closed evidence | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | Superseded by B.1.2.1 evidence |

All files have real PNG signatures and exact 1440x900 dimensions. They were
transcoded from the in-app browser's uncropped viewport capture encoding only;
no crop, resize, zoom, transform, or content-concealing edit was applied.

## Findings

### B12-P1-01 — closed

- `FINDING_ID`: `B12-P1-01`
- `ROUTE`: `/dashboard?asset=ETHUSDT`
- `USER_ACTION`: Click ETHUSDT opportunity card once.
- `STATUS`: `CLOSED_BY_OWNER_FREEZE_EXCEPTION`
- `OWNER_EXCEPTION`: Visible card-level `当前` is no longer required. Selected
  outline, PageHeader, selected-asset content, and `aria-pressed` jointly
  communicate selection.
- `CARD_VISIBLE_CURRENT_LABEL_COUNT`: `0`
- `PAGEHEADER_CURRENT_ASSET`: `PASS`
- `ARIA_PRESSED`: `PASS`
- `BLOCKS_INTERACTION_ACCEPTANCE`: `NO`
- `EVIDENCE`: `../b1_2_1_opportunity_selection/`

### B12-P1-02 — closed

- `FINDING_ID`: `B12-P1-02`
- `ROUTE`: `/dashboard?asset=ETHUSDT`
- `USER_ACTION`: Inspect the selected ETHUSDT AI audit control.
- `STATUS`: `CLOSED`
- `ROOT_CAUSE`: `UI_REVIEW_FIXTURE_STALE_IDENTITY`
- `ACTUAL_AFTER_FIX`: Non-BTC unavailable roles expose no borrowed analysisId
  or traceId; the audit control has no href and says `审计链尚未形成`.
- `PRODUCTION_STALE_IDENTITY`: `NOT_VERIFIED`
- `BLOCKS_INTERACTION_ACCEPTANCE`: `NO`
- `EVIDENCE`: `../b1_2_1_opportunity_selection/04-eth-audit-not-formed.png`

### B12-EG-01

- `FINDING_ID`: `B12-EG-01`
- `ROUTE`: `/messages`
- `USER_ACTION`: Open Messages as the authenticated test user.
- `EXPECTED`: A current-user Message with `PUSH_SNAPSHOT` is required to test
  the Recheck owner chain.
- `ACTUAL`: Active Message count is zero.
- `REQUEST_EVIDENCE`: `GET /api/workspace/messages`, status 200; mapper total 0.
- `SCREENSHOT`: `09-messages-current-state.png`
- `BLOCKER_CLASS`: `EVIDENCE_GAP`
- `REPRODUCIBILITY`: `ALWAYS` in the current isolated runtime database.
- `BLOCKS_INTERACTION_ACCEPTANCE`: `NO` as a product failure; IA-03 remains
  `NOT_VERIFIED`.
- `RECOMMENDED_NEXT_PACKAGE`: None until Owner provides a legal runtime Message
  owner chain or separately authorizes controlled data.

## Safety and validation evidence

- Product Source Gate: PASS at the source Head.
- Workflow Contract: PASS at the source Head.
- Directed Maven tests: PASS for
  `FundamentalAiV41FrontendRuntimeAlignmentContractTest`,
  `WorkspacePushRecheckServiceTest`, and
  `PushRecheckAccessBoundaryKnifeBTest`.
- B.1.2.1 final-Head CI links are intentionally pending until the final push.
  Exact run links belong in the canonical PR handoff and must not trigger an
  additional documentation-only commit cycle.
- Fake SCHEDULED, old user/pushId, manual/replay, cross-user, and read-only
  bind/F5 safety gates remain `AUTOMATED_TEST` evidence only in this package.
  They are not represented as browser PASS.
- Production code changed: YES, limited to the OpportunityCard
  `aria-pressed` accessibility contract.
- UI-review fixture changed: YES, limited to clearing borrowed analysisId and
  traceId from unavailable non-BTC roles.
- PR #1195 remains OPEN, DRAFT, and UNMERGED.
