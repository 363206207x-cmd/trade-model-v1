# CoinGlass Derivatives Business Integration

## Package

- Package: `BIZ-1`
- Branch: `codex/biz1-coinglass-business-integration`
- Base main: `628973d50d55e6331bd446f750184ccfa8a0c4c2`
- Scope: deterministic business adoption of the cached CG-1 derivatives snapshot
- Production readiness: `BLOCKED`
- Live CoinGlass verification: `NOT_RUN`
- Live provider calls made by this package: `0`

This package connects the existing normalized `DerivativesRiskSnapshot` to evidence, the eight-score model, rule decisions, opportunity state, internal Push/Recheck, execution-plan readiness, manual-position monitoring, Confused inputs, Hot Reset candidates, and Dashboard Home. It does not add a second provider call path and it does not authorize a trading action.

## Ownership And Call Boundary

The business chain is:

```text
ProviderCallCoordinator
  -> four isolated CoinGlass dataset snapshots
  -> CoinGlassDerivativesSnapshotService cache
  -> CoordinatedDerivativesSnapshotReadAdapter.peek(...)
  -> DerivativesBusinessIntegrationService
  -> Evidence / Scores / Decision / State / Push / Plan / Monitor / Home
```

All BIZ-1 consumers use cache-only `peek` reads. They cannot call the CoinGlass transport adapter. Position price ticks do not refresh CoinGlass. Routine provider scans do not call AI. Missing or stale derivatives data remains explicit and fails closed.

## Evidence Mapping

Source-backed evidence types include:

- OI expansion, contraction, price/OI divergence, and OHLCV-volume-confirmed price/OI alignment.
- Funding positive/negative extreme and funding-direction conflict.
- Long/short crowding and crowding-direction conflict.
- Liquidation spike and liquidation imbalance.
- Exchange concentration risk.
- Derivatives partial, stale, and unavailable states.

`exchangeConcentrationScore` has one canonical unit: a decimal ratio in the range `0.0..1.0` computed as the largest valid exchange OI divided by aggregate OI. The provider value is not multiplied by 100. The default high-concentration threshold is `0.70`, which means 70%; values at the boundary are included. A configured threshold outside `(0.0, 1.0]` fails closed to `0.70` and records `RULE_CONFIG_OUT_OF_RANGE:derivatives_evidence_config.exchange_concentration_high`.

Each evidence item preserves symbol, direction, strength, confidence, current/comparison values, timeframe, provider, provider data time, fetch time, source/freshness states, source field, reason code, trace ID, analysis ID, and rule version. Missing numeric values remain null and are never converted to zero.

## Eight-Score Contract

CoinGlass evidence has bounded contributions to all existing score categories:

1. trend structure
2. capital momentum
3. leverage risk
4. liquidity quality
5. sentiment temperature
6. event impact
7. macro environment
8. overall credibility

The assembler requires all eight real score values before creating the eight-score composite. Incomplete sets produce no composite; missing values are not synthesized. The composite can adjust the existing rule score by at most `-10..+10`. The 4h rule direction remains authoritative, so derivatives evidence and the score aggregate cannot independently flip direction.

## Multi-Timeframe Decision Rule

- Formal timeframes: `5m`, `15m`, `1h`, `4h`.
- `4h` owns the base direction.
- At least three of four timeframes must align with 4h.
- `4h` and `1h` must agree for formal convergence.
- `1m` is not a formal execution-plan direction.
- OI and Funding are required for a confirm path when the configured requirement is enabled.
- Partial data can retain observation/review paths but cannot silently become complete evidence.

CoinGlass is confirmation, downgrade, and risk evidence only. It does not create a direction.
Price/OI alignment becomes strong confirmation only when the current authoritative 5m volume is at least the previous closed bar volume; missing or weaker volume leaves the alignment unconfirmed.

## Decision And Opportunity Boundaries

The derivatives assessment may:

- reduce confidence;
- raise risk;
- downgrade plan mode to prepare/warning only;
- keep a candidate observing or waiting for trigger;
- block confirm when required datasets are unavailable or stale;
- add driver-conflict, execution-instability, microstructure-trap, and cause/effect-divergence inputs to Confused evaluation.

It may not:

- reverse the rule direction;
- create a UserPosition;
- open, close, reduce, or reverse a position;
- submit an order;
- enable auto-trading.

Opportunity discovery remains the existing asset-state path. A trigger requires formal timeframe convergence, complete plan boundaries, acceptable risk, and required derivatives readiness. High-risk or Confused states remain fail-closed.

## Push And Recheck

Internal Push snapshots record `PREPARE`, `CONFIRM`, or `WARNING` derivatives mode plus required/status/freshness/provider-time/trace/reason metadata.

Push Recheck uses the same cached snapshot:

- missing required derivatives -> fail-closed invalidation with `DERIVATIVES_REQUIRED` or `DERIVATIVES_UNAVAILABLE`;
- stale derivatives -> fail-closed invalidation with `DERIVATIVES_STALE`;
- partial but usable derivatives -> review waiting with `DERIVATIVES_PARTIAL`;
- valid caller price behavior remains unchanged.

No external Push or Telegram send is added.

## Execution Plan And Position Monitor

Execution-plan entry, stop, targets, and invalidation boundaries remain OHLCV-owned. Derivatives data can only downgrade readiness, request revalidation, reduce leverage/position suggestions, or add a conservative reason. It cannot invent a boundary.

Position Monitor reads the shared cached derivatives snapshot on the configured 60-second cadence. It may add high-risk, weakened-logic, or manual-review/revalidation reasons to a real open manual position. It never changes the position status and never creates a position. Closed positions remain excluded.

## Confused And Hot Reset

Cause/effect and crowding/liquidation conflicts feed the existing Confused calculation through bounded sub-score deltas. They do not bypass the existing Confused policy.

An OI-collapse or liquidation-spike assessment may create an existing `HotResetCandidateCommand`. The existing Hot Reset service remains the policy/persistence owner. BIZ-1 does not add an automatic trade action to Hot Reset.

## Rule Configuration

Twenty-four defaults are versioned in `tm_rule_config` for:

- `derivatives_evidence_config`
- `derivatives_score_config`
- `derivatives_decision_config`
- `derivatives_monitor_config`

H2 bootstrap and PostgreSQL Flyway `V6` carry the same keys. Invalid/missing config uses explicit conservative defaults and records fallback reasons; no fallback silently relaxes a blocking rule.
The score caps, eight-score adjustment cap/factor, minimum confirmation data quality, and monitor refresh cadence are consumed from those keys rather than remaining production-only constants.

## Traceability

The derivatives evidence and assessment carry provider/source/freshness time, trace ID, analysis ID, rule version, reason codes, and source field. Decision, internal Push, plan readiness, monitor reason, Hot Reset candidate, and Dashboard Home consume that assessment without a second source read.

Traceability is `PARTIAL` at the release level because production CoinGlass live evidence and a real end-to-end review replay are not part of this package. This package does not claim either as PASS.

## Deterministic Test Evidence

The focused BIZ-1 suite covers more than forty deterministic cases across:

- price/OI direction and divergence;
- positive/negative funding extremes;
- long/short crowding;
- liquidation and exchange concentration;
- partial/stale/unavailable/missing mandatory datasets;
- all eight score contributions and composite behavior;
- direction preservation and four-timeframe convergence;
- opportunity state progression and fail-closed trigger rules;
- plan boundary ownership and suggestion downgrade;
- position-monitor manual-review semantics;
- Push Recheck shared-cache and stale failure;
- Dashboard Home semantic summary;
- no UserPosition creation, no AI call, no external send, and no trading actions;
- H2/Flyway configuration alignment.

## Remaining Gaps

- Real CoinGlass authenticated smoke remains not run because this package made zero live provider calls.
- Opportunity triggering reuses the existing read-only `UserPositionRiskAdapter` and fails closed when that risk view is unavailable; BIZ-1 does not introduce a new account-risk owner.
- Funding rapid-reversal Hot Reset input remains unavailable because the normalized snapshot has no prior-funding comparison field; a single extreme value is not relabeled as a reversal.
- Review-level full trace replay remains partial until a real/versioned provider-backed scenario is accepted.
- Production deployment gates remain incomplete.

## Safety Statement

This package adds no auto-open, auto-close, auto-reverse, order execution, auto-trading, external Push send, fake position, or fake review record. Production deployment remains blocked.
