# Secrets Manager Credential Rotation Evidence Run

Branch: `codex/pdr-live14-secrets-manager-credential-rotation-evidence`
Base main commit: `1e8f4df0`
Package: PDR-LIVE14 Secrets Manager Credential Rotation Evidence
Status: CONTROLLED EVIDENCE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records controlled secrets manager / credential rotation production-readiness evidence. It is not production deployment, does not access a production server, does not access a production database, does not inspect or print real secret values, does not ask the user to paste secrets into chat, and does not add trading runtime behavior.

## Sources Inspected

- `src/main/resources/application-prod.yml`
- `.env.example`
- `src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java`
- `src/test/java/org/example/trademodel/config/ProductionProfileSafetyGuardTest.java`
- `docs/SECRETS_AND_ACCESS_HARDENING.md`
- `docs/SECRETS_HTTPS_ACCESS_EVIDENCE_RUN.md`
- `docs/ACCESS_AUDIT_RATE_LIMIT_EVIDENCE_RUN.md`
- `docs/HTTPS_REVERSE_PROXY_EVIDENCE_RUN.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`
- `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md`

No `.env` file was read. No production host, production database, secret manager, TLS certificate, private key, API key, cookie, authorization header, datasource URL with credentials, or provider secret value was inspected or printed.

## Evidence Status Summary

| Area | Status | Evidence / Finding |
|---|---|---|
| Repo secret hygiene | GUARD_PASS | Tracked env inventory shows `.env.example` only; the template is placeholder-only and states not to commit `.env` or real secrets. This package did not read untracked `.env` files. |
| application-prod env / secret requirements | GUARD_PASS | `application-prod.yml` pulls datasource, admin, Binance, scheduler, and rate-limit values from environment placeholders rather than literals. |
| ProductionProfileSafetyGuard secret validation | GUARD_PASS | Guard rejects missing datasource URL/username/password, blank/unsafe admin credentials, missing Binance key/secret for prod Binance provider, and missing explicitly enabled AI provider key/model/base URL. |
| Secrets manager integration | DOCUMENTED_WITH_PLAN | `docs/SECRETS_AND_ACCESS_HARDENING.md` defines acceptable secret-store approaches and minimum requirements. No real secret-store injection evidence exists. |
| Credential rotation evidence | DOCUMENTED_WITH_PLAN | Rotation checklist is now explicit for admin, datasource, Binance/API provider, and AI provider credentials. No actual rotation drill was run. |
| Secret value redaction | GUARD_PASS / DOCUMENTED_WITH_PLAN | PDR-LIVE12 proves app access/auth/rate-limit logs avoid sensitive values; release evidence still needs real server log redaction proof. |

## Repo Secret Hygiene Status

Status: GUARD_PASS for tracked-file hygiene; full production secret handling remains blocked until external secret-store evidence exists.

Evidence:

1. `git ls-files` reports `.env.example` as the tracked env template and does not report a real `.env` file.
2. `.env.example` uses placeholders and explicitly says not to commit `.env` or real secrets.
3. This package did not read untracked `.env` files and did not inspect real environment variables.
4. No secret value was printed or committed in this package.

## application-prod Env / Secret Requirement Status

Status: GUARD_PASS.

`application-prod.yml` requires production values through environment placeholders for:

1. `PROD_DATASOURCE_URL`
2. `PROD_DATASOURCE_USERNAME`
3. `PROD_DATASOURCE_PASSWORD`
4. `APP_ADMIN_USERNAME`
5. `APP_ADMIN_PASSWORD`
6. `BINANCE_API_KEY`
7. `BINANCE_API_SECRET`
8. Production scheduler policy and scheduler classifications
9. Rate-limit configuration

AI provider keys remain required only when the matching AI provider is explicitly enabled.

## ProductionProfileSafetyGuard Secret Validation Status

Status: GUARD_PASS.

Guard coverage includes:

1. Missing production datasource URL / username / password fails closed.
2. H2 memory datasource fails closed in prod.
3. Missing Binance key / secret fails closed when prod provider is `BINANCE`.
4. Missing admin username / password fails closed.
5. Unsafe admin password defaults such as `password`, `admin`, `change-me`, `changeme`, `123456`, and `dev-local-password` fail closed.
6. Explicitly enabled OpenAI, Gemini, or xAI provider without key/model/base URL fails closed.
7. Unsafe public bind, actuator exposure, scheduler policy, and rate-limit config fail closed.

## Secrets Manager Evidence Status

Status: DOCUMENTED_WITH_PLAN.

Acceptable future secret-store options remain:

1. Cloud secrets manager.
2. Vault or equivalent self-hosted secret manager.
3. SOPS/age or platform-native encrypted secret store with restricted deploy access.
4. A deployment platform secret store that injects env vars without writing `.env` to the repository.

Required future PASS evidence:

1. Secret names and versions recorded without values.
2. Deployment reads secrets from the selected store without committing `.env`.
3. Redacted startup or smoke evidence proves required env keys are present without printing values.
4. Access policy records who can read, rotate, revoke, and audit each secret class.
5. Evidence bundle confirms no datasource URL with embedded password, API key, token, cookie, or authorization header is printed.

No real secret-store integration was accessed or validated in this package.

## Credential Rotation Evidence Status

Status: DOCUMENTED_WITH_PLAN.

No actual credential rotation was performed. No secret was revoked, regenerated, or validated against a production-like server. The following rotation checklist defines the required future evidence.

## Rotation Checklist: Admin Credential

1. Generate a new operator password in the approved secret store.
2. Update `APP_ADMIN_PASSWORD` secret version without printing the value.
3. Restart or reload the controlled app instance using the new secret.
4. Run authenticated smoke with redacted output.
5. Confirm the old admin credential no longer authenticates.
6. Record operator, timestamp, secret version, validation command, and rollback owner without recording the password.

## Rotation Checklist: Datasource Credential

1. Create or rotate the PostgreSQL app user credential in a controlled DB environment.
2. Store the new `PROD_DATASOURCE_USERNAME` / `PROD_DATASOURCE_PASSWORD` version in the approved secret store.
3. Validate application startup and readiness using the new datasource secret.
4. Revoke the old datasource credential after the new connection is proven.
5. Run a readonly dashboard/review smoke.
6. Record DB role name, secret version, validation timestamp, and rollback plan without recording passwords or full connection URLs.

## Rotation Checklist: Binance / API Provider Credentials

1. Create a readonly Binance/API key with no withdrawal, order, or trading permission.
2. Store `BINANCE_API_KEY` and `BINANCE_API_SECRET` in the approved secret store.
3. Run only readonly provider smoke with bounded timeout and redacted output.
4. Revoke or disable the previous key after smoke passes.
5. Confirm no order, execution, auto-open, auto-close, auto-reverse, auto-trading, Telegram send, or external Push capability is enabled.
6. Record key identifier, permission class, secret version, provider smoke command, and timestamp without recording key values.

## Rotation Checklist: AI Provider Credentials

1. Create provider keys for OpenAI, Gemini, and/or xAI only if the target release requires those providers.
2. Store keys in the approved secret store and keep providers disabled until explicitly enabled for smoke.
3. Run bounded provider smoke only with redacted output.
4. Record provider, model, key version identifier, smoke result, timeout, and cost/budget guard without recording key values.
5. Revoke replaced keys after validation or record a release-owner waiver if the provider is not required for the target release.

## Safety Confirmation

- No production server was accessed.
- No production DB was accessed.
- No real secrets were printed.
- No real secrets were committed.
- No `.env` was committed.
- No secret manager was accessed.
- No credential was rotated or revoked.
- No destructive operation was run.
- No orders were placed.
- No order execution path was introduced.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No auto-trading behavior was introduced.
- No external Push was sent.
- No fake positions were created.
- No fake review records were created.

## Production Readiness Decision

Production readiness remains BLOCKED.

Secrets manager / credential rotation evidence moves from `MISSING_EVIDENCE` to `DOCUMENTED_WITH_PLAN` because this package records the required secret-store options, injection evidence requirements, and rotation checklists. It is not `PASS` or `GUARD_PASS` for real secret-store operation because no approved secret manager was accessed, no production-like server secret injection was proven, and no rotation drill was run.

## Remaining Blockers

1. Real secrets manager injection evidence remains missing.
2. Admin credential rotation drill evidence remains missing.
3. Datasource credential rotation drill evidence remains missing.
4. Binance/API provider credential rotation and readonly permission evidence remains missing.
5. AI provider credential smoke remains missing or requires release-owner waiver.
6. External context/news/macro provider evidence remains missing or requires release-owner waiver.
7. Real HTTPS reverse-proxy smoke remains missing.
8. Real server smoke and release-owner approval remain missing.

## Next Recommendation

Recommended next package: `PDR-LIVE15 AI Provider / External Context Release Policy Evidence`, a controlled real-server smoke package, or a release-owner waiver package for missing optional providers. Production deployment must remain blocked until a later release-gate package records complete PASS evidence and explicit approval.
