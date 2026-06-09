# Runtime Design: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Design the minimal review-only runtime wiring for `{module}` using existing owner paths. This is design only, not implementation.

## Allowed Changes
Documentation updates only, scoped to design and source-of-truth handoff.

## Forbidden Changes
No Java business code, tests, dashboard business logic, schema, config, pom, Push, Candidate, Decision generation, Point, trading, order/execution, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`, freeze rules, and the source-read document for `{module}`.

## Required Checks
Run status/bootstrap checks, targeted owner-path greps, `bash scripts/check-workflow-contract.sh`, and diff/forbidden-path checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, designed status mapping, and next allowed action.

## PR Risk Hint
A-risk when docs-only and no business files changed.

Next allowed action: {next_allowed_action}
