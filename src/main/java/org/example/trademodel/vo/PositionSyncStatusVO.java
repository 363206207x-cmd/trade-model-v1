package org.example.trademodel.vo;

import java.time.LocalDateTime;

public class PositionSyncStatusVO {
    private String freshnessStatus;
    private String freshnessDetail;
    private Integer staleThresholdMinutes;

    private String configuredProviderType;
    private String activeProviderType;
    private String activeProviderName;
    private Boolean fallbackOccurred;
    private String fallbackReason;

    private LocalDateTime lastSyncStartTime;
    private LocalDateTime lastSyncEndTime;
    private Boolean lastSyncSuccess;
    private String lastSyncMessage;
    private Integer lastFetchedOpenCount;
    private Integer lastUpsertedCount;
    private Integer lastClosedCount;

    private Integer currentOpenPositionCount;

    public String getConfiguredProviderType() {
        return configuredProviderType;
    }

    public void setConfiguredProviderType(String configuredProviderType) {
        this.configuredProviderType = configuredProviderType;
    }

    public String getActiveProviderType() {
        return activeProviderType;
    }

    public void setActiveProviderType(String activeProviderType) {
        this.activeProviderType = activeProviderType;
    }

    public String getActiveProviderName() {
        return activeProviderName;
    }

    public void setActiveProviderName(String activeProviderName) {
        this.activeProviderName = activeProviderName;
    }

    public Boolean getFallbackOccurred() {
        return fallbackOccurred;
    }

    public void setFallbackOccurred(Boolean fallbackOccurred) {
        this.fallbackOccurred = fallbackOccurred;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public LocalDateTime getLastSyncStartTime() {
        return lastSyncStartTime;
    }

    public void setLastSyncStartTime(LocalDateTime lastSyncStartTime) {
        this.lastSyncStartTime = lastSyncStartTime;
    }

    public LocalDateTime getLastSyncEndTime() {
        return lastSyncEndTime;
    }

    public void setLastSyncEndTime(LocalDateTime lastSyncEndTime) {
        this.lastSyncEndTime = lastSyncEndTime;
    }

    public Boolean getLastSyncSuccess() {
        return lastSyncSuccess;
    }

    public void setLastSyncSuccess(Boolean lastSyncSuccess) {
        this.lastSyncSuccess = lastSyncSuccess;
    }

    public String getLastSyncMessage() {
        return lastSyncMessage;
    }

    public void setLastSyncMessage(String lastSyncMessage) {
        this.lastSyncMessage = lastSyncMessage;
    }

    public Integer getLastFetchedOpenCount() {
        return lastFetchedOpenCount;
    }

    public void setLastFetchedOpenCount(Integer lastFetchedOpenCount) {
        this.lastFetchedOpenCount = lastFetchedOpenCount;
    }

    public Integer getLastUpsertedCount() {
        return lastUpsertedCount;
    }

    public void setLastUpsertedCount(Integer lastUpsertedCount) {
        this.lastUpsertedCount = lastUpsertedCount;
    }

    public Integer getLastClosedCount() {
        return lastClosedCount;
    }

    public void setLastClosedCount(Integer lastClosedCount) {
        this.lastClosedCount = lastClosedCount;
    }

    public Integer getCurrentOpenPositionCount() {
        return currentOpenPositionCount;
    }

    public void setCurrentOpenPositionCount(Integer currentOpenPositionCount) {
        this.currentOpenPositionCount = currentOpenPositionCount;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getFreshnessDetail() {
        return freshnessDetail;
    }

    public void setFreshnessDetail(String freshnessDetail) {
        this.freshnessDetail = freshnessDetail;
    }

    public Integer getStaleThresholdMinutes() {
        return staleThresholdMinutes;
    }

    public void setStaleThresholdMinutes(Integer staleThresholdMinutes) {
        this.staleThresholdMinutes = staleThresholdMinutes;
    }
}
