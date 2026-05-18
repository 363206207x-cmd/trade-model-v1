package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class EntryOwnershipValidationResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "entryOwnershipRequest",
            "runtimeKlineContext",
            "ruleOwnedEntryCandidate",
            "freshness",
            "conflict"
    );

    private String symbol;
    private String timeframe;
    private final EntryOwnershipValidationStatusEnum validationStatus =
            EntryOwnershipValidationStatusEnum.INCOMPLETE;
    private final EntryOwnershipValidationMissingReasonEnum missingReason =
            EntryOwnershipValidationMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static EntryOwnershipValidationResult missingSource(String symbol, String timeframe) {
        return missingSource(symbol, timeframe, DEFAULT_MISSING_FIELDS);
    }

    public static EntryOwnershipValidationResult missingSource(
            String symbol,
            String timeframe,
            List<String> missingFields
    ) {
        EntryOwnershipValidationResult result = new EntryOwnershipValidationResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
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

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public EntryOwnershipValidationStatusEnum getValidationStatus() {
        return validationStatus;
    }

    public EntryOwnershipValidationMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }
}
