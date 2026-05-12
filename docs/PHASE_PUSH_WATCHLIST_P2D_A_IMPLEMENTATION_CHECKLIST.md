# Push Watchlist P2D-A Implementation Checklist

## 一、P2D-A implementation 总原则

- P2D-A 只管理首页展示位 Display Slots。
- P2D-A 不管理 Watchlist Pool。
- 不调用 `POST /api/rule/push-watchlist`。
- 不改 schema。
- 不改后端 API。
- 不改 Push 判定逻辑。
- 不恢复项目外源码。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- Display Slots 不等于推送候选全集。
- Watchlist Pool 才是推送候选最大边界。

## 二、P2D-A 最小实现目标

- 默认展示 6 个资产。
- 用户可从当前 dashboard 搜索 / 输入资产加入 Display Slots。
- 用户可从 Display Slots 移除资产。
- 用户可恢复默认 6 个。
- 最多 6 个 Display Slots。
- 超过 6 个时提示替换或先移除。
- 使用 localStorage 保存 display slots。
- 不写入 Watchlist Pool。
- 不触发 push 配置更新。
- 不触发交易动作。

## 三、允许修改文件

建议只允许：

- `src/main/resources/templates/dashboard.html`

可选测试：

- `src/test/java/org/example/trademodel/controller/DashboardControllerTest.java`

测试文件只允许补静态文案 / guardrail，不做后端测试。

明确不允许：

- `RuleController.java`
- `RuleConfigService.java`
- `RuleConfigServiceImpl.java`
- `RuleConfigMapper.java`
- `schema.sql`
- `application.yml`
- `PushSnapshotService.java`
- `PushRecheckServiceImpl.java`
- RuleEngine / Opportunity / TradeReview
- 新页面 / 新模板

## 四、localStorage 设计 checklist

建议 key：

- `tradeModel.displaySlots.v1`

存储格式：

- JSON array of symbols

示例：

```json
["BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT","XRPUSDT","DOGEUSDT"]
```

规则：

- trim。
- uppercase。
- 去空。
- 去重。
- 最多 6 个。
- 空或解析失败时恢复默认。
- 不向后端提交。
- 不改变 Watchlist Pool。

## 五、默认展示位建议

默认 6 个：

- BTCUSDT
- ETHUSDT
- SOLUSDT
- BNBUSDT
- XRPUSDT
- DOGEUSDT

必须写明：

- 默认展示位不是交易建议。
- 默认展示位不是推送全集。
- 默认展示位不是 Watchlist Pool。

## 六、dashboard UI checklist

- 在重点资产区域已有“恢复默认 / 添加 / 管理”附近整合 Display Slots 管理。
- 或在 `watchlistStatusPanel` 中加入“首页展示位”说明。
- 文案必须区分：
  - 首页展示位。
  - 观察库 / Watchlist Pool。
- 不重排首页结构。
- 不移动持仓监控 / AI 裁决 / tilesRow。
- 不新增大卡片。

## 七、Display Slots 与 Watchlist Pool 关系提示

UI 中必须提示：

- 首页展示位只影响首页显示。
- 观察库决定推送候选。
- 非观察库资产即使显示在首页，也不进入推送候选。
- 可先加入观察库，再加入首页展示位。
- 后续低频扫描 / 机会提升另做。

## 八、测试 checklist

实现后至少验证：

- compile。
- test-compile。
- `DashboardControllerTest`。
- 静态 grep：
  - 首页展示位。
  - Display Slots。
  - Watchlist Pool。
  - 最多 6 个。
  - 恢复默认。
  - 不是推送全集。
  - 非观察库资产不进入推送候选。
  - `tradeModel.displaySlots.v1`。
- forbidden grep：
  - `POST /api/rule/push-watchlist`。
  - method POST。
  - order API。
  - apiKey。
  - secret。
  - 自动开仓。
  - 自动平仓。
  - 自动反手。
- smoke：
  - `/dashboard` 200。
  - Display Slots 文案可见。
  - Watchlist API 不受影响。
  - 不触发 `POST /api/rule/push-watchlist`。

## 九、P2D-A 禁止内容

- 不做后端 display slots API。
- 不做 display slots schema/table。
- 不改 Watchlist Pool。
- 不调用 watchlist POST。
- 不做低频扫描。
- 不做 Promote To Home。
- 不做 latest-price recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / PlanBoundary。
- 不做 Opportunity / TradeReview。
- 不做自动交易。
- 不接交易所 order API。
- 不恢复项目外大轨道源码。

## 十、风险 checklist

- 用户误以为首页展示资产都会推送。
- 用户误以为默认 6 个就是观察库。
- localStorage 无法跨设备。
- localStorage 可能被清空。
- 与 Watchlist Pool UI 容易混淆。
- dashboard.html 继续膨胀。
- 未来 Promote To Home 可能引起自动替换争议。

## 十一、建议 commit 顺序

1. 提交本 checklist。
2. P2D-A dashboard localStorage Display Slots 最小实现。
3. P2D-A DashboardControllerTest / 静态 guardrail。
4. P2D-A smoke 验证。
5. P2D-A verification 文档。

## 十二、下一步建议

- 提交本 checklist 后，再进入 P2D-A dashboard Display Slots 最小实现方案。
- 不直接开始大改 dashboard.html。
- 不恢复项目外大轨道源码。
