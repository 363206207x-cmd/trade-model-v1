# P3-U1 Personal Login Page and Session Authentication

## Status

- Main baseline at branch creation: `d84f0b95023d0ef50443b96d61972c1dbfbdeec8`
- Branch: `codex/p3-u1-personal-login-session-auth`
- Delivery state: `PR_OPEN_UNMERGED` in Draft PR #1133; the capability is not effective on merged main until the PR is reviewed and merged.
- Production readiness: `BLOCKED`
- P3-U2: not allowed before reviewed merged-main activation of P3-U1.

## Authentication Contract

P3-U1 replaces the formal application access path with Spring Security form
login and a server-side Session. It does not add JWT, refresh tokens, OAuth,
registration, password recovery, a role-management UI, or a custom token
system.

- Login page and processing path: `GET /login`, `POST /login`
- Successful login target: the saved request when present, otherwise `/dashboard`
- Failed login target: `/login?error=true` with one generic message
- Logout: CSRF-protected `POST /logout`, Session invalidation, JSESSIONID deletion, then `/login?logout=true`
- Browser unauthenticated behavior: redirect to `/login`
- API unauthenticated behavior: sanitized JSON `401` without Basic challenge details
- Public paths: login/static assets/error plus health/liveness/readiness only
- Protected paths: Dashboard, Review, and personal/operational APIs
- Session creation: `IF_REQUIRED`
- Session fixation: migrate Session ID after authentication
- Session timeout: `TRADE_MODEL_SESSION_TIMEOUT`, default `30m`
- Cookie: HttpOnly, SameSite=Lax; Secure defaults off for local HTTP and on in the prod profile
- CSRF: enabled for the authenticated chain; login, logout, and existing browser writes carry the framework token

The existing `trade-model.auth.enabled=false` compatibility mode remains for
explicit broad test contexts. `ProductionProfileSafetyGuard` rejects that mode
in prod.

## Personal User Storage

`tm_user` contains only:

- `id`
- unique non-null `username`
- non-null `password_hash`
- non-null `created_at`
- nullable `last_login_at`

H2 local/test initialization uses `schema.sql`. PostgreSQL uses
`V8__personal_user_session_authentication.sql`. Passwords use BCrypt. The
password hash is never sent to a Controller, HTML, API response, authentication
audit line, or default mapper log.

Current migration smoke contracts expect the current V1-to-V8 chain and 28
`tm_*` tables. Historical V7 evidence tests are target-pinned to V7 so prior
evidence is not silently reclassified. Controlled PostgreSQL V8 execution was
not run because Docker/Testcontainers was unavailable; this is not a
PostgreSQL PASS claim.

## Initial Personal User

Set both values only in the runtime environment or approved secret injection:

```text
TRADE_MODEL_INITIAL_USERNAME=<personal username>
TRADE_MODEL_INITIAL_PASSWORD=<long local secret>
```

At startup the bootstrap:

1. runs only when authentication is enabled and both values are present;
2. normalizes and validates the username and requires a password of at least 12 characters;
3. creates a BCrypt-backed user only when the table is empty and the username is absent;
4. never overwrites an existing user or password;
5. fails closed for incomplete or unsafe input;
6. logs only the normalized username and a status enum.

The Compose and P3-H ConfigTree templates map the existing versioned
application credential into `trade-model.auth.initial-password`; no credential
value is committed. Real server deployment and real secret-store injection are
not part of this package.

## Failure Limit And Audit

- Threshold: 5 consecutive failures
- Failure window: 15 minutes
- Temporary lock: 15 minutes
- Reset: successful authentication or lock expiry
- Storage: single-instance in-memory, access-ordered, capped at 1024 usernames
- Invalid/oversized username key: bounded and never queried from the user store
- User-facing error: identical for unknown user, wrong password, and temporary block

Sanitized structured logs cover success, invalid credentials, temporary block,
authentication-store failure, last-login write failure, and logout. They never
contain password, BCrypt hash, Session ID, Cookie, CSRF token, request body, or
authorization data. Authentication fails closed if the user query or
`last_login_at` write fails.

## Validation Evidence

Automated validation on this branch:

- P3-U1 and existing security targeted tests: PASS
- Full Maven suite: 4040 tests, 0 failures, 0 errors, 14 environment-gated skips
- Docker/Testcontainers: `ENVIRONMENT_GATED_SKIP`
- Responsive static contract: PASS at 320/375/390-width CSS constraints, 16px inputs, 48px controls, no horizontal-overflow layout
- Real mobile Safari: NOT_RUN
- Real mobile Chrome: NOT_RUN

Bounded localhost validation used port `18081`, a disposable file-backed H2
database, explicit test-only credentials, and all Provider/AI/scheduler flags
disabled. It proved:

- unauthenticated Dashboard -> `/login`;
- login page `200` with CSRF token;
- wrong password -> generic failure;
- correct password -> `/dashboard`;
- Dashboard refresh retained the authenticated Session;
- response Cookie included HttpOnly and SameSite=Lax;
- CSRF-protected POST logout invalidated access;
- first boot created one user;
- second boot reported `SKIPPED_ALREADY_EXISTS` and created zero users;
- the same password still authenticated after restart;
- the pre-restart Session no longer authenticated after restart;
- runtime logs contained zero plaintext-password and zero BCrypt-hash matches.

No real Provider, AI, Telegram, Push, order, position mutation, or trading call
ran during validation.

## Remaining Gates

P3-U1 remains unmerged until its Draft PR is independently reviewed. The
current P3-H exact-V7 deployment harness and Basic-auth smoke scripts are
historical/deferred integration surfaces; they must receive an explicit
Session/V8 deployment-contract package before any real staging attempt.
Controlled PostgreSQL V8, real mobile browser, real reverse-proxy Session/CSRF,
real secret-store injection, credential rotation, and release-owner approval
remain missing. Production deployment cannot proceed.
