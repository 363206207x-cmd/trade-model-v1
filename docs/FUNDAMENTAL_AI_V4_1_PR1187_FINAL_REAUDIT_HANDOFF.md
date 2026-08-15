# Fundamental AI v4.1 PR #1187 Final Re-audit Handoff

Status: `PENDING_EXACT_HEAD_CI_THEN_ONE_INDEPENDENT_REAUDIT`

The reviewer must pin the remote PR #1187 Head produced by this closure. The
review is invalid if the Head changes after CI or during audit.

## Required Review

1. Confirm PR remains Draft, branch is
   `codex/v4-1-target-runtime-blocker-remediation`, local/remote are 0/0, and
   the worktree is clean.
2. Compare the exact Head with baseline
   `303165e0e935bab5a474767f425b2420be8445a6`.
3. Verify every row in the root-cause matrix and all provider production entry
   points; inspect actual call-order/count assertions.
4. Reproduce unsupported symbol/timeframe, region restricted, provider
   disabled, source unavailable, not configured, stale revalidation failure,
   supported primary, independently supported fallback, and all-unsupported
   no-fabrication cases.
5. Verify exact quote/market/contract/timeframe identity and that generic quote
   enablement cannot authorize disabled OHLCV.
6. Verify one unsupported asset does not erase five successful analyses and
   aggregate state remains truthful `PARTIAL`.
7. Verify CoinGlass disabled/key-missing/RPM-missing/invalid/positive states,
   explicit 80/300 budgets, exact `CG-API-KEY` arguments, secret redaction, and
   production implicit-default count zero.
8. Run Product Source Gate, Workflow Contract, authorization validation,
   focused B01-B04 tests, full Maven, Java 17 clean package, standard packaged
   JAR PostgreSQL 16 smoke, secret/duplicate/automatic-trading scans, and
   `git diff --check`.
9. Confirm no Schema, API product contract, Figma, Desktop, Mobile, threshold,
   Three-AI authority, Position Monitoring contract, or automatic-trading
   change.

## Expected Recommendation Boundary

Only the exact Head may receive `APPROVE` or `REQUEST_CHANGES`. This handoff
does not authorize merge, deployment, live secrets, or a new implementation
package. Live provider and exact-model acceptance remains a post-merge Product
Owner-controlled step.
