# FE-01 Overview Dashboard Final Exact-HEAD Review

## 1. Final Result

| Item | Result |
|---|---|
| Review date | 2026-07-24 |
| Review mode | Read-only exact-HEAD review |
| Branch | `codex/p3-u2-iphone-home-mobile-projection-p1` |
| Reviewed HEAD | `b939c8b8ae84d3eb93d2d5eb60c3e3c5be268a58` |
| Worktree | `DIRTY` |
| FE01 remediation in current worktree | `PASS_CANDIDATE` |
| FE01 remediation in reviewed HEAD | `NO` |
| FE01_FINAL_STATUS | `FAIL` |
| FE02_ALLOWED | `NO` |
| Production readiness | `BLOCKED` |

The FE-01 remediation behaves correctly in the current worktree, but the
reviewed commit does not contain that remediation. The passing Maven, contract,
and iOS DOM evidence therefore binds to the dirty worktree, not to the reviewed
HEAD. This is not an exact-HEAD PASS.

## 2. Exact-HEAD Inclusion

Result: `FAIL`

The following required FE-01 artifacts exist only in the current worktree:

- `src/main/resources/static/js/frontend-contract.js` is untracked and does
  not exist in `HEAD`;
- `src/test/java/org/example/trademodel/controller/FrontendImplementationFoundationContractTest.java`
  is untracked and does not exist in `HEAD`;
- the restricted desktop refresh implementation and its endpoint-allowlist
  tests are modifications after `HEAD`;
- the real-template iOS DOM endpoint test is a modification after `HEAD`.

Consequently, `b939c8b8ae84d3eb93d2d5eb60c3e3c5be268a58` cannot be treated as the
immutable FE-01 remediation commit.

## 3. Dashboard API Contract

### Desktop

Result: `FAIL_AT_HEAD`

The reviewed HEAD's `refreshDashboard()` still calls:

- `fetchLocalRealPipelineStatus()`;
- `fetchProviderRuntimeStatus()`;
- `requestDetailForSelectedSymbol()`;
- `refreshDashboardDiagnostics()` on Home failure.

This violates the FE-01 rule that the normal Overview refresh depend only on:

```text
GET /api/dashboard/home
```

The current worktree removes that fan-out and fails closed through
`renderDashboardHomeUnavailable()`, but that fix is not committed.

### Mobile

Result: `PASS_AT_HEAD`

The reviewed HEAD's mobile script contains one fetch path:

```text
GET /api/dashboard/home
```

No additional mobile Overview API dependency was found.

## 4. Asset-State Semantics

Result: `FAIL_EXACT_HEAD_NOT_BOUND`

The required semantic is:

```text
triggered = 条件已触发，不代表已开仓，也不代表用户持仓
```

The current worktree encodes this in the shared frontend contract and mobile
projection. The reviewed HEAD does not contain that shared contract or an
equivalent explicit frontend mapping. It therefore cannot prove the required
semantic at the exact commit.

No automatic position creation was found, but absence of automatic creation
does not replace the required explicit UI-state contract.

## 5. Execution Plan and User Position Separation

Result: `FAIL_AT_HEAD`

The reviewed HEAD's desktop `renderHomeExecutionFromPayload()` switches the
Execution Plan region into `positionMode` and renders User Position and
Position Monitor fields inside that same renderer, including:

- position status;
- user entry price;
- floating P/L;
- user stop loss and take profit;
- monitoring status and suggested manual action.

This does not preserve the required presentation boundary:

```text
Execution Plan != User Position
```

The current worktree separates these render paths and applies the exact-plan
fail-closed guard, but those changes are not in the reviewed HEAD.

## 6. Figma Baseline

Result: `PASS`

Read-only inspection of Figma file
`rdMYmsAvZYkXHJX8hdl7UN` confirmed:

| Node | Name | Size | Page |
|---|---|---|---|
| `53:3` | `Overview Dashboard / Desktop Web` | `1440 x 2584` | `01 Overview Dashboard` |
| `53:53` | `Overview Dashboard / iPhone` | `430 x 2266` | `01 Overview Dashboard` |

No Figma node was modified during this review.

## 7. Test-Evidence Binding

### Current Worktree Evidence

| Check | Result |
|---|---|
| Full Maven reports | `4116` tests, `0` failures, `0` errors, `14` environment-gated skips |
| FE-01 targeted Maven/contract tests | `155` tests, `0` failures, `0` errors |
| iOS DOM suite | `16` tests, `0` failures, iPhone 17 Pro Max Simulator |

The targeted contract suite was rerun after the remediation files and passed.
The iOS result bundle confirms all 16 selected DOM tests passed.

### Exact-HEAD Binding

Result: `FAIL`

These tests loaded modified and untracked worktree files. At least two tested
contract artifacts do not exist in the reviewed HEAD. The evidence is valid for
the remediation candidate in the current worktree, but it is not valid
exact-HEAD evidence for `b939c8b8ae84d3eb93d2d5eb60c3e3c5be268a58`.

## 8. Safety Review

| Boundary | Result |
|---|---|
| Automatic open/close/reverse | `NOT_ADDED` |
| Buy/sell/order controls | `NOT_ADDED` |
| AI voting or AI rule bypass | `NOT_ADDED` |
| Execution Plan treated as User Position | `REVIEWED_HEAD_UI_SEPARATION_FAIL` |
| Production readiness | `BLOCKED` |

## 9. Remaining Blockers

1. Isolate the intended FE-00/FE-01 remediation from unrelated local Xcode
   signing/project changes.
2. Commit the complete remediation so one new immutable HEAD contains the
   implementation and its tests.
3. Start from a clean worktree at that exact HEAD.
4. Rerun full Maven, FE-01 contract tests, iOS DOM tests, and repository gates.
5. Perform a new independent exact-HEAD review.
6. Merge the reviewed package to `main` before treating it as effective or
   entering FE-02.

## 10. Gate Decision

```text
FE01_FINAL_STATUS:
FAIL

FE02_ALLOWED:
NO
```

Current phase is NOT DONE. Next phase is NOT allowed.
