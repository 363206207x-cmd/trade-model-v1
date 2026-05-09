# PHASE_WORKSPACE_GROUP_F_UNATTRIBUTED_FILES_INVENTORY_DESIGN

## 任务

- TASK-026J-1：F 组无法归因文件清单冻结设计

## 一、当前前提

1. 当前分支为 `backup/workspace-dirty-main-20260506`。
2. A/B/C/D/E 分组均已最终收口。
3. F 组只处理仍无法明确归因的剩余文件。
4. F 组不允许直接删除 / 移动 / 回滚。
5. F 组必须先清单冻结，再分子组审计。
6. 新功能开发继续暂停。
7. 大范围 dashboard / review / push 改造继续暂停。

## 二、F1 基础设施与高风险运行文件边界

1. `.gitignore`。
2. `src/main/resources/application.yml`。
3. `src/main/resources/schema.sql`。
4. `src/main/java/org/example/trademodel/service/impl/RunBaselineServiceImpl.java`。
5. `src/main/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImpl.java`。
6. `src/test/java/org/example/trademodel/service/impl/MonitorAlertWriteServiceImplTest.java`。
7. 其他未归因但可能影响编译、运行、启动、schema、配置、监控、告警、基线计算的生产文件。
8. F1 优先级最高。
9. F1 不允许直接删除。
10. F1 不允许散点回滚。
11. F1 必须逐文件审计其变更来源、是否被 A/B/C/D/E 间接依赖、是否影响编译/运行。

## 三、F1 重点风险

1. `application.yml` 可能影响启动、端口、数据源、调度、第三方配置。
2. `schema.sql` 可能影响所有表结构和测试初始化。
3. `RunBaselineServiceImpl` 可能影响分析基线、评分或决策输入。
4. `MonitorAlertWriteServiceImpl` 可能影响告警、监控、推送前置信号。
5. `.gitignore` 可能影响后续版本管理与备份产物处理。
6. F1 文件可能影响编译或运行行为。
7. F1 必须先审计后决定保留 / 拆分 / 回滚 / 文档化。

## 四、F2 文档与草案边界

1. `docs` 下未能明确归入 A/B/C/D/E 的阶段文档。
2. `docs/homepage-validation-checklist-template.md`。
3. `docs/rule-publish-schema-draft-v1.sql`。
4. `docs/db/migration-drafts/*`。
5. 中文名 `.docx` / `.txt` 文档。
6. `CODEX_*` 操作手册类文档。
7. 疑似重复 design / closure / draft 文档。
8. F2 主要风险是重复、过期、误导、归档混乱。
9. F2 不应在 F1 前处理删除。
10. F2 需要先标注：保留 / 合并 / 归档 / 待人工确认。

## 五、F2 重点风险

1. 同主题多版本文档并存导致后续执行口径混乱。
2. draft SQL 与正式 schema 混淆。
3. 中文 docx / txt 与 markdown 版本重复。
4. 操作手册类文档可能不是项目业务产物。
5. 过期文档可能误导 Codex / Cursor。
6. 不能直接删除，需先冻结清单与用途判断。

## 六、F3 备份与中间产物边界

1. `backups/workspace-isolation/*`。
2. tar / txt / patch / missing / clean 清单。
3. 工作区隔离过程生成的备份产物。
4. 打包中间产物。
5. F3 主要风险是体积、重复、污染 git status。
6. F3 不影响编译运行的概率较高，但不能未确认就删除。
7. F3 应最后处理。
8. F3 处置前必须确认备份是否已有替代、是否仍需保留。

## 七、F3 重点风险

1. 删除备份可能失去回滚证据。
2. 保留备份可能污染仓库状态。
3. patch / txt 可能包含工作区恢复线索。
4. tar 包可能重复但仍有审计价值。
5. F3 需要单独决定：保留在仓库外 / 归档 / 忽略 / 后续人工删除。

## 八、收口顺序

1. 先做 F1 基础设施与高风险运行文件逐项只读审计。
2. F1 完成后，再做 F2 文档与草案清单审计。
3. F2 完成后，再做 F3 备份与中间产物清单审计。
4. F1/F2/F3 均完成清单与处置建议后，再做 F 组总收口审计。
5. F 组总收口前，不删除、不移动、不回滚任何文件。
6. F 组总收口前，新功能开发继续暂停。

## 九、禁止事项

1. 不直接 git clean。
2. 不直接删除 untracked 文件。
3. 不直接删除 backups。
4. 不直接回滚 `application.yml`。
5. 不直接回滚 `schema.sql`。
6. 不直接回滚生产 Java 文件。
7. 不直接合并重复文档。
8. 不直接移动文档。
9. 不把草案 SQL 当正式 schema。
10. 不把备份产物当无用文件直接清理。
11. 不恢复新功能开发。
12. 不做大范围 dashboard / review / push 改造。
13. 不自动下单。
14. 不自动平仓。
15. 不自动反手。

## 十、后续审计输出要求

1. F1 必须逐文件输出：文件名 / 当前状态 / 可能归属 / 风险等级 / 是否影响编译 / 是否影响运行 / 建议动作。
2. F2 必须逐类输出：文档组 / 可能主题 / 是否重复 / 是否过期 / 建议保留方式。
3. F3 必须逐类输出：备份类型 / 生成阶段 / 是否仍需 / 是否可外部归档 / 是否建议后续人工确认删除。
4. 所有建议只能是“建议”，不得在审计阶段执行删除或移动。
