package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class SourceTraceEntryPositiveCompletionFixtureInput {

    private final String symbol;
    private final String timeframe;
    private final String sourceTraceEntryOwnershipCompletionPath;
    private final BigDecimal entryPriceSource;
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
    private final List<String> missingFields;
    private final List<String> sourceTags;
    private final String fixtureOnlyEvidenceShape;
    private final List<String> fixtureOnlyEvidenceRefs;

    private SourceTraceEntryPositiveCompletionFixtureInput(Builder builder) {
        this.symbol = builder.symbol;
        this.timeframe = builder.timeframe;
        this.sourceTraceEntryOwnershipCompletionPath = builder.sourceTraceEntryOwnershipCompletionPath;
        this.entryPriceSource = builder.entryPriceSource;
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
        this.missingFields = new ArrayList<>(builder.missingFields);
        this.sourceTags = new ArrayList<>(builder.sourceTags);
        this.fixtureOnlyEvidenceShape = builder.fixtureOnlyEvidenceShape;
        this.fixtureOnlyEvidenceRefs = new ArrayList<>(builder.fixtureOnlyEvidenceRefs);
    }

    static SourceTraceEntryPositiveCompletionFixtureInput syntheticFixture() {
        return builder().build();
    }

    static SourceTraceEntryPositiveCompletionFixtureInput withSourceTags(List<String> sourceTags) {
        return builder().sourceTags(sourceTags).build();
    }

    static SourceTraceEntryPositiveCompletionFixtureInput withFixtureOnlyEvidence(
            String fixtureOnlyEvidenceShape,
            List<String> fixtureOnlyEvidenceRefs
    ) {
        return builder()
                .fixtureOnlyEvidenceShape(fixtureOnlyEvidenceShape)
                .fixtureOnlyEvidenceRefs(fixtureOnlyEvidenceRefs)
                .build();
    }

    static Builder builder() {
        return new Builder();
    }

    boolean hasRuntimeLikeSourceTags() {
        return !sourceTags.isEmpty();
    }

    String getSymbol() {
        return symbol;
    }

    String getTimeframe() {
        return timeframe;
    }

    String getSourceTraceEntryOwnershipCompletionPath() {
        return sourceTraceEntryOwnershipCompletionPath;
    }

    BigDecimal getEntryPriceSource() {
        return entryPriceSource;
    }

    String getEntrySourceType() {
        return entrySourceType;
    }

    String getEntrySourceTimeframe() {
        return entrySourceTimeframe;
    }

    String getEntrySourceReason() {
        return entrySourceReason;
    }

    String getEntrySourceRef() {
        return entrySourceRef;
    }

    String getRuleId() {
        return ruleId;
    }

    String getRuleVersion() {
        return ruleVersion;
    }

    String getSourceWindow() {
        return sourceWindow;
    }

    String getFreshnessStatus() {
        return freshnessStatus;
    }

    Long getObservedAtMs() {
        return observedAtMs;
    }

    Long getDecisionCreateTimeMs() {
        return decisionCreateTimeMs;
    }

    Boolean getConflictsWithStop() {
        return conflictsWithStop;
    }

    Boolean getConflictsWithTakeProfit() {
        return conflictsWithTakeProfit;
    }

    Boolean getConflictsWithRiskReward() {
        return conflictsWithRiskReward;
    }

    Boolean getConflictsWithLiquidity() {
        return conflictsWithLiquidity;
    }

    Boolean getConflictsWithMultiTimeframe() {
        return conflictsWithMultiTimeframe;
    }

    Boolean getConflictsWithEvent() {
        return conflictsWithEvent;
    }

    Boolean getConflictsWithWick() {
        return conflictsWithWick;
    }

    List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    List<String> getSourceTags() {
        return new ArrayList<>(sourceTags);
    }

    String getFixtureOnlyEvidenceShape() {
        return fixtureOnlyEvidenceShape;
    }

    List<String> getFixtureOnlyEvidenceRefs() {
        return new ArrayList<>(fixtureOnlyEvidenceRefs);
    }

    static final class Builder {

        private String symbol = "BTCUSDT";
        private String timeframe = "15m";
        private String sourceTraceEntryOwnershipCompletionPath = "fixture-only-completion-path";
        private BigDecimal entryPriceSource = new BigDecimal("1.00");
        private String entrySourceType = "rule-owned-boundary";
        private String entrySourceTimeframe = "15m";
        private String entrySourceReason = "fixture-only-source-reason";
        private String entrySourceRef = "fixture-source-ref";
        private String ruleId = "fixture-entry-rule";
        private String ruleVersion = "fixture-v1";
        private String sourceWindow = "fixture-window";
        private String freshnessStatus = "FRESH";
        private Long observedAtMs = 100L;
        private Long decisionCreateTimeMs = 200L;
        private Boolean conflictsWithStop = Boolean.FALSE;
        private Boolean conflictsWithTakeProfit = Boolean.FALSE;
        private Boolean conflictsWithRiskReward = Boolean.FALSE;
        private Boolean conflictsWithLiquidity = Boolean.FALSE;
        private Boolean conflictsWithMultiTimeframe = Boolean.FALSE;
        private Boolean conflictsWithEvent = Boolean.FALSE;
        private Boolean conflictsWithWick = Boolean.FALSE;
        private List<String> missingFields = List.of("fixture-only-not-runtime-ready");
        private List<String> sourceTags = List.of();
        private String fixtureOnlyEvidenceShape;
        private List<String> fixtureOnlyEvidenceRefs = List.of();

        Builder missingFields(List<String> missingFields) {
            this.missingFields = missingFields == null ? List.of() : new ArrayList<>(missingFields);
            return this;
        }

        Builder sourceTags(List<String> sourceTags) {
            this.sourceTags = sourceTags == null ? List.of() : new ArrayList<>(sourceTags);
            return this;
        }

        Builder fixtureOnlyEvidenceShape(String fixtureOnlyEvidenceShape) {
            this.fixtureOnlyEvidenceShape = fixtureOnlyEvidenceShape;
            return this;
        }

        Builder fixtureOnlyEvidenceRefs(List<String> fixtureOnlyEvidenceRefs) {
            this.fixtureOnlyEvidenceRefs = fixtureOnlyEvidenceRefs == null
                    ? List.of()
                    : new ArrayList<>(fixtureOnlyEvidenceRefs);
            return this;
        }

        SourceTraceEntryPositiveCompletionFixtureInput build() {
            return new SourceTraceEntryPositiveCompletionFixtureInput(this);
        }
    }
}
