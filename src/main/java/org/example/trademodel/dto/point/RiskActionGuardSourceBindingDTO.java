package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiskActionGuardSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_RISK_ACTION_GUARD_BINDING,
        REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL,
        UNKNOWN
    }

    public enum LiquidityState {
        NORMAL,
        DEGRADED,
        SEVERELY_DEGRADED,
        UNKNOWN
    }

    private final String riskActionGuardContextId;
    private final String symbol;
    private final String market;
    private final String primaryTimeframe;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final LiquidityState liquidityState;
    private final Boolean liquidityDegraded;
    private final Boolean stampedeDetected;
    private final Boolean wickOnlyDetected;
    private final Boolean multiTimeframeConfirmed;
    private final Boolean strongReversalClaimed;
    private final RiskLevel riskLevel;
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
    private final boolean trustedSource;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;
    private final BindingStatus bindingStatus;

    private RiskActionGuardSourceBindingDTO(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            LiquidityState liquidityState,
            Boolean liquidityDegraded,
            Boolean stampedeDetected,
            Boolean wickOnlyDetected,
            Boolean multiTimeframeConfirmed,
            Boolean strongReversalClaimed,
            RiskLevel riskLevel,
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
            BindingStatus bindingStatus
    ) {
        this.riskActionGuardContextId = riskActionGuardContextId;
        this.symbol = symbol;
        this.market = market;
        this.primaryTimeframe = primaryTimeframe;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
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
        this.allowedReviewOnlyActionLabels = immutableCopy(allowedReviewOnlyActionLabels);
        this.blockedActionLabels = immutableCopy(blockedActionLabels);
        this.guardDecisionLabel = guardDecisionLabel;
        this.guardReason = guardReason;
        this.riskActionCategory = riskActionCategory;
        this.riskActionBoundaryRef = riskActionBoundaryRef;
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

    public static RiskActionGuardSourceBindingDTO incomplete(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            List<String> missingFields,
            String missingReason
    ) {
        return new RiskActionGuardSourceBindingDTO(
                riskActionGuardContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
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

    public static RiskActionGuardSourceBindingDTO blockedFailClosed(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            List<String> blockedActionLabels,
            List<String> blockedReasons,
            String blockedReason
    ) {
        return new RiskActionGuardSourceBindingDTO(
                riskActionGuardContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                blockedActionLabels,
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

    public static RiskActionGuardSourceBindingDTO degraded(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            LiquidityState liquidityState,
            Boolean liquidityDegraded,
            Boolean stampedeDetected,
            Boolean wickOnlyDetected,
            Boolean multiTimeframeConfirmed,
            Boolean strongReversalClaimed,
            RiskLevel riskLevel,
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
            String observedAt,
            String createdAt,
            String missingReason,
            Boolean trustedSource
    ) {
        return new RiskActionGuardSourceBindingDTO(
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
                List.of(),
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
        );
    }

    public static RiskActionGuardSourceBindingDTO reviewOnly(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            LiquidityState liquidityState,
            Boolean liquidityDegraded,
            Boolean stampedeDetected,
            Boolean wickOnlyDetected,
            Boolean multiTimeframeConfirmed,
            Boolean strongReversalClaimed,
            RiskLevel riskLevel,
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
            Boolean trustedSource
    ) {
        return new RiskActionGuardSourceBindingDTO(
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
                null,
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
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
        return sourceTraceRefs;
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

    public LiquidityState getLiquidityState() {
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

    public RiskLevel getRiskLevel() {
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
        return allowedReviewOnlyActionLabels;
    }

    public List<String> getBlockedActionLabels() {
        return blockedActionLabels;
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

    private static <T> List<T> immutableCopy(List<T> values) {
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
