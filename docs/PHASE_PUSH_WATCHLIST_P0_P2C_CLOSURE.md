# Push Watchlist P0-P2C Closure

## 一、阶段范围

本收口覆盖 Push Watchlist 从 P0 到 P2C 的完整闭环：

- P0：watchlist fail-closed gate。
- P1：watchlist 配置维护 / audit / API。
- P2A：dashboard 当前 watchlist 状态只读展示。
- P2B：dashboard 最近 audit 历史只读展示。
- P2C：dashboard 受控 Watchlist Pool 写入 UI。

## 二、关键 commits

- `0dc15d0 docs(push): define watchlist P0 boundaries`
- `fdbcb16 feat(push): gate push candidates by watchlist`
- `3ea31b9 docs(push): record watchlist P0 verification`
- `6d83920 feat(schema): add push watchlist audit table`
- `2705e48 feat(push): add watchlist audit mapper`
- `d3731bc feat(push): add watchlist rule config service`
- `2172db5 feat(push): expose watchlist rule APIs`
- `0a80403 docs(push): record watchlist P1 verification`
- `b9bc4c2 docs(push): define watchlist P2 display plan`
- `971cdd6 feat(dashboard): show push watchlist status`
- `d4fa5fc docs(push): record watchlist P2A verification`
- `da673c4 docs(push): define watchlist P2B audit display plan`
- `68396b1 feat(dashboard): show watchlist audit history`
- `298584f docs(push): record watchlist P2B verification`
- `1667386 docs(push): define watchlist P2C write UI plan`
- `179a2f4 docs(push): add watchlist P2C implementation checklist`
- `e7ffd4e docs(push): clarify watchlist display pool boundary`
- `6a8f74a feat(dashboard): add watchlist write controls`
- `8a2d8db docs(push): record watchlist P2C verification`

## 三、P0 已完成能力

- 读取 `tm_rule_config` / `push.watchlist.symbols`。
- 非 watchlist fail-closed。
- missing / disabled / empty / exception 均 fail-closed。
- `PushSnapshotService` 写入前 gate。
- `PushRecheckServiceImpl` recheck 前 gate。
- 不改 schema。
- 不接自动交易。

## 四、P1 已完成能力

- 新增 `tm_push_watchlist_config_audit`。
- 新增 `PushWatchlistConfigAuditVO`。
- 新增 `PushWatchlistConfigAuditMapper`。
- RuleConfig watchlist read/write。
- audit 写入。
- `reloadRules`。
- `GET /api/rule/push-watchlist`。
- `POST /api/rule/push-watchlist`。
- `GET /api/rule/push-watchlist/audit`。
- P1 regression + smoke 已通过。

## 五、P2A 已完成能力

- dashboard `watchlistStatusPanel`。
- 当前 watchlist 状态只读展示。
- 调用 `GET /api/rule/push-watchlist`。
- 调用 `GET /api/rule/push-watchlist/audit?limit=1`。
- 展示状态 / 观察资产 / 数量 / 配置来源 / ruleValue / 最近变更。
- 展示非交易指令 / 不自动下单 / 非 watchlist 不进入推送候选。
- P2A smoke 已通过。

## 六、P2B 已完成能力

- dashboard 最近 watchlist audit 历史只读展示。
- 调用 `GET /api/rule/push-watchlist/audit?limit=5`。
- 展示最近 3-5 条 audit。
- 展示 changedBy / changeReason / beforeSymbols / afterSymbols / beforeEnabled / afterEnabled / createTime。
- 空态：暂无变更记录。
- 失败态：最近变更读取失败，不影响当前观察范围。
- 字段兜底：未知 / 未填写 / 空 / 未知时间。
- P2B smoke 已通过。

## 七、P2C 已完成能力

- dashboard 受控 Watchlist Pool 写入 UI。
- 编辑观察列表。
- symbols / enabled / operator / reason。
- operator / reason 必填。
- `window.confirm` 保存前二次确认。
- `POST /api/rule/push-watchlist` 只在用户保存并确认后触发。
- 保存成功后重新 GET watchlist 和 audit。
- 保存失败不更新本地展示。
- 空列表 / disabled 继续 fail-closed。
- P2C smoke 已通过。

## 八、Display Slots / Watchlist Pool 新边界

- 首页默认展示 6 个资产是 Display Slots / 首页展示位。
- Display Slots 不是推送全集。
- Display Slots 不是唯一观察库。
- Watchlist Pool / 观察库才是推送候选最大边界。
- 观察库可以多于 6 个。
- 不在观察库的资产不推。
- 不在首页但在观察库的资产，后续可低频扫描。
- 观察库资产出现机会后，后续可 Promote To Home。
- P2C 编辑的是 Watchlist Pool，不编辑 Display Slots。
- P2C 不做低频扫描。
- P2C 不做机会提升。
- P2C 不做首页展示位管理。

## 九、验证结果汇总

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
- P2C POST smoke PASS。
- audit 回显 PASS。
- BTC / ETH / SOL detail 主链路 PASS。

## 十、当前运行边界

- Push 只是人工查看提醒。
- 非交易指令。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 非 watchlist 不推。
- governance_missed 不推。
- HIGH_RISK / CONFUSED / INVALIDATED / COOLING 不直接变成机会推送。
- 踩踏状态禁止机会推送。
- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。

## 十一、明确未做 / 暂缓项

- P2D 首页展示位管理暂缓。
- P3A Watchlist Pool 低频扫描暂缓。
- P3B Promote To Home 机会提升暂缓。
- latest-price recheck 暂缓。
- asset-state gate 暂缓。
- stampede guard / 踩踏保护暂缓。
- RuleEngine / PlanBoundary 暂缓。
- Opportunity / TradeReview / ReviewCenter 暂缓。
- RuleImprovement 暂缓。
- 自动交易暂缓。
- 项目外大轨道源码继续隔离。

## 十二、当前工作区状态

- tracked clean。
- staged 为空。
- src 下无 untracked。
- docs untracked 仍保留。
- 大轨道源码仍在项目外 workspace。
- 当前 HEAD 为 `8a2d8db`。

## 十三、下一阶段建议

- 先暂停继续开发。
- 如继续，必须先做方案文档。
- 可选下一阶段：
  1. P2D Display Slots 首页展示位管理方案。
  2. P3A Watchlist Pool 低频扫描方案。
  3. P3B Promote To Home 机会提升方案。
  4. Push Recheck latest-price 方案。
  5. asset-state gate 方案。
  6. stampede guard 方案。
- 不建议直接实现。
- 不建议一次性恢复大轨道源码。
