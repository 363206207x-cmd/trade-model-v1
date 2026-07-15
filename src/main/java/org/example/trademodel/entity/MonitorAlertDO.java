package org.example.trademodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class MonitorAlertDO {

    private String id;
    private String analysisId;
    private String assetSymbol;
    private String alertType;
    private String alertLevel;
    private String alertMessage;
    private String status;
    private String cooldownUntil;
    private String suppressReason;
    private String traceId;
    private String ruleVersion;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
    /** UTC-naive persistence value; display reads continue to use {@link #createdAt}. */
    private LocalDateTime createdAtUtc;
    /** UTC-naive persistence value; display reads continue to use {@link #updatedAt}. */
    private LocalDateTime updatedAtUtc;
    /** UTC-naive persistence value; display reads continue to use {@link #cooldownUntil}. */
    private LocalDateTime cooldownUntilUtc;
    private Integer isDeleted;
    private Integer versionNo;

    public MonitorAlertDO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(String cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public String getSuppressReason() {
        return suppressReason;
    }

    public void setSuppressReason(String suppressReason) {
        this.suppressReason = suppressReason;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonIgnore
    public LocalDateTime getCreatedAtUtc() {
        return createdAtUtc;
    }

    public void setCreatedAtUtc(LocalDateTime createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    @JsonIgnore
    public LocalDateTime getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(LocalDateTime updatedAtUtc) {
        this.updatedAtUtc = updatedAtUtc;
    }

    @JsonIgnore
    public LocalDateTime getCooldownUntilUtc() {
        return cooldownUntilUtc;
    }

    public void setCooldownUntilUtc(LocalDateTime cooldownUntilUtc) {
        this.cooldownUntilUtc = cooldownUntilUtc;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }
}
