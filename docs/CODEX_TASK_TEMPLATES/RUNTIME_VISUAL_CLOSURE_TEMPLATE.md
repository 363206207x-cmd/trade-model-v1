# Runtime Visual Closure: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Browser-verify the dashboard surface for `{module}` and record closure. This is visual verification only, not new functionality.

## Allowed Changes
Documentation and source-of-truth updates only.

## Forbidden Changes
No Java business code, tests, dashboard business logic, schema, config, pom, Push, Candidate, Decision generation, Point, trading, order/execution, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, active status, source of truth, freeze rules, runtime verification record, implementation record, and dashboard template.

## Required Checks
Run workflow contract, compile/test checks requested by the package, browser visual verification, forbidden semantics checks, and diff checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, visual verification result, and next allowed action.

## PR Risk Hint
A-risk when docs-only visual closure and no business files changed.

Next allowed action: {next_allowed_action}
