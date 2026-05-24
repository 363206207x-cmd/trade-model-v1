package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class WatchlistScanResultDTO {

    private static final String REASON_DISABLED = "DISABLED";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String REASON_CANDIDATE_ATTENTION_REVIEW_ONLY = "CANDIDATE_ATTENTION_REVIEW_ONLY";
    private static final String REASON_PROMOTE_TO_HOME_REVIEW_ONLY = "PROMOTE_TO_HOME_REVIEW_ONLY";

    private final String symbol;
    private final Boolean watchlistMember;
    private final WatchlistScanStatusEnum scanStatus;
    private final String scanReason;
    private final String dataQualityStatus;
    private final List<String> blockingReasons;
    private final Boolean candidateAttentionAllowed;
    private final Boolean promoteToHomeAllowed;
    private final Boolean opportunityPushAllowed;
    private final Boolean manualReviewRequired;
    private final Boolean notTradeInstruction;
    private final Boolean entryStopTpRrGenerated;
    private final Boolean readinessUpgraded;
    private final Boolean tradingActionCreated;

    private WatchlistScanResultDTO(
            String symbol,
            Boolean watchlistMember,
            WatchlistScanStatusEnum scanStatus,
            String scanReason,
            String dataQualityStatus,
            List<String> blockingReasons,
            Boolean candidateAttentionAllowed,
            Boolean promoteToHomeAllowed
    ) {
        this.symbol = symbol;
        this.watchlistMember = watchlistMember;
        this.scanStatus = scanStatus;
        this.scanReason = scanReason;
        this.dataQualityStatus = dataQualityStatus;
        this.blockingReasons = copy(blockingReasons);
        this.candidateAttentionAllowed = Boolean.TRUE.equals(candidateAttentionAllowed);
        this.promoteToHomeAllowed = Boolean.TRUE.equals(promoteToHomeAllowed);
        this.opportunityPushAllowed = false;
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.entryStopTpRrGenerated = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
    }

    public static WatchlistScanResultDTO disabled(String symbol, String reason) {
        return new WatchlistScanResultDTO(
                symbol,
                null,
                WatchlistScanStatusEnum.DISABLED,
                reason == null ? REASON_DISABLED : reason,
                "DISABLED",
                reason == null ? List.of(REASON_DISABLED) : List.of(reason),
                false,
                false
        );
    }

    public static WatchlistScanResultDTO blockedNotWatchlist(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanResultDTO(
                symbol,
                false,
                WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST,
                REASON_BLOCKED_NOT_WATCHLIST,
                "BLOCKED",
                withReason(blockingReasons, REASON_BLOCKED_NOT_WATCHLIST),
                false,
                false
        );
    }

    public static WatchlistScanResultDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanResultDTO(
                symbol,
                null,
                WatchlistScanStatusEnum.INCOMPLETE,
                REASON_INCOMPLETE,
                "INCOMPLETE",
                withReason(blockingReasons, REASON_INCOMPLETE),
                false,
                false
        );
    }

    public static WatchlistScanResultDTO reviewOnly(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanResultDTO(
                symbol,
                true,
                WatchlistScanStatusEnum.REVIEW_ONLY,
                REASON_REVIEW_ONLY,
                "REVIEW_ONLY",
                blockingReasons,
                false,
                false
        );
    }

    public static WatchlistScanResultDTO candidateAttentionReviewOnly(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanResultDTO(
                symbol,
                true,
                WatchlistScanStatusEnum.CANDIDATE_ATTENTION,
                REASON_CANDIDATE_ATTENTION_REVIEW_ONLY,
                "REVIEW_ONLY",
                blockingReasons,
                true,
                false
        );
    }

    public static WatchlistScanResultDTO promoteToHomeReviewOnly(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistScanResultDTO(
                symbol,
                true,
                WatchlistScanStatusEnum.PROMOTE_TO_HOME_REVIEW,
                REASON_PROMOTE_TO_HOME_REVIEW_ONLY,
                "REVIEW_ONLY",
                blockingReasons,
                false,
                true
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public Boolean getWatchlistMember() {
        return watchlistMember;
    }

    public WatchlistScanStatusEnum getScanStatus() {
        return scanStatus;
    }

    public String getScanReason() {
        return scanReason;
    }

    public String getDataQualityStatus() {
        return dataQualityStatus;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public Boolean getCandidateAttentionAllowed() {
        return candidateAttentionAllowed;
    }

    public Boolean getPromoteToHomeAllowed() {
        return promoteToHomeAllowed;
    }

    public Boolean getOpportunityPushAllowed() {
        return opportunityPushAllowed;
    }

    public Boolean getManualReviewRequired() {
        return manualReviewRequired;
    }

    public Boolean getNotTradeInstruction() {
        return notTradeInstruction;
    }

    public Boolean getEntryStopTpRrGenerated() {
        return entryStopTpRrGenerated;
    }

    public Boolean getReadinessUpgraded() {
        return readinessUpgraded;
    }

    public Boolean getTradingActionCreated() {
        return tradingActionCreated;
    }

    private static List<String> withReason(List<String> reasons, String defaultReason) {
        List<String> resolvedReasons = copy(reasons);
        if (!resolvedReasons.contains(defaultReason)) {
            resolvedReasons.add(defaultReason);
        }
        return resolvedReasons;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
