package org.example.trademodel.dto.watchlistscan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WatchlistScanScoreDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final WatchlistScanScoreStatusEnum scoreStatus;
    private final BigDecimal scanScore;
    private final BigDecimal confidenceScore;
    private final BigDecimal dataQualityScore;
    private final String source;
    private final String sourceTrace;
    private final List<String> blockingReasons;
    private final List<String> reviewReasons;
    private final List<String> scoreReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean opportunityPushAllowed;
    private final boolean candidateAttentionAllowed;
    private final boolean promoteToHomeAllowed;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private WatchlistScanScoreDTO(
            String symbol,
            WatchlistScanScoreStatusEnum scoreStatus,
            BigDecimal scanScore,
            BigDecimal confidenceScore,
            BigDecimal dataQualityScore,
            String source,
            String sourceTrace,
            List<String> blockingReasons,
            List<String> reviewReasons,
            List<String> scoreReasons
    ) {
        this.symbol = symbol;
        this.scoreStatus = scoreStatus == null ? WatchlistScanScoreStatusEnum.INCOMPLETE : scoreStatus;
        this.scanScore = scanScore;
        this.confidenceScore = confidenceScore;
        this.dataQualityScore = dataQualityScore;
        this.source = source;
        this.sourceTrace = sourceTrace;
        this.blockingReasons = copy(blockingReasons);
        this.reviewReasons = copy(reviewReasons);
        this.scoreReasons = copy(scoreReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.opportunityPushAllowed = false;
        this.candidateAttentionAllowed = false;
        this.promoteToHomeAllowed = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static WatchlistScanScoreDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanScoreDTO(
                symbol,
                WatchlistScanScoreStatusEnum.INCOMPLETE,
                null,
                null,
                null,
                null,
                null,
                withReason(blockingReasons, REASON_INCOMPLETE),
                List.of(),
                List.of()
        );
    }

    public static WatchlistScanScoreDTO disabled(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanScoreDTO(
                symbol,
                WatchlistScanScoreStatusEnum.DISABLED,
                null,
                null,
                null,
                null,
                null,
                withReason(blockingReasons, REASON_DISABLED),
                List.of(),
                List.of()
        );
    }

    public static WatchlistScanScoreDTO reviewOnly(
            String symbol,
            BigDecimal scanScore,
            BigDecimal confidenceScore,
            BigDecimal dataQualityScore,
            String source,
            List<String> scoreReasons,
            List<String> blockingReasons
    ) {
        return new WatchlistScanScoreDTO(
                symbol,
                WatchlistScanScoreStatusEnum.REVIEW_ONLY,
                safeScore(scanScore),
                safeScore(confidenceScore),
                safeScore(dataQualityScore),
                source,
                source,
                blockingReasons,
                List.of("SCANSCORE_REVIEW_ONLY"),
                scoreReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public WatchlistScanScoreStatusEnum getScoreStatus() {
        return scoreStatus;
    }

    public BigDecimal getScanScore() {
        return scanScore;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public String getSource() {
        return source;
    }

    public String getSourceTrace() {
        return sourceTrace;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public List<String> getReviewReasons() {
        return copy(reviewReasons);
    }

    public List<String> getScoreReasons() {
        return copy(scoreReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isOpportunityPushAllowed() {
        return opportunityPushAllowed;
    }

    public boolean isCandidateAttentionAllowed() {
        return candidateAttentionAllowed;
    }

    public boolean isPromoteToHomeAllowed() {
        return promoteToHomeAllowed;
    }

    public boolean isReadinessUpgraded() {
        return readinessUpgraded;
    }

    public boolean isTradingActionCreated() {
        return tradingActionCreated;
    }

    public boolean isEntryStopTpRrGenerated() {
        return entryStopTpRrGenerated;
    }

    private static BigDecimal safeScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = copy(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
