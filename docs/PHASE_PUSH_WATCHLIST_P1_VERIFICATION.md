# Push Watchlist P1 Verification

## 一、验证对象

- `6d83920 feat(schema): add push watchlist audit table`
- `2705e48 feat(push): add watchlist audit mapper`
- `d3731bc feat(push): add watchlist rule config service`
- `2172db5 feat(push): expose watchlist rule APIs`

## 二、P1 已完成能力

- 使用 `tm_rule_config` / `push.watchlist.symbols` 作为 watchlist 真值来源。
- 提供 `GET /api/rule/push-watchlist`。
- 提供 `POST /api/rule/push-watchlist`。
- 提供 `GET /api/rule/push-watchlist/audit`。
- 新增 `tm_push_watchlist_config_audit`。
- 支持 RuleConfig watchlist read/write。
- 支持 watchlist 配置变更 audit 写入。
- 配置更新后调用 `reloadRules`。
- 支持 symbol normalize / dedupe / validation。
- 延续 P0 非 watchlist fail-closed 行为。

## 三、测试结果

- `compile` PASS
- `test-compile` PASS
- `PushWatchlistConfigAuditMapperTest` PASS
- `RuleConfigServiceImplTest` PASS
- `RuleControllerWatchlistTest` PASS
- `WatchlistPushEligibilityServiceImplTest` PASS
- `PushSnapshotServiceTest` PASS
- `PushRecheckServiceImplTest` PASS
- `DashboardControllerTest` PASS
- `DecisionServiceImplTest` PASS
- `ManualPositionControllerTest` PASS

## 四、API smoke 结果

- `/api/dashboard/summary` 200
- BTC detail 200
- ETH detail 200
- SOL detail 200
- `/api/system/position-sync-status` 200, `freshnessStatus=FRESH`
- `/api/position-monitor/open` 200
- `/api/rule/push-watchlist` 200
- `/api/rule/push-watchlist/audit?limit=5` 200

## 五、watchlist 写入 smoke

- `POST /api/rule/push-watchlist` 成功。
- POST body:
  - `symbols = BTCUSDT, ETHUSDT`
  - `enabled = true`
  - `operator = smoke-test`
  - `reason = P1 local smoke verification`
- POST 后 GET 可读。
- audit 中有 `smoke-test`。
- audit 中有 `P1 local smoke verification`。
- audit `afterSymbols = BTCUSDT,ETHUSDT`。
- audit `ruleVersion = p1-watchlist`。

## 六、dashboard/detail 字段路径确认

真实字段路径：

- `decision.latestPrice`
- `decision.priceChangePct`
- `decision.dataQualityScore`
- `decision.hasOpenPosition`
- `decision.positionSide`
- `decision.avgOpenPrice`
- `decision.readModelTruthStatus`
- `decision.readModelFallbackReason`
- `marketEnvironmentMini.sourceType`
- `scoreEightItems`
- `reviewSummary`
- `planReadiness`
- `assetEventTimeline`

BTC:

- `decision.latestPrice = 80978.3`
- `decision.priceChangePct = 0.250942`
- `decision.dataQualityScore = 85`
- `decision.readModelTruthStatus = FULL`
- `decision.readModelFallbackReason = null`
- `decision.hasOpenPosition = true`
- `decision.positionSide = LONG`
- `decision.avgOpenPrice = 63520.5`
- `marketEnvironmentMini.sourceType = OKX_24H_FALLBACK`
- `scoreEightItems = list len 8`
- `planReadiness = object`
- `reviewSummary = object`
- `assetEventTimeline = list len 1`

ETH:

- `decision.latestPrice = 2299.83`
- `decision.priceChangePct = -1.345236`
- `decision.dataQualityScore = 85`
- `decision.readModelTruthStatus = FULL`
- `decision.readModelFallbackReason = null`
- `decision.hasOpenPosition = true`
- `decision.positionSide = SHORT`
- `decision.avgOpenPrice = 3120.8`
- `marketEnvironmentMini.sourceType = OKX_24H_FALLBACK`
- `scoreEightItems = list len 8`
- `planReadiness = object`
- `reviewSummary = object`
- `assetEventTimeline = list len 1`

SOL:

- `decision.latestPrice = 96.03`
- `decision.priceChangePct = 0.692042`
- `decision.dataQualityScore = 85`
- `decision.readModelTruthStatus = FULL`
- `decision.readModelFallbackReason = null`
- `decision.hasOpenPosition = false`
- `decision.positionSide = null`
- `decision.avgOpenPrice = null`
- `marketEnvironmentMini.sourceType = OKX_24H_FALLBACK`
- `scoreEightItems = list len 8`
- `planReadiness = object`
- `reviewSummary = object`
- `assetEventTimeline = list len 0`

## 七、字段缺失结论

- `latestPrice` / `priceChangePct` / `sourceType` / `dataQualityScore` / `readModelTruthStatus` 均存在。
- `MISSING_KEYS = []`。
- 字段不在顶层，而在 `decision` / `marketEnvironmentMini` 路径下。

## 八、边界确认

- P1 不包含 UI。
- P1 不包含 latest-price recheck。
- P1 不包含 asset-state gate。
- P1 不包含 stampede guard。
- P1 不包含 RuleEngine / PlanBoundary。
- P1 不包含 Opportunity / TradeReview / RuleImprovement。
- P1 不自动下单。
- P1 不自动开仓。
- P1 不自动平仓。
- P1 不自动反手。
- P1 不接 order API。

## 九、工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十、当前结论

Push Watchlist P1 implemented and verified。

## 十一、后续建议

- 暂停继续开发。
- 后续如进入 P2，应先做 P2 方案。
- 不要一次性恢复大轨道源码。
