# P208 Watchlist Scan Runtime DTO Skeleton Verification

## 1. 阶段定位

P208 只实现 DTO / enum / tests（数据传输对象 / 枚举 / 测试）骨架。

P208 新增 `WatchlistRuntimeSnapshotDTO`、`WatchlistScanResultDTO` 和 `WatchlistScanStatusEnum`，只用于承载未来观察库扫描前的 fail-closed（失败关闭）和 review-only（只允许复核）状态。

P208 不是 runtime scan（运行时扫描）。

## 2. 本轮已验证边界

- 无 runtime read（运行时读取）。
- 无 live / external data read（实时 / 外部数据读取）。
- 无 service / mapper / controller / scheduler / dashboard wiring（服务 / 映射器 / 控制器 / 定时器 / 首页接线）。
- 无 MarketQuoteClient（行情客户端）。
- 无真实扫描。
- 无 ScanScore implementation（扫描分数实现）。
- 无 Candidate Attention implementation（候选关注实现）。
- 无 Promote To Home execution（提升到首页观察执行）。
- 无 opportunity promote execution（机会提升执行）。
- 无 opportunity push execution（机会推送执行）。
- 无 readiness（可执行就绪）升级。
- 无真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 无 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 3. 安全默认值

`WatchlistRuntimeSnapshotDTO` 保持：

- `manualReviewRequired=true`。
- `notTradeInstruction=true`。
- 非观察库资产 fail-closed（失败关闭）。
- 未知观察库成员关系保持 incomplete（信息不完整）。
- missing / stale data（缺失 / 过期数据）只允许 incomplete（信息不完整）或 review-only（只允许复核）。

`WatchlistScanResultDTO` 保持：

- `manualReviewRequired=true`。
- `notTradeInstruction=true`。
- `opportunityPushAllowed=false`。
- `entryStopTpRrGenerated=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。

任何 factory（工厂方法）都不允许打开机会推送、真实点位、可执行就绪或交易动作。

## 4. 测试命令和结果

```bash
./mvnw -q -Dtest=WatchlistRuntimeSnapshotDTOTest,WatchlistScanResultDTOTest test
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

P208 只是 skeleton（骨架），不是 runtime scan（运行时扫描）。

P208 不授权真实低频扫描、运行时数据读取、行情接入、扫描分数计算、候选关注流程、提升首页执行、机会推送执行、真实点位、readiness（可执行就绪）、order API（下单接口）、execution API（执行接口）或 auto-trading（自动交易）。
