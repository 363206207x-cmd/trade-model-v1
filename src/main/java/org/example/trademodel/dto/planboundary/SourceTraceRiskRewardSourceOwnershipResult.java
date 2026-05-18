package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceRiskRewardSourceOwnershipResult {

    private static final List<String> DEFAULT_MISSING_FIELDS = List.of(
            "rrSource",
            "rrRuleRef"
    );

    private String symbol;
    private String timeframe;
    private final SourceTraceRiskRewardSourceOwnershipStatusEnum ownershipStatus =
            SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE;
    private final SourceTraceRiskRewardSourceMissingReasonEnum missingReason =
            SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE;
    private final SourceTraceRiskRewardSourceReviewModeEnum reviewMode =
            SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY;
    private final BigDecimal rrSource = null;
    private final String rrRuleRef = null;
    private final List<String> missingFields = new ArrayList<>(DEFAULT_MISSING_FIELDS);
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public static SourceTraceRiskRewardSourceOwnershipResult missingSource(String symbol, String timeframe) {
        SourceTraceRiskRewardSourceOwnershipResult result = new SourceTraceRiskRewardSourceOwnershipResult();
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

    public SourceTraceRiskRewardSourceOwnershipStatusEnum getOwnershipStatus() {
        return ownershipStatus;
    }

    public SourceTraceRiskRewardSourceMissingReasonEnum getMissingReason() {
        return missingReason;
    }

    public SourceTraceRiskRewardSourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public BigDecimal getRrSource() {
        return rrSource;
    }

    public String getRrRuleRef() {
        return rrRuleRef;
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
