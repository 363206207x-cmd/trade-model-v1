# PHASE: GitHub / Codex / ChatGPT PR Workflow Smoke Test

## Purpose
This draft PR is **only** a smoke test for the GitHub / Codex / ChatGPT pull request workflow.

## What this verifies
- Codex cloud environment can read this repository.
- Codex can create a branch and prepare a pull request.
- ChatGPT + Codex PR collaboration path is working end-to-end.

## What this does **not** mean
- This PR does **not** represent V1 feature progress.
- No V1 business capability is advanced by this change.

## Repository workflow policy (re-stated)
- All future V1 changes must go through: **feature branch + PR + review**.
- Direct commits to `main` are **not allowed**.

## Mandatory human review areas
Any PR involving the following areas requires explicit manual review:
- `RuleEngine`
- `PlanBoundary`
- `ExecutionPlan`
- `Push`
- Risk Action Guard
- Automated trading boundaries

## Current V1 safety boundaries (unchanged)
- Automated trading is still **prohibited** in V1.
- Order API integration is still **prohibited** in V1.
- Unattended / unreviewed merges are still **prohibited**.
