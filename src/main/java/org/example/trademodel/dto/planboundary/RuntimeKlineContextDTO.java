package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RuntimeKlineContextDTO {
    private String symbol;
    private String timeframe;
    private LocalDateTime klineWindowStart;
    private LocalDateTime klineWindowEnd;
    private Integer klineCount;
    private BigDecimal latestOpen;
    private BigDecimal latestHigh;
    private BigDecimal latestLow;
    private BigDecimal latestClose;
    private BigDecimal latestVolume;
    private BigDecimal previousClose;
    private BigDecimal highestHigh;
    private BigDecimal lowestLow;
    private BigDecimal averageVolume;
    private String dataSourceName;
    private String sourceType;
    private Integer dataQualityScore;
    private RuntimeKlineContextStatusEnum staleStatus;
    private LocalDateTime fetchTime;
    private LocalDateTime generatedAt;
    private List<String> missingFields = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private String ruleVersion;
    private List<RuntimeKlineItemDTO> klineItems = new ArrayList<>();

    public static RuntimeKlineContextDTO missing(String symbol, String timeframe, String reason) {
        return withStatus(symbol, timeframe, RuntimeKlineContextStatusEnum.UNKNOWN, reason);
    }

    public static RuntimeKlineContextDTO stale(String symbol, String timeframe, String reason) {
        return withStatus(symbol, timeframe, RuntimeKlineContextStatusEnum.STALE, reason);
    }

    public static RuntimeKlineContextDTO fresh(String symbol, String timeframe) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setSymbol(symbol);
        context.setTimeframe(timeframe);
        context.setStaleStatus(RuntimeKlineContextStatusEnum.FRESH);
        context.setGeneratedAt(LocalDateTime.now());
        return context;
    }

    private static RuntimeKlineContextDTO withStatus(String symbol,
                                                     String timeframe,
                                                     RuntimeKlineContextStatusEnum staleStatus,
                                                     String reason) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setSymbol(symbol);
        context.setTimeframe(timeframe);
        context.setStaleStatus(staleStatus);
        context.setGeneratedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            context.getBlockingReasons().add(reason);
        }
        return context;
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

    public LocalDateTime getKlineWindowStart() {
        return klineWindowStart;
    }

    public void setKlineWindowStart(LocalDateTime klineWindowStart) {
        this.klineWindowStart = klineWindowStart;
    }

    public LocalDateTime getKlineWindowEnd() {
        return klineWindowEnd;
    }

    public void setKlineWindowEnd(LocalDateTime klineWindowEnd) {
        this.klineWindowEnd = klineWindowEnd;
    }

    public Integer getKlineCount() {
        return klineCount;
    }

    public void setKlineCount(Integer klineCount) {
        this.klineCount = klineCount;
    }

    public BigDecimal getLatestOpen() {
        return latestOpen;
    }

    public void setLatestOpen(BigDecimal latestOpen) {
        this.latestOpen = latestOpen;
    }

    public BigDecimal getLatestHigh() {
        return latestHigh;
    }

    public void setLatestHigh(BigDecimal latestHigh) {
        this.latestHigh = latestHigh;
    }

    public BigDecimal getLatestLow() {
        return latestLow;
    }

    public void setLatestLow(BigDecimal latestLow) {
        this.latestLow = latestLow;
    }

    public BigDecimal getLatestClose() {
        return latestClose;
    }

    public void setLatestClose(BigDecimal latestClose) {
        this.latestClose = latestClose;
    }

    public BigDecimal getLatestVolume() {
        return latestVolume;
    }

    public void setLatestVolume(BigDecimal latestVolume) {
        this.latestVolume = latestVolume;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public BigDecimal getHighestHigh() {
        return highestHigh;
    }

    public void setHighestHigh(BigDecimal highestHigh) {
        this.highestHigh = highestHigh;
    }

    public BigDecimal getLowestLow() {
        return lowestLow;
    }

    public void setLowestLow(BigDecimal lowestLow) {
        this.lowestLow = lowestLow;
    }

    public BigDecimal getAverageVolume() {
        return averageVolume;
    }

    public void setAverageVolume(BigDecimal averageVolume) {
        this.averageVolume = averageVolume;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Integer dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public RuntimeKlineContextStatusEnum getStaleStatus() {
        return staleStatus;
    }

    public void setStaleStatus(RuntimeKlineContextStatusEnum staleStatus) {
        this.staleStatus = staleStatus;
    }

    public LocalDateTime getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(LocalDateTime fetchTime) {
        this.fetchTime = fetchTime;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public List<RuntimeKlineItemDTO> getKlineItems() {
        return klineItems;
    }

    public void setKlineItems(List<RuntimeKlineItemDTO> klineItems) {
        this.klineItems = klineItems;
    }
}
