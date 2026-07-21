# P3-U2 iPhone Home Mobile Projection P1 Implementation

## Status

- Phase: `P3-U2 / IN_PROGRESS / PARTIAL`
- Implementation base: `99b858f0db61d70891e088eb7add41ee7de07c1c`
- Mobile route: `/dashboard/mobile`
- Production readiness: `BLOCKED`
- Real iPhone validation: `NOT_RUN`

This package implements a mobile information projection of the existing Dashboard. It does not create a second business workflow and does not authorize trading.

## Frozen Contract

1. **Entry route**: authenticated users enter the projection at `/dashboard/mobile`.
2. **Data source**: initial rendering uses `DashboardHomeService#getHome(selectedSymbol, 3, null)` and `DashboardHomeVO`.
3. **Desktop relationship**: `/dashboard` and `dashboard.html` remain unchanged. The mobile page is a separate presentation template over the same read model.
4. **No duplicated calculation**: market, risk, opportunity, execution, position, and AI semantics remain owned by the existing backend service.
5. **iOS navigation**: `BackendConfiguration.rootURL` appends `/dashboard/mobile`; Session Cookie, CSRF, login, logout, and trusted-origin navigation remain unchanged.
6. **Module order**: header, seven system states, alerts/events, watch assets, execution advice, position monitor, AI evidence review, and bottom navigation.
7. **Watch selection**: the page renders at most three real `assets[]` rows using an accessible horizontal radio pattern.
8. **Execution linkage**: an asset selection calls the existing read-only `/api/dashboard/home` endpoint and replaces only `executionSuggestion` presentation fields.
9. **AI linkage**: the same response replaces `aiDecision` and its embedded consistency summary. Only one of `GPT_FINAL`, `GEMINI_REVIEW`, or `GROK_CHALLENGE` is visible at a time.
10. **Position independence**: `positions[]` is rendered once from the initial page read model. Asset changes do not request or mutate position DOM.
11. **Disclosure defaults**: full execution fields, position details, consistency supplements, and role evidence use native closed `details` elements.
12. **Empty states**: missing data remains visibly empty with `--`, `等待同步`, `暂无告警`, `暂无关键事件`, `暂无重点资产`, `暂无手动持仓`, or equivalent fail-closed wording.
13. **No-source fields**: `top.holdingRisk` is not rendered. No static fixture value is substituted for a missing backend field.
14. **Review navigation**: the review destination remains the real same-origin route `/review/dashboard`.
15. **Position route**: a complete position page remains `CONTRACT_UNRESOLVED`; the page shows a disabled `完整持仓页待实现` control and creates no route.
16. **Appearance**: light and dark colors follow `prefers-color-scheme`; color is supplementary and never the sole state signal.
17. **Device layout**: 440 pt uses the primary pager and up to three position summaries; the 428 pt breakpoint stacks alerts/events and shows up to two position summaries without scaling the page.
18. **Accessibility**: controls are at least 44 pt, tabs/radios expose selection state, native disclosures expose expanded state, headings follow document order, and text can wrap under Dynamic Type.
19. **Desktop boundary**: the desktop controller, template, endpoint contract, and behavior are not changed by this package.
20. **Validation**: MVC/authentication, template and JavaScript contracts, iOS URL/security tests, full Java regression, Swift XCTest, simulator build, and actual localhost visual acceptance are required before review.

## Runtime Interaction

The server renders one initial `DashboardHomeVO`. A watch-asset change makes one bounded same-origin GET to:

```text
/api/dashboard/home?selectedSymbol=<selected>&limit=3
```

The response updates only execution and AI/consistency sections. A failed or invalid response clears those dependent conclusions to fail-closed copy. It never reuses stale conclusions for the newly requested symbol. Position monitor nodes are not touched.

## Semantic Ownership

| Concern | Owner |
| --- | --- |
| Market/risk/data state | Existing Dashboard backend |
| Asset ranking and labels | Existing Dashboard backend |
| Execution advice | Existing `executionSuggestion` read model |
| Manual positions | Existing `positions[]` read model |
| AI role results | Existing `aiDecision.tabs[]` read model |
| Consistency synthesis | Existing `aiDecision.consistency` read model |
| Mobile ordering/collapse/selection | Mobile projection only |

`finalPlanMode` is read only from the `GPT_FINAL` tab. It is not read from `aiDecision.consistency`.

## Explicit Safety Boundary

```text
MOBILE_IS_PROJECTION: YES
BUSINESS_SEMANTICS_SOURCE: WEB_BACKEND
NEW_TRADING_SEMANTICS: NONE
STATIC_FIXTURE_IN_PRODUCTION: FORBIDDEN
DESKTOP_DASHBOARD_BEHAVIOR_CHANGE: NONE
notTradeInstruction: true
notExecutable: true
notAutoTrading: true
```

This implementation does not call providers, call AI, send Telegram or Push, create or mutate a position, submit an order, deploy a server, or change production readiness.

## Runtime Acceptance Evidence

The authenticated browser flow was exercised against the exact implementation branch on a disposable H2-backed local server. An unauthenticated request redirected to `/login`; form login returned to `/dashboard/mobile`; the same Session then reached `/review/dashboard`. No credential value is recorded in this repository.

The iOS visual run used the locally built `TradeModelApp.app` and the same exact-branch mobile projection on a second disposable local-only port. Authentication was disabled only for that visual harness because authentication and saved-request behavior were already covered by the authenticated browser run and MVC tests. This does not change application defaults or provide production evidence.

Acceptance results:

- asset sequence `BTCUSDT -> ETHUSDT -> SOLUSDT -> BTCUSDT` updated selected asset, execution, AI, and consistency presentation through the existing endpoint;
- the position monitor outer DOM remained byte-for-byte unchanged across asset switches;
- execution disclosure opened and closed using native `details`;
- each AI role tab became the single selected role in turn;
- the Home control reset the page to `scrollY=0` and focused `mobile-home-title`;
- Review navigated to `/review/dashboard`;
- unresolved template token count was zero;
- visible interactive controls measured at least 44 pt;
- the page shell stayed at the viewport width with global horizontal overflow clipped while the asset pager retained its intentional local horizontal scroll;
- the 17 Pro Max Simulator passed Light, Dark, and one-step larger Dynamic Type (`large` to `extra-large`) visual inspection;
- the 12 Pro Max Simulator passed independent Light and Dark visual inspection without page scaling.

During Simulator acceptance, repeated `didFinishNavigation` publication from `UIViewRepresentable.updateUIView` kept the loading overlay active and produced a SwiftUI runtime warning. `WebViewState` now publishes only when `phase` or `canGoBack` actually changes. The idempotence regression test proves a repeated identical completion produces no additional publication. Authentication, Session, Cookie, CSRF, trusted-origin, and URL security contracts are unchanged.

## Implementation Screenshots

- `docs/implementation-screenshots/p3-u2-mobile-17pm-dark-first-screen.png`
- `docs/implementation-screenshots/p3-u2-mobile-12pm-dark-first-screen.png`
- `docs/implementation-screenshots/p3-u2-simulator-17pm-light.png`
- `docs/implementation-screenshots/p3-u2-simulator-17pm-dark.png`
- `docs/implementation-screenshots/p3-u2-simulator-12pm-light.png`
- `docs/implementation-screenshots/p3-u2-simulator-12pm-dark.png`
- `docs/implementation-screenshots/p3-u2-simulator-17pm-dark-large-text.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-position-summary.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-ai-gpt.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-ai-gemini.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-ai-grok.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-review-route.png`
- `docs/implementation-screenshots/p3-u2-mobile-17pm-home-scroll-reset.png`

The screenshots are generated from the running implementation. Design-contract prototype screenshots are retained separately under `docs/design/p3-u2-iphone-home-ia-v2/screenshots/` and are not presented as runtime evidence.

## Validation

- Java full regression: `4108` tests, `0` failures, `0` errors, `14` environment-gated skips.
- Swift XCTest on iPhone 17 Pro Max Simulator: `50` tests, all passed.
- Workflow contract: `PASS` (`WORKFLOW_CONTRACT_OK`).
- Git diff check: `PASS`.
- Delivery check: `ENVIRONMENT_PATH_GATED_ONLY`; the isolated worktree path is `/Users/xuchao/Documents/trade-model-v1-p0-0-audit/p3-u2-work`, while the script requires `/Users/xuchao/Documents/trade-model-v1`. It exited with code `2` before business checks and is not reported as a pass.
- Real iPhone validation: `NOT_RUN`.
