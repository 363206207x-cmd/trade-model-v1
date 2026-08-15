# Fundamental AI v4.1 Asset Pool Interaction Report

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

## Operation Semantics

### Top Up Defaults

`POST /api/asset-pool/defaults/top-up` adds only missing system-default assets.
User-added active assets and all historical Analysis/Opportunity/Plan/Review
records remain intact. The compatibility route `/restore-default` delegates to
the same top-up behavior.

### Reset Defaults

`POST /api/asset-pool/defaults/reset` is the separate destructive-to-current-
tracking operation and is guarded by UI confirmation. It restores the current
observing set to system defaults while preserving history and UserPosition
monitoring. Removed custom/default overrides become `TRACKING_STOPPED`; they
are not deleted.

### Search And Remove

Search supports normalized symbol, name and alias matching. Add/remove continue
to use the persistent Asset Pool owner. Removal changes the active tracking
state and cannot erase prior evidence or create a position.

## Scan CTA

- Empty Pool: top-up is primary; scan is not emphasized.
- First scan not started: start scan is primary.
- Queued/running: button is disabled and real AsyncTask stage is shown.
- Top6 available: scan is secondary.
- Failed/stale: retry scan is primary.
- `SUCCEEDED` and `PARTIAL` are terminal display states; both can be retried by
  a new explicit scan request.

## Evidence

- `/asset-pool`
- `docs/evidence/v4_1_final_p1_remediation/runtime/asset-pool-top-up-full.png`
- `docs/evidence/v4_1_final_p1_remediation/runtime/asset-pool-scan-complete-full.png`
- `PersistentAssetPoolServiceTest`
- `AssetPoolTrackingStoppedIntegrationTest`
- `BinanceMarketAssetCatalogTest`

`TOP_UP_DEFAULTS = PASS`

`RESET_DEFAULTS_CONFIRMATION = PASS`

`SCAN_CTA_STATEFUL = PASS`
