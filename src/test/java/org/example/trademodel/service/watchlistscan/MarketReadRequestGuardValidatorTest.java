package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.junit.jupiter.api.Test;

class MarketReadRequestGuardValidatorTest {

    private final MarketReadRequestGuardValidator validator = new MarketReadRequestGuardValidator();

    @Test
    void validReviewOnlyDtoPassesAsReviewOnlyValidation() {
        MarketReadRequestGuardValidationResult result = validator.validate(validRequest());

        assertThat(result.getStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY);
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getValidationReasons()).contains("MARKET_READ_REQUEST_GUARD_REVIEW_ONLY");
        assertSafetyFlags(result);
    }

    @Test
    void nullRequestBlocked() {
        MarketReadRequestGuardValidationResult result = validator.validate(null);

        assertThat(result.getStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.BLOCKED);
        assertThat(result.getValidationReasons())
                .contains("NULL_MARKET_READ_REQUEST", "MARKET_READ_REQUEST_GUARD_FAIL_CLOSED");
        assertSafetyFlags(result);
    }

    @Test
    void missingSourceContractIdBlocked() {
        MarketReadRequestGuardValidationResult result = validator.validate(requestWithSourceContractId(null));

        assertBlockedFor(result, "MISSING_SOURCE_CONTRACT_ID");
    }

    @Test
    void missingWatchlistPoolProofBlocked() {
        MarketReadRequestGuardValidationResult result = validator.validate(requestWithWatchlistPoolProof(""));

        assertBlockedFor(result, "MISSING_WATCHLIST_POOL_PROOF");
    }

    @Test
    void missingRequestedTimeframesBlocked() {
        MarketReadRequestGuardValidationResult result = validator.validate(requestWithTimeframes(List.of()));

        assertBlockedFor(result, "MISSING_REQUESTED_TIMEFRAMES");
    }

    @Test
    void missingScanTimestampBlocked() {
        MarketReadRequestGuardValidationResult result = validator.validate(requestWithScanTimestamp(null));

        assertBlockedFor(result, "MISSING_SCAN_TIMESTAMP");
    }

    @Test
    void stalePolicyMissingOrInvalidBlocked() throws Exception {
        MarketReadRequestDTO missingPolicy = validRequest();
        forceValue(missingPolicy, "stalePolicy", null);

        assertBlockedFor(validator.validate(missingPolicy), "STALE_POLICY_NOT_FAIL_CLOSED");
        assertBlockedFor(
                validator.validate(requestWithStalePolicy("ALLOW_STALE")),
                "STALE_POLICY_NOT_FAIL_CLOSED"
        );
    }

    @Test
    void missingDataPolicyMissingOrInvalidBlocked() throws Exception {
        MarketReadRequestDTO missingPolicy = validRequest();
        forceValue(missingPolicy, "missingDataPolicy", null);

        assertBlockedFor(validator.validate(missingPolicy), "MISSING_DATA_POLICY_NOT_FAIL_CLOSED");
        assertBlockedFor(
                validator.validate(requestWithMissingDataPolicy("ALLOW_MISSING")),
                "MISSING_DATA_POLICY_NOT_FAIL_CLOSED"
        );
    }

    @Test
    void reviewOnlyFalsePathIsBlocked() throws Exception {
        MarketReadRequestDTO request = validRequest();
        forceValue(request, "reviewOnly", false);

        assertBlockedFor(validator.validate(request), "REVIEW_ONLY_REQUIRED");
    }

    @Test
    void notTradeInstructionFalsePathIsBlocked() throws Exception {
        MarketReadRequestDTO request = validRequest();
        forceValue(request, "notTradeInstruction", false);

        assertBlockedFor(validator.validate(request), "NOT_TRADE_INSTRUCTION_REQUIRED");
    }

    @Test
    void blockingReasonsPreserved() {
        MarketReadRequestGuardValidationResult result = validator.validate(validRequest());

        assertThat(result.getBlockingReasons())
                .contains("manual_gate", "risk_guard_before_delivery", "MARKET_READ_REQUEST_REVIEW_ONLY");
        result.getBlockingReasons().add("mutated");
        assertThat(result.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersPreserved() {
        MarketReadRequestGuardValidationResult result = validator.validate(validRequest());

        assertThat(result.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        result.getRiskBlockers().add("mutated");
        assertThat(result.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void validatorResultRemainsManualReviewRequiredAndNotTradeInstruction() {
        List<MarketReadRequestGuardValidationResult> results = List.of(
                validator.validate(validRequest()),
                validator.validate(null),
                validator.validate(requestWithSourceContractId(null)),
                validator.validate(requestWithWatchlistPoolProof(null)),
                validator.validate(requestWithTimeframes(List.of())),
                validator.validate(requestWithScanTimestamp(null)),
                validator.validate(requestWithStalePolicy("ALLOW_STALE")),
                validator.validate(requestWithMissingDataPolicy("ALLOW_MISSING"))
        );

        for (MarketReadRequestGuardValidationResult result : results) {
            assertSafetyFlags(result);
        }
    }

    @Test
    void validatorHasNoMarketQuoteClientOrBinanceMarketQuoteClientDependency() {
        assertNoForbiddenSurface(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
    }

    @Test
    void validatorDoesNotReadRuntimeLiveOrExternalData() {
        assertNoForbiddenSurface(List.of("Runtime", "Live", "External", "DataSource", "Provider", "Jdbc"));
    }

    @Test
    void validatorDoesNotCreateScanOutputScoreCandidatePushReadinessPointOrTradingBehavior() {
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

    private void forceValue(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                MarketReadRequestGuardValidator.class,
                MarketReadRequestGuardValidationResult.class,
                MarketReadRequestGuardValidationStatusEnum.class
        )) {
            assertNoFragment(type.getSimpleName(), forbiddenFragments);
            for (Annotation annotation : type.getAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("org.springframework");
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
