# Release Owner Decision Register

Package: PDR-LIVE18 Release Owner Decision Register / Waiver Policy
Branch: `codex/pdr-live18-release-owner-decision-register`
Current main commit: `f0998b3a`
Status date: 2026-07-09

Production deployment readiness: BLOCKED
Deployment decision: DO NOT DEPLOY
Release owner status: RECORDED_2026_08_23

## Scope

This package records the release-owner decision register and waiver policy for remaining production-readiness gates. It is not production deployment. It does not access a production server, production database, real secret store, or provider secret. It does not print or commit secrets. It does not call providers and does not add trading runtime behavior.

## Sources Reviewed

- `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md`
- `docs/FINAL_CONDITIONAL_READINESS_REVIEW.md`
- `docs/AI_EXTERNAL_PROVIDER_RELEASE_POLICY_EVIDENCE.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`
- `docs/PROJECT_CURRENT_STATE.md`
- `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`

## Existing Owner / Waiver Evidence

Historical snapshot from the original PDR-LIVE18 package; superseded for the
current private single-user candidate by the 2026-08-23 section below.

No explicit release-owner approval, provider waiver, deployment timing approval, rollback owner approval, or incident owner approval was found in the reviewed status-source documents.

Current classification:

| Item | Status | Interpretation |
|---|---:|---|
| Release owner identity / approval | MISSING_EVIDENCE | No named release owner has approved a release bundle. |
| Waiver policy | REGISTERED_BY_THIS_PACKAGE | This package defines the register and waiver requirements, but approves no waiver. |
| Existing provider waiver | MISSING_EVIDENCE | No OpenAI/Gemini/xAI/external provider waiver exists. |
| Deployment approval | MISSING_EVIDENCE | No deployment timing or go/no-go approval exists. |

## Waiver Policy

1. No waiver is approved automatically.
2. `SKIPPED_MISSING_SECRET` is not PASS.
3. `DOCUMENTED_WITH_PLAN` is not PASS.
4. `DOCUMENTED_WITH_CONFIG` is not PASS.
5. `OPTIONAL_WITH_WAIVER` is not effective unless a release owner explicitly records the provider/gate, target release, reason, fallback behavior, residual risk, and expiration/review date.
6. Real server smoke is non-waivable for `READY`. It is also required before `CONDITIONALLY_READY_CANDIDATE` unless a separate owner-approved private-only staging exception names the exact target and endpoint constraints.
7. A waived optional dependency cannot introduce order execution, auto-open, auto-close, auto-reverse, auto-trading, external Push send, fake positions, or fake review records.
8. Production readiness remains BLOCKED until the release evidence bundle records PASS evidence or explicit release-owner-approved conditional evidence for every required gate.

## Decision Register

| Gate / decision item | Current evidence | Current decision status | Blocks CONDITIONALLY_READY_CANDIDATE | Blocks READY | Exact owner decision or evidence required |
|---|---:|---:|---:|---:|---|
| Real server smoke | SKIPPED_MISSING_CONTROLLED_SERVER | REQUIRED_PASS | YES | YES | Provide controlled non-production or production-like server smoke PASS for health/readiness and core APIs. |
| HTTPS / proxy auth smoke | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Provide TLS/redirect/HSTS/forwarded-header/authenticated dashboard-review smoke through intended proxy, or an explicit private-only staging exception for candidate only. |
| Real secret-store injection | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Provide redacted secret-store names/versions/injection evidence with no values. |
| Credential rotation drill | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Run and record admin, datasource, Binance/API, and AI provider credential rotation drills, or owner-approved staged rotation plan for candidate only. |
| OpenAI | LIVE_PROVIDER_PASS_2026_08_23 | RELEASE_OWNER_DECISION_REQUIRED | YES | YES | Controlled redacted connectivity passed; choose REQUIRED_PASS, OPTIONAL_WITH_WAIVER, or DISABLED_FOR_RELEASE and require application-lineage evidence when enabled. |
| Gemini | LIVE_PROVIDER_HTTP_400_2026_08_23 | REQUIRED_PASS | YES | YES | Correct the provider request/runtime configuration in a separately authorized task, then provide redacted connectivity and application-lineage PASS. |
| xAI / Grok | LIVE_PROVIDER_PASS_2026_08_23 | RELEASE_OWNER_DECISION_REQUIRED | YES | YES | Controlled redacted connectivity passed; choose REQUIRED_PASS, OPTIONAL_WITH_WAIVER, or DISABLED_FOR_RELEASE and require application-lineage evidence when enabled. |
| External context / news / macro / ETF | SKIPPED_MISSING_SECRET | RELEASE_OWNER_DECISION_REQUIRED | YES | YES | Choose REQUIRED_PASS, OPTIONAL_WITH_WAIVER, DISABLED_FOR_RELEASE, or NOT_APPLICABLE; if required, provide controlled PASS evidence. |
| Release timing | MISSING_EVIDENCE | RELEASE_OWNER_DECISION_REQUIRED | YES | YES | Release owner must approve target window, rollback cutoff, and go/no-go criteria. |
| Rollback owner | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Name rollback owner and confirm backup/restore authority and rollback decision path. |
| Incident owner | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Name incident owner and confirm monitoring, escalation, and communication responsibility. |
| Final release-owner approval | MISSING_EVIDENCE | REQUIRED_PASS | YES | YES | Release owner must approve the completed redacted release evidence bundle. |

## Exact Owner Decisions Required

1. Name the release owner for the target release candidate.
2. Decide whether OpenAI is `REQUIRED_PASS`, `OPTIONAL_WITH_WAIVER`, or `DISABLED_FOR_RELEASE`.
3. Decide whether Gemini is `REQUIRED_PASS`, `OPTIONAL_WITH_WAIVER`, or `DISABLED_FOR_RELEASE`.
4. Decide whether xAI/Grok is `REQUIRED_PASS`, `OPTIONAL_WITH_WAIVER`, or `DISABLED_FOR_RELEASE`.
5. Decide whether external context/news/macro/ETF is `REQUIRED_PASS`, `OPTIONAL_WITH_WAIVER`, `DISABLED_FOR_RELEASE`, or `NOT_APPLICABLE`.
6. Decide whether a private-only staging exception is allowed for HTTPS/proxy evidence before candidate review; no exception can make the system `READY`.
7. Approve or reject any staged credential-rotation plan before candidate review; actual drill evidence is still required before `READY` unless explicitly waived with residual risk.
8. Name rollback and incident owners.
9. Approve release timing only after evidence is complete.
10. Approve the final release evidence bundle before any deployment decision changes.

## Production Readiness Decision

Production readiness remains BLOCKED.

Deployment decision remains DO NOT DEPLOY.

This package creates the decision register and waiver policy. It does not approve a waiver, name a release owner, mark skipped/missing/planned/config-only evidence as PASS, or unlock production deployment.

## Remaining Blockers

1. Release owner is not named and has not approved the evidence bundle.
2. Real server smoke remains `SKIPPED_MISSING_CONTROLLED_SERVER`.
3. HTTPS/proxy auth smoke remains `MISSING_EVIDENCE`.
4. Real secret-store injection remains `MISSING_EVIDENCE`.
5. Credential rotation drill remains `MISSING_EVIDENCE`.
6. OpenAI and xAI/Grok controlled live connectivity passed on 2026-08-23, but remain unclassified by owner decision and lack complete application-lineage evidence; Gemini returned HTTP 400 and remains blocked.
7. External context/news/macro/ETF providers remain `SKIPPED_MISSING_SECRET` or missing harness evidence and unclassified by owner decision.
8. Release timing, rollback owner, and incident owner remain undecided.

## Next Recommendation

Proceed to a release-owner decision capture package if a release owner can provide explicit choices, or to controlled real-server PASS evidence, real HTTPS/proxy auth smoke, real secret-store injection, or credential-rotation drill evidence. The next package must not be production deployment and must preserve no-trading/no-order/no-external-push guardrails.

## Safety Confirmation

- No production server was accessed.
- No production DB was accessed.
- No real secret store was accessed.
- No secret values were printed or committed.
- No `.env` file was committed.
- No provider call was made.
- No orders were placed.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No order execution or auto-trading behavior was introduced.
- No external Push was sent.
- No fake positions or fake review records were created.

## 2026-08-23 private single-user release policy

This section supersedes the missing Owner classifications above for the
current `PRIVATE_SINGLE_USER_WEB` candidate. It does not authorize deployment.

| Decision | Owner record |
|---|---|
| Release owner | repository Owner `363206207x-cmd` |
| Rollback owner | repository Owner `363206207x-cmd` |
| Incident owner | repository Owner `363206207x-cmd` |
| Go/No-Go | `NO_GO_PENDING_TECHNICAL_GATES_AND_COINGLASS` |
| Kraken | `REQUIRED_PASS` |
| OpenAI/GPT | `REQUIRED_PASS` |
| Gemini | `REQUIRED_PASS` |
| xAI/Grok | `REQUIRED_PASS` |
| CoinGlass | `DEFERRED_MISSING_PRIVATE_KEY` |
| Binance | `DISABLED_FOR_RELEASE_DUE_HTTP_451` |
| External news/macro/ETF/context | `DISABLED_FOR_RELEASE` unless separately configured and independently PASS |
| Recheck | `DEFERRED_UNTIL_LEGAL_SOURCE` when no natural in-app `PUSH_SNAPSHOT` exists |
| Legacy routes | retained, authenticated, and unlinked from formal navigation |
| Partial close writes | outside release scope; full close retained |

Gemini remains blocked by provider account/location/region policy. No
authorized remote P3H staging identity is configured. Current readiness is
`PRODUCTION_READINESS=BLOCKED_MULTIPLE`; `DEPLOYMENT_ALLOWED=NO`.
