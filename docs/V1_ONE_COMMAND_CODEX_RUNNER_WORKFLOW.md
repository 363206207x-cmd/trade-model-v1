# V1 One-Command Codex Runner Workflow

## 1. Executive Summary

This pack adds a minimal one-command workflow layer for Trade Model V1.

Before this pack, the project already had drift-prevention rules and fixed local fallback scripts, but the user still had to copy and paste too many steps:

- run `bash scripts/v1-auto.sh next`;
- copy the generated task into Codex;
- wait for Codex to finish;
- manually run PR checks;
- manually mark ready / merge / sync;
- manually ask for the next task.

This pack adds the missing operator shortcuts:

- `bash scripts/v1-codex-run-next.sh`
- `bash scripts/v1-pr-complete.sh <PR> A "<SUBJECT>"`
- `bash scripts/v1-pr-complete.sh <PR> B "<SUBJECT>" --confirm-reviewed`

It does not change Java business behavior, tests, dashboard business logic, schema/config/pom, runtime capability, Push, Candidate, Decision generation, Point, order/execution, or auto-trading.

## 2. New Entry Points

### Run Next Codex Task

```bash
bash scripts/v1-codex-run-next.sh
```

This command:

- verifies the current branch is `main`;
- verifies the worktree is clean;
- verifies there is no open PR according to `scripts/v1-state.sh`;
- verifies `CAN_CONTINUE_NEXT_PACKAGE: YES`;
- runs `bash scripts/v1-auto.sh next`;
- finds the generated task file;
- starts Codex CLI if available;
- otherwise prints the task file path for manual copy.

It never stages, commits, pushes, creates a PR, or merges.

### Complete A-Risk PR

```bash
bash scripts/v1-pr-complete.sh <PR> A "<SUBJECT>"
```

A-risk means low-risk docs/workflow-only scope. The command:

- runs `bash scripts/v1-auto.sh check-pr <PR> A`;
- waits for `quality-gate` and `workflow-contract` to pass;
- marks the PR ready if needed;
- calls `bash scripts/v1-merge-sync.sh <PR> "<SUBJECT>" --risk A --confirm`;
- runs `bash scripts/v1-state.sh`;
- runs `bash scripts/v1-auto.sh next`.

It never bypasses `scripts/v1-merge-sync.sh`.

### Complete B-Risk PR After Review

```bash
bash scripts/v1-pr-complete.sh <PR> B "<SUBJECT>"
```

B-risk means implementation or dashboard/test scope. By default it only checks and summarizes. It does not merge.

After explicit human/assistant review:

```bash
bash scripts/v1-pr-complete.sh <PR> B "<SUBJECT>" --confirm-reviewed
```

Even with `--confirm-reviewed`, the script reprints changed files and the risk boundary before calling `scripts/v1-merge-sync.sh`.

## 3. Risk Rules

| Risk | Behavior |
|---|---|
| A | May auto ready / merge after PR scope, mergeability, and CI checks pass. Scope must remain docs/scripts workflow-only. |
| B | Checks and summarizes by default. Merge requires `--confirm-reviewed` and still goes through `scripts/v1-merge-sync.sh`. |
| C | Never auto merges. Prints STOP and requires manual review. |

B-risk currently allows only the minimal implementation paths that have been repeatedly used by the review-only runtime slices:

- `src/main/java/org/example/trademodel/controller/DashboardController.java`
- `src/main/resources/templates/dashboard.html`
- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`
- `docs/*`
- `scripts/*`

It still blocks schema/config/pom changes, DTO/Validator/Assembler files, Push, Candidate, Decision generation, Point, final direction, entry/stop/TP/RR, order/execution, auto-trading, replay execution, review result generation, P359, and P360.

## 4. Stop Conditions

The one-command runner stops when:

- current branch is not `main`;
- worktree is dirty;
- open PR is not `none`;
- `MAIN_SYNC` is not `OK`;
- `CAN_CONTINUE_NEXT_PACKAGE` is not `YES`;
- `BLOCKERS` is not `none`;
- Codex CLI is unavailable;
- CI does not finish within 15 minutes;
- required checks fail;
- changed files exceed the declared risk scope.

Codex shell `GH_NOT_AVAILABLE` remains GitHub status unknown. It is not project failure by itself, but the runner cannot auto-continue without open PR evidence.

## 5. Non-Scope

This workflow pack does not:

- complete Review / Replay visual closure;
- mark Review / Replay result status as a full closed runtime slice;
- change business endpoints or dashboard business logic;
- change Java business code;
- change tests;
- change schema/config/pom;
- connect Push or external channels;
- generate Candidate, Decision, Point, final direction, entry/stop/TP/RR, order/execution, or auto-trading behavior;
- raise capability level beyond `REVIEW_ONLY_RUNTIME partial`.

The next business action remains `Review / Replay Result Status Visual Verification / Closure`.

## 6. Verification

Required checks for this pack:

- `bash scripts/check-workflow-contract.sh`
- `bash scripts/v1-state.sh`
- `bash scripts/v1-auto.sh next`
- `bash scripts/codex-next-task.sh`
- `bash -n scripts/v1-auto.sh`
- `bash -n scripts/v1-pr-complete.sh`
- `bash -n scripts/v1-codex-run-next.sh`
- `git diff --check`
- `git diff --cached --check`

ShellCheck is optional. If it is unavailable, that does not block the pack.
