# B.1.2.3.2 Header / Status Timestamp Transport Evidence

Evidence classification: `UI_REVIEW_FIXTURE / CONTROLLED_TRANSPORT_EVIDENCE`.
This is not live-provider data and does not alter normal production sources.

## Heads and PR state

- Start Head: `015cf232089fa581417212994b7e954393a5ec7b`
- New Head: exact final PR Head is recorded in the canonical PR comment and
  final task output after commit; a Git commit cannot contain its own SHA.
- Branch: `codex/frontend-interaction-runtime-closure`
- PR: `#1195`, OPEN / DRAFT / UNMERGED

## Call points

Before:

- `DashboardHomeVO.HeaderVO.updatedAt`: `LocalDateTime`
- `DashboardHomeServiceImpl`: converted the formal source through
  `LocalDateTime.ofInstant(globalDataUpdatedAt, ZoneOffset.UTC)`
- System Status Data: `globalDataUpdateCard(globalDataUpdatedAt)`
- Header: converted offset-free value passed to `setUpdatedAt`
- Frontend: Header and Status already called the same production `clockTime()`

After:

- `DashboardHomeVO.HeaderVO.updatedAt`: `Instant`
- Header: `home.getHeader().setUpdatedAt(globalDataUpdatedAt)`
- System Status Data: unchanged `globalDataUpdateCard(globalDataUpdatedAt)`
- Frontend: unchanged `clockTime(header.updatedAt)` and
  `clockTime(state.dataQuality.value)`

The source chain remains:

`tm_persisted_ohlcv_bar.close_time_ms ->`
`PersistedOhlcvBarMapper.selectLatestClosedBar() ->`
`LocalRealDataStatusService.latestClosedBarAt() -> globalDataUpdatedAt`.

Readiness time, application start time, current time, and Provider CONNECTED
are not timestamp fallbacks.

## Serialization contract

Application-configured Jackson serializes the complete `DashboardHomeVO` with:

```json
{
  "header": {"updatedAt": "2026-08-20T09:56:00Z"},
  "systemState": {"dataQuality": {"value": "2026-08-20T09:56:00Z"}}
}
```

- Header JSON: `2026-08-20T09:56:00Z`
- Status JSON: `2026-08-20T09:56:00Z`
- Byte-identical: YES
- Explicit offset: YES
- Numeric timestamp: NO
- Offset-free Header timestamp: NO
- Missing persisted bar: both fields null/omitted by the same Jackson rules

## Production formatter matrix

The Maven-executed Node script extracts and executes production `has()` and
`clockTime()` from `src/main/resources/static/js/home-runtime.js`.

| Case | Status | Header | Result |
|---|---:|---:|---|
| `TZ=UTC`, offset input | 09:56 | 09:56 | PASS |
| `TZ=Asia/Shanghai`, offset input | 17:56 | 17:56 | PASS |
| `TZ=Asia/Shanghai`, legacy Header without offset | 17:56 | 09:56 | counterexample PASS |
| null / undefined / empty | — | — | fail closed PASS |

Readiness-only changes and BTC/ETH selection changes leave both consumers on
the unchanged global closed-bar Instant. A newer persisted closed bar changes
both consumers together.

## Browser evidence

- Screenshot:
  `docs/evidence/b1_2_3_2/home-timestamp-asia-shanghai-1440x900.png`
- Runtime: authenticated standard release JAR, isolated `ui-review` profile
- Viewport: 1440x900
- Zoom: 100%
- Host timezone: Asia/Shanghai
- Controlled transport input: one fixed
  `2026-08-20T09:56:00Z` inserted into both fields by a temporary localhost
  route-interception proxy outside the repository
- PageHeader: `更新于 17:56`
- System Status Data: `更新于 17:56`
- Production data, schema and UI-review service changed: NO

## Validation

- Directed tests: `133/133` PASS
- LOCAL RUN Full Maven: `4786` tests / `0` failures / `0` errors /
  `14` skipped
- First exact-head CI profile: `938` tests / `0` failures / `0` errors /
  `0` skipped
- Required check categories on the first implementation Head:
  `quality-gate` PASS (two duplicate triggers, one category) and
  `workflow-contract` PASS (one category)
- Final docs-only Head: must repeat both required categories before the final
  report
- Product Source Gate: PASS
- Workflow Contract: PASS on the clean implementation Head
- PR remains Draft / unmerged: YES

Prior evidence directories `b1_2_3` and `b1_2_3_1` were not modified.
