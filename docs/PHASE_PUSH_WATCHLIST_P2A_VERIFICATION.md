# Push Watchlist P2A Verification

## 一、验证对象

- `b9bc4c2 docs(push): define watchlist P2 display plan`
- `de5e582 docs(push): add watchlist P2A implementation checklist`
- `971cdd6 feat(dashboard): show push watchlist status`

## 二、P2A 已完成能力

- Dashboard 只读展示 Push Watchlist 状态。
- 新增 `watchlistStatusPanel`。
- 展示“重点观察推送范围”。
- 调用 `GET /api/rule/push-watchlist`。
- 调用 `GET /api/rule/push-watchlist/audit?limit=1`。
- 展示状态、观察资产、数量、配置来源、`ruleValue`、最近变更。
- 展示边界文案：仅人工查看提醒、非交易指令、不自动下单、非 watchlist 不进入推送候选。
- 覆盖读取失败、空列表、禁用态文案。

## 三、测试结果

- `./mvnw -q -DskipTests compile`: PASS
- `./mvnw -q -DskipTests test-compile`: PASS
- `./mvnw -q -Dtest=DashboardControllerTest test`: PASS

## 四、静态 grep 结果

已确认存在：

- `watchlistStatusPanel`
- `重点观察推送范围`
- `非交易指令`
- `不自动下单`
- `非 watchlist 不进入推送候选`
- `push.watchlist.symbols`
- `GET /api/rule/push-watchlist`
- `GET /api/rule/push-watchlist/audit?limit=1`

已确认未发现：

- `POST /api/rule/push-watchlist`
- `order API`
- `apiKey` / `secret`
- 自动下单 / 自动开仓 / 自动平仓 / 自动反手行为

说明：

- 静态 grep 如命中既有“不会自动平仓 / 不自动反手”等否定文案，不属于自动交易行为。

## 五、API smoke 结果

- `/dashboard`: HTTP 200
- `/api/rule/push-watchlist`: HTTP 200
- `/api/rule/push-watchlist/audit?limit=1`: HTTP 200
- `/api/dashboard/summary`: HTTP 200
- BTC detail: HTTP 200
- ETH detail: HTTP 200
- SOL detail: HTTP 200

## 六、watchlist API 样本

本次 smoke 环境返回：

- `ruleKey = push.watchlist.symbols`
- `enabled = false`
- `symbols = []`
- `audit = []`

说明：

- 本次环境下 watchlist 为空且 disabled，页面应展示禁用 / 空列表 fail-closed 文案。
- 这符合 P2A 只读展示和 P0/P1 fail-closed 边界。

## 七、dashboard HTML 面板确认

已确认 `/dashboard` HTML 中可见：

- `watchlistStatusPanel`
- `重点观察推送范围`
- `非交易指令`
- `不自动下单`
- `非 watchlist 不进入推送候选`

## 八、detail 主链路确认

BTC / ETH / SOL detail 均存在：

- `latestPrice`
- `priceChangePct`
- `dataQualityScore`
- `readModelTruthStatus`
- `sourceType`

本次 smoke 返回：

- `readModelTruthStatus = FULL`
- `sourceType = OKX_24H_FALLBACK`

## 九、边界确认

- P2A 不包含 POST 修改 UI。
- P2A 不包含 schema。
- P2A 不包含后端 API 变更。
- P2A 不包含 Push workflow 变更。
- P2A 不包含 RuleEngine / PlanBoundary。
- P2A 不包含 Opportunity / TradeReview / RuleImprovement。
- P2A 不自动下单。
- P2A 不自动开仓。
- P2A 不自动平仓。
- P2A 不自动反手。
- P2A 不接 order API。

## 十、工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十一、当前结论

Push Watchlist P2A dashboard read-only display implemented and smoke verified。

## 十二、后续建议

- 暂停继续开发。
- 后续如进入 P2B，应先做 P2B 方案。
- 不要直接做写入 UI / latest-price recheck / asset-state gate / stampede guard。
- 不要恢复大轨道源码。
