# Runtime Implementation: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Implement the minimal review-only runtime status for `{module}` using existing owner paths only.

## Allowed Changes
Only the files explicitly allowed by the readiness gate: minimal controller/service glue when necessary, minimal dashboard status/copy when necessary, targeted tests, and source-of-truth docs.

## Forbidden Changes
No schema, config, pom, Push, external channel, Candidate generation, Decision generation, Point generation, final direction, entry/stop/TP/RR, order/execution, auto-trading, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, active status, source of truth, freeze rules, source-read, design, and readiness documents for `{module}`.

## Required Checks
Run workflow contract, compile, test-compile, targeted tests, forbidden semantics grep, and diff checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, implemented endpoint/panel/status fields, tests, and next allowed action.

## PR Risk Hint
B-risk if Java, tests, or dashboard behavior changes; requires explicit user merge approval.

Next allowed action: {next_allowed_action}
