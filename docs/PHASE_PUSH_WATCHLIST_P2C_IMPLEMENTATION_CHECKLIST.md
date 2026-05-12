# Push Watchlist P2C Implementation Checklist

## 一、P2C implementation 总原则

- P2C 是受控配置 UI，不是交易功能。
- 只允许修改 dashboard.html，除非 checklist 后续明确需要最小测试文件。
- 不新增后端 API。
- 不改 schema。
- 不改 RuleConfigService。
- 不改 PushSnapshotService / PushRecheckServiceImpl。
- 不恢复项目外源码。
- 不自动下单 / 开仓 / 平仓 / 反手。
- 不接 order API。
- 不默认六币。
- 空列表 / disabled 继续 fail-closed。

## 二、P2C 最小实现目标

- 在 watchlistStatusPanel 附近增加“编辑观察列表”入口。
- 编辑区默认折叠。
- 展开后显示：
  - symbols 输入框。
  - enabled 开关。
  - operator 输入。
  - reason 输入。
  - 保存配置按钮。
  - 取消按钮。
- 保存前必须显示确认弹窗。
- 确认弹窗展示 before / after：
  - 原观察列表。
  - 新观察列表。
  - 原启用状态。
  - 新启用状态。
  - 操作人。
  - 变更原因。
- 保存成功后重新加载 watchlist 和 audit。
- 保存失败显示错误，不更新主展示。

## 三、允许修改文件

建议只允许：

- src/main/resources/templates/dashboard.html

可选测试：

- src/test/java/org/example/trademodel/controller/DashboardControllerTest.java

可选测试只允许补静态文案 / guardrail，不做后端测试。

明确不允许：

- RuleController.java
- RuleConfigService.java
- RuleConfigServiceImpl.java
- RuleConfigMapper.java
- schema.sql
- application.yml
- PushSnapshotService.java
- PushRecheckServiceImpl.java
- RuleEngine / Opportunity / TradeReview
- 新页面 / 新模板

## 四、dashboard 修改位置 checklist

- 复用现有 watchlistStatusPanel。
- 编辑入口放在当前只读 watchlist 状态区域下方。
- 默认折叠，避免干扰首页。
- 不新增大卡片。
- 不重排 layer1 / tilesRow / homeWorkbench / 持仓监控 / AI 裁决。
- 不移动既有模块。
- 不改变 P2A / P2B 的只读展示。

## 五、前端数据处理 checklist

- symbols 输入支持逗号 / 空格 / 换行分隔。
- 前端 trim / uppercase / 去空。
- 最终 normalize / dedupe 以后端返回为准。
- operator 为空禁止提交。
- reason 为空禁止提交。
- empty symbols 允许提交，但必须二次确认提示“空列表将关闭推送候选”。
- enabled=false 允许提交，但必须提示 fail-closed。
- 保存成功后必须重新 GET /api/rule/push-watchlist。
- 保存成功后必须重新 GET /api/rule/push-watchlist/audit?limit=5。
- 保存失败不更新本地展示。

## 六、POST 调用 checklist

- 仅在用户点击保存并通过二次确认后调用 POST。
- POST endpoint：/api/rule/push-watchlist。
- body 字段：
  - symbols。
  - enabled。
  - operator。
  - reason。
- 不允许自动触发 POST。
- 不允许页面加载时 POST。
- 不允许编辑框变化时 POST。
- 不允许失败后自动重试写入。

## 七、UI 文案 checklist

- 编辑观察列表。
- 保存配置。
- 取消。
- 操作人。
- 配置变更原因。
- 保存前确认。
- 原观察列表。
- 新观察列表。
- 原启用状态。
- 新启用状态。
- 空列表将关闭推送候选。
- 禁用后不会生成观察推送。
- 仅人工配置，不是交易指令。
- 不自动下单。
- 保存成功，配置已更新。
- 保存失败，配置未更新。

## 八、测试 checklist

实现后至少验证：

- compile。
- test-compile。
- DashboardControllerTest。
- 静态 grep：
  - 编辑观察列表。
  - 保存配置。
  - 操作人。
  - 配置变更原因。
  - 保存前确认。
  - 空列表将关闭推送候选。
  - POST /api/rule/push-watchlist。
  - 不自动下单。
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
  - dashboard 无 template error。

## 九、P2C 禁止内容

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

## 十、P2C 风险 checklist

- 写入 UI 被误解为交易按钮。
- 空列表被误解为默认全量推送。
- operator / reason 缺失导致 audit 不可追踪。
- POST 失败后本地 UI 和后端状态不一致。
- dashboard.html 继续膨胀。
- 误引入 Push workflow / RuleEngine / 自动交易语义。
- 用户误以为 watchlist 是交易建议。

## 十一、建议 commit 顺序

1. 提交本 checklist。
2. P2C dashboard 写入 UI 最小实现。
3. P2C DashboardControllerTest / 静态 guardrail。
4. P2C smoke 验证。
5. P2C verification 文档。

也可以在实现很小的情况下，将 dashboard 实现与最小测试放在同一个 commit，但必须先复核。

## 十二、下一步建议

- 提交本 checklist 后，再进入 P2C dashboard 写入 UI 最小实现方案。
- 不直接开始大改 dashboard.html。
- 不恢复项目外大轨道源码。
