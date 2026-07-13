# Dashboard Browser Visual Acceptance

## 1. Result and boundary

Dashboard Home passed the required deterministic browser acceptance on branch `codex/dashboard-state-semantics-audit-fix`, based on `ac8c8ca469944a40551e3472eb8c99d1582d83f7` plus this closure change.

- Acceptance result: **PASS** for the ten required fixture scenarios, thirteen interactions, and three desktop CSS viewports.
- Evidence source: the real `dashboard.html` served by `scripts/dashboard-visual-acceptance-fixture.py` on offline `localhost`.
- Fixture refresh is disabled so test timing remains deterministic. Production refresh behavior is unchanged.
- Every non-GET request is rejected by the fixture. No live AI, market provider, CoinGlass, Push, Telegram, scheduler, position write, order, or secret was used.
- Screenshots are local evidence under `.runtime/dashboard-visual-acceptance/` and are intentionally not committed.
- These screenshots are **not** real-market or production-runtime evidence.
- Production readiness remains **BLOCKED**. This acceptance does not authorize deployment or any trading behavior.

## 2. Desktop viewport evidence

| CSS viewport | Browser observation | Screenshot artifact | Result |
|---|---|---|---|
| 1920 x 1080 | `innerWidth=1920`, `scrollWidth=clientWidth=1920`; sidebar `x=0..232`, main content starts at `x=368`; six cards are all 217 px high; every two-line conclusion has equal client/scroll height; the final position action header remains inside its card. | `01-normal-1920x1080.png`, 1436 x 1080 | PASS |
| 1440 x 900 | `innerWidth=1440`, `scrollWidth=clientWidth=1440`; no page-level horizontal scroll; six cards remain aligned; position and execution sections stack without covering controls. | `01b-normal-1440x900.png`, 1436 x 900 | PASS |
| 1366 x 768 | `innerWidth=1366`, `scrollWidth=clientWidth=1366`; no page-level horizontal scroll; sidebar and top bar do not cover content; the position action column remains within the card. | `01c-normal-1366x768.png`, 1366 x 768 | PASS |

The in-app browser was genuinely set to the listed CSS viewports. Its screenshot surface is capped at 1436 physical pixels, so the 1920 and 1440 PNG artifacts are 1436 pixels wide. The DOM viewport and overflow measurements above are retained as the authoritative responsive-layout evidence; no image was stretched, stitched, or fabricated.

## 3. Scenario matrix

| Scenario | Viewport | Operation | Expected | Actual browser observation | Screenshot | Result |
|---|---:|---|---|---|---|---|
| A. Normal observation | 1920 x 1080, 1440 x 900, 1366 x 768 | Load six analyzed assets | Separate state/direction, mixed neutral/bull/bear semantics, equal cards, bounded conclusions | `观察 / 候选 / 高风险观察 / 冲突状态 / 冷却 / 已失效` render in a dedicated second headline row; cards are equal height and conclusions are bounded to two readable lines. `WAITING_TRIGGER`/`TRIGGERED` are not presented as production lifecycle evidence. | `01-normal-1920x1080.png`, `01b-normal-1440x900.png`, `01c-normal-1366x768.png` | PASS |
| B. Low data quality | 1440 x 900 | Load `low-quality` | High-risk fail-closed state, `观望`, low confidence, high risk, no old plan values | Selected card shows `高风险观察 / 观望 / 低 / 高 / 暂不建议`; execution status says data quality is insufficient and all eight plan values are `暂无`. | `02-low-quality-1440x900.png` | PASS |
| C. AI disabled plus asset block | 1440 x 900 | Load `ai-disabled-blocked`, inspect AI region | AI disabled, consistency/mode not applicable, asset block remains independent | Header is neutral `已禁用`; role panel contains run state and explanation only; consistency score is `--`; consistency, conflict, and AI plan mode are `不适用`; asset direction block is `是`; no extreme-divergence claim appears. | `03-ai-disabled-blocked-1440x900.png` | PASS |
| D. AI timeout | 1440 x 900 | Load `ai-timeout`, inspect AI region | Warning visual, no role conclusions, no raw error code | Header is `调用超时` with warning tone; role panel is status-only; consistency is not applicable; no direction, confidence, plan result, or raw error code is rendered. | `10-ai-timeout-1440x900.png` | PASS |
| E. Expired plan | 1366 x 768 | Load `plan-expired` | Explicit expiry and cleared boundaries | Execution status is `当前暂无完整执行计划 / 计划已失效，等待重新分析`; all eight plan values are `暂无`. | `06-plan-expired-1366x768.png` | PASS |
| F. State/plan trace mismatch | 1366 x 768 | Load `trace-mismatch` | Current state remains visible; plan fails closed | Current asset card remains visible; execution status is `状态已更新，原计划需重新分析`; all plan values are `暂无`. | `11-trace-mismatch-1366x768.png` | PASS |
| G. Position with monitor record | 1440 x 900 | Load `position-monitored` | Position monitor owns the main section and original plan is collapsed | Entry/current/PnL/user stop/user TP, logic, direction support, reversal, risk, manual advice, last monitor, and `暂无下次监控排期` are present. Missing system stop/TP are `暂无`; original plan starts collapsed. | `04-position-monitored-1440x900.png` | PASS |
| H. Position without monitor record | 1366 x 768 | Load `position-waiting` | Uniform first-monitor waiting state | Logic, direction support, reversal, risk, advice, and monitor times say `等待首次监控` or `暂无`; no unsupported `暂无反转信号`, `当前方向仍获支持`, or low-risk claim is shown. | `05-position-waiting-1366x768.png` | PASS |
| I. Default placeholder | 1440 x 900 | Load `placeholder`, hover and click placeholder | Noninteractive `等待首轮分析` card | Placeholder is a `DIV`, has `aria-disabled=true`, has no `data-symbol`, uses the default cursor and no transform, does not change selection, and contains no fabricated direction/risk. | `09-default-placeholder-1440x900.png` | PASS |
| J. Home failure after stale data | 1366 x 768 | First Home call succeeds; asset switch makes the next Home call fail | Clear old business state and show explicit resync states | Before failure the selected asset has valid fixture data. After failure there is no selected stale card; trend becomes `— / 等待同步`; position says `首页数据暂不可用`; execution says `当前不展示执行计划`; AI consistency is `不适用`; prior asset terms are absent. | `07-home-failure-1366x768.png` | PASS |

## 4. Interaction acceptance

| # | Interaction | Actual browser result | Result |
|---:|---|---|---|
| 1 | First page load | Header, sidebar, seven KPIs, six assets, position, execution, and AI modules render from one Home payload. | PASS |
| 2 | Click first asset tile | Tile receives the selected state and all Home-owned regions remain on the same symbol. | PASS |
| 3 | Click another sidebar asset | ETH selection updates the corresponding tile and market trend to `弱偏多`. | PASS |
| 4 | Select through search | Selecting `SOL/USDT` leaves search value `SOL/USDT`, selected symbol `SOLUSDT`, and trend `观望`; the suggestion click no longer falls through to the tile underneath. | PASS |
| 5 | Rapid asset switching | BTC -> ETH -> BNB settles on `BNBUSDT` with trend `震荡`; no stale intermediate result remains. | PASS |
| 6 | Switch all three AI role tabs | Exactly one tab is active at a time; GPT final, Gemini review, and Grok challenge each render their own role-specific fields. | PASS |
| 7 | Expand/collapse original plan | Disclosure changes `closed -> open -> closed`; no new plan is promoted. | PASS |
| 8 | Open/close manual-position drawer | Drawer opens and closes via Cancel without submission or fixture write. | PASS |
| 9 | Open/close manual-close drawer | Drawer title is `记录平仓 · BTC/USDT`; it closes via Cancel without submission or position mutation. | PASS |
| 10 | Browser refresh | Fixture source remains visible, six assets return, selected symbol resets deterministically, and horizontal overflow remains absent. | PASS |
| 11 | Detail response later than Home | At 300 ms and after the 1.5 s delayed detail response, selected ETH, trend, execution state, and consistency payload are byte-for-byte equivalent. Detail does not overwrite Home. | PASS |
| 12 | Home failure after stale state | The first valid Home view is cleared after the next Home request fails; position, plan, AI, and consistency show explicit unavailable/not-applicable states. | PASS |
| 13 | Toggle light/dark theme | Theme changes to `dark` and back to light; body/card/table colors remain readable, no horizontal scroll appears, and no content state changes. | PASS |

## 5. Visible-copy and content checks

- Compact asset cards contain only trading pair, asset state, market bias, confidence, risk, worth-opening review state, and current conclusion.
- No compact card exposes provider source, data status, timeframe freshness, evidence count, analysis ID, trace ID, internal error code, database time, or diagnostic field.
- Required business surfaces contain no visible `WAITING_SYNC`, `DISABLED`, `CONFIRM`, `LEVEL_1_CONSISTENT`, `DEFAULT_SLOT`, `BACKEND_PENDING`, `fallback`, `degraded`, `missing`, `review-only`, or `manual review only` token.
- The tested surfaces contain no `自动开仓`, `自动平仓`, `自动反手`, `自动下单`, or `order submitted` wording.
- `AI 已禁用` is muted rather than green; timeout is warning-toned; status badges do not inherit market direction colors.
- AI conflict blocking and asset directional blocking are separate rows.

## 6. Browser-found corrections

The browser pass found and closed these P0/P1 presentation defects in the current branch:

1. Restored a persistent desktop asset sidebar and reserved its layout width.
2. Kept Home header pills under the Home payload owner so diagnostic refreshes cannot overwrite them.
3. Bounded asset conclusions to two lines and kept all six cards equal height.
4. Stacked position/execution modules at medium desktop widths so the final table column remains usable.
5. Separated AI applicability from asset directional blocking, including the consistency card's conflict-level fallback.
6. Added neutral/warning header tones for disabled/timeout AI states.
7. Prevented search suggestion clicks from falling through to an underlying asset tile.
8. Put long asset-state badges on their own headline row so symbols and status text cannot overlap.
9. Added a functional light/dark theme control and corrected dark table-header contrast.
10. Made the Home-failure fixture reproducibly return one valid Home snapshot before failing the next asset-switch request.

## 7. Screenshot manifest

Generation timezone: Asia/Shanghai (`+0800`).

| File | CSS viewport | PNG pixels | Generated | SHA-256 |
|---|---:|---:|---|---|
| `01-normal-1920x1080.png` | 1920 x 1080 | 1436 x 1080 | 2026-07-13 22:07:02 | `98fcb6f48386ffa56c617a94d71a9d06e3e24354562ac1014664f2b3f977fb6e` |
| `01b-normal-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:07:03 | `1a7a09a9a1c0147e004877d553a9ec64ab5ba672385d8588f1afa44e8cd7b010` |
| `01c-normal-1366x768.png` | 1366 x 768 | 1366 x 768 | 2026-07-13 22:07:04 | `7741320271f1f3c5f5814a0fc19533cf8fba0c77920d0500dba539ec8323600f` |
| `02-low-quality-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:07:05 | `73bc7e9692a503d04842d320cfc7ebb61c99fd9df7ac6da2c831b5daa700fbfa` |
| `03-ai-disabled-blocked-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:09:05 | `371c8cc52fccc8b634717c91b90c1cd0791274ec4783be83f33caa7e35f684cb` |
| `04-position-monitored-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:07:23 | `b37f603cae16b282641c3671100a0335ce7027eeb9144692da4ae6b1f900f23d` |
| `05-position-waiting-1366x768.png` | 1366 x 768 | 1366 x 768 | 2026-07-13 22:07:24 | `7f019de66a4b8bf1065313859de431c3530beca0f99998c081ec8c33bafb6ef6` |
| `06-plan-expired-1366x768.png` | 1366 x 768 | 1366 x 768 | 2026-07-13 22:07:25 | `bc489ab46d3c944bf8f5a2a56799944a932f6ad155ea3c501268c7f6ca90f476` |
| `07-home-failure-1366x768.png` | 1366 x 768 | 1366 x 768 | 2026-07-13 22:08:09 | `a39fc274fd2fce3147489aa6a001fc7e0d69214466ccd07fef3efb958ce85257` |
| `08-dark-mode-smoke-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:21:54 | `db86df5c168d589382589b239d2f4e68378c6e1828dc14f17f9d45cd507ae045` |
| `09-default-placeholder-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:07:26 | `3969d88de4940705729c6de0c15a7ed5a182b1db1dcdef9f654ed3691427e501` |
| `10-ai-timeout-1440x900.png` | 1440 x 900 | 1436 x 900 | 2026-07-13 22:09:35 | `5181d3ca3293f41d9223ca64e2373286198203b8ab5471070268d775029a11e3` |
| `11-trace-mismatch-1366x768.png` | 1366 x 768 | 1366 x 768 | 2026-07-13 22:07:28 | `b6225b9ab19947662f46adbcebf951b3b30662914061f74dd4ff3ccdd9ff6156` |

## 8. Final decision

The deterministic Dashboard browser acceptance is complete. No further business feature expansion is authorized by this package. PR #1125 must remain Draft until the user performs the final visual sign-off and decides whether to mark it Ready for Review. Production deployment remains **BLOCKED**.
