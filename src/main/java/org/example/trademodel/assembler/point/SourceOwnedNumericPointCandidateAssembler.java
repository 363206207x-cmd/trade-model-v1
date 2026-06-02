package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyNumericPointProposalDTO;
import org.example.trademodel.validator.point.NumericPointSafetyValidator;

public class SourceOwnedNumericPointCandidateAssembler {

    private static final String REASON_CONTEXT_MISSING = "SOURCE_OWNED_CONTEXT_MISSING";
    private static final String REASON_SOURCE_UNTRUSTED = "SOURCE_UNTRUSTED";
    private static final String REASON_ENTRY_SOURCE_REF_MISSING = "ENTRY_SOURCE_REF_MISSING";
    private static final String REASON_STOP_SOURCE_REF_MISSING = "STOP_SOURCE_REF_MISSING";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final ReviewOnlyNumericPointProposalAssembler proposalAssembler;

    public SourceOwnedNumericPointCandidateAssembler() {
        this(new ReviewOnlyNumericPointProposalAssembler());
    }

    public SourceOwnedNumericPointCandidateAssembler(
            ReviewOnlyNumericPointProposalAssembler proposalAssembler
    ) {
        this.proposalAssembler = proposalAssembler == null
                ? new ReviewOnlyNumericPointProposalAssembler()
                : proposalAssembler;
    }

    public SourceOwnedAssembledNumericPoint assemble(SourceOwnedNumericPointContext context) {
        ReviewOnlyNumericPointProposalAssembler.AssemblyInput assemblyInput = toAssemblyInput(context);
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                proposalAssembler.assemble(assemblyInput);
        return new SourceOwnedAssembledNumericPoint(
                assembled,
                assembled.getProposal(),
                assembled.getValidationResult()
        );
    }

    private static ReviewOnlyNumericPointProposalAssembler.AssemblyInput toAssemblyInput(
            SourceOwnedNumericPointContext context
    ) {
        if (context == null) {
            return ReviewOnlyNumericPointProposalAssembler.AssemblyInput.of(
                    null,
                    null,
                    List.of(),
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
                    List.of(REASON_CONTEXT_MISSING),
                    List.of(),
                    List.of(),
                    ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE
            );
        }

        if (hasUntrustedSource(context)) {
            return blockedInput(context, REASON_SOURCE_UNTRUSTED);
        }

        ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus = requestedStatusFor(context);
        return ReviewOnlyNumericPointProposalAssembler.AssemblyInput.of(
                context.getSymbol(),
                context.getMarket(),
                context.getRequestedTimeframes(),
                context.getSourceTraceRefs(),
                context.getRuntimeKlineContextRefs(),
                context.getDataQualityContextRef(),
                context.getMultiTimeframeContextRef(),
                context.getRiskActionGuardRef(),
                context.getWatchlistPoolProof(),
                toEntryReviewPoint(context.getEntryContext()),
                toStopReviewPoint(context.getStopContext()),
                toTakeProfitReviewLevels(context.getTakeProfitContexts()),
                toRiskRewardReviewField(context.getRiskRewardContext()),
                missingReasonsFor(context, requestedStatus),
                context.getBlockedReasons(),
                context.getForbiddenSemantics(),
                requestedStatus
        );
    }

    private static ReviewOnlyNumericPointProposalAssembler.AssemblyInput blockedInput(
            SourceOwnedNumericPointContext context,
            String reason
    ) {
        List<String> blockedReasons = context.getBlockedReasons();
        if (blockedReasons.isEmpty()) {
            blockedReasons = List.of(reason);
        }

        return ReviewOnlyNumericPointProposalAssembler.AssemblyInput.of(
                context.getSymbol(),
                context.getMarket(),
                context.getRequestedTimeframes(),
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
                blockedReasons,
                context.getForbiddenSemantics(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private static ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatusFor(
            SourceOwnedNumericPointContext context
    ) {
        ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus = context.getRequestedStatus();
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED;
        }
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED
                .equals(requestedStatus)) {
            return ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED;
        }
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
                .equals(requestedStatus)) {
            if (isEntrySourceRefMissing(context)) {
                return ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE;
            }
            if (isStopSourceRefMissing(context)) {
                return ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE;
            }
            if (hasMissingPointWithReason(context)) {
                return ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED;
            }
            return ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE;
        }
        return ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE;
    }

    private static List<String> missingReasonsFor(
            SourceOwnedNumericPointContext context,
            ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
    ) {
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE.equals(requestedStatus)) {
            if (isEntrySourceRefMissing(context)) {
                return List.of(REASON_ENTRY_SOURCE_REF_MISSING);
            }
            if (isStopSourceRefMissing(context)) {
                return List.of(REASON_STOP_SOURCE_REF_MISSING);
            }
            List<String> missingReasons = context.getMissingReasons();
            return missingReasons.isEmpty() ? List.of(REASON_UNSUPPORTED_STATUS) : missingReasons;
        }
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED
                .equals(requestedStatus)) {
            return context.getMissingReasons();
        }
        return context.getMissingReasons();
    }

    private static ReviewOnlyNumericPointProposalDTO.EntryReviewPoint toEntryReviewPoint(
            SourceOwnedEntryContext entryContext
    ) {
        if (entryContext == null) {
            return null;
        }
        return ReviewOnlyNumericPointProposalDTO.EntryReviewPoint.of(
                entryContext.getEntryPrice(),
                entryContext.getEntryZoneLow(),
                entryContext.getEntryZoneHigh(),
                entryContext.getEntryTimeframe(),
                entryContext.getEntrySourceRef(),
                entryContext.getEntryMissingReason()
        );
    }

    private static ReviewOnlyNumericPointProposalDTO.StopReviewPoint toStopReviewPoint(
            SourceOwnedStopContext stopContext
    ) {
        if (stopContext == null) {
            return null;
        }
        return ReviewOnlyNumericPointProposalDTO.StopReviewPoint.of(
                stopContext.getStopPrice(),
                stopContext.getStopZoneLow(),
                stopContext.getStopZoneHigh(),
                stopContext.getStopTimeframe(),
                stopContext.getStopSourceRef(),
                stopContext.getStopMissingReason()
        );
    }

    private static List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> toTakeProfitReviewLevels(
            List<SourceOwnedTakeProfitContext> takeProfitContexts
    ) {
        List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels = new ArrayList<>();
        for (SourceOwnedTakeProfitContext takeProfitContext : copy(takeProfitContexts)) {
            if (takeProfitContext != null && !isBlank(takeProfitContext.getTakeProfitSourceRef())) {
                takeProfitLevels.add(ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel.of(
                        takeProfitContext.getLevel(),
                        takeProfitContext.getTakeProfitPrice(),
                        takeProfitContext.getTakeProfitZoneLow(),
                        takeProfitContext.getTakeProfitZoneHigh(),
                        takeProfitContext.getTakeProfitTimeframe(),
                        takeProfitContext.getTakeProfitSourceRef(),
                        takeProfitContext.getTakeProfitMissingReason()
                ));
            }
        }
        return takeProfitLevels;
    }

    private static ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField toRiskRewardReviewField(
            SourceOwnedRiskRewardContext riskRewardContext
    ) {
        if (riskRewardContext == null || isBlank(riskRewardContext.getCalculationTrace())) {
            return null;
        }
        return ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField.of(
                riskRewardContext.getRiskRewardValue(),
                riskRewardContext.getRiskRewardFormula(),
                riskRewardContext.getEntrySourceRef(),
                riskRewardContext.getStopSourceRef(),
                riskRewardContext.getTakeProfitSourceRef(),
                riskRewardContext.getCalculationTrace(),
                riskRewardContext.getRiskRewardMissingReason()
        );
    }

    private static boolean hasUntrustedSource(SourceOwnedNumericPointContext context) {
        if (context.getEntryContext() != null && !context.getEntryContext().isTrustedSource()) {
            return true;
        }
        if (context.getStopContext() != null && !context.getStopContext().isTrustedSource()) {
            return true;
        }
        for (SourceOwnedTakeProfitContext takeProfitContext : context.getTakeProfitContexts()) {
            if (takeProfitContext != null && !takeProfitContext.isTrustedSource()) {
                return true;
            }
        }
        return context.getRiskRewardContext() != null && !context.getRiskRewardContext().isTrustedSource();
    }

    private static boolean isEntrySourceRefMissing(SourceOwnedNumericPointContext context) {
        return context.getEntryContext() != null && isBlank(context.getEntryContext().getEntrySourceRef());
    }

    private static boolean isStopSourceRefMissing(SourceOwnedNumericPointContext context) {
        return context.getStopContext() != null && isBlank(context.getStopContext().getStopSourceRef());
    }

    private static boolean hasMissingPointWithReason(SourceOwnedNumericPointContext context) {
        if (context.getMissingReasons().isEmpty()) {
            return false;
        }
        return context.getTakeProfitContexts().isEmpty() || context.getRiskRewardContext() == null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static class SourceOwnedNumericPointContext {
        private final String symbol;
        private final String market;
        private final List<String> requestedTimeframes;
        private final List<String> sourceTraceRefs;
        private final List<String> runtimeKlineContextRefs;
        private final String dataQualityContextRef;
        private final String multiTimeframeContextRef;
        private final String riskActionGuardRef;
        private final String watchlistPoolProof;
        private final SourceOwnedEntryContext entryContext;
        private final SourceOwnedStopContext stopContext;
        private final List<SourceOwnedTakeProfitContext> takeProfitContexts;
        private final SourceOwnedRiskRewardContext riskRewardContext;
        private final List<String> missingReasons;
        private final List<String> blockedReasons;
        private final List<String> forbiddenSemantics;
        private final ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus;

        private SourceOwnedNumericPointContext(
                String symbol,
                String market,
                List<String> requestedTimeframes,
                List<String> sourceTraceRefs,
                List<String> runtimeKlineContextRefs,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String watchlistPoolProof,
                SourceOwnedEntryContext entryContext,
                SourceOwnedStopContext stopContext,
                List<SourceOwnedTakeProfitContext> takeProfitContexts,
                SourceOwnedRiskRewardContext riskRewardContext,
                List<String> missingReasons,
                List<String> blockedReasons,
                List<String> forbiddenSemantics,
                ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
        ) {
            this.symbol = symbol;
            this.market = market;
            this.requestedTimeframes = copy(requestedTimeframes);
            this.sourceTraceRefs = copy(sourceTraceRefs);
            this.runtimeKlineContextRefs = copy(runtimeKlineContextRefs);
            this.dataQualityContextRef = dataQualityContextRef;
            this.multiTimeframeContextRef = multiTimeframeContextRef;
            this.riskActionGuardRef = riskActionGuardRef;
            this.watchlistPoolProof = watchlistPoolProof;
            this.entryContext = entryContext;
            this.stopContext = stopContext;
            this.takeProfitContexts = copy(takeProfitContexts);
            this.riskRewardContext = riskRewardContext;
            this.missingReasons = copy(missingReasons);
            this.blockedReasons = copy(blockedReasons);
            this.forbiddenSemantics = copy(forbiddenSemantics);
            this.requestedStatus = requestedStatus;
        }

        public static SourceOwnedNumericPointContext of(
                String symbol,
                String market,
                List<String> requestedTimeframes,
                List<String> sourceTraceRefs,
                List<String> runtimeKlineContextRefs,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String watchlistPoolProof,
                SourceOwnedEntryContext entryContext,
                SourceOwnedStopContext stopContext,
                List<SourceOwnedTakeProfitContext> takeProfitContexts,
                SourceOwnedRiskRewardContext riskRewardContext,
                List<String> missingReasons,
                List<String> blockedReasons,
                List<String> forbiddenSemantics,
                ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
        ) {
            return new SourceOwnedNumericPointContext(
                    symbol,
                    market,
                    requestedTimeframes,
                    sourceTraceRefs,
                    runtimeKlineContextRefs,
                    dataQualityContextRef,
                    multiTimeframeContextRef,
                    riskActionGuardRef,
                    watchlistPoolProof,
                    entryContext,
                    stopContext,
                    takeProfitContexts,
                    riskRewardContext,
                    missingReasons,
                    blockedReasons,
                    forbiddenSemantics,
                    requestedStatus
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

        public SourceOwnedEntryContext getEntryContext() {
            return entryContext;
        }

        public SourceOwnedStopContext getStopContext() {
            return stopContext;
        }

        public List<SourceOwnedTakeProfitContext> getTakeProfitContexts() {
            return copy(takeProfitContexts);
        }

        public SourceOwnedRiskRewardContext getRiskRewardContext() {
            return riskRewardContext;
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

        public ReviewOnlyNumericPointProposalDTO.ProposalStatus getRequestedStatus() {
            return requestedStatus;
        }
    }

    public static class SourceOwnedEntryContext {
        private final BigDecimal entryPrice;
        private final BigDecimal entryZoneLow;
        private final BigDecimal entryZoneHigh;
        private final String entryTimeframe;
        private final String entrySourceRef;
        private final String entryMissingReason;
        private final boolean trustedSource;

        private SourceOwnedEntryContext(
                BigDecimal entryPrice,
                BigDecimal entryZoneLow,
                BigDecimal entryZoneHigh,
                String entryTimeframe,
                String entrySourceRef,
                String entryMissingReason,
                boolean trustedSource
        ) {
            this.entryPrice = entryPrice;
            this.entryZoneLow = entryZoneLow;
            this.entryZoneHigh = entryZoneHigh;
            this.entryTimeframe = entryTimeframe;
            this.entrySourceRef = entrySourceRef;
            this.entryMissingReason = entryMissingReason;
            this.trustedSource = trustedSource;
        }

        public static SourceOwnedEntryContext of(
                BigDecimal entryPrice,
                BigDecimal entryZoneLow,
                BigDecimal entryZoneHigh,
                String entryTimeframe,
                String entrySourceRef,
                String entryMissingReason,
                boolean trustedSource
        ) {
            return new SourceOwnedEntryContext(
                    entryPrice,
                    entryZoneLow,
                    entryZoneHigh,
                    entryTimeframe,
                    entrySourceRef,
                    entryMissingReason,
                    trustedSource
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

        public boolean isTrustedSource() {
            return trustedSource;
        }
    }

    public static class SourceOwnedStopContext {
        private final BigDecimal stopPrice;
        private final BigDecimal stopZoneLow;
        private final BigDecimal stopZoneHigh;
        private final String stopTimeframe;
        private final String stopSourceRef;
        private final String stopMissingReason;
        private final boolean trustedSource;

        private SourceOwnedStopContext(
                BigDecimal stopPrice,
                BigDecimal stopZoneLow,
                BigDecimal stopZoneHigh,
                String stopTimeframe,
                String stopSourceRef,
                String stopMissingReason,
                boolean trustedSource
        ) {
            this.stopPrice = stopPrice;
            this.stopZoneLow = stopZoneLow;
            this.stopZoneHigh = stopZoneHigh;
            this.stopTimeframe = stopTimeframe;
            this.stopSourceRef = stopSourceRef;
            this.stopMissingReason = stopMissingReason;
            this.trustedSource = trustedSource;
        }

        public static SourceOwnedStopContext of(
                BigDecimal stopPrice,
                BigDecimal stopZoneLow,
                BigDecimal stopZoneHigh,
                String stopTimeframe,
                String stopSourceRef,
                String stopMissingReason,
                boolean trustedSource
        ) {
            return new SourceOwnedStopContext(
                    stopPrice,
                    stopZoneLow,
                    stopZoneHigh,
                    stopTimeframe,
                    stopSourceRef,
                    stopMissingReason,
                    trustedSource
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

        public boolean isTrustedSource() {
            return trustedSource;
        }
    }

    public static class SourceOwnedTakeProfitContext {
        private final int level;
        private final BigDecimal takeProfitPrice;
        private final BigDecimal takeProfitZoneLow;
        private final BigDecimal takeProfitZoneHigh;
        private final String takeProfitTimeframe;
        private final String takeProfitSourceRef;
        private final String takeProfitMissingReason;
        private final boolean trustedSource;

        private SourceOwnedTakeProfitContext(
                int level,
                BigDecimal takeProfitPrice,
                BigDecimal takeProfitZoneLow,
                BigDecimal takeProfitZoneHigh,
                String takeProfitTimeframe,
                String takeProfitSourceRef,
                String takeProfitMissingReason,
                boolean trustedSource
        ) {
            this.level = level;
            this.takeProfitPrice = takeProfitPrice;
            this.takeProfitZoneLow = takeProfitZoneLow;
            this.takeProfitZoneHigh = takeProfitZoneHigh;
            this.takeProfitTimeframe = takeProfitTimeframe;
            this.takeProfitSourceRef = takeProfitSourceRef;
            this.takeProfitMissingReason = takeProfitMissingReason;
            this.trustedSource = trustedSource;
        }

        public static SourceOwnedTakeProfitContext of(
                int level,
                BigDecimal takeProfitPrice,
                BigDecimal takeProfitZoneLow,
                BigDecimal takeProfitZoneHigh,
                String takeProfitTimeframe,
                String takeProfitSourceRef,
                String takeProfitMissingReason,
                boolean trustedSource
        ) {
            return new SourceOwnedTakeProfitContext(
                    level,
                    takeProfitPrice,
                    takeProfitZoneLow,
                    takeProfitZoneHigh,
                    takeProfitTimeframe,
                    takeProfitSourceRef,
                    takeProfitMissingReason,
                    trustedSource
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

        public boolean isTrustedSource() {
            return trustedSource;
        }
    }

    public static class SourceOwnedRiskRewardContext {
        private final BigDecimal riskRewardValue;
        private final String riskRewardFormula;
        private final String entrySourceRef;
        private final String stopSourceRef;
        private final String takeProfitSourceRef;
        private final String calculationTrace;
        private final String riskRewardMissingReason;
        private final boolean trustedSource;

        private SourceOwnedRiskRewardContext(
                BigDecimal riskRewardValue,
                String riskRewardFormula,
                String entrySourceRef,
                String stopSourceRef,
                String takeProfitSourceRef,
                String calculationTrace,
                String riskRewardMissingReason,
                boolean trustedSource
        ) {
            this.riskRewardValue = riskRewardValue;
            this.riskRewardFormula = riskRewardFormula;
            this.entrySourceRef = entrySourceRef;
            this.stopSourceRef = stopSourceRef;
            this.takeProfitSourceRef = takeProfitSourceRef;
            this.calculationTrace = calculationTrace;
            this.riskRewardMissingReason = riskRewardMissingReason;
            this.trustedSource = trustedSource;
        }

        public static SourceOwnedRiskRewardContext of(
                BigDecimal riskRewardValue,
                String riskRewardFormula,
                String entrySourceRef,
                String stopSourceRef,
                String takeProfitSourceRef,
                String calculationTrace,
                String riskRewardMissingReason,
                boolean trustedSource
        ) {
            return new SourceOwnedRiskRewardContext(
                    riskRewardValue,
                    riskRewardFormula,
                    entrySourceRef,
                    stopSourceRef,
                    takeProfitSourceRef,
                    calculationTrace,
                    riskRewardMissingReason,
                    trustedSource
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

        public boolean isTrustedSource() {
            return trustedSource;
        }
    }

    public static class SourceOwnedAssembledNumericPoint {
        private final ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled;
        private final ReviewOnlyNumericPointProposalDTO proposal;
        private final NumericPointSafetyValidator.ValidationResult validationResult;

        private SourceOwnedAssembledNumericPoint(
                ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled,
                ReviewOnlyNumericPointProposalDTO proposal,
                NumericPointSafetyValidator.ValidationResult validationResult
        ) {
            this.assembled = assembled;
            this.proposal = proposal;
            this.validationResult = validationResult;
        }

        public ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint getAssembled() {
            return assembled;
        }

        public ReviewOnlyNumericPointProposalDTO getProposal() {
            return proposal;
        }

        public NumericPointSafetyValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
