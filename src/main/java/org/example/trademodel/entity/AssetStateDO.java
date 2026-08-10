package org.example.trademodel.entity;

import org.example.trademodel.enums.AssetStateEnum;

import java.time.LocalDateTime;

/**
 * 权威资产状态行（tm_asset_state）；与 MyBatis 写入一致，非 JPA 托管。
 */
public class AssetStateDO {

    private Long id;

    private String symbol;

    /** 存库为枚举名，与 {@link AssetStateEnum} 一致 */
    private AssetStateEnum state;

    private Integer confusedScore;

    private Integer confusedLowStreak = 0;

    private Boolean hotResetFlag = false;

    private String hotResetTriggerType;

    private String hotResetTriggerValue;

    private LocalDateTime hotResetTime;

    private String preResetState;

    private String postResetState;

    private String opportunityId;

    private LocalDateTime stateEnteredAt;

    private LocalDateTime coolingUntil;

    private String lastTransitionReason;

    private String lastTriggerSource;

    private String lastAnalysisId;

    private LocalDateTime lastUpdateTime = LocalDateTime.now();

    private String traceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AssetStateEnum getState() {
        return state;
    }

    public void setState(AssetStateEnum state) {
        this.state = state;
    }

    public Integer getConfusedScore() {
        return confusedScore;
    }

    public void setConfusedScore(Integer confusedScore) {
        this.confusedScore = confusedScore;
    }

    public Integer getConfusedLowStreak() {
        return confusedLowStreak;
    }

    public void setConfusedLowStreak(Integer confusedLowStreak) {
        this.confusedLowStreak = confusedLowStreak;
    }

    public Boolean getHotResetFlag() {
        return hotResetFlag;
    }

    public void setHotResetFlag(Boolean hotResetFlag) {
        this.hotResetFlag = hotResetFlag;
    }

    public String getHotResetTriggerType() {
        return hotResetTriggerType;
    }

    public void setHotResetTriggerType(String hotResetTriggerType) {
        this.hotResetTriggerType = hotResetTriggerType;
    }

    public String getHotResetTriggerValue() {
        return hotResetTriggerValue;
    }

    public void setHotResetTriggerValue(String hotResetTriggerValue) {
        this.hotResetTriggerValue = hotResetTriggerValue;
    }

    public LocalDateTime getHotResetTime() {
        return hotResetTime;
    }

    public void setHotResetTime(LocalDateTime hotResetTime) {
        this.hotResetTime = hotResetTime;
    }

    public String getPreResetState() {
        return preResetState;
    }

    public void setPreResetState(String preResetState) {
        this.preResetState = preResetState;
    }

    public String getPostResetState() {
        return postResetState;
    }

    public void setPostResetState(String postResetState) {
        this.postResetState = postResetState;
    }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public LocalDateTime getStateEnteredAt() { return stateEnteredAt; }
    public void setStateEnteredAt(LocalDateTime stateEnteredAt) { this.stateEnteredAt = stateEnteredAt; }
    public LocalDateTime getCoolingUntil() { return coolingUntil; }
    public void setCoolingUntil(LocalDateTime coolingUntil) { this.coolingUntil = coolingUntil; }
    public String getLastTransitionReason() { return lastTransitionReason; }
    public void setLastTransitionReason(String lastTransitionReason) { this.lastTransitionReason = lastTransitionReason; }
    public String getLastTriggerSource() { return lastTriggerSource; }
    public void setLastTriggerSource(String lastTriggerSource) { this.lastTriggerSource = lastTriggerSource; }
    public String getLastAnalysisId() { return lastAnalysisId; }
    public void setLastAnalysisId(String lastAnalysisId) { this.lastAnalysisId = lastAnalysisId; }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
