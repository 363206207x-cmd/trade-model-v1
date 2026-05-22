# BACKEND-P151 Production Wiring Readiness Audit

## 1. 这一步是干嘛的

P151 是 Production Wiring（真正接入系统运行链路）之前的 Readiness Audit（是否允许进入下一步的审计）。

这一步只回答一个问题：P140-P150 已经完成的范围门、缺口审计、输入契约、测试计划、INCOMPLETE（证据不完整状态）测试、BLOCKED（禁止推进状态）测试，是否已经足够让项目直接进入真实系统接线。

P151 的结论是：还不够直接进入完整 Production Wiring（真正接入系统运行链路）。

P151 本轮只做文档审计：

- 不写 Java。
- 不新增测试。
- 不接真实系统运行链路。
- 不读取 runtime data（运行时数据）。
- 不读取 live market data（实时行情）。
- 不接 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不生成 VALID（有效候选状态）。
- 不升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不打开 dashboard readiness（页面显示可执行状态）。
- 不接 order / execution / automation / auto-trading（下单 / 执行 / 自动化 / 自动交易）。

## 2. 检查范围

P151 检查 P140-P150 的结果。

已检查的阶段如下：

- P140：Production Wiring Preparation Scope Gate（真正接线前的范围门）
  - 定义未来接线前可以审计什么、仍然禁止什么、哪些前置条件必须存在。
  - 结论是 P140 只定义范围，不授权实现。
- P141：SourceTrace Runtime Gap Audit（证据来源运行时缺口审计）
  - 确认 SourceTrace（证据来源追踪）字段和部分 DTO / fixture 能力已经存在。
  - 同时确认 runtime SourceTrace population（运行时证据来源填充）仍然缺失。
- P142：Source-Owned Candidate Input Contract（证据来源候选输入契约）
  - 定义 Candidate（候选交易计划）输入必须有 owner、source ref、timeframe、window、freshness、rule id、rule version、reason、conflict state。
  - 定义 entry / stop / TP / RR 的 numeric source ownership（数值来源归属）要求。
- P143：Source-Owned Candidate Design Matrix（证据来源候选设计矩阵）
  - 把 P142 契约拆成 input family（输入家族）矩阵。
  - 结论是没有任何 family 已经足够支持 source-owned runtime candidate generation（运行时证据来源候选生成）。
- P144：Source-Owned Candidate Test Plan（证据来源候选测试计划）
  - 规划未来测试类别。
  - 明确 P144 不新增测试、不授权 production implementation（生产实现）。
- P145：INCOMPLETE guard test 授权门
  - 只授权一个最小 INCOMPLETE（证据不完整状态）guard test。
  - 不授权 BLOCKED（禁止推进状态）、VALID（有效候选状态）、readiness（是否允许进入下一步）、production wiring（真正接入系统运行链路）。
- P146：INCOMPLETE guard test 实现
  - 新增 `SourceOwnedCandidateIncompleteGuardTest`。
  - 证明缺少 source-owned evidence（证据来源归属证据）时必须 fail closed（失败时保持关闭）到 INCOMPLETE（证据不完整状态）。
- P147：INCOMPLETE guard test 收口
  - 确认 P145-P146 只覆盖 INCOMPLETE（证据不完整状态）fail-closed。
  - 确认没有进入 BLOCKED、VALID、readiness 或 production wiring。
- P148：BLOCKED guard test 授权门
  - 只授权一个最小 BLOCKED（禁止推进状态）guard test。
  - 不授权 broad substitution suite（大范围替代数据测试）、Risk Action Guard（风险动作保护器）、VALID（有效候选状态）、production wiring（真正接入系统运行链路）。
- P149：BLOCKED guard test 实现
  - 新增 `SourceOwnedCandidateBlockedGuardTest`。
  - 证明证据存在但冲突、不安全、被禁止时必须保持 BLOCKED（禁止推进状态）。
- P150：BLOCKED guard test 收口
  - 确认 P148-P149 的 BLOCKED（禁止推进状态）测试线完成。
  - 推荐 P151 作为只读 / 文档审计，判断是否足以授权第一根真实运行链路。

## 3. 已经具备的安全保护

当前已经具备以下安全保护：

- INCOMPLETE（证据不完整状态）不能推进。
- BLOCKED（证据冲突 / 不安全 / 被禁止状态）不能推进。
- `manualReviewRequired=true`（必须人工复核）。
- `notTradeInstruction=true`（不是交易指令）。
- `reviewMode=REVIEW_ONLY`（只允许复核）。
- 不会生成 trade instruction（交易指令）。
- 不会生成 executable surface（可执行入口）。
- 不会生成 readiness surface（可执行就绪状态）。
- 不会生成 order / execution / automation surface（下单 / 执行 / 自动化入口）。

这些保护说明：系统已经能在 DTO / fixture 层面证明两类基础失败状态不会被误推进：

- 缺证据、缺字段、无法判断时，保持 INCOMPLETE（证据不完整状态）。
- 证据存在但冲突、不安全、被禁止时，保持 BLOCKED（禁止推进状态）。

这是一层必要保护，但还不是完整 Production Wiring（真正接入系统运行链路）授权。

## 4. 还缺什么

P151 审计确认，现在仍然缺以下关键内容：

- 真实运行时 SourceTrace（证据来源追踪）怎么填充。
- 谁在真实运行时提供 source owner（证据来源所有者）。
- 谁在真实运行时提供 source ref（证据来源引用）。
- 谁在真实运行时提供 source timeframe（证据来源周期）。
- 谁在真实运行时提供 source window（证据来源窗口）。
- entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）的证据来源从哪里来。
- 哪个 service（服务）负责生成 Candidate（候选交易计划）。
- 什么时候仍然必须 INCOMPLETE（证据不完整状态）。
- 什么时候仍然必须 BLOCKED（禁止推进状态）。
- 什么时候可以进入下一步的 review-only path（只复核路径），但仍然不允许 VALID（有效候选状态）。
- Runtime SourceTrace（运行时证据来源追踪）和 DTO / fixture 测试之间的真实连接还没有定义。
- BoundaryCandidateService（边界候选服务）的 production VALID path（生产有效候选路径）仍然没有授权。
- ExecutionPlan readiness（执行计划是否允许进入下一步）现在不能升级。
- dashboard readiness（页面显示可执行状态）现在不能打开。

最重要的缺口是：现在已经有防止错误推进的测试，但还没有定义第一根真实接线到底从哪里开始、允许改哪些文件、运行时证据从哪里来、如何保持 REVIEW_ONLY（只允许复核）。

## 5. 是否允许下一步开始真实接线

P151 给出明确结论：

- 不允许直接接完整 Production Wiring（真正接入系统运行链路）。
- 不允许直接生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不允许直接生成 VALID（有效候选状态）。
- 不允许直接调用 `BoundaryCandidateDTO.valid(...)`。
- 不允许直接升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不允许直接打开 dashboard readiness（页面显示可执行状态）。
- 不允许直接接 order / execution / automation / auto-trading（下单 / 执行 / 自动化 / 自动交易）。

可以允许的下一步只有更小的 scope gate（范围门）：

- 先定义第一根最小真实接线路径。
- 先定义允许审计和未来可能修改的文件。
- 先定义运行时 SourceTrace（证据来源追踪）如何被填充。
- 先定义所有输出如何继续保持 REVIEW_ONLY（只允许复核）。
- 先定义哪些情况必须继续 INCOMPLETE（证据不完整状态）或 BLOCKED（禁止推进状态）。

第一根真实接线即使未来被授权，也必须仍然只输出 REVIEW_ONLY（只允许复核），不能输出交易指令，不能输出 VALID（有效候选状态），不能输出 readiness（可执行就绪状态）。

## 6. 推荐下一步

推荐下一步是：

```text
P152: First Production Wiring Scope Gate
```

中文解释：P152 是 First Production Wiring Scope Gate（第一根真实接线范围门）。

P152 应该继续是文档范围门，不直接写代码。

P152 应该只定义第一根真实接线允许改哪些文件、允许读哪些已有代码、仍然禁止哪些路径。P152 要明确第一根真实接线从哪里开始。建议第一根真实接线只围绕 SourceTrace runtime population（运行时证据来源填充）方案展开，不允许生成真实交易点位。

P152 仍然必须禁止：

- VALID（有效候选状态）。
- 真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）生成。
- ExecutionPlan readiness（执行计划是否允许进入下一步）升级。
- dashboard readiness（页面显示可执行状态）打开。
- order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

STOP 也是合法选择。如果不继续进入 P152，当前系统仍然保持 read-only（只读）、review-only（只复核）、non-actionable（不可操作）的安全姿态。

## 7. 仍然禁止的路径

以下路径在 P151 之后仍然禁止：

- production candidate generation（生产候选交易计划生成）
- source-owned runtime candidate generation（运行时证据来源候选生成）
- real entry / stop / TP / RR value generation（真实入场 / 止损 / 止盈 / 盈亏比生成）
- production VALID mapping（生产环境映射为有效候选）
- BoundaryCandidateService VALID production path（边界候选服务生产有效路径）
- `BoundaryCandidateDTO.valid(...)` production calls（生产环境调用 valid）
- ExecutionPlan readiness upgrade（执行计划升级为可执行）
- dashboard readiness mutation（页面显示可执行状态）
- `dashboard.html` changes（页面改动）
- controller / endpoint / API wiring（接口接线）
- schema / config / service / mapper changes（数据库 / 配置 / 服务 / 映射改动）
- runtime data reads（读取运行时数据）
- live market data reads（读取实时行情）
- external data integration（接外部数据）
- exchange clients（交易所客户端）
- `WebClient` / `RestTemplate`（网络请求工具）
- order API（下单接口）
- execution API（执行接口）
- scheduler / automation / auto-trading（定时器 / 自动化 / 自动交易）

## 8. P151 边界确认

P151 本轮只完成一个审计文档：

- 新增 `docs/PHASE_BACKEND_P151_PRODUCTION_WIRING_READINESS_AUDIT.md`。
- 删除 `docs/P151.md`。

P151 本轮确认：

- 不新增 Java。
- 不新增测试。
- 不改 production Java（生产代码）。
- 不改现有测试。
- 不改 `dashboard.html`。
- 不新增 controller / endpoint / API / schema / config / service / mapper。
- 不读取 runtime data（运行时数据）。
- 不读取 live market data（实时行情）。
- 不读取 external data（外部数据）。
- 不生成真实 entry / stop / TP / RR（入场 / 止损 / 止盈 / 盈亏比）。
- 不调用 `BoundaryCandidateDTO.valid(...)`。
- 不升级 ExecutionPlan readiness（执行计划是否允许进入下一步）。
- 不接 order / execution / scheduler / automation / auto-trading（下单 / 执行 / 定时器 / 自动化 / 自动交易）。

P151 stops here. P151 不合并 PR，不进入 production wiring（真正接入系统运行链路）。
