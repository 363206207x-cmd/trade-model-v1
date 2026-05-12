# Push Watchlist P2D-A Verification

## 一、验证对象

- b161d9f docs(push): define watchlist P2D display slots plan
- df95c60 docs(push): add watchlist P2D-A implementation checklist
- fc21067 feat(dashboard): manage display slots locally

## 二、P2D-A 已完成能力

- dashboard localStorage Display Slots 管理。
- localStorage key = `tradeModel.displaySlots.v1`。
- 默认 6 个资产：
  - BTCUSDT
  - ETHUSDT
  - SOLUSDT
  - BNBUSDT
  - XRPUSDT
  - DOGEUSDT
- 最多 6 个资产。
- 可添加 / 移除 / 恢复默认。
- symbols 处理包含 trim / uppercase / 去空 / 去重。
- 读取失败 / 非数组 / 空数组时恢复默认。
- Display Slots 只影响首页展示。
- Display Slots 不影响 Watchlist Pool。
- Display Slots 不影响 Push 判定逻辑。
- Display Slots 操作不触发 watchlist POST。

## 三、测试结果

- compile PASS。
- test-compile PASS。
- DashboardControllerTest PASS。

## 四、静态 grep 结果

确认存在：

- 首页展示位
- Display Slots
- 最多 6 个
- 恢复默认
- `tradeModel.displaySlots.v1`
- Watchlist Pool
- 非观察库资产不进入推送候选
- Display Slots JS 函数

记录说明：

- 全文件 grep 命中既有 P2C `method:"POST"` 路径，这是受控 Watchlist Pool 写入 UI，不是 P2D-A Display Slots 操作。
- 未发现 `order API` / `apiKey` / `secret` / 后端 display slots API / display slots schema / 低频扫描实现 / Promote To Home 实现。

## 五、API smoke 结果

- `/dashboard` HTTP 200。
- `/api/rule/push-watchlist` HTTP 200。
- `/api/rule/push-watchlist/audit?limit=5` HTTP 200。
- `/api/dashboard/summary` HTTP 200。
- BTC detail HTTP 200。
- ETH detail HTTP 200。
- SOL detail HTTP 200。

## 六、dashboard HTML 文案确认

`/dashboard` HTML 中可见：

- 首页展示位
- Display Slots
- 最多 6 个
- 恢复默认
- 当前展示只影响首页
- Watchlist Pool
- 非观察库资产不进入推送候选

## 七、边界确认

- P2D-A 只管理 Display Slots / 首页展示位。
- Display Slots 不是 Watchlist Pool。
- Display Slots 不是推送全集。
- Display Slots 不改变 Watchlist Pool。
- Display Slots 操作不触发 `POST /api/rule/push-watchlist`。
- P2D-A 不改后端 API。
- P2D-A 不改 schema。
- P2D-A 不改 Push 判定逻辑。
- P2D-A 不做 low-frequency scan。
- P2D-A 不做 Promote To Home。
- P2D-A 不自动下单。
- P2D-A 不自动开仓。
- P2D-A 不自动平仓。
- P2D-A 不自动反手。
- P2D-A 不接 order API。

## 八、工作区状态

- tracked clean。
- staged 为空。
- src 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 九、当前结论

Push Watchlist P2D-A dashboard localStorage Display Slots implemented and smoke verified。

## 十、后续建议

- 暂停继续开发。
- 后续如继续，应先做下一阶段方案。
- 可选下一阶段：
  - P2D-B 后端 Display Slots 持久化方案
  - P3A Watchlist Pool 低频扫描方案
  - P3B Promote To Home 机会提升方案
  - 首页布局整理方案
- 不要直接做 latest-price recheck / asset-state gate / stampede guard。
- 不要恢复大轨道源码。
