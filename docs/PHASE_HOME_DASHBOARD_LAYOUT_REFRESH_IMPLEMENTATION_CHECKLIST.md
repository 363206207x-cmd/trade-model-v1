# Phase Home Dashboard Layout Refresh Implementation Checklist

## 一、实施总原则

- 本阶段只做 dashboard 布局整理，不做业务逻辑变更。
- 不改后端 API。
- 不改 schema。
- 不改 Push 判定逻辑。
- 不改 Watchlist Pool 逻辑。
- 不改 Display Slots 逻辑。
- 不删除已验证能力。
- 不接自动交易。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做 low-frequency scan / Promote To Home。
- 每次实现必须小 commit，避免一次性大改。

## 二、允许修改文件

建议只允许：

- `src/main/resources/templates/dashboard.html`

可选测试：

- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

测试文件只允许补静态文案 / template guardrail，不做后端行为测试。

明确不允许：

- Java service / controller / mapper
- `schema.sql`
- `application.yml`
- `PushSnapshotService` / `PushRecheckServiceImpl`
- `RuleEngine` / `Opportunity` / `TradeReview`
- 新页面 / 新模板

## 三、最小布局目标

第一轮实现只做：

- 配置 / 管理类内容折叠。
- Watchlist Pool 写入 UI 默认折叠。
- Watchlist audit 历史可折叠或小列表。
- Display Slots 管理不抢主视图。
- 保持 Display Slots 资产卡在重点资产监控区。
- 保持实时告警 + 关键事件在第二层。
- 保持持仓监控 + 执行建议并排。
- 保持 AI 三方裁决摘要可见但不拥挤。

第一轮不做大面积 DOM 重排，不改变现有数据加载顺序，不删除任何已验证模块。

## 四、优先折叠模块 checklist

建议优先折叠：

- Watchlist 写入 UI。
- Watchlist audit 历史完整列表。
- Display Slots 管理细节。
- legacy detail panels。
- debug / status 细节。
- 规则配置说明。
- 长说明文案。

折叠目标是降低首页常驻信息密度，不是隐藏风险边界。关键边界文案仍需以短句保留。

## 五、主视图保留模块 checklist

必须保留可见：

- 全局状态摘要。
- 实时告警。
- 关键事件。
- Display Slots 资产卡。
- 已开仓监控。
- 执行建议。
- AI 三方裁决摘要。
- 必要的 Watchlist / Display Slots 边界短文案。

主视图优先服务状态判断、监控扫描和人工复核，不承载配置表单的完整展开态。

## 六、建议 DOM 调整顺序

建议按以下顺序小步执行：

1. 先只增加 `details` / `summary` 折叠，不移动主要模块。
2. 再收缩 Watchlist 配置区域。
3. 再整理 Display Slots 控制区文案。
4. 再整理持仓监控 / 执行建议并排。
5. 最后整理 AI 三方裁决区。

每一步单独复核，不要一次完成全部。若某一步引入视觉或模板风险，应停止并先完成验证。

## 七、CSS / 视觉密度 checklist

建议：

- 减少长说明常驻显示。
- 将说明文案移入 help / `details`。
- 保留小标签而不是大段文字。
- 卡片间距统一。
- 同类按钮归组。
- Watchlist / Display Slots 用不同小标题。
- 配置项默认折叠。
- 主决策信息优先显示。

CSS 调整应集中在 dashboard 现有样式范围内，避免引入新的全局视觉体系。

## 八、Watchlist / Display Slots 布局边界

- Display Slots 在资产监控区。
- Watchlist Pool 在配置折叠区。
- Watchlist 写入 UI 默认折叠。
- audit 历史默认折叠或最多显示少量。
- 首页展示位不代表推送候选。
- Watchlist Pool 才决定推送候选。
- 非 Watchlist Pool 资产不进入推送候选。

布局整理必须继续维持 P2C / P2D-A 已固定的语义边界：Display Slots 是首页展示配置，Watchlist Pool 是推送候选最大边界。

## 九、安全边界

- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 不把 Watchlist 当交易信号。
- 不把 Display Slots 当交易信号。
- 高风险不等于自动平仓 / 反手。
- 踩踏状态禁止机会推送。
- 插针不等于趋势反转。

布局调整只能改变信息层级和默认展开方式，不得改变风险动作分层。

## 十、测试 checklist

实现后至少验证：

- `compile`
- `test-compile`
- `DashboardControllerTest`
- `/dashboard` HTTP 200
- `/api/dashboard/summary` HTTP 200
- BTC / ETH / SOL detail HTTP 200
- `/api/rule/push-watchlist` HTTP 200

页面仍可见：

- 首页展示位
- Display Slots
- Watchlist Pool
- 实时告警
- 关键事件
- 已开仓监控
- 执行建议
- AI 三方裁决

并确认：

- 不出现 template error。
- dashboard summary/detail 主链路不受影响。
- Watchlist API 不受影响。
- Display Slots localStorage 逻辑不受影响。

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

以上关键词若仅出现在本 checklist 的禁止项中，不代表风险实现；实现 diff 中不得新增这些业务能力。

## 十二、建议 commit 顺序

建议：

1. 提交本 checklist。
2. Layout P1：折叠配置 / 管理区。
3. Layout P2：压缩 Watchlist / Display Slots 文案密度。
4. Layout P3：整理持仓监控 / 执行建议 / AI 裁决。
5. Layout smoke。
6. Layout verification 文档。

每个实现 commit 都应保持小范围、可复核、可回滚。

## 十三、下一步建议

- 提交本 checklist 后，再进入 Layout P1 最小实现方案。
- 不直接大改 `dashboard.html`。
- 不恢复项目外大轨道源码。
