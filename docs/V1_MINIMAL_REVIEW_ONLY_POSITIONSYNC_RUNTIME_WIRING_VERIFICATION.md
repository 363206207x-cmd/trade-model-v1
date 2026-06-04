# V1 Minimal Review-Only PositionSync Runtime Wiring Verification

This document verifies the merged #839 minimal dashboard PositionSync review-only runtime slice.

It is verification only. It does not add Java service logic, controller logic, provider logic, scheduler logic, mapper logic, schema, config, pom changes, endpoints, DTOs, Validators, Assemblers, Orchestrators, MarketQuoteClient wiring, Push, external channels, point generation, final direction, order execution, or auto-trading.

## 1. Executive Summary

#839 verification passes for the intended minimal scope: dashboard now reuses the existing `/api/system/position-sync-status` endpoint and existing `PositionSyncStatusVO` fields to display review-only PositionSync provider / fallback / simulated / freshness / last-sync / open-position-count state.

The slice remains `REVIEW_ONLY_RUNTIME partial`. It is not Production Wiring. It does not add new backend production wiring; it only makes an existing dashboard/API runtime slice visible and safer to read.

No new trading action semantics were added. The dashboard still contains existing negative boundary copy such as "No order, execution, reverse, signal, or auto-trading action is available here"; those are prohibitive guardrails, not action affordances.

This is the first stop-loss track that produces a small user-visible runtime capability after the #830 audit: a human can see whether PositionSync is simulated, fallback, fresh, incomplete, or blocked without treating it as trading advice.

## 2. Verification Matrix

| Check | Result | Evidence |
|---|---|---|
| `bash scripts/check-workflow-contract.sh` | PASS | Returned `WORKFLOW_CONTRACT_OK`. |
| `./mvnw -q -DskipTests compile` | PASS | Compile completed successfully. |
| `./mvnw -q -DskipTests test-compile` | PASS | Test compile completed successfully. |
| `./mvnw -q -Dtest=DashboardControllerTest test` | PASS | Targeted dashboard/controller test completed successfully. |
| dashboard static fields | PASS | `dashboard.html` contains `providerStatusValue`, current provider, configured provider, fallback, fallback reason, last sync success, freshness, open count, and last sync time display slots. |
| review-only labels | PASS | `dashboard.html` contains `只读状态，不是交易建议`. |
| simulated fallback warning | PASS | `dashboard.html` contains `模拟来源不等于真实 Binance 持仓`. |
| review-only PositionSync statuses | PASS | `dashboard.html` contains `REVIEW_ONLY_POSITION_SYNC_READY`, `SIMULATED_FALLBACK`, `INCOMPLETE`, and `BLOCKED_FAIL_CLOSED` mapping logic. |
| no new DTO / Validator / Assembler | PASS | `git diff --name-only main...HEAD` after this verification only contains docs/status files; #839 implementation added no DTO / Validator / Assembler. |
| no backend service/controller/provider/schema changes | PASS | Forbidden path check for service, controller, provider, scheduler, mapper, schema, config, and pom is empty. |
| no trading semantics added | PASS | Grep hits in `dashboard.html` are existing negative guardrail copy such as "No order..." and "No readiness / point / entry / stop / TP / RR generated"; no action control or executable instruction was added. |

## 3. Runtime Smoke

Runtime smoke was executed.

First attempt:

- Command: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
- Result: failed under normal sandbox with `java.net.SocketException: Operation not permitted` while binding the local Tomcat port.

Escalated local smoke:

- Command: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
- Result: service started on `http://localhost:8081`.
- `/dashboard`: HTTP `200`, downloaded page size `175504` bytes.
- `/api/system/position-sync-status`: HTTP `200`, returned `configuredProviderType=SIMULATED`, `activeProviderType=SIMULATED`, `activeProviderName=simulated-provider-v1`, `fallbackOccurred=false`, `freshnessStatus=FRESH`, `lastSyncSuccess=true`, and `currentOpenPositionCount=2`.

The local app was stopped after smoke. Starting the existing app also triggered existing scheduler logs and existing simulated PositionSync updates. This verification did not add or change those schedulers; it only observed the current runtime behavior.

Browser visual verification was not completed in this verification package because no in-app browser automation tool was exposed for this turn after tool discovery. The next safest follow-up is a small dashboard smoke / visual verification pass, not a new feature track.

## 4. Capability Conclusion

Current capability level is `REVIEW_ONLY_RUNTIME partial`.

This is not Production Wiring.

This is not trade-ready.

This is not a complete Position Monitor feature.

This is not point generation.

This is not Push.

This is not AI decision expansion.

This is a narrow dashboard-visible review-only runtime slice over an existing PositionSync status endpoint.

## 5. Next Step Recommendation

Recommendation: **C. Dashboard smoke / visual verification follow-up**.

Reason:

- HTTP smoke passed, but true browser visual verification was not completed.
- The next smallest risk-reducing action is to visually confirm the dashboard renders the new status rows without overlap or misleading copy.
- This avoids jumping back into P359 / P360 or into another skeleton family.
- After visual smoke passes, the project can choose between Source-Owned Runtime vs Existing Point Proposal Merge Map and the next minimal runtime slice selection with better confidence.

Do not start:

- P359;
- P360;
- new DTO;
- new Validator;
- new Assembler;
- new Orchestrator;
- Three AI;
- Position Monitor expansion;
- Push;
- point generation;
- order / execution / auto-trading.

## 6. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Verification only, confirms `REVIEW_ONLY_RUNTIME partial`
- 是否接 service/runtime/dashboard/API: No new wiring; verifies existing dashboard/API slice
- 是否符合 #830 审计建议: Yes
