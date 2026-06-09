# Source Read: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Read the existing source path for `{module}` and decide whether it can become a minimal review-only runtime slice. This is source-read only, not implementation.

## Allowed Changes
Documentation updates only, scoped to source-read records and source-of-truth handoff.

## Forbidden Changes
No Java business code, tests, dashboard business logic, schema, config, pom, Push, Candidate, Decision generation, Point, trading, order/execution, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, `docs/SESSION_BOOTSTRAP.md`, `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`, and module-relevant source-read predecessors.

## Required Checks
Run status/bootstrap checks, targeted grep/source inventory commands, `bash scripts/check-workflow-contract.sh`, and diff/forbidden-path checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, source-read conclusion, and next allowed action.

## PR Risk Hint
A-risk when docs-only and no business files changed.

Next allowed action: {next_allowed_action}
