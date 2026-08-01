# Trade Model V1 Product Completion Matrix

Status: `P1A_AUDIT_COMPLETE_P1B_AUTHORIZATION_PENDING_MERGED_MAIN`

This matrix recalculates maturity from product evidence, not code volume, test count, PR count, or Governance status. Its Product Foundation baseline is effective at main `0828a9dc034174093f06815e2f79a221ddaa1ef4`. The completed P1A audit and pending P1B authorization do not advance Home maturity. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## 1. Allowed Product States

Only these states may be used:

- `NOT_STARTED`
- `STRUCTURE_ONLY`
- `PARTIAL`
- `FUNCTIONAL_UNVALIDATED`
- `REAL_SCENARIO_VALIDATED`
- `DEPLOYMENT_READY`
- `EFFECTIVE_IN_PRODUCTION`

Definitions:

| State | Product meaning |
|---|---|
| NOT_STARTED | no usable product path found |
| STRUCTURE_ONLY | shell, DTO, contract, placeholder, or isolated read exists without complete module flow |
| PARTIAL | meaningful portions exist, but product/design/data/interaction/state coverage is incomplete |
| FUNCTIONAL_UNVALIDATED | end-to-end function is plausible and test-backed, but required real scenario and/or target-device validation is absent |
| REAL_SCENARIO_VALIDATED | real data or verifiable historical scenario has passed with traceable evidence |
| DEPLOYMENT_READY | real-scenario validated plus production configuration, security, observability, rollback, and device/browser readiness |
| EFFECTIVE_IN_PRODUCTION | deployed and continuously verified in the intended production environment |

No business module is `REAL_SCENARIO_VALIDATED`, `DEPLOYMENT_READY`, or `EFFECTIVE_IN_PRODUCTION` in this P0 baseline.

## 2. Dimension Scale

Dimension cells use `PASS`, `PARTIAL`, `MISSING`, or `NOT_RUN`. A module's overall state is constrained by its weakest required acceptance evidence, especially Real Data, Real Interaction, Real Scenario, iPhone, and Deployment.

## 3. Product Completion Matrix

| Module | Overall state | Product contract | Design | Semantics | Real data wiring | Real interaction | Five-state handling | Real scenario | iPhone | Deployment | Evidence | Principal gap |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Login | FUNCTIONAL_UNVALIDATED | PASS | PARTIAL | PASS | PASS | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | Spring Security/Login page and tests | no real iPhone/session-expiry/deployment scenario |
| Home | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | `GET /api/dashboard/home`, desktop/mobile shell, contract tests, completed `P1A_HOME_ALIGNMENT_AUDIT.md` | bounded P1B implementation plus final interaction/field provenance/screenshot/real-data acceptance absent |
| Focus Assets | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | Home asset projection and selected-symbol flow | full bias/confidence/state/MTF/evidence trace not accepted together |
| Execution Plan | FUNCTIONAL_UNVALIDATED | PASS | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | plan service/VO/Home projection and safety contracts | complete exact-plan read trace and real-market validation absent |
| Eight Scores | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL / FALLBACK_PRESENT | PARTIAL | PARTIAL | NOT_RUN | PARTIAL | MISSING | `GET /api/score/list`, `ScoreController`, and `ScoreServiceImpl` light-rule/base-score path | complete normalized-evidence inputs, calibration, exact analysis snapshot, and formal Home-confidence use are unvalidated |
| Three AI | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PASS | NOT_RUN | MISSING | MISSING | three fixed roles, hard availability gate, FE-03 detail link | unified real evidence package, model run, four-level conflict/fallback not proven end to end |
| Positions | FUNCTIONAL_UNVALIDATED | PASS | PARTIAL | PASS | PASS | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | manual-open/manual-close/open/exact owner-scoped APIs and pages | real user workflow, remaining quantity, target-device evidence absent |
| Position Monitoring | FUNCTIONAL_UNVALIDATED | PASS | PARTIAL | PASS | PARTIAL | PARTIAL | PASS | NOT_RUN | MISSING | MISSING | exact owner read, latest resolver/logs, fail-closed contracts | live cadence, reversal/wick/liquidity/risk scenarios not validated |
| Messages | PARTIAL | PASS | PARTIAL | PASS | PASS | MISSING | PASS | NOT_RUN | MISSING | MISSING | OPPORTUNITY public and POSITION_RISK private read projections | Message Center/Push Detail product UI and screenshots absent |
| Detail Pages | PARTIAL | PASS | PARTIAL | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | Analysis Detail, Asset Detail, position/read routes | exact-plan/position/push/replay details not one accepted coherent set |
| Review | FUNCTIONAL_UNVALIDATED | PASS | MISSING | PASS | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | review aggregate/summary/detail/log endpoints and tests | real closed-position and missed-opportunity outcomes absent |
| My | STRUCTURE_ONLY | PARTIAL | PARTIAL | PARTIAL | MISSING | PARTIAL | PARTIAL | NOT_RUN | MISSING | MISSING | shell/profile node and logout foundation | formal field contract and real account/settings data absent |
| iPhone | FUNCTIONAL_UNVALIDATED | PASS | PARTIAL | PASS | PARTIAL | PARTIAL | PASS | NOT_RUN | PARTIAL | MISSING | merged Xcode SwiftUI/WKWebView app; Debug/Release simulator builds; 47 unit/security/project tests; 1 UI test; simulator install/launch | real-device install/login/Session lifecycle, real network/server, background recovery, final device layout, and distribution are unvalidated |
| Server | FUNCTIONAL_UNVALIDATED | PASS | N/A | PASS | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | N/A | MISSING | Spring Boot service, migrations/config/status contracts | production deployment/HTTPS/secrets/rollback/long-run evidence absent |
| Production Data | PARTIAL | PASS | N/A | PARTIAL | PARTIAL | N/A | PARTIAL | NOT_RUN | N/A | MISSING | market/provider/source-health foundations | sustained real multi-source coverage/freshness/data-quality evidence absent |
| Observability | PARTIAL | PARTIAL | N/A | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NOT_RUN | N/A | MISSING | health/status/trace/log surfaces | production alerts, SLOs, retention, incident and recovery proof absent |

## 4. Evidence Interpretation

### What is genuinely present

- A substantial rule/evidence/score/plan/AI/position/review backend foundation.
- An eight-score endpoint and derived light-rule implementation, with fixed base/default values still present; this is engineering foundation, not proof of the formal real-evidence score chain or calibrated confidence.
- Authenticated manual UserPosition APIs with owner-scoped exact reads.
- Position monitoring read projection and state-contract hardening.
- Public OPPORTUNITY versus private POSITION_RISK message projection and read-state model.
- Desktop/mobile web shells, Analysis Detail reuse, and frontend contract tests.
- A merged iPhone Xcode SwiftUI/WKWebView foundation with simulator build/test/install/launch evidence.
- Extensive automated tests and workflow checks as engineering evidence.

### What those facts do not prove

- They do not prove final Home/Figma alignment.
- They do not prove real-provider freshness across all required sources.
- They do not prove a real user position changes monitor state under real market movement.
- They do not prove three-AI quality or calibrated conflict handling on real evidence.
- They do not prove Message Center/Push Detail UI exists and works.
- They do not prove real iPhone usability.
- They do not prove production deployment readiness or live operational effectiveness.

## 5. Module Notes

### Login

Authentication structure is functional in a web test environment. It remains unvalidated on real iPhone/WKWebView lifecycle, deployed HTTPS/cookie settings, session expiry, and recovery.

### Home and Focus Assets

The Home read model and responsive shell are meaningful. P1A has completed the read-only gap decision and identified the ExecutionPlan/UserPosition projection coupling, but no Home implementation or acceptance capability moved. Product acceptance still requires the final module order, asset-card context-only click, exact plan/AI linkage, complete source mapping, real screenshots, and real data in all five states. Those are not one proven flow today.

### Execution Plan

The plan domain is mature relative to other modules and has strong boundary tests. It remains advisory and must keep exact source/version trace. A generation endpoint or DTO is insufficient; P1 must verify the displayed plan against real evidence and no stale/latest fallback.

### Three AI

The fixed roles and unavailable-role fail-closed behavior exist. Real model calls over one immutable evidence package, rule-first authority, four conflict levels, fallback, and trace quality require P3 validation.

### Eight Scores

`GET /api/score/list` returns all eight named dimensions, but its current path derives scores from a constructed environment and an empty analysis object while using fixed base/default values and light rules. It is `PARTIAL / FUNCTIONAL_UNVALIDATED` with `FALLBACK_PRESENT`; it is not evidence-driven complete, calibrated, production-ready, or authorized as the formal Home confidence source.

### Positions and Monitoring

Manual user facts, exact IDs, owner isolation, read pages, logs, and state resolver are present. The product plan additionally requires real moving-price scenarios, plan-versus-actual comparison, wick filtering, reversal classification, risk and suggestion quality, alerts, and full archive.

### Messages

The backend privacy/state contract is meaningful, but the Message Center and Push Detail UI remain unaccepted. Therefore the module is not functional from the user's perspective.

### Review

Read models and classifications exist. No recorded real closed-position or missed-opportunity scenario establishes product validity.

### My, Server, Production Data, Observability

These remain material delivery gaps. A browser shell, status endpoint, or test environment is not equivalent to real account content, a deployed server, sustained production data, or operational readiness.

### iPhone

The merged Xcode project, SwiftUI/WKWebView client, Session/Cookie/CSRF compatibility foundation, simulator Debug/Release builds, 47 unit/security/project tests, one UI launch test, and simulator install/launch establish a real simulator-level app foundation. They do not establish real-device login/session persistence, production-server connectivity, background recovery, final size/accessibility interaction acceptance, TestFlight/distribution, or production effectiveness.

## 6. Completion Rule

A module advances only when evidence satisfies `docs/PRODUCT_ACCEPTANCE_STANDARD.md`. Docs-only, DTO-only, review-only, preview-only, dashboard-only, fallback-only, no-op, mock-only, or automated-test-only work cannot advance a business module to completion.
