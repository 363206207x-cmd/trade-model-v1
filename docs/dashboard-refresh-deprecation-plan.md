# Dashboard Refresh 旧接口退场计划（最小版）

## 1. 目标与边界

- 目标：在不扩大改造面的前提下，安全下线旧接口 `/api/dashboard/refresh`，统一到新查询契约（`/api/dashboard/summary` 与 `/api/dashboard/detail`）。
- 边界：仅覆盖依赖识别、迁移顺序、灰度与删除条件；不新增功能，不做大规模前端重构。

## 2. 调用方依赖清单

先完成“调用方-字段依赖清单”，避免删接口时靠猜测。

建议最小清单字段如下：

- 调用方名称（页面/服务/脚本/测试）
- 当前调用接口（是否仍调用 `/api/dashboard/refresh`）
- 实际消费字段列表
- 对应替代接口（`summary` 或 `detail`）
- 负责人
- 迁移状态（未开始/迁移中/已完成）
- 备注（风险、阻塞、临时兼容说明）

## 3. 迁移顺序

### 阶段 A：冻结旧接口

- 旧接口仅保留兼容层职责，不再承载新需求。
- 禁止为 `/api/dashboard/refresh` 新增业务字段或新语义。
- 在接口文档与注释中明确标记 deprecated。

### 阶段 B：按调用方迁移

- 概览/列表类调用迁移到 `/api/dashboard/summary`。
- 单标的详情调用迁移到 `/api/dashboard/detail`。
- 每迁移一处即删除该处旧接口字段消费，避免长期双读。

### 阶段 C：兼容收口

- 旧接口内部保持最薄适配，不再堆叠分支逻辑。
- 未迁移调用方必须登记在依赖清单并标注负责人和计划时间。

### 阶段 D：deprecated 可观测提示（新增小收口）

- 在旧 `/api/dashboard/refresh` 的响应头或日志中增加 deprecated 标记。
- 目标：让调用方和排查人员可快速识别“仍在使用旧接口”。
- 建议标记信息至少包含：接口路径、deprecated 状态、建议迁移目标接口。

### 阶段 D 落地状态（可观测）

- [x] **可观测已落地（响应头）**：`/api/dashboard/refresh` 返回 `Deprecation: true`，并附带 `Link: </api/dashboard/summary>; rel="alternate"; title="replacement"`，JSON body 与迁移前一致；`/api/dashboard/summary`、`/api/dashboard/detail` 不增加该标记。
- 最小验收命令（本地服务启动后，将 `PORT` 换为实际端口）：

```bash
curl -sI "http://localhost:PORT/api/dashboard/refresh" | grep -E '^(Deprecation|Link):'
```

期望：`Deprecation: true`，且存在 `Link` 指向 `summary`。

## 4. 灰度与回滚

- 灰度方式：按调用方或流量比例逐步切换到新接口。
- 观察窗口：每批灰度后至少观察 24-48 小时。
- 核心指标：错误率、P95/P99 耗时、关键页面可用性。
- 回滚策略：仅回滚路由或开关，不回滚新查询契约代码与数据模型。

## 5. 删除准入条件

仅在以下条件全部满足后，才允许删除旧接口：

- 连续 7 天旧接口调用量归零（或仅白名单压测流量）。
- 新接口（`summary/detail`）稳定，无明显错误率或耗时回归。
- 契约测试与关键端到端验证全部通过。
- 依赖清单全部勾销，并由各调用方负责人确认。

## 6. 最终删除动作

- 删除 `/api/dashboard/refresh` 路由及其专属兼容逻辑。
- 删除旧接口专属测试，保留并补强 `summary/detail` 契约测试。
- 更新 API 文档和变更记录，标注正式移除版本与日期。

## 7. 最小执行清单模板

以下表格用于直接开填与持续勾销（每个调用方一行）：

| 调用方名称 | 类型（页面 / 服务 / 脚本 / 测试） | 是否仍调用 /api/dashboard/refresh | 实际消费字段 | 对应新接口替代方案（summary / detail） | 迁移负责人 | 当前状态（未开始 / 迁移中 / 已完成） | 计划日期 | 证据链接 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 示例：Dashboard 首页 | 页面 | 是 | totalProfit, positions, alerts | summary | 张三 | 迁移中 | 2026-04-20 | [PR #123](https://example.com/pr/123) | 需要先补埋点 |
|  |  |  |  |  |  |  |  |  |  |
