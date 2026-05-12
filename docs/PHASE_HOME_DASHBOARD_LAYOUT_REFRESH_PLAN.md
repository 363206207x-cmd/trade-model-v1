# Home Dashboard Layout Refresh Plan

## 一、背景

- 首页功能持续增加，已经出现模块堆叠和视觉拥挤。
- Push Watchlist P0-P2C 已完成，Watchlist Pool 的查询、audit、受控写入 UI 和边界文案均已收口。
- P2D-A Display Slots 已完成，首页展示位已经通过 localStorage 管理。
- 当前需要整理首页布局，而不是继续在 `dashboard.html` 中直接堆功能。
- 本方案只定义首页布局整理方向，不实现代码、不改 API、不改变 V1 决策链路。

## 二、当前首页模块盘点

当前 dashboard 已包含以下模块：

- 顶部导航 / 首页总览：标题、运行状态、更新时间、自动刷新、帮助、主题、设置。
- layer1 系统状态卡：系统摘要、系统运行状态、数据源、持仓同步、Hot Reset、confused / high risk 等状态。
- 实时告警 / 关键事件：高优先级提醒、行情警告、今日遗漏但正确机会。
- Display Slots / tilesRow：首页展示位、最多 6 个资产、添加 / 移除 / 恢复默认。
- Watchlist Pool 状态：`watchlistStatusPanel` 中展示重点观察推送范围。
- Watchlist audit：最近变更列表与读取失败 / 空态文案。
- Watchlist 写入 UI：编辑观察列表、operator / reason、二次确认、受控 POST。
- 已开仓监控：手动录入持仓、持仓事实、风险等级、边界状态、人工处置参考。
- 执行建议：detail tab 中承接有效期、失效条件、计划摘要。
- AI 三方裁决：AI 角色意见、冲突复核、原始输出与一致性提示。
- detail 区：当前结论、决策原因、AI、执行建议、API 关键信息、持仓跟踪。
- review / plan / score / timeline 等信息：reviewReasons、plan readiness、score eight items、asset event timeline 相关摘要和明细。

## 三、当前问题

- Watchlist / Display Slots / 写入 UI 信息密度过高。
- 配置类 UI 和决策类 UI 混在一起，系统摘要卡既承载状态又承载配置。
- 首页展示位和观察库虽然已区分，但视觉上仍可能混淆。
- 持仓监控 / 执行建议 / AI 裁决需要更稳定的层级，避免只作为 detail tab 被淹没。
- 实时告警 / 关键事件应保持第二行优先级，不应被配置区挤压。
- `dashboard.html` 继续膨胀，后续维护成本上升。
- 后续 P3A / RuleEngine 如果继续叠加，会让首页从“决策面板”变成“功能堆栈”。

## 四、推荐首页目标结构

### 第 0 层：顶部栏

- 导航。
- 更新时间。
- 系统状态灯。
- 搜索 / 通知入口。

### 第 1 层：全局状态摘要

- 系统状态。
- 数据质量。
- AI 状态。
- 风险等级。
- Position Source。
- 可执行机会 / confused / hot reset 等只作为状态展示，不在此层塞操作表单。

### 第 2 层：实时告警 + 关键事件

- 实时告警。
- 关键事件日历。
- 保持首页第二行优先级。

### 第 3 层：重点资产监控 / Display Slots

- 最多 6 个首页展示位。
- 明确 Display Slots 只影响首页展示。
- Display Slots 不代表推送候选。
- Display Slots 不等于 Watchlist Pool。

### 第 4 层：持仓监控 + 执行建议

- 已开仓监控。
- 执行建议。
- 建议并排展示，让“已有仓位”和“下一步人工处置参考”靠近。
- 仍保持人工复核 / 非交易指令边界。

### 第 5 层：AI 三方裁决

- GPT 最终裁决。
- Gemini 冲突复核。
- Grok 反方挑战。
- 裁决一致性。
- 不跳二级页，默认展示摘要，可展开详情。

### 第 6 层：配置 / 管理折叠区

- Watchlist Pool 当前状态。
- Watchlist audit 最近变更。
- Watchlist 写入 UI。
- Display Slots 管理。
- 这些默认可折叠，不抢占决策主视图。

## 五、Watchlist / Display Slots 布局原则

- Display Slots 在资产监控区展示。
- Watchlist Pool 在配置折叠区展示。
- Watchlist 写入 UI 默认折叠。
- audit 历史默认折叠或小列表。
- 首页展示位不代表推送候选。
- Watchlist Pool 才决定推送候选。
- 非 Watchlist Pool 资产不进入推送候选。

## 六、哪些模块应该保留在首页主视图

- 全局状态。
- 实时告警。
- 关键事件。
- Display Slots 资产卡。
- 已开仓监控。
- 执行建议。
- AI 三方裁决摘要。

## 七、哪些模块应该折叠

- Watchlist 写入 UI。
- Watchlist audit 历史完整列表。
- Display Slots 管理细节。
- legacy detail panels。
- debug / status 细节。
- 规则配置类说明。

## 八、首页布局实施建议

建议分阶段实施：

1. Layout P0：只做方案文档。
2. Layout P1：CSS / DOM 小重排，不改 API。
3. Layout P2：折叠 Watchlist 配置区。
4. Layout P3：优化持仓 / 执行建议 / AI 裁决区。
5. Layout P4：视觉密度和移动适配。

每一步必须单独 commit，不能一次性大改。

## 九、禁止内容

- 不改后端 API。
- 不改 schema。
- 不改 Push 判定逻辑。
- 不改 Watchlist Pool 逻辑。
- 不改 Display Slots 逻辑。
- 不接自动交易。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做 low-frequency scan / Promote To Home。
- 不删除已验证能力。

## 十、测试策略

布局实现后至少验证：

- compile。
- test-compile。
- DashboardControllerTest。
- `/dashboard` HTTP 200。
- summary/detail 200。
- watchlist API 200。
- 页面仍可见：
  - 首页展示位
  - Display Slots
  - Watchlist Pool
  - 实时告警
  - 关键事件
  - 已开仓监控
  - 执行建议
  - AI 三方裁决
- 不出现 template error。

## 十一、下一步建议

- 先提交本方案文档。
- 不直接改 `dashboard.html`。
- 下一步创建 implementation checklist。
- 然后再做最小布局重排。
