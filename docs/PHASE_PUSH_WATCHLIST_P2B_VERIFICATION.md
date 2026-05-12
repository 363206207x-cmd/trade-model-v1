# Push Watchlist P2B Verification

## 一、验证对象

- `da673c4 docs(push): define watchlist P2B audit display plan`
- `2c1ddb6 docs(push): add watchlist P2B implementation checklist`
- `68396b1 feat(dashboard): show watchlist audit history`

## 二、P2B 已完成能力

- dashboard `watchlistStatusPanel` 展示最近 watchlist audit 历史。
- 调用 `GET /api/rule/push-watchlist/audit?limit=5`。
- 展示最近 3-5 条 audit。
- 展示空态：暂无变更记录。
- 展示失败态：最近变更读取失败，不影响当前观察范围。
- 展示字段：变更人、原观察、新观察、原因、时间。
- 字段缺失兜底：未知 / 未填写 / 空 / 未知时间。
- 只读展示，不调用 POST。
- 不做写入 UI。
- 不改后端 / schema / Push workflow。

## 三、测试结果

- `./mvnw -q -DskipTests compile`：PASS。
- `./mvnw -q -DskipTests test-compile`：PASS。
- `./mvnw -q -Dtest=DashboardControllerTest test`：PASS。

## 四、静态 grep 结果

确认存在：

- 最近变更
- 暂无变更记录
- 最近变更读取失败
- 变更人
- 原观察
- 新观察
- 原因
- 时间
- `/api/rule/push-watchlist/audit?limit=5`

确认未发现：

- watchlist POST
- method POST
- 编辑弹窗
- order API
- apiKey / secret

说明：

- “保存”命中的是既有手动监控 / 平仓记录按钮，不是 P2B 写入 UI。
- “自动平仓 / 自动反手”命中的是既有否定边界文案，不是自动交易行为。

## 五、API smoke 结果

- `/dashboard`：HTTP 200。
- `/api/rule/push-watchlist`：HTTP 200。
- `/api/rule/push-watchlist/audit?limit=5`：HTTP 200。
- `/api/dashboard/summary`：HTTP 200。
- BTC detail：HTTP 200。
- ETH detail：HTTP 200。
- SOL detail：HTTP 200。

## 六、watchlist API 样本

`/api/rule/push-watchlist` 返回：

- `enabled=false`
- `symbols=[]`
- `ruleKey=push.watchlist.symbols`

`/api/rule/push-watchlist/audit?limit=5` 返回：

- `data=[]`

说明：

- 当前环境下无 audit 记录时 dashboard 应展示“暂无变更记录”。
- 这符合 P2B 空态展示预期。

## 七、dashboard HTML 面板确认

`/dashboard` HTML 中可见：

- `watchlistStatusPanel`
- 重点观察推送范围
- 最近变更
- 暂无变更记录
- 变更人
- 原观察
- 新观察
- 非交易指令
- 不自动下单
- 非 watchlist 不进入推送候选

## 八、detail 主链路确认

BTC / ETH / SOL detail 均存在：

- `latestPrice`
- `priceChangePct`
- `dataQualityScore`
- `readModelTruthStatus=FULL`
- `sourceType=OKX_24H_FALLBACK`

说明：

- SOL 额外出现 `TM_MONITOR_ALERT` 来源字段，属于其它 detail 数据段，不影响行情主链路确认。

## 九、边界确认

- P2B 不包含 POST。
- P2B 不包含写入 UI。
- P2B 不包含保存按钮。
- P2B 不包含编辑弹窗。
- P2B 不包含后端 API 变更。
- P2B 不包含 schema。
- P2B 不包含 Push workflow 变更。
- P2B 不包含 RuleEngine / Opportunity / TradeReview。
- P2B 不自动下单。
- P2B 不自动开仓。
- P2B 不自动平仓。
- P2B 不自动反手。
- P2B 不接 order API。

## 十、工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十一、当前结论

Push Watchlist P2B dashboard recent audit read-only display implemented and smoke verified。

## 十二、后续建议

- 暂停继续开发。
- 后续如进入 P2C，应先做方案。
- 不要直接做写入 UI。
- 不要做 latest-price recheck / asset-state gate / stampede guard。
- 不要恢复大轨道源码。
