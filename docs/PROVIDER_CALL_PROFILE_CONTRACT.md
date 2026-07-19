# Provider Call Profile Contract

## User and Runtime Profiles

Users may select `AUTO`, `LOW`, `STANDARD`, or `HIGH`. `EMERGENCY` is
system-only and cannot be selected through the API or Dashboard.

For each canonical instrument:

```text
effectiveProfile = max(
  user base profile,
  active-position safety floor,
  volatility/event/risk escalation
)
```

`AUTO` resolves to the normal `STANDARD` base. A manual `HIGH` is never
automatically reduced. A `LOW` active position remains `LOW`, whose price
cadence is bounded to 15 seconds. System escalation is per instrument, so an
emergency on BTC cannot elevate unrelated assets.

## Default Frequency Matrix

| Profile | Position price | Watchlist price | Candidate price | Discovery scan | Position derivatives | Full analysis debounce |
|---|---:|---:|---:|---:|---:|---:|
| LOW | 15s | 60s | 120s | 900s | 120s | 300s |
| STANDARD | 10s | 30s | 60s | 600s | 60s | 60s |
| HIGH | 5s | 15s | 30s | 300s | 60s | 20s |
| EMERGENCY | 3s | 5s affected | 10s affected | low/paused 900s | minimum 40s | 20s |

OHLCV, derivatives, external context, full-analysis debounce, and AI checkpoint
debounce are independent settings. A price refresh never implies a derivatives,
OHLCV, external-context, or AI refresh.

`frequencyMatrixVersion` is a stable hash of the formal cadence configuration.
Equal configuration produces equal versions. Scan plans, call audits, candidate
promotion results, and notification eligibility results carry this version.

## Escalation and Recovery

Reason codes include `ACTIVE_POSITION`, `NEAR_USER_STOP`,
`NEAR_USER_TARGET`, `VOLATILITY_SPIKE`, `STRONG_REVERSAL`, `HIGH_RISK`,
`CONFUSED`, `HOT_RESET`, `EXTERNAL_EVENT`, `MANUAL_HIGH`, and
`RECOVERY_HYSTERESIS`.

HIGH and EMERGENCY have configurable minimum holds. Recovery requires
consecutive confirmation, observes cooldown, and moves down one level at a
time. All calculations use an injected UTC clock. Changing a profile only
changes future due-time decisions; it never invokes a provider or AI.

## Read-Only and Execution Ownership

Profile planning has two explicit paths:

- `currentUniverse()` and `currentPlan()` are side-effect-free reads for the
  Dashboard, runtime-status API, and single-asset profile API. They read an
  existing transition through `ScanProfileTransitionService.current()` and do
  not create state, advance recovery cycles, change timing fields, or write
  transition audit.
- `evaluateUniverseForExecution(scanCycleTraceId)` and
  `planForExecution(scanCycleTraceId)` are the Scheduler-only mutation path.
  One real scan cycle evaluates each relevant canonical instrument at most
  once; PRICE, OHLCV, DERIVATIVES, and EXTERNAL_CONTEXT reuse that cycle's
  effective profile.

Scheduler-disabled, provider-call-disabled, and missing-refresh-port paths
return before execution planning. Recovery confirmation is therefore owned by
real Scheduler scan cycles only. `current()` preserves the last real
evaluation reason for display without evaluating thresholds again. Transition
audit is written only when an execution evaluation actually changes profile.

## API and Persistence

- `GET /api/provider-call/base-profile`
- `PUT /api/provider-call/base-profile`
- `GET /api/provider-call/runtime-status`

The endpoints require current administrator authentication and expose no
secret. The existing single-user config owner is reused; no new table or
migration is introduced. Runtime status exposes base/effective profiles,
reasons, downgrade timing, cadence, budget, health, counts, and notification
scope without scheduling a refresh.

Branch evidence for this separation is offline and pending review. It does not
prove live provider or production operation.

Production readiness remains `BLOCKED`.
