package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class SourceTraceWickSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "wickSource"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceWickSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceWickSourceMissingReasonEnum missingReason =
            SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceWickSourceReviewModeEnum reviewMode =
            SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY;
    private final String wickSource = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceWickSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceWickSourceOwnershipResult result = new SourceTraceWickSourceOwnershipResult();
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

    public SourceTraceWickSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceWickSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceWickSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public String getWickSource() {
        return wickSource;
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
