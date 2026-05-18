package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceEntryCompletionResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "sourceTraceEntryCompletionPath",
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceEntryCompletionStatusEnum completionStatus =
            SourceTraceEntryCompletionStatusEnum.INCOMPLETE;
    private SourceTraceEntryCompletionMissingReasonEnum missingReason =
            SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final BigDecimal entryPriceSource = null;
    private final String entrySourceType = null;
    private final String entrySourceTimeframe = null;
    private final String entrySourceReason = null;
    private final String entrySourceRef = null;
    private final boolean sourceTraceEntryCompleted = false;
    private final boolean completionReady = false;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);

    public static SourceTraceEntryCompletionResult unwired(String symbol, String timeframe) {
        return incomplete(
                symbol,
                timeframe,
                SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED,
                DEFAULT_MISSING_FIELDS
        );
    }

    public static SourceTraceEntryCompletionResult incomplete(
            String symbol,
            String timeframe,
            SourceTraceEntryCompletionMissingReasonEnum missingReason,
            List<String> missingFields
    ) {
        SourceTraceEntryCompletionResult result = new SourceTraceEntryCompletionResult();
        result.symbol = symbol;
        result.timeframe = timeframe;
        result.missingReason = missingReason == null
                ? SourceTraceEntryCompletionMissingReasonEnum.UNSAFE_COMPLETION
                : missingReason;
        result.missingFields.clear();
        if (missingFields == null || missingFields.isEmpty()) {
            result.missingFields.addAll(DEFAULT_MISSING_FIELDS);
        } else {
            result.missingFields.addAll(missingFields);
        }
        return result;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public SourceTraceEntryCompletionStatusEnum getCompletionStatus() {
        return completionStatus;
    }

    public SourceTraceEntryCompletionMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public BigDecimal getEntryPriceSource() {
        return entryPriceSource;
    }

    public String getEntrySourceType() {
        return entrySourceType;
    }

    public String getEntrySourceTimeframe() {
        return entrySourceTimeframe;
    }

    public String getEntrySourceReason() {
        return entrySourceReason;
    }

    public String getEntrySourceRef() {
        return entrySourceRef;
    }

    public boolean isSourceTraceEntryCompleted() {
        return sourceTraceEntryCompleted;
    }

    public boolean isCompletionReady() {
        return completionReady;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }
}
