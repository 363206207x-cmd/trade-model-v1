# Fundamental AI v4.1 Target Runtime Blocker Remediation Authorization

Authorization status: `AUTHORIZED_PENDING_MERGED_MAIN`

Exact authorized package:
`FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION`

Risk: `B_RISK_TARGET_RUNTIME_REMEDIATION`

Implementation status: `NOT_STARTED`

Source date: `2026-08-15`

## 1. Authority and Effectivity

This authorization is subordinate to the sole active v4.1 Product Source:

`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

Its scope is derived only from the four reproduced blockers in the merged-main
target-runtime acceptance at exact commit
`3a6f56afaf6fbba3d094d532f7f9555a23ac30a1` and the mappings in:

- `docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_SOURCE_MAPPING.md`;
- `docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_OWNERSHIP_MAP.md`.

This authorization becomes effective only after its reviewed commit is merged
to clean, synchronized `main`, the Product Source Gate, Workflow Contract,
authorization validator, Duplicate Skeleton gate and Maven validation pass,
and the exact machine-readable package resolves as allowed.

## 2. Exact Allowed Scope

The exact package may:

1. create one remediation branch and one Draft PR from the authorization merge;
2. modify standard Maven release dependencies/packaging so ordinary
   `./mvnw clean package` includes the existing Flyway runtime;
3. modify only necessary target-runtime settings in `application.yml` and
   `application-prod.yml`;
4. extend existing provider capability and exact instrument mapping;
5. classify HTTP 451 as `REGION_RESTRICTED` without meaningless retries;
6. isolate scan results per asset and report truthful aggregate state;
7. extend existing AI provider RPM, cost, budget and exact-model readiness;
8. add a secret-safe, cached target-runtime preflight;
9. reuse the existing PasswordPolicy for bootstrap/preflight validation;
10. add a secure random runtime-password helper that does not persist or log
    a password by default;
11. align required bootstrap failure with readiness while preserving existing
    user no-overwrite behavior;
12. add or update tests and deployment/environment/smoke/audit handoff docs;
13. extend existing API status fields only when required for explicit states;
14. add sequential migration V14 only if independent persistence is genuinely
    required and no existing owner can carry the state;
15. validate with isolated PostgreSQL and secret-free test environments;
16. prepare an independent remediation audit handoff after implementation.

## 3. CoinGlass Boundary

```text
COINGLASS_IMPLEMENTATION_CONTRACT_ALLOWED=true
COINGLASS_LIVE_SECRET_REQUIRED_FOR_IMPLEMENTATION=false
COINGLASS_SECRET_REPOSITORY_WRITE_ALLOWED=false
COINGLASS_LIVE_ACCEPTANCE_DEFERRED=true
```

The exact package may verify configuration, `CG-API-KEY` header construction,
rate limits, `NOT_CONFIGURED` fail-closed behavior and
`sourceId/observedAt/freshness` provenance with a mock HTTP server. Such a test
is protocol evidence, never live-provider evidence. CoinGlass must not replace
the OHLCV primary source. Subscription RPM is injected configuration, never a
business constant.

## 4. Forbidden Scope

The package must not:

- modify the canonical product design, Figma, Desktop UI or Mobile;
- change Eight Scores, data-quality formula or thresholds;
- change Market Bias, Opportunity State, Plan Mode, Candidate thresholds or
  Final validation;
- weaken Three-AI authority, Candidate/Final separation, Final/UserPosition
  separation, Position Monitoring trust, or Push Recheck semantics;
- remove ADA to hide provider coverage or silently substitute quote assets;
- fabricate market data, Evidence, AI output, Opportunity, Candidate or Final;
- create a second provider, instrument, AI, auth or migration system;
- submit or print API keys, passwords, database credentials, cookies or auth
  headers;
- inject a live CoinGlass key into the repository;
- create or mutate UserPosition automatically;
- automatically open, close, add, reduce, reverse, order or trade;
- deploy a target or production runtime;
- begin another product package.

## 5. Machine Permission Contract

Before this authorization is merged, every remediation permission remains
`false`. After merged-main effectivity, and only for the exact package:

```text
REQUESTED_PACKAGE: FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION
REQUEST_CLASS: AUTHORIZED_IMPLEMENTATION_PACKAGE
REPOSITORY_EDITS_ALLOWED: true
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
IMPLEMENTATION_STATUS: NOT_STARTED
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: false
MOBILE_IMPLEMENTATION_ALLOWED: false
```

The merged final-interaction package, abbreviations, typos, broader packages,
automatic-trading packages, Mobile packages and Figma packages resolve all
three repository/implementation/PR permissions to `false`.

## 6. Capability Boundary

This authorization changes permission records only.

- `CAPABILITY_MOVEMENT=NONE`
- `TARGET_RUNTIME_STATUS=BLOCKED_BY_IMPLEMENTATION_DEFECT`
- `DEPLOYMENT_READINESS=BLOCKED`
- `IMPLEMENTATION_STATUS=NOT_STARTED`
