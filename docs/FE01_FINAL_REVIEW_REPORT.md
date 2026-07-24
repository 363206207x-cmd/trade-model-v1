# FE-01 Overview Dashboard Final Exact-Commit Review

## 1. Review Decision

| Item | Result |
|---|---|
| Review date | 2026-07-24 |
| Review mode | Read-only exact-commit review |
| Reviewed commit | `a000e115c39c500b1bcb8e140673aa3896d594d8` |
| Commit subject | `fix(frontend): close FE-01 overview contract gaps` |
| Branch | `codex/p3-u2-iphone-home-mobile-projection-p1` |
| Isolated review worktree | `CLEAN` |
| FE01_FINAL_STATUS | `PASS` |
| FE02_ALLOWED | `NO` |
| Production readiness | `BLOCKED` |

The FE-01 implementation and its tests are complete and internally consistent
at the reviewed commit. No scoped code blocker remains.

`FE02_ALLOWED` remains `NO` because the reviewed commit is not contained in
local `main`. Repository governance requires reviewed merged main（已合并主线）,
synced main（已同步主线）, and a clean worktree（干净工作区）before a later
business package starts.

## 2. Commit Integrity

Result: `PASS`

The reviewed object exists as a Git commit and contains 13 scoped files:

- the FE-01 final review and governing frontend interaction documents;
- Desktop and Mobile Overview templates/scripts/styles;
- the shared frontend contract;
- Java controller/contract/security tests;
- the iOS Dashboard DOM interaction test.

An isolated detached worktree was created directly from
`a000e115c39c500b1bcb8e140673aa3896d594d8`. Its status was clean before and
after validation.

The original development worktree has no post-commit diff in any of the 13
reviewed FE-01 files. It still contains pre-existing local Xcode signing files
and untracked historical/design documents. Those files are not part of this
commit or this review result.

## 3. Dashboard API Contract

### Desktop

Result: `PASS`

The normal `refreshDashboard()` call graph contains:

```text
refreshDashboard
  -> fetchDashboardHome
  -> renderDashboardHomePayload
  -> renderDashboardHomeUnavailable on failure
```

`fetchDashboardHome()` calls only:

```text
GET /api/dashboard/home
```

It does not call `requestDetailForSelectedSymbol()`,
`refreshDashboardDiagnostics()`, `fetchLocalRealPipelineStatus()`, or
`fetchProviderRuntimeStatus()`.

Legacy diagnostics helpers remain in the template for non-FE-01 operational
surfaces, but they have no call site from the normal Overview refresh flow.
They therefore do not create an Overview diagnostic API fan-out.

### Mobile

Result: `PASS`

The Mobile Overview script has one network fetch:

```text
GET /api/dashboard/home
```

No additional Mobile Overview diagnostic endpoint was found.

## 4. Asset-State Semantics

Result: `PASS`

The shared frontend contract and Mobile template both define:

```text
triggered = 条件已触发，不代表已开仓
```

No FE-01 mapping treats `triggered` as:

- 已开仓;
- 当前持仓;
- 持仓监控中;
- a generated `UserPosition`.

The Desktop compact label `已触发` remains an asset-condition label and is not
used as position evidence.

## 5. Execution Plan and User Position Separation

Result: `PASS`

The reviewed implementation preserves:

```text
Execution Plan != User Position
```

Desktop:

- `renderHomeExecutionFromPayload()` reads only the system suggestion and
  renders direction, entry, stop, take-profit, leverage, position suggestion,
  validity, and invalidation fields;
- `renderHomePositionsFromPayload()` separately reads actual user-position and
  monitor facts;
- exact plan identity and validity are checked before numeric plan boundaries
  are shown.

Mobile:

- Execution Suggestion and Position Monitor are separate sections;
- system suggestions are labelled for manual review only;
- user entry, leverage, size, stop loss, and take profit remain position facts;
- asset selection updates Decision/Plan/AI context without mutating the
  Position Monitor DOM.

No plan creates a position, and no buy/sell/order/automatic-trading control was
added.

## 6. Validation Binding

All validation below ran from the clean isolated worktree at the exact reviewed
commit.

| Validation | Result |
|---|---|
| Full Maven regression | `4116` tests, `0` failures, `0` errors, `14` environment-gated skips |
| FE-01 contract tests | `155` tests, `0` failures, `0` errors, `0` skips |
| iOS Dashboard DOM tests | `16` tests, `0` failures |
| `git diff --check` | `PASS` |
| Worktree after tests | `CLEAN` |

The 14 full-suite skips are Docker/Testcontainers environment gates. They are
not reported as Docker, PostgreSQL, or Testcontainers PASS.

No live Provider call, live AI call, external notification, order, position
mutation, or production deployment was executed by this review.

## 7. Remaining Blockers

No scoped FE-01 code blocker remains.

Delivery blockers before FE-02:

1. Push the reviewed commit if it is not already on the remote branch.
2. Complete the repository's PR review and merge authorization flow.
3. Merge the reviewed FE-01 package to `main`.
4. Sync local `main` and confirm a clean worktree.
5. Re-run the merged-main state gate before starting FE-02.

## 8. Final Gate

```text
FE01_FINAL_STATUS:
PASS

FE01_EXACT_COMMIT_REVIEW:
PASS

FE01_EFFECTIVE_ON_MERGED_MAIN:
NO

FE02_ALLOWED:
NO

REMAINING_BLOCKERS:
MERGED_MAIN_EFFECTIVITY_AND_CLEAN_SYNC

PRODUCTION_READINESS:
BLOCKED
```

Current FE-01 review is complete at the exact commit. The package is not yet
effective on merged main, so the next phase is not allowed.
