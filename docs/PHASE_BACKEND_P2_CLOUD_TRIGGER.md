# BACKEND-P2 Cloud Trigger

This file exists only to create a Draft PR entry for the BACKEND-P2 Codex Cloud / GitHub Agent workflow.

Authoritative task source:
- GitHub Issue #81: BACKEND-P2 Dashboard SourceTrace and DerivativesRiskContext Detail Wiring Pack

Execution boundary:
- BACKEND-P2 only.
- Read-only / fail-closed wiring only.
- No P19.
- No Coinglass or external API integration.
- No live execution integration.
- No order API.
- No automated trading.
- No dashboard.html changes.
- No schema changes unless stopped and reported first.

Expected Codex output:
- Replace or extend this trigger branch with the real BACKEND-P2 rollbackable task package.
- Keep missing SourceTrace / DerivativesRiskContext explicit and fail-closed.
- Update the PR with exact files changed and tests run.
