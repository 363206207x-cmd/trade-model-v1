package org.example.trademodel.dto.planboundary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MarketStructureBoundaryRequest {

    private String symbol;
    private String direction;
    private String timeframe;
    private LocalDateTime generatedAt;
    private Long generatedAtEpochMs;
    private List<RuntimeKlineItemDTO> bars = new ArrayList<>();
    private boolean allowRrLadder;
    private Integer maxTargets;
    private Integer minBars;
    private Long freshnessLimitMs;
    private Boolean riskActionGuardBlocked;
    private String riskActionGuardReason;
    private String leverageSuggestion;
    private String analysisId;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getGeneratedAtEpochMs() {
        return generatedAtEpochMs;
    }

    public void setGeneratedAtEpochMs(Long generatedAtEpochMs) {
        this.generatedAtEpochMs = generatedAtEpochMs;
    }

    public List<RuntimeKlineItemDTO> getBars() {
        return bars;
    }

    public void setBars(List<RuntimeKlineItemDTO> bars) {
        this.bars = bars == null ? new ArrayList<>() : new ArrayList<>(bars);
    }

    public boolean isAllowRrLadder() {
        return allowRrLadder;
    }

    public void setAllowRrLadder(boolean allowRrLadder) {
        this.allowRrLadder = allowRrLadder;
    }

    public Integer getMaxTargets() {
        return maxTargets;
    }

    public void setMaxTargets(Integer maxTargets) {
        this.maxTargets = maxTargets;
    }

    public Integer getMinBars() {
        return minBars;
    }

    public void setMinBars(Integer minBars) {
        this.minBars = minBars;
    }

    public Long getFreshnessLimitMs() {
        return freshnessLimitMs;
    }

    public void setFreshnessLimitMs(Long freshnessLimitMs) {
        this.freshnessLimitMs = freshnessLimitMs;
    }

    public Boolean getRiskActionGuardBlocked() {
        return riskActionGuardBlocked;
    }

    public void setRiskActionGuardBlocked(Boolean riskActionGuardBlocked) {
        this.riskActionGuardBlocked = riskActionGuardBlocked;
    }

    public String getRiskActionGuardReason() {
        return riskActionGuardReason;
    }

    public void setRiskActionGuardReason(String riskActionGuardReason) {
        this.riskActionGuardReason = riskActionGuardReason;
    }

    public String getLeverageSuggestion() {
        return leverageSuggestion;
    }

    public void setLeverageSuggestion(String leverageSuggestion) {
        this.leverageSuggestion = leverageSuggestion;
    }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String value) { this.analysisId = value; }
}
