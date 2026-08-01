# P1A Home Alignment Readiness and Gap Audit

Status: `COMPLETED`

This is the durable repository record for the completed read-only P1A audit.
It records product gaps and the bounded input to P1B; it does not claim that
Home is implemented, accepted, or complete.

## Audit Basis

- `docs/PRODUCT_SOURCE_OF_TRUTH.md`
- `docs/product-sources/V1_PRODUCT_ARCHITECTURE.md`
- `docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md`
- `docs/design/P3_U2_IPHONE_HOME_IA_V2.md`
- `docs/design/P3_U2_IPHONE_HOME_FIELD_MAPPING.md`
- `docs/PRODUCT_ACCEPTANCE_STANDARD.md`
- Current `GET /api/dashboard/home` read projection and desktop/mobile bindings

## Read-Only Findings

| Area | Current evidence | Gap retained for implementation or later validation |
|---|---|---|
| Home structure | Desktop and mobile Home shells expose status, asset, plan, AI, position, and event content. | The final module order is not aligned consistently across desktop and mobile. |
| Focus assets | Selected-symbol context and substantial asset-card fields exist. | Complete provenance, multi-timeframe meaning, data quality, freshness, and calibrated confidence are not proven together. |
| Asset interaction | Asset selection can request a new Home context. | Selection must update AssetState, ExecutionPlan, three-AI summary, and consistency without navigating or selecting a UserPosition by symbol. |
| Execution Plan | The Home projection can expose an existing asset plan. | A matching UserPosition can replace the asset plan with `POSITION_MONITORING`; these product domains must be separated. |
| Three AI | The fixed GPT Final, Gemini Review, and Grok Challenge roles and unavailable-role fail-closed behavior exist. | One immutable real evidence package, conflict semantics, and end-to-end real model evidence remain unaccepted. |
| Top3 positions | Owner-scoped open UserPosition data is available independently. | Position content must remain unchanged by selected-asset context and must never gate the asset plan. |
| Five states | Several fail-closed and stale-cache protections exist. | READY, PARTIAL, EMPTY, ERROR, and MISSING still require one aligned desktop/mobile acceptance flow. |
| Real scenario | Automated contract evidence exists. | Required payload traces, screenshots, asset-switch scenarios, partial data, forced failure, iPhone, and real-market acceptance remain outstanding. |

## Field-Source Result

The audit classifies the current Home field set as mixed `REAL`, `DERIVED`,
`FALLBACK`, `MISSING`, and `UNKNOWN`. No fallback score or label is authorized
as calibrated confidence. Exact field provenance and real-scenario acceptance
remain P1B validation work; the P1A audit does not upgrade Home beyond
`PARTIAL`.

## Bounded P1B Input

The independently reviewed implementation input is
`docs/P1B_AUTHORIZATION_SCOPE.md`:

- separate the selected asset's ExecutionPlan projection from owner-scoped
  UserPosition/Position Monitor content;
- preserve the existing endpoint and JSON shape;
- bind the final Home order and selected-asset context only within the
  authorized Home read projection;
- clear selected-asset success data on Error or Missing;
- keep all paths read-only with no position or trading mutation.

P1B remains unavailable until that authorization record and its operational
handoff are independently reviewed, merged to clean/synced `main`, and accepted
by `scripts/v1-state.sh` with Product Source Gate `PASS` and no conflicting PR.

## Audit Decision

- `P1A_COMPLETION_STATUS: COMPLETED`
- `HOME_PRODUCT_STATUS: PARTIAL`
- `P1B_SCOPE_DECISION: APPROVED_PENDING_MERGED_MAIN`
- `BUSINESS_CODE_CHANGED: NO`
- `UI_CHANGED: NO`
- `TRADING_CAPABILITY_MOVEMENT: NONE`
