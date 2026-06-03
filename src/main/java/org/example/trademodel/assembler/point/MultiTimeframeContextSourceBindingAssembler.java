package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.MultiTimeframeContextSourceBindingDTO;
import org.example.trademodel.validator.point.MultiTimeframeContextSourceBindingValidator;

public class MultiTimeframeContextSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING = "MULTITIMEFRAME_BINDING_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final MultiTimeframeContextSourceBindingValidator validator;

    public MultiTimeframeContextSourceBindingAssembler() {
        this(new MultiTimeframeContextSourceBindingValidator());
    }

    public MultiTimeframeContextSourceBindingAssembler(MultiTimeframeContextSourceBindingValidator validator) {
        this.validator = validator == null ? new MultiTimeframeContextSourceBindingValidator() : validator;
    }

    public AssembledMultiTimeframeContextSourceBinding assemble(AssemblyInput input) {
        MultiTimeframeContextSourceBindingDTO context = contextFrom(input);
        MultiTimeframeContextSourceBindingValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledMultiTimeframeContextSourceBinding(context, validationResult);
    }

    private MultiTimeframeContextSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return MultiTimeframeContextSourceBindingDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    REASON_INPUT_MISSING
            );
        }

        MultiTimeframeContextSourceBindingDTO.BindingStatus requestedStatus = input.getRequestedStatus();
        if (MultiTimeframeContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return MultiTimeframeContextSourceBindingDTO.blockedFailClosed(
                    input.getMultiTimeframeContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getTimeframeRefs(),
                    input.getBlockedReasons(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return MultiTimeframeContextSourceBindingDTO.degraded(
                    input.getMultiTimeframeContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getTimeframeRefs(),
                    input.getTimeframeScores(),
                    input.getTimeframeDirections(),
                    input.getTimeframeWeights(),
                    input.getAlignedTimeframes(),
                    input.getConflictedTimeframes(),
                    input.getMissingTimeframes(),
                    input.getStaleTimeframes(),
                    input.getDominantDirection(),
                    input.getAlignmentScore(),
                    input.getConflictScore(),
                    input.getWeightedAgreementScore(),
                    input.getMinimumRequiredTimeframesPassed(),
                    input.getDataQualityPassed(),
                    input.getHardThresholdPassed(),
                    input.getWarningThresholdPassed(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getTrustedSource()
            );
        }

        if (MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING
                .equals(requestedStatus)) {
            return MultiTimeframeContextSourceBindingDTO.reviewOnly(
                    input.getMultiTimeframeContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getTimeframeRefs(),
                    input.getTimeframeScores(),
                    input.getTimeframeDirections(),
                    input.getTimeframeWeights(),
                    input.getAlignedTimeframes(),
                    input.getConflictedTimeframes(),
                    input.getMissingTimeframes(),
                    input.getStaleTimeframes(),
                    input.getDominantDirection(),
                    input.getAlignmentScore(),
                    input.getConflictScore(),
                    input.getWeightedAgreementScore(),
                    input.getMinimumRequiredTimeframesPassed(),
                    input.getDataQualityPassed(),
                    input.getHardThresholdPassed(),
                    input.getWarningThresholdPassed(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getTrustedSource()
            );
        }

        String fallback = MultiTimeframeContextSourceBindingDTO.BindingStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return MultiTimeframeContextSourceBindingDTO.incomplete(
                input.getMultiTimeframeContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getPrimaryTimeframe(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getDataQualityContextRef(),
                input.getTimeframeRefs(),
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
        private final String multiTimeframeContextId;
        private final String symbol;
        private final String market;
        private final String primaryTimeframe;
        private final List<String> sourceTraceRefs;
        private final String runtimeKlineContextRef;
        private final String dataQualityContextRef;
        private final List<String> timeframeRefs;
        private final List<BigDecimal> timeframeScores;
        private final List<String> timeframeDirections;
        private final List<BigDecimal> timeframeWeights;
        private final List<String> alignedTimeframes;
        private final List<String> conflictedTimeframes;
        private final List<String> missingTimeframes;
        private final List<String> staleTimeframes;
        private final String dominantDirection;
        private final BigDecimal alignmentScore;
        private final BigDecimal conflictScore;
        private final BigDecimal weightedAgreementScore;
        private final Boolean minimumRequiredTimeframesPassed;
        private final Boolean dataQualityPassed;
        private final Boolean hardThresholdPassed;
        private final Boolean warningThresholdPassed;
        private final List<String> missingFields;
        private final List<String> degradedReasons;
        private final List<String> blockedReasons;
        private final String observedAt;
        private final String createdAt;
        private final String missingReason;
        private final String blockedReason;
        private final Boolean trustedSource;
        private final MultiTimeframeContextSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
                String multiTimeframeContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                List<String> timeframeRefs,
                List<BigDecimal> timeframeScores,
                List<String> timeframeDirections,
                List<BigDecimal> timeframeWeights,
                List<String> alignedTimeframes,
                List<String> conflictedTimeframes,
                List<String> missingTimeframes,
                List<String> staleTimeframes,
                String dominantDirection,
                BigDecimal alignmentScore,
                BigDecimal conflictScore,
                BigDecimal weightedAgreementScore,
                Boolean minimumRequiredTimeframesPassed,
                Boolean dataQualityPassed,
                Boolean hardThresholdPassed,
                Boolean warningThresholdPassed,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                MultiTimeframeContextSourceBindingDTO.BindingStatus requestedStatus
        ) {
            this.multiTimeframeContextId = multiTimeframeContextId;
            this.symbol = symbol;
            this.market = market;
            this.primaryTimeframe = primaryTimeframe;
            this.sourceTraceRefs = copy(sourceTraceRefs);
            this.runtimeKlineContextRef = runtimeKlineContextRef;
            this.dataQualityContextRef = dataQualityContextRef;
            this.timeframeRefs = copy(timeframeRefs);
            this.timeframeScores = copy(timeframeScores);
            this.timeframeDirections = copy(timeframeDirections);
            this.timeframeWeights = copy(timeframeWeights);
            this.alignedTimeframes = copy(alignedTimeframes);
            this.conflictedTimeframes = copy(conflictedTimeframes);
            this.missingTimeframes = copy(missingTimeframes);
            this.staleTimeframes = copy(staleTimeframes);
            this.dominantDirection = dominantDirection;
            this.alignmentScore = alignmentScore;
            this.conflictScore = conflictScore;
            this.weightedAgreementScore = weightedAgreementScore;
            this.minimumRequiredTimeframesPassed = minimumRequiredTimeframesPassed;
            this.dataQualityPassed = dataQualityPassed;
            this.hardThresholdPassed = hardThresholdPassed;
            this.warningThresholdPassed = warningThresholdPassed;
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
                String multiTimeframeContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                List<String> timeframeRefs,
                List<BigDecimal> timeframeScores,
                List<String> timeframeDirections,
                List<BigDecimal> timeframeWeights,
                List<String> alignedTimeframes,
                List<String> conflictedTimeframes,
                List<String> missingTimeframes,
                List<String> staleTimeframes,
                String dominantDirection,
                BigDecimal alignmentScore,
                BigDecimal conflictScore,
                BigDecimal weightedAgreementScore,
                Boolean minimumRequiredTimeframesPassed,
                Boolean dataQualityPassed,
                Boolean hardThresholdPassed,
                Boolean warningThresholdPassed,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                MultiTimeframeContextSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
                    multiTimeframeContextId,
                    symbol,
                    market,
                    primaryTimeframe,
                    sourceTraceRefs,
                    runtimeKlineContextRef,
                    dataQualityContextRef,
                    timeframeRefs,
                    timeframeScores,
                    timeframeDirections,
                    timeframeWeights,
                    alignedTimeframes,
                    conflictedTimeframes,
                    missingTimeframes,
                    staleTimeframes,
                    dominantDirection,
                    alignmentScore,
                    conflictScore,
                    weightedAgreementScore,
                    minimumRequiredTimeframesPassed,
                    dataQualityPassed,
                    hardThresholdPassed,
                    warningThresholdPassed,
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

        public String getMultiTimeframeContextId() {
            return multiTimeframeContextId;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getMarket() {
            return market;
        }

        public String getPrimaryTimeframe() {
            return primaryTimeframe;
        }

        public List<String> getSourceTraceRefs() {
            return copy(sourceTraceRefs);
        }

        public String getRuntimeKlineContextRef() {
            return runtimeKlineContextRef;
        }

        public String getDataQualityContextRef() {
            return dataQualityContextRef;
        }

        public List<String> getTimeframeRefs() {
            return copy(timeframeRefs);
        }

        public List<BigDecimal> getTimeframeScores() {
            return copy(timeframeScores);
        }

        public List<String> getTimeframeDirections() {
            return copy(timeframeDirections);
        }

        public List<BigDecimal> getTimeframeWeights() {
            return copy(timeframeWeights);
        }

        public List<String> getAlignedTimeframes() {
            return copy(alignedTimeframes);
        }

        public List<String> getConflictedTimeframes() {
            return copy(conflictedTimeframes);
        }

        public List<String> getMissingTimeframes() {
            return copy(missingTimeframes);
        }

        public List<String> getStaleTimeframes() {
            return copy(staleTimeframes);
        }

        public String getDominantDirection() {
            return dominantDirection;
        }

        public BigDecimal getAlignmentScore() {
            return alignmentScore;
        }

        public BigDecimal getConflictScore() {
            return conflictScore;
        }

        public BigDecimal getWeightedAgreementScore() {
            return weightedAgreementScore;
        }

        public Boolean getMinimumRequiredTimeframesPassed() {
            return minimumRequiredTimeframesPassed;
        }

        public Boolean getDataQualityPassed() {
            return dataQualityPassed;
        }

        public Boolean getHardThresholdPassed() {
            return hardThresholdPassed;
        }

        public Boolean getWarningThresholdPassed() {
            return warningThresholdPassed;
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

        public MultiTimeframeContextSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static <T> List<T> copy(List<T> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledMultiTimeframeContextSourceBinding {
        private final MultiTimeframeContextSourceBindingDTO context;
        private final MultiTimeframeContextSourceBindingValidator.ValidationResult validationResult;

        private AssembledMultiTimeframeContextSourceBinding(
                MultiTimeframeContextSourceBindingDTO context,
                MultiTimeframeContextSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public MultiTimeframeContextSourceBindingDTO getContext() {
            return context;
        }

        public MultiTimeframeContextSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
