package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceEntrySourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceEntrySourceOwnershipStatusEnum ownershipStatus =
            SourceTraceEntrySourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceEntrySourceMissingReasonEnum missingReason =
            SourceTraceEntrySourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final BigDecimal entryPriceSource = null;
    private final String entrySourceType = null;
    private final String entrySourceTimeframe = null;
    private final String entrySourceReason = null;
    private final String entrySourceRef = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceEntrySourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceEntrySourceOwnershipResult result = new SourceTraceEntrySourceOwnershipResult();
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

    public SourceTraceEntrySourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceEntrySourceMissingReasonEnum getMissingReason() {
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
