# PHASE_DASHBOARD_RESIDUAL_AUDIT_AFTER_P_TRACK_UI_COMMIT

## 1. 当前阶段与已完成 commits

当前已完成：

- `44e5a41 feat(position): close P-track manual position monitor backend`
- `0557e63 feat(position): add P-track dashboard monitor UI`
- `cf826a0 docs(workspace): add cleanup plan after P-track commits`
- `6f801f2 docs(codex): add execution guardrails for staged workspace`
- `0b8353c docs(workspace): align current docs index after P-track commits`

当前进入的是 commit 6 前 dashboard residual audit，不是 dashboard UI 开发，不是后端契约闭合。

## 2. dashboard.html residual diff 规模

- `src/main/resources/templates/dashboard.html` 当前 residual diff：
  `2995 insertions(+), 1384 deletions(-)`
- 总 diff 约 4379 行。
- 结论：该 diff 明显不是小边界，禁止整文件 add / 禁止整文件 commit。

## 3. commit 2 已经提交的 P 轨最小 UI

commit 2 `0557e63 feat(position): add P-track dashboard monitor UI` 已经提交：

- 手动录入持仓
- 持仓监控 open rows
- 手动复查
- 记录平仓

当前 residual 中的 P 轨重复 / 变体内容只能作为 residual 观察，不允许再次提交覆盖 commit 2 的受控版本。

## 4. 当前 residual 分组

- P 轨重复 / 变体
- sidebar / top nav / 首页框架
- homeSummaryBar / KPI
- legacy panels
- review-center / trade review 跳转
- score-eight
- planReadiness
- assetEventTimeline
- monitoring summary / 全局告警扩展
- 纯样式调整

这些主题不得混入同一个 dashboard 代码 commit。

## 5. 禁止直接提交的内容

明确禁止：

- 整文件提交 `dashboard.html`
- 把 review-center / score-eight / planReadiness / assetEventTimeline 混入一个 dashboard commit
- 把 sidebar / legacy / homeSummaryBar 混入同一代码 commit
- 把 P 轨重复 UI 再次提交并覆盖 commit 2
- 在后端契约未闭合前提交依赖新字段的新 UI

## 6. 后续推荐拆分顺序

1. 先提交 dashboard residual audit 文档冻结。
2. 再做 dashboard 后端契约审计。
3. 再分别审计 sidebar / homeSummaryBar / legacy。
4. review-center 独立轨道。
5. score-eight 独立轨道。
6. planReadiness 独立轨道。
7. assetEventTimeline 独立轨道。
8. 每个轨道先只读审计，再最小 patch，再编译/测试。

## 7. 需要后端契约闭合后才能提交的 UI

依赖以下后端契约或 read model 的 UI，不得只提交前端壳：

- DashboardController
- DashboardDetailResponseVO
- DashboardControllerTest
- Score read model
- Plan readiness read model
- Asset event timeline read model
- Review center / trade review read model

## 8. 不允许的 git 操作

禁止：

- `git add .`
- `git add src/main/resources/templates/dashboard.html`
- `git add docs`
- `git add src`
- `git commit -a`
- 混入 `schema.sql`
- 混入 `application.yml`
- 混入 `backups`
- 混入 `CODEX_AUTONOMOUS_TASK_QUEUE.md`
- 未经审计直接 commit

## 9. 明确未完成项

以下内容未完成：

- dashboard residual 代码未收口
- review-center 未在本 commit 完成
- score-eight 未在本 commit 完成
- planReadiness 未在本 commit 完成
- assetEventTimeline 未在本 commit 完成
- legacy cleanup 未完成
- schema/config 未处理
- RuleEngine / Push / TradeReview / Opportunity 未完成

## 10. commit 6 最小边界

commit 6 只包含：

`docs/PHASE_DASHBOARD_RESIDUAL_AUDIT_AFTER_P_TRACK_UI_COMMIT.md`

commit 6 不包含：

- `dashboard.html`
- 任何 Java 代码
- `schema.sql`
- `application.yml`
- `CODEX_AUTONOMOUS_TASK_QUEUE.md`
- 其它 docs
- backups

本文档不得作为以下口径使用：

- dashboard 已完成
- review-center 已完成
- score-eight 已完成
- planReadiness 已完成
- assetEventTimeline 已完成
- legacy cleanup 已完成
- schema/config 已处理
- RuleEngine / Push / TradeReview / Opportunity 已完成
- 可以整文件提交 dashboard.html
