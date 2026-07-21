# P3-U1 Personal Login Page and Session Authentication

## Status

- Main baseline at branch creation: `d84f0b95023d0ef50443b96d61972c1dbfbdeec8`
- Branch: `codex/p3-u1-personal-login-session-auth`
- Delivery state: `PR_OPEN_READY_UNMERGED` in PR #1133; the capability is not effective on merged main until the new exact Head is reviewed and merged.
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
evidence is not silently reclassified. The P3-H migration service now applies
the canonical Flyway chain through V8 before the application can start; core
and steady-state verification require eight successful migrations, the exact
V8 schema fingerprint, `tm_user`, and its five columns. No V8 SQL is copied
into Compose or a second schema source.

## Initial Personal User

Set both values only in the runtime environment or approved secret injection:

```text
TRADE_MODEL_INITIAL_USERNAME=<personal username>
TRADE_MODEL_INITIAL_PASSWORD=<long local secret>
```

At startup the bootstrap:

1. runs only when authentication is enabled and both values are present;
2. normalizes and validates the username and rejects blank, short, known-default, and template-style passwords;
3. creates a BCrypt-backed user only when the table is empty and the username is absent;
4. never overwrites an existing user or password;
5. fails closed for incomplete or unsafe input;
6. logs only the normalized username and a status enum.

The Compose and P3-H ConfigTree templates map the existing versioned
application credential into `trade-model.auth.initial-password`; no credential
value is committed. The application database role remains read-only for every
business table. Its only write privileges are the exact `tm_user` bootstrap
columns, `last_login_at`, and `tm_user_id_seq` usage required by this
authentication contract. Real server deployment and real secret-store
injection are not part of this package.

`.env.example` leaves `TRADE_MODEL_INITIAL_PASSWORD` blank. Operators must use
an approved secret store or an untracked local environment file; there is no
fixed, publicly reusable password placeholder. The production safety guard and
bootstrap share the same minimal initial-password policy, including explicit
rejection of `replace-with-*`, change/default/example/sample/placeholder
templates, existing weak sentinels, blanks, and values shorter than 12
characters.

## Failure Limit And Audit

- Threshold: 5 consecutive failures
- Failure window: 15 minutes
- Temporary lock: 15 minutes
- Reset: successful authentication or lock expiry
- Storage: separate single-instance in-memory state for confirmed users and
  unknown usernames; each state area is independently capped at the configured
  1024-entry default
- Confirmed-user state: active failures and locks are never evicted by unknown
  username traffic; expired state is cleaned, and confirmed-user capacity
  exhaustion fails closed instead of deleting active protection
- Unknown-username state: access-ordered and bounded; unknown entries may evict
  only other unknown entries
- Successful authentication: resets only that confirmed user's state
- Invalid/oversized username key: bounded and never queried from the user store
- User-facing error: identical for unknown user, wrong password, and temporary block

Sanitized structured logs cover success, invalid credentials, temporary block,
authentication-store failure, last-login write failure, and logout. They never
contain password, BCrypt hash, Session ID, Cookie, CSRF token, request body, or
authorization data. Authentication fails closed if the user query or
`last_login_at` write fails.

Canonical usernames are at most 64 characters and contain only ASCII letters,
digits, `.`, `_`, `-`, and optional `@`. Spaces, `=`, controls, Unicode line
separators, and invisible characters fail through the same generic login
response. Audit fields apply a second deterministic ASCII encoding layer, so
CR/LF/TAB, U+0085, U+2028, U+2029, spaces, `=`, and other field separators
cannot create a new log line or inject an `outcome`/`reason` field.

## Validation Evidence

Automated validation on this branch:

- P3-U1 security-blocker targeted suite: 96 tests, 0 failures, 0 errors, 0 skips
- Full Maven suite: 4099 tests, 0 failures, 0 errors, 14 environment-gated skips
- Docker/Testcontainers: `ENVIRONMENT_GATED_SKIP`
- P3-H runtime PostgreSQL V8 validation: `PASS_LOCAL_DISPOSABLE_ONLY`
- PostgreSQL V8 status: `PASS_LOCAL_DISPOSABLE_POSTGRESQL16_ONLY`
- Real production PostgreSQL V8 validation: `NOT_RUN`
- Local disposable Session/CSRF smoke: `PASS`
- Real reverse-proxy Session/CSRF: `NOT_RUN`
- Secret Store injection and credential rotation: `MISSING_EVIDENCE`
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

The exact-Head disposable P3-H run used PostgreSQL 16, applied canonical Flyway
V1-V8, verified `tm_user`, and completed the browser-equivalent smoke sequence:
`GET /login`, login CSRF extraction, `POST /login`, Session Cookie reuse,
authenticated Dashboard/Review reads, authenticated logout CSRF extraction,
`POST /logout`, and rejection of the old Session. This is local disposable
evidence only. It is not real production PostgreSQL or real reverse-proxy
Session/CSRF evidence.

## Security Review Blocker Closure

- `HIGH-1`: known-user failure and lock state is isolated from the bounded
  unknown-username LRU, so unknown spray cannot evict active account protection.
- `HIGH-2`: the public environment template contains no accepted fixed initial
  password, and the shared guard/bootstrap policy rejects known template values.
- `MEDIUM-1`: usernames use the explicit ASCII allowlist and audit values receive
  deterministic separator/control encoding before structured logging.

These fixes are implemented and locally validated on the PR branch. PR #1133
is Ready and unmerged; any new P3-H/Smoke fix commit requires exact-Head CI and
independent re-review before merge authorization.

## Remaining Gates

P3-U1 remains unmerged. The current branch closes the code-level P3-H gap with
the existing one-shot migration service, exact V8/`tm_user` verification, and
bounded auth-only database writes. `prod-smoke.sh` and the release gate now use
form login, a temporary Cookie jar, CSRF-protected logout, and post-logout
Session invalidation. The smoke reacquires the post-authentication CSRF token
from the authenticated Dashboard logout form after Session fixation migration;
there is no current Basic Auth compatibility path.
Historical P3-G/PDR Basic-auth evidence remains historical and is not a current
P3-U1 deployment contract. Real mobile browser, real reverse-proxy Session/CSRF,
real secret-store injection, credential rotation, and release-owner approval
remain missing. Production deployment cannot proceed.
