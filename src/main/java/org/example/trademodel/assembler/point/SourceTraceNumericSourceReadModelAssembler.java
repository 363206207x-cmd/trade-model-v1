package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import org.example.trademodel.dto.point.SourceTraceNumericSourceContextDTO;
import org.example.trademodel.validator.point.SourceTraceNumericSourceReadModelValidator;

public class SourceTraceNumericSourceReadModelAssembler {

    private static final String REASON_INPUT_MISSING = "SOURCE_TRACE_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final SourceTraceNumericSourceReadModelValidator validator;

    public SourceTraceNumericSourceReadModelAssembler() {
        this(new SourceTraceNumericSourceReadModelValidator());
    }

    public SourceTraceNumericSourceReadModelAssembler(SourceTraceNumericSourceReadModelValidator validator) {
        this.validator = validator == null ? new SourceTraceNumericSourceReadModelValidator() : validator;
    }

    public AssembledSourceTraceNumericSource assemble(AssemblyInput input) {
        SourceTraceNumericSourceContextDTO context = contextFrom(input);
        SourceTraceNumericSourceReadModelValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledSourceTraceNumericSource(context, validationResult);
    }

    private SourceTraceNumericSourceContextDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return SourceTraceNumericSourceContextDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    REASON_INPUT_MISSING
            );
        }

        SourceTraceNumericSourceContextDTO.SourceTraceStatus requestedStatus = input.getRequestedStatus();
        if (SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return SourceTraceNumericSourceContextDTO.blockedFailClosed(
                    input.getSourceTraceId(),
                    input.getSourceOwner(),
                    input.getSourceType(),
                    input.getSourceContractId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getNumericFieldName(),
                    input.getNumericFieldRole(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED
                .equals(requestedStatus)) {
            return SourceTraceNumericSourceContextDTO.degraded(
                    input.getSourceTraceId(),
                    input.getSourceOwner(),
                    input.getSourceType(),
                    input.getSourceContractId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getNumericFieldName(),
                    input.getNumericFieldRole(),
                    input.getNumericValue(),
                    input.getNumericValueLow(),
                    input.getNumericValueHigh(),
                    input.getSourceUnit(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getFreshnessStatus(),
                    input.getSourceConfidence(),
                    input.getSourceRef(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardRef(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED)
            );
        }

        if (SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE.equals(requestedStatus)) {
            return SourceTraceNumericSourceContextDTO.reviewOnly(
                    input.getSourceTraceId(),
                    input.getSourceOwner(),
                    input.getSourceType(),
                    input.getSourceContractId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getNumericFieldName(),
                    input.getNumericFieldRole(),
                    input.getNumericValue(),
                    input.getNumericValueLow(),
                    input.getNumericValueHigh(),
                    input.getSourceUnit(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getFreshnessStatus(),
                    input.getSourceConfidence(),
                    input.getSourceRef(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardRef()
            );
        }

        String fallback = SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return SourceTraceNumericSourceContextDTO.incomplete(
                input.getSourceTraceId(),
                input.getSourceOwner(),
                input.getSourceType(),
                input.getSourceContractId(),
                input.getSymbol(),
                input.getMarket(),
                input.getTimeframe(),
                input.getNumericFieldName(),
                input.getNumericFieldRole(),
                requiredOrFallback(input.getMissingReason(), fallback)
        );
    }

    private static String requiredOrFallback(String reason, String fallbackReason) {
        return isBlank(reason) ? fallbackReason : reason;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AssemblyInput {
        private final String sourceTraceId;
        private final String sourceOwner;
        private final SourceTraceNumericSourceContextDTO.SourceType sourceType;
        private final String sourceContractId;
        private final String symbol;
        private final String market;
        private final String timeframe;
        private final String numericFieldName;
        private final SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole;
        private final BigDecimal numericValue;
        private final BigDecimal numericValueLow;
        private final BigDecimal numericValueHigh;
        private final String sourceUnit;
        private final String observedAt;
        private final String createdAt;
        private final SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus;
        private final BigDecimal sourceConfidence;
        private final String sourceRef;
        private final String runtimeKlineContextRef;
        private final String dataQualityContextRef;
        private final String multiTimeframeContextRef;
        private final String riskActionGuardRef;
        private final String missingReason;
        private final String blockedReason;
        private final SourceTraceNumericSourceContextDTO.SourceTraceStatus requestedStatus;

        private AssemblyInput(
                String sourceTraceId,
                String sourceOwner,
                SourceTraceNumericSourceContextDTO.SourceType sourceType,
                String sourceContractId,
                String symbol,
                String market,
                String timeframe,
                String numericFieldName,
                SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
                BigDecimal numericValue,
                BigDecimal numericValueLow,
                BigDecimal numericValueHigh,
                String sourceUnit,
                String observedAt,
                String createdAt,
                SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
                BigDecimal sourceConfidence,
                String sourceRef,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String missingReason,
                String blockedReason,
                SourceTraceNumericSourceContextDTO.SourceTraceStatus requestedStatus
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
            this.requestedStatus = requestedStatus;
        }

        public static AssemblyInput of(
                String sourceTraceId,
                String sourceOwner,
                SourceTraceNumericSourceContextDTO.SourceType sourceType,
                String sourceContractId,
                String symbol,
                String market,
                String timeframe,
                String numericFieldName,
                SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
                BigDecimal numericValue,
                BigDecimal numericValueLow,
                BigDecimal numericValueHigh,
                String sourceUnit,
                String observedAt,
                String createdAt,
                SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
                BigDecimal sourceConfidence,
                String sourceRef,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String missingReason,
                String blockedReason,
                SourceTraceNumericSourceContextDTO.SourceTraceStatus requestedStatus
        ) {
            return new AssemblyInput(
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
                    missingReason,
                    blockedReason,
                    requestedStatus
            );
        }

        public String getSourceTraceId() {
            return sourceTraceId;
        }

        public String getSourceOwner() {
            return sourceOwner;
        }

        public SourceTraceNumericSourceContextDTO.SourceType getSourceType() {
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

        public SourceTraceNumericSourceContextDTO.NumericFieldRole getNumericFieldRole() {
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

        public SourceTraceNumericSourceContextDTO.FreshnessStatus getFreshnessStatus() {
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

        public SourceTraceNumericSourceContextDTO.SourceTraceStatus getRequestedStatus() {
            return requestedStatus;
        }
    }

    public static class AssembledSourceTraceNumericSource {
        private final SourceTraceNumericSourceContextDTO context;
        private final SourceTraceNumericSourceReadModelValidator.ValidationResult validationResult;

        private AssembledSourceTraceNumericSource(
                SourceTraceNumericSourceContextDTO context,
                SourceTraceNumericSourceReadModelValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public SourceTraceNumericSourceContextDTO getContext() {
            return context;
        }

        public SourceTraceNumericSourceReadModelValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
