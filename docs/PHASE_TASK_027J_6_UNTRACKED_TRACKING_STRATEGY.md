# PHASE_TASK_027J_6_UNTRACKED_TRACKING_STRATEGY

## 一、当前前提

1. 当前分支：`backup/workspace-dirty-main-20260506`。
2. `TASK-027J-5` 已完成构建前只读引用完整性审计。
3. 当前不执行 `git add`。
4. 当前不 clean（含 `git clean`）。
5. 当前不删除、不移动、不归档。
6. 当前不编译、不测试、不启动。
7. 当前不恢复功能开发。

## 二、跟踪化策略原则

1. 不按单文件随机 `git add`，避免打破能力面依赖闭环。
2. 不将所有未跟踪文件一次性纳入，避免把治理风险与证据链资产混合。
3. 不将 `backups` 纳入常规开发跟踪化候选。
4. 不将 `docs` 证据链随意删除或清理。
5. 按能力面成组纳入，保持 controller/service/mapper/entity/template/test 同步。
6. 每组纳入前必须完成：引用完整性审计、边界语义审计、最小回退策略说明。
7. `git add` 必须另立任务执行；本任务只做策略设计，不做实施。

## 三、能力面跟踪化分组

1. manual position  
2. position monitor  
3. position close / trade result / trade review  
4. rule improvement  
5. candidate readonly audit  
6. opportunity log / review aggregate  
7. watchlist audit / push eligibility  
8. execution / plan / plan boundary support  
9. page controller / templates  
10. tests  
11. test resources  
12. docs  
13. backups  
14. AGENTS.md  

## 四、建议跟踪化优先级

1. `P0`：建议优先纳入跟踪候选，缺失会破坏测试或核心能力闭环。
2. `P1`：建议能力面成组纳入，但需后续治理审计或文案护栏。
3. `P2`：建议继续冻结观察，不立即纳入。
4. `P3`：不建议纳入常规跟踪，作为证据留存或备份资产处理。

## 五、策略表字段

每组统一使用以下字段：

1. 能力面  
2. 涉及文件组  
3. 建议优先级  
4. 是否建议纳入 git 跟踪候选  
5. 是否需要成组纳入  
6. 前置条件  
7. 不纳入风险  
8. 纳入风险  
9. 回退策略  
10. 当前禁止动作  

## 六、建议初稿

| 能力面 | 涉及文件组 | 建议优先级 | 建议纳入 git 跟踪候选 | 需要成组纳入 | 前置条件 | 不纳入风险 | 纳入风险 | 回退策略 | 当前禁止动作 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| test resources | `src/test/resources/application-test.yml`、`src/test/resources/application.properties`、`mockito-extensions/*` | P0 | 是 | 是 | 已完成 J-5 引用完整性审计；确认 test profile/H2 mem 基线 | 测试回退到 dev file DB、污染 `data/`、基线漂移 | 若与已有配置冲突会暴露测试配置差异 | 仅策略阶段定义回退路径；实施任务中按清单回退 | 不 `git add`、不编译、不测试 |
| manual position + position monitor | 相关 controller/service/mapper/entity/DTO/VO/tests | P1 | 是 | 是 | 写端点语义需保持“人工事实记录/复查” | 能力面长期游离，后续构建前难做闭环验证 | 若单点纳入会造成依赖断裂 | 后续实施任务按能力面分批并保留清单化回退 | 不 `git add`、不改 Java |
| position close / trade result / trade review | 相关 controller/page/service/mapper/entity/templates/tests | P1 | 是 | 是 | 确认不反向影响 execution 主链 | 复盘链路持续未跟踪，审计成本上升 | 若与 execution 关联未隔离清楚，可能放大变更面 | 后续实施前先做边界复核 + 清单回退说明 | 不 `git add`、不启动 |
| rule improvement | `RuleImprovementSuggestion*` 全链路 | P2 | 暂不建议立即纳入 | 是 | 先补齐 operator/reason/audit 治理闭环审计结论 | 写端点长期未跟踪，治理证据分散 | 提前纳入可能让治理不足的写端点提前进入构建候选 | 待专项治理结论后再按组纳入 | 不 `git add`、不功能开发 |
| candidate readonly audit | `RuleEngineCandidateReadonlyAudit*` 全链路 | P1 | 是 | 是 | 维持只读语义与非执行边界 | 只读审计链路无法稳定复核 | 若脱离关联测试/模板单独纳入会失配 | 后续与 page/template/tests 同批纳入 | 不 `git add`、不改配置 |
| opportunity log / review aggregate | `OpportunityLog*`、`ReviewAggregate*` 相关链路 | P1（倾向） | 是 | 是 | 确认不影响 push/execution 主链 | 复盘聚合证据难持续沉淀 | 若依赖边界未定义，可能与主链耦合扩散 | 先做依赖说明后分组纳入 | 不 `git add`、不编译 |
| watchlist audit / push eligibility | `PushWatchlistConfigAudit*`、`WatchlistPushEligibility*` | P1 | 是 | 是 | 不得改变 GROUP_E watchlist gate / fail-closed 结论 | 审计链路与可观测性不稳定 | 若处理不当可能被误解为放开 gate | 实施前固定“不可改变 gate”检查单 | 不 `git add`、不改 Java |
| execution / plan / plan boundary support | plan boundary support/service、相关 VO/tests | P2 | 暂不建议立即纳入 | 是 | PARTIAL runtime 继续关闭，先完成治理边界复核 | 长期未跟踪导致边界复杂化 | 提前纳入可能被误读为可放行 runtime | 待 runtime gate 审计后再评估 | 不 `git add`、不启动 |
| page controller / templates | 各 `*PageController` + `templates/*.html` | P1 | 是 | 是 | 必须与对应 API/service 同批 | 页面与端点可能失配 | 单独纳入会出现页面/接口不一致 | 与对应能力面成组实施并回退 | 不 `git add`、不改模板 |
| tests | `src/test/java/**` | P1 | 是 | 是 | 跟能力面代码同批纳入 | 失去边界语义与引用闭合保障 | 单独纳入会造成测试与实现不匹配 | 按能力面实施计划同步回退 | 不 `git add`、不运行测试 |
| docs | `docs/*` 证据链资产 | P2 | 暂不建议立即大规模纳入 | 否（按专题） | 先做 docs 分层整理策略 | 证据链继续分散但可保留 | 一次性纳入会引入高噪声、治理困难 | 后续专题整理任务分批处理 | 不 `git add`、不 clean |
| backups | `backups/workspace-isolation/*` | P3 | 否 | 否 | 维持证据留存策略 | 常规开发不可见，但证据仍在 | 纳入常规跟踪会污染开发主线 | 仅留存策略任务处理，不进常规跟踪 | 不 `git add`、不 clean |
| AGENTS.md | `AGENTS.md` | P0（当前更建议） | 是（候选） | 否（单文件治理） | 确认与现行规则一致，不覆盖既有治理边界 | 规则源继续未跟踪，协作约束不稳定 | 若内容漂移可能引入规则冲突 | 实施任务中先做规则一致性复核再纳入 | 不 `git add`、不改内容 |

## 七、git add 执行前置条件

1. `TASK-027J-6` 仅输出策略，不执行 `git add`。
2. 真正执行 `git add` 前必须另立任务。
3. 真正执行 `git add` 前必须提交文件清单（按能力面）。
4. 真正执行 `git add` 前必须按能力面分批，不得“一次性全量”。
5. 真正执行 `git add` 前必须完成构建前只读引用完整性审计（已由 J-5 完成，实施前需复核时效）。
6. 真正执行 `git add` 前必须说明回退方式（清单级、能力面级）。
7. 真正 `git add` 后也不能直接编译/测试；编译与测试需单独验证任务。

## 八、当前禁止动作

不 `git add`、不 clean、不 reset、不 checkout、不 stash、不删除、不移动、不归档、不合并、不编译、不测试、不启动、不恢复功能开发。

## 九、后续最小任务建议

1. `TASK-027J-7`：test resources 跟踪化最小实施计划。  
2. `TASK-027J-8`：manual position / position monitor 跟踪化实施计划。  
3. `TASK-027J-9`：trade review / candidate readonly audit 跟踪化实施计划。  
4. `TASK-027J-10`：docs / backups 证据链整理策略。  
5. `TASK-027J-11`：真实启动兼容性验证前 `data/` 备份计划。  
6. `TASK-027J-12`：首轮编译前最终只读核对。  

## 十、最终结论

1. 本轮只生成未跟踪资源跟踪化策略。
2. 不执行 `git add`。
3. 不执行 `git clean`。
4. 不删除、不移动、不归档。
5. 不恢复功能开发。
6. 后续必须按能力面、按最小任务分批推进。
