# Push Watchlist P2D Display Slots Plan

## 一、P2D 背景

- P0-P2C 已完成 watchlist 推送候选闭环。
- Display Slots / Watchlist Pool 边界已修正。
- 当前 dashboard 默认展示资产与 Watchlist Pool 需要进一步解耦。
- P2D 只做首页展示位管理方案，不做低频扫描和机会提升。

## 二、核心概念

### 1. Display Slots / 首页展示位

- 首页最多展示 6 个资产。
- 用于高频可视化。
- 可以有默认值。
- 不等于 Watchlist Pool。
- 不等于推送候选全集。

### 2. Watchlist Pool / 观察库

- 用户配置的推送候选边界。
- 可以多于 6 个。
- 不在观察库不推。
- 已由 P2C UI 管理。

### 3. Promoted Asset / 提升到首页资产

- 未来由低频扫描发现机会后提升。
- P2D 只预留概念，不实现。

## 三、P2D 推荐目标

推荐：P2D 只做 Display Slots 管理方案，不改 Push 判定逻辑。

最小目标：

- 定义首页 6 个展示位的数据来源。
- 定义默认展示位。
- 定义用户自定义展示位。
- 定义恢复默认。
- 定义展示位与 Watchlist Pool 的关系。
- 定义未来 Promote To Home 的预留边界。

## 四、P2D 不做内容

- 不改 Watchlist Pool。
- 不改 `POST /api/rule/push-watchlist`。
- 不改 `PushSnapshotService` / `PushRecheckServiceImpl`。
- 不做低频扫描。
- 不做 Promote To Home。
- 不做 latest-price recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。
- 不接 order API。

## 五、数据来源方案评估

### 方案 A：前端 localStorage 管理 Display Slots

- 优点：无需后端、无需 schema、实现快。
- 缺点：无法跨设备、无审计。
- 结论：适合 P2D 最小实现。

### 方案 B：复用 tm_rule_config 新增 display slots key

示例 key：`dashboard.display.slots`。

- 优点：可跨设备、可服务端管理。
- 缺点：需要后端 API / audit，范围变大。
- 结论：建议后置。

### 方案 C：新增独立 display slots 表

- 优点：结构清晰。
- 缺点：schema/API/UI 都要新增，超出 P2D 最小目标。
- 结论：暂缓。

## 六、推荐最小实现方向

推荐：P2D-A：前端 localStorage 管理首页 Display Slots。

理由：

- 不改 schema。
- 不改后端 API。
- 不影响 Watchlist Pool。
- 不影响 Push 判定逻辑。
- 与当前 dashboard 已有自定义监控 / localStorage 思路接近。
- 风险最低。

## 七、P2D-A 交互方案

- 首页默认展示 6 个资产。
- 用户可调整首页展示资产。
- 用户可恢复默认 6 个。
- 用户可从 Watchlist Pool 中选择资产加入首页展示位。
- 不在 Watchlist Pool 的资产如被手动加入首页展示，应只作为展示，不进入推送候选，或提示“未在观察库，不参与推送”。
- 首页展示位最多 6 个。
- 超过 6 个时提示替换。
- Display Slots 管理不触发 Push 配置写入。
- Display Slots 管理不触发交易行为。

## 八、默认展示位建议

默认展示可以是：

- BTCUSDT
- ETHUSDT
- SOLUSDT
- BNBUSDT
- XRPUSDT
- DOGEUSDT

默认展示位只是 UI 初始展示，不是推送全集，也不是交易建议。

## 九、和 Watchlist Pool 的关系

- Display Slots 可以是 Watchlist Pool 的子集。
- Watchlist Pool 可以大于 Display Slots。
- Display Slots 中资产不一定都在 Watchlist Pool。
- 只有 Watchlist Pool 内资产才允许进入推送候选。
- 非 Watchlist Pool 资产即使在首页展示，也不进入推送候选。
- P2D 不改变 P0/P1/P2C 的推送边界。

## 十、未来 Promote To Home 预留

- P3B 可让 Watchlist Pool 中未在首页展示的资产，在出现值得关注机会时提升到首页。
- P2D 只定义展示位机制。
- P2D 不做机会识别。
- P2D 不做自动替换首页资产。
- P2D 不做低频扫描。

## 十一、安全边界

- Display Slots 只是 UI 展示配置。
- 不是交易信号。
- 不是推送候选全集。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- governance_missed 不推。
- 踩踏状态禁止机会推送。
- 高风险不等于自动平仓 / 反手。
- 插针不等于趋势反转。

## 十二、测试策略

如果后续实现 P2D-A，至少验证：

- compile。
- test-compile。
- `DashboardControllerTest`。
- dashboard HTML 静态 grep：
  - Display Slots。
  - 首页展示位。
  - 恢复默认。
  - 最多 6 个。
  - 不是推送全集。
  - Watchlist Pool。
- smoke：
  - `/dashboard` 200。
  - Watchlist Pool API 不受影响。
  - Display Slots 操作不触发 `POST /api/rule/push-watchlist`。
  - 不出现自动交易文案。

## 十三、风险

- 用户可能误以为首页展示资产都会推送。
- 用户可能误以为默认 6 个就是观察库。
- localStorage 无法跨设备。
- 和 Watchlist Pool UI 容易混淆。
- 未来 Promote To Home 可能引起自动替换争议。
- dashboard.html 继续膨胀。

## 十四、执行顺序建议

1. 提交本 P2D 方案文档。
2. 创建 P2D-A implementation checklist。
3. P2D-A localStorage Display Slots 最小实现。
4. P2D-A smoke 验证。
5. P2D-A verification 文档。

## 十五、下一步建议

- 先提交本方案文档。
- 不直接实现。
- 不恢复项目外大轨道源码。
- 不进入低频扫描 / Promote To Home。
