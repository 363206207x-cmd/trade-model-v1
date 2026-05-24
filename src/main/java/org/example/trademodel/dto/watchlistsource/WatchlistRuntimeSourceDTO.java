package org.example.trademodel.dto.watchlistsource;

import java.util.ArrayList;
import java.util.List;

public class WatchlistRuntimeSourceDTO {

    private static final String DATA_QUALITY_BLOCKED = "BLOCKED";
    private static final String DATA_QUALITY_INCOMPLETE = "INCOMPLETE";
    private static final String DATA_QUALITY_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String STALE_STATUS_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String STALE_STATUS_UNKNOWN = "UNKNOWN";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_SOURCE_UNAVAILABLE = "SOURCE_UNAVAILABLE";
    private static final String REASON_MISSING_SOURCE = "MISSING_SOURCE";

    private final String symbol;
    private final Boolean watchlistMember;
    private final String watchlistSource;
    private final WatchlistRuntimeSourceTypeEnum sourceType;
    private final String sourceRef;
    private final String sourceUpdatedAt;
    private final String receivedAt;
    private final WatchlistRuntimeFreshnessStatusEnum freshnessStatus;
    private final WatchlistRuntimeSourceStatusEnum sourceStatus;
    private final String staleStatus;
    private final String dataQualityStatus;
    private final List<String> missingFields;
    private final List<String> staleFields;
    private final List<String> blockingReasons;
    private final Boolean manualReviewRequired;
    private final Boolean notTradeInstruction;
    private final Boolean opportunityPushAllowed;
    private final Boolean readinessUpgraded;
    private final Boolean tradingActionCreated;
    private final Boolean entryStopTpRrGenerated;

    private WatchlistRuntimeSourceDTO(
            String symbol,
            Boolean watchlistMember,
            String watchlistSource,
            WatchlistRuntimeSourceTypeEnum sourceType,
            String sourceRef,
            String sourceUpdatedAt,
            String receivedAt,
            WatchlistRuntimeFreshnessStatusEnum freshnessStatus,
            WatchlistRuntimeSourceStatusEnum sourceStatus,
            String staleStatus,
            String dataQualityStatus,
            List<String> missingFields,
            List<String> staleFields,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.watchlistMember = watchlistMember;
        this.watchlistSource = watchlistSource;
        this.sourceType = sourceType == null ? WatchlistRuntimeSourceTypeEnum.UNKNOWN : sourceType;
        this.sourceRef = sourceRef;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.receivedAt = receivedAt;
        this.freshnessStatus = freshnessStatus == null
                ? WatchlistRuntimeFreshnessStatusEnum.UNKNOWN
                : freshnessStatus;
        this.sourceStatus = sourceStatus == null
                ? WatchlistRuntimeSourceStatusEnum.NOT_IMPLEMENTED
                : sourceStatus;
        this.staleStatus = staleStatus;
        this.dataQualityStatus = dataQualityStatus;
        this.missingFields = copy(missingFields);
        this.staleFields = copy(staleFields);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.opportunityPushAllowed = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static WatchlistRuntimeSourceDTO blockedNotWatchlist(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSourceDTO(
                symbol,
                false,
                null,
                WatchlistRuntimeSourceTypeEnum.UNKNOWN,
                null,
                null,
                null,
                WatchlistRuntimeFreshnessStatusEnum.NOT_AVAILABLE,
                WatchlistRuntimeSourceStatusEnum.BLOCKED_NOT_WATCHLIST,
                STALE_STATUS_UNKNOWN,
                DATA_QUALITY_BLOCKED,
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_BLOCKED_NOT_WATCHLIST)
        );
    }

    public static WatchlistRuntimeSourceDTO incomplete(
            String symbol,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSourceDTO(
                symbol,
                null,
                null,
                WatchlistRuntimeSourceTypeEnum.UNKNOWN,
                null,
                null,
                null,
                WatchlistRuntimeFreshnessStatusEnum.UNKNOWN,
                WatchlistRuntimeSourceStatusEnum.INCOMPLETE,
                STALE_STATUS_UNKNOWN,
                DATA_QUALITY_INCOMPLETE,
                missingFields,
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static WatchlistRuntimeSourceDTO staleReviewOnly(
            String symbol,
            List<String> staleFields,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSourceDTO(
                symbol,
                true,
                null,
                WatchlistRuntimeSourceTypeEnum.UNKNOWN,
                null,
                null,
                null,
                WatchlistRuntimeFreshnessStatusEnum.STALE,
                WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY,
                STALE_STATUS_REVIEW_ONLY,
                DATA_QUALITY_REVIEW_ONLY,
                List.of(),
                staleFields,
                blockingReasons
        );
    }

    public static WatchlistRuntimeSourceDTO sourceUnavailable(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSourceDTO(
                symbol,
                null,
                null,
                WatchlistRuntimeSourceTypeEnum.UNKNOWN,
                null,
                null,
                null,
                WatchlistRuntimeFreshnessStatusEnum.NOT_AVAILABLE,
                WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE,
                STALE_STATUS_UNKNOWN,
                DATA_QUALITY_INCOMPLETE,
                List.of("sourceRef"),
                List.of(),
                withReason(blockingReasons, REASON_SOURCE_UNAVAILABLE)
        );
    }

    public static WatchlistRuntimeSourceDTO availableReviewOnly(
            String symbol,
            WatchlistRuntimeSourceTypeEnum sourceType,
            String sourceRef,
            List<String> blockingReasons
    ) {
        if (sourceType == null || WatchlistRuntimeSourceTypeEnum.UNKNOWN.equals(sourceType)) {
            return incomplete(
                    symbol,
                    List.of("sourceType"),
                    withReason(blockingReasons, REASON_MISSING_SOURCE)
            );
        }
        if (sourceRef == null || sourceRef.isBlank()) {
            return incomplete(
                    symbol,
                    List.of("sourceRef"),
                    withReason(blockingReasons, REASON_MISSING_SOURCE)
            );
        }
        return new WatchlistRuntimeSourceDTO(
                symbol,
                true,
                null,
                sourceType,
                sourceRef,
                null,
                null,
                WatchlistRuntimeFreshnessStatusEnum.FRESH,
                WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY,
                STALE_STATUS_UNKNOWN,
                DATA_QUALITY_REVIEW_ONLY,
                List.of(),
                List.of(),
                blockingReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public Boolean getWatchlistMember() {
        return watchlistMember;
    }

    public String getWatchlistSource() {
        return watchlistSource;
    }

    public WatchlistRuntimeSourceTypeEnum getSourceType() {
        return sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public String getReceivedAt() {
        return receivedAt;
    }

    public WatchlistRuntimeFreshnessStatusEnum getFreshnessStatus() {
        return freshnessStatus;
    }

    public WatchlistRuntimeSourceStatusEnum getSourceStatus() {
        return sourceStatus;
    }

    public String getStaleStatus() {
        return staleStatus;
    }

    public String getDataQualityStatus() {
        return dataQualityStatus;
    }

    public List<String> getMissingFields() {
        return copy(missingFields);
    }

    public List<String> getStaleFields() {
        return copy(staleFields);
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public Boolean getManualReviewRequired() {
        return manualReviewRequired;
    }

    public Boolean getNotTradeInstruction() {
        return notTradeInstruction;
    }

    public Boolean getOpportunityPushAllowed() {
        return opportunityPushAllowed;
    }

    public Boolean getReadinessUpgraded() {
        return readinessUpgraded;
    }

    public Boolean getTradingActionCreated() {
        return tradingActionCreated;
    }

    public Boolean getEntryStopTpRrGenerated() {
        return entryStopTpRrGenerated;
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
