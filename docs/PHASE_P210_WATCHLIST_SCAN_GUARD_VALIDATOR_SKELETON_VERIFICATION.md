# P210 Watchlist Scan Guard Validator Skeleton Verification

## 1. 阶段定位

P210 只实现 guard / validator / tests（保护器 / 校验器 / 测试）骨架。

P210 新增 `WatchlistScanGuardValidator` 和 `DefaultWatchlistScanGuardValidator`，只消费 P208 `WatchlistRuntimeSnapshotDTO`，只返回 P208 `WatchlistScanResultDTO`。

P210 不是 runtime scan（运行时扫描）。

## 2. 本轮已验证边界

- 无 runtime read（运行时读取）。
- 无 live / external data read（实时 / 外部数据读取）。
- 无 MarketQuoteClient（行情客户端）。
- 无 mapper / controller / scheduler / dashboard wiring（映射器 / 控制器 / 定时器 / 首页接线）。
- 无真实扫描。
- 无真实 ScanScore computation（扫描分数计算）。
- 无 Candidate Attention workflow（候选关注流程）。
- 无 Promote To Home workflow（提升到首页观察流程）。
- 无 opportunity promote execution（机会提升执行）。
- 无 opportunity push execution（机会推送执行）。
- 无 readiness（可执行就绪）升级。
- 无真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 无 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 3. Guard 输出规则

`DefaultWatchlistScanGuardValidator` 保持：

- `snapshot == null` => `INCOMPLETE`。
- 非观察库资产 => `BLOCKED_NOT_WATCHLIST`。
- unknown watchlist membership（未知观察库成员关系）=> `INCOMPLETE`。
- missing fields（缺失字段）=> `INCOMPLETE`。
- `dataQualityStatus=INCOMPLETE` / `BLOCKED` => safe incomplete state（安全信息不完整状态）。
- `dataQualityStatus=REVIEW_ONLY` 或 `staleStatus=REVIEW_ONLY` => `REVIEW_ONLY`。
- 其它安全输入仍返回 `REVIEW_ONLY`，不进入真实分数、候选关注、提升首页或推送。

所有输出都保持：

- `manualReviewRequired=true`。
- `notTradeInstruction=true`。
- `opportunityPushAllowed=false`。
- `entryStopTpRrGenerated=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。

## 4. 测试命令和结果

```bash
./mvnw -q -Dtest=DefaultWatchlistScanGuardValidatorTest test
```

结果：通过。

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

```bash
./mvnw -q -DskipTests test-compile
```

结果：通过。

## 5. git diff --check 结果

```bash
git diff --check
```

结果：通过，无输出。

## 6. 当前结论

P210 只是 guard skeleton（保护器骨架），不是 runtime scan（运行时扫描）。

P210 不授权真实低频扫描、运行时数据读取、行情接入、扫描分数计算、候选关注流程、提升首页执行、机会推送执行、真实点位、readiness（可执行就绪）、order API（下单接口）、execution API（执行接口）或 auto-trading（自动交易）。
