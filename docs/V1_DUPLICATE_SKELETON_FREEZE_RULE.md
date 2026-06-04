# V1 Duplicate Skeleton Freeze Rule

This rule turns the #830 global audit conclusion into a project workflow gate.

It is a workflow / governance rule only. It does not add Java, tests, runtime wiring, dashboard wiring, market data reads, push, external channel, executable point generation, order execution, or auto-trading.

## 1. Freeze Rule

After this rule is merged, the project must not default to continuing the following package types:

- new DTO;
- new Validator;
- new Assembler;
- new Orchestrator;
- new docs-only plan;
- new verification-only package;
- new source-binding wrapper;
- new runtime-candidate wrapper;
- new point-candidate wrapper.

An exception is allowed only when the proposed package satisfies at least one of these conditions:

- directly reduces duplicate objects;
- directly merges Cursor-era assets with Codex-era skeletons;
- directly selects canonical ownership;
- directly connects to an existing service / runtime / dashboard / API review-only path;
- directly moves capability level toward `REVIEW_ONLY_RUNTIME`;
- directly resolves a runtime wiring gap identified by the #830 audit.

If the package only adds another wrapper, carrier, validator, assembler, plan, or verification document without reducing duplication or moving toward runtime usability, it must be rejected.

## 2. Required Pre-check Before Any New Package

Before any new package, GPT / Codex must answer:

- Does this create a new DTO / Validator / Assembler / Orchestrator?
- Is there a Cursor-era or Codex-era object that can be reused instead?
- Will this increase duplicate skeleton surface?
- Does this connect a real input source?
- Does this connect service / dashboard / API?
- Does this raise the capability level?
- Is this only another review-only wrapper?
- Does this bypass the #830 audit recommendation?

If the answer shows the package is only a new skeleton or wrapper, Codex must refuse to continue and redirect to the allowed stop-loss tracks.

## 3. P359 / P360 Status

- P359 branch exists, but it is not merged.
- PR #829 was closed without merge.
- P359 does not count as completed progress.
- P360 is not allowed to start.
- P359 may be reconsidered only after ownership map and wiring plan work proves it will not add duplicate skeleton surface.
- The default decision is not to revive P359.

## 4. Next Allowed Tracks

Only the following stop-loss tracks are allowed next:

1. Cursor Artifact Inventory + Ownership Map.
2. Runtime Wiring Target Selection Plan.
3. Source-Owned Runtime vs Existing Point Proposal Merge Map.
4. Minimal Review-Only Runtime Integration Plan.
5. Existing Dashboard / Service / API Runtime Slice Selection.

The following tracks remain frozen:

- Three AI;
- Position Monitor expansion;
- Dashboard expansion;
- Push external channel;
- executable point generation;
- new source binding family;
- new candidate family;
- new point family;
- order / execution / auto-trading.

## 5. Capability-Level Rule

Package count is not progress.

Future progress must be stated with capability levels:

| Level | Name |
|---:|---|
| 0 | NOT_STARTED |
| 1 | DOCS_ONLY_GATE |
| 2 | SKELETON |
| 3 | TARGETED_TEST |
| 4 | TEST_ONLY_WIRING |
| 5 | REVIEW_ONLY_RUNTIME |
| 6 | PRODUCTION_WIRING |
| 7 | PRODUCTION_READY |

Every proposed next step must state:

- current level;
- target level;
- whether it truly raises the level;
- if it does not raise the level, why it is still worth doing.

## 6. Required Final Output For Future Packages

Every future Codex final response must add:

- 是否创建新骨架: Yes / No
- 是否复用 Cursor-era 资产: Yes / No
- 是否减少重复: Yes / No
- 是否提升 capability level: Yes / No
- 是否接 service/runtime/dashboard/API: Yes / No
- 是否符合 #830 审计建议: Yes / No

These lines are required in addition to the task-specific output fields.

## 7. Source-Of-Truth Update

Current active block:

- `Global Duplicate Skeleton Freeze Rule`

Next required action:

- `Cursor Artifact Inventory + Ownership Map`

Current stop decision:

- Do not continue P359.
- Do not start P360.
- New skeleton freeze is active.
- The next allowed action is ownership mapping, not feature development.
