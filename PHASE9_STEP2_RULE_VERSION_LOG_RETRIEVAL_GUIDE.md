# Phase 9 Step 2 - Rule Version Log Retrieval Guide

## 文档目的

本文件用于承接「第九阶段 Step 2：规则版本审计链可检索升级」下一轮交付，输出三项最小可执行内容：

- 最小检索使用说明
- 推荐查询样例
- Step 2 验收清单

范围控制：仅覆盖已上线接口 `GET /api/review/rule-version-logs` 的查询使用，不扩 schema、不扩平台能力、不引入复杂分页协议。

## 1. 最小检索使用说明

### 1.1 接口定义

- Method: `GET`
- Path: `/api/review/rule-version-logs`
- 返回壳: `ApiResponse<List<ReviewAggregateVO.RuleVersionLogSummary>>`

### 1.2 查询参数（全部可选）

- `analysisId`: 分析链路 ID 精准过滤
- `ruleVersion`: 规则版本过滤
- `operator`: 操作人过滤（例如 `SYSTEM`）
- `rollbackFlag`: 回滚标记过滤
- `errorType`: 错误类型过滤（支持旧数据 fallback 匹配）
- `changeCategory`: 变更分类过滤（支持旧数据 fallback 匹配）
- `keyword`: 关键字模糊搜索（`changeSummary` / `changeDetail`）
- `createdAtFrom`: 起始时间（含）
- `createdAtTo`: 结束时间（含）
- `limit`: 返回条数限制，默认 `20`，最大 `50`

### 1.3 输入收敛与边界行为

- 所有字符串参数会先做 `trim`
- 仅空白字符串会收敛为 `null`（即视为不传）
- `limit <= 0` 时按默认值 `20` 处理
- `limit > 50` 时按上限 `50` 截断

### 1.4 返回字段说明（单条日志）

- `id`: 日志主键
- `analysisId`: 关联分析 ID（缺失时可由 `changeSummary` fallback）
- `ruleVersion`: 规则版本
- `errorType`: 错误类型（缺失时可由 `changeSummary` fallback）
- `changeCategory`: 变更分类（缺失时可由 `changeSummary` fallback）
- `changeSummary`: 变更摘要（原文）
- `changeDetail`: 变更详情（原文）
- `operator`: 操作人
- `rollbackFlag`: 回滚标记
- `createdAt`: 创建时间
- `fallbackMatched`: 是否触发了旧数据兼容补齐

### 1.5 兼容链说明（旧数据不报废）

当结构化字段缺失时，服务层仍会从 `changeSummary` 解析并回填以下字段：

- `analysisId`
- `errorType`
- `changeCategory`

`fallbackMatched=true` 可作为兼容命中标记，用于审计和后续迁移观察。

## 2. 推荐查询样例

> 示例默认本地服务地址为 `http://localhost:8080`，可按实际环境替换。

### 2.1 查某次分析链路的全部规则日志（常用入口）

```bash
curl "http://localhost:8080/api/review/rule-version-logs?analysisId=a-100&limit=20"
```

### 2.2 按规则版本 + 错误类型定位问题归因

```bash
curl "http://localhost:8080/api/review/rule-version-logs?ruleVersion=v8&errorType=MISS&limit=30"
```

### 2.3 按操作人 + 回滚标记排查发布/回滚动作

```bash
curl "http://localhost:8080/api/review/rule-version-logs?operator=SYSTEM&rollbackFlag=Y&limit=20"
```

### 2.4 按变更分类 + 关键字做语义筛查

```bash
curl "http://localhost:8080/api/review/rule-version-logs?changeCategory=REVIEW_FEEDBACK_SAVED&keyword=adjustmentSuggestion&limit=20"
```

### 2.5 按时间窗口做阶段回溯

```bash
curl "http://localhost:8080/api/review/rule-version-logs?createdAtFrom=2026-04-01T00:00:00&createdAtTo=2026-04-17T23:59:59&limit=50"
```

### 2.6 组合查询（验收推荐）

```bash
curl "http://localhost:8080/api/review/rule-version-logs?analysisId=a-100&ruleVersion=v8&operator=SYSTEM&changeCategory=REVIEW_FEEDBACK_SAVED&createdAtFrom=2026-04-01T00:00:00&createdAtTo=2026-04-17T23:59:59&limit=50"
```

### 2.7 响应示例（结构）

```json
{
  "code": 200,
  "msg": "success",
  "requestId": "req-1713333333333",
  "serverTime": "2026-04-17T15:20:30.000",
  "data": [
    {
      "id": "rvl-1",
      "analysisId": "a-100",
      "ruleVersion": "v8",
      "errorType": "MISS",
      "changeCategory": "REVIEW_FEEDBACK_SAVED",
      "changeSummary": "REVIEW_FEEDBACK_SAVED;analysisId=a-100;ruleVersion=v8;errorType=MISS",
      "changeDetail": "errorType=MISS,actualOutcome=...",
      "operator": "SYSTEM",
      "rollbackFlag": "N",
      "createdAt": "2026-04-17T15:00:00",
      "fallbackMatched": false
    }
  ]
}
```

## 3. Step 2 验收清单（可打勾执行）

### 3.1 接口与契约

- [ ] `GET /api/review/rule-version-logs` 可独立访问，不依赖聚合接口
- [ ] 参数集合完整覆盖：`analysisId/ruleVersion/operator/rollbackFlag/errorType/changeCategory/keyword/createdAtFrom/createdAtTo/limit`
- [ ] 返回 `ApiResponse` 壳 + `RuleVersionLogSummary` 字段齐全

### 3.2 查询能力

- [ ] 单维过滤可用（任一参数可独立生效）
- [ ] 多维组合过滤可用（至少验证 3 个参数组合）
- [ ] `keyword` 可命中 `changeSummary` 与 `changeDetail`
- [ ] 时间窗口过滤（`createdAtFrom`/`createdAtTo`）可生效

### 3.3 输入收敛与安全边界

- [ ] 字符串参数 `trim` 生效
- [ ] 空白字符串按 `null` 处理，不污染查询条件
- [ ] `limit<=0` 回落默认 `20`
- [ ] `limit>50` 被安全截断到 `50`

### 3.4 旧数据兼容链

- [ ] 结构化字段缺失时可从 `changeSummary` 回填 `analysisId/errorType/changeCategory`
- [ ] 兼容命中场景 `fallbackMatched=true`
- [ ] 旧数据可查且不会影响新数据查询

### 3.5 自动化验证

- [ ] 已有服务层测试覆盖参数收敛与过滤传递
- [ ] 已有服务层测试覆盖 fallback 回填行为
- [ ] 回归执行通过（建议最小命令：`mvn test -Dtest=RuleVersionLogQueryServiceImplTest`）

## 4. 非本轮目标（保持边界）

以下事项暂不作为 Step 2 第一轮通过条件：

- 复杂分页（pageNo/pageSize/total）
- 独立检索页面
- Dashboard 接入
- 深度索引与性能压测优化
- fallback 降级策略重构
- 对外文档站点化发布
