# V1 Progress Source Of Truth

This document is the operational rule for deciding Trade Model V1 progress.

## 1. Completion Rule

Only merged `main` counts as done.

The following never count as completed:

- open PR;
- draft PR;
- branch;
- Issue;
- Codex output;
- chat memory;
- local commit not merged to `main`;
- remote branch not merged to `main`;
- planned scope;
- PR body promise;
- marker / placeholder file without completed content.

If a branch or PR contains useful work but has not merged, write it as `open / pending / not completed`.

## 2. Required New-Window Read Order

Every new chat window, Codex task, progress answer, and PR review must read:

1. `git log --oneline -5`
2. `docs/V1_PROGRESS_SOURCE_OF_TRUTH.md`
3. `docs/V1_CAPABILITY_MATRIX.md`
4. `docs/V1_CURRENT_STATE.md`
5. `docs/PROJECT_PROGRESS_INDEX.md`

Then read the task-specific docs and code.

Do not use chat memory to decide current progress.

## 3. Current Main Baseline

Current merged main at P291B creation:

- `26e8943 BACKEND-P291A Workflow Reset and Progress Source of Truth Pack (#703)`

Merged P287-P291A packages:

- P287: Market-Read Request DTO Authorization Gate.
- P288: MarketReadRequestDTO Java Skeleton.
- P289: MarketReadRequestDTO Closure and Guard Validator Authorization Scope Pack.
- P290: MarketReadRequestGuardValidator Java Skeleton.
- P291A: Workflow Reset and Progress Source of Truth Pack.

Known open PR at P291B creation:

- PR #705 P292 MarketReadRequest Test-Only Wiring and Review-Only Assembler Slice.

PR #705 is not completed until merged.

## 4. Capability Levels

Use only these levels:

| Level | Name | Operational meaning |
|---:|---|---|
| 0 | NOT_STARTED | No merged artifact for this capability. |
| 1 | DOCS_ONLY_GATE | Docs, scope, authorization, closure, or plan only. |
| 2 | SKELETON | Code shape exists, but behavior is not fully proven. |
| 3 | TARGETED_TEST | Skeleton plus focused unit/targeted tests. |
| 4 | TEST_ONLY_WIRING | Components are connected only in test scope or fixtures. |
| 5 | REVIEW_ONLY_RUNTIME | Runtime/UI can show non-executable manual-review output. |
| 6 | PRODUCTION_WIRING | Real runtime wiring exists, but not necessarily production-ready. |
| 7 | PRODUCTION_READY | Production-ready behavior with safety, observability, and review closure. |

## 5. Misclassification Rules

Do not write docs-only as production complete.

Do not write skeleton as production wiring.

Do not write targeted test as runtime behavior.

Do not write test-only wiring as production.

Do not write review-only as no output.

Do not write blocked as no useful output.

Do not write legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` as completion of the new scan-chain.

Do not write Display Slots as Watchlist Pool.

Do not write an open PR as merged main.

## 6. Review-Only Rule

Review-only means useful output may be shown to a human while staying:

- manual-review required;
- not a trade instruction;
- non-executable;
- blocked from automatic order, close, reverse, leverage change, execution, or external send unless separately authorized.

Review-only can include proposals and downgrade suggestions. It must not become automatic execution.

## 7. Progress Answer Rule

Every progress answer must state:

- current `main` HEAD;
- whether relevant PRs are merged or open;
- capability level from `docs/V1_CAPABILITY_MATRIX.md`;
- next business-chain step from `docs/V1_MVP_REALITY_ROADMAP.md`;
- whether the answer is about docs, skeleton/test, test-only wiring, review-only runtime, or production wiring.
