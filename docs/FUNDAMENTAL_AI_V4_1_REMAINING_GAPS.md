# Fundamental AI v4.1 Remaining Gaps

Status: `REMEDIATION_COMPLETE_PENDING_INDEPENDENT_REAUDIT`

## Merge-Readiness Evidence Still Required

1. The independent Backend Capability Audit must re-audit the four remediated
   findings and issue a new `APPROVE` or `REQUEST_CHANGES` decision.
2. Merged-main validation has not run. The candidate is not effective until
   reviewed, merged, and validated on clean/synced main.
3. PR CI must run against the remediated head; local validation cannot replace
   the required PR Merge Gate checks.

PostgreSQL V11 is not an open local validation gap. The complete controlled
migration test passed against disposable PostgreSQL `16.14`, including the
historical V8 -> V11 path and the new timeframe, Conflict Level, and
UserPosition source constraints.

## Existing Non-Blocking Audit Debt

The remediation intentionally did not expand into the independent audit's
non-blocking findings:

- existing legacy Review rows are not backfilled with new provenance;
- market-catalog degradation remains silent;
- the machine-readable authorization projection still needs reconciliation at
  Merge Gate even though the authorization merge is present in this branch's
  base and the Product Source/Workflow gates pass.

## Runtime Acceptance Not Claimed

- No live AI provider call was made; provider credentials were not used.
- Full-market search uses the real Binance exchange-info catalog at runtime;
  provider unavailability falls back to the existing configured catalog and
  does not fabricate a full-market success claim.
- No production database, production server, or production secret was used.

## Intentional Boundaries

- Figma: unchanged.
- Mobile: unchanged.
- PositionMonitor contract: preserved, not rewritten.
- Trading execution: absent.
- Automatic open/close/reverse/order: absent.

No known local compile, unit, integration, H2 persistence, PostgreSQL V11,
product-source gate, or workflow-contract failure remains.
