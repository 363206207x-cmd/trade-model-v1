# Network And DOM Identity Record

Runtime source head: `38b1faa7df3d374da25edc6b8723ad0297b198ae`

## UI-review API identity

| Requested ID | HTTP | projection.position.id | monitor.positionId | Asset | Direction | Source |
| --- | ---: | ---: | ---: | --- | --- | --- |
| 7101 | 200 | 7101 | 7101 | BTCUSDT | LONG | SYSTEM_PLAN_POSITION |
| 7102 | 200 | 7102 | 7102 | ETHUSDT | SHORT | MANUAL_INDEPENDENT |
| 7103 | 200 | 7103 | 7103 | SOLUSDT | LONG | SYSTEM_PLAN_POSITION |
| 7999 | 404 | n/a | n/a | n/a | n/a | n/a |

The browser performed the required Home and list clicks. Each hydrated detail
contained exactly one `[data-position-id]`, matching the route and API ID.
Home links preserved `/dashboard?asset=BTCUSDT`; list links preserved
`/positions?tab=active`.

## Close-entry safety

- Home visible close-action count: `0`
- `/positions` visible close-action count: `0`
- Active detail close action: visible
- O07 bound position: `7101`
- O07 heading: `记录平仓 · BTCUSDT`
- UI-review manual-close POST count: `0`
- UI-review fixture write operations: `0`

## Normal-profile isolation

- `/api/workspace/positions/monitoring`: HTTP `200`, active count `0`
- Fixture IDs 7101/7102/7103 in normal list: `0`
- `/api/workspace/positions/7101/monitoring`: HTTP `404`
- Normal source remains authenticated-user, owner-scoped `UserPosition` read
- Production manual-close controller/service changed files: `0`

## Validation

- Java 17 compile: `PASS`
- Directed tests: `PASS`
- Full Maven: `4771` tests, `4757` passed, `14` skipped, `0` failures, `0` errors
- Product Source Gate: `PASS`
- Workflow Contract: `PASS`
- JavaScript syntax: `PASS`
- `git diff --check`: `PASS`
- Schema/Flyway changes: `0`
