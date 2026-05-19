package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit internal read-only input for a future SourceTrace entry completion
 * assembler.
 *
 * <p>This request is a data shape only. It is not a production completion
 * source, readiness gate, persistence model, order path, or automation surface.
 */
public class SourceTraceEntryReadOnlyCompletionRequest {

    private final String symbol;
    private final String timeframe;
    private final String sourceTraceEntryOwnershipCompletionPath;
    private final String entrySourceType;
    private final String entrySourceTimeframe;
    private final String entrySourceReason;
    private final String entrySourceRef;
    private final String ruleId;
    private final String ruleVersion;
    private final String sourceWindow;
    private final String freshnessStatus;
    private final Long observedAtMs;
    private final Long decisionCreateTimeMs;
    private final Boolean conflictsWithStop;
    private final Boolean conflictsWithTakeProfit;
    private final Boolean conflictsWithRiskReward;
    private final Boolean conflictsWithLiquidity;
    private final Boolean conflictsWithMultiTimeframe;
    private final Boolean conflictsWithEvent;
    private final Boolean conflictsWithWick;
    private final List<String> sourceRefs;
    private final List<String> sourceTags;
    private final boolean liquidityStress;
    private final boolean liquidityStampede;
    private final boolean eventDataMissing;
    private final boolean multiTimeframeAgreementOnly;
    private final boolean wickOrPinBarEvidenceOnly;

    private SourceTraceEntryReadOnlyCompletionRequest(Builder builder) {
        this.symbol = builder.symbol;
        this.timeframe = builder.timeframe;
        this.sourceTraceEntryOwnershipCompletionPath = builder.sourceTraceEntryOwnershipCompletionPath;
        this.entrySourceType = builder.entrySourceType;
        this.entrySourceTimeframe = builder.entrySourceTimeframe;
        this.entrySourceReason = builder.entrySourceReason;
        this.entrySourceRef = builder.entrySourceRef;
        this.ruleId = builder.ruleId;
        this.ruleVersion = builder.ruleVersion;
        this.sourceWindow = builder.sourceWindow;
        this.freshnessStatus = builder.freshnessStatus;
        this.observedAtMs = builder.observedAtMs;
        this.decisionCreateTimeMs = builder.decisionCreateTimeMs;
        this.conflictsWithStop = builder.conflictsWithStop;
        this.conflictsWithTakeProfit = builder.conflictsWithTakeProfit;
        this.conflictsWithRiskReward = builder.conflictsWithRiskReward;
        this.conflictsWithLiquidity = builder.conflictsWithLiquidity;
        this.conflictsWithMultiTimeframe = builder.conflictsWithMultiTimeframe;
        this.conflictsWithEvent = builder.conflictsWithEvent;
        this.conflictsWithWick = builder.conflictsWithWick;
        this.sourceRefs = new ArrayList<>(builder.sourceRefs);
        this.sourceTags = new ArrayList<>(builder.sourceTags);
        this.liquidityStress = builder.liquidityStress;
        this.liquidityStampede = builder.liquidityStampede;
        this.eventDataMissing = builder.eventDataMissing;
        this.multiTimeframeAgreementOnly = builder.multiTimeframeAgreementOnly;
        this.wickOrPinBarEvidenceOnly = builder.wickOrPinBarEvidenceOnly;
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

    public String getSourceTraceEntryOwnershipCompletionPath() {
        return sourceTraceEntryOwnershipCompletionPath;
    }

    public String getEntrySourceType() {
        return entrySourceType;
    }

    public String getEntrySourceTimeframe() {
        return entrySourceTimeframe;
    }

    public String getEntrySourceReason() {
        return entrySourceReason;
    }

    public String getEntrySourceRef() {
        return entrySourceRef;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public Long getObservedAtMs() {
        return observedAtMs;
    }

    public Long getDecisionCreateTimeMs() {
        return decisionCreateTimeMs;
    }

    public Boolean getConflictsWithStop() {
        return conflictsWithStop;
    }

    public Boolean getConflictsWithTakeProfit() {
        return conflictsWithTakeProfit;
    }

    public Boolean getConflictsWithRiskReward() {
        return conflictsWithRiskReward;
    }

    public Boolean getConflictsWithLiquidity() {
        return conflictsWithLiquidity;
    }

    public Boolean getConflictsWithMultiTimeframe() {
        return conflictsWithMultiTimeframe;
    }

    public Boolean getConflictsWithEvent() {
        return conflictsWithEvent;
    }

    public Boolean getConflictsWithWick() {
        return conflictsWithWick;
    }

    public List<String> getSourceRefs() {
        return new ArrayList<>(sourceRefs);
    }

    public List<String> getSourceTags() {
        return new ArrayList<>(sourceTags);
    }

    public boolean isLiquidityStress() {
        return liquidityStress;
    }

    public boolean isLiquidityStampede() {
        return liquidityStampede;
    }

    public boolean isEventDataMissing() {
        return eventDataMissing;
    }

    public boolean isMultiTimeframeAgreementOnly() {
        return multiTimeframeAgreementOnly;
    }

    public boolean isWickOrPinBarEvidenceOnly() {
        return wickOrPinBarEvidenceOnly;
    }

    public static final class Builder {

        private String symbol;
        private String timeframe;
        private String sourceTraceEntryOwnershipCompletionPath;
        private String entrySourceType;
        private String entrySourceTimeframe;
        private String entrySourceReason;
        private String entrySourceRef;
        private String ruleId;
        private String ruleVersion;
        private String sourceWindow;
        private String freshnessStatus;
        private Long observedAtMs;
        private Long decisionCreateTimeMs;
        private Boolean conflictsWithStop;
        private Boolean conflictsWithTakeProfit;
        private Boolean conflictsWithRiskReward;
        private Boolean conflictsWithLiquidity;
        private Boolean conflictsWithMultiTimeframe;
        private Boolean conflictsWithEvent;
        private Boolean conflictsWithWick;
        private List<String> sourceRefs = List.of();
        private List<String> sourceTags = List.of();
        private boolean liquidityStress;
        private boolean liquidityStampede;
        private boolean eventDataMissing;
        private boolean multiTimeframeAgreementOnly;
        private boolean wickOrPinBarEvidenceOnly;

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }

        public Builder sourceTraceEntryOwnershipCompletionPath(String sourceTraceEntryOwnershipCompletionPath) {
            this.sourceTraceEntryOwnershipCompletionPath = sourceTraceEntryOwnershipCompletionPath;
            return this;
        }

        public Builder entrySourceType(String entrySourceType) {
            this.entrySourceType = entrySourceType;
            return this;
        }

        public Builder entrySourceTimeframe(String entrySourceTimeframe) {
            this.entrySourceTimeframe = entrySourceTimeframe;
            return this;
        }

        public Builder entrySourceReason(String entrySourceReason) {
            this.entrySourceReason = entrySourceReason;
            return this;
        }

        public Builder entrySourceRef(String entrySourceRef) {
            this.entrySourceRef = entrySourceRef;
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

        public Builder sourceWindow(String sourceWindow) {
            this.sourceWindow = sourceWindow;
            return this;
        }

        public Builder freshnessStatus(String freshnessStatus) {
            this.freshnessStatus = freshnessStatus;
            return this;
        }

        public Builder observedAtMs(Long observedAtMs) {
            this.observedAtMs = observedAtMs;
            return this;
        }

        public Builder decisionCreateTimeMs(Long decisionCreateTimeMs) {
            this.decisionCreateTimeMs = decisionCreateTimeMs;
            return this;
        }

        public Builder conflictsWithStop(Boolean conflictsWithStop) {
            this.conflictsWithStop = conflictsWithStop;
            return this;
        }

        public Builder conflictsWithTakeProfit(Boolean conflictsWithTakeProfit) {
            this.conflictsWithTakeProfit = conflictsWithTakeProfit;
            return this;
        }

        public Builder conflictsWithRiskReward(Boolean conflictsWithRiskReward) {
            this.conflictsWithRiskReward = conflictsWithRiskReward;
            return this;
        }

        public Builder conflictsWithLiquidity(Boolean conflictsWithLiquidity) {
            this.conflictsWithLiquidity = conflictsWithLiquidity;
            return this;
        }

        public Builder conflictsWithMultiTimeframe(Boolean conflictsWithMultiTimeframe) {
            this.conflictsWithMultiTimeframe = conflictsWithMultiTimeframe;
            return this;
        }

        public Builder conflictsWithEvent(Boolean conflictsWithEvent) {
            this.conflictsWithEvent = conflictsWithEvent;
            return this;
        }

        public Builder conflictsWithWick(Boolean conflictsWithWick) {
            this.conflictsWithWick = conflictsWithWick;
            return this;
        }

        public Builder sourceRefs(List<String> sourceRefs) {
            this.sourceRefs = sourceRefs == null ? List.of() : new ArrayList<>(sourceRefs);
            return this;
        }

        public Builder sourceTags(List<String> sourceTags) {
            this.sourceTags = sourceTags == null ? List.of() : new ArrayList<>(sourceTags);
            return this;
        }

        public Builder liquidityStress(boolean liquidityStress) {
            this.liquidityStress = liquidityStress;
            return this;
        }

        public Builder liquidityStampede(boolean liquidityStampede) {
            this.liquidityStampede = liquidityStampede;
            return this;
        }

        public Builder eventDataMissing(boolean eventDataMissing) {
            this.eventDataMissing = eventDataMissing;
            return this;
        }

        public Builder multiTimeframeAgreementOnly(boolean multiTimeframeAgreementOnly) {
            this.multiTimeframeAgreementOnly = multiTimeframeAgreementOnly;
            return this;
        }

        public Builder wickOrPinBarEvidenceOnly(boolean wickOrPinBarEvidenceOnly) {
            this.wickOrPinBarEvidenceOnly = wickOrPinBarEvidenceOnly;
            return this;
        }

        public SourceTraceEntryReadOnlyCompletionRequest build() {
            return new SourceTraceEntryReadOnlyCompletionRequest(this);
        }
    }
}
