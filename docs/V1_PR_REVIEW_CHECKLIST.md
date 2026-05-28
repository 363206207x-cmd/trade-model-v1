# V1 PR Review Checklist（V1 PR 审查清单）

本文件固定每次 PR（Pull Request，合并请求）审查标准。

## 1. 基础信息

每次审 PR 必须确认：

```text
PR 编号：#xxx
Issue 编号：#xxx
Branch（分支）：xxx
Base（目标分支）：main
Head commit（当前提交）：xxx
风险档位：A / B / C
```

## 2. 改动范围检查

必须检查：

- 是否只修改 Issue（问题单）和 PR（合并请求）允许的文件。
- 是否删除 placeholder（占位文件）。
- 是否新增未授权文件。
- 是否改了 Java。
- 是否改了 test（测试）。
- 是否改了 dashboard.html（首页页面）。
- 是否改了 schema.sql（数据库结构）。
- 是否改了 application.yml（配置）。
- 是否改了 controller / endpoint / API（控制器 / 接口）。
- 是否改了 service（服务）。
- 是否改了 mapper（数据库映射）。
- 是否改了 scheduler（定时器）。
- 是否改了 MarketQuoteClient（行情客户端）。

## 3. 交易边界检查

必须确认没有越界进入：

- order API（下单接口）。
- execution API（执行接口）。
- auto-trading（自动交易）。
- 自动平仓。
- 自动反手。
- 自动买入 / 自动卖出。
- 自动修改止损。
- 真实 entry / stop / TP（入场 / 止损 / 止盈）。
- 真实 RR（盈亏比）。
- ExecutionPlan readiness（执行计划可执行就绪）。
- opportunity push execution（机会推送执行）。
- production risk action（生产风控动作）。

## 4. V1 安全字段检查

凡涉及只读、风险、执行计划、候选计划、推送、扫描、持仓监控，必须确认：

- 是否保留 `manualReviewRequired=true`（必须人工复核）。
- 是否保留 `notTradeInstruction=true`（不是交易指令）。
- 是否保留 review-only（只允许复核）。
- 是否保留 fail-closed（失败关闭）。
- 是否明确不是自动交易。
- 是否明确不是交易指令。

## 5. Watchlist / Display Slots 检查

涉及观察库、首页展示位、扫描、推送时，必须确认：

- Display Slots（首页展示位）是否仍只是 UI（页面）展示优先级。
- Watchlist Pool（观察库池）是否仍是推送候选最大边界。
- 是否禁止默认六币固定推送。
- 是否禁止非观察库资产进入推送候选。
- 是否禁止把机会提升解释成下单。
- 是否禁止把机会提升解释成交易信号。
- 是否禁止把机会提升解释成 readiness（可执行就绪）。

## 6. Low-Frequency Scan（低频扫描）检查

涉及低频扫描时，必须确认：

- 是否只扫描 Watchlist Pool（观察库池）。
- 是否禁止扫描 Display Slots（首页展示位）全集。
- 是否禁止扫描非观察库资产。
- 是否禁止默认六币扫描后推送。
- 是否禁止接 MarketQuoteClient（行情客户端），除非明确授权。
- 是否禁止读取 runtime / live / external data（运行时 / 实时 / 外部数据），除非明确授权。
- 是否禁止生成 ScanScore（扫描分数），除非明确授权。
- 是否禁止生成 Candidate Attention（候选关注），除非明确授权。
- 是否禁止生成 Promote To Home（提升到首页观察），除非明确授权。
- 是否禁止 opportunity push execution（机会推送执行）。

## 7. CI（自动测试）检查

必须检查：

- CI（自动测试）是否成功。
- Compile（编译）是否成功。
- Verify（验证）是否成功。
- 若有 targeted test（目标测试），是否成功。
- 若 CI 未完成，不允许合并。
- 若 CI 失败，不允许合并，必须让 Codex 修复。

## 8. git diff 检查

必须检查：

```bash
git diff --check
```

或 GitHub CI 中等价检查。

如果发现空格错误、冲突标记、格式异常，不允许合并。

## 9. 合并判断

### A 档

满足以下条件可以由助手直接推进合并：

- 改动属于低风险。
- CI（自动测试）通过。
- 无越界。
- 无交易动作。
- 无 schema（数据库结构）漂移。
- 无 API（接口）扩散。
- 无自动交易风险。

### B 档

满足以下条件需要用户一句业务确认：

- 有新增 Java / service / scheduler skeleton / dashboard 小改。
- 技术上通过审查。
- 但可能影响用户理解或后续业务方向。

助手只问：

```text
这个 PR 是 B 档。
它做了 XXX。
没有越界。
是否同意合并？
```

### C 档

以下情况必须暂停：

- 影响真实交易方向。
- 生成真实点位。
- 升级 readiness（可执行就绪）。
- 接下单 / 执行 / 自动交易。
- 大改 schema（数据库结构）。
- 大改 dashboard（首页信息架构）。
- 删除大量代码。
- 修改核心决策链。

## 10. 审查输出模板

```text
当前状态
- PR：#xxx
- 档位：A / B / C
- CI（自动测试）：通过 / 未通过
- 是否可合并：可以 / 不建议 / 暂停

这个 PR 做了什么
- XXX

有没有越界
- Java：是 / 否
- test（测试）：是 / 否
- dashboard.html：首页页面：是 / 否
- schema（数据库结构）：是 / 否
- API（接口）：是 / 否
- MarketQuoteClient（行情客户端）：是 / 否
- 真实点位：是 / 否
- readiness（可执行就绪）：是 / 否
- 自动交易：是 / 否

我的建议
- A 档：已合并 / 可直接合并
- B 档：请用户确认业务方向
- C 档：暂停，等待明确授权
```

## 11. P291A 后新增审查项

每个 PR 必须额外检查：

- capability level 是否提升。
- 是否重复 blocked list 而没有新能力。
- PR title 是否夸大了实际 diff。
- 产品可用性是否增加。
- 是否把 review-only 误写成 no output。
- 是否有 allowed downgrade output。
- 是否把 docs-only 写成 production complete。
- 是否把 skeleton 写成 production wiring。
- 是否把 open PR / branch / Issue 写成 current main 已完成。
- 是否把 legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` 误写成新 scan-chain 已完成。

审查结论必须写明：

```text
Capability level before:
Capability level after:
Business-chain step moved:
Allowed review-only output preserved or improved:
Low-value repeat risk: yes / no
```

如果 capability level 没有提升，必须说明该 PR 是否仍有必要合并。
