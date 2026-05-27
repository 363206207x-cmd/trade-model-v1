package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.RealScanInputContractDTO;
import org.example.trademodel.dto.watchlistscan.RealScanInputContractStatusEnum;
import org.junit.jupiter.api.Test;

class RealScanInputContractGuardValidatorTestOnlyWiringTest {

    private final TestOnlyGuardValidatorWiring wiring = new TestOnlyGuardValidatorWiring(
            new DefaultRealScanInputContractGuardValidator()
    );

    @Test
    void testOnlyWiringCanPassValidLookingInputIntoGuardAndRemainReviewOnly() {
        RealScanInputContractDTO result = wiring.validate(validInput("watchlist:BTCUSDT:v1", true));

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getWatchlistPoolMember()).isTrue();
        assertThat(result.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
        assertThat(result.getBlockingReasons()).contains(
                "REAL_SCAN_INPUT_REVIEW_ONLY",
                "REAL_SCAN_INPUT_GUARD_REVIEW_ONLY"
        );
        assertSafetyFlags(result);
    }

    @Test
    void missingWatchlistPoolProofRemainsBlockedMissingProof() {
        RealScanInputContractDTO result = wiring.validate(validInput("", true));

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF);
        assertThat(result.getBlockingReasons()).contains("MISSING_WATCHLIST_POOL_PROOF");
        assertSafetyFlags(result);
    }

    @Test
    void nonWatchlistInputRemainsBlockedNotWatchlist() {
        RealScanInputContractDTO result = wiring.validate(validInput("watchlist:ETHUSDT:v1", false));

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafetyFlags(result);
    }

    @Test
    void nullInputRemainsIncomplete() {
        RealScanInputContractDTO result = wiring.validate(null);

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("NULL_REAL_SCAN_INPUT_CONTRACT", "INCOMPLETE");
        assertSafetyFlags(result);
    }

    @Test
    void blockedInputCannotBeUpgradedToReviewOnly() {
        RealScanInputContractDTO blockedMissingProof = validInput("", true);
        RealScanInputContractDTO blockedNotWatchlist = validInput("watchlist:XRPUSDT:v1", false);

        RealScanInputContractDTO missingProofResult = wiring.validate(blockedMissingProof);
        RealScanInputContractDTO notWatchlistResult = wiring.validate(blockedNotWatchlist);

        assertThat(missingProofResult.getStatus())
                .isEqualTo(RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF);
        assertThat(notWatchlistResult.getStatus())
                .isEqualTo(RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertSafetyFlags(missingProofResult);
        assertSafetyFlags(notWatchlistResult);
    }

    @Test
    void allOutputsPreserveManualReviewAndNotTradeInstruction() {
        List<RealScanInputContractDTO> results = List.of(
                wiring.validate(null),
                wiring.validate(validInput("", true)),
                wiring.validate(validInput("watchlist:SOLUSDT:v1", false)),
                wiring.validate(validInput("watchlist:ADAUSDT:v1", true)),
                wiring.validate(RealScanInputContractDTO.incomplete("BNBUSDT", List.of("manual_gate")))
        );

        for (RealScanInputContractDTO result : results) {
            assertSafetyFlags(result);
        }
    }

    @Test
    void testOnlyWiringExposesNoProductionDependencySurface() {
        List<String> forbiddenFragments = List.of(
                "Spring",
                "Controller",
                "Scheduler",
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Mapper",
                "Repository",
                "DataSource",
                "JdbcTemplate",
                "Provider",
                "Webhook",
                "Telegram",
                "Email",
                "Order",
                "Execution",
                "AutoTrading"
        );

        for (Class<?> type : wiredTypes()) {
            assertNoForbiddenSurface(type, forbiddenFragments);
        }
    }

    @Test
    void testOnlyWiringDoesNotExposeDownstreamWorkflowSurface() {
        List<String> forbiddenFragments = List.of(
                "scanoutput",
                "score",
                "candidate",
                "push",
                "readiness",
                "point",
                "tradingaction",
                "order",
                "execution",
                "entry",
                "takeprofit",
                "riskreward"
        );

        for (Class<?> type : wiredTypes()) {
            assertNoForbiddenSurface(type, forbiddenFragments);
        }
    }

    private static List<Class<?>> wiredTypes() {
        return List.of(
                TestOnlyGuardValidatorWiring.class,
                RealScanInputContractGuardValidator.class,
                DefaultRealScanInputContractGuardValidator.class
        );
    }

    private RealScanInputContractDTO validInput(String watchlistPoolProof, boolean watchlistPoolMember) {
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

    private void assertSafetyFlags(RealScanInputContractDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private void assertNoForbiddenSurface(Class<?> type, List<String> forbiddenFragments) {
        assertNoFragment(type.getName(), forbiddenFragments);
        for (Annotation annotation : type.getAnnotations()) {
            assertNoFragment(annotation.annotationType().getName(), forbiddenFragments);
        }
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
            assertNoFragment(field.getType().getName(), forbiddenFragments);
        }
        for (Method method : type.getDeclaredMethods()) {
            assertNoFragment(method.getName(), forbiddenFragments);
            assertNoFragment(method.getReturnType().getName(), forbiddenFragments);
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertNoFragment(parameterType.getName(), forbiddenFragments);
            }
        }
    }

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase();
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase());
        }
    }

    private static final class TestOnlyGuardValidatorWiring {

        private final RealScanInputContractGuardValidator guardValidator;

        private TestOnlyGuardValidatorWiring(RealScanInputContractGuardValidator guardValidator) {
            this.guardValidator = guardValidator;
        }

        private RealScanInputContractDTO validate(RealScanInputContractDTO input) {
            return guardValidator.validate(input);
        }
    }
}
