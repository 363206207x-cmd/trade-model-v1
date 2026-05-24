package org.example.trademodel.dto.watchlistsource;

import java.util.ArrayList;
import java.util.List;

public class RuntimeSourceReadRequestDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";

    private final String symbol;
    private final Boolean watchlistPoolOnly;
    private final String requestedBy;
    private final String requestReason;
    private final Boolean manualReviewRequired;
    private final Boolean notTradeInstruction;
    private final List<String> missingFields;
    private final List<String> blockingReasons;

    private RuntimeSourceReadRequestDTO(
            String symbol,
            Boolean watchlistPoolOnly,
            String requestedBy,
            String requestReason,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.watchlistPoolOnly = watchlistPoolOnly == null || watchlistPoolOnly;
        this.requestedBy = requestedBy;
        this.requestReason = requestReason;
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.missingFields = copy(missingFields);
        this.blockingReasons = copy(blockingReasons);
    }

    public static RuntimeSourceReadRequestDTO forWatchlistPool(
            String symbol,
            String requestedBy,
            String requestReason
    ) {
        return new RuntimeSourceReadRequestDTO(
                symbol,
                true,
                requestedBy,
                requestReason,
                List.of(),
                List.of()
        );
    }

    public static RuntimeSourceReadRequestDTO incomplete(
            String symbol,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        return new RuntimeSourceReadRequestDTO(
                symbol,
                true,
                null,
                null,
                missingFields,
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public Boolean getWatchlistPoolOnly() {
        return watchlistPoolOnly;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public Boolean getManualReviewRequired() {
        return manualReviewRequired;
    }

    public Boolean getNotTradeInstruction() {
        return notTradeInstruction;
    }

    public List<String> getMissingFields() {
        return copy(missingFields);
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
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
