package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class MarketReadOnlyCandidateResultDTO {

    private final String symbol;
    private final String timeframe;
    private final MarketReadOnlySnapshotStatusEnum snapshotStatus;
    private final MarketReadOnlyCandidateStatusEnum candidateStatus;
    private final String entryReview;
    private final String stopReview;
    private final String tpReview;
    private final String rrReview;
    private final String sourceOwnershipSummary;
    private final String numericSourceSummary;
    private final MarketReadOnlyEvidenceStatusEnum freshnessStatus;
    private final String sourceWindow;
    private final String ruleVersion;
    private final MarketReadOnlyEvidenceStatusEnum conflictFamilyStatus;
    private final Integer dataQualityScore;
    private final String riskActionGuardReview;
    private final List<String> blockingReasons;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    private MarketReadOnlyCandidateResultDTO(Builder builder) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = builder.snapshot;
        this.symbol = snapshot == null ? null : snapshot.getSymbol();
        this.timeframe = snapshot == null ? null : snapshot.getTimeframe();
        this.snapshotStatus = snapshot == null ? null : snapshot.getSnapshotStatus();
        this.entryReview = builder.entryReview;
        this.stopReview = builder.stopReview;
        this.tpReview = builder.tpReview;
        this.rrReview = builder.rrReview;
        this.sourceOwnershipSummary = builder.sourceOwnershipSummary;
        this.numericSourceSummary = builder.numericSourceSummary;
        this.freshnessStatus = snapshot == null ? null : snapshot.getFreshnessStatus();
        this.sourceWindow = snapshot == null ? null : snapshot.getSourceWindow();
        this.ruleVersion = snapshot == null ? null : snapshot.getRuleVersion();
        this.conflictFamilyStatus = snapshot == null ? null : snapshot.getConflictFamilyStatus();
        this.dataQualityScore = snapshot == null ? null : snapshot.getDataQualityScore();
        this.riskActionGuardReview = builder.riskActionGuardReview;
        this.blockingReasons = resolveBlockingReasons(snapshot, builder.blockingReasons);
        this.candidateStatus = resolveCandidateStatus(snapshot, builder.blockingReasons);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public MarketReadOnlySnapshotStatusEnum getSnapshotStatus() {
        return snapshotStatus;
    }

    public MarketReadOnlyCandidateStatusEnum getCandidateStatus() {
        return candidateStatus;
    }

    public String getEntryReview() {
        return entryReview;
    }

    public String getStopReview() {
        return stopReview;
    }

    public String getTpReview() {
        return tpReview;
    }

    public String getRrReview() {
        return rrReview;
    }

    public String getSourceOwnershipSummary() {
        return sourceOwnershipSummary;
    }

    public String getNumericSourceSummary() {
        return numericSourceSummary;
    }

    public MarketReadOnlyEvidenceStatusEnum getFreshnessStatus() {
        return freshnessStatus;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public MarketReadOnlyEvidenceStatusEnum getConflictFamilyStatus() {
        return conflictFamilyStatus;
    }

    public Integer getDataQualityScore() {
        return dataQualityScore;
    }

    public String getRiskActionGuardReview() {
        return riskActionGuardReview;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    private MarketReadOnlyCandidateStatusEnum resolveCandidateStatus(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            List<String> directBlockingReasons
    ) {
        if (snapshot == null) {
            return MarketReadOnlyCandidateStatusEnum.INCOMPLETE;
        }
        if (directBlockingReasons != null && !directBlockingReasons.isEmpty()) {
            return MarketReadOnlyCandidateStatusEnum.BLOCKED;
        }
        if (snapshot.getSnapshotStatus() == MarketReadOnlySnapshotStatusEnum.BLOCKED) {
            return MarketReadOnlyCandidateStatusEnum.BLOCKED;
        }
        if (snapshot.getSnapshotStatus() == MarketReadOnlySnapshotStatusEnum.INCOMPLETE) {
            return MarketReadOnlyCandidateStatusEnum.INCOMPLETE;
        }
        return MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE;
    }

    private List<String> resolveBlockingReasons(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            List<String> directBlockingReasons
    ) {
        List<String> reasons = new ArrayList<>();
        if (snapshot == null) {
            reasons.add("missing_snapshot");
            return reasons;
        }
        reasons.addAll(copy(directBlockingReasons));
        for (String missingField : snapshot.getMissingFields()) {
            reasons.add("snapshot_missing:" + missingField);
        }
        for (String blocker : snapshot.getBlockerEvidence()) {
            String prefix = snapshot.getSnapshotStatus() == MarketReadOnlySnapshotStatusEnum.BLOCKED
                    ? "snapshot_blocked:"
                    : "snapshot_incomplete:";
            reasons.add(prefix + blocker);
        }
        return reasons;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static class Builder {

        private MarketReadOnlyEvidenceSnapshotDTO snapshot;
        private String entryReview;
        private String stopReview;
        private String tpReview;
        private String rrReview;
        private String sourceOwnershipSummary;
        private String numericSourceSummary;
        private String riskActionGuardReview;
        private List<String> blockingReasons = new ArrayList<>();

        public Builder snapshot(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
            this.snapshot = snapshot;
            return this;
        }

        public Builder entryReview(String entryReview) {
            this.entryReview = entryReview;
            return this;
        }

        public Builder stopReview(String stopReview) {
            this.stopReview = stopReview;
            return this;
        }

        public Builder tpReview(String tpReview) {
            this.tpReview = tpReview;
            return this;
        }

        public Builder rrReview(String rrReview) {
            this.rrReview = rrReview;
            return this;
        }

        public Builder sourceOwnershipSummary(String sourceOwnershipSummary) {
            this.sourceOwnershipSummary = sourceOwnershipSummary;
            return this;
        }

        public Builder numericSourceSummary(String numericSourceSummary) {
            this.numericSourceSummary = numericSourceSummary;
            return this;
        }

        public Builder riskActionGuardReview(String riskActionGuardReview) {
            this.riskActionGuardReview = riskActionGuardReview;
            return this;
        }

        public Builder blockingReasons(List<String> blockingReasons) {
            this.blockingReasons = copy(blockingReasons);
            return this;
        }

        public MarketReadOnlyCandidateResultDTO build() {
            return new MarketReadOnlyCandidateResultDTO(this);
        }
    }
}
