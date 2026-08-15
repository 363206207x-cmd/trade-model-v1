# Fundamental AI v4.1 Target Runtime Blocker Remediation

Status: `IMPLEMENTATION_COMPLETE_PENDING_INDEPENDENT_AUDIT`

Authorized package:
`FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`

Base main: `b1b49a0de4090fd93a12b14e18c1c980669d0162`

## Scope And Result

| Blocker | Existing owner reused | Implemented result |
|---|---|---|
| B01 | Maven/Spring Boot build and Flyway V1-V13 | Standard `clean package` now includes Flyway core, PostgreSQL support and V1-V13. Production owns schema initialization through Flyway; packaged startup fails closed on migration validation failure. |
| B02 | Canonical instrument registry, provider adapters and Asset Pool scan | Exact provider/instrument/timeframe capability states, canonical HTTP 451 `REGION_RESTRICTED`, no quote substitution, and per-asset scan isolation with truthful aggregate counts. |
| B03 | AI provider clients, budget guard and provider readiness | Missing/zero/positive configuration states, exact frozen-model verification, cache-only status reads, authenticated explicit reverify and fallback-not-ready enforcement. |
| B04 | Existing password policy, PersonalUser bootstrap and actuator readiness | One password policy owner, explicit bootstrap states, readiness/liveness separation, secret-safe preflight and secure password generation. |

CoinGlass remains the existing evidence provider. Missing key or disabled state is
`NOT_CONFIGURED`; it neither fabricates derivatives evidence nor replaces the
OHLCV provider.

## API Delta

Additive authenticated endpoints only:

- `POST /api/asset-pool/scan-summary`
- `POST /api/asset-pool/batch-scan-summary`
- `GET /api/asset-pool/capabilities/{symbol}`
- `POST /api/ai/providers/{provider}/reverify`

`GET /api/ai/orchestrator/status` adds sanitized configuration-presence and
canonical readiness fields. No secret value, authorization header or full
prompt is returned.

## Persistence And Product Boundaries

- `SCHEMA_CHANGED=NO`; V1-V13 are reused unchanged and no V14 was needed.
- `DATA_QUALITY_ALGORITHM_CHANGED=NO`.
- `DATA_QUALITY_THRESHOLD_CHANGED=NO`.
- `CANDIDATE_PROMOTION_THRESHOLD_CHANGED=NO`.
- Data Quality 55 remains `OBSERVING + BLOCKED` and is not treated as a defect.
- `FAKE_EVIDENCE_COUNT=0`; missing values remain missing or fail closed.
- Figma, Desktop, Mobile, Opportunity transitions, Three-AI authority and
  Candidate/Final contracts are unchanged.
- Automatic open, close, add, reduce, reverse, order and trade capability: `0`.

## Duplicate Skeleton Check

- 是否创建新骨架: NO. New records are supporting runtime states under existing owners.
- 是否复用 Cursor-era 资产: YES. Existing Flyway, provider, Asset Pool, AI and auth owners are reused.
- 是否减少重复: YES. The special Flyway profile and duplicate password checks were removed.
- 是否提升 capability level: YES. Packaging/readiness moved from documented intent to executable target-runtime behavior.
- 是否接 service/runtime/dashboard/API: YES. Existing runtime services and authenticated status APIs expose the new states.
- 是否符合 #830 审计建议: YES. No duplicate business family or placeholder-only package was introduced.

## Independent Audit Boundary

The independent audit must use the exact implementation Head and rerun the
standard packaged-JAR PostgreSQL smoke, focused contracts, full Maven suite,
Product Source Gate, Workflow Contract and authorization validator. This
implementation does not self-approve, merge, deploy or perform live-secret
acceptance.
