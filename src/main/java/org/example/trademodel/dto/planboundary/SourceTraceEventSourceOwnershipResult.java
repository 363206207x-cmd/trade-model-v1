package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class SourceTraceEventSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "eventSource"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceEventSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceEventSourceMissingReasonEnum missingReason =
            SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceEventSourceReviewModeEnum reviewMode =
            SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY;
    private final String eventSource = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceEventSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceEventSourceOwnershipResult result = new SourceTraceEventSourceOwnershipResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
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

    public SourceTraceEventSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceEventSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceEventSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public String getEventSource() {
        return eventSource;
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
