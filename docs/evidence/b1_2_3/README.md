# B.1.2.3 UI Review Evidence

Scope: RINE LOGIC Web copy/status ownership plus the two authorized Position
detail fixes. All screenshots use the isolated `ui-review` profile and are
fixtures, not production ownership evidence.

## Screenshots

- `b123-login-rine-logic.png`: formal login brand and removed slogans.
- `b123-home-1440.png`: Home 1440x900, short titles, English-only role tabs.
- `b123-home-1080.png`: Home 1080x900, no horizontal overflow or text clipping.
- `b123-analysis.png`: Analysis PageHeader, RINE LOGIC document title, no subtitle.
- `b123-positions.png`: full active Position list; one detail link per row; no close action.
- `b123-position-7101.png`: BTCUSDT detail, original `/dashboard` return target, no self-link.
- `b123-position-7101-o07.png`: controlled O07 bound to position 7101 / BTCUSDT.
- `b123-position-7102.png`: ETHUSDT identity proof, no self-link.
- `b123-position-7103.png`: SOLUSDT identity proof, no self-link.

## Runtime observations

| Check | Result |
|---|---|
| Home 1440 horizontal overflow | 0 |
| Home 1080 horizontal overflow | 0 |
| Home 1080 visible text clipping | 0 |
| Browser console errors | 0 |
| Home/list close actions | 0 |
| Active list detail links | 7103, 7102, 7101; one each |
| 7101 detail self-links | 0 |
| 7102 detail self-links | 0 |
| 7103 detail self-links | 0 |
| 7101 top return target | `/dashboard` |
| O07 identity | `7101` / `BTCUSDT` |
| O07 cancel POST count | 0 (no manual-close POST in access log) |
| 7999 monitoring API | 404; detail fails closed; close action hidden |

## Close action matrices

Pure function:

| status | visible |
|---|---|
| `OPEN` | true |
| `open` | true |
| `PARTIALLY_CLOSED` | true |
| `CLOSED` | false |
| `null` | false |
| `undefined` | false |
| empty | false |
| `UNKNOWN` | false |
| unrecognized | false |

DOM contract:

| status | action DOM state |
|---|---|
| `OPEN` | visible |
| `PARTIALLY_CLOSED` | visible |
| `CLOSED` | hidden |
| missing/unknown | hidden |
| load failure | hidden |

The executable matrices are in
`WorkspacePositionCloseEntryRuntimeContractTest`.

## Status ownership

| Cell | Producer | Display rule |
|---|---|---|
| Environment | formal macro/BTC environment owner | formal value, otherwise `—` |
| System | formal system-level risk/safety owner | formal value, otherwise `—` |
| Data | Historical finding at Head `5066a61c`: readiness time was incorrectly bound; superseded by `LocalRealDataStatusService.latestClosedBarAt` in B.1.2.3.1 | `更新于 HH:mm`, otherwise `—` |
| Service | formal provider availability collection | `n/m 可用`, no denominator -> `—` |
| Account | all owner active UserPositions | `n 笔`, none -> `—` |
| Hot Reset | explicit system field | `关闭` / `已触发`, missing -> `—` |

`Provider CONNECTED` alone cannot produce `新鲜`. B.1.2.3.1 corrected the data
timestamp owner because this historical package still read readiness time.
Current executable evidence is indexed at `docs/evidence/b1_2_3_1/README.md`.
The UI-review status values are only visual fixtures.

## Copy/DOM counts

- Formal visible `Fundamental AI`: 0 in login/Home/workspace production templates.
- Login `个人复核入口`: 0.
- Login `多源证据决策系统`: 0.
- PageHeader subtitle DOM: 0.
- Home role-tab visible labels: exactly `GPT`, `Gemini`, `Grok`.
- Detail-card `查看详情`: 0.
