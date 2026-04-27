# Phase 10 — Dashboard `/api/dashboard/refresh` 退场可观测收口

## 1. 本轮范围

- 仅为 `/api/dashboard/refresh` 增加退场可观测头
- 不改业务 JSON 结构
- 不影响 `/api/dashboard/summary`、`/api/dashboard/detail`

## 2. 已完成项

- `Deprecation: true`
- `Link: </api/dashboard/summary>; rel="alternate"; title="replacement"`
- `refresh` 保持原 `DashboardSummaryResponseVO` 契约
- docs 已补「可观测已落地」

## 3. 验收命令

```bash
curl -sI "http://localhost:PORT/api/dashboard/refresh" | grep -E '^(Deprecation|Link):'
```

## 4. 测试结果

- `refresh_keepsLegacyContractAndMetrics`
- `summary_usesDefaultLimitWhenAbsent`
- `detail_doesNotExposeDeprecationHeader`

## 5. 本轮不做什么

- 不下线 `refresh`；不直接下线 `/api/dashboard/refresh`
- 不改 `summary` / `detail` 语义；不改 `/api/dashboard/summary`、`/api/dashboard/detail` 逻辑
- 不改 Dashboard 页面
- 不做监控面板、监控大盘或报表；不做复杂报表
- 不扩成平台化治理系统

## 6. 收口结论

退场可观测已落地，可作为后续真正退场前的观测基础。
