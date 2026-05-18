package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceTakeProfitSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "tpPriceSources",
            "tpSourceType",
            "tpSourceTimeframe",
            "tpSourceReason",
            "tpSourceRef"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceTakeProfitSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceTakeProfitSourceMissingReasonEnum missingReason =
            SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceTakeProfitSourceReviewModeEnum reviewMode =
            SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY;
    private final List<BigDecimal> tpPriceSources = new ArrayList<>();
    private final String tpSourceType = null;
    private final String tpSourceTimeframe = null;
    private final String tpSourceReason = null;
    private final String tpSourceRef = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceTakeProfitSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceTakeProfitSourceOwnershipResult result = new SourceTraceTakeProfitSourceOwnershipResult();
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

    public SourceTraceTakeProfitSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceTakeProfitSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceTakeProfitSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public List<BigDecimal> getTpPriceSources() {
        return new ArrayList<>(tpPriceSources);
    }

    public String getTpSourceType() {
        return tpSourceType;
    }

    public String getTpSourceTimeframe() {
        return tpSourceTimeframe;
    }

    public String getTpSourceReason() {
        return tpSourceReason;
    }

    public String getTpSourceRef() {
        return tpSourceRef;
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
