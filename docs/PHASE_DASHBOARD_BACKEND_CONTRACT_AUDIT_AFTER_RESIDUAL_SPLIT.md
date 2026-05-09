# PHASE_DASHBOARD_BACKEND_CONTRACT_AUDIT_AFTER_RESIDUAL_SPLIT

## 1. 当前阶段与已完成 commits

当前已完成：

- 44e5a41 feat(position): close P-track manual position monitor backend
- 0557e63 feat(position): add P-track dashboard monitor UI
- cf826a0 docs(workspace): add cleanup plan after P-track commits
- 6f801f2 docs(codex): add execution guardrails for staged workspace
- 0b8353c docs(workspace): align current docs index after P-track commits
- 1697874 docs(dashboard): add residual audit after P-track UI commit

当前进入的是 commit 7 前 dashboard backend contract audit。

本阶段不是 dashboard 后端实现，不是 dashboard.html 修改，不是 schema/config 收口。

## 2. 本轮审计的 3 个后端候选文件

本轮只读审计的 dashboard 后端契约候选文件：

- src/main/java/org/example/trademodel/controller/DashboardController.java
- src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java
- src/test/java/org/example/trademodel/controller/DashboardControllerTest.java

这 3 个文件只是 dashboard 后端契约候选，不代表可以直接整组提交。

## 3. 3 个文件 diff 规模

当前 3 个文件 diff 规模：

- 合计：151 insertions(+), 9 deletions(-)
- DashboardController.java：59 insertions, 4 deletions
- DashboardDetailResponseVO.java：37 insertions
- DashboardControllerTest.java：55 insertions, 5 deletions

该规模本身不算巨大，但主题数量和依赖面已经超过一个安全小 commit 的边界。

## 4. 当前改动主题分组

当前 3 个文件混有多个主题：

- dashboard summary / detail 契约
- DashboardDetailResponseVO 字段扩展
- score-eight
- planReadiness
- assetEventTimeline
- reviewSummary / review-center
- dashboard guardrail test
- summary alert limit / title
- 测试调整

结论：这些主题不能混成一个代码 commit。

## 5. 未提交依赖矩阵

Modified:

- src/main/java/org/example/trademodel/service/ScoreService.java
- src/main/java/org/example/trademodel/service/impl/ScoreServiceImpl.java
- src/main/java/org/example/trademodel/service/MonitorService.java
- src/main/java/org/example/trademodel/service/impl/MonitorServiceImpl.java
- src/main/java/org/example/trademodel/service/ReviewService.java
- src/main/java/org/example/trademodel/service/impl/ReviewServiceImpl.java

Untracked:

- src/main/java/org/example/trademodel/service/PlanReadinessService.java
- src/main/java/org/example/trademodel/service/impl/PlanReadinessServiceImpl.java
- src/main/java/org/example/trademodel/vo/ScoreEightItemVO.java
- src/main/java/org/example/trademodel/vo/PlanReadinessVO.java
- src/main/java/org/example/trademodel/vo/PlanReadinessReasonVO.java
- src/main/java/org/example/trademodel/vo/PlanReadinessSourceFieldVO.java
- src/main/java/org/example/trademodel/vo/AssetEventTimelineItemVO.java
- src/main/java/org/example/trademodel/vo/AnalysisReviewSummaryVO.java

只提交 3 个候选文件会导致编译失败风险和 Bean 风险。

## 6. schema / config 依赖风险

本轮 3 个文件 diff 未直接要求 application.yml。

但 score-eight / reviewSummary / assetEventTimeline / planReadiness 可能间接依赖 mapper / schema / read model。

在未审计对应 Service / Mapper / schema 前，不应声明 schema 已闭合。

schema.sql 和 application.yml 不属于 commit 7。

## 7. 为什么不能直接提交这 3 个文件

不能直接提交 DashboardController / DashboardDetailResponseVO / DashboardControllerTest，原因如下：

- 多主题混杂。
- 依赖未提交 VO / Service / Impl。
- PlanReadinessService 仍是 untracked 依赖。
- DashboardControllerTest 读取 dashboard.html 模板，但 dashboard.html residual 未提交。
- 直接提交会有编译风险、Bean 风险、测试风险、边界污染风险。

因此不能直接提交这 3 个后端候选文件。

## 8. 后续推荐拆分顺序

建议后续按以下顺序推进：

1. 先提交 dashboard backend contract audit 文档冻结。
2. 再做 score-eight 独立审计。
3. 再做 planReadiness 独立审计。
4. 再做 assetEventTimeline 独立审计。
5. 再做 reviewSummary / review-center 独立审计。
6. 再做 dashboard summary/detail alert limit/title 小变更审计。
7. 每个轨道必须同时检查 Controller / VO / Service / Impl / Mapper / schema / Test。
8. 每个代码提交必须先编译，必要时跑相关测试。

## 9. 不允许的 git 操作

禁止：

- git add .
- git add src
- git add src/main/java/org/example/trademodel/controller/DashboardController.java
- git add src/main/java/org/example/trademodel/vo/DashboardDetailResponseVO.java
- git add src/test/java/org/example/trademodel/controller/DashboardControllerTest.java
- git add src/main/resources/templates/dashboard.html
- git commit -a
- 混入 schema.sql
- 混入 application.yml
- 混入 CODEX_AUTONOMOUS_TASK_QUEUE.md
- 未经审计直接 commit

## 10. 明确未完成项

以下内容未完成：

- dashboard backend contract 未收口
- DashboardController 未在本 commit 完成
- DashboardDetailResponseVO 未在本 commit 完成
- DashboardControllerTest 未在本 commit 完成
- score-eight 未在本 commit 完成
- planReadiness 未在本 commit 完成
- assetEventTimeline 未在本 commit 完成
- review-center / reviewSummary 未在本 commit 完成
- schema/config 未处理
- dashboard.html 未处理
- RuleEngine / Push / TradeReview / Opportunity 未完成

## 11. commit 7 最小边界

commit 7 只包含：

- docs/PHASE_DASHBOARD_BACKEND_CONTRACT_AUDIT_AFTER_RESIDUAL_SPLIT.md

commit 7 不包含：

- DashboardController.java
- DashboardDetailResponseVO.java
- DashboardControllerTest.java
- dashboard.html
- 任何 Java 代码
- schema.sql
- application.yml
- CODEX_AUTONOMOUS_TASK_QUEUE.md
- 其它 docs
- backups

文档禁止口径：

- 不得写 dashboard backend 已完成。
- 不得写 DashboardController 已完成。
- 不得写 score-eight 已完成。
- 不得写 planReadiness 已完成。
- 不得写 assetEventTimeline 已完成。
- 不得写 review-center 已完成。
- 不得写 schema/config 已处理。
- 不得写可以直接提交 3 个后端候选文件。
- 不得写可以直接提交 dashboard.html。
