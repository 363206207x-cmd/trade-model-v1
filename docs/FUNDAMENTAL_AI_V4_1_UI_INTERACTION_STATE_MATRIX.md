# Fundamental AI v4.1 UI Interaction And State Matrix

## Asset Pool And Dynamic Top6

| State | UI expression | Allowed actions |
|---|---|---|
| Pool empty | 资产池暂无观察资产 | 搜索资产, 恢复默认 |
| Pool populated, no eligible opportunity | 当前没有进入重点机会的资产 | 重新扫描, 打开资产池 |
| Ranking unavailable | 机会排序暂不可用 | 重试, 打开资产池 |
| Ranked | Up to six authoritative projection rows | switch asset, remove, scan, manage pool |

Search does not mutate the Pool. `加入资产池` and `按需分析` remain disabled until the user explicitly selects a search result.

## Asset Context Switching

| Area | Switch behavior |
|---|---|
| Final Execution Plan | clear immediately, show loading, then bind selected asset result |
| Three AI | clear immediately, show loading, then bind selected asset result |
| Conflict/final adjustment | clear with the decision context |
| System Status | unchanged |
| Alerts and events | unchanged |
| Dynamic Top6 | unchanged |
| User positions | unchanged |

Each request carries a monotonically increasing context token. A late response whose token is no longer current is ignored.

## Position Monitoring

| State | Required expression | Forbidden expression |
|---|---|---|
| No position | 暂无活动持仓; manual-entry boundary; 录入持仓 / 查看历史 | fake row, risk, PnL, close button |
| Waiting monitor | waiting/unavailable status | trusted risk, conclusion, action, PnL |
| Verified | P1-KD judgment, facts, and basis fields | semantic fallback or Execution Plan fields |
| Open Top3 | maximum three independent Position rows | automatic action |

## Final Plan

| State | Required expression |
|---|---|
| Loading | skeleton / 正在同步计划 |
| Absent | 暂无最终执行计划 + one reason |
| Rule veto | 规则校验未通过 |
| Stale | 数据已过期 |
| Validated Final | full frozen Final fields and non-trading boundary |

Candidate-only, missing-source, invalid-chain, and stale responses never open the Final body.

## Three AI

One workspace contains three tabs and exactly one visible role. Role state and each collection state remain independent. Top evidence is visible; remaining evidence and technical audit metadata use progressive disclosure. Unavailable roles render one compact state rather than placeholder fields.
