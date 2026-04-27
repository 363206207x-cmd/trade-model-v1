package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class RunBaselineVO {

    private LocalDateTime generatedAt;
    private Integer windowMinutes;
    private SystemHealthSnapshot systemHealth;
    private PositionSyncBaselineSnapshot positionSync;
    private PerformanceSummary performance;
    private AlertSummary alertSummary;
    private DataQualitySummary dataQualitySummary;
    private RecheckSummary recheckSummary;
    private HotResetSummary hotResetSummary;

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(Integer windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public SystemHealthSnapshot getSystemHealth() {
        return systemHealth;
    }

    public void setSystemHealth(SystemHealthSnapshot systemHealth) {
        this.systemHealth = systemHealth;
    }

    public PositionSyncBaselineSnapshot getPositionSync() {
        return positionSync;
    }

    public void setPositionSync(PositionSyncBaselineSnapshot positionSync) {
        this.positionSync = positionSync;
    }

    public PerformanceSummary getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceSummary performance) {
        this.performance = performance;
    }

    public AlertSummary getAlertSummary() {
        return alertSummary;
    }

    public void setAlertSummary(AlertSummary alertSummary) {
        this.alertSummary = alertSummary;
    }

    public DataQualitySummary getDataQualitySummary() {
        return dataQualitySummary;
    }

    public void setDataQualitySummary(DataQualitySummary dataQualitySummary) {
        this.dataQualitySummary = dataQualitySummary;
    }

    public RecheckSummary getRecheckSummary() {
        return recheckSummary;
    }

    public void setRecheckSummary(RecheckSummary recheckSummary) {
        this.recheckSummary = recheckSummary;
    }

    public HotResetSummary getHotResetSummary() {
        return hotResetSummary;
    }

    public void setHotResetSummary(HotResetSummary hotResetSummary) {
        this.hotResetSummary = hotResetSummary;
    }

    public static class PositionSyncBaselineSnapshot extends PositionSyncStatusVO {
        private String availabilityStatus;
        private String availabilityDetail;

        public String getAvailabilityStatus() {
            return availabilityStatus;
        }

        public void setAvailabilityStatus(String availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
        }

        public String getAvailabilityDetail() {
            return availabilityDetail;
        }

        public void setAvailabilityDetail(String availabilityDetail) {
            this.availabilityDetail = availabilityDetail;
        }
    }

    public static class SystemHealthSnapshot {
        private String cpuUsage;
        private String memoryUsage;
        private String databaseStatus;
        private String databaseStatusDetail;
        private String schedulerStatus;
        private String schedulerStatusDetail;

        public String getCpuUsage() {
            return cpuUsage;
        }

        public void setCpuUsage(String cpuUsage) {
            this.cpuUsage = cpuUsage;
        }

        public String getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(String memoryUsage) {
            this.memoryUsage = memoryUsage;
        }

        public String getDatabaseStatus() {
            return databaseStatus;
        }

        public void setDatabaseStatus(String databaseStatus) {
            this.databaseStatus = databaseStatus;
        }

        public String getDatabaseStatusDetail() {
            return databaseStatusDetail;
        }

        public void setDatabaseStatusDetail(String databaseStatusDetail) {
            this.databaseStatusDetail = databaseStatusDetail;
        }

        public String getSchedulerStatus() {
            return schedulerStatus;
        }

        public void setSchedulerStatus(String schedulerStatus) {
            this.schedulerStatus = schedulerStatus;
        }

        public String getSchedulerStatusDetail() {
            return schedulerStatusDetail;
        }

        public void setSchedulerStatusDetail(String schedulerStatusDetail) {
            this.schedulerStatusDetail = schedulerStatusDetail;
        }
    }

    public static class PerformanceSummary {
        private Map<String, RuntimeMetricSnapshot> metrics = new LinkedHashMap<>();
        private Boolean hasSamples;
        private Long totalSampleCount;
        private String sampleBoundaryDetail;
        private Long baselineAssembleDurationMs;

        public Map<String, RuntimeMetricSnapshot> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, RuntimeMetricSnapshot> metrics) {
            this.metrics = metrics;
        }

        public Boolean getHasSamples() {
            return hasSamples;
        }

        public void setHasSamples(Boolean hasSamples) {
            this.hasSamples = hasSamples;
        }

        public Long getTotalSampleCount() {
            return totalSampleCount;
        }

        public void setTotalSampleCount(Long totalSampleCount) {
            this.totalSampleCount = totalSampleCount;
        }

        public String getSampleBoundaryDetail() {
            return sampleBoundaryDetail;
        }

        public void setSampleBoundaryDetail(String sampleBoundaryDetail) {
            this.sampleBoundaryDetail = sampleBoundaryDetail;
        }

        public Long getBaselineAssembleDurationMs() {
            return baselineAssembleDurationMs;
        }

        public void setBaselineAssembleDurationMs(Long baselineAssembleDurationMs) {
            this.baselineAssembleDurationMs = baselineAssembleDurationMs;
        }
    }

    public static class RuntimeMetricSnapshot {
        private Long lastDurationMs;
        private BigDecimal avgDurationMs;
        private Long sampleCount;

        public Long getLastDurationMs() {
            return lastDurationMs;
        }

        public void setLastDurationMs(Long lastDurationMs) {
            this.lastDurationMs = lastDurationMs;
        }

        public BigDecimal getAvgDurationMs() {
            return avgDurationMs;
        }

        public void setAvgDurationMs(BigDecimal avgDurationMs) {
            this.avgDurationMs = avgDurationMs;
        }

        public Long getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(Long sampleCount) {
            this.sampleCount = sampleCount;
        }
    }

    public static class AlertSummary {
        private Integer openCountWindow;
        private Integer suppressedCountWindow;
        private BigDecimal suppressionRatioWindow;
        private Integer dataQualityOpenCountWindow;
        private Integer dataQualitySuppressedCountWindow;
        private BigDecimal dataQualitySuppressionRatioWindow;

        public Integer getOpenCountWindow() {
            return openCountWindow;
        }

        public void setOpenCountWindow(Integer openCountWindow) {
            this.openCountWindow = openCountWindow;
        }

        public Integer getSuppressedCountWindow() {
            return suppressedCountWindow;
        }

        public void setSuppressedCountWindow(Integer suppressedCountWindow) {
            this.suppressedCountWindow = suppressedCountWindow;
        }

        public BigDecimal getSuppressionRatioWindow() {
            return suppressionRatioWindow;
        }

        public void setSuppressionRatioWindow(BigDecimal suppressionRatioWindow) {
            this.suppressionRatioWindow = suppressionRatioWindow;
        }

        public Integer getDataQualityOpenCountWindow() {
            return dataQualityOpenCountWindow;
        }

        public void setDataQualityOpenCountWindow(Integer dataQualityOpenCountWindow) {
            this.dataQualityOpenCountWindow = dataQualityOpenCountWindow;
        }

        public Integer getDataQualitySuppressedCountWindow() {
            return dataQualitySuppressedCountWindow;
        }

        public void setDataQualitySuppressedCountWindow(Integer dataQualitySuppressedCountWindow) {
            this.dataQualitySuppressedCountWindow = dataQualitySuppressedCountWindow;
        }

        public BigDecimal getDataQualitySuppressionRatioWindow() {
            return dataQualitySuppressionRatioWindow;
        }

        public void setDataQualitySuppressionRatioWindow(BigDecimal dataQualitySuppressionRatioWindow) {
            this.dataQualitySuppressionRatioWindow = dataQualitySuppressionRatioWindow;
        }
    }

    public static class DataQualitySummary {
        private Integer analysisRunCountWindow;
        private Integer lowQualityCountWindow;
        private BigDecimal lowQualityRatioWindow;
        private Integer lowQualityThreshold;

        public Integer getAnalysisRunCountWindow() {
            return analysisRunCountWindow;
        }

        public void setAnalysisRunCountWindow(Integer analysisRunCountWindow) {
            this.analysisRunCountWindow = analysisRunCountWindow;
        }

        public Integer getLowQualityCountWindow() {
            return lowQualityCountWindow;
        }

        public void setLowQualityCountWindow(Integer lowQualityCountWindow) {
            this.lowQualityCountWindow = lowQualityCountWindow;
        }

        public BigDecimal getLowQualityRatioWindow() {
            return lowQualityRatioWindow;
        }

        public void setLowQualityRatioWindow(BigDecimal lowQualityRatioWindow) {
            this.lowQualityRatioWindow = lowQualityRatioWindow;
        }

        public Integer getLowQualityThreshold() {
            return lowQualityThreshold;
        }

        public void setLowQualityThreshold(Integer lowQualityThreshold) {
            this.lowQualityThreshold = lowQualityThreshold;
        }
    }

    public static class RecheckSummary {
        private Integer totalCountWindow;
        private Map<String, Integer> statusCountsWindow = new LinkedHashMap<>();

        public Integer getTotalCountWindow() {
            return totalCountWindow;
        }

        public void setTotalCountWindow(Integer totalCountWindow) {
            this.totalCountWindow = totalCountWindow;
        }

        public Map<String, Integer> getStatusCountsWindow() {
            return statusCountsWindow;
        }

        public void setStatusCountsWindow(Map<String, Integer> statusCountsWindow) {
            this.statusCountsWindow = statusCountsWindow;
        }
    }

    public static class HotResetSummary {
        private Integer eventCountWindow;
        private Map<String, Integer> triggerTypeCountsWindow = new LinkedHashMap<>();
        private Boolean latestHotResetFired;
        private String latestHotResetSymbol;
        private String latestHotResetTriggerType;
        private String latestHotResetTriggerValue;
        private LocalDateTime latestHotResetTime;

        public Integer getEventCountWindow() {
            return eventCountWindow;
        }

        public void setEventCountWindow(Integer eventCountWindow) {
            this.eventCountWindow = eventCountWindow;
        }

        public Map<String, Integer> getTriggerTypeCountsWindow() {
            return triggerTypeCountsWindow;
        }

        public void setTriggerTypeCountsWindow(Map<String, Integer> triggerTypeCountsWindow) {
            this.triggerTypeCountsWindow = triggerTypeCountsWindow;
        }

        public Boolean getLatestHotResetFired() {
            return latestHotResetFired;
        }

        public void setLatestHotResetFired(Boolean latestHotResetFired) {
            this.latestHotResetFired = latestHotResetFired;
        }

        public String getLatestHotResetSymbol() {
            return latestHotResetSymbol;
        }

        public void setLatestHotResetSymbol(String latestHotResetSymbol) {
            this.latestHotResetSymbol = latestHotResetSymbol;
        }

        public String getLatestHotResetTriggerType() {
            return latestHotResetTriggerType;
        }

        public void setLatestHotResetTriggerType(String latestHotResetTriggerType) {
            this.latestHotResetTriggerType = latestHotResetTriggerType;
        }

        public String getLatestHotResetTriggerValue() {
            return latestHotResetTriggerValue;
        }

        public void setLatestHotResetTriggerValue(String latestHotResetTriggerValue) {
            this.latestHotResetTriggerValue = latestHotResetTriggerValue;
        }

        public LocalDateTime getLatestHotResetTime() {
            return latestHotResetTime;
        }

        public void setLatestHotResetTime(LocalDateTime latestHotResetTime) {
            this.latestHotResetTime = latestHotResetTime;
        }
    }
}
