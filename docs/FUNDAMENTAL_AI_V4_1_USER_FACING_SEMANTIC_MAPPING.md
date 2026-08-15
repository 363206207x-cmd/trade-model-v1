# Fundamental AI v4.1 User-Facing Semantic Mapping

## Ownership

The sole runtime owner is `TradeModelFrontendContract.USER_FACING_SEMANTIC_MAPPER`. Raw contract codes remain available to audit logic but do not appear as primary user copy.

## Plan State

| Contract value | User-facing value |
|---|---|
| `NO_COMPLETE_PLAN` | 暂无最终执行计划 |
| `WAITING_ANALYSIS` | 等待分析 |
| `INSUFFICIENT_DATA` | 数据不足 |
| `STALE` | 数据已过期 |
| `RULE_VETOED` | 规则校验未通过 |
| `AI_UNAVAILABLE` | AI解释暂不可用 |
| `PREPARATION` | 等待触发 |
| `OBSERVATION` | 当前仅观察 |
| `BLOCKED` | 当前已阻断 |

## AI Role State

| Contract value | User-facing value | Tone |
|---|---|---|
| `READY` | 分析完成 | positive |
| `PARTIAL` | 部分结果可用 | warning |
| `FALLBACK` | 当前使用规则降级结果 | warning |
| `UNAVAILABLE` | 当前分析不可用 | unavailable |
| `ERROR` | 分析失败 | danger |

## Collection State

| Contract value | User-facing value |
|---|---|
| `FOUND` | 已发现 |
| `NONE_FOUND` | 完成检查，未发现 |
| `INSUFFICIENT_DATA` | 数据不足，无法判断 |
| `SOURCE_UNAVAILABLE` | 数据来源暂不可用 |
| `STALE` | 数据已过期 |
| `NO_VERIFIABLE_FAILURE_PATH` | 暂无可验证失败路径 |

## Direction, Risk, Confidence, And Plan Mode

Market Bias uses the frozen eight-state labels. Risk and confidence use `低 / 中 / 高 / 极高` where supported. Plan Mode uses `确认型 / 预备型 / 缩减型 / 观察 / 阻断`. Mixed AI strings are token-mapped before rendering, so transitions such as `HIGH -> MEDIUM` are shown as `高 -> 中`.

## Primary-Surface Prohibition

These internal strings must not appear in the primary UI: `NO_COMPLETE_PLAN`, `SOURCE_UNAVAILABLE`, `UNAVAILABLE`, `PRODUCT_CONTRACT_ONLY`, `OWNER_SCOPED`, `OWNER SCOPED`, `STRUCTURED AI DECISION CHAIN`, `Role State`, `Data State`, `Provider`, `Trace`, and `Fallback`.

Provider, trace, raw state, fallback reason, analysis identity, and generation metadata are permitted only inside collapsed audit details.

## Fail-Closed Rule

An unknown or absent value maps to a neutral unavailable state. The mapper never invents a positive, trusted, actionable, or numeric value.
