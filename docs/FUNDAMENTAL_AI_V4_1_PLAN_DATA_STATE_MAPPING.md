# Fundamental AI v4.1 Plan Data State Mapping

## Separation Rule

Plan Mode answers how the validated Final permits participation. Plan Data State answers why no Final can currently be displayed. They are separate objects and neither falls back to the other.

## Missing-Final States

| Internal state | Primary copy | Detail behavior |
|---|---|---|
| `UNSELECTED` | 请选择资产 | Select a focus asset to view its plan |
| `LOADING`, `WAITING_ANALYSIS` | 正在分析 | Use the current real analysis stage |
| `FETCHING_DATA` | 正在分析 | 正在获取数据 |
| `GENERATING_EVIDENCE` | 正在分析 | 正在生成证据 |
| `BUILDING_CANDIDATE` | 正在分析 | 正在形成候选计划 |
| `REVIEWING_RISK` | 正在分析 | 正在进行风险复核 |
| `WAITING_RULE_VALIDATION`, `RULE_VALIDATION_PENDING` | 等待规则校验 | Candidate remains outside Final |
| `CANDIDATE_ONLY` | 等待规则校验 | Candidate exists; no validated Final exists |
| `INSUFFICIENT_DATA`, `DATA_QUALITY_BLOCKED`, `PLAN_INCOMPLETE` | 当前数据不足 | Show the real missing-data reason when available |
| `SOURCE_UNAVAILABLE`, `PLAN_IDENTITY_MISSING`, `PLAN_IDENTITY_ERROR` | 数据来源暂不可用 | Fail closed |
| `STALE`, `EXPIRED`, `STATE_SNAPSHOT_MISMATCH` | 当前结果已过期 | Rescan or reanalysis required |
| `REVALIDATION_REQUIRED` | 等待规则校验 | Existing result requires revalidation |
| `RULE_VETOED` | 尚未形成最终计划 | Candidate did not pass rule validation |
| `NO_COMPLETE_PLAN`, `PLAN_MISSING`, `MISSING` | 尚未形成最终计划 | No persisted Final exists |
| `ERROR` | 数据来源暂不可用 | Read failure; fail closed |

## Invariants

- PREPARATION, OBSERVATION, and BLOCKED never use a missing-Final state.
- AI role unavailability does not hide a valid Final.
- Candidate data never populates the Execution Plan region.
- Raw state enums may appear only inside collapsed audit details.
- No state creates a synthetic price, risk, confidence, or percentage.
