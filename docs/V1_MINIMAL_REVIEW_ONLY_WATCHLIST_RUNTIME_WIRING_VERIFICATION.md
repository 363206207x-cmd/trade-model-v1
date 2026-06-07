# V1 Minimal Review-Only Watchlist Runtime Wiring Verification

## 1. Executive Summary

#855 verification passed for the minimal Watchlist review-only runtime slice.

- `/api/rule/push-watchlist` is available and returned HTTP 200 during runtime smoke.
- The API response includes `status`, `configKey`, `symbols`, `source`, `empty`, `failClosed`, `reviewOnly`, `displaySlotsAreCandidatePool`, `reason`, and `message`.
- The observed runtime state was `WATCHLIST_CONFIG_MISSING`, with `reviewOnly=true`, `displaySlotsAreCandidatePool=false`, `empty=true`, and `failClosed=true`.
- `/dashboard` returned HTTP 200 and the Watchlist status panel / Display Slots boundary copy is present in the rendered HTML.
- The slice remains review-only. It does not send Push, read MarketQuote, generate candidates, generate points, generate final direction, or create trading actions.
- No DTO / Validator / Assembler / Orchestrator was added by this verification.
- Current capability remains `REVIEW_ONLY_RUNTIME partial`; Watchlist can now be marked as a second partial review-only runtime slice after PositionSync.
- Next step should be Watchlist visual verification / closure, because this package completed HTTP/API/dashboard smoke but did not perform browser visual layout verification.

## 2. Verification Matrix

| Check | Result | Evidence |
|---|---|---|
| workflow contract | PASS | `bash scripts/check-workflow-contract.sh` returned `WORKFLOW_CONTRACT_OK`. |
| compile | PASS | `./mvnw -q -DskipTests compile` completed successfully. |
| test-compile | PASS | `./mvnw -q -DskipTests test-compile` completed successfully. |
| RuleControllerTest | PASS | `./mvnw -q -Dtest=RuleControllerTest test` completed successfully. |
| DashboardControllerTest | PASS | `./mvnw -q -Dtest=DashboardControllerTest test` completed successfully. |
| RuleConfigWatchlistPoolReadAdapterTest | PASS | `./mvnw -q -Dtest=RuleConfigWatchlistPoolReadAdapterTest test` completed successfully. |
| API smoke `/api/rule/push-watchlist` | PASS | Runtime smoke returned HTTP 200 with the required response fields. |
| dashboard smoke `/dashboard` | PASS | Runtime smoke returned HTTP 200 and the required Watchlist / Display Slots copy was present in HTML. |
| no DTO / Validator / Assembler | PASS | `grep -R "class .*DTO|Validator|Assembler" -n src/main/java/org/example/trademodel \| grep -i "watchlist"` returned no output. |
| no schema/config/pom | PASS | Forbidden path check found no schema, config, pom, Java, test, or dashboard edits in this verification package. |
| no Push / MarketQuote / candidate / point / trading | PASS | Forbidden semantic grep found no new executable Watchlist wiring; template hits are existing negative safety copy only. |
| no P359 / P360 | PASS | P359 and P360 remain frozen in source-of-truth status. |

## 3. API Verification

- Endpoint: `GET /api/rule/push-watchlist`
- HTTP status: `200`
- Returned fields:
  - `status`
  - `configKey`
  - `symbols`
  - `source`
  - `empty`
  - `failClosed`
  - `reviewOnly`
  - `displaySlotsAreCandidatePool`
  - `reason`
  - `message`

Observed response data:

```json
{
  "status": "WATCHLIST_CONFIG_MISSING",
  "configKey": "push.watchlist.symbols",
  "symbols": [],
  "source": "MISSING",
  "empty": true,
  "failClosed": true,
  "reviewOnly": true,
  "displaySlotsAreCandidatePool": false,
  "reason": "WATCHLIST_CONFIG_MISSING",
  "message": "Watchlist Pool config is missing; Display Slots are not the candidate pool."
}
```

The endpoint is read-only. The smoke did not write config, send Push, read MarketQuote data, create candidates, create point proposals, or call any execution path.

## 4. Dashboard Verification

Dashboard smoke used `GET /dashboard` and returned HTTP 200.

The rendered HTML includes:

- `watchlistStatusPanel`
- `Display Slots 只是首页展示位`
- `Display Slots 不是候选池`
- `默认六个币不是候选池`
- `只读状态，不发送 Push`
- `不在 Watchlist Pool 不进入候选/推送/扫描/点位`

This confirms the dashboard exposes Watchlist Pool status and clearly separates DB-backed Watchlist Pool from homepage Display Slots. It also confirms Display Slots are not presented as the candidate pool.

No executable Watchlist action was observed. Existing dashboard text contains negative safety copy such as no order / execution and no point generation; those are boundary warnings, not executable semantics.

## 5. Boundary Confirmation

- No DTO / Validator / Assembler / Orchestrator was added.
- No schema / config / pom file was changed.
- No Push external channel was connected.
- No MarketQuote path was connected.
- No candidate generation was added.
- No point generation was added.
- No final direction was generated.
- No order / execution / auto-trading path was added.
- P359 and P360 remain frozen.

## 6. Capability-Level Conclusion

Current level remains `REVIEW_ONLY_RUNTIME partial`.

PositionSync already has `REVIEW_ONLY_RUNTIME partial` after #839 / #840 / #841.

Watchlist now also has a partial review-only runtime slice after #855 plus this verification: the system can expose a read-only Watchlist Pool status through API and dashboard, with fail-closed behavior when config is missing and with Display Slots clearly marked as not being the candidate pool.

This is still not Production Wiring.

It is not Push, not MarketQuote wiring, not candidate generation, not point generation, not final direction, and not trading capability.

## 7. Next Step Decision

Recommendation: **A. Watchlist visual verification / closure**.

Reason: compile, tests, API smoke, dashboard HTTP smoke, and forbidden-boundary checks passed. The remaining useful closure step is browser visual verification to confirm the Watchlist status panel is visible, readable, and not layout-breaking in the actual page. It should remain verification-only and must not expand into Push, MarketQuote, candidate, point, or trading work.

Do not continue P359 or P360. Do not add a new DTO, Validator, Assembler, Three AI, Position Monitor expansion, Push external channel, MarketQuote wiring, point generation, order, execution, or auto-trading.

## 8. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: Verification confirms Watchlist `REVIEW_ONLY_RUNTIME partial` after smoke/tests passed.
- 是否接 service/runtime/dashboard/API: Verification only; verifies #855 minimal API/dashboard wiring.
- 是否符合 #830 审计建议: Yes
