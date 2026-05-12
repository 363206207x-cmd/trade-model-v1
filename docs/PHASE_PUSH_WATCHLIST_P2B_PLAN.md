# Push Watchlist P2B Plan

## 一、P2B 背景

- P0 已完成 read-only watchlist gate，非 watchlist fail-closed。
- P1 已完成 watchlist 配置查询、更新、audit API。
- P2A 已完成 dashboard 只读展示 watchlist 状态。
- 当前 P2B 不再补基础 API，而是补“最近变更可见性”。
- P2B 首选继续只读，不进入写入 UI。

## 二、P2B 候选方向复盘

### 1. watchlist 写入 UI

- 价值高：能让用户在 dashboard 完成人工配置闭环。
- 风险高：需要 `operator` / `reason` / 确认弹窗 / 防误操作。
- 容易被误解为交易动作或默认放开推送。
- 暂缓，不作为 P2B 最小目标。

### 2. audit 列表页 / dashboard 最近 audit 只读增强

- 价值中高：能让用户看到 watchlist 最近变更，增强可追踪性。
- 风险低：只读展示，不改变配置。
- 复用 `GET /api/rule/push-watchlist/audit`。
- 不需要后端变更。
- 不需要 schema 变更。
- 推荐作为 P2B。

### 3. ops overview 增强

- 价值中：能统一展示 Push / watchlist / audit 状态。
- 风险中：容易扩大到 Push/Recheck 运维链路。
- 暂不作为 P2B 最小目标。

## 三、P2B 推荐目标

推荐目标：

P2B：dashboard 最近 watchlist audit 只读增强。

推荐原因：

- 直接复用 P1 audit API。
- 不调用 POST。
- 不改 schema。
- 不改后端。
- 不改 Push 判定逻辑。
- 不恢复外部源码。
- 能让用户看到 watchlist 最近变更，增强可追踪性。
- 与 P2A 只读展示方向一致。

## 四、P2B 范围

P2B 包含：

- 在现有 `watchlistStatusPanel` 内或附近展示最近 3-5 条 audit。
- 调用 `GET /api/rule/push-watchlist/audit?limit=5`。
- 展示 `changedBy`。
- 展示 `changeReason`。
- 展示 `beforeSymbols` / `afterSymbols`。
- 展示 `beforeEnabled` / `afterEnabled`。
- 展示 `createTime`。
- 空态：暂无变更记录。
- 失败态：最近变更读取失败，不影响 watchlist 主状态。
- 明确只读 / 非交易指令 / 不自动下单边界。

P2B 不包含：

- POST 修改 UI。
- 编辑表单。
- 保存按钮。
- 批量导入 symbol。
- 单独 audit 页面。
- 后端 API 变更。
- schema 变更。
- Push/Recheck 逻辑变更。
- latest-price recheck。
- asset-state gate。
- stampede guard。
- RuleEngine / Opportunity / TradeReview。
- 自动交易。

## 五、P2B 最小闭合包

建议最小文件：

- `src/main/resources/templates/dashboard.html`

可选测试：

- `DashboardControllerTest` 或 dashboard template guardrail test。

不需要：

- Java controller。
- Java service。
- mapper。
- schema。
- `application.yml`。
- 新页面 / templates。

## 六、P2B UI 建议

建议展示区域：

- 复用 P2A 的 `watchlistStatusPanel`。
- 在“最近变更”下方新增最近变更列表。
- 不新增大卡片。
- 不重排首页结构。
- 不移动 `tilesRow` / `homeWorkbench` / 持仓监控 / AI 裁决区域。

建议文案：

- 最近变更。
- 暂无变更记录。
- 最近变更读取失败，不影响当前观察范围。
- 变更人：smoke-test。
- 原观察：BTCUSDT。
- 新观察：BTCUSDT, ETHUSDT。
- 状态：禁用 -> 启用。
- 原因：P1 local smoke verification。
- 时间：`createTime`。

## 七、P2B 前端数据获取 checklist

- P2A 已调用 `audit?limit=1`。
- P2B 可调整为 `audit?limit=5`。
- audit fetch 失败不影响主 watchlist 展示。
- audit 空列表显示“暂无变更记录”。
- audit 列表只渲染文本，不提供操作按钮。
- 不调用 POST。
- 不引入写入状态。

## 八、P2B 测试策略

实现后至少验证：

- compile。
- test-compile。
- `DashboardControllerTest`。
- 静态 grep：
  - 最近变更。
  - 暂无变更记录。
  - 最近变更读取失败。
  - `beforeSymbols` / `afterSymbols` 或中文对应文案。
  - `/api/rule/push-watchlist/audit?limit=5`。
- forbidden grep：
  - `POST /api/rule/push-watchlist`。
  - method POST。
  - 自动下单。
  - 自动开仓。
  - 自动平仓。
  - 自动反手。
  - order API。
- smoke：
  - `/dashboard` 200。
  - `/api/rule/push-watchlist` 200。
  - `/api/rule/push-watchlist/audit?limit=5` 200。
  - dashboard 无 template error。

## 九、P2B 风险

- `dashboard.html` 不能大重排。
- audit 列表不能变成编辑入口。
- audit 空态不能误导为推送关闭，主状态仍来自 `GET /push-watchlist`。
- 不要把 audit 记录当成推送候选。
- 不要把 `governance_missed` 当成推送候选。
- 不要引入 Push/Recheck 业务逻辑。
- 不要误导为交易信号。
- 不要出现自动交易文案。

## 十、暂缓方向

- watchlist 写入 UI。
- latest-price recheck。
- asset-state gate。
- stampede guard。
- ops overview 深度增强。
- RuleEngine / PlanBoundary。
- Opportunity / TradeReview。
- RuleImprovement。
- 自动交易。

## 十一、风险动作分层提醒

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- P2B 不得产生自动交易动作。
- Push 只是人工查看提醒，不是交易信号。

## 十二、P2B 执行顺序

1. 提交本 P2B 方案文档。
2. 创建 P2B implementation checklist。
3. P2B dashboard 最近 audit 只读展示最小实现。
4. P2B regression + smoke。
5. 再评估是否进入 P2C 写入 UI 方案。

## 十三、下一步建议

- 先提交本方案文档。
- 不直接实现。
- 不恢复项目外大轨道源码。
- 不直接做写入 UI。
