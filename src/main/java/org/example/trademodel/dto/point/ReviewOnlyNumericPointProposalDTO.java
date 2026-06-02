package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReviewOnlyNumericPointProposalDTO {

    public enum ProposalStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_NUMERIC_POINT_CANDIDATE,
        REVIEW_ONLY_NUMERIC_POINT_DEGRADED,
        RECHECK_REQUIRED,
        MANUAL_REVIEW_REQUIRED
    }

    private final String symbol;
    private final String market;
    private final List<String> requestedTimeframes;
    private final ProposalStatus proposalStatus;
    private final List<String> sourceTraceRefs;
    private final List<String> runtimeKlineContextRefs;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final String riskActionGuardRef;
    private final String watchlistPoolProof;
    private final EntryReviewPoint entry;
    private final StopReviewPoint stop;
    private final List<TakeProfitReviewLevel> takeProfitLevels;
    private final RiskRewardReviewField riskReward;
    private final List<String> missingReasons;
    private final List<String> blockedReasons;
    private final List<String> forbiddenSemantics;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean recheckRequired;
    private final boolean riskActionGuardRequired;
    private final boolean sourceTraceRequired;
    private final boolean runtimeKlineContextRequired;
    private final boolean dataQualityRequired;
    private final boolean multiTimeframeRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;

    private ReviewOnlyNumericPointProposalDTO(
            String symbol,
            String market,
            List<String> requestedTimeframes,
            ProposalStatus proposalStatus,
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            EntryReviewPoint entry,
            StopReviewPoint stop,
            List<TakeProfitReviewLevel> takeProfitLevels,
            RiskRewardReviewField riskReward,
            List<String> missingReasons,
            List<String> blockedReasons,
            List<String> forbiddenSemantics
    ) {
        this.symbol = symbol;
        this.market = market;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.proposalStatus = proposalStatus;
        this.sourceTraceRefs = copy(sourceTraceRefs);
        this.runtimeKlineContextRefs = copy(runtimeKlineContextRefs);
        this.dataQualityContextRef = dataQualityContextRef;
        this.multiTimeframeContextRef = multiTimeframeContextRef;
        this.riskActionGuardRef = riskActionGuardRef;
        this.watchlistPoolProof = watchlistPoolProof;
        this.entry = entry;
        this.stop = stop;
        this.takeProfitLevels = copy(takeProfitLevels);
        this.riskReward = riskReward;
        this.missingReasons = copy(missingReasons);
        this.blockedReasons = copy(blockedReasons);
        this.forbiddenSemantics = copy(forbiddenSemantics);
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.recheckRequired = true;
        this.riskActionGuardRequired = true;
        this.sourceTraceRequired = true;
        this.runtimeKlineContextRequired = true;
        this.dataQualityRequired = true;
        this.multiTimeframeRequired = true;
        this.incompleteSafe = true;
        this.failClosed = proposalStatus == ProposalStatus.BLOCKED_FAIL_CLOSED;
    }

    public static ReviewOnlyNumericPointProposalDTO incomplete(
            String symbol,
            String market,
            List<String> requestedTimeframes,
            List<String> missingReasons
    ) {
        return new ReviewOnlyNumericPointProposalDTO(
                symbol,
                market,
                requestedTimeframes,
                ProposalStatus.INCOMPLETE,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                requiredReasons(missingReasons, "missingReasons"),
                List.of(),
                List.of()
        );
    }

    public static ReviewOnlyNumericPointProposalDTO blockedFailClosed(
            String symbol,
            String market,
            List<String> requestedTimeframes,
            List<String> blockedReasons,
            List<String> forbiddenSemantics
    ) {
        return new ReviewOnlyNumericPointProposalDTO(
                symbol,
                market,
                requestedTimeframes,
                ProposalStatus.BLOCKED_FAIL_CLOSED,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                requiredReasons(blockedReasons, "blockedReasons"),
                forbiddenSemantics
        );
    }

    public static ReviewOnlyNumericPointProposalDTO degraded(
            String symbol,
            String market,
            List<String> requestedTimeframes,
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            EntryReviewPoint entry,
            StopReviewPoint stop,
            List<TakeProfitReviewLevel> takeProfitLevels,
            RiskRewardReviewField riskReward,
            List<String> missingReasons
    ) {
        return new ReviewOnlyNumericPointProposalDTO(
                symbol,
                market,
                requestedTimeframes,
                ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED,
                sourceTraceRefs,
                runtimeKlineContextRefs,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                watchlistPoolProof,
                entry,
                stop,
                takeProfitLevels,
                riskReward,
                requiredReasons(missingReasons, "missingReasons"),
                List.of(),
                List.of()
        );
    }

    public static ReviewOnlyNumericPointProposalDTO reviewOnlyCandidate(
            String symbol,
            String market,
            List<String> requestedTimeframes,
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            EntryReviewPoint entry,
            StopReviewPoint stop,
            List<TakeProfitReviewLevel> takeProfitLevels,
            RiskRewardReviewField riskReward,
            List<String> forbiddenSemantics
    ) {
        return new ReviewOnlyNumericPointProposalDTO(
                symbol,
                market,
                requestedTimeframes,
                ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE,
                sourceTraceRefs,
                runtimeKlineContextRefs,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                watchlistPoolProof,
                entry,
                stop,
                takeProfitLevels,
                riskReward,
                List.of(),
                List.of(),
                forbiddenSemantics
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMarket() {
        return market;
    }

    public List<String> getRequestedTimeframes() {
        return copy(requestedTimeframes);
    }

    public ProposalStatus getProposalStatus() {
        return proposalStatus;
    }

    public List<String> getSourceTraceRefs() {
        return copy(sourceTraceRefs);
    }

    public List<String> getRuntimeKlineContextRefs() {
        return copy(runtimeKlineContextRefs);
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

    public String getWatchlistPoolProof() {
        return watchlistPoolProof;
    }

    public EntryReviewPoint getEntry() {
        return entry;
    }

    public StopReviewPoint getStop() {
        return stop;
    }

    public List<TakeProfitReviewLevel> getTakeProfitLevels() {
        return copy(takeProfitLevels);
    }

    public RiskRewardReviewField getRiskReward() {
        return riskReward;
    }

    public List<String> getMissingReasons() {
        return copy(missingReasons);
    }

    public List<String> getBlockedReasons() {
        return copy(blockedReasons);
    }

    public List<String> getForbiddenSemantics() {
        return copy(forbiddenSemantics);
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

    public boolean isRecheckRequired() {
        return recheckRequired;
    }

    public boolean isRiskActionGuardRequired() {
        return riskActionGuardRequired;
    }

    public boolean isSourceTraceRequired() {
        return sourceTraceRequired;
    }

    public boolean isRuntimeKlineContextRequired() {
        return runtimeKlineContextRequired;
    }

    public boolean isDataQualityRequired() {
        return dataQualityRequired;
    }

    public boolean isMultiTimeframeRequired() {
        return multiTimeframeRequired;
    }

    public boolean isIncompleteSafe() {
        return incompleteSafe;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public static class EntryReviewPoint {

        private final BigDecimal entryPrice;
        private final BigDecimal entryZoneLow;
        private final BigDecimal entryZoneHigh;
        private final String entryTimeframe;
        private final String entrySourceRef;
        private final String entryMissingReason;

        private EntryReviewPoint(
                BigDecimal entryPrice,
                BigDecimal entryZoneLow,
                BigDecimal entryZoneHigh,
                String entryTimeframe,
                String entrySourceRef,
                String entryMissingReason
        ) {
            this.entryPrice = entryPrice;
            this.entryZoneLow = entryZoneLow;
            this.entryZoneHigh = entryZoneHigh;
            this.entryTimeframe = entryTimeframe;
            this.entrySourceRef = entrySourceRef;
            this.entryMissingReason = entryMissingReason;
        }

        public static EntryReviewPoint of(
                BigDecimal entryPrice,
                BigDecimal entryZoneLow,
                BigDecimal entryZoneHigh,
                String entryTimeframe,
                String entrySourceRef,
                String entryMissingReason
        ) {
            return new EntryReviewPoint(
                    entryPrice,
                    entryZoneLow,
                    entryZoneHigh,
                    entryTimeframe,
                    entrySourceRef,
                    entryMissingReason
            );
        }

        public BigDecimal getEntryPrice() {
            return entryPrice;
        }

        public BigDecimal getEntryZoneLow() {
            return entryZoneLow;
        }

        public BigDecimal getEntryZoneHigh() {
            return entryZoneHigh;
        }

        public String getEntryTimeframe() {
            return entryTimeframe;
        }

        public String getEntrySourceRef() {
            return entrySourceRef;
        }

        public String getEntryMissingReason() {
            return entryMissingReason;
        }
    }

    public static class StopReviewPoint {

        private final BigDecimal stopPrice;
        private final BigDecimal stopZoneLow;
        private final BigDecimal stopZoneHigh;
        private final String stopTimeframe;
        private final String stopSourceRef;
        private final String stopMissingReason;

        private StopReviewPoint(
                BigDecimal stopPrice,
                BigDecimal stopZoneLow,
                BigDecimal stopZoneHigh,
                String stopTimeframe,
                String stopSourceRef,
                String stopMissingReason
        ) {
            this.stopPrice = stopPrice;
            this.stopZoneLow = stopZoneLow;
            this.stopZoneHigh = stopZoneHigh;
            this.stopTimeframe = stopTimeframe;
            this.stopSourceRef = stopSourceRef;
            this.stopMissingReason = stopMissingReason;
        }

        public static StopReviewPoint of(
                BigDecimal stopPrice,
                BigDecimal stopZoneLow,
                BigDecimal stopZoneHigh,
                String stopTimeframe,
                String stopSourceRef,
                String stopMissingReason
        ) {
            return new StopReviewPoint(
                    stopPrice,
                    stopZoneLow,
                    stopZoneHigh,
                    stopTimeframe,
                    stopSourceRef,
                    stopMissingReason
            );
        }

        public BigDecimal getStopPrice() {
            return stopPrice;
        }

        public BigDecimal getStopZoneLow() {
            return stopZoneLow;
        }

        public BigDecimal getStopZoneHigh() {
            return stopZoneHigh;
        }

        public String getStopTimeframe() {
            return stopTimeframe;
        }

        public String getStopSourceRef() {
            return stopSourceRef;
        }

        public String getStopMissingReason() {
            return stopMissingReason;
        }
    }

    public static class TakeProfitReviewLevel {

        private final int level;
        private final BigDecimal takeProfitPrice;
        private final BigDecimal takeProfitZoneLow;
        private final BigDecimal takeProfitZoneHigh;
        private final String takeProfitTimeframe;
        private final String takeProfitSourceRef;
        private final String takeProfitMissingReason;

        private TakeProfitReviewLevel(
                int level,
                BigDecimal takeProfitPrice,
                BigDecimal takeProfitZoneLow,
                BigDecimal takeProfitZoneHigh,
                String takeProfitTimeframe,
                String takeProfitSourceRef,
                String takeProfitMissingReason
        ) {
            this.level = level;
            this.takeProfitPrice = takeProfitPrice;
            this.takeProfitZoneLow = takeProfitZoneLow;
            this.takeProfitZoneHigh = takeProfitZoneHigh;
            this.takeProfitTimeframe = takeProfitTimeframe;
            this.takeProfitSourceRef = takeProfitSourceRef;
            this.takeProfitMissingReason = takeProfitMissingReason;
        }

        public static TakeProfitReviewLevel of(
                int level,
                BigDecimal takeProfitPrice,
                BigDecimal takeProfitZoneLow,
                BigDecimal takeProfitZoneHigh,
                String takeProfitTimeframe,
                String takeProfitSourceRef,
                String takeProfitMissingReason
        ) {
            return new TakeProfitReviewLevel(
                    level,
                    takeProfitPrice,
                    takeProfitZoneLow,
                    takeProfitZoneHigh,
                    takeProfitTimeframe,
                    takeProfitSourceRef,
                    takeProfitMissingReason
            );
        }

        public int getLevel() {
            return level;
        }

        public BigDecimal getTakeProfitPrice() {
            return takeProfitPrice;
        }

        public BigDecimal getTakeProfitZoneLow() {
            return takeProfitZoneLow;
        }

        public BigDecimal getTakeProfitZoneHigh() {
            return takeProfitZoneHigh;
        }

        public String getTakeProfitTimeframe() {
            return takeProfitTimeframe;
        }

        public String getTakeProfitSourceRef() {
            return takeProfitSourceRef;
        }

        public String getTakeProfitMissingReason() {
            return takeProfitMissingReason;
        }
    }

    public static class RiskRewardReviewField {

        private final BigDecimal riskRewardValue;
        private final String riskRewardFormula;
        private final String entrySourceRef;
        private final String stopSourceRef;
        private final String takeProfitSourceRef;
        private final String calculationTrace;
        private final String riskRewardMissingReason;

        private RiskRewardReviewField(
                BigDecimal riskRewardValue,
                String riskRewardFormula,
                String entrySourceRef,
                String stopSourceRef,
                String takeProfitSourceRef,
                String calculationTrace,
                String riskRewardMissingReason
        ) {
            this.riskRewardValue = riskRewardValue;
            this.riskRewardFormula = riskRewardFormula;
            this.entrySourceRef = entrySourceRef;
            this.stopSourceRef = stopSourceRef;
            this.takeProfitSourceRef = takeProfitSourceRef;
            this.calculationTrace = calculationTrace;
            this.riskRewardMissingReason = riskRewardMissingReason;
        }

        public static RiskRewardReviewField of(
                BigDecimal riskRewardValue,
                String riskRewardFormula,
                String entrySourceRef,
                String stopSourceRef,
                String takeProfitSourceRef,
                String calculationTrace,
                String riskRewardMissingReason
        ) {
            return new RiskRewardReviewField(
                    riskRewardValue,
                    riskRewardFormula,
                    entrySourceRef,
                    stopSourceRef,
                    takeProfitSourceRef,
                    calculationTrace,
                    riskRewardMissingReason
            );
        }

        public BigDecimal getRiskRewardValue() {
            return riskRewardValue;
        }

        public String getRiskRewardFormula() {
            return riskRewardFormula;
        }

        public String getEntrySourceRef() {
            return entrySourceRef;
        }

        public String getStopSourceRef() {
            return stopSourceRef;
        }

        public String getTakeProfitSourceRef() {
            return takeProfitSourceRef;
        }

        public String getCalculationTrace() {
            return calculationTrace;
        }

        public String getRiskRewardMissingReason() {
            return riskRewardMissingReason;
        }
    }

    private static List<String> requiredReasons(List<String> reasons, String name) {
        List<String> copied = copy(reasons);
        if (copied.isEmpty() || copied.stream().allMatch(ReviewOnlyNumericPointProposalDTO::isBlank)) {
            throw new IllegalArgumentException(name + " must include at least one reason");
        }
        return copied;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
