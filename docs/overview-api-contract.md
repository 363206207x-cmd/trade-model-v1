# Push Recheck Ops Overview API Contract

## 1. Purpose

This document fixes the contract for the minimal read-only operations overview API introduced in:

- Phase 10
- Module 4
- Round 2
- Step 4/5 follow-up alignment

This API is **not** a task center, **not** a platformized operations system, and **not** a dashboard redesign.
It is only used for:

- minimal ops read-only query
- frontend / QA integration alignment
- acceptance verification
- preventing contract drift between implementation and docs

---

## 2. Endpoint

```http
GET /api/push/recheck/ops/overview
```

Supported query parameters:

- `dispatchBatchId` (optional)
- `dispatchInstructionId` (optional)
- `auditLimit` (optional)
- `logLimit` (optional)

---

## 3. Response Envelope

Current project implementation uses `ApiResponse.success()` and returns:

- `code = 200`
- `msg = "success"`

Example:

```json
{
  "code": 200,
  "msg": "success",
  "requestId": "req-1713348000000",
  "serverTime": "2026-04-17T15:20:00",
  "data": {
    "config": {
      "limit": 50,
      "maxAttempts": 3,
      "minRetryMinutes": 15,
      "updatedBy": "system_admin",
      "updatedTime": "2026-04-17T14:50:00"
    },
    "auditSummary": {
      "auditCount": 5,
      "latestAuditTime": "2026-04-17T14:50:00",
      "latestAuditOperator": "system_admin",
      "latestAuditSummary": "Updated dispatch config: maxAttempts 2 -> 3, minRetryMinutes 10 -> 15"
    },
    "latestReplaySummary": {
      "dispatchBatchId": "batch-20260417-001",
      "dispatchInstructionId": "instruction-20260417-003",
      "triggerSource": "REPLAY",
      "totalCount": 6,
      "successCount": 2,
      "blockingCount": 2,
      "waitingCount": 1,
      "expiredCount": 1,
      "replayCount": 6,
      "latestExecutionStatus": "RISK_BLOCKED",
      "latestExecutionTime": "2026-04-17T15:05:12",
      "hasError": false,
      "latestErrorCode": null
    },
    "recentLogs": [
      {
        "logId": 12018,
        "dispatchBatchId": "batch-20260417-001",
        "dispatchInstructionId": "instruction-20260417-003",
        "triggerSource": "REPLAY",
        "executionStatus": "RISK_BLOCKED",
        "executionErrorCode": null,
        "createTime": "2026-04-17T15:05:12"
      },
      {
        "logId": 12017,
        "dispatchBatchId": "batch-20260417-001",
        "dispatchInstructionId": "instruction-20260417-002",
        "triggerSource": "REPLAY",
        "executionStatus": "VALID_EXECUTABLE",
        "executionErrorCode": null,
        "createTime": "2026-04-17T15:04:40"
      },
      {
        "logId": 12016,
        "dispatchBatchId": "batch-20260417-001",
        "dispatchInstructionId": "instruction-20260417-001",
        "triggerSource": "REPLAY",
        "executionStatus": "EXPIRED",
        "executionErrorCode": null,
        "createTime": "2026-04-17T15:03:58"
      }
    ]
  }
}
```

---

## 4. Data Blocks

The `data` section contains exactly 4 blocks:

- `config`
- `auditSummary`
- `latestReplaySummary`
- `recentLogs`

### 4.1 config

Current dispatch config summary.

Fields:

- `limit`
- `maxAttempts`
- `minRetryMinutes`
- `updatedBy`
- `updatedTime`

### 4.2 auditSummary

Latest config audit summary.

Fields:

- `auditCount`
- `latestAuditTime`
- `latestAuditOperator`
- `latestAuditSummary`

### 4.3 latestReplaySummary

Latest replay summary, reusing `PushRecheckReplaySummaryVO`.

Fields:

- `dispatchBatchId`
- `dispatchInstructionId`
- `triggerSource`
- `totalCount`
- `successCount`
- `blockingCount`
- `waitingCount`
- `expiredCount`
- `replayCount`
- `latestExecutionStatus`
- `latestExecutionTime`
- `hasError`
- `latestErrorCode`

### 4.4 recentLogs

Recent execution log excerpts.

Fields:

- `logId`
- `dispatchBatchId`
- `dispatchInstructionId`
- `triggerSource`
- `executionStatus`
- `executionErrorCode`
- `createTime`

---

## 5. Empty / Zero Value Contract

### 5.1 config

If config exists:

- return real values

If config does not exist:

- return an object instead of `null`
- numeric fields use service default values or `0`
- `updatedBy` may be `null`
- `updatedTime` may be `null`

### 5.2 auditSummary

If no audit exists:

```json
{
  "auditCount": 0,
  "latestAuditTime": null,
  "latestAuditOperator": null,
  "latestAuditSummary": null
}
```

### 5.3 latestReplaySummary

If no matching replay / log exists:

- return a zero summary object instead of `null`

Rules:

- `totalCount = 0`
- `successCount = 0`
- `blockingCount = 0`
- `waitingCount = 0`
- `expiredCount = 0`
- `replayCount = 0`
- `latestExecutionStatus = null`
- `latestExecutionTime = null`
- `latestErrorCode = null`
- `hasError = false`

Additionally:

- `dispatchBatchId` may echo request value
- `dispatchInstructionId` may be `null`
- `triggerSource` may be `null`

### 5.4 recentLogs

If no data exists:

- return `[]`
- do not return `null`

---

## 6. Aggregation Rules

Current replay summary aggregation is based on existing `tm_push_recheck_log` records only.
No new summary table is introduced.

Mapping rules:

- `VALID_EXECUTABLE -> successCount`
- `VALID_WAITING -> waitingCount`
- `EXPIRED -> expiredCount`
- `INVALIDATED -> blockingCount`
- `DRIFTED -> blockingCount`
- `RISK_BLOCKED -> blockingCount`
- `CONFUSED_BLOCKED -> blockingCount`

Replay count rule:

- `triggerSource = REPLAY -> replayCount`

Latest execution rule:

- latest status is taken from the newest log record
- current implementation uses descending `logId` ordering
- `latestExecutionStatus` and `latestExecutionTime` must correspond to that latest record

Error rule:

- if execution error exists, `hasError = true`
- `latestErrorCode` uses the latest available error code
- if no error exists, `latestErrorCode = null`

---

## 7. Minimal Integration Checklist

### 7.1 Envelope validation

Verify response always contains:

- `code`
- `msg`
- `requestId`
- `serverTime`
- `data`

### 7.2 Data block validation

Verify `data` always contains:

- `config`
- `auditSummary`
- `latestReplaySummary`
- `recentLogs`

### 7.3 Empty data validation

Use a non-existing batch id, for example:

```http
GET /api/push/recheck/ops/overview?dispatchBatchId=batch-not-exist
```

Expected behavior:

- HTTP 200
- no 500 error
- no missing fields
- `auditSummary.auditCount = 0` if no audit
- `latestReplaySummary.totalCount = 0`
- all replay counters are `0`
- `recentLogs = []`

### 7.4 Replay aggregation validation

Verify mapping:

- `VALID_EXECUTABLE -> successCount`
- `VALID_WAITING -> waitingCount`
- `EXPIRED -> expiredCount`
- `INVALIDATED | DRIFTED | RISK_BLOCKED | CONFUSED_BLOCKED -> blockingCount`
- `triggerSource = REPLAY -> replayCount`

### 7.5 Latest status validation

Verify latest log record is used:

- sorted by latest `logId`
- `latestExecutionStatus` matches the latest record
- `latestExecutionTime` matches the latest record

### 7.6 Error validation

If latest replay contains error:

- `hasError = true`
- `latestErrorCode` is not `null`

If no error:

- `latestErrorCode = null`

---

## 8. Out of Scope

This contract explicitly does not include:

- task center
- large dashboard page
- platformized replay system
- complex reports
- multi-dimensional filtering center
- operations platform redesign
- new summary persistence tables
- dashboard module refactor

---

## 9. Change Control

Any future field rename, nullability change, or response envelope change must update:

- implementation
- test cases
- this contract document

to avoid contract drift.
