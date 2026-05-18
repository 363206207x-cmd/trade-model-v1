package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class SourceTraceMultiTimeframeSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "multiTimeframeSource"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceMultiTimeframeSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceMultiTimeframeSourceMissingReasonEnum missingReason =
            SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceMultiTimeframeSourceReviewModeEnum reviewMode =
            SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY;
    private final String multiTimeframeSource = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceMultiTimeframeSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceMultiTimeframeSourceOwnershipResult result = new SourceTraceMultiTimeframeSourceOwnershipResult();
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

    public SourceTraceMultiTimeframeSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceMultiTimeframeSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceMultiTimeframeSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public String getMultiTimeframeSource() {
        return multiTimeframeSource;
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
