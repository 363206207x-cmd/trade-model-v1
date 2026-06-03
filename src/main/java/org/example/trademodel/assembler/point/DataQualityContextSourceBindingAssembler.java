package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.DataQualityContextSourceBindingDTO;
import org.example.trademodel.validator.point.DataQualityContextSourceBindingValidator;

public class DataQualityContextSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING = "DATA_QUALITY_BINDING_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final DataQualityContextSourceBindingValidator validator;

    public DataQualityContextSourceBindingAssembler() {
        this(new DataQualityContextSourceBindingValidator());
    }

    public DataQualityContextSourceBindingAssembler(DataQualityContextSourceBindingValidator validator) {
        this.validator = validator == null ? new DataQualityContextSourceBindingValidator() : validator;
    }

    public AssembledDataQualityContextSourceBinding assemble(AssemblyInput input) {
        DataQualityContextSourceBindingDTO context = contextFrom(input);
        DataQualityContextSourceBindingValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledDataQualityContextSourceBinding(context, validationResult);
    }

    private DataQualityContextSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return DataQualityContextSourceBindingDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    REASON_INPUT_MISSING
            );
        }

        DataQualityContextSourceBindingDTO.BindingStatus requestedStatus = input.getRequestedStatus();
        if (DataQualityContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return DataQualityContextSourceBindingDTO.blockedFailClosed(
                    input.getDataQualityContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getBlockedReasons(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return DataQualityContextSourceBindingDTO.degraded(
                    input.getDataQualityContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityScore(),
                    input.getDataQualityGrade(),
                    input.getHardThresholdPassed(),
                    input.getWarningThresholdPassed(),
                    input.getSourceTraceCompletenessScore(),
                    input.getRuntimeKlineCompletenessScore(),
                    input.getOhlcvCompletenessScore(),
                    input.getFreshnessScore(),
                    input.getMultiTimeframeConsistencyScore(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getTrustedSource()
            );
        }

        if (DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING
                .equals(requestedStatus)) {
            return DataQualityContextSourceBindingDTO.reviewOnly(
                    input.getDataQualityContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityScore(),
                    input.getDataQualityGrade(),
                    input.getHardThresholdPassed(),
                    input.getWarningThresholdPassed(),
                    input.getSourceTraceCompletenessScore(),
                    input.getRuntimeKlineCompletenessScore(),
                    input.getOhlcvCompletenessScore(),
                    input.getFreshnessScore(),
                    input.getMultiTimeframeConsistencyScore(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getTrustedSource()
            );
        }

        String fallback = DataQualityContextSourceBindingDTO.BindingStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return DataQualityContextSourceBindingDTO.incomplete(
                input.getDataQualityContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getTimeframe(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getMissingFields(),
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
        private final String dataQualityContextId;
        private final String symbol;
        private final String market;
        private final String timeframe;
        private final List<String> sourceTraceRefs;
        private final String runtimeKlineContextRef;
        private final BigDecimal dataQualityScore;
        private final DataQualityContextSourceBindingDTO.DataQualityGrade dataQualityGrade;
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
        private final Boolean trustedSource;
        private final DataQualityContextSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
                String dataQualityContextId,
                String symbol,
                String market,
                String timeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                BigDecimal dataQualityScore,
                DataQualityContextSourceBindingDTO.DataQualityGrade dataQualityGrade,
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
                DataQualityContextSourceBindingDTO.BindingStatus requestedStatus
        ) {
            this.dataQualityContextId = dataQualityContextId;
            this.symbol = symbol;
            this.market = market;
            this.timeframe = timeframe;
            this.sourceTraceRefs = copy(sourceTraceRefs);
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
            this.missingFields = copy(missingFields);
            this.degradedReasons = copy(degradedReasons);
            this.blockedReasons = copy(blockedReasons);
            this.observedAt = observedAt;
            this.createdAt = createdAt;
            this.missingReason = missingReason;
            this.blockedReason = blockedReason;
            this.trustedSource = trustedSource;
            this.requestedStatus = requestedStatus;
        }

        public static AssemblyInput of(
                String dataQualityContextId,
                String symbol,
                String market,
                String timeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                BigDecimal dataQualityScore,
                DataQualityContextSourceBindingDTO.DataQualityGrade dataQualityGrade,
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
                DataQualityContextSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
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
                    missingReason,
                    blockedReason,
                    trustedSource,
                    requestedStatus
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
            return copy(sourceTraceRefs);
        }

        public String getRuntimeKlineContextRef() {
            return runtimeKlineContextRef;
        }

        public BigDecimal getDataQualityScore() {
            return dataQualityScore;
        }

        public DataQualityContextSourceBindingDTO.DataQualityGrade getDataQualityGrade() {
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
            return copy(missingFields);
        }

        public List<String> getDegradedReasons() {
            return copy(degradedReasons);
        }

        public List<String> getBlockedReasons() {
            return copy(blockedReasons);
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

        public Boolean getTrustedSource() {
            return trustedSource;
        }

        public DataQualityContextSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static List<String> copy(List<String> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledDataQualityContextSourceBinding {
        private final DataQualityContextSourceBindingDTO context;
        private final DataQualityContextSourceBindingValidator.ValidationResult validationResult;

        private AssembledDataQualityContextSourceBinding(
                DataQualityContextSourceBindingDTO context,
                DataQualityContextSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public DataQualityContextSourceBindingDTO getContext() {
            return context;
        }

        public DataQualityContextSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
