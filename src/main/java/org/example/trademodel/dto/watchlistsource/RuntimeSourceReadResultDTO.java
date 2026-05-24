package org.example.trademodel.dto.watchlistsource;

import java.util.ArrayList;
import java.util.List;

public class RuntimeSourceReadResultDTO {

    private static final String FIELD_RUNTIME_SOURCE = "runtimeSource";
    private static final String REASON_RUNTIME_SOURCE_MISSING = "RUNTIME_SOURCE_MISSING";

    private final String symbol;
    private final WatchlistRuntimeSourceDTO runtimeSource;
    private final WatchlistRuntimeSourceStatusEnum readStatus;
    private final List<String> missingFields;
    private final List<String> blockingReasons;
    private final Boolean manualReviewRequired;
    private final Boolean notTradeInstruction;
    private final Boolean opportunityPushAllowed;
    private final Boolean readinessUpgraded;
    private final Boolean tradingActionCreated;
    private final Boolean entryStopTpRrGenerated;

    private RuntimeSourceReadResultDTO(
            String symbol,
            WatchlistRuntimeSourceDTO runtimeSource,
            WatchlistRuntimeSourceStatusEnum readStatus,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.runtimeSource = runtimeSource;
        this.readStatus = readStatus == null ? WatchlistRuntimeSourceStatusEnum.INCOMPLETE : readStatus;
        this.missingFields = copy(missingFields);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.opportunityPushAllowed = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static RuntimeSourceReadResultDTO sourceUnavailable(
            String symbol,
            List<String> blockingReasons
    ) {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.sourceUnavailable(symbol, blockingReasons);
        return fromRuntimeSource(source);
    }

    public static RuntimeSourceReadResultDTO incomplete(
            String symbol,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.incomplete(
                symbol,
                missingFields,
                blockingReasons
        );
        return fromRuntimeSource(source);
    }

    public static RuntimeSourceReadResultDTO fromRuntimeSource(WatchlistRuntimeSourceDTO source) {
        if (source == null) {
            WatchlistRuntimeSourceDTO incompleteSource = WatchlistRuntimeSourceDTO.incomplete(
                    null,
                    List.of(FIELD_RUNTIME_SOURCE),
                    List.of(REASON_RUNTIME_SOURCE_MISSING)
            );
            return fromRuntimeSource(incompleteSource);
        }
        return new RuntimeSourceReadResultDTO(
                source.getSymbol(),
                source,
                source.getSourceStatus(),
                source.getMissingFields(),
                source.getBlockingReasons()
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public WatchlistRuntimeSourceDTO getRuntimeSource() {
        return runtimeSource;
    }

    public WatchlistRuntimeSourceStatusEnum getReadStatus() {
        return readStatus;
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

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
