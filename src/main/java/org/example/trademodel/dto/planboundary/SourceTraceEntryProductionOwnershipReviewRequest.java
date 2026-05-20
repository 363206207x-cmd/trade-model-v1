package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Inert request shape for a future read-only production ownership review
 * boundary.
 *
 * <p>This DTO carries explicit owner evidence only. It does not compute entry
 * values, validate ownership, complete SourceTrace, register as a Spring bean,
 * persist data, or connect to runtime consumers.
 */
public class SourceTraceEntryProductionOwnershipReviewRequest {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "sourceTraceEntryOwnershipCompletionPath",
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef",
            "sourceWindow",
            "ruleId",
            "ruleVersion",
            "freshnessOwnership",
            "conflictFamilyOwnership",
            "auditEnvelope",
            "consumerIsolationEnvelope",
            "authenticationVisibility"
    );

    private String symbol;
    private String timeframe;
    private String sourceTraceEntryOwnershipCompletionPath;
    private String entryPriceSource;
    private String entrySourceType;
    private String entrySourceTimeframe;
    private String entrySourceReason;
    private String entrySourceRef;
    private String sourceWindow;
    private String ruleId;
    private String ruleVersion;
    private String freshnessOwnership;
    private String conflictFamilyOwnership;
    private String authenticationVisibility;
    private SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
            new SourceTraceEntryProductionOwnershipAuditEnvelope();
    private SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope consumerIsolationEnvelope =
            new SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope();
    private List<String> ownerEvidenceFields = new ArrayList<>();
    private List<String> sourceRefs = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);

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

    public String getSourceTraceEntryOwnershipCompletionPath() {
        return sourceTraceEntryOwnershipCompletionPath;
    }

    public void setSourceTraceEntryOwnershipCompletionPath(String sourceTraceEntryOwnershipCompletionPath) {
        this.sourceTraceEntryOwnershipCompletionPath = sourceTraceEntryOwnershipCompletionPath;
    }

    public String getEntryPriceSource() {
        return entryPriceSource;
    }

    public void setEntryPriceSource(String entryPriceSource) {
        this.entryPriceSource = entryPriceSource;
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

    public String getSourceWindow() {
        return sourceWindow;
    }

    public void setSourceWindow(String sourceWindow) {
        this.sourceWindow = sourceWindow;
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

    public String getFreshnessOwnership() {
        return freshnessOwnership;
    }

    public void setFreshnessOwnership(String freshnessOwnership) {
        this.freshnessOwnership = freshnessOwnership;
    }

    public String getConflictFamilyOwnership() {
        return conflictFamilyOwnership;
    }

    public void setConflictFamilyOwnership(String conflictFamilyOwnership) {
        this.conflictFamilyOwnership = conflictFamilyOwnership;
    }

    public String getAuthenticationVisibility() {
        return authenticationVisibility;
    }

    public void setAuthenticationVisibility(String authenticationVisibility) {
        this.authenticationVisibility = authenticationVisibility;
    }

    public SourceTraceEntryProductionOwnershipAuditEnvelope getAuditEnvelope() {
        return auditEnvelope;
    }

    public void setAuditEnvelope(SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope) {
        this.auditEnvelope = auditEnvelope == null
                ? new SourceTraceEntryProductionOwnershipAuditEnvelope()
                : auditEnvelope;
    }

    public SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope getConsumerIsolationEnvelope() {
        return consumerIsolationEnvelope;
    }

    public void setConsumerIsolationEnvelope(
            SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope consumerIsolationEnvelope
    ) {
        this.consumerIsolationEnvelope = consumerIsolationEnvelope == null
                ? new SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope()
                : consumerIsolationEnvelope;
    }

    public List<String> getOwnerEvidenceFields() {
        return new ArrayList<>(ownerEvidenceFields);
    }

    public void setOwnerEvidenceFields(List<String> ownerEvidenceFields) {
        this.ownerEvidenceFields = ownerEvidenceFields == null
                ? new ArrayList<>()
                : new ArrayList<>(ownerEvidenceFields);
    }

    public List<String> getSourceRefs() {
        return new ArrayList<>(sourceRefs);
    }

    public void setSourceRefs(List<String> sourceRefs) {
        this.sourceRefs = sourceRefs == null ? new ArrayList<>() : new ArrayList<>(sourceRefs);
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null || missingFields.isEmpty()
                ? new ArrayList<>(DEFAULT_MISSING_FIELDS)
                : new ArrayList<>(missingFields);
    }
}
