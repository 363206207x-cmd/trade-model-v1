# Fundamental AI Local-Real Ownership Map

| Concern | Existing owner to reuse | Duplicate owner allowed |
|---|---|---|
| Asset Pool scan | `PersistentAssetPoolService` | No |
| Analysis run and persistence | Existing analysis orchestrator/repositories | No |
| Local-real readiness | `LocalRealReadinessService` | No |
| Provider/readiness projection | `LocalRealDataStatusService`, `ProviderReadinessService` | No |
| Dashboard aggregate | `DashboardHomeService` and existing VOs | No |
| Opportunity Top projection | Existing Opportunity ranking/projection | No |
| User position | Existing UserPosition service/repository | No |
| Final plan | Existing FinalExecutionPlan owner | No |
| AI role state | Existing Candidate/Review/Challenge/Resolver owners | No |

## Duplicate Skeleton Gate

The successor must connect existing owners. It may not create a second Asset
Pool, readiness counter, Dashboard aggregate, Opportunity, Final, UserPosition
or Three-AI object hierarchy.

