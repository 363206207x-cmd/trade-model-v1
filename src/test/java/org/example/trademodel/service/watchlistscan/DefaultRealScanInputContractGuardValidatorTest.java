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

class DefaultRealScanInputContractGuardValidatorTest {

    private final DefaultRealScanInputContractGuardValidator validator =
            new DefaultRealScanInputContractGuardValidator();

    @Test
    void nullInputFailsClosed() {
        RealScanInputContractDTO result = validator.validate(null);

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("NULL_REAL_SCAN_INPUT_CONTRACT", "INCOMPLETE");
        assertSafetyFlags(result);
    }

    @Test
    void missingWatchlistPoolProofFailsClosed() {
        RealScanInputContractDTO result = validator.validate(validInput("", true));

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF);
        assertThat(result.getBlockingReasons()).contains("MISSING_WATCHLIST_POOL_PROOF");
        assertSafetyFlags(result);
    }

    @Test
    void nonWatchlistInputFailsClosed() {
        RealScanInputContractDTO result = validator.validate(validInput("watchlist:ETHUSDT:v1", false));

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafetyFlags(result);
    }

    @Test
    void validLookingInputRemainsReviewOnlyAndNotTradeInstruction() {
        RealScanInputContractDTO result = validator.validate(validInput("watchlist:BTCUSDT:v1", true));

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
    void validatorPreservesManualReviewAndNotTradeInstructionAcrossOutputs() {
        List<RealScanInputContractDTO> results = List.of(
                validator.validate(null),
                validator.validate(validInput("", true)),
                validator.validate(validInput("watchlist:SOLUSDT:v1", false)),
                validator.validate(validInput("watchlist:ADAUSDT:v1", true)),
                validator.validate(RealScanInputContractDTO.incomplete("BNBUSDT", List.of("manual_gate")))
        );

        for (RealScanInputContractDTO result : results) {
            assertSafetyFlags(result);
        }
    }

    @Test
    void blockedInputCannotBeUpgradedToReviewOnly() {
        RealScanInputContractDTO blockedMissingProof = validInput("", true);
        RealScanInputContractDTO blockedNotWatchlist = validInput("watchlist:XRPUSDT:v1", false);

        RealScanInputContractDTO missingProofResult = validator.validate(blockedMissingProof);
        RealScanInputContractDTO notWatchlistResult = validator.validate(blockedNotWatchlist);

        assertThat(missingProofResult.getStatus())
                .isEqualTo(RealScanInputContractStatusEnum.BLOCKED_MISSING_WATCHLIST_PROOF);
        assertThat(notWatchlistResult.getStatus())
                .isEqualTo(RealScanInputContractStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertSafetyFlags(missingProofResult);
        assertSafetyFlags(notWatchlistResult);
    }

    @Test
    void incompleteInputRemainsFailClosed() {
        RealScanInputContractDTO result = validator.validate(
                RealScanInputContractDTO.incomplete("DOGEUSDT", List.of("source_context_missing"))
        );

        assertThat(result.getStatus()).isEqualTo(RealScanInputContractStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "source_context_missing",
                "INCOMPLETE",
                "INCOMPLETE_REAL_SCAN_INPUT_CONTRACT"
        );
        assertSafetyFlags(result);
    }

    @Test
    void validatorDoesNotExposeDownstreamWorkflowSurface() {
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

        for (Class<?> type : List.of(
                RealScanInputContractGuardValidator.class,
                DefaultRealScanInputContractGuardValidator.class
        )) {
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
    }

    @Test
    void implementationExposesNoForbiddenDependencySurface() {
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
                "Webhook",
                "Telegram",
                "Email",
                "Provider",
                "Order",
                "Execution",
                "AutoTrading"
        );

        for (Class<?> type : List.of(
                RealScanInputContractGuardValidator.class,
                DefaultRealScanInputContractGuardValidator.class
        )) {
            assertNoFragment(type.getName(), forbiddenFragments);
            for (Annotation annotation : type.getAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("org.springframework");
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

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase();
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase());
        }
    }
}
