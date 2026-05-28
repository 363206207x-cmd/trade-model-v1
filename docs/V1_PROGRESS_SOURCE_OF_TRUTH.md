# V1 Progress Source Of Truth

This document defines how Trade Model V1 progress is determined.

## Completion Rule

Only merged `main` counts as completed project state.

The following do not count as completed:

- open Issue;
- open PR;
- draft PR;
- local branch;
- remote branch;
- unmerged commit;
- chat memory;
- planned scope;
- docs-only authorization;
- code skeleton without the next capability layer;
- test-only fixture when production wiring is being discussed.

## Required Progress Inputs

Every progress answer must check these sources in this order:

1. `git log --oneline -5` on `main`
2. `docs/V1_CURRENT_STATE.md`
3. `docs/PROJECT_PROGRESS_INDEX.md`
4. `docs/V1_CAPABILITY_MATRIX.md`
5. `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`

If these sources disagree, merged `main` wins and the docs must be corrected.

## Forbidden Progress Shortcuts

Do not use chat memory to determine progress.

Do not treat docs-only work as production completion.

Do not treat a skeleton as production wiring.

Do not treat targeted tests as runtime behavior.

Do not treat an open PR, open branch, or open Issue as already merged.

Do not treat legacy runtime clients as proof that the new scan-chain is complete.

Do not count repeated blocked-list documents as product usability progress.

## Capability Language

Progress must be described by capability level:

| Level | Name | Meaning |
|---:|---|---|
| 0 | NOT_STARTED | No usable project artifact for this capability. |
| 1 | DOCS_ONLY_GATE | Documentation, scope, authorization, or closure only. |
| 2 | SKELETON | Code shape exists but does not yet prove behavior with targeted tests. |
| 3 | TARGETED_TEST | Skeleton plus focused tests or safe rule tests. |
| 4 | TEST_ONLY_WIRING | Components are connected only in tests or fixtures. |
| 5 | REVIEW_ONLY_RUNTIME | Runtime or UI can show safe, non-executable, manual-review output. |
| 6 | PRODUCTION_WIRING | Real runtime wiring exists, still not necessarily production-ready. |
| 7 | PRODUCTION_READY | Production-ready behavior with complete safety, observability, and review requirements. |

## Review-Only Principle

Review-only does not mean no output.

Review-only means useful proposals and risk actions may be shown to a human while staying:

- manual-review required;
- not a trade instruction;
- non-executable;
- blocked from automatic order, close, reverse, leverage change, execution, or external send unless separately authorized.
