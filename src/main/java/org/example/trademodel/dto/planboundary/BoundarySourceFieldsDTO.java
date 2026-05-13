package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BoundarySourceFieldsDTO {
    private LocalDateTime klineWindowStart;
    private LocalDateTime klineWindowEnd;
    private Integer klineCount;
    private String timeframe;
    private BigDecimal swingHighRef;
    private BigDecimal swingLowRef;
    private BigDecimal supportRef;
    private BigDecimal resistanceRef;
    private BigDecimal atrValue;
    private Integer atrPeriod;
    private String bufferRule;
    private String rrRule;
    private String dataSourceName;
    private String sourceType;
    private BigDecimal dataQualityScore;
    private String staleStatus;
    private List<String> evidenceRefs = new ArrayList<>();
    private List<String> decisionRefs = new ArrayList<>();
    private String ruleVersion;

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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public BigDecimal getSwingHighRef() {
        return swingHighRef;
    }

    public void setSwingHighRef(BigDecimal swingHighRef) {
        this.swingHighRef = swingHighRef;
    }

    public BigDecimal getSwingLowRef() {
        return swingLowRef;
    }

    public void setSwingLowRef(BigDecimal swingLowRef) {
        this.swingLowRef = swingLowRef;
    }

    public BigDecimal getSupportRef() {
        return supportRef;
    }

    public void setSupportRef(BigDecimal supportRef) {
        this.supportRef = supportRef;
    }

    public BigDecimal getResistanceRef() {
        return resistanceRef;
    }

    public void setResistanceRef(BigDecimal resistanceRef) {
        this.resistanceRef = resistanceRef;
    }

    public BigDecimal getAtrValue() {
        return atrValue;
    }

    public void setAtrValue(BigDecimal atrValue) {
        this.atrValue = atrValue;
    }

    public Integer getAtrPeriod() {
        return atrPeriod;
    }

    public void setAtrPeriod(Integer atrPeriod) {
        this.atrPeriod = atrPeriod;
    }

    public String getBufferRule() {
        return bufferRule;
    }

    public void setBufferRule(String bufferRule) {
        this.bufferRule = bufferRule;
    }

    public String getRrRule() {
        return rrRule;
    }

    public void setRrRule(String rrRule) {
        this.rrRule = rrRule;
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

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public String getStaleStatus() {
        return staleStatus;
    }

    public void setStaleStatus(String staleStatus) {
        this.staleStatus = staleStatus;
    }

    public List<String> getEvidenceRefs() {
        return evidenceRefs;
    }

    public void setEvidenceRefs(List<String> evidenceRefs) {
        this.evidenceRefs = evidenceRefs;
    }

    public List<String> getDecisionRefs() {
        return decisionRefs;
    }

    public void setDecisionRefs(List<String> decisionRefs) {
        this.decisionRefs = decisionRefs;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }
}
