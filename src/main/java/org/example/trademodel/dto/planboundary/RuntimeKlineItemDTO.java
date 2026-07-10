package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RuntimeKlineItemDTO {

    private Long openTimeMs;
    private Long closeTimeMs;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal volume;
    private String provider;
    private String providerMarketType;
    private String sourceEndpoint;
    private String sourceBatchId;
    private String sourceTraceId;
    private Integer sourceVersion;
    private LocalDateTime fetchTime;
    private String sourceStatus;
    private String freshnessStatus;
    private String provenanceVersion;
    private String ingestionRunId;
    private LocalDateTime ingestedAt;
    private String qualityStatus;

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

    public LocalDateTime getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(LocalDateTime fetchTime) {
        this.fetchTime = fetchTime;
    }

    public String getSourceStatus() {
        return sourceStatus;
    }

    public void setSourceStatus(String sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getProvenanceVersion() {
        return provenanceVersion;
    }

    public void setProvenanceVersion(String provenanceVersion) {
        this.provenanceVersion = provenanceVersion;
    }

    public String getIngestionRunId() {
        return ingestionRunId;
    }

    public void setIngestionRunId(String ingestionRunId) {
        this.ingestionRunId = ingestionRunId;
    }

    public LocalDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(LocalDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }
}
