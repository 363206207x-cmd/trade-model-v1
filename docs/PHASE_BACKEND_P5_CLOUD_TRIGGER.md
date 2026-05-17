# BACKEND-P5 Cloud Trigger

This file exists only to create a Draft PR entry for the BACKEND-P5 Codex Cloud / GitHub Agent workflow.

Authoritative task source:
- GitHub Issue #87: BACKEND-P5 Dashboard RuntimeKline Minimal Safe Wiring Pack

Execution boundary:
- BACKEND-P5 only.
- Minimal safe wiring only.
- No P19.
- No Coinglass or external API integration.
- No live execution integration.
- No order API.
- No automated trading.
- No dashboard.html changes.
- No schema changes.
- Do not complete SourceTrace or RuntimeKline.
- Do not treat latestPrice as entry source.

Expected Codex output:
- Replace or extend this trigger branch with the real BACKEND-P5 rollbackable package.
- Wire only safe metadata fields; keep SourceTrace and RuntimeKline incomplete.
- Update the PR with exact files changed and tests run.
