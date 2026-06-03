package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataQualityContextSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_DATA_QUALITY_BINDING,
        REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
    }

    public enum DataQualityGrade {
        HIGH,
        MEDIUM,
        LOW,
        UNKNOWN
    }

    private final String dataQualityContextId;
    private final String symbol;
    private final String market;
    private final String timeframe;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final BigDecimal dataQualityScore;
    private final DataQualityGrade dataQualityGrade;
    private final Boolean hardThresholdPassed;
    private final Boolean warningThresholdPassed;
    private final BigDecimal sourceTraceCompletenessScore;
    private final BigDecimal runtimeKlineCompletenessScore;
    private final BigDecimal ohlcvCompletenessScore;
    private final BigDecimal freshnessScore;
    private final BigDecimal multiTimeframeConsistencyScore;
    private final List<String> missingFields;
    private final List<String> degradedReasons;
    private final List<String> blockedReasons;
    private final String observedAt;
    private final String createdAt;
    private final String missingReason;
    private final String blockedReason;
    private final boolean trustedSource;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;
    private final BindingStatus bindingStatus;

    private DataQualityContextSourceBindingDTO(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            BigDecimal dataQualityScore,
            DataQualityGrade dataQualityGrade,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            BigDecimal sourceTraceCompletenessScore,
            BigDecimal runtimeKlineCompletenessScore,
            BigDecimal ohlcvCompletenessScore,
            BigDecimal freshnessScore,
            BigDecimal multiTimeframeConsistencyScore,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            BindingStatus bindingStatus
    ) {
        this.dataQualityContextId = dataQualityContextId;
        this.symbol = symbol;
        this.market = market;
        this.timeframe = timeframe;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityScore = dataQualityScore;
        this.dataQualityGrade = dataQualityGrade;
        this.hardThresholdPassed = hardThresholdPassed;
        this.warningThresholdPassed = warningThresholdPassed;
        this.sourceTraceCompletenessScore = sourceTraceCompletenessScore;
        this.runtimeKlineCompletenessScore = runtimeKlineCompletenessScore;
        this.ohlcvCompletenessScore = ohlcvCompletenessScore;
        this.freshnessScore = freshnessScore;
        this.multiTimeframeConsistencyScore = multiTimeframeConsistencyScore;
        this.missingFields = immutableCopy(missingFields);
        this.degradedReasons = immutableCopy(degradedReasons);
        this.blockedReasons = immutableCopy(blockedReasons);
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.missingReason = missingReason;
        this.blockedReason = blockedReason;
        this.trustedSource = bindingStatus != BindingStatus.BLOCKED_FAIL_CLOSED && Boolean.TRUE.equals(trustedSource);
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.incompleteSafe = true;
        this.failClosed = bindingStatus == BindingStatus.BLOCKED_FAIL_CLOSED;
        this.bindingStatus = bindingStatus;
    }

    public static DataQualityContextSourceBindingDTO incomplete(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            List<String> missingFields,
            String missingReason
    ) {
        return new DataQualityContextSourceBindingDTO(
                dataQualityContextId,
                symbol,
                market,
                timeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                null,
                DataQualityGrade.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                missingFields,
                List.of(),
                List.of(),
                null,
                null,
                requiredReason(missingReason, "missingReason"),
                null,
                Boolean.TRUE,
                BindingStatus.INCOMPLETE
        );
    }

    public static DataQualityContextSourceBindingDTO blockedFailClosed(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            List<String> blockedReasons,
            String blockedReason
    ) {
        return new DataQualityContextSourceBindingDTO(
                dataQualityContextId,
                symbol,
                market,
                timeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                null,
                DataQualityGrade.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                blockedReasons,
                null,
                null,
                null,
                requiredReason(blockedReason, "blockedReason"),
                Boolean.FALSE,
                BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    public static DataQualityContextSourceBindingDTO degraded(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            BigDecimal dataQualityScore,
            DataQualityGrade dataQualityGrade,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            BigDecimal sourceTraceCompletenessScore,
            BigDecimal runtimeKlineCompletenessScore,
            BigDecimal ohlcvCompletenessScore,
            BigDecimal freshnessScore,
            BigDecimal multiTimeframeConsistencyScore,
            List<String> missingFields,
            List<String> degradedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            Boolean trustedSource
    ) {
        return new DataQualityContextSourceBindingDTO(
                dataQualityContextId,
                symbol,
                market,
                timeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityScore,
                dataQualityGrade,
                hardThresholdPassed,
                warningThresholdPassed,
                sourceTraceCompletenessScore,
                runtimeKlineCompletenessScore,
                ohlcvCompletenessScore,
                freshnessScore,
                multiTimeframeConsistencyScore,
                missingFields,
                degradedReasons,
                List.of(),
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
        );
    }

    public static DataQualityContextSourceBindingDTO reviewOnly(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            BigDecimal dataQualityScore,
            DataQualityGrade dataQualityGrade,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            BigDecimal sourceTraceCompletenessScore,
            BigDecimal runtimeKlineCompletenessScore,
            BigDecimal ohlcvCompletenessScore,
            BigDecimal freshnessScore,
            BigDecimal multiTimeframeConsistencyScore,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            Boolean trustedSource
    ) {
        return new DataQualityContextSourceBindingDTO(
                dataQualityContextId,
                symbol,
                market,
                timeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityScore,
                dataQualityGrade,
                hardThresholdPassed,
                warningThresholdPassed,
                sourceTraceCompletenessScore,
                runtimeKlineCompletenessScore,
                ohlcvCompletenessScore,
                freshnessScore,
                multiTimeframeConsistencyScore,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                null,
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING
        );
    }

    public String getDataQualityContextId() {
        return dataQualityContextId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMarket() {
        return market;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public List<String> getSourceTraceRefs() {
        return sourceTraceRefs;
    }

    public String getRuntimeKlineContextRef() {
        return runtimeKlineContextRef;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public DataQualityGrade getDataQualityGrade() {
        return dataQualityGrade;
    }

    public Boolean getHardThresholdPassed() {
        return hardThresholdPassed;
    }

    public Boolean getWarningThresholdPassed() {
        return warningThresholdPassed;
    }

    public BigDecimal getSourceTraceCompletenessScore() {
        return sourceTraceCompletenessScore;
    }

    public BigDecimal getRuntimeKlineCompletenessScore() {
        return runtimeKlineCompletenessScore;
    }

    public BigDecimal getOhlcvCompletenessScore() {
        return ohlcvCompletenessScore;
    }

    public BigDecimal getFreshnessScore() {
        return freshnessScore;
    }

    public BigDecimal getMultiTimeframeConsistencyScore() {
        return multiTimeframeConsistencyScore;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public List<String> getDegradedReasons() {
        return degradedReasons;
    }

    public List<String> getBlockedReasons() {
        return blockedReasons;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public boolean isTrustedSource() {
        return trustedSource;
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isIncompleteSafe() {
        return incompleteSafe;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public BindingStatus getBindingStatus() {
        return bindingStatus;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String requiredReason(String reason, String fieldName) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return reason;
    }
}
