# Frontend Interaction Runtime Closure Ownership Map

| Concern | Reused owner | Duplicate owner allowed |
|---|---|---|
| Asset catalog search | Existing catalog/search controller and service | No |
| Observation membership | Existing Asset Pool service | No |
| Analysis preview | Existing analysis-preview service/API | No |
| Home runtime | Existing approved Home template/runtime | No |
| Route and overlay state | Existing Desktop controllers/templates | No |
| Position facts | Existing UserPosition/Position Monitoring owners | No |
| Final plan | Existing validated FinalExecutionPlan owner | No |

Duplicate Skeleton Gate: PASS when the implementation only binds these owners
and creates no replacement business object, service or persistence model.
