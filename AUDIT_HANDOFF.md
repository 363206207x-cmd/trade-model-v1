# Fundamental AI Frontend Audit Handoff

This document is an external-audit map for the current desktop frontend. It does not redefine product behavior or authorize runtime providers.

## Frontend Location

The frontend is server-rendered by Spring Boot and Thymeleaf. There is no Node package or `package.json`.

- Templates: `src/main/resources/templates/`
- Browser JavaScript: `src/main/resources/static/js/`
- Styles: `src/main/resources/static/css/`
- Shared shell icon: `src/main/resources/static/icons/app-shell.svg`

## Start

Requirements: Java 17. The local launcher creates private local credentials under the ignored `.runtime/` directory and disables external providers, AI calls, Telegram delivery, schedulers, and auto trading by default.

```bash
./scripts/run-local.sh
```

For the isolated visual-review fixture profile:

```bash
./scripts/run-local.sh --ui-review
```

The launcher prints the selected local URL. The authenticated Home route is `/dashboard`.

## Primary Routes

| Product area | Route | Controller/template | Browser runtime |
| --- | --- | --- | --- |
| Home | `/dashboard` | `src/main/java/org/example/trademodel/controller/DashboardController.java`; `src/main/resources/templates/home.html` | `src/main/resources/static/js/home-runtime.js` |
| Positions | `/positions` | `src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java`; `src/main/resources/templates/workspace.html` | `src/main/resources/static/js/workspace.js` |
| Analysis | `/analysis` | `src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java`; `src/main/resources/templates/workspace.html` | `src/main/resources/static/js/workspace.js` |
| Messages | `/messages` | `src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java`; `src/main/resources/templates/workspace.html` | `src/main/resources/static/js/workspace.js` |
| Me | `/me` | `src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java`; `src/main/resources/templates/workspace.html` | `src/main/resources/static/js/workspace.js` |

The Home data endpoint is `GET /api/dashboard/home`, implemented by `src/main/java/org/example/trademodel/controller/DashboardHomeController.java` and `src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java`.

## Three-AI Components

The production Home uses one workspace with role tabs, not three independent cards.

- Workspace/tab markup: `src/main/resources/templates/home.html`
- GPT renderer: `renderGpt` in `src/main/resources/static/js/home-runtime.js`
- Gemini renderer: `renderGemini` in `src/main/resources/static/js/home-runtime.js`
- Grok renderer: `renderGrok` in `src/main/resources/static/js/home-runtime.js`
- Conflict Summary renderer: `renderConflict` in `src/main/resources/static/js/home-runtime.js`
- Analysis-page role tabs: `src/main/resources/templates/workspace.html`
- Analysis-page browser binding: `src/main/resources/static/js/workspace.js`
- Structured role transport contract: `src/main/java/org/example/trademodel/ai/AiRoleResultsPayload.java`

## Fixtures And Mock Data

Production runtime data is not supplied by these files. They are limited to the `ui-review` profile, tests, or explicitly controlled visual acceptance.

- Home review fixture: `src/main/java/org/example/trademodel/uireview/UiReviewDashboardHomeService.java`
- Asset-pool review fixture: `src/main/java/org/example/trademodel/uireview/UiReviewAssetPoolService.java`
- Workspace/plan review fixture: `src/main/java/org/example/trademodel/uireview/UiReviewWorkspacePlanFixture.java`
- Fixture runtime guard: `src/main/java/org/example/trademodel/uireview/UiReviewRuntimeGuard.java`
- Fixture profile: `src/main/resources/application-ui-review.yml`
- Visual acceptance fixture: `scripts/dashboard-visual-acceptance-fixture.py`
- Controlled PostgreSQL fixture: `scripts/p3-generated-fixture-data.sql`
- Frontend runtime contract tests: `src/test/java/org/example/trademodel/controller/HomeUiReviewRuntimeContractTest.java`

## Provider Integration Paths

No provider secret is committed. Configuration names only are listed in `.env.example`.

### Binance

- Public OHLCV: `src/main/java/org/example/trademodel/market/client/impl/BinancePublicOhlcvProvider.java`
- Market quote: `src/main/java/org/example/trademodel/market/client/impl/BinanceMarketQuoteClient.java`
- Funding/open interest: `src/main/java/org/example/trademodel/market/client/impl/BinanceUsdtMPerpFundingClient.java`, `src/main/java/org/example/trademodel/market/client/impl/BinanceUsdtMOpenInterestClient.java`
- Position adapter: `src/main/java/org/example/trademodel/position/BinancePositionProvider.java`
- Derivatives snapshot: `src/main/java/org/example/trademodel/providercall/snapshot/BinanceDerivativesSnapshotService.java`

### CoinGlass

- Client/transport: `src/main/java/org/example/trademodel/providercall/coinglass/CoinGlassV4Client.java`, `src/main/java/org/example/trademodel/providercall/coinglass/JdkCoinGlassV4HttpTransport.java`
- Adapter and snapshot assembly: `src/main/java/org/example/trademodel/providercall/coinglass/CoinGlassV4ProviderAdapter.java`, `src/main/java/org/example/trademodel/providercall/coinglass/CoinGlassDerivativesSnapshotService.java`
- Provider package: `src/main/java/org/example/trademodel/providercall/coinglass/`

### Other Providers

- Kraken public OHLCV: `src/main/java/org/example/trademodel/market/client/impl/KrakenPublicOhlcvProvider.java`
- OpenAI GPT: `src/main/java/org/example/trademodel/ai/OpenAiProviderClient.java`
- Google Gemini: `src/main/java/org/example/trademodel/ai/GeminiProviderClient.java`
- xAI Grok: `src/main/java/org/example/trademodel/ai/XaiProviderClient.java`
- Provider boundaries and fail-closed no-call adapters: `src/main/java/org/example/trademodel/providercall/adapter/`

## Data-Origin Map

| Frontend field | Source and path |
| --- | --- |
| `selectedAsset` | The browser sends `selectedSymbol` to `GET /api/dashboard/home`. `DashboardHomeServiceImpl` resolves it against the ranked Home projection/decision context and returns `DashboardHomeVO.selectedAssetContext`. `home-runtime.js` keeps the selected symbol in the URL and renders that API value. |
| `roleState` | A formal per-role value in `AiRoleResultsPayload.RolePayload`. It is decoded/validated by `AiRoleResultsCodec` and `AiDecisionChainResponseParser`, copied by `DashboardHomeServiceImpl.copyFormalAiContract`, returned as `DashboardHomeVO.AiTabVO.roleState`, and consumed by `home-runtime.js`. |
| `review_result` | The transport name is `reviewResult`. It is the Gemini field in `AiRoleResultsPayload.RolePayload`, parsed by `AiDecisionChainResponseParser`, copied to `DashboardHomeVO.AiTabVO.reviewResult`, and rendered only by the Gemini role branch in `home-runtime.js`. It is not a Final-plan field. |
| `failure_path_state` | The transport name is `failurePathState`. It is the Grok collection-state field paired with `failurePaths` in `AiRoleResultsPayload.RolePayload`, parsed/normalized by `AiDecisionChainResponseParser` and `AiRoleResultsCodec`, copied to `DashboardHomeVO.AiTabVO.failurePathState`, and rendered by the Grok role branch in `home-runtime.js`. |

Missing or untrusted structured role values remain unavailable/fail-closed; browser rendering does not synthesize them from another role field.

## Secret Handling

- `.env`, `.env.*`, local runtime directories, common key/certificate formats, dependency caches, coverage, and build output are ignored.
- `.env.example` contains variable names only and no runtime values.
- Real credentials belong outside the repository and must never be copied into fixtures, reports, logs, screenshots, or commits.
