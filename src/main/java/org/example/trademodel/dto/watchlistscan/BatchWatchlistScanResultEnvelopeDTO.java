package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchWatchlistScanResultEnvelopeDTO {

    private static final String INCOMPLETE_REASON = "INCOMPLETE";

    private final String batchId;
    private final String requestId;
    private final String source;
    private final boolean watchlistPoolOnly;
    private final boolean disabledByDefault;
    private final List<String> requestedSymbols;
    private final List<String> acceptedSymbols;
    private final List<String> rejectedSymbols;
    private final List<String> missingSymbols;
    private final List<String> duplicateSymbols;
    private final List<String> invalidSymbols;
    private final List<String> nonWatchlistSymbols;
    private final List<WatchlistScanResultDTO> results;
    private final List<String> blockingReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean opportunityPushAllowed;
    private final boolean candidateAttentionAllowed;
    private final boolean promoteToHomeAllowed;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private BatchWatchlistScanResultEnvelopeDTO(
            String batchId,
            String requestId,
            String source,
            boolean watchlistPoolOnly,
            boolean disabledByDefault,
            List<String> requestedSymbols,
            List<String> acceptedSymbols,
            List<String> rejectedSymbols,
            List<String> missingSymbols,
            List<String> duplicateSymbols,
            List<String> invalidSymbols,
            List<String> nonWatchlistSymbols,
            List<WatchlistScanResultDTO> results,
            List<String> blockingReasons
    ) {
        this.batchId = batchId;
        this.requestId = requestId;
        this.source = source;
        this.watchlistPoolOnly = watchlistPoolOnly;
        this.disabledByDefault = disabledByDefault;
        this.requestedSymbols = copyStrings(requestedSymbols);
        this.acceptedSymbols = copyStrings(acceptedSymbols);
        this.rejectedSymbols = copyStrings(rejectedSymbols);
        this.missingSymbols = copyStrings(missingSymbols);
        this.duplicateSymbols = copyStrings(duplicateSymbols);
        this.invalidSymbols = copyStrings(invalidSymbols);
        this.nonWatchlistSymbols = copyStrings(nonWatchlistSymbols);
        this.results = copyResults(results);
        this.blockingReasons = copyStrings(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.opportunityPushAllowed = false;
        this.candidateAttentionAllowed = false;
        this.promoteToHomeAllowed = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static BatchWatchlistScanResultEnvelopeDTO incomplete(
            String batchId,
            String requestId,
            String source,
            List<String> requestedSymbols,
            List<String> blockingReasons
    ) {
        return new BatchWatchlistScanResultEnvelopeDTO(
                batchId,
                requestId,
                source,
                true,
                true,
                requestedSymbols,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                withReason(blockingReasons, INCOMPLETE_REASON)
        );
    }

    public static BatchWatchlistScanResultEnvelopeDTO reviewOnly(
            String batchId,
            String requestId,
            String source,
            List<String> requestedSymbols,
            List<String> acceptedSymbols,
            List<String> rejectedSymbols,
            List<WatchlistScanResultDTO> results,
            List<String> blockingReasons
    ) {
        return new BatchWatchlistScanResultEnvelopeDTO(
                batchId,
                requestId,
                source,
                true,
                true,
                requestedSymbols,
                acceptedSymbols,
                rejectedSymbols,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                results,
                blockingReasons
        );
    }

    public static BatchWatchlistScanResultEnvelopeDTO of(
            String batchId,
            String requestId,
            String source,
            List<String> requestedSymbols,
            List<String> acceptedSymbols,
            List<String> rejectedSymbols,
            List<String> missingSymbols,
            List<String> duplicateSymbols,
            List<String> invalidSymbols,
            List<String> nonWatchlistSymbols,
            List<WatchlistScanResultDTO> results,
            List<String> blockingReasons
    ) {
        return new BatchWatchlistScanResultEnvelopeDTO(
                batchId,
                requestId,
                source,
                true,
                true,
                requestedSymbols,
                acceptedSymbols,
                rejectedSymbols,
                missingSymbols,
                duplicateSymbols,
                invalidSymbols,
                nonWatchlistSymbols,
                results,
                blockingReasons
        );
    }

    public String getBatchId() {
        return batchId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSource() {
        return source;
    }

    public boolean isWatchlistPoolOnly() {
        return watchlistPoolOnly;
    }

    public boolean isDisabledByDefault() {
        return disabledByDefault;
    }

    public List<String> getRequestedSymbols() {
        return copyStrings(requestedSymbols);
    }

    public List<String> getAcceptedSymbols() {
        return copyStrings(acceptedSymbols);
    }

    public List<String> getRejectedSymbols() {
        return copyStrings(rejectedSymbols);
    }

    public List<String> getMissingSymbols() {
        return copyStrings(missingSymbols);
    }

    public List<String> getDuplicateSymbols() {
        return copyStrings(duplicateSymbols);
    }

    public List<String> getInvalidSymbols() {
        return copyStrings(invalidSymbols);
    }

    public List<String> getNonWatchlistSymbols() {
        return copyStrings(nonWatchlistSymbols);
    }

    public List<WatchlistScanResultDTO> getResults() {
        return copyResults(results);
    }

    public List<String> getBlockingReasons() {
        return copyStrings(blockingReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isOpportunityPushAllowed() {
        return opportunityPushAllowed;
    }

    public boolean isCandidateAttentionAllowed() {
        return candidateAttentionAllowed;
    }

    public boolean isPromoteToHomeAllowed() {
        return promoteToHomeAllowed;
    }

    public boolean isReadinessUpgraded() {
        return readinessUpgraded;
    }

    public boolean isTradingActionCreated() {
        return tradingActionCreated;
    }

    public boolean isEntryStopTpRrGenerated() {
        return entryStopTpRrGenerated;
    }

    private static List<String> copyStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<WatchlistScanResultDTO> copyResults(List<WatchlistScanResultDTO> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> merged = new ArrayList<>();
        if (reasons != null) {
            merged.addAll(reasons);
        }
        if (reason != null && !merged.contains(reason)) {
            merged.add(reason);
        }
        return merged;
    }
}
