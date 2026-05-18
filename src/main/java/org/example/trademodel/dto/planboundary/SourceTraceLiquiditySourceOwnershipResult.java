package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class SourceTraceLiquiditySourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "liquiditySource"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceLiquiditySourceOwnershipStatusEnum ownershipStatus =
            SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceLiquiditySourceMissingReasonEnum missingReason =
            SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceLiquiditySourceReviewModeEnum reviewMode =
            SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY;
    private final String liquiditySource = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceLiquiditySourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceLiquiditySourceOwnershipResult result = new SourceTraceLiquiditySourceOwnershipResult();
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

    public SourceTraceLiquiditySourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceLiquiditySourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceLiquiditySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public String getLiquiditySource() {
        return liquiditySource;
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
