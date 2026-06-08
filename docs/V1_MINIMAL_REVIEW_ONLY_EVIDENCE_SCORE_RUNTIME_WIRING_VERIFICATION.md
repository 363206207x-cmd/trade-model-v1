# Minimal Review-Only Evidence / Score Runtime Wiring Verification

# 1. Executive Summary

#869 验证通过。`/api/dashboard/evidence-score-status?symbol=BTCUSDT` 可用，runtime smoke 返回 HTTP 200，并包含 Evidence / Score review-only 状态字段。

Dashboard Evidence / Score status panel 可见，`/dashboard` smoke 返回 HTTP 200，并包含 `evidenceScoreStatusPanel`、API 路径、只读边界、不是 Candidate / Decision / Point、Watchlist / MarketQuote 边界、Display Slots 不是候选池等文案。

本包仍然是 review-only verification。没有新增 DTO / Validator / Assembler，没有接 Push / Candidate / Decision / Point / Trading，没有生成候选、方向、点位或交易动作。当前 capability level 保持 `REVIEW_ONLY_RUNTIME partial`；Evidence / Score slice 经 #869 和本次 verification 后可以标记为 `REVIEW_ONLY_RUNTIME partial`，下一步应做 `Evidence / Score visual verification / closure`。

# 2. Verification Matrix

| Check | Result | Evidence |
|---|---|---|
| workflow contract | PASS | `bash scripts/check-workflow-contract.sh` -> `WORKFLOW_CONTRACT_OK` |
| compile | PASS | `./mvnw -q -DskipTests compile` completed successfully |
| test-compile | PASS | `./mvnw -q -DskipTests test-compile` completed successfully |
| DashboardControllerTest | PASS | `./mvnw -q -Dtest=DashboardControllerTest test` completed successfully |
| EvidenceServiceImplTest | PASS | `./mvnw -q -Dtest=EvidenceServiceImplTest test` completed successfully |
| ScoreServiceImplTest | PASS | `./mvnw -q -Dtest=ScoreServiceImplTest test` completed successfully |
| ReviewAggregateServiceImplEvidenceTopItemsTest | PASS | `./mvnw -q -Dtest=ReviewAggregateServiceImplEvidenceTopItemsTest test` completed successfully |
| ReviewAggregateServiceImplScoreTopItemsTest | PASS | `./mvnw -q -Dtest=ReviewAggregateServiceImplScoreTopItemsTest test` completed successfully |
| API smoke `/api/dashboard/evidence-score-status` | PASS | HTTP 200 for `?symbol=BTCUSDT`; returned required fields and review-only flags |
| dashboard smoke `/dashboard` | PASS | HTTP 200; HTML contains `evidenceScoreStatusPanel`, API path, Evidence / Score safety copy, Watchlist / MarketQuote boundary, and Display Slots boundary |
| no DTO / Validator / Assembler | PASS | `grep -R "class .*DTO\|Validator\|Assembler" ... | grep -i "evidence\|score" || true` returned no matches |
| no schema/config/pom | PASS | forbidden path check confirms no `src/main/resources`, schema, config, or `pom.xml` changes in this package |
| no Push / Candidate / Decision / Point / Trading | PASS | Forbidden semantic grep returned no new matches for order, execution, candidate ranking, risk reward, position size, leverage, or trading output tokens |
| no P359 / P360 | PASS | P359 / P360 remain frozen; no branch or implementation continuation in this package |

# 3. API Verification

- Endpoint: `GET /api/dashboard/evidence-score-status?symbol=BTCUSDT`
- HTTP status: `200`
- Returned fields: `status`, `symbol`, `evidenceCount`, `scoreCount`, `evidenceAvailable`, `scoreAvailable`, `evidenceTopItems`, `scoreTopItems`, `sourceTraceComplete`, `sourceHealth`, `reason`, `message`, `reviewOnly`, `notTradingSignal`, `notCandidateSignal`, `notDecisionSignal`, `notPointSignal`, `watchlistBounded`, `marketQuoteChecked`, `displaySlotsAreCandidatePool`, `failClosed`
- Observed `reviewOnly`: `true`
- Observed `notTradingSignal`: `true`
- Observed `notCandidateSignal`: `true`
- Observed `notDecisionSignal`: `true`
- Observed `notPointSignal`: `true`
- Observed `watchlistBounded`: `true`
- Observed `marketQuoteChecked`: `true`
- Observed `displaySlotsAreCandidatePool`: `false`
- Observed status value: `EVIDENCE_SCORE_REVIEW_ONLY_READY`
- Observed sourceHealth: `OK`
- Observed failClosed: `false`
- Read-only confirmation: the endpoint is a GET status read and does not write config, send Push, generate Candidate / Decision / Point output, or call order/execution.

# 4. Dashboard Verification

- Evidence / Score status panel exists: `evidenceScoreStatusPanel` is present in `/dashboard` HTML.
- Evidence / Score status is displayed through the panel and `/api/dashboard/evidence-score-status` fetch path.
- Evidence count / score count are part of the status surface and API response.
- Top evidence / top score summary is available through `evidenceTopItems` and `scoreTopItems`.
- Source trace / source health is represented by `sourceTraceComplete` and `sourceHealth`.
- Review-only copy is visible: `Evidence / Score 是只读状态，不是交易信号`.
- Candidate / Decision / Point boundary is visible: `不是 Candidate；不是 Decision；不是 Point；不是交易信号`.
- Watchlist / MarketQuote boundary is visible: `Watchlist Pool 和 MarketQuote freshness / fallback 边界仍适用`.
- Display Slots boundary is visible: `Display Slots 不是候选池`.
- No candidate, decision, point, or trading action semantics are exposed by the dashboard smoke.

# 5. Boundary Confirmation

- no DTO / Validator / Assembler: confirmed by grep and changed-file inventory.
- no schema/config/pom: confirmed by forbidden path check.
- no Push external channel: no Push send or external channel wiring was added.
- no Candidate generation: endpoint and dashboard are status-only.
- no Decision generation: endpoint and dashboard do not create or emit Decision output.
- no Point generation: endpoint and dashboard do not create entry / stop / TP / RR or point proposals.
- no final direction: no final direction output was added.
- no order / execution / auto-trading: forbidden semantics remain absent.
- no all-market scan: this verification does not add or trigger a new scan universe.
- no Display Slots promotion: Display Slots remain explicitly not a candidate pool.
- P359 / P360 frozen: no P359 continuation or P360 start.

# 6. Capability-Level Conclusion

Current level remains `REVIEW_ONLY_RUNTIME partial`.

PositionSync slice: `REVIEW_ONLY_RUNTIME partial`.

Watchlist slice: `REVIEW_ONLY_RUNTIME partial`.

MarketQuote slice: `REVIEW_ONLY_RUNTIME partial`.

Evidence / Score slice经过 #869 和本次 verification 后可标记为 `REVIEW_ONLY_RUNTIME partial`。

This is still not Production Wiring. It is not Push. It is not Candidate generation. It is not Decision generation. It is not Point generation. It is not Trading.

# 7. Next Step Decision

Decision: **A. Evidence / Score visual verification / closure**.

Reason: API smoke and dashboard HTML smoke passed, but this package did not perform browser visual verification. The next package should only open `/dashboard`, confirm the Evidence / Score panel is visibly rendered without overlap, and close the slice visually. It must not proceed to P359, P360, new DTO / Validator / Assembler, Three AI, Position Monitor expansion, Push external channel, Candidate generation, Decision generation, Point generation, order execution, or auto-trading.

# 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Verification confirms Evidence / Score `REVIEW_ONLY_RUNTIME partial` if smoke/tests pass
- 是否接 service/runtime/dashboard/API: Verification only; verifies #869 minimal API/dashboard wiring
- 是否符合 #830 审计建议: Yes
