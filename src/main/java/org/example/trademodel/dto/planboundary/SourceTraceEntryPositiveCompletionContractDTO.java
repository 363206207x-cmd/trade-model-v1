package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO-only shape for a future positive SourceTrace entry completion contract.
 *
 * <p>P50 deliberately keeps this as an inert data carrier. It is not a
 * completion implementation, Spring service, adapter, readiness gate,
 * persistence model, order path, or auto-trading surface.
 */
public class SourceTraceEntryPositiveCompletionContractDTO {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "sourceTraceEntryOwnershipCompletionPath",
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef",
            "ruleId",
            "ruleVersion",
            "sourceWindow",
            "freshnessStatus",
            "observedAtMs",
            "decisionCreateTimeMs",
            "conflictsWithStop",
            "conflictsWithTakeProfit",
            "conflictsWithRiskReward",
            "conflictsWithLiquidity",
            "conflictsWithMultiTimeframe",
            "conflictsWithEvent",
            "conflictsWithWick"
    );

    private SourceTraceEntryPositiveCompletionStatusEnum completionStatus =
            SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE;
    private SourceTraceEntryPositiveCompletionTransitionEnum completionTransition =
            SourceTraceEntryPositiveCompletionTransitionEnum.NONE;
    private final boolean sourceTraceEntryCompleted = false;
    private final boolean completionReady = false;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;
    private String symbol;
    private String timeframe;
    private String sourceTraceEntryOwnershipCompletionPath;
    private BigDecimal entryPriceSource;
    private String entrySourceType;
    private String entrySourceTimeframe;
    private String entrySourceReason;
    private String entrySourceRef;
    private String ruleId;
    private String ruleVersion;
    private String sourceWindow;
    private String freshnessStatus;
    private Long observedAtMs;
    private Long decisionCreateTimeMs;
    private Boolean conflictsWithStop;
    private Boolean conflictsWithTakeProfit;
    private Boolean conflictsWithRiskReward;
    private Boolean conflictsWithLiquidity;
    private Boolean conflictsWithMultiTimeframe;
    private Boolean conflictsWithEvent;
    private Boolean conflictsWithWick;
    private SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason =
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum.DEFAULT_FAIL_CLOSED;
    private List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);

    public SourceTraceEntryPositiveCompletionStatusEnum getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum completionStatus) {
        this.completionStatus = completionStatus == null
                ? SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE
                : completionStatus;
    }

    public SourceTraceEntryPositiveCompletionTransitionEnum getCompletionTransition() {
        return completionTransition;
    }

    public void setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum completionTransition) {
        this.completionTransition = completionTransition == null
                ? SourceTraceEntryPositiveCompletionTransitionEnum.NONE
                : completionTransition;
    }

    public boolean isSourceTraceEntryCompleted() {
        return sourceTraceEntryCompleted;
    }

    public boolean isCompletionReady() {
        return completionReady;
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

    public BigDecimal getEntryPriceSource() {
        return entryPriceSource;
    }

    public void setEntryPriceSource(BigDecimal entryPriceSource) {
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

    public Boolean getConflictsWithStop() {
        return conflictsWithStop;
    }

    public void setConflictsWithStop(Boolean conflictsWithStop) {
        this.conflictsWithStop = conflictsWithStop;
    }

    public Boolean getConflictsWithTakeProfit() {
        return conflictsWithTakeProfit;
    }

    public void setConflictsWithTakeProfit(Boolean conflictsWithTakeProfit) {
        this.conflictsWithTakeProfit = conflictsWithTakeProfit;
    }

    public Boolean getConflictsWithRiskReward() {
        return conflictsWithRiskReward;
    }

    public void setConflictsWithRiskReward(Boolean conflictsWithRiskReward) {
        this.conflictsWithRiskReward = conflictsWithRiskReward;
    }

    public Boolean getConflictsWithLiquidity() {
        return conflictsWithLiquidity;
    }

    public void setConflictsWithLiquidity(Boolean conflictsWithLiquidity) {
        this.conflictsWithLiquidity = conflictsWithLiquidity;
    }

    public Boolean getConflictsWithMultiTimeframe() {
        return conflictsWithMultiTimeframe;
    }

    public void setConflictsWithMultiTimeframe(Boolean conflictsWithMultiTimeframe) {
        this.conflictsWithMultiTimeframe = conflictsWithMultiTimeframe;
    }

    public Boolean getConflictsWithEvent() {
        return conflictsWithEvent;
    }

    public void setConflictsWithEvent(Boolean conflictsWithEvent) {
        this.conflictsWithEvent = conflictsWithEvent;
    }

    public Boolean getConflictsWithWick() {
        return conflictsWithWick;
    }

    public void setConflictsWithWick(Boolean conflictsWithWick) {
        this.conflictsWithWick = conflictsWithWick;
    }

    public SourceTraceEntryPositiveCompletionDowngradeReasonEnum getDowngradeReason() {
        return downgradeReason;
    }

    public void setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason) {
        this.downgradeReason = downgradeReason == null
                ? SourceTraceEntryPositiveCompletionDowngradeReasonEnum.DEFAULT_FAIL_CLOSED
                : downgradeReason;
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
