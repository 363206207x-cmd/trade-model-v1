# Push Watchlist P2C Verification

## 一、验证对象

- `1667386 docs(push): define watchlist P2C write UI plan`
- `179a2f4 docs(push): add watchlist P2C implementation checklist`
- `e7ffd4e docs(push): clarify watchlist display pool boundary`
- `6a8f74a feat(dashboard): add watchlist write controls`

## 二、P2C 已完成能力

- dashboard 已增加受控 Watchlist Pool 写入 UI。
- 提供“编辑观察列表”入口。
- 支持 symbols 输入。
- 支持 enabled 开关。
- operator 必填。
- reason 必填。
- 保存前使用 `window.confirm` 二次确认。
- 仅用户点击保存并确认后才调用 POST。
- POST endpoint 为 `/api/rule/push-watchlist`。
- 保存成功后重新 reload / `loadWatchlistStatus`。
- 保存失败不更新本地展示。
- 空列表 / disabled 继续按 fail-closed 处理。
- 已加入 Display Slots / Watchlist Pool 边界文案。
- “不默认六币”旧口径已移除。

## 三、测试结果

- compile PASS。
- test-compile PASS。
- `DashboardControllerTest` PASS。

## 四、静态 grep 结果

已确认存在：

- 编辑观察列表
- 保存配置
- 操作人
- 配置变更原因
- 保存前确认
- 当前编辑的是观察库
- Watchlist Pool
- Display Slots
- 首页默认展示位不等于推送全集
- `POST /api/rule/push-watchlist`
- 不自动下单
- 非交易指令

已确认未发现：

- 不默认六币
- 没有默认六币
- 不能默认六币
- order API
- apiKey
- secret

说明：

- 自动平仓 / 自动反手如有命中，仅为既有否定边界文案，不是自动交易行为。

## 五、API smoke 结果

- `/dashboard` HTTP 200。
- `/api/rule/push-watchlist` HTTP 200。
- `/api/rule/push-watchlist/audit?limit=5` HTTP 200。
- `/api/dashboard/summary` HTTP 200。
- BTC detail HTTP 200。
- ETH detail HTTP 200。
- SOL detail HTTP 200。

## 六、POST smoke 结果

- `POST /api/rule/push-watchlist` HTTP 200。
- POST 后 GET 可读。
- audit after POST 可读。
- symbols = BTCUSDT / ETHUSDT。
- enabled = true。
- ruleValue = `BTCUSDT,ETHUSDT`。
- audit changedBy = `p2c-smoke`。
- audit changeReason = `P2C controlled write UI smoke verification`。
- audit ruleVersion = `p1-watchlist`。
- reload 后 GET 可读。

## 七、dashboard HTML 文案确认

`/dashboard` HTML 中可见：

- 编辑观察列表
- 保存配置
- 操作人
- 配置变更原因
- 保存前确认
- 当前编辑的是观察库
- Watchlist Pool
- Display Slots
- 空列表将关闭推送候选
- 非交易指令
- 不自动下单

## 八、detail 主链路确认

BTC / ETH / SOL detail 均存在：

- latestPrice
- priceChangePct
- dataQualityScore
- readModelTruthStatus
- sourceType

确认：

- readModelTruthStatus = FULL。
- 主行情 sourceType = OKX_24H_FALLBACK。

说明：

- ETH 额外出现 TM_MONITOR_ALERT 来源段，属于其它 detail 数据段，不影响行情主链路确认。

## 九、边界确认

- P2C 是受控配置 UI，不是交易功能。
- P2C 编辑 Watchlist Pool，不编辑首页 Display Slots。
- 首页默认展示位不等于推送全集。
- “不默认六币”旧口径已移除。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 不改 schema。
- 不改后端 API。
- 不改 Push 判定逻辑。
- 不接 RuleEngine / Opportunity / TradeReview。
- 不做 low-frequency scan。
- 不做 promote-to-home。

## 十、工作区状态

- tracked clean。
- staged 为空。
- src 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十一、当前结论

Push Watchlist P2C dashboard controlled write UI implemented and smoke verified。

## 十二、后续建议

- 暂停继续开发。
- 如继续，应先做下一阶段方案。
- 后续候选：
  - P2D Display Slots 管理方案。
  - P3A Watchlist Pool 低频扫描方案。
  - P3B Promote To Home 机会提升方案。
- 不要直接做 latest-price recheck / asset-state gate / stampede guard。
- 不要恢复大轨道源码。
