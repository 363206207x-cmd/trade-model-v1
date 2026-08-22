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
| IA-01 Home opportunity selection | `FAIL` | BTCUSDT -> ETHUSDT selection, URL persistence, refresh, Back/Forward, Plan fail-closed state, AI state, and single-selection behavior worked. The selected card has no user-visible `当前` label, and the selected ETHUSDT audit link targets a BTC trace. See B12-P1-01 and B12-P1-02. |
| IA-02 Analysis Preview | `PASS` | Formal `/analysis` search and Preview worked. PREVIEW stayed isolated from Opportunity/Candidate/Final semantics, one role rendered at a time, unknown mode failed closed, and no role tab or first visual intersected the sticky Header. |
| IA-03 Messages -> Recheck | `NOT_VERIFIED` | The current authenticated user had zero active Messages, therefore no legal `PUSH_SNAPSHOT` owner chain was available. No naked pushId route was constructed and screenshots 8-13 were not fabricated. |
| `B1_2_INTERACTION_ACCEPTANCE_DONE` | `NO` | Two P1 findings block interaction acceptance. |

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
- The selected ETHUSDT audit link was
  `/audit/ui-review-trace-btc-gpt_final?returnTo=%2Fdashboard%3Fasset%3DETHUSDT`.
  The audit page did not show BTC output; it failed closed with no trusted
  trace, and its return URL remained the ETHUSDT Home context.
- The card state had `.is-selected` but no visible `当前` text.
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
| Message OPEN POST count | `0` |
| PUSH_OPEN POST count on bind/F5/refresh | `0 / 0 / 0` |
| PUSH_OPEN row delta on bind/F5/refresh | `NOT_VERIFIED / NOT_VERIFIED / NOT_VERIFIED` |
| Retry request count | `0` |
| ERROR Retry browser | `NOT_VERIFIED` |

No Recheck route was opened because doing so without a Message-owned
`PUSH_SNAPSHOT` would violate the owner-chain contract.

## Network interaction ledger

| Step | User action | Method | URL | Status | Object ID | Read/Write | Result |
|---|---|---|---|---:|---|---|---|
| IA-01.0 | Open Home | GET | `/dashboard` and `/api/dashboard/home` | 200 | `BTCUSDT` | Read | BTCUSDT selected. |
| IA-01.1 | Click ETH card once | GET | `/api/dashboard/home` | 200 | `ETHUSDT` | Read | URL became `/dashboard?asset=ETHUSDT`; exactly one selected card. |
| IA-01.2 | Refresh Home | GET | `/dashboard?asset=ETHUSDT` and `/api/dashboard/home` | 200 | `ETHUSDT` | Read | ETHUSDT restored. |
| IA-01.3 | Open related audit | GET | `/audit/ui-review-trace-btc-gpt_final?returnTo=%2Fdashboard%3Fasset%3DETHUSDT`; then `/api/ai/audit-chain` | 200 / 404 | `ui-review-trace-btc-gpt_final` | Read | Wrong-asset trace target failed closed; ETH return context preserved. |
| IA-02.0 | Search BTC | GET | `/api/asset-pool/search?query=BTC&limit=20` | 200 | `BTCUSDT` | Read | Existing asset selected. |
| IA-02.1 | Click Start Preview once | POST | `/api/asset-pool/search/BTCUSDT/analysis-preview?timeframe=5m` | 200 | `ui-review-preview-analysis` | Preview action | Preview result only; no Opportunity/Candidate/Final/UserPosition. |
| IA-02.2 | Open Preview route | GET | `/analysis/ui-review-preview-analysis` | 200 | `ui-review-preview-analysis` | Read | PREVIEW page rendered. |
| IA-02.3 | Click GPT/Gemini/Grok tabs | NONE | N/A | N/A | role only | Client state | One role visible; no request. |
| IA-02.4 | Open controlled unknown mode | GET | `/analysis/ui-review-analysis-unknown` | 200 | `ui-review-analysis-unknown` | Read | Fail closed. |
| IA-03.0 | Open Messages | GET | `/messages` and `/api/workspace/messages` | 200 | none | Read | Active Message count 0. |
| IA-03.1 | Message OPEN | NONE | N/A | N/A | unavailable | None | `NOT_VERIFIED`; no legal owner chain. |
| IA-03.2 | Recheck bind/F5/refresh/Retry/return | NONE | N/A | N/A | unavailable | None | `NOT_VERIFIED`; no route fabricated. |

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
| `10-eth-audit-entry-fail-closed.png` | ETH Home audit link target | 1440x900 | 0 | `UI_REVIEW_FIXTURE` | B12-P1-02 |

All files have real PNG signatures and exact 1440x900 dimensions. They were
transcoded from the in-app browser's uncropped viewport capture encoding only;
no crop, resize, zoom, transform, or content-concealing edit was applied.

## Findings

### B12-P1-01

- `FINDING_ID`: `B12-P1-01`
- `ROUTE`: `/dashboard?asset=ETHUSDT`
- `USER_ACTION`: Click ETHUSDT opportunity card once.
- `EXPECTED`: The unique selected card displays an explicit user-visible
  `当前` state.
- `ACTUAL`: Selection is unique and visually styled with `.is-selected`, but
  no card contains user-visible `当前` text.
- `REQUEST_EVIDENCE`: One `GET /api/dashboard/home`, status 200, no write.
- `SCREENSHOT`: `02-home-after-eth.png`
- `BLOCKER_CLASS`: `P1`
- `REPRODUCIBILITY`: `ALWAYS` in the controlled UI-review state.
- `BLOCKS_INTERACTION_ACCEPTANCE`: `YES`
- `RECOMMENDED_NEXT_PACKAGE`: Owner-authorized targeted Home selection-state
  patch only.

### B12-P1-02

- `FINDING_ID`: `B12-P1-02`
- `ROUTE`: `/dashboard?asset=ETHUSDT`
- `USER_ACTION`: Open `查看完整审计` from the selected ETHUSDT AI area.
- `EXPECTED`: Related audit/detail entry uses the selected ETHUSDT context.
- `ACTUAL`: Link targets `ui-review-trace-btc-gpt_final`. The destination
  correctly fails closed and does not display BTC output, while returnTo keeps
  ETHUSDT.
- `REQUEST_EVIDENCE`: Page `GET` status 200 followed by fail-closed
  `GET /api/ai/audit-chain` status 404.
- `SCREENSHOT`: `10-eth-audit-entry-fail-closed.png`
- `BLOCKER_CLASS`: `P1`
- `REPRODUCIBILITY`: `ALWAYS` in the controlled UI-review state.
- `BLOCKS_INTERACTION_ACCEPTANCE`: `YES`
- `RECOMMENDED_NEXT_PACKAGE`: Owner-authorized selected-asset audit-link
  binding patch only.

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

## Safety and exact-Head evidence

- Product Source Gate: PASS at the source Head.
- Workflow Contract: PASS at the source Head.
- Directed Maven tests: PASS for
  `FundamentalAiV41FrontendRuntimeAlignmentContractTest`,
  `WorkspacePushRecheckServiceTest`, and
  `PushRecheckAccessBoundaryKnifeBTest`.
- `quality-gate` SUCCESS runs:
  - <https://github.com/363206207x-cmd/trade-model-v1/actions/runs/32514730702/job/96873691263>
  - <https://github.com/363206207x-cmd/trade-model-v1/actions/runs/32514727332/job/96873680328>
- `workflow-contract` SUCCESS run:
  <https://github.com/363206207x-cmd/trade-model-v1/actions/runs/32514730762/job/96873691663>
- Fake SCHEDULED, old user/pushId, manual/replay, cross-user, and read-only
  bind/F5 safety gates remain `AUTOMATED_TEST` evidence only in this package.
  They are not represented as browser PASS.
- Production code changed: NO.
- Fixture changed: NO.
- PR #1195 remains OPEN, DRAFT, and UNMERGED.
