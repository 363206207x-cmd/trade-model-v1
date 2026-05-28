# V1 Operator Workflow Contract（V1 操作工作流契约）

本文件是 Trade Model V1 的长期工作流宪法。以后新聊天窗口、Codex、PR 审查和阶段推进，都必须先读取本文件，再读取 `docs/V1_CURRENT_STATE.md`，然后再继续工作。

## 1. 使用者约束

- 用户不是程序员。
- 全程优先使用中文。
- 如必须使用英文术语，必须在英文后补中文解释，例如 `Pull Request（合并请求）`、`CI（自动测试）`、`merge（合并）`。
- 回复必须直接、可执行、少解释。
- 不要让用户判断代码细节、CI、schema、mapper、controller、service、测试或专业术语。
- 用户只负责业务方向确认，不负责技术审查。
- 每轮只做一个明确交付物；在不越界时，默认按最大安全任务包组织。
- 不要平台化、不要发散、不要一次推进多个功能。

## 2. 新窗口启动规则

新窗口第一步必须读取：

1. `docs/V1_OPERATOR_WORKFLOW_CONTRACT.md`
2. `docs/V1_CURRENT_STATE.md`
3. `docs/V1_CODEX_TASK_TEMPLATE.md`
4. `docs/V1_PR_REVIEW_CHECKLIST.md`
5. `docs/PROJECT_PROGRESS_INDEX.md`

读取后只输出短确认：

```text
工作流契约：已读取
当前阶段：已确认
本轮交付物：XXX
风险等级：A / B / C
是否需要用户业务授权：是 / 否
```

如果当前阶段、交付物、风险等级或授权要求不清楚，不允许创建 Issue（问题单）或 PR（合并请求）。

## 3. 固定工作顺序

每一轮必须按以下顺序：

1. 判断下一步最大安全任务包；如遇授权门或风险跨档，则拆分并暂停。
2. 查重 open Issue（未关闭问题单）。
3. 查重 open PR（未合并请求）。
4. 查重 branch（分支）。
5. 如果无重复，创建 Issue（问题单）。
6. 创建 branch（分支）。
7. 创建 placeholder / marker（占位文件）。
8. 创建 Draft PR（草稿合并请求）。
9. 输出 Codex 任务包。
10. 用户执行 Codex 后，只需发：`审 PR #xxx`。
11. 助手审查 PR（合并请求）。
12. 按 A/B/C 档决定是否合并或请求用户业务授权。
13. 合并后给用户本地同步命令。
14. 用户贴回 `git status` 和 `git log --oneline -5`。
15. 确认 main（主分支）clean（干净）后继续下一步。

## 4. 最大安全任务包规则

后续默认任务包不是“能拆多小就拆多小”的微型拆分模式，而是 maximum safe task pack（最大安全任务包）。

同一风险档位、同一模块 / 同一业务轨道、同一验证方式、不会跨越 Authorization Gate（授权门）的内容，应尽量合并成一个 Codex 任务。

不要把安全可合并的工作拆成大量微型任务，避免用户反复复制粘贴。

A/B/C 授权规则优先于提速规则。最大安全任务包不能跨越到未授权的 B 档、C 档或 Critical（关键高风险）边界。

最大安全任务包不等于允许接入真实交易、runtime data（运行时数据）、`MarketQuoteClient`（行情客户端）、push execution（推送执行）、Readiness（可执行就绪）、order API（下单接口）、execution API（执行接口）或 auto-trading（自动交易）。

如果任务内存在任何 C 档内容，必须拆出并暂停，等待用户明确授权。

输出给用户复制的命令、Codex prompt（Codex 提示词）、`gh` 命令，必须尽量作为一个完整可复制块输出，不要拆散成多段。

用户不负责判断代码细节，只负责业务方向确认；技术审查由助手完成。

## 5. A/B/C 合并授权规则

### A 档：低风险，可由助手审查通过后直接推进合并

包括：

- 文档类 PR（合并请求）。
- checklist（检查清单）。
- verification（验证文档）。
- closure（收口文档）。
- scope audit（范围审计）。
- authorization gate（授权门）。
- freeze doc（冻结文档）。
- PROJECT_PROGRESS_INDEX（项目进度索引）刷新。
- placeholder / marker（占位文件）。
- 小型 DTO（数据传输对象）。
- enum（枚举）。
- fail-closed test（失败关闭测试）。
- 不影响 runtime（运行时）的测试补充。
- 不改 schema（数据库结构）、不改 API（接口）、不改交易逻辑、不接自动交易的小改动。

处理方式：助手审查范围、CI（自动测试）、越界和风险后，可以直接推进合并；如平台拦截，则只给用户一条本地 `gh` 命令。

### B 档：中风险，助手审查后只向用户请求一句业务方向确认

包括：

- 新 service（服务）。
- 新 validator（验证器）。
- 新 controller endpoint（控制器接口），但只读。
- mapper（数据库映射）只读方法。
- dashboard.html（首页页面）小改。
- SourceTrace（证据来源追踪）主链增强。
- BoundaryCandidate（边界候选交易计划）主链增强。
- ExecutionPlan（执行计划）只读增强。
- RiskActionGuard（风险动作保护器）接入前置。
- Push / Watchlist（推送 / 观察库）逻辑增强。
- disabled-by-default scheduler skeleton（默认关闭的定时器骨架）。

处理方式：助手给用户三句话：

```text
这个 PR 做了什么：XXX。
有没有越界：没有 / 有。
我的建议：可以合并 / 不建议合并。请确认业务方向。
```

用户只需回复：`同意合并` 或 `不同意，先不合并`。

### C 档：高风险，必须用户明确授权，不允许自动合并

包括：

- 真实 entry / stop / TP（入场 / 止损 / 止盈）数值生成。
- 真实 RR（盈亏比）生成。
- RuleEngine（规则引擎）主链接入。
- RiskActionGuard（风险动作保护器）正式接生产决策链。
- 影响综合决策方向的规则。
- 推送触发逻辑。
- opportunity push execution（机会推送执行）。
- 手动持仓风险推送。
- 强反转处理逻辑。
- 移动止损真实计算。
- schema（数据库结构）大改。
- dashboard（首页）信息架构大改。
- 删除大量代码或大规模重构。
- order API（下单接口）。
- execution API（执行接口）。
- 交易所写接口。
- auto-trading（自动交易）。

处理方式：必须暂停合并，解释业务方向影响，等待用户明确授权。

## 6. PR 审查输出格式

审 PR 时固定输出：

```text
当前状态
- PR：#xxx
- 档位：A / B / C
- CI（自动测试）：通过 / 未通过
- 是否可合并：可以 / 不建议 / 暂停

这个 PR 做了什么
- 一句话说明

有没有越界
- 是否改 Java
- 是否改 schema（数据库结构）
- 是否改 dashboard.html（首页页面）
- 是否接 API（接口）
- 是否接 MarketQuoteClient（行情客户端）
- 是否生成真实点位
- 是否升级 readiness（可执行就绪）
- 是否接自动交易

我的建议
- A 档：助手直接合并 / 给本地命令
- B 档：用户只需确认业务方向
- C 档：暂停，等用户明确授权
```

## 7. 禁止事项

除非 Issue（问题单）和授权门明确允许，否则禁止：

- 自动下单。
- 自动平仓。
- 自动反手。
- 自动修改止损。
- 生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 升级 ExecutionPlan readiness（执行计划可执行就绪）。
- 接 order API（下单接口）。
- 接 execution API（执行接口）。
- 接 auto-trading（自动交易）。
- 把 Display Slots（首页展示位）当作 Watchlist Pool（观察库池）。
- 默认六币固定推送。
- 非观察库资产进入机会推送候选。
- 在踩踏状态推送机会、反手或新开仓。
- 把短线插针当作趋势反转。

## 8. 本地同步命令

每次合并后给用户这条命令：

```bash
cd /Users/xuchao/Documents/trade-model-v1 && git switch main && git pull origin main && git status && git log --oneline -5
```

## 9. 新窗口唯一迁移提示词

以后新窗口只需要发送：

```text
继续 Trade Model V1 当前工作流。
请先以 docs/V1_OPERATOR_WORKFLOW_CONTRACT.md 为最高工作流准则，
再读取 docs/V1_CURRENT_STATE.md 判断当前阶段。
不要总结，不要发散，不要重新解释规则。
每轮只做一个最大安全任务包；如遇授权门或风险跨档，则拆分并暂停。
先查重 open Issue / PR。
如果无重复，创建 Issue、branch、marker、Draft PR。
最后输出：当前状态 / 下一步 / Issue-PR-Branch / Codex 任务包 / 审查结论。
```

## 10. P291A 工作流重置规则

P291A 之后，项目推进必须从“只设边界”切换为“安全但可用的 review-only MVP 链路”。

### 10.1 Business-chain MAX_SAFE_PACK

优先按业务链路推进最大安全任务包，而不是按 closure / authorization / still-blocked 小碎包推进。

默认业务链路为：

1. Watchlist candidate source
2. Market read request wiring
3. Market read adapter
4. scan output
5. review-only ScanScore
6. review-only Candidate
7. internal Opportunity Push preview
8. Push Recheck
9. review-only Execution Advice
10. entry / stop / TP / RR proposal
11. manual position entry
12. position monitoring
13. AI conflict downgrade
14. dashboard MVP smoke
15. review / missed-valid logging

### 10.2 Closure-only 限制

禁止把 closure-only / still-blocked-only 作为默认下一步。

docs-only closure 和下一步 authorization 如果风险档位一致，默认合并为一个 Scope Pack。

连续两个 docs-only 包之后，下一包必须提升能力层级，除非跨风险档位或用户明确要求继续文档审计。

### 10.3 Capability Level Required

每个任务必须声明提升哪个 capability level，使用：

- `0 NOT_STARTED`
- `1 DOCS_ONLY_GATE`
- `2 SKELETON`
- `3 TARGETED_TEST`
- `4 TEST_ONLY_WIRING`
- `5 REVIEW_ONLY_RUNTIME`
- `6 PRODUCTION_WIRING`
- `7 PRODUCTION_READY`

如果没有提升能力层级，必须解释为什么仍值得做。

### 10.4 Merged Main Only

只有 merged `main` 算完成。

open PR、branch、Issue、draft PR、聊天记忆、计划文档都不能写成已完成。

### 10.5 Review-only Allowed Output Principle

review-only 不等于 no output。

允许 safe / manual-review / not-trade-instruction 的输出，例如 entry zone proposal、stop zone proposal、TP proposal、RR estimate、position size suggestion、leverage cap suggestion、invalidation condition、reduce position、tighten stop、move stop、partial take-profit、wait for trigger、plan invalidated、internal push preview、risk downgraded candidate、confused with recovery condition。

禁止 automatic order、automatic close、automatic reverse、automatic leverage change、automatic execution 和 auto-trading。

## 11. 本文件优先级

本文件是工作流最高准则。若聊天记忆、旧提示词、旧文档和本文件冲突，优先按本文件执行。项目业务框架仍以 `docs/PROJECT_PROGRESS_INDEX.md` 和阶段文档为准。
