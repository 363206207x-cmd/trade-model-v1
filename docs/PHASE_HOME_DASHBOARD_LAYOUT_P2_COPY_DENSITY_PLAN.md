# Home Dashboard Layout P2 Copy Density Plan

## 一、背景

- Layout P1 已完成配置 / 管理区折叠，Watchlist 写入 UI、Watchlist audit 完整列表和 Display Slots 管理细节已经从主视图中收拢。
- 当前首页仍存在 Watchlist / Display Slots 文案密度高、重复说明多的问题。
- Layout P2 只定义文案密度压缩方案，不做业务逻辑变更。
- 本阶段不实现代码、不改模板、不改 API。

## 二、当前文案问题盘点

- Display Slots 与 Watchlist Pool 边界说明在主视图、管理折叠区、Watchlist 编辑区和弹窗确认文案中多处重复。
- “不代表推送候选 / 非观察库资产不进入推送候选 / 非交易指令 / 不自动下单”重复出现，容易抢占主视图注意力。
- Watchlist 写入 UI、Display Slots 管理、audit 区域存在较多说明文本，虽然已经折叠，但展开后信息密度仍偏高。
- 主视图需要保留短提示，长说明应进入折叠 help 或详情说明。
- 中文 / 英文术语混排较多，例如 Display Slots、Watchlist Pool、观察库、首页展示位需要统一展示方式。

## 三、Layout P2 目标

- 主视图只保留短文案。
- 长说明进入折叠帮助。
- 同一边界只在一个主位置完整解释，其它位置使用短标签或短句提醒。
- Watchlist / Display Slots 的概念保持清楚，但不抢占主视图。
- 不删除已验证能力。
- 不改变 DOM 大结构。
- 不改业务逻辑。

## 四、推荐文案分层

### 1. 主视图短标签

建议主视图只保留高频扫描所需的短标签：

- 首页展示位
- 最多 6 个
- 只影响首页展示
- 观察库决定推送候选
- 非交易指令

### 2. 折叠区简短说明

建议配置 / 管理折叠区保留一到三句短说明：

- Display Slots 只控制首页显示，不代表推送候选。
- Watchlist Pool 是推送候选边界。
- 非观察库资产不会进入推送候选。

### 3. 帮助 / 详情长说明

建议把低频阅读的说明放入 help 或详情说明：

- 首页默认展示 6 个资产只是 UI 展示位，不是推送全集，也不是交易建议。
- 低频扫描和机会提升属于后续阶段。

## 五、Watchlist 文案压缩建议

- Watchlist Pool 状态区保留短文案，优先展示当前状态、数量、启用状态和最近变更摘要。
- 写入 UI 内只保留 operator / reason / 保存确认所需的必要说明。
- “不自动下单 / 非交易指令”保留一处即可，避免在状态区、编辑区和确认弹窗中重复堆叠。
- audit 区域不重复解释 Watchlist Pool 概念，只表达“最近变更记录”以及空态 / 失败态。
- 成功 / 失败提示保持明确但短，例如“保存成功，配置已更新”“保存失败，配置未更新”。

## 六、Display Slots 文案压缩建议

- 资产监控区保留“首页展示位 / 最多 6 个”。
- Display Slots 管理折叠区保留“只影响首页展示”。
- 长说明放到折叠 help。
- 避免每个按钮附近重复写“不是推送全集”。
- “Watchlist Pool 决定推送候选”保留一个短提示即可。

## 七、术语统一建议

建议统一以下术语：

- 首页展示位（Display Slots）
- 观察库（Watchlist Pool）
- 推送候选
- 非交易指令
- 低频扫描
- 机会提升

要求：

- 中文为主，英文放括号中。
- 同一个概念不要在不同位置使用多套叫法。
- “默认六个币”改为“默认 6 个首页展示位”。

## 八、Layout P2 不做内容

- 不改后端 API。
- 不改 schema。
- 不改 Push 判定逻辑。
- 不改 Watchlist Pool 逻辑。
- 不改 Display Slots localStorage 逻辑。
- 不做低频扫描。
- 不做 Promote To Home。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。
- 不接 order API。

## 九、测试策略

实现后至少验证：

- compile。
- test-compile。
- DashboardControllerTest。
- `/dashboard` HTTP 200。
- 页面仍可见：
  - 首页展示位
  - Display Slots
  - Watchlist Pool
  - 编辑观察列表
  - 保存配置
  - 最近变更
  - 非交易指令
  - 不自动下单
- 页面无 template error。
- Watchlist API 不受影响。

## 十、风险

- 文案压缩过度可能导致用户分不清 Display Slots 和 Watchlist Pool。
- 删除提示可能误导用户以为首页展示就是推送候选。
- 保留过多提示则页面继续拥挤。
- 中文 / 英文术语不统一会造成理解混乱。
- 不应把文案压缩变成业务逻辑改动。

## 十一、执行顺序建议

建议：

1. 提交本 Layout P2 方案文档。
2. 创建 Layout P2 implementation checklist。
3. Layout P2 最小文案压缩实现。
4. Layout P2 smoke 验证。
5. Layout P2 verification 文档。

## 十二、下一步建议

- 先提交本方案文档。
- 不直接改 `dashboard.html`。
- 不恢复大轨道源码。
- 不进入 RuleEngine / P3A / P3B。
