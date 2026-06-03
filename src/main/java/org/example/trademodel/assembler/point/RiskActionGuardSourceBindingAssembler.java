package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.RiskActionGuardSourceBindingDTO;
import org.example.trademodel.validator.point.RiskActionGuardSourceBindingValidator;

public class RiskActionGuardSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING = "RISK_ACTION_GUARD_BINDING_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final RiskActionGuardSourceBindingValidator validator;

    public RiskActionGuardSourceBindingAssembler() {
        this(new RiskActionGuardSourceBindingValidator());
    }

    public RiskActionGuardSourceBindingAssembler(RiskActionGuardSourceBindingValidator validator) {
        this.validator = validator == null ? new RiskActionGuardSourceBindingValidator() : validator;
    }

    public AssembledRiskActionGuardSourceBinding assemble(AssemblyInput input) {
        RiskActionGuardSourceBindingDTO context = contextFrom(input);
        RiskActionGuardSourceBindingValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledRiskActionGuardSourceBinding(context, validationResult);
    }

    private RiskActionGuardSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return RiskActionGuardSourceBindingDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    List.of(),
                    REASON_INPUT_MISSING
            );
        }

        RiskActionGuardSourceBindingDTO.BindingStatus requestedStatus = input.getRequestedStatus();
        if (RiskActionGuardSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return RiskActionGuardSourceBindingDTO.blockedFailClosed(
                    input.getRiskActionGuardContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getBlockedActionLabels(),
                    input.getBlockedReasons(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return RiskActionGuardSourceBindingDTO.degraded(
                    input.getRiskActionGuardContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getLiquidityState(),
                    input.getLiquidityDegraded(),
                    input.getStampedeDetected(),
                    input.getWickOnlyDetected(),
                    input.getMultiTimeframeConfirmed(),
                    input.getStrongReversalClaimed(),
                    input.getRiskLevel(),
                    input.getRiskScore(),
                    input.getActionRiskScore(),
                    input.getProposedActionLabel(),
                    input.getAllowedReviewOnlyActionLabels(),
                    input.getBlockedActionLabels(),
                    input.getGuardDecisionLabel(),
                    input.getGuardReason(),
                    input.getRiskActionCategory(),
                    input.getRiskActionBoundaryRef(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getTrustedSource()
            );
        }

        if (RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
                .equals(requestedStatus)) {
            return RiskActionGuardSourceBindingDTO.reviewOnly(
                    input.getRiskActionGuardContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getLiquidityState(),
                    input.getLiquidityDegraded(),
                    input.getStampedeDetected(),
                    input.getWickOnlyDetected(),
                    input.getMultiTimeframeConfirmed(),
                    input.getStrongReversalClaimed(),
                    input.getRiskLevel(),
                    input.getRiskScore(),
                    input.getActionRiskScore(),
                    input.getProposedActionLabel(),
                    input.getAllowedReviewOnlyActionLabels(),
                    input.getBlockedActionLabels(),
                    input.getGuardDecisionLabel(),
                    input.getGuardReason(),
                    input.getRiskActionCategory(),
                    input.getRiskActionBoundaryRef(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getTrustedSource()
            );
        }

        String fallback = RiskActionGuardSourceBindingDTO.BindingStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return RiskActionGuardSourceBindingDTO.incomplete(
                input.getRiskActionGuardContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getPrimaryTimeframe(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getDataQualityContextRef(),
                input.getMultiTimeframeContextRef(),
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
        private final String riskActionGuardContextId;
        private final String symbol;
        private final String market;
        private final String primaryTimeframe;
        private final List<String> sourceTraceRefs;
        private final String runtimeKlineContextRef;
        private final String dataQualityContextRef;
        private final String multiTimeframeContextRef;
        private final RiskActionGuardSourceBindingDTO.LiquidityState liquidityState;
        private final Boolean liquidityDegraded;
        private final Boolean stampedeDetected;
        private final Boolean wickOnlyDetected;
        private final Boolean multiTimeframeConfirmed;
        private final Boolean strongReversalClaimed;
        private final RiskActionGuardSourceBindingDTO.RiskLevel riskLevel;
        private final BigDecimal riskScore;
        private final BigDecimal actionRiskScore;
        private final String proposedActionLabel;
        private final List<String> allowedReviewOnlyActionLabels;
        private final List<String> blockedActionLabels;
        private final String guardDecisionLabel;
        private final String guardReason;
        private final String riskActionCategory;
        private final String riskActionBoundaryRef;
        private final List<String> missingFields;
        private final List<String> degradedReasons;
        private final List<String> blockedReasons;
        private final String observedAt;
        private final String createdAt;
        private final String missingReason;
        private final String blockedReason;
        private final Boolean trustedSource;
        private final RiskActionGuardSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
                String riskActionGuardContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                RiskActionGuardSourceBindingDTO.LiquidityState liquidityState,
                Boolean liquidityDegraded,
                Boolean stampedeDetected,
                Boolean wickOnlyDetected,
                Boolean multiTimeframeConfirmed,
                Boolean strongReversalClaimed,
                RiskActionGuardSourceBindingDTO.RiskLevel riskLevel,
                BigDecimal riskScore,
                BigDecimal actionRiskScore,
                String proposedActionLabel,
                List<String> allowedReviewOnlyActionLabels,
                List<String> blockedActionLabels,
                String guardDecisionLabel,
                String guardReason,
                String riskActionCategory,
                String riskActionBoundaryRef,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                RiskActionGuardSourceBindingDTO.BindingStatus requestedStatus
        ) {
            this.riskActionGuardContextId = riskActionGuardContextId;
            this.symbol = symbol;
            this.market = market;
            this.primaryTimeframe = primaryTimeframe;
            this.sourceTraceRefs = copy(sourceTraceRefs);
            this.runtimeKlineContextRef = runtimeKlineContextRef;
            this.dataQualityContextRef = dataQualityContextRef;
            this.multiTimeframeContextRef = multiTimeframeContextRef;
            this.liquidityState = liquidityState;
            this.liquidityDegraded = liquidityDegraded;
            this.stampedeDetected = stampedeDetected;
            this.wickOnlyDetected = wickOnlyDetected;
            this.multiTimeframeConfirmed = multiTimeframeConfirmed;
            this.strongReversalClaimed = strongReversalClaimed;
            this.riskLevel = riskLevel;
            this.riskScore = riskScore;
            this.actionRiskScore = actionRiskScore;
            this.proposedActionLabel = proposedActionLabel;
            this.allowedReviewOnlyActionLabels = copy(allowedReviewOnlyActionLabels);
            this.blockedActionLabels = copy(blockedActionLabels);
            this.guardDecisionLabel = guardDecisionLabel;
            this.guardReason = guardReason;
            this.riskActionCategory = riskActionCategory;
            this.riskActionBoundaryRef = riskActionBoundaryRef;
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
                String riskActionGuardContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                RiskActionGuardSourceBindingDTO.LiquidityState liquidityState,
                Boolean liquidityDegraded,
                Boolean stampedeDetected,
                Boolean wickOnlyDetected,
                Boolean multiTimeframeConfirmed,
                Boolean strongReversalClaimed,
                RiskActionGuardSourceBindingDTO.RiskLevel riskLevel,
                BigDecimal riskScore,
                BigDecimal actionRiskScore,
                String proposedActionLabel,
                List<String> allowedReviewOnlyActionLabels,
                List<String> blockedActionLabels,
                String guardDecisionLabel,
                String guardReason,
                String riskActionCategory,
                String riskActionBoundaryRef,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                RiskActionGuardSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
                    riskActionGuardContextId,
                    symbol,
                    market,
                    primaryTimeframe,
                    sourceTraceRefs,
                    runtimeKlineContextRef,
                    dataQualityContextRef,
                    multiTimeframeContextRef,
                    liquidityState,
                    liquidityDegraded,
                    stampedeDetected,
                    wickOnlyDetected,
                    multiTimeframeConfirmed,
                    strongReversalClaimed,
                    riskLevel,
                    riskScore,
                    actionRiskScore,
                    proposedActionLabel,
                    allowedReviewOnlyActionLabels,
                    blockedActionLabels,
                    guardDecisionLabel,
                    guardReason,
                    riskActionCategory,
                    riskActionBoundaryRef,
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

        public String getRiskActionGuardContextId() {
            return riskActionGuardContextId;
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

        public String getMultiTimeframeContextRef() {
            return multiTimeframeContextRef;
        }

        public RiskActionGuardSourceBindingDTO.LiquidityState getLiquidityState() {
            return liquidityState;
        }

        public Boolean getLiquidityDegraded() {
            return liquidityDegraded;
        }

        public Boolean getStampedeDetected() {
            return stampedeDetected;
        }

        public Boolean getWickOnlyDetected() {
            return wickOnlyDetected;
        }

        public Boolean getMultiTimeframeConfirmed() {
            return multiTimeframeConfirmed;
        }

        public Boolean getStrongReversalClaimed() {
            return strongReversalClaimed;
        }

        public RiskActionGuardSourceBindingDTO.RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public BigDecimal getRiskScore() {
            return riskScore;
        }

        public BigDecimal getActionRiskScore() {
            return actionRiskScore;
        }

        public String getProposedActionLabel() {
            return proposedActionLabel;
        }

        public List<String> getAllowedReviewOnlyActionLabels() {
            return copy(allowedReviewOnlyActionLabels);
        }

        public List<String> getBlockedActionLabels() {
            return copy(blockedActionLabels);
        }

        public String getGuardDecisionLabel() {
            return guardDecisionLabel;
        }

        public String getGuardReason() {
            return guardReason;
        }

        public String getRiskActionCategory() {
            return riskActionCategory;
        }

        public String getRiskActionBoundaryRef() {
            return riskActionBoundaryRef;
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

        public RiskActionGuardSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static <T> List<T> copy(List<T> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledRiskActionGuardSourceBinding {
        private final RiskActionGuardSourceBindingDTO context;
        private final RiskActionGuardSourceBindingValidator.ValidationResult validationResult;

        private AssembledRiskActionGuardSourceBinding(
                RiskActionGuardSourceBindingDTO context,
                RiskActionGuardSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public RiskActionGuardSourceBindingDTO getContext() {
            return context;
        }

        public RiskActionGuardSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
