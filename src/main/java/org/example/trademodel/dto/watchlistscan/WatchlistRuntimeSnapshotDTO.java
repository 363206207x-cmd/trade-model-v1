package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class WatchlistRuntimeSnapshotDTO {

    private static final String DATA_QUALITY_BLOCKED = "BLOCKED";
    private static final String DATA_QUALITY_INCOMPLETE = "INCOMPLETE";
    private static final String DATA_QUALITY_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String STALE_STATUS_UNKNOWN = "UNKNOWN";
    private static final String STALE_STATUS_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String REASON_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_MEMBERSHIP_UNKNOWN = "WATCHLIST_MEMBERSHIP_UNKNOWN";

    private final String symbol;
    private final Boolean watchlistMember;
    private final String watchlistSource;
    private final String dataQualityStatus;
    private final String staleStatus;
    private final List<String> missingFields;
    private final List<String> blockingReasons;
    private final Boolean manualReviewRequired;
    private final Boolean notTradeInstruction;

    private WatchlistRuntimeSnapshotDTO(
            String symbol,
            Boolean watchlistMember,
            String watchlistSource,
            String dataQualityStatus,
            String staleStatus,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.watchlistMember = watchlistMember;
        this.watchlistSource = watchlistSource;
        this.dataQualityStatus = dataQualityStatus;
        this.staleStatus = staleStatus;
        this.missingFields = copy(missingFields);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
    }

    public static WatchlistRuntimeSnapshotDTO blockedNotWatchlist(
            String symbol,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSnapshotDTO(
                symbol,
                false,
                null,
                DATA_QUALITY_BLOCKED,
                STALE_STATUS_UNKNOWN,
                List.of(),
                withReason(blockingReasons, REASON_NOT_WATCHLIST)
        );
    }

    public static WatchlistRuntimeSnapshotDTO incomplete(
            String symbol,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        return new WatchlistRuntimeSnapshotDTO(
                symbol,
                null,
                null,
                DATA_QUALITY_INCOMPLETE,
                STALE_STATUS_UNKNOWN,
                missingFields,
                withReason(blockingReasons, REASON_MEMBERSHIP_UNKNOWN)
        );
    }

    public static WatchlistRuntimeSnapshotDTO reviewOnly(
            String symbol,
            Boolean watchlistMember,
            List<String> blockingReasons
    ) {
        if (watchlistMember == null) {
            return incomplete(symbol, List.of("watchlistMember"), blockingReasons);
        }
        if (Boolean.FALSE.equals(watchlistMember)) {
            return blockedNotWatchlist(symbol, blockingReasons);
        }
        return new WatchlistRuntimeSnapshotDTO(
                symbol,
                true,
                null,
                DATA_QUALITY_REVIEW_ONLY,
                STALE_STATUS_REVIEW_ONLY,
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

    public String getDataQualityStatus() {
        return dataQualityStatus;
    }

    public String getStaleStatus() {
        return staleStatus;
    }

    public List<String> getMissingFields() {
        return copy(missingFields);
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
