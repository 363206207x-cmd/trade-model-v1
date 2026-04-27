package org.example.trademodel.entity;

public class AccountRiskSnapshotDO {

    private String id;
    private String analysisId;
    private String accountId;
    private String totalRiskScore;
    private String correlationRisk;
    private String maxDrawdownEstimate;
    private String var95;
    private String snapshotTime;
    private String traceId;
    private String ruleVersion;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
    private Integer isDeleted;
    private Integer versionNo;

    public AccountRiskSnapshotDO() {
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getTotalRiskScore() {
        return totalRiskScore;
    }

    public void setTotalRiskScore(String totalRiskScore) {
        this.totalRiskScore = totalRiskScore;
    }

    public String getCorrelationRisk() {
        return correlationRisk;
    }

    public void setCorrelationRisk(String correlationRisk) {
        this.correlationRisk = correlationRisk;
    }

    public String getMaxDrawdownEstimate() {
        return maxDrawdownEstimate;
    }

    public void setMaxDrawdownEstimate(String maxDrawdownEstimate) {
        this.maxDrawdownEstimate = maxDrawdownEstimate;
    }

    public String getVar95() {
        return var95;
    }

    public void setVar95(String var95) {
        this.var95 = var95;
    }

    public String getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(String snapshotTime) {
        this.snapshotTime = snapshotTime;
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
