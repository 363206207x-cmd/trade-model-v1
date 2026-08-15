# Fundamental AI v4.1 Deployment Runbook

Status: `READY_AFTER_MERGED_MAIN_VALIDATION` is the highest state this runbook
can authorize. It does not mean `DEPLOYED` or `PRODUCTION_EFFECTIVE`.

## Ownership

- Release Owner: Product Owner, unless explicitly delegated.
- Rollback Decision Owner: Product Owner, unless explicitly delegated.
- Backup confirmation and smoke confirmation may be performed by the same
  Product Owner for this personal system.

## Supported Modes

### LOCAL_TRUSTED

- Bind to `127.0.0.1` or an explicitly controlled private interface.
- Do not expose the application directly to the public Internet.
- HTTPS is optional only when every client and hop is trusted and local.
- Keep `TRADE_MODEL_PRODUCTION_ALLOW_PUBLIC_BIND=false`.

### PUBLIC_REVERSE_PROXY

- Bind the Java service to `127.0.0.1`; expose only the reverse proxy.
- Terminate HTTPS at the proxy. Plain HTTP must redirect to HTTPS.
- Forward the original scheme, host and client address using the proxy's
  trusted forwarding configuration. Do not trust forwarding headers from
  arbitrary Internet clients.
- Set `TRADE_MODEL_SESSION_COOKIE_SECURE=true` and preserve `HttpOnly` and
  `SameSite=Lax` Session cookie attributes.
- Apply at least HSTS, `X-Content-Type-Options: nosniff`, a restrictive
  `Referrer-Policy`, frame protection, and a reviewed Content Security Policy.
- Never expose the H2 console or PostgreSQL port publicly.

## Release Procedure

1. Confirm the approved commit is merged to `main`, local `main` equals
   `origin/main`, and the worktree is clean.
2. Record the release artifact checksum and build it with Java 17 using
   `./mvnw clean package`. The standard JAR includes Flyway; no special Maven
   profile is permitted. Do not deploy a local IDE output directory.
3. Inject environment variables from the host secret store. Do not place
   secrets in Git, frontend assets, command transcripts, logs, screenshots or
   evidence reports.
4. Run `bash scripts/target-runtime-preflight.sh`. Resolve every blocked state
   before startup. Optional exact-model probes require an authenticated Session
   and do not replace business-chain acceptance.
5. Run `bash scripts/product-source-gate.sh`,
   `bash scripts/check-workflow-contract.sh`, the authorization validator and
   the full Maven suite.
6. Create and verify a PostgreSQL backup before starting the new artifact.
7. Stop the prior artifact, retain it for rollback, and start the approved
   standard JAR with `java -jar` and the `prod` profile.
8. Let Flyway apply V1 through V13 from
   `classpath:db/migration`. If migration fails, readiness must remain
   unavailable and the release stops.
9. Verify liveness, readiness and provider health separately. Application
   liveness does not prove provider availability.
10. Run the authenticated deployment smoke in
   `docs/FUNDAMENTAL_AI_V4_1_DEPLOYMENT_SMOKE_TEST.md`.
11. Confirm the fourteen Desktop routes, login/session/logout, Dynamic Top6,
    one-role AI workspace and fail-closed states.
12. Confirm scheduler policy and each explicitly enabled scheduler approval.
13. Record the artifact, database backup, smoke result, Release Owner and
  Rollback Decision Owner in the release record.

Before any live release, independently run
`bash scripts/standard-release-postgresql-smoke.sh` against its disposable
PostgreSQL 16 container. It validates empty V1-V13 migration, existing V13
restart, form login/CSRF Session/logout invalidation, and checksum fail-closed
behavior using the packaged JAR.

CoinGlass may remain disabled. If enabled for external calls, both
`COINGLASS_API_KEY` and an explicit positive `COINGLASS_ADVERTISED_RPM` are
required; there is no Standard-plan RPM fallback. Provider capability failures
must be resolved through the capability directory or provider configuration,
never by probing a market-data endpoint.

## Runtime Operations

- The application emits logs to standard output. The deployment owner must
  route them to a bounded system journal or a directory managed by `logrotate`.
- Retain enough history to diagnose authentication, provider, scheduler and
  migration failures; never log keys, passwords, Session cookies or full AI
  prompts containing secrets.
- Static resources are packaged in the repeatable application artifact. No
  local absolute path or test fixture is allowed in the production default
  path.
- Provider degradation is shown as provider health and fail-closed product
  state; it must not be masked by application `UP`.

## Fourteen Desktop Routes

`/login`, `/dashboard`, `/asset-pool`, `/positions`,
`/positions/{positionId}`, `/reviews`, `/reviews/{reviewId}`, `/analysis`,
`/analysis/{analysisId}`, `/messages`, `/recheck/{pushSnapshotId}`,
`/plans/{planId}`, `/calendar`, `/audit/{traceId}`, and `/me` are covered by
the route contract. `/login` is the authentication entry and the remaining
fourteen product routes are checked by the canonical route suite.

## Rollback Trigger

Rollback when migration, readiness, authentication, canonical Home, provider
trust, scheduler safety or data integrity cannot be restored inside the
release window. Follow the six-step process in
`docs/FUNDAMENTAL_AI_V4_1_DATABASE_BACKUP_AND_ROLLBACK.md`.
