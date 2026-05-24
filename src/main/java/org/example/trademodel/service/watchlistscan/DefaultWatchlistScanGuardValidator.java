package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;

public class DefaultWatchlistScanGuardValidator implements WatchlistScanGuardValidator {

    private static final String DATA_QUALITY_BLOCKED = "BLOCKED";
    private static final String DATA_QUALITY_INCOMPLETE = "INCOMPLETE";
    private static final String DATA_QUALITY_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String STALE_STATUS_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String REASON_NULL_SNAPSHOT = "NULL_SNAPSHOT";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_WATCHLIST_MEMBERSHIP_UNKNOWN = "WATCHLIST_MEMBERSHIP_UNKNOWN";
    private static final String REASON_MISSING_FIELDS = "MISSING_FIELDS";
    private static final String REASON_DATA_QUALITY_BLOCKED = "DATA_QUALITY_BLOCKED";
    private static final String REASON_DATA_QUALITY_INCOMPLETE = "DATA_QUALITY_INCOMPLETE";
    private static final String REASON_REVIEW_ONLY = "REVIEW_ONLY";
    private static final String REASON_STALE_REVIEW_ONLY = "STALE_REVIEW_ONLY";
    private static final String REASON_NO_SCORE_NO_PUSH_GUARD = "NO_SCORE_NO_PUSH_GUARD";

    @Override
    public WatchlistScanResultDTO validate(WatchlistRuntimeSnapshotDTO snapshot) {
        if (snapshot == null) {
            return WatchlistScanResultDTO.incomplete(null, List.of(REASON_NULL_SNAPSHOT));
        }

        String symbol = snapshot.getSymbol();
        List<String> baseReasons = snapshot.getBlockingReasons();

        if (Boolean.FALSE.equals(snapshot.getWatchlistMember())) {
            return WatchlistScanResultDTO.blockedNotWatchlist(
                    symbol,
                    withReasons(baseReasons, REASON_BLOCKED_NOT_WATCHLIST)
            );
        }

        List<String> missingFields = snapshot.getMissingFields();
        if (!missingFields.isEmpty()) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    withReasons(baseReasons, REASON_MISSING_FIELDS, missingFields)
            );
        }

        if (snapshot.getWatchlistMember() == null) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    withReasons(baseReasons, REASON_WATCHLIST_MEMBERSHIP_UNKNOWN)
            );
        }

        String dataQualityStatus = snapshot.getDataQualityStatus();
        if (DATA_QUALITY_BLOCKED.equals(dataQualityStatus)) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    withReasons(baseReasons, REASON_DATA_QUALITY_BLOCKED)
            );
        }
        if (DATA_QUALITY_INCOMPLETE.equals(dataQualityStatus)) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    withReasons(baseReasons, REASON_DATA_QUALITY_INCOMPLETE)
            );
        }

        if (DATA_QUALITY_REVIEW_ONLY.equals(dataQualityStatus)) {
            return WatchlistScanResultDTO.reviewOnly(
                    symbol,
                    withReasons(baseReasons, REASON_REVIEW_ONLY)
            );
        }

        if (STALE_STATUS_REVIEW_ONLY.equals(snapshot.getStaleStatus())) {
            return WatchlistScanResultDTO.reviewOnly(
                    symbol,
                    withReasons(baseReasons, REASON_STALE_REVIEW_ONLY)
            );
        }

        return WatchlistScanResultDTO.reviewOnly(
                symbol,
                withReasons(baseReasons, REASON_NO_SCORE_NO_PUSH_GUARD)
        );
    }

    private static List<String> withReasons(
            List<String> baseReasons,
            String requiredReason
    ) {
        return withReasons(baseReasons, requiredReason, List.of());
    }

    private static List<String> withReasons(
            List<String> baseReasons,
            String requiredReason,
            List<String> detailReasons
    ) {
        List<String> resolvedReasons = new ArrayList<>();
        if (baseReasons != null) {
            resolvedReasons.addAll(baseReasons);
        }
        addIfAbsent(resolvedReasons, requiredReason);
        if (detailReasons != null) {
            for (String detailReason : detailReasons) {
                addIfAbsent(resolvedReasons, detailReason);
            }
        }
        return resolvedReasons;
    }

    private static void addIfAbsent(List<String> reasons, String reason) {
        if (reason != null && !reasons.contains(reason)) {
            reasons.add(reason);
        }
    }
}
