package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;

class MarketReadRequestTestOnlyWiring {

    private static final String REASON_GUARD_MISSING = "TEST_ONLY_MARKET_READ_REQUEST_GUARD_MISSING";
    private static final String REASON_GUARD_RESULT_MISSING = "TEST_ONLY_MARKET_READ_REQUEST_GUARD_RESULT_MISSING";

    private final MarketReadRequestGuardValidator guardValidator;

    MarketReadRequestTestOnlyWiring() {
        this(new MarketReadRequestGuardValidator());
    }

    MarketReadRequestTestOnlyWiring(MarketReadRequestGuardValidator guardValidator) {
        this.guardValidator = guardValidator;
    }

    MarketReadRequestGuardValidationResult assembleReviewOnlyValidation(MarketReadRequestDTO request) {
        if (guardValidator == null) {
            return MarketReadRequestGuardValidationResult.blocked(
                    List.of(REASON_GUARD_MISSING),
                    blockingReasonsOf(request),
                    riskBlockersOf(request)
            );
        }

        MarketReadRequestGuardValidationResult result = guardValidator.validate(request);
        if (result == null) {
            return MarketReadRequestGuardValidationResult.blocked(
                    List.of(REASON_GUARD_RESULT_MISSING),
                    blockingReasonsOf(request),
                    riskBlockersOf(request)
            );
        }

        return result;
    }

    private static List<String> blockingReasonsOf(MarketReadRequestDTO request) {
        return request == null ? List.of() : request.getBlockingReasons();
    }

    private static List<String> riskBlockersOf(MarketReadRequestDTO request) {
        return request == null ? List.of() : request.getRiskBlockers();
    }
}
