package org.example.trademodel.assembler.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyNumericPointProposalDTO;
import org.example.trademodel.validator.point.NumericPointSafetyValidator;

public class ReviewOnlyNumericPointProposalAssembler {

    private static final String REASON_INPUT_MISSING = "INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final NumericPointSafetyValidator validator;

    public ReviewOnlyNumericPointProposalAssembler() {
        this(new NumericPointSafetyValidator());
    }

    public ReviewOnlyNumericPointProposalAssembler(NumericPointSafetyValidator validator) {
        this.validator = validator == null ? new NumericPointSafetyValidator() : validator;
    }

    public AssembledReviewOnlyNumericPoint assemble(AssemblyInput input) {
        ReviewOnlyNumericPointProposalDTO proposal = proposalFrom(input);
        NumericPointSafetyValidator.ValidationResult validationResult = validator.validate(proposal);
        return new AssembledReviewOnlyNumericPoint(proposal, validationResult);
    }

    private ReviewOnlyNumericPointProposalDTO proposalFrom(AssemblyInput input) {
        if (input == null) {
            return ReviewOnlyNumericPointProposalDTO.incomplete(
                    null,
                    null,
                    List.of(),
                    List.of(REASON_INPUT_MISSING)
            );
        }

        ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus = input.getRequestedStatus();
        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return ReviewOnlyNumericPointProposalDTO.blockedFailClosed(
                    input.getSymbol(),
                    input.getMarket(),
                    input.getRequestedTimeframes(),
                    requiredOrFallback(input.getBlockedReasons(), REASON_BLOCKED_REASON_REQUIRED),
                    input.getForbiddenSemantics()
            );
        }

        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED
                .equals(requestedStatus)) {
            return ReviewOnlyNumericPointProposalDTO.degraded(
                    input.getSymbol(),
                    input.getMarket(),
                    input.getRequestedTimeframes(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRefs(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardRef(),
                    input.getWatchlistPoolProof(),
                    input.getEntry(),
                    input.getStop(),
                    input.getTakeProfitLevels(),
                    input.getRiskReward(),
                    requiredOrFallback(input.getMissingReasons(), REASON_MISSING_REASON_REQUIRED)
            );
        }

        if (ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
                .equals(requestedStatus)) {
            return ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
                    input.getSymbol(),
                    input.getMarket(),
                    input.getRequestedTimeframes(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRefs(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardRef(),
                    input.getWatchlistPoolProof(),
                    input.getEntry(),
                    input.getStop(),
                    input.getTakeProfitLevels(),
                    input.getRiskReward(),
                    input.getForbiddenSemantics()
            );
        }

        return ReviewOnlyNumericPointProposalDTO.incomplete(
                input.getSymbol(),
                input.getMarket(),
                input.getRequestedTimeframes(),
                requiredOrFallback(input.getMissingReasons(), REASON_UNSUPPORTED_STATUS)
        );
    }

    private static List<String> requiredOrFallback(List<String> reasons, String fallbackReason) {
        List<String> resolvedReasons = copy(reasons);
        if (resolvedReasons.isEmpty() || resolvedReasons.stream().allMatch(ReviewOnlyNumericPointProposalAssembler::isBlank)) {
            return List.of(fallbackReason);
        }
        return resolvedReasons;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static class AssemblyInput {

        private final String symbol;
        private final String market;
        private final List<String> requestedTimeframes;
        private final List<String> sourceTraceRefs;
        private final List<String> runtimeKlineContextRefs;
        private final String dataQualityContextRef;
        private final String multiTimeframeContextRef;
        private final String riskActionGuardRef;
        private final String watchlistPoolProof;
        private final ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry;
        private final ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop;
        private final List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels;
        private final ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward;
        private final List<String> missingReasons;
        private final List<String> blockedReasons;
        private final List<String> forbiddenSemantics;
        private final ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus;

        private AssemblyInput(
                String symbol,
                String market,
                List<String> requestedTimeframes,
                List<String> sourceTraceRefs,
                List<String> runtimeKlineContextRefs,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String watchlistPoolProof,
                ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
                ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
                List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
                ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward,
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
            this.entry = entry;
            this.stop = stop;
            this.takeProfitLevels = copy(takeProfitLevels);
            this.riskReward = riskReward;
            this.missingReasons = copy(missingReasons);
            this.blockedReasons = copy(blockedReasons);
            this.forbiddenSemantics = copy(forbiddenSemantics);
            this.requestedStatus = requestedStatus;
        }

        public static AssemblyInput of(
                String symbol,
                String market,
                List<String> requestedTimeframes,
                List<String> sourceTraceRefs,
                List<String> runtimeKlineContextRefs,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardRef,
                String watchlistPoolProof,
                ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
                ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
                List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
                ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward,
                List<String> missingReasons,
                List<String> blockedReasons,
                List<String> forbiddenSemantics,
                ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
        ) {
            return new AssemblyInput(
                    symbol,
                    market,
                    requestedTimeframes,
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

        public ReviewOnlyNumericPointProposalDTO.EntryReviewPoint getEntry() {
            return entry;
        }

        public ReviewOnlyNumericPointProposalDTO.StopReviewPoint getStop() {
            return stop;
        }

        public List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> getTakeProfitLevels() {
            return copy(takeProfitLevels);
        }

        public ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField getRiskReward() {
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

        public ReviewOnlyNumericPointProposalDTO.ProposalStatus getRequestedStatus() {
            return requestedStatus;
        }
    }

    public static class AssembledReviewOnlyNumericPoint {
        private final ReviewOnlyNumericPointProposalDTO proposal;
        private final NumericPointSafetyValidator.ValidationResult validationResult;

        private AssembledReviewOnlyNumericPoint(
                ReviewOnlyNumericPointProposalDTO proposal,
                NumericPointSafetyValidator.ValidationResult validationResult
        ) {
            this.proposal = proposal;
            this.validationResult = validationResult;
        }

        public ReviewOnlyNumericPointProposalDTO getProposal() {
            return proposal;
        }

        public NumericPointSafetyValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
