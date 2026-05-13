# Home Dashboard Layout P2 Verification

## 一、验证对象

- `ca94732 docs(dashboard): define home layout P2 copy density plan`
- `1eb9557 docs(dashboard): add home layout P2 copy density checklist`
- `ab3398c feat(dashboard): reduce watchlist copy density`

## 二、Layout P2 已完成能力

- 已完成 Watchlist / Display Slots 文案密度压缩。
- 已将主视图文案短文案化。
- 已减少长说明，或将长说明收敛到折叠区。
- 已统一核心术语：
  - 首页展示位（Display Slots）
  - 观察库（Watchlist Pool）
  - 推送候选
  - 非交易指令
  - 默认 6 个首页展示位
- 未改业务逻辑。
- 未改 Watchlist POST。
- 未改 Display Slots localStorage。
- 未改 Push 判定逻辑。
- 未改后端 API。
- 未改 schema。

## 三、测试结果

- `compile` PASS。
- `test-compile` PASS。
- `DashboardControllerTest` PASS。

## 四、静态 grep 结果

已确认存在：

- 首页展示位 / Display Slots
- 观察库 / Watchlist Pool
- 推送候选
- 非交易指令
- 不自动下单
- 默认 6 个首页展示位
- 保存前确认
- 空列表将关闭推送候选

已确认未发现：

- 默认六个币
- 自定义监控
- 关注币
- order API
- apiKey
- secret

说明：

- 自动平仓 / 自动反手如有命中，仅为既有否定边界文案，不是自动交易行为。

## 五、API smoke 结果

- `/dashboard` HTTP 200。
- `/api/dashboard/summary` HTTP 200。
- BTC detail HTTP 200。
- ETH detail HTTP 200。
- SOL detail HTTP 200。
- `/api/rule/push-watchlist` HTTP 200。
- `/api/rule/push-watchlist/audit?limit=5` HTTP 200。

## 六、dashboard HTML 文案确认

`/dashboard` HTML 中已确认可见：

- 首页展示位 / Display Slots
- 观察库 / Watchlist Pool
- 推送候选
- 非交易指令
- 不自动下单
- 编辑观察列表
- 保存配置
- 最近变更

## 七、边界确认

- Layout P2 只做文案密度压缩。
- 不做业务逻辑变更。
- 不改 Watchlist POST 逻辑。
- 不改 Display Slots localStorage 逻辑。
- 不改 Push 判定逻辑。
- 不改后端 API。
- 不改 schema。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做 low-frequency scan / Promote To Home。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。

## 八、运行说明

- 普通 sandbox 启动因本地端口绑定限制失败。
- 提升权限启动成功。
- 已看到 `Tomcat started on port 8081` 和 `Started TradeModelApplication`。
- 日志中的 Binance 451 属于既有行情 fallback 场景，已走 OKX fallback，不是 Layout P2 错误。

## 九、工作区状态

- tracked clean。
- staged 为空。
- `src/main/java` / `src/test/java` / `src/main/resources` 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十、当前结论

Home dashboard Layout P2 copy density cleanup implemented and smoke verified。

## 十一、后续建议

- 暂停继续开发。
- 后续如继续，应先做下一阶段方案或 checklist。
- 可选后续方向：
  - Layout P3：整理持仓监控 / 执行建议 / AI 裁决区。
  - copy convergence：统一实时告警 / 关键事件 / AI 三方裁决文案。
  - 回到 RuleEngine / PlanBoundary 主轨道。
- 不要直接大改 dashboard。
- 不要恢复大轨道源码。
