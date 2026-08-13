# Fundamental AI v4.1 Frontend Contract Mapping

## Scope

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Base main: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Final frozen source SHA-256: `91bcfbd154bc43b2176107bfc65a948271e10e3e9862027f3647dc13bf5e0900`
- Frontend owners: existing Desktop Home and existing Analysis Detail only
- Schema, Mobile, Figma, and core decision algorithms: unchanged

## Mapping Matrix

| Frozen requirement | Existing backend/source owner | Frontend consumption | Before | Candidate alignment | Verification |
|---|---|---|---|---|---|
| Asset Pool may contain more than six assets | Existing `/api/asset-pool` APIs and persistent pool service | Asset Pool drawer in `dashboard.html` | Header controls existed but management was incomplete | Search input, single/batch add, single/batch delete, restore default, single/batch scan, status feedback | `FundamentalAiV41FrontendRuntimeAlignmentContractTest`; browser pool count 10 |
| Search by symbol, name, and alias | Existing `/api/asset-pool/search` | Search results in Asset Pool drawer | Search was not a complete working flow | Debounced real endpoint search with explicit result selection | Contract test; browser search for `ARB` |
| On-demand Three-AI preview is non-persistent | Existing `/api/asset-pool/search/{symbol}/analysis-preview` | Preview panel inside Asset Pool drawer | No complete preview boundary | Requires `previewOnly=true` and all opportunity/final persistence flags false before rendering | Contract test; `asset-search-three-ai-preview.png` |
| Explicit add is required before scheduling/ranking | Existing pool mutation endpoints | Add action in search result | Boundary not visible | Preview and add are separate actions; preview never mutates local pool or Top6 | Contract test; controlled browser count 10 -> 11 only after Add |
| Home Top6 is authoritative | `HomeTopOpportunityProjection` in Home response | Focus Asset cards | Legacy/default shortcuts could obscure ownership | Frontend consumes returned order only; no local ranking, fixed symbol, or default fill | Contract test; ranking service tests; `dynamic-top6.png` |
| Fewer than six opportunities remain fewer than six | Home projection | Focus Assets empty/partial renderer | Fill behavior was ambiguous | Exact response length is rendered; zero uses a real empty state | Contract test; `state-empty.png` |
| Asset click changes only selected analysis context | Home projection keyed by `analysisId` | Final Plan and Three-AI renderers | Context residue risk | Selection resets dependent Final/AI context and uses selected `analysisId` | Contract test; browser tab/context checks |
| Market Bias has exactly eight values | Merged v4.1 enums | `frontend-contract.js` | Legacy direction labels could act as fallback | Exact eight-value map; unknown/null fail closed | Contract test |
| Opportunity State has exactly eight values | Merged Opportunity model | Asset card state label | Could be conflated with execution permission | Exact eight-value map independent from bias and plan mode | Contract test |
| Plan Mode has exactly five values | Merged Final/Resolver model | Asset and Final Plan labels | Legacy status aliases could conflate meanings | Exact five-value map; `BLOCKED` is not observation | Contract test |
| Final Plan only after validation | Existing `ExecutionPlan` final record and rule validation | Final Plan region | Home exposed only a partial execution suggestion | Region opens only for `finalPlan=true`, validation PASS, complete chain/source gates, and `notTradeInstruction=true` | Contract and service tests; `candidate-not-final.png` |
| Complete Final Execution Plan fields | Existing merged `ExecutionPlan` columns | `DashboardHomeVO.ExecutionSuggestion` and Final renderer | Required fields were not projected | Existing VO extended with entry/stop/target logic, add/reduce/abandon conditions, RR, timeframes, validity, validation, reasons, and chain IDs | `DashboardHomeServiceImplTest`; `position-and-final-plan.png` |
| Candidate must not appear as Final | Candidate/Final persisted owners | Final Plan guard | Candidate status was not a complete UI gate | Candidate-only state renders an explicit non-final status and no plan field grid | Contract test; `candidate-not-final.png` |
| One Three-AI Workspace | Merged structured role results | Tabbed workspace in Home | Three role presentation was incomplete | One container, three tabs, one visible role panel | Contract test; browser role switch; `three-ai-workspace-consistency.png` |
| Role metadata | AI role result projection | Role metadata block | Provider/source/fallback metadata was not exposed | Projects `analysisId`, `traceId`, `generatedAt`, `roleState`, provider, source role, reason codes, fallback and reason | Service and contract tests |
| GPT structured semantics | GPT role result | GPT tab | Summary-heavy display | Core judgment, supporting/opposing evidence, collection states, 4h/1h/15m/5m, bias adjustment, candidate summary | Contract test and browser workspace evidence |
| Gemini structured semantics | Gemini role result | Gemini tab | Summary-heavy display | Evidence gaps, logic conflicts, underestimated risks, collection states, downgrade before/after/reason/recovery, enum result | Contract test and browser role switch |
| Grok structured semantics | Grok role result | Grok tab | Summary-heavy display | Failure paths, opposing scenarios, event/microstructure risks, watch indicators, collection states | Contract test and browser role switch |
| Role state is not collection state | Structured AI contract | `frontend-contract.js` and collection renderer | Empty states could collapse to generic text | Exact role states and exact collection states are independently mapped | Contract test; timeout fail-closed evidence |
| Anti-hallucination | Structured AI contract | Evidence/list renderers | Empty content could receive generic success text | Arrays are always arrays; state-specific empty messages; no cross-role fallback or fabricated items | Contract test; `three-ai-timeout-fail-closed.png` |
| AI Consistency is not a fourth role | Conflict Resolver projection | Compact adjacent summary | Boundary could be visually ambiguous | Only conflict level, final bias/mode, main reason, recovery condition, and data state; no vote/percentage | Contract test; workspace screenshot |
| Position source is explicit | Existing `UserPosition.sourceType/finalPlanId` | Position Monitoring source label | Source fields were not in Home VO | Projects `SYSTEM_PLAN_POSITION` with plan ID or `MANUAL_INDEPENDENT` without fabricated plan | Service and frontend contract tests |
| Position trust gate remains fail closed | Existing P2 trusted monitor contract | Position Monitoring renderer | Existing mapping needed regression protection | Untrusted/stale/pending positions hide risk, mark price, PnL, conclusion and action | Service tests; waiting/stale screenshots |
| Risk level and trend are independent | Existing P2 fields | Position summary | Escalation could be inferred from HIGH alone | Escalation shown only when trend is increased, never from level alone | Service tests; stable/increased browser scenarios |
| Analysis Detail owns full chain | Existing Analysis aggregate/audit endpoints | Existing `analysis-detail.html/js/css` | Detail omitted complete role/audit source chain | Existing page extended for 8 scores, evidence freshness, role tabs, resolver, rule validation, ordered audit stages and Final source | `AnalysisDetailFrontendContractTest`; detail screenshots |
| Score headline must be sourced | Exact `ScoreItem` list | Analysis Detail summary | Static `--` | Reads exact score item `scoreType=COMPREHENSIVE_CONFIDENCE`; missing remains `--` | `AnalysisDetailFrontendContractTest` |
| No automatic trading | Existing safety boundary | All user-visible plan/action copy | Regression risk | Final Plan is explicitly human review only; no order/open/close/reverse endpoint or action added | `StaticNoTradeInstructionGuardTest`; full Maven suite |

## Projection Changes

The only Java changes are extensions to the existing Home read projection:

- `DashboardHomeVO.Position`: `sourceType`, `finalPlanId`.
- `DashboardHomeVO.ExecutionSuggestion`: already persisted Final Plan fields and audit IDs.
- `DashboardHomeVO.AiRoleTab`: provider/source/fallback metadata already supplied by the merged role projection.
- `DashboardHomeServiceImpl`: direct, null-preserving mapping from existing owners. No semantic fallback and no new write path.

## Ownership Confirmation

No second Asset Pool, Opportunity, Candidate, Final Plan, AI trace, resolver, rule validation, Position, monitor, Home, or Analysis Detail owner was created. The frontend consumes merged-main owners through existing endpoints and the existing Home projection.
