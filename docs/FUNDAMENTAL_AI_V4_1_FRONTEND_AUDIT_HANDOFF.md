# Fundamental AI v4.1 Frontend Audit Handoff

## Audit Target

- Package: `FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT`
- Issue: `#1178`
- Branch: `codex/v4-1-frontend-runtime-alignment`
- Base: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Audit mode: read-only, exact candidate Head
- Candidate state: `IMPLEMENTED_PENDING_MERGE`

The independent auditor must use the PR Head as the immutable audit target and must not fix findings inside the audit.

## Required Reading

1. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_CONTRACT_MAPPING.md`
2. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_RUNTIME_ALIGNMENT_REPORT.md`
3. `docs/FUNDAMENTAL_AI_V4_1_SCENARIO_VALIDATION_REPORT.md`
4. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_TEST_REPORT.md`
5. `docs/FUNDAMENTAL_AI_V4_1_FRONTEND_REMAINING_GAPS.md`
6. `docs/evidence/v4_1_frontend_runtime_alignment/README.md`

## Independent Audit Checklist

### Scope

- Confirm no schema or migration file changed.
- Confirm no Mobile or Figma file changed.
- Confirm no core v4.1 decision algorithm changed.
- Confirm only the existing Home, Home read projection, and Analysis Detail are used.
- Search for automatic open/close/add/reduce/reverse/order capability; expected count is zero.

### Asset Pool And Top6

- Verify all management actions call existing Asset Pool endpoints.
- Verify preview remains non-persistent.
- Verify Home renders backend order and never ranks/fills in JavaScript.
- Verify invalidated/cooling/confused/blocked and untrusted data are not presented as positive opportunities.

### State Semantics

- Verify exact Market Bias 8, Opportunity State 8, and Plan Mode 5 sets.
- Verify the dimensions are independent and unknown values fail closed.
- Verify no legacy direction/status fallback crosses dimensions.

### Final Plan

- Verify the plan grid opens only for a validated Final with complete source/chain gates.
- Verify Candidate cannot be exposed as Final.
- Verify every frozen Final field is directly sourced or null.
- Verify `notTradeInstruction=true` remains mandatory.

### Three AI And Consistency

- Verify one workspace, three tabs, one visible role.
- Verify role metadata and every formal array/collection state.
- Verify empty states distinguish none found, insufficient, unavailable, stale, and no verifiable failure path.
- Verify no cross-role fallback, generated evidence, vote, percentage, or fourth role.

### Position And Detail

- Verify `SYSTEM_PLAN_POSITION` requires a plan link and `MANUAL_INDEPENDENT` does not fabricate one.
- Verify P2 trust/freshness gates and risk level/trend separation remain intact.
- Verify Analysis Detail shows eight scores, evidence freshness, three roles, resolver, rule validation, and Final source chain without a duplicate detail page.

### Validation

Re-run:

```text
./mvnw test -q
bash scripts/product-source-gate.sh
bash scripts/check-workflow-contract.sh
git diff --check
```

Review the current-code screenshots at `docs/evidence/v4_1_frontend_runtime_alignment/`.

## Decision

Allowed result:

- `APPROVE`
- `REQUEST_CHANGES`

Approval must not claim target-runtime provider acceptance. That evidence remains pending and is explicitly outside the deterministic browser package.

## Next Boundary

No merge, Mobile work, Figma change, next product package, or automatic-trading expansion is allowed as part of this handoff.
