# Fundamental AI v4.1 Scenario Validation Report

Status: `IMPLEMENTED_PENDING_INDEPENDENT_PRODUCT_AUDIT`

Current OneShot status: `33/33 CONTRACT SCENARIOS MAPPED`; target-provider
evidence remains explicitly separate from controlled runtime evidence.

## Final Interaction Required Scenarios

| # | Scenario | Evidence owner | Result |
|---:|---|---|---|
| 1 | First use / empty Pool | Asset Pool empty state + route QA | PASS |
| 2 | Top-up defaults and first scan | Pool service/controller + async state | PASS |
| 3 | Pool over 10 assets | persistent Pool tests | PASS |
| 4 | Dynamic Top6 six assets | ranking/Home tests | PASS |
| 5 | Dynamic Top6 fewer than six | ranking/Home fail-closed tests | PASS |
| 6 | Same-asset multi-timeframe aggregation | ranking aggregation test | PASS |
| 7 | Selected asset exits Top6 | URL/context contract test | PASS |
| 8 | Preview asset not in Pool | Preview source-gate test | PASS |
| 9 | Preview to explicit Pool add | controller/Pool tests | PASS |
| 10 | waiting_trigger to Final PREPARATION | decision-chain tests | PASS |
| 11 | triggered to Plan Revalidation | revalidation service tests | PASS |
| 12 | Five Plan Modes | resolver/validator/frontend tests | PASS |
| 13 | Six Plan lifecycle states | migration/mapper/UI contracts | PASS |
| 14 | GPT Preview mode | structured role contract | PASS |
| 15 | GPT Opportunity mode | orchestrator contract | PASS |
| 16 | Gemini structured review | structured role contract | PASS |
| 17 | Grok no-verifiable-failure-path | anti-hallucination contract | PASS |
| 18 | AI partial/fallback/unavailable | orchestrator/UI fail-closed tests | PASS |
| 19 | Valid Final to manual actual position | UserPosition boundary tests | PASS |
| 20 | Manual independent position | UserPosition source tests | PASS |
| 21 | Plan version changes, opening plan retained | lifecycle/position tests | PASS |
| 22 | Position risk escalation | Position Monitoring tests | PASS |
| 23 | Manual close to Review | position/review tests | PASS |
| 24 | Missed opportunity at-time/later split | review tests + V13 columns | PASS |
| 25 | Message Center and Telegram delivery | message/channel tests | PASS |
| 26 | Push Recheck seven results | existing Push Recheck tests + route | PASS |
| 27 | Hot Reset scoped revalidation | Hot Reset/revalidation tests | PASS |
| 28 | Event-window revalidation | event/revalidation contract tests | PASS |
| 29 | Account risk COMPLETE/PARTIAL/UNKNOWN | mapper/UI contract tests | PASS |
| 30 | AsyncTask partial/failure/retry | async service tests | PASS |
| 31 | Full Audit Chain partial/complete | audit query/route tests | PASS |
| 32 | No fake data | source/UI/browser gates | PASS |
| 33 | Automatic trading capability count | forbidden-capability scans | `0` |

The PASS labels above are product-contract and controlled-runtime results. They
do not convert local empty states into live provider evidence.

## Evidence Classes

- `BROWSER_CONTROLLED`: deterministic current-code fixture, suitable for UI and state-contract evidence only.
- `BACKEND_CONTROLLED`: existing service/controller tests using actual merged application ownership and transitions.
- `STATIC_CONTRACT`: source-level guard against fallback, duplicate ownership, or forbidden UI behavior.
- `TARGET_RUNTIME`: live target provider/runtime evidence. Not available in this package.

Fixture values are never treated as live market, AI, or opportunity evidence.

## Required Scenarios A-J

| Scenario | Evidence | Result | Notes |
|---|---|---|---|
| A. Pool > 6 and dynamic Top6 | Browser pool contains 10 manageable assets; Home shows exactly six authoritative projections. Add/remove/restore/scan interactions complete. `PersistentAssetPoolServiceTest` and `OpportunityPriorityRankingServiceImplTest` validate storage and ranking changes. | PASS | No JavaScript ranking or fixed-symbol fill. `v4_1_latest_ui/runtime/04-dynamic-top6-six.png`. |
| B. Fewer than six qualified opportunities | Partial controlled response renders five assets; no default assets or fake values are inserted. | PASS | `v4_1_latest_ui/runtime/05-dynamic-top6-less-than-six.png`; static Top6 contract test. |
| C. Search asset on-demand analysis | Real input accepts a symbol/name/alias query and exposes results; structured preview continues to require all persistence booleans false. | PASS | `v4_1_latest_ui/runtime/06-search-input.png`; contract tests. |
| D. Explicitly add to Pool | Add changes controlled pool count only after the explicit action; scan reports per-symbol success/partial/error state. Existing pool service tests cover persisted add and observing entry. | PASS | Browser count 10 -> 11; batch scan 10 success, 0 pending. |
| E. Complete Final Plan | Validated Final renders complete fields; AI explanations remain in the workspace. Candidate/non-Final response renders no Final body. Decision-chain and rule-validator tests cover resolver/validation ownership. | PASS | `v4_1_latest_ui/runtime/10-execution-final.png`, `11-execution-blocked.png`. |
| F. Five Plan Modes | Exact `CONFIRMATION`, `PREPARATION`, `REDUCED`, `OBSERVATION`, `BLOCKED` maps are asserted independently from bias/state. Resolver/rule-validator tests cover mode transitions and blocked state. | PASS | No `BLOCKED` -> observation alias. |
| G. Three-AI empty-value contract | Exact role states, collection states, empty arrays, `NONE_FOUND`, `INSUFFICIENT_DATA`, `SOURCE_UNAVAILABLE`, `STALE`, and `NO_VERIFIABLE_FAILURE_PATH` are guarded. | PASS | Frontend contract test plus structured AI backend tests. |
| H. AI exception paths | Timeout/unavailable role renders real `UNAVAILABLE` and `SOURCE_UNAVAILABLE`; no evidence is synthesized. Existing orchestrator tests cover exception, timeout, fallback, and partial results. | PASS | `v4_1_latest_ui/runtime/16-ai-partial-failure.png`. |
| I. System-plan position | `SYSTEM_PLAN_POSITION` requires and displays `finalPlanId`; trusted monitoring is independent from plan display. User position service and Home service tests prove manual creation boundary. | PASS | Browser verified-monitor scenario plus service tests. |
| J. Independent manual position | `MANUAL_INDEPENDENT` remains valid without a fabricated Final Plan; Home maps its explicit source and only provable monitor fields. | PASS | `UserPositionServiceImplTest`, `DashboardHomeServiceImplTest`, frontend contract test. |

## Position Trust Scenarios

| Scenario | Expected | Result |
|---|---|---|
| Position exists, no trusted monitor | Entry facts only; risk, mark price, PnL, conclusion, and action hidden | PASS |
| Verified and fresh monitor | Frozen monitoring fields visible | PASS |
| `HIGH + STABLE` | High risk shown, no escalation inference | PASS |
| `HIGH + INCREASED` | Risk escalation shown | PASS |
| Stale monitor | Fail closed to unavailable/waiting state | PASS |
| Multiple positions | Each position keeps independent risk and trend | PASS |

Latest visual evidence: `v4_1_latest_ui/runtime/08-position-no-position.png` and `09-position-open-top3.png`. Existing backend/service scenarios retain the trusted, risk-trend and stale-data assertions.

## Latest UI Runtime Scenarios

| Scenario | Observed result | Result |
|---|---|---|
| First viewport | System Status, alert/event, Dynamic Top6 and entry into the 60/40 decision region are present at `1440 x 900` | PASS |
| Latest Figma baseline | Nodes `28:154`, `31:23`, `520:212`, `523:748`, `35:97`, `35:4`, `35:35`, `35:66` captured; old node `519:3` rejected | PASS |
| Asset context switch | BTC to ETH changes Final Plan and all role analysis IDs; System Status, alerts/events and positions remain unchanged | PASS |
| Single AI workspace | GPT, Gemini and Grok each render alone when selected | PASS |
| Empty structured evidence | Collection state remains explicit and arrays remain empty; no `--` or fabricated evidence | PASS |
| Light / dark | Latest visual tokens render both modes with no overflow | PASS |
| Browser quality | Horizontal/text overflow, top-level overlap, console error/warning, unhandled rejection and detached busy state all equal zero | PASS |

Evidence: `docs/evidence/v4_1_latest_ui/README.md` and `browser-qa.json`.

## Page-State Scenarios

| State | Result |
|---|---|
| Loading | PASS |
| Empty | PASS |
| Partial | PASS |
| Error with retry | PASS |
| Candidate but not Final | PASS |
| AI unavailable/timeout | PASS |

## Real Scenario Status

```text
CONTROLLED_CONTRACT_AND_BROWSER_SCENARIOS=PASS
TARGET_RUNTIME_LIVE_PROVIDER_EVIDENCE=TARGET_RUNTIME_EVIDENCE_PENDING
REAL_SCENARIO_STATUS=PARTIAL
```

This remaining evidence does not block frontend contract alignment. It does block any claim that the screenshots prove live provider accuracy or production acceptance.

## Post-Authorization Revalidation

All `33/33` controlled contract scenarios were rerun or re-bound to the exact
post-sync application hashes. The four Desktop widths and 14-route sweep used
the authenticated application, not the visual fixture. Dynamic opportunity,
Final-only, structured Three-AI, manual position, fail-closed and zero-auto-
trading assertions remained PASS. Target-provider evidence remains a separate
acceptance class and was not fabricated by this package.

`POST_AUTH_SCENARIO_STATUS = 33/33 PASS`
