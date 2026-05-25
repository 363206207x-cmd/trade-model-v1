# P255 Promote To Home Scope Gate

## 1. 阶段定位

P255 定义 Promote To Home scope gate。P255 不实现 Promote To Home，不写 Java，不改 dashboard，不创建首页提升执行。

## 2. Promote To Home 定义

Promote To Home 只是首页展示提升。

Promote To Home 不做以下事情：

- 不改变 Watchlist Pool。
- 不改变 Batch universe。
- 不触发 Push。
- 不触发 Readiness。
- 不生成交易计划。
- 不生成 entry / stop / TP / RR。

## 3. Promote To Home 前置条件

Promote To Home 后续如果进入实现，必须满足：

- Candidate Attention review-only 之后。
- 必须保留人工复核。
- 必须保留非交易指令。
- 必须有 blockingReasons / reasons / source trace。
- 必须受 Risk Action Guard 约束。

## 4. Display Slots 边界

- Display Slots 是首页展示位。
- Display Slots 不是 Watchlist Pool。
- Display Slots 不是 batch universe。
- 默认六币不是 batch universe。
- Promote To Home 不能把非观察库资产加入候选池。

## 5. 结论

Promote To Home 需要独立授权门。Promote To Home 不得和 Candidate Attention Java 实现混在一个 production action 中。P255 不授权实现。
