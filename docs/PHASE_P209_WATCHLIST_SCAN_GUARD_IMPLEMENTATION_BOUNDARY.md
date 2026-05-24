# P209 Watchlist Scan Guard Implementation Boundary

## 1. 阶段定位

P209 只定义未来 guard / validator（保护器 / 校验器）的实现边界。

P209 不实现 guard / validator。

P209 不写 Java。

## 2. 未来 P210 可考虑内容

未来 P210 可以考虑 pure guard / validator / tests（纯保护器 / 校验器 / 测试）。

未来 P210 可以消费 P208 DTO（数据传输对象）。

未来 P210 可以根据输入 DTO 返回 safe DTO state（安全数据对象状态）。

未来 P210 可以只做 no-score / no-push / no-readiness guard（无分数 / 无推送 / 无可执行就绪保护）。

未来 P210 可以只做 fail-closed 判断。

## 3. 未来 guard 允许输出

未来 guard / validator（保护器 / 校验器）允许输出：

- `BLOCKED_NOT_WATCHLIST`。
- `INCOMPLETE`。
- `REVIEW_ONLY`。
- `NOT_IMPLEMENTED`。
- `candidateAttentionReviewOnly`：仅在不打开 push / readiness / trading（推送 / 可执行就绪 / 交易）时允许。
- `promoteToHomeReviewOnly`：仅在不打开 push / readiness / trading（推送 / 可执行就绪 / 交易）时允许。

以上输出只能服务人工复核，不得被解释为交易信号或执行许可。

## 4. 未来 guard 禁止

未来 guard / validator（保护器 / 校验器）禁止：

- 不读 runtime / live / external data（运行时 / 实时 / 外部数据）。
- 不接 MarketQuoteClient（行情客户端）。
- 不接 service / mapper / controller / scheduler / dashboard（服务 / 映射器 / 控制器 / 定时器 / 首页）。
- 不计算真实 ScanScore（扫描分数）。
- 不排序真实资产。
- 不创建 Candidate Attention workflow（候选关注流程）。
- 不创建 Promote To Home workflow（提升到首页观察流程）。
- 不创建 opportunity push execution（机会推送执行）。
- 不升级 readiness（可执行就绪）。
- 不生成 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不创建交易动作。

## 5. 结论

P210 如实现，也必须先做 pure guard / validator / tests（纯保护器 / 校验器 / 测试）。

P210 不得直接进入真实扫描或行情接入。
