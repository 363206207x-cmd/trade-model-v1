# Runtime Readiness Gate: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Verify whether `{module}` may enter minimal review-only implementation. This is readiness only, not implementation.

## Allowed Changes
Documentation updates only, scoped to readiness and source-of-truth handoff.

## Forbidden Changes
No Java business code, tests, dashboard business logic, schema, config, pom, Push, Candidate, Decision generation, Point, trading, order/execution, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, active status, source of truth, freeze rules, source-read, and design documents for `{module}`.

## Required Checks
Run status/bootstrap checks, targeted readiness greps, `bash scripts/check-workflow-contract.sh`, and diff/forbidden-path checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, GO/NO-GO decision, and next allowed action.

## PR Risk Hint
A-risk when docs-only and no business files changed.

Next allowed action: {next_allowed_action}
