package org.example.trademodel.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 账户风险快照（第一轮最小闭环），与表 tm_account_risk_snapshot 对齐。
 */
public class TmAccountRiskSnapshotDO {

    private Long id;
    private String analysisId;
    private String symbol;
    private String ownerType;
    private Long ownerId;
    private String accountRiskStatus;
    private String riskLevelSnapshot;
    private Boolean riskAllowed;
    private String riskReasonCode;
    private String riskReasonText;
    private BigDecimal positionExposure;
    private BigDecimal maxAllowedExposure;
    private BigDecimal candidateLeverage;
    private BigDecimal maxAllowedLeverage;
    private BigDecimal grossNotional;
    private BigDecimal leverageRisk;
    private BigDecimal positionSizeRisk;
    private BigDecimal concentrationRisk;
    private BigDecimal correlationRisk;
    private BigDecimal drawdownOrVarRisk;
    private BigDecimal aggregateRiskScore;
    private String sourceStatus;
    private String accountRiskCoverageState = "UNKNOWN";
    private LocalDateTime observedAt;
    private LocalDateTime freshUntil;
    private String snapshotSource;
    private Integer snapshotVersion;
    private String sourceNote;
    private String traceId;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getAccountRiskStatus() { return accountRiskStatus; }
    public void setAccountRiskStatus(String accountRiskStatus) { this.accountRiskStatus = accountRiskStatus; }

    public String getRiskLevelSnapshot() {
        return riskLevelSnapshot;
    }

    public void setRiskLevelSnapshot(String riskLevelSnapshot) {
        this.riskLevelSnapshot = riskLevelSnapshot;
    }

    public Boolean getRiskAllowed() {
        return riskAllowed;
    }

    public void setRiskAllowed(Boolean riskAllowed) {
        this.riskAllowed = riskAllowed;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    public String getRiskReasonCode() {
        return riskReasonCode;
    }

    public void setRiskReasonCode(String riskReasonCode) {
        this.riskReasonCode = riskReasonCode;
    }

    public String getRiskReasonText() {
        return riskReasonText;
    }

    public void setRiskReasonText(String riskReasonText) {
        this.riskReasonText = riskReasonText;
    }

    public BigDecimal getPositionExposure() {
        return positionExposure;
    }

    public void setPositionExposure(BigDecimal positionExposure) {
        this.positionExposure = positionExposure;
    }

    public BigDecimal getMaxAllowedExposure() {
        return maxAllowedExposure;
    }

    public void setMaxAllowedExposure(BigDecimal maxAllowedExposure) {
        this.maxAllowedExposure = maxAllowedExposure;
    }

    public BigDecimal getCandidateLeverage() { return candidateLeverage; }
    public void setCandidateLeverage(BigDecimal candidateLeverage) { this.candidateLeverage = candidateLeverage; }
    public BigDecimal getMaxAllowedLeverage() { return maxAllowedLeverage; }
    public void setMaxAllowedLeverage(BigDecimal maxAllowedLeverage) { this.maxAllowedLeverage = maxAllowedLeverage; }

    public BigDecimal getGrossNotional() { return grossNotional; }
    public void setGrossNotional(BigDecimal grossNotional) { this.grossNotional = grossNotional; }
    public BigDecimal getLeverageRisk() { return leverageRisk; }
    public void setLeverageRisk(BigDecimal leverageRisk) { this.leverageRisk = leverageRisk; }
    public BigDecimal getPositionSizeRisk() { return positionSizeRisk; }
    public void setPositionSizeRisk(BigDecimal positionSizeRisk) { this.positionSizeRisk = positionSizeRisk; }
    public BigDecimal getConcentrationRisk() { return concentrationRisk; }
    public void setConcentrationRisk(BigDecimal concentrationRisk) { this.concentrationRisk = concentrationRisk; }
    public BigDecimal getCorrelationRisk() { return correlationRisk; }
    public void setCorrelationRisk(BigDecimal correlationRisk) { this.correlationRisk = correlationRisk; }
    public BigDecimal getDrawdownOrVarRisk() { return drawdownOrVarRisk; }
    public void setDrawdownOrVarRisk(BigDecimal drawdownOrVarRisk) { this.drawdownOrVarRisk = drawdownOrVarRisk; }
    public BigDecimal getAggregateRiskScore() { return aggregateRiskScore; }
    public void setAggregateRiskScore(BigDecimal aggregateRiskScore) { this.aggregateRiskScore = aggregateRiskScore; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }
    public String getAccountRiskCoverageState() { return accountRiskCoverageState; }
    public void setAccountRiskCoverageState(String value) { this.accountRiskCoverageState = value; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDateTime observedAt) { this.observedAt = observedAt; }
    public LocalDateTime getFreshUntil() { return freshUntil; }
    public void setFreshUntil(LocalDateTime freshUntil) { this.freshUntil = freshUntil; }

    public String getSnapshotSource() {
        return snapshotSource;
    }

    public void setSnapshotSource(String snapshotSource) {
        this.snapshotSource = snapshotSource;
    }

    public Integer getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(Integer snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
