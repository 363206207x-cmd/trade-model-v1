package org.example.trademodel.analysisrun;

import java.time.LocalDateTime;

public class AnalysisRunClaimRequest {
    private final String analysisId;
    private final String traceId;
    private final String requestId;
    private final String idempotencyKey;
    private final String symbol;
    private final String timeframe;
    private final LocalDateTime analysisTime;
    private final String ruleVersion;
    private final AnalysisRunTriggerType triggerType;
    private final String triggerReference;
    private final String parentAnalysisId;
    private final String parentTraceId;
    private final String inputSnapshotJson;
    private final String inputSnapshotHash;
    private final String leaseOwner;
    private final LocalDateTime leaseExpiresAt;
    private final String ownerType;
    private final Long ownerId;
    private final Long assetId;
    private final boolean preview;

    public AnalysisRunClaimRequest(String analysisId, String traceId, String requestId, String idempotencyKey,
                                   String symbol, String timeframe, LocalDateTime analysisTime, String ruleVersion,
                                   AnalysisRunTriggerType triggerType, String triggerReference,
                                   String parentAnalysisId, String parentTraceId,
                                   String inputSnapshotJson, String inputSnapshotHash,
                                   String leaseOwner, LocalDateTime leaseExpiresAt) {
        this(analysisId, traceId, requestId, idempotencyKey, symbol, timeframe, analysisTime,
                ruleVersion, triggerType, triggerReference, parentAnalysisId, parentTraceId,
                inputSnapshotJson, inputSnapshotHash, leaseOwner, leaseExpiresAt,
                "SYSTEM", 0L, null, false);
    }

    public AnalysisRunClaimRequest(String analysisId, String traceId, String requestId, String idempotencyKey,
                                   String symbol, String timeframe, LocalDateTime analysisTime, String ruleVersion,
                                   AnalysisRunTriggerType triggerType, String triggerReference,
                                   String parentAnalysisId, String parentTraceId,
                                   String inputSnapshotJson, String inputSnapshotHash,
                                   String leaseOwner, LocalDateTime leaseExpiresAt,
                                   String ownerType, Long ownerId, Long assetId, boolean preview) {
        this.analysisId = analysisId;
        this.traceId = traceId;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.analysisTime = analysisTime;
        this.ruleVersion = ruleVersion;
        this.triggerType = triggerType;
        this.triggerReference = triggerReference;
        this.parentAnalysisId = parentAnalysisId;
        this.parentTraceId = parentTraceId;
        this.inputSnapshotJson = inputSnapshotJson;
        this.inputSnapshotHash = inputSnapshotHash;
        this.leaseOwner = leaseOwner;
        this.leaseExpiresAt = leaseExpiresAt;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.assetId = assetId;
        this.preview = preview;
    }

    public String getAnalysisId() { return analysisId; }
    public String getTraceId() { return traceId; }
    public String getRequestId() { return requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public String getRuleVersion() { return ruleVersion; }
    public AnalysisRunTriggerType getTriggerType() { return triggerType; }
    public String getTriggerReference() { return triggerReference; }
    public String getParentAnalysisId() { return parentAnalysisId; }
    public String getParentTraceId() { return parentTraceId; }
    public String getInputSnapshotJson() { return inputSnapshotJson; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public String getLeaseOwner() { return leaseOwner; }
    public LocalDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public String getOwnerType() { return ownerType; }
    public Long getOwnerId() { return ownerId; }
    public Long getAssetId() { return assetId; }
    public boolean isPreview() { return preview; }
}
