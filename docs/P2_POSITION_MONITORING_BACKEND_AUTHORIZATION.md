# P2 Position Monitoring Backend Implementation Authorization

Status: `AUTHORIZED_PENDING_MERGED_MAIN`

Authorized package: `P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION`

This record is the independent product authorization required after Product
P1B completion. It authorizes a bounded Position Monitoring backend package
only after this record is reviewed, merged to `main`, and accepted by the
runtime gate on a clean, synced worktree. It does not implement the package,
change product maturity, or adopt any existing candidate diff automatically.

## 1. Effective Predecessor

Product P1B Home Alignment Implementation is `COMPLETE`. PR #1163 is effective
on merged main at `d6f6a4dd53ae9b39b095eb65220525fb76928fba`, and the P1B
acceptance closure is effective at
`2830f6c42f46c560f4b05b62b8584a0847eaf644`.

Authorization remains distinct from implementation. Product P2 remains
unimplemented until the separately reviewed backend package is merged and
validated on `main`.

## 2. Authorized Backend Scope

The package may complete only the backend contract needed by the frozen
Position Monitoring product semantics:

1. Position monitor persistence and schema changes for independent entry-logic
   status, monitor conclusion, reversal status, risk-change reason, source
   trust, observation time, and freshness.
2. Entity, enum, mapper, service, and read-model changes required to preserve
   those independent meanings without fallback mixing.
3. Per-UserPosition risk calculation with `LOW`, `MEDIUM`, `HIGH`, and
   `EXTREME`, using exact position facts and market context rather than a
   user-level aggregate risk copied to every position.
4. A monitor-source trust gate where only verified and fresh observations may
   expose mark price, PnL, risk, conclusion, and suggested manual action.
5. Explicit mark-price provenance, observation time, freshness, nullable PnL,
   and fail-closed missing-data behavior.
6. Position-monitor state support for no position, open monitoring, waiting
   for monitor data, escalated risk, invalidated plan, and closed removal from
   Home active positions.
7. Dashboard Home Position projection and API fields strictly required by the
   frozen contract, while keeping ExecutionPlan and UserPosition independent.
8. Focused unit, integration, JSON-contract, migration, and regression tests.

Schema and API changes are authorized only inside this exact contract. They do
not authorize a general Position, Dashboard, trading, AI, or notification
expansion.

## 3. Required Semantic Separation

The implementation must keep these fields independent:

- `entryLogicStatus`: whether the original entry rationale remains valid;
- `monitorConclusion`: the current monitoring result;
- `reversalStatus`: no, weak, or strong reversal;
- `riskReason`: why position risk changed;
- `suggestedAction`: a manual recommendation, never an execution command;
- `riskLevel`: risk for the exact UserPosition, not account or aggregate risk.

Missing data must remain missing. No field may fall back through another
semantic domain, no missing numeric result may default to zero, and no stale or
unverified log may appear as a successful current result.

## 4. Permanent Safety Boundary

This authorization does not permit:

- automatic open, close, partial close, reduce, add, reverse, or order;
- monitor-triggered execution or any executable trading instruction;
- treating `triggered`, ExecutionPlan, or `tm_real_position` as UserPosition;
- creating a UserPosition without explicit authenticated user input;
- AI overriding the rule-layer base direction;
- Push Recheck acting as trading authorization;
- Mobile or Figma changes;
- Three AI, Score, Message/Push, notification, Telegram, or unrelated Home
  expansion.

All suggested actions remain advisory and require human action.

## 5. Candidate Isolation

The existing branch `codex/p2-position-monitoring-backend-contract` is a local
implementation candidate only. Its uncommitted files are not part of this
authorization package, are not effective product state, and are not approved
by existence. After this authorization becomes effective on merged main, the
next action is an exact-scope review of that candidate against this record and
the frozen Position Monitoring contract before commit or Draft PR creation.

## 6. Runtime Authorization

Before merged-main effectivity, an exact request for
`P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION` must return
`BLOCKED_PENDING_P2_AUTHORIZATION_MERGED_MAIN` with implementation permission
false.

After merged-main effectivity, a clean and synced repository, Product Source
Gate `PASS`, and no active conflicting PR, the same exact request may return:

```text
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
```

No differently named or unscoped package is authorized.

## 7. Completion Evidence Required Later

The implementation package must return for independent review with:

1. schema, entity, enum, mapper, service, and API diffs mapped to frozen fields;
2. per-position risk tests, including different risks and `EXTREME`;
3. stale, pending-verification, invalid, and missing-source fail-closed tests;
4. reversal, conclusion, risk-reason, and manual-action enum coverage;
5. mark-price provenance and `PnL=null` missing-data evidence;
6. closed-position exclusion and ExecutionPlan/UserPosition separation tests;
7. Dashboard Home JSON contract evidence;
8. full Maven, Product Source Gate, Workflow Contract, and scope validation.

Authorization is not Product P2 completion. Product P2 may advance only after
the implementation and its acceptance evidence are independently reviewed and
effective on merged `main`.
