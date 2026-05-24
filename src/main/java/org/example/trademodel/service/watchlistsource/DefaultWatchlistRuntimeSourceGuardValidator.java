package org.example.trademodel.service.watchlistsource;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeFreshnessStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;

public class DefaultWatchlistRuntimeSourceGuardValidator implements WatchlistRuntimeSourceGuardValidator {

    private static final String REASON_SOURCE_MISSING = "SOURCE_MISSING";
    private static final String REASON_NULL_SOURCE = "NULL_SOURCE";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_WATCHLIST_MEMBERSHIP_UNKNOWN = "WATCHLIST_MEMBERSHIP_UNKNOWN";
    private static final String REASON_MISSING_FIELDS = "MISSING_FIELDS";
    private static final String REASON_SOURCE_UNAVAILABLE = "SOURCE_UNAVAILABLE";
    private static final String REASON_SOURCE_STATUS_INCOMPLETE = "SOURCE_STATUS_INCOMPLETE";
    private static final String REASON_STALE_REVIEW_ONLY = "STALE_REVIEW_ONLY";
    private static final String REASON_FRESHNESS_UNKNOWN = "FRESHNESS_UNKNOWN";
    private static final String REASON_FRESHNESS_NOT_AVAILABLE = "FRESHNESS_NOT_AVAILABLE";
    private static final String REASON_FRESHNESS_EXPIRED = "FRESHNESS_EXPIRED";
    private static final String REASON_FRESHNESS_STALE = "FRESHNESS_STALE";
    private static final String REASON_FRESH_REVIEW_ONLY = "FRESH_REVIEW_ONLY";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_WATCHLIST_MEMBER = "watchlistMember";
    private static final String FIELD_FRESHNESS_STATUS = "freshnessStatus";

    @Override
    public WatchlistRuntimeSourceDTO validate(WatchlistRuntimeSourceDTO source) {
        if (source == null) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    null,
                    List.of(FIELD_SOURCE),
                    List.of(REASON_SOURCE_MISSING, REASON_NULL_SOURCE)
            );
        }

        String symbol = source.getSymbol();
        List<String> baseReasons = source.getBlockingReasons();

        if (Boolean.FALSE.equals(source.getWatchlistMember())) {
            return WatchlistRuntimeSourceDTO.blockedNotWatchlist(
                    symbol,
                    withReasons(baseReasons, REASON_BLOCKED_NOT_WATCHLIST)
            );
        }

        WatchlistRuntimeSourceStatusEnum sourceStatus = source.getSourceStatus();
        if (WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE.equals(sourceStatus)) {
            return WatchlistRuntimeSourceDTO.sourceUnavailable(
                    symbol,
                    withReasons(baseReasons, REASON_SOURCE_UNAVAILABLE)
            );
        }
        if (WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY.equals(sourceStatus)) {
            return WatchlistRuntimeSourceDTO.staleReviewOnly(
                    symbol,
                    source.getStaleFields(),
                    withReasons(baseReasons, REASON_STALE_REVIEW_ONLY)
            );
        }

        List<String> missingFields = source.getMissingFields();
        if (!missingFields.isEmpty()) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    missingFields,
                    withReasons(baseReasons, REASON_MISSING_FIELDS, missingFields)
            );
        }

        if (source.getWatchlistMember() == null) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    withFields(missingFields, FIELD_WATCHLIST_MEMBER),
                    withReasons(baseReasons, REASON_WATCHLIST_MEMBERSHIP_UNKNOWN)
            );
        }

        if (WatchlistRuntimeSourceStatusEnum.INCOMPLETE.equals(sourceStatus)) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    missingFields,
                    withReasons(baseReasons, REASON_SOURCE_STATUS_INCOMPLETE)
            );
        }

        WatchlistRuntimeFreshnessStatusEnum freshnessStatus = source.getFreshnessStatus();
        if (WatchlistRuntimeFreshnessStatusEnum.UNKNOWN.equals(freshnessStatus)) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    withFields(missingFields, FIELD_FRESHNESS_STATUS),
                    withReasons(baseReasons, REASON_FRESHNESS_UNKNOWN)
            );
        }
        if (WatchlistRuntimeFreshnessStatusEnum.NOT_AVAILABLE.equals(freshnessStatus)) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    withFields(missingFields, FIELD_FRESHNESS_STATUS),
                    withReasons(baseReasons, REASON_FRESHNESS_NOT_AVAILABLE)
            );
        }
        if (WatchlistRuntimeFreshnessStatusEnum.EXPIRED.equals(freshnessStatus)) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbol,
                    withFields(source.getStaleFields(), FIELD_FRESHNESS_STATUS),
                    withReasons(baseReasons, REASON_FRESHNESS_EXPIRED)
            );
        }
        if (WatchlistRuntimeFreshnessStatusEnum.STALE.equals(freshnessStatus)) {
            return WatchlistRuntimeSourceDTO.staleReviewOnly(
                    symbol,
                    source.getStaleFields(),
                    withReasons(baseReasons, REASON_FRESHNESS_STALE)
            );
        }

        return WatchlistRuntimeSourceDTO.availableReviewOnly(
                symbol,
                source.getSourceType(),
                source.getSourceRef(),
                withReasons(baseReasons, REASON_FRESH_REVIEW_ONLY)
        );
    }

    private static List<String> withFields(
            List<String> baseFields,
            String requiredField
    ) {
        List<String> resolvedFields = new ArrayList<>();
        if (baseFields != null) {
            resolvedFields.addAll(baseFields);
        }
        addIfAbsent(resolvedFields, requiredField);
        return resolvedFields;
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

    private static void addIfAbsent(List<String> values, String value) {
        if (value != null && !values.contains(value)) {
            values.add(value);
        }
    }
}
