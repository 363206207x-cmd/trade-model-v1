# BACKEND-P7 Cloud Trigger

This file exists only to create a Draft PR entry for the BACKEND-P7 Codex Cloud / GitHub Agent workflow.

Authoritative task source:
- GitHub Issue #91: BACKEND-P7 Dashboard SourceTrace Analysis Anchor Metadata Pack

Execution boundary:
- BACKEND-P7 only.
- Minimal analysis-anchor metadata only.
- No P19.
- No external data integration.
- No live execution integration.
- No order API.
- No automated trading.
- No dashboard.html changes.
- No schema changes.
- Do not complete SourceTrace or RuntimeKline.
- Do not treat latestPrice as entry source.

Expected Codex output:
- Replace or extend this trigger branch with the real BACKEND-P7 rollbackable package.
- Wire only safe analysis-anchor metadata if available; keep SourceTrace and RuntimeKline incomplete.
- Update the PR with exact files changed and tests run.
