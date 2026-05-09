# PHASE_WORKSPACE_GROUP_F2_DOCS_DRAFTS_DUPLICATES_INVENTORY_FREEZE

## 任务

- TASK-026J-34：F2 文档 / 草案 / 重复文件清单冻结文档

---

## 一、当前前提

1. 当前分支为 `backup/workspace-dirty-main-20260506`。
2. F1 已最终收口。
3. F2 当前仅做文档清单冻结。
4. F3 与新功能开发继续暂停。
5. 本轮不删除、不移动、不归档、不合并任何文档。

---

## 二、F2 文档分组

1. F2-01 治理与流程手册组。
2. F2-02 阶段收口与设计主干组。
3. F2-03 迁移与草案 SQL 组。
4. F2-04 Homepage / Checklist 组。
5. F2-05 中文 docx / txt 资产组。
6. F2-06 根目录通用说明组。

---

## 三、F2-01 治理与流程手册组

### 代表文件

1. `docs/CODEX_CLI_CURSOR_OPERATION_GUIDE.md`
2. `docs/CODEX_TASK_HANDOFF_TEMPLATE.md`
3. `docs/CODEX_PHASE_EXECUTION_RULE.md`

### 当前判断

1. 建议保留。
2. 属于执行口径与协作流程核心。
3. 不建议归档。
4. 不建议删除。

### 风险

1. 若存在旧版操作手册，可能误导 Codex / Cursor。
2. 后续可建立「当前有效手册索引」，但本轮不实施。

---

## 四、F2-02 阶段收口与设计主干组

### 代表文件

1. `docs/PHASE_WORKSPACE_GROUP_F_UNATTRIBUTED_FILES_INVENTORY_DESIGN.md`
2. `docs/PHASE_WORKSPACE_GROUP_F1_INFRA_HIGH_RISK_FINAL_CLOSURE.md`
3. `docs/PHASE_PLAN_BOUNDARY_STRUCTURED_*` 系列
4. 其他 `PHASE_*_DESIGN` / `PHASE_*_CLOSURE` / `PHASE_*_AUDIT` / `PHASE_*_FREEZE_INDEX` 文档

### 当前判断

1. 保留与重复候选并存。
2. closure 类文档优先级通常高于 design / draft。
3. design / audit 类文档作为审计证据保留。
4. 后续建议建立主索引与替代关系。
5. 本轮不合并、不归档、不删除。

### 风险

1. 同主题多版本并存导致执行口径混乱。
2. Codex / Cursor 可能读取旧 design 而忽略最新 closure。
3. 后续需要「当前有效文档索引」。

---

## 五、F2-03 迁移与草案 SQL 组

### 代表文件

1. `docs/rule-publish-schema-draft-v1.sql`
2. `docs/db/migration-drafts/V2__rule_publish_schema.sql`
3. `docs/PHASE_V1_BASELINE_MIGRATION_DRAFT_AUDIT.md`

### 当前判断

1. 保留作为审计证据。
2. 归档候选。
3. 草案 SQL 不得视为正式 schema。
4. 正式 schema 仍以 `src/main/resources/schema.sql` 与 F1-Schema closure 为准。
5. 本轮不移动、不删除、不归档。

### 风险

1. 草案 SQL 可能被误用于正式开发。
2. migration draft 可能被误认为已执行迁移。
3. 后续必须补「草案 / 非正式 / 不得直接执行」标记或索引说明。

---

## 六、F2-04 Homepage / Checklist 组

### 代表文件

1. `docs/homepage-validation-checklist-template.md`

### 当前判断

1. 待人工确认。
2. 可能仍有验收模板价值。
3. 可能已被 dashboard / review closure 替代。
4. 本轮不删除、不移动。

### 风险

1. 若旧 checklist 与当前首页冻结方向冲突，可能误导后续 UI 修改。
2. 后续需确认是否仍使用。

---

## 七、F2-05 中文 docx / txt 资产组

### 代表文件

1. `docs/UI 设计说明书.docx`
2. `docs/复盘与 AI 冲突处理统一落地方案.txt`
3. `docs/持仓监控完整方案.docx`

### 当前判断

1. 待人工确认。
2. 归档候选。
3. 可能与 markdown 主文档存在重复或旧版本差异。
4. 本轮不删除、不移动、不转换格式。

### 风险

1. 中文 docx / txt 可能包含原始需求，应保留审计价值。
2. 也可能与当前 closure 口径冲突。
3. 后续需建立「中文资产 ↔ 当前主文档」映射关系。

---

## 八、F2-06 根目录通用说明组

### 代表文件

1. `docs/overview-api-contract.md`
2. `docs/baseline-snapshot-2026-04-27.md`
3. `docs/dashboard-refresh-deprecation-plan.md`

### 当前判断

1. 待人工确认。
2. 部分可能仍是有效契约。
3. 部分可能已被后续 closure 替代。
4. 本轮不删除、不移动。

### 风险

1. 通用说明类文档容易被误读为当前最终口径。
2. 需要后续确认是否仍为有效入口文档。

---

## 九、F2 阶段结论

1. F2 不建议立即删除、移动、归档、合并任何文档。
2. F2 当前建议先冻结清单，再分组审计。
3. F2 总体风险等级为中到高。
4. 最大风险是旧文档误导后续 Codex / Cursor。
5. 后续需要建立「当前有效文档索引」。
6. 后续需要标注草案 SQL 与正式 schema 边界。
7. 后续需要确认中文 docx / txt 与 markdown 主文档关系。
8. 后续需要确认 checklist / overview / snapshot 是否仍有效。

---

## 十、后续处理建议

1. 下一步优先做 F2 文档索引 / 有效口径冻结。
2. 暂不归档。
3. 暂不删除。
4. 暂不移动。
5. 暂不合并。
6. F2 完成前不进入 F3。
7. F 组完成前不恢复新功能开发。
8. F 组完成前不执行 git clean / 删除 backups / 删除 untracked 文档。

---

## 十一、本轮执行记录（TASK-026J-34）

- 仅新增本文件：`docs/PHASE_WORKSPACE_GROUP_F2_DOCS_DRAFTS_DUPLICATES_INVENTORY_FREEZE.md`。
- 未修改 Java、schema.sql、Mapper、Service、Controller、`dashboard.html`、`review.html`、`application.yml`、`.gitignore`。
- 未运行编译与测试。
- 未移动、删除、归档、合并任何既有文档。
