# Fundamental AI v4.1 Scenario Validation Report

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
| First viewport | System Status, alert/event, Dynamic Top6 and entry into the 70/30 decision region are present at `1440 x 900` | PASS |
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
