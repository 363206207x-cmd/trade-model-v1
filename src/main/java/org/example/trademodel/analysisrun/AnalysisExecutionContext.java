package org.example.trademodel.analysisrun;

import java.time.LocalDateTime;

public class AnalysisExecutionContext {
    private final String analysisId;
    private final String traceId;
    private final String requestId;
    private final String idempotencyKey;
    private final String symbol;
    private final String timeframe;
    private final LocalDateTime analysisTime;
    private final LocalDateTime canonicalAnalysisTimeBucket;
    private final String ruleVersion;
    private final AnalysisRunTriggerType triggerType;
    private final String triggerReference;
    private final String parentAnalysisId;
    private final String parentTraceId;
    private final String inputSnapshotJson;
    private final String inputSnapshotHash;
    private final String leaseOwner;
    private final Integer claimVersion;
    private final Integer attemptCount;
    private final boolean runAlreadyClaimed;

    public AnalysisExecutionContext(String analysisId, String traceId, String requestId, String idempotencyKey,
                                    String symbol, String timeframe, LocalDateTime analysisTime,
                                    LocalDateTime canonicalAnalysisTimeBucket, String ruleVersion,
                                    AnalysisRunTriggerType triggerType, String triggerReference,
                                    String parentAnalysisId, String parentTraceId,
                                    String inputSnapshotJson, String inputSnapshotHash,
                                    String leaseOwner, Integer claimVersion, Integer attemptCount,
                                    boolean runAlreadyClaimed) {
        this.analysisId = analysisId;
        this.traceId = traceId;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.analysisTime = analysisTime;
        this.canonicalAnalysisTimeBucket = canonicalAnalysisTimeBucket;
        this.ruleVersion = ruleVersion;
        this.triggerType = triggerType;
        this.triggerReference = triggerReference;
        this.parentAnalysisId = parentAnalysisId;
        this.parentTraceId = parentTraceId;
        this.inputSnapshotJson = inputSnapshotJson;
        this.inputSnapshotHash = inputSnapshotHash;
        this.leaseOwner = leaseOwner;
        this.claimVersion = claimVersion;
        this.attemptCount = attemptCount;
        this.runAlreadyClaimed = runAlreadyClaimed;
    }

    public String getAnalysisId() { return analysisId; }
    public String getTraceId() { return traceId; }
    public String getRequestId() { return requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public LocalDateTime getCanonicalAnalysisTimeBucket() { return canonicalAnalysisTimeBucket; }
    public String getRuleVersion() { return ruleVersion; }
    public AnalysisRunTriggerType getTriggerType() { return triggerType; }
    public String getTriggerReference() { return triggerReference; }
    public String getParentAnalysisId() { return parentAnalysisId; }
    public String getParentTraceId() { return parentTraceId; }
    public String getInputSnapshotJson() { return inputSnapshotJson; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public String getLeaseOwner() { return leaseOwner; }
    public Integer getClaimVersion() { return claimVersion; }
    public Integer getAttemptCount() { return attemptCount; }
    public boolean isRunAlreadyClaimed() { return runAlreadyClaimed; }
}
