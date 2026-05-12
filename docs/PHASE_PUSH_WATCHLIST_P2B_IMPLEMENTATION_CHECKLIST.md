# Push Watchlist P2B Implementation Checklist

## 一、P2B implementation 总原则

- P2B 是只读 audit 展示增强。
- 不进入写入 UI。
- 不调用 POST /api/rule/push-watchlist。
- 不改 schema。
- 不改后端 API。
- 不改 Push 判定逻辑。
- 不恢复项目外源码。
- 不单独开新页面。
- 不自动下单 / 开仓 / 平仓 / 反手。
- 不改变 dashboard 大结构。
- 只在 P2A 的 watchlistStatusPanel 内或附近做最小增强。

## 二、P2B 最小实现目标

- 展示最近 3-5 条 watchlist audit。
- 显示 changedBy。
- 显示 changeReason。
- 显示 beforeSymbols / afterSymbols。
- 显示 beforeEnabled / afterEnabled。
- 显示 createTime。
- 空态显示“暂无变更记录”。
- 失败态显示“最近变更读取失败，不影响当前观察范围”。
- 列表只读，无按钮，无表单，无编辑入口。

## 三、允许修改文件

建议只允许：

- src/main/resources/templates/dashboard.html

可选测试：

- 如已有 dashboard template guardrail，可最小补充 DashboardControllerTest 静态断言。
- 若不改测试，则后续必须通过 smoke 验证。

明确不允许：

- Java controller
- Java service
- mapper
- schema.sql
- application.yml
- 新页面 / 新模板
- PushSnapshotService / PushRecheckServiceImpl
- RuleEngine / Opportunity / TradeReview

## 四、dashboard 修改位置 checklist

- 复用现有 watchlistStatusPanel。
- 在“最近变更”区域从单条扩展为 3-5 条列表。
- 不新增大卡片。
- 不重排 layer1 / tilesRow / homeWorkbench / 持仓监控 / AI 裁决。
- 不移动既有模块。
- 不改变 P2A 主状态展示。

## 五、前端数据获取 checklist

- 将 audit 请求从 limit=1 调整为 limit=5，或新增单独最近 audit 列表请求。
- 仍调用 GET /api/rule/push-watchlist/audit?limit=5。
- audit fetch 失败不影响主 watchlist 展示。
- audit 空列表显示“暂无变更记录”。
- audit 列表只渲染文本。
- 不调用 POST。
- 不引入保存状态。
- 不引入编辑状态。

## 六、UI 文案 checklist

建议文案：

- 最近变更
- 暂无变更记录
- 最近变更读取失败，不影响当前观察范围
- 变更人
- 原观察
- 新观察
- 状态
- 原状态 / 新状态
- 原因
- 时间
- 只读记录
- 非交易指令
- 不自动下单

## 七、数据展示格式建议

每条 audit 可展示为轻量行：

- changedBy + createTime
- beforeSymbols → afterSymbols
- beforeEnabled → afterEnabled
- changeReason

字段缺失处理：

- changedBy 为空显示“未知”。
- changeReason 为空显示“未填写”。
- beforeSymbols / afterSymbols 为空显示“空”。
- createTime 为空显示“未知时间”。

## 八、测试 checklist

实现后至少验证：

- compile
- test-compile
- DashboardControllerTest
- 静态 grep：
  - 最近变更
  - 暂无变更记录
  - 最近变更读取失败
  - 变更人
  - 原观察
  - 新观察
  - /api/rule/push-watchlist/audit?limit=5
- forbidden grep：
  - POST /api/rule/push-watchlist
  - method POST
  - 自动下单
  - 自动开仓
  - 自动平仓
  - 自动反手
  - order API
- smoke：
  - /dashboard 200
  - /api/rule/push-watchlist 200
  - /api/rule/push-watchlist/audit?limit=5 200
  - dashboard 无 template error
  - 页面 HTML 可见最近变更文案

## 九、forbidden grep checklist

每次 staging 前必须确认 diff 不包含：

- POST /api/rule/push-watchlist
- method: POST
- schema.sql
- application.yml
- RuleController
- RuleConfigService
- PushSnapshotService
- PushRecheckServiceImpl
- latestPrice recheck
- AssetStateMapper
- stampede
- RuleEngine
- Opportunity
- TradeReview
- 自动下单
- 自动开仓
- 自动平仓
- 自动反手
- order API
- apiKey
- secret

说明：apiKey / secret 仅作为 forbidden grep 关键词出现，不代表文档包含实际敏感信息。

## 十、P2B 不做内容

- 不做写入 UI。
- 不做保存按钮。
- 不做编辑弹窗。
- 不做批量导入。
- 不做单独 audit 页面。
- 不做后端 API。
- 不做 schema。
- 不做 Push/Recheck 逻辑变更。
- 不做 latest-price recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。

## 十一、建议 commit 顺序

1. 提交本 checklist。
2. P2B dashboard 最近 audit 只读展示最小实现。
3. P2B DashboardControllerTest / 静态 guardrail。
4. P2B smoke 验证。

也可以在实现很小的情况下，将 dashboard 实现与最小测试放在同一个 commit，但必须先复核。

## 十二、下一步建议

- 提交本 checklist 后，再进入 P2B dashboard 最近 audit 只读展示最小实现方案。
- 不直接开始大改 dashboard.html。
- 不恢复项目外大轨道源码。
