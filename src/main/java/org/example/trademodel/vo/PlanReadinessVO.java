package org.example.trademodel.vo;

import java.util.ArrayList;
import java.util.List;

public class PlanReadinessVO {

    private String readinessStatus;
    private String readinessText;
    private Integer readinessLevel;
    private String primaryReason;
    private List<PlanReadinessReasonVO> blockingReasons;
    private List<PlanReadinessSourceFieldVO> sourceFields;
    private String ruleVersion;

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public String getReadinessText() {
        return readinessText;
    }

    public void setReadinessText(String readinessText) {
        this.readinessText = readinessText;
    }

    public Integer getReadinessLevel() {
        return readinessLevel;
    }

    public void setReadinessLevel(Integer readinessLevel) {
        this.readinessLevel = readinessLevel;
    }

    public String getPrimaryReason() {
        return primaryReason;
    }

    public void setPrimaryReason(String primaryReason) {
        this.primaryReason = primaryReason;
    }

    public List<PlanReadinessReasonVO> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<PlanReadinessReasonVO> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public List<PlanReadinessSourceFieldVO> getSourceFields() {
        return sourceFields;
    }

    public void setSourceFields(List<PlanReadinessSourceFieldVO> sourceFields) {
        this.sourceFields = sourceFields;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    /**
     * Ensures JSON arrays are non-null for clients that expect empty arrays.
     */
    public void ensureCollectionsNonNull() {
        if (blockingReasons == null) {
            blockingReasons = new ArrayList<>();
        }
        if (sourceFields == null) {
            sourceFields = new ArrayList<>();
        }
    }
}
