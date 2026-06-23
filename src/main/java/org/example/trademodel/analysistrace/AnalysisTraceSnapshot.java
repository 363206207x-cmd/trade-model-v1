package org.example.trademodel.analysistrace;

import org.example.trademodel.entity.AnalysisRunDO;

import java.util.List;

public class AnalysisTraceSnapshot {
    private final String analysisId;
    private final String traceId;
    private final String requestId;
    private final String idempotencyKey;
    private final String symbol;
    private final String timeframe;
    private final String status;
    private final String triggerType;
    private final String triggerReference;
    private final String parentAnalysisId;
    private final String parentTraceId;
    private final String inputSnapshotHash;
    private final String inputSnapshotJson;
    private final List<String> evidenceIds;
    private final List<String> scoreIds;
    private final List<String> decisionIds;
    private final List<String> executionPlanIds;
    private final List<String> positionMonitorLogIds;
    private final List<String> reviewResultIds;
    private final List<String> aiCallIds;
    private final List<String> opportunityIds;
    private final int pushSnapshotCount;
    private final boolean reviewOnly = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;
    private final boolean notUserPositionCreation = true;
    private final boolean notUserPositionMutation = true;
    private final boolean notPushSend = true;
    private final boolean notExternalChannel = true;

    public AnalysisTraceSnapshot(AnalysisRunDO run,
                                 List<String> evidenceIds,
                                 List<String> scoreIds,
                                 List<String> decisionIds,
                                 List<String> executionPlanIds,
                                 List<String> positionMonitorLogIds,
                                 List<String> reviewResultIds,
                                 List<String> aiCallIds,
                                 List<String> opportunityIds,
                                 int pushSnapshotCount) {
        this.analysisId = run.getAnalysisId();
        this.traceId = run.getTraceId();
        this.requestId = run.getRequestId();
        this.idempotencyKey = run.getIdempotencyKey();
        this.symbol = run.getSymbol();
        this.timeframe = run.getTimeframe();
        this.status = run.getStatus();
        this.triggerType = run.getTriggerType();
        this.triggerReference = run.getTriggerReference();
        this.parentAnalysisId = run.getParentAnalysisId();
        this.parentTraceId = run.getParentTraceId();
        this.inputSnapshotHash = run.getInputSnapshotHash();
        this.inputSnapshotJson = run.getInputSnapshotJson();
        this.evidenceIds = evidenceIds;
        this.scoreIds = scoreIds;
        this.decisionIds = decisionIds;
        this.executionPlanIds = executionPlanIds;
        this.positionMonitorLogIds = positionMonitorLogIds;
        this.reviewResultIds = reviewResultIds;
        this.aiCallIds = aiCallIds;
        this.opportunityIds = opportunityIds;
        this.pushSnapshotCount = pushSnapshotCount;
    }

    public String getAnalysisId() { return analysisId; }
    public String getTraceId() { return traceId; }
    public String getRequestId() { return requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public String getStatus() { return status; }
    public String getTriggerType() { return triggerType; }
    public String getTriggerReference() { return triggerReference; }
    public String getParentAnalysisId() { return parentAnalysisId; }
    public String getParentTraceId() { return parentTraceId; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public String getInputSnapshotJson() { return inputSnapshotJson; }
    public List<String> getEvidenceIds() { return evidenceIds; }
    public List<String> getScoreIds() { return scoreIds; }
    public List<String> getDecisionIds() { return decisionIds; }
    public List<String> getExecutionPlanIds() { return executionPlanIds; }
    public List<String> getPositionMonitorLogIds() { return positionMonitorLogIds; }
    public List<String> getReviewResultIds() { return reviewResultIds; }
    public List<String> getAiCallIds() { return aiCallIds; }
    public List<String> getOpportunityIds() { return opportunityIds; }
    public int getPushSnapshotCount() { return pushSnapshotCount; }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotUserPositionCreation() { return notUserPositionCreation; }
    public boolean isNotUserPositionMutation() { return notUserPositionMutation; }
    public boolean isNotPushSend() { return notPushSend; }
    public boolean isNotExternalChannel() { return notExternalChannel; }
}
