# Phase 9 Step 3 - Review Aggregate Layering Contract

## 文档目的

本文件用于承接「第九阶段 Step 3：Review 聚合分层（摘要 vs 明细）与性能护栏」第二轮输出，交付三项可直接执行内容：

- 接口契约文档（summary/detail/legacy）
- 验收清单（含性能护栏校验项）
- 前端接入顺序执行指令

范围控制：仅覆盖 `GET /api/review/aggregate/{analysisId}` 及其分层接口，不扩新 section、不改数据库结构、不引入分页协议升级。

## 1. 接口契约文档

### 1.1 接口总览

- 兼容旧接口：`GET /api/review/aggregate/{analysisId}`
- 首屏摘要接口：`GET /api/review/aggregate/{analysisId}/summary`
- 明细懒加载接口：`GET /api/review/aggregate/{analysisId}/detail`

统一返回壳：`ApiResponse<T>`

错误语义：

- `analysisId` 不存在：`404`（`analysis not found: {analysisId}`）
- `detail.section` 非法：`400`（`unsupported detail section: ...`）

### 1.2 旧接口（兼容保留）

- Path: `GET /api/review/aggregate/{analysisId}`
- 用途：兼容历史调用方，返回“大而全”聚合结构
- 返回体：`ReviewAggregateVO`
- 说明：本轮不建议新接入方继续依赖该接口；建议迁移到 summary + detail 组合

### 1.3 摘要接口契约（首屏轻载）

- Path: `GET /api/review/aggregate/{analysisId}/summary`
- 用途：首屏先轻，优先渲染核心复盘上下文
- 返回体：`ReviewAggregateSummaryVO`

核心字段：

- `run`
- `decision`
- `plan`
- `reviewClosure`
- `detailSections`（明细 section 元信息）

`detailSections` 说明：

- `section`: 明细分区名
- `total`: 分区总量（用于前端显示“还有多少条”）
- `recommendedLimit`: 推荐首轮拉取条数

当前支持的 section 元信息：

- `pushRecheck`
- `missed`
- `alerts`
- `ruleVersionLogs`
- `hotReset`

### 1.4 明细接口契约（按 section 懒加载）

- Path: `GET /api/review/aggregate/{analysisId}/detail`
- Query 参数：
  - `section`（可选，默认 `pushRecheck`）
  - `limit`（可选，默认 `20`）
- 返回体：`ReviewAggregateDetailVO`

公共字段：

- `analysisId`: 当前 analysis 上下文
- `section`: 实际生效 section
- `limitApplied`: 护栏收敛后实际生效 limit
- `total`: 该 section 总量
- `truncated`: 是否被截断（`total > 返回条数`）

当前允许的 `section`：

- `pushRecheck`
- `missed`
- `alerts`
- `ruleVersionLogs`
- `hotReset`

### 1.5 性能护栏契约

- `limit <= 0`：回落默认值 `20`
- `limit > 50`：截断到上限 `50`
- 即：`limitApplied` 永远落在 `(0, 50]`
- `pushRecheck` section 内部，单条 push 的 `rechecks` 最多返回 `5` 条（避免明细嵌套爆量）

特殊说明：

- `ruleVersionLogs` 明细当前直接按查询结果返回，`truncated` 固定 `false`
- `hotReset` 明细为单对象，不受 `limit` 条数语义影响，但仍返回统一公共字段

### 1.6 示例请求

```bash
curl "http://localhost:8080/api/review/aggregate/a-100/summary"
curl "http://localhost:8080/api/review/aggregate/a-100/detail?section=pushRecheck&limit=20"
curl "http://localhost:8080/api/review/aggregate/a-100/detail?section=alerts&limit=50"
curl "http://localhost:8080/api/review/aggregate/a-100/detail?section=hotReset"
```

## 2. Step 3 验收清单（可打勾执行）

### 2.1 接口可用性与分层成立

- [ ] `GET /api/review/aggregate/{analysisId}/summary` 返回 `200` 且仅承载首屏核心
- [ ] `GET /api/review/aggregate/{analysisId}/detail` 可按 `section` 懒加载
- [ ] `GET /api/review/aggregate/{analysisId}` 仍可访问（兼容未破坏）

### 2.2 section 分发正确性

- [ ] `section=pushRecheck` 返回 `pushRecheck` 明细
- [ ] `section=missed` 返回 `missed` 明细
- [ ] `section=alerts` 返回 `alerts` 明细
- [ ] `section=ruleVersionLogs` 返回 `ruleVersionLogs` 明细
- [ ] `section=hotReset` 返回 `hotReset` 明细
- [ ] `section` 为空/空白时默认落到 `pushRecheck`
- [ ] 非法 `section` 返回 `400`

### 2.3 性能护栏与边界

- [ ] `limit<=0` 时 `limitApplied=20`
- [ ] `limit>50` 时 `limitApplied=50`
- [ ] 各明细 section 均返回 `total` 与 `truncated`
- [ ] `pushRecheck` 中每条 push 的 `rechecks` 条数不超过 `5`

### 2.4 错误语义

- [ ] 不存在的 `analysisId` 调 summary 返回 `404`
- [ ] 不存在的 `analysisId` 调 detail 返回 `404`
- [ ] 非法 `section` 调 detail 返回 `400`

### 2.5 自动化回归

- [ ] `ReviewAggregateServiceImplTest` 覆盖 section 分发
- [ ] `ReviewAggregateServiceImplTest` 覆盖 limit 护栏
- [ ] `ReviewAggregateServiceImplTest` 覆盖 push 内部 recheck 限流
- [ ] 回归通过（建议：`mvn test -Dtest=ReviewAggregateServiceImplTest`）

## 3. 前端接入顺序执行指令

### 3.1 接入原则（先轻后重）

- 先接 summary，确保首屏稳定出内容
- 明细按用户可见区域触发拉取，不做首屏全量并发
- 对旧接口保留降级开关，避免一次性切换引发联调阻塞

### 3.2 执行步骤（按顺序）

1) **首屏改造**

- 首屏数据源改为：`GET /api/review/aggregate/{analysisId}/summary`
- 首屏只消费：`run/decision/plan/reviewClosure`
- 从 `detailSections` 渲染各区“可展开 + 数量提示”

2) **明细分区改造**

- 分区展开时调用：`GET /api/review/aggregate/{analysisId}/detail`
- 请求参数使用 `section + limit`
- 首次建议统一 `limit=20`；超长区允许“查看更多”升级到 `50`

3) **列表与状态处理**

- 若返回 `truncated=true`，前端显示“已展示前 N 条，可继续加载”
- 若返回 `404`，显示“分析不存在或已失效”
- 若返回 `400`，提示“明细分区参数非法”，并回退到默认 section

4) **兼容与回退**

- 保留旧接口调用开关（feature flag 或配置位）
- 新链路异常时可临时回退 `GET /api/review/aggregate/{analysisId}`
- 回退仅作为兜底，不作为常态路径

5) **收口前检查**

- 对比新旧首屏关键字段一致性（run/decision/plan/reviewClosure）
- 验证 detail 五个 section 均可独立渲染
- 验证 `limitApplied/total/truncated` 前端文案正确

### 3.3 前端联调最小用例

- 用例 A：正常 analysis，先 summary 再拉 `pushRecheck(limit=20)`
- 用例 B：`alerts(limit=999)`，验证 `limitApplied=50`
- 用例 C：`missed(limit=0)`，验证 `limitApplied=20`
- 用例 D：`section=unknown`，验证 `400`
- 用例 E：不存在 `analysisId`，验证 `404`

## 4. 非本轮目标（保持边界）

以下不作为本轮通过条件：

- 前端最终 UI/交互精修
- 明细 section 再细分
- 旧接口下线与彻底移除
- 体积/耗时基准脚本自动化
- 文档站点化发布
