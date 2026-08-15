# Fundamental AI v4.1 Latest Approved UI Figma Mapping

## Audit Identity

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Branch: `codex/v4-1-frontend-runtime-alignment`
- PR: `#1179` (Draft)
- Starting implementation head: `490919d6f8c763ffaac634cfbffd02ad8eaf66c4`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Figma file: `rdMYmsAvZYkXHJX8hdl7UN`
- Rejected visual baseline: node `519:3` (`Desktop Home / P1-KB Final Contract`)

Business semantics come from the final frozen product contract. Figma controls visual structure and expression. The merged API remains the only runtime data owner. No screenshot or fixture value is a production data source.

## Visual Contract Matrix

| Module | Figma Source Node | Current Old UI | New Implementation | Data Source | Visual Test |
|---|---|---|---|---|---|
| Navigation | Approved Design Foundation and frozen Home shell | Legacy shell remained visually coupled to old Home cards | Existing fixed Desktop navigation retained; latest Home begins in a dedicated production root | Existing route/navigation contract | `runtime/01-desktop-1440x900-light.png` |
| Header | Approved Home interaction and Design Foundation | Old dashboard heading and runtime panels competed with product content | Compact title, data-updated context, theme and global actions; legacy diagnostics remain outside the production Home root | Dashboard Home metadata | `runtime/01-desktop-1440x900-light.png` |
| System Status | Approved Home interaction and Design Foundation | Status was presented through the old metric-tile treatment | One restrained six-state strip: Market Trend, Risk Level, Data Quality, AI System, Executable Opportunity, Hot Reset | `DashboardHomeVO.systemState` and existing status projections | `runtime/01-desktop-1440x900-light.png` |
| Risk Alert | Approved Home interaction and Design Foundation | Old generic dashboard alert card | Compact risk-event row with severity, title, time, and existing Message Center target | Existing Dashboard Home alert projection | `runtime/01-desktop-1440x900-light.png` |
| Event Calendar | Approved Home interaction and Design Foundation | Old generic dashboard event card | Compact event row paired with alert, preserving Home summary boundary | Existing Dashboard Home event projection | `runtime/01-desktop-1440x900-light.png` |
| Dynamic Top6 | `28:154` | Three/fixed-symbol visual assumptions and legacy coin tiles | Latest Asset Card structure, up to six cards in authoritative backend order; no local ranking or default fill | `HomeTopOpportunityProjection` | `runtime/04-dynamic-top6-six.png`; `runtime/05-dynamic-top6-less-than-six.png` |
| Search / Asset Pool | `28:154` plus frozen Asset Pool interaction | Search could be decorative and management was visually incomplete | Native search input, suggestions, Add, Asset Pool, Remove, Restore Default, scan state and Pool management | Existing `/api/asset-pool/**` endpoints | `runtime/06-search-input.png`; `runtime/07-asset-pool-open.png` |
| Position Monitoring | `520:212`, `523:748`, with container reference `32:26` | Old database-like fields and legacy row/card selectors | P1-KD three-level Top3 rows: judgment, position facts, monitoring basis; explicit No Position and missing-trust states | `DashboardHomeVO.Position` and P2 trusted monitor contract | `runtime/08-position-no-position.png`; `runtime/09-position-open-top3.png` |
| Final Execution Plan | `31:23` | Partial suggestion and long generic field treatment; Candidate risked sharing presentation | Compact Final-only result card, priorities grouped as direction/mode, entry-stop-target, validity, risk limits, secondary conditions | `DashboardHomeVO.ExecutionSuggestion` guarded by `frontendContract.executionPlanAccess` | `runtime/10-execution-final.png`; `runtime/11-execution-blocked.png` |
| GPT_FINAL | `35:4` in component set `35:97` | Legacy summary card and shared generic AI layout | Current-role content in one workspace: core judgment, supporting/opposing evidence, multi-timeframe explanation, bias adjustment and Candidate rationale | Structured GPT role projection | `runtime/12-gpt-tab.png` |
| GEMINI_REVIEW | `35:35` in component set `35:97` | Legacy summary card | Evidence gaps, logic conflicts, underestimated risks, downgrade before/after/reason, recovery and review result | Structured Gemini role projection | `runtime/13-gemini-tab.png` |
| GROK_CHALLENGE | `35:66` in component set `35:97` | Legacy summary/news-like treatment | Failure paths, opposing scenarios, external-event and microstructure risks, watch indicators | Structured Grok role projection | `runtime/14-grok-tab.png` |
| Three-AI Workspace | `35:97` | Three cards or three simultaneous role summaries | One workspace, three tabs, one visible role, structured role metadata and detail entry | `DashboardHomeVO.AiDecision` | `runtime/12-gpt-tab.png`; `runtime/13-gemini-tab.png`; `runtime/14-grok-tab.png` |
| AI Consistency | Approved adjunct treatment beside `35:97` and frozen consistency contract | Could read as a fourth AI module or pseudo-vote | Compact resolver summary only: conflict level, final bias/mode, reason, recovery and data state | Conflict Resolver projection | `runtime/15-ai-consistency.png` |
| Empty / Partial / Error | Component state sets `28:154`, `523:748`, `35:97` | Generic `--` placeholders and visually complete fake states | Explicit loading, partial, unavailable, waiting, blocked and structured collection states; no fabricated plan/evidence/value | Existing module state, trust and collection-state contracts | `runtime/05-dynamic-top6-less-than-six.png`; `runtime/08-position-no-position.png`; `runtime/11-execution-blocked.png`; `runtime/16-ai-partial-failure.png`; `runtime/17-empty-evidence.png` |

## Production Ownership

- DOM and rendering: `src/main/resources/templates/dashboard.html`
- Latest Desktop visual layer: `src/main/resources/static/css/dashboard-latest.css`
- Frozen enum/state guards: `src/main/resources/static/js/frontend-contract.js`
- Deterministic visual input only: `scripts/dashboard-visual-acceptance-fixture.py`
- Contract tests: `FundamentalAiV41FrontendRuntimeAlignmentContractTest`, `DashboardControllerTest`
- Screenshot and metric index: `docs/evidence/v4_1_latest_ui/README.md`, `browser-qa.json`

## Binding Boundaries

1. Asset selection updates only Final Plan, GPT/Gemini/Grok, AI Consistency, and selected-asset context.
2. System Status, alerts/events, Dynamic Top6 composition, and User Positions do not change with the selected asset.
3. Candidate content cannot enter the Final Plan body.
4. Position Monitoring never derives a User Position from an Execution Plan.
5. Missing, stale, pending, invalid, or unavailable data remains null/closed rather than receiving a display default.

## Result

```text
LATEST_FIGMA_BASELINE_IDENTIFIED=PASS
OLD_P1KB_HOME_USED_AS_TARGET=NO
LATEST_UI_PRODUCTION_BINDING=PASS
FIGMA_CHANGED=NO
```
