package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceStopSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "stopPriceSource",
            "stopSourceType",
            "stopSourceTimeframe",
            "stopSourceReason",
            "stopSourceRef"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceStopSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceStopSourceMissingReasonEnum missingReason =
            SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceStopSourceReviewModeEnum reviewMode =
            SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY;
    private final BigDecimal stopPriceSource = null;
    private final String stopSourceType = null;
    private final String stopSourceTimeframe = null;
    private final String stopSourceReason = null;
    private final String stopSourceRef = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceStopSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceStopSourceOwnershipResult result = new SourceTraceStopSourceOwnershipResult();
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

    public SourceTraceStopSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceStopSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceStopSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public BigDecimal getStopPriceSource() {
        return stopPriceSource;
    }

    public String getStopSourceType() {
        return stopSourceType;
    }

    public String getStopSourceTimeframe() {
        return stopSourceTimeframe;
    }

    public String getStopSourceReason() {
        return stopSourceReason;
    }

    public String getStopSourceRef() {
        return stopSourceRef;
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
