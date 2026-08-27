# TRINE LOGIC Core Production Loop Automation Implementation Report

## Scope

- Authorized package: `FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION`
- Starting main: `ba40c8caf4bb8d752f8833f3a089a6f01a86fa2e`
- Implementation branch: `codex/core-production-loop-automation`
- Telegram source: closed, unmerged PR #1201 at
  `b158b7a89a4fdb9bd2254a210ecd258e26032161`
- Database schema changes: none
- Frontend, Home, Me, login, Figma, and Mobile changes: none
- Automatic trading, order, open, close, reduce, and reverse capabilities: none

## Ownership And Reuse

The implementation reuses the existing production owners:

- Asset Pool is the only automatic opportunity-discovery universe.
- `AssetState` owns state-sensitive eligibility, scan claims, scan audit data,
  and restart recovery.
- `AnalysisRun` owns every requested full analysis and its existing trace chain.
- persisted OHLCV owns source-specific, closed-candle market facts.
- `UserPosition` and `PositionMonitorLog` continue to own position monitoring.
- `Message` remains the application business-fact owner.
- `ChannelDelivery` remains the Telegram delivery, retry, and terminal-outcome owner.
- The PR #1201 policy, formatter, commit listener, dispatcher, and delivery
  whitelist are integrated as the single Telegram implementation.

No second scheduler, Telegram pipeline, scan fact table, `nextScanAt` column, or
position model was introduced.

## Production Data Flow

### Opportunity chain

1. Read every active Asset Pool scan target, including assets outside Home Top 6.
2. Claim the due `AssetState` identity atomically by owner, symbol, and timeframe.
3. Evaluate source-owned Binance Spot closed windows for `5m`, `15m`, `1h`, and
   `4h`.
4. Persist the lightweight scan result, reason, freshness, structure signature,
   rule version, trace, timestamps, and the next eligible time derivable from the
   current state cadence.
5. Request the existing full analysis chain only for promotion, a legal new-candle
   recalculation, material evidence change, Hot Reset, or another frozen legal
   recalculation condition.
6. Require CoinGlass readiness before an automatic full analysis request. Missing
   credentials produce `BLOCKED_EXTERNAL_CREDENTIAL`; no Final or Telegram fact is
   fabricated.
7. A successful existing Final/Message chain can create a Telegram delivery only
   through the integrated two-category whitelist.

Normal `triggered` minute polling is lightweight. A full analysis is requested
only after a material rule-owned change; routine polls call GPT, Gemini, and Grok
zero times.

### Position chain

1. Read every `OPEN` and `PARTIALLY_CLOSED` `UserPosition`; `CLOSED` is excluded.
2. Claim each position independently so one failure cannot stop the remaining
   positions.
3. Require Binance Spot closed-window trust for all four product timeframes.
4. Use the persisted UserPosition entry, stop-loss, and take-profit facts.
5. Persist a successful monitor result only when the existing monitor contract is
   `VERIFIED` and fresh; otherwise fail closed.
6. Publish only a qualifying position business fact through the existing
   Message-after-commit and ChannelDelivery path.

No position is created or mutated by either scheduler.

## Fixed Cadences And Default Safety

- `observing`: 15 minutes
- `candidate`: 5 minutes
- `waiting_trigger`: 2 minutes
- `triggered`: 1 minute, lightweight by default
- active position monitor: 30 seconds

Git defaults remain off for global schedulers, OHLCV ingestion, Asset Pool
analysis, position monitoring, Telegram enablement, Telegram external calls, and
Telegram dispatch. Production requires explicit opt-in and the existing private
runtime safety policy. Missing or inconsistent configuration fails closed.

## OHLCV Coverage

The ingestion universe is the union of:

- all active Asset Pool symbols; and
- all active UserPosition symbols.

It is not capped to Home Top 6. The scheduler requests only Binance public Spot
closed candles, keeps provider ownership explicit, does not fall back to Kraken,
uses the existing persistence contract, and isolates provider failures by symbol
and timeframe with bounded retry backoff.

## Telegram Contract

- Three in-application Message categories remain available.
- Telegram delivery remains limited to a complete `CONFIRMATION` Final and a
  qualifying active-position material change.
- Plan safety changes remain in-app only.
- `REDUCED`, `PREPARATION`, `OBSERVATION`, `BLOCKED`, Preview, Candidate-only,
  incomplete Final, untrusted position monitor, and closed position deliveries
  remain blocked.
- The same user, plan ID, and `CONFIRMATION` identity owns at most one Telegram
  delivery for the plan lifetime. Provider failure retries that delivery; elapsed
  cooldown, restart, rescan, or a new Message ID cannot create another one.
- A new plan ID may create a new eligible delivery.
- Position changes retain their existing concrete-change cooldown and escalation
  behavior.

## Failure Isolation And Audit

- Asset scan, OHLCV ingestion, and position monitoring isolate one target failure
  and continue processing remaining targets.
- Failure audit text records an exception type rather than a provider exception
  message, avoiding accidental credential propagation.
- A full-analysis request and a successful full-analysis watermark are distinct;
  failed requests remain eligible for a legal retry.
- Scan claims and lifetime delivery lookups are persisted, so restart does not
  erase idempotency.

## PostgreSQL Runtime Remediation

The first private Staging natural-run attempt exposed one PostgreSQL-only SQL
parameter inference defect after a lightweight scan was successfully claimed.
`completeScheduledScanAudit` expressed the optional analysis trace as a standalone
`? IS NOT NULL` parameter before comparing a second placeholder with `trace_id`.
PostgreSQL could not infer the standalone null parameter type, so completion audit
writes failed closed with `BadSqlGrammarException`.

The mapper now compares both optional identities directly with the typed
`trace_id` column. A null analysis trace remains false under SQL null semantics,
while the claim trace remains the primary completion owner. No schema, state,
cadence, source, or business rule changed. Focused mapper integration coverage now
proves completion against the claim trace when the analysis trace is null, and the
existing resulting-analysis-trace path remains covered.

## Automated Validation

Focused coverage includes full-pool and non-Top-6 scanning, state cadences,
promotion and material-change gates, triggered lightweight polling, persistent
claims, restart recovery, multi-instance idempotency, dynamic OHLCV coverage,
four-timeframe source trust, single-target failure isolation, all active position
coverage, actual stop/take facts, position lock isolation, Telegram category and
lifetime-delivery rules, orphan/requeue/dispatcher defenses, and production
opt-in safety.

Java 17 full Maven local run:

- tests: 4858
- passed: 4844
- failures: 0
- errors: 0
- skipped: 14 (Docker/Testcontainers unavailable on the local runner)

Local deterministic gates:

- Product Source Gate: pass
- Core production-loop authorization validation: pass
- `git diff --check`: pass

Exact-head CI and private Staging runtime acceptance are intentionally reported
outside this document after the candidate commit exists. Runtime acceptance must
remain not ready if CoinGlass is missing, no real active position exists, no
eligible automatic Telegram event occurs, the owner does not confirm receipt, or
the required 35-minute natural soak is incomplete.

## Remaining Runtime Gates

- exact-head required checks
- exact-head artifact and private Staging deployment checksum match
- rollback JAR and PostgreSQL custom dump with verified offsite checksum
- CoinGlass private configuration presence
- at least 35 minutes of natural scheduler soak
- two eligible Asset Pool due-cycle observations, including non-Top-6 coverage
- real active-position two-cycle monitoring, when such a position exists
- one naturally eligible automatic Telegram event and owner receipt, when such an
  event occurs
- restoration of every private runtime switch to false on every exit path

These are runtime evidence gates, not implementation defaults, and are not marked
pass by this report.
