# Fundamental AI v4.1 Final Interaction Page and Runtime Authorization

Authorization status: `AUTHORIZED_PENDING_MERGED_MAIN`

Authorization package:
`FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION`

Implementation status: `NOT_STARTED`

Source date: `2026-08-14`

## 1. Authority

This authorization is subordinate to the sole canonical Product Source:

`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`

Normative annexes:

- `docs/FUNDAMENTAL_AI_V4_1_PAGE_ROUTE_COMPONENT_MATRIX.md`;
- `docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_OBJECT_OWNERSHIP_MAP.md`;
- `docs/FUNDAMENTAL_AI_V4_1_PR1179_REUSE_AND_SUPERSESSION_MAP.md`;
- `docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md`.

It becomes effective only when this authorization commit is merged into a
clean, synchronized `main`, the Product Source Gate and Workflow Contract pass,
and the exact machine-readable package resolves as allowed.

## 2. Exact Authorized Scope

The exact package may:

1. continue implementation on PR #1179 branch
   `codex/v4-1-frontend-runtime-alignment` after synchronizing/rebasing it onto
   the merged authorization main;
2. implement all fourteen Desktop routes, eleven overlays, fifty-four component
   families and eighty-one Desktop acceptance states;
3. reuse and adapt approved PR #1179 frontend, view-model, tests and visual
   assets according to the reuse map;
4. extend existing frontend view models, Home projection and APIs only as
   required by the frozen interaction source;
5. implement explicit `ANALYSIS_PREVIEW` and `OPPORTUNITY_DECISION` modes;
6. implement waiting-trigger Candidate/reviews/resolver/validation to Final
   `PREPARATION`, followed by triggered Plan Revalidation;
7. implement independent Plan lifecycle, versions, supersession and
   PlanRevalidationRecord;
8. implement the single Message fact owner, Telegram binding/delivery/filtering
   and routed Push Recheck;
9. complete Asset Pool operations, multi-timeframe dynamic Top6 and selected
   asset context persistence;
10. complete Review/Missed Opportunity, original/latest plan position comparison,
    Event Calendar, My/Settings and Full Audit Chain;
11. add the minimum sequential Schema migration and API changes required by
    canonical ownership, never a parallel skeleton;
12. update application/business tests, browser scenarios, documentation and
    audit evidence needed for independent product-level audit.
13. modify the single registered Canonical Figma file
    `rdMYmsAvZYkXHJX8hdl7UN` for the frozen Desktop scope only;
14. implement the fourteen Desktop routed pages, eleven overlays, fifty-four
    component families and eighty-one Desktop acceptance states in that file;
15. extend approved components and variants with Auto Layout and Variables,
    record resulting Figma node IDs, and produce route/state-matched
    Figma/runtime visual comparisons.

The Canonical Figma permission is an implementation permission inside this
exact package. It does not make any pre-amendment Figma mutation retroactively
authorized and it does not establish runtime acceptance by itself.

## 3. Required Reuse

The implementation must preserve PR #1179's Final-only rendering, five Plan
Modes, Candidate/Final separation, single structured Three-AI workspace,
manual UserPosition lifecycle, P2 Position Monitoring trust gate, fail-closed
behavior, no-fake-data contract, zero automatic trading and approved Desktop
visual components.

Existing Analysis, Opportunity, Plan, Position, Monitoring, Review, Message,
Home and Asset Pool owners are extended, not recreated. The ownership map is a
merge gate.

## 4. Forbidden Scope

The package must not:

- automatically open, close, add, reduce or reverse a position;
- call an exchange order endpoint or add automatic order capability;
- allow AI to bypass Rule Validation or modify the Opportunity state machine;
- let Preview create Opportunity, Candidate, Resolver, Validation or Final;
- expose Candidate as Final or auto-convert Final into UserPosition;
- fabricate market, evidence, AI, plan, position, message, progress or audit data;
- use fake percentages for asynchronous work;
- create duplicate business ownership;
- create a second Figma file or a second Design System;
- modify any Figma file other than Canonical file
  `rdMYmsAvZYkXHJX8hdl7UN`;
- create Mobile frames or Mobile screenshots;
- replace editable Canonical Figma layers with static screenshots;
- treat a static Figma artifact as a substitute for running code;
- implement Mobile pages, Mobile CSS/JS, Mobile navigation or Mobile screenshots;
- start a later product package.

## 5. Required Acceptance

Independent audit must prove:

- all 18 disambiguation contracts in the canonical source;
- all routes, overlays, states, components and prototype flows in the matrix;
- Preview isolation and Opportunity Decision completeness;
- Asset Pool-only continuous discovery and dynamic multi-timeframe Top6;
- Candidate/Final/Resolver/Validation separation and source trace;
- Message/Telegram/Recheck/Revalidation boundaries;
- manual Position and immutable opening-plan baseline;
- fail-closed missing/stale/source-unavailable paths;
- no semantic fallback, no fake data and automatic trading capability count 0;
- Product Source, Workflow, Duplicate Skeleton, Maven, migration, browser and
  real runtime scenarios appropriate to changed scope.

## 6. Machine Permission Contract

After merged-main effectivity, and only for the exact package:

```text
AUTHORIZED_PACKAGE: FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: true
MOBILE_IMPLEMENTATION_ALLOWED: false
IMPLEMENTATION_STATUS: NOT_STARTED
```

Any old frontend package, the historical backend package, a typo, an unscoped
package or a broader package must return implementation, PR creation and
Canonical Figma Desktop permissions `false`. Mobile implementation remains
`false` for every package.

## 7. Current Authorization Task Boundary

This authorization task changes only Product Source, contracts, ownership,
reuse mapping, delivery/machine state, gates and reports. It changes no
application code, API, Schema, Figma or Mobile implementation and leaves PR
#1179 Head `62ba9702e54b268ef27158bcff7e33422e23015e` unchanged.

That Head is an existing implementation candidate created before this
Canonical Figma permission amendment became effective. Its disposition is
`REUSABLE_PENDING_AUTHORIZATION_RECONCILIATION`. After this amendment is
merged, PR #1179 may be synchronized with amended `main`; all validation and
independent audit must use the resulting new exact Head. The pre-sync Head
cannot receive final approval or merge authorization.
