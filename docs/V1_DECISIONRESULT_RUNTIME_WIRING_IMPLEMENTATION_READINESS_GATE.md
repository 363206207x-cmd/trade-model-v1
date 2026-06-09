# Minimal Review-Only DecisionResult Runtime Wiring Implementation Readiness Gate

# 1. Executive Summary

结论：**GO 到最小 implementation**。

GO 的原因是 #873 source read 和 #874 design 已经确认现有 `tm_decision_result` / `DecisionResultMapper` / `DecisionService` / `DecisionResultVO` / `DashboardController` summary-detail / dashboard detail assets 足够作为 DecisionResult read-model owner path。当前缺口不是 owner path 缺失，而是缺少 dedicated review-only status endpoint / panel；这个缺口可以在下一包以最小只读方式补齐。

最小 implementation 允许做：

- 新增或复用一个最小 DecisionResult review-only status endpoint；
- 新增或复用一个最小 dashboard status panel；
- 只显示 existing read-model status、symbol、analysisId、decision availability、confidence、`ai_role_results` availability、source trace、source health、failClosed 和安全边界 flags；
- 使用 `Map` / existing `DecisionResultVO` / existing dashboard response object，不新增 DTO；
- 添加 targeted controller/dashboard tests；
- 更新 source-of-truth docs。

最小 implementation 禁止做：

- 生成 Candidate；
- 生成新的 Decision；
- 生成 Point；
- 生成 final direction；
- 输出 entry / stop / TP / RR、position size、leverage、order action；
- 接 Push external channel；
- 接 order / execution / auto-trading；
- 新增 DTO / Validator / Assembler / Orchestrator；
- 修改 schema / config / pom；
- 继续 P359 / P360。

是否需要新增 DTO / Validator / Assembler：**No**。

是否需要改 schema：**No**。

是否需要接 Push / Candidate / Decision generation / Point / Trading：**No**。

下一允许动作：**Minimal Review-Only DecisionResult Runtime Wiring Implementation**。

# 2. Readiness Gate Result

Decision: **A. GO: Minimal Review-Only DecisionResult Runtime Wiring Implementation**。

下一步只能是 implementation，且 implementation 仍必须是 review-only。它必须复用 existing `DecisionResult` / `DecisionService` / `DecisionResultMapper` / `tm_decision_result` / dashboard summary-detail assets；不得新增 DTO / Validator / Assembler；不得改 schema；不得接 Push / Candidate / Decision generation / Point / Trading。

GO 的具体理由：

- `DecisionResult` owner path exists: `tm_decision_result`、`DecisionResultMapper`、`DecisionServiceImpl`、`DecisionResultVO` 都已存在。
- Existing API exists: `/api/dashboard/summary` 与 `/api/dashboard/detail` 已能读取 DecisionResult read model。
- Dedicated status endpoint missing: 这是下一包最小实现的合理目标，而不是阻塞项。
- Existing dashboard assets exist: dashboard decision shells、source trace、read-model fallback、AI role rendering、workbench display 已存在。
- Existing tests exist: `DashboardControllerTest`、`DecisionServiceImplTest`、`DecisionResultMapperLatestPlanIntegrationTest`、dashboard display adapter tests 可作为 targeted test 基础。
- Existing risky fields are known: `entryZone`、`stopLoss`、`takeProfitRules`、`leverageSuggestion`、`positionSuggestion` 等必须排除在 status fields 之外或明确 context-only。

# 3. Required Future Implementation Boundary

如果 GO，未来 implementation 只允许：

- 新增或复用一个最小 DecisionResult review-only status endpoint；
- 新增或复用一个最小 dashboard status panel；
- 显示 `status` / `symbol` / `analysisId` / `decisionAvailable` / `confidence` / `ai_role_results availability` / `source trace` / `source health` / `failClosed`；
- 明确 `reviewOnly=true`；
- 明确 `notTradingSignal=true`；
- 明确 `notCandidateSignal=true`；
- 明确 `notDecisionGeneration=true`；
- 明确 `notPointSignal=true`；
- 明确 `watchlistBounded=true`；
- 明确 `marketQuoteChecked=true`；
- 明确 `evidenceScoreChecked=true`；
- 明确 `displaySlotsAreCandidatePool=false`。

未来 implementation 不允许：

- 生成 Candidate；
- 生成新的 Decision；
- 生成 Point；
- 生成 final direction；
- 输出 entry / stop / TP / RR；
- 输出 position size；
- 输出 leverage；
- 接 Push external channel；
- 接 order / execution / auto-trading；
- 新增 DTO / Validator / Assembler / Orchestrator；
- 修改 schema / config / pom；
- 继续 P359 / P360。

# 4. Status Mapping Readiness

| Status | 是否可用现有资产判断 | 数据来源 | 缺口 | Implementation 是否允许落地 | 是否 fail-closed |
|---|---|---|---|---|---|
| `DECISIONRESULT_REVIEW_ONLY_READY` | Yes | `DecisionService.getLatestDecisionResultBySymbol` / `DecisionResultVO` / `DashboardController` detail | 需要最小 status mapping 汇总 `decisionId`、`analysisId`、`symbol`、`readModelTruthStatus`、source trace 和 safety flags | Yes，只读落地 | No |
| `DECISIONRESULT_MISSING_FAIL_CLOSED` | Yes | `DecisionService.getLatestDecisionResultBySymbol` 返回 null 或 `DecisionResultMapper` 无 latest row | 需要 endpoint 将 null 明确映射为 missing / failClosed，而不是让 dashboard 误读为空白 | Yes，只读落地 | Yes |
| `DECISIONRESULT_READ_MODEL_PARTIAL` | Yes | `DecisionResultVO.readModelTruthStatus` / `readModelFallbackReason` | 需要把 `PARTIAL` 与 `LEGACY_MISSING:*` 显示为 read-model partial，不显示为可执行结论 | Yes，只读落地 | Yes |
| `DECISIONRESULT_SOURCE_TRACE_PARTIAL` | Partial | `DashboardSourceTraceDetailAdapter` / `SourceTraceDTO` / dashboard detail safe defaults | source trace completeness 需要在最小 status 中只取 existing detail/source-trace summary，不新增 source owner | Yes，只读落地 | Yes |
| `DECISIONRESULT_AI_ROLE_PARTIAL` | Yes | `tm_decision_result.ai_role_results` / `DecisionResultVO.aiRoleResults` / dashboard AI role rendering | 需要只显示 present/missing，不解释为 Three AI final arbiter | Yes，只读落地 | Yes |
| `DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED` | Partial | `DecisionResultVO.createTime` / `DecisionResultMapper.selectLastDecisionTime` | stale threshold 未统一；下一包可先将 createTime missing / unknown 映射为 fail-closed，不做复杂 freshness 规则 | Yes，只读落地；复杂 stale threshold 不允许扩大 | Yes |
| `DECISIONRESULT_BLOCKED_FAIL_CLOSED` | Yes | symbol normalization failure、owner path exception、read-model ambiguity | 需要在 endpoint/dashboard 显示 blocked reason 并保持 safety flags | Yes，只读落地 | Yes |

# 5. Existing Asset Readiness

| Asset | Exists? | Reusable? | Needs new DTO? | Needs schema change? | Risk | Decision |
|---|---:|---:|---:|---:|---|---|
| DecisionResult | Yes | Yes | No | No | Existing fields include action-looking text; must remain read-model context only | Reuse |
| DecisionService | Yes | Yes | No | No | `DecisionServiceImpl` enriches quote/position read-model; status must not become trading signal | Reuse |
| mapper | Yes | Yes | No | No | Joined plan fields include entry/stop/TP/leverage-like fields | Reuse only for read-only status; avoid exposing point/trade fields |
| schema | Yes | Yes | No | No | `tm_decision_result` already has fields including `ai_role_results`; no schema changes needed | Reuse |
| ai_role_results | Yes | Yes | No | No | Can be misread as Three AI arbiter | Show availability only; no Three AI expansion |
| dashboard summary/detail API | Yes | Yes | No | No | Existing APIs mix broad read-model fields | Reuse or add tiny dedicated status endpoint with safe field subset |
| dashboard template/assets | Yes | Yes | No | No | No dedicated `decisionResultStatusPanel` yet | Add minimal status/copy only if implementation touches dashboard |
| tests | Yes | Yes | No | No | Missing dedicated DecisionResult status tests | Extend targeted controller/dashboard tests only |
| source trace/provenance | Yes/Partial | Yes | No | No | Source trace can be partial; ambiguity must fail closed | Reuse existing detail/source trace summary |
| fail-closed flags | Partial | Yes | No | No | Adjacent display adapters have safe defaults; DecisionResult status needs explicit flag | Add status-level `failClosed` |
| review-only flags | Partial | Yes | No | No | Existing detail copy has not-trade-instruction semantics; dedicated DecisionResult status needs explicit flags | Add status-level review-only flags |

# 6. Test Readiness

未来 implementation 最小测试范围：

- controller/API smoke test；
- dashboard template existence / model attribute test；
- status mapping unit/controller test；
- fail-closed missing DecisionResult test；
- `ai_role_results` partial test；
- forbidden semantics test；
- no Push / Candidate / Decision generation / Point / Trading test or grep check。

Implementation 后合并前还必须跑：

- `bash scripts/check-workflow-contract.sh`
- `./mvnw -q -DskipTests compile`
- `./mvnw -q -DskipTests test-compile`
- targeted controller/dashboard/DecisionService tests
- `git diff --check`
- forbidden path check
- forbidden semantics grep

# 7. Boundary With Existing Completed Slices

未来 implementation 不得绕过：

- Watchlist / RuleConfig；
- MarketQuote freshness / fallback；
- Evidence / Score status；
- Display Slots boundary；
- source trace / source health；
- fail-closed ambiguity rule。

DecisionResult status 只能说明“已有 persisted read model 是否可读、是否完整、是否只读安全”。它不能让不在 Watchlist Pool 的资产进入候选链路，不能忽略 MarketQuote stale/missing/fallback，也不能忽略 Evidence / Score incomplete 状态。

# 8. Explicit No-Overreach Confirmation

- 是否接 Push：No
- 是否接 external channel：No
- 是否生成 Candidate：No
- 是否生成新的 Decision：No
- 是否生成 Point：No
- 是否生成 final direction：No
- 是否输出 entry/stop/TP/RR：No
- 是否接 order/execution/auto-trading：No
- 是否继续 P359/P360：No
- 是否新增 DTO/Validator/Assembler：No
- 是否改 schema/config/pom：No
- 是否提升 capability level：No, readiness only

# 9. Final Recommendation

明确结论：**GO 到 Minimal Review-Only DecisionResult Runtime Wiring Implementation**。

下一允许动作是最小 implementation：只允许新增或复用一个只读 DecisionResult status endpoint、最小 dashboard status panel/copy、targeted tests 和 source-of-truth docs；必须复用 existing `DecisionService` / `DecisionResultMapper` / `DecisionResultVO` / schema / dashboard assets。禁止新增 DTO / Validator / Assembler，禁止改 schema/config/pom，禁止接 Push / Candidate / Decision generation / Point / Trading，禁止输出 entry / stop / TP / RR、final direction、position size、leverage 或 order action。

合并前必须满足 workflow contract、compile、test-compile、targeted tests、diff checks、forbidden path check、forbidden semantics grep。它仍是 `REVIEW_ONLY_RUNTIME partial`，因为本 gate 只授权下一步展示已有 DecisionResult read-model 状态，不授权 Production Wiring、Push、Candidate generation、Decision generation、Point generation 或 Trading。
