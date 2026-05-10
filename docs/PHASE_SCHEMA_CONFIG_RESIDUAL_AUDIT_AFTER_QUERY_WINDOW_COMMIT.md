# PHASE_SCHEMA_CONFIG_RESIDUAL_AUDIT_AFTER_QUERY_WINDOW_COMMIT

## 1. 阶段定位

本文档记录 commit 14 之后的 schema/config residual 分箱审计。

commit 14 已完成：

- 49ce16d fix(query): move alert window filtering to java time bounds

本阶段只做文档记录，不提交 `schema.sql`，不提交 `application.yml`，不做 Java / dashboard / schema / config 代码收口。

## 2. 当前 git 现场

- staged 为空。
- `src/main/resources/schema.sql` 有 residual：226 insertions(+), 48 deletions(-)。
- `src/main/resources/application.yml` 有 residual：16 insertions(+), 1 deletion(-)。
- 工作区仍有其它 modified / untracked 内容。

## 3. 核心结论

- 不允许整文件提交 `schema.sql`。
- 不允许整文件提交 `application.yml`。
- commit 15 不做 schema/config 代码提交。
- 当前更适合先记录 residual 分箱，后续按轨道拆分。

## 4. schema.sql residual 分箱

### A. P-track / position monitor

- 涉及 `tm_position_monitor_record`、`tm_position_trade_result`。
- 前面 P-track Java 已提交，但 schema 仍未提交。
- 可以后续单独成 P-track schema commit。
- 必须使用 index-only patch。
- 必须对照已提交 Java 字段确认匹配。

### B. Push / push recheck

- 涉及 `tm_push_watchlist_config_audit`、`tm_push_snapshot` 新索引、`tm_push_recheck_log` 新索引。
- 对应业务轨道未完整提交。
- 建议延后，或单独做 Push schema 审计。

### C. RuleEngine

- 涉及 `tm_rule_engine_candidate_readonly_audit`。
- Java 仍大量 untracked / residual。
- 必须延后。

### D. Opportunity

- 涉及 `tm_opportunity_log`。
- `OpportunityLog` 相关文件仍 untracked。
- 必须延后。

### E. TradeReview / ReviewCenter

- 涉及 `tm_position_trade_review`。
- 对应 TradeReview / ReviewCenter 仍 untracked。
- 必须延后。

### F. RuleImprovement

- 涉及 `tm_rule_improvement_suggestion`。
- 对应 RuleImprovement 文件仍 untracked。
- 必须延后。

### G. index / query performance

- 涉及 push snapshot、account risk、push recheck、monitor alert 等索引。
- 可以后续单独做 performance/index commit。
- 需要逐项确认对应 Java 查询是否已经提交。
- 不应混入业务表新增。

### H. H2 compatibility

- 包括索引 `DESC` 移除、idempotent schema 适配等。
- 可单独审计。
- 不应和业务表新增混提。

### I. 其它

- 例如 `tm_account_risk_snapshot` 新索引。
- 更偏 summary/query performance 支撑。
- 不应混入业务 schema 大包。

## 5. application.yml residual 分箱

### A. H2 file DB / datasource 本地配置

- `jdbc:h2:mem` 改为 `jdbc:h2:file:./data/...`。
- 属于本地持久化开发配置。
- 直接提交到主 `application.yml` 风险高。
- 更适合 example 或 local profile。

### B. schema init / SQL 初始化

- 新增 `spring.sql.init.mode: always`。
- 依赖 schema idempotency。
- 可能影响启动行为。
- 需单独配置审计。

### C. Plan Boundary feature flag

- 新增 `plan.boundary.partial.runtime.enabled`。
- 属于 Plan Boundary 轨道。
- 不应混入 schema/config commit。

### D. server / port / profile

- 当前未见 server / port / profile 变更。

### E. 其它

- 注释强调本地状态，不是业务数据。
- 仍不适合直接提交主配置。

## 6. 潜在后续拆分路线

1. P-track schema only commit
   - 只纳入 `tm_position_monitor_record` / `tm_position_trade_result`。
   - 必须 index-only patch。
   - 必须对照已提交 P-track Java 字段。
   - 需 compile / test-compile / P-track 相关测试。

2. performance/index only commit
   - 只纳入已经有 Java 查询支撑的索引。
   - 不新增业务表。
   - 不改配置。

3. H2 compatibility schema commit
   - 只处理 H2 兼容表达。
   - 不混业务 schema。

4. application local profile / example config commit
   - 不直接改主 `application.yml`。
   - 如需要，新增 `application-local.example.yml` 或文档说明。

5. Push / RuleEngine / Opportunity / TradeReview / RuleImprovement schema
   - 分别等对应 Java 轨道进入提交阶段时再做。

## 7. 风险提示

- 整文件提交 `schema.sql` 会混入 Push、RuleEngine、Opportunity、TradeReview、RuleImprovement、索引优化、H2 兼容。
- 整文件提交 `application.yml` 会混入本地 H2 file DB、SQL init、Plan Boundary flag。
- schema 与 Java 代码不同步可能导致启动失败或 mapper 查询失败。
- 本地配置误提交可能改变所有人的启动行为。
- schema/config 必须使用 index-only patch，不允许整文件 add。

## 8. 当前不做什么

- 不提交 `schema.sql`。
- 不提交 `application.yml`。
- 不修改 Java。
- 不修改 `dashboard.html`。
- 不处理 review-center / TradeReview。
- 不处理 RuleEngine / Push / Opportunity。
- 不实现 Risk Action Guard。
- 不引入自动交易语义。
- 不运行编译/测试。

## 9. 建议的 commit 15 主题

建议 commit message：

```text
docs(schema): document schema config residual split
```

## 10. 后续建议

commit 15 若提交，只提交本审计文档。

commit 16 之后再决定是否做 P-track schema only 或 dashboard.html residual 审计。
