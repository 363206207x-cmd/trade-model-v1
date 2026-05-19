package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Inert read-only display shape for already fail-closed SourceTrace entry
 * seam output.
 *
 * <p>This DTO is deliberately not a controller payload registration,
 * persistence model, readiness gate, order path, or automation surface.
 */
public class SourceTraceEntryReadOnlyDisplayDTO {

    private String symbol;
    private String timeframe;
    private String completionStatus = SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name();
    private String completionTransition = SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name();
    private String downgradeReason = SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name();
    private String reviewMode = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name();
    private boolean readOnlyIntegrationSeamUnwired;
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private boolean sourceTraceEntryCompleted;
    private boolean completionReady;
    private List<String> missingFields = new ArrayList<>();
    private List<String> unsafeFields = new ArrayList<>();
    private List<String> blockingFields = new ArrayList<>();
    private String statusLabel = "Incomplete - review only";
    private String statusHelperCopy = "SourceTrace entry completion is not complete.";
    private String transitionLabel = "No completion transition";
    private String transitionHelperCopy = "No completion transition has occurred.";
    private String downgradeLabel = "Missing required source evidence";
    private String downgradeHelperCopy = "Required ownership/source/freshness/conflict evidence is missing.";
    private String reviewModeLabel = "Review only";
    private String reviewModeHelperCopy = "This output is for human review and cannot be used as an instruction.";
    private String manualReviewLabel = "Manual review required";
    private String manualReviewHelperCopy = "Human review is mandatory before any future interpretation.";
    private String nonInstructionLabel = "Not a trade instruction";
    private String nonInstructionHelperCopy = "Do not use this output to open, close, reverse, or place orders.";
    private String sourceTraceLabel = "SourceTrace entry not completed";
    private String sourceTraceHelperCopy = "No runtime SourceTrace entry completion has occurred.";
    private String readinessLabel = "Completion not ready";
    private String readinessHelperCopy = "This output is not readiness and cannot unlock execution planning.";
    private String seamLabel = "Read-only seam unwired";
    private String seamHelperCopy =
            "The read-only seam exists only as a fail-closed review boundary. "
                    + "It is not wired to runtime completion or readiness.";
    private String missingFieldsLabel = "Missing required evidence";
    private String missingFieldsHelperCopy = "Missing evidence prevents SourceTrace entry completion.";
    private String unsafeFieldsLabel = "Unsafe evidence";
    private String unsafeFieldsHelperCopy =
            "Unsafe or runtime-like evidence prevents completion and requires review.";
    private String severity = "blocking_review";
    private String readinessEffect = "blocks_completion_ready";
    private String sourceTraceEffect = "source_trace_entry_completed_false";
    private String instructionEffect = "not_trade_instruction";

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

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public String getCompletionTransition() {
        return completionTransition;
    }

    public void setCompletionTransition(String completionTransition) {
        this.completionTransition = completionTransition;
    }

    public String getDowngradeReason() {
        return downgradeReason;
    }

    public void setDowngradeReason(String downgradeReason) {
        this.downgradeReason = downgradeReason;
    }

    public String getReviewMode() {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode) {
        this.reviewMode = reviewMode;
    }

    public boolean isReadOnlyIntegrationSeamUnwired() {
        return readOnlyIntegrationSeamUnwired;
    }

    public void setReadOnlyIntegrationSeamUnwired(boolean readOnlyIntegrationSeamUnwired) {
        this.readOnlyIntegrationSeamUnwired = readOnlyIntegrationSeamUnwired;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }

    public boolean isSourceTraceEntryCompleted() {
        return sourceTraceEntryCompleted;
    }

    public void setSourceTraceEntryCompleted(boolean sourceTraceEntryCompleted) {
        this.sourceTraceEntryCompleted = sourceTraceEntryCompleted;
    }

    public boolean isCompletionReady() {
        return completionReady;
    }

    public void setCompletionReady(boolean completionReady) {
        this.completionReady = completionReady;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
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
        this.blockingFields = blockingFields == null ? new ArrayList<>() : new ArrayList<>(blockingFields);
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusHelperCopy() {
        return statusHelperCopy;
    }

    public void setStatusHelperCopy(String statusHelperCopy) {
        this.statusHelperCopy = statusHelperCopy;
    }

    public String getTransitionLabel() {
        return transitionLabel;
    }

    public void setTransitionLabel(String transitionLabel) {
        this.transitionLabel = transitionLabel;
    }

    public String getTransitionHelperCopy() {
        return transitionHelperCopy;
    }

    public void setTransitionHelperCopy(String transitionHelperCopy) {
        this.transitionHelperCopy = transitionHelperCopy;
    }

    public String getDowngradeLabel() {
        return downgradeLabel;
    }

    public void setDowngradeLabel(String downgradeLabel) {
        this.downgradeLabel = downgradeLabel;
    }

    public String getDowngradeHelperCopy() {
        return downgradeHelperCopy;
    }

    public void setDowngradeHelperCopy(String downgradeHelperCopy) {
        this.downgradeHelperCopy = downgradeHelperCopy;
    }

    public String getReviewModeLabel() {
        return reviewModeLabel;
    }

    public void setReviewModeLabel(String reviewModeLabel) {
        this.reviewModeLabel = reviewModeLabel;
    }

    public String getReviewModeHelperCopy() {
        return reviewModeHelperCopy;
    }

    public void setReviewModeHelperCopy(String reviewModeHelperCopy) {
        this.reviewModeHelperCopy = reviewModeHelperCopy;
    }

    public String getManualReviewLabel() {
        return manualReviewLabel;
    }

    public void setManualReviewLabel(String manualReviewLabel) {
        this.manualReviewLabel = manualReviewLabel;
    }

    public String getManualReviewHelperCopy() {
        return manualReviewHelperCopy;
    }

    public void setManualReviewHelperCopy(String manualReviewHelperCopy) {
        this.manualReviewHelperCopy = manualReviewHelperCopy;
    }

    public String getNonInstructionLabel() {
        return nonInstructionLabel;
    }

    public void setNonInstructionLabel(String nonInstructionLabel) {
        this.nonInstructionLabel = nonInstructionLabel;
    }

    public String getNonInstructionHelperCopy() {
        return nonInstructionHelperCopy;
    }

    public void setNonInstructionHelperCopy(String nonInstructionHelperCopy) {
        this.nonInstructionHelperCopy = nonInstructionHelperCopy;
    }

    public String getSourceTraceLabel() {
        return sourceTraceLabel;
    }

    public void setSourceTraceLabel(String sourceTraceLabel) {
        this.sourceTraceLabel = sourceTraceLabel;
    }

    public String getSourceTraceHelperCopy() {
        return sourceTraceHelperCopy;
    }

    public void setSourceTraceHelperCopy(String sourceTraceHelperCopy) {
        this.sourceTraceHelperCopy = sourceTraceHelperCopy;
    }

    public String getReadinessLabel() {
        return readinessLabel;
    }

    public void setReadinessLabel(String readinessLabel) {
        this.readinessLabel = readinessLabel;
    }

    public String getReadinessHelperCopy() {
        return readinessHelperCopy;
    }

    public void setReadinessHelperCopy(String readinessHelperCopy) {
        this.readinessHelperCopy = readinessHelperCopy;
    }

    public String getSeamLabel() {
        return seamLabel;
    }

    public void setSeamLabel(String seamLabel) {
        this.seamLabel = seamLabel;
    }

    public String getSeamHelperCopy() {
        return seamHelperCopy;
    }

    public void setSeamHelperCopy(String seamHelperCopy) {
        this.seamHelperCopy = seamHelperCopy;
    }

    public String getMissingFieldsLabel() {
        return missingFieldsLabel;
    }

    public void setMissingFieldsLabel(String missingFieldsLabel) {
        this.missingFieldsLabel = missingFieldsLabel;
    }

    public String getMissingFieldsHelperCopy() {
        return missingFieldsHelperCopy;
    }

    public void setMissingFieldsHelperCopy(String missingFieldsHelperCopy) {
        this.missingFieldsHelperCopy = missingFieldsHelperCopy;
    }

    public String getUnsafeFieldsLabel() {
        return unsafeFieldsLabel;
    }

    public void setUnsafeFieldsLabel(String unsafeFieldsLabel) {
        this.unsafeFieldsLabel = unsafeFieldsLabel;
    }

    public String getUnsafeFieldsHelperCopy() {
        return unsafeFieldsHelperCopy;
    }

    public void setUnsafeFieldsHelperCopy(String unsafeFieldsHelperCopy) {
        this.unsafeFieldsHelperCopy = unsafeFieldsHelperCopy;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReadinessEffect() {
        return readinessEffect;
    }

    public void setReadinessEffect(String readinessEffect) {
        this.readinessEffect = readinessEffect;
    }

    public String getSourceTraceEffect() {
        return sourceTraceEffect;
    }

    public void setSourceTraceEffect(String sourceTraceEffect) {
        this.sourceTraceEffect = sourceTraceEffect;
    }

    public String getInstructionEffect() {
        return instructionEffect;
    }

    public void setInstructionEffect(String instructionEffect) {
        this.instructionEffect = instructionEffect;
    }
}
