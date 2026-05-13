# PHASE_HOME_DASHBOARD_LAYOUT_P1_VERIFICATION

## 一、验证对象

- e7ed551 docs(dashboard): define home layout refresh plan
- b03b20e docs(dashboard): add home layout refresh checklist
- 6c54a56 feat(dashboard): fold management sections

## 二、Layout P1 已完成能力

- Watchlist 写入 UI 默认折叠。
- Watchlist audit 完整列表折叠。
- Display Slots 管理细节折叠。
- 长说明文案压缩。
- 主视图保留简短摘要。
- 未改业务逻辑。
- 未改后端 API。
- 未改 schema。
- 未改 Push 判定逻辑。
- 未接自动交易。

## 三、测试结果

- compile PASS。
- test-compile PASS。
- DashboardControllerTest PASS。

## 四、静态 grep 结果

存在：

- details
- summary
- config-fold
- watchlist-editor-shell
- watchlist-audit-fold
- management-section
- compact-note
- 编辑观察列表
- Watchlist Pool 配置
- 查看最近变更记录
- 首页展示位管理
- 首页展示位
- Display Slots
- Watchlist Pool

未发现：

- order API
- apiKey
- secret

说明：

- 自动平仓 / 自动反手如有命中，仅为既有否定边界文案，不是自动交易行为。

## 五、API smoke 结果

- /dashboard HTTP 200。
- /api/dashboard/summary HTTP 200。
- BTC detail HTTP 200。
- ETH detail HTTP 200。
- SOL detail HTTP 200。
- /api/rule/push-watchlist HTTP 200。
- /api/rule/push-watchlist/audit?limit=5 HTTP 200。

## 六、dashboard HTML 文案确认

/dashboard HTML 中可见：

- 编辑观察列表
- 查看最近变更记录
- 首页展示位管理
- 首页展示位 / Display Slots
- Watchlist Pool
- 已开仓监控
- 执行建议
- 高优先级提醒
- 今日遗漏但正确机会
- AI 角色意见
- GPT / Gemini / Grok 裁决内容

说明：

- 当前页面现有文案中，实时告警 / 关键事件 / AI 三方裁决分别对应为高优先级提醒、今日遗漏但正确机会、AI 角色意见。
- 后续如要统一文案，应单独做 copy convergence，不混入 Layout P1。

## 七、边界确认

- Layout P1 只做配置 / 管理区折叠。
- 不做全首页重排。
- 不删除已验证能力。
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
- 看到 Tomcat started on port 8081 和 Started TradeModelApplication。
- 日志中的 Binance 451 属于既有行情 fallback 场景，已走 OKX fallback，不是 Layout P1 错误。

## 九、工作区状态

- tracked clean。
- staged 为空。
- src 下无 untracked。
- docs untracked 仍保留。
- 8081 已释放。

## 十、当前结论

Home dashboard Layout P1 management section folding implemented and smoke verified。

## 十一、后续建议

- 暂停继续开发。
- 后续如继续，应先做下一阶段方案或 checklist。
- 可选：
  - Layout P2：压缩 Watchlist / Display Slots 文案密度。
  - Layout P3：整理持仓监控 / 执行建议 / AI 裁决区。
  - copy convergence：统一实时告警 / 关键事件 / AI 三方裁决文案。
- 不要直接大改 dashboard。
- 不要恢复大轨道源码。
