# TRINE LOGIC Multi-User Account Registration Test Report

Status: `LOCAL_VALIDATION_PASS_PENDING_EXACT_HEAD_CI_AND_STAGING`

Date: 2026-08-25

Branch: `codex/multi-user-account-registration-closure`

Implementation baseline:
`0145038bca0713f0ad4dda490541e2533e7486d0`

## Local Results

| Validation | Result |
|---|---|
| Java 17 full Maven suite | `4850` tests, `0` failures, `0` errors, `14` skipped |
| Focused registration/account/isolation suite | PASS |
| Existing Position Monitoring and decision-chain regression suite | PASS |
| JavaScript syntax (`workspace.js`, `auth-forms.js`) | PASS |
| Product Source Gate | PASS |
| PostgreSQL 16 standard release smoke | PASS |
| PostgreSQL V1 to V15 | `15/15_PASS` |
| Focused PostgreSQL migration contract rerun | PASS |
| Existing V15 restart | PASS |
| Checksum and migration failure readiness | PASS_FAIL_CLOSED |
| Packaged JAR form login / Session / CSRF / logout | PASS |
| Local browser registration and account workflow | PASS |
| Exact-head CI | PENDING candidate commit and push |
| Private Staging migration and browser acceptance | NOT RUN |

The Maven Testcontainers tests retain their existing skip behavior because the
repository's Testcontainers 1.19 Docker client is incompatible with the local
Docker 29 API. Real PostgreSQL 16 evidence was obtained independently through
the repository's disposable CLI-based standard-release smoke. This is not H2
or fixture migration evidence.

## Focused Contract Coverage

- Eight-character password accepted; seven-character, mismatched and invalid
  passwords rejected.
- Case-insensitive duplicate and reserved Owner usernames rejected.
- Accounts two through ten accepted; the eleventh enabled account rejected.
- Concurrent requests for the final slot produce exactly one successful
  registration.
- Registration-disabled mode fails closed.
- The preserved user 1 remains the only `xuchao` Owner; registration cannot
  elevate a user.
- Owner password setup is single-use and expiry bounded.
- Owner and USER sessions coexist; logout is session-local; force logout and
  disable invalidate only the target user's sessions; re-enable permits a new
  login subject to the active-account limit.
- Two-user tests isolate watch pools, positions, analyses, plans, messages,
  reviews and detail routes while shared market data remains global.
- New accounts receive real empty user state and do not inherit Owner data.
- BCrypt, Session, CSRF, rate limiting and no-plaintext-password boundaries are
  covered by focused and full-suite tests.

## Browser Evidence

An ephemeral local release-JAR run with external providers, schedulers, AI and
Telegram calls disabled verified:

1. `/register` renders the TRINE LOGIC registration form.
2. A USER registration redirects to `/login?registered=true`.
3. The USER can log in and sees isolated empty dashboard, position, analysis,
   message and security views with no Owner controls.
4. Self-password change invalidates the current session and redirects to the
   login page.
5. The Owner can log in and view both the canonical Owner and USER rows.
6. Owner disable and re-enable controls update the USER state.

The run used an ephemeral H2 database and temporary credentials that were
deleted after shutdown. No credential, cookie or Session value is recorded in
this report. Concurrent independent browser sessions and preservation of the
real Owner dataset remain mandatory private-Staging acceptance checks.

## PostgreSQL Evidence

The standard release JAR was exercised against a disposable PostgreSQL 16
database. Flyway applied V1-V15, restarted cleanly at V15, rejected checksum
and migration failures through readiness, and passed form-login Session/CSRF
and logout checks. The disposable database was removed after validation.

The first exact-head `quality-gate` run against implementation commit
`08d6a3a91386448ce714fabaa0950635cb7781ca` exposed a migration-test fixture
that inserted a V11 `tm_user_position` without the V15-required `user_id`.
The fixture now uses the preserved Owner (`user_id=1`), so the assertions once
again exercise the intended V11 plan/source constraints instead of failing at
the V15 ownership constraint. The focused PostgreSQL 16 migration test and the
Java 17 full Maven suite pass after this test-only correction; application and
migration code are unchanged by the correction.

## Remaining Acceptance Gates

- Create and push the candidate commit without rewriting history.
- Verify local, remote branch and Draft PR exact-head identity.
- Pass `quality-gate` push, `quality-gate` pull_request and
  `workflow-contract` for that exact SHA.
- Back up private Staging, record a rollback point, deploy the same audited
  SHA, run V15, and verify the preserved Owner and existing data.
- Complete the two-browser Owner/USER workflow with Owner-supplied credentials,
  Tailnet-only access and public exposure equal to zero.
