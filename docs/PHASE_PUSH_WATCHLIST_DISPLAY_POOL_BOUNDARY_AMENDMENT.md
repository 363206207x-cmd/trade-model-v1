# Push Watchlist Display Slots / Watchlist Pool Boundary Amendment

## 一、背景修正

本修正文档用于澄清 Push Watchlist 后续阶段中的首页展示位、观察库、低频扫描和机会提升边界。

当前关键修正：

- 首页默认展示的 6 个资产只是 Display Slots，不是后端推送全集。
- 首页默认展示的 6 个资产不是唯一观察库。
- 用户可以向 Watchlist Pool 添加多个资产，不限于首页 6 个展示位。
- 不在首页展示但仍在 Watchlist Pool 中的资产，可以进入后续低频扫描设计。
- 不在 Watchlist Pool 中的资产，仍不进入推送候选。

## 二、首页展示列表 Display Slots

Display Slots 指首页用于高频查看、快速对比和操作入口展示的资产位置。

边界：

- 首页可以默认展示 6 个资产。
- 这 6 个资产只是首页展示位。
- Display Slots 不等于推送候选全集。
- Display Slots 不等于 Watchlist Pool 的容量上限。
- Display Slots 不代表系统默认允许推送全部默认六币。
- Display Slots 的管理应作为独立阶段设计。

当前结论：

- 首页展示位解决的是可见性和操作效率问题。
- 首页展示位不直接决定 Push eligibility。
- 首页展示位不应被 P2C 写入 UI 混同为 watchlist 配置。

## 三、观察库 Watchlist Pool

Watchlist Pool 指用户人工维护的观察资产集合，是推送候选的最大边界。

边界：

- 用户可以向 Watchlist Pool 添加多个资产。
- Watchlist Pool 不限于首页 6 个 Display Slots。
- Watchlist Pool 中的资产可以部分展示在首页，部分不展示在首页。
- Watchlist Pool 是 Push 候选的最大边界。
- 不在 Watchlist Pool 中的资产仍不进入推送候选。
- Watchlist Pool 不是交易指令集合。
- Watchlist Pool 不代表自动下单、自动开仓、自动平仓或自动反手。

当前结论：

- P2C 编辑的是 Watchlist Pool。
- P2C 不编辑首页 6 个 Display Slots。
- P2C 不把默认首页资产自动写入 Watchlist Pool。

## 四、低频扫描 Low-Frequency Scan

Low-frequency Scan 指对 Watchlist Pool 中未在首页 Display Slots 展示的资产进行低频观察。

边界：

- 只扫描 Watchlist Pool 内资产。
- 只扫描未在首页展示的 Watchlist Pool 资产。
- 不扫描 Watchlist Pool 之外的资产。
- 不改变 P0 fail-closed 边界。
- 不改变 Push 判定逻辑。
- 不直接产生自动交易动作。

当前结论：

- P2C 不做低频扫描。
- Low-frequency Scan 应拆到后续 P3A 单独设计。
- P3A 需要单独定义频率、数据源、风险过滤、Push eligibility 和 smoke 验证。

## 五、机会提升 Promote To Home

Promote To Home 指 Watchlist Pool 中未在首页展示的资产，在低频扫描中出现值得关注机会后，可以提升到首页 Display Slots。

边界：

- Promote To Home 只适用于 Watchlist Pool 内资产。
- 不在 Watchlist Pool 中的资产不能通过 Promote To Home 进入首页。
- Promote To Home 不是交易信号。
- Promote To Home 不触发自动下单。
- Promote To Home 不触发自动开仓、自动平仓或自动反手。
- Promote To Home 不改变 order API 边界。

当前结论：

- P2C 不做机会提升。
- Promote To Home 应拆到后续 P3B 单独设计。
- P3B 需要明确提升条件、展示位冲突处理、人工确认和回退策略。

## 六、P2C 边界修正

P2C 的职责：

- 编辑 Watchlist Pool。
- 复用现有 watchlist 配置能力。
- 保持受控配置 UI 语义。
- 保持人工配置、审计可追踪和 fail-closed 边界。

P2C 不做：

- 不做首页 6 个 Display Slots 管理。
- 不做低频扫描。
- 不做机会提升到首页。
- 不做 Push 判定逻辑变更。
- 不做最新价 recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。

关键澄清：

- P2C 编辑的是观察库 Watchlist Pool，不是首页六个展示位。
- Watchlist Pool 可以大于首页展示位数量。
- 首页 Display Slots 可以从 Watchlist Pool 中选择展示，但这属于后续 P2D。
- 空 Watchlist Pool 仍然 fail-closed。
- disabled Watchlist Pool 仍然 fail-closed。
- 不在 Watchlist Pool 中的资产仍不推。

## 七、后续拆分建议

建议后续阶段拆分为：

1. P2C：观察库写入 UI
   - 编辑 Watchlist Pool。
   - 不管理首页 Display Slots。
   - 不做低频扫描。
   - 不做机会提升。

2. P2D：首页展示位管理
   - 管理首页 6 个 Display Slots。
   - 明确展示位默认值、替换、固定和排序规则。
   - 不改变 Watchlist Pool 的推送候选边界。

3. P3A：观察库低频扫描
   - 只扫描 Watchlist Pool 中未在首页展示的资产。
   - 定义低频扫描频率、数据源和风险过滤。
   - 不扫描 Watchlist Pool 外资产。

4. P3B：机会提升到首页
   - 将 Watchlist Pool 中值得关注的资产提升到首页 Display Slots。
   - 定义提升条件、人工确认、展示位冲突和回退策略。
   - 不自动交易。

## 八、持续运行边界

以下边界继续保持：

- Push 只是人工查看提醒。
- Push 不是交易信号。
- 不自动下单。
- 不自动开仓。
- 不自动平仓。
- 不自动反手。
- 不接 order API。
- 不在 Watchlist Pool 中的资产不推。
- governance_missed 不推。
- 踩踏状态禁止机会推送。
- HIGH_RISK / CONFUSED / INVALIDATED / COOLING 不直接变成机会推送。
- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。

## 九、当前执行建议

当前建议：

- 暂停继续扩展 P2C dashboard 写入 UI 实现。
- 先提交本边界修正文档。
- P2C 后续实现必须按本修正理解 Watchlist Pool 和 Display Slots。
- 不直接实现 P2D / P3A / P3B。
- 不恢复项目外大轨道源码。
- 不引入自动交易动作。

