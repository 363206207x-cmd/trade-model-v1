# PHASE_BACKEND_P4_DASHBOARD_RUNTIME_KLINE_READINESS_AUDIT

## 1. Audit Purpose

This document records the BACKEND-P4 read-only audit for Dashboard SourceTrace and RuntimeKline readiness.

The goal is to classify which RuntimeKline, latest price, timeframe, data quality, kline, and market-data fields are safe to wire in a future BACKEND-P5 implementation.

This audit does not modify dashboard rendering, Java production code, schema, external data integrations, order APIs, or trading behavior.

## 2. Baseline And Scope

| Item | Value |
|---|---|
| Target PR | #86 |
| Target issue | #85 |
| Branch | `codex/backend-p4-runtime-kline-readiness` |
| Baseline commit | `2e998d6 feat(backend): wire safe production-backed SourceTrace detail fields` |
| Branch trigger commit | `1c4c13e chore(backend): create BACKEND-P4 cloud trigger entry` |
| Package type | Read-only audit plus documentation only |
| Dashboard HTML | Not modified |
| Java production code | Not modified |
| Schema | Not modified |
| External APIs | Not added |
| Order API / auto-trading | Not added |

## 3. Files Inspected

| Area | Files / Classes |
|---|---|
| RuntimeKline contract | `RuntimeKlineContextDTO` |
| SourceTrace contracts | `SourceTraceDTO`, `DerivativesRiskContextDTO`, `DefaultSourceAssembler` |
| Dashboard detail wiring | `DefaultDashboardSourceTraceDetailAdapter`, `PlanBoundarySourceTraceAdapter`, `DefaultPlanBoundarySourceTraceAdapter` |
| Decision read model | `DecisionResultVO`, `DecisionResultMapper`, `DecisionServiceImpl` |
| Kline / quote inputs | `RealMarketDataFetcherService`, `MarketQuoteClient`, `BinanceMarketQuoteClient`, `MarketQuoteSnapshot` |
| Market environment | `RealMarketEnvironmentService`, `MarketEnvironmentSnapshotDO`, `MarketEnvironmentSnapshotMapper` |
| Derivatives partial inputs | `BinanceUsdtMOpenInterestClient`, `BinanceUsdtMPerpFundingClient`, `MarketDataScheduler` |
| Tests reviewed | `DefaultSourceAssemblerTest` |
| Prior baseline docs | `PHASE_BACKEND_P1_DASHBOARD_DATA_COMPLETENESS_AUDIT.md`, `PHASE_BACKEND_P2_DASHBOARD_SOURCETRACE_DETAIL_WIRING_RESULT.md`, `PHASE_BACKEND_P3_DASHBOARD_SOURCETRACE_PRODUCTION_READINESS_RESULT.md` |

## 4. Readiness Classification Legend

| Classification | Meaning |
|---|---|
| `REAL` | Production-backed and safe for the specific target field. |
| `REAL_PARTIAL` | Production-backed for display or metadata, but not complete enough for SourceTrace readiness. |
| `CONTRACT_ONLY` | DTO or interface exists, but no confirmed production assembly path. |
| `FIXTURE_ONLY` | Present only in tests or local fixtures. |
| `MISSING` | No confirmed source is available in the dashboard detail chain. |
| `UNSAFE_FOR_WIRING` | A value exists, but wiring it would create misleading semantics. |
| `SAFE_FAIL_CLOSED` | Must remain missing, incomplete, watch-only, or review-only. |

## 5. RuntimeKlineContext Readiness Matrix

| RuntimeKline Area | Current Source Observed | Readiness | Required Fallback | P5 Recommendation |
|---|---|---|---|---|
| `RuntimeKlineContextDTO` object | DTO exists and `DefaultSourceAssembler` can consume it. | `CONTRACT_ONLY` | `INCOMPLETE` | Build or pass a production-backed context only after source ownership is clear. |
| `symbol` | Dashboard detail receives symbol and decision row includes symbol. | `REAL_PARTIAL` | None for display; incomplete for RuntimeKline context. | Can be reused as identity only; not enough to complete RuntimeKline. |
| `timeframe` | Exists in analysis / market environment domains, but not exposed on `DecisionResultVO` dashboard detail path. | `MISSING` | `INCOMPLETE` | Only wire if selected from persisted `tm_analysis_run.timeframe` or equivalent trusted source. |
| `latestPrice` | `DecisionServiceImpl` enriches `DecisionResultVO.latestPrice` from `MarketQuoteClient.fetch24hTicker`. | `REAL_PARTIAL` | `WATCH_ONLY` / review-only | May be a quote display source; must not become entry, stop, or TP source. |
| OHLCV / kline window | `RealMarketDataFetcherService.fetchKlines` and `DecisionEngineService` use live kline fetches internally. | `MISSING` for dashboard detail | `INCOMPLETE` | Do not call external APIs from dashboard detail. Use persisted or already-assembled runtime context if introduced later. |
| `dataQualityScore` | `DecisionResultMapper` selects `ar.data_quality_score AS dataQualityScore`. BACKEND-P3 wires it to derivatives-risk metadata. | `REAL_PARTIAL` | SourceTrace remains incomplete | Keep as metadata only until RuntimeKline context carries full source and stale metadata. |
| stale status | No confirmed stale-status field in dashboard detail RuntimeKline path. | `MISSING` | `INCOMPLETE` | Add only with explicit context timestamp and freshness rule. |
| missing fields | Dashboard detail adapter already emits fail-closed missing-field lists. | `REAL` for fail-closed display | `SAFE_FAIL_CLOSED` | Keep lists explicit; do not remove missing markers when source is partial. |
| blocking reasons | RuntimeKline context does not provide production blocking reasons in dashboard detail. | `MISSING` | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` | Future blocker source should be separate from text execution-plan summaries. |
| entry / stop / TP / RR sources | No production-backed numeric boundary source in dashboard detail path. | `MISSING` | `INCOMPLETE` / `WATCH_ONLY` | Do not infer from latest price or text summaries. |
| liquidity source | No complete production liquidity stress source for SourceTrace. | `MISSING` | `SAFE_FAIL_CLOSED` | Keep missing until explicit liquidity source exists. |
| multi-timeframe source | BACKEND-P3 safely wires `DecisionResultVO.multiTfConvergence` as label-only source. | `REAL_PARTIAL` | SourceTrace remains incomplete | Keep as partial signal; not enough to complete RuntimeKline or SourceTrace. |
| event window source | No production event-window blocker in dashboard detail path. | `MISSING` | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` | Add only from explicit event blocker contract. |
| wick source | No production wick-confirmation source in dashboard detail path. | `MISSING` | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` | Add only from explicit wick confirmation contract. |

## 6. Latest Price Audit

`DecisionServiceImpl#getLatestDecisionResultBySymbol` fetches quote data through `safeFetchQuote(row.getSymbol())`.

That path uses `MarketQuoteClient.fetch24hTicker`, backed by `BinanceMarketQuoteClient`, and can populate:

- `DecisionResultVO.latestPrice`
- `DecisionResultVO.priceChangePct`
- `DecisionResultVO.priceUpdateTimeMs`

This is useful as a production read-model enrichment for display, but it is not sufficient as RuntimeKlineContext readiness.

The latest price lacks:

- timeframe-specific OHLCV window
- stale status and freshness gate
- entry/stop/TP numeric source reasoning
- source timeframe
- boundary invalidation logic
- RiskActionGuard approval

Therefore P4 classifies latest price as `REAL_PARTIAL`.

It must not be used as:

- entry price source
- stop price source
- TP price source
- RR source
- execution readiness proof
- order trigger

## 7. Timeframe Audit

Timeframe appears in several upstream areas, including analysis runs and market environment snapshots, but the dashboard detail SourceTrace chain does not currently receive a trusted timeframe field.

Observed state:

- `DecisionEngineService` receives timeframe during decision calculation.
- `MarketEnvironmentSnapshotDO` and mapper fields include timeframe.
- `DecisionResultVO` does not expose timeframe.
- `DecisionResultMapper.findLatestDecisionResultBySymbolJoined` does not select timeframe for dashboard detail.
- `DefaultDashboardSourceTraceDetailAdapter` receives `symbol` plus `DecisionResultVO`, not a RuntimeKline context.

P4 classifies dashboard-detail timeframe as `MISSING`.

Future BACKEND-P5 may wire timeframe only if:

- the source is persisted or already present in the decision read model,
- the source label is explicit,
- tests verify that missing timeframe remains fail-closed,
- no external API call is introduced from dashboard detail.

## 8. Data Quality Score Audit

`DecisionResultMapper` selects `ar.data_quality_score AS dataQualityScore`.

BACKEND-P3 already wires this value into `DerivativesRiskContextDTO.dataQualityScore` when present.

This is safe as metadata, but not enough to complete SourceTrace or RuntimeKlineContext.

Data quality does not prove:

- entry source completeness
- stop source completeness
- TP source completeness
- RR source completeness
- kline window freshness
- derivatives risk completeness
- execution readiness

P4 classifies data quality as `REAL_PARTIAL`.

It should remain a supporting metadata field until a complete runtime source context is assembled.

## 9. Kline / OHLCV And Market Service Audit

| Source | Observed Capability | Readiness For Dashboard SourceTrace |
|---|---|---|
| `RealMarketDataFetcherService.fetchKlines` | Can call Binance spot kline endpoint for symbol / interval / limit. | Not safe to call from dashboard detail; no persisted RuntimeKline context confirmed. |
| `DecisionEngineService` | Uses short-window klines internally for bullish convergence and push invalidation calculations. | Runtime-only internal input; not exposed as SourceTrace detail. |
| `MarketQuoteClient` / `BinanceMarketQuoteClient` | Fetches 24h ticker snapshot. | Safe only as partial quote/display metadata. |
| `RealMarketEnvironmentService` | Combines quote, funding, open interest, and environment classification. | Partial environment signal; not full derivatives risk context. |
| `BinanceUsdtMOpenInterestClient` | Fetches current open interest from Binance USD-M endpoint. | Partial OI point only; no OI history chain. |
| `BinanceUsdtMPerpFundingClient` | Fetches latest funding rate. | Partial funding point only; no funding history chain. |
| `MarketEnvironmentSnapshotDO` | Stores timeframe, funding, OI, OI delta, derivatives crowding state, and source type. | Useful partial source for future audit, but not enough for full SourceTrace completion. |

P4 does not wire any of these fields into dashboard SourceTrace.

## 10. Derivatives Risk Readiness

| Derivatives Area | Current State | Readiness | Required Fallback |
|---|---|---|---|
| OI latest point | Partial source exists through Binance USD-M OI and snapshot fields. | `REAL_PARTIAL` | Review-only metadata. |
| OI history | No complete history contract confirmed in dashboard detail. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Funding latest point | Partial source exists through Binance USD-M funding and snapshot fields. | `REAL_PARTIAL` | Review-only metadata. |
| Funding history | No complete history contract confirmed in dashboard detail. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Liquidation cluster | No source found. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Leverage distribution | No source found. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Long / short ratio | No source found. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Liquidity stress | No complete source found. | `MISSING` | `SAFE_FAIL_CLOSED` |
| Event window blocker | No source found. | `MISSING` | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` |
| Wick confirmation source | No source found. | `MISSING` | `WATCH_ONLY` / `SAFE_FAIL_CLOSED` |

Funding, OI, liquidation, leverage, and long/short ratio must not directly generate trade actions.

## 11. SourceTrace Impact

The current dashboard SourceTrace detail should remain fail-closed for most RuntimeKline and derivatives fields.

Safe current state:

- `multiTimeframeSource` may remain partially wired from `DecisionResultVO.multiTfConvergence`.
- `DerivativesRiskContextDTO.dataQualityScore` may remain wired from `DecisionResultVO.dataQualityScore`.
- Missing field lists should continue to expose absent runtime and derivatives sources.

Unsafe current actions:

- do not remove `runtimeKlineContext` from missing fields,
- do not remove `latestPrice` from missing fields merely because quote display exists,
- do not remove `timeframe` until dashboard detail receives a trusted persisted timeframe,
- do not classify data quality as full SourceTrace readiness,
- do not infer entry/stop/TP/RR from text fields,
- do not treat execution-plan labels as numeric boundary sources.

## 12. Unsafe Wiring Prohibitions

BACKEND-P5 must not wire:

- `latestPrice` as entry price,
- `latestPrice` as stop price,
- `latestPrice` as TP price,
- `latestPrice` as RR,
- execution-plan text as entry / stop / TP / RR,
- direction text as order side,
- risk label as order intent,
- funding / OI / liquidation / leverage / long-short ratio as direct trade action,
- dashboard display state as execution permission.

BoundaryCandidate `VALID` remains manual-review only.

ExecutionPlan readiness remains advisory / review-only unless future gates explicitly prove all required sources and review conditions.

## 13. Recommended BACKEND-P5 Scope

Recommended BACKEND-P5 should stay narrow and safe:

1. Add or document a production-backed runtime context read path only if it uses already persisted or already assembled data.
2. If timeframe is selected, source it from a trusted persisted analysis or market snapshot field, not from UI text.
3. If latest price is exposed, label it as quote/latest-price metadata only, not boundary entry.
4. Keep SourceTrace incomplete when OHLCV window, stale status, entry, stop, TP, RR, liquidity, event, or wick sources are missing.
5. Add tests proving that partial latest price and data quality do not complete SourceTrace.
6. Add tests proving that text execution-plan fields cannot become numeric boundary sources.
7. Keep all missing derivatives-risk fields fail-closed.
8. Do not call external APIs from dashboard detail adapters.
9. Do not modify `dashboard.html` unless a later frontend-specific task explicitly allows it.
10. Do not add order API, order placement, close position, reverse position, or auto-trading logic.

## 14. Tests

No tests were run for this P4 package.

Reason:

- This package is read-only audit plus documentation only.
- No Java production code was changed.
- No test code was changed.
- No dashboard, schema, config, or external integration was changed.

Suggested verification commands for reviewers:

```bash
git diff --name-only -- src/main/java src/test/java src/main/resources schema config
git diff --name-only -- docs
```

Expected result:

- no source / test / resource / schema / config changes,
- only the P4 audit document replaces the temporary cloud trigger artifact in this PR.

## 15. Boundary Confirmations

This P4 package confirms:

- no `dashboard.html` change,
- no Java production code change,
- no schema change,
- no external API integration added,
- no Coinglass integration added,
- no order API added,
- no auto-trading added,
- no actual ExecutionPlan generation added,
- no entry / stop / TP production numeric values generated,
- latest price is not treated as entry source,
- BoundaryCandidate `VALID` remains manual-review and not-trade-instruction,
- missing SourceTrace / derivatives-risk context remains fail-closed.

## 16. Current Conclusion

RuntimeKline readiness is not complete in the dashboard SourceTrace detail path.

The project has partial production-backed signals:

- quote/latest price display metadata,
- data quality metadata,
- partial multi-timeframe convergence label,
- partial market environment OI/funding snapshot signals.

However, these do not complete RuntimeKlineContext, SourceTrace, DerivativesRiskContext, BoundaryCandidate `VALID`, or ExecutionPlan readiness.

BACKEND-P5 should only wire fields that are already production-backed and semantically safe. Everything else must remain missing and fail-closed.
