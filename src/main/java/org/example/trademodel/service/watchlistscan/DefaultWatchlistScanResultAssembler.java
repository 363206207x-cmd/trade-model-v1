package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;

public class DefaultWatchlistScanResultAssembler implements WatchlistScanResultAssembler {

    private static final String REASON_ASSEMBLY_INPUT_MISSING = "ASSEMBLY_INPUT_MISSING";
    private static final String REASON_MISSING_RUNTIME_SOURCE = "MISSING_RUNTIME_SOURCE";
    private static final String REASON_SOURCE_UNAVAILABLE = "SOURCE_UNAVAILABLE";
    private static final String REASON_STALE_REVIEW_ONLY = "STALE_REVIEW_ONLY";
    private static final String REASON_AVAILABLE_REVIEW_ONLY = "AVAILABLE_REVIEW_ONLY";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_GUARD_BLOCKED = "GUARD_BLOCKED";
    private static final String REASON_ASSEMBLY_FAILED = "ASSEMBLY_FAILED";
    private static final String REASON_INCOMPLETE = "INCOMPLETE";

    private final WatchlistScanGuardValidator guardValidator;

    public DefaultWatchlistScanResultAssembler(WatchlistScanGuardValidator guardValidator) {
        this.guardValidator = guardValidator;
    }

    @Override
    public WatchlistScanResultDTO assemble(RuntimeSourceReadResultDTO runtimeSourceReadResult) {
        String symbol = null;
        try {
            if (runtimeSourceReadResult == null) {
                return WatchlistScanResultDTO.incomplete(null, List.of(REASON_ASSEMBLY_INPUT_MISSING));
            }

            symbol = runtimeSourceReadResult.getSymbol();
            WatchlistRuntimeSourceDTO runtimeSource = runtimeSourceReadResult.getRuntimeSource();
            WatchlistRuntimeSourceStatusEnum readStatus = runtimeSourceReadResult.getReadStatus();
            List<String> reasons = runtimeSourceReadResult.getBlockingReasons();

            if (runtimeSource == null) {
                return resultWithoutRuntimeSource(symbol, readStatus, reasons);
            }

            symbol = resolveSymbol(symbol, runtimeSource);
            readStatus = resolveStatus(readStatus, runtimeSource.getSourceStatus());
            reasons = merge(reasons, runtimeSource.getBlockingReasons());

            if (isBlockedNotWatchlist(runtimeSource, readStatus, reasons)) {
                return WatchlistScanResultDTO.blockedNotWatchlist(
                        symbol,
                        withReason(reasons, REASON_BLOCKED_NOT_WATCHLIST)
                );
            }
            if (WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE.equals(readStatus)) {
                return WatchlistScanResultDTO.incomplete(
                        symbol,
                        withReason(reasons, REASON_SOURCE_UNAVAILABLE)
                );
            }
            if (WatchlistRuntimeSourceStatusEnum.INCOMPLETE.equals(readStatus)
                    || WatchlistRuntimeSourceStatusEnum.NOT_IMPLEMENTED.equals(readStatus)) {
                return WatchlistScanResultDTO.incomplete(
                        symbol,
                        withReason(merge(reasons, runtimeSource.getMissingFields()), REASON_INCOMPLETE)
                );
            }
            if (WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY.equals(readStatus)) {
                return reviewOnlyAfterGuard(symbol, reasons, REASON_STALE_REVIEW_ONLY);
            }
            if (WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY.equals(readStatus)) {
                return reviewOnlyAfterGuard(symbol, reasons, REASON_AVAILABLE_REVIEW_ONLY);
            }

            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    withReason(reasons, REASON_MISSING_RUNTIME_SOURCE)
            );
        } catch (RuntimeException ex) {
            return WatchlistScanResultDTO.incomplete(symbol, List.of(REASON_ASSEMBLY_FAILED));
        }
    }

    private WatchlistScanResultDTO resultWithoutRuntimeSource(
            String symbol,
            WatchlistRuntimeSourceStatusEnum readStatus,
            List<String> reasons
    ) {
        if (WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE.equals(readStatus)) {
            return WatchlistScanResultDTO.incomplete(symbol, withReason(reasons, REASON_SOURCE_UNAVAILABLE));
        }
        return WatchlistScanResultDTO.incomplete(symbol, withReason(reasons, REASON_MISSING_RUNTIME_SOURCE));
    }

    private WatchlistScanResultDTO reviewOnlyAfterGuard(
            String symbol,
            List<String> reasons,
            String reviewReason
    ) {
        if (guardValidator == null) {
            return WatchlistScanResultDTO.incomplete(symbol, withReason(reasons, REASON_GUARD_BLOCKED));
        }

        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                symbol,
                true,
                withReason(reasons, reviewReason)
        );
        WatchlistScanResultDTO guardResult = guardValidator.validate(snapshot);
        if (guardResult == null || !WatchlistScanStatusEnum.REVIEW_ONLY.equals(guardResult.getScanStatus())) {
            return failClosedFromGuard(symbol, reasons, guardResult);
        }

        return WatchlistScanResultDTO.reviewOnly(
                symbol,
                withReason(merge(reasons, guardResult.getBlockingReasons()), reviewReason)
        );
    }

    private WatchlistScanResultDTO failClosedFromGuard(
            String symbol,
            List<String> reasons,
            WatchlistScanResultDTO guardResult
    ) {
        List<String> mergedReasons = withReason(
                merge(reasons, guardResult == null ? List.of() : guardResult.getBlockingReasons()),
                REASON_GUARD_BLOCKED
        );
        if (guardResult != null
                && WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST.equals(guardResult.getScanStatus())) {
            return WatchlistScanResultDTO.blockedNotWatchlist(symbol, mergedReasons);
        }
        return WatchlistScanResultDTO.incomplete(symbol, mergedReasons);
    }

    private boolean isBlockedNotWatchlist(
            WatchlistRuntimeSourceDTO runtimeSource,
            WatchlistRuntimeSourceStatusEnum readStatus,
            List<String> reasons
    ) {
        return Boolean.FALSE.equals(runtimeSource.getWatchlistMember())
                || WatchlistRuntimeSourceStatusEnum.BLOCKED_NOT_WATCHLIST.equals(readStatus)
                || reasons.contains(REASON_BLOCKED_NOT_WATCHLIST);
    }

    private WatchlistRuntimeSourceStatusEnum resolveStatus(
            WatchlistRuntimeSourceStatusEnum readStatus,
            WatchlistRuntimeSourceStatusEnum sourceStatus
    ) {
        return readStatus == null ? sourceStatus : readStatus;
    }

    private String resolveSymbol(String resultSymbol, WatchlistRuntimeSourceDTO runtimeSource) {
        return runtimeSource.getSymbol() == null ? resultSymbol : runtimeSource.getSymbol();
    }

    private List<String> withReason(List<String> reasons, String defaultReason) {
        List<String> resolvedReasons = copy(reasons);
        if (!resolvedReasons.contains(defaultReason)) {
            resolvedReasons.add(defaultReason);
        }
        return resolvedReasons;
    }

    private List<String> merge(List<String> first, List<String> second) {
        List<String> merged = copy(first);
        for (String value : copy(second)) {
            if (!merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged;
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
