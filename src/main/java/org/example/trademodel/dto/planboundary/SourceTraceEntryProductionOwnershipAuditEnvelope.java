package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Inert audit metadata shape for a future production ownership review boundary.
 *
 * <p>This DTO does not validate, complete SourceTrace, register as a Spring
 * bean, persist data, or connect to readiness, orders, automation, or external
 * data.
 */
public class SourceTraceEntryProductionOwnershipAuditEnvelope {

    private static final List<String> DEFAULT_MISSING_AUDIT_FIELDS = List.of(
            "ownershipFieldKey",
            "ownerFamily",
            "ownerId",
            "sourceRef",
            "sourceWindow",
            "sourceTimeframe",
            "ruleId",
            "ruleVersion",
            "freshnessStatus",
            "observedAtMs",
            "decisionCreateTimeMs",
            "conflictFamilyEvidenceStatus",
            "visibilityState",
            "consumerIsolationProof"
    );

    private String ownershipFieldKey;
    private String ownerFamily;
    private String ownerId;
    private String sourceRef;
    private String sourceWindow;
    private String sourceTimeframe;
    private String ruleId;
    private String ruleVersion;
    private String freshnessStatus;
    private Long observedAtMs;
    private Long decisionCreateTimeMs;
    private String conflictFamilyEvidenceStatus;
    private String downgradeReason;
    private String rollbackReason;
    private String visibilityState;
    private String consumerIsolationProof;
    private final boolean auditEvidencePresent = false;
    private List<String> missingAuditFields = new ArrayList<>(DEFAULT_MISSING_AUDIT_FIELDS);

    public String getOwnershipFieldKey() {
        return ownershipFieldKey;
    }

    public void setOwnershipFieldKey(String ownershipFieldKey) {
        this.ownershipFieldKey = ownershipFieldKey;
    }

    public String getOwnerFamily() {
        return ownerFamily;
    }

    public void setOwnerFamily(String ownerFamily) {
        this.ownerFamily = ownerFamily;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public void setSourceWindow(String sourceWindow) {
        this.sourceWindow = sourceWindow;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public void setSourceTimeframe(String sourceTimeframe) {
        this.sourceTimeframe = sourceTimeframe;
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

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public Long getObservedAtMs() {
        return observedAtMs;
    }

    public void setObservedAtMs(Long observedAtMs) {
        this.observedAtMs = observedAtMs;
    }

    public Long getDecisionCreateTimeMs() {
        return decisionCreateTimeMs;
    }

    public void setDecisionCreateTimeMs(Long decisionCreateTimeMs) {
        this.decisionCreateTimeMs = decisionCreateTimeMs;
    }

    public String getConflictFamilyEvidenceStatus() {
        return conflictFamilyEvidenceStatus;
    }

    public void setConflictFamilyEvidenceStatus(String conflictFamilyEvidenceStatus) {
        this.conflictFamilyEvidenceStatus = conflictFamilyEvidenceStatus;
    }

    public String getDowngradeReason() {
        return downgradeReason;
    }

    public void setDowngradeReason(String downgradeReason) {
        this.downgradeReason = downgradeReason;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public String getVisibilityState() {
        return visibilityState;
    }

    public void setVisibilityState(String visibilityState) {
        this.visibilityState = visibilityState;
    }

    public String getConsumerIsolationProof() {
        return consumerIsolationProof;
    }

    public void setConsumerIsolationProof(String consumerIsolationProof) {
        this.consumerIsolationProof = consumerIsolationProof;
    }

    public boolean isAuditEvidencePresent() {
        return auditEvidencePresent;
    }

    public List<String> getMissingAuditFields() {
        return new ArrayList<>(missingAuditFields);
    }

    public void setMissingAuditFields(List<String> missingAuditFields) {
        this.missingAuditFields = missingAuditFields == null || missingAuditFields.isEmpty()
                ? new ArrayList<>(DEFAULT_MISSING_AUDIT_FIELDS)
                : new ArrayList<>(missingAuditFields);
    }
}
