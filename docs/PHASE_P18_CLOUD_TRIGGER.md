# P18 Cloud Trigger

This file exists only to create a Draft PR entry for the P18 Codex Cloud / GitHub Agent workflow.

Authoritative task source:
- GitHub Issue #67: P18 DerivativesRiskContext / SourceTrace Fixture Extension Pack

Execution boundary:
- P18 only.
- No P19.
- No Coinglass API integration.
- No external HTTP client or API keys.
- No order API.
- No automated trading.
- VALID must remain manual-review / not-trade-instruction.
- Missing, stale, abnormal, or conflicting derivatives-risk context must fail closed or stay review-only.

Expected Codex output:
- Replace or extend this trigger branch with the real P18 rollbackable task package.
- Include P18 docs, focused local fixture tests, fixture catalog, and verification notes as needed.
- Open/update the PR with exact files changed and tests run.
