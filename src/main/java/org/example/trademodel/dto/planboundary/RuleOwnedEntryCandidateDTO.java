package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

/**
 * Rule-owned entry candidate metadata for a future SourceTrace adapter.
 *
 * <p>The DTO carries a candidate supplied by a rule-owned source. It does not
 * calculate, infer, or populate any executable entry value by itself.
 */
public class RuleOwnedEntryCandidateDTO {

    private String symbol;
    private String decisionTimeframe;
    private BigDecimal candidateEntryBoundary;
    private String entrySourceType;
    private String entrySourceTimeframe;
    private String entrySourceReason;
    private String entrySourceRef;
    private String ruleId;
    private String ruleVersion;
    private String sourceWindow;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDecisionTimeframe() {
        return decisionTimeframe;
    }

    public void setDecisionTimeframe(String decisionTimeframe) {
        this.decisionTimeframe = decisionTimeframe;
    }

    public BigDecimal getCandidateEntryBoundary() {
        return candidateEntryBoundary;
    }

    public void setCandidateEntryBoundary(BigDecimal candidateEntryBoundary) {
        this.candidateEntryBoundary = candidateEntryBoundary;
    }

    public String getEntrySourceType() {
        return entrySourceType;
    }

    public void setEntrySourceType(String entrySourceType) {
        this.entrySourceType = entrySourceType;
    }

    public String getEntrySourceTimeframe() {
        return entrySourceTimeframe;
    }

    public void setEntrySourceTimeframe(String entrySourceTimeframe) {
        this.entrySourceTimeframe = entrySourceTimeframe;
    }

    public String getEntrySourceReason() {
        return entrySourceReason;
    }

    public void setEntrySourceReason(String entrySourceReason) {
        this.entrySourceReason = entrySourceReason;
    }

    public String getEntrySourceRef() {
        return entrySourceRef;
    }

    public void setEntrySourceRef(String entrySourceRef) {
        this.entrySourceRef = entrySourceRef;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public void setSourceWindow(String sourceWindow) {
        this.sourceWindow = sourceWindow;
    }
}
