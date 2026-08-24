# TRINE LOGIC v4.1 Global Non-CoinGlass Staging Closure

## Scope and identity

- Start Head: `acc0c0eafd1ac862fc4ca57cab54af25a9296723`
- Implementation Head: `4951bef07eaf659fce895f340391e44ac238caf7`
- Final evidence Head: the exact Git object containing this report; the SHA is recorded in PR #1195 and the final task response.
- Deployed implementation Head during runtime validation: `4951bef07eaf659fce895f340391e44ac238caf7`
- Branch: `codex/frontend-interaction-runtime-closure`
- PR: `#1195`, Draft, open, unmerged
- Staging: private Tailscale HTTPS only; Funnel disabled; public application exposure zero
- CoinGlass: excluded because no private key was available; live call count zero

No UI-review fixture, fake Opportunity, fake Final, fake position, fake Message,
fake Recheck, or automatic trading action was used.

## Fresh finding register

| ID | Area / user action | Expected contract | Actual root cause before repair | Severity | Fixable | Repair / validation | Final status |
|---|---|---|---|---|---|---|---|
| GN-01 | Search BTC and run Preview | Authoritative Kraken closed bars reach Preview | OHLCV scheduler was disabled, so BTC 5m/15m/1h/4h had no persisted authoritative rows | P1 | YES | Explicit Staging scheduler opt-in; three real cycles; 24 asset/timeframe pairs; BTC formal reads and Preview | CLOSED |
| GN-02 | Preview market environment | Disabled Binance must never be selected | Generic provider-call enablement allowed the disabled Binance heuristic path to be considered | P1 | YES | Binance provider and fallback remain false; persisted Kraken environment selected; Binance external log count 0 | CLOSED |
| GN-03 | Provider readiness | Existing non-CoinGlass credentials must be usable without secret disclosure | Six required per-model cost inputs were absent | P1 | YES | Root-owned `0600` AI runtime file received authorized pricing configuration; all three application probes returned HTTP 200 and exact-model authorized | CLOSED |
| GN-04 | Task center after terminal Preview | Terminal rows are not active and do not show a false partial-success state | Controller collapsed non-literal success to PARTIAL; Workspace counted PARTIAL active | P1 | YES | Stable reason mapping, terminal state mapper, retry/cancel rules, executable Node matrix, Java tests, Staging task active count 0 | CLOSED |
| GN-05 | Three-AI controlled Preview | Call only when frozen input contract is complete | Real Kraken-only run produced DQ 55; derivatives evidence was unavailable; AI input gate correctly blocked formal calls | EVIDENCE_GAP | NO | Fail Closed retained. Provider connectivity separately verified. No Candidate/Final/Opportunity created | BLOCKED_COINGLASS_INPUT |
| GN-06 | Position close | Execute only against a genuinely recorded Owner-authorized position | Staging has zero UserPosition rows | EVIDENCE_GAP | NO | Automated contracts retained; no position fabricated | BLOCKED_NO_OWNER_AUTHORIZED_POSITION |
| GN-07 | Recheck | Message -> PushSnapshot -> PUSH_OPEN only | Staging has zero legal Message/Recheck rows | EVIDENCE_GAP | NO | Automated contracts retained; no Message or PushSnapshot fabricated | BLOCKED_NO_LEGAL_SOURCE |
| GN-08 | Offsite backup | Use an existing authorized target | No authorized offsite target exists | EVIDENCE_GAP | NO | Local backup/checksum and isolated restore passed; no upload attempted | BLOCKED_NO_AUTHORIZED_TARGET |
| GN-09 | Browser screenshots | Normal-mode screenshots through Owner Tailnet browser | In-app browser is outside Tailnet; desktop Chrome connector unavailable | EVIDENCE_GAP | NO | HTTP/API/session checks completed; exact Owner capture checklist below; Funnel remained off | OWNER_HANDOFF |

Fresh counts: P0 0; P1 4 closed / 0 open; P2 0; evidence gaps 5.
Fixable non-CoinGlass blockers: before 4, after 0.

## Before / after runtime matrices

### Database state

| Object | Before | After controlled runtime | Interpretation |
|---|---:|---:|---|
| AnalysisRun | 2 failed | 3 total; latest SUCCESS / ANALYSIS_PREVIEW / preview=true | Exactly one controlled Preview added |
| AI call audit rows | 0 | 3 input-gate rows | GPT/Gemini/Grok audit chain preserved without fabricated output |
| Asset/Opportunity state | 0 | 0 | Preview did not promote an Opportunity |
| Candidate | 0 | 0 | No Candidate created from incomplete input |
| Final | 0 | 0 | No Final created |
| UserPosition | 0 | 0 | No position created |
| Message | 0 | 0 | No message created |
| Recheck | 0 | 0 | No Recheck created |
| Async tasks | 2 historical terminal + 0 active | 2 historical terminal + 1 succeeded + 0 active | Historical evidence retained; terminal semantics fixed |

### Runtime configuration presence

| Capability | Effective state | Evidence |
|---|---|---|
| Kraken external OHLCV | Explicitly enabled for private Staging | Persisted real closed bars and scheduler logs |
| Binance provider | Disabled | Effective provider and fallback false; external call count 0 |
| OpenAI | Enabled and configured | Application provider probe HTTP 200, exact model authorized |
| Gemini | Enabled and configured | Application provider probe HTTP 200, exact model authorized |
| xAI | Enabled and configured | Application provider probe HTTP 200, exact model authorized |
| CoinGlass | Disabled / unavailable | No private key; external call count 0 |
| Secret files | `0600 root:root` | Runtime `stat`; no values emitted |

## Kraken authoritative OHLCV lineage

`Kraken external closed bars -> KrakenPairResolver canonical symbol ->`
`tm_persisted_ohlcv_bar -> PersistedRealMarketEnvironmentService ->`
`AnalysisAssemblerServiceImpl -> rule/decision layer -> AI input gate`

The six watch-pool assets each have real Kraken rows for 5m, 15m, 1h and
4h, giving 24 distinct canonical asset/timeframe pairs. After the bounded
runtime operation, BTCUSDT had 107/102/100/100 closed bars for
5m/15m/1h/4h. The controlled Preview logged
`sourceType=KRAKEN_PERSISTED_OHLCV` and `closedBars=400` for its formal
multi-timeframe environment. No source was relabelled as Binance.

## Binance classification

Classification: `DISABLED_HEURISTIC_STILL_SELECTED`, closed by runtime policy.
Effective Binance enabled=false and fallback=false. The controlled Preview
used persisted Kraken OHLCV. Binance external call count after deployment: 0.
No VPN, proxy, fallback, or geographic workaround was used.

## Non-CoinGlass provider evidence

| Provider | Application probe | Authorized model | Configuration | Result |
|---|---|---|---|---|
| OpenAI | HTTP 200 | `gpt-5.6-sol` | RPM/cost/budget values present and positive | PASS |
| Gemini | HTTP 200 | `gemini-3.5-flash` | RPM/cost/budget values present and positive | PASS |
| xAI | HTTP 200 | `grok-4.5` | RPM/cost/budget values present and positive | PASS |

Provider request IDs were present for OpenAI and xAI; Gemini returned no
provider request ID. No secret, request body, response body, or credential
value was copied into evidence.

## Controlled Preview and Three-AI boundary

- HTTP: 200
- Analysis: `ana-046615653a514effbfc357320f05dc82`
- Trace: `trace-2c1525e07c8b41dc96e9310ef8ecba87`
- Result: `EXECUTED / ANALYSIS_EXECUTED`
- Preview-only: true
- Pool mutation / Opportunity / Candidate / Final persistence: false / false / false / false
- Persisted data quality: 55
- Scores: frozen eight score types present
- Evidence: five real/provenance-bearing rows; derivative availability row is unavailable
- AI roles: three `NOT_CALLED_INPUT_GATE` audit rows, zero model-call cost
- Gate reasons include data quality below 85, incomplete evidence contract and no significant evidence change

This is the required honest boundary: provider connectivity is PASS, while
the full Three-AI runtime chain is `BLOCKED_COINGLASS_INPUT`. Lowering the
quality threshold or inventing derivatives evidence was forbidden and was
not done.

## Home, task center, routes and interactions

- Authenticated HTTP 200: `/dashboard`, `/positions`, `/analysis`, `/messages`, `/me`.
- Home API: PASS; latest formal data timestamp present; assets 0 because no formal Opportunity exists; positions 0.
- Watch pool remains 6 in PostgreSQL and is not treated as Top6 Opportunities.
- Task API: total 3; active 0; succeeded 1; historical failed/partial 2.
- The shared frontend mapper executes queued/running/succeeded/failed/partial/data-unavailable scenarios.
- Terminal failed/partial rows are not counted as active; retry/cancel rules remain bounded.
- No read/F5 operation created another analysis attempt.
- Existing Owner copy, GPT/Gemini/Grok tab labels, returnTo, position boundaries and responsive contracts passed directed regression.

## Infrastructure and operations

| Gate | Result |
|---|---|
| Application / PostgreSQL active and enabled | PASS |
| Flyway PostgreSQL V1 to V14 | PASS; 14 successful migrations |
| Application DB role least privilege | PASS |
| Backup timer active/enabled | PASS |
| Fresh backup and SHA-256 verification | PASS |
| Isolated restore | PASS after copying a temporary `0600 postgres` restore artifact; root-owned production backup permissions were preserved |
| Application restart recovery | PASS |
| PostgreSQL restart recovery | PASS |
| Scheduler recurrence | PASS; at least three independent Kraken cycles observed |
| Scheduler overlap/duplicate protection | PASS by existing contract tests and idempotent persisted keys |
| Liveness / readiness | UP / UP |
| Recent service error count | 0 |
| Secret value log candidates | 0 |
| Disk / memory / DB | root disk 12%; MemAvailable 6,933,156 KB; DB 12,983,319 bytes at audit |
| Application / PostgreSQL listen | IPv4-mapped loopback 127.0.0.1:8081; loopback 5432 |
| Tailscale Serve | `https://rine-staging.tailf2f07d.ts.net` tailnet-only -> `127.0.0.1:8081` |
| Funnel / public app exposure | OFF / 0 |
| Offsite backup | BLOCKED_NO_AUTHORIZED_TARGET |

The initial restore attempt correctly failed because PostgreSQL could not read
the root-owned backup. The validation copied it to an isolated temporary
`0600 postgres` file; production backup ownership was not weakened.

## Browser evidence and Owner handoff

The in-app browser could not enter the Owner Tailnet and returned a connection
closure. A desktop Chrome control connection was not available. No Funnel or
public port was enabled to work around this. Screenshot evidence is therefore
`OWNER_HANDOFF`; HTTP/API/database/session evidence is complete.

Owner capture checklist in the dedicated no-proxy Chrome window:

1. Open the private HTTPS URL and capture `/login`.
2. Sign in and capture `/dashboard`, including the real global data timestamp and empty formal Opportunity state.
3. Search/select BTCUSDT without starting another Preview; capture selection identity.
4. Open task center; capture one succeeded Preview, the two historical terminal attempts and active count 0.
5. Open `/analysis/ana-046615653a514effbfc357320f05dc82`; capture GPT/Gemini/Grok fail-closed input-gate state one role at a time.
6. Capture `/positions` and `/messages` honest empty states.
7. Capture DevTools Console count 0, Network request/POST counts, viewport inner/client/scroll widths and the current URL.
8. Log out and confirm the authenticated route redirects to login.

Do not repeat provider probes or the Preview during screenshot capture.

## Validation accounting

- Directed package: 10 JUnit tests plus 1 executable Node state matrix; all PASS.
- Local Java 17 full Maven: `4799` tests, `0` failures, `0` errors, `14` controlled Docker/Testcontainers-unavailable skips.
- Frontend executable state matrix: PASS.
- JavaScript syntax: PASS.
- Product Source Gate: PASS.
- Workflow Contract: PASS after the clean implementation commit.
- `git diff --check`: PASS.
- Implementation-Head exact CI: `quality-gate` PASS and `workflow-contract` PASS; duplicate same-name jobs count as one category.
- Final evidence-Head exact CI: recorded in PR #1195 and the final task response after this evidence commit; no self-referential SHA/check claim is embedded here.

## Remaining blockers

- CoinGlass live key, snapshot freshness/persistence and AI-run consumption.
- Owner-authorized position for destructive full-close E2E.
- Legal `PUSH_SNAPSHOT` Message for real Recheck.
- Authorized offsite backup destination.
- Owner Tailnet browser screenshots.
- Owner review and merge decision.

All confirmed fixable non-CoinGlass P0/P1 findings are closed. The branch and
PR remain open, Draft and unmerged. `CURRENT_PHASE_DONE=NO`, `MERGE=NO`, and
`PRODUCTION_DEPLOYMENT_ALLOWED=NO`.
