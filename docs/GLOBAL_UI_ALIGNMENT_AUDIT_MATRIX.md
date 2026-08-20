# Fundamental AI Global UI Alignment Audit Matrix

Baseline: `97697e84caf5da2279233530912319b6561e9c2e`

Authority:

1. `唯一产品开发方案_最终冻结版.docx`
2. `Fundamental_AI_v4.1_最终交互逻辑与页面设计开发规格_冻结版.docx`
3. `Fundamental_AI_v4.1_UI设计与交互执行冻结文件_今日细化最终版.docx`
4. Owner scope override in the current task

This matrix is the pre-edit mapping required by the current package. `Status` describes the audited baseline, not the intended result.

| ID | Frozen requirement / finding | Template / route | CSS / shell | Runtime / mapper | UI review fixture | Contract / acceptance test | Baseline status | This change |
|---|---|---|---|---|---|---|---|---|
| H-01 | Contract tests use the 2026-08-20 UI freeze and allow `持仓监控 · 基于已录入` and `Final Execution Plan` | `home.html` | `home.css` | `home-runtime.js` | `UiReviewDashboardHomeService` | `ApprovedFigmaHomeRuntimeContractTest`, new global contract test | FAIL | Replace legacy copy/pixel assertions with frozen semantic geometry assertions |
| H-02 | Exactly one `/dashboard` Home; no legacy Home branch | `home.html`, `workspace.html`, `DashboardController`, `DesktopWorkspaceController` | `home.css`, `workspace.css` | `home-runtime.js`, `workspace.js` | N/A | new global contract test | FAIL | Remove Home markup/runtime/styles from workspace while preserving non-Home routes |
| H-03 | No current-UI 60:40 or `3fr 2fr` residue | `workspace.html`, current task pages | `workspace.css`, tests/docs | `workspace.js` | N/A | new residue test | FAIL | Delete legacy Home residue and update stale tests/docs in current UI scope |
| H-04 | One AppShell/token system across Home and primary task pages | `home.html`, `workspace.html` | `home.css`, `workspace.css` | shared semantic contract | fixture uses same routes | shell/IA tests | PARTIAL | Align rail, header, padding, gap, focus and token names |
| H-05 | Collapsed 64px rail; optional 216px overlay expansion that never pushes content | `home.html`, `workspace.html` | `home.css`, `workspace.css` | workspace navigation handlers | N/A | shell contract test | PARTIAL | Add one overlay rail behavior and shared geometry |
| H-06 | PageHeader 52px, horizontal padding 24px, module gap 16px | `home.html`, `workspace.html` | both stylesheets | N/A | actual route rendering | browser geometry test | FAIL | Normalize shell dimensions |
| H-07 | Exact frozen color tokens | all current Desktop templates | `home.css`, `workspace.css` | semantic tone mapper | fixture exercises tones | token contract test | FAIL | Define and consume the exact 11 frozen tokens |
| S-01 | Status scopes exactly Environment / System / Data / Service / Recorded Account / Hot Reset | `home.html` | `home.css` | `renderSystemStatus` and header projection | full-state fixture | status contract test | FAIL | Rename and bind six exact scopes |
| S-02 | Opportunity count absent from SystemStatusBar | `home.html` | N/A | `renderSystemStatus` | fixture still has six cards outside bar | status contract test | FAIL | Remove status opportunity binding |
| S-03 | Account copy explicitly based on recorded positions; empty = `— / 无已录入持仓` | `home.html` | semantic tones | Dashboard header/status mapping | 3-position aggregate | status/empty-state tests | FAIL | Bind account aggregate and canonical empty copy |
| O-01 | Backend Top6 excludes invalidated/cooling/confused/BLOCKED/high-risk and dedupes by asset | N/A | N/A | `DashboardHomeServiceImpl` projection | fixture contains only eligible projections | service + frontend defensive tests | PARTIAL | Enforce backend eligibility and deterministic dedupe |
| O-02 | Frontend defensively rejects ineligible/duplicate cards and never pads | `home.html` | grid only | `renderOpportunities` | six legal cards | runtime contract test | FAIL | Add eligibility filter/dedupe before rendering |
| O-03 | Every card uses its own Final, timeframe, conflict and ranking fields | opportunity card renderer | `home.css` | Asset projection fields and semantic mapper | per-card varied projections | per-card contract test | FAIL | Remove selected-Final assumptions and show projection-owned fields |
| O-04 | No validated Final renders `—`, never Candidate/Preview | opportunity card renderer | compact final strip | access guard | mixed fixture | no-final test | PASS/PARTIAL | Preserve guard and add defensive assertions |
| O-05 | Triggered revalidation uses `正在重验` | opportunity card renderer | state badge | semantic mapper | triggered fixture asset | state-copy test | FAIL | Add canonical revalidation label |
| O-06 | Main-content breakpoints: 6x1 at >=1240, 3x2 below 1240, no horizontal scroll, card 112-128px | Home opportunity section | `home.css` container queries | N/A | six-card fixture | geometry/browser tests | FAIL | Establish main container and exact queries |
| D-01 | Decision grid is 7fr/3fr with 16px gap | Home decision row | `home.css` | N/A | full fixture | geometry test | FAIL | Replace pixel-fr ratio with frozen semantic grid |
| D-02 | Below 1120 main-content width order is Plan then Position | Home decision row | `home.css` container query | N/A | full fixture | browser/order test | FAIL | Stack and order exact modules |
| P-01 | Position title is `持仓监控 · 基于已录入` | `home.html`, Positions IA | styles | runtime aggregate | 3-row fixture | copy contract | FAIL | Restore frozen title |
| P-02 | Position row uses 22/28/28/22 columns | renderer markup | `home.css` | PositionVO | varied fixture | structure/geometry tests | FAIL | Rebuild compact row groups without changing backend fields |
| P-03 | Identity: symbol, direction, sourceType only | row renderer | alignment rules | source normalization | system/manual examples | semantic binding test | FAIL | Remove openedAt from identity |
| P-04 | Facts: entry/mark and PnL/openedAt | row renderer | tabular numeric alignment | trust-gated fields | trusted and untrusted examples | binding/trust tests | FAIL | Group persistent and market facts correctly |
| P-05 | Judgment: logic/reversal and risk/riskTrend | row renderer | centered short states | independent backend fields | normal/warning/negative states | distinct-binding test | FAIL | Restore riskTrend and separate judgments |
| P-06 | Conclusion/action: monitorConclusion and suggestedAction + detail | row renderer | left narrative/action | no fallback derivation | varied conclusions | distinct-binding test | FAIL | Remove semantic fallback and group conclusion/action |
| P-07 | Risk reason and monitor time stay out of Home compact row | row renderer | N/A | remain in detail APIs | fixture may carry but Home hides | absence test | FAIL | Remove from compact renderer |
| P-08 | Logic and conclusion remain independently owned and worded | row renderer | semantic tones | VO fields | valid logic + distinct conclusion | no-fallback test | PARTIAL | Bind independently and use distinct visible labels |
| P-09 | VERIFIED+FRESH shows trusted fields; Pending/Stale/Invalid/SourceUnavailable hide them while keeping opening facts | row renderer | unknown/warning tones | `dataState`/trust contract | all five trust states | fail-closed tests | PARTIAL | Centralize trust gate and render state-specific unknowns |
| P-10 | Canonical sources: SYSTEM_PLAN_POSITION with finalPlanId; independent = MANUAL_INDEPENDENT | row renderer | source badge | boundary normalization | both source types | source test | FAIL | Normalize legacy aliases only at boundary |
| P-11 | Semantic tones are centralized, text/badge only, with non-color cues | all state-bearing components | exact token tones | semantic mapper | tone fixture set | tone/accessibility tests | FAIL | Add shared tone resolver and visible labels |
| F-01 | Title exactly `Final Execution Plan` | `home.html`, plan detail | `home.css` | plan renderer | validated Final | copy contract | FAIL | Rename module and route copy |
| F-02 | Exactly three layers: status, key conditions, metadata | plan renderer | three-layer CSS | validated Final mapper | complete Final fixture | structure test | PARTIAL | Remove generic fact-grid semantics and bind version |
| F-03 | Lifecycle tone reflects lifecycle state | plan renderer | semantic tone tokens | lifecycle mapper | valid/revalidation/blocked fixtures | tone test | FAIL | Apply lifecycle-specific tone |
| F-04 | Confused/BLOCKED and needs-revalidation show reason, recovery and latest revalidation | plan renderer | compact reason block | Final projection | controlled states | state contract test | FAIL | Render source-defined state details |
| F-05 | Only validated Final may populate module | plan renderer | N/A | shared plan access gate | complete/no-final fixtures | guard test | PASS | Preserve and broaden regression coverage |
| A-01 | One workspace and three tabs only | `home.html`, Analysis IA | AI layout | role normalization | all roles | workspace contract | PASS | Preserve |
| A-02 | Gemini reviewResult is authoritative; contradiction fails closed | role renderer | role state tone | structured role mapper | approve/downgrade/unavailable | contradiction test | FAIL | Gate adjustment UI on usable reviewResult |
| A-03 | Grok FOUND renders source-backed trigger -> evolution -> invalidation | role renderer | failure-path layout | structured challenge mapper | FOUND path | causal-path test | PARTIAL | Render exact chain fields |
| A-04 | Conflict Summary consumes resolver fields and never synthesizes explanations | conflict aside | secondary styling | resolver result mapper | L2 fixture | resolver-origin test | PARTIAL | Remove generic defaults; fail closed |
| A-05 | Preserve approved hierarchy terms; waiting_trigger only with PREPARATION | role renderer | N/A | semantic mapper | legal selected chain | state/mode test | PARTIAL | Preserve terms and validate legal pair |
| A-06 | Role tabs support arrows, Home/End, roving tabindex and visible focus | AI tabs | focus tokens | keyboard handlers | browser fixture | keyboard test | FAIL | Add WAI-ARIA tab behavior |
| IA-01 | Positions IA order and CTA hierarchy match freeze | `workspace.html` positions section | shared task styles | workspace positions runtime | UI review data | IA order/empty tests | FAIL | Recompose existing section without new business fields |
| IA-02 | Analysis IA includes search, Mode, quality, MTF, evidence/scores, AI tabs, valid Opportunity/Final, Preview boundary | analysis section | shared task styles | existing analysis/search APIs | UI review projection | IA/preview tests | FAIL | Recompose and keep preview isolated |
| IA-03 | Messages uses only in-app Message facts, three display groups and target routing; no Preview noise | messages section | shared task styles | message API/runtime | controlled in-app messages | IA/Telegram absence tests | FAIL | Remove channel delivery presentation and regroup messages |
| IA-04 | Me has left anchors and one settings surface for Risk Preference and Asset Pool/Data Source; Save only when dirty | me section | shared settings layout | existing settings/runtime | controlled settings | dirty-state/absence tests | FAIL | Remove Telegram/auth surfaces and add bounded dirty state |
| IA-05 | No Telegram UI/copy/settings/retry/O10/handlers/fixtures/assertions/evidence | workspace and overlays | workspace styles | workspace runtime frontend only | no Telegram fixture | global absence test | FAIL | Remove all current frontend Telegram ownership; leave dormant backend untouched |
| IA-06 | Login/session/security files untouched and excluded | `login.html`, auth controllers | `login.css` | auth runtime | N/A | scope diff assertion | PASS | No changes |
| FD-01 | One reusable FocusedDetailShell, preferably plan detail | plan/detail section | shared focused shell | existing plan endpoint | controlled plan | shell/route contract test | FAIL | Implement reusable shell around existing plan route |
| FD-02 | Focused detail inherits rail/header/tokens/focus and has 760-960px surface or 8+4 | workspace plan route | workspace CSS | N/A | controlled plan | geometry/browser test | FAIL | Add focused shell variant |
| FD-03 | Focused detail contains one object/task and no Home status/Top6/70:30/AI wall | plan route conditional markup | styles | runtime plan load | controlled plan | absence test | FAIL | Isolate detail content |
| T-01 | Static contracts enforce current semantics, not old screenshots/pixels | all relevant templates | CSS contracts | runtime | fixture | current frontend tests | FAIL | Update stale tests and add one frozen alignment suite |
| T-02 | Copy inventory is recalculated; auth strings excluded | all current Desktop pages except login/auth | styles N/A | generated user copy | fixture copy | inventory test | FAIL | Replace old 108-row assumption with current inventory |
| V-01 | Normal mode contains no fixture data; ui-review has zero external calls and production guard | actual `/dashboard` | same styles | profile guard | full fixture | runtime/browser acceptance | PASS | Preserve and rerun |
| V-02 | Visual evidence covers Home, responsive layouts, tones, trust states, roles, primary IA and focused detail | all scoped routes | all scoped CSS | all scoped runtime | full fixture | screenshot manifest | FAIL | Capture actual browser evidence after implementation |

## Explicit boundaries

- No Figma edits.
- No schema or migration edits.
- No login, logout, session, authentication controller, security configuration or auth-test edits.
- No automatic trading, automatic position mutation or order endpoint.
- No fake data in normal or production mode.
- Existing backend Telegram implementation is not removed or expanded; only current frontend/UI exposure is removed.
- PR #1195 remains Draft and unmerged for Owner review.
