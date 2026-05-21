package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class MarketReadOnlyEvidenceSnapshotDTO {

    private final String symbol;
    private final String timeframe;
    private final List<String> evidenceRefs;
    private final List<MarketReadOnlyEvidenceFamilyEnum> evidenceFamilies;
    private final String sourceOwner;
    private final String sourceRef;
    private final String sourceTimeframe;
    private final String sourceReason;
    private final String sourceWindow;
    private final String ruleId;
    private final String ruleVersion;
    private final MarketReadOnlyEvidenceStatusEnum freshnessStatus;
    private final MarketReadOnlyEvidenceStatusEnum conflictFamilyStatus;
    private final Integer dataQualityScore;
    private final MarketReadOnlyEvidenceStatusEnum eventEvidenceStatus;
    private final MarketReadOnlyEvidenceStatusEnum liquidityEvidenceStatus;
    private final MarketReadOnlyEvidenceStatusEnum wickPinBarEvidenceStatus;
    private final MarketReadOnlyEvidenceStatusEnum multiTimeframeEvidenceStatus;
    private final String riskActionGuardContext;
    private final List<String> forbiddenInputMarkers;
    private final List<String> noGoEvidenceMarkers;
    private final List<String> riskActionGuardBlockers;
    private final List<String> missingFields;
    private final List<String> blockerEvidence;
    private final MarketReadOnlySnapshotStatusEnum snapshotStatus;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    private MarketReadOnlyEvidenceSnapshotDTO(Builder builder) {
        this.symbol = builder.symbol;
        this.timeframe = builder.timeframe;
        this.evidenceRefs = copy(builder.evidenceRefs);
        this.evidenceFamilies = copy(builder.evidenceFamilies);
        this.sourceOwner = builder.sourceOwner;
        this.sourceRef = builder.sourceRef;
        this.sourceTimeframe = builder.sourceTimeframe;
        this.sourceReason = builder.sourceReason;
        this.sourceWindow = builder.sourceWindow;
        this.ruleId = builder.ruleId;
        this.ruleVersion = builder.ruleVersion;
        this.freshnessStatus = builder.freshnessStatus;
        this.conflictFamilyStatus = builder.conflictFamilyStatus;
        this.dataQualityScore = builder.dataQualityScore;
        this.eventEvidenceStatus = builder.eventEvidenceStatus;
        this.liquidityEvidenceStatus = builder.liquidityEvidenceStatus;
        this.wickPinBarEvidenceStatus = builder.wickPinBarEvidenceStatus;
        this.multiTimeframeEvidenceStatus = builder.multiTimeframeEvidenceStatus;
        this.riskActionGuardContext = builder.riskActionGuardContext;
        this.forbiddenInputMarkers = copy(builder.forbiddenInputMarkers);
        this.noGoEvidenceMarkers = copy(builder.noGoEvidenceMarkers);
        this.riskActionGuardBlockers = copy(builder.riskActionGuardBlockers);
        this.missingFields = resolveMissingFields();
        this.blockerEvidence = resolveBlockerEvidence();
        this.snapshotStatus = resolveSnapshotStatus();
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

    public List<String> getEvidenceRefs() {
        return copy(evidenceRefs);
    }

    public List<MarketReadOnlyEvidenceFamilyEnum> getEvidenceFamilies() {
        return copy(evidenceFamilies);
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public String getSourceReason() {
        return sourceReason;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public MarketReadOnlyEvidenceStatusEnum getFreshnessStatus() {
        return freshnessStatus;
    }

    public MarketReadOnlyEvidenceStatusEnum getConflictFamilyStatus() {
        return conflictFamilyStatus;
    }

    public Integer getDataQualityScore() {
        return dataQualityScore;
    }

    public MarketReadOnlyEvidenceStatusEnum getEventEvidenceStatus() {
        return eventEvidenceStatus;
    }

    public MarketReadOnlyEvidenceStatusEnum getLiquidityEvidenceStatus() {
        return liquidityEvidenceStatus;
    }

    public MarketReadOnlyEvidenceStatusEnum getWickPinBarEvidenceStatus() {
        return wickPinBarEvidenceStatus;
    }

    public MarketReadOnlyEvidenceStatusEnum getMultiTimeframeEvidenceStatus() {
        return multiTimeframeEvidenceStatus;
    }

    public String getRiskActionGuardContext() {
        return riskActionGuardContext;
    }

    public List<String> getForbiddenInputMarkers() {
        return copy(forbiddenInputMarkers);
    }

    public List<String> getNoGoEvidenceMarkers() {
        return copy(noGoEvidenceMarkers);
    }

    public List<String> getRiskActionGuardBlockers() {
        return copy(riskActionGuardBlockers);
    }

    public List<String> getMissingFields() {
        return copy(missingFields);
    }

    public List<String> getBlockerEvidence() {
        return copy(blockerEvidence);
    }

    public MarketReadOnlySnapshotStatusEnum getSnapshotStatus() {
        return snapshotStatus;
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

    private List<String> resolveMissingFields() {
        List<String> missing = new ArrayList<>();
        addMissingText(missing, symbol, "symbol");
        addMissingText(missing, timeframe, "timeframe");
        addMissingList(missing, evidenceRefs, "evidenceRefs");
        addMissingList(missing, evidenceFamilies, "evidenceFamilies");
        addMissingText(missing, sourceOwner, "sourceOwner");
        addMissingText(missing, sourceRef, "sourceRef");
        addMissingText(missing, sourceTimeframe, "sourceTimeframe");
        addMissingText(missing, sourceReason, "sourceReason");
        addMissingText(missing, sourceWindow, "sourceWindow");
        addMissingText(missing, ruleId, "ruleId");
        addMissingText(missing, ruleVersion, "ruleVersion");
        addMissingStatus(missing, freshnessStatus, "freshnessStatus");
        addMissingStatus(missing, conflictFamilyStatus, "conflictFamilyStatus");
        if (dataQualityScore == null) {
            missing.add("dataQualityScore");
        }
        addMissingStatus(missing, eventEvidenceStatus, "eventEvidenceStatus");
        addMissingStatus(missing, liquidityEvidenceStatus, "liquidityEvidenceStatus");
        addMissingStatus(missing, wickPinBarEvidenceStatus, "wickPinBarEvidenceStatus");
        addMissingStatus(missing, multiTimeframeEvidenceStatus, "multiTimeframeEvidenceStatus");
        addMissingText(missing, riskActionGuardContext, "riskActionGuardContext");
        return missing;
    }

    private List<String> resolveBlockerEvidence() {
        List<String> blockers = new ArrayList<>();
        blockers.addAll(forbiddenInputMarkers);
        blockers.addAll(noGoEvidenceMarkers);
        blockers.addAll(riskActionGuardBlockers);
        addBlockingStatus(blockers, freshnessStatus, "freshnessStatus");
        addBlockingStatus(blockers, conflictFamilyStatus, "conflictFamilyStatus");
        addBlockingStatus(blockers, eventEvidenceStatus, "eventEvidenceStatus");
        addBlockingStatus(blockers, liquidityEvidenceStatus, "liquidityEvidenceStatus");
        addBlockingStatus(blockers, wickPinBarEvidenceStatus, "wickPinBarEvidenceStatus");
        addBlockingStatus(blockers, multiTimeframeEvidenceStatus, "multiTimeframeEvidenceStatus");
        if (freshnessStatus == MarketReadOnlyEvidenceStatusEnum.STALE) {
            blockers.add("stale_source_window");
        }
        return blockers;
    }

    private MarketReadOnlySnapshotStatusEnum resolveSnapshotStatus() {
        if (hasUnsafeBlocker()) {
            return MarketReadOnlySnapshotStatusEnum.BLOCKED;
        }
        if (!missingFields.isEmpty() || !blockerEvidence.isEmpty()) {
            return MarketReadOnlySnapshotStatusEnum.INCOMPLETE;
        }
        return MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW;
    }

    private boolean hasUnsafeBlocker() {
        return !forbiddenInputMarkers.isEmpty()
                || !noGoEvidenceMarkers.isEmpty()
                || !riskActionGuardBlockers.isEmpty()
                || isBlockingStatus(freshnessStatus)
                || isBlockingStatus(conflictFamilyStatus)
                || isBlockingStatus(eventEvidenceStatus)
                || isBlockingStatus(liquidityEvidenceStatus)
                || isBlockingStatus(wickPinBarEvidenceStatus)
                || isBlockingStatus(multiTimeframeEvidenceStatus);
    }

    private void addMissingText(List<String> missing, String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            missing.add(fieldName);
        }
    }

    private void addMissingList(List<String> missing, List<?> value, String fieldName) {
        if (value == null || value.isEmpty()) {
            missing.add(fieldName);
        }
    }

    private void addMissingStatus(List<String> missing, MarketReadOnlyEvidenceStatusEnum value, String fieldName) {
        if (value == null || value == MarketReadOnlyEvidenceStatusEnum.MISSING) {
            missing.add(fieldName);
        }
    }

    private void addBlockingStatus(
            List<String> blockers,
            MarketReadOnlyEvidenceStatusEnum value,
            String fieldName
    ) {
        if (isBlockingStatus(value)) {
            blockers.add(fieldName + ":" + value.name());
        }
    }

    private boolean isBlockingStatus(MarketReadOnlyEvidenceStatusEnum value) {
        return value == MarketReadOnlyEvidenceStatusEnum.CONFLICT
                || value == MarketReadOnlyEvidenceStatusEnum.BLOCKED
                || value == MarketReadOnlyEvidenceStatusEnum.NO_GO
                || value == MarketReadOnlyEvidenceStatusEnum.FORBIDDEN_INPUT
                || value == MarketReadOnlyEvidenceStatusEnum.RISK_ACTION_GUARD_BLOCKER;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static class Builder {

        private String symbol;
        private String timeframe;
        private List<String> evidenceRefs = new ArrayList<>();
        private List<MarketReadOnlyEvidenceFamilyEnum> evidenceFamilies = new ArrayList<>();
        private String sourceOwner;
        private String sourceRef;
        private String sourceTimeframe;
        private String sourceReason;
        private String sourceWindow;
        private String ruleId;
        private String ruleVersion;
        private MarketReadOnlyEvidenceStatusEnum freshnessStatus;
        private MarketReadOnlyEvidenceStatusEnum conflictFamilyStatus;
        private Integer dataQualityScore;
        private MarketReadOnlyEvidenceStatusEnum eventEvidenceStatus;
        private MarketReadOnlyEvidenceStatusEnum liquidityEvidenceStatus;
        private MarketReadOnlyEvidenceStatusEnum wickPinBarEvidenceStatus;
        private MarketReadOnlyEvidenceStatusEnum multiTimeframeEvidenceStatus;
        private String riskActionGuardContext;
        private List<String> forbiddenInputMarkers = new ArrayList<>();
        private List<String> noGoEvidenceMarkers = new ArrayList<>();
        private List<String> riskActionGuardBlockers = new ArrayList<>();

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }

        public Builder evidenceRefs(List<String> evidenceRefs) {
            this.evidenceRefs = copy(evidenceRefs);
            return this;
        }

        public Builder evidenceFamilies(List<MarketReadOnlyEvidenceFamilyEnum> evidenceFamilies) {
            this.evidenceFamilies = copy(evidenceFamilies);
            return this;
        }

        public Builder sourceOwner(String sourceOwner) {
            this.sourceOwner = sourceOwner;
            return this;
        }

        public Builder sourceRef(String sourceRef) {
            this.sourceRef = sourceRef;
            return this;
        }

        public Builder sourceTimeframe(String sourceTimeframe) {
            this.sourceTimeframe = sourceTimeframe;
            return this;
        }

        public Builder sourceReason(String sourceReason) {
            this.sourceReason = sourceReason;
            return this;
        }

        public Builder sourceWindow(String sourceWindow) {
            this.sourceWindow = sourceWindow;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
            return this;
        }

        public Builder freshnessStatus(MarketReadOnlyEvidenceStatusEnum freshnessStatus) {
            this.freshnessStatus = freshnessStatus;
            return this;
        }

        public Builder conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum conflictFamilyStatus) {
            this.conflictFamilyStatus = conflictFamilyStatus;
            return this;
        }

        public Builder dataQualityScore(Integer dataQualityScore) {
            this.dataQualityScore = dataQualityScore;
            return this;
        }

        public Builder eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum eventEvidenceStatus) {
            this.eventEvidenceStatus = eventEvidenceStatus;
            return this;
        }

        public Builder liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum liquidityEvidenceStatus) {
            this.liquidityEvidenceStatus = liquidityEvidenceStatus;
            return this;
        }

        public Builder wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum wickPinBarEvidenceStatus) {
            this.wickPinBarEvidenceStatus = wickPinBarEvidenceStatus;
            return this;
        }

        public Builder multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum multiTimeframeEvidenceStatus) {
            this.multiTimeframeEvidenceStatus = multiTimeframeEvidenceStatus;
            return this;
        }

        public Builder riskActionGuardContext(String riskActionGuardContext) {
            this.riskActionGuardContext = riskActionGuardContext;
            return this;
        }

        public Builder forbiddenInputMarkers(List<String> forbiddenInputMarkers) {
            this.forbiddenInputMarkers = copy(forbiddenInputMarkers);
            return this;
        }

        public Builder noGoEvidenceMarkers(List<String> noGoEvidenceMarkers) {
            this.noGoEvidenceMarkers = copy(noGoEvidenceMarkers);
            return this;
        }

        public Builder riskActionGuardBlockers(List<String> riskActionGuardBlockers) {
            this.riskActionGuardBlockers = copy(riskActionGuardBlockers);
            return this;
        }

        public MarketReadOnlyEvidenceSnapshotDTO build() {
            return new MarketReadOnlyEvidenceSnapshotDTO(this);
        }
    }
}
