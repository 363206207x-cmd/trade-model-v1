# PHASE_WORKSPACE_GROUP_F3_BACKUPS_INTERMEDIATE_INVENTORY_FREEZE

## 任务

- TASK-026J-39：F3 backups / 中间产物清单冻结文档

---

## 一、当前前提

1. 当前分支为 `backup/workspace-dirty-main-20260506`。
2. F1 已最终收口。
3. F2 已最终收口。
4. F3 只读审计已完成。
5. 本轮只新增 F3 清单冻结文档。
6. 本轮不删除、不移动、不归档、不合并任何文件。
7. 本轮不执行 git clean。
8. 新功能开发继续暂停。

---

## 二、F3 范围确认

1. F3 范围与 F 组无法归因清单设计一致。
2. F3 核心对象为 `backups/workspace-isolation/*`。
3. F3 扩展关注 tar / patch / diff / missing / clean / tmp / temp 类中间产物。
4. F3 关注中文路径 / 中文命名文件。
5. F3 关注疑似重复备份。
6. F3 关注可能仍有恢复价值的备份。
7. F3 关注可作为审计证据保留的中间产物。
8. F3 当前不做实际处置。

---

## 三、backups/workspace-isolation 清单冻结

代表文件包括：

1. `workspace-status-short-20260506.txt`
2. `workspace-tracked-diff-20260506.patch`
3. `workspace-tracked-files-20260506.txt`
4. `workspace-untracked-files-20260506.txt`
5. `workspace-untracked-files-20260506.tar.gz`
6. `workspace-untracked-files-20260506-clean.txt`
7. `workspace-untracked-files-20260506-clean.tar.gz`
8. `workspace-untracked-files-20260506-cjk-decoded.txt`
9. `workspace-untracked-files-20260506-cjk-missing-after-decode.txt`
10. `workspace-untracked-files-20260506-cjk.tar.gz`
11. `workspace-untracked-files-20260506-missing.txt`

当前判断：

1. 整体建议保留为审计证据。
2. 不建议立即删除。
3. 不建议立即移动。
4. 不建议立即归档。
5. 不建议立即合并。
6. 后续可作为归档候选，但必须人工确认。

---

## 四、tar / patch / diff / missing / clean / tmp / temp 类文件边界

1. `.tar.gz` 文件可能包含未跟踪文件备份。
2. `.patch` 文件可能包含 tracked diff 恢复证据。
3. missing / clean / cjk 相关 txt 可能记录清理或编码处理过程。
4. `pom.xml.backup` / `pom.xml.tmp` 属根目录临时 / 备份命名候选。
5. `target/` 属构建产物，通常可再生成，但本轮不删除。
6. `.git/hooks/*sample` 属 grep 噪声，不作为业务备份处置对象。
7. 所有此类文件本轮仅冻结边界，不处置。

---

## 五、中文路径 / 中文命名文件边界

1. `docs/` 下中文 `.docx` / `.txt` 已在 F2 中归为中文资产。
2. 中文文件名具有原始需求或审计价值。
3. cjk 相关 tarball / txt 说明曾存在中文路径或编码处理问题。
4. 中文 docs 与备份之间的对应关系需后续人工确认。
5. 本轮不删除、不移动、不转换格式。

---

## 六、疑似重复备份结论

1. `workspace-untracked-files-20260506` 多个 tar.gz 版本存在同源多次导出可能。
2. clean / cjk / full 版本可能存在内容重叠。
3. 多份 txt 清单可能存在重复或分轨记录。
4. 疑似重复不等于可以删除。
5. 后续若要精简，必须先做只读校验，例如 shasum 或解包 diff。
6. 校验任务必须单独立项，且不得默认删除。

---

## 七、建议保留的审计证据

1. `workspace-tracked-diff-20260506.patch`
2. `workspace-status-short-20260506.txt`
3. `workspace-tracked-files-20260506.txt`
4. `workspace-untracked-files-20260506.txt`
5. `workspace-untracked-files-20260506-missing.txt`
6. cjk decoded / missing 清单
7. 至少一份完整 untracked tar.gz
8. clean 版本与 cjk 版本在确认冗余前均保留。

---

## 八、归档候选

1. 重复 tarball 变体。
2. clean 与 full 并存且后续校验一致时的次要副本。
3. cjk 与 full 并存且后续校验一致时的次要副本。
4. 根目录 `pom.xml.backup` / `pom.xml.tmp` 若确认无恢复价值，可列为后续处置候选。
5. `target/` 构建产物可列为后续本地清理候选。
6. 所有归档候选均需人工确认。
7. 本轮不执行归档。

---

## 九、待人工确认项

1. 是否需要保留全部三份 tar.gz。
2. tar.gz 之间是否存在唯一信息差异。
3. clean / cjk / missing 清单是否仍有唯一审计价值。
4. `pom.xml.backup` 是否仍有恢复价值。
5. `pom.xml.tmp` 是否可删除。
6. `target/` 是否仅按本地构建产物处理。
7. 中文 docs 与 cjk 备份的对应关系。
8. 是否需要后续建立备份文件索引。

---

## 十、禁止立即删除项

1. 禁止立即删除 `backups/workspace-isolation/`。
2. 禁止立即删除 `workspace-tracked-diff-20260506.patch`。
3. 禁止立即删除 `workspace-untracked-files-20260506*.tar.gz`。
4. 禁止立即删除 `workspace-untracked-files-20260506*.txt`。
5. 禁止立即批量删除未跟踪 docs。
6. 禁止立即批量删除未跟踪 backups。
7. 禁止立即执行 git clean。
8. 禁止基于文件名“像临时文件”直接删除。

---

## 十一、git clean 禁止边界

1. 当前不建议执行 git clean。
2. F 组总收口完成前禁止执行 git clean。
3. 未经人工确认禁止执行 `git clean -fd`。
4. 未经人工确认禁止执行 `git clean -fdx`。
5. 不得用 git clean 批量清除 untracked 文档或 backups。
6. 如未来需要清理，必须先有清单、人工确认、备份策略与回滚说明。

---

## 十二、F3 对代码与主链路影响

1. F3 只处理 backups / 中间产物清单。
2. F3 不修改 Java。
3. F3 不修改 schema。
4. F3 不修改 dashboard / review。
5. F3 不修改 push / execution / position 主链路。
6. F3 不修改 Risk Action Guard。
7. F3 未引入自动下单 / 平仓 / 反手语义。
8. 误删文件才可能产生工程风险，因此当前禁止清理。

---

## 十三、F3 拆分建议

建议后续可拆为：

1. F3-A `backups/workspace-isolation` 清单冻结。
2. F3-B 根目录临时 / 备份命名文件确认，例如 `pom.xml.backup` / `pom.xml.tmp`。
3. F3-C `target/` 与构建报告策略确认。
4. F3-D 重复 tarball 只读校验。

当前冻结文档仅记录分组，不执行拆分任务。

---

## 十四、F3 阶段结论

1. F3 只读审计已完成。
2. F3 当前必须先冻结清单。
3. F3 不建议立即删除、移动、归档、合并任何文件。
4. F3 不建议立即执行 git clean。
5. F3 总体风险来自误删和无法恢复。
6. F3 完成清单冻结后，可进入 F3 总收口审计。
7. F 组完成前继续暂停新功能开发。

---

## 十五、下一阶段建议

1. 将本文件作为 F3 清单冻结文档。
2. 下一步进入 F3 总收口审计。
3. 不要在 F3 总收口前做归档或删除。
4. 后续如要做 shasum / 解包 diff，应单独立项，只读执行。
5. F 组完成前继续禁止 git clean。
6. F 组完成前继续禁止删除 backups。
7. F 组完成前继续禁止删除 untracked 文档。
8. F 组完成前不恢复新功能开发。

---

## 本轮交付自检（TASK-026J-39）

### 十七、是否修改既有 docs / Java / schema / Mapper / Service / Controller / dashboard / review / application.yml / .gitignore

否。本轮仅新增本文件，未修改上述任何项。

### 十八、是否移动 / 删除 / 归档 / 合并任何文件

否。

### 十九、是否运行编译 / 测试

否（按任务约束本轮不运行）。

### 二十、是否触碰禁止事项

否。未触碰用户所列禁止范围（含未修改既有 docs、未动 backups、未执行 git clean / 分支切换等）。
