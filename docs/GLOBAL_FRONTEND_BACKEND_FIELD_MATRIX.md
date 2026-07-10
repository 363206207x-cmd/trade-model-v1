# Global Frontend / Backend Field Matrix

## Scope

This matrix traces the primary Dashboard Home and Review Center labels from renderer to VO/service, upstream source, persistence, empty-state behavior, and test coverage. Repeated six-asset and four-review-tab rows are grouped when their contracts are identical.

## Dashboard Header and System State

| UI field | Frontend source | Backend mapping | Authoritative source | Empty behavior | Coverage | Classification |
|---|---|---|---|---|---|---|
| Selected symbol | `renderDashboardHome` / asset selection | `DashboardHomeVO.header.selectedSymbol` | Request selection, then first focus asset | First supported focus asset | Controller/service tests | `IMPLEMENTED_AND_TRACED` |
| Rule version | `home.header.ruleVersion` | latest decision rule version | `tm_decision_result.rule_version` | Blank/waiting | Service tests | `IMPLEMENTED_AND_TRACED` |
| Updated time | `home.header.updatedAt` | max relevant update time | Aggregated persisted records | Compact display | Service tests | `IMPLEMENTED_AND_TRACED` |
| Market trend | top status card | `systemState.marketTrend` | selected decision `market_bias` | `暂无` | Template and service tests | `IMPLEMENTED_AND_TRACED` |
| Risk level | top status card | `systemState.riskLevel` | selected decision `risk_level` | `暂无` | Template and service tests | `IMPLEMENTED_AND_TRACED` |
| Data quality | top status card | `systemState.dataQuality` | selected decision `data_quality_score` | blank/waiting | Service tests | `IMPLEMENTED_AND_TRACED` |
| AI conflict | top status card | `systemState.aiConflict` | selected decision conflict level/score | `暂无` | Service tests | `IMPLEMENTED_AND_TRACED` |
| Pending review | top status card | `systemState.pendingReview` | push review-pending aggregate | zero | Service tests | `IMPLEMENTED_AND_TRACED` |
| Confused | top status card | `systemState.confused` | Light system status / decision asset state | `否` or waiting | Service tests | `IMPLEMENTED_AND_TRACED` |
| Hot Reset | top status card | `systemState.hotReset` | latest Hot Reset status | waiting/none | Service tests | `IMPLEMENTED_AND_TRACED` |

## Focus Assets

The fixed visual slots are BTC/USDT, ETH/USDT, SOL/USDT, BNB/USDT, XRP/USDT, and DOGE/USDT. They are display slots, not fabricated positions.

| UI field | VO field | Service source | Persistence/source | Empty behavior | Classification |
|---|---|---|---|---|---|
| Symbol | `assets[].symbol` | focus-asset list and selected decision | request/config + decision lookup | fixed symbol remains visible | `IMPLEMENTED_AND_TRACED` |
| Direction | `assets[].marketBias` | decision market bias | `tm_decision_result.market_bias` | value area blank | `IMPLEMENTED_AND_TRACED` |
| Composite score | `assets[].compositeScore` | explicitly null | no authoritative composite score exists | score pill with blank value | `NOT_IMPLEMENTED` |
| Confidence | `assets[].confidenceLevel` | decision confidence | `tm_decision_result.confidence_level` | blank | `IMPLEMENTED_AND_TRACED` |
| Risk | `assets[].riskLevel` | decision risk | `tm_decision_result.risk_level` | blank | `IMPLEMENTED_AND_TRACED` |
| Asset state | `assets[].assetState` | exact enum snapshot mapping | `tm_decision_result.asset_state_snapshot` / `tm_asset_state` | waiting | `IMPLEMENTED_AND_TRACED` |
| Worth opening | `assets[].worthOpening` | rule decision result only | `tm_decision_result.worth_opening` | blank | `IMPLEMENTED_AND_TRACED` |

## Alerts, Events, and Diagnostics

| UI field/group | Backend source | Persistence/source | Empty behavior | Classification |
|---|---|---|---|---|
| Active alerts | home alerts | `tm_monitor_alert` | compact empty state | `IMPLEMENTED_AND_TRACED` |
| Latest critical events | diagnostics/event summary | Hot Reset, push recheck, monitor/review logs | compact empty state | `IMPLEMENTED_AND_TRACED` |
| Provider readiness | diagnostics | config/readiness helpers | `WAITING_SYNC` / configured | `SEMANTIC_DRIFT` |
| Telegram status | push/diagnostics | fixed readonly `WAITING_SYNC` contract | no verified Telegram connection source | waiting | `PLACEHOLDER_ONLY` |
| Global status check icon | static template icon plus dynamic text | icon does not follow backend state | template | checkmark can appear while waiting | `RENDERED_NOT_BACKED` |

## Position Monitor

Only rows returned from `DashboardHomeVO.positions` are rendered. An execution suggestion is not converted into a position.

| UI field | VO/source field | Service mapping | Authoritative source | Empty behavior | Classification |
|---|---|---|---|---|---|
| Asset / direction | `symbol`, `direction` | UserPosition VO | `tm_user_position` | `暂无持仓` | `IMPLEMENTED_AND_TRACED` |
| User entry price | `entryPrice` | UserPosition VO | `tm_user_position.entry_price` | blank | `IMPLEMENTED_AND_TRACED` |
| Current price | `currentPrice` | `MarketQuoteClient` snapshot | live/public quote client | blank on unavailable | `IMPLEMENTED_AND_TRACED` |
| Floating PnL | `floatingPnl`, `pnlPct` | long/short calculation when quote exists | position + quote | blank on unavailable | `IMPLEMENTED_AND_TRACED` |
| Leverage | `leverage` | UserPosition VO | `tm_user_position.leverage` | blank | `IMPLEMENTED_AND_TRACED` |
| Position size | `positionSize` | UserPosition quantity | `tm_user_position.quantity` | blank | `IMPLEMENTED_AND_TRACED` |
| Entry logic | `entryLogicStatus` | latest monitor log | `tm_position_monitor_log` | `等待监控` | `IMPLEMENTED_AND_TRACED` |
| Direction support | `directionSupportStatus` | latest monitor log | `tm_position_monitor_log` | `等待同步` | `IMPLEMENTED_AND_TRACED` |
| Reversal status | `reversalStatus` | latest monitor log | `tm_position_monitor_log` | `等待监控` | `IMPLEMENTED_AND_TRACED` |
| Risk level | `riskLevel` | latest monitor log / position risk | monitor and risk snapshot | `等待同步` | `IMPLEMENTED_AND_TRACED` |
| Current advice | `suggestedManualAction` / display text | conservative monitor enum mapping | `tm_position_monitor_log.suggested_action` | `等待监控` | `IMPLEMENTED_AND_TRACED` |
| Next validation | frontend countdown | dashboard refresh cadence, not backend judgment | browser timer | `等待监控` | `IMPLEMENTED_AND_TRACED` |
| Record close | row action | manual-close API | `tm_user_position` update + review handoff | hidden without position ID | `IMPLEMENTED_AND_TRACED` |
| Position source | `sourceType` | service hardcodes `MANUAL` | persisted source is not preserved | no warning | `WRONG_SOURCE_MAPPING` |
| Optional quantity/leverage | form defaults omitted values to `1` | manual-open payload | browser-generated value | not visibly distinguished | `RENDERED_NOT_BACKED` |

## Execution Suggestion

| UI field | VO field | Backend source | Boundary rule | Empty behavior | Classification |
|---|---|---|---|---|---|
| Readiness status | derived from boundary completeness | `hasCompleteBoundary` and overloaded `validPeriod` | entry + stop + TP required | `当前暂无完整执行计划` | `WRONG_SOURCE_MAPPING` |
| Direction | `direction` | decision/plan direction | rule decision | blank when incomplete | `IMPLEMENTED_AND_TRACED` |
| Entry zone | `entryZone` | execution plan boundary | market-structure source trace | blank when incomplete | `BLOCKED_NO_REAL_DATA` |
| Stop loss | `stopLoss` | execution plan boundary | market-structure source trace | blank when incomplete | `BLOCKED_NO_REAL_DATA` |
| Take-profit rules | `takeProfitRules` | execution plan boundary | market-structure source trace | blank when incomplete | `BLOCKED_NO_REAL_DATA` |
| Leverage | `leverageSuggestion` | execution plan | fallback `1-5x` when no trace | visually hidden when incomplete | `PLACEHOLDER_ONLY` |
| Position suggestion | `positionSuggestion` | execution plan | fallback risk sentence when no trace | visually hidden when incomplete | `PLACEHOLDER_ONLY` |
| Valid period | `validPeriod` | decision/plan mapper | service may place readiness text here | hidden when incomplete | `WRONG_SOURCE_MAPPING` |
| Invalid condition | `invalidCondition` | plan first, decision fallback | complete plan required by home renderer | blank when incomplete | `IMPLEMENTED_AND_TRACED` |

## AI Decision Workspace

| UI field/group | VO field | Intended source | Actual source behavior | Classification |
|---|---|---|---|---|
| GPT final summary | `aiDecision.roles.GPT_FINAL` | structured final adjudicator result | JSON parser plus decision fallback | `WRONG_SOURCE_MAPPING` |
| Gemini review summary | `aiDecision.roles.GEMINI_REVIEW` | structured conflict review | producer stores non-JSON compact text; parser yields empty | `RENDERED_NOT_BACKED` |
| Grok challenge summary | `aiDecision.roles.GROK_CHALLENGE` | structured counter-challenge | producer stores non-JSON compact text; parser yields empty | `RENDERED_NOT_BACKED` |
| Role evidence lists | support/objection/risk/block fields | AI result persistence | no authoritative structured role-evidence store | `NOT_IMPLEMENTED` |
| Final tendency | consistency final tendency | rule direction/final decision | decision result | `IMPLEMENTED_AND_TRACED` |
| Conflict level | consistency conflict level | AI conflict resolver | decision result | `IMPLEMENTED_AND_TRACED` |
| Conflict score | consistency conflict score | AI conflict resolver | decision result | `IMPLEMENTED_AND_TRACED` |
| Plan mode | consistency plan mode | resolver/orchestrator | decision result | `IMPLEMENTED_AND_TRACED` |
| Confused blocked | consistency blocked state | decision asset/confused state | decision result | `IMPLEMENTED_AND_TRACED` |
| Consistency score | `consistencyScore` | no implemented calculation | always null | `PLACEHOLDER_ONLY` |
| Consistency level | `consistencyLevel` | no implemented calculation | always null | `PLACEHOLDER_ONLY` |
| One-line summary | `consistencySummary` | no implemented synthesis | always null | `PLACEHOLDER_ONLY` |

## Push Inbox

| UI field | Backend source | Persistence/source | Empty behavior | Classification |
|---|---|---|---|---|
| Pending count | PushSnapshot aggregate | `tm_push_snapshot` | zero | `IMPLEMENTED_AND_TRACED` |
| Review-passed count | review-only recheck aggregate | `tm_push_snapshot` / `tm_push_recheck_log` | zero | `IMPLEMENTED_AND_TRACED` |
| Invalidated count | recheck aggregate | push tables | zero | `IMPLEMENTED_AND_TRACED` |
| Position risk | hardcoded zero | no source | zero | `PLACEHOLDER_ONLY` |
| Inbox items | recent snapshots and recheck state | push tables | empty list | `IMPLEMENTED_AND_TRACED` |
| Telegram | fixed waiting status | no verified connection source | `WAITING_SYNC` | `PLACEHOLDER_ONLY` |

## Review Center

| Tab/field group | API source | Persistence/source | Empty behavior | Classification |
|---|---|---|---|---|
| Position reviews | closed manual positions + review projection | `tm_user_position`, monitor/review records | empty list | `IMPLEMENTED_AND_TRACED` |
| Opportunity reviews | final OpportunityLog outcomes | `tm_opportunity_log` | empty list | `IMPLEMENTED_AND_TRACED` |
| Push reviews | snapshots and latest recheck | `tm_push_snapshot`, `tm_push_recheck_log` | empty list | `IMPLEMENTED_AND_TRACED` |
| Rule feedback | review result + rule version logs | `tm_review_result`, `tm_rule_version_log` | empty list | `IMPLEMENTED_AND_TRACED` |
| Push clicked state | no authoritative source | always null | blank | `NOT_IMPLEMENTED` |
| Rule-feedback processing status | no workflow source | null | blank | `NOT_IMPLEMENTED` |
| Review diagnostics | local source availability | ready/empty strings | empty | `SEMANTIC_DRIFT` |

## Backend Fields Not Fully Used by the Frontend

| Backend data | Current use | Classification |
|---|---|---|
| Seven non-trend score categories | persisted but not consumed by decision | `BACKEND_FIELD_UNUSED` |
| Five of eight score rows | omitted by top-three summary | `IMPLEMENTED_NOT_RENDERED` |
| Detailed AI call logs | audit storage only | `IMPLEMENTED_NOT_RENDERED` |
| Full source-trace boundary metadata | plan generation/tests; only summarized on home | `IMPLEMENTED_NOT_RENDERED` |
| Opportunity MFE/MAE and detailed evaluation | available through opportunity/review paths, not home | `IMPLEMENTED_NOT_RENDERED` |
| Exact position monitor log history | latest summary only on home | `IMPLEMENTED_NOT_RENDERED` |
