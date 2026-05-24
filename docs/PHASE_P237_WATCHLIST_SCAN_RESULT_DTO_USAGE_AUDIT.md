# P237 Watchlist Scan Result DTO Usage Audit

## 1. 阶段定位

本文只审计 `WatchlistScanResultDTO` 是否足够承载 review-only / blocked / incomplete。

本文不实现 Java。

本文不改 DTO。

## 2. 审计问题

### 当前是否已有 symbol？

是。`WatchlistScanResultDTO` 当前已有 `symbol` 字段和 `getSymbol()`。

### 当前是否已有 status / scanStatus？

是。`WatchlistScanResultDTO` 当前已有 `scanStatus` 字段，类型为 `WatchlistScanStatusEnum`，并有 `getScanStatus()`。

### 当前是否能表达 INCOMPLETE？

是。`WatchlistScanStatusEnum` 当前包含 `INCOMPLETE`，`WatchlistScanResultDTO.incomplete(...)` 可生成该状态。

### 当前是否能表达 BLOCKED？

部分足够。当前没有通用 `BLOCKED` enum，但已有 `BLOCKED_NOT_WATCHLIST`，且 `dataQualityStatus` 可为 `BLOCKED`。未来 source unavailable 如需通用 blocked，可以先用 `INCOMPLETE` 或 `BLOCKED_NOT_WATCHLIST` 之外的 `blockingReasons` 表达；不得本轮补字段。

### 当前是否能表达 REVIEW_ONLY / AVAILABLE_REVIEW_ONLY？

是。`WatchlistScanStatusEnum` 当前包含 `REVIEW_ONLY`，`WatchlistScanResultDTO.reviewOnly(...)` 可表达 review-only。`AVAILABLE_REVIEW_ONLY` 可映射为 `REVIEW_ONLY`，并在 `blockingReasons` 中保留来源原因。

### 当前是否有 missingFields？

否。`WatchlistScanResultDTO` 当前没有单独 `missingFields` 字段。

当前可临时通过 `blockingReasons` 承载 `MISSING_RUNTIME_SOURCE`、`MISSING_FIELDS` 或具体缺失字段名。若后续需要一等字段，必须另开 DTO 授权门。

### 当前是否有 blockingReasons？

是。`WatchlistScanResultDTO` 当前已有 `blockingReasons` 字段和防御性复制 getter。

### 当前是否有 manualReviewRequired？

是。`WatchlistScanResultDTO` 当前已有 `manualReviewRequired`，构造时固定为 `true`。

### 当前是否有 notTradeInstruction？

是。`WatchlistScanResultDTO` 当前已有 `notTradeInstruction`，构造时固定为 `true`。

### 当前是否有 opportunityPushAllowed？

是。`WatchlistScanResultDTO` 当前已有 `opportunityPushAllowed`，构造时固定为 `false`。

### 当前是否有 readinessUpgraded？

是。`WatchlistScanResultDTO` 当前已有 `readinessUpgraded`，构造时固定为 `false`。

### 当前是否有 tradingActionCreated？

是。`WatchlistScanResultDTO` 当前已有 `tradingActionCreated`，构造时固定为 `false`。

### 当前是否有 entryStopTpRrGenerated？

是。`WatchlistScanResultDTO` 当前已有 `entryStopTpRrGenerated`，构造时固定为 `false`。

### 当前是否包含 ScanScore 字段？

否。`WatchlistScanResultDTO` 当前未包含 `ScanScore` 字段。

如果后续出现 score 字段或 score output，P237 不允许使用真实分数，必须另开授权门。

### 当前是否足够支持 no-score / no-push / no-readiness 语义？

是，当前字段足够支持 no-score / no-push / no-readiness 语义：

- 无 `ScanScore` 字段。
- `opportunityPushAllowed=false`。
- `readinessUpgraded=false`。
- `tradingActionCreated=false`。
- `entryStopTpRrGenerated=false`。
- `manualReviewRequired=true`。
- `notTradeInstruction=true`。

## 3. 审计结论格式

### 已足够字段

当前已足够支持最小 review-only skeleton 的字段：

- `symbol`
- `watchlistMember`
- `scanStatus`
- `scanReason`
- `dataQualityStatus`
- `blockingReasons`
- `manualReviewRequired`
- `notTradeInstruction`
- `opportunityPushAllowed`
- `readinessUpgraded`
- `tradingActionCreated`
- `entryStopTpRrGenerated`

当前也已有 `candidateAttentionAllowed` 和 `promoteToHomeAllowed`，但 P237 不授权使用它们触发 Candidate Attention 或 Promote To Home。

### 不足字段

当前不足或需要谨慎的字段：

- 没有单独 `missingFields`。
- 没有单独 `staleFields`。
- 没有通用 `SOURCE_UNAVAILABLE` scan status。
- 没有通用 `GUARD_BLOCKED` scan status。
- 没有 `RuntimeSourceReadResultDTO` 到 scan result 的 assembler。

### 暂不需要新增字段的理由

P237 是 docs-only usage audit，不允许补 DTO 字段。

最小 review-only / blocked / incomplete skeleton 可以先使用现有 `scanStatus` + `blockingReasons` + `dataQualityStatus` 表达。

如果后续发现 `missingFields`、`staleFields`、`SOURCE_UNAVAILABLE` 或 `GUARD_BLOCKED` 必须成为一等字段，必须另开 DTO authorization gate。

### 是否可以先用现有字段表达 review-only skeleton

可以。现有字段足够表达一个不打分、不推送、不升级 readiness、不生成点位的 review-only scan result skeleton。

## 4. 未来最小 Java 候选

仅作为方案，后续可能考虑：

- `WatchlistScanResultAssemblyService` 或 `WatchlistScanResultAssembler`
- `DefaultWatchlistScanResultAssembler`
- `DefaultWatchlistScanResultAssemblerTest`

具体是否新增以后续授权门为准。

## 5. 结论

P237 不改 DTO。

如果 DTO 不足，不允许本轮补字段。

后续补 DTO 必须另开授权门。
