# Push Watchlist P2C Plan

## 一、P2C 背景

- P0 已完成非 watchlist fail-closed gate。
- P1 已完成 watchlist config / audit / API。
- P2A 已完成 dashboard 当前 watchlist 状态只读展示。
- P2B 已完成 dashboard 最近 audit 历史只读展示。
- 当前 P2C 的目标是让用户在 dashboard 上人工维护 watchlist，而不是产生交易动作。

## 二、P2C 是否值得做

P2C 值得做，但必须作为受控配置 UI，而不是交易功能。

- 价值：减少 curl/API 手工配置，提升 watchlist 配置可用性，并补齐人工配置到 audit 回显的闭环。
- 风险：写入 UI 容易被误解为交易按钮，也容易被误解为“放开推送”。
- 不需要新增后端。
- 不需要新增 schema。
- 不需要恢复外部源码。
- 不改变 Push 判定逻辑。
- 必须先方案文档，再 checklist，再实现。

## 三、P2C 推荐最小目标

推荐最小目标：在现有 `watchlistStatusPanel` 附近增加默认折叠的“编辑观察列表”入口。

- 复用现有 `POST /api/rule/push-watchlist`。
- 只改 dashboard.html。
- 可选补 DashboardControllerTest 静态 guardrail。
- 不新增 controller / service / mapper。
- 不改 schema / config。
- 不改 PushSnapshotService / PushRecheckServiceImpl。
- 不改变 Push 判定逻辑。

## 四、P2C 交互方案

- “编辑观察列表”默认不展开。
- 表单字段包含：
  - symbols 输入。
  - enabled 开关。
  - operator 必填。
  - reason 必填。
- 保存前必须二次确认。
- 确认弹窗展示：
  - 原观察列表。
  - 新观察列表。
  - 原启用状态。
  - 新启用状态。
  - 操作人。
  - 变更原因。
- 空列表明确提示：空列表将关闭推送候选。
- 保存成功后：
  - 重新 GET watchlist。
  - 重新 GET audit。
  - 显示成功提示。
- 保存失败后：
  - 显示错误。
  - 不更新页面本地状态。

## 五、P2C 安全边界

- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 不默认六币。
- 空列表 fail-closed。
- disabled fail-closed。
- 非 watchlist 不推。
- governance_missed 不推。
- HIGH_RISK / CONFUSED / INVALIDATED / COOLING 不直接变机会推送。
- 踩踏状态禁止机会推送。
- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。

## 六、P2C UI 文案建议

- 编辑观察列表。
- 保存配置。
- 取消。
- 配置变更原因。
- 操作人。
- 保存前确认。
- 原观察列表。
- 新观察列表。
- 原启用状态。
- 新启用状态。
- 空列表将关闭推送候选。
- 仅人工配置，不是交易指令。
- 不自动下单。
- 保存成功，配置已更新。
- 保存失败，配置未更新。
- POST 仅更新观察列表配置，不触发交易动作。

## 七、P2C 数据处理规则

- symbols 输入可用逗号、空格、换行分隔。
- 前端可做基本 trim / uppercase / 去空。
- 最终 normalize / dedupe 仍以后端 RuleConfigService 为准。
- operator / reason 为空时前端禁止提交。
- enabled 可为 true / false。
- symbols 空数组允许提交，但必须明确提示 fail-closed。
- 保存成功后以 GET 返回结果为准，不以本地表单值为准。

## 八、P2C 测试策略

实现后至少验证：

- compile。
- test-compile。
- DashboardControllerTest。
- 静态 grep：
  - POST /api/rule/push-watchlist。
  - operator。
  - reason。
  - 保存前确认。
  - 空列表将关闭推送候选。
  - 不自动下单。
  - 配置变更原因。
- forbidden grep：
  - order API。
  - apiKey。
  - secret。
  - 自动开仓。
  - 自动平仓。
  - 自动反手。
- smoke：
  - /dashboard 200。
  - GET /api/rule/push-watchlist 200。
  - POST /api/rule/push-watchlist 200。
  - GET /api/rule/push-watchlist/audit?limit=5 200。
  - 保存后 audit 回显。
  - 页面无 template error。

## 九、P2C 风险

- 写入 UI 被误解为交易按钮。
- 空列表被误解为默认全量推送。
- operator / reason 缺失导致 audit 不可追踪。
- POST 失败后本地 UI 和后端状态不一致。
- dashboard.html 继续膨胀。
- 误引入 Push workflow / RuleEngine / 自动交易语义。
- 写入 UI 可能让用户误以为 watchlist 是交易建议。

## 十、P2C 禁止内容

- 不新增后端 API。
- 不改 schema。
- 不改 RuleConfigService。
- 不改 PushSnapshotService。
- 不改 PushRecheckServiceImpl。
- 不做 latest-price recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / PlanBoundary。
- 不做 Opportunity / TradeReview。
- 不做自动交易。
- 不接交易所 order API。
- 不恢复项目外大轨道源码。

## 十一、P2C 执行顺序

1. 提交本 P2C 方案文档。
2. 创建 P2C implementation checklist。
3. P2C 最小 UI 实现。
4. P2C regression + smoke。
5. P2C verification 文档。
6. 再评估是否进入 latest-price recheck / asset-state / stampede guard 方案。

## 十二、下一步建议

- 先提交本方案文档。
- 不直接实现。
- 不恢复项目外大轨道源码。
- 不直接改 dashboard.html。
