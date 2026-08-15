# Fundamental AI v4.1 Target Runtime Blocker Remediation

Status: `FINAL_ONESHOT_CLOSURE_COMPLETE_PENDING_INDEPENDENT_REAUDIT`

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

## Final OneShot Closure

- `ProviderCapabilityRegistry` is the single pre-call capability owner for
  routed OHLCV, coordinated OHLCV refresh, current price, Binance derivatives,
  provider scan, and every analysis/Push Recheck/Position Monitoring path that
  converges on those adapters.
- The order is canonical identity, exact provider/market/contract/timeframe
  decision, external invocation, response validation, then an independently
  authorized fallback. A blocked primary never grants fallback permission.
- Unsupported symbol/timeframe, region restriction, provider disabled,
  source unavailable, not configured, and failed stale-directory revalidation
  all produce zero market-data calls.
- Stale capability is revalidated only through Kraken `AssetPairs` or Binance
  `exchangeInfo`; neither directory path probes an OHLCV, quote, funding, open
  interest, account, position, or order endpoint.
- Direct production dependencies on public quote/OHLCV clients are restricted
  to the existing router, snapshot adapters, and directory owners by an
  architecture test. `DIRECT_PROVIDER_BYPASS_COUNT=0`.
- CoinGlass RPM is nullable and explicit. Missing, zero/negative, and positive
  values remain distinct; no production source, config, script, or example
  environment contains an implicit 300 RPM fallback.
- The Binance market catalog also honors explicit provider/external-call
  enablement before its directory request and otherwise uses only the existing
  configured local catalog.

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

Local closure evidence is recorded in
`docs/FUNDAMENTAL_AI_V4_1_PR1187_FINAL_TARGET_RUNTIME_CLOSURE.md`. PR #1187
remains Draft and unmerged; this phase is not DONE until the independent
re-audit and later merge gates complete.
