package org.example.trademodel.dto.point;

import java.math.BigDecimal;

public class SourceTraceNumericSourceContextDTO {

    public enum SourceType {
        STRUCTURE_LEVEL(true),
        INVALIDATION_LEVEL(true),
        LIQUIDITY_POOL_LEVEL(true),
        PRIOR_HIGH_LOW_LEVEL(true),
        VWAP_OR_VOLUME_LEVEL(true),
        ATR_REFERENCE_LEVEL(true),
        MULTITIMEFRAME_CONFIRMATION_LEVEL(true),
        RISK_ACTION_GUARD_REFERENCE(true),
        MANUAL_REVIEW_SOURCE(true),
        AI_PROSE_ONLY(false),
        DASHBOARD_TEXT_ONLY(false),
        SCORE_LABEL_ONLY(false),
        FINAL_DECISION_TEXT_ONLY(false),
        LATEST_PRICE_ONLY(false),
        LATEST_CLOSE_ONLY(false),
        HARDCODED_DEFAULT(false),
        MANUALLY_INVENTED_FALLBACK(false),
        DISPLAY_SLOT_ONLY(false),
        ORDER_BOOK_DIRECT(false),
        ORDER_EXECUTION_PATH(false),
        EXTERNAL_PROVIDER_DIRECT(false),
        AUTO_TRADING_PATH(false);

        private final boolean sourceOwnedEvidence;

        SourceType(boolean sourceOwnedEvidence) {
            this.sourceOwnedEvidence = sourceOwnedEvidence;
        }

        public boolean isSourceOwnedEvidence() {
            return sourceOwnedEvidence;
        }
    }

    public enum NumericFieldRole {
        ENTRY_PRICE,
        ENTRY_ZONE_LOW,
        ENTRY_ZONE_HIGH,
        STOP_PRICE,
        STOP_ZONE_LOW,
        STOP_ZONE_HIGH,
        TAKE_PROFIT_PRICE,
        TAKE_PROFIT_ZONE_LOW,
        TAKE_PROFIT_ZONE_HIGH,
        RISK_REWARD_VALUE,
        SOURCE_ONLY_REFERENCE
    }

    public enum FreshnessStatus {
        FRESH,
        STALE,
        UNKNOWN
    }

    public enum SourceTraceStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_SOURCE_TRACE,
        REVIEW_ONLY_SOURCE_TRACE_DEGRADED
    }

    private final String sourceTraceId;
    private final String sourceOwner;
    private final SourceType sourceType;
    private final String sourceContractId;
    private final String symbol;
    private final String market;
    private final String timeframe;
    private final String numericFieldName;
    private final NumericFieldRole numericFieldRole;
    private final BigDecimal numericValue;
    private final BigDecimal numericValueLow;
    private final BigDecimal numericValueHigh;
    private final String sourceUnit;
    private final String observedAt;
    private final String createdAt;
    private final FreshnessStatus freshnessStatus;
    private final BigDecimal sourceConfidence;
    private final String sourceRef;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final String riskActionGuardRef;
    private final String missingReason;
    private final String blockedReason;
    private final boolean trustedSource;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;
    private final SourceTraceStatus sourceTraceStatus;

    private SourceTraceNumericSourceContextDTO(
            String sourceTraceId,
            String sourceOwner,
            SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh,
            String sourceUnit,
            String observedAt,
            String createdAt,
            FreshnessStatus freshnessStatus,
            BigDecimal sourceConfidence,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String missingReason,
            String blockedReason,
            SourceTraceStatus sourceTraceStatus
    ) {
        this.sourceTraceId = sourceTraceId;
        this.sourceOwner = sourceOwner;
        this.sourceType = sourceType;
        this.sourceContractId = sourceContractId;
        this.symbol = symbol;
        this.market = market;
        this.timeframe = timeframe;
        this.numericFieldName = numericFieldName;
        this.numericFieldRole = numericFieldRole;
        this.numericValue = numericValue;
        this.numericValueLow = numericValueLow;
        this.numericValueHigh = numericValueHigh;
        this.sourceUnit = sourceUnit;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.freshnessStatus = freshnessStatus;
        this.sourceConfidence = sourceConfidence;
        this.sourceRef = sourceRef;
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityContextRef = dataQualityContextRef;
        this.multiTimeframeContextRef = multiTimeframeContextRef;
        this.riskActionGuardRef = riskActionGuardRef;
        this.missingReason = missingReason;
        this.blockedReason = blockedReason;
        this.sourceTraceStatus = sourceTraceStatus;
        this.trustedSource = sourceTraceStatus != SourceTraceStatus.BLOCKED_FAIL_CLOSED;
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.incompleteSafe = true;
        this.failClosed = sourceTraceStatus == SourceTraceStatus.BLOCKED_FAIL_CLOSED;
    }

    public static SourceTraceNumericSourceContextDTO incomplete(
            String sourceTraceId,
            String sourceOwner,
            SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            NumericFieldRole numericFieldRole,
            String missingReason
    ) {
        return new SourceTraceNumericSourceContextDTO(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                null,
                null,
                null,
                null,
                null,
                null,
                FreshnessStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                requiredReason(missingReason, "missingReason"),
                null,
                SourceTraceStatus.INCOMPLETE
        );
    }

    public static SourceTraceNumericSourceContextDTO blockedFailClosed(
            String sourceTraceId,
            String sourceOwner,
            SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            NumericFieldRole numericFieldRole,
            String blockedReason
    ) {
        return new SourceTraceNumericSourceContextDTO(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                null,
                null,
                null,
                null,
                null,
                null,
                FreshnessStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                requiredReason(blockedReason, "blockedReason"),
                SourceTraceStatus.BLOCKED_FAIL_CLOSED
        );
    }

    public static SourceTraceNumericSourceContextDTO degraded(
            String sourceTraceId,
            String sourceOwner,
            SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh,
            String sourceUnit,
            String observedAt,
            String createdAt,
            FreshnessStatus freshnessStatus,
            BigDecimal sourceConfidence,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String missingReason
    ) {
        return new SourceTraceNumericSourceContextDTO(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                numericValueLow,
                numericValueHigh,
                sourceUnit,
                observedAt,
                createdAt,
                freshnessStatus,
                sourceConfidence,
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                requiredReason(missingReason, "missingReason"),
                null,
                SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED
        );
    }

    public static SourceTraceNumericSourceContextDTO reviewOnly(
            String sourceTraceId,
            String sourceOwner,
            SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh,
            String sourceUnit,
            String observedAt,
            String createdAt,
            FreshnessStatus freshnessStatus,
            BigDecimal sourceConfidence,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef
    ) {
        return new SourceTraceNumericSourceContextDTO(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                numericValueLow,
                numericValueHigh,
                sourceUnit,
                observedAt,
                createdAt,
                freshnessStatus,
                sourceConfidence,
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                null,
                null,
                SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE
        );
    }

    public String getSourceTraceId() {
        return sourceTraceId;
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceContractId() {
        return sourceContractId;
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

    public String getNumericFieldName() {
        return numericFieldName;
    }

    public NumericFieldRole getNumericFieldRole() {
        return numericFieldRole;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public BigDecimal getNumericValueLow() {
        return numericValueLow;
    }

    public BigDecimal getNumericValueHigh() {
        return numericValueHigh;
    }

    public String getSourceUnit() {
        return sourceUnit;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public FreshnessStatus getFreshnessStatus() {
        return freshnessStatus;
    }

    public BigDecimal getSourceConfidence() {
        return sourceConfidence;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getRuntimeKlineContextRef() {
        return runtimeKlineContextRef;
    }

    public String getDataQualityContextRef() {
        return dataQualityContextRef;
    }

    public String getMultiTimeframeContextRef() {
        return multiTimeframeContextRef;
    }

    public String getRiskActionGuardRef() {
        return riskActionGuardRef;
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

    public SourceTraceStatus getSourceTraceStatus() {
        return sourceTraceStatus;
    }

    private static String requiredReason(String reason, String fieldName) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return reason;
    }
}
