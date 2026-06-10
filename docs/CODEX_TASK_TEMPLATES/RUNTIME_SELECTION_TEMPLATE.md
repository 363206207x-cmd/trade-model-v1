# Runtime Slice Selection: {module}

Current main: {current_main}
Branch: {branch}
Phase: {phase}

## Task Goal
Compare remaining safe runtime slice candidates after the latest completed visual closure and select the next minimal, verifiable, user-visible review-only runtime slice. This is selection / source-read-lite only, not implementation.

## Allowed Changes
Documentation and source-of-truth updates only.

## Forbidden Changes
No Java business code, tests, dashboard business logic, schema, config, pom, Push, Candidate, Decision generation, Point, trading, order/execution, DTO, Validator, Assembler, Orchestrator, P359, or P360.

## Required Reads
Read `AGENTS.md`, active status, source of truth, freeze rules, the latest visual closure record, capability matrix, roadmap, and relevant source-read-lite inventory outputs.

## Required Checks
Run workflow contract, `bash scripts/v1-state.sh`, source-read-lite grep inventory for candidate slices, forbidden semantics checks, and diff checks.

## Output Contract
Report branch, commit, changed files, commands, overreach status, selected next runtime slice, and next allowed action.

## PR Risk Hint
A-risk when docs-only selection and no business files changed.

Next allowed action: {next_allowed_action}
