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
    private String riskLevelSnapshot;
    private Boolean riskAllowed;
    private String riskReasonCode;
    private String riskReasonText;
    private BigDecimal positionExposure;
    private BigDecimal maxAllowedExposure;
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
