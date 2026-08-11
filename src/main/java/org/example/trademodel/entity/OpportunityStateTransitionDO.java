package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class OpportunityStateTransitionDO {
    private String transitionId;
    private String opportunityId;
    private String symbol;
    private String timeframe;
    private String analysisId;
    private String traceId;
    private String fromState;
    private String toState;
    private String reason;
    private String triggerSource;
    private Integer transitionPriority;
    private Boolean suppressed;
    private LocalDateTime occurredAt;

    public String getTransitionId() { return transitionId; }
    public void setTransitionId(String transitionId) { this.transitionId = transitionId; }
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getFromState() { return fromState; }
    public void setFromState(String fromState) { this.fromState = fromState; }
    public String getToState() { return toState; }
    public void setToState(String toState) { this.toState = toState; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public Integer getTransitionPriority() { return transitionPriority; }
    public void setTransitionPriority(Integer transitionPriority) { this.transitionPriority = transitionPriority; }
    public Boolean getSuppressed() { return suppressed; }
    public void setSuppressed(Boolean suppressed) { this.suppressed = suppressed; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
