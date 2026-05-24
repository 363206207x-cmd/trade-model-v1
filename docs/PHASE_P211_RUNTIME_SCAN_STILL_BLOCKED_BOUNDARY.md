# P211 Runtime Scan Still Blocked Boundary

## 1. 阶段定位

本文明确 DTO skeleton（数据对象骨架）和 guard skeleton（保护器骨架）已存在，但 runtime scan（运行时扫描）仍然被阻断。

P211 不解除任何 runtime / market / scheduler / push / readiness / trading（运行时 / 行情 / 定时器 / 推送 / 可执行就绪 / 交易）禁令。

P211 只写文档，不实现 runtime scan（运行时扫描）。

## 2. 当前已有但不是生产链路的内容

当前已有但不是生产链路的内容：

- Low-Frequency Scan Scheduler disabled-by-default skeleton（低频扫描定时器默认关闭骨架）。
- `WatchlistRuntimeSnapshotDTO`。
- `WatchlistScanResultDTO`。
- `WatchlistScanStatusEnum`。
- `WatchlistScanGuardValidator`。
- `DefaultWatchlistScanGuardValidator`。

这些内容只能作为安全边界、DTO（数据传输对象）和 guard（保护器）骨架，不能被解释为真实生产扫描链路。

## 3. 仍然未完成

仍然未完成：

- real low-frequency scan（真实低频扫描）。
- runtime data source（运行时数据源）。
- MarketQuoteClient integration（行情客户端接入）。
- active scheduler（激活定时器）。
- scan loop（扫描循环）。
- real ScanScore（真实扫描分数）。
- Candidate Attention workflow（候选关注流程）。
- Promote To Home workflow（提升到首页观察流程）。
- Opportunity Push execution（机会推送执行）。
- real entry / stop / TP / RR（真实入场 / 止损 / 止盈 / 盈亏比）。
- readiness（可执行就绪）。
- order / execution / auto-trading（下单 / 执行 / 自动交易）。

## 4. 必须保持阻断

必须保持阻断：

- scheduler（定时器）仍默认关闭，不得激活。
- MarketQuoteClient integration（行情客户端接入）仍 blocked（阻断）。
- Candidate Attention / Promote To Home（候选关注 / 提升到首页观察）仍是 review-only concept（只允许复核概念），不是 execution workflow（执行流程）。
- Opportunity Push execution（机会推送执行）仍 blocked（阻断）。
- auto-trading（自动交易）不在 V1 范围内，保持关闭。

## 5. 结论

后续要进入 runtime scan（运行时扫描），必须另开授权门。

不能把 DTO + guard skeleton（数据对象 + 保护器骨架）误判为真实扫描完成。
