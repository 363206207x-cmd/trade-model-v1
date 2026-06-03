package org.example.trademodel.dto.point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WatchlistPoolProofSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING,
        REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED
    }

    private final String watchlistPoolProofContextId;
    private final String symbol;
    private final String market;
    private final String primaryTimeframe;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final String riskActionGuardContextRef;
    private final String watchlistPoolRef;
    private final String watchlistPoolVersion;
    private final Boolean watchlistPoolEnabled;
    private final Boolean watchlistPoolEmpty;
    private final Boolean watchlistPoolMember;
    private final String watchlistMembershipSource;
    private final String watchlistMembershipObservedAt;
    private final String watchlistMembershipExpiresAt;
    private final Boolean proofFresh;
    private final Boolean proofStale;
    private final Boolean displaySlotOnly;
    private final Boolean defaultDisplaySlot;
    private final String displaySlotRef;
    private final Boolean promotedToHomeCandidate;
    private final Boolean lowFrequencyScanCandidate;
    private final String auditRef;
    private final String operatorRef;
    private final String membershipReason;
    private final String proofReason;
    private final String allowedCandidateBoundaryLabel;
    private final String blockedCandidateBoundaryLabel;
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

    private WatchlistPoolProofSourceBindingDTO(
            String watchlistPoolProofContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolRef,
            String watchlistPoolVersion,
            Boolean watchlistPoolEnabled,
            Boolean watchlistPoolEmpty,
            Boolean watchlistPoolMember,
            String watchlistMembershipSource,
            String watchlistMembershipObservedAt,
            String watchlistMembershipExpiresAt,
            Boolean proofFresh,
            Boolean proofStale,
            Boolean displaySlotOnly,
            Boolean defaultDisplaySlot,
            String displaySlotRef,
            Boolean promotedToHomeCandidate,
            Boolean lowFrequencyScanCandidate,
            String auditRef,
            String operatorRef,
            String membershipReason,
            String proofReason,
            String allowedCandidateBoundaryLabel,
            String blockedCandidateBoundaryLabel,
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
        this.watchlistPoolProofContextId = watchlistPoolProofContextId;
        this.symbol = symbol;
        this.market = market;
        this.primaryTimeframe = primaryTimeframe;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityContextRef = dataQualityContextRef;
        this.multiTimeframeContextRef = multiTimeframeContextRef;
        this.riskActionGuardContextRef = riskActionGuardContextRef;
        this.watchlistPoolRef = watchlistPoolRef;
        this.watchlistPoolVersion = watchlistPoolVersion;
        this.watchlistPoolEnabled = watchlistPoolEnabled;
        this.watchlistPoolEmpty = watchlistPoolEmpty;
        this.watchlistPoolMember = watchlistPoolMember;
        this.watchlistMembershipSource = watchlistMembershipSource;
        this.watchlistMembershipObservedAt = watchlistMembershipObservedAt;
        this.watchlistMembershipExpiresAt = watchlistMembershipExpiresAt;
        this.proofFresh = proofFresh;
        this.proofStale = proofStale;
        this.displaySlotOnly = displaySlotOnly;
        this.defaultDisplaySlot = defaultDisplaySlot;
        this.displaySlotRef = displaySlotRef;
        this.promotedToHomeCandidate = promotedToHomeCandidate;
        this.lowFrequencyScanCandidate = lowFrequencyScanCandidate;
        this.auditRef = auditRef;
        this.operatorRef = operatorRef;
        this.membershipReason = membershipReason;
        this.proofReason = proofReason;
        this.allowedCandidateBoundaryLabel = allowedCandidateBoundaryLabel;
        this.blockedCandidateBoundaryLabel = blockedCandidateBoundaryLabel;
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

    public static WatchlistPoolProofSourceBindingDTO incomplete(
            String watchlistPoolProofContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolRef,
            List<String> missingFields,
            String missingReason
    ) {
        return new WatchlistPoolProofSourceBindingDTO(
                watchlistPoolProofContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolRef,
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

    public static WatchlistPoolProofSourceBindingDTO blockedFailClosed(
            String watchlistPoolProofContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolRef,
            List<String> blockedReasons,
            String blockedCandidateBoundaryLabel,
            String blockedReason
    ) {
        return new WatchlistPoolProofSourceBindingDTO(
                watchlistPoolProofContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolRef,
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                blockedCandidateBoundaryLabel,
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

    public static WatchlistPoolProofSourceBindingDTO degraded(
            String watchlistPoolProofContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolRef,
            String watchlistPoolVersion,
            Boolean watchlistPoolEnabled,
            Boolean watchlistPoolEmpty,
            Boolean watchlistPoolMember,
            String watchlistMembershipSource,
            String watchlistMembershipObservedAt,
            String watchlistMembershipExpiresAt,
            Boolean proofFresh,
            Boolean proofStale,
            Boolean displaySlotOnly,
            Boolean defaultDisplaySlot,
            String displaySlotRef,
            Boolean promotedToHomeCandidate,
            Boolean lowFrequencyScanCandidate,
            String auditRef,
            String operatorRef,
            String membershipReason,
            String proofReason,
            String allowedCandidateBoundaryLabel,
            String blockedCandidateBoundaryLabel,
            List<String> missingFields,
            List<String> degradedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            Boolean trustedSource
    ) {
        return new WatchlistPoolProofSourceBindingDTO(
                watchlistPoolProofContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolRef,
                watchlistPoolVersion,
                watchlistPoolEnabled,
                watchlistPoolEmpty,
                watchlistPoolMember,
                watchlistMembershipSource,
                watchlistMembershipObservedAt,
                watchlistMembershipExpiresAt,
                proofFresh,
                proofStale,
                displaySlotOnly,
                defaultDisplaySlot,
                displaySlotRef,
                promotedToHomeCandidate,
                lowFrequencyScanCandidate,
                auditRef,
                operatorRef,
                membershipReason,
                proofReason,
                allowedCandidateBoundaryLabel,
                blockedCandidateBoundaryLabel,
                missingFields,
                degradedReasons,
                List.of(),
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED
        );
    }

    public static WatchlistPoolProofSourceBindingDTO reviewOnly(
            String watchlistPoolProofContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolRef,
            String watchlistPoolVersion,
            Boolean watchlistPoolEnabled,
            Boolean watchlistPoolEmpty,
            Boolean watchlistPoolMember,
            String watchlistMembershipSource,
            String watchlistMembershipObservedAt,
            String watchlistMembershipExpiresAt,
            Boolean proofFresh,
            Boolean proofStale,
            Boolean displaySlotOnly,
            Boolean defaultDisplaySlot,
            String displaySlotRef,
            Boolean promotedToHomeCandidate,
            Boolean lowFrequencyScanCandidate,
            String auditRef,
            String operatorRef,
            String membershipReason,
            String proofReason,
            String allowedCandidateBoundaryLabel,
            String blockedCandidateBoundaryLabel,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            Boolean trustedSource
    ) {
        return new WatchlistPoolProofSourceBindingDTO(
                watchlistPoolProofContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolRef,
                watchlistPoolVersion,
                watchlistPoolEnabled,
                watchlistPoolEmpty,
                watchlistPoolMember,
                watchlistMembershipSource,
                watchlistMembershipObservedAt,
                watchlistMembershipExpiresAt,
                proofFresh,
                proofStale,
                displaySlotOnly,
                defaultDisplaySlot,
                displaySlotRef,
                promotedToHomeCandidate,
                lowFrequencyScanCandidate,
                auditRef,
                operatorRef,
                membershipReason,
                proofReason,
                allowedCandidateBoundaryLabel,
                blockedCandidateBoundaryLabel,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                null,
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING
        );
    }

    public String getWatchlistPoolProofContextId() {
        return watchlistPoolProofContextId;
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

    public String getRiskActionGuardContextRef() {
        return riskActionGuardContextRef;
    }

    public String getWatchlistPoolRef() {
        return watchlistPoolRef;
    }

    public String getWatchlistPoolVersion() {
        return watchlistPoolVersion;
    }

    public Boolean getWatchlistPoolEnabled() {
        return watchlistPoolEnabled;
    }

    public Boolean getWatchlistPoolEmpty() {
        return watchlistPoolEmpty;
    }

    public Boolean getWatchlistPoolMember() {
        return watchlistPoolMember;
    }

    public String getWatchlistMembershipSource() {
        return watchlistMembershipSource;
    }

    public String getWatchlistMembershipObservedAt() {
        return watchlistMembershipObservedAt;
    }

    public String getWatchlistMembershipExpiresAt() {
        return watchlistMembershipExpiresAt;
    }

    public Boolean getProofFresh() {
        return proofFresh;
    }

    public Boolean getProofStale() {
        return proofStale;
    }

    public Boolean getDisplaySlotOnly() {
        return displaySlotOnly;
    }

    public Boolean getDefaultDisplaySlot() {
        return defaultDisplaySlot;
    }

    public String getDisplaySlotRef() {
        return displaySlotRef;
    }

    public Boolean getPromotedToHomeCandidate() {
        return promotedToHomeCandidate;
    }

    public Boolean getLowFrequencyScanCandidate() {
        return lowFrequencyScanCandidate;
    }

    public String getAuditRef() {
        return auditRef;
    }

    public String getOperatorRef() {
        return operatorRef;
    }

    public String getMembershipReason() {
        return membershipReason;
    }

    public String getProofReason() {
        return proofReason;
    }

    public String getAllowedCandidateBoundaryLabel() {
        return allowedCandidateBoundaryLabel;
    }

    public String getBlockedCandidateBoundaryLabel() {
        return blockedCandidateBoundaryLabel;
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
