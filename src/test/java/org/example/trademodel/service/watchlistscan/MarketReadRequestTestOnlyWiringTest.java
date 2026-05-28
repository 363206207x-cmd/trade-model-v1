package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.junit.jupiter.api.Test;

class MarketReadRequestTestOnlyWiringTest {

    private final MarketReadRequestTestOnlyWiring wiring = new MarketReadRequestTestOnlyWiring();

    @Test
    void validReviewOnlyDtoFlowsThroughTestOnlyWiringToValidator() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(validRequest());

        assertThat(result.getStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY);
        assertThat(result.getValidationReasons()).contains("MARKET_READ_REQUEST_GUARD_REVIEW_ONLY");
        assertThat(result.isBlocked()).isFalse();
        assertSafetyFlags(result);
    }

    @Test
    void missingSourceContractIdRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithSourceContractId(null)
        );

        assertBlockedFor(result, "MISSING_SOURCE_CONTRACT_ID");
    }

    @Test
    void missingWatchlistPoolProofRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithWatchlistPoolProof("")
        );

        assertBlockedFor(result, "MISSING_WATCHLIST_POOL_PROOF");
    }

    @Test
    void missingRequestedTimeframesRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithTimeframes(List.of())
        );

        assertBlockedFor(result, "MISSING_REQUESTED_TIMEFRAMES");
    }

    @Test
    void missingScanTimestampRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithScanTimestamp(null)
        );

        assertBlockedFor(result, "MISSING_SCAN_TIMESTAMP");
    }

    @Test
    void stalePolicyInvalidRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithStalePolicy("ALLOW_STALE")
        );

        assertBlockedFor(result, "STALE_POLICY_NOT_FAIL_CLOSED");
    }

    @Test
    void missingDataPolicyInvalidRemainsBlocked() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(
                requestWithMissingDataPolicy("ALLOW_MISSING")
        );

        assertBlockedFor(result, "MISSING_DATA_POLICY_NOT_FAIL_CLOSED");
    }

    @Test
    void blockingReasonsPreservedThroughWiring() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(validRequest());

        assertThat(result.getBlockingReasons())
                .contains("manual_gate", "risk_guard_before_delivery", "MARKET_READ_REQUEST_REVIEW_ONLY");
        result.getBlockingReasons().add("mutated");
        assertThat(result.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersPreservedThroughWiring() {
        MarketReadRequestGuardValidationResult result = wiring.assembleReviewOnlyValidation(validRequest());

        assertThat(result.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        result.getRiskBlockers().add("mutated");
        assertThat(result.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void outputRemainsManualReviewRequired() {
        for (MarketReadRequestGuardValidationResult result : allRepresentativeResults()) {
            assertThat(result.isManualReviewRequired()).isTrue();
        }
    }

    @Test
    void outputRemainsNotTradeInstruction() {
        for (MarketReadRequestGuardValidationResult result : allRepresentativeResults()) {
            assertThat(result.isNotTradeInstruction()).isTrue();
        }
    }

    @Test
    void noMarketQuoteClientOrBinanceMarketQuoteClientDependency() {
        assertNoForbiddenSurface(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
    }

    @Test
    void noRuntimeLiveOrExternalDataRead() {
        assertNoForbiddenSurface(List.of("Runtime", "Live", "External", "DataSource", "Provider", "Jdbc"));
    }

    @Test
    void noScanOutputScoreCandidatePushReadinessPointOrTradingBehavior() {
        assertNoForbiddenSurface(List.of(
                "ScanOutput",
                "Score",
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution",
                "Entry",
                "Stop",
                "TakeProfit",
                "Tp",
                "Rr"
        ));
    }

    @Test
    void testOnlyWiringClassIsUnderTestScopeNotProductionServiceWiring() {
        String classLocation = MarketReadRequestTestOnlyWiring.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();

        assertThat(classLocation).contains("test-classes");
        assertThat(Files.exists(Path.of(
                "src/main/java/org/example/trademodel/service/watchlistscan/MarketReadRequestTestOnlyWiring.java"
        ))).isFalse();
        assertThat(MarketReadRequestTestOnlyWiring.class.getAnnotations()).isEmpty();
    }

    private List<MarketReadRequestGuardValidationResult> allRepresentativeResults() {
        return List.of(
                wiring.assembleReviewOnlyValidation(validRequest()),
                wiring.assembleReviewOnlyValidation(null),
                wiring.assembleReviewOnlyValidation(requestWithSourceContractId(null)),
                wiring.assembleReviewOnlyValidation(requestWithWatchlistPoolProof(null)),
                wiring.assembleReviewOnlyValidation(requestWithTimeframes(List.of())),
                wiring.assembleReviewOnlyValidation(requestWithScanTimestamp(null)),
                wiring.assembleReviewOnlyValidation(requestWithStalePolicy("ALLOW_STALE")),
                wiring.assembleReviewOnlyValidation(requestWithMissingDataPolicy("ALLOW_MISSING")),
                new MarketReadRequestTestOnlyWiring(null).assembleReviewOnlyValidation(validRequest())
        );
    }

    private MarketReadRequestDTO validRequest() {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate", "risk_guard_before_delivery")
        );
    }

    private MarketReadRequestDTO requestWithSourceContractId(String sourceContractId) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                sourceContractId,
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithWatchlistPoolProof(String watchlistPoolProof) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                watchlistPoolProof,
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithTimeframes(List<String> requestedTimeframes) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                requestedTimeframes,
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithScanTimestamp(Instant scanTimestamp) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                scanTimestamp,
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithStalePolicy(String stalePolicy) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                stalePolicy,
                "FAIL_CLOSED",
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithMissingDataPolicy(String missingDataPolicy) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                missingDataPolicy,
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );
    }

    private void assertBlockedFor(MarketReadRequestGuardValidationResult result, String reason) {
        assertThat(result.getStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.BLOCKED);
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getValidationReasons()).contains(reason, "MARKET_READ_REQUEST_GUARD_FAIL_CLOSED");
        assertSafetyFlags(result);
    }

    private void assertSafetyFlags(MarketReadRequestGuardValidationResult result) {
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isFailClosed()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                MarketReadRequestTestOnlyWiring.class,
                MarketReadRequestGuardValidator.class,
                MarketReadRequestGuardValidationResult.class,
                MarketReadRequestGuardValidationStatusEnum.class
        )) {
            assertNoFragment(type.getSimpleName(), forbiddenFragments);
            for (Annotation annotation : type.getAnnotations()) {
                assertNoFragment(annotation.annotationType().getName(), forbiddenFragments);
            }
            for (Field field : type.getDeclaredFields()) {
                assertNoFragment(field.getName(), forbiddenFragments);
                assertNoFragment(field.getType().getSimpleName(), forbiddenFragments);
            }
            for (Method method : type.getDeclaredMethods()) {
                assertNoFragment(method.getName(), forbiddenFragments);
                assertNoFragment(method.getReturnType().getSimpleName(), forbiddenFragments);
                for (Class<?> parameterType : method.getParameterTypes()) {
                    assertNoFragment(parameterType.getSimpleName(), forbiddenFragments);
                }
            }
        }
    }

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase();
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase());
        }
    }
}
