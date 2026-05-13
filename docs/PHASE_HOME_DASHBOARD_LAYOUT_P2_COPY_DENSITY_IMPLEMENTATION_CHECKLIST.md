# Home Dashboard Layout P2 Copy Density Implementation Checklist

## 一、Layout P2 implementation 总原则

- Layout P2 只做文案密度压缩，不做业务逻辑变更。
- 不改后端 API。
- 不改 schema。
- 不改 Push 判定逻辑。
- 不改 Watchlist Pool 逻辑。
- 不改 Display Slots localStorage 逻辑。
- 不删除已验证能力。
- 不接自动交易。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做 low-frequency scan / Promote To Home。
- 不重排全首页。

## 二、允许修改文件

建议只允许：

- `src/main/resources/templates/dashboard.html`

可选测试：

- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

测试文件只允许补静态文案 / template guardrail。

明确不允许：

- Java service / controller / mapper
- `schema.sql`
- `application.yml`
- `PushSnapshotService` / `PushRecheckServiceImpl`
- `RuleEngine` / `Opportunity` / `TradeReview`
- 新页面 / 新模板

## 三、文案压缩目标

- 主视图短文案化。
- 长说明移入 `details` / help。
- 重复出现的“不代表推送候选 / 非观察库资产不进入推送候选 / 非交易指令 / 不自动下单”减少重复。
- 保留关键安全边界。
- 不让用户误解 Display Slots = Watchlist Pool。
- 不让用户误解 Watchlist = 交易信号。

## 四、术语统一 checklist

统一为：

- 首页展示位（Display Slots）
- 观察库（Watchlist Pool）
- 推送候选
- 非交易指令
- 低频扫描
- 机会提升
- 默认 6 个首页展示位

要求：

- 中文为主，英文放括号中。
- 不再使用“默认六个币”。
- 不混用“自定义监控 / 首页展示 / 关注币”等造成混乱的旧词，除非保留兼容说明。

## 五、主视图保留短文案 checklist

建议主视图只保留：

- 首页展示位（最多 6 个）
- 只影响首页展示
- 观察库决定推送候选
- 非交易指令
- 不自动下单

## 六、折叠帮助文案 checklist

长说明放入折叠区域，例如：

- 首页展示位只控制首页显示，不代表推送候选。
- 观察库是推送候选边界，非观察库资产不会进入推送候选。
- 低频扫描和机会提升属于后续阶段。
- Display Slots / Watchlist Pool 关系说明。

## 七、Watchlist 文案压缩 checklist

- Watchlist Pool 状态区保留短文案。
- 写入 UI 内只保留 operator / reason / 保存确认必要说明。
- “不自动下单 / 非交易指令”只保留必要位置。
- audit 区域不重复解释 Watchlist Pool 概念。
- 成功 / 失败提示保持明确但短。

## 八、Display Slots 文案压缩 checklist

- 资产监控区保留“首页展示位（最多 6 个）”。
- Display Slots 管理折叠区保留“只影响首页展示”。
- 长说明放到折叠 help。
- 避免每个按钮附近重复写“不是推送全集”。
- “观察库决定推送候选”保留一个短提示。

## 九、禁止内容

- 不改后端 API。
- 不改 schema。
- 不改 Push 判定逻辑。
- 不改 Watchlist Pool。
- 不改 Display Slots localStorage。
- 不做低频扫描。
- 不做机会提升。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。
- 不接 order API。
- 不删除已验证功能。

## 十、测试 checklist

实现后至少验证：

- compile
- test-compile
- DashboardControllerTest
- `/dashboard` HTTP 200
- `/api/dashboard/summary` HTTP 200
- `/api/rule/push-watchlist` HTTP 200

页面仍可见：

- 首页展示位
- Display Slots
- Watchlist Pool
- 编辑观察列表
- 保存配置
- 最近变更
- 非交易指令
- 不自动下单

并确认：

- 页面无 template error。

## 十一、forbidden grep checklist

每次 staging 前必须确认没有：

- `schema.sql`
- `application.yml`
- `RuleController`
- `RuleConfigService`
- `PushSnapshotService`
- `PushRecheckServiceImpl`
- `RuleEngine`
- `Opportunity`
- `TradeReview`
- `order API`
- `apiKey`
- `secret`
- 自动开仓
- 自动平仓
- 自动反手
- low-frequency scan 已实现
- Promote To Home 已实现
- 默认六个币

以上关键词若仅出现在本 checklist 的禁止项中，不代表风险实现；实现 diff 中不得新增这些业务能力或旧口径。

## 十二、风险 checklist

- 文案压缩过度导致用户分不清 Display Slots / Watchlist Pool。
- 删除过多提示导致误解为交易信号。
- 保留过多提示导致页面继续拥挤。
- 术语不统一导致理解混乱。
- 文案调整误删已验证功能。

## 十三、建议 commit 顺序

建议：

1. 提交本 checklist。
2. Layout P2 最小文案压缩实现。
3. Layout P2 smoke 验证。
4. Layout P2 verification 文档。
5. 再评估 Layout P3 或回到 RuleEngine / PlanBoundary。

## 十四、下一步建议

- 提交本 checklist 后，再进入 Layout P2 最小文案压缩实现方案。
- 不直接大改 `dashboard.html`。
- 不恢复项目外大轨道源码。
