package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class AssetDO {

    private Long id;
    private String symbol;
    private String assetName;
    private String source;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    private String extJson;

    public AssetDO() {
    }

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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getExtJson() { return extJson; }
    public void setExtJson(String extJson) { this.extJson = extJson; }
}
