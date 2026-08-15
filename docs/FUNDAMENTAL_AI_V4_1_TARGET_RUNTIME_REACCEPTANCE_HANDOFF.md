# Fundamental AI v4.1 Target Runtime Reacceptance Handoff

Status: `READY_FOR_ONE_INDEPENDENT_FINAL_REAUDIT_AFTER_EXACT_HEAD_CI`

The independent reviewer must pin the Draft PR's exact implementation Head.
No implementation claim may be transferred to a different commit.

## Required Audit Sequence

1. Verify exact authorized package, branch scope, clean worktree and no Schema,
   Figma, Desktop or Mobile change.
2. Run Product Source Gate, Workflow Contract and the exact authorization
   validator.
3. Inspect `FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_ROOT_CAUSE_MATRIX.md`, then run
   focused B01-B04, provider call-order/count, CoinGlass, login/session and
   regression tests.
4. Run `./mvnw test -q` and `./mvnw clean package -q` on Java 17.
5. Run `bash scripts/standard-release-postgresql-smoke.sh --skip-package` with
   disposable PostgreSQL 16.
6. Inspect the standard JAR for Flyway core, PostgreSQL support and V1-V13.
7. Verify exact provider/instrument/timeframe/market/contract states, HTTP 451
   classification, stale directory-only revalidation, independent fallback,
   no production direct-client bypass, and one-failure/five-success Pool
   isolation.
8. Verify cached exact-model readiness, fallback-not-ready and secret
   redaction without live CI keys.
9. Verify weak-password readiness DOWN, existing-user no overwrite and
   login/session/logout regression.
10. Run secret, automatic-trading, duplicate-skeleton and `git diff --check`
    scans.
11. Verify missing/zero/negative CoinGlass RPM fail closed, explicit 80/300 are
    used exactly, and the repository production implicit-default count is zero.

## Live Reacceptance Deferred Until After Merge

Live target-runtime acceptance must inject secrets from the operator's secret
store and independently verify exact AI accounts, enabled market providers,
login and the real decision chain. CoinGlass live acceptance remains deferred
until a real key is deliberately configured. No live key belongs in Git, CI,
screenshots, traces or reports.

Data Quality 55 remains valid fail-closed evidence. The reviewer must not
modify scores or thresholds to obtain a Candidate or Final.
