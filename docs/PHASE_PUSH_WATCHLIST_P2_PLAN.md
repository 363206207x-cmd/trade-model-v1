# Push Watchlist P2 Plan

## 一、P2 背景

Push Watchlist P0 已完成 read-only watchlist gate，推送候选必须先通过只读观察名单边界，非 watchlist 默认 fail-closed。

Push Watchlist P1 已完成 watchlist 配置查询、更新、audit 记录与规则 reload 能力。P1 regression + smoke 已通过，包括 compile / test-compile、watchlist POST 后 GET 可读、audit 写入 reason、dashboard summary/detail 正常，以及确认无自动交易 / order API 行为。

因此，当前阶段不需要继续补 schema 或 API 基础能力。P2 的重点应从“可用”转向“可见 / 可观测”：让用户能在 dashboard 或首页明确看到当前 Push Watchlist 配置状态，并理解它只是人工查看提醒边界，不是交易指令。

## 二、P2 候选方向评估

| 方向 | 价值 | 风险 | 是否需要 schema | 是否需要恢复外部源码 | 是否适合下一步 |
| --- | --- | --- | --- | --- | --- |
| 1. watchlist UI / dashboard 入口 | 直接把 P0/P1 能力暴露为可见状态，降低误解和运维成本 | dashboard.html 已多次修正，需避免大重排 | 否 | 否 | 是，推荐作为 P2A |
| 2. latest-price recheck | 可提升推送前价格新鲜度，减少过时行情造成的误判 | 会触及 Push/Recheck 业务逻辑，可能扩大回归面 | 可能不需要 | 可能不需要 | 否，暂缓 |
| 3. asset-state gate | 可把资产状态纳入推送边界，增强治理一致性 | 容易影响候选生成和治理分类，需要重新梳理边界 | 可能需要 | 可能不需要 | 否，暂缓 |
| 4. stampede guard / 踩踏保护 | 可强化踩踏状态下禁止机会推送的安全边界 | 触及机会推送判定，回归成本较高 | 可能不需要 | 可能不需要 | 否，暂缓 |
| 5. ops overview 增强 | 提升运维观测能力，便于统一查看 Push / rule / audit 状态 | 容易变成大范围 dashboard 改造 | 否或少量 | 可能需要 | 否，等 P2A 后再评估 |
| 6. push 配置状态展示 | 可直接展示 enabled、symbols、ruleValue、最近 audit，快速闭合可观测性 | 需要控制为只读，避免误变成配置入口 | 否 | 否 | 是，适合作为 P2A 核心 |
| 7. P1 API 回归文档 / 验收 checklist | 固化 P1 验收路径，降低后续回归成本 | 只产生文档价值，不直接提升产品可见性 | 否 | 否 | 可作为 P2A 前置或伴随文档 |

## 三、P2 推荐目标

推荐目标为 P2A：只读 watchlist 配置状态展示 / dashboard 入口。

推荐原因：

- 直接复用 P1 的 `GET /api/rule/push-watchlist`。
- 可选复用 `GET /api/rule/push-watchlist/audit`。
- 不改 schema。
- 不改后端 API。
- 不恢复项目外源码。
- 不改变 Push 判定逻辑。
- 不引入自动交易风险。
- 能让用户在首页看到当前 watchlist 配置状态。

## 四、P2A 范围

P2A 包含：

- 首页或 dashboard 中只读展示当前 watchlist 状态。
- 显示 `enabled`。
- 显示 `symbols`。
- 显示 symbol count。
- 显示 `ruleValue`。
- 可选显示最近 audit 一条或最近 audit 时间。
- 明确显示“只读 / 人工配置 / 非交易指令”边界文案。
- 调用现有 `GET /api/rule/push-watchlist`。
- 如展示 audit，调用现有 `GET /api/rule/push-watchlist/audit?limit=1`。

P2A 不包含：

- POST 修改 UI。
- 表单编辑。
- 批量导入 symbol。
- audit 列表页。
- Push/Recheck 逻辑变更。
- latest-price recheck。
- asset-state gate。
- stampede guard。
- RuleEngine / Opportunity / TradeReview。
- schema 变更。
- 自动交易。

## 五、P2A 最小闭合包

建议最小文件：

- `dashboard.html` 或当前 dashboard template。
- 如已有 JS 内联，则只在现有 JS 中加 fetch / render。
- 不新增 controller。
- 不新增 service。
- 不新增 mapper。
- 不新增 schema。

最小 UI 建议：

- 放在重点观察资产 / Push 状态附近。
- 标题：重点观察推送范围。
- 展示：
  - 状态：启用 / 禁用。
  - 观察资产：BTCUSDT, ETHUSDT。
  - 数量：2。
  - 来源：`push.watchlist.symbols`。
  - 最近变更：如 audit 可用。
  - 提示：仅人工查看提醒，不是交易信号，不自动下单。

## 六、P2A 测试策略

至少覆盖：

- `DashboardControllerTest` 或 dashboard template guardrail test。
- 静态 grep 确认存在 watchlist UI 文案。
- 启动 smoke：
  - `/dashboard` 返回 200。
  - `/api/rule/push-watchlist` 返回 200。
  - `/api/rule/push-watchlist/audit?limit=1` 返回 200。
  - dashboard 页面无 template error。
- 不需要改 Push P0/P1 service tests，除非 UI 引起接口变动。

## 七、P2A 风险

- `dashboard.html` 已多次修正，不能大重排。
- 只读展示不能变成配置编辑。
- 不要把 watchlist 当成默认六币。
- 不要把 `governance_missed` 当推送候选。
- 不要引入 Push/Recheck 业务逻辑。
- 不要误导为交易信号。
- 不要出现自动下单 / 自动开仓 / 自动平仓 / 自动反手文案。

## 八、暂缓方向

以下方向在 P2A 之前暂缓：

- latest-price recheck。
- asset-state gate。
- stampede guard。
- RuleEngine / PlanBoundary。
- Opportunity / TradeReview。
- RuleImprovement。
- watchlist POST UI。
- audit 管理页。
- ops overview 深度增强。

## 九、风险动作分层提醒

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。
- P2 不得产生自动交易动作。
- Push 只是人工查看提醒，不是交易信号。

## 十、P2 执行顺序

1. 提交本 P2 方案文档。
2. 创建 P2A implementation checklist。
3. P2A 最小 UI 只读实现。
4. P2A regression + smoke。
5. 再评估 P2B 是否做 POST 修改 UI 或 ops overview。

## 十一、下一步建议

先提交本方案文档，不直接实现 P2A，不恢复项目外大轨道源码。提交后再进入 P2A implementation checklist，继续保持最小闭合和只读展示边界。
