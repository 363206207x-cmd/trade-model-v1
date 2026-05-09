# PHASE_WORKSPACE_GROUP_F2_CURRENT_DOC_INDEX_FREEZE

## 任务

- TASK-026J-35：F2 当前有效文档索引与口径冻结

---

## 一、当前前提

1. 当前分支为 `backup/workspace-dirty-main-20260506`。
2. 已完成并提交的当前基线：
   - `44e5a41 feat(position): close P-track manual position monitor backend`
   - `0557e63 feat(position): add P-track dashboard monitor UI`
   - `cf826a0 docs(workspace): add cleanup plan after P-track commits`
   - `6f801f2 docs(codex): add execution guardrails for staged workspace`
3. A/B/C/D/E/F1 相关 closure 文档仅作为历史阶段证据读取，不代表剩余工作区已全部完成。
4. F2 文档清单与 F3 backups / intermediate inventory 已作为 workspace cleanup evidence 提交。
5. F3 cleanup / 删除 / 移动 / final closure 尚未执行，不能据此认为 backups 或 intermediate files 已清理完成。
6. 本轮不删除、不移动、不归档、不合并任何文档。
7. 新功能开发继续暂停，RuleEngine / Push / TradeReview / Opportunity / RuleImprovement 均属于 deferred independent tracks。

---

## 二、后续 Codex / Cursor 优先读取顺序

1. `AGENTS.md`
2. `docs/V1_FRAMEWORK_LOCK.md`
3. `docs/CODEX_PHASE_EXECUTION_RULE.md`
4. `docs/CODEX_TASK_HANDOFF_TEMPLATE.md`
5. `docs/CURSOR_RISK_HANDOFF_AND_UNFINISHED_SCOPE.md`
6. `docs/PHASE_WORKSPACE_GROUP_F_UNATTRIBUTED_FILES_INVENTORY_DESIGN.md`
7. `docs/PHASE_TASK_027J_6_UNTRACKED_TRACKING_STRATEGY.md`
8. `docs/PHASE_WORKSPACE_GROUP_F2_DOCS_DRAFTS_DUPLICATES_INVENTORY_FREEZE.md`
9. `docs/PHASE_WORKSPACE_GROUP_F3_BACKUPS_INTERMEDIATE_INVENTORY_FREEZE.md`
10. `docs/PHASE_WORKSPACE_GROUP_F2_CURRENT_DOC_INDEX_FREEZE.md`
11. `docs/CODEX_AUTONOMOUS_TASK_QUEUE.md`：defer / needs refresh；暂不作为当前有效主口径，后续必须单独刷新、审计、再决定是否提交。

---

## 三、当前有效主口径文档

1. `AGENTS.md`：仓库协作和执行规则入口。
2. `docs/V1_FRAMEWORK_LOCK.md`：V1 框架锁定口径。
3. `docs/CODEX_PHASE_EXECUTION_RULE.md`：Codex 阶段执行规则。
4. `docs/CODEX_TASK_HANDOFF_TEMPLATE.md`：任务交接模板。
5. `docs/CURSOR_RISK_HANDOFF_AND_UNFINISHED_SCOPE.md`：Cursor 风险交接与未完成范围索引。
6. `docs/PHASE_WORKSPACE_GROUP_F_UNATTRIBUTED_FILES_INVENTORY_DESIGN.md`：未归属工作区文件 inventory 设计。
7. `docs/PHASE_TASK_027J_6_UNTRACKED_TRACKING_STRATEGY.md`：untracked 文件追踪策略。
8. `docs/PHASE_WORKSPACE_GROUP_F2_DOCS_DRAFTS_DUPLICATES_INVENTORY_FREEZE.md`：docs drafts / duplicates inventory freeze。
9. `docs/PHASE_WORKSPACE_GROUP_F3_BACKUPS_INTERMEDIATE_INVENTORY_FREEZE.md`：backups / intermediate inventory freeze。
10. `docs/PHASE_WORKSPACE_GROUP_F2_CURRENT_DOC_INDEX_FREEZE.md`：本索引文档。

---

## 四、当前已提交基线

1. `44e5a41 feat(position): close P-track manual position monitor backend`：P 轨 manual position / position monitor 后端闭合。
2. `0557e63 feat(position): add P-track dashboard monitor UI`：P 轨 dashboard 最小 UI 集成。
3. `cf826a0 docs(workspace): add cleanup plan after P-track commits`：workspace cleanup / inventory / tracking strategy 文档。
4. `6f801f2 docs(codex): add execution guardrails for staged workspace`：AGENTS / Codex / V1 执行规则入口。

---

## 五、暂缓参考文档

1. `docs/CODEX_AUTONOMOUS_TASK_QUEUE.md` 当前归类为 defer / needs refresh。
2. 该文件暂不作为当前有效主口径。
3. 该文件后续必须单独刷新、审计、再决定是否提交。
4. 在刷新前，不得用该文件覆盖已提交 commit、AGENTS、V1 framework lock、Codex execution rules 或 workspace cleanup evidence。

---

## 六、仅作审计证据的文档

1. 各类 `PHASE_*_DESIGN` 文档。
2. 各类 `PHASE_*_AUDIT` 文档。
3. 各类 `PHASE_*_CLOSURE` 中已被 FINAL_CLOSURE 汇总覆盖的中间文档。
4. 各类 `PHASE_*_FREEZE_INDEX` 文档。
5. 历史迁移审计文档。
6. 这些文档不得优先于当前用户指令、AGENTS、V1 framework lock、Codex execution rules 或已提交 workspace cleanup evidence 使用。
7. 若 design 与 closure 冲突，以已提交 closure / inventory / workspace cleanup evidence 为准。
8. 若 audit 与已提交 evidence 冲突，以已提交 evidence 为准。

---

## 七、草案 / 非正式文档

1. `docs/rule-publish-schema-draft-v1.sql`。
2. `docs/db/migration-drafts/*`。
3. 任何文件名含 draft / migration-drafts / schema-draft 的文档。
4. 草案 SQL 不得视为正式 schema。
5. 正式 schema 仍以 `src/main/resources/schema.sql` 与 `docs/PHASE_WORKSPACE_GROUP_F1_SCHEMA_FINAL_CLOSURE.md` 为准。
6. 草案 SQL 不得直接执行。
7. migration draft 不代表已执行迁移。
8. 后续如需启用草案，必须单独立项、重新审计、编译测试。

---

## 八、待人工确认文档

1. `docs/homepage-validation-checklist-template.md`。
2. `docs/overview-api-contract.md`。
3. `docs/baseline-snapshot-2026-04-27.md`。
4. `docs/dashboard-refresh-deprecation-plan.md`。
5. 中文 `.docx` 文件。
6. 中文 `.txt` 文件。
7. 其他非 markdown 需求资产。
8. 待人工确认文档本轮不删除、不移动、不归档。
9. 若其内容与 final closure 冲突，以 final closure 为准。

---

## 九、中文 docx / txt 资产口径

1. 中文 docx / txt 可能包含原始需求，具有审计价值。
2. 中文 docx / txt 不作为 Codex / Cursor 当前优先执行口径。
3. 中文 docx / txt 若与 markdown closure 冲突，以 markdown final closure 为准。
4. 后续如需继续使用，应单独建立"中文资产 ↔ 当前主文档"映射。
5. 本轮不转换格式、不删除、不移动。

---

## 十、Homepage / Checklist / Overview / Snapshot 口径

1. homepage validation checklist 暂为待人工确认。
2. overview-api-contract 可能仍有契约参考价值，但需人工确认是否被后续 closure 替代。
3. baseline snapshot 可能仍有历史基线价值，但不得优先于当前 final closure。
4. dashboard refresh deprecation plan 需与 D 组 final closure 对齐。
5. 这些文件本轮不删除、不移动、不归档。

---

## 十一、后续工具读取规则

1. Codex / Cursor 执行新任务前，应优先读取当前有效主口径文档。
2. 不得从 draft SQL 推导正式 schema。
3. 不得从旧 design 覆盖已提交 closure / inventory / workspace cleanup evidence。
4. 不得从中文 docx / txt 直接生成代码，除非用户明确指定并重新冻结。
5. 不得从 checklist / snapshot 直接覆盖已收口结论。
6. 遇到口径冲突时，优先级为：用户当前指令 > `AGENTS.md` / `docs/V1_FRAMEWORK_LOCK.md` / `docs/CODEX_PHASE_EXECUTION_RULE.md` > 已提交 handoff / workspace cleanup evidence > 已提交 closure / inventory > design / audit / draft > `docs/CODEX_AUTONOMOUS_TASK_QUEUE.md` 仅在刷新后恢复参考。
7. 如果仍冲突，必须停止并请求人工确认。
8. 不得自动删除冲突文档。
9. 不得自动移动冲突文档。
10. 不得自动合并冲突文档。

---

## 十二、F2 / F3 阶段关系

1. F2 当前文档风险已通过索引方式降级。
2. F2 暂不执行删除、移动、归档、合并。
3. F2 仍保留待人工确认项。
4. F2 可视为完成"清单冻结 + 当前有效口径索引修正"。
5. F3 inventory evidence 已存在并已作为 workspace cleanup evidence 提交。
6. F3 cleanup / 删除 / 移动 / final closure 仍需后续独立审计，不代表 backups 或 intermediate files 已清理完成。
7. 后续若需要真正归档，必须单开归档设计与人工确认任务。

---

## 十三、Deferred independent tracks

1. RuleEngine 属于 deferred independent track。
2. Push 属于 deferred independent track。
3. TradeReview 属于 deferred independent track。
4. Opportunity 属于 deferred independent track。
5. RuleImprovement 属于 deferred independent track。
6. 上述轨道不得借本文档进入实现阶段。
7. 上述轨道后续必须各自独立审计、独立拆分、独立提交。

---

## 十四、下一阶段建议

1. 继续按独立 commit 审计 dashboard residual audit。
2. 继续按独立 commit 审计 docs index / docs closure follow-up。
3. 继续按独立 commit 审计 workspace cleanup follow-up。
4. 继续按独立 commit 审计 RuleEngine independent audit。
5. 继续按独立 commit 审计 Push independent audit。
6. 继续按独立 commit 审计 TradeReview independent audit。
7. 继续按独立 commit 审计 Opportunity independent audit。
8. 继续按独立 commit 审计 RuleImprovement independent audit。
9. F 组完成前不恢复新功能开发。
10. F 组完成前不执行 git clean / 删除 backups / 删除 untracked 文档。
