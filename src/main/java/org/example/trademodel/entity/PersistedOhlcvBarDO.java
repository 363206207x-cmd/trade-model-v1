package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PersistedOhlcvBarDO {
    private Long id;
    private String symbol;
    private String timeframe;
    private Long openTimeMs;
    private Long closeTimeMs;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private Long tradeCount;
    private BigDecimal takerBuyBaseVolume;
    private BigDecimal takerBuyQuoteVolume;
    private Boolean closed;
    private String provider;
    private String providerMarketType;
    private String sourceEndpoint;
    private String sourceBatchId;
    private String sourceTraceId;
    private Integer sourceVersion;
    private LocalDateTime ingestedAt;
    private LocalDateTime updatedAt;
    private String qualityStatus;
    private String qualityReason;
    private String rawPayloadHash;
    private Integer isDeleted;

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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Long getOpenTimeMs() {
        return openTimeMs;
    }

    public void setOpenTimeMs(Long openTimeMs) {
        this.openTimeMs = openTimeMs;
    }

    public Long getCloseTimeMs() {
        return closeTimeMs;
    }

    public void setCloseTimeMs(Long closeTimeMs) {
        this.closeTimeMs = closeTimeMs;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public void setQuoteVolume(BigDecimal quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    public Long getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(Long tradeCount) {
        this.tradeCount = tradeCount;
    }

    public BigDecimal getTakerBuyBaseVolume() {
        return takerBuyBaseVolume;
    }

    public void setTakerBuyBaseVolume(BigDecimal takerBuyBaseVolume) {
        this.takerBuyBaseVolume = takerBuyBaseVolume;
    }

    public BigDecimal getTakerBuyQuoteVolume() {
        return takerBuyQuoteVolume;
    }

    public void setTakerBuyQuoteVolume(BigDecimal takerBuyQuoteVolume) {
        this.takerBuyQuoteVolume = takerBuyQuoteVolume;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderMarketType() {
        return providerMarketType;
    }

    public void setProviderMarketType(String providerMarketType) {
        this.providerMarketType = providerMarketType;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getSourceBatchId() {
        return sourceBatchId;
    }

    public void setSourceBatchId(String sourceBatchId) {
        this.sourceBatchId = sourceBatchId;
    }

    public String getSourceTraceId() {
        return sourceTraceId;
    }

    public void setSourceTraceId(String sourceTraceId) {
        this.sourceTraceId = sourceTraceId;
    }

    public Integer getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(Integer sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public LocalDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(LocalDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public String getQualityReason() {
        return qualityReason;
    }

    public void setQualityReason(String qualityReason) {
        this.qualityReason = qualityReason;
    }

    public String getRawPayloadHash() {
        return rawPayloadHash;
    }

    public void setRawPayloadHash(String rawPayloadHash) {
        this.rawPayloadHash = rawPayloadHash;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
