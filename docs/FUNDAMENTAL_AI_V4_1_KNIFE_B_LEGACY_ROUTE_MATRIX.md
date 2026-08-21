# Fundamental AI v4.1 Knife B Legacy Route Matrix

Baseline: `de11316a4bfd414f6b1ea95c8ee8bfd4f9d64469`

Method: authenticated route/controller inspection plus exact-path searches across templates, JavaScript, controllers and tests. Inbound counts exclude the route's own controller declaration. No route status was changed by Knife B.

| Route | Current HTTP status | Controller | Template | Inbound reference count | Formal replacement | Action taken | Remaining decision |
|---|---:|---|---|---:|---|---|---|
| `/dashboard` | 200 | `DashboardController.dashboard` | `home.html` | 46 | Formal Home | None | Keep as the only active Desktop Home. |
| `/dashboard/analysis-detail` | 200 with valid `analysisId`; 400 without it | `AnalysisDetailController.analysisDetail` | `analysis-detail.html` | 7 | `/analysis/{analysisId}` | None; registered as `LEGACY_SURFACE` | Retire only under a separately authorized route-retirement contract. |
| `/dashboard/asset-detail` | 200 with valid `selectedSymbol`; 400 without it | `AssetDetailController.assetDetail` | `asset-detail.html` | 5 | `/analysis?asset={symbol}` | None; registered as `LEGACY_SURFACE` | Existing consumers remain, so no 410/redirect is permitted here. |
| `/dashboard/positions` | 200 | `PositionMonitoringPageController.desktopPositionMonitoring` | `position-monitoring.html` | 3 | `/positions` | None; registered as `LEGACY_SURFACE` | Existing template/JS/tests remain. |
| `/dashboard/mobile` | 200 after authentication | `MobileDashboardController.mobileDashboard` | `dashboard-mobile.html` | 6 | No Desktop replacement; legacy Mobile surface | None; registered as `LEGACY_SURFACE` | Mobile is out of Knife B scope. |
| `/dashboard/mobile/positions` | 200 after authentication | `PositionMonitoringPageController.mobilePositionMonitoring` | `position-monitoring.html` | 6 | No Desktop replacement; legacy Mobile surface | None; registered as `LEGACY_SURFACE` | Mobile is out of Knife B scope. |
| `/positions` | 200 | `DesktopWorkspaceController.positions` | `workspace.html` | 14 | Formal Positions workspace | Kept formal | Active and History tabs now use owner-scoped Position sources. |
| `/positions/{positionId}` | 200 shell; data API fails closed/404 for unowned IDs | `DesktopWorkspaceController.positionDetail` | `workspace.html` | 6 | Formal Position detail | Kept formal | Owner enforcement remains in the API projection. |
| `/analysis` | 200 | `DesktopWorkspaceController.analysis` | `workspace.html` | 12 | Formal Analysis workspace | Kept formal | Search selection prepares Preview only. |
| `/analysis/{analysisId}` | 200 shell; data API fails closed/404 for unowned IDs | `DesktopWorkspaceController.analysisDetail` | `workspace.html` | 6 | Formal Analysis hydration route | Kept formal | Uses the same R08 workspace, not a second detail shell. |

## Active Home Proof

- `DashboardController.dashboard()` returns `home` for `/dashboard`.
- No Knife B navigation or newly modified link targets a legacy `/dashboard/*` surface.
- `dashboard-mobile.html`, `analysis-detail.html`, `asset-detail.html`, and `position-monitoring.html` remain legacy surfaces; none is reclassified as a second active Home.
- No 301, 302, 410, or bulk redirect was added.
