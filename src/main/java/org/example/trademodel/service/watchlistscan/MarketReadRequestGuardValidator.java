package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;

public class MarketReadRequestGuardValidator {

    private static final String REASON_NULL_REQUEST = "NULL_MARKET_READ_REQUEST";
    private static final String REASON_MISSING_SOURCE_CONTRACT_ID = "MISSING_SOURCE_CONTRACT_ID";
    private static final String REASON_MISSING_WATCHLIST_POOL_PROOF = "MISSING_WATCHLIST_POOL_PROOF";
    private static final String REASON_MISSING_REQUESTED_TIMEFRAMES = "MISSING_REQUESTED_TIMEFRAMES";
    private static final String REASON_MISSING_SCAN_TIMESTAMP = "MISSING_SCAN_TIMESTAMP";
    private static final String REASON_STALE_POLICY_NOT_FAIL_CLOSED = "STALE_POLICY_NOT_FAIL_CLOSED";
    private static final String REASON_MISSING_DATA_POLICY_NOT_FAIL_CLOSED = "MISSING_DATA_POLICY_NOT_FAIL_CLOSED";
    private static final String REASON_REVIEW_ONLY_REQUIRED = "REVIEW_ONLY_REQUIRED";
    private static final String REASON_NOT_TRADE_INSTRUCTION_REQUIRED = "NOT_TRADE_INSTRUCTION_REQUIRED";

    public MarketReadRequestGuardValidationResult validate(MarketReadRequestDTO request) {
        if (request == null) {
            return MarketReadRequestGuardValidationResult.blocked(
                    List.of(REASON_NULL_REQUEST),
                    List.of(REASON_NULL_REQUEST),
                    List.of()
            );
        }

        List<String> validationReasons = new ArrayList<>();
        if (isBlank(request.getSourceContractId())) {
            validationReasons.add(REASON_MISSING_SOURCE_CONTRACT_ID);
        }
        if (isBlank(request.getWatchlistPoolProof())) {
            validationReasons.add(REASON_MISSING_WATCHLIST_POOL_PROOF);
        }
        if (request.getRequestedTimeframes().isEmpty()) {
            validationReasons.add(REASON_MISSING_REQUESTED_TIMEFRAMES);
        }
        if (request.getScanTimestamp() == null) {
            validationReasons.add(REASON_MISSING_SCAN_TIMESTAMP);
        }
        if (!"FAIL_CLOSED".equals(request.getStalePolicy())) {
            validationReasons.add(REASON_STALE_POLICY_NOT_FAIL_CLOSED);
        }
        if (!"FAIL_CLOSED".equals(request.getMissingDataPolicy())) {
            validationReasons.add(REASON_MISSING_DATA_POLICY_NOT_FAIL_CLOSED);
        }
        if (!request.isReviewOnly()) {
            validationReasons.add(REASON_REVIEW_ONLY_REQUIRED);
        }
        if (!request.isNotTradeInstruction()) {
            validationReasons.add(REASON_NOT_TRADE_INSTRUCTION_REQUIRED);
        }

        if (!validationReasons.isEmpty()) {
            return MarketReadRequestGuardValidationResult.blocked(
                    validationReasons,
                    request.getBlockingReasons(),
                    request.getRiskBlockers()
            );
        }

        return MarketReadRequestGuardValidationResult.reviewOnly(
                List.of(),
                request.getBlockingReasons(),
                request.getRiskBlockers()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
