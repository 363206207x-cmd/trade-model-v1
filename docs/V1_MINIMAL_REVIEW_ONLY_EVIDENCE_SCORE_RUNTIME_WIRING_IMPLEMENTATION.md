# V1 Minimal Review-Only Evidence / Score Runtime Wiring Implementation

## 1. Executive Summary

本包执行最小 Evidence / Score review-only runtime implementation。

- Current Mainline（当前主线）: Readiness / Point Mainline.
- Current Block（当前模块）: Minimal Review-Only Evidence / Score Runtime Wiring Implementation.
- Capability Movement（能力层级变化）: `REVIEW_ONLY_RUNTIME partial` remains; this package prepares the Evidence / Score slice for verification.
- User-visible Output（用户可见输出）: `/api/dashboard/evidence-score-status` exposes read-only Evidence / Score runtime status, and dashboard shows Evidence / Score availability, counts, top summary, source trace / source health, and safety labels.
- Overreach Boundary（越界边界）: no DTO / Validator / Assembler / Orchestrator, no schema/config/pom, no Push, no Candidate generation, no Decision generation, no Point generation, no direction output, no order/execution, no P359/P360.

The implementation reuses the existing `DashboardController` detail owner path, `EvidenceService`, `ScoreService`, `EvidenceBriefVO`, and `ScoreBriefVO`. It adds no new wrapper owner.

## 2. API Surface

Added minimal read-only endpoint:

```text
GET /api/dashboard/evidence-score-status?symbol=BTCUSDT
```

Returned review-only status fields:

| Field | Meaning |
|---|---|
| `status` | One of the Evidence / Score review-only statuses. |
| `symbol` | Selected dashboard symbol. |
| `evidenceCount` / `scoreCount` | Counts from existing top-item owner reads. |
| `evidenceAvailable` / `scoreAvailable` | Whether existing owner paths returned readable rows. |
| `evidenceTopItems` / `scoreTopItems` | Existing brief rows from `EvidenceService` / `ScoreService`. |
| `sourceTraceComplete` | Minimal completeness check for display-level source trace. |
| `sourceHealth` | `OK`, `PARTIAL`, `MISSING`, or `BLOCKED`. |
| `reason` / `message` | Display-only explanation. |
| `reviewOnly` | Always `true`. |
| `notTradingSignal` | Always `true`. |
| `notCandidateSignal` | Always `true`. |
| `notDecisionSignal` | Always `true`. |
| `notPointSignal` | Always `true`. |
| `watchlistBounded` | Always `true`; Watchlist Pool boundary still applies. |
| `marketQuoteChecked` | Always `true`; MarketQuote freshness/fallback boundary still applies. |
| `displaySlotsAreCandidatePool` | Always `false`. |
| `failClosed` | True unless both Evidence and Score are readable with display-level source trace. |

Allowed statuses implemented:

- `EVIDENCE_SCORE_REVIEW_ONLY_READY`
- `EVIDENCE_MISSING_FAIL_CLOSED`
- `SCORE_MISSING_FAIL_CLOSED`
- `EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED`
- `EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL`
- `EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED`

## 3. Dashboard Surface

`dashboard.html` now includes a minimal `evidenceScoreStatusPanel` below the Watchlist and MarketQuote review-only panels.

The panel displays:

- Evidence / Score review-only status;
- selected symbol;
- Evidence count;
- Score count;
- top evidence / top score summary when available;
- source trace completeness;
- source health;
- review-only / not trading signal copy;
- not Candidate / not Decision / not Point copy;
- Watchlist Pool and MarketQuote freshness/fallback boundary copy.

The dashboard fetches:

```text
/api/dashboard/evidence-score-status?symbol=<selected dashboard symbol>
```

If no selected symbol exists, the panel remains incomplete/fail-closed and does not scan a default market universe.

## 4. Tests

Targeted tests added / strengthened:

| Test | Coverage |
|---|---|
| `DashboardControllerTest` | Verifies the Evidence / Score endpoint fields, `reviewOnly=true`, `notTradingSignal=true`, `notCandidateSignal=true`, `notDecisionSignal=true`, `notPointSignal=true`, missing analysis fail-closed behavior, dashboard DOM/copy, and absence of executable candidate/point/trading fields. |

## 5. Boundary Confirmation

- No DTO / Validator / Assembler / Orchestrator was created.
- No schema/config/pom was changed.
- No Push external channel was connected.
- No Candidate generation was connected.
- No Decision generation was connected.
- No Point generation was connected.
- No executable numeric trade levels, final direction, order, execution, or automation output was generated.
- No all-market scan was added.
- Display Slots remain homepage display only and are not promoted to candidate pool.
- Watchlist Pool and MarketQuote freshness/fallback boundaries remain explicit.
- P359 / P360 remain frozen.

## 6. Next Required Action

Next required action: `Minimal Review-Only Evidence / Score Runtime Wiring Verification`.

Verification must run workflow contract, compile, test-compile, targeted dashboard/controller tests, relevant Evidence / Score service tests, forbidden path checks, forbidden semantics grep, and API/dashboard smoke if applicable.
