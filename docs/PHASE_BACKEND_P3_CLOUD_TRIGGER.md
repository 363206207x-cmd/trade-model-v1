# BACKEND-P3 Cloud Trigger

This file exists only to create a Draft PR entry for the BACKEND-P3 Codex Cloud / GitHub Agent workflow.

Authoritative task source:
- GitHub Issue #83: BACKEND-P3 Dashboard SourceTrace Production Source Readiness Pack

Execution boundary:
- BACKEND-P3 only.
- Production source readiness / minimal safe wiring only.
- No P19.
- No Coinglass or external API integration.
- No live execution integration.
- No order API.
- No automated trading.
- No dashboard.html changes.
- No schema changes unless stopped and reported first.

Expected Codex output:
- Replace or extend this trigger branch with the real BACKEND-P3 rollbackable package.
- Wire only production-backed fields if safe; keep all others missing and fail-closed.
- Update the PR with exact files changed and tests run.
