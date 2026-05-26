package org.example.trademodel.dto.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealScanInputContractDTOTest {

    @Test
    void defaultManualReviewSafetyFlagsRemainTrue() {
        RealScanInputContractDTO input = RealScanInputContractDTO.incomplete(
                "BTCUSDT",
                List.of("missing_watchlist_context")
        );

        assertThat(input.getStatus()).isEqualTo(RealScanInputContractStatusEnum.INCOMPLETE);
        assertThat(input.isManualReviewRequired()).isTrue();
        assertThat(input.isNotTradeInstruction()).isTrue();
        assertThat(input.getBlockingReasons()).contains("missing_watchlist_context", "INCOMPLETE");
    }

    @Test
    void missingWatchlistProofFailsClosed() {
        RealScanInputContractDTO input = validReviewOnlyInput("", true);

        assertThat(input.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF);
        assertThat(input.getWatchlistPoolMember()).isTrue();
        assertThat(input.getBlockingReasons()).contains("MISSING_WATCHLIST_POOL_PROOF");
        assertThat(input.isManualReviewRequired()).isTrue();
        assertThat(input.isNotTradeInstruction()).isTrue();
    }

    @Test
    void nonWatchlistInputFailsClosed() {
        RealScanInputContractDTO input = validReviewOnlyInput("watchlist:BTCUSDT:v1", false);

        assertThat(input.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(input.getWatchlistPoolMember()).isFalse();
        assertThat(input.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertThat(input.isManualReviewRequired()).isTrue();
        assertThat(input.isNotTradeInstruction()).isTrue();
    }

    @Test
    void validLookingInputRemainsReviewOnlyAndNotTradeInstruction() {
        RealScanInputContractDTO input = validReviewOnlyInput("watchlist:BTCUSDT:v1", true);

        assertThat(input.getStatus()).isEqualTo(RealScanInputContractStatusEnum.REVIEW_ONLY);
        assertThat(input.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(input.getSource()).isEqualTo("WATCHLIST_POOL");
        assertThat(input.getRequestedScanReason()).isEqualTo("manual_review_scan");
        assertThat(input.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
        assertThat(input.getWatchlistConfigVersion()).isEqualTo("watchlist-v1");
        assertThat(input.isMarketReadRequired()).isTrue();
        assertThat(input.isDataAvailabilityExpected()).isTrue();
        assertThat(input.getStaleInputBehavior()).isEqualTo("FAIL_CLOSED");
        assertThat(input.getMissingInputBehavior()).isEqualTo("FAIL_CLOSED");
        assertThat(input.getBlockingReasons()).contains("REAL_SCAN_INPUT_REVIEW_ONLY");
        assertThat(input.isManualReviewRequired()).isTrue();
        assertThat(input.isNotTradeInstruction()).isTrue();
    }

    @Test
    void listFieldsAreDefensivelyCopied() {
        List<String> requestedTimeframes = new ArrayList<>(List.of("15m", "1h"));
        List<String> riskBlockers = new ArrayList<>(List.of("stampede_review"));
        List<String> reviewOnlySafetyFlags = new ArrayList<>(List.of("review_only"));
        List<String> blockingReasons = new ArrayList<>(List.of("manual_gate"));

        RealScanInputContractDTO input = RealScanInputContractDTO.reviewOnly(
                "ETHUSDT",
                "WATCHLIST_POOL",
                "manual_review_scan",
                true,
                "watchlist:ETHUSDT:v1",
                "watchlist-v1",
                requestedTimeframes,
                Instant.parse("2026-05-26T00:00:00Z"),
                true,
                true,
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                riskBlockers,
                reviewOnlySafetyFlags,
                blockingReasons
        );

        requestedTimeframes.add("mutated");
        riskBlockers.add("mutated");
        reviewOnlySafetyFlags.add("mutated");
        blockingReasons.add("mutated");

        assertThat(input.getRequestedTimeframes()).containsExactly("15m", "1h");
        assertThat(input.getRiskBlockers()).containsExactly("stampede_review");
        assertThat(input.getReviewOnlySafetyFlags()).containsExactly("review_only");
        assertThat(input.getBlockingReasons()).containsExactly("manual_gate", "REAL_SCAN_INPUT_REVIEW_ONLY");

        input.getRequestedTimeframes().add("mutated");
        input.getRiskBlockers().add("mutated");
        input.getReviewOnlySafetyFlags().add("mutated");
        input.getBlockingReasons().add("mutated");

        assertThat(input.getRequestedTimeframes()).containsExactly("15m", "1h");
        assertThat(input.getRiskBlockers()).containsExactly("stampede_review");
        assertThat(input.getReviewOnlySafetyFlags()).containsExactly("review_only");
        assertThat(input.getBlockingReasons()).containsExactly("manual_gate", "REAL_SCAN_INPUT_REVIEW_ONLY");
    }

    @Test
    void enumNamesExposeNoForbiddenSurface() {
        List<String> forbiddenTerms = List.of(
                "BUY",
                "SELL",
                "LONG",
                "SHORT",
                "READY",
                "EXECUTABLE",
                "SENT",
                "TRADE",
                "ORDER",
                "ENTRY",
                "STOP",
                "TAKE_PROFIT"
        );

        for (String enumName : Arrays.stream(RealScanInputContractStatusEnum.values()).map(Enum::name).toList()) {
            for (String forbiddenTerm : forbiddenTerms) {
                assertThat(enumName).doesNotContain(forbiddenTerm);
            }
        }
    }

    @Test
    void dtoFieldsAndMethodNamesExposeNoForbiddenSurface() {
        List<String> forbiddenTerms = List.of(
                "order",
                "execution",
                "entry",
                "stop",
                "takeprofit",
                "riskreward",
                "provider",
                "externalchannel",
                "messagesending",
                "readiness"
        );

        for (Field field : RealScanInputContractDTO.class.getDeclaredFields()) {
            String fieldName = field.getName().toLowerCase();
            for (String forbiddenTerm : forbiddenTerms) {
                assertThat(fieldName).doesNotContain(forbiddenTerm);
            }
        }
        for (Method method : RealScanInputContractDTO.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            for (String forbiddenTerm : forbiddenTerms) {
                assertThat(methodName).doesNotContain(forbiddenTerm);
            }
        }
    }

    @Test
    void implementationHasNoForbiddenDependencySurface() {
        List<String> forbiddenTypeTerms = List.of(
                "Controller",
                "Scheduler",
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Webhook",
                "Telegram",
                "Email",
                "Mapper",
                "Repository",
                "DataSource",
                "JdbcTemplate",
                "Order",
                "Execution",
                "AutoTrading"
        );

        for (Class<?> type : List.of(RealScanInputContractDTO.class, RealScanInputContractStatusEnum.class)) {
            assertNoForbiddenTypeName(type, forbiddenTypeTerms);
            for (Field field : type.getDeclaredFields()) {
                assertNoForbiddenTypeName(field.getType(), forbiddenTypeTerms);
            }
            for (Method method : type.getDeclaredMethods()) {
                assertNoForbiddenTypeName(method.getReturnType(), forbiddenTypeTerms);
                for (Class<?> parameterType : method.getParameterTypes()) {
                    assertNoForbiddenTypeName(parameterType, forbiddenTypeTerms);
                }
            }
        }
    }

    private RealScanInputContractDTO validReviewOnlyInput(String watchlistPoolProof, boolean watchlistPoolMember) {
        return RealScanInputContractDTO.reviewOnly(
                "BTCUSDT",
                "WATCHLIST_POOL",
                "manual_review_scan",
                watchlistPoolMember,
                watchlistPoolProof,
                "watchlist-v1",
                List.of("15m", "1h"),
                Instant.parse("2026-05-26T00:00:00Z"),
                true,
                true,
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                List.of("risk_guard_before_push"),
                List.of("review_only", "not_trade_instruction"),
                List.of("manual_gate")
        );
    }

    private void assertNoForbiddenTypeName(Class<?> type, List<String> forbiddenTypeTerms) {
        String typeName = type.getName();
        for (String forbiddenTerm : forbiddenTypeTerms) {
            assertThat(typeName).doesNotContain(forbiddenTerm);
        }
    }
}
