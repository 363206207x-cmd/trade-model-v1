# Home User Copy Inventory

Scope: production `home.html`, `home-runtime.js`, Home read model values, and shared frontend semantic mapping. Internal IDs and fixture-only source values are excluded because they are never rendered as copy.

| Component | Selector | Current Copy | Source | Issue Type | Final Copy |
|---|---|---|---|---|---|
| Header | `h1` | Fundamental AI | template | none | Fundamental AI |
| Header | `#selectedAssetContext` | 尚未选择机会资产 | template/runtime | none | 尚未选择机会资产 |
| Header | `#headerDataSource` | 数据状态同步中 | template/runtime | semantic mapping | 数据 · {状态} · AI · {状态} |
| Header | `#headerUpdatedAt` | 更新时间待同步 | template/runtime | none | 更新时间待同步 / 更新于 {时间} |
| Navigation | `[aria-label=首页]` | 首页 | template | none | 首页 |
| Navigation | `[aria-label=持仓]` | 持仓 | template | none | 持仓 |
| Navigation | `[aria-label=AI分析]` | AI分析 | template | none | AI分析 |
| Navigation | `[aria-label=消息]` | 消息 | template | none | 消息 |
| Navigation | `[aria-label=我的]` | 我的 | template | none | 我的 |
| Navigation | `.rail-logout button` | 退出登录 | template | none | 退出登录 |
| Status | `#statusMarket` | 等待同步 | template/read model | none | {市场趋势} |
| Status | `#statusRisk` | 等待同步 | template/read model | none | {风险等级} |
| Status | `#statusQuality` | 等待同步 | template/read model | none | {数据质量} |
| Status | `#statusAi` | 等待同步 | template/read model | enum exposure guard | {AI 状态标签} |
| Status | `#statusOpportunity` | 等待同步 | template/runtime | none | {数量} 个 / 暂无 |
| Status | `#statusReset` | 等待同步 | template/read model | enum exposure guard | {Hot Reset 状态} |
| Alert | `.signal-kind` | 告警 | template | none | 告警 |
| Alert | `#homeAlert strong` | dynamic message | read model/runtime | technical token guard | {用户可见告警摘要} |
| Alert | `#homeAlert em` | dynamic level | semantic mapping | enum exposure guard | 高优先级 / 需关注 / 读取失败 |
| Event | `.signal-kind` | 事件 | template | none | 事件 |
| Event | `#homeEvent strong` | dynamic event | read model | none | {事件名称} |
| Signal empty | `#signalEmpty` | 暂无需要关注的告警或事件 | template | none | 暂无需要关注的告警或事件 |
| Opportunity | `#opportunityHeading` | 机会资产 · 0 | template/runtime | none | 机会资产 · {有效机会数} |
| Search | `#homeAssetSearch` | 搜索资产 | template | none | 搜索资产 |
| Search | `#homeSelectedSearchSymbol` | 尚未选择资产 | template/runtime | none | 尚未选择资产 |
| Search | `#homeSelectedSearchState` | 请先从搜索结果中选择 | template/runtime | conversational | 未选择搜索结果 |
| Search | `#homeAssetPoolCount` | 观察资产池 · 同步中 | template/runtime | none | 观察资产池 · {数量} |
| Search | `#homePreviewAsset` | 分析 | template/runtime | locked action | 分析 |
| Search | `#homeAddAsset` | 添加 | template/runtime | locked action | 添加 |
| Search | `#homeAddAsset` | 已添加 | runtime | locked action | 已添加 |
| Search | `#homeAssetSearchStatus` | 正在分析… | runtime | conversational | 分析中 |
| Search | `#homeAssetSearchStatus` | 正在添加… | runtime | conversational | 添加中 |
| Search | `.search-result strong` | 未找到资产 | runtime | none | 未找到资产 |
| Search | `.search-result small` | 请尝试其他名称或交易对 | runtime | conversational | 可更换名称或交易对 |
| Search | `.search-result em` | 未添加 | runtime | none | 未添加 |
| Search | `.search-result em` | 已添加 | runtime | none | 已添加 |
| Opportunity card | `.asset-identity strong` | dynamic name | read model | none | {资产名称} |
| Opportunity card | `.state-badge` | raw opportunity state | semantic mapping | enum exposure guard | {机会状态标签} |
| Opportunity card | `.opportunity-metrics` | 机会评分 | runtime | none | 机会评分 |
| Opportunity card | `.opportunity-metrics` | 置信度 | runtime | none | 置信度 |
| Opportunity card | `.opportunity-metrics` | 风险 | runtime | none | 风险 |
| Opportunity card | `.opportunity-final` | Final Bias | runtime | technical copy | 最终偏向 |
| Opportunity card | `.opportunity-final` | Plan Mode | runtime | technical copy | 计划模式 |
| Opportunity empty | `#opportunityEmpty strong` | 当前没有进入重点机会的资产 | template | verbose | 暂无重点机会 |
| Opportunity empty | `#opportunityEmpty span` | 资产池仍会持续分析，新的有效机会将按优先级进入这里。 | template | redundant explanation | removed |
| Position | `#positionHeading` | 持仓监控 | template | locked title | 持仓监控 |
| Position | `#positionAggregate` | 活动 0 · 最高风险 暂无评估 · 等待评估 | template/runtime | none | 活动 {数} · 最高风险 {等级} · {覆盖状态} |
| Position | `.position-header-actions a` | 查看全部 | template | none | 查看全部 |
| Position | `.position-judgment` | 持仓风险 | runtime | none | 持仓风险 |
| Position | `.position-judgment` | 监控结论 | runtime | none | 监控结论 |
| Position | `.position-judgment` | 建议动作 | runtime | none | 建议动作 |
| Position | `.position-facts` | 开仓价 | runtime | none | 开仓价 |
| Position | `.position-facts` | 标记价格 | runtime | none | 标记价格 |
| Position | `.position-facts` | 盈亏 | runtime | none | 盈亏 |
| Position | `.position-facts` | 开仓时间 | runtime | none | 开仓时间 |
| Position | `.monitor-details` | 入场逻辑状态 | runtime | none | 入场逻辑状态 |
| Position | `.monitor-details` | 反转状态 | runtime | none | 反转状态 |
| Position | `.monitor-details` | 风险变化原因 | runtime | none | 风险变化原因 |
| Position | `.monitor-details` | 最近监控时间 | runtime | none | 最近监控时间 |
| Position | `.position-detail-link` | 详情 | runtime | none | 详情 |
| Position empty | `#positionEmpty strong` | 暂无手动录入持仓 | template | verbose | 暂无持仓 |
| Position empty | `#positionEmpty span` | 录入真实开仓事实后，系统才会进入持续监控。 | template | redundant explanation | removed |
| Position empty | `#positionEmpty a` | 录入持仓 | template | none | 录入持仓 |
| Plan | `#planHeading` | 执行计划 | template | locked title | 执行计划 |
| Plan | `#planAsset` | 未选择资产 | template/runtime | none | 未选择资产 / {资产} |
| Plan | `#planDetailLink` | 查看详情 | template | none | 查看详情 |
| Plan empty | `.plan-empty strong` | 尚未形成 | runtime | domain term | 尚未形成 |
| Plan empty | `.plan-empty span` | 当前没有通过规则校验的 Final Execution Plan。 | runtime | technical/verbose | 尚未形成有效计划 |
| Plan | `.plan-decision` | 最终方向 · 计划模式 | runtime | none | 最终方向 · 计划模式 |
| Plan | `.plan-conditions` | 入场 / 触发 | runtime | none | 入场 / 触发 |
| Plan | `.plan-conditions` | 失效条件 | runtime | none | 失效条件 |
| Plan | `.plan-conditions` | 止损 | runtime | none | 止损 |
| Plan | `.plan-conditions` | 目标 | runtime | none | 目标 |
| Plan | `.plan-conditions` | 杠杆 | runtime | none | 杠杆 |
| Plan | `.plan-conditions` | 仓位 | runtime | none | 仓位 |
| Plan | `.plan-metadata` | 有效期 | runtime | none | 有效期 |
| Plan | `.plan-metadata` | 版本 / 来源 + internal plan ID | runtime | internal ID exposure | 来源 + semantic source state |
| AI | `#aiWorkspaceHeading` | AI 分析 | template | locked title | AI 分析 |
| AI | `[data-ai-role=GPT_FINAL]` | GPT 综合判断 | template | none | GPT 综合判断 |
| AI | `[data-ai-role=GEMINI_REVIEW]` | Gemini 冲突复核 | template | none | Gemini 冲突复核 |
| AI | `[data-ai-role=GROK_CHALLENGE]` | Grok 反方挑战 | template | none | Grok 反方挑战 |
| GPT | `.ai-first-visual` | GPT Candidate · 非 Final | runtime | domain boundary | GPT Candidate · 非 Final |
| GPT | `.ai-first-visual` | Market Bias | runtime | frozen domain term | Market Bias |
| GPT | `.ai-first-visual` | Opportunity State · Candidate Mode | runtime | frozen domain term | Opportunity State · Candidate Mode |
| GPT | `.ai-section h3` | 形成原因 | runtime | none | 形成原因 |
| GPT | `.ai-section h3` | 证据 · raw collection state | semantic mapping | enum exposure guard | 证据 · {集合状态标签} |
| GPT | `.ai-section h3` | 反对证据 · raw collection state | semantic mapping | enum exposure guard | 反对证据 · {集合状态标签} |
| GPT | `.ai-summary-footer` | Candidate 摘要 | runtime | none | Candidate 摘要 |
| Gemini | `.ai-first-visual` | 复核结果 | runtime | none | 复核结果 |
| Gemini | `.ai-first-visual` | raw DOWNGRADE | runtime | enum exposure | 建议降级 |
| Gemini | `.ai-first-visual` | 调整建议 | runtime | none | 调整建议 |
| Gemini | `.ai-first-visual` | 对 Candidate | runtime | object boundary | 对 Candidate |
| Gemini | `.ai-section h3` | Before → After | runtime | frozen hierarchy | Before → After |
| Gemini | `.ai-section h3` | 证据缺口 · 逻辑冲突 · 风险低估 | runtime | none | 证据缺口 · 逻辑冲突 · 风险低估 |
| Gemini | `.ai-summary-footer` | 恢复条件 | runtime | none | 恢复条件 |
| Grok | `.ai-first-visual` | 失败路径 | runtime | none | 失败路径 |
| Grok | `.ai-section h3` | 反向情景 | runtime | none | 反向情景 |
| Grok | `.ai-section h3` | 外部事件风险 | runtime | none | 外部事件风险 |
| Grok | `.ai-section h3` | 微观结构风险 | runtime | none | 微观结构风险 |
| Grok | `.ai-section h3` | 继续观察指标 | runtime | none | 继续观察指标 |
| Conflict | `#conflictSummary h3` | Conflict Summary | runtime | technical copy | 冲突摘要 |
| Conflict | `#conflictSummary` | 冲突等级 | runtime | none | 冲突等级 |
| Conflict | `#conflictSummary` | 最终偏向 | runtime | none | 最终偏向 |
| Conflict | `#conflictSummary` | 最终计划 | runtime | none | 最终计划 |
| Conflict | `#conflictSummary` | 主要原因 | runtime | none | 主要原因 |
| Conflict | `#conflictSummary` | 恢复条件 | runtime | none | 恢复条件 |
| AI footer | `#aiMetadata` | raw role state/provider | semantic mapping/runtime | enum exposure guard | 角色状态 {标签} · 生成时间 {时间} · 来源 {名称} |
| AI footer | `#auditChainLink` | 查看完整审计链 | template | frozen long action | 查看完整审计链 |

Review outcome: conversational defaults, generic disclaimers, raw enum output, internal IDs, and redundant Home explanations are removed or guarded by semantic mapping.
