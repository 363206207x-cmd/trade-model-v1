package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.vo.AssetAnalysisVO;

public class AnalysisRunResult {
    private final String status;
    private final String reasonCode;
    private final String message;
    private final String analysisId;
    private final String traceId;
    private final String requestId;
    private final String idempotencyKey;
    private final String symbol;
    private final String timeframe;
    private final String triggerType;
    private final String triggerReference;
    private final AssetAnalysisVO analysis;
    private final boolean acceptedForExecution;
    private final boolean duplicateTriggerBlocked;
    private final boolean concurrentTriggerBlocked;
    private final boolean failureRecovery;
    private final boolean expiredLeaseRecovery;
    private final boolean partialStateRecoveryBlocked;
    private final boolean maxRecoveryAttemptsExceeded;
    private final boolean reviewOnly = true;
    private final boolean manualReviewOnly = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;
    private final boolean notUserPositionCreation = true;
    private final boolean notUserPositionMutation = true;
    private final boolean notPushSend = true;
    private final boolean notExternalChannel = true;

    private AnalysisRunResult(String status, String reasonCode, String message, AnalysisRunDO run,
                              AssetAnalysisVO analysis, boolean acceptedForExecution,
                              boolean duplicateTriggerBlocked, boolean concurrentTriggerBlocked,
                              boolean failureRecovery, boolean expiredLeaseRecovery,
                              boolean partialStateRecoveryBlocked, boolean maxRecoveryAttemptsExceeded) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.message = message;
        this.analysisId = run != null ? run.getAnalysisId() : null;
        this.traceId = run != null ? run.getTraceId() : null;
        this.requestId = run != null ? run.getRequestId() : null;
        this.idempotencyKey = run != null ? run.getIdempotencyKey() : null;
        this.symbol = run != null ? run.getSymbol() : null;
        this.timeframe = run != null ? run.getTimeframe() : null;
        this.triggerType = run != null ? run.getTriggerType() : null;
        this.triggerReference = run != null ? run.getTriggerReference() : null;
        this.analysis = analysis;
        this.acceptedForExecution = acceptedForExecution;
        this.duplicateTriggerBlocked = duplicateTriggerBlocked;
        this.concurrentTriggerBlocked = concurrentTriggerBlocked;
        this.failureRecovery = failureRecovery;
        this.expiredLeaseRecovery = expiredLeaseRecovery;
        this.partialStateRecoveryBlocked = partialStateRecoveryBlocked;
        this.maxRecoveryAttemptsExceeded = maxRecoveryAttemptsExceeded;
    }

    public static AnalysisRunResult executed(AnalysisRunDO run, AssetAnalysisVO analysis,
                                             boolean failureRecovery, boolean expiredLeaseRecovery) {
        String status = failureRecovery ? "RECOVERED_FAILED_EXECUTED"
                : expiredLeaseRecovery ? "RECOVERED_EXPIRED_LEASE_EXECUTED" : "EXECUTED";
        return new AnalysisRunResult(status, "ANALYSIS_EXECUTED", "analysis run executed",
                run, analysis, true, false, false, failureRecovery, expiredLeaseRecovery, false, false);
    }

    public static AnalysisRunResult duplicateSuccess(AnalysisRunDO run) {
        return new AnalysisRunResult("DUPLICATE_SUCCESS_BLOCKED", "IDEMPOTENCY_DUPLICATE_SUCCESS",
                "duplicate trigger blocked; existing successful analysis run reused", run, null,
                false, true, false, false, false, false, false);
    }

    public static AnalysisRunResult inProgress(AnalysisRunDO run) {
        return new AnalysisRunResult("CONCURRENT_TRIGGER_BLOCKED", "IDEMPOTENCY_IN_PROGRESS",
                "concurrent trigger blocked by active analysis lease", run, null,
                false, false, true, false, false, false, false);
    }

    public static AnalysisRunResult recoveryBlocked(AnalysisRunDO run, String reasonCode, String message) {
        return new AnalysisRunResult("RECOVERY_BLOCKED", reasonCode, message, run, null,
                false, false, false, false, false, true, false);
    }

    public static AnalysisRunResult maxAttempts(AnalysisRunDO run) {
        return new AnalysisRunResult("RECOVERY_BLOCKED_MAX_ATTEMPTS", "MAX_RECOVERY_ATTEMPTS_EXCEEDED",
                "analysis run recovery attempt limit reached", run, null,
                false, false, false, false, false, false, true);
    }

    public static AnalysisRunResult failed(AnalysisRunDO run, String message) {
        return new AnalysisRunResult("FAILED", "ANALYSIS_EXECUTION_FAILED", message, run, null,
                false, false, false, false, false, false, false);
    }

    public String getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
    public String getAnalysisId() { return analysisId; }
    public String getTraceId() { return traceId; }
    public String getRequestId() { return requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public String getTriggerType() { return triggerType; }
    public String getTriggerReference() { return triggerReference; }
    public AssetAnalysisVO getAnalysis() { return analysis; }
    public boolean isAcceptedForExecution() { return acceptedForExecution; }
    public boolean isDuplicateTriggerBlocked() { return duplicateTriggerBlocked; }
    public boolean isConcurrentTriggerBlocked() { return concurrentTriggerBlocked; }
    public boolean isFailureRecovery() { return failureRecovery; }
    public boolean isExpiredLeaseRecovery() { return expiredLeaseRecovery; }
    public boolean isPartialStateRecoveryBlocked() { return partialStateRecoveryBlocked; }
    public boolean isMaxRecoveryAttemptsExceeded() { return maxRecoveryAttemptsExceeded; }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotUserPositionCreation() { return notUserPositionCreation; }
    public boolean isNotUserPositionMutation() { return notUserPositionMutation; }
    public boolean isNotPushSend() { return notPushSend; }
    public boolean isNotExternalChannel() { return notExternalChannel; }
}
