# PHASE_HOME_SOURCE_FREEZE_DASHBOARD_SOURCETRACE_VISIBILITY

## 1. Document Purpose

This document freezes the current dashboard SourceTrace metadata visibility baseline.

It records the completed backend-to-homepage visibility chain for SourceTrace and DerivativesRiskContext metadata.

This is a documentation-only freeze record.

No code, schema, dashboard template, external integration, order API, or auto-trading behavior is changed by this document.

## 2. Freeze Scope

Freeze target:

- Dashboard SourceTrace metadata visibility
- Dashboard DerivativesRiskContext metadata visibility
- Read-only homepage diagnostics for selected asset detail
- Non-actionable safety boundaries
- Known missing fields and remaining fail-closed gaps

This freeze does not claim:

- SourceTrace is complete
- RuntimeKlineContext is complete
- DerivativesRiskContext production data is complete
- BoundaryCandidate VALID is a trade instruction
- ExecutionPlan readiness is executable
- order API or auto-trading exists

## 3. Completed Backend-To-Homepage Chain

The current completed chain is:

| Phase | Record | Completed Result |
|---|---|---|
| BACKEND-P2 | `PHASE_BACKEND_P2_DASHBOARD_SOURCETRACE_DETAIL_WIRING_RESULT.md` | `/api/dashboard/detail` exposes fail-closed `sourceTrace` and `derivativesRiskContext`. |
| BACKEND-P3 | `PHASE_BACKEND_P3_DASHBOARD_SOURCETRACE_PRODUCTION_READINESS_RESULT.md` | Production readiness remains partial; missing production sources stay explicit. |
| BACKEND-P6 | `PHASE_BACKEND_P6_DASHBOARD_TIMEFRAME_QUOTE_FRESHNESS_SOURCE_OWNERSHIP_RESULT.md` | Timeframe, quote ownership metadata, and RuntimeKline unavailable marker are exposed safely. |
| BACKEND-P7 | `PHASE_BACKEND_P7_DASHBOARD_SOURCETRACE_ANALYSIS_ANCHOR_METADATA_RESULT.md` | Decision and analysis anchor metadata are wired into SourceTrace. |
| HOME-SOURCE-P1 | `PHASE_HOME_SOURCE_P1_DASHBOARD_SOURCETRACE_METADATA_VISIBILITY_RESULT.md` | Existing SourceTrace and derivatives-risk metadata are surfaced in dashboard read-only diagnostics. |

The chain now supports reviewer visibility from persisted decision read-model metadata to dashboard diagnostics.

The chain remains read-only and fail-closed.

## 4. Current Visible SourceTrace Metadata

The homepage can surface these existing `/api/dashboard/detail` fields:

| Field | Meaning | Boundary |
|---|---|---|
| `sourceTrace.fallbackStatus` | SourceTrace fallback state | Shows incomplete/fail-closed state only. |
| `sourceTrace.decisionId` | Decision row anchor | Metadata only. |
| `sourceTrace.decisionIdSource` | Decision ID source label | Metadata only. |
| `sourceTrace.analysisId` | Analysis row anchor | Metadata only. |
| `sourceTrace.analysisIdSource` | Analysis ID source label | Metadata only. |
| `sourceTrace.decisionCreateTime` | Decision creation timestamp | Metadata only. |
| `sourceTrace.decisionCreateTimeSource` | Decision time source label | Metadata only. |
| `sourceTrace.timeframe` | Persisted analysis timeframe | Metadata only; does not complete RuntimeKline. |
| `sourceTrace.timeframeSource` | Timeframe source label | Metadata only. |
| `sourceTrace.quoteLatestPrice` | Quote latest price metadata | Not an entry source. |
| `sourceTrace.quoteLatestPriceSource` | Quote latest price source label | Not a boundary source. |
| `sourceTrace.quoteFreshnessStatus` | Quote update-time availability marker | Not kline stale status. |
| `sourceTrace.quotePriceUpdateTimeSource` | Quote update-time source label | Metadata only. |
| `sourceTrace.runtimeKlineContextStatus` | RuntimeKline availability marker | Currently `UNAVAILABLE`. |
| `sourceTrace.runtimeKlineContextSource` | RuntimeKline availability source label | Explains missing runtime context. |
| `sourceTrace.missingFields` | Compact missing-field summary | Diagnostics only. |

These fields are safe to display because they are ownership and status metadata.

They do not satisfy SourceTrace completeness.

They do not produce entry, stop, TP, RR, liquidity, event, or wick values.

## 5. Current Visible DerivativesRiskContext Metadata

The homepage can surface these existing `/api/dashboard/detail` fields:

| Field | Meaning | Boundary |
|---|---|---|
| `derivativesRiskContext.fallbackStatus` | Derivatives-risk fallback state | Usually `SAFE_FAIL_CLOSED_ONLY` when production data is missing. |
| `derivativesRiskContext.timeframe` | Persisted analysis timeframe | Metadata only. |
| `derivativesRiskContext.timeframeSource` | Timeframe source label | Metadata only. |
| `derivativesRiskContext.missingFields` | Compact missing-field summary | Diagnostics only. |

The homepage must keep derivatives-risk visibility fail-closed when OI, funding, liquidation, leverage, long/short, liquidity stress, event, or wick data is missing.

## 6. Non-Actionable Safety Boundaries

The current dashboard source metadata visibility remains non-actionable.

Required safety boundaries:

- SourceTrace metadata is not a trading signal.
- DerivativesRiskContext metadata is not a trading signal.
- BoundaryCandidate VALID remains manual-review only.
- BoundaryCandidate VALID remains not-trade-instruction.
- ExecutionPlan readiness is not automatic execution.
- Display-only metadata cannot produce an order.
- DTO-only metadata cannot produce an order.
- Missing source data cannot produce an order.
- `latestPrice` is quote metadata only.
- `latestPrice` is not entry source.
- quote freshness is not kline stale status.
- RuntimeKline `UNAVAILABLE` must remain visibly incomplete.
- SourceTrace `INCOMPLETE` must remain visibly incomplete.
- DerivativesRiskContext `SAFE_FAIL_CLOSED_ONLY` must remain visibly fail-closed.

Risk Action Guard principles still apply:

- high risk does not mean direct stop-loss,
- high risk does not mean direct reverse,
- wick/spike does not mean trend reversal,
- stampede state blocks new position, reverse, and opportunity push.

## 7. Known Remaining Gaps

Known gaps after the current freeze:

| Area | Current State | Required Future Work |
|---|---|---|
| RuntimeKlineContext | Unavailable in dashboard detail | Wire verified OHLCV window, freshness, and stale status before marking complete. |
| entry source | Missing | Add production-backed source assembler only when traceable. |
| stop source | Missing | Add production-backed source assembler only when traceable. |
| TP source | Missing | Add production-backed source assembler only when traceable. |
| RR source | Missing | Derive only from traceable entry / stop / TP inputs. |
| liquidity source | Missing | Keep fail-closed until production source is explicit. |
| event source | Missing | Keep WATCH_ONLY / SAFE_FAIL_CLOSED_ONLY until production source exists. |
| wick source | Missing | Keep WATCH_ONLY / SAFE_FAIL_CLOSED_ONLY until confirmation source exists. |
| OI history | Missing | Do not infer from display-only fields. |
| funding history | Missing | Do not infer from display-only fields. |
| liquidation cluster | Missing | Do not infer from display-only fields. |
| leverage distribution | Missing | Do not infer from display-only fields. |
| long/short ratio | Missing | Do not infer from display-only fields. |

These gaps are expected.

They are not regressions.

They define the next backend readiness boundary.

## 8. Recommended Next Work

Recommended next work:

1. Keep this HOME-SOURCE-FREEZE baseline stable before additional homepage source UI changes.
2. Add focused dashboard visibility tests only if future UI changes affect rendered labels.
3. Continue backend source readiness work before exposing any additional boundary fields.
4. Define a verified RuntimeKlineContext production wiring pack before changing RuntimeKline status.
5. Define a verified SourceTrace source assembler pack before exposing entry / stop / TP / RR fields.
6. Keep derivatives-risk data fail-closed until production sources are explicit and tested.
7. Keep Push / Recheck / Watchlist names aligned with review-only semantics.

Do not proceed to executable semantics from this freeze record.

## 9. Tests

Tests were not run for this freeze record.

Reason:

- documentation-only change,
- no code change,
- no dashboard template change,
- no schema change,
- no backend logic change.

## 10. Freeze Conclusion

The current dashboard SourceTrace metadata visibility chain is frozen as a safe read-only baseline.

The homepage now has enough metadata visibility for reviewers to understand why SourceTrace, RuntimeKlineContext, and derivatives-risk context remain incomplete or fail-closed.

The dashboard still does not generate trading actions.

The dashboard still does not display latestPrice as entry source.

The dashboard still does not make SourceTrace or RuntimeKline look complete.

The next implementation work should focus on verified production source readiness, not execution.
