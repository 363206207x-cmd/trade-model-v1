# Stale PR / Issue Hygiene

Open PRs and Issues should only remain open when they represent the current active work item or a known future item.

Stale PRs and Issues pollute Source of Truth because scripts and humans can mistake them for the current task.

Before entering a new mainline, check stale PRs and Issues. If a stale item is clearly covered by later merged work, close it or label it stale / superseded. If coverage is uncertain, record `needs human review` and do not close it.

## Current P291H Cleanup

| Item | Title | Result | Reason |
|---|---|---|---|
| PR #701 | BACKEND-P291 MarketReadRequestGuardValidator Closure and Test-Only Wiring Authorization Scope Pack | Closed as stale / superseded in P291H | Superseded by merged P291A / P291C / P291D / P291E / P291F / P291G workflow packs and later market-read slices. |
| PR #707 | BACKEND-P291B Source of Truth Operational Fill Pack | Closed as stale / superseded in P291H | Superseded by merged P291A / P291C / P291D / P291E / P291F / P291G source-of-truth and workflow packs. |
| Issue #700 | BACKEND-P291 MarketReadRequestGuardValidator Closure and Test-Only Wiring Authorization Scope Pack | Closed as stale / superseded in P291H | Superseded by later merged workflow and market-read packages. |
| Issue #706 | BACKEND-P291B Source of Truth Operational Fill Pack | Closed as stale / superseded in P291H | Superseded by later merged source-of-truth and workflow packages. |

Do not close active PRs, active Issues, or items whose purpose cannot be confirmed.
