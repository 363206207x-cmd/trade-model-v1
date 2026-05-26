package org.example.trademodel.dto.watchlistscan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RealScanInputContractDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_MISSING_WATCHLIST_POOL_PROOF = "MISSING_WATCHLIST_POOL_PROOF";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_REVIEW_ONLY = "REAL_SCAN_INPUT_REVIEW_ONLY";

    private final String symbol;
    private final String source;
    private final String requestedScanReason;
    private final Boolean watchlistPoolMember;
    private final String watchlistPoolProof;
    private final String watchlistConfigVersion;
    private final List<String> requestedTimeframes;
    private final Instant scanTimestamp;
    private final boolean marketReadRequired;
    private final boolean dataAvailabilityExpected;
    private final String staleInputBehavior;
    private final String missingInputBehavior;
    private final List<String> riskBlockers;
    private final List<String> reviewOnlySafetyFlags;
    private final RealScanInputContractStatusEnum status;
    private final List<String> blockingReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;

    private RealScanInputContractDTO(
            String symbol,
            String source,
            String requestedScanReason,
            Boolean watchlistPoolMember,
            String watchlistPoolProof,
            String watchlistConfigVersion,
            List<String> requestedTimeframes,
            Instant scanTimestamp,
            boolean marketReadRequired,
            boolean dataAvailabilityExpected,
            String staleInputBehavior,
            String missingInputBehavior,
            List<String> riskBlockers,
            List<String> reviewOnlySafetyFlags,
            RealScanInputContractStatusEnum status,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.source = source;
        this.requestedScanReason = requestedScanReason;
        this.watchlistPoolMember = watchlistPoolMember;
        this.watchlistPoolProof = watchlistPoolProof;
        this.watchlistConfigVersion = watchlistConfigVersion;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.scanTimestamp = scanTimestamp;
        this.marketReadRequired = marketReadRequired;
        this.dataAvailabilityExpected = dataAvailabilityExpected;
        this.staleInputBehavior = staleInputBehavior;
        this.missingInputBehavior = missingInputBehavior;
        this.riskBlockers = copy(riskBlockers);
        this.reviewOnlySafetyFlags = copy(reviewOnlySafetyFlags);
        this.status = status == null ? RealScanInputContractStatusEnum.INCOMPLETE : status;
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
    }

    public static RealScanInputContractDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new RealScanInputContractDTO(
                symbol,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                false,
                false,
                null,
                null,
                List.of(),
                List.of(),
                RealScanInputContractStatusEnum.INCOMPLETE,
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static RealScanInputContractDTO reviewOnly(
            String symbol,
            String source,
            String requestedScanReason,
            Boolean watchlistPoolMember,
            String watchlistPoolProof,
            String watchlistConfigVersion,
            List<String> requestedTimeframes,
            Instant scanTimestamp,
            boolean marketReadRequired,
            boolean dataAvailabilityExpected,
            String staleInputBehavior,
            String missingInputBehavior,
            List<String> riskBlockers,
            List<String> reviewOnlySafetyFlags,
            List<String> blockingReasons
    ) {
        if (!Boolean.TRUE.equals(watchlistPoolMember)) {
            return new RealScanInputContractDTO(
                    symbol,
                    source,
                    requestedScanReason,
                    watchlistPoolMember,
                    watchlistPoolProof,
                    watchlistConfigVersion,
                    requestedTimeframes,
                    scanTimestamp,
                    marketReadRequired,
                    dataAvailabilityExpected,
                    staleInputBehavior,
                    missingInputBehavior,
                    riskBlockers,
                    reviewOnlySafetyFlags,
                    RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST,
                    withReason(blockingReasons, REASON_BLOCKED_NOT_WATCHLIST)
            );
        }
        if (isBlank(watchlistPoolProof)) {
            return new RealScanInputContractDTO(
                    symbol,
                    source,
                    requestedScanReason,
                    true,
                    watchlistPoolProof,
                    watchlistConfigVersion,
                    requestedTimeframes,
                    scanTimestamp,
                    marketReadRequired,
                    dataAvailabilityExpected,
                    staleInputBehavior,
                    missingInputBehavior,
                    riskBlockers,
                    reviewOnlySafetyFlags,
                    RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF,
                    withReason(blockingReasons, REASON_MISSING_WATCHLIST_POOL_PROOF)
            );
        }
        return new RealScanInputContractDTO(
                symbol,
                source,
                requestedScanReason,
                true,
                watchlistPoolProof,
                watchlistConfigVersion,
                requestedTimeframes,
                scanTimestamp,
                marketReadRequired,
                dataAvailabilityExpected,
                staleInputBehavior,
                missingInputBehavior,
                riskBlockers,
                reviewOnlySafetyFlags,
                RealScanInputContractStatusEnum.REVIEW_ONLY,
                withReason(blockingReasons, REASON_REVIEW_ONLY)
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSource() {
        return source;
    }

    public String getRequestedScanReason() {
        return requestedScanReason;
    }

    public Boolean getWatchlistPoolMember() {
        return watchlistPoolMember;
    }

    public String getWatchlistPoolProof() {
        return watchlistPoolProof;
    }

    public String getWatchlistConfigVersion() {
        return watchlistConfigVersion;
    }

    public List<String> getRequestedTimeframes() {
        return copy(requestedTimeframes);
    }

    public Instant getScanTimestamp() {
        return scanTimestamp;
    }

    public boolean isMarketReadRequired() {
        return marketReadRequired;
    }

    public boolean isDataAvailabilityExpected() {
        return dataAvailabilityExpected;
    }

    public String getStaleInputBehavior() {
        return staleInputBehavior;
    }

    public String getMissingInputBehavior() {
        return missingInputBehavior;
    }

    public List<String> getRiskBlockers() {
        return copy(riskBlockers);
    }

    public List<String> getReviewOnlySafetyFlags() {
        return copy(reviewOnlySafetyFlags);
    }

    public RealScanInputContractStatusEnum getStatus() {
        return status;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
