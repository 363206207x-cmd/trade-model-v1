package org.example.trademodel.dto.ohlcv;

import org.example.trademodel.entity.PersistedOhlcvBarDO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PersistedOhlcvReadinessResult {
    private String symbol;
    private String timeframe;
    private Integer requiredWindowSize;
    private PersistedOhlcvReadinessStatus status;
    private PersistedOhlcvStaleReasonCode staleReasonCode;
    private String staleReasonText;
    private List<String> missingFields = new ArrayList<>();
    private List<PersistedOhlcvBarDO> bars = new ArrayList<>();
    private Long latestCloseTimeMs;
    private LocalDateTime latestIngestedAt;
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;

    public boolean isFresh() {
        return status == PersistedOhlcvReadinessStatus.FRESH;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Integer getRequiredWindowSize() {
        return requiredWindowSize;
    }

    public void setRequiredWindowSize(Integer requiredWindowSize) {
        this.requiredWindowSize = requiredWindowSize;
    }

    public PersistedOhlcvReadinessStatus getStatus() {
        return status;
    }

    public void setStatus(PersistedOhlcvReadinessStatus status) {
        this.status = status;
    }

    public PersistedOhlcvStaleReasonCode getStaleReasonCode() {
        return staleReasonCode;
    }

    public void setStaleReasonCode(PersistedOhlcvStaleReasonCode staleReasonCode) {
        this.staleReasonCode = staleReasonCode;
    }

    public String getStaleReasonText() {
        return staleReasonText;
    }

    public void setStaleReasonText(String staleReasonText) {
        this.staleReasonText = staleReasonText;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
    }

    public List<PersistedOhlcvBarDO> getBars() {
        return new ArrayList<>(bars);
    }

    public void setBars(List<PersistedOhlcvBarDO> bars) {
        this.bars = bars == null ? new ArrayList<>() : new ArrayList<>(bars);
    }

    public Long getLatestCloseTimeMs() {
        return latestCloseTimeMs;
    }

    public void setLatestCloseTimeMs(Long latestCloseTimeMs) {
        this.latestCloseTimeMs = latestCloseTimeMs;
    }

    public LocalDateTime getLatestIngestedAt() {
        return latestIngestedAt;
    }

    public void setLatestIngestedAt(LocalDateTime latestIngestedAt) {
        this.latestIngestedAt = latestIngestedAt;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }
}
