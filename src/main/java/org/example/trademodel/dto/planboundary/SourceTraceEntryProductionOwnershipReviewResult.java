package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Inert fail-closed result shape for a future read-only production ownership
 * review boundary.
 *
 * <p>This result is not a production completion, readiness signal, persistence
 * model, order path, automation path, or external integration surface.
 */
public class SourceTraceEntryProductionOwnershipReviewResult {

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
    private static final List<String> DEFAULT_BLOCKING_FIELDS = List.of(
            "productionOwnershipReviewBoundaryUnwired",
            "productionWiringStillBlocked"
    );

    private SourceTraceEntryProductionOwnershipReviewStatusEnum reviewStatus =
            SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE;
    private SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum downgradeReason =
            SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.DEFAULT_FAIL_CLOSED;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;
    private final boolean sourceTraceEntryCompleted = false;
    private final boolean completionReady = false;
    private String symbol;
    private String timeframe;
    private List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private List<String> unsafeFields = new ArrayList<>();
    private List<String> blockingFields = new ArrayList<>(DEFAULT_BLOCKING_FIELDS);

    public SourceTraceEntryProductionOwnershipReviewStatusEnum getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(SourceTraceEntryProductionOwnershipReviewStatusEnum reviewStatus) {
        this.reviewStatus = reviewStatus == null
                ? SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE
                : reviewStatus;
    }

    public SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum getDowngradeReason() {
        return downgradeReason;
    }

    public void setDowngradeReason(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum downgradeReason) {
        this.downgradeReason = downgradeReason == null
                ? SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.DEFAULT_FAIL_CLOSED
                : downgradeReason;
    }

    public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isSourceTraceEntryCompleted() {
        return sourceTraceEntryCompleted;
    }

    public boolean isCompletionReady() {
        return completionReady;
    }

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

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null || missingFields.isEmpty()
                ? new ArrayList<>(DEFAULT_MISSING_FIELDS)
                : new ArrayList<>(missingFields);
    }

    public List<String> getUnsafeFields() {
        return new ArrayList<>(unsafeFields);
    }

    public void setUnsafeFields(List<String> unsafeFields) {
        this.unsafeFields = unsafeFields == null ? new ArrayList<>() : new ArrayList<>(unsafeFields);
    }

    public List<String> getBlockingFields() {
        return new ArrayList<>(blockingFields);
    }

    public void setBlockingFields(List<String> blockingFields) {
        this.blockingFields = blockingFields == null || blockingFields.isEmpty()
                ? new ArrayList<>(DEFAULT_BLOCKING_FIELDS)
                : new ArrayList<>(blockingFields);
    }
}
