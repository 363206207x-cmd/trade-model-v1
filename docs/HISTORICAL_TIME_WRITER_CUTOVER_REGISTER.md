# Historical Time Writer Cutover Register

## Status Contract

This register separates merged code from operational deployment evidence.
Local tests, disposable PostgreSQL fixtures, a branch, or a merged commit do
not prove that a production writer was deployed or that a first post-cutover
row is UTC-verifiable.

Allowed local code status: `CODE_MERGED`.

Required operational status until real redacted evidence exists:
`MISSING_OPERATIONAL_EVIDENCE`.

Production readiness remains `BLOCKED`.

## Register

| Writer | Code status | Code merge commit | Actual deployed commit | Actual deployment time | Startup-log time | DB migration/service restart time | First verifiable new record time | Trusted reference | Operator | Approval status |
|---|---|---|---|---|---|---|---|---|---|---|
| `monitorAlertUtcWriterCutover` | `CODE_MERGED` | `ace5e560d35f214499a06d5478361318d371ee65` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` |
| `hotResetUtcWriterCutover` | `CODE_MERGED` | `ace5e560d35f214499a06d5478361318d371ee65` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` |
| `analysisTimeContractCutover` | `CODE_MERGED` | `e9b78697f2c2baba9ed7565f6b0b9658fbe4e419` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` | `MISSING_OPERATIONAL_EVIDENCE` |

## Evidence Required To Change Status

Each writer requires all of the following before it can be classified as a
verified operational cutover:

1. The exact deployed commit, not merely a merged-main commit.
2. Deployment time represented as an instant.
3. Redacted application startup-log evidence.
4. Relevant database migration or service restart time.
5. The first row whose writer version and reference instant are both proven.
6. A trusted evidence reference that does not expose business data or secrets.
7. Named operator confirmation and release-owner approval.

One writer's evidence cannot approve another writer. Aggregate timestamp
patterns cannot substitute for deployment and first-row evidence. Historical
rows remain unverified and must not be shifted automatically.

## P3 Status

P3 input is currently `BLOCKED_MISSING_SANITIZED_RELEASE_LIKE_DUMP`, so no
release-like historical inventory or backup/restore evidence was collected.
Even a future P3 PASS can only record local clone behavior; it cannot establish
the production cutover fields above.

No production database was accessed and no historical row was modified.
