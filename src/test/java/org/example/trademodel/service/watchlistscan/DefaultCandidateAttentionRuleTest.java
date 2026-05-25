package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionStatusEnum;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultCandidateAttentionRuleTest {

    private final DefaultCandidateAttentionRule rule = new DefaultCandidateAttentionRule();

    @Test
    void nullScoreFailsClosed() {
        CandidateAttentionDTO result = rule.evaluate("BTCUSDT", null);

        assertThat(result.getAttentionStatus()).isEqualTo(CandidateAttentionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blankSymbolFailsClosed() {
        CandidateAttentionDTO result = rule.evaluate(" ", reviewOnlyScore("BTCUSDT"));

        assertThat(result.getAttentionStatus()).isEqualTo(CandidateAttentionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unsafeScoreFailsClosed() throws Exception {
        CandidateAttentionDTO result = rule.evaluate("BTCUSDT", unsafeScore());

        assertThat(result.getAttentionStatus()).isEqualTo(CandidateAttentionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_UNSAFE", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonReviewOnlyScoreFailsClosed() {
        WatchlistScanScoreDTO score = WatchlistScanScoreDTO.disabled(
                "BTCUSDT",
                List.of("SCANSCORE_DISABLED_BY_TEST")
        );

        CandidateAttentionDTO result = rule.evaluate("BTCUSDT", score);

        assertThat(result.getAttentionStatus()).isEqualTo(CandidateAttentionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("SCANSCORE_DISABLED_BY_TEST", "DISABLED", "SCANSCORE_NOT_REVIEW_ONLY", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void safeReviewOnlyScoreReturnsReviewOnlyCandidateAttention() {
        CandidateAttentionDTO result = rule.evaluate("btcusdt", reviewOnlyScore("BTCUSDT"));

        assertThat(result.getAttentionStatus()).isEqualTo(CandidateAttentionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getAttentionReasons()).contains("CANDIDATE_ATTENTION_REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void scoreReasonsAndBlockingReasonsArePreserved() {
        WatchlistScanScoreDTO score = WatchlistScanScoreDTO.reviewOnly(
                "BTCUSDT",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "unit-test",
                List.of("SCANSCORE_REVIEW_ONLY_SKELETON", "SCORE_REASON"),
                List.of("AVAILABLE_REVIEW_ONLY", "BLOCKING_REASON")
        );

        CandidateAttentionDTO result = rule.evaluate("BTCUSDT", score);

        assertThat(result.getScoreReasons())
                .contains("SCANSCORE_REVIEW_ONLY_SKELETON", "SCORE_REASON");
        assertThat(result.getBlockingReasons())
                .contains("AVAILABLE_REVIEW_ONLY", "BLOCKING_REASON");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveNoExecutionDefaults() throws Exception {
        List<CandidateAttentionDTO> outputs = List.of(
                rule.evaluate("BTCUSDT", null),
                rule.evaluate(" ", reviewOnlyScore("BTCUSDT")),
                rule.evaluate("BTCUSDT", unsafeScore()),
                rule.evaluate("BTCUSDT", WatchlistScanScoreDTO.incomplete("BTCUSDT", List.of("MISSING"))),
                rule.evaluate("BTCUSDT", reviewOnlyScore("BTCUSDT"))
        );

        for (CandidateAttentionDTO output : outputs) {
            assertSafeNoExecutionDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION"));
        List<String> scoreReasons = new ArrayList<>(List.of("SCORE"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK"));

        CandidateAttentionDTO result = CandidateAttentionDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                attentionReasons,
                scoreReasons,
                blockingReasons
        );

        attentionReasons.add("MUTATED_ATTENTION");
        scoreReasons.add("MUTATED_SCORE");
        blockingReasons.add("MUTATED_BLOCK");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getScoreReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getAttentionReasons()).containsExactly("ATTENTION");
        assertThat(result.getScoreReasons()).containsExactly("SCORE");
        assertThat(result.getBlockingReasons()).containsExactly("BLOCK");
    }

    @Test
    void enumHasNoTradingOrExecutionStatuses() {
        List<String> forbidden = List.of(
                "BUY",
                "SELL",
                "LONG",
                "SHORT",
                "READY",
                "EXECUTABLE",
                "PUSHED",
                "PROMOTED"
        );

        for (CandidateAttentionStatusEnum status : CandidateAttentionStatusEnum.values()) {
            assertThat(status.name()).isNotIn(forbidden);
        }
    }

    @Test
    void reflectionDeclaresNoForbiddenFieldsOrMethods() {
        List<String> forbidden = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Scheduler",
                "Controller",
                "PushRecheckService",
                "PushSnapshotService",
                "DataSource",
                "JdbcTemplate",
                "Scheduled"
        );

        for (Field field : DefaultCandidateAttentionRule.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(CandidateAttentionRule.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = CandidateAttentionRule.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(CandidateAttentionDTO.class);

        for (Method method : DefaultCandidateAttentionRule.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("push");
            assertThat(methodName).doesNotContain("promote");
            assertThat(methodName).doesNotContain("readiness");
            assertThat(methodName).doesNotContain("order");
            assertThat(methodName).doesNotContain("execute");
            assertThat(methodName).doesNotContain("trade");
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static WatchlistScanScoreDTO reviewOnlyScore(String symbol) {
        return WatchlistScanScoreDTO.reviewOnly(
                symbol,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "unit-test",
                List.of("SCANSCORE_REVIEW_ONLY_SKELETON"),
                List.of("AVAILABLE_REVIEW_ONLY")
        );
    }

    private static WatchlistScanScoreDTO unsafeScore() throws Exception {
        WatchlistScanScoreDTO score = reviewOnlyScore("BTCUSDT");
        Field field = WatchlistScanScoreDTO.class.getDeclaredField("candidateAttentionAllowed");
        field.setAccessible(true);
        field.setBoolean(score, true);
        return score;
    }

    private static void assertSafeNoExecutionDefaults(CandidateAttentionDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isOpportunityPushAllowed()).isFalse();
        assertThat(result.isPromoteToHomeAllowed()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
