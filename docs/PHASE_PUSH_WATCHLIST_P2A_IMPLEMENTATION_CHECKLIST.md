# Push Watchlist P2A Implementation Checklist

## 一、P2A implementation 总原则

- P2A 是只读展示，不是配置编辑。
- 不调用 `POST /api/rule/push-watchlist`。
- 只调用 `GET /api/rule/push-watchlist`。
- audit 可选，只能调用 `GET /api/rule/push-watchlist/audit?limit=1`。
- 不改 schema。
- 不改后端 API。
- 不改 Push 判定逻辑。
- 不恢复项目外源码。
- 不自动下单 / 开仓 / 平仓 / 反手。
- 不改变首页既有布局结构，只做小卡片/小区域插入。

## 二、P2A 最小实现目标

- 在 dashboard 首页展示当前 watchlist 配置状态。
- 展示 `enabled`。
- 展示 `symbols`。
- 展示 symbol count。
- 展示 `ruleValue` 或来源 `push.watchlist.symbols`。
- 可选展示最近 audit 时间 / changedBy。
- 显示边界文案：
  - 仅人工查看提醒。
  - 非交易指令。
  - 不自动下单。
  - 非 watchlist 不进入推送候选。

## 三、允许修改文件

建议只允许：

- `src/main/resources/templates/dashboard.html`。
- 如已有 dashboard template test，则允许对应最小 test 文件。

明确不允许：

- Java controller。
- Java service。
- mapper。
- `schema.sql`。
- `application.yml`。
- RuleEngine / Opportunity / TradeReview。
- PushSnapshotService / PushRecheckServiceImpl。

## 四、dashboard 插入位置建议

- 优先放在现有“重点观察资产 / Push 状态 / 系统状态”附近。
- 不要重排首页三层结构。
- 不移动持仓监控。
- 不移动 AI 裁决。
- 不影响已验证的 detail / summary / actionAdvice。
- 如果找不到合适位置，先新增轻量只读卡片，不做复杂布局。

## 五、前端数据获取 checklist

- 页面加载时 fetch `/api/rule/push-watchlist`。
- 成功后渲染 enabled / symbols / count / ruleValue。
- 失败时显示“读取失败，按 fail-closed 处理”。
- 空列表时显示“当前未配置观察资产，推送候选关闭”。
- disabled 时显示“观察推送已禁用”。
- 可选 fetch `/api/rule/push-watchlist/audit?limit=1`。
- audit 失败不影响主 watchlist 展示。

## 六、UI 文案 checklist

建议文案：

- 标题：重点观察推送范围。
- 状态：启用 / 禁用。
- 观察资产：BTCUSDT, ETHUSDT。
- 数量：2。
- 配置来源：`push.watchlist.symbols`。
- 最近变更：operator / time。
- 边界：仅人工查看提醒，非交易指令，不自动下单。
- 空状态：未配置观察资产，系统不会生成观察推送。

## 七、测试 checklist

实现后至少验证：

- compile。
- test-compile。
- dashboard template / controller 相关测试。
- `/dashboard` 200。
- `/api/rule/push-watchlist` 200。
- dashboard 页面无 template error。
- 静态 grep 存在：
  - 重点观察推送范围。
  - 非交易指令。
  - 不自动下单。
  - `push.watchlist.symbols`。
- 不需要跑全量测试，除非 dashboard 改动较大。

## 八、forbidden grep checklist

每次 staging 前必须确认 diff 不包含：

- `POST /api/rule/push-watchlist`。
- `schema.sql`。
- `application.yml`。
- RuleController。
- RuleConfigService。
- PushSnapshotService。
- PushRecheckServiceImpl。
- latestPrice recheck。
- AssetStateMapper。
- stampede。
- RuleEngine。
- Opportunity。
- TradeReview。
- 自动下单。
- 自动开仓。
- 自动平仓。
- 自动反手。
- order API。
- apiKey。
- secret。

## 九、P2A 不做内容

- 不做编辑 UI。
- 不做保存按钮。
- 不做 audit 管理页。
- 不做 Push/Recheck 逻辑变更。
- 不做 latest-price recheck。
- 不做 asset-state gate。
- 不做 stampede guard。
- 不做 RuleEngine / Opportunity / TradeReview。
- 不做自动交易。

## 十、建议 commit 顺序

1. 提交本 checklist。
2. P2A dashboard 只读展示最小实现。
3. P2A dashboard/template test。
4. P2A smoke 验证。

如实现和测试很小，可以实现 + test 同一 commit，但必须先复核。

## 十一、下一步建议

提交本 checklist 后，再进入 P2A dashboard 只读展示最小实现方案。不直接开始大改 `dashboard.html`。
