# Push Watchlist P0-P2B Closure

## 一、阶段范围

本收口覆盖 Push Watchlist 从 P0 到 P2B 的完整闭环：

- P0：watchlist fail-closed gate。
- P1：watchlist 配置维护 / audit / API。
- P2A：dashboard 当前 watchlist 状态只读展示。
- P2B：dashboard 最近 audit 历史只读展示。

## 二、关键 commits

- `0dc15d0 docs(push): define watchlist P0 boundaries`
- `fdbcb16 feat(push): gate push candidates by watchlist`
- `3ea31b9 docs(push): record watchlist P0 verification`
- `9a6a59f docs(push): define watchlist P1 config audit plan`
- `dc73acb docs(push): add watchlist P1 implementation checklist`
- `6d83920 feat(schema): add push watchlist audit table`
- `2705e48 feat(push): add watchlist audit mapper`
- `d3731bc feat(push): add watchlist rule config service`
- `2172db5 feat(push): expose watchlist rule APIs`
- `0a80403 docs(push): record watchlist P1 verification`
- `b9bc4c2 docs(push): define watchlist P2 display plan`
- `de5e582 docs(push): add watchlist P2A implementation checklist`
- `971cdd6 feat(dashboard): show push watchlist status`
- `d4fa5fc docs(push): record watchlist P2A verification`
- `da673c4 docs(push): define watchlist P2B audit display plan`
- `2c1ddb6 docs(push): add watchlist P2B implementation checklist`
- `68396b1 feat(dashboard): show watchlist audit history`
- `298584f docs(push): record watchlist P2B verification`

## 三、P0 已完成能力

- 读取 `tm_rule_config` / `push.watchlist.symbols`。
- 非 watchlist fail-closed。
- missing / disabled / empty / exception fail-closed。
- `PushSnapshotService` 写入前 gate。
- `PushRecheckServiceImpl` recheck 前 gate。
- 不改 schema。
- 不接自动交易。

## 四、P1 已完成能力

- 新增 `tm_push_watchlist_config_audit`。
- 新增 `PushWatchlistConfigAuditVO`。
- 新增 `PushWatchlistConfigAuditMapper`。
- `RuleConfig` watchlist read/write。
- audit 写入。
- `reloadRules`。
- `GET /api/rule/push-watchlist`。
- `POST /api/rule/push-watchlist`。
- `GET /api/rule/push-watchlist/audit`。
- P1 regression + smoke 已通过。

## 五、P2A 已完成能力

- dashboard `watchlistStatusPanel`。
- 展示当前 watchlist 状态。
- 调用 `GET /api/rule/push-watchlist`。
- 调用 `GET /api/rule/push-watchlist/audit?limit=1`。
- 展示状态 / 观察资产 / 数量 / 配置来源 / `ruleValue` / 最近变更。
- 展示非交易指令 / 不自动下单 / 非 watchlist 不进入推送候选。
- P2A smoke 已通过。

## 六、P2B 已完成能力

- dashboard 最近 watchlist audit 历史只读展示。
- 调用 `GET /api/rule/push-watchlist/audit?limit=5`。
- 展示最近 3-5 条 audit。
- 展示 `changedBy` / `changeReason` / `beforeSymbols` / `afterSymbols` / `beforeEnabled` / `afterEnabled` / `createTime`。
- 空态：暂无变更记录。
- 失败态：最近变更读取失败，不影响当前观察范围。
- 字段兜底：未知 / 未填写 / 空 / 未知时间。
- P2B smoke 已通过。

## 七、验证结果汇总

- compile PASS。
- test-compile PASS。
- `PushWatchlistConfigAuditMapperTest` PASS。
- `RuleConfigServiceImplTest` PASS。
- `RuleControllerWatchlistTest` PASS。
- `WatchlistPushEligibilityServiceImplTest` PASS。
- `PushSnapshotServiceTest` PASS。
- `PushRecheckServiceImplTest` PASS。
- `DashboardControllerTest` PASS。
- Dashboard/API smoke PASS。

## 八、当前运行边界

- Push 只是人工查看提醒。
- 非交易指令。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 非 watchlist 不推。
- `governance_missed` 不推。
- HIGH_RISK / CONFUSED / INVALIDATED / COOLING 不直接变成机会推送。
- 踩踏状态禁止机会推送。
- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。

## 九、明确未做 / 暂缓项

- watchlist 写入 UI 暂缓。
- latest-price recheck 暂缓。
- asset-state gate 暂缓。
- stampede guard / 踩踏保护暂缓。
- RuleEngine / PlanBoundary 暂缓。
- Opportunity / TradeReview / ReviewCenter 暂缓。
- RuleImprovement 暂缓。
- 自动交易暂缓。
- 项目外大轨道源码继续隔离。

## 十、当前工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。
- 大轨道源码仍在项目外 workspace。
- 当前 HEAD 为 `298584f`。

## 十一、下一阶段建议

- 先暂停继续开发。
- 如继续，必须先做方案文档。
- 可选下一阶段：
  1. P2C watchlist 写入 UI 方案。
  2. Push Recheck latest-price 方案。
  3. asset-state gate 方案。
  4. stampede guard 方案。
- 不建议直接实现。
- 不建议一次性恢复大轨道源码。
