package org.example.trademodel.dto.marketread;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MarketReadRequestDTOTest {

    @Test
    void defaultReviewOnlyIsTrue() {
        MarketReadRequestDTO request = new MarketReadRequestDTO();

        assertThat(request.isReviewOnly()).isTrue();
        assertThat(request.isManualReviewRequired()).isTrue();
    }

    @Test
    void defaultNotTradeInstructionIsTrue() {
        MarketReadRequestDTO request = new MarketReadRequestDTO();

        assertThat(request.isNotTradeInstruction()).isTrue();
    }

    @Test
    void frozenFieldsCanCarryContractData() {
        Instant scanTimestamp = Instant.parse("2026-05-28T00:00:00Z");

        MarketReadRequestDTO request = validRequest(scanTimestamp);

        assertThat(request.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(request.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(request.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
        assertThat(request.getWatchlistConfigVersion()).isEqualTo("watchlist-v1");
        assertThat(request.getRequestedScanReason()).isEqualTo("manual_review_scan");
        assertThat(request.getScanTimestamp()).isEqualTo(scanTimestamp);
        assertThat(request.getDataAvailabilityExpectation()).isEqualTo("EXPECTED_REVIEW_ONLY");
        assertThat(request.getGuardValidationStatus()).isEqualTo("REVIEW_ONLY");
        assertThat(request.isReviewOnly()).isTrue();
        assertThat(request.isNotTradeInstruction()).isTrue();
        assertThat(request.isManualReviewRequired()).isTrue();
    }

    @Test
    void blockingReasonsArePreserved() {
        MarketReadRequestDTO request = validRequest(Instant.parse("2026-05-28T00:00:00Z"));

        assertThat(request.getBlockingReasons())
                .contains("manual_gate", "risk_guard_before_delivery", "MARKET_READ_REQUEST_REVIEW_ONLY");
    }

    @Test
    void riskBlockersArePreserved() {
        List<String> riskBlockers = new ArrayList<>(List.of("stampede_review", "risk_action_guard_required"));

        MarketReadRequestDTO request = MarketReadRequestDTO.reviewOnly(
                "ETHUSDT",
                "market-read-request-002",
                "real-scan-input-contract-002",
                "watchlist:ETHUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m", "1h"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                riskBlockers,
                "REVIEW_ONLY",
                List.of("manual_gate")
        );

        riskBlockers.add("mutated");

        assertThat(request.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");

        request.getRiskBlockers().add("mutated");

        assertThat(request.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void requestedTimeframesArePreserved() {
        List<String> requestedTimeframes = new ArrayList<>(List.of("15m", "1h"));

        MarketReadRequestDTO request = MarketReadRequestDTO.reviewOnly(
                "SOLUSDT",
                "market-read-request-003",
                "real-scan-input-contract-003",
                "watchlist:SOLUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                requestedTimeframes,
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("risk_action_guard_required"),
                "REVIEW_ONLY",
                List.of("manual_gate")
        );

        requestedTimeframes.add("mutated");

        assertThat(request.getRequestedTimeframes()).containsExactly("15m", "1h");

        request.getRequestedTimeframes().add("mutated");

        assertThat(request.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void sourceContractIdIsPreserved() {
        MarketReadRequestDTO request = validRequest(Instant.parse("2026-05-28T00:00:00Z"));

        assertThat(request.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        MarketReadRequestDTO request = validRequest(Instant.parse("2026-05-28T00:00:00Z"));

        assertThat(request.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void stalePolicyCanRepresentFailClosed() {
        MarketReadRequestDTO request = MarketReadRequestDTO.reviewOnly(
                "BNBUSDT",
                "market-read-request-004",
                "real-scan-input-contract-004",
                "watchlist:BNBUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                null,
                "FAIL_CLOSED",
                List.of(),
                "REVIEW_ONLY",
                List.of()
        );

        assertThat(request.getStalePolicy()).isEqualTo("FAIL_CLOSED");
    }

    @Test
    void missingDataPolicyCanRepresentFailClosed() {
        MarketReadRequestDTO request = MarketReadRequestDTO.reviewOnly(
                "BNBUSDT",
                "market-read-request-005",
                "real-scan-input-contract-005",
                "watchlist:BNBUSDT:v1",
                "watchlist-v1",
                "manual_review_scan",
                List.of("15m"),
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                null,
                List.of(),
                "REVIEW_ONLY",
                List.of()
        );

        assertThat(request.getMissingDataPolicy()).isEqualTo("FAIL_CLOSED");
    }

    @Test
    void missingContractInputsAreRepresentedAsBlockedReasons() {
        MarketReadRequestDTO request = new MarketReadRequestDTO();

        assertThat(request.getBlockingReasons())
                .contains(
                        "BLOCKED_MISSING_SOURCE_CONTRACT_ID",
                        "BLOCKED_MISSING_WATCHLIST_POOL_PROOF",
                        "BLOCKED_MISSING_REQUESTED_TIMEFRAMES",
                        "BLOCKED_MISSING_SCAN_TIMESTAMP"
                );
        assertThat(request.getStalePolicy()).isEqualTo("FAIL_CLOSED");
        assertThat(request.getMissingDataPolicy()).isEqualTo("FAIL_CLOSED");
    }

    @Test
    void dtoContainsNoForbiddenExecutableTradingProviderFieldsByReflection() {
        Set<String> fieldNames = Arrays.stream(MarketReadRequestDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fieldNames)
                .containsExactlyInAnyOrder(
                        "symbol",
                        "requestId",
                        "sourceContractId",
                        "watchlistPoolProof",
                        "watchlistConfigVersion",
                        "requestedScanReason",
                        "requestedTimeframes",
                        "scanTimestamp",
                        "dataAvailabilityExpectation",
                        "stalePolicy",
                        "missingDataPolicy",
                        "riskBlockers",
                        "reviewOnly",
                        "notTradeInstruction",
                        "guardValidationStatus",
                        "blockingReasons"
                );

        assertThat(fieldNames)
                .doesNotContain(
                        "price",
                        "entry",
                        "stop",
                        "takeProfit",
                        "tp",
                        "rr",
                        "orderId",
                        "executionId",
                        "providerCredential",
                        "apiKey",
                        "secret",
                        "message",
                        "telegram",
                        "email",
                        "webhook",
                        "readiness",
                        "score",
                        "candidate",
                        "push"
                );
    }

    private MarketReadRequestDTO validRequest(Instant scanTimestamp) {
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
                List.of("manual_gate", "risk_guard_before_delivery")
        );
    }
}
