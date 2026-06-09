# Minimal Review-Only DecisionResult Runtime Wiring Verification

# 1. Executive Summary

#876 验证通过。

- endpoint 是否可用: Yes。`GET /api/dashboard/decision-result-status?symbol=BTCUSDT` 已由 `DashboardControllerTest` 覆盖，full test 通过。
- dashboard panel 是否存在: Yes。`decisionResultStatusPanel` 已在 `dashboard.html` 中存在，并由 dashboard template test 覆盖。
- status mapping 是否完整: Yes。七个 `DECISIONRESULT_*` 状态均可在 controller / dashboard / tests 中定位。
- fail-closed 是否可验证: Yes。missing DecisionResult、read-model partial、source trace partial、`ai_role_results` partial 均由 tests 覆盖；unknown / blocked 状态作为默认安全状态存在。
- 是否有 Push / Candidate / Decision generation / Point / Trading 越界: No。验证包没有改 Java / test / dashboard；#876 endpoint 是只读 status read，不触发生成、候选、点位、Push 或交易。
- 是否可以进入 visual verification / closure: Yes。
- 下一允许动作: `DecisionResult Visual Verification / Closure`。

# 2. Verification Commands

| Command | Result |
|---|---|
| `git status --short` | PASS: worktree clean before docs update |
| `git branch --show-current` | PASS: `decisionresult-runtime-wiring-verification` |
| `git log --oneline --decorate --all --max-count=120` | PASS: local branch at `0c7d4d4 feat(decision): show review-only runtime status (#876)` |
| `bash scripts/v1-state.sh` | PASS: branch clean, `MAIN_SYNC: OK`; open PR unknown only because local `gh` unavailable |
| `bash scripts/v1-session-bootstrap.sh` | PASS: confirmed next step was DecisionResult verification and highlighted source-of-truth docs needed updating from implementation to verification |
| `bash scripts/check-workflow-contract.sh` | PASS: `WORKFLOW_CONTRACT_OK` |
| `./mvnw -q -DskipTests compile` | PASS |
| `./mvnw -q -DskipTests test-compile` | PASS |
| `./mvnw -q test` | PASS |
| `grep -R "decision-result-status\|decisionResultStatusPanel\|DECISIONRESULT_" -n src/main/java src/main/resources src/test/java docs \| head -1000` | Executed as requested; local basic grep treats `\|` literally, so a supplemental `grep -R -E` was used for evidence |
| `grep -R -E "decision-result-status\|decisionResultStatusPanel\|DECISIONRESULT_" -n src/main/java src/main/resources src/test/java docs \| head -1000` | PASS: found endpoint, DOM panel, status mapping, tests, and implementation docs |
| `grep -R "placeOrder\|createOrder\|submitOrder\|auto-trading\|order execution\|entry / stop / TP\|final direction" -n src/main/java src/main/resources src/test/java docs \| head -300 \|\| true` | Executed as requested |
| `grep -R -E "placeOrder\|createOrder\|submitOrder\|auto-trading\|order execution\|entry / stop / TP\|final direction" -n src/main/java src/main/resources src/test/java docs \| head -300 \|\| true` | PASS with known historical / negative guard references only; no new Java/test/dashboard changes in this verification package |
| `git diff --check` | PASS |
| `git diff --cached --check` | PASS |
| `git diff --check main...HEAD` | PASS before docs update; final diff check must remain clean after this docs package |

# 3. Endpoint Verification

| Endpoint | Method | Purpose | Trigger generation? | Trading semantics? | Result |
|---|---|---|---|---|---|
| `/api/dashboard/decision-result-status?symbol=BTCUSDT` | GET | DecisionResult review-only runtime status over existing `DecisionService` / `DecisionResultVO` owner path | No. It reads existing persisted DecisionResult read model only. | No. It returns safety flags and excludes executable fields. | PASS |

Endpoint verification details:

- endpoint exists in `DashboardController` as `@GetMapping("/api/dashboard/decision-result-status")`;
- endpoint is read-only and uses existing `DecisionService.getLatestDecisionResultBySymbol`;
- endpoint does not write DecisionResult, candidate, point, push, order, or execution state;
- endpoint does not trigger Decision generation;
- endpoint does not generate Candidate;
- endpoint does not generate Point;
- endpoint does not send Push;
- endpoint does not execute trading;
- API smoke is covered by `DashboardControllerTest` and full `./mvnw -q test`.

# 4. Dashboard Verification

| DOM id | Location | Shows status? | Shows review-only copy? | Shows boundary copy? | Shows forbidden action? | Result |
|---|---|---:|---:|---:|---:|---|
| `decisionResultStatusPanel` | After Evidence / Score status panel and before the main workbench | Yes | Yes | Yes, Watchlist / MarketQuote / Evidence-Score and Display Slots boundary copy | No entry/stop/TP/RR, no Push button, no order action | PASS |

Dashboard verification details:

- `decisionResultStatusPanel` exists in `dashboard.html`;
- dashboard fetches `/api/dashboard/decision-result-status`;
- dashboard shows DecisionResult status, symbol, analysisId, decision availability, confidence, `ai_role_results` availability / summary, source trace, source health, and fail-closed status;
- dashboard copy states DecisionResult is review-only and not a trading signal;
- dashboard copy states it is not Candidate, not new Decision generation, and not Point;
- dashboard copy states Watchlist / MarketQuote / Evidence-Score boundaries still apply;
- dashboard copy states Display Slots are not a candidate pool;
- dashboard does not add entry / stop / TP / RR display in this panel;
- dashboard does not add Push or order actions in this panel.

# 5. Status Mapping Verification

| Status | Verified? | Fail-closed? | Source | Notes |
|---|---:|---:|---|---|
| `DECISIONRESULT_REVIEW_ONLY_READY` | Yes | No | `DashboardController`, `dashboard.html`, `DashboardControllerTest` | Ready path returns review-only status when owner-path read is complete. |
| `DECISIONRESULT_MISSING_FAIL_CLOSED` | Yes | Yes | `DashboardController`, `DashboardControllerTest` | Missing DecisionResult returns `decisionAvailable=false` and `failClosed=true`. |
| `DECISIONRESULT_READ_MODEL_PARTIAL` | Yes | Yes | `DashboardController`, `DashboardControllerTest` | Partial read model returns fail-closed status. |
| `DECISIONRESULT_SOURCE_TRACE_PARTIAL` | Yes | Yes | `DashboardController`, `DashboardControllerTest` | Missing source trace anchors return fail-closed status. |
| `DECISIONRESULT_AI_ROLE_PARTIAL` | Yes | Yes | `DashboardController`, `dashboard.html`, `DashboardControllerTest` | Missing `ai_role_results` returns partial status; not Three AI expansion. |
| `DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED` | Yes | Yes | `DashboardController`, `dashboard.html`, `DashboardControllerTest` | Unknown create time / read-model completeness maps to safe fail-closed. |
| `DECISIONRESULT_BLOCKED_FAIL_CLOSED` | Yes | Yes | `DashboardController`, `dashboard.html` | Default blocked status before owner-path read succeeds. |

# 6. Test Coverage Verification

- controller/API smoke test: Yes, `DashboardControllerTest` covers `/api/dashboard/decision-result-status`.
- dashboard model/template test: Yes, template test covers `decisionResultStatusPanel`, endpoint URL, and safety copy.
- status mapping test: Yes, ready, missing, read-model partial, source trace partial, and AI role partial paths are covered.
- missing DecisionResult fail-closed test: Yes.
- `ai_role_results` partial test: Yes.
- forbidden semantics test: Yes, API response tests assert executable candidate / decision / point / trading fields are not exposed.
- no Push / Candidate / Decision generation / Point / Trading grep check: Yes. Supplemental `grep -R -E` shows only known historical / negative guard references outside this docs-only verification diff.

# 7. Boundary Verification

- 是否接 Push：No
- 是否接 external channel：No
- 是否生成 Candidate：No
- 是否生成新的 Decision：No
- 是否生成 Point：No
- 是否生成 final direction：No
- 是否输出 entry/stop/TP/RR：No
- 是否接 order/execution/auto-trading：No
- 是否新增 DTO/Validator/Assembler：No
- 是否改 schema/config/pom：No
- 是否继续 P359/P360：No
- 是否提升 capability level：No

# 8. Final Recommendation

明确结论：verification 通过，可以进入 `DecisionResult Visual Verification / Closure`。

下一允许动作：只做浏览器视觉验证和 closure，确认 dashboard 中 `decisionResultStatusPanel` 真实可见、copy 清楚、无 layout overlap、无 Push / Candidate / Decision generation / Point / Trading 语义。

为什么仍是 `REVIEW_ONLY_RUNTIME partial`：#876 只是把已有 persisted DecisionResult read model 以只读 API/dashboard status 展示给人工 review；它不写入、生成、发送或执行任何业务动作。

为什么不是 Production Wiring：没有生产决策生成链路、候选链路、点位链路、Push 通道、订单/执行链路或自动交易链路。

为什么不是 Push / Candidate / Decision generation / Point / Trading：endpoint 和 dashboard panel 均只显示 review-only status，不生成 Candidate，不生成新的 Decision，不生成 entry / stop / TP / RR，不生成 final direction，不发送 Push，不接 order / execution / auto-trading。
