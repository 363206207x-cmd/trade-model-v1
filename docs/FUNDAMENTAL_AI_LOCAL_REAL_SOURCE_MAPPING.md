# Fundamental AI Local-Real Source Mapping

## Product Contract Mapping

| Product meaning | Canonical source/owner | Authorized successor action |
|---|---|---|
| Asset Pool is the opportunity source | `PersistentAssetPoolService` and existing Asset Pool persistence | Observe normal manual scans; no second pool |
| Analysis completion is persisted truth | Existing analysis orchestrator, `AnalysisRun` and repositories | Refresh readiness only from authoritative persisted/completed results |
| Readiness is backend-owned | `LocalRealReadinessService`, `LocalRealDataStatusService`, provider health/freshness | Synchronize the existing projection; never calculate in JavaScript |
| Home is a read projection | `DashboardHomeService` and existing dashboard owners | Bind the active `/dashboard` Home to current runtime/database state |
| Opportunity is not Asset Pool count | Existing Opportunity and Home Top projection | Show only real opportunities; quality threshold remains 70 |
| Final is not Candidate | Existing FinalExecutionPlan owner | Show Final only when a real Final exists |
| Position is user-owned | Existing UserPosition and PositionMonitor owners | Never convert ExecutionPlan into UserPosition |
| Three AI state is stored/runtime truth | Existing Candidate, Review, Challenge, Resolver and Rule Validation owners | Show honest NOT_CALLED/unavailable states when absent |

No product source is redefined by this authorization.

