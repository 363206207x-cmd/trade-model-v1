package org.example.trademodel.dto.marketread;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MarketReadRequestDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final String watchlistConfigVersion;
    private final String requestedScanReason;
    private final List<String> requestedTimeframes;
    private final Instant scanTimestamp;
    private final String dataAvailabilityExpectation;
    private final String stalePolicy;
    private final String missingDataPolicy;
    private final List<String> riskBlockers;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final String guardValidationStatus;
    private final List<String> blockingReasons;

    public MarketReadRequestDTO() {
        this(
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of()
        );
    }

    private MarketReadRequestDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            String watchlistConfigVersion,
            String requestedScanReason,
            List<String> requestedTimeframes,
            Instant scanTimestamp,
            String dataAvailabilityExpectation,
            String stalePolicy,
            String missingDataPolicy,
            List<String> riskBlockers,
            String guardValidationStatus,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.watchlistConfigVersion = watchlistConfigVersion;
        this.requestedScanReason = requestedScanReason;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.scanTimestamp = scanTimestamp;
        this.dataAvailabilityExpectation = dataAvailabilityExpectation;
        this.stalePolicy = failClosedPolicy(stalePolicy);
        this.missingDataPolicy = failClosedPolicy(missingDataPolicy);
        this.riskBlockers = copy(riskBlockers);
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.guardValidationStatus = guardValidationStatus;
        this.blockingReasons = failClosedReasons(
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                scanTimestamp,
                blockingReasons
        );
    }

    public static MarketReadRequestDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            String watchlistConfigVersion,
            String requestedScanReason,
            List<String> requestedTimeframes,
            Instant scanTimestamp,
            String dataAvailabilityExpectation,
            String stalePolicy,
            String missingDataPolicy,
            List<String> riskBlockers,
            String guardValidationStatus,
            List<String> blockingReasons
    ) {
        return new MarketReadRequestDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                watchlistConfigVersion,
                requestedScanReason,
                requestedTimeframes,
                scanTimestamp,
                dataAvailabilityExpectation,
                stalePolicy,
                missingDataPolicy,
                riskBlockers,
                guardValidationStatus,
                blockingReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSourceContractId() {
        return sourceContractId;
    }

    public String getWatchlistPoolProof() {
        return watchlistPoolProof;
    }

    public String getWatchlistConfigVersion() {
        return watchlistConfigVersion;
    }

    public String getRequestedScanReason() {
        return requestedScanReason;
    }

    public List<String> getRequestedTimeframes() {
        return copy(requestedTimeframes);
    }

    public Instant getScanTimestamp() {
        return scanTimestamp;
    }

    public String getDataAvailabilityExpectation() {
        return dataAvailabilityExpectation;
    }

    public String getStalePolicy() {
        return stalePolicy;
    }

    public String getMissingDataPolicy() {
        return missingDataPolicy;
    }

    public List<String> getRiskBlockers() {
        return copy(riskBlockers);
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isManualReviewRequired() {
        return reviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public String getGuardValidationStatus() {
        return guardValidationStatus;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    private static String failClosedPolicy(String value) {
        return isBlank(value) ? "FAIL_CLOSED" : value;
    }

    private static List<String> failClosedReasons(
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            Instant scanTimestamp,
            List<String> blockingReasons
    ) {
        List<String> resolvedReasons = copy(blockingReasons);
        addIfMissing(resolvedReasons, "MARKET_READ_REQUEST_REVIEW_ONLY");
        if (isBlank(sourceContractId)) {
            addIfMissing(resolvedReasons, "BLOCKED_MISSING_SOURCE_CONTRACT_ID");
        }
        if (isBlank(watchlistPoolProof)) {
            addIfMissing(resolvedReasons, "BLOCKED_MISSING_WATCHLIST_POOL_PROOF");
        }
        if (requestedTimeframes == null || requestedTimeframes.isEmpty()) {
            addIfMissing(resolvedReasons, "BLOCKED_MISSING_REQUESTED_TIMEFRAMES");
        }
        if (scanTimestamp == null) {
            addIfMissing(resolvedReasons, "BLOCKED_MISSING_SCAN_TIMESTAMP");
        }
        return resolvedReasons;
    }

    private static void addIfMissing(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
