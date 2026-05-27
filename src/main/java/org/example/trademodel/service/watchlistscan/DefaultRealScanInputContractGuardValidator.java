package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.RealScanInputContractDTO;
import org.example.trademodel.dto.watchlistscan.RealScanInputContractStatusEnum;

public class DefaultRealScanInputContractGuardValidator implements RealScanInputContractGuardValidator {

    private static final String REASON_NULL_INPUT = "NULL_REAL_SCAN_INPUT_CONTRACT";
    private static final String REASON_SAFETY_FLAGS_MISSING = "SAFETY_FLAGS_MISSING";
    private static final String REASON_MISSING_WATCHLIST_PROOF = "MISSING_WATCHLIST_POOL_PROOF";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_INCOMPLETE_INPUT = "INCOMPLETE_REAL_SCAN_INPUT_CONTRACT";
    private static final String REASON_REVIEW_ONLY_GUARD = "REAL_SCAN_INPUT_GUARD_REVIEW_ONLY";
    private static final String REASON_VALIDATION_FAILED = "REAL_SCAN_INPUT_GUARD_VALIDATION_FAILED";

    @Override
    public RealScanInputContractDTO validate(RealScanInputContractDTO input) {
        try {
            if (input == null) {
                return RealScanInputContractDTO.incomplete(null, List.of(REASON_NULL_INPUT));
            }

            if (!input.isManualReviewRequired() || !input.isNotTradeInstruction()) {
                return RealScanInputContractDTO.incomplete(
                        input.getSymbol(),
                        withReason(input.getBlockingReasons(), REASON_SAFETY_FLAGS_MISSING)
                );
            }

            RealScanInputContractStatusEnum status = input.getStatus();
            if (RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST.equals(status)
                    || RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF.equals(status)) {
                return input;
            }

            if (!RealScanInputContractStatusEnum.REVIEW_ONLY.equals(status)) {
                return RealScanInputContractDTO.incomplete(
                        input.getSymbol(),
                        withReason(input.getBlockingReasons(), REASON_INCOMPLETE_INPUT)
                );
            }

            if (!Boolean.TRUE.equals(input.getWatchlistPoolMember())) {
                return guardedReviewOnly(input, input.getWatchlistPoolMember(), REASON_BLOCKED_NOT_WATCHLIST);
            }

            if (isBlank(input.getWatchlistPoolProof())) {
                return guardedReviewOnly(input, true, REASON_MISSING_WATCHLIST_PROOF);
            }

            return guardedReviewOnly(input, true, REASON_REVIEW_ONLY_GUARD);
        } catch (RuntimeException ex) {
            String symbol = input == null ? null : input.getSymbol();
            return RealScanInputContractDTO.incomplete(symbol, List.of(REASON_VALIDATION_FAILED));
        }
    }

    private static RealScanInputContractDTO guardedReviewOnly(
            RealScanInputContractDTO input,
            Boolean watchlistPoolMember,
            String reason
    ) {
        return RealScanInputContractDTO.reviewOnly(
                input.getSymbol(),
                input.getSource(),
                input.getRequestedScanReason(),
                watchlistPoolMember,
                input.getWatchlistPoolProof(),
                input.getWatchlistConfigVersion(),
                input.getRequestedTimeframes(),
                input.getScanTimestamp(),
                input.isMarketReadRequired(),
                input.isDataAvailabilityExpected(),
                input.getStaleInputBehavior(),
                input.getMissingInputBehavior(),
                input.getRiskBlockers(),
                input.getReviewOnlySafetyFlags(),
                withReason(input.getBlockingReasons(), reason)
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = reasons == null ? new ArrayList<>() : new ArrayList<>(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }
}
