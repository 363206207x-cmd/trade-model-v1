# P212 Watchlist Scan Guard Test-Only Wiring Verification

## 1. 阶段定位

P212 只实现 non-runtime test-only wiring / assembler skeleton（非运行时、仅测试级接线 / 组装器骨架）。

P212 只新增：

- `WatchlistScanGuardWiringAssembler`。
- `DefaultWatchlistScanGuardWiringAssembler`。
- `DefaultWatchlistScanGuardWiringAssemblerTest`。

P212 只允许把 `WatchlistRuntimeSnapshotDTO` 交给 P210 `WatchlistScanGuardValidator`，再返回 `WatchlistScanResultDTO` 的安全状态。

P212 不是 runtime scan（运行时扫描），不是 scan loop（扫描循环），不是行情接入。

## 2. 本轮安全边界确认

P212 没有做以下事项：

- 无 scheduler（定时器）接线。
- 无 MarketQuoteClient（行情客户端）。
- 无 runtime / live / external data（运行时 / 实时 / 外部数据）读取。
- 无 scan loop（扫描循环）。
- 无 real ScanScore computation（真实扫描分数计算）。
- 无 Candidate Attention workflow（候选关注流程）。
- 无 Promote To Home workflow（提升到首页观察流程）。
- 无 opportunity push execution（机会推送执行）。
- 无 readiness（可执行就绪）升级。
- 无 real entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。
- 无 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 3. Test-only wiring 行为

`DefaultWatchlistScanGuardWiringAssembler` 只做三件事：

- 默认构造函数创建本地 `DefaultWatchlistScanGuardValidator`。
- 构造函数可注入本地 `WatchlistScanGuardValidator`。
- `assembleReviewOnlyResult(snapshot)` 只调用 `guard.validate(snapshot)` 并返回 guard result。

Fail-closed（失败关闭）规则：

- `guard == null` 时返回 `INCOMPLETE`，原因包含 `GUARD_MISSING`。
- `guard.validate(snapshot) == null` 时返回 `INCOMPLETE`，原因包含 `GUARD_RESULT_MISSING`。
- 所有输出保持 `manualReviewRequired=true`。
- 所有输出保持 `notTradeInstruction=true`。
- 所有输出保持 `opportunityPushAllowed=false`。
- 所有输出保持 `entryStopTpRrGenerated=false`。
- 所有输出保持 `readinessUpgraded=false`。
- 所有输出保持 `tradingActionCreated=false`。

## 4. 验证命令和结果

本轮验证结果：

```bash
./mvnw -q -Dtest=DefaultWatchlistScanGuardWiringAssemblerTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
git diff --check
```

结果：

- `./mvnw -q -Dtest=DefaultWatchlistScanGuardWiringAssemblerTest test`：通过。
- `./mvnw -q -DskipTests compile`：通过。
- `./mvnw -q -DskipTests test-compile`：通过。
- `git diff --check`：通过，无 whitespace error。

## 5. 当前结论

P212 只是 test-only wiring skeleton（仅测试级接线骨架）。

P212 不授权 runtime scan（运行时扫描）、MarketQuoteClient integration（行情客户端接入）、scheduler behavior changes（定时器行为改变）、ScanScore computation（扫描分数计算）、Candidate Attention / Promote To Home workflow（候选关注 / 提升到首页观察流程）或 Opportunity Push execution（机会推送执行）。
