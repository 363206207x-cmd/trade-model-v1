package org.example.trademodel.service;

import org.example.trademodel.enums.HotResetEventTypeEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HotResetResult {
    private String eventId;
    private String eventKey;
    private HotResetEventTypeEnum eventType;
    private boolean triggered;
    private boolean deduplicated;
    private String analysisId;
    private String rebuildAnalysisId;
    private String symbol;
    private String timeframe;
    private String preState;
    private String postState;
    private int decisionInvalidatedCount;
    private int planRevalidationCount;
    private int pushInvalidatedCount;
    private Integer confusedScoreBefore;
    private Integer confusedScoreAfter;
    private String accountRiskStatus;
    private String accountRiskLevel;
    private boolean accountRiskBlocked;
    private boolean rebuildTriggered;
    private String executionStatus;
    private List<String> reasonCodes = new ArrayList<>();
    private LocalDateTime occurredAt;
    private LocalDateTime completedAt;

    private final boolean reviewOnly = true;
    private final boolean manualReviewOnly = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;
    private final boolean notUserPositionCreation = true;
    private final boolean notUserPositionMutation = true;
    private final boolean notAutoClose = true;
    private final boolean notAutoReverse = true;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public HotResetEventTypeEnum getEventType() { return eventType; }
    public void setEventType(HotResetEventTypeEnum eventType) { this.eventType = eventType; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public boolean isDeduplicated() { return deduplicated; }
    public void setDeduplicated(boolean deduplicated) { this.deduplicated = deduplicated; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getRebuildAnalysisId() { return rebuildAnalysisId; }
    public void setRebuildAnalysisId(String rebuildAnalysisId) { this.rebuildAnalysisId = rebuildAnalysisId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getPreState() { return preState; }
    public void setPreState(String preState) { this.preState = preState; }
    public String getPostState() { return postState; }
    public void setPostState(String postState) { this.postState = postState; }
    public int getDecisionInvalidatedCount() { return decisionInvalidatedCount; }
    public void setDecisionInvalidatedCount(int decisionInvalidatedCount) { this.decisionInvalidatedCount = decisionInvalidatedCount; }
    public int getPlanRevalidationCount() { return planRevalidationCount; }
    public void setPlanRevalidationCount(int planRevalidationCount) { this.planRevalidationCount = planRevalidationCount; }
    public int getPushInvalidatedCount() { return pushInvalidatedCount; }
    public void setPushInvalidatedCount(int pushInvalidatedCount) { this.pushInvalidatedCount = pushInvalidatedCount; }
    public Integer getConfusedScoreBefore() { return confusedScoreBefore; }
    public void setConfusedScoreBefore(Integer confusedScoreBefore) { this.confusedScoreBefore = confusedScoreBefore; }
    public Integer getConfusedScoreAfter() { return confusedScoreAfter; }
    public void setConfusedScoreAfter(Integer confusedScoreAfter) { this.confusedScoreAfter = confusedScoreAfter; }
    public String getAccountRiskStatus() { return accountRiskStatus; }
    public void setAccountRiskStatus(String accountRiskStatus) { this.accountRiskStatus = accountRiskStatus; }
    public String getAccountRiskLevel() { return accountRiskLevel; }
    public void setAccountRiskLevel(String accountRiskLevel) { this.accountRiskLevel = accountRiskLevel; }
    public boolean isAccountRiskBlocked() { return accountRiskBlocked; }
    public void setAccountRiskBlocked(boolean accountRiskBlocked) { this.accountRiskBlocked = accountRiskBlocked; }
    public boolean isRebuildTriggered() { return rebuildTriggered; }
    public void setRebuildTriggered(boolean rebuildTriggered) { this.rebuildTriggered = rebuildTriggered; }
    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes); }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotUserPositionCreation() { return notUserPositionCreation; }
    public boolean isNotUserPositionMutation() { return notUserPositionMutation; }
    public boolean isNotAutoClose() { return notAutoClose; }
    public boolean isNotAutoReverse() { return notAutoReverse; }
}
