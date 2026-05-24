# P211 Watchlist Scan Guard Wiring Authorization Gate

## 1. 阶段定位

P211 只定义未来 guard wiring（保护器接线）的授权边界。

P211 不实现 wiring（接线）。

P211 不写 Java，不新增测试，不修改 P210 guard / validator（保护器 / 校验器）。

## 2. 未来 P212 可考虑内容

未来 P212 可以考虑非 runtime（非运行时）的 test-only assembler（仅测试组装器）或 service boundary（服务边界）。

未来 P212 可以调用 P210 guard（保护器）。

未来 P212 可以只验证输入 DTO（数据传输对象）到输出 DTO 的安全链路。

未来 P212 可以只做单元测试级 wiring（接线）。

未来 P212 可以只做 no-score / no-push / no-readiness / no-trading guard wiring（无分数 / 无推送 / 无可执行就绪 / 无交易保护接线）。

## 3. 未来 P212 禁止

未来 P212 禁止：

- 不接 scheduler（定时器）。
- 不接 MarketQuoteClient（行情客户端）。
- 不读 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不创建 scan loop（扫描循环）。
- 不创建 real ScanScore computation（真实扫描分数计算）。
- 不创建 Candidate Attention workflow（候选关注流程）。
- 不创建 Promote To Home workflow（提升到首页观察流程）。
- 不创建 opportunity push execution（机会推送执行）。
- 不升级 readiness（可执行就绪）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建交易动作。
- 不接 order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 4. 未来 P212 必须保持

未来 P212 必须保持：

- `opportunityPushAllowed=false`。
- `entryStopTpRrGenerated=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。
- `manualReviewRequired=true`。
- `notTradeInstruction=true`。

这些字段只能服务人工复核和失败关闭，不能被解释为交易信号、推送执行或 readiness（可执行就绪）。

## 5. 结论

P212 如实现，也必须先做非 runtime wiring / tests（非运行时接线 / 测试）。

P212 不得直接进入真实扫描或行情接入。
