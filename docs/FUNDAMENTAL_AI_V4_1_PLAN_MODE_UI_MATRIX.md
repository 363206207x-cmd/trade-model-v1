# Fundamental AI v4.1 Plan Mode UI Matrix

## Formal Final Modes

| Internal mode | Type label | Current plan status | Primary meaning | Default content |
|---|---|---|---|---|
| `CONFIRMATION` | 确认型 | 条件已确认 | Direction, evidence, trigger, risk, and validation are satisfied | Full plan |
| `PREPARATION` | 预备型 | 等待触发 | Direction and core logic exist; trigger is pending | Trigger, timeframe, upgrade, invalidation, validity; trusted optional levels only |
| `REDUCED` | 缩减型 | 降低强度 | Opportunity remains valid with effective counterevidence or risk | Full plan plus downgrade, risk, limits, and recovery |
| `OBSERVATION` | 观察 | 当前仅观察 | Analysis is valuable but no directional participation plan is formed | Observation reason, indicators, upgrade, invalidation, window |
| `BLOCKED` | 阻断 | 当前已阻断 | Data, risk, conflict, state, or rule validation blocks participation | Block reason, conflict, risk, recovery, revalidation |

## Section Matrix

| Section | CONFIRMATION | PREPARATION | REDUCED | OBSERVATION | BLOCKED |
|---|:---:|:---:|:---:|:---:|:---:|
| Final summary | Yes | Yes | Yes | Yes | Yes |
| 入场与触发 | Yes | Trusted optional values only | Yes | No | No |
| 失效与止损 | Yes | Invalidation focus | Yes | No | No |
| 目标与趋势跟踪 | Yes | Trusted optional values only | Yes | No | No |
| 风险限制 | Yes | No default values | Yes, emphasized | No | Risk reason only |
| 时间有效性 | Yes | Validity focus | Yes | Observation window | Revalidation condition |
| 降级与恢复 | No | Upgrade condition | Yes, emphasized | Upgrade condition | Recovery condition |

## Rendering Rules

- Every mode is a persisted, validated Final result.
- Mode labels are user-facing; raw enums stay in audit details only.
- Empty optional fields are omitted.
- OBSERVATION and BLOCKED never render entry, stop, or target sections.
- PREPARATION never becomes `暂无计划` merely because a trigger or optional price level is missing.
- Color communicates mode status without turning unavailable data into success.
